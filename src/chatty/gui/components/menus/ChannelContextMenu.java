
package chatty.gui.components.menus;

import chatty.lang.Language;
import java.awt.event.ActionEvent;


/**
 * The default Context Menu for the Channel
 * 
 * @author tduva
 */
public class ChannelContextMenu extends ContextMenu {
    
    private static final String MISC_MENU = Language.getString("channelCm.menu.misc");
    
    private final ContextMenuListener listener;
    
    public ChannelContextMenu(ContextMenuListener listener) {
        this.listener = listener;
        
        addItem("channelInfo", Language.getString("menubar.dialog.channelInfo"));
        addItem("channelAdmin", Language.getString("menubar.dialog.channelAdmin"));
        addSeparator();
        ContextMenuHelper.addStreamsOptions(this, 1, false);
        addSeparator();
        
        addItem("hostchannel", Language.getString("channelCm.hostChannel"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("joinHostedChannel", Language.getString("channelCm.joinHosted"), MISC_MENU);
        addItem("copy", Language.getString("channelCm.copyStreamname"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("chatRules", Language.getString("channelCm.dialog.chatRules"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("follow", Language.getString("channelCm.follow"), MISC_MENU);
        addItem("unfollow", Language.getString("channelCm.unfollow"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("srcOpen", Language.getString("channelCm.speedruncom"), MISC_MENU);
        
        addSeparator();
        addItem("closeChannel", Language.getString("channelCm.closeChannel"));
        
        CommandMenuItems.addCommands(CommandMenuItems.MenuType.CHANNEL, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.menuItemClicked(e);
        }
    }
    
}
