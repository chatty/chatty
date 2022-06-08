
package chatty.util.irc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class IrcBadgesTest {

    @Test
    public void testEmpty() {
        assertEmpty(IrcBadges.parse(null));
        assertEmpty(IrcBadges.parse(""));
        assertEmpty(IrcBadges.parse(" "));
        assertEmpty(IrcBadges.parse(" , "));
    }
    
    private static void assertEmpty(IrcBadges b) {
        assertTrue(b.isEmpty());
        assertEquals(0, b.size());
        assertFalse(b.hasId("subscriber"));
        assertFalse(b.hasIdVersion("subscriber", "1"));
        assertNull(b.get("subscriber"));
    }
    
    @Test
    public void test() {
        IrcBadges b = IrcBadges.parse("moderator/1,partner/2");
        assertFalse(b.isEmpty());
        assertEquals(2, b.size());
        assertTrue(b.hasId("moderator"));
        assertTrue(b.hasId("partner"));
        assertFalse(b.hasId("1"));
        assertTrue(b.hasIdVersion("moderator", "1"));
        assertTrue(b.hasIdVersion("partner", "2"));
        assertFalse(b.hasIdVersion("subscriber", "1"));
        assertNull(b.get("subscriber"));
        assertEquals("1", b.get("moderator"));
        assertEquals("moderator", b.getId(0));
        assertEquals("1", b.getVersion(0));
        assertEquals("partner", b.getId(1));
        assertEquals("2", b.getVersion(1));
        List<String> list = new ArrayList<>();
        b.forEach((id, version) -> {
            list.add(id);
            list.add(version);
        });
        assertEquals(Arrays.asList(new String[]{"moderator", "1", "partner", "2"}), list);
        
        IrcBadges b2 = IrcBadges.parse("subscriber/24");
        assertFalse(b2.isEmpty());
        assertEquals(1, b2.size());
        assertEquals("subscriber", b2.getId(0));
        assertEquals("24", b2.getVersion(0));
    }
    
}
