
package chatty.util.api;

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
    
}
