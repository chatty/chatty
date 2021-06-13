
package chatty.gui.components.textpane;

import chatty.User;
import chatty.util.StringUtil;
import chatty.util.api.Emoticons;
import chatty.util.irc.MsgTags;

/**
 *
 * @author tduva
 */
public class UserNotice extends InfoMessage {
    
    public final String type;
    public final User user;
    public final String attachedMessage;
    public final Emoticons.TagEmotes emotes;
    public final String infoText;
    
    public UserNotice(String type, User user, String infoText, String message,
            MsgTags tags) {
        super(Type.INFO, makeFullText(type, infoText, message), tags);
        this.type = type;
        this.user = user;
        this.attachedMessage = message;
        this.emotes = Emoticons.parseEmotesTag(tags.getRawEmotes());
        this.infoText = infoText;
    }
    
    /**
     * Create a new UserNotice similiar to the given one, but with different
     * tags.
     *
     * @param other
     * @param tags 
     */
    public UserNotice(UserNotice other, MsgTags tags) {
        this(other.type, other.user, other.infoText, other.attachedMessage, tags);
    }
    
    private static String makeFullText(String type, String text, String message) {
        if (StringUtil.isNullOrEmpty(message)) {
            return String.format("[%s] %s", type, text);
        }
        return String.format("[%s] %s [%s]", type, text, message);
    }
    
}
