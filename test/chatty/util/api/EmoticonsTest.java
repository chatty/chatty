
package chatty.util.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class EmoticonsTest {
    
    /**
     * Test of parseEmotesTag method, of class Emoticons.
     */
    @Test
    public void testParseEmotesTag() {
        System.out.println("parseEmotesTag (Errors)");
        System.out.println(Emoticons.parseEmotesTag(null));
        System.out.println(Emoticons.parseEmotesTag(""));
        System.out.println(Emoticons.parseEmotesTag("/"));
        System.out.println(Emoticons.parseEmotesTag(""));
        System.out.println(Emoticons.parseEmotesTag("1/2"));
        System.out.println(Emoticons.parseEmotesTag("1:1-2,3-/2:1"));
        
        System.out.println("parseEmotesTag (Regular)");
        System.out.println(Emoticons.parseEmotesTag("1:2-4/2:6-7"));
        System.out.println(Emoticons.parseEmotesTag("4:2-7,10-12/13:2-3"));
    }
    
    @Test
    public void testParseEmotesets() {
        testParseEmotesets2("1,2,3", "1", "2", "3");
        testParseEmotesets2("1,2,", "1", "2");
        testParseEmotesets2("1234_B,0", "1234_B", "0");
        testParseEmotesets2(",,a,,", "a");
        Assert.assertNull(Emoticons.parseEmotesets(",,,"));
        Assert.assertNull(Emoticons.parseEmotesets(""));
        Assert.assertNull(Emoticons.parseEmotesets(null));
        Assert.assertNull(Emoticons.parseEmotesets(" "));
    }
    
    private static void testParseEmotesets2(String input, String... result) {
        Set<String> resultSet = new HashSet<>(Arrays.asList(result));
        Assert.assertEquals(Emoticons.parseEmotesets(input), resultSet);
    }
    
}
