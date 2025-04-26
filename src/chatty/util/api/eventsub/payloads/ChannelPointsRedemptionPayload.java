
package chatty.util.api.eventsub.payloads;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.eventsub.Payload;
import org.json.simple.JSONObject;

/**
 * Payload for custom channel point redemptions. Auto redemptions don't seem
 * that necessary, like unlocking emotes or sending highlighted messages that
 * show up anyway.
 *
 * @author tduva
 */
public class ChannelPointsRedemptionPayload extends Payload {

    public final String stream;
    public final String redeemedByUsername;
    public final String rewardId;
    public final String rewardTitle;
    public final int rewardCost;
    public final String status;
    public final String attachedMsg;
    public final String redemptionId;
    public final boolean isUpdate;
    
    public ChannelPointsRedemptionPayload(String stream, String username, String rewardId,
            String rewardTitle, int rewardCost, String status, String attachedMsg, String redemptionId, boolean isUpdate) {
        this.stream = stream;
        this.redeemedByUsername = username;
        this.rewardId = rewardId;
        this.rewardTitle = rewardTitle;
        this.rewardCost = rewardCost;
        this.status = status;
        this.attachedMsg = attachedMsg;
        this.redemptionId = redemptionId;
        this.isUpdate = isUpdate;
    }
    
    public static ChannelPointsRedemptionPayload decode(JSONObject payload, boolean isUpdate) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            String stream = JSONUtil.getString(event, "broadcaster_user_login");
            String username = JSONUtil.getString(event, "user_login");
            String attachedMsg = JSONUtil.getString(event, "user_input");
            String redemptionId = JSONUtil.getString(event, "id");
            
            JSONObject reward = (JSONObject) event.get("reward");
            String rewardId = JSONUtil.getString(reward, "id");
            String rewardTitle = JSONUtil.getString(reward, "title");
            int rewardCost = JSONUtil.getInteger(reward, "cost", 0);
            
            String status = JSONUtil.getString(event, "status");
            
            if (!StringUtil.isNullOrEmpty(stream, username, rewardId, rewardTitle)) {
                return new ChannelPointsRedemptionPayload(stream, username, rewardId,
                        rewardTitle, rewardCost, status, attachedMsg, redemptionId, isUpdate);
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("ChannelPointsRedemption[%s redeemed '%s' for %d points]",
                redeemedByUsername, rewardTitle, rewardCost);
    }
    
} 