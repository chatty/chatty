
package chatty;

import chatty.util.DateTime;
import chatty.util.LogUtil;
import chatty.util.MiscUtil;
import chatty.util.SingleInstance;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
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
     * Enables the hotkey feature for running commercials (windows only).
     */
    public static final boolean HOTKEY = MiscUtil.OS_WINDOWS;
    
    /**
     * The Chatty website as it can be opened in the menu.
     */
    public static final String WEBSITE =
            "http://chatty.github.io";
    
    /**
     * The Twitch client id of this program.
     * 
     * If you compile this program yourself, you should create your own client
     * id on http://www.twitch.tv/kraken/oauth2/clients/new
     */
    public static final String CLIENT_ID = "spyiu9jqdnfjtwv6l1xjk5zgt8qb91l";
    
    /**
     * The redirect URI for getting a token.
     */
    public static final String REDIRECT_URI = "http://127.0.0.1:61324/token/";
    
    /**
     * Version number of this version of Chatty, consisting of numbers seperated
     * by points. May contain a single "b" for beta versions, which are counted
     * as older (so 0.8.7b4 is older than 0.8.7).
     */
    public static final String VERSION = "0.8.7";
    
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
    
    public static final String COMPILE_INFO = "JDK8";
    
    /**
     * For use with the -single commandline argument, if no port is specified.
     * Unregistered port from the User Ports range.
     */
    private static final int DEFAULT_SINGLE_INSTANCE_PORT = 48724;
    
    // End of configuration
    
    /**
     * When this program was started
     */
    public static final long STARTED_TIME = System.currentTimeMillis();

    /**
     * If true, use the current working directory to save settings etc.
     */
    private static boolean useCurrentDirectory = false;
    
    /**
     * Parse the commandline arguments and start the actual chat client.
     * 
     * @param args The commandline arguments.
     */
    public static void main(String[] args) {
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
        
        if (parsedArgs.containsKey("cd")) {
            useCurrentDirectory = true;
        }

        final TwitchClient client = new TwitchClient(parsedArgs);
        
        // Adding listener just in case, will do nothing if not used
        SingleInstance.setNewInstanceListener(new SingleInstance.NewInstanceListener() {

            @Override
            public void newInstance(String message) {
                Map<String, String> args = decodeParametersFromJSON(message);
                if (args.containsKey("channel")) {
                    String channel = args.get("channel");
                    client.joinChannels(Helper.parseChannelsFromString(channel, false));
                }
            }
        });
        
        LogUtil.startMemoryUsageLogging();
    }
    
    /**
     * Encodes the given Map of parameters to a JSON String.
     * 
     * @param parameters The Map of parsed parameters
     * @return The JSON String
     */
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
            return new HashMap();
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
    
    /**
     * Gets the directory to save data in (settings, cache) and also creates it
     * if necessary.
     *
     * @return
     */
    public static String getUserDataDirectory() {
        if (useCurrentDirectory) {
            return System.getProperty("user.dir") + File.separator;
        }
        String dir = System.getProperty("user.home")
                + File.separator 
                + ".chatty" 
                + File.separator;
        new File(dir).mkdirs();
        return dir;
    }
    
    public static String getExportDirectory() {
        String dir = getUserDataDirectory()+"exported"+File.separator;
        new File(dir).mkdirs();
        return dir;
    }
    
    public static String getCacheDirectory() {
        String dir = getUserDataDirectory()+"cache"+File.separator;
        new File(dir).mkdirs();
        return dir;
    }
    
    public static String getWorkingDirectory() {
        return System.getProperty("user.dir")+File.separator;
    }
    
    public static String getSoundDirectory() {
        return getWorkingDirectory()+"sounds"+File.separator;
    }
    
    public static String getImageDirectory() {
        return getWorkingDirectory()+"img"+File.separator;
    }
    
    public static String getBackupDirectory() {
        return getUserDataDirectory()+"backup"+File.separator;
    }
    
    public static String getDebugLogDirectory() {
        return getUserDataDirectory()+"debuglogs"+File.separator;
    }
    
    public static String chattyVersion() {
        return String.format("Chatty Version %s%s%s / %s",
                Chatty.VERSION,
                (Chatty.HOTKEY ? " Hotkey": ""),
                (Chatty.DEBUG ? " (Debug)" : ""),
                COMPILE_INFO);
    }
    
    public static String uptime() {
        return DateTime.ago(STARTED_TIME);
    }
}
