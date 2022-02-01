
package chatty.util.api.pubsub;

import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class Helper {
    
    public static String createOutgoingMessage(String type, String nonce, Object data) {
        Map<String, Object> object = new HashMap<>();
        object.put("type", type);
        if (nonce != null) {
            object.put("nonce", nonce);
        }
        if (data != null) {
            object.put("data", data);
        }
        return new JSONObject(object).toJSONString();
    }
    
    public static String getStreamFromTopic(String topic, Map<String, String> userIds) {
        String userId = topic.substring(topic.lastIndexOf(".") + 1);
        return userIds.get(userId);
    }
    
    public static String removeToken(String token, String message) {
        return message.replace(token, "<censored>");
    }
    
    public static void main(String[] args) {
        String topic = "abc.123";
        System.out.println(Long.valueOf(topic.substring(topic.indexOf(".")+1)));
    }
    
}
