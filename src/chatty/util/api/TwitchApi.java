
package chatty.util.api;

import chatty.Helper;
import chatty.util.TwitchEmotes.EmotesetInfo;
import chatty.util.api.CommunitiesManager.CommunitiesListener;
import chatty.util.api.CommunitiesManager.Community;
import chatty.util.api.CommunitiesManager.CommunityListener;
import chatty.util.api.CommunitiesManager.CommunityPutListener;
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
    protected final EmoticonManager emoticonManager;
    protected final EmoticonManager2 emoticonManager2;
    protected final CheerEmoticonManager cheersManager;
    protected final CheerEmoticonManager2 cheersManager2;
    protected final FollowerManager followerManager;
    protected final FollowerManager subscriberManager;
    protected final BadgeManager badgeManager;
    protected final UserIDs userIDs;
    protected final ChannelInfoManager channelInfoManager;
    protected final CommunitiesManager communitiesManager;
    
    private volatile Long tokenLastChecked = Long.valueOf(0);
    
    protected volatile String defaultToken;

    protected final Requests requests;

    public TwitchApi(TwitchApiResultListener apiResultListener,
            StreamInfoListener streamInfoListener) {
        this.resultListener = apiResultListener;
        this.streamInfoManager = new StreamInfoManager(this, streamInfoListener);
        emoticonManager = new EmoticonManager(apiResultListener);
        cheersManager = new CheerEmoticonManager(apiResultListener);
        cheersManager2 = new CheerEmoticonManager2(this, resultListener);
        followerManager = new FollowerManager(Follower.Type.FOLLOWER, this, resultListener);
        subscriberManager = new FollowerManager(Follower.Type.SUBSCRIBER, this, resultListener);
        badgeManager = new BadgeManager(this);
        requests = new Requests(this, resultListener);
        channelInfoManager = new ChannelInfoManager(this, resultListener);
        userIDs = new UserIDs(this);
        communitiesManager = new CommunitiesManager(this);
        emoticonManager2 = new EmoticonManager2(resultListener, requests);
        getCommunityTop(r -> {});
    }
    
    
    //=================
    // Chat / Emoticons
    //=================
    
    public void requestCheerEmoticons(boolean forcedUpdate) {
        if (forcedUpdate || !cheersManager.load(false)) {
            requests.requestCheerEmoticons(forcedUpdate);
        }
    }
    
    public void getEmotesBySets(Integer... emotesets) {
        getEmotesBySets(new HashSet<>(Arrays.asList(emotesets)));
    }
    
    public void getEmotesBySets(Set<Integer> emotesets) {
        emoticonManager2.addEmotesets(emotesets);
    }
    
    public void getEmotesByStreams(String... streams) {
        emoticonManager2.addStreams(new HashSet<>(Arrays.asList(streams)));
    }
    
    public void refreshEmotes() {
        emoticonManager2.refresh();
    }
    
    public void refreshEmotesOld() {
        requests.requestEmoticons(true);
    }
    
    public void requestEmotesNow() {
        emoticonManager2.requestNow();
    }
    
    public void setEmotesetInfo(EmotesetInfo info) {
        emoticonManager2.setEmotesetInfo(info);
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
    
    public void requestRoomsNow(String channel) {
        if (!Helper.isRegularChannel(channel)) {
            return;
        }
        String room = Helper.toStream(channel);
        getUserIdAsap(r -> {
            if (r.hasError()) {
                resultListener.roomsInfo(new RoomsInfo(room, null));
            } else {
                requests.requestRooms(r.getId(room), room);
            }
        }, room);
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

    public void getChatInfo(String stream) {
        requests.requestChatInfo(stream);
    }
    
    public void getFollowers(String stream) {
        followerManager.request(stream);
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
            requests.verifyToken(token);
        }
    }
    
    public void verifyToken(String token) {
        requests.verifyToken(token);
    }
    
    public String getToken() {
        return defaultToken;
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
    
    /**
     * Gets Community by id, which is guaranteed to contain description/rules.
     * Cached only, so it doesn't requests it if it's not in the cache.
     * 
     * @param id
     * @return 
     */
    public Community getCachedCommunityInfo(String id) {
        return communitiesManager.getCachedByIdWithInfo(id);
    }
    
    /**
     * Gets community by id, which may or may not contain description/rules.
     * Requests it if not cached (but not again in case of error).
     * 
     * @param id The community id
     * @param listener 
     */
    public void getCommunity(String id, CommunityListener listener) {
        if (id == null || id.isEmpty()) {
            listener.received(null, "No community id.");
        } else {
            communitiesManager.getById(id, listener);
        }
    }
    
    /**
     * Gets the communities for the given ids, in the same order, however only
     * returns communities that could be retrieved without error.
     * 
     * If the communities were all cached the listener is notified immediately,
     * in the same thread, otherwise it will request asynchronously.
     * 
     * Duplicate ids will be reduced to one entry in the result.
     * 
     * @param ids
     * @param listener 
     */
    public void getCommunities(List<String> ids, CommunitiesListener listener) {
        if (ids == null || ids.isEmpty()) {
            listener.received(null, "No community ids.");
        } else {
            CommunityListener clistener = new CommunityListener() {
                
                private final Community[] data = new Community[ids.size()];
                private int counter = ids.size();

                @Override
                public void received(Community community, String error) {
                    List<Community> result = null;
                    synchronized(data) {
                        counter--;
                        if (community != null) {
                            //System.out.println("received (" + counter + "/" + ids.size() + "): " + community + " " + community.getId());
                            // Find occurence of id in request, to retain order
                            // (this implicitly also removes duplicates, since it
                            // only finds the first occurence)
                            data[ids.indexOf(community.getId())] = community;
                        } else {
                            //System.out.println("error (" + counter + "/" + ids.size() + "): "+error);
                        }
                        if (counter == 0) {
                            // All received, make and send result
                            result = new ArrayList<>();
                            for (Community c : data) {
                                if (c != null) {
                                    result.add(c);
                                }
                            }
                            //System.out.println("result: " + result);
                        }
                    }
                    if (result != null) {
                        listener.received(result, null);
                    }
                }
            };
            
            // Request using the created listener
            for (String id : ids) {
                communitiesManager.getById(id, clistener);
            }
        }
    }
    
    /**
     * Requests the current top 100 communities.
     * 
     * @param listener 
     */
    public void getCommunityTop(CommunitiesManager.CommunityTopListener listener) {
        requests.getCommunitiesTop(listener);
    }
    
    /**
     * Requests the community by name (no caching).
     * 
     * @param name The name of the community
     * @param listener 
     */
    public void getCommunityByName(String name, CommunityListener listener) {
        if (name != null && !name.isEmpty()) {
            requests.getCommunityByName(name, listener);
        } else {
            listener.received(null, "Invalid community name");
        }
    }
    
    /**
     * Immediately requests the Community for the given id (no caching).
     * 
     * @param id
     * @param listener 
     */
    public void getCommunityById(String id, CommunityListener listener) {
        requests.getCommunityById(id, listener);
    }
    
    public void setCommunities(String channelName, List<Community> communities,
            CommunityPutListener listener) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                listener.result("Failed getting user id");
            } else {
                List<String> communityIds = new ArrayList<>();
                for (Community c : communities) {
                    communityIds.add(c.getId());
                }
                requests.setCommunities(r.getId(channelName), communityIds, defaultToken, listener);
            }
        }, channelName);
    }
    
    public void getCommunitiesForChannel(String channelName, CommunitiesListener listener) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                listener.received(null, "Error resolving id.");
            } else {
                requests.getCommunities(r.getId(channelName), listener);
            }
        }, channelName);
    }
    
    public void runCommercial(String stream, int length) {
        userIDs.getUserIDsAsap(r -> {
            if (r.hasError()) {
                resultListener.runCommercialResult(stream, "Failed to resolve id", RequestResultCode.UNKNOWN);
            } else {
                requests.runCommercial(r.getId(stream), stream, defaultToken, length);
            }
        }, stream);
    }
    
    public void autoModApprove(String msgId) {
        requests.autoMod("approve", msgId, defaultToken);
    }
    
    public void autoModDeny(String msgId) {
        requests.autoMod("deny", msgId, defaultToken);
    }

}