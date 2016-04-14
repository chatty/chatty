
package chatty.util;

import chatty.util.api.Emoticon;
import java.util.Set;

/**
 * Receive requested emoticons and related stuff.
 * 
 * @author tduva
 */
public interface EmoticonListener {
    void receivedEmoticons(Set<Emoticon> emoticons);
    void receivedBotNames(String stream, Set<String> names);
}
