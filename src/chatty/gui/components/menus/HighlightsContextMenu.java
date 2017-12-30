
package chatty.gui.components.menus;

import chatty.Chatty;
import java.awt.event.ActionEvent;

/**
 * Context menu for the Highlights/Ignored Messages dialog (also used for Stream
 * Chat dialog currently).
 * 
 * @author tduva
 */
public class HighlightsContextMenu extends ContextMenu {

    public HighlightsContextMenu() {
        addItem("clearHighlights", Chatty.lang.GET("HIGHLIGHTSCONTEXTMENU_CLEAR", "Clear"));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
    
}
