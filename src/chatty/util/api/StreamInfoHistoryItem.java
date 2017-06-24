
package chatty.util.api;

import chatty.util.api.CommunitiesManager.Community;
import chatty.util.api.StreamInfo.StreamType;

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
    private final StreamType streamType;
    private final Community community;
    private final long streamDuration;
    private final long streamDurationWithPicnic;
    
    public StreamInfoHistoryItem(long time) {
        this.viewers = -1;
        this.status = null;
        this.game = null;
        this.online = false;
        this.statusAndGame = null;
        this.time = time;
        this.title = "Stream offline";
        this.streamType = null;
        this.community = null;
        this.streamDuration = -1;
        this.streamDurationWithPicnic = -1;
    }
    
    public StreamInfoHistoryItem(long time, int viewers, String status, String game,
            StreamType streamType, Community community, long startedTime,
            long startedTimeWithPicnic) {
        this.viewers = viewers;
        this.status = status;
        this.game = game;
        this.online = true;
        this.statusAndGame = status+game;
        this.time = time;
        if (status == null) {
            title = "No stream title set";
        } else {
            title = status;
        }
        this.streamType = streamType;
        this.community = community;
        this.streamDuration = startedTime;
        this.streamDurationWithPicnic = startedTimeWithPicnic;
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
    
    public Community getCommunity() {
        return community;
    }
    
    public StreamType getStreamType() {
        return streamType;
    }
    
    public long getStreamDuration() {
        return streamDuration;
    }
    
    public long getStreamDurationWithPicnic() {
        return streamDurationWithPicnic;
    }
    
}
