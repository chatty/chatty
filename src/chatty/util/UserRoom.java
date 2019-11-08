
package chatty.util;

import chatty.Room;
import chatty.User;
import java.util.Objects;

/**
 * A room, with an optional user attached.
 * 
 * @author tduva
 */
public class UserRoom implements Comparable<UserRoom> {

    /**
     * The room, required.
     */
    public final Room room;
    
    /**
     * May be null, only if user already exists.
     */
    public final User user;

    public UserRoom(Room room, User user) {
        this.room = room;
        this.user = user;
    }

    @Override
    public int compareTo(UserRoom o) {
        if (Objects.equals(this, o)) {
            return 0;
        }
        if (o == null || o.room.getChannel() == null) {
            return -1;
        }
        return -o.room.getChannel().compareTo(this.room.getChannel());
    }
        
}
