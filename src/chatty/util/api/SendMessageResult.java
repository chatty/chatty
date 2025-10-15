
package chatty.util.api;

import chatty.util.JSONUtil;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class SendMessageResult {

    private static final Logger LOGGER = Logger.getLogger(SendMessageResult.class.getName());
    
    public final boolean wasSent;
    public final String msgId;
    public final String dropReasonMessage;
    
    protected SendMessageResult(boolean ok, String msgId, String dropReasonMessage) {
        this.wasSent = ok;
        this.msgId = msgId;
        this.dropReasonMessage = dropReasonMessage;
    }
    
    public static SendMessageResult parse(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray data = (JSONArray) root.get("data");
            JSONObject actualData = (JSONObject) data.get(0);
            String msgId = JSONUtil.getString(actualData, "message_id");
            boolean isOk = JSONUtil.getBoolean(actualData, "is_sent", false);
            JSONObject dropReason = (JSONObject) actualData.get("drop_reason");
            String dropCode = null;
            String dropMessage = null;
            if (dropReason != null) {
                dropCode = JSONUtil.getString(dropReason, "code");
                dropMessage = JSONUtil.getString(dropReason, "message");
            }
            return new SendMessageResult(isOk, msgId, dropMessage);
        }
        catch (Exception ex) {
            LOGGER.warning("Failed sending message: "+ex);
        }
        return new SendMessageResult(false, null, "An error occured sending the message");
    }
    
}
