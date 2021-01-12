
package chatty.gui;

import chatty.User;
import chatty.gui.components.Channel;
import chatty.util.api.Emoticon;
import chatty.util.api.usericons.Usericon;
import java.awt.event.MouseEvent;

/**
 *
 * @author tduva
 */
public interface UserListener {
    
    /**
     * 
     * @param user
     * @param e Can be null
     */
    public void userClicked(User user, String messageId, String autoModMsgId, MouseEvent e);
    public void emoteClicked(Emoticon emote, MouseEvent e);
    public void usericonClicked(Usericon usericon, MouseEvent e);
    public void linkClicked(Channel channel, String link);

}
