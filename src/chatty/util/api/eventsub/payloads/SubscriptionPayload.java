
package chatty.util.api.eventsub.payloads;

import chatty.util.JSONUtil;
import chatty.util.api.eventsub.Payload;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class SubscriptionPayload extends Payload {

    public final String id;
    public final String status;
    
    public SubscriptionPayload(String id, String status) {
        this.id = id;
        this.status = status;
    }
    
    public static SubscriptionPayload decode(JSONObject payload) {
        JSONObject subscription = (JSONObject) payload.get("subscription");
        String id = JSONUtil.getString(subscription, "id");
        String status = JSONUtil.getString(subscription, "status");
        return new SubscriptionPayload(id, status);
    }
    
}
