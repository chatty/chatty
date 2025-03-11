
package chatty.util.api.eventsub.payloads;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.eventsub.Payload;
import chatty.util.api.eventsub.payloads.SuspiciousMessagePayload.Treatment;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class SuspiciousUpdatePayload extends Payload {
    
    private static final Logger LOGGER = Logger.getLogger(SuspiciousUpdatePayload.class.getName());

    public final String stream;
    public final String targetUsername;
    public final String moderatorUsername;
    public final Treatment treatment;
    
    public SuspiciousUpdatePayload(
                                   String stream,
                                   String targetUsername,
                                   String moderatorUsername,
                                   Treatment treatment) {
        this.stream = stream;
        this.targetUsername = targetUsername;
        this.moderatorUsername = moderatorUsername;
        this.treatment = treatment;
    }
    
    public static SuspiciousUpdatePayload decode(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        
        String stream = JSONUtil.getString(event, "broadcaster_user_login");
        String targetUsername = JSONUtil.getString(event, "user_login");
        String modUsername = JSONUtil.getString(event, "moderator_user_login");
        Treatment treatment = Treatment.fromId(JSONUtil.getString(event, "low_trust_status"));
        
        if (!StringUtil.isNullOrEmpty(stream, targetUsername, modUsername)) {
            return new SuspiciousUpdatePayload(stream, targetUsername, modUsername, treatment);
        }
        return null;
    }
    
    public String makeInfo() {
        return String.format("Updated to: %s (@%s)",
                             SuspiciousMessagePayload.getTreatmentShort(treatment),
                             moderatorUsername);
    }

}
