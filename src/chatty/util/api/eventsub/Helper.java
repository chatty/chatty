
package chatty.util.api.eventsub;

import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class Helper {

    public static String makeAddEventSubBody(String type, Map<String, String> condition, String sessionId, String version) {
        Map<String, Object> root = new HashMap<>();
        root.put("type", type);
        root.put("version", version);
        root.put("condition", condition);
        
        Map<String, String> transport = new HashMap<>();
        transport.put("method", "websocket");
        transport.put("session_id", sessionId);
        root.put("transport", transport);
        
        return new JSONObject(root).toJSONString();
    }
    
}
