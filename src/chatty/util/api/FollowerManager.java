
package chatty.util.api;

import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.Follower.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.Optional;
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
                        api.requests.requestFollowers(streamId, stream);
                    } else if (type == Follower.Type.SUBSCRIBER) {
                        api.requests.requestSubscribers(streamId, stream, api.getToken());
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
    protected synchronized void received(int responseCode, String stream, String json) {
        FollowerInfo result = parseFollowers(stream, json);
        if (result != null) {
            noError(stream);
            cached.put(stream, result);
            if (type == Follower.Type.FOLLOWER) {
                listener.receivedFollowers(result);
                if (hasNewFollowers(result.followers)) {
                    listener.newFollowers(result);
                }
            } else if (type == Follower.Type.SUBSCRIBER) {
                listener.receivedSubscribers(result);
            }
            requested.add(stream);
        } else {
            parseRequestError(responseCode, stream);
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
    protected synchronized void receivedSingle(int responseCode, String stream, String json, String username) {
        if (!cachedSingle.containsKey(stream)) {
            cachedSingle.put(stream, new HashMap<>());
        }
        if (responseCode == 404) {
            listener.receivedFollower(stream, username, TwitchApi.RequestResultCode.NOT_FOUND, null);
            cachedSingle.get(stream).put(username, new Follower(type, username, null, -1, false, false));
        } else {
            // Parsing adds to alreadyFollowed automatically
            Follower result = parseFollowerSingle(stream, username, json);
            if (result != null) {
                cachedSingle.get(stream).put(username, result);
                listener.receivedFollower(stream, username, TwitchApi.RequestResultCode.SUCCESS, result);
            } else {
                listener.receivedFollower(stream, username, TwitchApi.RequestResultCode.FAILED, null);
            }
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
            Object root = parser.parse(json);
            if (!(root instanceof JSONObject)) {
                LOGGER.warning("Error parsing "+type+": root should be object");
                return null;
            }
            JSONObject data = (JSONObject)root;
            
            Object follows = data.get("follows");
            if (type == Follower.Type.SUBSCRIBER) {
                follows = data.get("subscriptions");
            }
            if (!(follows instanceof JSONArray)) {
                LOGGER.warning("Error parsing "+type+": follows/subs should be object");
                return null;
            }
            for (Object o : (JSONArray)follows) {
                Follower follower = parseFollower(stream, o);
                if (follower != null) {
                    result.add(follower);
                }
            }
            Object totalObject = data.get("_total");
            if (totalObject instanceof Number) {
                total = ((Number)totalObject).intValue();
            }
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing "+type+": "+ex);
            return null;
        }
        return new FollowerInfo(type, stream, result, total);
    }
    
    private Follower parseFollower(String stream, Object o) {
        try {
            JSONObject data = (JSONObject)o;
            String created_at = (String)data.get("created_at");
            long time = DateTime.parseDatetime(created_at);
            JSONObject user = (JSONObject)data.get("user");
            String display_name = (String)user.get("display_name");
            String name = (String)user.get("name");
            
            return createFollowerItem(stream, name, display_name, time);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing entry of "+type+": "+o+" ["+ex+"]");
        }
        return null;
    }

    private Follower parseFollowerSingle(String stream, String user, String json) {
        try {
            JSONParser parser = new JSONParser();
            Object root = parser.parse(json);
            if (!(root instanceof JSONObject)) {
                LOGGER.warning("Error parsing "+type+": root should be object");
                return null;
            }
            JSONObject data = (JSONObject)root;
            String created_at = (String)data.get("created_at");
            long time = DateTime.parseDatetime(created_at);

            return createFollowerItem(stream, user, user, time);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing entry of "+type+" for user "+user+": "+json+" ["+ex+"]");
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
    private Follower createFollowerItem(String stream, String name, String display_name, long time) {
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
        Follower newEntry = new Follower(type, name, display_name, time, refollow, newFollow);
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

    private void parseRequestError(int responseCode, String stream) {
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
            api.requests.getSingleFollower(stream, streamId, username, userId);
        } else {
            api.userIDs.getUserIDsAsap(r -> {
                if (r.hasError()) {
                    listener.receivedFollower(stream, username, TwitchApi.RequestResultCode.FAILED, null);
                } else {
                    api.requests.getSingleFollower(stream, r.getId(stream), username, r.getId(username));
                }
            }, stream, username);
        }
    }
    
}
