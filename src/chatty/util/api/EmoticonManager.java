
package chatty.util.api;

import chatty.Chatty;
import chatty.Logging;
import chatty.util.SimpleCache;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
public class EmoticonManager {
    
    private static final Logger LOGGER = Logger.getLogger(EmoticonManager.class.getName());
    /**
     * How long the Emoticons can be cached in a file after they are updated
     * from the API.
     */
    public static final int CACHED_EMOTICONS_EXPIRE_AFTER = 60*60*24;
    
    private static final String FILE = Chatty.getCacheDirectory()+"emoticon_images";
    
    private final SimpleCache cache;
    private final TwitchApiResultListener listener;
    
    public EmoticonManager(TwitchApiResultListener listener) {
        this.listener = listener;
        this.cache = new SimpleCache("emoticons", FILE, CACHED_EMOTICONS_EXPIRE_AFTER);
    }
    
    /**
     * Tries to read the emoticons from file.
     * 
     * @param useFileEvenIfExpired
     * @return true if emoticons were loaded, false if emoticons should be
     *  requested from the API.
     */
    protected boolean loadEmoticons(boolean useFileEvenIfExpired) {
        String fromFile = loadEmoticonsFromFile(useFileEvenIfExpired);
        if (fromFile != null) {
            Set<Emoticon> parsed = parseEmoticons(fromFile);
            if (parsed == null) {
                return false;
            }
            listener.receivedEmoticons(parsed);
            LOGGER.info("Using emoticons list from file."+(useFileEvenIfExpired ? " (forced)" : ""));
            return true;
        }
        return false;
    }
    
    protected void emoticonsReceived(String result, String type) {
        Set<Emoticon> parsed = parseEmoticons(result);
        if (parsed != null) {
            if (!parsed.isEmpty()) {
                saveEmoticonsToFile(result);
            }
            if (listener != null) {
                listener.receivedEmoticons(parsed);
            }
        }
        if (parsed == null || parsed.isEmpty()) {
            if (!type.equals("update")) {
                loadEmoticons(true);
            } else {
                LOGGER.log(Logging.USERINFO, "Error requesting emotes from API.");
            }
        }
    }
    
    /**
     * Saves the given json text (which should be the list of emoticons as
     * received from the Twitch API v2) into a file.
     *
     * @param json
     */
    private void saveEmoticonsToFile(String json) {
        synchronized(cache) {
            cache.save(json);
        }
    }
    
    /**
     * Loads emoticons list from the file.
     * 
     * @return The json as received from the Twitch API v2 or null if the file
     * isn't recent enough or an error occured
     */
    private String loadEmoticonsFromFile(boolean loadEvenIfExpired) {
        synchronized(cache) {
            return cache.load(loadEvenIfExpired);
        }
    }
    
    /**
     * Parses the list of emoticons from the Twitch API.
     * 
     * @param json
     * @return 
     */
    private Set<Emoticon> parseEmoticons(String json) {
        Set<Emoticon> result = new HashSet<>();
        if (json == null) {
            return null;
        }
        JSONParser parser = new JSONParser();
        int errors = 0;
        try {
            JSONObject root = (JSONObject)parser.parse(json);
            JSONArray emoticons = (JSONArray)root.get("emoticons");
            for (Object obj : emoticons) {
                if (obj instanceof JSONObject) {
                    JSONObject emote_json = (JSONObject)obj;
                    Emoticon emote = parseEmoticon(emote_json);
                    if (emote == null) {
                        if (errors < 10) {
                            LOGGER.warning("Error loading emote: "+emote_json);
                        }
                        errors++;
                    } else {
                        result.add(emote);
                    }
                }
            }
            if (errors > 0) {
                LOGGER.warning(errors+" emotes couldn't be loaded");
            }
            if (errors > 100) {
                return null;
            }
            return result;
        } catch (ParseException | NullPointerException | ClassCastException ex) {
            LOGGER.warning("Error parsing emoticons: "+ex);
            return null;
        }
    }
    
    /**
     * Parses an Emoticon from the given JSONObject.
     * 
     * @param emote The JSONObject containing the emoticon data
     * @return The Emoticon object or null if an error occured
     */
    private Emoticon parseEmoticon(JSONObject emote) {
        try {
            String code = (String)emote.get("code");
            int id = ((Number)emote.get("id")).intValue();
            String url = Emoticon.getTwitchEmoteUrlById(id, 1);
            Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.TWITCH, code, url);
            if (emote.get("emoticon_set") != null) {
                int emoteSet = ((Number)emote.get("emoticon_set")).intValue();
                b.setEmoteset(emoteSet);
            }
            b.setNumericId(id);
            return b.build();
        } catch (NullPointerException | ClassCastException ex) {
            return null;
        }
    }
    
}
