
package chatty.gui.components.menus;

import chatty.Chatty;
import java.awt.event.ActionEvent;


/**
 * The default Context Menu for the Channel
 * 
 * @author tduva
 */
public class ChannelContextMenu extends ContextMenu {
    
    private static final String MISC_MENU = Chatty.lang.GET("CHANNELCONTEXTMENU_MISC_MENU", "Miscellaneous");
    
    private final ContextMenuListener listener;
    
    public ChannelContextMenu(ContextMenuListener listener) {
        this.listener = listener;
        
        addItem("channelInfo", Chatty.lang.GET("CHANNELCONTEXTMENU_CHANNELINFO", "Channel Info"));
        addItem("channelAdmin", Chatty.lang.GET("CHANNELCONTEXTMENU_CHANNELADMIN", "Channel Admin"));
        addSeparator();
        ContextMenuHelper.addStreamsOptions(this, 1, false);
        addSeparator();
        
        addItem("hostchannel", Chatty.lang.GET("CHANNELCONTEXTMENU_HOSTCHANNEL", "Host Channel"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("joinHostedChannel", Chatty.lang.GET("CHANNELCONTEXTMENU_JOINHOSTEDCHAN", "Join Hosted Channel"), MISC_MENU);
        addItem("copy", Chatty.lang.GET("CHANNELCONTEXTMENU_COPYSTREAMNAME", "Copy Stream Name"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("chatRules", Chatty.lang.GET("CHANNELCONTEXTMENU_SHOWCHATRULES", "Show Chat Rules"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("follow", Chatty.lang.GET("CHANNELCONTEXTMENU_FOLLOWCHANNEL", "Follow Channel"), MISC_MENU);
        addItem("unfollow", Chatty.lang.GET("CHANNELCONTEXTMENU_UNFOLLOWCHANNEL", "Unfollow Channel"), MISC_MENU);
        
        addSeparator();
        addItem("closeChannel", Chatty.lang.GET("CHANNELCONTEXTMENU_CLOSECHANNEL", "Close Channel"));
        
        CommandMenuItems.addCommands(CommandMenuItems.MenuType.CHANNEL, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.menuItemClicked(e);
        }
    }
    
}
