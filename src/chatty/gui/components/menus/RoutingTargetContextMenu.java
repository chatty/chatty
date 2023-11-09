
package chatty.gui.components.menus;

import chatty.gui.DockedDialogHelper;
import chatty.gui.components.Channel;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Context menu for Custom Tabs.
 * 
 * @author tduva
 */
public class RoutingTargetContextMenu extends ContextMenu {

    public RoutingTargetContextMenu(List<Channel> openChannels,
                                    boolean fixedChannelEnabled,
                                    boolean addAllEntry,
                                    boolean showAll,
                                    String currentChannel) {
        
        if (openChannels != null) {
            addItem("clearAll", "Clear all");
            addItem("clearCurrent", "Clear current");
            addSeparator();
            DockedDialogHelper.addChannelSelectionToContextMenu(
                    this,
                    openChannels,
                    fixedChannelEnabled,
                    addAllEntry,
                    showAll,
                    currentChannel);
        }
        else {
            addItem("clearAll", "Clear");
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
    
}
