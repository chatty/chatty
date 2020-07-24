
package chatty.gui.components.updating;

import chatty.util.DateTime;
import chatty.util.Debugging;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author tduva
 */
public class Stuff {
    
    private static final Logger LOGGER = Logger.getLogger(Stuff.class.getName());
    
    private static boolean initialized = false;
    private static Path javaHome = null;
    private static Path javawExe = null;
    private static Path chattyExe = null;
    private static Path chattyExeDir = null;
    private static Path jarPath = null;
    private static Path jarDir = null;
    private static Path tempDir = null;
    
    public static synchronized Path getTempDir() {
        checkInitialized();
        return tempDir;
    }
    
    public static synchronized boolean isStandalone() {
        checkInitialized();
        return chattyExe != null && javawExe == null;
    }
    
    /**
     * Check if the bare minimum of variables has been successfully set for an
     * install and Chatty restart to be possible.
     * 
     * @return true if install can be attempted
     */
    public static synchronized boolean installPossible() {
        checkInitialized();
        return jarDir != null && tempDir != null;
    }
    
    public static synchronized Path getInstallDir(boolean standaloneInstaller) {
        checkInitialized();
        if (standaloneInstaller) {
            return chattyExeDir;
        }
        return jarDir;
    }
    
    public static synchronized Path getJarPath() {
        checkInitialized();
        return jarPath;
    }
    
    public static synchronized Path getChattyExeDir() {
        checkInitialized();
        return chattyExeDir;
    }
    
    public static synchronized Path getChattyExe() {
        checkInitialized();
        return chattyExe;
    }
    
    public static synchronized Path getJavawExe() {
        checkInitialized();
        return javawExe;
    }
    
    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized");
        }
    }
    
    /**
     * Initialize variables used for updating.
     */
    public static synchronized void init() {
        init(determineJarPath());
    }
    
    public static Path determineJarPath() {
        try {
            Path jarPath = Paths.get(Stuff.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarPath.toString().endsWith(".jar")
                    || !Files.exists(jarPath)
                    || !Files.isRegularFile(jarPath)) {
                jarPath = null;
            }
            return jarPath;
        } catch (URISyntaxException ex) {
            LOGGER.warning("jar: "+ex);
        }
        return null;
    }
    
    /**
     * Initialize variables used for updating, with the jar directory set
     * manually (for testing).
     * 
     * @param jarPathTemp 
     */
    public static synchronized void init(Path jarPathTemp) {
        try {
            javaHome = Paths.get(System.getProperty("java.home"));

            Path javawExeTemp = javaHome.resolve("bin").resolve("javaw.exe");
            if (Files.exists(javawExeTemp)) {
                javawExe = javawExeTemp;
            } else {
                javawExe = null;
            }

            jarPath = jarPathTemp;
            if (jarPath != null) {
                jarDir = jarPath.getParent();
            } else {
                jarDir = null;
            }

            if (jarDir != null) {
                Path chattyExeTemp = javaHome.getParent().resolve("Chatty.exe");
                Path chattyExeTemp2 = jarDir.getParent().resolve("Chatty.exe");
                if (Files.exists(chattyExeTemp) && chattyExeTemp.equals(chattyExeTemp2)) {
                    chattyExe = chattyExeTemp;
                    chattyExeDir = chattyExe.getParent();
                } else {
                    chattyExe = null;
                }
            }

            Path tempDirTemp = Paths.get(System.getProperty("java.io.tmpdir"));
            if (Files.exists(tempDirTemp) && Files.isWritable(tempDirTemp)) {
                tempDir = tempDirTemp;
            }
        } catch (Exception ex) {
            LOGGER.warning("Error initializing stuff: " + ex);
        }
        LOGGER.info(String.format("Updating Stuff: javaHome: %s / javawExe: %s / jarPath: %s / chattyExe: %s / tempDir: %s",
                javaHome, javawExe != null ? "yes" : "no", jarPath, chattyExe, tempDir));
        initialized = true;
    }
    
    /**
     * Get the full path to the executable with the given name in the system
     * temp dir, with a timestamp added to ensure it's unique.
     *
     * @param name
     * @return 
     */
    public static Path getTempFilePath(String name) {
        return Stuff.getTempDir().resolve(name+"_"+System.currentTimeMillis()+".exe");
    }
    
    /**
     * Delete all files from the temp dir matching the naming scheme of the
     * setup/log files, that are older than 7 days.
     */
    public static void clearOldSetups() {
        checkInitialized();
        if (getTempDir() == null) {
            LOGGER.warning("Failed to delete old setup files: Invalid temp dir");
            return;
        }
        Instant oldIfBefore = Instant.now().minus(Duration.ofDays(7));
        Pattern fileNameCheck = Pattern.compile("Chatty_.*setup.*[0-9]+\\.exe(\\.log)?");
        LOGGER.info("Checking if old setup files should be deleted..");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                getTempDir(),
                file -> {
                    if (Debugging.isEnabled("updater")
                            && fileNameCheck.matcher(file.getFileName().toString()).matches()) {
                        Debugging.println(file.getFileName().toString());
                    }
                    return fileNameCheck.matcher(file.getFileName().toString()).matches()
                        && Files.getLastModifiedTime(file).toInstant().isBefore(oldIfBefore);
                            })) {
            for (Path file : stream) {
                try {
                    String age = DateTime.ago(Files.getLastModifiedTime(file).toMillis(), 0, 0, DateTime.H);
                    Files.delete(file);
                    LOGGER.info("Deleted old setup file ("+age+"): "+file);
                } catch (IOException ex) {
                    LOGGER.warning("Failed to delete old setup file: "+ex);
                }
            }
        } catch (IOException ex) {
            LOGGER.warning("Failed to delete old setup files: "+ex);
        }
    }
    
}
