
package chatty.util.chatlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Open, lock and write to a single logfile. The name of the logfiles is based
 * on the given name, but with ".log" added to the end.
 * 
 * @author tduva
 */
public class LogFile {
    
    private static final Logger LOGGER = Logger.getLogger(LogFile.class.getName());
    
    private static final String CHARSET = "UTF-8";
    
    // How many different files to try to open in case of error
    private static final int MAX_ATTEMPTS = 3;
    
    private BufferedWriter writer;
    private boolean valid;
    private Path file;
    
    private LogFile(Path path, String name) {
        // * can't be part of a filename (for Bouncer messages, e.g. *status)
        name = name.replace("*", "_");
        for (int i=0;i<MAX_ATTEMPTS;i++) {
            String fileName;
            if (i == 0) {
                fileName = name+".log";
            } else {
                fileName = name+"-"+i+".log";
            }
            file = path.resolve(fileName);
            if (tryFile(file.toFile())) {
                break;
            }
        }
    }
    
    /**
     * Creates a LogFile object for the given path and channel name.
     * 
     * @param path The path where the file should be created under.
     * @param channel The name of the channel, of which the filename is created.
     * @return The LogFile or null if an error occured while opening the file.
     */
    public static LogFile get(Path path, String channel) {
        LogFile file = new LogFile(path, channel);
        if (file.valid) {
            return file;
        }
        return null;
    }

    public Path getPath() {
        return file;
    }
    
    public boolean write(String line) {
        //System.out.println("Writing "+line);
        if (!valid) {
            LOGGER.warning("Log: Tried writing to invalid file "+file+"");
            return false;
        }
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
            return true;
        } catch (IOException ex) {
            LOGGER.warning("Log: Error writing to "+file+" ("+ex.getLocalizedMessage()+")");
            close();
            return false;
        }
    }
    
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Properly close the file, which means it can't be used anymore.
     */
    public void close() {
        closeResources();
        valid = false;
        LOGGER.info("Log: Closed file " + file.toAbsolutePath());
    }

    /**
     * Close the writer, if necessary.
     */
    private void closeResources() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException ex) {
            LOGGER.warning("Log: Could not close " + file.toAbsolutePath() + " (" + ex.getLocalizedMessage() + ")");
        }
    }
    
    /**
     * Tries to open the given file and obtain a lock on it. This may fail when
     * the file is already used by another process, or any other IOException of
     * course.
     * 
     * @param file
     * @return 
     */
    private boolean tryFile(File file) {
        try {
            LOGGER.info("Log: Trying to open "+file.getAbsolutePath());
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(raf.length());
            FileChannel channel = raf.getChannel();
            FileLock lock = channel.tryLock();
            if (lock != null) {
                writer = new BufferedWriter(Channels.newWriter(channel, CHARSET));
                valid = true;
                return true;
            }
        } catch (IOException ex) {
            LOGGER.warning("Log: Lock failed ("+file+" / "+ex+")");
        }
        LOGGER.warning("Log: Lock failed ("+file+")");
        return false;
    }
    
//    private void openFile(Path file) {
//        try {
//            writer = Files.newBufferedWriter(file, CHARSET,
//                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
//            valid = true;
//            this.file = file;
//            LOGGER.info("Log: File "+file.toAbsolutePath()+" opened.");
//        } catch (IOException ex) {
//            LOGGER.warning("Log: Error opening file "+file+" ("+ex.getLocalizedMessage()+")");
//        }
//    }
    
    
}
