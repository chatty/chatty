
package chatty.gui.components.menus;

import java.awt.event.ActionEvent;

/**
 * Context menu for the Highlights/Ignored Messages dialog (also used for Stream
 * Chat dialog currently).
 * 
 * @author tduva
 */
public class HighlightsContextMenu extends ContextMenu {

    public HighlightsContextMenu() {
        addItem("clearHighlights", "Clear");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
    
}
