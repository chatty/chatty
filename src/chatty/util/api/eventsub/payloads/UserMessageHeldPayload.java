
package chatty.util.api.eventsub.payloads;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.eventsub.Payload;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class UserMessageHeldPayload extends Payload {

    public final String info;
    public final String stream;
    
    public UserMessageHeldPayload(String stream, String info) {
        this.stream = stream;
        this.info = info;
    }
    
    public static UserMessageHeldPayload decode(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            String stream = JSONUtil.getString(event, "broadcaster_user_login");
            String msgText = JSONUtil.getString((JSONObject) event.get("message"), "text");
            String status = JSONUtil.getString(event, "status");
            String info = String.format(getInfoTemplate(status),
                                     StringUtil.shortenTo(msgText, 30));
            if (!StringUtil.isNullOrEmpty(stream, info)) {
                return new UserMessageHeldPayload(stream, info);
            }
        }
        return null;
    }
    
    private static String getInfoTemplate(String status) {
        if (status == null) {
            return "Your message '%s' has been held for review.";
        }
        else {
            switch (status) {
                case "approved":
                    return "Your held message '%s' has been approved.";
                case "denied":
                    return "Your held message '%s' has been denied.";
                case "invalid":
                    return "Your held message '%s' has expired.";
            }
        }
        return "Your held message '%s' has had an unexpected update.";
    }
    
}
