
package chatty.gui.components.menus;

import chatty.Room;
import chatty.User;
import chatty.gui.components.Channel;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.StreamInfo;
import chatty.util.api.usericons.Usericon;
import chatty.util.dnd.DockContent;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 *
 * @author tduva
 */
public interface ContextMenuListener {
    public void userMenuItemClicked(ActionEvent e, User user, String msgId, String autoModMsgId);
    public void urlMenuItemClicked(ActionEvent e, String url);
    public void menuItemClicked(ActionEvent e);
    public void textMenuItemClick(ActionEvent e, String selected);
    public void roomsMenuItemClicked(ActionEvent e, Collection<Room> rooms);
    public void channelMenuItemClicked(ActionEvent e, Channel channel);
    public void tabMenuItemClicked(ActionEvent e, DockContent content);
    public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams);
    public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos);
    public void emoteMenuItemClicked(ActionEvent e, EmoticonImage emote);
    public void usericonMenuItemClicked(ActionEvent e, Usericon usericon);
}
