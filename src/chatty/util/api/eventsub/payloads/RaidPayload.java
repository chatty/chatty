
package chatty.util.api.eventsub.payloads;

import chatty.util.JSONUtil;
import chatty.util.api.eventsub.Payload;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class RaidPayload extends Payload {

    public final String fromLogin;
    public final String toLogin;
    public final int viewers;
    
    public RaidPayload(String fromLogin, String toLogin, int viewers) {
        this.fromLogin = fromLogin;
        this.toLogin = toLogin;
        this.viewers = viewers;
    }
    
    public static RaidPayload decode(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            String fromLogin = JSONUtil.getString(event, "from_broadcaster_user_login");
            String toLogin = JSONUtil.getString(event, "to_broadcaster_user_login");
            int viewers = JSONUtil.getInteger(event, "viewers", -1);
            return new RaidPayload(fromLogin, toLogin, viewers);
        }
        return null;
    }
    
}
