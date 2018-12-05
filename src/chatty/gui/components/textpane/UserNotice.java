
package chatty.gui.components.textpane;

import chatty.User;
import chatty.util.StringUtil;
import chatty.util.api.Emoticons;

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
            Emoticons.TagEmotes emotes) {
        super(Type.INFO, makeFullText(type, infoText, message));
        this.type = type;
        this.user = user;
        this.attachedMessage = message;
        this.emotes = emotes;
        this.infoText = infoText;
    }
    
    private static String makeFullText(String type, String text, String message) {
        if (StringUtil.isNullOrEmpty(message)) {
            return String.format("[%s] %s", type, text);
        }
        return String.format("[%s] %s [%s]", type, text, message);
    }
    
}
