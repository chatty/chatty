
package chatty.gui.components.menus;

import java.awt.event.ActionEvent;


/**
 * The default Context Menu for the Channel
 * 
 * @author tduva
 */
public class ChannelContextMenu extends ContextMenu {
    
    private static final String MISC_MENU = "Miscellaneous";
    
    private final ContextMenuListener listener;
    
    public ChannelContextMenu(ContextMenuListener listener) {
        this.listener = listener;
        
        addItem("channelInfo", "Channel Info");
        addItem("channelAdmin", "Channel Admin");
        addSeparator();
        ContextMenuHelper.addStreamsOptions(this, 1, false);
        addSeparator();
        
        addItem("hostchannel", "Host Channel", MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("joinHostedChannel", "Join Hosted Channel", MISC_MENU);
        addItem("copy", "Copy Stream Name", MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("chatRules", "Show Chat Rules", MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("follow", "Follow Channel", MISC_MENU);
        addItem("unfollow", "Unfollow Channel", MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("srcOpen", "Open Speedrun.com", MISC_MENU);
        
        addSeparator();
        addItem("closeChannel", "Close Channel");
        
        CommandMenuItems.addCommands(CommandMenuItems.MenuType.CHANNEL, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.menuItemClicked(e);
        }
    }
    
}
