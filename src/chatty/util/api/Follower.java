
package chatty.util.api;

/**
 *
 * @author tduva
 */
public class Follower {
    
    /**
     * The name of the follower (display name, so it might not be all
     * lowercase).
     */
    public final String name;
    
    /**
     * The time the user followed at.
     */
    public final long time;
    
    /**
     * Whether the user was already followed before with a different follow-time
     * (during this Chatty session, and of course if data was requested at that
     * point).
     */
    public final boolean refollow;
    
    /**
     * Whether it is a new follower for this request (during this Chatty
     * session).
     */
    public final boolean newFollower;
    
    /**
     * Creates a new Follower item.
     * 
     * @param name The name of the follower
     * @param time The time the API returned as follow date (timestamp)
     * @param refollow Whether it was detected as a refollow
     * @param newFollower Whether it is a new follower in this request
     */
    public Follower(String name, long time, boolean refollow, boolean newFollower) {
        this.name = name;
        this.time = time;
        this.refollow = refollow;
        this.newFollower = newFollower;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
