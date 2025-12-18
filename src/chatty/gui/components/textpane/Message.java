
package chatty.gui.components.textpane;

import chatty.gui.Highlighter.Match;
import java.util.List;

/**
 *
 * @author tduva
 */
public class Message {
    
    private static long LINE_COUNTER;
    
    public static long getLineId() {
        return LINE_COUNTER++;
    }
    
    public long lineId = getLineId();
    public final String msgId;
    public final String text;
    public final List<Match> highlightMatches;
    public final List<Match> replaceMatches;
    public final String replacement;
    
    public Message(String msgId, String text, List<Match> highlightMatches,
            List<Match> replaceMatches, String replacement) {
        this.msgId = msgId;
        this.text = text;
        this.highlightMatches = highlightMatches;
        this.replaceMatches = replaceMatches;
        this.replacement = replacement;
    }
}
