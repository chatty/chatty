
package chatty.util.api;

import chatty.util.Debugging;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class EmoticonParsing {
    
    private static final Logger LOGGER = Logger.getLogger(EmoticonParsing.class.getName());
    
    /**
     * Parse result of ?emotesets=0 request.
     * 
     * @param json
     * @return 
     */
    protected static EmoticonUpdate parseEmoticonSets(String json, EmoticonUpdate.Source source) {
        if (json == null) {
            return null;
        }
        Set<Emoticon> emotes = new HashSet<>();
        Set<String> emotesets = new HashSet<>();
        JSONParser parser = new JSONParser();
        int errors = 0;
        try {
            JSONObject root = (JSONObject)parser.parse(json);
            JSONObject sets = (JSONObject)root.get("emoticon_sets");
            for (Object key : sets.keySet()) {
                String emoteSet = (String)key;
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
                        if (!Debugging.isEnabled("et") || !emote.code.equals("joshO")) {
                            emotes.add(emote);
                        }
                    }
                }
                emotesets.add(emoteSet);
            }
            if (errors > 0) {
                LOGGER.warning(errors+" emotes couldn't be loaded");
            }
            if (errors > 100) {
                return null;
            }
            if (source == EmoticonUpdate.Source.USER_EMOTES) {
                return new EmoticonUpdate(emotes, Emoticon.Type.TWITCH, null, null, emotesets, source);
            }
            return new EmoticonUpdate(emotes);
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
    private static Emoticon parseEmoticon(JSONObject emote, String emoteSet) {
        try {
            String code = (String)emote.get("code");
            int id = ((Number)emote.get("id")).intValue();
            Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.TWITCH, code, null);
            if (emote.get("emoticon_set") != null) {
                emoteSet = String.valueOf(((Number)emote.get("emoticon_set")).longValue());
            }
            b.setEmoteset(emoteSet);
            b.setStringId(String.valueOf(id));
            return b.build();
        } catch (NullPointerException | ClassCastException ex) {
            return null;
        }
    }

}
