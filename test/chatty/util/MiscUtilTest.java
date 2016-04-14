
package chatty.util;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class MiscUtilTest {
    
    @Test
    public void testParseArgs() {
        Map<String, String> result = new HashMap<>();
        String[] input;
        
        input = new String[]{"-cd","-channel","test"};
        result.put("cd", "");
        result.put("channel","test");
        assertEquals(MiscUtil.parseArgs(input), result);
        result.clear();
        
        input = new String[]{"-cd","-channel","test, abc"};
        result.put("cd", "");
        result.put("channel","test, abc");
        assertEquals(MiscUtil.parseArgs(input), result);
        result.clear();
        
        // Empty args
        input = new String[]{};
        assertEquals(MiscUtil.parseArgs(input), result);
        result.clear();
        
        // Empty argument key
        input = new String[]{"-"};
        result.put("", "");
        assertEquals(MiscUtil.parseArgs(input), result);
        result.clear();
        
        input = new String[]{"-", "abc test"};
        result.put("", "abc test");
        assertEquals(MiscUtil.parseArgs(input), result);
        result.clear();
        
        // Repeating the same argument key
        input = new String[]{"-channel","test, abc","-channel","jfwe"};
        result.put("channel","jfwe");
        assertEquals(MiscUtil.parseArgs(input), result);
        result.clear();
        
        // No argument key at all
        input = new String[]{"cd"};
        assertEquals(MiscUtil.parseArgs(input), result);
        result.clear();
    }
}
