
package chatty.gui.components.menus;

import chatty.gui.components.Channel;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Map;

/**
 * The Context Menu that appears on the tab bar.
 * 
 * @author tduva
 */
public class TabContextMenu extends ContextMenu {

    private final ContextMenuListener listener;
    private final Channel chan;
    
    public TabContextMenu(ContextMenuListener listener, Channel chan, Map<String, Collection<Channel>> info) {
        this.listener = listener;
        this.chan = chan;
        
        addItem("popoutChannel", "Popout "+chan.getChannel());
        addItem("popoutChannelWindow", "Popout as Window");
        addSeparator();
        addItem("closeChannel", "Close");
        
        String closeTabsMenu = "Close Tabs";
        addNumItem("closeAllTabsButCurrent", "Except current", closeTabsMenu, info);
        addNumItem("closeAllTabsToLeft", "To left of current", closeTabsMenu, info);
        addNumItem("closeAllTabsToRight", "To right of current", closeTabsMenu, info);
        addSeparator(closeTabsMenu);
        addNumItem("closeAllTabsOffline", "Offline", closeTabsMenu, info);
        addSeparator(closeTabsMenu);
        addNumItem("closeAllTabs", "All", closeTabsMenu, info);
        
        addSeparator();
        String closeAllMenu = "Close All";
        addNumItem("closeAllTabs2ButCurrent", "Except current", closeAllMenu, info);
        addSeparator(closeAllMenu);
        addNumItem("closeAllTabs2Offline", "Offline", closeAllMenu, info);
        addSeparator(closeAllMenu);
        addNumItem("closeAllTabs2", "All", closeAllMenu, info);
        
        CommandMenuItems.addCommands(CommandMenuItems.MenuType.CHANNEL, this);
    }
    
    private void addNumItem(String cmd, String label, String submenu, Map<String, Collection<Channel>> info) {
        if (info.containsKey(cmd)) {
            addItem(cmd, String.format("%s (%d)", label, info.get(cmd).size()), submenu);
        }
        else {
            addItem(cmd, label, submenu);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.channelMenuItemClicked(e, chan);
        }
    }
    
}
