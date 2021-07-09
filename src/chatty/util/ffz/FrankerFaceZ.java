
package chatty.util.ffz;

import chatty.Helper;
import chatty.util.RetryManager;
import chatty.util.api.usericons.Usericon;
import chatty.util.StringUtil;
import chatty.util.UrlRequest;
import chatty.util.api.Emoticon;
import chatty.util.api.EmoticonUpdate;
import chatty.util.api.TwitchApi;
import chatty.util.settings.Settings;
import java.util.*;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Request FrankerFaceZ emoticons,mod icons and bot names.
 * 
 * @author tduva
 */
public class FrankerFaceZ {
    
    private static final Logger LOGGER = Logger.getLogger(FrankerFaceZ.class.getName());
    
    private enum Type { GLOBAL, ROOM, FEATURE_FRIDAY };
    
    private final FrankerFaceZListener listener;
    
    // State
    private boolean botNamesRequested;
    private String botBadgeId = null;
    // stream -> roomBadges(badgeId -> names)
    private final Map<String, Map<String, Set<String>>> roomBadgeUsernames = new HashMap<>();

    /**
     * Feature Friday
     */
    private static final long FEATURE_FRIDAY_UPDATE_DELAY = 6*60*60*1000;
    
    private boolean featureFridayTimerStarted;
    private String featureFridayChannel;
    private int featureFridaySet = -1;
    
    private final WebsocketManager ws;
    private final TwitchApi api;
    
    public FrankerFaceZ(FrankerFaceZListener listener, Settings settings,
            TwitchApi api) {
        this.listener = listener;
        this.ws = new WebsocketManager(listener, settings);
        this.api = api;
    }
    
    public void connectWs() {
        ws.connect();
    }
    
    public void disconnectWs() {
        ws.disconnect();
    }
    
    public String getWsStatus() {
        return ws.getStatus();
    }
    
    public boolean isWsConnected() {
        return ws.isConnected();
    }
    
    public void joined(String room) {
        room = Helper.toStream(room);
        ws.addRoom(room);
    }
    
    public void left(String room) {
        room = Helper.toStream(room);
        ws.removeRoom(room);
    }
    
    public void setFollowing(String user, String room, String following) {
        ws.setFollowing(user, room, following);
    }
    
    /**
     * Requests the emotes for the given channel and global emotes. It only
     * requests each set of emotes once, unless {@code forcedUpdate} is true.
     * 
     * @param stream The name of the channel/stream
     * @param forcedUpdate Whether to update even if it was already requested
     */
    public synchronized void requestEmotes(String stream, boolean forcedUpdate) {
        stream = Helper.toStream(stream);
        if (stream == null || stream.isEmpty()) {
            return;
        }
        String username = stream;
        api.getUserId(r -> {
            if (!r.hasError()) {
                request(Type.ROOM, username, r.getId(username), forcedUpdate);
            }
        }, username);
        requestGlobalEmotes(false);
        if (!botNamesRequested) {
            requestBotNames();
            botNamesRequested = true;
        }
    }
    
    /**
     * Request global FFZ emotes.
     * 
     * @param forcedUpdate If this is true, it forces the update, otherwise it
     * only requests the emotes when not already requested this session
     */
    public synchronized void requestGlobalEmotes(boolean forcedUpdate) {
        request(Type.GLOBAL, null, null, forcedUpdate);
        requestFeatureFridayEmotes(forcedUpdate);
    }
    
    /**
     * Start timer to check for Feature Friday emotes. Does nothing if the timer
     * is already started.
     */
    public synchronized void autoUpdateFeatureFridayEmotes() {
        if (featureFridayTimerStarted) {
            return;
        }
        featureFridayTimerStarted = true;
        Timer timer = new Timer("FFZ Feature Friday", true);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                requestFeatureFridayEmotes(true);
            }
        }, FEATURE_FRIDAY_UPDATE_DELAY, FEATURE_FRIDAY_UPDATE_DELAY);
    }
    
    /**
     * Request Feature Friday emotes.
     * 
     * @param forcedUpdate If this is true, it forces the update, otherwise it
     * only requests the emotes when not already requested this session
     */
    public synchronized void requestFeatureFridayEmotes(boolean forcedUpdate) {
        request(Type.FEATURE_FRIDAY, null, null, forcedUpdate);
    }
    
    /**
     * Issue a request of the given type and stream.
     * 
     * <p>
     * The URL which is used for the request is build from the paramters. Only
     * requests each URL once, unless {@code forcedUpdate} is true. Always
     * prevents the same URL from being requested twice at the same time.</p>
     * 
     * <p>This is not safe to be called unsynchronized, because of the way
     * check the URL for being already requested/pending is done.</p>
     *
     * @param type The type of request
     * @param stream The stream, can be {@code null} if not needed for this type
     * @param id The id to use for the request, depending on the type
     * @param forcedUpdate Whether to request even if already requested before
     */
    private synchronized void request(final Type type, final String stream,
            String id, boolean forcedUpdate) {
        final String url = getUrl(type, id);
        if (forcedUpdate) {
            requestNow(type, stream, id, url);
        }
        else {
            RetryManager.getInstance().retry(url, k -> requestNow(type, stream, id, url));
        }
    }
    
    private void requestNow(final Type type, final String stream, String id, String url) {
        // Create request and run it in a separate thread
        UrlRequest request = new UrlRequest();
        request.setLabel("FFZ/"+stream);
        request.setUrl(url);
        request.async((result, responseCode) -> {
            if (Integer.toString(responseCode).startsWith("4")) {
                RetryManager.getInstance().setNotFound(url);
            }
            else if (responseCode != 200 && result == null) {
                RetryManager.getInstance().setError(url);
            }
            else {
                RetryManager.getInstance().setSuccess(url);
            }
            parseResult(type, stream, id, result);
        });
    }
    
    /**
     * Gets the URL for the given request type and stream.
     * 
     * @param type The type
     * @param id The stream, if applicable to the type
     * @return The URL as a String
     */
    private String getUrl(Type type, String id) {
        if (type == Type.GLOBAL) {
            return "https://api.frankerfacez.com/v1/set/global";
        } else if (type == Type.FEATURE_FRIDAY) {
            if (id == null) {
                return "https://cdn.frankerfacez.com/script/event.json";
//                return "http://127.0.0.1/twitch/ffz_feature";
            } else {
                // The stream is a set id in this case
                return "https://api.frankerfacez.com/v1/set/"+id;
//                return "http://127.0.0.1/twitch/ffz_v1_set_"+stream;
            }
        } else {
            return "https://api.frankerfacez.com/v1/room/id/"+id;
        }
    }
    
    /**
     * Parse the result of several types of API requests.
     * 
     * @param type The type of request
     * @param stream The stream (null for some types), just used for info here
     * @param id The id of the resource that was requested, depending on the
     * type (e.g. room id or emote set)
     * @param result The result JSON, may be null if the request failed
     */
    private void parseResult(Type type, String stream, String id, String result) {
        if (result == null) {
            return;
        }
        if (type == Type.FEATURE_FRIDAY && id == null) {
            // Response of the first request having only the info which channel
            handleFeatureFriday(result);
            return;
        }
        
        // Determine whether these emotes should be global
        final boolean global = type == Type.GLOBAL || type == Type.FEATURE_FRIDAY;
        String globalText = global ? "global" : "local";
        
        Set<Emoticon> emotes = new HashSet<>();
        List<Usericon> usericons = new ArrayList<>();
        
        // Parse depending on type
        if (type == Type.GLOBAL) {
            emotes = FrankerFaceZParsing.parseGlobalEmotes(result);
        } else if (type == Type.ROOM) {
            // If type is ROOM, stream should be available
            emotes = FrankerFaceZParsing.parseRoomEmotes(result, stream);
            addRoomBadgeUsernames(stream, FrankerFaceZParsing.parseRoomBadges(result));
            Usericon modIcon = FrankerFaceZParsing.parseModIcon(result, stream);
            if (modIcon != null) {
                usericons.add(modIcon);
            }
        } else if (type == Type.FEATURE_FRIDAY) {
            emotes = FrankerFaceZParsing.parseSetEmotes(result, Emoticon.SubType.FEATURE_FRIDAY, null);
            for (Emoticon emote : emotes) {
                if (featureFridayChannel != null) {
                    emote.setStream(featureFridayChannel);
                }
            }
        }
        
        LOGGER.info("|[FFZ] ("+stream+", "+globalText+"): "+emotes.size()+" emotes received.");
        if (!usericons.isEmpty()) {
            LOGGER.info("|[FFZ] ("+stream+"): "+usericons.size()+" usericons received.");
        }
        
        // Package accordingly and send the result to the listener
        EmoticonUpdate.Builder updateBuilder = new EmoticonUpdate.Builder(emotes);
        updateBuilder.setTypeToRemove(Emoticon.Type.FFZ);
        if (type == Type.FEATURE_FRIDAY) {
            updateBuilder.setSubTypeToRemove(Emoticon.SubType.FEATURE_FRIDAY);
        }
        else if (type == Type.ROOM) {
            updateBuilder.setSubTypeToRemove(Emoticon.SubType.REGULAR);
            updateBuilder.setRoomToRemove(stream);
        }
        else if (type == Type.GLOBAL) {
            updateBuilder.setSubTypeToRemove(Emoticon.SubType.REGULAR);
        }
        listener.channelEmoticonsReceived(updateBuilder.build());
        // Return icons if mod icon was found (will be empty otherwise)
        listener.usericonsReceived(usericons);
    }
    
    /**
     * Parse event JSON, update variables and request appropriate emotes if
     * feature friday was found.
     * 
     * @param json 
     */
    private void handleFeatureFriday(String json) {
        // Also updates featureFridayChannel field
        int set = parseFeatureFriday(json);
        if (set == -1) {
            // No feature friday found
            featureFridaySet = -1;
            clearFeatureFridayEmotes();
            LOGGER.info(String.format("|[FFZ] No Feature Friday found: %s",
                    StringUtil.trim(StringUtil.removeLinebreakCharacters(
                            StringUtil.shortenTo(json, 100)))));
        } else {
            // Feature friday found
            if (featureFridaySet != set) {
                /**
                 * If set changed, clear current emotes. If set is still the
                 * same then it can be assumed that emotes haven't changed much,
                 * so removing them when the actual emotes request is returned
                 * should be enough.
                 */
                clearFeatureFridayEmotes();
            }
            featureFridaySet = set;
            request(Type.FEATURE_FRIDAY, null, String.valueOf(set), true);
        }
    }
    
    /**
     * Parse event JSON and return feature friday emote set. Also updates the
     * feature friday channel variable.
     * 
     * @param json
     * @return The feature friday FFZ emote set, or -1 if none was found or an
     * error occured
     */
    private int parseFeatureFriday(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            int set = ((Number)root.get("set")).intValue();
            featureFridayChannel = (String)root.get("channel");
            return set;
        } catch (ParseException | NullPointerException | ClassCastException ex) {
            // Assume no feature friday
        }
        featureFridayChannel = null;
        return -1;
    }
    
    /**
     * Send a message to the listener to clear all FFZ Feature Friday emotes.
     */
    private void clearFeatureFridayEmotes() {
        EmoticonUpdate.Builder updateBuilder = new EmoticonUpdate.Builder(null);
        updateBuilder.setTypeToRemove(Emoticon.Type.FFZ);
        updateBuilder.setSubTypeToRemove(Emoticon.SubType.FEATURE_FRIDAY);
        listener.channelEmoticonsReceived(updateBuilder.build());
    }

    /**
     * Request and parse FFZ bot names.
     */
    public void requestBotNames() {
        UrlRequest request = new UrlRequest("https://api.frankerfacez.com/v1/badge/bot");
        request.setLabel("FFZ Bots");
        request.async((result, responseCode) -> {
            if (result != null && responseCode == 200) {
                Set<String> botNames = FrankerFaceZParsing.getBotNames(result);
                LOGGER.info("|[FFZ Bots] Found " + botNames.size() + " names");
                listener.botNamesReceived(null, botNames);
                synchronized(roomBadgeUsernames) {
                    // Find bot badge id, so it can be used for room badges
                    botBadgeId = FrankerFaceZParsing.getBotBadgeId(result);
                }
                updateRoomBotNames();
            }
        });
    }
    
    /**
     * Cache room badges, so bot names can be retrieved from it.
     * 
     * @param stream
     * @param names 
     */
    private void addRoomBadgeUsernames(String stream, Map<String, Set<String>> names) {
        if (stream == null || names == null || names.isEmpty()) {
            return;
        }
        synchronized(roomBadgeUsernames) {
            roomBadgeUsernames.put(stream, names);
        }
        updateRoomBotNames();
    }
    
    /**
     * Check the cached room badges for the bot badge id and add the bot names
     * if present. Clear cached badges afterwards since they don't have any
     * other use at the moment.
     */
    private void updateRoomBotNames() {
        Map<String, Set<String>> result = new HashMap<>();
        synchronized (roomBadgeUsernames) {
            if (botBadgeId != null) {
                for (Map.Entry<String, Map<String, Set<String>>> room : roomBadgeUsernames.entrySet()) {
                    String stream = room.getKey();
                    Set<String> names = room.getValue().get(botBadgeId);
                    if (names != null) {
                        result.put(stream, names);
                    }
                }
                roomBadgeUsernames.clear();
            }
        }
        for (Map.Entry<String, Set<String>> entry : result.entrySet()) {
            LOGGER.info("|[FFZ Bots] ("+entry.getKey()+"): Found " + entry.getValue().size() + " names");
            listener.botNamesReceived(entry.getKey(), entry.getValue());
        }
    }
    
}
