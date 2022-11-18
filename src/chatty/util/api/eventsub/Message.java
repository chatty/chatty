
package chatty.util.api.eventsub;

import chatty.util.JSONUtil;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Received message.
 * 
 * @author tduva
 */
public class Message {
    
    private static final Logger LOGGER = Logger.getLogger(Message.class.getName());
    
    public final String id;
    public final String type;
    public final String subType;
    public final String subVersion;
    public final long timestamp;
    
    /**
     * Data of the message. Can be null.
     */
    public final Payload data;
    
    public Message(String type, String id, String subType, String subVersion, long timestamp, Payload data) {
        this.type = type;
        this.id = id;
        this.data = data;
        this.subType = subType;
        this.subVersion = subVersion;
        this.timestamp = timestamp;
    }
    
    public static Message fromJson(String json, Map<String, String> userIds) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            JSONObject metadata = (JSONObject) root.get("metadata");
            JSONObject payload = (JSONObject) root.get("payload");
            
            String type = JSONUtil.getString(metadata, "message_type");
            String id = JSONUtil.getString(metadata, "message_id");
            long timestamp = JSONUtil.getDatetime(metadata, "message_timestamp", 0);
            String subType = JSONUtil.getString(metadata, "subscription_type");
            String subVersion = JSONUtil.getString(metadata, "subscription_version");
            
            Payload data = Payload.decode(payload, userIds, type, subType);
            return new Message(type, id, subType, subVersion, timestamp, data);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing EventSub message: "+ex);
            return null;
        }
    }
    
    @Override
    public String toString() {
        return type+"["+id+"/"+"/"+data+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Message other = (Message) obj;
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.type);
        hash = 61 * hash + Objects.hashCode(this.id);
        hash = 61 * hash + Objects.hashCode(this.data);
        return hash;
    }
    
}
