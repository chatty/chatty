
package chatty.util.irc;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of IRCv3 tags with Twitch-specific methods.
 * 
 * @author tduva
 */
public class MsgTags extends IrcMsgTags {
    
    public static final MsgTags EMPTY = new MsgTags(null);

    public MsgTags(Map<String, String> tags) {
        super(tags);
    }
    
    public String getId() {
        return get("id");
    }
    
    public int getBits() {
        return getInteger("bits", 0);
    }

    public String getRawEmotes() {
        return get("emotes");
    }
    
    public boolean isHighlightedMessage() {
        return isValue("msg-id", "highlighted-message");
    }
    
    public boolean isCustomReward() {
        return containsKey("custom-reward-id");
    }
    
    public String getCustomRewardId() {
        return get("custom-reward-id");
    }
    
    public boolean isFromPubSub() {
        return isValue("chatty-source", "pubsub");
    }
    
    //================
    // Factory Methods
    //================
    
    /**
     * Parse the given IRCv3 tags String (no leading @) into a IrcMsgTags object.
     * 
     * @param tags The tags String
     * @return IrcMsgTags object, empty if tags was null
     */
    public static MsgTags parse(String tags) {
        Map<String, String> parsedTags = parseTags(tags);
        if (parsedTags == null) {
            return EMPTY;
        }
        return new MsgTags(parsedTags);
    }
    
    /**
     * Create a new IrcMsgTags object with the given key/value pairs.
     * 
     * @param args Alternating key/value pairs
     * @return IrcMsgTags object
     */
    public static MsgTags create(String... args) {
        return new MsgTags(createTags(args));
    }
    
    /**
     * Merges the key/value pairs of the two MsgTags objects. If a key appears
     * in both of them, the value in the first object will be used.
     * 
     * @param a
     * @param b
     * @return 
     */
    public static MsgTags merge(MsgTags a, MsgTags b) {
        Map<String, String> result = new HashMap<>();
        b.fill(result);
        a.fill(result);
        return new MsgTags(result);
    }
    
}
