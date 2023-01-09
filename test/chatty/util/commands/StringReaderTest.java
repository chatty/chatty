
package chatty.util.commands;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class StringReaderTest {

    @Test
    public void test() {
        StringReader reader = new StringReader("input");
        assertEquals("in", reader.peek(2));
        assertEquals("", reader.peek(0));
        assertEquals("in", reader.peek(2));
        assertTrue(reader.accept("in"));
        assertEquals("p", reader.peek(1));
        assertEquals("n", reader.last());
        assertTrue(reader.accept("p"));
        assertEquals("u", reader.peek(1));
        assertEquals("ut", reader.peek(2));
        assertEquals("", reader.peek(3));
        assertEquals("u", reader.next());
        assertEquals("t", reader.peek(1));
        assertEquals("", reader.peek(2));
        assertEquals("", reader.peek(3));
    }
    
}
