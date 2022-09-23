
package chatty;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class CommandsTest {

    @Test
    public void testCommandParsedArgs() {
        testFailedParsedArgs("", 1);
        testFailedParsedArgs("-", 1);
        testFailedParsedArgs("-a", 1);
        testFailedParsedArgs("-a ", 1);
        testFailedParsedArgs("abc 123", 3);
        testParsedArgs("-a", 0, null, "a");
        testParsedArgs("-a ", 0, null, "a");
        testParsedArgs("-a  ", 1, new String[]{" "}, "a");
        testParsedArgs("-a 123", 1, new String[]{"123"}, "a");
        testParsedArgs("abc", 1, new String[]{"abc"}, null);
        testParsedArgs("abc 123", 1, new String[]{"abc 123"}, null);
        testParsedArgs("abc 123", 2, new String[]{"abc", "123"}, null);
        testParsedArgs("- abc", 1, new String[]{"abc"}, null);
        testParsedArgs("-123 abc", 1, new String[]{"abc"}, "123");
        testParsedArgs("abc", 0, new String[]{"abc"}, null);
        testParsedArgs("-a abc", 0, new String[]{"abc"}, "a");
        testParsedArgs("-a abc 123", 0, new String[]{"abc 123"}, "a");
        
        testFailedParsedArgs("", 1, 1);
        testFailedParsedArgs("a b c", 1, 3);
        testFailedParsedArgs("a b c", 1, 4);
        testFailedParsedArgs("a b c", 1, 5);
        testParsedArgs("a", 2, 1, new String[]{"a"}, null);
        testParsedArgs("a b c", 2, 2, new String[]{"a", "b c"}, null);
        testParsedArgs("a b c", 2, 1, new String[]{"a", "b c"}, null);
        testParsedArgs("a b c", 2, 0, new String[]{"a", "b c"}, null);
        testParsedArgs("a b", 2, 1, new String[]{"a", "b"}, null);
        testParsedArgs("a", 2, 1, new String[]{"a"}, null);
    }
    
    private static void testFailedParsedArgs(String input, int num) {
        testFailedParsedArgs(input, num, num);
    }
    
    private static void testFailedParsedArgs(String input, int num, int numRequired) {
        assertNull(Commands.CommandParsedArgs.parse(input, num, numRequired));
    }
    
    private static void testParsedArgs(String input, int num, String[] args, String options) {
        testParsedArgs(input, num, num, args, options);
    }
    
    private static void testParsedArgs(String input, int num, int numRequired, String[] args, String options) {
        Commands.CommandParsedArgs parsed = Commands.CommandParsedArgs.parse(input, num, numRequired);
        assertArrayEquals(args, parsed.args);
        assertEquals(options, parsed.options);
    }
    
}
