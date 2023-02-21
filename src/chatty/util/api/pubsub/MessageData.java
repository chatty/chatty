
package chatty.util.api.pubsub;

import chatty.util.Debugging;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Data of a message. Different subclasses contain topic specific data.
 * 
 * @author tduva
 */
public class MessageData {

    public final long created_at = System.currentTimeMillis();
    public final String topic;
    public final String message;

    public MessageData(String topic, String message) {
        this.topic = topic;
        this.message = message;
    }

    public static MessageData decode(JSONObject data, Map<String, String> userIds) throws ParseException {
        if (data == null) {
            return null;
        }
        String topic = (String)data.get("topic");
        String message = (String)data.get("message");
        Debugging.println("pubsub-msg", "%s: %s", topic, message);
        if (topic.startsWith("chat_moderator_actions")) {
            return ModeratorActionData.decode(topic, message, userIds);
        }
        else if (topic.startsWith("automod-queue.")) {
            return ModeratorActionData.decodeAutoMod(topic, message, userIds);
        }
        else if (topic.startsWith("channel-points-channel-v1") || topic.startsWith("community-points-channel-v1")) {
            RewardRedeemedMessageData result = RewardRedeemedMessageData.decode(topic, message, userIds);
            if (result != null) {
                return result;
            }
        }
        else if (topic.startsWith("user-moderation-notifications")) {
            UserModerationMessageData result = UserModerationMessageData.decode(topic, message, userIds);
            if (result != null) {
                return result;
            }
        }
        else if (topic.startsWith("low-trust-users")) {
            MessageData result = LowTrustUserMessageData.decode(topic, message, userIds);
            if (result != null) {
                return result;
            }
            result = LowTrustUserUpdateData.decode(topic, message, userIds);
            if (result != null) {
                return result;
            }
        }
        return new MessageData(topic, message);
    }

}
