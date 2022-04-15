
package chatty.gui.components.textpane;

import chatty.User;

/**
 *
 * @author tduva
 */
public class AutoModMessage extends InfoMessage {

    public final User user;
    public final String msgId;
    public final String message;
    
    private final int msgStart;
    private final int msgEnd;
    
    public AutoModMessage(User user, String message, String id) {
        super(Type.INFO, makeText(user, message));
        this.user = user;
        this.msgId = id;
        this.message = message;
        this.msgStart = text.length() - message.length();
        this.msgEnd = text.length();
    }
    
    @Override
    public int getMsgStart() {
        return msgStart;
    }
    
    @Override
    public int getMsgEnd() {
        return msgEnd;
    }
    
    private static String makeText(User user, String message) {
        return String.format("[AutoMod] <%s> %s",
                user.getDisplayNick(),
                message);
    }
    
}
