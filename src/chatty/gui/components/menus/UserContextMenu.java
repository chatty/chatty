
package chatty.gui.components.menus;

import chatty.User;
import chatty.lang.Language;
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
    
    private static final String MISC_MENU = Language.getString("userCm.menu.misc");
    
    public UserContextMenu(User user, String autoModMsgId,
            ContextMenuListener listener) {
        this.listener = listener;
        this.user = user;
        this.autoModMsgId = autoModMsgId;
        
        addItem("userinfo", Language.getString("userCm.user", user.getDisplayNick()));
        addSeparator();
        ContextMenuHelper.addStreamsOptions(this, 1, false);
        addSeparator();
        addItem("join", Language.getString("userCm.join", user.getName()));
        addSeparator();
        if (autoModMsgId != null) {
            addItem("autoModApprove", "Approve");
            addItem("autoModDeny", "Deny");
            addSeparator();
        }
        
        // Misc Submenu
        addItem("copyNick", Language.getString("userCm.copyName"), MISC_MENU);
        addItem("copyDisplayNick", Language.getString("userCm.copyDisplayName"), MISC_MENU);
        addSeparator(MISC_MENU);
        ContextMenuHelper.addIgnore(this, user.getName(), MISC_MENU, false);
        ContextMenuHelper.addIgnore(this, user.getName(), MISC_MENU, true);
        addSeparator(MISC_MENU);
        addItem("follow", Language.getString("userCm.follow"), MISC_MENU);
        addItem("unfollow", Language.getString("userCm.unfollow"), MISC_MENU);
        addSeparator(MISC_MENU);
        addItem("setcolor", Language.getString("userCm.setColor"), MISC_MENU);
        addItem("setname", Language.getString("userCm.setName"), MISC_MENU);
        
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