
package chatty.gui.components.textpane;

import chatty.User;

/**
 *
 * @author tduva
 */
public class AutoModMessage extends Message {

    public final User user;
    
    public AutoModMessage(User user, String text, String id) {
        super(id, text);
        this.user = user;
    }
    
}
