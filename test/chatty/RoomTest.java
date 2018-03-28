
package chatty;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class RoomTest {
    
    @Test
    public void testCreateRegular() {
        testFields(Room.createRegular("#channel"),
                "#channel",
                "#channel",
                "channel",
                true,
                "#channel",
                "#channel",
                "#channel",
                null,
                null);
        
        testFields(Room.createRegular("$name"),
                "$name",
                "$name",
                null,
                true,
                "$name",
                "$name",
                "$name",
                null,
                null);
        
        testFields(Room.createRegular("*name"),
                "*name",
                "*name",
                null,
                true,
                "*name",
                "*name",
                "_name",
                null,
                null);
        
        testFields(Room.createRegular("#chatrooms:7236692:832eaedf-6b94-4ba3-8c63-a0d854a0237b"),
                "#chatrooms:7236692:832eaedf-6b94-4ba3-8c63-a0d854a0237b",
                null,
                null,
                false,
                "#chatrooms:7236692:832eaedf-6b94-4ba3-8c63-a0d854a0237b",
                "#chatrooms:7236692:832eaedf-6b94-4ba3-8c63-a0d854a0237b",
                "#chatrooms_7236692_832eaedf-6b94-4ba3-8c63-a0d854a0237b",
                null,
                null);
    }
    
    @Test
    public void testCreateFromChannel() {
        testFields(Room.createFromChannel("#chatrooms:7236692:832eaedf-6b94-4ba3-8c63-a0d854a0237b", "spoilers", "#tduva"),
                "#chatrooms:7236692:832eaedf-6b94-4ba3-8c63-a0d854a0237b",
                "#tduva",
                "tduva",
                false,
                "spoilers",
                "#tduva (spoilers)",
                "#tduva-spoilers",
                null,
                "7236692");
    }
    
    @Test
    public void testCreateFromId() {
        testFields(Room.createFromId("832eaedf-6b94-4ba3-8c63-a0d854a0237b", "spoilers", "7236692", "#tduva", "Talk about spoilers here"),
                "#chatrooms:7236692:832eaedf-6b94-4ba3-8c63-a0d854a0237b",
                "#tduva",
                "tduva",
                false,
                "spoilers",
                "#tduva (spoilers)",
                "#tduva-spoilers",
                "Talk about spoilers here",
                "7236692");
    }
    
    private void testFields(Room r, String channel, String ownerChannel,
            String stream, boolean isOwner, String name, String displayName,
            String fileName, String topic, String streamId) {
        assertEquals(r.getChannel(), channel);
        assertEquals(r.getOwnerChannel(), ownerChannel);
        assertEquals(r.getStream(), stream);
        assertEquals(r.isOwner(), isOwner);
        assertEquals(r.isChatroom(), !isOwner); // Currently !isOwner only applies to chatrooms
        assertEquals(r.getName(), name);
        assertEquals(r.getDisplayName(), displayName);
        assertEquals(r.getFilename(), fileName);
        assertEquals(r.getTopic(), topic);
        assertEquals(r.getStreamId(), streamId);
    }
    
}
