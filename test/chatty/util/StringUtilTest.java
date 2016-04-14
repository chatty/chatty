
package chatty.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class StringUtilTest {
    
    
    @Test
    public void testRemoveLinebreakCharacters() {
        assertEquals(StringUtil.removeLinebreakCharacters("abc\r\nabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc\rabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc\nabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc abc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc\r\r\r\r\rabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc\r\n\n\r\rabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("\nabc abc"), " abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("\r"), " ");
    }
    
    @Test
    public void testRemoveDuplicateWhitespace() {
        assertEquals(StringUtil.removeDuplicateWhitespace("abc  abc"), "abc abc");
        assertEquals(StringUtil.removeDuplicateWhitespace("abc   abc"), "abc abc");
        assertEquals(StringUtil.removeDuplicateWhitespace("abcabc"), "abcabc");
        assertEquals(StringUtil.removeDuplicateWhitespace("abc abc"), "abc abc");
        assertEquals(StringUtil.removeDuplicateWhitespace("  "), " ");
        assertEquals(StringUtil.removeDuplicateWhitespace(""), "");
    }
    
    @Test
    public void testAppend() {
        assertEquals(StringUtil.append("abc", "|", "abc"), "abc|abc");
        assertEquals(StringUtil.append("abc", "", "abc"), "abcabc");
        assertEquals(StringUtil.append(null, "|", "b"), "b");
        assertEquals(StringUtil.append("", "|", "b"), "b");
        assertEquals(StringUtil.append("abc", "|", null), "abc|null");
        assertEquals(StringUtil.append("abc", null, "abc"), "abcnullabc");
        assertEquals(StringUtil.append(null, null, null), null);
    }
}
