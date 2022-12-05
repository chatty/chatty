
package chatty.util.api.eventsub.payloads;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.eventsub.Payload;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class ShieldModePayload extends Payload {

    public final boolean enabled;
    public final String stream;
    public final String moderatorLogin;
    
    public ShieldModePayload(boolean enabled, String stream, String moderatorLogin) {
        this.enabled = enabled;
        this.stream = stream;
        this.moderatorLogin = moderatorLogin;
    }
    
    public static ShieldModePayload decode(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        JSONObject sub = (JSONObject) payload.get("subscription");
        if (event != null && sub != null) {
            boolean enabled = JSONUtil.getString(sub, "type", "").equals("channel.shield_mode.begin");
            String stream = JSONUtil.getString(event, "broadcaster_user_login");
            String moderatorLogin = JSONUtil.getString(event, "moderator_user_login");
            if (!StringUtil.isNullOrEmpty(stream, moderatorLogin)) {
                return new ShieldModePayload(enabled, stream, moderatorLogin);
            }
        }
        return null;
    }
    
}
