
package chatty.util.api;

import chatty.Logging;
import chatty.util.SimpleCache;
import java.util.logging.Logger;

/**
 * Saves valid data that is received from an API (usually), to a file.
 * 
 * @author tduva
 */
public abstract class CachedManager {
    
    private static final Logger LOGGER = Logger.getLogger(CachedManager.class.getName());
    
    
    private final SimpleCache cache;
    private final String label;
    
    /**
     * Create a new CachedManager with some values for the cache.
     * 
     * @param file The file to store the cache in
     * @param expireTime After how many seconds the cache should count as
     * expired
     * @param label Label used for debug output
     */
    public CachedManager(String file, long expireTime, String label) {
        this.cache = new SimpleCache(label, file, expireTime);
        this.label = "["+label+"] ";
    }
    
    /**
     * Data that was received from the API can be parsed/handled here. Return
     * true if the data should be saved.
     * 
     * @param data The raw data
     * @return true if the data is valid and should be cached, false otherwise
     */
    public abstract boolean handleData(String data);
    
    /**
     * Load the data from the cache. If the data is cached and valid, then this
     * will return true.
     * 
     * @param useFileEvenIfExpired Load the data from the file (if available)
     * even if it has expired
     * @return true if the cached data is being used, false otherwise
     */
    public boolean load(boolean useFileEvenIfExpired) {
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
    
    /**
     * This is used to give the manager raw data received from the API.
     * 
     * @param data The raw data, usually JSON, but doesn't matter for this
     * @param wasForcedUpdate Whether this was a forced update, to refresh the
     * data, or the initial request to get any data in the first place (if the
     * request failed, or the data isn't valid, it will only load from the cache
     * if this was the initial request)
     */
    public void dataReceived(String data, boolean wasForcedUpdate) {
        boolean dataValid = handleData(data);
        if (dataValid) {
            saveToFile(data);
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
     * Saves the given text into a file.
     *
     * @param json
     */
    private void saveToFile(String json) {
        synchronized (cache) {
            cache.save(json);
        }
    }

    /**
     * Loads cached data from the file.
     *
     * @param loadEvenIfExpired Whether to load the cache, even if the file is
     * expired
     * @return The data as received from the API, or null if the file
     * isn't recent enough or an error occured
     */
    private String loadFromFile(boolean loadEvenIfExpired) {
        synchronized (cache) {
            return cache.load(loadEvenIfExpired);
        }
    }
    
}
