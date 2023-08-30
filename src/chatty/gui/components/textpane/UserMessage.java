
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
    public Object ignoreSource;
    public Object routingSource;
    public User localUser;
    
    public UserMessage(User user, String text, Emoticons.TagEmotes emotes,
            String id, int bits, List<Match> highlightMatches,
            List<Match> replaceMatches, String replacement, MsgTags tags) {
        super(id, text, highlightMatches, replaceMatches, replacement);
        this.user = user;
        this.emotes = emotes;
        this.bits = bits;
        this.tags = tags;
    }
    
    public UserMessage copy() {
        UserMessage result = new UserMessage(user, text, emotes, id, bits, highlightMatches, replaceMatches, replacement, tags);
        result.color = color;
        result.backgroundColor = backgroundColor;
        result.whisper = whisper;
        result.highlighted = highlighted;
        result.ignored_compact = ignored_compact;
        result.action = action;
        result.colorSource = colorSource;
        result.highlightSource = highlightSource;
        result.ignoreSource = ignoreSource;
        result.routingSource = routingSource;
        result.localUser = localUser;
        return result;
    }
    
}
