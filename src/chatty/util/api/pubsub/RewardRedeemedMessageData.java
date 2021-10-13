
package chatty.util.api.pubsub;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
public class RewardRedeemedMessageData extends MessageData {
    
    public final String stream;
    public final String msg;
    public final String attached_msg;
    public final String username;
    public final String type;
    public final String reward_id;
    
    public RewardRedeemedMessageData(String topic, String message, String stream, String type, String username, String msg, String attachedMsg, String reward_id) {
        super(topic, message);
        this.stream = stream;
        this.msg = msg;
        this.attached_msg = attachedMsg;
        this.type = type;
        this.username = username;
        this.reward_id = reward_id;
    }
    
    public static RewardRedeemedMessageData decode(String topic, String message, Map<String, String> userIds) throws ParseException {
        String stream = Helper.getStreamFromTopic(topic, userIds);
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject)parser.parse(message);
        if (topic.startsWith("channel-points-channel-v1") || topic.startsWith("community-points-channel-v1")) {
            String msgType = (String)root.getOrDefault("type", "");
            JSONObject data = (JSONObject)root.get("data");
            if (msgType.equals("reward-redeemed")) {
                JSONObject redemption = (JSONObject)data.get("redemption");
                JSONObject user = (JSONObject)redemption.get("user");
                String displayName = JSONUtil.getString(user, "display_name");
                String username = JSONUtil.getString(user, "login");
                JSONObject reward = (JSONObject)redemption.get("reward");
                String reward_id = JSONUtil.getString(reward, "id");
                String title = JSONUtil.getString(reward, "title");
                String input = JSONUtil.getString(redemption, "user_input");
                String status = JSONUtil.getString(redemption, "status");
                int cost = JSONUtil.getInteger(reward, "cost", -1);
                if (!StringUtil.isNullOrEmpty(username, displayName, stream, reward_id)) {
                    String fullfilled = status != null && status.equalsIgnoreCase("fullfilled") ? " (fullfilled)" : "";
                    String msg = String.format("%s redeemed %s (%,d)%s",
                            displayName, title, cost, fullfilled);
                    return new RewardRedeemedMessageData(topic, message, stream, "Points", username, msg, input, reward_id);
                }
            }
        }
        return null;
    }
    
}
