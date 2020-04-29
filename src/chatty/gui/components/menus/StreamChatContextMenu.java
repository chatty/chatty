
package chatty.gui.components.menus;

import chatty.TwitchClient;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author tduva
 */
public class StreamChatContextMenu extends ContextMenu {
    
    public static TwitchClient client;
    
    private static final String CHANNEL_SETTING = "streamChatChannels";

    public StreamChatContextMenu() {
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
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
    
}
