
package chatty.util.ffz;

import chatty.Helper;
import chatty.util.api.usericons.Usericon;
import chatty.util.StringUtil;
import chatty.util.UrlRequest;
import chatty.util.api.Emoticon;
import chatty.util.api.EmoticonUpdate;
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
    
    /**
     * The channels that have already been requested in this session.
     */
    private final Set<String> alreadyRequested
            = Collections.synchronizedSet(new HashSet<String>());
    
    /**
     * The channels whose request is currently pending. Channels get removed
     * from here again once the request result is received.
     */
    private final Set<String> requestPending
            = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Feature Friday
     */
    private static final long FEATURE_FRIDAY_UPDATE_DELAY = 6*60*60*1000;
    
    private boolean featureFridayTimerStarted;
    private String featureFridayChannel;
    private int featureFridaySet = -1;
    
    private final WebsocketManager ws;
    
    public FrankerFaceZ(FrankerFaceZListener listener, Settings settings) {
        this.listener = listener;
        this.ws = new WebsocketManager(listener, settings);
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
        request(Type.ROOM, stream, forcedUpdate);
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
        request(Type.GLOBAL, null, forcedUpdate);
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
        request(Type.FEATURE_FRIDAY, null, forcedUpdate);
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
     * @param forcedUpdate Whether to request even if already requested before
     */
    private void request(final Type type, final String stream, boolean forcedUpdate) {
        final String url = getUrl(type, stream);
        if (requestPending.contains(url)
                || (alreadyRequested.contains(url) && !forcedUpdate)) {
            return;
        }
        alreadyRequested.add(url);
        requestPending.add(url);
        
        // Create request and run it in a separate thread
        UrlRequest request = new UrlRequest();
        request.setLabel("FFZ");
        request.setUrl(url);
        request.async((result, responseCode) -> {
            requestPending.remove(url);
            parseResult(type, stream, result);
        });
    }
    
    /**
     * Gets the URL for the given request type and stream.
     * 
     * @param type The type
     * @param stream The stream, if applicable to the type
     * @return The URL as a String
     */
    private String getUrl(Type type, String stream) {
        if (type == Type.GLOBAL) {
            return "https://api.frankerfacez.com/v1/set/global";
        } else if (type == Type.FEATURE_FRIDAY) {
            if (stream == null) {
                return "https://cdn.frankerfacez.com/script/event.json";
//                return "http://127.0.0.1/twitch/ffz_feature";
            } else {
                // The stream is a set id in this case
                return "https://api.frankerfacez.com/v1/set/"+stream;
//                return "http://127.0.0.1/twitch/ffz_v1_set_"+stream;
            }
        } else {
            return "https://api.frankerfacez.com/v1/room/"+stream;
        }
    }
    
    private void parseResult(Type type, String stream, String result) {
        if (result == null) {
            return;
        }
        if (type == Type.FEATURE_FRIDAY && stream == null) {
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
            emotes = FrankerFaceZParsing.parseRoomEmotes(result);
            Usericon modIcon = FrankerFaceZParsing.parseModIcon(result);
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
        
        LOGGER.info("[FFZ] ("+stream+", "+globalText+"): "+emotes.size()+" emotes received.");
        if (!usericons.isEmpty()) {
            LOGGER.info("[FFZ] ("+stream+"): "+usericons.size()+" usericons received.");
        }
        
        // Package accordingly and send the result to the listener
        EmoticonUpdate emotesUpdate;
        if (type == Type.FEATURE_FRIDAY) {
            emotesUpdate = new EmoticonUpdate(emotes, Emoticon.Type.FFZ,
                     Emoticon.SubType.FEATURE_FRIDAY, null);
        } else {
            emotesUpdate = new EmoticonUpdate(emotes);
        }
        listener.channelEmoticonsReceived(emotesUpdate);
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
            LOGGER.info(String.format("[FFZ] No Feature Friday found: %s",
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
            request(Type.FEATURE_FRIDAY, String.valueOf(set), true);
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
        listener.channelEmoticonsReceived(new EmoticonUpdate(null,
                Emoticon.Type.FFZ,
                Emoticon.SubType.FEATURE_FRIDAY,
                null));
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
                LOGGER.info("[FFZ Bots] Found " + botNames.size() + " names");
                listener.botNamesReceived(botNames);
            }
        });
    }
}
