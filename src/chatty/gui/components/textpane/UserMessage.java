
package chatty.gui.components.textpane;

import chatty.User;
import chatty.util.api.Emoticons;
import java.awt.Color;

/**
 * A single chat message, containing all the metadata.
 * 
 * @author tduva
 */
public class UserMessage extends Message {
    
    public final User user;
    public final Emoticons.TagEmotes emotes;
    public final int bits;
    public Color color;
    public boolean whisper;
    public boolean highlighted;
    public boolean ignored_compact;
    public boolean action;
    
    public UserMessage(User user, String text, Emoticons.TagEmotes emotes,
            String id, int bits) {
        super(id, text);
        this.user = user;
        this.emotes = emotes;
        this.bits = bits;
    }
}
