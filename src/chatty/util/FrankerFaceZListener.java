
package chatty.util;

import chatty.Usericon;
import chatty.util.api.EmoticonUpdate;
import java.util.List;
import java.util.Set;

/**
 *
 * @author tduva
 */
public interface FrankerFaceZListener {
    public void channelEmoticonsReceived(EmoticonUpdate emotes);
    public void usericonsReceived(List<Usericon> icons);
    public void botNamesReceived(Set<String> botNames);
}
