
package chatty.util.api.pubsub;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Received message.
 * 
 * @author tduva
 */
public class Message {
    
    private static final Logger LOGGER = Logger.getLogger(Message.class.getName());
    
    /**
     * Basic type of the message. Should never be null.
     */
    public final String type;
    
    /**
     * Message identifier. Can be null.
     */
    public final String nonce;
    
    /**
     * Data of the message. Can be null.
     */
    public final MessageData data;
    
    /**
     * Attached error message. Can be null.
     */
    public final String error;
    
    public Message(String type, String nonce, MessageData data, String error) {
        this.type = type;
        this.nonce = nonce;
        this.data = data;
        this.error = error;
    }
    
    public static Message fromJson(String json, Map<String, String> userIds) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            String type = (String)root.get("type");
            if (type == null) {
                LOGGER.warning("PubSub message type null");
                return null;
            }
            
            String nonce = (String)root.get("nonce");
            String error = (String)root.get("error");
            
            MessageData data = MessageData.decode((JSONObject)root.get("data"), userIds);
            return new Message(type, nonce, data, error);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing PubSub message: "+ex);
            return null;
        }
    }
    
    @Override
    public String toString() {
        return type+"["+nonce+"/"+error+"/"+data+"]";
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
        if (!Objects.equals(this.nonce, other.nonce)) {
            return false;
        }
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        if (!Objects.equals(this.error, other.error)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.type);
        hash = 61 * hash + Objects.hashCode(this.nonce);
        hash = 61 * hash + Objects.hashCode(this.data);
        hash = 61 * hash + Objects.hashCode(this.error);
        return hash;
    }
    
}
