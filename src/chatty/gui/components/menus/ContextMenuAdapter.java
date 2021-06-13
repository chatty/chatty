
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
public class ContextMenuAdapter implements ContextMenuListener {

    private final ContextMenuListener listener;
    
    public ContextMenuAdapter() {
        this.listener = null;
    }
    
    /**
     * 
     * @param listener ContextMenuListener to forward events to by default
     */
    public ContextMenuAdapter(ContextMenuListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void userMenuItemClicked(ActionEvent e, User user, String msgId, String autoModMsgId) {
        if (listener != null) {
            listener.userMenuItemClicked(e, user, msgId, autoModMsgId);
        }
    }

    @Override
    public void urlMenuItemClicked(ActionEvent e, String url) {
        if (listener != null) {
            listener.urlMenuItemClicked(e, url);
        }
    }

    @Override
    public void menuItemClicked(ActionEvent e) {
        if (listener != null) {
            listener.menuItemClicked(e);
        }
    }

    @Override
    public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams) {
        if (listener != null) {
            listener.streamsMenuItemClicked(e, streams);
        }
    }

    @Override
    public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos) {
        if (listener != null) {
            listener.streamInfosMenuItemClicked(e, streamInfos);
        }
    }

    @Override
    public void emoteMenuItemClicked(ActionEvent e, EmoticonImage emote) {
        if (listener != null) {
            listener.emoteMenuItemClicked(e, emote);
        }
    }

    @Override
    public void usericonMenuItemClicked(ActionEvent e, Usericon usericon) {
        if (listener != null) {
            listener.usericonMenuItemClicked(e, usericon);
        }
    }

    @Override
    public void roomsMenuItemClicked(ActionEvent e, Collection<Room> rooms) {
        if (listener != null) {
            listener.roomsMenuItemClicked(e, rooms);
        }
    }

    @Override
    public void channelMenuItemClicked(ActionEvent e, Channel channel) {
        if (listener != null) {
            listener.channelMenuItemClicked(e, channel);
        }
    }

    @Override
    public void textMenuItemClick(ActionEvent e, String selected) {
        if (listener != null) {
            listener.textMenuItemClick(e, selected);
        }
    }

    @Override
    public void tabMenuItemClicked(ActionEvent e, DockContent content) {
        if (listener != null) {
            listener.tabMenuItemClicked(e, content);
        }
    }
    
}
