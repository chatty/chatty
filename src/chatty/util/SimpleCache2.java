
package chatty.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class SimpleCache2 {
    
    private static final Logger LOGGER = Logger.getLogger(SimpleCache2.class.getName());
   
    private final String debugPrefix;
    
    private final String id;
    private final long expireTime;
    private final Path file;
    
    private Function<List<String>, Boolean> handleLinesCallback;
    private Supplier<List<String>> refreshLinesCallback;
    
    private volatile boolean loadSuccess;
    private volatile boolean cacheInvalid;
    private volatile boolean pendingRefresh;
    
    public SimpleCache2(String id, String file, long expireTime) {
        this.id = id;
        this.file = Paths.get(file);
        this.expireTime = expireTime;
        this.debugPrefix = "C["+id+"] Cache: ";
    }
    
    /**
     * Set a function that receives loaded data and returns true if the data
     * is valid, in which case the data counts as successfully loaded, meaning
     * no more attempts are made to load it from file (only refresh).
     * 
     * The loaded data is a List of String objects, however it may als be null.
     * 
     * @param callback 
     */
    public void setHandleDataCallback(Function<List<String>,Boolean> callback) {
        this.handleLinesCallback = callback;
    }
    
    /**
     * Set a supplier of lines of text. Used to refresh the data for this cache.
     * Usually this would be an API request or something.
     * 
     * @param callback 
     */
    public void setRefreshLinesCallback(Supplier<List<String>> callback) {
        this.refreshLinesCallback = callback;
    }
    
    /**
     * Load the data, first trying from cache, and if that fails refreshing the
     * data. If data was already loaded or the cache already deemed invalid,
     * then this does nothing.
     */
    public void load() {
        if (cacheInvalid || loadSuccess) {
            return;
        }
        boolean loadedFromCache = loadFromCache(false);
        if (!loadedFromCache) {
            refresh();
        }
    }
    
    /**
     * Load the data asynchronously.
     * 
     * @see load()
     */
    public void asyncLoad() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                load();
            }
        }).start();
    }
    
    /**
     * Load the data from the cache (file), with no conditions.
     * @param force If true the cache will be loaded even if it expired
     * @return true if data was successfully loaded, false otherwise
     */
    private boolean loadFromCache(boolean force) {
        List<String> lines = loadFromFile(force);
        if (lines != null) {
            if (handleLinesCallback != null) {
                boolean valid = handleLinesCallback.apply(lines);
                if (valid) {
                    loadSuccess = true;
                    return true;
                } else {
                    cacheInvalid = true;
                }
            }
        }
        return false;
    }
    
    /**
     * Refresh data, meaning it will be requested from the refresh callback.
     * If there is already a pending refresh, or no callback was set, then
     * nothing is done.
     * 
     * Also loads the new data and saves it to file, it valid. If the refresh
     * fails, and no data has been loaded yet at all, another attempt is made
     * to load it from file, even if the file expired.
     */
    public void refresh() {
        if (pendingRefresh) {
            return;
        }
        if (refreshLinesCallback != null) {
            pendingRefresh = true;
            List<String> lines = refreshLinesCallback.get();
            boolean valid = handleLinesCallback.apply(lines);
            if (valid) {
                saveToFile(lines);
                loadSuccess = true;
            } else if (!loadSuccess) {
                loadFromCache(true);
            }
        }
        pendingRefresh = false;
    }
    
    /**
     * Refresh data asynchronously.
     * 
     * @see refresh()
     */
    public void asyncRefresh() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                refresh();
            }
        }).start();
    }
    
    /**
     * Loads lines from file.
     * 
     * @param forceLoad If true, the file is read even if it is expired
     * @return The read lines, or null if an error occured or the file expired
     */
    private synchronized List<String> loadFromFile(boolean forceLoad) {
        LOGGER.info(debugPrefix+"Trying to load..");
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            long time = Long.parseLong(reader.readLine());
            long timePassed = (System.currentTimeMillis() / 1000) - time;
            if (forceLoad || timePassed < expireTime) {
                return getLines(reader);
            } else {
                LOGGER.info(debugPrefix+"Expired");
            }
        } catch (IOException | NumberFormatException ex) {
            cacheInvalid = true;
            LOGGER.warning(debugPrefix+"Error loading ["+ex+"]");
        }
        return null;
    }

    /**
     * Reads a list of lines from the given input stream.
     * 
     * @param reader
     * @return
     * @throws IOException 
     */
    public static List<String> getLines(BufferedReader reader) throws IOException {
        List<String> result = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        return result;
    }
    
    /**
     * Writes the given lines to file.
     * 
     * @param lines The lines of text
     */
    private synchronized void saveToFile(List<String> lines) {
        LOGGER.info(debugPrefix+"Trying to save..");
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(Long.toString(System.currentTimeMillis() / 1000));
            for (String line : lines) {
                // Use this rather than writer.newLine() to keep it consistent
                // between different OS (only Chatty needs to read the file)
                writer.write("\n");
                writer.write(line);
            }
            LOGGER.info(debugPrefix+"Saved.");
        }
        catch (IOException ex) {
            LOGGER.warning(debugPrefix+"Error saving ["+ex+"]");
        }
    }
    
}
