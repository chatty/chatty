
package chatty.gui.components.textpane;

import chatty.gui.Highlighter.Match;
import java.util.List;

/**
 *
 * @author tduva
 */
public class Message {
    
    public final String id;
    public final String text;
    public final List<Match> highlightMatches;
    
    public Message(String id, String text, List<Match> highlightMatches) {
        this.id = id;
        this.text = text;
        this.highlightMatches = highlightMatches;
    }
}
