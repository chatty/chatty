
package chatty.util;

import chatty.User;
import java.util.HashMap;
import java.util.Map;

/**
 * Store users most recently affected by a command or their User Dialog being
 * open, per channel.
 * 
 * @author tduva
 */
public class RecentlyAffectedUsers {
    
    private static final Map<String, UniqueLimitedRingBuffer<User>> users = new HashMap<>();
    
    /**
     * Add a user, removing the user if already present, effectively moving it
     * to the end (most recent) of the list.
     * 
     * @param user 
     */
    public synchronized static void addUser(User user) {
        if (!users.containsKey(user.getChannel())) {
            users.put(user.getChannel(), new UniqueLimitedRingBuffer<>(10));
        }
        users.get(user.getChannel()).append(user);
    }
    
    /**
     * Get and remove the most recent user for this channel.
     * 
     * @param channel
     * @return The {@code User} or {@code null} if none is present
     */
    public synchronized static User poll(String channel) {
        if (users.containsKey(channel)) {
            return users.get(channel).pollLast();
        }
        return null;
    }
    
}
