
package chatty.util;

import chatty.Chatty;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class Debugging {
    
    private static final long TIMED_OUTPUT_DELAY = 5000;
    
    private static final Set<String> enabled = new HashSet<>();
    private static final Map<String, Long> stopwatchData = new HashMap<>();
    private static final Set<OutputListener> outputListeners = new HashSet<>();
    private static final Map<String, String> timedOutput = new HashMap<>();
    private static final Map<String, Long> timedOutputTimes = new HashMap<>();
    private static final Timer timer;
    
    static {
        timer = new Timer("Debugging", true);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (!isEnabled("rt")) {
                    for (Map.Entry<String, String> entry : timedOutput.entrySet()) {
                        String type = entry.getKey();
                        String value = entry.getValue();
                        long lastValueAgo = System.currentTimeMillis() - timedOutputTimes.get(type);
                        if (lastValueAgo < TIMED_OUTPUT_DELAY) {
                            println(String.format("[%s] %s", type, value));
                        }
                    }
                }
            }
        }, TIMED_OUTPUT_DELAY, TIMED_OUTPUT_DELAY);
    }
    
    public synchronized static String command(String parameter) {
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
    
    public synchronized static boolean isEnabled(String... ids) {
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
    
    public synchronized static void registerForOutput(OutputListener listener) {
        if (listener != null) {
            outputListeners.add(listener);
        }
    }
    
    public synchronized static void println(String line) {
        for (OutputListener o : outputListeners) {
            o.debug(line);
        }
        Chatty.println(line);
    }
    
    public synchronized static void printlnf(String line, Object... args) {
        println(String.format(line, args));
    }
    
    public synchronized static void printlnTimed(String type, String line) {
        if (isEnabled("rt")) {
            println(line);
        }
        timedOutput.put(type, line);
        timedOutputTimes.put(type, System.currentTimeMillis());
    }
    
    public synchronized static void println(String type, String line, Object... args) {
        if (isEnabled(type)) {
            println(String.format(line, args));
        }
    }
    
    public static void edt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            println("!EDT "+StringUtil.join(st));
        }
    }
    
    public synchronized static long millisecondsElapsed(String id) {
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
