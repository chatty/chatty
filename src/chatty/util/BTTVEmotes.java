
package chatty.util;

import chatty.Chatty;
import chatty.Helper;
import chatty.util.api.Emoticon;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Requests and parses the BTTV emotes.
 * 
 * @author tduva
 */
public class BTTVEmotes {
    
    private static final Logger LOGGER = Logger.getLogger(BTTVEmotes.class.getName());
    
    private static final String URL = "https://api.betterttv.net/2/emotes";
    //private static final String URL = "http://127.0.0.1/twitch/emotes.json";
    
    private static final String URL_CHANNEL = "https://api.betterttv.net/2/channels/";
    
    private static final int CACHE_EXPIRES_AFTER = 60 * 60 * 24;
    private static final String FILE = Chatty.getCacheDirectory() + "bttvemotes";
    
    private final EmoticonListener listener;
    private final SimpleCache cache;
    
    private final Set<String> requestPending =
            Collections.synchronizedSet(new HashSet<String>());
    private final Set<String> alreadyRequested =
            Collections.synchronizedSet(new HashSet<String>());
    
    public BTTVEmotes(EmoticonListener listener) {
        this.listener = listener;
        this.cache = new SimpleCache("BTTV", FILE, CACHE_EXPIRES_AFTER);
    }
    
    public synchronized void requestEmotes(String channel, boolean forcedUpdate) {
        String stream = Helper.toStream(channel);
        if (!Helper.isValidStream(stream) && !"$global$".equals(stream)) {
            return;
        }
        if (!forcedUpdate && alreadyRequested.contains(stream)) {
            return;
        }
        if (stream.equals("$global$")) {
            loadGlobal(forcedUpdate);
        } else {
            request(stream);
        }
    }
    
    private void loadGlobal(boolean forcedUpdate) {
        String cached = null;
        if (!forcedUpdate) {
            cached = cache.load();
        }
        if (cached != null) {
            loadEmotes(cached, null);
        } else {
            request("$global$");
        }
    }
    
    private void request(final String stream) {
        if (requestPending.contains(stream)) {
            return;
        }
        String url = getUrlForStream(stream);
        alreadyRequested.add(stream);
        requestPending.add(stream);
        UrlRequest request = new UrlRequest(url);
        request.setLabel("BTTV");
        request.async((result, responseCode) -> {
            if (responseCode == 200) {
                if (loadEmotes(result, stream) > 0 && stream.equals("$global$")) {
                    cache.save(result);
                }
            }
            requestPending.remove(stream);
        });
    }
    
    private String getUrlForStream(String stream) {
        if (stream.equals("$global$")) {
            return URL;
        }
        return URL_CHANNEL+stream;
    }
    
    /**
     * Load stuff from the given JSON in the context of the given channel
     * restriction. The channel restriction can be "$global$" which means all
     * channels.
     * 
     * @param json The JSON
     * @param streamRestriction
     * @return 
     */
    private int loadEmotes(String json, String streamRestriction) {
        if (streamRestriction != null && streamRestriction.equals("$global$")) {
            streamRestriction = null;
        }
        Set<Emoticon> emotes = parseEmotes(json, streamRestriction);
        Set<String> bots = parseBots(json);
        LOGGER.info("|[BTTV] Found " + emotes.size() + " emotes / "+bots.size()+" bots");
        listener.receivedEmoticons(emotes);
        listener.receivedBotNames(streamRestriction, bots);
        return emotes.size();
    }
    
    /**
     * Parse list of bots from the given JSON.
     * 
     * @param json
     * @return 
     */
    private static Set<String> parseBots(String json) {
        Set<String> result = new HashSet<>();
        if (json == null) {
            return result;
        }
        JSONParser parser = new JSONParser();
        try {
            JSONObject root = (JSONObject)parser.parse(json);
            JSONArray botsArray = (JSONArray)root.get("bots");
            if (botsArray == null) {
                // No bots for this channel
                return result;
            }
            for (Object o : botsArray) {
                result.add((String)o);
            }
        } catch (ParseException | ClassCastException ex) {
            LOGGER.warning("|[BTTV] Error parsing bots: "+ex);
        }
        return result;
    }
    
    /**
     * Parse emotes from the given JSON.
     * 
     * @param json
     * @param channelRestriction
     * @return 
     */
    private static Set<Emoticon> parseEmotes(String json, String channelRestriction) {
        Set<Emoticon> emotes = new HashSet<>();
        if (json == null) {
            return emotes;
        }
        JSONParser parser = new JSONParser();
        try {
            JSONObject root = (JSONObject)parser.parse(json);
            String urlTemplate = (String)root.get("urlTemplate");
            if (urlTemplate == null || urlTemplate.isEmpty()) {
                LOGGER.warning("No URL Template");
                return emotes;
            }
            JSONArray emotesArray = (JSONArray)root.get("emotes");
            for (Object o : emotesArray) {
                if (o instanceof JSONObject) {
                    Emoticon emote = parseEmote((JSONObject)o, urlTemplate,
                            channelRestriction);
                    if (emote != null) {
                        emotes.add(emote);
                    }
                }
            }
        } catch (ParseException | ClassCastException ex) {
            // ClassCastException is also caught in parseEmote(), so it won't
            // quit completely when one emote is invalid.
            LOGGER.warning("|[BTTV] Error parsing emotes: "+ex);
        }
        return emotes;
    }
    
    /**
     * Parse a single emote from the given JSONObject.
     * 
     * @param o The object containing the emote info
     * @param urlTemplate The URL Template to use for this emote
     * @param channelRestriction The channel restriction to use
     * @return 
     */
    private static Emoticon parseEmote(JSONObject o, String urlTemplate, String channelRestriction) {
        try {
            String url = urlTemplate;
            String code = (String)o.get("code");
            String sourceChannel = (String)o.get("channel");
            String id = (String)o.get("id");
            String imageType = null;
            if (o.get("imageType") instanceof String) {
                imageType = (String)o.get("imageType");
            }
            
            if (code == null || code.isEmpty() || id == null || id.isEmpty()) {
                return null;
            }

            Emoticon.Builder builder = new Emoticon.Builder(Emoticon.Type.BTTV,
                    code, url);
            builder.setCreator(sourceChannel);
            builder.setLiteral(true);
            builder.setStringId(id);
            if (channelRestriction != null) {
                builder.addStreamRestriction(channelRestriction);
                builder.setStream(channelRestriction);
            }
            if (imageType != null && imageType.equals("gif")) {
                builder.setAnimated(true);
            }
            
            // Adds restrictions to emote (if present)
            Object restriction = o.get("restrictions");
            if (restriction != null && restriction instanceof JSONObject) {
                 JSONObject restrictions = (JSONObject)restriction;
                for (Object r : restrictions.keySet()) {
                    boolean knownAndValid = addRestriction(r, restrictions,
                            builder);
                    // Don't add emotes with unknown or invalid restrictions
                    if (!knownAndValid) {
                        return null;
                    }
                }
            }
            return builder.build();
        } catch (ClassCastException | NullPointerException ex) {
            LOGGER.warning("|[BTTV] Error parsing emote: "+o+" ["+ex+"]");
            return null;
        }
    }
    
    /**
     * Helper to add a restriction to the emote. Returns whether the restriction
     * is known and valid, so unknown restrictions can prevent the emote from
     * being added at all.
     * 
     * @param restriction The name of the restriction
     * @param restrictions The value(s) of the restriction
     * @param builder Emote builder to put the restriction in
     * @return true if the restriction is known and valid, false otherwise
     */
    private static boolean addRestriction(Object restriction,
            JSONObject restrictions, Emoticon.Builder builder) {
        try {
            String key = (String)restriction;
            if (key.equals("channels")) {
                for (Object chan : (JSONArray) restrictions.get(restriction)) {
                    if (chan instanceof String) {
                        builder.addStreamRestriction((String) chan);
                    }
                }
                return true;
            } else if (key.equals("emoticonSet")) {
                Object emoticon_set = restrictions.get(key);
                if (emoticon_set != null) {
                    if (emoticon_set instanceof String) {
                        // This also includes "night"
                        return false;
                    } else {
                        builder.setEmoteset(((Number) emoticon_set).intValue());
                        return true;
                    }
                }
            } else {
                /**
                 * Unknown or unhandled restriction, ignore restriction and
                 * return true anyway if restriction value is empty.
                 */
                Object value = restrictions.get(restriction);
                if (value == null || ((JSONArray)value).isEmpty()) {
                    return true;
                }
            }
        } catch (NullPointerException | ClassCastException ex) {
            // Don't do anything, just return false
        }
        return false;
    }
    
}
