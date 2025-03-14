
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
        assertNull(b.getVersion("subscriber"));
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
        assertNull(b.getVersion("subscriber"));
        assertEquals("1", b.getVersion("moderator"));
        assertEquals("moderator", b.getId(0));
        assertEquals("1", b.getVersion(0));
        assertEquals("partner", b.getId(1));
        assertEquals("2", b.getVersion(1));
        assertEquals(b.toString(), "moderator/1,partner/2");

        IrcBadges b2 = IrcBadges.parse("subscriber/24");
        assertFalse(b2.isEmpty());
        assertEquals(1, b2.size());
        assertEquals("subscriber", b2.getId(0));
        assertEquals("24", b2.getVersion(0));
        assertEquals(b2.toString(), "subscriber/24");

        IrcBadges b3 = IrcBadges.parse("predictions/A/B,subscriber/1");
        assertEquals("1", b3.getVersion("subscriber"));
        assertEquals("A/B", b3.getVersion("predictions"));
        assertEquals(2, b3.size());
        assertEquals(b3.toString(), "predictions/A/B,subscriber/1");

        IrcBadges b4 = IrcBadges.parse("123/ABC,,,");
        assertEquals("ABC", b4.getVersion("123"));
        assertEquals(1, b4.size());
        assertEquals(b4.toString(), "123/ABC");
    }
    
}
