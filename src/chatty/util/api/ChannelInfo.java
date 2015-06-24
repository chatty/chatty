
package chatty.util.api;

/**
 * Holds info about the channel (title, game). This is different to stream info
 * because this isn't associated with a stream (so it doesn't have viewercount)
 * and used differently.
 * 
 * @author tduva
 */
public class ChannelInfo {
    
    private final String status;
    private final String game;
    
    public ChannelInfo(String status, String game) {
        this.status = status;
        this.game = game;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getGame() {
        return game;
    }
    
}
