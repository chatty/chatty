
package chatty.util;

import chatty.Chatty;
import chatty.Helper;
import chatty.util.api.CachedManager;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import javax.swing.Timer;
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
    private volatile Map<String, Pronoun> pronouns = new HashMap<>();
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
    
    private static final String NOT_FOUND = "__EMPTY_RESULT__";
    
    private static final String CACHE_FILE1 = Chatty.getPath(Chatty.PathType.CACHE).resolve("pronouns1").toString();
    public static final int CACHE_EXPIRES_AFTER = 60*60*24;
    
    private static final Path CACHE_FILE2 = Chatty.getPath(Chatty.PathType.CACHE).resolve("pronouns2");
    
    public Pronouns() {
        data = new CachedBulkManager<>(new CachedBulkManager.Requester<String, String>() {
            @Override
            public void request(CachedBulkManager<String, String> manager, Set<String> asap, Set<String> normal, Set<String> backlog) {
                String username = manager.makeAndSetRequested(asap, normal, backlog, 1).iterator().next();
                UrlRequest request = new UrlRequest("https://api.pronouns.alejo.io/v1/users/" + username);
                request.async((result, responseCode) -> {
                    if (responseCode == 404) {
                        // This is a valid response (old API was empty array)
                        manager.setResult(username, NOT_FOUND);
                    } else if (result == null) {
                        manager.setError(username);
                    } else {
                        String pronoun_id = parseUser(result);
                        if (pronoun_id == null) {
                            /**
                             * Empty result means it won't overwrite an already
                             * requested one in getUser2(), but it will not find
                             * a pronoun for it.
                             */
                            manager.setResult(username, NOT_FOUND);
                        }
                        else {
                            manager.setResult(username, pronoun_id);
                        }
                    }
                });
            }
        }, "[Pronouns] ", CachedBulkManager.DAEMON | CachedBulkManager.UNIQUE);
        
        data.setCacheTimes(1, 14, TimeUnit.DAYS);
        data.loadCacheFromFile(CACHE_FILE2, input -> {
            String[] split = input.split(",", 2);
            if (split.length == 2) {
                return new Pair<>(split[0], split[1]);
            }
            return null;
        });
        requestPronouns();
    }
    
    public void saveCache() {
        data.saveCacheToFile(CACHE_FILE2, (key, item) -> {
            if (NOT_FOUND.equals(item)) {
                return null;
            }
            return key+","+item;
        });
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
        if (!Helper.isValidStream(username)) {
            return;
        }
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
    
    private long userCounter = 0;
    
    public String getUser2(String username) {
        if (!Helper.isValidStream(username)) {
            return null;
        }
        /**
         * Keep only the most recent requests. This should ensure that it
         * doesn't keep requesting for ages, even after e.g. leaving a busy
         * channel.
         */
        userCounter++;
        String unique = "user" + (userCounter % 5);
        return getDisplay(data.getOrQuerySingle(unique, null, CachedBulkManager.NONE, username));
    }
    
    private void sendResult(BiConsumer<String, String> listener, String username, CachedBulkManager.Result<String, String> result) {
        String r = getDisplay(result.get(username));
        if (r != null) {
            listener.accept(username, r);
        }
    }
    
    private void requestPronouns() {
        CachedManager cache = new CachedManager(CACHE_FILE1, CACHE_EXPIRES_AFTER, "Pronouns1") {
            @Override
            public boolean handleData(String data) {
                Map<String, Pronoun> parsed = parsePronouns(data);
                if (!parsed.isEmpty()) {
                    pronouns = parsed;
                    return true;
                }
                return false;
            }
        };
        
        if (!cache.load()) {
            UrlRequest request = new UrlRequest("https://api.pronouns.alejo.io/v1/pronouns");
            request.async((result, responseCode) -> {
                cache.dataReceived(result, false);
            });
        }
    }
    
    private Map<String, Pronoun> parsePronouns(String json) {
        Map<String, Pronoun> result = new HashMap<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            for (Object o : root.values()) {
                JSONObject entry = (JSONObject)o;
                String name = JSONUtil.getString(entry, "name");
                String subject = JSONUtil.getString(entry, "subject");
                String object = JSONUtil.getString(entry, "object");
                if (name != null && subject != null) {
                    result.put(name, new Pronoun(name, subject, object));
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing pronouns: "+ex);
        }
        return result;
    }
    
    private static class Pronoun {
        
        public final String id;
        public final String subject;
        public final String object;
        
        private Pronoun(String id, String subject, String object) {
            this.id = id;
            this.subject = subject;
            this.object = object;
        }
        
        public String getDisplay() {
            if (StringUtil.isNullOrEmpty(object) || subject.equals(object)) {
                return subject;
            }
            return String.format("%s/%s",
                                 subject, object);
        }
        
        public String getDisplay(Pronoun combineWith) {
            if (combineWith == null || this.equals(combineWith)) {
                return getDisplay();
            }
            return String.format("%s/%s",
                                 subject, combineWith.subject);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Pronoun other = (Pronoun) obj;
            return Objects.equals(this.id, other.id);
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.id);
            return hash;
        }
        
    }
    
    private String parseUser(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            String id = JSONUtil.getString(root, "pronoun_id");
            String alt_id = JSONUtil.getString(root, "alt_pronoun_id");
            return combineIds(id, alt_id);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing pronouns: "+ex);
        }
        return null;
    }
    
    /**
     * Combine ids into one string.
     * 
     * Needs to be converted into a string for the file cache anyway, so let's
     * just use this everywhere, so fewer changes are necessary.
     * 
     * @param id
     * @param altId
     * @return 
     */
    private static String combineIds(String id, String altId) {
        if (!StringUtil.isNullOrEmpty(id)) {
            if (!StringUtil.isNullOrEmpty(altId)) {
                return id + "|" + altId;
            }
            return id;
        }
        return null;
    }
    
    /**
     * Get the text that should actually be displayed to the user.
     * 
     * @param combinedIds
     * @return 
     */
    private String getDisplay(String combinedIds) {
        if (combinedIds == null) {
            return null;
        }
        String[] split = combinedIds.split("\\|");
        String id = split[0];
        String altId = null;
        if (split.length == 2) {
            altId = split[1];
        }
        Pronoun pronoun = pronouns.get(id);
        if (pronoun != null) {
            return pronoun.getDisplay(pronouns.get(altId));
        }
        return null;
    }
    
}
