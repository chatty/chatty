
package chatty.util.api;

import chatty.Chatty;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class EmoticonSizeCache {
    
    private static final Logger LOGGER = Logger.getLogger(EmoticonSizeCache.class.getName());
    
    private static final Path FILE = Paths.get(Chatty.getCacheDirectory() + "emoticon_sizes");
    
    private static final Map<String, Dimension> data = new HashMap<>();
    private static final Charset CHARSET = Charset.forName("UTF-8");
    
    public synchronized static void setSize(String id, int width, int height) {
        data.put(id, new Dimension(width, height));
    }
    
    public synchronized static Dimension getSize(String id) {
        return data.get(id);
    }
    
    public synchronized static void removeSize(String id) {
        data.remove(id);
    }
    
    public synchronized static void saveToFile() {
        try (BufferedWriter writer = Files.newBufferedWriter(FILE, CHARSET)) {
            for (String id : data.keySet()) {
                Dimension d = data.get(id);
                writer.write(id + " " + d.width + "x" + d.height);
                writer.newLine();
            }
        } catch (IOException ex) {
            LOGGER.warning("Error saving emoticon sizes: " + ex);
        }
    }
    
    public synchronized static void loadFromFile() {
        try (BufferedReader reader = Files.newBufferedReader(FILE, CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    line = line.trim();
                    String[] split = line.split(" ");
                    String id = split[0];
                    String[] size = split[1].split("x");
                    int width = Integer.parseInt(size[0]);
                    int height = Integer.parseInt(size[1]);
                    setSize(id, width, height);
                } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
                    // Just ignore this line if an error occurs
                }
            }
        } catch (IOException ex) {
            LOGGER.warning("Error loading emoticon sizes: " + ex);
        }
    }
}
