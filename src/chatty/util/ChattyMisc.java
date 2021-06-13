
package chatty.util;

import java.util.HashMap;
import java.util.Map;
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
    
    public static void request() {
        UrlRequest request = new UrlRequest("https://tduva.com/res/misc");
        request.setLabel("Misc");
        request.async((result, responseCode) -> {
            if (responseCode == 200) {
                parseMisc(result);
            }
        });
    }
    
    private static void parseMisc(String text) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(text);
            
            parseCombinedEmotes((JSONArray)root.get("combined_emotes"));
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
        LOGGER.info(String.format("Found %d combined emotes", result.size()));
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
    
}
