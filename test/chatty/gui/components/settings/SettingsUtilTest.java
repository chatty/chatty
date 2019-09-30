
package chatty.gui.components.settings;

import static chatty.gui.components.settings.SettingsUtil.removeHtmlConditions;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class SettingsUtilTest {
    
    @Test
    public void testRemoveHtmlConditions() {
        assertEquals(removeHtmlConditions(
                "a<!--#START:streamStatus#-->___<!--#END:streamStatus#-->b", "stream"),
                "ab");
        assertEquals(removeHtmlConditions(
                "a<!--#START:streamStatus#-->___<!--#END:streamStatus#-->b", "streamStatusAbc"),
                "ab");
        assertEquals(removeHtmlConditions(
                "a<!--#START:streamStatus#-->___<!--#END:streamStatus#-->b", "streamStatus"),
                "a<!--#START:streamStatus#-->___<!--#END:streamStatus#-->b");
        assertEquals(removeHtmlConditions(
                "a<!--#START:abc#-->___<!--#END:abc#-->b", "streamStatus"),
                "ab");
        assertEquals(removeHtmlConditions(
                "a<!--#START:!streamStatus#-->___<!--#END:!streamStatus#-->b", "stream"),
                "a<!--#START:!streamStatus#-->___<!--#END:!streamStatus#-->b");
        assertEquals(removeHtmlConditions(
                "a<!--#START:!streamStatus#-->___<!--#END:!streamStatus#-->b", "streamStatus"),
                "ab");
        assertEquals(removeHtmlConditions(
                "a<!--#START:a#-->___<!--#END:a#-->b<!--#START:a#-->___<!--#END:a#-->c", "a"),
                "a<!--#START:a#-->___<!--#END:a#-->b<!--#START:a#-->___<!--#END:a#-->c");
        assertEquals(removeHtmlConditions(
                "a<!--#START:a#-->___<!--#END:a#-->b<!--#START:a#-->___<!--#END:a#-->c", "b"),
                "abc");
        assertEquals(removeHtmlConditions(
                "a<!--#START:a#-->_<!--#START:b#-->_<!--#END:b#-->_<!--#END:a#-->b", "a"),
                "a<!--#START:a#-->__<!--#END:a#-->b");
        assertEquals(removeHtmlConditions(
                "a<!--#START:a#-->_<!--#START:b#-->_<!--#END:b#-->_<!--#END:a#-->b", "b"),
                "ab");
    }
    
}
