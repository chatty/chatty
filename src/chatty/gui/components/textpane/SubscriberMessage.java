
package chatty.gui.components.textpane;

import chatty.User;
import chatty.util.api.Emoticons;

/**
 *
 * @author tduva
 */
public class SubscriberMessage extends UserNotice {
    
    public final int months;
    
    public SubscriberMessage(User user, String text, String message, int months,
            Emoticons.TagEmotes emotes, String id) {
        super("Notification", user, text, message, emotes, id);
        this.months = months;
    }
}
