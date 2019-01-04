
package chatty.gui.components.textpane;

import chatty.User;
import chatty.gui.Highlighter.Match;
import chatty.util.api.Emoticons;
import java.util.List;

/**
 *
 * @author tduva
 */
public class SubscriberMessage extends UserNotice {
    
    public final int months;
    
    public SubscriberMessage(User user, String text, String message, int months,
            Emoticons.TagEmotes emotes) {
        super("Notification", user, text, message, emotes);
        this.months = months;
    }
}
