
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
        assertArrayEquals(Proc.split("\"a \\b\\ c\""), new String[]{"a \\b\\ c"});              // "a \b\ c" -> a \b\ c
        assertArrayEquals(Proc.split("\\\"a\\\" b"), new String[]{"\"a\"","b"});                // \"a\" b -> "a",b
        assertArrayEquals(Proc.split("\\\"a b\\\""), new String[]{"\"a","b\""});                // \"a b\" -> "a,b"
        assertArrayEquals(Proc.split("\"1 2 3\\\""), new String[]{"1 2 3\\"});                  // "1 2 3\" -> 1 2 3\
        assertArrayEquals(Proc.split("\"1 2 3\\\\\""), new String[]{"1 2 3\\\\"});              // "1 2 3\\" -> 1 2 3\\
        assertArrayEquals(Proc.split("\"1 2 3\\\" abc"), new String[]{"1 2 3\\", "abc"});       // "1 2 3\" abc -> 1 2 3\,abc (last quote is used as closing quote)
        assertArrayEquals(Proc.split("\"1 2 3\\\" abc\""), new String[]{"1 2 3\" abc"});        // "1 2 3\" abc" -> 1 2 3" abc
        assertArrayEquals(Proc.split("\"1 2 3\\\" a \"b c\""), new String[]{"1 2 3\" a ","b","c"}); // "1 2 3\" a "b c" -> 1 2 3" a ,b,c
        assertArrayEquals(Proc.split("\"1 2 3\\\\\" abc"), new String[]{"1 2 3\\\\", "abc"});   // "1 2 3\\" abc -> 1 2 3 \\,abc
        assertArrayEquals(Proc.split("\"1 2 3\\\\\"\""), new String[]{"1 2 3\\\""});            // "1 2 3\\"" -> 1 2 3\"
    }
    
}
