
package chatty.util.api;

import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.Follower.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.Optional;
import java.util.function.Consumer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
public class FollowerManager {
    
    private static final Logger LOGGER = Logger.getLogger(FollowerManager.class.getName());

    /**
     * The minimum delay between requests per channel.
     */
    private static final int REQUEST_DELAY = 60;
    
    /**
     * Save FollowerInfo (containing Followers and some meta information) for
     * each channel it was requested for.
     */
    private final Map<String, FollowerInfo> cached = new HashMap<>();
    
    /**
     * Whether this channel has already been requested during this session. Used
     * to not set the followers as new for the first request of a channel.
     */
    private final Set<String> requested = new HashSet<>();
    
    /**
     * Saves followers by name. Used to check if the same follower was already
     * seen as following before and whether it was with the same time.
     */
    // TODO: Clear cache somehow, when too many elements?
    private final Map<String, Map<String, Follower>> alreadyFollowed = new HashMap<>();
    
    /**
     * Saves request errors per stream, so requests can be delayed if errors
     * occur (e.g. 404).
     */
    private final Map<String, Integer> errors = new HashMap<>();
    
    private static final int CACHED_SINGLE_EXPIRE_TIME = 30*60*1000;
    
    /**
     * Used to store and retrieve followers for single follower requests. May
     * contain Follower objects with time=-1 for users who are not following.
     */
    private final Map<String, Map<String, Follower>> cachedSingle = new HashMap<>();
    
    /**
     * The type of this FollowerManager, which can be either for followers or
     * subscribers.
     */
    private final Follower.Type type;
    
    private final TwitchApi api;
    private final TwitchApiResultListener listener;

    public FollowerManager(Follower.Type type, TwitchApi api, TwitchApiResultListener listener) {
        this.type = type;
        this.api = api;
        this.listener = listener;
    }
    
    /**
     * Checks if this has new followers. Only checks the first follower in the
     * list, because followers at the end might be new, even though they
     * actually are not (if someone unfollows, older followers might show up for
     * the first time this session).
     * 
     * @param followers The list of followers, ordered from new to old
     * @return Whether any followers are assumed as new
     */
    private boolean hasNewFollowers(List<Follower> followers) {
        return !followers.isEmpty() && followers.get(0).newFollower;
    }
    
    /**
     * Adds error "points", so requests are delayed if errors happen.
     * 
     * @param stream
     * @param amount 
     */
    private void error(String stream, int amount) {
        Integer current = errors.get(stream);
        if (current == null) {
            current = 0;
        }
        if (current > 10) {
            current = 10;
        }
        errors.put(stream, current+amount);
    }
    
    private void noError(String stream) {
        errors.remove(stream);
    }
    
    /**
     * Checks if there is info already cached and whether it is old enough to
     * be updated, in which case it requests the data from the API.
     * 
     * @param streamName The name of the stream to request the data for
     */
    protected synchronized void request(String streamName) {
        if (streamName == null || streamName.isEmpty()) {
            return;
        }
        final String stream = StringUtil.toLowerCase(streamName);
        FollowerInfo cachedInfo = cached.get(stream);
        if (cachedInfo == null || checkTimePassed(cachedInfo)) {
            api.userIDs.getUserIDsAsap(r -> {
                if (!r.hasError()) {
                    String streamId = r.getId(stream);
                    if (type == Follower.Type.FOLLOWER) {
                        api.requests.requestFollowersNew(streamId, stream);
                    } else if (type == Follower.Type.SUBSCRIBER) {
                        api.requests.requestSubscribers(streamId, stream);
                    }
                } else {
                    FollowerInfo errorResult = new FollowerInfo(type, stream, "Could not resolve id");
                    sendResult(type, errorResult);
                    cached.put(stream, errorResult);
                }
            }, stream);
        } else {
            sendResult(type, cachedInfo);
        }
    }
    
    /**
     * Checks if enough time has passed from the last time the data was
     * requested for this FollowerInfo (so stream). Also takes into account
     * errors that occured when requesting or parsing the data, in which case it
     * waits longer in between requests.
     * 
     * @param info
     * @return 
     */
    private boolean checkTimePassed(FollowerInfo info) {
        Integer errorCount = errors.get(info.stream);
        if (errorCount == null) {
            errorCount = 0;
        }
        if (System.currentTimeMillis() - info.time > REQUEST_DELAY*1000+(REQUEST_DELAY*1000*(errorCount)/2)) {
            return true;
        }
        return false;
    }
    
    /**
     * Received data from the API, so parse it or handle a possible error, then
     * give it to the listener.
     * 
     * @param responseCode The HTTP response code from the API request
     * @param stream The name of the stream this data is for
     * @param json The data returned from the API
     */
    protected void received(int responseCode, String stream, String json) {
        FollowerInfo result = null;
        if (type == Follower.Type.FOLLOWER) {
            result = parseFollowers(stream, json);
        }
        else if (type == Follower.Type.SUBSCRIBER) {
            result = parseSubscribers(stream, json);
        }
        if (result != null) {
            addAccountCreationTimes(result, updatedInfo -> {
                processResult(updatedInfo.stream, updatedInfo);
            });
        } else {
            parseRequestError(responseCode, stream);
        }
    }
    
    /**
     * Requests user info for all the followers and adds the account creation
     * dates to a new FollowerInfo object. Must only be called with a valid
     * FollowerInfo.
     * 
     * @param info The FollowerInfo to be updated
     * @param resultListener Called with the updated FollowerInfo object
     */
    private void addAccountCreationTimes(FollowerInfo info, Consumer<FollowerInfo> resultListener) {
        List<String> usernames = info.getUsernames();
        if (usernames.isEmpty()) {
            resultListener.accept(info);
        }
        else {
            api.userInfoManager.getCached(this, usernames, (t) -> {
                List<Follower> updatedFollowers = new ArrayList<>();
                for (Follower f : info.followers) {
                    UserInfo userInfo = t.get(f.name);
                    if (userInfo != null) {
                        updatedFollowers.add(f.setAccountCreationTime(userInfo.createdAt));
                    }
                    else {
                        updatedFollowers.add(f);
                    }
                }
                FollowerInfo updatedInfo = info.replaceFollowers(updatedFollowers);
                resultListener.accept(updatedInfo);
            });
        }
    }
    
    private void processResult(String stream, FollowerInfo result) {
        synchronized (this) {
            noError(stream);
            cached.put(stream, result);
            requested.add(stream);
        }
        if (type == Follower.Type.FOLLOWER) {
            listener.receivedFollowers(result);
            if (hasNewFollowers(result.followers)) {
                listener.newFollowers(result);
            }
        }
        else if (type == Follower.Type.SUBSCRIBER) {
            listener.receivedSubscribers(result);
        }
    }

    /**
     * Received data from the API for a single follow, so parse it or handle a possible error, then
     * give it to the listener.
     *
     * @param responseCode The HTTP response code from the API request
     * @param stream The name of the stream this data is for
     * @param json The data returned from the API
     */
    protected synchronized void receivedSingle(int responseCode, String stream, String json, String username, boolean ownFollow) {
        // Parsing adds to alreadyFollowed automatically
        FollowerInfo followerInfo = ownFollow ? parseOwnFollow(stream, username, json) : parseFollowers(stream, json);
        if (followerInfo != null) {
            if (!cachedSingle.containsKey(stream)) {
                cachedSingle.put(stream, new HashMap<>());
            }
            if (followerInfo.followers.isEmpty()) {
                listener.receivedFollower(stream, username, TwitchApi.RequestResultCode.NOT_FOUND, null);
                cachedSingle.get(stream).put(username, new Follower(type, username, null, -1, -1, false, false, null, null));
            }
            else {
                Follower result = followerInfo.followers.get(0);
                cachedSingle.get(stream).put(username, result);
                listener.receivedFollower(stream, username, TwitchApi.RequestResultCode.SUCCESS, result);
            }
        }
        else {
            listener.receivedFollower(stream, username, TwitchApi.RequestResultCode.FAILED, null);
        }
    }
    
    private FollowerInfo parseOwnFollow(String stream, String userName, String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray data = (JSONArray) root.get("data");
            
            if (!data.isEmpty()) {
                JSONObject channelFollow = (JSONObject) data.get(0);
                long followedAt = DateTime.parseDatetime((String) channelFollow.get("followed_at"));
                Follower follower = createFollowerItem(stream, userName, null, followedAt, -1, null, null);
                List<Follower> result = new ArrayList<>();
                result.add(follower);
                return new FollowerInfo(type, stream, result, -1, -1);
            }
            return new FollowerInfo(type, stream, new ArrayList<>(), -1, -1);
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing "+type+": "+ex);
            return null;
        }
    }
    
    private FollowerInfo parseFollowers(String stream, String json) {
        List<Follower> result = new ArrayList<>();
        int total = -1;
        if (json == null) {
            LOGGER.warning(type+" data null.");
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray data = (JSONArray) root.get("data");
            
            for (Object o : data) {
                Follower follower = parseFollower(stream, (JSONObject) o);
                if (follower != null) {
                    result.add(follower);
                }
            }
            total = JSONUtil.getInteger(root, "total", -1);
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing "+type+": "+ex);
            return null;
        }
        return new FollowerInfo(type, stream, result, total, -1);
    }
    
    private Follower parseFollower(String stream, JSONObject data) {
        try {
            long followedAt = DateTime.parseDatetime((String) data.get("followed_at"));
            String display_name = JSONUtil.getString(data, "from_name");
            if (display_name == null) {
                // Field in new API endpoint
                display_name = JSONUtil.getString(data, "user_name");
            }
            String name = JSONUtil.getString(data, "from_login");
            if (name == null) {
                // Field in new API endpoint
                name = JSONUtil.getString(data, "user_login");
            }
            return createFollowerItem(stream, name, display_name, followedAt, -1, null, null);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing entry of "+type+": "+data+" ["+ex+"]");
        }
        return null;
    }
    
    private FollowerInfo parseSubscribers(String stream, String json) {
        if (json == null) {
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray data = (JSONArray) root.get("data");
            
            List<Follower> result = new ArrayList<>();
            int total = JSONUtil.getInteger(root, "total", -1);
            int totalPoints = JSONUtil.getInteger(root, "points", -1);
            
            for (Object o : data) {
                JSONObject entry = (JSONObject) o;
                String username = JSONUtil.getString(entry, "user_login");
                String display_name = JSONUtil.getString(entry, "user_name");
                String info = "";
                String verboseInfo = "";
                switch (JSONUtil.getString(entry, "tier", "")) {
                    case "1000":
                        info += "T1";
                        break;
                    case "2000":
                        info += "T2";
                        break;
                    case "3000":
                        info += "T3";
                        break;
                }
                verboseInfo = JSONUtil.getString(entry, "plan_name");
                if (JSONUtil.getBoolean(entry, "is_gift", false)) {
                    info += String.format(" (%s)",
                            JSONUtil.getString(entry, "gifter_login"));
                    verboseInfo += String.format(" (gifted by %s)",
                            JSONUtil.getString(entry, "gifter_login"));
                }
                Follower f = createFollowerItem(stream, username, display_name, -1, -1, info, verboseInfo);
                if (f != null) {
                    result.add(f);
                }
            }
            return new FollowerInfo(type, stream, result, total, totalPoints);
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing subscribers: "+ex);
        }
        return null;
    }

    /**
     * Creates a new Follower item with the given values, also adding
     * information about whether it is a refollow/new follower in this request.
     * 
     * @param stream
     * @param name
     * @param time
     * @return 
     */
    private synchronized Follower createFollowerItem(String stream, String name, String display_name, long time, long userTime, String info, String verboseInfo) {
        if (name == null) {
            return null;
        }
        if (display_name == null || display_name.trim().isEmpty()) {
            display_name = name;
        }
        
        stream = StringUtil.toLowerCase(stream);
        // Add map for this stream if not already added
        if (!alreadyFollowed.containsKey(stream)) {
            alreadyFollowed.put(stream, new HashMap<>());
        }
        // Check if this follower is already present
        Map<String,Follower> entries = alreadyFollowed.get(stream);
        Follower existingEntry = entries.get(name);
        boolean refollow = false;
        boolean newFollow = true;
        if (existingEntry != null) {
            newFollow = false;
            if (existingEntry.follow_time != time) {
                refollow = true;
            }
        }
        // Don't assume as new if this is the first request that returned for
        // this channel (during this session), otherwise simply ALL would be
        // shown as new the first time
        if (!requested.contains(stream)) {
            newFollow = false;
        }
        Follower newEntry = new Follower(type, name, display_name, time, userTime, refollow, newFollow, info, verboseInfo);
        if (existingEntry == null) {
            alreadyFollowed.get(stream).put(name, newEntry);
        }
        return newEntry;
    }
    
    private void sendResult(Type type, FollowerInfo result) {
        if (type == Follower.Type.FOLLOWER) {
            listener.receivedFollowers(result);
        } else if (type == Follower.Type.SUBSCRIBER) {
            listener.receivedSubscribers(result);
        }
    }

    private synchronized void parseRequestError(int responseCode, String stream) {
        String errorMessage = "";
        if (responseCode == 404) {
            errorMessage = "Channel not found.";
            error(stream, 10);
        } else if (responseCode == 200) {
            errorMessage = "Parse error.";
            error(stream, 1);
        } else if (responseCode == 401 || responseCode == 403) {
            errorMessage = "Access denied.";
            error(stream, 1);
        } else if (responseCode == 422) {
            errorMessage = "No data for this channel.";
            error(stream, 10);
        } else {
            errorMessage = "Request error.";
            error(stream, 1);
        }
        FollowerInfo errorResult = new FollowerInfo(type, stream, errorMessage);
        cached.put(stream, errorResult);
        sendResult(type, errorResult);
    }

    public Follower getSingleFollower(String stream, String streamId, String username, String userId, boolean refresh) {
        Follower follower = null;
        synchronized(this) {
            if (cachedSingle.containsKey(stream)) {
                follower = cachedSingle.get(stream).get(username);
            }
        }
        if (follower != null) {
            if (System.currentTimeMillis() - follower.created_time > CACHED_SINGLE_EXPIRE_TIME
                    || refresh) {
                // Request again if expired, but still return existing
                requestSingleFollower(stream, streamId, username, userId);
            }
            // Don't return stale data when manually refreshing
            if (!refresh) {
                if (follower.follow_time == -1) {
                    listener.receivedFollower(stream, username, TwitchApi.RequestResultCode.NOT_FOUND, null);
                } else {
                    return follower;
                }
            }
        } else {
            requestSingleFollower(stream, streamId, username, userId);
        }
        return null;
    }
    
    private void requestSingleFollower(String stream, String streamId, String username, String userId) {
        if (streamId != null && userId != null) {
            api.requests.getSingleFollowerNew(stream, streamId, username, userId);
        } else {
            api.userIDs.getUserIDsAsap(r -> {
                if (r.hasError()) {
                    listener.receivedFollower(stream, username, TwitchApi.RequestResultCode.FAILED, null);
                } else {
                    api.requests.getSingleFollowerNew(stream, r.getId(stream), username, r.getId(username));
                }
            }, stream, username);
        }
    }
    
    private static final Instant OLD_FOLLOW_API_OFF = ZonedDateTime.of(2023, 8, 3, 0, 0, 0, 0, ZoneId.of("-07:00")).toInstant();
//    private static final Instant OLD_FOLLOW_API_OFF = ZonedDateTime.of(2023, 6, 17, 20, 29, 0, 0, ZoneId.of("+02:00")).toInstant(); // For testing only
    
    public static boolean forceNewFollowsApi() {
        return Debugging.isEnabled("newfollowerapi") || Instant.now().isAfter(OLD_FOLLOW_API_OFF);
    }
    
}
