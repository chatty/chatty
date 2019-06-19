
package chatty.gui.emoji;

import chatty.util.api.Emoticon;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class EmojiUtilTest {
    
    /**
     * Test that the function to check for possible Emoji actually matches all
     * Emoji.
     */
    @Test
    public void testContainsEmoji() {
        for (EmojiUtil.EmojiSet set : EmojiUtil.EmojiSet.values()) {
            Set<Emoticon> emotes = EmojiUtil.makeEmoticons(set.id);
            for (Emoticon emoji : emotes) {
                assertTrue("Failed to detect "+emoji.stringId+" ("+emoji.code+")",
                        EmojiUtil.mightContainEmoji(emoji.code));
            }
        }
    }
    
    /**
     * Test that each Emoji shortcode (including aliases) is only used once.
     */
    @Test
    public void testShortCodeUnique() {
        for (EmojiUtil.EmojiSet set : EmojiUtil.EmojiSet.values()) {
            Set<String> shortCodes = new HashSet<>();
            Set<Emoticon> emotes = EmojiUtil.makeEmoticons(set.id);
            for (Emoticon emoji : emotes) {
                assertFalse("Duplicate shortcode "+emoji.stringId,
                        shortCodes.contains(emoji.stringId));
                assertFalse("Duplicate shortcode "+emoji.stringIdAlias,
                        shortCodes.contains(emoji.stringIdAlias));
                if (emoji.stringId != null) {
                    shortCodes.add(emoji.stringId);
                }
                if (emoji.stringIdAlias != null) {
                    shortCodes.add(emoji.stringIdAlias);
                }
            }
        }
    }
    
}
