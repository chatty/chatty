
package chatty.util.api;

import chatty.util.JSONUtil;
import java.util.Objects;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * The message pinned to the chat by a moderator.
 * 
 * @author tduva
 */
public class PinnedMessage {
    
    private static final Logger LOGGER = Logger.getLogger(PinnedMessage.class.getName());

    public final String stream;
    public final String msgId;
    public final String broadcasterId;
    public final String senderUsername;
    public final String pinnedByUsername;
    public final String messageText;
    public final long endsAt;
    
    public PinnedMessage(String stream, String msgId, String broadcasterId, String senderUsername, String pinnedByUsername, String messageText, long endsAt) {
        this.stream = stream;
        this.msgId = msgId;
        this.broadcasterId = broadcasterId;
        this.senderUsername = senderUsername;
        this.pinnedByUsername = pinnedByUsername;
        this.messageText = messageText;
        this.endsAt = endsAt;
    }
    
    public static PinnedMessage parse(String stream, String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray data = (JSONArray) root.get("data");
            if (data.isEmpty()) {
                return null;
            }
            JSONObject entry = (JSONObject) data.iterator().next();
            String msgId = JSONUtil.getString(entry, "message_id");
            String broadcasterId = JSONUtil.getString(entry, "broadcaster_id");
            String senderUsername = JSONUtil.getString(entry, "sender_user_login");
            String pinnedByUsername = JSONUtil.getString(entry, "pinned_by_user_login");
            JSONObject message = (JSONObject) entry.get("message");
            String messageText = JSONUtil.getString(message, "text");
            long endsAt = JSONUtil.getDatetime(entry, "ends_at", -1);
            return new PinnedMessage(stream, msgId, broadcasterId, senderUsername, pinnedByUsername, messageText, endsAt);
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing pinned message: "+ex);
        }
        return null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PinnedMessage other = (PinnedMessage) obj;
        if (this.endsAt != other.endsAt) {
            return false;
        }
        if (!Objects.equals(this.stream, other.stream)) {
            return false;
        }
        if (!Objects.equals(this.msgId, other.msgId)) {
            return false;
        }
        return Objects.equals(this.pinnedByUsername, other.pinnedByUsername);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.stream);
        hash = 47 * hash + Objects.hashCode(this.msgId);
        hash = 47 * hash + Objects.hashCode(this.pinnedByUsername);
        hash = 47 * hash + (int) (this.endsAt ^ (this.endsAt >>> 32));
        return hash;
    }
    
}
