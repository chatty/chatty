
package chatty;

import chatty.util.StringUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class HelperTest {
    
    public HelperTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    @Test
    public void validateChannelTest() {
        assertTrue(Helper.validateChannel("joshimuz"));
        assertTrue(Helper.validateChannel("51nn3r"));
        assertTrue(Helper.validateChannel("#joshimuz"));
        assertFalse(Helper.validateChannel("##joshimuz"));
        assertFalse(Helper.validateChannel(""));
        assertFalse(Helper.validateChannel(" "));
        assertFalse(Helper.validateChannel("abc$"));
    }
    
    @Test
    public void checkChannelTest() {
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
}
