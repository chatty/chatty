
package chatty.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class ProcTest {
    
    @Test
    public void testSplit() {
        assertArrayEquals(Proc.split(""), new String[]{});
        assertArrayEquals(Proc.split("a"), new String[]{"a"});
        assertArrayEquals(Proc.split("\\a"), new String[]{"\\a"});
        assertArrayEquals(Proc.split("\"a"), new String[]{"a"});
        assertArrayEquals(Proc.split("\"a b cd\""), new String[]{"a b cd"});
        assertArrayEquals(Proc.split("a b cd"), new String[]{"a", "b", "cd"});
        assertArrayEquals(Proc.split("\"a \\\"b\\\" cd\""), new String[]{"a \"b\" cd"});
        assertArrayEquals(Proc.split("\"\""), new String[]{});
        assertArrayEquals(Proc.split("\" \""), new String[]{" "});
        assertArrayEquals(Proc.split("\"a \\b\\ c\""), new String[]{"a \\b\\ c"});
        assertArrayEquals(Proc.split("\\\"a\\\" b"), new String[]{"\"a\"","b"});
        assertArrayEquals(Proc.split("\\\"a b\\\""), new String[]{"\"a","b\""});
    }
    
}
