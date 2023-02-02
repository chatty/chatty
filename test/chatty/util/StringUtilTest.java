
package chatty.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
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
        assertEquals(StringUtil.append("abc", "|", null), "abc");
        assertEquals(StringUtil.append("abc", null, "abc"), "abcnullabc");
        assertEquals(StringUtil.append(null, null, null), null);
    }
    
    @Test
    public void testJoin() {
        Collection<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals(StringUtil.join(list, " "), "a b c");
        assertEquals(StringUtil.join(list, ", "), "a, b, c");
        assertEquals(StringUtil.join(list, ", ", 1), "b, c");
        assertEquals(StringUtil.join(list, ", ", 0, 2), "a, b");
        assertEquals(StringUtil.join(list, ", ", -1, 2), "a, b");
        assertEquals(StringUtil.join(list, ", ", -10, 2), "a, b");
        assertEquals(StringUtil.join(list, ", ", 1, 2), "b");
        assertEquals(StringUtil.join(list, ", ", 10, 2), "");
        assertEquals(StringUtil.join(list, "-", 0, 100), "a-b-c");
        assertEquals(StringUtil.join(list, ", ", 2), "c");
        assertEquals(StringUtil.join(list, ", ", 3), "");
        list.add(" d");
        assertEquals(StringUtil.join(list, ", "), "a, b, c,  d");
    }
    
    @Test
    public void testFirstToUpperCase() {
        assertEquals(StringUtil.firstToUpperCase(""), "");
        assertEquals(StringUtil.firstToUpperCase("a"), "A");
        assertEquals(StringUtil.firstToUpperCase(null), null);
        assertEquals(StringUtil.firstToUpperCase("Abc"), "Abc");
        assertEquals(StringUtil.firstToUpperCase("abc"), "Abc");
        assertEquals(StringUtil.firstToUpperCase(" abc"), " abc");
        
    }
    
    @Test
    public void testSplit() {
        assertEquals(StringUtil.split(null, 'a', 10), null);
        testSplit2(',', 0, "", "");
        testSplit2(',', 0, "a\\,b,c", "a,b", "c");
        testSplit2(',', 0, "a\\\\,b,c", "a\\", "b", "c");
        testSplit2(',', 0, "abc", "abc");
        testSplit2(',', 0, "\\abc", "abc");
        testSplit2(',', 0, "\\\\abc", "\\abc");
        testSplit2(',', 0, "a,b,c", "a", "b", "c");
        testSplit2(',', 1, "a,b,c", "a,b,c");
        testSplit2(',', 2, "a,b,c", "a", "b,c");
        testSplit2(',', 0, "'a,b',c", "a,b", "c");
        testSplit2(',', 0, "\\'a,b,c", "'a", "b", "c");
        testSplit2(',', 2, "\\'a,b,c'", "'a", "b,c'");
        testSplit2(',', '\'', '\\', 2, 2, "\\'a,b,c'", "'a", "b,c");
        testSplit2(',', 2, "a,b,\\c", "a", "b,\\c");
        testSplit2(',', '\'', '\\', 2, 2, "a,b,\\c", "a", "b,c");
        testSplit2(',', 2, "a\\,b,c", "a,b", "c");
        testSplit2(',', 0, "t|test=abc\\,lol", "t|test=abc,lol");
        
        // Double quotes
        testSplit2(',', 0, "''", "");
        testSplit2(',', 0, "\\''", "'");
        
        // Test quote == escape
        testSplit2Same(',', 0, "'a,b',c", "a,b", "c");
        testSplit2Same(',', 0, "'a,b'',c", "a,b',c");
        testSplit2Same(',', 0, "''", "'");
        testSplit2Same(',', 0, "'''", "'");
        testSplit2Same(',', 0, "''''", "''");
        testSplit2Same(',', 0, "''a,b',c,d", "'a", "b,c,d");
        testSplit2Same(',', 0, "''a,b,',c,d", "'a", "b", ",c,d");
        testSplit2Same(',', 0, "'a,b''',c", "a,b'", "c");
        
        // First split by space, then by comma (test not removing quote/escape)
        testSplit2(',', 0, StringUtil.split("a,b,'c d' e", ' ', '\'', '\\', 2, 0).get(0), "a", "b", "c d");
        testSplit2(',', 0, StringUtil.split("a,b,c\\ d e", ' ', '\'', '\\', 2, 0).get(0), "a", "b", "c d");
        testSplit2(',', '\'', '\'', 0, 1, StringUtil.split("a,b,'c'' d' e", ' ', '\'', '\'', 2, 0).get(0), "a", "b", "c' d");
        testSplit2(',', '\'', '\'', 0, 1, StringUtil.split("''a,b,'c d,e'", ' ', '\'', '\'', 2, 0).get(0), "'a", "b", "c d,e");
        testSplit2(',', '\'', '\'', 0, 1, StringUtil.split("''a,b,'''c'' d,e'", ' ', '\'', '\'', 2, 0).get(0), "'a", "b", "'c' d,e");
        
        // Various configurations
        testSplit2(',', '#', '#', 2, 1, "a,b,c", "a", "b,c");
        testSplit2(',', '#', '#', 2, 1, "a#,b,c", "a,b,c");
        testSplit2(' ', '-', '$', 2, 1, "abc- -123 -b c-", "abc 123", "-b c-");
        testSplit2(' ', '-', '$', 2, 1, "abc- $-123 -b c-", "abc -123 b", "c-");
        testSplit2(' ', '-', '$', 2, 1, "abc$ 123 -b c-", "abc 123", "-b c-");
        testSplit2(' ', '-', '$', 3, 1, "abc$ 123 -b c-", "abc 123", "b c");
        testSplit2(' ', '-', '$', 0, 1, "abc$ 123 -b c-", "abc 123", "b c");
        testSplit2(' ', '-', '$', 2, 2, "abc$ 123 -b c-", "abc 123", "b c");
        testSplit2(' ', '-', '$', 2, 0, "abc$ 123 -b c-", "abc$ 123", "-b c-");
        testSplit2(' ', '-', '-', 0, 0, "abc 123 -b c-", "abc", "123", "-b c-");
        testSplit2(' ', '-', '-', 0, 0, "abc 123 -b-- c-", "abc", "123", "-b-- c-");
        testSplit2(' ', '-', '$', 0, 0, "abc$ 123 -b c-", "abc$ 123", "-b c-");
    }
    
    private static void testSplit2(char split, int limit, String input, String... result) {
        testSplit2(split, '\'', '\\', limit, 1, input, result);
    }
    
    private static void testSplit2Same(char split, int limit, String input, String... result) {
        testSplit2(split, '\'', '\'', limit, 1, input, result);
    }
    
    private static void testSplit2(char split, char quote, char escape, int limit, int remove, String input, String... result) {
        assertEquals(Arrays.asList(result), StringUtil.split(input, split, quote, escape, limit, remove));
    }
    
    @Test
    public void testSplitLines() {
        assertArrayEquals(StringUtil.splitLines("a"), new String[]{"a"});
        assertArrayEquals(StringUtil.splitLines("a\nb"), new String[]{"a","b"});
        assertArrayEquals(StringUtil.splitLines("a\rb"), new String[]{"a","b"});
        assertArrayEquals(StringUtil.splitLines("a\r\nb"), new String[]{"a","b"});
        assertArrayEquals(StringUtil.splitLines("a\n\rb"), new String[]{"a","","b"}); // Invalid linebreak
    }
    
    @Test
    public void testQuote() {
        assertEquals("\"abc\"", StringUtil.quote("abc"));
        assertEquals("'abc'", StringUtil.quote("abc", "'"));
        assertEquals("''", StringUtil.quote("", "'"));
        assertEquals("'O''Neill'", StringUtil.quote("O'Neill", "'"));
        assertEquals("''''", StringUtil.quote("'", "'"));
        assertEquals("+abc+", StringUtil.quote("abc", "+"));
    }
    
    @Test
    public void testReplaceFunc() {
        assertEquals("a b c ", StringUtil.replaceFunc("~abc~", "~([a-z]+)~", m -> {
            return m.group(1).replaceAll("([a-z])", "$1 ");
        }));
    }
    
    @Test
    public void testSimilarity() {
        assertEquals(1, StringUtil.getSimilarity("", ""), 0);
        assertEquals(0, StringUtil.getSimilarity("a", ""), 0);
        assertEquals(0, StringUtil.getSimilarity("a", "b"), 0);
        assertEquals(0.6, StringUtil.getSimilarity("abc", "ab"), 0.1);
        assertEquals(0.8, StringUtil.getSimilarity("abcd", "abc"), 0.1);
        assertEquals(0.83, StringUtil.getSimilarity("This is a longer message", "This is a message that's longer"), 0.1);
        assertEquals(0.25, StringUtil.getSimilarity("night", "nacht"), 0.01);
        assertEquals(0.25, StringUtil.getSimilarity2("night", "nacht"), 0.01);
        assertEquals(0.5, StringUtil.getSimilarity("aa", "aaaa"), 0.01);
        assertEquals(1, StringUtil.getSimilarity2("aa", "aaaa"), 0.01);
        
        assertEquals(1, StringUtil.checkSimilarity("", "", 0, 1), 0);
        assertEquals(1, StringUtil.checkSimilarity("", "", 1, 1), 0);
        assertEquals(0, StringUtil.checkSimilarity("a", "", 0, 1), 0);
        assertEquals(0, StringUtil.checkSimilarity("a", "", 0.1f, 1), 0);
        
        assertEquals(0, StringUtil.checkSimilarity("aa", "aaaa", 0.6f, 1), 0.1);
        assertEquals(0.5, StringUtil.checkSimilarity("aa", "aaaa", 0.5f, 1), 0.01);
        assertEquals(1, StringUtil.checkSimilarity("aa", "aaaa", 0.6f, 2), 0.1);
        
        assertTrue(StringUtil.checkSimilarity("This is a longer message", "This is a message that's longer", 0.8f, 1) > 0);
    }
    
    @Test
    public void testRemoveWhitespaceAndMore() {
        assertEquals("", StringUtil.removeWhitespaceAndMore("!!", new char[]{'!'}));
        assertEquals("abc", StringUtil.removeWhitespaceAndMore("abc!", new char[]{'!'}));
        assertEquals("abc↗", StringUtil.removeWhitespaceAndMore("!abc!↗", new char[]{'!'}));
        assertEquals("abc", StringUtil.removeWhitespaceAndMore("!abc!↗", new char[]{'!', '↗'}));
        assertEquals("🐱", StringUtil.removeWhitespaceAndMore("!🐱!↗", new char[]{'!', '↗'}));
        // Array not sorted correctly, result is undefined, so not sure if this test works
//        assertEquals("!🐱!", StringUtil.removeWhitespaceAndMore("!🐱!↗", new char[]{'↗', '!'}));
        
        /**
         * Specifying a surrogate character directly would remove it, but most
         * of the time that would probably not be what is wanted.
         * 
         * StringUtil.getCharsFromString() can be used to ignore surrogates,
         * which excludes all chracters outside the BMP. This can be ok if the
         * use-case doesn't require it.
         */
        assertEquals("🐱", StringUtil.removeWhitespaceAndMore("!🐱!↗", StringUtil.getCharsFromString("!↗")));
        assertEquals("🐱", StringUtil.removeWhitespaceAndMore("!🐱!↗", StringUtil.getCharsFromString("↗!")));
        assertEquals("🐱", StringUtil.removeWhitespaceAndMore("!🐱!↗", StringUtil.getCharsFromString("↗!🐱abc")));
        assertEquals("!🐱!↗", StringUtil.removeWhitespaceAndMore("!🐱!↗", StringUtil.getCharsFromString("🐱")));
        assertEquals("!🐱!↗", StringUtil.removeWhitespaceAndMore("!🐱!↗", StringUtil.getCharsFromString("\uD83D")));
        // Surrogate directly specified, leaves the other surrogate
        assertEquals("!\uDC31!↗", StringUtil.removeWhitespaceAndMore("!🐱!↗", new char[]{'\uD83D'}));
    }
    
}
