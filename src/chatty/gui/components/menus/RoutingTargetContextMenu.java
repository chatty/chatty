
package chatty.gui.components.menus;

import chatty.gui.DockedDialogHelper;
import chatty.gui.components.Channel;
import chatty.gui.components.routing.RoutingTargetSettings;
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
                                    String currentChannel,
                                    int channelLogo) {
        
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
        
        final String logoSubmenu = "Channel Logos";
        int defaultSize = RoutingTargetSettings.CHANNEL_LOGO_DEFAULT;
        int currentSize = channelLogo;
        for (int i=30;i>10;i -= 2) {
            String action = "logoSize"+i;
            if (i == defaultSize) {
                addRadioItem(action, i+"px (default)", logoSubmenu, logoSubmenu);
            }
            else {
                addRadioItem(action, i+"px", logoSubmenu, logoSubmenu);
            }
            if (i == currentSize) {
                getItem(action).setSelected(true);
            }
        }
        addSeparator(logoSubmenu);
        addRadioItem("logoSize0", "Off", logoSubmenu, logoSubmenu);
        if (currentSize == 0) {
            getItem("logoSize0").setSelected(true);
        }
        
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
    
}
