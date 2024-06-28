
package chatty.util.api;

import chatty.Room;
import chatty.User;
import chatty.util.CachedBulkManager;
import chatty.util.StringUtil;
import chatty.util.api.BlockedTermsManager.BlockedTerm;
import chatty.util.api.BlockedTermsManager.BlockedTerms;
import chatty.util.api.UserIDs.UserIdResult;
import java.util.*;
import java.util.logging.Logger;
import chatty.util.api.ResultManager.CategoryResult;
import chatty.util.api.ResultManager.CreateClipResult;
import chatty.util.api.eventsub.EventSubAddResult;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
    protected final CachedBulkManager<Req, Boolean> m;
    protected final ResultManager resultManager;
    protected final UserInfoManager userInfoManager;
    protected final BlockedTermsManager blockedTermsManager;
    
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
        userInfoManager = new UserInfoManager(this);
        emoticonManager2 = new EmoticonManager2(resultListener, requests);
        blockedTermsManager = new BlockedTermsManager(requests);
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
        resultManager = new ResultManager();
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
    
    protected void setReceived(String requestId) {
        m.setResult(new Req(requestId, null), Boolean.TRUE);
    }
    
    protected void setError(String requestId) {
        m.setError(new Req(requestId, null));
    }
    
    protected void setNotFound(String requestId) {
        m.setNotFound(new Req(requestId, null));
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
    
    public void refreshEmotes() {
        emoticonManager2.refresh();
    }
    
    public void refreshSets(Set<String> emotesets) {
        emoticonManager2.refresh(emotesets);
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
    
    public void getChannelStatus(String stream) {
        getChannelStatus(stream, null);
    }
    
    public void getChannelStatus(String stream, String id) {
        if (id != null) {
            requests.getChannelStatus(id, stream);
        } else {
            userIDs.getUserIDsAsap(r -> {
                if (r.hasError()) {
                    resultListener.receivedChannelStatus(ChannelStatus.createInvalid(null, stream), TwitchApi.RequestResultCode.FAILED);
                } else {
                    requests.getChannelStatus(r.getId(stream), stream);
                }
            }, stream);
        }
    }
    
    public void getStreamLabels() {
        StreamLabels.request(requests);
    }
    
    public void getFollowers(String stream, boolean forceRefresh) {
        followerManager.request(stream, forceRefresh);
    }

    public Follower getSingleFollower(String stream, String streamId, String user, String userId, boolean refresh) {
        return followerManager.getSingleFollower(stream, streamId, user, userId, refresh);
    }
    
    public void getSubscribers(String stream, boolean forceRefresh) {
        subscriberManager.request(stream, forceRefresh);
    }
    
    public UserInfo getCachedUserInfo(String channel, Consumer<UserInfo> result) {
        return userInfoManager.getCached(channel, result);
    }
    
    public void getCachedUserInfo(List<String> logins, Consumer<Map<String, UserInfo>> result) {
        userInfoManager.getCached(null, logins, result);
    }
    
    public UserInfo getCachedUserInfoById(String id, Consumer<UserInfo> result) {
        return userInfoManager.getCachedById(id, result);
    }

    public void getCachedUserInfoById(List<String> ids, Consumer<Map<String, UserInfo>> result) {
        userInfoManager.getCachedById(null, ids, result);
    }

    public UserInfo getCachedOnlyUserInfo(String login) {
        return userInfoManager.getCachedOnly(login);
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
    
    
    //===================
    // Admin / Moderation
    //===================
    
    public void putChannelInfoNew(ChannelStatus info) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                resultListener.putChannelInfoResult(TwitchApi.RequestResultCode.FAILED, "Could not get user id");
            } else {
                String streamId = r.getId(info.channelLogin);
                if (!info.hasCategoryId()) {
                    // Search for category
                    performGameSearch(info.category.name, (categories) -> {
                        boolean categoryFound = false;
                        for (StreamCategory category : categories) {
                            if (category.nameMatches(info.category)) {
                                requests.putChannelInfoNew(streamId, info.changeCategory(category), defaultToken);
                                categoryFound = true;
                            }
                        }
                        if (!categoryFound) {
                            LOGGER.warning("Stream Category "+info.category.name+" not found");
                            resultListener.putChannelInfoResult(TwitchApi.RequestResultCode.FAILED, "Category not found");
                        }
                    });
                }
                else {
                    requests.putChannelInfoNew(streamId, info, defaultToken);
                }
            }
        }, info.channelLogin);
    }
    
    public void performGameSearch(String search, CategoryResult listener) {
        requests.getGameSearch(search, listener);
    }
    
    public void getBlockedTerms(String streamName, boolean refresh, Consumer<BlockedTerms> listener) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                listener.accept(null);
            }
            else {
                String streamId = r.getId(streamName);
                blockedTermsManager.getBlockedTerms(streamId, streamName, refresh, listener);
            }
        }, streamName);
    }
    
    public void addBlockedTerm(String streamName, String text, Consumer<BlockedTerm> listener) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                listener.accept(null);
            }
            else {
                String streamId = r.getId(streamName);
                requests.addBlockedTerm(streamId, streamName, text, listener);
            }
        }, streamName);
    }
    
    public void removeBlockedTerm(BlockedTerm term, Consumer<BlockedTerm> listener) {
        requests.removeBlockedTerm(term, listener);
    }
    
    //-------------
    // Commercials
    //-------------
    
    public void runCommercial(String stream, int length) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                resultListener.runCommercialResult(stream, "Failed to resolve id", RequestResultCode.UNKNOWN);
            } else {
                requests.runCommercial(r.getId(stream), stream, length);
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
    
    public void createClip(String stream) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                resultManager.inform(ResultManager.Type.CREATE_CLIP, (CreateClipResult l) -> l.result(null, null, "Failed to resolve channel id"));
            } else {
                requests.createClip(r.getId(stream));
            }
        }, stream);
    }
        
    public static String[] ANNOUNCEMENT_COLORS = new String[]{
        "", "primary", "blue", "green", "orange", "purple"
    };
    
    public void sendAnnouncement(String stream, String message, String color) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                resultListener.errorMessage("Failed to resolve channel id");
            } else {
                requests.sendAnnouncement(r.getId(stream), message, color);
            }
        }, stream);
    }
    
    public void subscribe(ResultManager.Type type, Object listener) {
        resultManager.subscribe(type, listener);
    }
    
    public void subscribe(ResultManager.Type type, Object unique, Object listener) {
        resultManager.subscribe(type, unique, listener);
    }
    
    public interface StreamMarkerResult {
        public void streamMarkerResult(String error);
    }
    
    public void ban(User targetUser, int length, String reason, SimpleRequestResultListener listener) {
        runWithUserIds(targetUser, listener, (streamId, targetId) -> {
            requests.ban(streamId, targetId, length, reason, listener);
        });
    }
    
    public void unban(User targetUser, SimpleRequestResultListener listener) {
        runWithUserIds(targetUser, listener, (streamId, targetId) -> {
            requests.unban(streamId, targetId, listener);
        });
    }
    
    public void deleteMsg(Room room, String msgId, SimpleRequestResultListener listener) {
        runWithStreamId(room, listener, streamId -> {
            requests.deleteMsg(streamId, msgId, listener);
        });
    }
    
    public void shoutout(User targetUser, SimpleRequestResultListener listener) {
        runWithUserIds(targetUser, listener, (streamId, targetId) -> {
            requests.shoutout(streamId, targetId, listener);
        });
    }
    
    public void warn(User targetUser, String reason, SimpleRequestResultListener listener) {
        runWithUserIds(targetUser, listener, (streamId, targetId) -> {
            requests.warn(streamId, targetId, reason, listener);
        });
    }
    
    public void setVip(User targetUser, boolean add, SimpleRequestResultListener listener) {
        runWithUserIds(targetUser, listener, (streamId, targetId) -> {
            requests.setVip(streamId, targetId, add, listener);
        });
    }
    
    public void setModerator(User targetUser, boolean add, SimpleRequestResultListener listener) {
        runWithUserIds(targetUser, listener, (streamId, targetId) -> {
            requests.setModerator(streamId, targetId, add, listener);
        });
    }
    
    public void requestModerators(Room room, SimpleRequestResultListener listener) {
        runWithStreamId(room, listener, streamId -> {
            requests.getModerators(streamId, listener);
        });
    }
    
    public void requestVips(Room room, SimpleRequestResultListener listener) {
        runWithStreamId(room, listener, streamId -> {
            requests.getVips(streamId, listener);
        });
    }
    
    public void startRaid(Room room, String targetUsername, SimpleRequestResultListener listener) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                listener.accept(SimpleRequestResult.error("Invalid username"));
            }
            else {
                requests.startRaid(r.getId(room.getStream()), r.getId(targetUsername), listener);
            }
        }, room.getStream(), targetUsername);
    }
    
    public void cancelRaid(Room room, SimpleRequestResultListener listener) {
        runWithStreamId(room, listener, streamId -> {
            requests.cancelRaid(streamId, listener);
        });
    }
    
    public void setShieldMode(Room room, boolean enabled, SimpleRequestResultListener listener) {
        runWithStreamId(room, listener, streamId -> {
            requests.setShieldMode(room.getStream(), streamId, enabled, listener);
        });
    }
    
    public void getShieldMode(Room room, boolean oncePerStream) {
        String requestId = "getShieldMode:" + room.getStream();
        if (oncePerStream) {
            int options = CachedBulkManager.ASAP | CachedBulkManager.UNIQUE;
            m.query(null, options, new Req(requestId, () -> {
                runWithStreamId(room, null, streamId -> {
                    requests.getShieldMode(room.getStream(), streamId, requestId);
                });
            }));
        }
        else {
            runWithStreamId(room, null, streamId -> {
                requests.getShieldMode(room.getStream(), streamId, requestId);
            });
        }
    }
    
    public void removeShieldModeCache(Room room) {
        String requestId = "getShieldMode:" + room.getStream();
        m.removeCachedValue(new Req(requestId, null));
    }
    
    public void whisper(String targetUsername, String msg, SimpleRequestResultListener listener) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                listener.accept(SimpleRequestResult.error("Invalid username"));
            }
            else {
                requests.whisper(r.getId(targetUsername), msg, listener);
            }
        }, targetUsername);
    }
    
    public void setColor(String color, SimpleRequestResultListener listener) {
        requests.setColor(color, listener);
    }
    
    public static final String CHAT_SETTINGS_EMOTEONLY = "emote_mode";
    public static final String CHAT_SETTINGS_FOLLOWER_MODE = "follower_mode";
    public static final String CHAT_SETTINGS_FOLLOWER_MODE_DURATION = "follower_mode_duration";
    public static final String CHAT_SETTINGS_SLOWMODE = "slow_mode";
    public static final String CHAT_SETTINGS_SLOWMODE_TIME = "slow_mode_wait_time";
    public static final String CHAT_SETTINGS_SUBONLY = "subscriber_mode";
    public static final String CHAT_SETTINGS_UNIQUE = "unique_chat_mode";
    
    public void updateChatSettings(Room room, SimpleRequestResultListener listener, Object... data) {
        runWithStreamId(room, listener, streamId -> {
            requests.updateChatSettings(streamId, data, listener);
        });
    }
    
    private void runWithUserIds(User targetUser, SimpleRequestResultListener listener, BiConsumer<String, String> run) {
        String roomId = targetUser.getRoom().getStreamId();
        String targetId = targetUser.getId();
        if (roomId == null || targetId == null) {
            userIDs.getUserIDsAsap(r -> {
                if (r.hasError()) {
                    listener.accept(SimpleRequestResult.error("Invalid username"));
                }
                else {
                    run.accept(
                            r.getId(targetUser.getRoom().getStream()),
                            r.getId(targetUser.getName()));
                }
            }, targetUser.getRoom().getStream(), targetUser.getName());
        }
        else {
            run.accept(roomId, targetId);
        }
    }
    
    private void runWithStreamId(Room room, SimpleRequestResultListener listener, Consumer<String> run) {
        String streamId = room.getStreamId();
        if (streamId == null) {
            userIDs.getUserIDsAsap(r -> {
                if (r.hasError() && listener != null) {
                    listener.accept(SimpleRequestResult.error("Invalid username"));
                }
                else {
                    run.accept(r.getId(room.getStream()));
                }
            }, room.getStream());
        }
        else {
            run.accept(streamId);
        }
    }
    
    public interface SimpleRequestResultListener {
        
        public void accept(SimpleRequestResult r);
        
    }
    
    public static class SimpleRequestResult {
        
        public final String error;
        public final String result;
        
        private SimpleRequestResult(String result, String error) {
            this.result = result;
            this.error = error;
        }
        
        public static SimpleRequestResult error(String errorMessage) {
            return new SimpleRequestResult(null, errorMessage);
        }
        
        public static SimpleRequestResult ok() {
            return new SimpleRequestResult(null, null);
        }
        
        public static SimpleRequestResult result(String result) {
            return new SimpleRequestResult(result, null);
        }
        
    }
    
    public void test() {
        requests.test();
    }
    
    public void addEventSub(String body, Consumer<EventSubAddResult> listener) {
        requests.addEventSub(body, listener);
    }
    
    public void removeEventSub(String id, Consumer<Integer> listener) {
        requests.removeEventSub(id, listener);
    }
    
}