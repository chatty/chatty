
package chatty.util.api;

import chatty.Chatty;
import chatty.Helper;
import chatty.util.api.usericons.Usericon;
import chatty.util.DateTime;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.usericons.UsericonFactory;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Handles TwitchApi requests and responses.
 * 
 * @author tduva
 */
public class TwitchApi {

    private final static Logger LOGGER = Logger.getLogger(TwitchApi.class.getName());
    
    private final HashMap<String,String> pendingRequest = new HashMap<>();
    
    /**
     * How long the Emoticons can be cached in a file after they are updated
     * from the API.
     */
    public static final int CACHED_EMOTICONS_EXPIRE_AFTER = 60*60*24;
    
    public static final int TOKEN_CHECK_DELAY = 600;
    
    
    enum RequestType {
        STREAM, EMOTICONS, VERIFY_TOKEN, CHAT_ICONS, CHANNEL, CHANNEL_PUT,
        GAME_SEARCH, COMMERCIAL, STREAMS, FOLLOWED_STREAMS, FOLLOWERS,
        SUBSCRIBERS, USERINFO, CHAT_SERVER, FOLLOW, UNFOLLOW, CHAT_INFO,
        GLOBAL_BADGES, ROOM_BADGES
    }
    
    public enum RequestResult {
        ACCESS_DENIED, SUCCESS, FAILED, NOT_FOUND, RUNNING_COMMERCIAL,
        INVALID_CHANNEL, INVALID_STREAM_STATUS, UNKNOWN
    }
    
    private final TwitchApiResultListener resultListener;
    
    private final StreamInfoManager streamInfoManager;
    private final EmoticonManager emoticonManager;
    private final FollowerManager followerManager;
    private final FollowerManager subscriberManager;
    private final BadgeManager badgeManager;
    private final UserIDs userIDs;
    
    private volatile Long tokenLastChecked = Long.valueOf(0);
    
    private volatile String defaultToken;
    
    private final Set<String> requestedChatIcons =
            Collections.synchronizedSet(new HashSet<String>());
    
    private final Map<String, ChannelInfo> cachedChannelInfo =
            Collections.synchronizedMap(new HashMap<String, ChannelInfo>());
    
    private final ExecutorService executor;

    public TwitchApi(TwitchApiResultListener apiResultListener,
            StreamInfoListener streamInfoListener) {
        this.resultListener = apiResultListener;
        this.streamInfoManager = new StreamInfoManager(this, streamInfoListener);
        emoticonManager = new EmoticonManager(apiResultListener);
        followerManager = new FollowerManager(Follower.Type.FOLLOWER, this, resultListener);
        subscriberManager = new FollowerManager(Follower.Type.SUBSCRIBER, this, resultListener);
        badgeManager = new BadgeManager(this);
        userIDs = new UserIDs(this);
        
        executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Get StreamInfo for the given stream. Always returns a StreamInfo object,
     * which may however be marked as invalid if the stream is no valid stream
     * name or does not exist or data hasn't been requested yet.
     *
     * The first request per stream is always invalid, because the info has
     * to be requested from the server first. Further request return a cached
     * version of the StreamInfo, until the info is marked as expired.
     * 
     * @param stream
     * @return The StreamInfo object
     */
    public StreamInfo getStreamInfo(String stream, Set<String> streams) {
        if (streams == null) {
            streams = new HashSet<>();
        }
        return streamInfoManager.getStreamInfo(stream, streams);
    }
    
    public void getFollowedStreams(String token) {
        streamInfoManager.getFollowedStreams(token);
    }
    
    public void manualRefreshStreams() {
        streamInfoManager.manualRefresh();
    }
    
    public void setToken(String token) {
        this.defaultToken = token;
    }
    
    public void getSubscribers(String stream) {
        subscriberManager.request(stream);
    }
    
    public void getFollowers(String stream) {
        followerManager.request(stream);
    }
    
    protected void requestFollowers(String stream) {
        String url = "https://api.twitch.tv/kraken/channels/"+stream+"/follows?direction=desc&limit=100&offset=0";
        //url = "http://127.0.0.1/twitch/followers";
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request = new TwitchApiRequest(this, RequestType.FOLLOWERS, url);
            //request.setApiVersion("v3");
            executor.execute(request);
        }
    }
    
    protected void requestSubscribers(String stream) {
        String url = "https://api.twitch.tv/kraken/channels/"+stream+"/subscriptions?direction=desc&limit=100&offset=0";
        if (Chatty.DEBUG) {
            url = "http://127.0.0.1/twitch/subscriptions_test";
        }
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request = new TwitchApiRequest(this, RequestType.SUBSCRIBERS, url, defaultToken);
            //request.setApiVersion("v3");
            executor.execute(request);
        }
    }
    
    protected void requestFollowedStreams(String token, String nextUrl) {
        String url;
        if (nextUrl != null) {
            url = nextUrl;
        } else {
            url = "https://api.twitch.tv/kraken/streams/followed?limit="
                    + StreamInfoManager.FOLLOWED_STREAMS_LIMIT + "&offset=0";
        }
        TwitchApiRequest request = new TwitchApiRequest(this,
                RequestType.FOLLOWED_STREAMS, url, token);
        executor.execute(request);
    }
    
    public void requestChatIcons(String stream, boolean forced) {
        String url = "https://api.twitch.tv/kraken/chat/"+stream+"/badges";
        //String url = "http://127.0.0.1/twitch/joshimuz_badges";
        if (attemptRequest(url, stream)) {
            if (forced || !requestedChatIcons.contains(stream)) {
                // Only request icons if they haven't been received before
                TwitchApiRequest request = new TwitchApiRequest(this, RequestType.CHAT_ICONS, url);
                executor.execute(request);
            }
        }
    }
    
    public void getGlobalBadges(boolean forceRefresh) {
        badgeManager.requestGlobalBadges(forceRefresh);
    }
    
    public void getRoomBadges(String room, boolean forceRefresh) {
        badgeManager.requestBadges(room, forceRefresh);
    }
    
    protected void requestGlobalBadges() {
        String url = "https://badges.twitch.tv/v1/badges/global/display?language=en";
        if (attemptRequest(url, null)) {
            TwitchApiRequest request = new TwitchApiRequest(this, RequestType.GLOBAL_BADGES, url);
            executor.execute(request);
        }
    }
    
    protected void requestRoomBadges(long roomId, String stream) {
        String url = "https://badges.twitch.tv/v1/badges/channels/"+roomId+"/display?language=en";
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request = new TwitchApiRequest(this, RequestType.ROOM_BADGES, url);
            executor.execute(request);
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
        if (forcedUpdate || !emoticonManager.loadEmoticons(false)) {
            String url = "https://api.twitch.tv/kraken/chat/emoticons";
            url = "https://api.twitch.tv/kraken/chat/emoticon_images";
            //url = "http://127.0.0.1/twitch/emoticons";
            if (attemptRequest(url, forcedUpdate ? "update" : "")) {
                TwitchApiRequest request = new TwitchApiRequest(this, RequestType.EMOTICONS, url);
                //request.setApiVersion("v3");
                executor.execute(request);
            }
            //requestResult(REQUEST_TYPE_EMOTICONS,"")
        }
    }
    
    public void requestUserInfo(String name) {
        String url = "https://api.twitch.tv/kraken/users/"+name;
        if (attemptRequest(url, name)) {
            TwitchApiRequest request =
                    new TwitchApiRequest(this,RequestType.USERINFO,url);
            executor.execute(request);
        }
    }
    
    /**
     * Sends a request to get streaminfo of the given stream.
     * 
     * @param stream 
     */
    protected void requestStreamInfo(String stream) {
        String url = "https://api.twitch.tv/kraken/streams/"+stream;
        //url = "http://127.0.0.1/twitch/streaminfo_motokosworld";
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request =
                    new TwitchApiRequest(this,RequestType.STREAM,url);
            executor.execute(request);
            //new Thread(request).start();
        }
    }
    
    protected String requestStreamsInfo(Set<String> streams) {
        String streamsString = StringUtil.join(streams, ",");
        String url = "https://api.twitch.tv/kraken/streams?offset=0&limit=100&channel="+streamsString;
        //url = "http://127.0.0.1/twitch/streams";
        TwitchApiRequest request =
                    new TwitchApiRequest(this,RequestType.STREAMS,url);
            executor.execute(request);
            //new Thread(request).start();
        return url;
    }
    
    public ChannelInfo getCachedChannelInfo(String stream) {
        ChannelInfo info = cachedChannelInfo.get(stream);
        if (info != null) {
            if (System.currentTimeMillis() - info.time > 600*1000) {
                getChannelInfo(stream);
            }
            return info;
        }
        getChannelInfo(stream);
        return null;
    }
    
    public void getChannelInfo(String stream) {
        if (stream == null || stream.isEmpty()) {
            return;
        }
        String url = "https://api.twitch.tv/kraken/channels/"+stream;
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request =
                    new TwitchApiRequest(this,RequestType.CHANNEL,url);
            executor.execute(request);
            //new Thread(request).start();
        }
    }
    
    public long getUserId(String username, UserIDs.UserIDListener listener) {
        return userIDs.getUserId(username, listener);
    }
    
    public void getGameSearch(String game) {
        if (game == null || game.isEmpty()) {
            return;
        }
        String encodedGame = "";
        try {
            encodedGame = URLEncoder.encode(game, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TwitchApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        final String url = "https://api.twitch.tv/kraken/search/games?type=suggest&q="+encodedGame;
        TwitchApiRequest request =
                new TwitchApiRequest(this, RequestType.GAME_SEARCH, url);
        executor.execute(request);
        //new Thread(request).start();
    }
    
    public void putChannelInfo(String stream, ChannelInfo info, String token) {
        if (stream == null) {
            return;
        }
        String url = "https://api.twitch.tv/kraken/channels/"+stream;
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request =
                    new TwitchApiRequest(this,RequestType.CHANNEL_PUT,url,token);
            request.setData(makeChannelInfoJson(info), "PUT");
            executor.execute(request);
            //new Thread(request).start();
        }
    }
    
    public void getServer(String channel) {
        if (channel == null) {
            return;
        }
        String url = "https://tmi.twitch.tv/servers?channel="+Helper.toStream(channel);
        if (attemptRequest(url, channel)) {
            TwitchApiRequest request =
                    new TwitchApiRequest(this, RequestType.CHAT_SERVER, url);
            executor.execute(request);
        }
    }
    
    public void requestChatInfo(String stream) {
        if (stream == null || !Helper.validateStream(stream)) {
            return;
        }
        String url = "https://api.twitch.tv/api/channels/"+stream+"/chat_properties";
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request = new TwitchApiRequest(this, RequestType.CHAT_INFO, url);
            executor.execute(request);
        }
    }
    
    /**
     * Checks if a request with the given url can be made. Returns true if no
     * request with that url is currently waiting for a response, false
     * otherwise.
     *
     * This also saves the stream this request url is associated with, so it
     * can more easily be retrieved when the reponse comes in.
     * 
     * @param url The URL of the request
     * @param stream The name of the stream
     * @return true if request can be made, false if it shouldn't
     */
    public boolean attemptRequest(String url, String stream) {
        synchronized (pendingRequest) {
            if (!pendingRequest.containsKey(url)) {
                pendingRequest.put(url, stream);
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
    private String removeRequest(String url) {
        synchronized(pendingRequest) {
            return pendingRequest.remove(url);
        }
    }
    
    /**
     * When access was denied when doing an authenticated request. Check the
     * token maybe subsequently.
     */
    protected void accessDenied() {
        resultListener.accessDenied();
    }
    
    /**
     * Verifies token, but only once the delay has passed. For automatic checks
     * instead of manual ones.
     * 
     * @param token 
     */
    public void checkToken(String token) {
        if (token != null && !token.isEmpty() &&
                (System.currentTimeMillis() - tokenLastChecked) / 1000 > TOKEN_CHECK_DELAY) {
            LOGGER.info("Checking token..");
            tokenLastChecked = Long.valueOf(System.currentTimeMillis());
            verifyToken(token);
        }
    }
    
    public void verifyToken(String token) {
        String url = "https://api.twitch.tv/kraken/";
        //url = "http://127.0.0.1/twitch/token";
        TwitchApiRequest request =
                new TwitchApiRequest(this,RequestType.VERIFY_TOKEN,url,token);
        executor.execute(request);
            //new Thread(request).start();
    }
    
    public void runCommercial(String stream, String token, int length) {
        String url = "https://api.twitch.tv/kraken/channels/"+stream+"/commercial";
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request
                    = new TwitchApiRequest(this, RequestType.COMMERCIAL, url, token);
            request.setData("length=" + length, "POST");
            request.setContentType("application/x-www-form-urlencoded");
            executor.execute(request);
        //new Thread(request).start();
        }
    }
    
    public void followChannel(String user, String target) {
        String url = String.format(
                "https://api.twitch.tv/kraken/users/%s/follows/channels/%s",
                user,
                target.toLowerCase());
        if (attemptRequest(url, target)) {
            TwitchApiRequest request = new TwitchApiRequest(this, RequestType.FOLLOW, url, defaultToken);
            request.setRequestType("PUT");
            executor.execute(request);
        }
    }
    
    public void unfollowChannel(String user, String target) {
        String url = String.format(
                "https://api.twitch.tv/kraken/users/%s/follows/channels/%s",
                user,
                target.toLowerCase());
        if (attemptRequest(url, target)) {
            TwitchApiRequest request = new TwitchApiRequest(this, RequestType.UNFOLLOW, url, defaultToken);
            request.setRequestType("DELETE");
            executor.execute(request);
        }
    }
    
    /**
     * Called by the request thread to work on the result of the request.
     * 
     * @param type The type of request
     * @param url The url of the request
     * @param result The result, usually json from the Twitch API or null if
     * an error occured
     * @param responseCode The HTTP response code
     * @param encoding
     */
    protected void requestResult(RequestType type, String url, String result, int responseCode, String error, String encoding) {
//        if (type == RequestType.CHANNEL) {
//            blubb();
//        } else {
//            return;
//        }
        
        int length = -1;
        if (result != null) {
            length = result.length();
        }
        String encodingText = encoding == null ? "" : ", "+encoding;
        LOGGER.info("GOT ("+responseCode+", "+length+encodingText+"): "+url
                +(error != null ? " ["+error+"]" : ""));
        String stream = removeRequest(url);
        if (type == RequestType.STREAM) {
            streamInfoManager.requestResult(url, result, responseCode, stream);
        }
        else if (type == RequestType.STREAMS) {
            streamInfoManager.requestResultStreams(url, result, responseCode);
        }
        else if (type == RequestType.EMOTICONS) {
            emoticonManager.emoticonsReceived(result, stream);
        }
        else if (type == RequestType.CHAT_ICONS) {
            if (result == null) {
                LOGGER.warning("Error requesting stream icons: "+result);
                return;
            }
            List<Usericon> icons = parseChatIcons(result, stream);
            if (icons == null) {
                LOGGER.warning("Error parsing stream icons: "+result);
                return;
            } else {
                resultListener.receivedUsericons(icons);
                requestedChatIcons.add(stream);
            }
        }
        else if (type == RequestType.GLOBAL_BADGES) {
            resultListener.receivedUsericons(badgeManager.handleGlobalBadgesResult(result));
        }
        else if (type == RequestType.ROOM_BADGES) {
            resultListener.receivedUsericons(badgeManager.handleRoomBadgesResult(result, stream));
        }
        else if (type == RequestType.CHANNEL || type == RequestType.CHANNEL_PUT) {
            handleChannelInfoResult(type, url, result, responseCode, stream);
        }
        else if (type == RequestType.GAME_SEARCH) {
            if (result == null) {
                LOGGER.warning("Error searching for game");
                return;
            }
            Set<String> games = parseGameSearch(result);
            if (games == null) {
                LOGGER.warning("Error parsing game search result");
                return;
            }
            resultListener.gameSearchResult(games);
        }
        else if (type == RequestType.FOLLOWERS) {
            followerManager.received(responseCode, stream, result);
        }
        else if (type == RequestType.SUBSCRIBERS) {
            subscriberManager.received(responseCode, stream, result);
        }
        else if (type == RequestType.USERINFO) {
            String displayName = parseNameFromUserInfo(result);
            resultListener.receivedDisplayName(StringUtil.toLowerCase(stream), displayName);
        }
        else if (type == RequestType.CHAT_SERVER) {
            resultListener.receivedServer(stream, parseServer(result));
        }
        else if (type == RequestType.CHAT_INFO) {
            resultListener.receivedChatInfo(ChatInfo.decode(stream, result));
        }
    }
    
    /**
     * Result for authorized requests.
     * 
     * @param type The type of request
     * @param url The requested URL
     * @param result The returned JSON
     * @param responseCode The HTTP response code
     * @param token The token used for authorization
     */
    protected void requestResult(RequestType type, String url,
            String result, int responseCode, String error, String encoding, String token) {
        int length = -1;
        if (result != null) {
            length = result.length();
        }
        String encodingText = encoding == null ? "" : ", "+encoding;
        LOGGER.info("GOT ("+responseCode+", "+length+encodingText
                +"): "+url+" (using authorization)"
                +(error != null ? " ["+error+"]" : ""));
        if (type == RequestType.VERIFY_TOKEN) {
            TokenInfo tokenInfo = parseVerifyToken(result);
            resultListener.tokenVerified(token, tokenInfo);
        }
        else if (type == RequestType.CHANNEL_PUT) {
            requestResult(type, url, result, responseCode, error, encoding);
        }
        else if (type == RequestType.COMMERCIAL) {
            String stream = removeRequest(url);
            String resultText = "Commercial probably not running (unknown response: "+responseCode+")";
            RequestResult resultCode = RequestResult.UNKNOWN;
            if (responseCode == 204) {
                resultText = "Running commercial..";
                resultCode = RequestResult.RUNNING_COMMERCIAL;
            }
            else if (responseCode == 422) {
                resultText = "Commercial length not allowed or trying to run too early.";
                resultCode = RequestResult.FAILED;
            }
            else if (responseCode == 401 || responseCode == 403) {
                resultText = "Can't run commercial: Access denied";
                resultCode = RequestResult.ACCESS_DENIED;
                accessDenied();
            }
            else if (responseCode == 404) {
                resultText = "Can't run commercial: Channel '"+stream+"' not found";
                resultCode = RequestResult.INVALID_CHANNEL;
            }
            if (resultListener != null) {
                resultListener.runCommercialResult(stream, resultText, resultCode);
            }
        }
        else if (type == RequestType.FOLLOWED_STREAMS) {
            streamInfoManager.requestResultFollows(result, responseCode);
        }
        else if (type == RequestType.SUBSCRIBERS) {
            String stream = removeRequest(url);
            subscriberManager.received(responseCode, stream, result);
        }
        else if (type == RequestType.FOLLOW) {
            String target = removeRequest(url);
            if (responseCode == 200) {
                long followTime = followGetTime(result);
                if (followTime != -1 && System.currentTimeMillis() - followTime > 5000) {
                    resultListener.followResult(String.format("Already following '%s' (since %s)",
                            target,
                            DateTime.ago(followTime, 0, 2, 0, DateTime.Formatting.VERBOSE)));
                } else {
                    resultListener.followResult("Now following '"+target+"'");
                }
            } else if (responseCode == 404) {
                resultListener.followResult("Couldn't follow '"+target+"' (channel not found)");
            } else if (responseCode == 401) {
                resultListener.followResult("Couldn't follow '"+target+"' (access denied)");
            } else {
                resultListener.followResult("Couldn't follow '"+target+"' (unknown error)");
            }
        }
        else if (type == RequestType.UNFOLLOW) {
            String target = removeRequest(url);
            if (responseCode == 204) {
                resultListener.followResult("No longer following '"+target+"'");
            } else if (responseCode == 404) {
                resultListener.followResult("Couldn't unfollow '"+target+"' (channel not found)");
            } else if (responseCode == 401) {
                resultListener.followResult("Couldn't unfollow '"+target+"' (access denied)");
            } else {
                resultListener.followResult("Couldn't unfollow '"+target+"' (unknown error)");
            }
        }
    }
    
    /**
     * Handle the ChannelInfo request result, which can also be from changing
     * the channel info.
     * 
     * @param type The type of request, in this case from changing channel info
     *  or simply from requesting ChannelInfo
     * @param url The URL that was called
     * @param result The JSON received
     * @param responseCode The HTTP response code of the request
     */
    private void handleChannelInfoResult(RequestType type, String url, String result,
            int responseCode, String stream) {
        // Handle requested ChannelInfo but also the result of changing
        // channel info, since that returns ChannelInfo as well.
        if (result == null || responseCode != 200) {
            handleChannelInfoResultError(stream, type, responseCode);
            return;
        }
        // Request should have gone through fine
        ChannelInfo info = parseChannelInfo(result);
        if (info == null) {
            LOGGER.warning("Error parsing channel info: " + result);
            handleChannelInfoResultError(stream, type, responseCode);
            return;
        }
        if (type == RequestType.CHANNEL_PUT) {
            resultListener.putChannelInfoResult(RequestResult.SUCCESS);
        }
        resultListener.receivedChannelInfo(stream, info, RequestResult.SUCCESS);
        cachedChannelInfo.put(stream, info);
        userIDs.channelInfoReceived(info);
    }
    
    /**
     * Handle the error of a ChannelInfo request result, this can be from
     * changing the channel info as well. Handle by logging the error as well
     * as informing the client who can inform the user on the GUI.
     * 
     * @param type
     * @param responseCode 
     */
    private void handleChannelInfoResultError(String stream, RequestType type, int responseCode) {
        if (type == RequestType.CHANNEL) {
            if (responseCode == 404) {
                resultListener.receivedChannelInfo(stream, null, RequestResult.NOT_FOUND);
            } else {
                resultListener.receivedChannelInfo(stream, null, RequestResult.FAILED);
            }
        } else {
            // The result of changing channel info requires some extra
            // handling, because it can have different response codes.
            if (responseCode == 404) {
                resultListener.putChannelInfoResult(RequestResult.NOT_FOUND);
            } else if (responseCode == 401 || responseCode == 403) {
                LOGGER.warning("Error setting channel info: Access denied");
                resultListener.putChannelInfoResult(RequestResult.ACCESS_DENIED);
                accessDenied();
            } else if (responseCode == 422) {
                LOGGER.warning("Error setting channel info: Probably invalid title");
                resultListener.putChannelInfoResult(RequestResult.INVALID_STREAM_STATUS);
            } else {
                LOGGER.warning("Error setting channel info: Unknown error (" + responseCode + ")");
                resultListener.putChannelInfoResult(RequestResult.FAILED);
            }
        }
    }
    
    /**
     * Read out the error from JSON, although this shouldn't happen with how
     * the data is requested. Probably not even needed.
     * 
     * @param json The JSON to parse from
     * @return true if no error occured, false otherwise
     */
//    private boolean checkStatus(String json) {
//        try {
//            JSONParser parser = new JSONParser();
//            JSONObject root = (JSONObject)parser.parse(json);
//            Number status = (Number)root.get("status");
//            if (status == null) {
//                return true;
//            }
//            LOGGER.warning((String)root.get("message"));
//            return false;
//        } catch (ParseException ex) {
//            LOGGER.warning("Error parsing for status: "+json+" "+ex.getLocalizedMessage());
//            return false;
//        }
//    }
    
    /**
     * Parses the JSON returned from the TwitchAPI that contains the token
     * info into a TokenInfo object.
     * 
     * @param json
     * @return The TokenInfo or null if an error occured.
     */
    private TokenInfo parseVerifyToken(String json) {
        if (json == null) {
            LOGGER.warning("Error parsing verify token result (null)");
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            
            JSONObject token = (JSONObject) root.get("token");
            
            if (token == null) {
                return null;
            }
            
            boolean valid = (Boolean)token.get("valid");
            
            if (!valid) {
                return new TokenInfo();
            }
            
            String username = (String)token.get("user_name");
            JSONObject authorization = (JSONObject)token.get("authorization");
            JSONArray scopes = (JSONArray)authorization.get("scopes");
            
            boolean allowCommercials = scopes.contains("channel_commercial");
            boolean allowEditor = scopes.contains("channel_editor");
            boolean chatAccess = scopes.contains("chat_login");
            boolean userAccess = scopes.contains("user_read");
            boolean readSubscriptions = scopes.contains("channel_subscriptions");
            boolean userEditFollows = scopes.contains("user_follows_edit");
            
            return new TokenInfo(username, chatAccess, allowEditor, allowCommercials, userAccess, readSubscriptions, userEditFollows);
        }
        catch (ParseException e) {
            return null;
        }
    }
    
    /**
     * Parses the icons info returned from the TwitchAPI into a ChatIcons object
     * containing the URLs.
     * 
     * @param json
     * @return 
     */
    private List<Usericon> parseChatIcons(String json, String stream) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);

            List<Usericon> iconsNew = new ArrayList<>();
            addUsericon(iconsNew, Usericon.Type.MOD, null, getChatIconUrl(root, "mod", "image"));
            addUsericon(iconsNew, Usericon.Type.SUB, stream, getChatIconUrl(root, "subscriber", "image"));
            addUsericon(iconsNew, Usericon.Type.TURBO, null, getChatIconUrl(root, "turbo", "image"));
            addUsericon(iconsNew, Usericon.Type.BROADCASTER, null, getChatIconUrl(root, "broadcaster", "image"));
            addUsericon(iconsNew, Usericon.Type.STAFF, null, getChatIconUrl(root, "staff", "image"));
            addUsericon(iconsNew, Usericon.Type.ADMIN, null, getChatIconUrl(root, "admin", "image"));
            addUsericon(iconsNew, Usericon.Type.GLOBAL_MOD, null, getChatIconUrl(root, "global_mod", "image"));
            
            return iconsNew;
        }
        catch (ParseException ex) {
            LOGGER.warning("Error parsing chat icons: "+ex.getLocalizedMessage());
            return null;
        }
    }
    
    private void addUsericon(List<Usericon> icons, Usericon.Type type, String stream, String url) {
        if (url != null && !url.isEmpty()) {
            Usericon icon = UsericonFactory.createTwitchIcon(type, stream, url, null);
            if (icon != null) {
                icons.add(icon);
            }
        }
    }
    
    
    
    /**
     * Returns the URL for a single icon, read from the given JSONObject.
     * 
     * @param root The JSONObject that contains the icon info.
     * @param name The name of the icon.
     * @param type The type of the icon (alpha or regular image).
     * @return 
     */
    private String getChatIconUrl(JSONObject root, String name, String type) {
        try {
            JSONObject icon = (JSONObject)root.get(name);
            String image = (String)icon.get(type);
            return image;
        } catch (NullPointerException ex) {
            LOGGER.warning("Error parsing chat icon "+name+": unexpected null");
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing chat icon "+name+": unexpected type");
        }
        return null;
    }
    
    /**
     * Parses the channel info returned from the Twitch API into a ChannelInfo
     * object.
     * 
     * @param json
     * @return 
     */
    private ChannelInfo parseChannelInfo(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            String name = (String)root.get("name");
            long id = ((Number)root.get("_id")).longValue();
            String status = (String)root.get("status");
            String game = (String)root.get("game");
            int views = JSONUtil.getInteger(root, "views", -1);
            int followers = JSONUtil.getInteger(root, "followers", -1);
            long createdAt = -1;
            try {
                createdAt = Util.parseTime(JSONUtil.getString(root, "created_at"));
            } catch (java.text.ParseException ex) {
                LOGGER.warning("Error parsing ChannelInfo: "+ex);
            }
            return new ChannelInfo(name, id, status, game, createdAt, followers, views);
        }
        catch (ParseException ex) {
            LOGGER.warning("Error parsing ChannelInfo.");
            return null;
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing ChannelInfo: Unexpected type");
            return null;
        }
        
    }
    
    /**
     * Turns a ChannelInfo object to JOSN to send it to the API.
     * 
     * @param info The ChannelInfo object
     * @return The created JSON
     */
    private String makeChannelInfoJson(ChannelInfo info) {
        JSONObject root = new JSONObject();
        Map channel = new HashMap();
        channel.put("status",info.getStatus());
        channel.put("game",info.getGame());
        root.put("channel",channel);
        return root.toJSONString();
    }
    
    /**
     * Parse the list of games that was returned by the game search.
     * 
     * @param json
     * @return 
     */
    private Set<String> parseGameSearch(String json) {
        Set<String> result = new HashSet<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            Object games = root.get("games");
            
            if (!(games instanceof JSONArray)) {
                LOGGER.warning("Error parsing game search: Should be array");
                return null;
            }
            Iterator it = ((JSONArray)games).iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof JSONObject) {
                    String name = (String)((JSONObject)obj).get("name");
                    result.add(name);
                }
            }
            return result;
            
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing game search.");
            return null;
        } catch (NullPointerException ex) {
            LOGGER.warning("Error parsing game search: Unexpected null");
            return null;
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing game search: Unexpected type");
            return null;
        }
    }
    
    private String parseNameFromUserInfo(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            String displayName = (String)root.get("display_name");
            return displayName;
        } catch (ParseException | ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing userinfo: "+ex);
        }
        return null;
    }
    
    private String parseServer(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray servers = (JSONArray)root.get("servers");
            return (String)servers.get(0);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing server: "+ex);
        }
        return null;
    }
    
    private long followGetTime(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            long time = Util.parseTime((String)root.get("created_at"));
            return time;
        } catch (Exception ex) {
            return -1;
        }
    }
}