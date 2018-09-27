
package chatty.util;

import chatty.Chatty;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class Debugging {
    
    private static final Set<String> enabled = new HashSet<>();
    private static final Map<String, Long> stopwatchData = new HashMap<>();
    private static final Set<OutputListener> outputListeners = new HashSet<>();
    
    public static String command(String parameter) {
        if (parameter == null) {
            return "Invalid parameter";
        }
        for (String id : parameter.split(" ")) {
            if (id.startsWith("+")) {
                enabled.add(id.substring(1));
            } else if (id.startsWith("-")) {
                enabled.remove(id.substring(1));
            } else {
                if (enabled.contains(id)) {
                    enabled.remove(id);
                } else {
                    enabled.add(id);
                }
            }
        }
        return "Now: "+enabled;
    }
    
    public static boolean isEnabled(String... ids) {
        if (enabled.isEmpty()) {
            return false;
        }
        for (String id : ids) {
            if (enabled.contains(id)) {
                return true;
            }
        }
        return false;
    }
    
    public static void registerForOutput(OutputListener listener) {
        if (listener != null) {
            outputListeners.add(listener);
        }
    }
    
    public static void println(String line) {
        for (OutputListener o : outputListeners) {
            o.debug(line);
        }
        Chatty.println(line);
    }
    
    public static void println(String type, String line) {
        if (isEnabled(type)) {
            println(line);
        }
    }
    
    public static void edt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            println("!EDT "+StringUtil.join(st));
        }
    }
    
    public static long millisecondsElapsed(String id) {
        Long previous = stopwatchData.get(id);
        stopwatchData.put(id, System.currentTimeMillis());
        if (previous == null) {
            return -1;
        }
        return System.currentTimeMillis() - previous;
    }
    
    public interface OutputListener {
        
        public void debug(String line);
    }
    
    /**
     * For filtering debug output that may contain commandline parameters
     * containing a token (not commonly used, but possible).
     * 
     * @param input
     * @return 
     */
    public static String filterToken(String input) {
        return input.replaceAll("(-set:token|-token|-password) \\w+", "$1 <token>");
    }
    
    // For testing
    public static void main(String[] args) {
        System.out.println(filterToken("-d \"G:\\chatty settings\" -set:token abc -token abc -password -password abc -connect"));
    }
    
}
