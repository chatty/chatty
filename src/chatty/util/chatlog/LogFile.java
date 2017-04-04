package chatty.util.chatlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * Open, lock and write to a single logfile. The name of the logfiles is based
 * on the given name, but with ".log" added to the end.
 *
 * @author tduva
 */
public class LogFile {

    /**
     * Debug logger.
     */
    private static final Logger LOGGER = Logger.getLogger(LogFile.class.getName());

    /**
     * Character set.
     */
    private static final String CHARSET = "UTF-8";

    /**
     * How many different files to try to open in case of error
     */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * Write buffer for the LogFile instance.
     */
    private BufferedWriter writer;

    /**
     * Rather or not we have a valid file. (eg. is writable)
     */
    private boolean valid;

    /**
     * System path to where the file is stored.
     */
    private Path file;

    /**
     * The time this LogFile instance was created.
     */
    private Calendar currentTime;
    
    private boolean lockFile = true;

    /**
     * LogFile constructor.
     *
     * @param path The system path of where the LogFile will be stored.
     * @param name Name of the LogFile to be stored.
     */
    private LogFile(Path path, String name, boolean lockFile) {
        this.lockFile = lockFile;
        currentTime = Calendar.getInstance();

        // * can't be part of a filename (for Bouncer messages, e.g. *status)
        name = name.replace("*", "_");

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String fileName;

            if (i == 0) {
                fileName = name + ".log";
            } else {
                fileName = name + "-" + i + ".log";
            }

            file = path.resolve(fileName);

            if (tryFile(file.toFile())) {
                break;
            }
        }
    }

    /**
     * Creates a LogFile object for the given path and name.
     *
     * @param path The path where the file should be created under.
     * @param name The name of the log file to be created.
     * @return The LogFile or null if an error occurred while opening the file.
     */
    public static LogFile get(Path path, String name, boolean lockFile) {
        LogFile file = new LogFile(path, name, lockFile);
        if (file.valid) {
            return file;
        }
        return null;
    }

    /**
     * Attempt to write a new line to the LogFile.
     *
     * @param line The message to be written to the file.
     * @return Returns true if the message is successfully logged.
     */
    public boolean write(String line) {
        if (!valid) {
            LOGGER.warning("Log: Tried writing to invalid file " + file + "");
            return false;
        }

        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
            return true;
        } catch (IOException ex) {
            LOGGER.warning("Log: Error writing to " + file + " (" + ex.getLocalizedMessage() + ")");
            close();
            return false;
        }
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
     * @param file The file to attempt to open.
     * @return Returns true if the file is successfully opened and locked.
     */
    private boolean tryFile(File file) {
        try {
            LOGGER.info("Log: Trying to open " + file.getAbsolutePath());
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(raf.length());
            FileChannel channel = raf.getChannel();
            if (lockFile) {
                FileLock lock = channel.tryLock();
                if (lock != null) {
                    writer = new BufferedWriter(Channels.newWriter(channel, CHARSET));
                    valid = true;
                    return true;
                }
            } else {
                writer = new BufferedWriter(Channels.newWriter(channel, CHARSET));
                valid = true;
                return true;
            }
        } catch (IOException ex) {
            LOGGER.warning("Log: Lock failed (" + file + " / " + ex + ")");
        }

        LOGGER.warning("Log: Lock failed (" + file + ")");
        return false;
    }

    /**
     * Getter for the `valid` property.
     *
     * @return The `valid` property.
     */
    public boolean isValid() {
        return valid;
    }
    
    public boolean isLocked() {
        return lockFile && valid;
    }

    /**
     * Getter for the path of the LogFile.
     *
     * @return The `file` property.
     */
    public Path getPath() {
        return file;
    }

    /**
     * Getter for the date that this LogFile instance was created.
     *
     * @return The `currentTime` property.
     */
    public Calendar getDate() {
        return currentTime;
    }
}
