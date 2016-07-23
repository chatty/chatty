
package chatty.gui.components.textpane;

import chatty.User;
import chatty.util.api.Emoticons;

/**
 *
 * @author tduva
 */
public class SubscriberMessage extends Message {
    
    public final User user;
    public final String attachedMessage;
    public final int months;
    public final Emoticons.TagEmotes emotes;
    
    public SubscriberMessage(User user, String text, String message, int months,
            Emoticons.TagEmotes emotes, String id) {
        super(id, text);
        this.user = user;
        this.attachedMessage = message;
        this.months = months;
        this.emotes = emotes;
    }
}
