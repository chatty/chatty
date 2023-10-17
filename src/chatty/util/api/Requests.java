
package chatty.util.api;

import chatty.Chatty;
import chatty.Helper;
import chatty.Room;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.BlockedTermsManager.BlockedTerm;
import chatty.util.api.BlockedTermsManager.BlockedTerms;
import chatty.util.api.TwitchApi.AutoModAction;
import chatty.util.api.TwitchApi.AutoModActionResult;
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
import chatty.util.api.ResultManager.CategoryResult;
import chatty.util.api.ResultManager.ShieldModeResult;
import chatty.util.api.TokenInfo.Scope;
import chatty.util.api.TwitchApi.SimpleRequestResult;
import chatty.util.api.TwitchApi.SimpleRequestResultListener;
import chatty.util.api.eventsub.EventSubAddResult;
import chatty.util.api.queue.ResultListener;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
        String url = String.format("https://api.twitch.tv/helix/users/follows?to_id=%s&first=100",
                streamId);
        newApi.add(url, "GET", api.defaultToken, (r) -> {
            api.followerManager.received(r.responseCode, stream, r.text);
        });
    }
    
    public void requestFollowersNew(String streamId, String stream) {
        boolean modAccess = AccessChecker.instance().check(Helper.toChannel(stream), TokenInfo.Scope.CHANNEL_FOLLOWERS, true, false);
        // Use old API in some cases while still available
        if (!modAccess && !FollowerManager.forceNewFollowsApi()) {
            requestFollowers(streamId, stream);
            return;
        }
        
        String url = makeUrl("https://api.twitch.tv/helix/channels/followers",
                "broadcaster_id", streamId,
                "first", "100");
        newApi.add(url, "GET", api.defaultToken, r -> {
            api.followerManager.received(r.responseCode, stream, r.text);
        });
    }
    
    protected void requestSubscribers(String streamId, String stream) {
        String url = String.format("https://api.twitch.tv/helix/subscriptions?broadcaster_id=%s&first=100",
                streamId);
        newApi.add(url, "GET", api.defaultToken, r -> {
            api.subscriberManager.received(r.responseCode, stream, r.text);
        });
    }
    
    public void getChannelStatus(String streamId, String stream) {
        String url = "https://api.twitch.tv/helix/channels?broadcaster_id="+streamId;
        newApi.add(url, "GET", api.defaultToken, r -> {
            if (r.responseCode == 200) {
                List<ChannelStatus> parsed = ChannelStatus.parseJson(r.text);
                if (parsed != null && parsed.size() > 0) {
                    ChannelStatus status = parsed.get(0);
                    listener.receivedChannelStatus(status, RequestResultCode.SUCCESS);
                    if (status.hasCategoryId()) {
                        api.resultManager.inform(ResultManager.Type.CATEGORY_RESULT, (CategoryResult l) -> {
                            l.result(Arrays.asList(new StreamCategory[]{status.category}));
                        });
                    }
                }
                else {
                    listener.receivedChannelStatus(ChannelStatus.createInvalid(streamId, stream), RequestResultCode.NOT_FOUND);
                }
            }
            else {
                listener.receivedChannelStatus(ChannelStatus.createInvalid(streamId, stream), RequestResultCode.UNKNOWN);
            }
        });
    }
    
    //===================
    // Stream Information
    //===================
    
    protected void requestFollowedStreams(String token, String cursor) {
        String url = String.format(Locale.ROOT, "https://api.twitch.tv/helix/streams/followed?user_id=%s&first=%d",
                api.localUserId,
                StreamInfoManager.FOLLOWED_STREAMS_LIMIT);
        if (!StringUtil.isNullOrEmpty(cursor)) {
            url += "&after="+cursor;
        }
        newApi.add(url, "GET", api.defaultToken, r -> {
            api.streamInfoManager.requestResultFollows(r.text, r.responseCode);
        });
    }
    
    /**
     * Sends a request to get streaminfo of the given stream.
     * 
     * @param stream 
     */
    protected void requestStreamInfo(String stream) {
        String url = "https://api.twitch.tv/helix/streams?first=100&user_login="+stream;
        newApi.add(url, "GET", api.defaultToken, r -> {
            api.streamInfoManager.requestResult(r.text, r.responseCode, stream);
        });
    }
    
    protected void requestStreamsInfo(Set<String> streams, Set<StreamInfo> expected) {
        String url = "https://api.twitch.tv/helix/streams?first=100&"+makeNewApiParameters("user_login", streams);
        newApi.add(url, "GET", api.defaultToken, r -> {
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
    
    public void requestUserInfo(Set<String> usernames) {
        String url = "https://api.twitch.tv/helix/users?"+makeNewApiParameters("login", usernames);
        newApi.add(url, "GET", api.defaultToken, r -> {
            Collection<UserInfo> parsedResult = UserInfoManager.parseJSON(r.text);
            Map<String, String> ids = null;
            if (parsedResult != null) {
                ids = new HashMap<>();
                for (UserInfo info : parsedResult) {
                    ids.put(info.login, info.id);
                }
            }
            // Error or missing values are handled in these methods as well
            api.userInfoManager.resultReceived(usernames, parsedResult);
            api.userIDs.handleRequestResult(usernames, ids);
        });
    }
    
    public void requestUserIDs(Set<String> usernames) {
        requestUserInfo(usernames);
    }

    public void requestUserInfoById(Set<String> requestedIds) {
        String url = "https://api.twitch.tv/helix/users?" + makeNewApiParameters("id", requestedIds);
        newApi.add(url, "GET", api.defaultToken, r -> {
            Collection<UserInfo> parsedResult = UserInfoManager.parseJSON(r.text);
            Map<String, String> ids = null;
            Set<String> usernames = new HashSet<>();
            if (parsedResult != null) {
                ids = new HashMap<>();
                for (UserInfo info : parsedResult) {
                    ids.put(info.login, info.id);
                }
                
                usernames = ids.keySet();
            }
            
            // Error or missing values are handled in these methods as well
            api.userInfoManager.idResultReceived(requestedIds, parsedResult);
            api.userIDs.handleRequestResult(usernames, ids);
        });
    }

    //================
    // User Management
    //================

    public void getSingleFollower(String stream, String streamID, String user, String userID) {
        if (StringUtil.isNullOrEmpty(stream, user, streamID, userID)) {
            return;
        }
        String url = String.format(
                "https://api.twitch.tv/helix/users/follows?from_id=%s&to_id=%s",
                userID,
                streamID);
        newApi.add(url, "GET", api.defaultToken, r -> {
            api.followerManager.receivedSingle(r.responseCode, stream, r.text, user, false);
        });
    }
    
    public void getSingleFollowerNew(String stream, String streamID, String user, String userID) {
        boolean modAccess = AccessChecker.instance().check(Helper.toChannel(stream), TokenInfo.Scope.CHANNEL_FOLLOWERS, true, false);
        // Use old API in some cases while still available
        if (!modAccess && !FollowerManager.forceNewFollowsApi()) {
            getSingleFollower(stream, streamID, user, userID);
            return;
        }
        
        if (StringUtil.isNullOrEmpty(stream, user, streamID, userID)) {
            return;
        }
        if (userID.equals(api.localUserId)) {
            // User may not have access to the channel's followers, but to their own
            getSingleOwnFollow(stream, streamID, user, userID);
            return;
        }
        if (!modAccess) {
            return;
        }
        String url = makeUrl("https://api.twitch.tv/helix/channels/followers",
                "broadcaster_id", streamID,
                "user_id", userID);
        newApi.add(url, "GET", api.defaultToken, r -> {
            api.followerManager.receivedSingle(r.responseCode, stream, r.text, user, false);
        });
    }
    
    public void getSingleOwnFollow(String stream, String streamID, String user, String userID) {
        if (StringUtil.isNullOrEmpty(stream, user, streamID, userID)) {
            return;
        }
        String url = makeUrl("https://api.twitch.tv/helix/channels/followed",
                "broadcaster_id", streamID,
                "user_id", userID);
        newApi.add(url, "GET", api.defaultToken, r -> {
            api.followerManager.receivedSingle(r.responseCode, stream, r.text, user, true);
        });
    }
    
    //=================
    // Admin/Moderation
    //=================
    
    public void putChannelInfoNew(String userId, ChannelStatus info, String token) {
        if (info == null || info.channelLogin == null) {
            return;
        }
        String url = "https://api.twitch.tv/helix/channels?broadcaster_id=" + userId;
        newApi.add(url, "PATCH", info.makePutJson(), token, r -> {
            switch (r.responseCode) {
                case 204:
                    listener.putChannelInfoResult(TwitchApi.RequestResultCode.SUCCESS, null);
                    break;
                case 401:
                case 403:
                    listener.putChannelInfoResult(TwitchApi.RequestResultCode.ACCESS_DENIED, getErrorMessage(r.errorText));
                    break;
                default:
                    listener.putChannelInfoResult(TwitchApi.RequestResultCode.FAILED, getErrorMessage(r.errorText));
                    break;
            }
        });
    }
    
    public void getContentLabels() {
        newApi.add("https://api.twitch.tv/helix/content_classification_labels", "GET", api.defaultToken, r -> {
            if (r.responseCode == 200) {
                StreamLabels.dataReceived(r.text);
            }
        });
    }
    
    public void getGameSearch(String game, CategoryResult listener) {
        if (game == null || game.isEmpty()) {
            return;
        }
        String encodedGame = "";
        try {
            encodedGame = URLEncoder.encode(game, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TwitchApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        final String url = "https://api.twitch.tv/helix/search/categories?query="+encodedGame;
        newApi.add(url, "GET", api.defaultToken, r -> {
            if (r.text != null) {
                Set<StreamCategory> categories = Parsing.parseCategorySearch(r.text);
                if (categories != null) {
                    listener.result(categories);
                    api.resultManager.inform(ResultManager.Type.CATEGORY_RESULT, (CategoryResult l) -> {
                        l.result(categories);
                    });
                }
            }
        });
    }
    
    public void runCommercial(String userId, String stream, int length) {
        String url = "https://api.twitch.tv/helix/channels/commercial";
        String json = JSONUtil.listMapToJSON(
                "broadcaster_id", userId,
                "length", length
        );
        newApi.add(url, "POST", json, api.defaultToken, r -> {
            String resultText = "Failed to start commercial (error " + r.responseCode + ")";
            RequestResultCode resultCode = RequestResultCode.UNKNOWN;
            if (r.responseCode == 204 || r.responseCode == 200) {
                resultText = "Running commercial..";
                resultCode = RequestResultCode.RUNNING_COMMERCIAL;
            }
            listener.runCommercialResult(stream, resultText, resultCode);
        });
    }
    
    public void autoMod(AutoModAction action, String msgId, String token, String localUserId) {
        String url = "https://api.twitch.tv/helix/moderation/automod/message";
        String json = JSONUtil.listMapToJSON(
                "user_id", localUserId,
                "msg_id", msgId,
                "action", action == AutoModAction.ALLOW ? "ALLOW" : "DENY");
        
        newApi.add(url, "POST", json, token, r -> {
            boolean handled = false;
            for (AutoModActionResult result : AutoModActionResult.values()) {
                if (r.responseCode == result.responseCode) {
                    listener.autoModResult(action, msgId, result);
                    handled = true;
                }
            }
            if (!handled) {
                listener.autoModResult(action, msgId, AutoModActionResult.OTHER_ERROR);
            }
        });
    }
    
    public void getBlockedTerms(String streamId, String login, String cursor) {
        String url = String.format(Locale.ROOT, "https://api.twitch.tv/helix/moderation/blocked_terms?broadcaster_id=%s&moderator_id=%s&first=%d",
                streamId,
                api.localUserId,
                BlockedTermsManager.MAX_RESULTS_PER_REQUEST);
        if (!StringUtil.isNullOrEmpty(cursor)) {
            url += "&after="+cursor;
        }
        newApi.add(url, "GET", api.defaultToken, r -> {
            api.blockedTermsManager.resultReceived(streamId, login, r.text, r.responseCode);
        });
    }
    
    public void addBlockedTerm(String streamId, String streamName, String text, Consumer<BlockedTerm> listener) {
        String url = String.format("https://api.twitch.tv/helix/moderation/blocked_terms?broadcaster_id=%s&moderator_id=%s",
                streamId,
                api.localUserId);
        Map<String, String> data = new HashMap<>();
        data.put("text", text);
        newApi.add(url, "POST", data, api.defaultToken, r -> {
            BlockedTerms parsed = BlockedTerms.parse(r.text, streamId, streamName);
            if (parsed != null && !parsed.hasError() && parsed.data.size() == 1) {
                listener.accept(parsed.data.get(0));
            }
            else {
                listener.accept(null);
            }
        });
    }
    
    public void removeBlockedTerm(BlockedTerm term, Consumer<BlockedTerm> listener) {
        String url = String.format("https://api.twitch.tv/helix/moderation/blocked_terms?broadcaster_id=%s&moderator_id=%s&id=%s",
                term.streamId,
                api.localUserId,
                term.id);
        newApi.add(url, "DELETE", api.defaultToken, r -> {
            if (r.responseCode == 204) {
                listener.accept(term);
            }
            else {
                listener.accept(null);
            }
        });
    }
    
    public void createStreamMarker(String userId, String description, String token, StreamMarkerResult listener) {
        Map<String, String> data = new HashMap<>();
        data.put("user_id", userId);
        if (description != null && !description.isEmpty()) {
            data.put("description", description);
        }
        newApi.add("https://api.twitch.tv/helix/streams/markers", "POST", data, token, r -> {
            switch (r.responseCode) {
                case 200:
                    listener.streamMarkerResult(null);
                    break;
                case 401:
                    listener.streamMarkerResult("Required access not available (please check <Main - Login..> for 'Edit broadcast')");
                    break;
                case 404:
                    listener.streamMarkerResult("No stream");
                    break;
                case 403:
                    listener.streamMarkerResult("Access denied");
                    break;
                default:
                    listener.streamMarkerResult("Unknown error ("+r.responseCode+")");
                    break;
            }
        });
    }
    
    public void sendAnnouncement(String streamId, String message, String color) {
        String url = String.format("https://api.twitch.tv/helix/chat/announcements?broadcaster_id=%s&moderator_id=%s",
                streamId, api.localUserId);
        if (StringUtil.isNullOrEmpty(color)) {
            color = "primary";
        }
        String json = JSONUtil.listMapToJSON(
                "message", message,
                "color", StringUtil.toLowerCase(color)
        );
        newApi.add(url, "POST", json, api.defaultToken, r -> {
            if (r.responseCode == 204) {
                // All fine
            }
            else if (r.responseCode == 400) {
                listener.errorMessage("Invalid announcement message or color");
            }
            else if (r.responseCode == 401) {
                listener.errorMessage("Announcement access denied (check 'Main - Account' for access)");
            }
            else {
                listener.errorMessage(String.format("Sending announcement failed (%d)", r.responseCode));
            }
        });
    }
    
    public void ban(String streamId, String targetId, int length, String reason, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/moderation/bans",
                "broadcaster_id", streamId,
                "moderator_id", api.localUserId);
        
        JSONObject data = new JSONObject();
        data.put("user_id", targetId);
        data.put("reason", reason);
        if (length > 0) {
            data.put("duration", length);
        }
        JSONObject json = new JSONObject();
        json.put("data", data);
        newApi.add(url, "POST", json.toJSONString(), api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void unban(String streamId, String targetId, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/moderation/bans",
                "broadcaster_id", streamId,
                "moderator_id", api.localUserId,
                "user_id", targetId);
        newApi.add(url, "DELETE", api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void deleteMsg(String streamId, String msgId, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/moderation/chat",
                "broadcaster_id", streamId,
                "moderator_id", api.localUserId);
        if (!StringUtil.isNullOrEmpty(msgId)) {
            url += "&message_id="+msgId;
        }
        newApi.add(url, "DELETE", api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void shoutout(String streamId, String targetId, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/chat/shoutouts",
                "from_broadcaster_id", streamId,
                "moderator_id", api.localUserId,
                "to_broadcaster_id", targetId);
        newApi.add(url, "POST", api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void setVip(String streamId, String targetId, boolean add, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/channels/vips",
                "broadcaster_id", streamId,
                "user_id", targetId);
        newApi.add(url, add ? "POST" : "DELETE", api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void setModerator(String streamId, String targetId, boolean add, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/moderation/moderators",
                "broadcaster_id", streamId,
                "user_id", targetId);
        newApi.add(url, add ? "POST" : "DELETE", api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void getModerators(String streamId, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/moderation/moderators",
                "broadcaster_id", streamId,
                "first", "100");
        newApi.add(url, "GET", api.defaultToken, r -> {
            handleModerators(r, listener, "moderators");
        });
    }
    
    public void getVips(String streamId, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/channels/vips",
                "broadcaster_id", streamId,
                "first", "100");
        newApi.add(url, "GET", api.defaultToken, r -> {
            handleModerators(r, listener, "VIPs");
        });
    }
    
    private static void handleModerators(ResultListener.Result r, SimpleRequestResultListener listener, String type) {
        if (r.text != null) {
            String result = Parsing.parseModerators(r.text, type);
            if (result == null) {
                listener.accept(SimpleRequestResult.error("Error parsing list"));
            }
            else {
                listener.accept(SimpleRequestResult.result(result));
            }
        }
        else {
            handleResult(r, listener);
        }
    }
    
    public void startRaid(String streamId, String targetId, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/raids",
                "from_broadcaster_id", streamId,
                "to_broadcaster_id", targetId);
        newApi.add(url, "POST", api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void cancelRaid(String streamId, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/raids",
                "broadcaster_id", streamId);
        newApi.add(url, "DELETE", api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void whisper(String targetId, String msg, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/whispers",
                "from_user_id", api.localUserId,
                "to_user_id", targetId);
        String json = JSONUtil.listMapToJSON("message", msg);
        newApi.add(url, "POST", json, api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void updateChatSettings(String streamId, Object[] data, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/chat/settings",
                "broadcaster_id", streamId,
                "moderator_id", api.localUserId);
        String json = JSONUtil.listMapToJSON(data);
        newApi.add(url, "PATCH", json, api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    public void setColor(String color, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/chat/color",
                "user_id", api.localUserId,
                "color", makeColor(color));
        newApi.add(url, "PUT", api.defaultToken, r -> {
            handleResult(r, listener);
        });
    }
    
    private static String makeColor(String color) {
        color = StringUtil.toLowerCase(color);
        switch (color) {
            case "dodgerblue":
                return "dodger_blue";
            case "springgreen":
                return "spring_green";
            case "yellowgreen":
                return "yellow_green";
            case "orangered":
                return "orange_red";
            case "goldenrod":
                return "golden_rod";
            case "hotpink":
                return "hot_pink";
            case "cadetblue":
                return "cadet_blue";
            case "seagreen":
                return "sea_green";
            case "blueviolet":
                return "blue_violet";
        }
        return color;
    }
    
    @SuppressWarnings("unchecked") // JSONObject
    public void setShieldMode(String stream, String streamId, boolean enabled, SimpleRequestResultListener listener) {
        String url = makeUrl("https://api.twitch.tv/helix/moderation/shield_mode",
                "broadcaster_id", streamId,
                "moderator_id", api.localUserId);
        JSONObject data = new JSONObject();
        data.put("is_active", enabled);
//        JSONObject json = new JSONObject();
//        json.put("data", data);
        newApi.add(url, "PUT", data.toJSONString(), api.defaultToken, r -> {
            handleResult(r, listener);
            Parsing.ShieldModeStatus status = Parsing.ShieldModeStatus.decode(r.text, stream);
            if (status != null) {
                api.resultManager.inform(ResultManager.Type.SHIELD_MODE_RESULT, (ShieldModeResult l) -> {
                    l.result(status.stream, status.enabled);
                });
            }
        });
    }
    
    public void getShieldMode(String stream, String streamId, String requestId) {
        String url = makeUrl("https://api.twitch.tv/helix/moderation/shield_mode",
                "broadcaster_id", streamId,
                "moderator_id", api.localUserId);
        newApi.add(url, "GET", api.defaultToken, r -> {
            Parsing.ShieldModeStatus status = Parsing.ShieldModeStatus.decode(r.text, stream);
            if (status != null) {
                api.resultManager.inform(ResultManager.Type.SHIELD_MODE_RESULT, (ShieldModeResult l) -> {
                    l.result(status.stream, status.enabled);
                });
            }
            switch (r.responseCode) {
                case 200:
                    api.setReceived(requestId);
                    break;
                case 404:
                    api.setNotFound(requestId);
                    break;
                default:
                    api.setError(requestId);
                    break;
            }
        });
    }
    
    private static void handleResult(ResultListener.Result r, SimpleRequestResultListener listener) {
        if (String.valueOf(r.responseCode).startsWith("2")) {
            listener.accept(SimpleRequestResult.ok());
        }
        else if (!StringUtil.isNullOrEmpty(getErrorMessage(r.errorText))) {
            String msg = getErrorMessage(r.errorText);
            String scopeMissingMsg = checkForMissingScope(msg);
            if (scopeMissingMsg != null) {
                msg = scopeMissingMsg;
            }
            
            /**
             * Each response code can have different reasons, so the error msg
             * needs to be used. The error msg returned by the API seems more
             * aimed at users of the API rather than for output to the user, so
             * this changes some common errors to a more concise version.
             */
            Map<String, String> replace = new HashMap<>();
            replace.put("The ID in broadcaster_id must match the user ID found in the request's OAuth token.", "Access only for broadcaster.");
            replace.put("is already banned", "Already banned.");
            replace.put("is not banned", "Not banned.");
            replace.put("may not be banned", "Can't be banned.");
            for (Map.Entry<String, String> entry : replace.entrySet()) {
                if (msg.contains(entry.getKey())) {
                    msg = entry.getValue();
                    break;
                }
            }
            if (Debugging.isEnabled("errormsg")) {
                LOGGER.info(String.format("Code: %s Error: %s Output: %s",
                        r.responseCode, r.errorText, msg));
            }
            listener.accept(SimpleRequestResult.error(msg));
        }
        else {
            listener.accept(SimpleRequestResult.error(String.format("Error (%d)", r.responseCode)));
        }
    }
    
    private static String checkForMissingScope(String msg) {
        Pattern p = Pattern.compile("Missing scope: ([a-z:_]+)");
        Matcher m = p.matcher(msg);
        if (m.matches()) {
            String scopeString = m.group(1);
            Scope scope = Scope.fromScopeString(scopeString);
            if (scope != null) {
                return Language.getString("login.accessMissing", scope.label);
            }
        }
        return null;
    }
    
    //=================
    // Chat / Emoticons
    //=================
    
    protected void requestGlobalBadges() {
        String url = "https://api.twitch.tv/helix/chat/badges/global";
        newApi.add(url, "GET", api.defaultToken, r -> {
            listener.receivedUsericons(api.badgeManager.handleGlobalBadgesResult(r.text));
        });
    }
    
    protected void requestRoomBadges(String roomId, String stream) {
        String url = "https://api.twitch.tv/helix/chat/badges?broadcaster_id="+roomId;
        newApi.add(url, "GET", api.defaultToken, r -> {
            listener.receivedUsericons(api.badgeManager.handleRoomBadgesResult(r.text, stream));
        });
    }
    
    public void requestEmotesByChannelId(String stream, String id, String requestId) {
        newApi.add("https://api.twitch.tv/helix/chat/emotes?broadcaster_id="+id, "GET", api.defaultToken, r -> {
            EmoticonUpdate parsed = EmoticonParsing.parseEmoteList(r.text, EmoticonUpdate.Source.HELIX_CHANNEL, stream, id);
            if (parsed != null) {
                listener.receivedEmoticons(parsed);
                api.setReceived(requestId);
                if (parsed.setsAdded != null) {
                    api.emoticonManager2.addRequested(parsed.setsAdded);
                }
            }
            else if (r.responseCode == 404) {
                api.setNotFound(requestId);
            }
            else {
                api.setError(requestId);
            }
        });
    }
    
    public void requestEmotesetsNew(Set<String> emotesets) {
        if (emotesets != null && !emotesets.isEmpty()) {
            String emotesetsParam = StringUtil.join(emotesets, "&emote_set_id=");
            String url = "https://api.twitch.tv/helix/chat/emotes/set?emote_set_id="+emotesetsParam;
            newApi.add(url, "GET", api.defaultToken, r -> {
                EmoticonUpdate result = EmoticonParsing.parseEmoteList(r.text, EmoticonUpdate.Source.HELIX_SETS, null, null);
                if (result != null) {
                    listener.receivedEmoticons(result);
                }
                else {
                    api.emoticonManager2.addError(emotesets);
                }
            });
        }
    }
    
    public void requestCheerEmoticons(String channelId, String stream) {
        String url = "https://api.twitch.tv/helix/bits/cheermotes?broadcaster_id="+channelId;
        newApi.add(url, "GET", api.defaultToken, r -> {
            api.cheersManager2.dataReceived(r.text, stream, channelId);
        });
    }
    
    public void test() {
        String url = "https://api.twitch.tv/helix/...";
        newApi.add(url, "GET", api.defaultToken, r -> {
            System.out.println(r.text);
        });
    }
    
    public void addEventSub(String body, Consumer<EventSubAddResult> listener) {
        newApi.add("https://api.twitch.tv/helix/eventsub/subscriptions", "POST", body, api.defaultToken, r -> {
            listener.accept(EventSubAddResult.decode(r));
        });
    }
    
    public void removeEventSub(String id, Consumer<Integer> listener) {
        newApi.add("https://api.twitch.tv/helix/eventsub/subscriptions?id="+id, "DELETE", api.defaultToken, r -> {
            listener.accept(r.responseCode);
        });
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
    
    public static String makeNewApiParameters(String key, Collection<String> values) {
        return key+"="+StringUtil.join(values, "&"+key+"=");
    }
    
    public static String makeUrl(String base, String... args) {
        String result = base;
        for (int i=0; i<args.length; i+=2) {
            if (i == 0) {
                result += "?";
            }
            else {
                result += "&";
            }
            try {
                result += args[i]+"="+URLEncoder.encode(args[i+1], "UTF-8");
            }
            catch (UnsupportedEncodingException ex) {
                return null;
            }
        }
        return result;
    }
    
    public static String getCursor(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONObject pagination = (JSONObject) root.get("pagination");
            if (pagination != null) {
                String cursor = JSONUtil.getString(pagination, "cursor");
                return !StringUtil.isNullOrEmpty(cursor) ? cursor : null;
            }
        }
        catch (Exception ex) {
            LOGGER.warning("Error getting cursor: "+ex);
        }
        return null;
    }
    
    public static String getErrorMessage(String json) {
        if (json == null) {
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject o = (JSONObject) parser.parse(json);
            return (String) o.get("message");
        }
        catch (ParseException ex) {
            return null;
        }
    }
    
}
