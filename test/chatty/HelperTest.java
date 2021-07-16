
package chatty;

import chatty.util.StringUtil;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class HelperTest {
    
    @Test
    public void validateChannelTest() {
        assertTrue(Helper.isValidChannel("joshimuz"));
        assertTrue(Helper.isValidChannel("51nn3r"));
        assertTrue(Helper.isValidChannel("#joshimuz"));
        assertFalse(Helper.isValidChannel("##joshimuz"));
        assertFalse(Helper.isValidChannel(""));
        assertFalse(Helper.isValidChannel(" "));
        assertFalse(Helper.isValidChannel("abc$"));
        
        assertTrue(Helper.isValidChannelStrict("#joshimuz"));
        assertFalse(Helper.isValidChannelStrict("joshimuz"));
        
        assertTrue(Helper.isRegularChannel("#joshimuz"));
        assertTrue(Helper.isRegularChannel("joshimuz"));
        assertFalse(Helper.isRegularChannel(""));
        assertFalse(Helper.isRegularChannel("#chatrooms:1234:abc-def"));
        assertFalse(Helper.isRegularChannel("chatrooms:"));
        
        assertTrue(Helper.isRegularChannelStrict("#joshimuz"));
        assertTrue(Helper.isRegularChannelStrict("#underscore_"));
        assertFalse(Helper.isRegularChannelStrict("joshimuz"));
        assertFalse(Helper.isRegularChannelStrict("#chatrooms:1234:abc-def"));
        
        assertTrue(Helper.isValidStream("joshimuz"));
        assertTrue(Helper.isValidStream("51nn3r"));
        assertFalse(Helper.isValidStream("chatrooms:1234:abc-def"));
        assertFalse(Helper.isValidStream(null));
        assertFalse(Helper.isValidStream(""));
        assertFalse(Helper.isValidStream(" "));
        
        assertEquals(Helper.toStream("#channel"), "channel");
        assertEquals(Helper.toStream(""), "");
        assertEquals(Helper.toStream("#chatrooms:1234:abc-def"), "chatrooms:1234:abc-def");
        
        assertEquals(Helper.toValidStream("#channel"), "channel");
        assertEquals(Helper.toValidStream(""), null);
        assertEquals(Helper.toValidStream("#chatrooms:1234:abc-def"), null);
        
        assertEquals(Helper.toValidChannel("#channel"), "#channel");
        assertEquals(Helper.toValidChannel("channel"), "#channel");
        assertEquals(Helper.toValidChannel("$channel"), null);
        assertEquals(Helper.toValidChannel("chatrooms:1234:abc-def"), "#chatrooms:1234:abc-def");
        assertEquals(Helper.toValidChannel("abc"), "#abc");
        assertNull(Helper.toValidChannel(""));
        assertNull(Helper.toValidChannel("#"));
        assertNull(Helper.toValidChannel(" 1"));
        assertEquals(Helper.toValidChannel("#abc"), "#abc");
    }
    
    @Test
    public void removeDuplicateWhitespaceTest() {
        assertEquals(StringUtil.removeDuplicateWhitespace(" ")," ");
        assertEquals(StringUtil.removeDuplicateWhitespace(""), "");
        assertEquals(StringUtil.removeDuplicateWhitespace("abc"),"abc");
        assertEquals(StringUtil.removeDuplicateWhitespace("a  b"), "a b");
        assertEquals(StringUtil.removeDuplicateWhitespace("       "), " ");
        assertEquals(StringUtil.removeDuplicateWhitespace(" a  b  "), " a b ");
    }
    
    @Test
    public void htmlspecialchars_encodeTest() {
        assertEquals(Helper.htmlspecialchars_encode("&"), "&amp;");
        assertEquals(Helper.htmlspecialchars_encode("&amp;"), "&amp;amp;");
        assertEquals(Helper.htmlspecialchars_encode("hello john & everyone else"), "hello john &amp; everyone else");
        assertEquals(Helper.htmlspecialchars_encode("<"), "&lt;");
        assertEquals(Helper.htmlspecialchars_encode(">"), "&gt;");
        assertEquals(Helper.htmlspecialchars_encode("\""), "&quot;");
        assertEquals(Helper.htmlspecialchars_encode("& >"), "&amp; &gt;");
    }
    
    @Test
    public void htmlspecialchars_decodeTest() {
        assertEquals(Helper.htmlspecialchars_decode("&amp;"), "&");
        assertEquals(Helper.htmlspecialchars_decode("&quot;"), "\"");
        assertEquals(Helper.htmlspecialchars_decode("&lt;"), "<");
        assertEquals(Helper.htmlspecialchars_decode("&gt;"), ">");
        assertEquals(Helper.htmlspecialchars_decode("abc &amp; test"), "abc & test");
    }
    
    @Test
    public void tagsvalue_decodeTest() {
        assertEquals(Helper.tagsvalue_decode("\\s"), " ");
        assertEquals(Helper.tagsvalue_decode("\\:"), ";");
        assertEquals(Helper.tagsvalue_decode("\\n"), "\n");
        assertEquals(Helper.tagsvalue_decode("\\\\s"), "\\s");
        assertEquals(Helper.tagsvalue_decode("abc\\stest"), "abc test");
        assertEquals(Helper.tagsvalue_decode(""), "");
        assertEquals(Helper.tagsvalue_decode(null), null);
        assertEquals(Helper.tagsvalue_decode(" "), " ");
        assertEquals(Helper.tagsvalue_decode("\\\\s\\s\\:"), "\\s ;");
    }
    
    @Test
    public void testUrls() {
        
        //---------------
        // Just matching
        //---------------
        String[] shouldMatch = new String[]{
            "twitch.tv",
            "Twitch.TV",
            "twitch.tv/abc",
            "twitch.tv/ABC",
            "http://twitch.tv",
            "https://twitch.tv",
            "amazon.de/ääh",
            "https://google.de",
            "google.com/abc(blah)",
            "http://börse.de",
            "www.twitch.tv/",
            "twitch.tv🐁"
        };
        
        /**
         * Trying to match the full string, so it's not necessary to check the
         * matches. Ofc it would find an URL in most of those.
         */
        String[] shouldNotMatch = new String[]{
            "twitch.tv.",
            "twitch.tv ",
            " twitch.tv",
            " twitch.tv ",
            "https:/twitch.tv",
            "https://twitch.tv:",
            "twitch.tv:",
            "twitch.tv,",
            "www.twitch.tv,",
            "www.twitch.tv/,",
            "asdas.abc",
            "http://",
            "http://www.",
            "https://",
            "https://www."
        };
        
        for (String test : shouldMatch) {
            assertTrue("Should match: "+test, Helper.getUrlPattern().matcher(test).matches());
        }
        
        for (String test : shouldNotMatch) {
            assertFalse("Should not match: "+test, Helper.getUrlPattern().matcher(test).matches());
        }
        
        //-------------------
        // Finding in String
        //-------------------
        String find = "Abc http://example.com http://example.com/abc(test) dumdidum(http://example.com) [http://example.com] "
                + "(http://example.com)[github.io] [github.io] (github.io) 🐣twitch.tv🐣 https:// www. not.a/domain "
                + "twitch.tv/🐁 twitch.tv github.io $ chatty.github.io Other text and whatnot https://chatty.github.io";
        String[] findTarget = new String[]{
            "http://example.com",
            "http://example.com/abc(test)",
            "http://example.com)",
            "http://example.com]",
            "http://example.com)[github.io]",
            "github.io]",
            "github.io)",
            "twitch.tv🐣",
            "twitch.tv/🐁",
            "twitch.tv",
            "github.io",
            "chatty.github.io",
            "https://chatty.github.io"
        };

        Matcher m = Helper.getUrlPattern().matcher(find);
        int i = 0;
        while (m.find()) {
            String found = m.group();
            String target = findTarget[i];
            assertTrue("Should have found: "+target+" found: "+found,
                    found.equals(target));
            i++;
        }
    }
    
    @Test
    public void removeEmojiVariationSelectorTest() {
        assertEquals(Helper.removeEmojiVariationSelector(null), null);
        assertEquals(Helper.removeEmojiVariationSelector("❤︎"), "❤");
        assertEquals(Helper.removeEmojiVariationSelector("❤︎︎"), "❤");
        assertEquals(Helper.removeEmojiVariationSelector("\uFE0F❤︎︎"), "❤");
        assertEquals(Helper.removeEmojiVariationSelector("\uFE0F\uFE0E"), "");
        assertEquals(Helper.removeEmojiVariationSelector("❤️ 	❤ 	❤︎"), "❤ 	❤ 	❤");
    }
    
    @Test
    public void getChainedCommandsTest() {
        chainedTest("", new String[]{});
        chainedTest(null, new String[]{});
        chainedTest("a", new String[]{"a"});
        chainedTest("a | b", new String[]{"a", "b"});
        chainedTest("a|b", new String[]{"a", "b"});
        chainedTest("a || b", new String[]{"a | b"});
        chainedTest("a||b", new String[]{"a|b"});
        chainedTest("a|||b", new String[]{"a||b"});
        chainedTest("|b", new String[]{"b"});
        chainedTest("a|b           |c", new String[]{"a", "b", "c"});
        chainedTest("||a|| | b | c", new String[]{"|a|", "b", "c"});
        chainedTest("||||||", new String[]{"|||||"});
        chainedTest("|||||| |", new String[]{"|||||"});
        chainedTest("|||||| | ||", new String[]{"|||||", "|"});
        chainedTest("| | | | | |", new String[]{});
        chainedTest("a | /chain b", new String[]{"a", "/chain b"});
        chainedTest("a |c \\|d |1||", new String[]{"a", "c \\", "d", "1|"});
        chainedTest("b | a        ", new String[]{"b", "a"});
    }
    
    private static void chainedTest(String input, String[] result) {
        assertArrayEquals(Helper.getChainedCommands(input).toArray(), result);
    }
    
    @Test
    public void parseChannelsFromStringTest() {
        parseChannelsTest("", true);
        parseChannelsTest("$abc", true);
        parseChannelsTest("#jaf-jiaefw,", true);
        parseChannelsTest("#abc", true, "#abc");
        parseChannelsTest("abc", true, "#abc");
        parseChannelsTest("abc", false, "abc");
        parseChannelsTest("#abc, abc", true, "#abc");
        parseChannelsTest("#abc, abc", false, "#abc", "abc");
        parseChannelsTest("#abc,abc", false, "#abc", "abc");
        parseChannelsTest(" #abc,     abc ", false, "#abc", "abc");
        parseChannelsTest(" #abc,     \nabc ", false, "#abc", "abc");
        parseChannelsTest(" #abc, $##afwe, abc, # ", false, "#abc", "abc");
        
        Addressbook ab = new Addressbook(null, null, null);
        ab.add("#chan", "cat");
        ab.add("user", "cat");
        ab.add("#chan2", "cat2");
        ab.add("user2", "cat2");
        
        parseChannelsTest("[cat]", true);
        Helper.addressbook = ab;
        parseChannelsTest("[cat]", false, "#chan", "user");
        parseChannelsTest("[cat]", true, "#chan", "#user");
        parseChannelsTest("[cat ]", true, "#chan", "#user");
        parseChannelsTest("[cat #]", true, "#chan");
        parseChannelsTest("[cat !#]", true, "#user");
        parseChannelsTest("[cat2]", true, "#chan2", "#user2");
        parseChannelsTest("#chan, [cat2]", true, "#chan", "#chan2", "#user2");
        parseChannelsTest("#chan, [cat2], #chan3", true, "#chan", "#chan2", "#user2", "#chan3");
        Helper.addressbook = null;
        parseChannelsTest("#chan, [cat2], #chan3", true, "#chan", "#chan3");
    }
    
    private static void parseChannelsTest(String input, boolean prepend, String... result) {
        assertEquals(new HashSet<>(Arrays.asList(result)), Helper.parseChannelsFromString(input, prepend));
    }
    
    @Test
    public void testChannelsListParsingRoundTrip() {
        Set<String> expected = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        expected.add("foo");
        expected.add("bar");
        expected.add("hello");
        expected.add("world");
        TreeSet<String> actual = channelListRoundTrip(expected, false);
        assertEquals(expected, actual);
    }

    @Test
    public void testPrependedChannelsListParsingRoundTrip() {
        Set<String> expected = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        expected.add("#foo");
        expected.add("#bar");
        expected.add("#hello");
        expected.add("#world");
        TreeSet<String> actual = channelListRoundTrip(expected, true);
        assertEquals(expected, actual);
    }

    private TreeSet<String> channelListRoundTrip(Set<String> original, boolean prepend) {
        TreeSet<String> copy = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        copy.addAll(Helper.parseChannelsFromString(Helper.buildStreamsString(original), prepend));
        return copy;
    }
}
