
package chatty.gui.colors;

import chatty.Addressbook;
import chatty.User;
import chatty.gui.Highlighter;
import chatty.gui.Highlighter.HighlightItem;
import chatty.util.irc.MsgTags;
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
        this.search = new Highlighter.HighlightItem(item, "msgcolor");
    }
    
    public boolean matches(HighlightItem.Type type, String text, int msgStart, int msgEnd, String channel,
            Addressbook ab, User user, User localUser, MsgTags tags) {
        return search.matches(type, text, msgStart, msgEnd, null, channel, ab, user, localUser, tags);
    }
    
}
