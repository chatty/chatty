
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
        
        addItem("popoutChannel", "Popout");
        addItem("closeChannel", "Close");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.menuItemClicked(e);
        }
    }
    
}
