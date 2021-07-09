
package chatty.util;

import chatty.util.api.EmoticonUpdate;
import java.util.Set;

/**
 * Receive requested emoticons and related stuff.
 * 
 * @author tduva
 */
public interface EmoticonListener {
    void receivedEmoticons(EmoticonUpdate emotesUpdate);
    void receivedBotNames(String stream, Set<String> names);
}
