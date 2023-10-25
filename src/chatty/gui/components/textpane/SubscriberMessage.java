
package chatty.gui.components.textpane;

import chatty.User;
import chatty.util.irc.MsgTags;

/**
 * Historically extra class, but also used to check for type.
 * 
 * @author tduva
 */
public class SubscriberMessage extends UserNotice {
    
    public SubscriberMessage(User user, String text, String message,
            MsgTags tags) {
        super("Notification", user, text, message, tags);
    }
    
    public SubscriberMessage(SubscriberMessage other) {
        super(other);
    }
    
    @Override
    public SubscriberMessage copy() {
        return new SubscriberMessage(this);
    }
    
}
