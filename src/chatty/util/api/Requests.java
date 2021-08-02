
package chatty.util.api;

import chatty.Chatty;
import chatty.Helper;
import chatty.Room;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.StringUtil;
import chatty.util.api.StreamTagManager.StreamTagsListener;
import chatty.util.api.StreamTagManager.StreamTag;
import chatty.util.api.StreamTagManager.StreamTagListener;
import chatty.util.api.StreamTagManager.StreamTagPutListener;
import chatty.util.api.StreamTagManager.StreamTagsResult;
import chatty.util.api.TwitchApi.AutoModAction;
import chatty.util.api.TwitchApi.AutoModActionResult;
import chatty.util.api.TwitchApi.GameSearchListener;
import chatty.util.api.TwitchApi.RequestResultCode;
import chatty.util.api.TwitchApi.StreamMarkerResult;
import chatty.util.api.TwitchApiRequest.TwitchApiRequestResult;
import chatty.util.api.queue.QueuedApi;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class Requests {
    
    private static final Logger LOGGER = Logger.getLogger(Requests.class.getName());
    
    private final ExecutorService executor;
    private final TwitchApi api;
    private final QueuedApi newApi;
    private final TwitchApiResultListener listener;
    
    public Requests(TwitchApi api, TwitchApiResultListener listener) {
        executor = Executors.newCachedThreadPool();
        this.api = api;
        this.listener = listener;
        this.newApi = new QueuedApi();
    }
    
    
    //====================
    // Channel Information
    //====================
    
    protected void requestFollowers(String streamId, String stream) {
        String url = "https://api.twitch.tv/kraken/channels/"+streamId+"/follows?direction=desc&limit=100&offset=0";
        //url = "http://127.0.0.1/twitch/followers";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                api.followerManager.received(r.responseCode, stream, r.text);
            });
        }
    }
    
    protected void requestSubscribers(String streamId, String stream, String token) {
        String url = "https://api.twitch.tv/kraken/channels/"+streamId+"/subscriptions?direction=desc&limit=100&offset=0";
        if (Chatty.DEBUG) {
            url = "http://127.0.0.1/twitch/subscriptions_test";
        }
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            request.setToken(token);
            execute(request, r -> {
                api.subscriberManager.received(r.responseCode, stream, r.text);
            });
        }
    }
    
    public void getChannelInfo(String streamId, String stream) {
        if (stream == null || stream.isEmpty()) {
            return;
        }
        String url = "https://api.twitch.tv/kraken/channels/"+streamId;
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                api.channelInfoManager.handleChannelInfoResult(false, r.text, r.responseCode, stream);
            });
        }
    }

    
    //===================
    // Stream Information
    //===================
    
    protected void requestFollowedStreams(String token, int offset) {
        String url = "https://api.twitch.tv/kraken/streams/followed?stream_type=all&limit="
                + StreamInfoManager.FOLLOWED_STREAMS_LIMIT + "&offset="+offset;
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        request.setToken(token);
        execute(request, r -> {
            api.streamInfoManager.requestResultFollows(r.text, r.responseCode);
        });
    }
    
    /**
     * Sends a request to get streaminfo of the given stream.
     * 
     * @param stream 
     */
    protected void requestStreamInfo(String stream) {
        api.userIDs.getUserIDs(r -> {
            if (r.hasError()) {
                api.streamInfoManager.requestResult(null, -1, stream);
            } else {
                requestStreamInfoById(stream, r.getId(stream));
            }
        }, stream);
    }
    
    private void requestStreamInfoById(String stream, String userId) {
        /**
         * Switched to /streams?channel= request since it didn't have the
         * viewercount bug with stream_type=all. Added stream_type=all since
         * live VODs weren't contained anymore.
         */
        String url = "https://api.twitch.tv/kraken/streams/?channel="+userId+"&stream_type=all";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            request.setToken(api.getToken());
            execute(request, r -> {
                api.streamInfoManager.requestResult(r.text, r.responseCode, stream);
            });
        }
    }
    
    protected void requestStreamsInfo(Set<String> streams, Set<StreamInfo> streamInfosForRequest) {
        api.userIDs.getUserIDs(r -> {
            if (r.getValidIDs().isEmpty()) {
                api.streamInfoManager.requestResultStreams(null, -1, streamInfosForRequest);
            } else {
                requestStreamsInfoById(r.getValidIDs(), streamInfosForRequest);
            }
        }, streams);
    }
    
    private void requestStreamsInfoById(Collection<String> ids, Set<StreamInfo> expected) {
        String streamsString = StringUtil.join(ids, ",");
        String url = "https://api.twitch.tv/kraken/streams?stream_type=all&offset=0&limit=100&channel=" + streamsString;
        //url = "http://127.0.0.1/twitch/streams";
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        request.setToken(api.getToken());
        execute(request, r -> {
            api.streamInfoManager.requestResultStreams(r.text, r.responseCode, expected);
        });
    }

    //=======
    // System
    //=======
    
    public void verifyToken(String token) {
        String url = "https://id.twitch.tv/oauth2/validate";
        // Not an old API request, but needs the same Authorization header
        TwitchApiRequest request = new TwitchApiRequest(url, null);
        request.setToken(token);
        execute(request, r -> {
            if (r.responseCode == 200) {
                TokenInfo tokenInfo = Parsing.parseVerifyToken(r.text);
                listener.tokenVerified(token, tokenInfo);
            }
            else if (r.responseCode == 401) {
                // Invalid token
                listener.tokenVerified(token, new TokenInfo());
            }
            else {
                // Another error occured
                listener.tokenVerified(token, null);
            }
        });
    }
    
    public void revokeToken(String token) {
        String url = "https://id.twitch.tv/oauth2/revoke?client_id="+Chatty.CLIENT_ID+"&token="+token;
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        request.setRequestType("POST");
        // Set so the token can be filtered from debug output
        request.setToken(token);
        execute(request, r -> {
            if (r.responseCode != 200) {
                listener.tokenRevoked("Failed to revoke token ("+r.responseCode+")");
            } else {
                listener.tokenRevoked(null);
            }
        });
    }
    
    public void requestUserIDs(Set<String> usernames) {
        String url = "https://api.twitch.tv/kraken/users?login="+StringUtil.join(usernames, ",");
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                api.userIDs.handleRequestResult(usernames, r.text);
            });
        }
    }

    //================
    // User Management
    //================
    
    public void followChannel(String userId, String targetId, String targetName, String token) {
        String url = String.format(
                "https://api.twitch.tv/kraken/users/%s/follows/channels/%s",
                userId,
                targetId);
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            request.setToken(token);
            request.setRequestType("PUT");
            execute(request, r -> {
                if (r.responseCode == 200) {
                    long followTime = Parsing.followGetTime(r.text);
                    if (followTime != -1 && System.currentTimeMillis() - followTime > 5000) {
                        listener.followResult(String.format("Already following '%s' (since %s)",
                                targetName,
                                DateTime.ago(followTime, 0, 2, 0, DateTime.Formatting.VERBOSE)));
                    } else {
                        listener.followResult("Now following '" + targetName + "'");
                    }
                } else if (r.responseCode == 404) {
                    listener.followResult("Couldn't follow '" + targetName + "' (channel not found)");
                } else if (r.responseCode == 401) {
                    listener.followResult("Couldn't follow '" + targetName + "' (access denied)");
                } else {
                    listener.followResult("Couldn't follow '" + targetName + "' (unknown error)");
                }
                if (r.responseCode != 200) {
                    listener.followResult("Note: Twitch planned to remove follow/unfollow functionality from their API on July 27, 2021.");
                }
            });
        }
    }
    
    public void unfollowChannel(String userId, String targetId, String targetName, String token) {
        String url = String.format(
                "https://api.twitch.tv/kraken/users/%s/follows/channels/%s",
                userId,
                targetId);
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            request.setToken(token);
            request.setRequestType("DELETE");
            execute(request, r -> {
                if (r.responseCode == 204) {
                    listener.followResult("No longer following '" + targetName + "'");
                } else if (r.responseCode == 404) {
                    listener.followResult("Couldn't unfollow '" + targetName + "' (channel not found)");
                } else if (r.responseCode == 401) {
                    listener.followResult("Couldn't unfollow '" + targetName + "' (access denied)");
                } else {
                    listener.followResult("Couldn't unfollow '" + targetName + "' (unknown error)");
                }
                if (r.responseCode != 204) {
                    listener.followResult("Note: Twitch planned to remove follow/unfollow functionality from their API on July 27, 2021.");
                }
            });
        }
    }

    public void getSingleFollower(String stream, String streamID, String user, String userID) {
        if (StringUtil.isNullOrEmpty(stream, user, streamID, userID)) {
            return;
        }
        String url = String.format(
                "https://api.twitch.tv/kraken/users/%s/follows/channels/%s",
                userID,
                streamID);
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                api.followerManager.receivedSingle(r.responseCode, stream, r.text, user);
            });
        }
    }
    
    //=================
    // Admin/Moderation
    //=================
    
    /**
     * 
     * @param userId
     * @param info
     * @param token 
     */
    public void putChannelInfo(String userId, ChannelInfo info, String token) {
        if (info == null || info.name == null) {
            return;
        }
        String url = "https://api.twitch.tv/kraken/channels/"+userId;
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            request.setToken(token);
            request.setData(api.channelInfoManager.makeChannelInfoJson(info), "PUT");
            execute(request, r -> {
                api.channelInfoManager.handleChannelInfoResult(true, r.text, r.responseCode, info.name);
            });
        }
    }
    
    private int allTagsRequestCount;
    
    public void getAllTags(StreamTagManager.StreamTagsListener listener) {
        allTagsRequestCount = 0;
        getAllTags(api.defaultToken, null, listener);
    }
    
    private void getAllTags(String token, String cursor, StreamTagManager.StreamTagsListener listener) {
        String url = "https://api.twitch.tv/helix/tags/streams?first=100";
        if (cursor != null) {
            url += "&after="+cursor;
        }
        allTagsRequestCount++;
        // Just in case
        LOGGER.info("Request "+allTagsRequestCount);
        if (allTagsRequestCount > 10) {
            return;
        }
        newApi.add(url, "GET", token, (result, responseCode) -> {
            if (responseCode == 200) {
                StreamTagsResult data = StreamTagManager.parseAllTags(result);
                if (data != null) {
                    listener.received(data.tags, null);
                    if (!StringUtil.isNullOrEmpty(data.cursor)) {
                        getAllTags(token, data.cursor, listener);
                    } else {
                        listener.received(null, null);
                    }
                    data.tags.forEach(t -> { api.communitiesManager.addTag(t); });
                } else {
                    listener.received(null, "Parse error");
                }
            } else {
                listener.received(null, "Error "+responseCode);
            }
        });
    }
    
    public void getTagsByIds(Set<String> ids, StreamTagsListener listener) {
        String parameters = "?tag_id="+StringUtil.join(ids, "&tag_id=");
        String url = "https://api.twitch.tv/helix/tags/streams"+parameters;
        newApi.add(url, "GET", api.defaultToken, (result, responseCode) -> {
            if (responseCode == 200) {
                StreamTagsResult data = StreamTagManager.parseAllTags(result);
                if (data != null) {
                    data.tags.forEach(t -> { api.communitiesManager.addTag(t); });
                    listener.received(data.tags, null);
                } else {
                    listener.received(null, "Parse error");
                }
            } else {
                listener.received(null, "Request error");
            }
        });
    }
    
    public void setStreamTags(String userId, Collection<StreamTag> tags,
            StreamTagPutListener listener) {
        List<String> tagIds = new ArrayList<>();
        tags.forEach(t -> tagIds.add(t.getId()));
        String url = "https://api.twitch.tv/helix/streams/tags?broadcaster_id="+userId;
        JSONObject data = new JSONObject();
        data.put("tag_ids", tagIds);
        newApi.add(url, "PUT", data.toJSONString(), api.defaultToken, (text, responseCode) -> {
            if (responseCode == 204) {
                listener.result(null);
            } else if (responseCode == 400 || responseCode == 403) {
                api.getInvalidStreamTags(tags, (t, e) -> {
                    if (e != null || t == null || t.isEmpty()) {
                        listener.result("Error "+responseCode);
                    } else {
                        listener.result("Invalid: "+t);
                    }
                });
            } else if (responseCode == 401) {
                listener.result("Access denied");
            } else {
                listener.result("Error "+responseCode);
            }
        });
    }
    
    public void getTagsByStream(String userId, StreamTagsListener listener) {
        String url = "https://api.twitch.tv/helix/streams/tags?broadcaster_id="+userId;
        newApi.add(url, "GET", api.defaultToken, (data, responseCode) -> {
            if (responseCode == 204 || responseCode == 404) {
                listener.received(null, null);
            } else if (responseCode == 200) {
                StreamTagsResult result = StreamTagManager.parseAllTags(data);
                if (result == null) {
                    listener.received(null, "Parse error");
                } else {
                    listener.received(result.tags, url);
                }
            } else {
                listener.received(null, "Error "+responseCode);
            }
        });
    }
    
    public void getGameSearch(String game, GameSearchListener listener) {
        if (game == null || game.isEmpty()) {
            return;
        }
        String encodedGame = "";
        try {
            encodedGame = URLEncoder.encode(game, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TwitchApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        final String url = "https://api.twitch.tv/kraken/search/games?query="+encodedGame;
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        execute(request, r -> {
            if (r.text != null) {
                Set<String> games = Parsing.parseGameSearch(r.text);
                if (games != null) {
                    listener.result(games);
                }
            }
        });
    }
    
    public void runCommercial(String userId, String stream, String token, int length) {
        String url = "https://api.twitch.tv/kraken/channels/"+userId+"/commercial";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            JSONObject data = new JSONObject();
            data.put("duration", length);
            request.setToken(token);
            request.setData(data.toJSONString(), "POST");
            request.setContentType("application/json");
            execute(request, r -> {
                String resultText = "Unknown response: " + r.responseCode;
                RequestResultCode resultCode = RequestResultCode.UNKNOWN;
                if (r.responseCode == 204 || r.responseCode == 200) { // Not sure from the docs, and hard to test without being partner
                    resultText = "Running commercial..";
                    resultCode = RequestResultCode.RUNNING_COMMERCIAL;
                } else if (r.responseCode == 422) {
                    resultText = "Commercial length not allowed or trying to run too early.";
                    resultCode = RequestResultCode.FAILED;
                } else if (r.responseCode == 401 || r.responseCode == 403) {
                    resultText = "Can't run commercial: Access denied";
                    resultCode = RequestResultCode.ACCESS_DENIED;
                    api.accessDenied();
                } else if (r.responseCode == 404) {
                    resultText = "Can't run commercial: Channel '" + stream + "' not found";
                    resultCode = RequestResultCode.INVALID_CHANNEL;
                }
                listener.runCommercialResult(stream, resultText, resultCode);
            });
        }
    }
    
    public void autoMod(AutoModAction action, String msgId, String token, String localUserId) {
        String url = "https://api.twitch.tv/helix/moderation/automod/message";
        JSONObject data = new JSONObject();
        data.put("user_id", localUserId);
        data.put("msg_id", msgId);
        data.put("action", action == AutoModAction.ALLOW ? "ALLOW" : "DENY");
        
        newApi.add(url, "POST", data.toJSONString(), token, (text, responseCode) -> {
            boolean handled = false;
            for (AutoModActionResult result : AutoModActionResult.values()) {
                if (responseCode == result.responseCode) {
                    listener.autoModResult(action, msgId, result);
                    handled = true;
                }
            }
            if (!handled) {
                listener.autoModResult(action, msgId, AutoModActionResult.OTHER_ERROR);
            }
        });
    }
    
    public void createStreamMarker(String userId, String description, String token, StreamMarkerResult listener) {
        Map<String, String> data = new HashMap<>();
        data.put("user_id", userId);
        if (description != null && !description.isEmpty()) {
            data.put("description", description);
        }
        newApi.add("https://api.twitch.tv/helix/streams/markers", "POST", data, token, (result, responseCode) -> {
            if (responseCode == 200) {
                listener.streamMarkerResult(null);
            } else if (responseCode == 401) {
                listener.streamMarkerResult("Required access not available (please check <Main - Login..> for 'Edit broadcast')");
            } else if (responseCode == 404) {
                listener.streamMarkerResult("No stream");
            } else if (responseCode == 403) {
                listener.streamMarkerResult("Access denied");
            } else {
                listener.streamMarkerResult("Unknown error ("+responseCode+")");
            }
        });
    }
    
    //=================
    // Chat / Emoticons
    //=================
    
    protected void requestGlobalBadges() {
        String url = "https://badges.twitch.tv/v1/badges/global/display?language=en";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                listener.receivedUsericons(api.badgeManager.handleGlobalBadgesResult(r.text));
            });
        }
    }
    
    protected void requestRoomBadges(String roomId, String stream) {
        String url = "https://badges.twitch.tv/v1/badges/channels/"+roomId+"/display?language=en";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                listener.receivedUsericons(api.badgeManager.handleRoomBadgesResult(r.text, stream));
            });
        }
    }
    
    public void requestEmotesByChannelId(String stream, String id, String requestId) {
        newApi.add("https://api.twitch.tv/helix/chat/emotes?broadcaster_id="+id, "GET", api.defaultToken, (result, responseCode) -> {
            EmoticonUpdate parsed = EmoticonParsing.parseEmoteList(result, EmoticonUpdate.Source.CHANNEL, stream, id);
            if (parsed != null) {
                listener.receivedEmoticons(parsed);
                api.setReceived(requestId);
                if (parsed.setsAdded != null) {
                    api.emoticonManager2.addRequested(parsed.setsAdded);
                }
            }
            else if (responseCode == 404) {
                api.setNotFound(requestId);
            }
            else {
                api.setError(requestId);
            }
        });
    }
    
    public void requestEmotesets(Set<String> emotesets) {
        if (emotesets != null && !emotesets.isEmpty()) {
            String emotesetsParam = StringUtil.join(emotesets, ",");
            String url = "https://api.twitch.tv/kraken/chat/emoticon_images?emotesets="+emotesetsParam;
            if (attemptRequest(url)) {
                TwitchApiRequest request = new TwitchApiRequest(url, "v5");
                execute(request, r -> {
                    EmoticonUpdate result = EmoticonParsing.parseEmoticonSets(r.text, EmoticonUpdate.Source.OTHER);
                    if (result != null) {
                        listener.receivedEmoticons(result);
                    } else {
                        api.emoticonManager2.addError(emotesets);
                    }
                });
            }
        }
        
            //requestResult(REQUEST_TYPE_EMOTICONS,"")
    }
    
    public void requestEmotesetsNew(Set<String> emotesets) {
        if (emotesets != null && !emotesets.isEmpty()) {
            String emotesetsParam = StringUtil.join(emotesets, "&emote_set_id=");
            String url = "https://api.twitch.tv/helix/chat/emotes/set?emote_set_id="+emotesetsParam;
            newApi.add(url, "GET", api.defaultToken, (text, responseCode) -> {
                EmoticonUpdate result = EmoticonParsing.parseEmoteList(text, EmoticonUpdate.Source.OTHER, null, null);
                if (result != null) {
                    listener.receivedEmoticons(result);
                }
                else {
                    api.emoticonManager2.addError(emotesets);
                }
            });
        }
    }
    
    public void requestUserEmotes(String userId) {
        String url = "https://api.twitch.tv/kraken/users/"+userId+"/emotes";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            request.setToken(api.defaultToken);
            execute(request, r -> {
                EmoticonUpdate result = EmoticonParsing.parseEmoticonSets(r.text, EmoticonUpdate.Source.USER_EMOTES);
                if (result != null) {
                    listener.receivedEmoticons(result);
                    api.setReceived("userEmotes");
                    if (result.setsAdded != null) {
                        /**
                         * New API may return more emotes (emotes with new id?)
                         * for same emotesets, so don't prevent those requests.
                         */
                        //api.emoticonManager2.addRequested(result.setsAdded);
                    }
                }
                else if (r.responseCode == 404) {
                    api.setNotFound("userEmotes");
                }
                else {
                    api.setError("userEmotes");
                }
            });
        }
    }
    
    public void requestCheerEmoticons(String channelId, String stream) {
        String url = "https://api.twitch.tv/kraken/bits/actions?channel_id="+channelId+"&include_sponsored=1";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                api.cheersManager2.dataReceived(r.text, stream, channelId);
            });
        }
    }
    
    //===================
    // Management Methods
    //===================
    
    public void execute(TwitchApiRequest request, RequestResultListener listener) {
        request.setOrigin(new TwitchApiRequestResult() {

            @Override
            public void requestResult(String url, String result, int responseCode, String error, String encoding, String token, String info) {
                int length = -1;
                if (result != null) {
                    length = result.length();
                }
                String encodingText = encoding == null ? "" : ", " + encoding;
                LOGGER.info("GOT (" + responseCode + ", " + length + encodingText
                        + "): " + filterToken(url, token)
                        + (token != null ? " (using authorization)" : "")
                        + (error != null ? " [" + error + "]" : ""));
                
                removeRequest(url);
                
                if (Debugging.isEnabled("requestresponse")) {
                    LOGGER.info(result);
                }
                
                listener.result(new RequestResult(result, responseCode));
            }
        });
        executor.execute(request);
    }
    
    public interface RequestResultListener {
        public void result(RequestResult result);
    }
    
    public static class RequestResult {
        
        public final String text;
        public final int responseCode;
        
        public RequestResult(String result, int responseCode) {
            this.text = result;
            this.responseCode = responseCode;
        }
        
    }
    
    private final Set<String> pendingRequest = new HashSet<>();
    
    /**
     * Checks if a request with the given url can be made. Returns true if no
     * request with that url is currently waiting for a response, false
     * otherwise.
     *
     * This also saves the stream this request url is associated with, so it
     * can more easily be retrieved when the reponse comes in.
     * 
     * @param url The URL of the request
     * @return true if request can be made, false if it shouldn't
     */
    public boolean attemptRequest(String url) {
        synchronized (pendingRequest) {
            if (!pendingRequest.contains(url)) {
                pendingRequest.add(url);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Removes the given url from the requests that are waiting for a response
     * and retrieves the name of the stream this url is associated with.
     * 
     * @param url The URL of the request.
     * @return The name of the stream associated with this request (or null if
     *  no stream was set for this url or the url wasn't found).
     */
    private void removeRequest(String url) {
        synchronized(pendingRequest) {
            pendingRequest.remove(url);
        }
    }
    
    public static String filterToken(String input, String token) {
        if (input != null && !StringUtil.isNullOrEmpty(token)) {
            return input.replace(token, "<token>");
        }
        return input;
    }
    
}
