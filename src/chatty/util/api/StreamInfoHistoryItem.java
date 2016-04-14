
package chatty.util.api;

/**
 * A single datapoint in the stream info history, holding information about
 * stream status like viewers, title, game etc.
 * 
 * @author tduva
 */
public class StreamInfoHistoryItem {
    
    private final boolean online;
    private final int viewers;
    private final String status;
    private final String game;
    private final String statusAndGame;
    private final long time;
    private final String title;
    
    public StreamInfoHistoryItem() {
        this.viewers = -1;
        this.status = null;
        this.game = null;
        this.online = false;
        this.statusAndGame = null;
        this.time = System.currentTimeMillis();
        this.title = "Stream offline";
    }
    
    public StreamInfoHistoryItem(int viewers, String status, String game) {
        this.viewers = viewers;
        this.status = status;
        this.game = game;
        this.online = true;
        this.statusAndGame = status+game;
        this.time = System.currentTimeMillis();
        if (status == null) {
            title = "No stream title set";
        } else {
            title = status;
        }
    }
    
    public int getViewers() {
        return viewers;
    }
    
    /**
     * Gets the status of the stream. Can be null.
     * 
     * @return 
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Gets the title of the stream, which is the same as the status, but it
     * never is null.
     * 
     * @return The status of the stream, or an appropriate text if null.
     */
    public String getTitle() {
        return title;
    }
    
    public String getGame() {
        return game;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public String getStatusAndGame() {
        return statusAndGame;
    }
    
    public long getTime() {
        return time;
    }
    
}
