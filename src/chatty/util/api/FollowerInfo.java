
package chatty.util.api;

import java.util.List;

/**
 * Follower Info holding a list of followers and when it was created. This is
 * mostly immutable, however the list of followers is not and should not be
 * modified.
 * 
 * @author tduva
 */
public class FollowerInfo {
    
    /**
     * The list of followers (not necessarily all followers).
     */
    public final List<Follower> followers;
    
    /**
     * How many followers there are in total (can be more than the list of
     * followers).
     */
    public final int total;
    
    /**
     * The time this object was created.
     */
    public final long time;
    
    /**
     * The stream these followers belong to.
     */
    public final String stream;
    
    /**
     * Whether an request error occured.
     */
    public final boolean requestError;
    
    /**
     * A short description of what the request error was.
     */
    public final String requestErrorDescription;
    
    /**
     * Creates a new follower info object.
     * 
     * @param stream
     * @param followers
     * @param total 
     */
    public FollowerInfo(String stream, List<Follower> followers, int total) {
        this.followers = followers;
        this.total = total;
        this.time = System.currentTimeMillis();
        this.stream = stream;
        this.requestError = false;
        this.requestErrorDescription = null;
    }
    
    /**
     * Creates a new follower info object for when an error occured.
     * 
     * @param stream The name of the stream
     * @param error The error message
     */
    public FollowerInfo(String stream, String error) {
        this.followers = null;
        this.total = -1;
        this.time = System.currentTimeMillis();
        this.stream = stream;
        this.requestError = true;
        this.requestErrorDescription = error;
    }
    
    @Override
    public String toString() {
        return total+" "+followers;
    }
    
}
