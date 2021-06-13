
package chatty.util.commands;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class CustomCommandsTest {
    
    @Test
    public void parseCommandWithNameTest() {
        splitTest("/abc blah", "abc", null, "blah");
        splitTest("/abc#chan blah", "abc", "chan", "blah");
        splitTest("abc blah", "abc", null, "blah");
        splitTest("abc#chan blah", "abc", "chan", "blah");
        splitTest("/abc# blah", "abc", "", "blah");
        splitTest("/slap /me slaps $$1 around a bit with a large trout",
                "slap", null, "/me slaps $$1 around a bit with a large trout");
        splitTest("/slap  /me slaps $$1 around a bit with a large trout",
                "slap", null, "/me slaps $$1 around a bit with a large trout");
        splitTest("/slap  /me slaps $$1 around a bit with a large trout ",
                "slap", null, "/me slaps $$1 around a bit with a large trout");
        splitTest("/slap   /me slaps $$1 around a bit with a large trout ",
                "slap", null, "/me slaps $$1 around a bit with a large trout");
        splitTest("/slap \\ /me slaps $$1 around a bit with a large trout ",
                "slap", null, " /me slaps $$1 around a bit with a large trout");
        
        assertNull(CustomCommands.parseCommandWithName(""));
        assertNull(CustomCommands.parseCommandWithName("#"));
        assertNull(CustomCommands.parseCommandWithName("##"));
        assertNull(CustomCommands.parseCommandWithName(" #"));
        assertNull(CustomCommands.parseCommandWithName(" ##"));
        assertNull(CustomCommands.parseCommandWithName(" ## "));
        assertNull(CustomCommands.parseCommandWithName("/"));
        assertNull(CustomCommands.parseCommandWithName("/abc"));
        assertNull(CustomCommands.parseCommandWithName("/abc "));
        assertNull(CustomCommands.parseCommandWithName("# abc blah"));
        assertNull(CustomCommands.parseCommandWithName("#abc blah"));
        assertNull(CustomCommands.parseCommandWithName("#abcblah"));
        assertNull(CustomCommands.parseCommandWithName("#/abcblah"));
        assertNull(CustomCommands.parseCommandWithName("/#abcblah"));
        
        assertNotNull(CustomCommands.parseCommandWithName("a# abc blah"));
        assertNotNull(CustomCommands.parseCommandWithName("/abc jfwef"));
    }
    
    private static void splitTest(String input, String name, String chan, String command) {
        CustomCommand test = CustomCommands.parseCommandWithName(input);
        CustomCommand ref = CustomCommand.parse(name, chan, command);
        assertEquals(ref, test);
    }
    
}
