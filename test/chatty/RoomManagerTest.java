
package chatty;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class RoomManagerTest {
    
    @Test
    public void testAddRoomsIfNone() {
        RoomManager m = new RoomManager(null, u -> {});
        Room r1 = Room.createRegular("#channel");
        Room r2 = Room.createRegular("#channel");
        m.addRoomsIfNone(Arrays.asList(r1));
        assertTrue(m.getRoom("#channel") == r1);
        m.addRoomsIfNone(Arrays.asList(r2));
        assertTrue(m.getRoom("#channel") == r1);
        m.addRoom(r2);
        assertTrue(m.getRoom("#channel") != r1);
        assertTrue(m.getRoom("#channel") == r2);
        
        RoomManager m2 = new RoomManager(null, u -> {});
        m2.getRoom("#channel");
        m2.addRoomsIfNone(Arrays.asList(r2));
        assertTrue(m2.getRoom("#channel") != r2);
    }
    
    @Test
    public void testOwnerChannel() {
        RoomManager m = new RoomManager(null, u -> {});
        Room r1 = Room.createRegular("#channel");
        m.addRoom(r1);
        assertTrue(m.getRoomsByOwner("#channel").iterator().next() == r1);
        Room r2 = Room.createFromChannel("#chatrooms:7236692:832eaedf-6b94-4ba3-8c63-a0d854a0237b", "spoilers", "#channel");
        m.addRoom(r2);
        assertTrue(m.getRoomsByOwner("#channel").size() == 2);
        assertTrue(m.getRoomsByOwner("#otherchannel").isEmpty());
        m.addRoom(Room.createRegular("$name"));
        assertTrue(m.getRoomsByOwner("$name").size() == 1);
    }
    
}
