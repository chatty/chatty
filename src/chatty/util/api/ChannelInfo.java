
package chatty.util.api;

import chatty.util.StringUtil;

/**
 * Holds info about the channel (title, game). This is different to stream info
 * because this isn't associated with a stream (so it doesn't have viewercount)
 * and used differently.
 * 
 * @author tduva
 */
public class ChannelInfo {
    
    public final String id;
    public final long time;
    public final String name;
    public final long createdAt;
    public final int followers;
    public final int views;
    public final long updatedAt;
    public final String broadcaster_type;
    public final String description;
    
    public ChannelInfo(String name, String id, long createdAt,
            int followers, int views, long updatedAt, String broadcaster_type,
            String description) {
        this.createdAt = createdAt;
        this.views = views;
        this.followers = followers;
        this.name = StringUtil.toLowerCase(name);
        this.id = id;
        this.time = System.currentTimeMillis();
        this.updatedAt = updatedAt;
        this.broadcaster_type = broadcaster_type;
        this.description = description;
    }
    
    @Override
    public String toString() {
        return name+"/"+id+"/"+createdAt+"/"+followers+"/"+views;
    }
    
}
