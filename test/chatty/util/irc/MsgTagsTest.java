
package chatty.util.irc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class MsgTagsTest {
    
    @Test
    public void test() {
        MsgTags emptyTags1 = MsgTags.EMPTY;
        assertEquals(emptyTags1.isEmpty(), true);
        assertEquals(emptyTags1.get("abc"), null);
        
        MsgTags emptyTags2 = MsgTags.parse(null);
        assertEquals(emptyTags2.isEmpty(), true);
        
        MsgTags emptyTags3 = MsgTags.parse("");
        assertEquals(emptyTags3.isEmpty(), true);
        
        MsgTags tags1 = MsgTags.parse("abc");
        assertEquals(tags1.isEmpty(), false);
        assertEquals(tags1.get("abc"), null);
        assertEquals(tags1.isTrue("abc"), false);
        
        MsgTags tags2 = MsgTags.parse("key=value");
        assertEquals(tags2.isEmpty(), false);
        assertEquals(tags2.get("key"), "value");
        assertEquals(tags2.isTrue("key"), false);
        assertEquals(tags2.getInteger("key", -1), -1);
        assertEquals(tags2.getInteger("abc", -1), -1);
        
        MsgTags tags3 = MsgTags.parse("badges=turbo/1;color=#0000FF;display-name=tduva;emote-sets=0,33,130,19194,19655;mod=0;subscriber=0;user-type=");
        assertEquals(tags3.isEmpty(), false);
        assertEquals(tags3.get("display-name"), "tduva");
        assertEquals(tags3.get("user-type"), "");
        assertEquals(tags3.get("user-type", null), "");
        
        MsgTags tags4 = MsgTags.parse("ban-duration=1;ban-reason=test\\smessage");
        assertFalse(tags4.isEmpty());
        assertEquals(tags4.get("ban-reason"), "test message");
        assertEquals(tags4.getLong("ban-duration", -1), 1);
    }
    
    @Test
    public void testToTagsString() {
        MsgTags tags1 = MsgTags.parse("ban-duration=1;ban-reason=test\\smessage\\:\\stest\\\\");
        String tags1String = tags1.toTagsString();
        MsgTags tags1Reparsed = MsgTags.parse(tags1String);
        System.out.println("1-O: "+tags1);
        System.out.println("1-I: "+tags1String);
        System.out.println("1-R: "+tags1Reparsed);
        assertFalse(tags1String.contains(" "));
        assertEquals(tags1, tags1Reparsed);
    }
    
}
