
package chatty;

import chatty.lang.Language;
import chatty.util.StringUtil;
import java.util.Locale;
import java.util.Objects;

/**
 *
 * @author tduva
 */
public class Room {
    
    public static final Room EMPTY = Room.createRegular("");
    
    private final String channel;
    private final String ownerChannel;
    private final String name;
    private final String customName;
    private final String id;
    private final String topic;
    private final String displayName;
    private final String fileName;
    private final boolean isOwner;
    private final boolean isChatroom;
    private final String streamId;
    private final String stream;
    
    public static Room createRegular(String channel) {
        return new Room(channel, null, channel, null, null, null, null);
    }
    
    public static Room createRegularWithId(String channel, String ownerId) {
        return new Room(channel, null, channel, ownerId, null, null, null);
    }
    
    public static Room createFromId(String id, String name, String ownerId, String ownerChannel, String topic) {
        String channel = "#chatrooms:"+ownerId+":"+id;
        return new Room(channel, id, name, ownerId, ownerChannel, null, topic);
    }
    
    public static Room createFromChannel(String channel, String name, String ownerChannel) {
        try {
            String[] split = channel.split(":");
            String ownerId = split[1];
            String id = split[2];
            return new Room(channel, id, name, ownerId, ownerChannel, null, null);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    private Room(String channel, String id, String name, String ownerId,
            String ownerChannel, String customName, String topic) {
        //System.out.println(channel);
        channel = StringUtil.toLowerCase(channel);
        this.isOwner = !channel.startsWith("#chatrooms:");
        this.isChatroom = channel.startsWith("#chatrooms:");
        this.channel = channel;
        this.topic = topic;
        this.name = name;
        
        if (ownerChannel != null) {
            this.ownerChannel = ownerChannel;
        } else if (isOwner) {
            this.ownerChannel = channel;
        } else {
            this.ownerChannel = null;
        }
        this.customName = customName;
        this.id = id;
        this.streamId = ownerId;
        
        // Only set stream if normal channel, and depending on whether it's
        // a chatroom that belongs to another channel
        if (channel.startsWith("#")) {
            if (isOwner) {
                stream = Helper.toStream(channel);
            } else {
                stream = Helper.toStream(ownerChannel);
            }
        } else {
            stream = null;
        }
        
        // Set displayName based on what info is available, fileName is
        // basicially a display name as well, but filename-safe
        if (channel.isEmpty()) {
            this.displayName = "";
            this.fileName = "";
        } else if (!isOwner && name != null && ownerChannel != null) {
            this.displayName = ownerChannel+" ("+name+")";
            this.fileName = ownerChannel+"-"+Helper.encodeFilename2(name);
        } else {
            this.displayName = name;
            this.fileName = Helper.encodeFilename2(channel);
        }
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Name purely for display. Might identical to the channel, but doesn't
     * have to be.
     * 
     * @return 
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * The name of the IRC channel.
     * 
     * @return 
     */
    public String getChannel() {
        return channel;
    }
    
    /**
     * The stream this Room belongs to.
     * 
     * @return The name of the stream, or null if no stream is associated
     */
    public String getStream() {
        return stream;
    }
    
    public boolean hasStream() {
        return stream != null;
    }
    
    public String getStreamId() {
        return streamId;
    }
    
    public boolean isOwner() {
        return isOwner;
    }
    
    /**
     * This will always be the channel name itself (e.g. #channel or $username),
     * except in the case for chatrooms, for which it is either the #channel
     * they belong to (if known) or null.
     * 
     * @return The channel or null (if chatroom and owner is not known)
     */
    public String getOwnerChannel() {
        return ownerChannel;
    }
    
    /**
     * More convenient way to check if getOwnerChannel() will return a non-null
     * value.
     * 
     * @return true if getOwnerChannel() will return non-null, false otherwise
     */
    public boolean hasOwnerChannel() {
        return ownerChannel != null;
    }
    
    public boolean isChatroom() {
        return isChatroom;
    }
    
    public String getFilename() {
        return fileName;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public boolean hasTopic() {
        return topic != null && !topic.isEmpty();
    }
    
    public String getTopicText() {
        return "["+Language.getString("chat.topic")+"] "+topic;
    }
    
    public boolean sameChannel(Room other) {
        return channel.equals(other.channel);
    }
    
    @Override
    public String toString() {
        return displayName;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Room other = (Room) obj;
        if (!Objects.equals(this.channel, other.channel)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.channel);
        return hash;
    }

}
