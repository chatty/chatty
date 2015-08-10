
package chatty;

import chatty.util.DateTime;
import chatty.util.LogUtil;
import java.io.File;
import java.util.HashMap;

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
    public static final boolean HOTKEY = false;
    
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
     * Version number of this version of Chatty
     */
    public static final String VERSION = "0.8.1";
    
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
        HashMap<String, String> parsedArgs = parseArguments(args);
        if (parsedArgs.containsKey("cd")) {
            useCurrentDirectory = true;
        }
        new TwitchClient(parsedArgs);
        
        LogUtil.startMemoryUsageLogging();
    }
    
    /**
     * Parses the command line arguments into a HashMap for easier use.
     * 
     * Example:
     * -username abc -password abcdefg -connect
     * 
     * Gets parsed into 2 arguments with parameter and one argument with an
     * empty parameter.
     * 
     * @param args The string array of arguments
     * @return The Hashmap with argument/parameter pairs
     */
    static HashMap<String,String> parseArguments(String[] args) {
        HashMap<String,String> result = new HashMap<>();
        String argumentName = null;
        for (String arg : args) {
            if (arg.startsWith(("-"))) {
                // Everything starting with "-" is an argument
                if (argumentName != null && !result.containsKey(argumentName)) {
                    result.put(argumentName,"");
                }
                argumentName = arg.substring(1);
            }
            else if (argumentName != null) {
                // Everything else is a value of the argument
                if (result.containsKey(argumentName)) {
                    String currentArgument = result.get(argumentName);
                    result.put(argumentName,currentArgument+" "+arg);
                }
                else {
                    result.put(argumentName,arg);
                }
            }
        }
        if (!result.containsKey(argumentName)) {
            result.put(argumentName, "");
        }
        return result; 
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
    
    public static String chattyVersion() {
        return "Chatty Version "+Chatty.VERSION
                +(Chatty.HOTKEY ? " Hotkey" : "")
                +(Chatty.DEBUG ? " (Debug)" : "");
    }
    
    public static String uptime() {
        return DateTime.ago(STARTED_TIME);
    }
}
