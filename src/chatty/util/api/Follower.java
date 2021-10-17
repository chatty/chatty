
package chatty.util.api;

/**
 *
 * @author tduva
 */
public class Follower {
    
    public enum Type { FOLLOWER, SUBSCRIBER }
    
    public final Type type;
    
    /**
     * The name of the follower (display name, so it might not be all
     * lowercase).
     */
    public final String name;
    
    public final String display_name;
    
    /**
     * The time the user followed at.
     */
    public final long follow_time;
    
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
    
    public final long created_time = System.currentTimeMillis();
    
    public final long user_created_time;
    
    public final String info;
    
    public final String verboseInfo;
    
    /**
     * Creates a new Follower item.
     * 
     * @param name The name of the follower
     * @param time The time the API returned as follow date (timestamp)
     * @param refollow Whether it was detected as a refollow
     * @param newFollower Whether it is a new follower in this request
     */
    public Follower(Type type, String name, String display_name, long time, long userTime, boolean refollow, boolean newFollower, String info, String verboseInfo) {
        this.type = type;
        this.name = name;
        this.display_name = display_name;
        this.follow_time = time;
        this.user_created_time = userTime;
        this.refollow = refollow;
        this.newFollower = newFollower;
        this.info = info;
        this.verboseInfo = verboseInfo;
    }
    
    public Follower setAccountCreationTime(long time) {
        return new Follower(type, name, display_name, follow_time, time, refollow, newFollower, info, verboseInfo);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
