
package chatty.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import javax.swing.Timer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class Pronouns {
    
    private static final Logger LOGGER = Logger.getLogger(Pronouns.class.getName());
    
    private static final Object LOCK = new Object();
    
    // Should be handled as immutable
    private volatile Map<String, String> pronouns = new HashMap<>();
    private final CachedBulkManager<String, String> data;
    private volatile static Pronouns instance;
    
    public static Pronouns instance() {
        synchronized(LOCK) {
            if (instance == null) {
                instance = new Pronouns();
            }
            return instance;
        }
    }
    
    public Pronouns() {
        data = new CachedBulkManager<>(new CachedBulkManager.Requester<String, String>() {
            @Override
            public void request(CachedBulkManager<String, String> manager, Set<String> asap, Set<String> normal, Set<String> backlog) {
                String username = manager.makeAndSetRequested(asap, normal, backlog, 1).iterator().next();
                UrlRequest request = new UrlRequest("https://pronouns.alejo.io/api/users/" + username);
                request.async((result, responseCode) -> {
                    if (responseCode == 404) {
                        manager.setNotFound(username);
                    } else if (result == null) {
                        manager.setError(username);
                    } else {
                        String pronoun_id = parseUser(result);
                        if (pronoun_id == null) {
                            manager.setNotFound(username);
                        }
                        else {
                            manager.setResult(username, pronoun_id);
                        }
                    }
                });
            }
        }, CachedBulkManager.DAEMON);
        requestPronouns();
    }
    
    private final Object UNIQUE = new Object();
    
    /**
     * Previous request is always overwritten (UNIQUE). If it should be used
     * from other stuff than just the User Dialog, this may need to change.
     * 
     * @param listener
     * @param username 
     */
    public void getUser(BiConsumer<String, String> listener, String username) {
        data.query(UNIQUE, (CachedBulkManager.Result<String, String> result) -> {
            if (pronouns.isEmpty()) {
                // If other request isn't finished yet, wait a bit
                Timer timer = new Timer(1000, e -> sendResult(listener, username, result));
                timer.setRepeats(false);
                timer.start();
            }
            else {
                sendResult(listener, username, result);
            }
        }, CachedBulkManager.ASAP, username);
    }
    
    private void sendResult(BiConsumer<String, String> listener, String username, CachedBulkManager.Result<String, String> result) {
        String r = pronouns.get(result.get(username));
        if (r != null) {
            listener.accept(username, r);
        }
    }
    
    private void requestPronouns() {
        UrlRequest request = new UrlRequest("https://pronouns.alejo.io/api/pronouns");
        request.async((result, responseCode) -> {
            if (result != null) {
                Map<String, String> parsed = parsePronouns(result);
                if (!parsed.isEmpty()) {
                    pronouns = parsed;
                }
            }
        });
    }
    
    private Map<String, String> parsePronouns(String json) {
        Map<String, String> result = new HashMap<>();
        try {
            JSONParser parser = new JSONParser();
            JSONArray root = (JSONArray)parser.parse(json);
            for (Object o : root) {
                JSONObject entry = (JSONObject)o;
                String name = JSONUtil.getString(entry, "name");
                String display = JSONUtil.getString(entry, "display");
                if (name != null && display != null) {
                    result.put(name, display);
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing pronouns: "+ex);
        }
        return result;
    }
    
    private String parseUser(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONArray root = (JSONArray)parser.parse(json);
            if (!root.isEmpty()) {
                return JSONUtil.getString((JSONObject) root.get(0), "pronoun_id");
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing pronouns: "+ex);
        }
        return null;
    }
    
}
