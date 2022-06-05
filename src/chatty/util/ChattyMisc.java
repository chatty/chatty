
package chatty.util;

import chatty.util.api.Emoticon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class ChattyMisc {
    
    private static final Logger LOGGER = Logger.getLogger(ChattyMisc.class.getName());
    
    private static volatile CombinedEmotesInfo combinedEmotesInfo = new CombinedEmotesInfo();
    private static final Object SMILIES_LOCK = new Object();
    private static Map<String, Set<Emoticon>> smilies;
    private static Map<String, Set<String>> smiliesSets;
    
    public static void request(Runnable responseReceived) {
        UrlRequest request = new UrlRequest("https://tduva.com/res/misc");
        request.setLabel("Misc");
        request.async((result, responseCode) -> {
            if (responseCode == 200) {
                parseMisc(result);
                responseReceived.run();
            }
        });
    }
    
    private static void parseMisc(String text) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(text);
            
            parseCombinedEmotes((JSONArray)root.get("combined_emotes"));
            parseSmilies((JSONObject)root.get("smilies"));
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing misc: "+ex);
        }
    }
    
    //==========================
    // Combined Emotes
    //==========================
    private static void parseCombinedEmotes(JSONArray root) {
        Map<String, Integer> result = new HashMap<>();
        for (Object entry : root) {
            JSONObject emote = (JSONObject)entry;
            String code = JSONUtil.getString(emote, "code");
            int offset = JSONUtil.getInteger(emote, "yOffset", 0);
            if (code != null) {
                result.put(code, offset);
            }
        }
        // TEST
//        result.put("CandyCane", -16);
//        result.put("ReinDeer", -16);
//        result.put("TopHat", -16);
//        result.put("SantaHat", -16);
        ///TEST
        LOGGER.info(String.format(Locale.ROOT, "Found %d combined emotes", result.size()));
        combinedEmotesInfo = new CombinedEmotesInfo(result);
    }
    
    public static CombinedEmotesInfo getCombinedEmotesInfo() {
        return combinedEmotesInfo;
    }
    
    public static class CombinedEmotesInfo {
        
        private final Map<String, Integer> data = new HashMap<>();
        
        public CombinedEmotesInfo() {
            // Empty
        }
        
        public CombinedEmotesInfo(Map<String, Integer> data) {
            this.data.putAll(data);
        }
        
        public boolean containsCode(String code) {
            return data.containsKey(code);
        }
        
        public int getOffset(String code) {
            Integer result = data.get(code);
            if (result != null) {
                return result;
            }
            return 0;
        }

        public boolean isEmpty() {
            return data.isEmpty();
        }
        
    }
    
    //==========================
    // Smilies
    //==========================
    private static void parseSmilies(JSONObject root) {
        Map<String, Set<Emoticon>> result = new HashMap<>();
        Map<String, Set<String>> setsResult = new HashMap<>();
        for (Object key : root.keySet()) {
            String type = (String) key;
            result.put(type, new HashSet<>());
            setsResult.put(type, new HashSet<>());
            JSONObject data = (JSONObject) root.get(key);
            JSONArray emotes = (JSONArray) data.get("emotes");
            for (Object item : emotes) {
                JSONObject emoteData = (JSONObject) item;
                String id = JSONUtil.getString(emoteData, "id");
                String code = JSONUtil.getString(emoteData, "code");
                String regex = JSONUtil.getString(emoteData, "regex");
                Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.TWITCH, code, null);
                b.setStringId(id);
                b.setRegex(regex);
                result.get(type).add(b.build());
            }
            JSONArray sets = (JSONArray) data.get("sets");
            if (sets != null) {
                for (Object item : sets) {
                    setsResult.get(type).add((String) item);
                }
            }
        }
        synchronized(SMILIES_LOCK) {
            smilies = result;
            smiliesSets = setsResult;
        }
    }
    
    public static Map<String, Set<Emoticon>> getSmilies() {
        synchronized(SMILIES_LOCK) {
            return smilies;
        }
    }
    
    public static String getTypeByEmoteId(String emoteId) {
        synchronized (SMILIES_LOCK) {
            if (smilies != null) {
                for (Map.Entry<String, Set<Emoticon>> entry : smilies.entrySet()) {
                    String type = entry.getKey();
                    for (Emoticon emote : entry.getValue()) {
                        if (emote.stringId.equals(emoteId)) {
                            return type;
                        }
                    }
                }
            }
            return null;
        }
    }
    
    public static String getTypeByEmoteSet(String setId) {
        synchronized (SMILIES_LOCK) {
            if (smiliesSets != null) {
                for (Map.Entry<String, Set<String>> entry : smiliesSets.entrySet()) {
                    String type = entry.getKey();
                    for (String set : entry.getValue()) {
                        if (set.equals(setId)) {
                            return type;
                        }
                    }
                }
            }
            return null;
        }
    }
    
}
