
package chatty.util.api.pubsub;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * For notifying users about their message being handled by AutoMod.
 * 
 * @author tduva
 */
public class UserModerationMessageData extends MessageData {
    
    public final String info;
    public final String msgId;
    public final String stream;
    
    public UserModerationMessageData(String topic, String message, String stream, String info, String msgId) {
        super(topic, message);
        this.stream = stream;
        this.info = info;
        this.msgId = msgId;
    }

    public static UserModerationMessageData decode(String topic, String message, Map<String, String> userIds) throws ParseException {
        String stream = Helper.getStreamFromTopic(topic, userIds);
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(message);
        String msgType = (String) root.getOrDefault("type", "");
        JSONObject data = (JSONObject) root.get("data");
        if (msgType.equals("automod_caught_message")) {
            String status = JSONUtil.getString(data, "status", "");
            String msgId = JSONUtil.getString(data, "message_id");
            String info = null;
            switch (status) {
                case "PENDING":
                    info = "You message has been held for review.";
                    break;
                case "ALLOWED":
                    info = "Your message has been approved.";
                    break;
                case "DENIED":
                    info = "Your message has been denied.";
                    break;
                case "EXPIRED":
                    info = "Your message has expired.";
                    break;
            }
            if (!StringUtil.isNullOrEmpty(stream, info, msgId)) {
                return new UserModerationMessageData(topic, message, stream, info, msgId);
            }
        }
        return null;
    }
    
}
