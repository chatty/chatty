
package chatty.util.api;

import chatty.Chatty;
import chatty.Helper;
import chatty.Room;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.CommunitiesManager.CommunitiesListener;
import chatty.util.api.CommunitiesManager.Community;
import chatty.util.api.CommunitiesManager.CommunityListener;
import chatty.util.api.CommunitiesManager.CommunityPutListener;
import chatty.util.api.TwitchApi.GameSearchListener;
import chatty.util.api.TwitchApi.RequestResultCode;
import chatty.util.api.TwitchApiRequest.TwitchApiRequestResult;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
    private final TwitchApiResultListener listener;
    
    public Requests(TwitchApi api, TwitchApiResultListener listener) {
        executor = Executors.newCachedThreadPool();
        this.api = api;
        this.listener = listener;
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
    
    protected void requestFollowedStreams(String token, String nextUrl) {
        String url;
        if (nextUrl != null) {
            url = nextUrl;
        } else {
            url = "https://api.twitch.tv/kraken/streams/followed?limit="
                    + StreamInfoManager.FOLLOWED_STREAMS_LIMIT + "&offset=0";
        }
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
        String url = "https://api.twitch.tv/kraken/streams/"+userId;
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
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
        String url = "https://api.twitch.tv/kraken/streams?offset=0&limit=100&channel=" + streamsString;
        //url = "http://127.0.0.1/twitch/streams";
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        execute(request, r -> {
            api.streamInfoManager.requestResultStreams(r.text, r.responseCode, expected);
        });
    }

    //=======
    // System
    //=======
    
    public void verifyToken(String token) {
        String url = "https://api.twitch.tv/kraken/";
        //url = "http://127.0.0.1/twitch/token";
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        request.setToken(token);
        execute(request, r -> {
            TokenInfo tokenInfo = Parsing.parseVerifyToken(r.text);
            listener.tokenVerified(token, tokenInfo);
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
            });
        }
    }
    
    
    //=================
    // Admin/Moderation
    //=================
    
    /**
     * 
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
    
    public void getCommunitiesTop(CommunitiesManager.CommunityTopListener listener) {
        String url = "https://api.twitch.tv/kraken/communities/top?limit=100";
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        execute(request, r -> {
            Collection<Community> result = CommunitiesManager.parseTop(r.text);
            listener.received(result);
            result.forEach(c -> { api.communitiesManager.addCommunity(c); });
        });
    }
    
    public void getCommunityByName(String name, CommunityListener listener) {
        try {
            String url = "https://api.twitch.tv/kraken/communities?name="+URLEncoder.encode(name, "UTF-8");
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                Community result = CommunitiesManager.parse(r.text);
                if (r.responseCode == 404) {
                    listener.received(null, "Community not found.");
                } else {
                    api.communitiesManager.addCommunity(result);
                    listener.received(result, null);
                }
            });
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Requests.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void getCommunityById(String id, CommunityListener listener) {
        String url = "https://api.twitch.tv/kraken/communities/"+id;
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        execute(request, r -> {
            Community result = CommunitiesManager.parse(r.text);
            if (r.responseCode == 404) {
                listener.received(null, "Community not found.");
            } else {
                api.communitiesManager.addCommunity(result);
                listener.received(result, null);
            }
        });
    }
    
    public void setCommunities(String userId, List<String> communityIds,
            String token, CommunityPutListener listener) {
        String url = "https://api.twitch.tv/kraken/channels/"+userId+"/communities";
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        request.setToken(token);
        request.setContentType("application/json");
        JSONObject data = new JSONObject();
        data.put("community_ids", communityIds);
        request.setData(data.toJSONString(), "PUT");
        execute(request, r -> {
            if (r.responseCode == 204) {
                listener.result(null);
            } else {
                listener.result("Error");
            }
        });
    }
    
    public void getCommunities(String userId, CommunitiesListener listener) {
        String url = "https://api.twitch.tv/kraken/channels/"+userId+"/communities";
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        execute(request, r -> {
            if (r.responseCode == 204 || r.responseCode == 404) { // 404 just in case Twitch changes it
                listener.received(null, null);
            } else {
                List<Community> result = CommunitiesManager.parseCommunities(r.text);
                if (result == null) {
                    listener.received(null, "Communities error");
                } else {
                    for (Community c : result) {
                        api.communitiesManager.addCommunity(c);
                    }
                    listener.received(result, null);
                }
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
    
    public void autoMod(String action, String msgId, String token) {
        if (!action.equals("approve") && !action.equals("deny")) {
            return;
        }
        String url = "https://api.twitch.tv/kraken/chat/twitchbot/"+action;
        JSONObject data = new JSONObject();
        data.put("msg_id", msgId);
        TwitchApiRequest request = new TwitchApiRequest(url, "v5");
        request.setData(data.toJSONString(), "POST");
        request.setToken(token);
        execute(request, r -> {
            if (r.responseCode == 204) {
                if (action.equals("approve")) {
                    listener.autoModResult("approved", msgId);
                } else if (action.equals("deny")) {
                    listener.autoModResult("denied", msgId);
                }
            } else if (r.responseCode == 404) {
                listener.autoModResult("404", msgId);
            } else if (r.responseCode == 400) {
                listener.autoModResult("400", msgId);
            } else {
                listener.autoModResult("error", msgId);
            }
        });
    }
    
    
    //=================
    // Chat / Emoticons
    //=================
    
    public void requestChatInfo(String stream) {
        if (!Helper.isValidStream(stream)) {
            return;
        }
        String url = "https://api.twitch.tv/api/channels/"+stream+"/chat_properties";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, null);
            execute(request, r -> {
                listener.receivedChatInfo(ChatInfo.decode(stream, r.text));
            });
        }
    }
    
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
    
    /**
     * Request to get the emoticons list from the API. If the list is already
     * available in a local file and is recent, that one is used. Otherwise
     * a request is issued and the emoticons are received and parsed when that
     * request is answered.
     *
     * @param forcedUpdate
     */
    public void requestEmoticons(boolean forcedUpdate) {
            String url = "https://api.twitch.tv/kraken/chat/emoticon_images";
            //url = "http://127.0.0.1/twitch/emoticons";
            if (attemptRequest(url)) {
                TwitchApiRequest request = new TwitchApiRequest(url, "v5");
                execute(request, r -> {
                    api.emoticonManager.dataReceived(r.text, forcedUpdate);
                });
            }
            //requestResult(REQUEST_TYPE_EMOTICONS,"")
    }
    
    public void requestEmotesets(Set<Integer> emotesets) {
        if (emotesets != null && !emotesets.isEmpty()) {
            String emotesetsParam = StringUtil.join(emotesets, ",");
            String url = "https://api.twitch.tv/kraken/chat/emoticon_images?emotesets="+emotesetsParam;
            if (attemptRequest(url)) {
                TwitchApiRequest request = new TwitchApiRequest(url, "v5");
                execute(request, r -> {
                    Set<Emoticon> result = EmoticonManager.parseEmoticonSets(r.text);
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

    public void requestCheerEmoticons(boolean forcedUpdate) {
        String url = "https://api.twitch.tv/kraken/bits/actions";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                api.cheersManager.dataReceived(r.text, forcedUpdate);
            });
        }
    }
    
    public void requestCheerEmoticons(String channelId, String stream) {
        String url = "https://api.twitch.tv/kraken/bits/actions?channel_id="+channelId;
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            execute(request, r -> {
                api.cheersManager2.dataReceived(r.text, stream, channelId);
            });
        }
    }
    
    public void requestRooms(String channelId, String stream) {
        String url = "https://api.twitch.tv/kraken/chat/"+channelId+"/rooms";
        if (attemptRequest(url)) {
            TwitchApiRequest request = new TwitchApiRequest(url, "v5");
            request.setToken(api.defaultToken);
            execute(request, r -> {
                RoomsInfo result = Parsing.parseRoomsInfo(stream, r.text);
                listener.roomsInfo(result);
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
                        + "): " + url
                        + (token != null ? " (using authorization)" : "")
                        + (error != null ? " [" + error + "]" : ""));
                
                removeRequest(url);
                
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
    
}
