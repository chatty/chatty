
package chatty.gui.components.menus;

import chatty.gui.Channels;
import chatty.gui.components.Channel;
import chatty.gui.components.settings.TabSettings;
import chatty.util.StringUtil;
import chatty.util.dnd.DockContent;
import chatty.util.settings.Settings;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * The Context Menu that appears on the tab bar.
 * 
 * @author tduva
 */
public class TabContextMenu extends ContextMenu {

    private final ContextMenuListener listener;
    private final DockContent content;
    
    public TabContextMenu(ContextMenuListener listener, DockContent content, Map<String, Collection<DockContent>> closeInfo, Settings settings) {
        this.listener = listener;
        this.content = content;
        
        String typeLabel = TabSettings.tabPosLabel(content.getId().substring(0, 1));
        
        //--------------------------
        // Popout
        //--------------------------
        addItem("popoutChannel", "Popout "+content.getTitle());
        addItem("popoutChannelWindow", "Popout as Window");
        addSeparator();
        
        //--------------------------
        // Close Tabs
        //--------------------------
        addItem("closeChannel", "Close");
        
        String closeTabsMenu = "This tab pane";
        String closeAllMenu = "All tabs";
        
        JMenu closeMenu = new JMenu("Close Tabs");
        JMenu closeMenuThis = new JMenu(closeTabsMenu);
        JMenu closeMenuAll = new JMenu(closeAllMenu);
        
        registerSubmenu(closeMenu);
        registerSubmenu(closeMenuThis);
        registerSubmenu(closeMenuAll);
        
        addNumItem("closeAllTabsButCurrent", "Except current", closeTabsMenu, closeInfo);
        addNumItem("closeAllTabsToLeft", "To left of current", closeTabsMenu, closeInfo);
        addNumItem("closeAllTabsToRight", "To right of current", closeTabsMenu, closeInfo);
        addSeparator(closeTabsMenu);
        addNumItem("closeAllTabsOffline", "Offline channels", closeTabsMenu, closeInfo);
        addSeparator(closeTabsMenu);
        addNumItem("closeAllTabs", "All", closeTabsMenu, closeInfo);
        
        addNumItem("closeAllTabs2ButCurrent", "Except current", closeAllMenu, closeInfo);
        addSeparator(closeAllMenu);
        addNumItem("closeAllTabs2Offline", "Offline channels", closeAllMenu, closeInfo);
        addSeparator(closeAllMenu);
        addNumItem("closeAllTabs2", "All", closeAllMenu, closeInfo);
        
        closeMenu.add(closeMenuThis);
        closeMenu.add(closeMenuAll);
        
        addCheckboxItem("tabsCloseSameType", "Same tab type only", "Close Tabs", settings.getBoolean("closeTabsSameType"));
        getItem("tabsCloseSameType").setToolTipText("Only close tabs of the same type you opened this menu for (currently "+typeLabel+")");
        
        add(closeMenu);
        
        //--------------------------
        // Order
        //--------------------------
        String customOrderMenuName = "This tab";
        String customOrderMenuName2 = typeLabel;
        JMenu orderMenu = new JMenu("Order");
        JMenu customOrderMenu = new JMenu(customOrderMenuName);
        JMenu customOrderMenu2 = new JMenu(customOrderMenuName2);
        registerSubmenu(orderMenu);
        registerSubmenu(customOrderMenu);
        registerSubmenu(customOrderMenu2);
        orderMenu.add(customOrderMenu);
        orderMenu.add(customOrderMenu2);
        addCheckboxItem("tabsAutoSort", "Resort on changes", "Order", settings.getBoolean("tabsAutoSort"));
        getItem("tabsAutoSort").setToolTipText("Automatically resort tabs when changing tab order settings");
        orderMenu.addSeparator();
        addItem("tabsSort", "Resort all tabs", "Order");
        
        Map<Long, List<String>> posIds = Channels.getTabPosIds(settings);
        addPosItems(posIds, Channels.getTabPos(settings, content.getId()), customOrderMenuName, "tabsPosTab");
        addPosItems(posIds, Channels.getTabPos(settings, content.getId().substring(0, 1)), customOrderMenuName2, "tabsPosType");
        
        addSeparator();
        add(orderMenu);
        
        //--------------------------
        // Custom Commands
        //--------------------------
        if (content instanceof Channels.DockChannelContainer
                && ((Channels.DockChannelContainer)content).getContent().getType() == Channel.Type.CHANNEL) {
            CommandMenuItems.addCommands(CommandMenuItems.MenuType.CHANNEL, this);
        }
    }
    
    private void addPosItems(Map<Long, List<String>> posIds, long pos, String menuName, String actionName) {
        for (long i=-10;i<=10;i++) {
            String title = String.valueOf(i);
            switch ((int)i) {
                case -10:
                    title += " (further in front)";
                    break;
                case 0:
                    title += " (Default)";
                    break;
                case 10:
                    title += " (further in back)";
                    break;
            }
            if (i != 0 && posIds.containsKey(i)) {
                List<String> ids = posIds.get(i);
                title += " / "+StringUtil.shortenTo(StringUtil.join(ids, ", "), 30);
            }
            addRadioItem(actionName+i, title, actionName, menuName);
        }
        JMenuItem tabsPosItem = getItem(actionName+pos);
        if (tabsPosItem != null) {
            tabsPosItem.setSelected(true);
        }
    }
    
    private void addNumItem(String cmd, String label, String submenu, Map<String, Collection<DockContent>> info) {
        if (info.containsKey(cmd)) {
            int size = info.get(cmd).size();
            JMenuItem item = addItem(cmd, String.format("%s (%d)", label, size), submenu);
            if (size == 0) {
                item.setEnabled(false);
            }
        }
        else {
            addItem(cmd, label, submenu);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.tabMenuItemClicked(e, content);
        }
    }
    
}
