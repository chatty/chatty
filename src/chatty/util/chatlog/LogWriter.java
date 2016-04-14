
package chatty.util.chatlog;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * Handles writing the log files. Retrieves data from a queue and manages files
 * to write the log into.
 * 
 * @author tduva
 */
public class LogWriter implements Runnable {
    
    private static final Logger LOGGER = Logger.getLogger(LogWriter.class.getName());
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZ");
    
    private static final int STATS_INTERVAL = 500;
    private static final int STATS_TIME_INTERVAL = 5*60*1000;
    
    private final Map<String, LogFile> files = new HashMap<>();
    private final Set<String> errors = new HashSet<>();
    private final BlockingQueue<LogItem> queue;
    private final Path path;
    
    private long addedQueueSize;
    private int addedQueueSizeCount;
    private int errorCount;
    private long lastStatsTime;
    private int maxQueueSize;
    private int totalLines;
    
    public LogWriter(BlockingQueue<LogItem> queue, Path path) {
        this.queue = queue;
        this.path = path;
    }
    
    @Override
    public void run() {
        boolean run = true;
        try {
            while (run) {
                //System.out.println("Waiting for a new item..");
                LogItem item = queue.take();
                stats(queue.size());
                if (item.channel == null) {
                    if (item.message == null) {
                        outputStats();
                        run = false;
                        closeAllFiles();
                    } else {
                        // Can't close any files here because it would
                        // remove an item during iteration
                        for (String channel : files.keySet()) {
                            handleMessage(channel, item.message);
                        }
                    }
                } else {
                    handleMessage(item.channel, item.message);
                }
            }
        } catch (InterruptedException ex) {
            System.out.println("Interrupted");
            closeAllFiles();
            Thread.currentThread().interrupt();
        }
    }
    
    private void closeAllFiles() {
        for (String channel : files.keySet()) {
            LogFile file = files.get(channel);
            closeFile(file);
        }
        files.clear();
    }
    
    private void handleMessage(String channel, String message) {
        if (message == null) {
            closeFileForChannel(channel);
        } else {
            writeLine(channel, message);
        }
    }
    
    private void writeLine(String channel, String line) {
        LogFile file = getFile(channel);
        if (file == null || !file.write(line)) {
            fileError(channel);
        }
    }
    
    private LogFile getFile(String channel) {
        LogFile file = files.get(channel);
        if (file != null && file.isValid()) {
            return file;
        }
        if (errors.contains(channel)) {
            return null;
        }
        return addFile(channel);
    }
    
    private LogFile addFile(String channel) {
        LogFile file = LogFile.get(path, channel);
        if (file == null) {
            errors.add(channel);
        } else {
            files.put(channel, file);
            file.write("# Log started: "+getDateTime());
            LOGGER.info("Log: Opened file " + file.getPath());
        }
        return file;
    }
    
    private void fileError(String channel) {
        //LOGGER.warning("LOG: Could not write to file for "+channel);
        files.remove(channel);
        errors.add(channel);
        errorCount++;
    }
    
    private void closeFileForChannel(String channel) {
        LogFile file = files.get(channel);
        closeFile(file);
        files.remove(channel);
    }
    
    private void closeFile(LogFile file) {
        if (file != null && file.isValid()) {
            file.write("# Log closed: "+getDateTime());
            file.write("-");
            file.close();
        }
    }

    private String getDateTime() {
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime());
    }
    
    private void stats(int size) {
        addedQueueSize += size;
        addedQueueSizeCount++;
        totalLines++;
        if (maxQueueSize < size) {
            maxQueueSize = size;
        }
        long lastStatsAgo = System.currentTimeMillis() - lastStatsTime;
        if (addedQueueSizeCount > STATS_INTERVAL || lastStatsAgo > STATS_TIME_INTERVAL) {
            outputStats();
        }
    }
    
    private void outputStats() {
        long avg = addedQueueSizeCount > 0 ? addedQueueSize / addedQueueSizeCount : 0;
        LOGGER.info("Log: total: "+totalLines+" / queue size (avg: "+avg
                +", max: "+maxQueueSize+") / errors: " + errorCount);
        addedQueueSize = 0;
        addedQueueSizeCount = 0;
        errorCount = 0;
        maxQueueSize = 0;
        lastStatsTime = System.currentTimeMillis();
    }

    public static class LogItem {

        public final String channel;
        public final String message;
        
        public LogItem(String channel, String message) {
            this.channel = channel;
            this.message = message;
        }
    }
    
}
