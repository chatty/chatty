
package chatty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
    
    private static final String VERSION = Chatty.VERSION;
    private static final String VERSION_URL = Chatty.VERSION_URL;
    private static final String VERSION_TEST_URL = Chatty.VERSION_TEST_URL;
    
    private final VersionListener listener;
    
    public Version(VersionListener listener) {
        this.listener = listener;
        checkForNewVersion();
        //versionReceived("0.3.1 Auto updater, Bugfixes");
    }
    
    private void checkForNewVersion() {
        LOGGER.info("Checking for new version..");
        new Thread(new VersionChecker()).start();
    }
    
    /**
     * Works with the received version String and compares it to the
     * current version. Expects a version in the format: "x.y.z info text"
     * ("info text" is optional).
     * 
     * @param versionChecked The version String as received from the server
     */
    private void versionReceived(String newVersion) {
        LOGGER.info("Version checked: current:"+VERSION+"/new:"+newVersion);
        if (newVersion == null) {
            return;
        }
        // Split version number and version info text
        String[] split = newVersion.split(" ", 2);
        newVersion = split[0];
        String newVersionInfo = "";
        if (split.length > 1) {
            newVersionInfo = split[1];
        }
        
        // Compare versions
        String currentVersion = VERSION;
        boolean isNewVersion = compareVersions(currentVersion, newVersion) == 1;
        listener.versionChecked(newVersion, newVersionInfo, isNewVersion);
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
        public void versionChecked(String version, String info, boolean isNew);
    }
    
    /**
     * Requests the version file from the internet in a new Thread.
     */
    private class VersionChecker implements Runnable {

        @Override
        public void run() {
            URL url;
            HttpURLConnection connection;
            try {
                String versionUrl = VERSION_URL;
                if (Chatty.DEBUG) {
                    versionUrl = VERSION_TEST_URL;
                }
                url = new URL(versionUrl);
                connection = (HttpURLConnection)url.openConnection();

                InputStream input = connection.getInputStream();
                String line;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                    line = reader.readLine();
                }
                versionReceived(line);
            } catch (MalformedURLException ex) {
                LOGGER.warning("Invalid version URL.");
            } catch (IOException ex) {
                LOGGER.warning("Error checking for new version: "+ex.getLocalizedMessage());
            }
        }
        
    }
    
}
