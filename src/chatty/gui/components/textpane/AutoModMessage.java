
package chatty.gui.components.textpane;

import chatty.User;
import chatty.gui.Highlighter.Match;
import java.util.List;

/**
 *
 * @author tduva
 */
public class AutoModMessage extends Message {

    public final User user;
    
    public AutoModMessage(User user, String text, String id, List<Match> highlightMatches) {
        super(id, text, highlightMatches, null, null);
        this.user = user;
    }
    
}
