
package chatty.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Can perform a backup (used on start) of certain files (settings). It just
 * copies them and gives them an appropriate name, rotating through different
 * numbers.
 * 
 * <p>
 * The backup is only performed in certain intervals. Both the last number as
 * well as the time of the last backup are stored in a meta info file. If the
 * file can't be read or the format is invalid, then the backup might always
 * be performed, but it will also use the default number, so only one set of
 * backups should be overwritten with possibly invalid data (if the user starts
 * again and again and it performs a backup everytime).</p>
 * 
 * @author tduva
 */
public class BackupManager {
    
    private static final Logger LOGGER = Logger.getLogger(BackupManager.class.getName());
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    
    /**
     * File where some meta information is stored.
     */
    private static final String META_FILE = "backup_meta";
    
    /**
     * Path used to resolve the files to backup, in case not a full path is
     * specified.
     */
    private final Path defaultSourcePath;
    
    /**
     * Path to backup the files to.
     */
    private final Path backupPath;
    
    /**
     * List of files to backup.
     */
    private final List<Path> files;
    
    private long lastBackup;
    private int number;
    
    /**
     * Creates a new backup manager with the given {@code backupPath}, where it
     * saves the files to, and {@code defaultSourcePath}, which is used for
     * resolving the files to backup in case they are not a full path.
     *
     * @param backupPath The path to save the files to
     * @param defaultSourcePath The path to load the files from, if no full path
     * is specified for the files.
     */
    public BackupManager(Path backupPath, Path defaultSourcePath) {
        this.backupPath = backupPath;
        this.defaultSourcePath = defaultSourcePath;
        this.files = new ArrayList<>();
    }
    
    /**
     * Adds a file to backup. Can be either a full path or just a part, which
     * will be resolved with the {@code defaultSourcePath}.
     * 
     * @param file The {@code Path} of the file
     */
    public void addFile(Path file) {
        files.add(file);
    }
    
    /**
     * Adds a file to backup. Can be either a full path or just a part, which
     * will be resolved with the {@code defaultSourcePath}.
     * 
     * @param fileName The name of the file or path as a {@code String}
     * @see addFile(Path)
     */
    public void addFile(String fileName) {
        addFile(Paths.get(fileName));
    }
    
    /**
     * Backups the previously added files if the {@code delay} has passed and
     * {@code count} is greater than 0, rotating the filenames between numbers 0
     * and {@code count}-1.
     * 
     * @param delay The minimum time in between backups, in seconds
     * @param count How many sets of backups to keep. Backups are numbered 0 to
     * n-1 based on this parameter
     */
    public void performBackup(int delay, int count) {
        loadMetadata();
        if (!checkBackupDelay(delay*1000) || count <= 0) {
            return;
        }
        try {
            Files.createDirectories(backupPath);
        } catch (IOException ex) {
            LOGGER.warning("Could not create backup dir: "+ex.getLocalizedMessage());
            return;
        }
        number = (number + 1) % count;
        LOGGER.info("Performing backup ("+number+")..");
        for (Path sourceFile : files) {
            sourceFile = defaultSourcePath.resolve(sourceFile);
            if (!Files.isRegularFile(sourceFile)) {
                continue;
            }
            String targetFileName = "backup_"+number+"_"+sourceFile.getFileName().toString();
            Path targetFile = backupPath.resolve(targetFileName);
            try {
                Files.copy(sourceFile, targetFile, REPLACE_EXISTING);
            } catch (IOException ex) {
                LOGGER.warning("Could not perform backup: "+ex);
            }
        }
        lastBackup = System.currentTimeMillis();
        saveMetadata();
    }
    
    /**
     * Checks if the given delay has passed since the last backup. Should be
     * checked only after loading the metadata.
     * 
     * @param backupDelay The minimum delay in between backups, in milliseconds
     * @return {@code true} if the delay has passed, {@code false} otherwise
     */
    private boolean checkBackupDelay(long backupDelay) {
        long ago = System.currentTimeMillis() - lastBackup;
        return ago > backupDelay;
    }
    
    /**
     * Loads some meta information from a file.
     * 
     * <p>
     * Format (on one line):
     * &lt;LastNumber&gt; &lt;LastBackup&gt;</p>
     */
    private void loadMetadata() {
        Path f = backupPath.resolve(META_FILE);
        try (BufferedReader reader = Files.newBufferedReader(f, CHARSET)) {
            String line = reader.readLine();
            String[] split = line.split(" ");
            number = Integer.parseInt(split[0]);
            lastBackup = Long.parseLong(split[1]);
        } catch (IOException ex) {
            LOGGER.warning("No backup meta file, using default. "+ex);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            LOGGER.warning("Backup meta file invalid format, using default. "+ex);
            number = 0;
            lastBackup = 0;
        }
    }
    
    /**
     * Saves the current meta information to a file.
     */
    private void saveMetadata() {
        Path f = backupPath.resolve(META_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(f, CHARSET)) {
            writer.write(String.valueOf(number)+" "+String.valueOf(lastBackup));
        } catch (IOException | NumberFormatException ex) {
            LOGGER.warning("Error writing backup meta file: "+ex);
        }
    }
    
}
