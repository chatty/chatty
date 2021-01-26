
package chatty.gui.components.textpane;

import chatty.User;
import chatty.gui.Highlighter.Match;
import chatty.util.api.Emoticons;
import chatty.util.irc.MsgTags;
import java.awt.Color;
import java.util.List;

/**
 * A single chat message, containing all the metadata.
 * 
 * @author tduva
 */
public class UserMessage extends Message {
    
    public final User user;
    public final Emoticons.TagEmotes emotes;
    public final int bits;
    public final MsgTags tags;
    public Color color;
    public Color backgroundColor;
    public boolean whisper;
    public boolean highlighted;
    public boolean ignored_compact;
    public boolean action;
    public Object colorSource;
    public Object highlightSource;
    
    public UserMessage(User user, String text, Emoticons.TagEmotes emotes,
            String id, int bits, List<Match> highlightMatches,
            List<Match> replaceMatches, String replacement, MsgTags tags) {
        super(id, text, highlightMatches, replaceMatches, replacement);
        this.user = user;
        this.emotes = emotes;
        this.bits = bits;
        this.tags = tags;
    }
}
