
package chatty.gui.components.menus;

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
    
    private static final String MISC_MENU = "Miscellaneous";
    
    public UserContextMenu(User user, ContextMenuListener listener) {
        this.listener = listener;
        this.user = user;
        
        addItem("userinfo", "User: "+user.getDisplayNick());
        addSeparator();
        ContextMenuHelper.addStreamsOptions(this, 1, false);
        addSeparator();
        addItem("join","Join #"+user.getName());
        addSeparator();
        
        // Misc Submenu
        addSubItem("copyNick", "Copy Name", MISC_MENU);
        addSubItem("copyDisplayNick", "Copy Display Name", MISC_MENU);
        addSeparator(MISC_MENU);
        ContextMenuHelper.addIgnore(this, user.getName(), MISC_MENU, false);
        ContextMenuHelper.addIgnore(this, user.getName(), MISC_MENU, true);
        addSeparator(MISC_MENU);
        addSubItem("follow", "Follow", MISC_MENU);
        addSubItem("unfollow", "Unfollow", MISC_MENU);
        addSeparator(MISC_MENU);
        addSubItem("setcolor", "Set color", MISC_MENU);
        addSubItem("setname", "Set name", MISC_MENU);
        
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
                addSubItem("addressbookEdit", "Edit", submenu);
                addSubItem("addressbookRemove", "Remove", submenu);
            } else {
                addSubItem("addressbookEdit", "Add", submenu);
            }
        }

        CommandMenuItems.addCommands(CommandMenuItems.MenuType.USER, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.userMenuItemClicked(e, user);
        }
    }
}