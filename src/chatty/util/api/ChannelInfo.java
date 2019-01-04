
package chatty.util.api;

import chatty.util.StringUtil;
import chatty.util.api.CommunitiesManager.Community;

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
    public final String status;
    public final String game;
    public final long updatedAt;
    
    public ChannelInfo(String name, String status, String game) {
        this(name, null, status, game, -1, -1, -1, -1);
    }
    
    public ChannelInfo(String name, String id, String status, String game, long createdAt,
            int followers, int views, long updatedAt) {
        this.status = status;
        this.game = game;
        this.createdAt = createdAt;
        this.views = views;
        this.followers = followers;
        this.name = StringUtil.toLowerCase(name);
        this.id = id;
        this.time = System.currentTimeMillis();
        this.updatedAt = updatedAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getGame() {
        return game;
    }
    
    @Override
    public String toString() {
        return name+"/"+id+"/"+status+"/"+game+"/"+createdAt+"/"+followers+"/"+views;
    }
    
}
