
package chatty.util.history;

import chatty.User;
import chatty.util.irc.MsgTags;

/**
 *
 * @author tduva
 */
public class QueuedMessage {

    public final User user;
    public final String text;
    public final boolean action;
    public final MsgTags tags;

    public QueuedMessage(User user, String text, boolean action, MsgTags tags) {
        this.user = user;
        this.text = text;
        this.action = action;
        this.tags = tags;
    }

}
