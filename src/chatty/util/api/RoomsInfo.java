
package chatty.util.api;

import chatty.Room;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class RoomsInfo {
    
    public final String stream;
    public final Set<Room> rooms;
    
    public RoomsInfo(String stream, Set<Room> rooms) {
        this.stream = stream;
        this.rooms = rooms;
    }
    
    @Override
    public String toString() {
        return stream+":"+rooms;
    }
    
    public String makeInfo() {
        if (rooms != null) {
            if (rooms.isEmpty()) {
                return "[ChatRooms] No rooms available";
            } else {
                return "[ChatRooms] "+rooms.size() + " rooms available to join for "+stream+" ('Channels'-Menu)";
            }
        } else {
            return "[ChatRooms] Error loading rooms";
        }
    }
    
}
