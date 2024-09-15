
package chatty.util.irc;

import chatty.util.StringUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of IRCv3 tags with Twitch-specific methods.
 * 
 * @author tduva
 */
public class MsgTags extends IrcMsgTags {
    
    public static final MsgTags EMPTY = new MsgTags(null, null);
    
    private Map<String, Object> objects;

    public MsgTags(Map<String, String> tags, Map<String, Object> objects) {
        super(tags);
        this.objects = objects;
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

    public boolean isHistoricMsg() {
        return hasValue("historic-timestamp");
    }

    public long getHistoricTimeStamp() {
        if (isHistoricMsg()) {
            return Long.parseLong(get("historic-timestamp"));
        } else {
            return -1;
        }
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
    
    public String getPowerUpInfo() {
        if (hasValue("msg-id")) {
            switch (get("msg-id")) {
                case "gigantified-emote-message":
                    return "Gigantified Emote";
                case "animated-message":
                    return String.format("Styled Message (%s)",
                                         StringUtil.shortenTo(get("animation-id"), 20));
            }
        }
        return null;
    }
    
    public boolean hasGigantifiedEmote() {
        return isValue("msg-id", "gigantified-emote-message");
    }
    
    public static final String IS_HIGHLIGHTED = "chatty-highlighted";
    
    public boolean isChattyHighlighted() {
        return hasValue(IS_HIGHLIGHTED);
    }
    
    //================
    // Factory Methods
    //================
    
    /**
     * Parse the given IRCv3 tags String (no leading @) into a MsgTags object.
     * 
     * @param tags The tags String
     * @return MsgTags object, empty if tags was null
     */
    public static MsgTags parse(String tags) {
        Map<String, String> parsedTags = parseTags(tags);
        if (parsedTags == null) {
            return EMPTY;
        }
        return new MsgTags(parsedTags, null);
    }
    
    /**
     * Create a new MsgTags object with the given key/value pairs.
     * 
     * @param args Alternating key/value pairs
     * @return MsgTags object
     */
    public static MsgTags create(String... args) {
        return new MsgTags(createTags(args), null);
    }
    
    public void fillObjects(Map<String, Object> map) {
        if (objects != null) {
            map.putAll(objects);
        }
    }
    
    private void addObject(String key, Object value) {
        if (objects == null) {
            objects = new HashMap<>();
        }
        objects.put(key, value);
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
        
        Map<String, Object> objectsResult = new HashMap<>();
        b.fillObjects(objectsResult);
        a.fillObjects(objectsResult);
        
        return new MsgTags(result, objectsResult);
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
        if (a != null) {
            a.fill(result);
        }
        result.put(key, value);
        
        Map<String, Object> objectsResult = new HashMap<>();
        if (a != null) {
            a.fillObjects(objectsResult);
        }
        
        return new MsgTags(result, objectsResult);
    }
    
    //=======
    // Links
    //=======
    
    @SuppressWarnings("unchecked")
    public List<Link> getLinks() {
        if (objects != null && objects.containsKey("links")) {
            return (List<Link>) objects.get("links");
        }
        return new ArrayList<>();
    }
    
    public static MsgTags createLinks(Link... input) {
        MsgTags tags = create("");
        tags.addObject("links", createLinksObject(input));
        return tags;
    }
    
    public static Object createLinksObject(Link... links) {
        List<Link> result = new ArrayList<>();
        for (Link link : links) {
            result.add(link);
        }
        return result;
    }
    
    public static class Link {
        
        public enum Type {
            JOIN, URL
        }
        
        public final Type type;
        public final String target;
        public final String label;
        public final int startIndex;
        public final int endIndex;
        
        public Link(Type type, String target, int startIndex, int endIndex) {
            this.type = type;
            this.target = target;
            this.label = "";
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        
        public Link(Type type, String target, String label) {
            this.type = type;
            this.target = target;
            this.label = label;
            this.startIndex = -1;
            this.endIndex = -1;
        }
        
        @Override
        public String toString() {
            return String.format("[%s.%s %s](%d-%d)",
                                 type, target, label, startIndex, endIndex);
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
            final Link other = (Link) obj;
            if (this.startIndex != other.startIndex) {
                return false;
            }
            if (this.endIndex != other.endIndex) {
                return false;
            }
            if (!Objects.equals(this.target, other.target)) {
                return false;
            }
            if (!Objects.equals(this.label, other.label)) {
                return false;
            }
            return this.type == other.type;
        }
        
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.type);
            hash = 79 * hash + Objects.hashCode(this.target);
            hash = 79 * hash + Objects.hashCode(this.label);
            hash = 79 * hash + this.startIndex;
            hash = 79 * hash + this.endIndex;
            return hash;
        }
        
    }
    
}
