
package chatty.util.api;

import chatty.Helper;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class CheerEmoticonManager2 {
    
    private final TwitchApi api;
    private final TwitchApiResultListener listener;
    
    private final Set<String> alreadyRequested = new HashSet<>();
    
    public CheerEmoticonManager2(TwitchApi api, TwitchApiResultListener listener) {
        this.api = api;
        this.listener = listener;
    }
    
    public void request(String stream, boolean force) {
        stream = Helper.toStream(stream);
        if (!Helper.isValidStream(stream)) {
            return;
        }
        synchronized(alreadyRequested) {
            if (!alreadyRequested.contains(stream) || force) {
                alreadyRequested.add(stream);
                request(stream);
            }
        }
    }
    
    private void request(String stream) {
        api.userIDs.getUserIDs(r -> {
            if (r.hasError()) {
                
            } else {
                api.requests.requestCheerEmoticons(r.getId(stream), stream);
            }
        }, stream);
    }
    
    protected void dataReceived(String result, String stream, String channelId) {
        Set<CheerEmoticon> parsed = CheerEmoticonManager.parse(result, stream);
        if (parsed != null && !parsed.isEmpty()) {
            listener.receivedCheerEmoticons(parsed);
        }
    }
    
}
