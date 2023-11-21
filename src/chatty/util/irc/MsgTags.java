
package chatty.util.irc;

import chatty.util.StringUtil;
import java.math.BigDecimal;
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
    
    public boolean hasId() {
        return hasValue("id");
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
        return hasValue("custom-reward-id");
    }
    
    public String getCustomRewardId() {
        return get("custom-reward-id");
    }
    
    public boolean isFromPubSub() {
        return isValue("chatty-source", "pubsub");
    }
    
    public String getChannelJoin() {
        return get("chatty-channel-join");
    }
    
    public String getChannelJoinIndices() {
        return get("chatty-channel-join-indices");
    }
    
    public boolean isRestrictedMessage() {
        return isValue("chatty-is-restricted", "1");
    }
    
    public boolean hasReplyUserMsg() {
        return hasValue("reply-parent-msg-body") && hasValue("reply-parent-display-name");
    }
    
    public String getReplyUserMsg() {
        if (hasReplyUserMsg()) {
            return String.format("<%s> %s",
                    get("reply-parent-display-name"),
                    get("reply-parent-msg-body"));
        }
        return null;
    }
    
    public boolean isReply() {
        return hasValue("reply-parent-msg-id");
    }
    
    public String getReplyParentMsgId() {
        return get("reply-parent-msg-id");
    }
    
    public String getHypeChatAmountText() {
        int amount = getInteger("pinned-chat-paid-amount", -1);
        String currency = get("pinned-chat-paid-currency");
        int exponent = getInteger("pinned-chat-paid-exponent", -1);
        if (amount > 0 && !StringUtil.isNullOrEmpty(currency) && exponent != -1) {
            return String.format("%s %s",
                    currency,
                    new BigDecimal(amount).scaleByPowerOfTen(-exponent));
        }
        return null;
    }
    
    public String getHypeChatInfo() {
        return String.format("Level %s Hype Chat for %s",
                get("pinned-chat-paid-level"),
                getHypeChatAmountText());
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
    
    /**
     * Creates a new MsgTags object with the given key/value pair added. If a key with the given
     * name already exists, it's value is overwritten.
     * 
     * @param a The original MsgTags object
     * @param key The key to be added
     * @param value The value to be added
     * @return A new MsgTags object
     */
    public static MsgTags addTag(MsgTags a, String key, String value) {
        Map<String, String> result = new HashMap<>();
        a.fill(result);
        result.put(key, value);
        return new MsgTags(result);
    }
    
}
