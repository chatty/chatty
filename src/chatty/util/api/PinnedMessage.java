
package chatty.util.api;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import java.util.Objects;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * The message pinned to the chat by a moderator.
 * <br><br>
 * There are generally the following states related to this object:
 * <ul>
 * <li>A {@code null} stored in {@link chatty.ChannelState} means no request
 * result yet</li>
 * <li>If an object is present (requests always return one):
 * <ul>
 * <li>If {@link isEmpty()} is {@code true} then the request was successful, but
 * there is no pinned message.</li>
 * <li>If {@link hasError()} is {@code true} then request failed in some way,
 * with the {@code error} field containing the reason.</li>
 * <li>Otherwise the data fields are populated with information on the pinned
 * message.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author tduva
 */
public class PinnedMessage {
    
    public static final PinnedMessage EMPTY = new PinnedMessage(null);
    
    private static final Logger LOGGER = Logger.getLogger(PinnedMessage.class.getName());

    public final String stream;
    public final String msgId;
    public final String broadcasterId;
    public final String senderUsername;
    public final String pinnedByUsername;
    public final String messageText;
    public final long endsAt;
    public final String error;
    
    public PinnedMessage(String stream, String msgId, String broadcasterId, String senderUsername, String pinnedByUsername, String messageText, long endsAt) {
        this.stream = stream;
        this.msgId = msgId;
        this.broadcasterId = broadcasterId;
        this.senderUsername = senderUsername;
        this.pinnedByUsername = pinnedByUsername;
        this.messageText = messageText;
        this.endsAt = endsAt;
        this.error = null;
    }
    
    public PinnedMessage(String error) {
        this.stream = null;
        this.msgId = null;
        this.broadcasterId = null;
        this.senderUsername = null;
        this.pinnedByUsername = null;
        this.messageText = null;
        this.endsAt = -1;
        this.error = error;
    }
    
    /**
     * No message was found from a successful API request.
     * 
     * @return 
     */
    public boolean isEmpty() {
        return error == null
                && StringUtil.isNullOrEmpty(stream, msgId, broadcasterId, senderUsername, pinnedByUsername, messageText);
    }
    
    /**
     * The API request was not successful, with the reason given in
     * {@code reason}.
     *
     * @return 
     */
    public boolean hasError() {
        return error != null;
    }
    
    public static PinnedMessage parse(String stream, String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray data = (JSONArray) root.get("data");
            if (data.isEmpty()) {
                return PinnedMessage.EMPTY;
            }
            JSONObject entry = (JSONObject) data.iterator().next();
            String msgId = JSONUtil.getString(entry, "message_id");
            String broadcasterId = JSONUtil.getString(entry, "broadcaster_id");
            String senderUsername = JSONUtil.getString(entry, "sender_user_login");
            String pinnedByUsername = JSONUtil.getString(entry, "pinned_by_user_login");
            JSONObject message = (JSONObject) entry.get("message");
            String messageText = JSONUtil.getString(message, "text");
            long endsAt = JSONUtil.getDatetime(entry, "ends_at", -1);
            if (!StringUtil.isNullOrEmpty(msgId, broadcasterId, senderUsername, pinnedByUsername, messageText)) {
                return new PinnedMessage(stream, msgId, broadcasterId, senderUsername, pinnedByUsername, messageText, endsAt);
            }
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing pinned message: "+ex);
        }
        return new PinnedMessage("Parse error");
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
        if (!Objects.equals(this.pinnedByUsername, other.pinnedByUsername)) {
            return false;
        }
        return Objects.equals(this.error, other.error);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(this.stream);
        hash = 13 * hash + Objects.hashCode(this.msgId);
        hash = 13 * hash + Objects.hashCode(this.pinnedByUsername);
        hash = 13 * hash + (int) (this.endsAt ^ (this.endsAt >>> 32));
        hash = 13 * hash + Objects.hashCode(this.error);
        return hash;
    }
    
}
