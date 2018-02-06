
package chatty.util.api;

import chatty.Chatty;
import java.util.HashSet;
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
public class EmoticonManager extends CachedManager {
    
    private static final Logger LOGGER = Logger.getLogger(EmoticonManager.class.getName());
    
    /**
     * How long the Emoticons can be cached in a file after they are updated
     * from the API.
     */
    public static final int CACHED_EMOTICONS_EXPIRE_AFTER = 60*60*24;
    
    private static final String FILE = Chatty.getCacheDirectory()+"emoticon_images";

    private final TwitchApiResultListener listener;
    
    public EmoticonManager(TwitchApiResultListener listener) {
        super(FILE, CACHED_EMOTICONS_EXPIRE_AFTER, "Emoticons");
        this.listener = listener;
    }
    
    @Override
    public boolean handleData(String data) {
        Set<Emoticon> result = parseEmoticons(data);
        if (result == null || result.isEmpty()) {
            return false;
        }
        listener.receivedEmoticons(result);
        return true;
    }
    
    /**
     * Parses the list of emoticons from the Twitch API.
     * 
     * @param json
     * @return 
     */
    private static Set<Emoticon> parseEmoticons(String json) {
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
                    Emoticon emote = parseEmoticon(emote_json, Emoticon.SET_UNDEFINED);
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
     * Parse result of ?emotesets=0 request.
     * 
     * @param json
     * @return 
     */
    protected static Set<Emoticon> parseEmoticonSets(String json) {
        Set<Emoticon> result = new HashSet<>();
        if (json == null) {
            return null;
        }
        JSONParser parser = new JSONParser();
        int errors = 0;
        try {
            JSONObject root = (JSONObject)parser.parse(json);
            JSONObject sets = (JSONObject)root.get("emoticon_sets");
            for (Object key : sets.keySet()) {
                int emoteSet = Integer.parseInt((String)key);
                JSONArray emoticons = (JSONArray)sets.get(key);
                for (Object obj : emoticons) {
                    JSONObject emote_json = (JSONObject)obj;
                    Emoticon emote = parseEmoticon(emote_json, emoteSet);
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
        } catch (Exception ex) {
            LOGGER.warning("Error parsing emoticons by sets: "+ex);
        }
        return null;
    }
    
    /**
     * Parses an Emoticon from the given JSONObject.
     * 
     * @param emote The JSONObject containing the emoticon data
     * @return The Emoticon object or null if an error occured
     */
    private static Emoticon parseEmoticon(JSONObject emote, int emoteSet) {
        try {
            String code = (String)emote.get("code");
            int id = ((Number)emote.get("id")).intValue();
            Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.TWITCH, code, null);
            if (emote.get("emoticon_set") != null) {
                emoteSet = ((Number)emote.get("emoticon_set")).intValue();
            }
            b.setEmoteset(emoteSet);
            b.setNumericId(id);
            return b.build();
        } catch (NullPointerException | ClassCastException ex) {
            return null;
        }
    }

}
