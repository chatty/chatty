
package chatty.util;

import chatty.Usericon;
import chatty.util.api.Emoticon;
import java.util.List;
import java.util.Set;

/**
 *
 * @author tduva
 */
public interface FrankerFaceZListener {
    public void channelEmoticonsReceived(Set<Emoticon> newEmotes);
    public void usericonsReceived(List<Usericon> icons);
    public void botNamesReceived(Set<String> botNames);
}
