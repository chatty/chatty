
package chatty.util.api;

import chatty.Logging;
import chatty.util.SimpleCache;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public abstract class CachedManager {
    
    private static final Logger LOGGER = Logger.getLogger(CachedManager.class.getName());
    
    
    private final SimpleCache cache;
    private final String label;
    
    public CachedManager(String file, long expireTime, String label) {
        this.cache = new SimpleCache(label, file, expireTime);
        this.label = "["+label+"] ";
    }
    
    protected abstract boolean handleData(String data);
    
    protected boolean load(boolean useFileEvenIfExpired) {
        String fromFile = loadFromFile(useFileEvenIfExpired);
        if (fromFile != null) {
            boolean dataValid = handleData(fromFile);
            if (dataValid) {
                LOGGER.info(label+"Using data from file." + (useFileEvenIfExpired ? " (forced)" : ""));
                return true;
            }
        }
        return false;
    }
    
    protected void dataReceived(String result, boolean wasForcedUpdate) {
        boolean dataValid = handleData(result);
        if (dataValid) {
            saveToFile(result);
        }
        else {
            if (!wasForcedUpdate) {
                load(true);
            } else {
                LOGGER.log(Logging.USERINFO, "Error requesting "+label+" from API.");
            }
        }
    }
    
    /**
     * Saves the given json text (which should be the list of emoticons as
     * received from the Twitch API v2) into a file.
     *
     * @param json
     */
    private void saveToFile(String json) {
        synchronized (cache) {
            cache.save(json);
        }
    }

    /**
     * Loads emoticons list from the file.
     *
     * @return The json as received from the Twitch API v2 or null if the file
     * isn't recent enough or an error occured
     */
    private String loadFromFile(boolean loadEvenIfExpired) {
        synchronized (cache) {
            return cache.load(loadEvenIfExpired);
        }
    }
    
}
