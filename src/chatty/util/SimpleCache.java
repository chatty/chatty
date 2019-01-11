
package chatty.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Writes a simple UTF-8 encoded textfile as a cache for Strings which can be
 * written/read easily. Saves a timestamp of when it was written into the file,
 * which is used to determine whether the cache expired when reading it.
 * 
 * @author tduva
 */
public class SimpleCache {
    
    private static final Logger LOGGER = Logger.getLogger(SimpleCache.class.getName());
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    
    private final String id;
    private final Path file;
    private final long expireTime;
    private final String debugPrefix;
    
    /**
     * Creates a new cache object for a single file.
     * 
     * @param id Used in debug messages to identify the cache contents
     * @param file The file to save into
     * @param expireTime The time in seconds that the cache should be valid for
     */
    public SimpleCache(String id, String file, long expireTime) {
        this.id = id;
        this.file = Paths.get(file);
        this.expireTime = expireTime;
        this.debugPrefix = "C["+id+"]";
    }
    
    /**
     * Saves the given text into the file specified for this cache.
     *
     * @param data The text to save
     */
    public void save(String data) {
        LOGGER.info(debugPrefix+" Cache: Trying to save..");
        try (BufferedWriter writer = Files.newBufferedWriter(file,CHARSET)) {
            writer.write(new Long(System.currentTimeMillis() / 1000).toString()+"\n");
            writer.write(data);
            LOGGER.info(debugPrefix+" Cache: Saved");
        }
        catch (IOException ex) {
            LOGGER.warning(debugPrefix+" Cache: Error saving ["+ex+"]");
        }
    }
    
    public String load() {
        return load(false);
    }
    
    /**
     * Load cached text from the file specified for this cache.
     *
     * @param loadEvenIfExpired
     * @return The cached text or null if the file isn't recent enough or an
     * error occured
     */
    public String load(boolean loadEvenIfExpired) {
        LOGGER.info(debugPrefix+" Cache: Trying to load..");
        try (BufferedReader reader = Files.newBufferedReader(file, CHARSET)) {
            long time = Long.parseLong(reader.readLine());
            long timePassed = (System.currentTimeMillis() / 1000) - time;
            if (!loadEvenIfExpired && timePassed > expireTime) {
                LOGGER.info(debugPrefix+" Cache: Did not load (expired)");
                return null;
            }
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line);
                data.append("\n");
            }
            return data.toString();
        } catch (IOException | NumberFormatException ex) {
            LOGGER.warning(debugPrefix+" Cache: Error loading ["+ex+"]");
            return null;
        }
    }
    
}
