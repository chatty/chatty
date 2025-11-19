
package chatty.gui.components.menus;

import chatty.gui.DockedDialogHelper;
import chatty.gui.components.Channel;
import static chatty.gui.components.menus.ContextMenuHelper.addNumericOptions;
import chatty.gui.components.routing.RoutingTargetSettings;
import chatty.gui.components.settings.RoutingSettingsTable;
import chatty.lang.Language;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

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
                                    String currentChannel,
                                    int channelLogoValue,
                                    int showChannelName) {
        
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
        addSeparator();
        addChannelLogoOptions(this, channelLogoValue);
        addShowChannelNameOptions(this, showChannelName);
    }
    
    public static void addChannelLogoOptions(ContextMenu menu, long channelLogoValue) {
        addNumericOptions(menu,
                          Language.getString("settings.label.routingTargets.channelLogo").replace(":", ""),
                          "logoSize",
                          channelLogoValue,
                          RoutingSettingsTable.makeChannelLogoValues());
    }
    
    public static void addShowChannelNameOptions(ContextMenu menu, long showChannelName) {
        addNumericOptions(menu,
                          Language.getString("settings.label.routingTargets.showChannelName").replace(":", ""),
                          "showChannelName",
                          showChannelName,
                          RoutingSettingsTable.makeChannelNameValues());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
    
}
