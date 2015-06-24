
package chatty.util;

import chatty.Chatty;
import chatty.Helper;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
public class TwitchEmotes {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchEmotes.class.getName());
    
    private static final String EMOTESET_URL = "http://direct.twitchemotes.com/api_cache/v2/sets.json";
    
    private static final int CACHED_EMOTICONS_EXPIRE_AFTER = 60 * 60 * 24;
    private static final String FILE = Chatty.getCacheDirectory() + "emotesets";
    
    private final TwitchEmotesListener listener;
    private final SimpleCache cache;
    private volatile boolean pendingRequest;
    
    /**
     * 
     * 
     * @param listener Cannot be null
     */
    public TwitchEmotes(TwitchEmotesListener listener) {
        this.listener = listener;
        cache = new SimpleCache("emotesets", FILE, CACHED_EMOTICONS_EXPIRE_AFTER);
    }
    
    public synchronized void requestEmotesets(boolean forcedUpdate) {
        String cached = null;
        if (!forcedUpdate) {
            cached = cache.load();
        }
        if (cached != null) {
            loadEmotesets(cached);
        } else {
            requestEmotesetsFromApi();
        }
    }
    
    private void requestEmotesetsFromApi() {
        if (pendingRequest) {
            return;
        }
        UrlRequest request = new UrlRequest(EMOTESET_URL) {
            
            @Override
            public void requestResult(String result, int responseCode) {
                if (responseCode == 200) {
                    if (loadEmotesets(result) > 0) {
                        cache.save(result);
                    }
                }
                pendingRequest = false;
            }
        };
        new Thread(request).start();
        pendingRequest = true;
    }
    
    private int loadEmotesets(String json) {
        Map<Integer, String> emotesetStreams = parseEmotesets(json);
        LOGGER.info("Found " + emotesetStreams.size() + " emotesets");
        listener.emotesetsReceived(emotesetStreams);
        return emotesetStreams.size();
    }
    
    private Map<Integer, String> parseEmotesets(String json) {
        Map<Integer, String> emotesetStreams = new HashMap<>();
        if (json == null) {
            return emotesetStreams;
        }
        JSONParser parser = new JSONParser();
        try {
            JSONObject root = (JSONObject)parser.parse(json);
            JSONObject sets = (JSONObject)root.get("sets");
            for (Object key : sets.keySet()) {
                try {
                    Integer emoteset = Integer.parseInt((String)key);
                    String stream = Helper.toStream((String)sets.get(key));
                    emotesetStreams.put(emoteset, stream);
                } catch (ClassCastException | NumberFormatException | NullPointerException ex) {
                    LOGGER.warning("Error parsing emoteset: "+key);
                }
            }
        } catch (ParseException | ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing emotesets: "+ex);
        }
        return emotesetStreams;
    }

    public static interface TwitchEmotesListener {
        public void emotesetsReceived(Map<Integer, String> emotesetStreams);
    }
    
}
