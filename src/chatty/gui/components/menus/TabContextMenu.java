
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
        
        addSubItem("closeAllTabsButCurrent", "Except current", subMenu);
        addSubItem("closeAllTabsToLeft", "To left of current", subMenu);
        addSubItem("closeAllTabsToRight", "To right of current", subMenu);
        addSeparator(subMenu);
        addSubItem("closeAllTabs", "All", subMenu);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.menuItemClicked(e);
        }
    }
    
}
