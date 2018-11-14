
package chatty.gui.colors;

import chatty.User;
import chatty.gui.Highlighter;
import chatty.util.StringUtil;
import java.awt.Color;

/**
 *
 * @author tduva
 */
public class MsgColorItem extends ColorItem {

    private final Highlighter.HighlightItem search;
    
    public MsgColorItem(String item,
            Color foreground, boolean foregroundEnabled,
            Color background, boolean backgroundEnabled) {
        super(item, foreground, foregroundEnabled, background, backgroundEnabled);
        this.search = new Highlighter.HighlightItem(item);
    }
    
    public boolean matches(User user, String text) {
        return search.matches(user, text);
    }
    
}
