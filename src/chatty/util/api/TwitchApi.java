
package chatty.util.api;

import chatty.Helper;
import chatty.util.CachedBulkManager;
import chatty.util.StringUtil;
import chatty.util.api.StreamTagManager.StreamTagsListener;
import chatty.util.api.StreamTagManager.StreamTag;
import chatty.util.api.StreamTagManager.StreamTagListener;
import chatty.util.api.StreamTagManager.StreamTagPutListener;
import chatty.util.api.UserIDs.UserIdResult;
import java.util.*;
import java.util.logging.Logger;

/**
 * Handles TwitchApi requests and responses.
 * 
 * @author tduva
 */
public class TwitchApi {

    private final static Logger LOGGER = Logger.getLogger(TwitchApi.class.getName());

    /**
     * How long the Emoticons can be cached in a file after they are updated
     * from the API.
     */
    public static final int CACHED_EMOTICONS_EXPIRE_AFTER = 60*60*24;
    
    public static final int TOKEN_CHECK_DELAY = 600;

    
    
    public enum RequestResultCode {
        ACCESS_DENIED, SUCCESS, FAILED, NOT_FOUND, RUNNING_COMMERCIAL,
        INVALID_CHANNEL, INVALID_STREAM_STATUS, UNKNOWN
    }
    
    private final TwitchApiResultListener resultListener;
    
    protected final StreamInfoManager streamInfoManager;
    protected final EmoticonManager2 emoticonManager2;
    protected final CheerEmoticonManager2 cheersManager2;
    protected final FollowerManager followerManager;
    protected final FollowerManager subscriberManager;
    protected final BadgeManager badgeManager;
    protected final UserIDs userIDs;
    protected final ChannelInfoManager channelInfoManager;
    protected final StreamTagManager communitiesManager;
    protected final CachedBulkManager<Req, Boolean> m;
    
    private volatile Long tokenLastChecked = Long.valueOf(0);
    
    protected volatile String defaultToken;
    protected volatile String localUserId;

    protected final Requests requests;

    public TwitchApi(TwitchApiResultListener apiResultListener,
            StreamInfoListener streamInfoListener) {
        this.resultListener = apiResultListener;
        this.streamInfoManager = new StreamInfoManager(this, streamInfoListener);
        cheersManager2 = new CheerEmoticonManager2(this, resultListener);
        followerManager = new FollowerManager(Follower.Type.FOLLOWER, this, resultListener);
        subscriberManager = new FollowerManager(Follower.Type.SUBSCRIBER, this, resultListener);
        badgeManager = new BadgeManager(this);
        requests = new Requests(this, resultListener);
        channelInfoManager = new ChannelInfoManager(this, resultListener);
        userIDs = new UserIDs(this);
        communitiesManager = new StreamTagManager();
        emoticonManager2 = new EmoticonManager2(resultListener, requests);
        m = new CachedBulkManager<>(new CachedBulkManager.Requester<Req, Boolean>() {

            @Override
            public void request(CachedBulkManager<Req, Boolean> manager, Set<Req> asap, Set<Req> normal, Set<Req> backlog) {
                Set<Req> requests = manager.makeAndSetRequested(asap, normal, backlog, 1);
                Req req = requests.iterator().next();
                if (req.request != null) {
                    req.request.run();
                }
            }
        }, "[Api] ", CachedBulkManager.NONE);
    }
    
    private static class Req {
        
        public final String key;
        public final Runnable request;
        
        public Req(String key, Runnable request) {
            this.key = key;
            this.request = request;
        }
        
        @Override
        public String toString() {
            return key;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Req other = (Req) obj;
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return true;
        }
        
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 37 * hash + Objects.hashCode(this.key);
            return hash;
        }
        
    }
    
    protected void setReceived(String key) {
        m.setResult(new Req(key, null), Boolean.TRUE);
    }
    
    protected void setError(String key) {
        m.setError(new Req(key, null));
    }
    
    protected void setNotFound(String key) {
        m.setNotFound(new Req(key, null));
    }
    
    //=================
    // Chat / Emoticons
    //=================
    
    /**
     * Request channel emotes if necessary.
     * 
     * @param stream The stream name (required)
     * @param id The stream id (optional)
     * @param refresh If true, request is done even if already requested before
     */
    public void getEmotesByChannelId(String stream, String id, boolean refresh) {
        if (id != null) {
            getEmotesByChannelId2(stream, id, refresh);
        } else {
            userIDs.getUserIDsAsap(r -> {
                if (!r.hasError()) {
                    getEmotesByChannelId2(stream, r.getId(stream), refresh);
                }
            }, stream);
        }
    }
    
    /**
     * Request channel emotes if necessary. Only does one request attempt for a
     * while, since at least currently it's only used for the Emote Dialog and
     * manually triggered anyway. The reload button should be implemented at
     * some point to be able to trigger a manual refresh.
     *
     * @param stream The stream name (required)
     * @param id The stream id (required)
     * @param refresh If true, request is done even if already requested before
     */
    private void getEmotesByChannelId2(String stream, String id, boolean refresh) {
        int options = CachedBulkManager.ASAP | CachedBulkManager.UNIQUE;
        if (refresh) {
            options = options | CachedBulkManager.REFRESH;
        }
        String requestId = "channel_emotes:" + id;
        m.query(null, options, new Req(requestId, () -> {
            requests.requestEmotesByChannelId(stream, id, requestId);
        }));
    }
    
    public void getEmotesBySets(String... emotesets) {
        getEmotesBySets(new HashSet<>(Arrays.asList(emotesets)));
    }
    
    public void getEmotesBySets(Set<String> emotesets) {
        emoticonManager2.addEmotesets(emotesets);
    }
    
    private static final Object USER_EMOTES_UNIQUE = new Object();
    
    public void getUserEmotes(String userId) {
        m.query(USER_EMOTES_UNIQUE,
                null,
                CachedBulkManager.ASAP | CachedBulkManager.WAIT | CachedBulkManager.REFRESH,
                new Req("userEmotes", () -> {
                    requests.requestUserEmotes(userId);
                }));
    }
    
    public void refreshEmotes() {
        emoticonManager2.refresh();
    }
    
    public void requestEmotesNow() {
        emoticonManager2.requestNow();
    }
    
    public void getGlobalBadges(boolean forceRefresh) {
        badgeManager.requestGlobalBadges(forceRefresh);
    }
    
    public void getRoomBadges(String room, boolean forceRefresh) {
        badgeManager.requestBadges(room, forceRefresh);
    }
    
    public void getCheers(String room, boolean forceRefresh) {
        cheersManager2.request(room, forceRefresh);
    }
    
    //====================
    // Channel Information
    //====================
    
    public void getChannelInfo(String stream) {
        getChannelInfo(stream, null);
    }
    
    public void getChannelInfo(String stream, String id) {
        if (id != null) {
            requests.getChannelInfo(id, stream);
        } else {
            userIDs.getUserIDsAsap(r -> {
                if (r.hasError()) {
                    resultListener.receivedChannelInfo(stream, null, TwitchApi.RequestResultCode.FAILED);
                } else {
                    requests.getChannelInfo(r.getId(stream), stream);
                }
            }, stream);
        }
    }
    
    public void getFollowers(String stream) {
        followerManager.request(stream);
    }

    public Follower getSingeFollower(String stream, String streamId, String user, String userId, boolean refresh) {
        return followerManager.getSingleFollower(stream, streamId, user, userId, refresh);
    }
    
    public void getSubscribers(String stream) {
        subscriberManager.request(stream);
    }
    
    /**
     * Get ChannelInfo, if cached. This will *not* request missing ChannelInfo.
     * 
     * @param stream
     * @return 
     */
    public ChannelInfo getOnlyCachedChannelInfo(String stream) {
        return channelInfoManager.getOnlyCachedChannelInfo(stream);
    }
    
    public ChannelInfo getCachedChannelInfo(String stream) {
        return getCachedChannelInfo(stream, null);
    }
    
    /**
     * Get ChannelInfo, which may be cached. This will request immediately if
     * not cached.
     * 
     * @param stream
     * @param id
     * @return 
     */
    public ChannelInfo getCachedChannelInfo(String stream, String id) {
        return channelInfoManager.getCachedChannelInfo(stream, id);
    }
    
    //===================
    // Stream Information
    //===================
    
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
    
    /**
     * Only return already cached StreamInfo, never create a new one.
     * 
     * @param stream
     * @return The StreamInfo object, or null if none exists
     */
    public StreamInfo getCachedStreamInfo(String stream) {
        return streamInfoManager.getCachedStreamInfo(stream);
    }
    
    public void getFollowedStreams(String token) {
        streamInfoManager.getFollowedStreams(token);
    }
    
    public void manualRefreshStreams() {
        streamInfoManager.manualRefresh();
    }

    //======
    // Token
    //======
    
    public void setToken(String token) {
        this.defaultToken = token;
    }
    
    public void setLocalUserId(String userId) {
        this.localUserId = userId;
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
     */
    public void checkToken() {
        if (!StringUtil.isNullOrEmpty(defaultToken) &&
                (System.currentTimeMillis() - tokenLastChecked) / 1000 > TOKEN_CHECK_DELAY) {
            LOGGER.info("Checking token..");
            tokenLastChecked = Long.valueOf(System.currentTimeMillis());
            requests.verifyToken(defaultToken);
        }
    }
    
    public void verifyToken(String token) {
        requests.verifyToken(token);
    }
    
    public String getToken() {
        return defaultToken;
    }
    
    public void revokeToken(String token) {
        requests.revokeToken(token);
    }
    
    //=========
    // User IDs
    //=========

    public void setUserId(String userName, String userId) {
        userIDs.setUserId(userName, userId);
    }
    
    public void waitForUserId(UserIDs.UserIdResultListener listener, String... names) {
        userIDs.waitForUserIDs(listener, names);
    }
    
    public void getUserId(UserIDs.UserIdResultListener listener, String... names) {
        userIDs.getUserIDs(listener, names);
    }
    
    /**
     * @see UserIDs#getUserIDsAsap(chatty.util.api.UserIDs.UserIdResultListener, java.lang.String...) 
     * 
     * @param listener
     * @param names 
     */
    public void getUserIdAsap(UserIDs.UserIdResultListener listener, String... names) {
        userIDs.getUserIDsAsap(listener, names);
    }
    
    public void requestUserId(String... names) {
        userIDs.requestUserIDs(names);
    }
    
    public void getUserIDsTest2(String usernames) {
        UserIdResult result = userIDs.requestUserIDs(usernames.split(" "));
        if (result != null) {
            System.out.println(result.getValidIDs());
        }
    }
    
    public void getUserIDsTest3(String usernames) {
        userIDs.waitForUserIDs(r -> {
            System.out.println(r.getValidIDs());
        }, usernames.split(" "));
    }
    
    //================
    // User Management
    //================
    
    public void followChannel(String user, String target) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                resultListener.followResult("Couldn't follow '" + target + "' ("+r.getError()+")");
            } else {
                requests.followChannel(r.getId(user), r.getId(target), target, defaultToken);
            }
        }, user, target);
    }
    
    public void unfollowChannel(String user, String target) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                resultListener.followResult("Couldn't unfollow '" + target + "' ("+r.getError()+")");
            } else {
                requests.unfollowChannel(r.getId(user), r.getId(target), target, defaultToken);
            }
        }, user, target);
    }
    
    
    //===================
    // Admin / Moderation
    //===================
    
    public void putChannelInfo(ChannelInfo info) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                resultListener.putChannelInfoResult(TwitchApi.RequestResultCode.FAILED);
            } else {
                requests.putChannelInfo(r.getId(info.name), info, defaultToken);
            }
        }, info.name);
    }
    
    public void performGameSearch(String search, GameSearchListener listener) {
        requests.getGameSearch(search, listener);
    }
    
    public interface GameSearchListener {
        public void result(Collection<String> result);
    }
    
    //-------------
    // Stream Tags
    //-------------
    /**
     * Gets Community by id, which is guaranteed to contain description/rules.
     * Cached only, so it doesn't requests it if it's not in the cache.
     * 
     * @param id
     * @return 
     */
    public StreamTag getCachedOnlyTagInfo(String id) {
        return communitiesManager.getCachedByIdWithInfo(id);
    }
    
    public void requestAllTags(StreamTagManager.StreamTagsListener listener) {
        requests.getAllTags(listener);
    }
    
    /**
     * Immediately requests the Community for the given id (no caching).
     * 
     * @param id
     * @param listener 
     */
    public void getStreamTagById(String id, StreamTagListener listener) {
        requests.getTagsByIds(new HashSet<>(Arrays.asList(new String[]{id})), (t,e) -> {
            if (t != null && t.size() == 1) {
                listener.received(t.iterator().next(), e);
            } else {
                listener.received(null, e);
            }
        });
    }
    
    public void getInvalidStreamTags(Collection<StreamTag> checkTags, StreamTagsListener listener) {
        Set<String> ids = new HashSet<>();
        checkTags.forEach(t -> ids.add(t.getId()));
        requests.getTagsByIds(ids, (resultTags, e) -> {
            if (e != null) {
                listener.received(resultTags, e);
            } else {
                Set<StreamTag> invalid = new HashSet<>();
                // Any found tags that can not be manually set are invalid
                resultTags.forEach(tag -> {
                    if (!tag.canUserSet()) {
                        invalid.add(tag);
                    }
                });
                // Any not found tags are invalid
                checkTags.forEach(tag -> {
                    if (!resultTags.contains(tag)) {
                        invalid.add(tag);
                    }
                });
                listener.received(invalid, e);
            }
        });
    }
    
    public void setStreamTags(String channelName, List<StreamTag> tags,
            StreamTagPutListener listener) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                listener.result("Failed getting user id");
            } else {
                requests.setStreamTags(r.getId(channelName), tags, listener);
            }
        }, channelName);
    }
    
    public void getTagsByStream(String stream, StreamTagsListener listener) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                listener.received(null, "Error resolving id.");
            } else {
                requests.getTagsByStream(r.getId(stream), listener);
            }
        }, stream);
    }
    
    //-------------
    // Commercials
    //-------------
    
    public void runCommercial(String stream, int length) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                resultListener.runCommercialResult(stream, "Failed to resolve id", RequestResultCode.UNKNOWN);
            } else {
                requests.runCommercial(r.getId(stream), stream, defaultToken, length);
            }
        }, stream);
    }
    
    //---------
    // AutoMod
    //---------
    
    public enum AutoModAction {
        ALLOW, DENY
    }
    
    public enum AutoModActionResult {
        SUCCESS(204, ""),
        ALREADY_PROCESSED(400, "Message already handled"),
        BAD_AUTH(401, "Access denied (check Main - Account for access)"),
        UNAUTHORIZED(403, "Access denied"),
        NOT_FOUND(404, "Invalid message id"),
        OTHER_ERROR(-1, "Unknown error");
        
        public final int responseCode;
        public final String errorMessage;
        
        AutoModActionResult(int responseCode, String errorMessage) {
            this.responseCode = responseCode;
            this.errorMessage = errorMessage;
        }
        
    }
    
    public void autoModApprove(String msgId) {
        requests.autoMod(AutoModAction.ALLOW, msgId, defaultToken, localUserId);
    }
    
    public void autoModDeny(String msgId) {
        requests.autoMod(AutoModAction.DENY, msgId, defaultToken, localUserId);
    }

    //---------------
    // Stream Marker
    //---------------
    
    public void createStreamMarker(String stream, String description, StreamMarkerResult listener) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                listener.streamMarkerResult("Failed to resolve channel id");
            } else {
                requests.createStreamMarker(r.getId(stream), description, defaultToken, listener);
            }
        }, stream);
    }
    
    public interface StreamMarkerResult {
        public void streamMarkerResult(String error);
    }
    
}