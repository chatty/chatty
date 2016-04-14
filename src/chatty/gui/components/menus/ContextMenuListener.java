
package chatty.gui.components.menus;

import chatty.User;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.StreamInfo;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author tduva
 */
public interface ContextMenuListener {
    public void userMenuItemClicked(ActionEvent e, User user);
    public void urlMenuItemClicked(ActionEvent e, String url);
    public void menuItemClicked(ActionEvent e);
    public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams);
    public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos);
    public void emoteMenuItemClicked(ActionEvent e, EmoticonImage emote);
}
