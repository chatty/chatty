
package chatty.gui;

import chatty.User;
import chatty.util.api.Emoticon;
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
    public void userClicked(User user, MouseEvent e);
    public void emoteClicked(Emoticon emote, MouseEvent e);
}
