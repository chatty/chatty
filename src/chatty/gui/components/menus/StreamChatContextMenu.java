
package chatty.gui.components.menus;

import chatty.TwitchClient;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author tduva
 */
public class StreamChatContextMenu extends ContextMenu {
    
    public static TwitchClient client;
    
    private static final String CHANNEL_SETTING = "streamChatChannels";
    private static final String LOGO_SETTING = "streamChatLogos";

    public StreamChatContextMenu(boolean isDocked) {
        addItem("clearHighlights", Language.getString("highlightedDialog.cm.clear"));
        addSeparator();
        List<String> channels = new ArrayList<>();
        List<String> enabledChannels = client.settings.getList(CHANNEL_SETTING);
        
        // Add enabled first
        channels.addAll(enabledChannels);
        // Add open, if not added yet
        addChans(channels, client.getOpenChannels());
        
        Collections.sort(channels);
        for (String chan : channels) {
            addCheckboxItem("toggleChannel."+chan, chan, "Enabled Channels", enabledChannels.contains(chan));
        }
        
        final String logoSubmenu = "Channel Logos";
        int defaultSize = Integer.parseInt(client.settings.getStringDefault(LOGO_SETTING));
        int currentSize = Integer.parseInt(client.settings.getString(LOGO_SETTING));
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
        addRadioItem("logoOff", "Off", logoSubmenu, logoSubmenu);
        if (currentSize == 0) {
            getItem("logoOff").setSelected(true);
        }
        addSeparator(logoSubmenu);
        addItem("logoReadme", "Readme", logoSubmenu);
        
        addSeparator();
        addCheckboxItem("dockToggleDocked", "Dock as tab", isDocked);
    }
    
    private static void addChans(List<String> chans, Collection<String> add) {
        for (String chan : add) {
            chan = StringUtil.toLowerCase(chan);
            if (!chans.contains(chan)) {
                chans.add(chan);
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().startsWith("toggleChannel.")) {
            String channel = e.getActionCommand().substring("toggleChannel.".length());
            if (!client.settings.listContains(CHANNEL_SETTING, channel)) {
                client.settings.listAdd(CHANNEL_SETTING, channel);
            }
            else {
                client.settings.listRemove(CHANNEL_SETTING, channel);
            }
            client.settings.setSettingChanged(CHANNEL_SETTING);
        }
        else if (e.getActionCommand().startsWith("logoSize")) {
            client.settings.setString(LOGO_SETTING, e.getActionCommand().substring("logoSize".length()));
        }
        else if (e.getActionCommand().equals("logoOff")) {
            client.settings.setString(LOGO_SETTING, "0");
        }
        else if (e.getActionCommand().equals("logoReadme")) {
            JOptionPane.showMessageDialog(this, "Note that the channel logos are only visible for channels that have been live this session.\nSetting changes only apply to new messages.");
        }
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
    
}
