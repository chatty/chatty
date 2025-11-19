
package chatty.gui.components.menus;

import chatty.lang.Language;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Context menu for the Highlights/Ignored Messages dialog (also used for Stream
 * Chat dialog currently).
 * 
 * @author tduva
 */
public class HighlightsContextMenu extends ContextMenu {

    public HighlightsContextMenu(boolean isDocked, boolean autoOpen, long showLegacyValue, long channelLogoValue, long showChannelNameValue) {
        addItem("clearHighlights", Language.getString("highlightedDialog.cm.clear"));
        addSeparator();
        addCheckboxItem("dockToggleDocked", "Dock as tab", isDocked);
        addCheckboxItem("dockToggleAutoOpenActivity", "Open on message", autoOpen);
        addSeparator();
        RoutingTargetContextMenu.addChannelLogoOptions(this, channelLogoValue);
        RoutingTargetContextMenu.addShowChannelNameOptions(this, showChannelNameValue);
        addLegacyChannelNameOptions(showLegacyValue);
    }
    
    private void addLegacyChannelNameOptions(long showLegacyValue) {
        Map<Long, String> showLegacyOptions = new HashMap<>();
        showLegacyOptions.put(0L, "Off");
        showLegacyOptions.put(1L, "Show channel name on own line");
        ContextMenuHelper.addNumericOptions(this, "Legacy Channel Name", "legacyChannelName", showLegacyValue, showLegacyOptions);
        getItem("legacyChannelName1").setToolTipText("Show channel name as a separate line before messages every few messages");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
    
}
