
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
    
    private final String item;
    private final Color color;
    private final Highlighter.HighlightItem search;
    
    public MsgColorItem(String item, Color color) {
        this.item = item;
        this.color = color;
        this.search = new Highlighter.HighlightItem(item);
    }
    
    @Override
    public String getId() {
        return item;
    }
    
    @Override
    public Color getColor() {
        return color;
    }
    
    public boolean matches(User user, String text) {
        return search.matches(user, text);
    }
    
}
