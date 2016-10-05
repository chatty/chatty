
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
        
        if (usericon.metaTitle.isEmpty()) {
            addItem("", "Badge: "+usericon.type.label, ContextMenuHelper.ICON_SPACING);
        } else {
            addItem("", "Badge: "+usericon.metaTitle, ContextMenuHelper.ICON_SPACING);
            if (!usericon.metaTitle.equals(usericon.metaDescription) && !usericon.metaDescription.isEmpty()) {
                addItem("", StringUtil.shortenTo(usericon.metaDescription, 30));
            }
        }
        if (usericon.source == Usericon.SOURCE_CUSTOM) {
            addItem("", "Custom Usericon");
        }
        if (!usericon.metaUrl.isEmpty()) {
            addSeparator();
            addItem("usericonUrl", "Click for info", ICON_WEB);
        }
        if (Chatty.DEBUG) {
            addItem("", usericon.toString());
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.usericonMenuItemClicked(e, usericon);
        }
    }
    
}
