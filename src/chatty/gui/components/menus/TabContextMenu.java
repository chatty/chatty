
package chatty.gui.components.menus;

import chatty.Chatty;
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
        
        String subMenu = Chatty.lang.GET("TABCONTEXTMENU_CLOSEALL", "Close All");
        
        addItem("popoutChannel", Chatty.lang.GET("TABCONTEXTMENU_POPOUT", "Popout"));addSeparator();
        addItem("closeChannel", Chatty.lang.GET("TABCONTEXTMENU_CLOSE", "Close"));
        
        addItem("closeAllTabsButCurrent", Chatty.lang.GET("TABCONTEXTMENU_EXCEPT_CURRENT", "Except current"), subMenu);
        addItem("closeAllTabsToLeft", Chatty.lang.GET("TABCONTEXTMENU_TOLEFTOFCURRENT", "To left of current"), subMenu);
        addItem("closeAllTabsToRight", Chatty.lang.GET("TABCONTEXTMENU_TORIGHTOFCURRENT", "To right of current"), subMenu);
        addSeparator(subMenu);
        addItem("closeAllTabs", Chatty.lang.GET("TABCONTEXTMENU_ALL", "All"), subMenu);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.menuItemClicked(e);
        }
    }
    
}
