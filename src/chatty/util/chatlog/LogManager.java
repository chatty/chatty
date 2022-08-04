
package chatty.util.chatlog;

import chatty.util.chatlog.LogWriter.LogItem;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Starts a new LogWriter and sends lines to it through the queue.
 * 
 * @author tduva
 */
public class LogManager {
  
    private static final Logger LOGGER = Logger.getLogger(LogManager.class.getName());
    
    private static final int QUEUE_CAPACITY = 100;
    private static final int MAX_WAIT = 10*1000;
    
    private final AtomicInteger errors = new AtomicInteger();

    private final BlockingQueue<LogItem> queue;
    private final Thread writerThread;

    public LogManager(Path path, String splitLogs, boolean useSubdirectories,
            boolean lockFiles) {
        path.toFile().mkdirs();
        if (!path.toFile().exists()) {
            LOGGER.warning("Log: Failed to create path: "+path);
        }
        this.queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.writerThread = new Thread(new LogWriter(queue, path, splitLogs, useSubdirectories, lockFiles), "LogWriter");
    }
    
    public void start() {
        writerThread.start();
    }

    /**
     * Closes the logging. Sends an item to tell the writer to close all files,
     * then waits for the thread to finish, so this could take some time.
     */
    public void close() {
        try {
            boolean added = queue.offer(new LogItem(null, null), MAX_WAIT, TimeUnit.MILLISECONDS);
            if (added) {
                writerThread.join(MAX_WAIT);
            } else {
                LOGGER.warning("Log: Could not close Log (Queue full)");
            }
        } catch (InterruptedException ex) {
            LOGGER.warning("Log: Interrupted when waiting for Log to finish..");
            Thread.currentThread().interrupt();
        }
    }

    
    public void writeLine(String channel, String line) {
        boolean added = queue.offer(new LogItem(channel, line));
        if (!added) {
            int current = errors.incrementAndGet();
            if (current % 20 == 0) {
                LOGGER.warning("Log: Failed writing "+errors+" lines (queue full)");
            }
        }
    }
}
