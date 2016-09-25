
package chatty.util.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * 
 * 
 * @author tduva
 */
public class ChatInfo {
    
    private static final Logger LOGGER = Logger.getLogger(ChatInfo.class.getName());
    
    public final String room;
    public final List<String> rules;
    
    public ChatInfo(String room, List<String> rules) {
        this.room = room;
        this.rules = rules;
    }
    
    public static ChatInfo decode(String room, String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            // Can and is allowed to be null
            List<String> rules = null;
            JSONArray rulesData = (JSONArray)root.get("chat_rules");
            if (rulesData != null) {
                rules = new ArrayList<>();
                for (Object item : rulesData) {
                    if (item instanceof String) {
                        rules.add((String)item);
                    }
                }
            }
            
            return new ChatInfo(room, rules);
        } catch (Exception ex) {
            LOGGER.warning("Error decoding ChatInfo: "+ex);
            return null;
        }
    }
}
