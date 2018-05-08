
package chatty.gui.components.menus;

import chatty.Chatty;
import static chatty.gui.components.menus.ContextMenuHelper.ICON_WEB;
import chatty.util.StringUtil;
import chatty.util.api.usericons.Usericon;
import java.awt.event.ActionEvent;

/**
 *
 * @author tduva
 */
public class UsericonContextMenu extends ContextMenu {

    private final ContextMenuListener listener;
    private final Usericon usericon;
    
    public UsericonContextMenu(Usericon usericon, ContextMenuListener listener) {
        this.listener = listener;
        this.usericon = usericon;
        
        //--------------------
        // General Description
        //--------------------
        if (usericon.metaTitle.isEmpty()) {
            addItem("badgeImage", "Badge: "+usericon.type.label, ContextMenuHelper.ICON_IMAGE);
        } else {
            addItem("badgeImage", "Badge: "+usericon.metaTitle, ContextMenuHelper.ICON_IMAGE);
            if (!usericon.metaTitle.equals(usericon.metaDescription) && !usericon.metaDescription.isEmpty()) {
                addItem("", StringUtil.shortenTo(usericon.metaDescription, 30));
            }
        }
        
        //--------
        // Submenu
        //--------
        String infoMenu = "More..";
        if (usericon.source == Usericon.SOURCE_CUSTOM) {
            infoMenu = "Custom Usericon";
        }
        if (!usericon.badgeType.isEmpty()) {
            addItem("copyBadgeType", "ID/Version: "+usericon.badgeType.toString(), -1, infoMenu, ContextMenuHelper.ICON_COPY);
        }
        if (usericon.restriction != null) {
            if (usericon.restriction.isEmpty()) {
                addItem("", "No Restriction", infoMenu);
            } else {
                addItem("", "Restriction: "+usericon.restriction, infoMenu);
            }
        }
        if (!usericon.channelRestriction.isEmpty()) {
            addItem("", "Channel: "+usericon.channelRestriction, infoMenu);
        }
        if (!usericon.badgeType.isEmpty()) {
            if (usericon.source == Usericon.SOURCE_TWITCH2
                    || usericon.source == Usericon.SOURCE_OTHER) {
                // Only show add options if original Twitch emote (custom emote
                // would already be added)
                addSeparator(infoMenu);
                addItem("addUsericonOfBadgeType", "Override/Hide ("+usericon.badgeType+")", infoMenu);
                addItem("addUsericonOfBadgeTypeAllVariants", "Override/Hide (all "+usericon.badgeType.id+" variants)", infoMenu);
            }
        }
        
        //---------
        // Meta URL
        //---------
        if (!usericon.metaUrl.isEmpty()) {
            addSeparator();
            addItem("usericonUrl", "Click for info", ICON_WEB);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.usericonMenuItemClicked(e, usericon);
        }
    }
    
}
