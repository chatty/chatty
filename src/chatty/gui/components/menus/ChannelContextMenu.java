
package chatty.gui.components.menus;

import chatty.Helper;
import chatty.gui.components.Channel;
import chatty.lang.Language;
import chatty.util.commands.Parameters;
import java.awt.event.ActionEvent;


/**
 * The default Context Menu for the Channel
 * 
 * @author tduva
 */
public class ChannelContextMenu extends ContextMenu {
    
    private static final String MISC_MENU = Language.getString("channelCm.menu.misc");
    
    private final ContextMenuListener listener;
    
    private final Channel channel;
    
    public ChannelContextMenu(ContextMenuListener listener, Channel channel) {
        this.listener = listener;
        this.channel = channel;
        
        Parameters parameters = Helper.createRoomParameters(channel.getRoom());
        
        addItem("channelInfo", Language.getString("menubar.dialog.channelInfo"));
        addItem("channelAdmin", Language.getString("menubar.dialog.channelAdmin"));
        addSeparator();
        ContextMenuHelper.addStreamsOptions(this, 1, false, parameters);
        addSeparator();
        
        addItem("raidchannel", Language.getString("channelCm.raidChannel"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("copy", Language.getString("channelCm.copyStreamname"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("srcOpen", Language.getString("channelCm.speedruncom"), MISC_MENU);
        
        addSeparator();
        addItem("closeChannel", Language.getString("channelCm.closeChannel"));
        
        CommandMenuItems.addCommands(CommandMenuItems.MenuType.CHANNEL, this, parameters);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.channelMenuItemClicked(e, channel);
        }
    }
    
}
