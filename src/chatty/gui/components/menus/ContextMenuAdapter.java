
package chatty.gui.components.menus;

import chatty.Room;
import chatty.User;
import chatty.gui.components.Channel;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.StreamInfo;
import chatty.util.api.usericons.Usericon;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 *
 * @author tduva
 */
public class ContextMenuAdapter implements ContextMenuListener {

    @Override
    public void userMenuItemClicked(ActionEvent e, User user, String msgId, String autoModMsgId) {

    }

    @Override
    public void urlMenuItemClicked(ActionEvent e, String url) {

    }

    @Override
    public void menuItemClicked(ActionEvent e) {

    }

    @Override
    public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams) {

    }

    @Override
    public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos) {

    }

    @Override
    public void emoteMenuItemClicked(ActionEvent e, EmoticonImage emote) {
    }

    @Override
    public void usericonMenuItemClicked(ActionEvent e, Usericon usericon) {
    }

    @Override
    public void roomsMenuItemClicked(ActionEvent e, Collection<Room> rooms) {
    }

    @Override
    public void channelMenuItemClicked(ActionEvent e, Channel channel) {
    }

    @Override
    public void textMenuItemClick(ActionEvent e, String selected) {
    }
    
}
