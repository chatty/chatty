
package chatty.gui.components.updating;

import chatty.Chatty;
import static chatty.Logging.USERINFO;
import chatty.gui.components.updating.Stuff;
import chatty.util.GitHub;
import chatty.util.GitHub.Release;
import chatty.util.GitHub.Releases;
import chatty.util.settings.Settings;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request a version file and check if it's a new version.
 * 
 * @author tduva
 */
public class Version {

    private static final Logger LOGGER = Logger.getLogger(Version.class.getName());
    
    /**
     * The interval to check version in (seconds)
     */
    private static final int CHECK_VERSION_INTERVAL = 60*60*24*2;
    
    private static final String VERSION = Chatty.VERSION;
    
    private final VersionListener listener;
    private final Settings settings;
    
    private Version(VersionListener listener, Settings settings) {
        this.listener = listener;
        this.settings = settings;
        //versionReceived("0.3.1 Auto updater, Bugfixes");
    }
    
    public static void check(Settings settings, VersionListener listener) {
        Version v = new Version(listener, settings);
        v.checkForNewVersion();
    }
    
    public static void request(Settings settings, VersionListener listener) {
        Version v = new Version(listener, settings);
        v.checkForNewVersionForced();
    }
    
    private void checkForNewVersion() {
        if (!settings.getBoolean("checkNewVersion")) {
            return;
        }
        /**
         * Check if enough time has passed since the last check.
         */
        long ago = System.currentTimeMillis() - settings.getLong("versionLastChecked");
        if (ago/1000 < CHECK_VERSION_INTERVAL) {
            /**
             * If not checking, check if update was detected last time.
             */
            String updateAvailable = settings.getString("updateAvailable");
            if (!updateAvailable.isEmpty()) {
                listener.versionChecked(updateAvailable, null);
            }
        } else {
            checkForNewVersionForced();
        }
    }
    
    public void checkForNewVersionForced() {
        settings.setLong("versionLastChecked", System.currentTimeMillis());
        LOGGER.log(USERINFO, "Checking for new version..");
        new Thread(() -> {
            Stuff.init();
            Stuff.clearOldSetups();
            versionReceived(GitHub.getReleases());
        }).start();
    }
    
    /**
     * Works with the received version String and compares it to the
     * current version. Expects a version in the format: "x.y.z info text"
     * ("info text" is optional).
     * 
     * @param versionChecked The version String as received from the server
     */
    private void versionReceived(Releases releases) {
        if (releases == null) {
            return;
        }
        Release release = releases.getLatest();
        if (releases.getLatestBeta() != null && settings.getBoolean("checkNewBeta")) {
            release = releases.getLatestBeta();
        }
        boolean isNewVersion = compareVersions(VERSION, release.getVersion()) == 1;
        if (isNewVersion) {
            settings.setString("updateAvailable", release.getVersion());
        }
        LOGGER.info(String.format("[UpdateCheck] Current: %s Latest: %s",
                VERSION, release.getVersion()));
        listener.versionChecked(isNewVersion ? release.getVersion() : null, releases);
    }
    
    /**
     * Compare version1 to version2 and return 1 if version2 is greater, -1
     * if version1 is greater and 0 if they are equal.
     * 
     * @param version1
     * @param version2
     * @return 
     */
    public static int compareVersions(String version1, String version2) {
        int[] v1 = versionToIntArray(version1);
        int[] v2 = versionToIntArray(version2);
        // Take the length of the longer version
        int length = v2.length;
        if (v1.length > v2.length) {
            length = v1.length;
        }
        for (int i=0; i<length; i++) {
            // Go through all parts, use 0 as default when no version part
            // for this version exists
            int vp1 = 0;
            int vp2 = 0;
            if (i < v1.length) {
                vp1 = v1[i];
            }
            if (i < v2.length) {
                vp2 = v2[i];
            }
            // If either is bigger than the other, there's no point in checking
            // any lower-level parts
            if (vp1 > vp2) {
                return -1;
            }
            if (vp2 > vp1) {
                return 1;
            }
        }
        return 0;
    }
    
    /**
     * Convert a version in the form "x.y.z" (any length) into an array of
     * integers "[x, y, z]".
     * 
     * The version may end in any letters and an optional number, optionally
     * separated by a dash (-), with the number put at the end of the array,
     * with a -1 as separator ("0.8.5b3" -> "[0, 8, 5, -1, 3]").
     * 
     * @param version
     * @return 
     */
    public static int[] versionToIntArray(String version) {
        int betaVersion = 0;
        Matcher m = Pattern.compile("([.0-9]+)(?:-?([a-z]+)([0-9]+)?)").matcher(version);
        if (m.matches()) {
            try {
                betaVersion = Integer.parseInt(m.group(3));
            } catch (NumberFormatException ex) {
                // Just keep default value
            }
            version = m.group(1);
        }
        
        String[] split = version.split("\\.");
        int[] intVersion = new int[split.length+(betaVersion > 0 ? 2 : 0)];
        
        for (int i=0;i<split.length;i++) {
            String part = split[i];
            int partI = 0;
            try {
                partI = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                // No action necessary, just use 0 as default
            }
            intVersion[i] = partI;
        }
        if (betaVersion > 0) {
            intVersion[intVersion.length - 2] = -1;
            intVersion[intVersion.length - 1] = betaVersion;
        }
        return intVersion;
    }
    
    public static interface VersionListener {
        public void versionChecked(String newVersion, GitHub.Releases releases);
    }
    
}
