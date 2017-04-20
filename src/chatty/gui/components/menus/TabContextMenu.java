
package chatty.gui.components.menus;

import java.awt.event.ActionEvent;

/**
 * The Context Menu that appears on the tab bar.
 * 
 * @author tduva
 */
public class TabContextMenu extends ContextMenu {

    private final ContextMenuListener listener;
    
    public TabContextMenu(ContextMenuListener listener) {
        this.listener = listener;
        
        String subMenu = "Close All";
        
        addItem("popoutChannel", "Popout");addSeparator();
        addItem("closeChannel", "Close");
        
        addItem("closeAllTabsButCurrent", "Except current", subMenu);
        addItem("closeAllTabsToLeft", "To left of current", subMenu);
        addItem("closeAllTabsToRight", "To right of current", subMenu);
        addSeparator(subMenu);
        addItem("closeAllTabs", "All", subMenu);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.menuItemClicked(e);
        }
    }
    
}
