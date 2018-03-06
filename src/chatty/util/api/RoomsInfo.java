
package chatty.util.api;

import chatty.Room;
import chatty.lang.Language;
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
                return "[ChatRooms] "+Language.getString("chat.rooms.none", stream);
            } else {
                return "[ChatRooms] "+Language.getString("chat.rooms.available", rooms.size(), stream);
            }
        } else {
            return "[ChatRooms] "+Language.getString("chat.rooms.error");
        }
    }
    
}
