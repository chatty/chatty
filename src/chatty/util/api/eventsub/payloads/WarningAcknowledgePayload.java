
package chatty.util.api.eventsub.payloads;

import chatty.Helper;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.eventsub.Payload;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class WarningAcknowledgePayload extends Payload {

    public final String stream;
    public final String username;
    
    public WarningAcknowledgePayload(String stream, String username) {
        this.stream = stream;
        this.username = username;
    }
    
    public static WarningAcknowledgePayload decode(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            String stream = JSONUtil.getString(event, "broadcaster_user_login");
            String username = JSONUtil.getString(event, "user_login");
            if (Helper.isValidStream(stream) && Helper.isValidStream(username)) {
                return new WarningAcknowledgePayload(stream, username);
            }
        }
        return null;
    }

}
