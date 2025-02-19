
package chatty;

import chatty.gui.components.updating.Stuff;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.ElapsedTime;
import chatty.util.LogUtil;
import chatty.util.MiscUtil;
import chatty.util.SingleInstance;
import chatty.util.settings.Settings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Main class that starts the client as well as parses the commandline
 * parameters and provides some app-specific global constants.
 * 
 * @author tduva
 */
public class Chatty {
    
    /**
     * Enables debug commands as well as some other behaviour that allows
     * debugging. Should be disabled for a regular release.
     */
    public static final boolean DEBUG = false;
    
    /**
     * Enables global hotkeys.
     */
    public static final boolean HOTKEY = true;
    
    /**
     * The Chatty website as it can be opened in the menu.
     */
    public static final String WEBSITE =
            "https://chatty.github.io";
    
    /**
     * The Twitch client id of this program.
     */
    public static final String CLIENT_ID = "spyiu9jqdnfjtwv6l1xjk5zgt8qb91l";
    
    /**
     * The redirect URI for getting a token.
     */
    public static final String REDIRECT_URI = "http://127.0.0.1:61324/token/";
    
    /**
     * Version number of this version of Chatty, consisting of numbers separated
     * by points. May contain a single "b" for beta versions, which are counted
     * as older (so 0.8.7b4 is older than 0.8.7).
     */
    public static final String VERSION = "0.27-b4"; // Remember changing the version in the help
    
    /**
     * Enable Version Checker (if you compile and distribute this yourself, you
     * may want to disable this)
     */
    public static final boolean VERSION_CHECK_ENABLED = true;
    
    /**
     * The regular URL of the textfile where the most recent version is stored.
     */
    public static final String VERSION_URL = "http://chatty.github.io/version.txt";
    
    /**
     * For testing purposes.
     */
    public static final String VERSION_TEST_URL = "http://127.0.0.1/twitch/version.txt";
    
    /**
     * For use with the -single commandline argument, if no port is specified.
     * Unregistered port from the User Ports range.
     */
    private static final int DEFAULT_SINGLE_INSTANCE_PORT = 48724;
    
    // End of configuration
    
    /**
     * When this program was started
     */
    private static final ElapsedTime UPTIME = new ElapsedTime(true);
    
    private static final long STARTED_TIME = System.currentTimeMillis();
    
    private final static CustomPaths paths = new CustomPaths();
    
    private static String[] args;
    
    /**
     * Parse the commandline arguments and start the actual chat client.
     * 
     * @param args The commandline arguments.
     */
    public static void main(String[] args) {
        
        Chatty.args = args;
        
        Map<String, String> parsedArgs = MiscUtil.parseArgs(args);
        
        /**
         * Continue only if single instance mode isn't enabled or registering
         * succeeded.
         */
        if (parsedArgs.containsKey("single")) {
            int port = getInstancePort(parsedArgs);
            if (!SingleInstance.registerInstance(port)) {
                SingleInstance.notifyRunningInstance(port,
                        encodeParametersToJSON(parsedArgs));
                // Exit program
                return;
            }
        }
        
        if (parsedArgs.containsKey("appwdir") && !parsedArgs.containsKey("regularwdir")) {
            Path path = Stuff.determineJarPath();
            if (path != null) {
                path = path.getParent();
                paths.setCustom(PathType.WORKING, path, "-appwdir", false);
                // Afterwards set the property as well in case a path is created
                // somewhere that relies on it
                System.setProperty("user.dir", path.toString());
            }
        }
        
        if (parsedArgs.containsKey("cd")) {
            paths.setCustom(PathType.SETTINGS, System.getProperty("user.dir"), "-cd", false);
        }
        if (parsedArgs.containsKey("portable")) {
            Path path = Stuff.determineJarPath();
            if (path != null) {
                paths.setCustom(PathType.SETTINGS, path.getParent().resolve("portable_settings"), "-portable", false);
            }
        }
        if (parsedArgs.containsKey("d")) {
            paths.setCustom(PathType.SETTINGS, parsedArgs.get("d"), "-d", true);
        }
        if (parsedArgs.containsKey("debugdir")) {
            paths.setCustom(PathType.DEBUG, parsedArgs.get("debugdir"), "-debugdir", true);
        }
        
        final TwitchClient client = new TwitchClient(parsedArgs);
        
        // Adding listener just in case, will do nothing if not used
        SingleInstance.setNewInstanceListener(message -> {
            Map<String, String> args1 = decodeParametersFromJSON(message);
            if (args1.containsKey("channel")) {
                String channel = args1.get("channel");
                client.joinChannels(Helper.parseChannelsFromString(channel, false));
            }
            client.customCommandLaunch(args1.get("cc"));
        });
        
        LogUtil.startMemoryUsageLogging();
        LogUtil.startEdtLockDetection();
    }
    
    /**
     * Encodes the given Map of parameters to a JSON String.
     * 
     * @param parameters The Map of parsed parameters
     * @return The JSON String
     */
    @SuppressWarnings("unchecked") // Raw type
    private static String encodeParametersToJSON(Map<String, String> parameters) {
        JSONObject object = new JSONObject();
        object.putAll(parameters);
        return object.toJSONString();
    }

    /**
     * Decodes the given JSON String into a Map of parameters.
     * 
     * @param json The JSON String
     * @return The Map of parameters or an empty Map if any error occurs
     */
    private static Map<String, String> decodeParametersFromJSON(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            Map<String, String> result = new HashMap<>();
            for (Object key : root.keySet()) {
                result.put((String)key, (String)root.get(key));
            }
            return result;
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }
    
    private static int getInstancePort(Map<String, String> args) {
        int port = DEFAULT_SINGLE_INSTANCE_PORT;
        String arg = args.get("single");
        if (arg != null) {
            try {
                port = Integer.parseInt(arg);
            } catch (NumberFormatException ex) {
                // Use default
            }
        }
        return port;
    }
    
    //--------------------------
    // Paths
    //--------------------------
    public enum PathType {
        SETTINGS(() -> Paths.get(System.getProperty("user.home"), ".chatty"), null),
        WORKING(() -> Paths.get(System.getProperty("user.dir")), null),
        BACKUP(() -> getUserDataDirectory().resolve("backup"), null),
        IMAGE(() -> getWorkingDirectory().resolve("img"), "imgPath"),
        DEBUG(() -> getUserDataDirectory().resolve("debuglogs"), null),
        CACHE(() -> getUserDataDirectory().resolve("cache"), "cachePath"),
        SOUND(() -> getWorkingDirectory().resolve("sounds"), "soundsPath"),
        EXPORT(() -> getUserDataDirectory().resolve("exported"), "exportPath"),
        LOGS(() -> getUserDataDirectory().resolve("logs"), "logPath");
        
        public final Supplier<Path> createDefault;
        public final String settingName;
        
        PathType(Supplier<Path> createDefault, String settingName) {
            this.createDefault = createDefault;
            this.settingName = settingName;
        }
        
    }
    
    private static Path getUserDataDirectory() {
        return paths.getPath(PathType.SETTINGS);
    }
    
    private static Path getWorkingDirectory() {
        return paths.getPath(PathType.WORKING);
    }
    
    /**
     * Turns the given path into an absolute path by using the current working
     * directory. This can be necessary if the "-appwdir" option was used, since
     * setting the "user.dir" property doesn't necessarily affect the regular
     * functions (such as Paths.get("test").toAbsolutePath()).
     *
     * @param path
     * @return The absolute path, or the given path if it is already absolute
     */
    public static Path toAbsolutePathWdir(Path path) {
        if (path.isAbsolute()) {
            return path;
        }
        return Paths.get(System.getProperty("user.dir"), path.toString());
    }
    
    public static Path getPath(PathType type) {
        return paths.getPath(type);
    }

    public static Path getPathCreate(PathType type) {
        return paths.getPathAndCreate(type);
    }
    
    public static void updateCustomPathFromSettings(PathType type) {
        paths.updateFromSettings(type);
    }
    
    public static void setCustomPath(PathType type, Path path) {
        paths.setCustom(type, path, null, true);
    }
    
    public static Path getDefaultPath(PathType type) {
        return paths.get(type).getDefault();
    }
    
    public static Path getOrigPath(PathType type) {
        return paths.get(type).getOrig();
    }
    
    public static String getPathDebug() {
        return paths.getDebugInfo();
    }
    
    public static String getCustomPathInfo(PathType type) {
        return paths.get(type).getCustomInfo();
    }
    
    public static String getPathInfo(PathType type) {
        return paths.getInfo(type);
    }

    public static String getInvalidCustomPath(PathType type) {
        return paths.get(type).getInvalid();
    }
    
    public static String getInvalidPathInfo() {
        return paths.getInvalidInfo();
    }
    
    public static void setSettings(Settings settings) {
        paths.setSettings(settings);
    }
    
    public static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }
    
    //--------------------------
    // Other
    //--------------------------
    public static String chattyVersion() {
        return String.format("Chatty Version %s%s%s",
                Chatty.VERSION,
                (Chatty.HOTKEY ? " Hotkey": ""),
                (Chatty.DEBUG ? " (Debug)" : ""));
    }
    
    public static String uptime() {
        return DateTime.duration(UPTIME.millisElapsedSync());
    }
    
    public static long uptimeMillis() {
        return UPTIME.millisElapsedSync();
    }
    
    public static long uptimeSeconds() {
        return UPTIME.secondsElapsedSync();
    }
    
    /**
     * The time in milliseconds when Chatty was started. For uptime using one of
     * the uptime functions is probably preferable, since they aren't influenced
     * by system time changes.
     *
     * @return 
     */
    public static long startedTime() {
        return STARTED_TIME;
    }
    
    public static String[] getArgs() {
        return args;
    }
    
    /**
     * Only println when the DEBUG flag is enabled.
     * 
     * @param output 
     */
    public static void println(String output) {
        if (Chatty.DEBUG || Debugging.isEnabled("println")) {
            System.out.println(output);
        }
    }
    
}
