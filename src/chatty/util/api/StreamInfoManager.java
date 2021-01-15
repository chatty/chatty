
package chatty.util.api;

import chatty.util.DateTime;
import chatty.util.ElapsedTime;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.StreamInfo.StreamType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Gets stream info from the TwitchApi.
 * 
 * 
 * Why when joining the first channel the stream info gets requested twice:
 * if getStreamInfo(stream, null) is called first, then stream can be requested,
 * and then immediately again by getStreamInfo(whatever, openStreams), because
 * requestStreamsInfo() doesn't check if the stream info has expired (to reduce
 * requests)
 * 
 * 
 * @author tduva
 */
public class StreamInfoManager {
    
    private final static Logger LOGGER = Logger.getLogger(StreamInfoManager.class.getName());

    /**
     * How often to request the StreamInfo from the API.
     */
    private static final int UPDATE_STREAMINFO_DELAY = 120;
    
    private static final int SPECIAL_CHECK_DELAY = 20;
    
    private static final int UPDATE_FOLLOWS_DELAY = 200;
    
    /**
     * How often to request the StreamInfo from the API, after there was a 404.
     */
    private static final int UPDATE_STREAMINFO_DELAY_NOT_FOUND = 300;
    
    
    /**
     * The number of followed streams to get in one request. 100 is the limit
     * Twitch has.
     */
    public static final int FOLLOWED_STREAMS_LIMIT = 100;
    
    /**
     * The maximum number of requests for followed streams in the set delay.
     */
    private static final int FOLLOWED_STREAMS_REQUEST_LIMIT = 8;
    
    /**
     * Number of requests made to get followed streams. This is used as a
     * safeguard to prevent too many follow-up requests (when more than 100
     * followed streams are live or at least the program thinks so). It is
     * only set to 1 when a new normal request is started, which shouldn't
     * happen more often than the UPDATE_FOLLOWS_DELAY.
     */
    private int followedStreamsRequests = 0;
    
    /**
     * Stores the already created StreamInfo objects.
     */
    private final HashMap<String, StreamInfo> cachedStreamInfo = new HashMap<>();
    
    /**
     * Stores when the streams info was last requested, so the delay can be
     * ensured.
     */
    private final ElapsedTime streamsInfoRequestedET = new ElapsedTime();
    private final ElapsedTime followsRequestedET = new ElapsedTime();
    private final ElapsedTime specialCheckDoneET = new ElapsedTime();
    
    private int followsRequestErrors = 0;
    private String prevToken = "";
    
    private int streamsRequestErrors = 0;

    private final StreamInfoListener listener;
    private final TwitchApi api;
    
    /**
     * A StreamInfo object to represent invalid stream info.
     */
    private final StreamInfo invalidStreamInfo;

    /**
     * Create a new manager object.
     * 
     * @param api A reference back to the API, so requests can be done.
     * @param listener Listener that is informed once stream info is updated.
     */
    public StreamInfoManager(TwitchApi api, StreamInfoListener listener) {
        this.listener = listener;
        this.api = api;
        // Create StreamInfo object for invalid stream names
        invalidStreamInfo = new StreamInfo("invalid", listener);
        // Set as requested so it won't ever be requested
        invalidStreamInfo.setRequested();
    }
    
    public synchronized void manualRefresh() {
        followsRequestedET.reset();
        streamsInfoRequestedET.reset();
    }
    
    public synchronized void getFollowedStreams(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        if (!prevToken.equals(token)) {
            followsRequestErrors = 0;
        }
        // For a manual refresh, the delay is reset
        if (checkTimePassed(followsRequestedET, UPDATE_FOLLOWS_DELAY,
                followsRequestErrors)) {
            prevToken = token;
            followsRequestedET.set();
            // This is only reset here, otherwise only increased
            followedStreamsRequests = 1;
            api.requests.requestFollowedStreams(token, 0);
        }
    }
    
    /**
     * In order to return more than 100 followed streams, several requests may
     * be necessary.
     * <p>
     * On the first request the value of {@code followedStreamsRequests} is set
     * to 1 (in getFollowedStreams() when enough time has passed), whereas
     * request results may call this for the next request (if number of results
     * is equal to the limit).
     */
    private void getFollowedStreamsNext() {
        if (followedStreamsRequests < FOLLOWED_STREAMS_REQUEST_LIMIT) {
            int offset = FOLLOWED_STREAMS_LIMIT*followedStreamsRequests;
            api.requests.requestFollowedStreams(prevToken, offset);
            followedStreamsRequests++;
        } else {
            LOGGER.warning("Followed streams: Not getting next url "
                    + "(limit reached: "+FOLLOWED_STREAMS_REQUEST_LIMIT+")");
        }
    }
    
    public static boolean checkTimePassed(ElapsedTime et, int delay, int errors) {
        return et.secondsElapsed(delay + errors * delay/4);
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
     * @param stream A single stream that the StreamInfo is being requested for
     * @param streams A Set of streams, all of which will probably be requested
     *  soon or from which status changes are expected (for example all open
     *  channels in chat). These might all be requested at this time.
     * @return The StreamInfo object
     */
    public synchronized StreamInfo getStreamInfo(String stream, Set<String> streams) {
        // Prefer to request several stream infos at once
        requestStreamsInfo(streams);
        checkRerequest(streams);
        // Then check to request this one, which can happen if the streams
        // request was requested not too long ago. This ofc is also to actually
        // return the StreamInfo object.
        StreamInfo cached = getStreamInfo(stream);
        if (cached.hasExpired() && !cached.isRequested()) {
            cached.setRequested();
            api.requests.requestStreamInfo(stream);
        }
        return cached;
    }
    
    public synchronized StreamInfo getCachedStreamInfo(String stream) {
        return cachedStreamInfo.get(StringUtil.toLowerCase(stream));
    }
    
    /**
     * Gets a StreamInfo object for the given stream name. Either returns the
     * already existing StreamInfo object for this stream or creates a new one.
     * 
     * @param stream
     * @return 
     */
    private StreamInfo getStreamInfo(String stream) {
        if (stream == null || stream.isEmpty()) {
            return invalidStreamInfo;
        }
        stream = StringUtil.toLowerCase(stream);
        StreamInfo cached = cachedStreamInfo.get(stream);
        if (cached == null) {
            cached = new StreamInfo(stream, listener);
            cached.setExpiresAfter(UPDATE_STREAMINFO_DELAY);
            cachedStreamInfo.put(stream, cached);
        }
        return cached;
    }
    
    /**
     * Check the given channels whether they meet the criteria for rechecking
     * offline status. This check is performed relatively often, but as opposed
     * to requestStreamsInfo() requests are only done rarely (when a stream just
     * went offline).
     * 
     * @param streams 
     */
    private void checkRerequest(Set<String> streams) {
        if (!checkTimePassed(specialCheckDoneET, SPECIAL_CHECK_DELAY,
                streamsRequestErrors)) {
            return;
        }
        requestStreamsInfo2(streams, true);
    }
    
    /**
     * Request info for a list of streams regulary. This first checks if enough
     * time has passed since the last request, then requests those streams that
     * are not currently waiting for a response.
     * 
     * @param streams 
     */
    private void requestStreamsInfo(Set<String> streams) {
        if (!checkTimePassed(streamsInfoRequestedET, UPDATE_STREAMINFO_DELAY,
                streamsRequestErrors)) {
            return;
        }
        requestStreamsInfo2(streams, false);
    }
    
    /**
     * Request info for a list of streams in one request. Puts together a list
     * of streams that fit the criteria (mainly that they aren't currently
     * waiting for a response) and then actually starts a request if there are
     * streams to request.
     * 
     * This is used for two different kinds of requests: The regular request of
     * all open channels and the special request that is used to recheck offline
     * status.
     *
     * @param streams The list of streams
     * @param special Whether this is a special request to recheck offline
     * status
     */
    private void requestStreamsInfo2(Set<String> streams, boolean special) {
        Set<String> streamsForRequest = new HashSet<>();
        Set<StreamInfo> streamInfosForRequest = new HashSet<>();
        for (String stream : streams) {
            StreamInfo cached = getStreamInfo(stream);
            // Don't check for expired, so all open chans are
            // requested, meaning hopefully less requests
            if (!cached.isRequested() && (!special || cached.recheckOffline())) {
                streamsForRequest.add(stream);
                streamInfosForRequest.add(cached);
                cached.setRequested();
            }
            if (streamsForRequest.size() > 90) {
                break;
            }
        }
        if (special) {
            specialCheckDoneET.set();
        }
        if (!streamsForRequest.isEmpty()) {
            if (!special) {
                streamsInfoRequestedET.set();
            }
            api.requests.requestStreamsInfo(streamsForRequest, streamInfosForRequest);
        }
    }
    
    /**
     * Parse result of the /streams/:channel/ request.
     * 
     * @param url
     * @param result
     * @param responseCode
     * @param stream 
     */
    protected synchronized void requestResult(String result, int responseCode, String stream) {
        // Probably don't use a lock here, but rely on the lock of the caller
        // so no deadlock can accur in case cachedStreamInfo is accessed from
        // somehwere where the TwitchApi isn't locked
        StreamInfo streamInfo = getStreamInfo(stream);
        if (result == null) {
            LOGGER.warning("Error requesting stream data "+stream+": " + result);
            if (responseCode == 404) {
                streamInfo.setExpiresAfter(UPDATE_STREAMINFO_DELAY_NOT_FOUND);
                streamInfo.setNotFound();
            }
            streamInfo.setUpdateFailed();
            return;
        }
        parseStream(streamInfo, result);

    }
    
    /**
     * Parse result of the /streams?channel=[..] request.
     * 
     * @param url
     * @param result
     * @param responseCode 
     */
    protected synchronized void requestResultStreams(String result, int responseCode, Set<StreamInfo> expected) {
        if (responseCode != 200 || result == null) {
            if (responseCode == 404) {
                streamsRequestErrors += 2;
            } else {
                streamsRequestErrors++;
            }
            LOGGER.warning("Unexpected response code "+responseCode
                    +" or result null (errors: "+streamsRequestErrors+")");
            streamsRequestError(expected);
        } else {
            if (parseStreams(result, expected) == -1) {
                streamsRequestErrors++;
            } else {
                streamsRequestErrors = 0;
            }
        }
    }
    
    /**
     * Can be used to tell all the StreamInfo objects that the update failed.
     * 
     * @param expected 
     */
    private void streamsRequestError(Set<StreamInfo> expected) {
        if (expected != null) {
            for (StreamInfo info : expected) {
                info.setUpdateFailed();
            }
        }
    }
    
    protected synchronized void requestResultFollows(String result, int responseCode) {
        if (responseCode == 200 && result != null) {
            int count = parseStreams(result, null);
            LOGGER.info("Got "+count+" (limit: "+FOLLOWED_STREAMS_LIMIT+") followed streams.");
            if (count == FOLLOWED_STREAMS_LIMIT) {
                getFollowedStreamsNext();
            }
            followsRequestErrors = 0;
        } else if (responseCode == 401) {
            followsRequestErrors += 4;
            LOGGER.warning("Access denied when getting followed streams.");
            api.accessDenied();
        } else {
            followsRequestErrors++;
        }
    }

    /**
     * Parses a single stream info response, which can contain a stream object
     * if the stream is online.
     * 
     * This parses the response to /streams/:channel/
     *
     * @param streamInfo The StreamInfo object to write the changes into
     * @param json The JSON to parse from
     */
    private void parseStream(StreamInfo streamInfo, String json) {
        
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            
            /**
             * See Requests.requestStreamInfoById().
             */
            JSONArray streams = (JSONArray) root.get("streams");
            if (streams.size() == 0) {
                streamInfo.setOffline();
            }
            else {
                JSONObject stream = (JSONObject) streams.get(0);

                StreamInfo result = parseStream(stream, false);
                if (result == null || result != streamInfo) {
                    LOGGER.warning("Error parsing stream ("
                            + streamInfo.getStream() + "): " + json);
                    streamInfo.setUpdateFailed();
                }
            }
        }
        catch (Exception ex) {
            streamInfo.setUpdateFailed();
            LOGGER.warning("Error parsing stream info: "+ex);
        }
        
    }

    /**
     * Parses a list of stream objects. Goes through all stream objects and
     * parses them (which also means it updates the appropriate StreamInfo
     * objects in the process), then sets all remaining streams that were
     * expected in this response to offline.
     * 
     * This parses the response to /streams?channel=[..]
     * 
     * @param json The JSON to parse, can't be null
     * @param streamInfos The StreamInfo objects that were expected for this
     *  response, can be null if none could be expected due to the nature of the
     *  request (e.g. followed channels)
     * @return The number of items or -1 if the whole json could not be parsed
     */
    private int parseStreams(String json, Set<StreamInfo> streamInfos) {
        try {
            JSONParser parser = new JSONParser();
            
            JSONArray streamsArray;
            try {
                JSONObject root = (JSONObject)parser.parse(json);
                streamsArray = (JSONArray)root.get("streams");
            } catch (ClassCastException ex) {
                LOGGER.warning("Error parsing streams: unexpected type");
                streamsRequestError(streamInfos);
                return -1;
            }
            
            if (streamsArray == null) {
                LOGGER.warning("Error parsing streams: streams array not found");
                streamsRequestError(streamInfos);
                return -1;
            }
            
            // Go through all streams, parse and update
            for (Object obj : streamsArray) {
                if (obj instanceof JSONObject) {
                    StreamInfo parsedInfo =
                            parseStream((JSONObject)obj, streamInfos == null);
                    if (parsedInfo == null) {
                        // Can't use setUpdateSucceeded(false) because it is
                        // not known which StreamInfo object it would be.
                        LOGGER.warning("Error parsing stream "+(JSONObject)obj);
                    }
                    if (streamInfos != null) {
                        streamInfos.remove(parsedInfo);
                    }
                } else {
                    LOGGER.warning("Element in array wasn't JSONObject "+obj);
                }
            }
            // Anything remaining, that was requested, should be offline
            // (or invalid, but which it is can't be determined)
            if (streamInfos != null) {
//                if (!streamInfos.isEmpty()) {
//                    System.out.println(json);
//                }
                for (StreamInfo info : streamInfos) {
                    info.setOffline();
                }
            }
            return streamsArray.size();
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing streams info: "+ex.getLocalizedMessage());
            streamsRequestError(streamInfos);
            return -1;
        }
    }
    
    /**
     * If uptime is greater than 10 years, it's probably not valid. Streams that
     * just started appear to sometimes return a wrong start time.
     */
    private static final long VALID_UPTIME_LIMIT = 10*365*24*60*60*1000;

    /**
     * Parse a stream object into a StreamInfo object. This gets the name of the
     * stream and gets the appropriate StreamInfo object, in which it then loads
     * the other extracted data like status, game, viewercount.
     * 
     * This is used for both the response from /streams/:channel/ as well as
     * /streams?channel=[..]
     *
     * @param stream The JSONObject containing the stream object.
     * @return The StreamInfo object or null if an error occured.
     */
    private StreamInfo parseStream(JSONObject stream, boolean follows) {
        if (stream == null) {
            LOGGER.warning("Error parsing stream: Should be JSONObject, not null");
            return null;
        }
        Number viewersTemp;
        String status;
        String game;
        String name;
        String display_name;
        StreamType streamType;
        long timeStarted = -1;
        String userId = null;
        boolean noChannelObject = false;
        String logo;
        try {
            // Get stream data
            viewersTemp = (Number) stream.get("viewers");
            //community_ids = JSONUtil.getStringList(stream, "community_ids");
            
            // Stream Type
            switch (JSONUtil.getString(stream, "stream_type")) {
                case "watch_party":
                    streamType = StreamType.WATCH_PARTY;
                    break;
                case "rerun":
                    streamType = StreamType.RERUN;
                    break;
                case "premiere":
                    streamType = StreamType.PREMIERE;
                    break;
                default:
                    streamType = StreamType.LIVE;
            }
            
            // Get channel data
            JSONObject channel = (JSONObject) stream.get("channel");
            if (channel == null) {
                LOGGER.warning("Error parsing StreamInfo: channel null");
                return null;
            }
            status = (String) channel.get("status");
            game = (String) channel.get("game");
            name = (String) channel.get("name");
            display_name = (String) channel.get("display_name");
            
            userId = String.valueOf(JSONUtil.getLong(channel, "_id", -1));
            if (userId.equals("-1")) {
                userId = JSONUtil.getString(channel, "_id");
            }
            if (!channel.containsKey("status")) {
                LOGGER.warning("Error parsing StreamInfo: no channel object ("+name+")");
                noChannelObject = true;
            }
            logo = JSONUtil.getString(channel, "logo");
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing StreamInfo: unpexected type");
            return null;
        }
        // Checks (status and game are allowed to be null)
        if (name == null || name.isEmpty()) {
            LOGGER.warning("Error parsing StreamInfo: name null or empty");
            return null;
        }
        if (viewersTemp == null) {
            LOGGER.warning("Error parsing StreamInfo: viewercount null ("+name+")");
            return null;
        }
        
        // Try to parse created_at
        try {
            timeStarted = DateTime.parseDatetime((String) stream.get("created_at"));
            if (timeStarted + VALID_UPTIME_LIMIT < System.currentTimeMillis()) {
                LOGGER.warning("Warning: Stream created_at for "+name+" seems invalid ("+stream.get("created_at")+")");
                timeStarted = -1;
            }
        } catch (Exception ex) {
            LOGGER.warning("Warning parsing StreamInfo: could not parse created_at ("+ex+")");
        }

        // Final stuff
        int viewers = viewersTemp.intValue();
        if (viewers < 0) {
            viewers = 0;
            LOGGER.warning("Warning: Viewercount should not be negative, set to 0 ("+name+").");
        }

        // Get and update stream info
        StreamInfo streamInfo = getStreamInfo(name);
        if (noChannelObject) {
            /**
             * If no channel object was present, assume previous title/game.
             */
            status = streamInfo.getStatus();
            game = streamInfo.getGame();
        }
        streamInfo.setDisplayName(display_name);
        if (streamInfo.setUserId(userId)) {
            // If not already done, send userId to UserIDs manager
            api.setUserId(name, userId);
        }
        if (logo != null) {
            streamInfo.setLogo(logo);
        }
        
        // Community (if cached, will immediately set Community correct again
        // for use in history, otherwise requested async and not in this history
        // item)
        streamInfo.setCommunities(null);
        //api.getCommunity(community_id, (r,e) -> { streamInfo.setCommunity(r); });
        //System.out.println("requesting: "+community_ids);
        //api.getCommunities(community_ids, (r,e) -> { streamInfo.setCommunities(r); });
        
        if (follows) {
            streamInfo.setFollowed(status, game, viewers, timeStarted, streamType);
        } else {
            streamInfo.set(status, game, viewers, timeStarted, streamType);
        }
        return streamInfo;
    }
    
}
