
package chatty.gui.components.menus;

import chatty.User;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.StreamInfo;
import chatty.util.api.usericons.Usericon;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 *
 * @author tduva
 */
public interface ContextMenuListener {
    public void userMenuItemClicked(ActionEvent e, User user, String msgId);
    public void urlMenuItemClicked(ActionEvent e, String url);
    public void menuItemClicked(ActionEvent e);
    public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams);
    public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos);
    public void emoteMenuItemClicked(ActionEvent e, EmoticonImage emote);
    public void usericonMenuItemClicked(ActionEvent e, Usericon usericon);
}
