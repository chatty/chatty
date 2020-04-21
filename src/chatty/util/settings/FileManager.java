
package chatty.util.settings;

import chatty.util.DateTime;
import chatty.util.MiscUtil;
import chatty.util.settings.FileManager.SaveResult.CancelReason;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Load and save a pre-defined set of files, with convenience features such as
 * only write if content changed and backups.
 * 
 * There are two types of backups: The session backup is written at the same
 * time a file is saved. Other backups are created when the backup method is
 * called.
 * 
 * @author tduva
 */
public class FileManager {
    
    private static final Logger LOGGER = Logger.getLogger(FileManager.class.getName());
    
    private static final String MANUAL_BACKUP_PREFIX = "manual_";
    private static final String AUTO_BACKUP_PREFIX = "auto_";
    private static final String SESSION_BACKUP_PREFIX = "session_";
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    
    private final Map<String, FileSettings> files = new HashMap<>();
    private final Set<String> backupLoaded = new HashSet<>();
    private final Map<String, String> knownContent = new HashMap<>();
    private final Path basePath;
    private final Path backupPath;
    
    private boolean savingPaused;
    
    public FileManager(Path basePath, Path backupPath) {
        this.basePath = basePath;
        this.backupPath = backupPath;
    }
    
    public synchronized void add(String id, String fileName, boolean backupEnabled,
                                 FileContentInfoProvider provider) {
        FileSettings settings = new FileSettings(id, basePath.resolve(fileName), backupEnabled, provider);
        files.put(id, settings);
    }
    
    /**
     * If set to true, don't attempt to save settings.
     * 
     * @param paused 
     */
    public synchronized void setSavingPaused(boolean paused) {
        this.savingPaused = paused;
        LOGGER.info("Saving paused: "+paused);
    }
    
    public synchronized SaveResult save(String id, String content, boolean force) {
        SaveResult.Builder result = new SaveResult.Builder(id);
        FileSettings fileSettings = files.get(id);
        if (savingPaused) {
            result.setCancelled(CancelReason.SAVING_PAUSED);
            return result.make();
        }
        if (fileSettings == null) {
            LOGGER.warning("[Save] Invalid file id: "+id);
            result.setCancelled(CancelReason.INVALID_ID);
            return result.make();
        }
        if (!force && knownContent.containsKey(id) && Objects.equals(knownContent.get(id), content)) {
            if (content != null) {
                LOGGER.info("Not writing "+id+" (known content)");
            }
            result.setCancelled(CancelReason.KNOWN_CONTENT);
            return result.make();
        }
        if (!force && backupLoaded.contains(id)) {
            LOGGER.info("Not writing "+id+" (backup loaded this session)");
            result.setCancelled(CancelReason.BACKUP_LOADED);
            return result.make();
        }
        try {
            Path target = fileSettings.path;
            if (content == null) {
                boolean removed = removeFile(target);
                if (removed) {
                    result.setRemoved();
                }
            }
            else {
                saveToFile(target, content); // IOException
                result.setWritten(target);
            }
            // Only count as written if no exception was thrown
            knownContent.put(id, content);
        }
        catch (IOException ex) {
            result.setWriteError(ex);
        }

        if (fileSettings.backupEnabled && content != null) {
            try {
                Path backupTarget = backupPath.resolve(SESSION_BACKUP_PREFIX+"_" + fileSettings.path.getFileName());
                saveToFile(backupTarget, content);
                result.setBackupWritten(backupTarget);
            }
            catch (IOException ex) {
                result.setBackupError(ex);
            }
        }
        return result.make();
    }
    
    /**
     * Load the contents from the file with the given id.
     * 
     * @param id
     * @return The file contents, or null if a file with the given id doesn't
     * exist
     * @throws IOException 
     */
    public synchronized String load(String id) throws IOException {
        FileSettings fileSettings = files.get(id);
        if (fileSettings == null) {
            LOGGER.warning("[Load] Invalid file id: "+id);
            return null;
        }
        String content = loadFromFile(fileSettings.path);
        knownContent.put(id, content);
        return content;
    }
    
    private String loadFromFile(Path file) throws IOException {
        return new String(Files.readAllBytes(file), CHARSET);
    }
    
    private void saveToFile(Path file, String content) throws IOException {
        LOGGER.info("Saving contents to file: " + file);
        try {
            Files.createDirectories(file.getParent());
        }
        catch (IOException ex) {
            // If e.g. a symbolic link dir already exists this may fail, but still be valid for writing
            LOGGER.warning("Failed to create "+file.getParent()+", let's try writing anyway..");
        }
        try {
            Path tempFile = file.resolveSibling(file.getFileName().toString() + "-temp");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, CHARSET)) {
                writer.write(content);
            }
            MiscUtil.moveFile(tempFile, file);
        }
        catch (IOException ex) {
            LOGGER.warning("Error saving file: " + ex);
            throw ex;
        }
    }
    
    /**
     * Remove the given file.
     * 
     * @param file
     * @return true if the file was removed, false if it didn't exist or an
     * error occured
     */
    private boolean removeFile(Path file) {
        try {
            Files.delete(file);
            LOGGER.info("Removed unused file: " + file);
            return true;
        }
        catch (NoSuchFileException ex) {
            // Don't need to remove non-existing file
            LOGGER.info("Unused file doesn't exist: " + file);
        }
        catch (IOException ex) {
            LOGGER.warning("Error removing unused file: " + ex);
        }
        return false;
    }
    
    public synchronized void loadBackup(FileInfo info) throws IOException {
        Files.copy(info.file, info.settings.path, REPLACE_EXISTING);
        backupLoaded.add(info.settings.id);
    }
    
    /**
     * Perform a backup depending on the given settings.
     * 
     * @param backupDelay How many seconds between backups, based on the latest
     * created backup
     * @param keepCount How many backup files to keep, others may be deleted
     * @throws IOException If an error occured reading current backups or
     * creating the new backup
     */
    public synchronized void backup(long backupDelay, int keepCount) throws IOException {
        if (keepCount <= 0) {
            return;
        }
        //--------------------------
        // Check current backups
        //--------------------------
        FileInfos backupFiles = getBackupFileInfo();
        long latestTimestamp = backupFiles.getLatestTimestamp(AUTO_BACKUP_PREFIX);
        List<FileInfo> autoFiles = backupFiles.filter(AUTO_BACKUP_PREFIX);
        
        LOGGER.info(String.format("[Backup] Latest: %s Delay: %s Count: %d Auto: %d Keep: %d",
                DateTime.formatFullDatetime(latestTimestamp*1000),
                DateTime.duration(backupDelay*1000, 1, 0),
                backupFiles.count(),
                autoFiles.size(),
                keepCount));
        
        //--------------------------
        // Delete files
        //--------------------------
        /**
         * In case the settings didn't load properly, it shouldn't delete many
         * backups if the user had it higher than default.
         */
        int toDelete = Math.min(autoFiles.size() - keepCount, 2);
        for (FileInfo file : autoFiles) {
            if (file.timestamp != -1 && toDelete > 0) {
                try {
                    Files.deleteIfExists(file.file);
                    LOGGER.info("[Backup] Deleted old backup: "+file.file);
                    toDelete--;
                }
                catch (IOException ex) {
                    LOGGER.warning("[Backup] Failed to delete backup: " + ex);
                }
            }
        }
        
        //--------------------------
        // Create new backup
        //--------------------------
        // Perform backup if enough time has passed since newest backup file with timestamp
        if (System.currentTimeMillis()/1000 - latestTimestamp > backupDelay) {
            doBackup(AUTO_BACKUP_PREFIX);
        }
    }
    
    public List<SaveResult> manualBackup() {
        return doBackup(MANUAL_BACKUP_PREFIX);
    }
    
    /**
     * Backup all enabled files.
     * 
     * @param prefix The backup file prefix to use
     * @return List of files, with cancel reason or backup save state set
     */
    private List<SaveResult> doBackup(String prefix) {
        List<SaveResult> result = new ArrayList<>();
        for (FileSettings file : files.values()) {
            result.add(doBackup(file, prefix));
        }
        return result;
    }
    
    /**
     * Perform backup for a single file, if enabled.
     * 
     * @param file The file settings
     * @param prefix The backup file prefix to use
     * @return Result with cancel reason or backup save state set
     */
    private SaveResult doBackup(FileSettings file, String prefix) {
        SaveResult.Builder fileResult = new SaveResult.Builder(file.id);
        if (!file.backupEnabled) {
            fileResult.setCancelled(CancelReason.INVALID);
            return fileResult.make();
        }
        try {
            String content = loadFromFile(file.path);
            FileContentInfo info = file.infoProvider.getInfo(content);
            if (!info.isValid) {
                LOGGER.info("[Backup] Didn't perform backup (invalid content): " + file.path);
                fileResult.setCancelled(CancelReason.INVALID_CONTENT);
                return fileResult.make();
            }
            Path backupTarget = backupPath.resolve(prefix + (System.currentTimeMillis() / 1000) + "__" + file.path.getFileName());
            Files.copy(file.path, backupTarget, REPLACE_EXISTING);
            LOGGER.info("[Backup] Backup performed: " + backupTarget);
            fileResult.setBackupWritten(backupTarget);
        }
        catch (IOException ex) {
            fileResult.setBackupError(ex);
            LOGGER.warning("[Backup] Failed: "+ex);
        }
        return fileResult.make();
    }
    
    public synchronized Path getBackupPath() {
        return backupPath;
    }
    
    /**
     * Return a list of backup files, sorted by oldest first.
     * 
     * @return
     * @throws IOException 
     */
    public synchronized FileInfos getBackupFileInfo() throws IOException {
        List<FileInfo> result = new ArrayList<>();
        Set<FileVisitOption> options = new HashSet<>();
        options.add(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(backupPath, options, 1, new SimpleFileVisitor<Path>() {
            
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                String fileName = file.getFileName().toString();
                String origFileName = getOrigFileName(fileName);
                if (hasValidPrefix(fileName) && origFileName != null) {
                    FileSettings s = getFileSettingsByName(origFileName);
                    if (s != null) {
//                        System.out.println(file+" -> "+origFileName+" "+DateTime.agoText(attrs.lastModifiedTime().toMillis()));
                        try {
                            String content = loadFromFile(file);
                            FileContentInfo info = s.infoProvider.getInfo(content);
                            result.add(new FileInfo(s, file, attrs.lastModifiedTime().toMillis(), attrs.size(), getTimestamp(fileName), info.isValid, info.info));
                        }
                        catch (IOException ex) {
                            result.add(new FileInfo(s, file, attrs.lastModifiedTime().toMillis(), attrs.size(), getTimestamp(fileName), false, "Error: "+ex));
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
            
        });
        Collections.sort(result, (a, b) -> {
            if (a.modifiedTime > b.modifiedTime) {
                return 1;
            }
            if (a.modifiedTime < b.modifiedTime) {
                return -1;
            }
            if (a.timestamp > b.timestamp) {
                return 1;
            }
            if (a.timestamp < b.timestamp) {
                return -1;
            }
            return 0;
        });
        return new FileInfos(result);
    }
    
    private FileSettings getFileSettingsByName(String fileName) {
        for (FileSettings s : files.values()) {
            if (s.path.getFileName().toString().equals(fileName)) {
                return s;
            }
        }
        return null;
    }
    
    /**
     * Prefix a filename must have to be considered any kind of backup file.
     * 
     * @param fileName
     * @return 
     */
    private boolean hasValidPrefix(String fileName) {
        return fileName.startsWith(MANUAL_BACKUP_PREFIX)
                || fileName.startsWith(AUTO_BACKUP_PREFIX)
                || fileName.startsWith(SESSION_BACKUP_PREFIX);
    }
    
    private static String getOrigFileName(String fileName) {
        int index = fileName.indexOf("__");
        if (index == -1 || index+2 == fileName.length()) {
            return null;
        }
        return fileName.substring(index+2);
    }
    
    // TODO: For manual backups? (They should usually be written at the same time)
    private static final Pattern FIND_TIMESTAMP = Pattern.compile(AUTO_BACKUP_PREFIX+"([0-9]+)__.+");
    
    private static long getTimestamp(String fileName) {
        Matcher m = FIND_TIMESTAMP.matcher(fileName);
        if (m.matches()) {
            return Long.parseLong(m.group(1));
        }
        return -1;
    }
    
    public static class FileSettings {
        
        public final String id;
        public final Path path;
        public final boolean backupEnabled;
        public final FileContentInfoProvider infoProvider;
        
        public FileSettings(String id, Path path, boolean backupEnabled,
                            FileContentInfoProvider infoProvider) {
            this.id = id;
            this.path = path;
            this.backupEnabled = backupEnabled;
            this.infoProvider = infoProvider;
        }
        
    }
    
    public static class FileInfos {
        
        private final List<FileInfo> data;
        
        public FileInfos(List<FileInfo> data) {
            this.data = data;
        }
        
        public List<FileInfo> getList() {
            return new ArrayList<>(data);
        }
        
        public long getLatestTimestamp(String prefix) {
            long latestTimestamp = 0;
            for (FileInfo file : data) {
                if (file.timestamp > latestTimestamp
                        && file.nameStartsWith(prefix)) {
                    latestTimestamp = file.timestamp;
                }
            }
            return latestTimestamp;
        }
        
        /**
         * Get a list of only files with the given prefix, keeping the original
         * sorting.
         *
         * @param prefix
         * @return 
         */
        public List<FileInfo> filter(String prefix) {
            List<FileInfo> result = new ArrayList<>();
            for (FileInfo file : data) {
                if (file.nameStartsWith(prefix)) {
                    result.add(file);
                }
            }
            return result;
        }
        
        public int count() {
            return data.size();
        }
        
    }
    
    public static class FileInfo {
        
        private final FileSettings settings;
        private final String info;
        private final long modifiedTime;
        private final boolean isValid;
        private final Path file;
        private final long timestamp;
        private final long size;

        public FileInfo(FileSettings settings, Path file, long modifiedTime, long size, long timestamp, boolean isValid, String info) {
            this.file = file;
            this.modifiedTime = modifiedTime;
            this.info = info;
            this.isValid = isValid;
            this.timestamp = timestamp;
            this.settings = settings;
            this.size = size;
        }
        
        public FileSettings getSettings() {
            return settings;
        }
        
        public String getInfo() {
            return info;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public Path getFile() {
            return file;
        }
        
        public boolean nameStartsWith(String prefix) {
            return file.getFileName().toString().startsWith(prefix);
        }
        
        public long getModifiedTime() {
            return modifiedTime;
        }
        
        public long getSize() {
            return size;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Session backups are created/modified at the same time. Other backups
         * are copied, so they retain their original modified time, but have
         * an additional created timestamp added in the name.
         * 
         * @return The timestamp, in milliseconds, when the backup was created
         * (written or copied)
         */
        public long getCreated() {
            return timestamp == -1 ? modifiedTime : timestamp*1000;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] Mod:%s Bu:%s valid: %s (%s)",
                    file, DateTime.ago(modifiedTime), DateTime.ago(timestamp*1000), isValid, info);
        }
        
    }
    
    public static interface FileContentInfoProvider {
        
        public FileContentInfo getInfo(String content);
        
    }
    
    public static class SaveResult {
        
        public enum CancelReason {
            BACKUP_LOADED, INVALID_ID, KNOWN_CONTENT, SAVING_PAUSED, INVALID_CONTENT, INVALID
        }
        
        private static class Builder {
            
            private String id;
            private boolean written;
            private boolean backupWritten;
            private Throwable writeError;
            private Throwable backupError;
            private Path filePath;
            private Path backupPath;
            private boolean removed;
            private CancelReason cancelReason;
            
            public Builder(String id) {
                this.id = id;
            }
            
            public Builder setWritten(Path path) {
                this.written = true;
                this.filePath = path;
                return this;
            }
            
            public Builder setBackupWritten(Path path) {
                this.backupWritten = true;
                this.backupPath = path;
                return this;
            }
            
            public Builder setWriteError(Throwable writeError) {
                this.writeError = writeError;
                return this;
            }
            
            public Builder setBackupError(Throwable backupError) {
                this.backupError = backupError;
                return this;
            }
            
            public Builder setRemoved() {
                this.removed = true;
                return this;
            }
            
            public Builder setCancelled(CancelReason reason) {
                this.cancelReason = reason;
                return this;
            }
            
            public SaveResult make() {
                return new SaveResult(this);
            }
            
        }
        
        public final String id;
        public final boolean written;
        public final boolean backupWritten;
        public final Throwable writeError;
        public final Throwable backupError;
        public final Path filePath;
        public final Path backupPath;
        public final boolean removed;
        public final CancelReason cancelReason;
        
        private SaveResult(Builder builder) {
            this.id = builder.id;
            this.written = builder.written;
            this.backupWritten = builder.backupWritten;
            this.writeError = builder.writeError;
            this.backupError = builder.backupError;
            this.filePath = builder.filePath;
            this.backupPath = builder.backupPath;
            this.removed = builder.removed;
            this.cancelReason = builder.cancelReason;
        }
        
    }
    
    public static class FileContentInfo {
        
        public final boolean isValid;
        public final String info;
        
        public FileContentInfo(boolean isValid, String info) {
            this.isValid = isValid;
            this.info = info;
        }
        
    }
    
    public static final void main(String[] args) throws IOException {
        FileManager m = new FileManager(Paths.get("H:\\test123"), Paths.get("H:\\test123\\backupp"));
        String content = "content\nabc\rblah\r\n";
        m.add("test", "filename", true, new FileContentInfoProvider() {

            @Override
            public FileContentInfo getInfo(String content) {
                if (content != null && !content.isEmpty()) {
                    return new FileContentInfo(true, content.length()+" characters");
                }
                return new FileContentInfo(false, "Empty file");
            }
        });
//        m.save("test", content);
//        
//        String read = m.loadFromFile(Paths.get("H:\\test123\\filename"));
//        System.out.println(read.equals(content));
        
//        for (FileInfo info : m.getFileInfo()) {
//            System.out.println(info);
//        }
//        
//        System.out.println(getTimestamp("abc"));
//        System.out.println(getTimestamp("auto_123456__"));
//        System.out.println(getTimestamp("auto_123456__abc"));
//        long a = -1;
//        long b = 3232323232323L;
//        System.out.println((a - System.currentTimeMillis()));
        
//        m.backup((int)DateTime.HOUR, 5);
        
//        BackupManager mg = new BackupManager(m);
//        mg.setModal(true);
//        mg.setLocationRelativeTo(null);
//        mg.open();
//        System.exit(0);
    }
    
}
