
package chatty.gui.components.menus;

import chatty.Chatty;
import static chatty.gui.components.menus.ContextMenuHelper.ICON_WEB;
import chatty.util.StringUtil;
import chatty.util.api.CachedImage;
import chatty.util.api.usericons.Usericon;
import java.awt.event.ActionEvent;

/**
 *
 * @author tduva
 */
public class UsericonContextMenu extends ContextMenu {

    private final ContextMenuListener listener;
    private final CachedImage<Usericon> usericonImage;
    
    public UsericonContextMenu(CachedImage<Usericon> usericonImage, ContextMenuListener listener) {
        this.listener = listener;
        this.usericonImage = usericonImage;
        Usericon usericon = usericonImage.getObject();
        
        //--------------------
        // General Description
        //--------------------
        if (usericon.metaTitle.isEmpty()) {
            addItem("", "Badge: "+usericon.type.label);
        } else {
            addItem("", "Badge: "+usericon.metaTitle);
            if (!usericon.metaTitle.equals(usericon.metaDescription) && !usericon.metaDescription.isEmpty()) {
                addItem("", StringUtil.shortenTo(usericon.metaDescription, 30));
            }
        }
        addItem("badgeImage", usericonImage.getSizeString(), ContextMenuHelper.ICON_IMAGE);
        
        if (usericon.source != Usericon.SOURCE_CUSTOM) {
            addSeparator();
            addItem("hideUsericonOfBadgeType", "Hide badges of this type");
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
        if (usericon.source != Usericon.SOURCE_CUSTOM) {
            if (usericon.badgeType.isEmpty()) {
                addItem("addUsericonOfBadgeType", "Override/Hide (" + usericon.type.label + ")", infoMenu);
            }
            else {
                addSeparator(infoMenu);
                addItem("addUsericonOfBadgeType", "Override/Hide (" + usericon.badgeType + ")", infoMenu);
                addItem("addUsericonOfBadgeTypeAllVariants", "Override/Hide (all " + usericon.badgeType.id + " variants)", infoMenu);
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
            listener.usericonMenuItemClicked(e, usericonImage);
        }
    }
    
}
