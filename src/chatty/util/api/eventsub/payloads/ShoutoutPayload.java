
package chatty.util.api.eventsub.payloads;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.eventsub.Payload;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class ShoutoutPayload extends Payload {

    public final String stream;
    public final String moderator_login;
    public final String target_login;
    public final String target_name;
    
    public ShoutoutPayload(String stream, String moderator_login, String target_login, String target_name) {
        this.stream = stream;
        this.moderator_login = moderator_login;
        this.target_login = target_login;
        this.target_name = target_name;
    }
    
    public static ShoutoutPayload decode(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            String stream = JSONUtil.getString(event, "broadcaster_user_login");
            String moderatorLogin = JSONUtil.getString(event, "moderator_user_login");
            String targetLogin = JSONUtil.getString(event, "to_broadcaster_user_login");
            String targetName = JSONUtil.getString(event, "to_broadcaster_user_name");
            if (!StringUtil.isNullOrEmpty(stream, moderatorLogin, targetLogin, targetName)) {
                return new ShoutoutPayload(stream, moderatorLogin, targetLogin, targetName);
            }
        }
        return null;
    }
    
}
