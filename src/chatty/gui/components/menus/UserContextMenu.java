
package chatty.gui.components.menus;

import chatty.Chatty;
import chatty.User;
import chatty.util.commands.CustomCommand;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;

/**
 * The Context Menu for a single User. E.g. opened when right-clicking on a user
 * in chat, on the userlist or in the user info dialog.
 * 
 * @author tduva
 */
public class UserContextMenu extends ContextMenu {
    
    private final ContextMenuListener listener;
    private final User user;
    private final String autoModMsgId;
    
    private static final String MISC_MENU = Chatty.lang.GET("USERCONTEXTMENU_MISC_MENU", "Miscellaneous");
    
    public UserContextMenu(User user, String autoModMsgId,
            ContextMenuListener listener) {
        this.listener = listener;
        this.user = user;
        this.autoModMsgId = autoModMsgId;
        
        addItem("userinfo", String.format(Chatty.lang.GET("USERCONTEXTMENU_USERINFO", "User: %s"), user.getDisplayNick()));
        addSeparator();
        ContextMenuHelper.addStreamsOptions(this, 1, false);
        addSeparator();
        addItem("join",String.format(Chatty.lang.GET("USERCONTEXTMENU_JOIN_CHANHASH", "Join #%s"), user.getName()));
        addSeparator();
        if (autoModMsgId != null) {
            addItem("autoModApprove", "Approve");
            addItem("autoModDeny", "Deny");
            addSeparator();
        }
        
        // Misc Submenu
        addItem("copyNick", "Copy Name", MISC_MENU);
        addItem("copyDisplayNick", "Copy Display Name", MISC_MENU);
        addSeparator(MISC_MENU);
        ContextMenuHelper.addIgnore(this, user.getName(), MISC_MENU, false);
        ContextMenuHelper.addIgnore(this, user.getName(), MISC_MENU, true);
        addSeparator(MISC_MENU);
        addItem("follow", "Follow", MISC_MENU);
        addItem("unfollow", "Unfollow", MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("setcolor", "Set color", MISC_MENU);
        addItem("setname", "Set name", MISC_MENU);
        
        // Get the preset categories from the addressbook, which may be empty
        // if not addressbook is set to this user
        List<String> presetCategories = user.getPresetCategories();
        if (presetCategories != null) {
            final String submenu = "Addressbook";
            
            // Get this user's categories. If this is null, then the user isn't
            // in the addressbook
            Set<String> userCategories = user.getCategories();
            
            // Add all preset categories and select them if the user has them
            for (String presetCategory : presetCategories) {
                boolean selected = userCategories != null
                        ? userCategories.contains(presetCategory) : false;
                addCheckboxItem("cat" + presetCategory, presetCategory,
                        submenu, selected);
            }
            
            // Add seperator only if any preset categories exist
            if (!presetCategories.isEmpty()) {
                addSeparator(submenu);
            }
            
            // Add "add" or "edit" buttons depending on whether the user is
            // already in the addressbook
            if (userCategories != null) {
                addItem("addressbookEdit", "Edit", submenu);
                addItem("addressbookRemove", "Remove", submenu);
            } else {
                addItem("addressbookEdit", "Add", submenu);
            }
        }

        CommandMenuItems.addCommands(CommandMenuItems.MenuType.USER, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.userMenuItemClicked(e, user, autoModMsgId);
        }
    }
}