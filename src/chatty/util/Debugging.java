
package chatty.util;

import chatty.Chatty;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Stuff to help with debugging.
 * 
 * @author tduva
 */
public class Debugging {
    
    private static final Logger LOGGER = Logger.getLogger(Debugging.class.getName());
    
    private static final long TIMED_OUTPUT_DELAY = 5000;
    
    private static final Set<String> enabled = new HashSet<>();
    private static final Map<String, Long> stopwatchData = new HashMap<>();
    private static final Set<OutputListener> outputListeners = new HashSet<>();
    private static final Map<String, String> timedOutput = new HashMap<>();
    private static final Map<String, Long> timedOutputTimes = new HashMap<>();
    private static final Timer timer;
    private static final Map<String, Long> counter = new HashMap<>();
    
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
    
    public static void edtLoud() {
        if (!SwingUtilities.isEventDispatchThread()) {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            LOGGER.warning("!EDT "+StringUtil.join(st));
        }
    }
    
    /**
     * Get the amount of milliseconds that have passed since the last time this
     * was called with the given id. The time is updated every time this is
     * called.
     * 
     * @param id The identifier, can be any string
     * @return The amount of milliseconds that have passed since the last call
     * (for the given id)
     */
    public synchronized static long millisecondsElapsed(String id) {
        Long previous = stopwatchData.get(id);
        stopwatchData.put(id, System.currentTimeMillis());
        if (previous == null) {
            return -1;
        }
        return System.currentTimeMillis() - previous;
    }
    
    /**
     * Test if at least the given amount of milliseconds has passed since the
     * last time this was called for the given id. The time is updated every
     * time this is called, so there has to be a long enough break between
     * whatever events this is used for to return true. If this has not yet been
     * called with the given id it will return true.
     * 
     * @param id The identifier, can be any string
     * @param milliseconds The amount of milliseconds
     * @return true if enough time has passed since the last call or it hasn't
     * been called yet (for the given id)
     */
    public synchronized static boolean millisecondsElapsed(String id, long milliseconds) {
        long elapsed = millisecondsElapsed(id);
        return elapsed == -1 || elapsed >= milliseconds;
    }
    
    /**
     * Same as {@link millisecondsElapsed(String, long)}, except that the time
     * is updated only when this returns true.
     * 
     * @param id The identifier, can be any string
     * @param milliseconds The amount of milliseconds
     * @return true if enough time has passed since the last time this returned
     * true or it hasn't been called yet (for the given id)
     */
    public synchronized static boolean millisecondsElapsedLenient(String id, long milliseconds) {
        Long previous = stopwatchData.get(id);
        if (previous == null
                || System.currentTimeMillis() - previous >= milliseconds) {
            stopwatchData.put(id, System.currentTimeMillis());
            return true;
        }
        return false;
    }
    
    public synchronized static long count(String key) {
        long value = 0;
        if (counter.containsKey(key)) {
            value = counter.get(key) + 1;
        }
        counter.put(key, value);
        return value;
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
    
    public static void writeToFile(String output) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("E:\\abcdtest"), Charset.forName("UTF-8"))) {
            writer.append(output);
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    public static String getStacktrace(Exception ex) {
        try {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        } catch (Exception ex2) {
            LOGGER.warning("Error occured trying to get stacktrace: "+ex2);
        }
        return null;
    }
    
    public static String getStacktraceFiltered(Exception ex) {
        try {
            return ex+"\n\t"+StringUtil.join(filterStacktrace(ex.getStackTrace()), "\n\t ");
        }
        catch (Exception ex2) {
            LOGGER.warning("Error occured trying to get stacktrace: "+ex2);
        }
        return null;
    }
    
    public static String getStacktraceFilteredFlat(Exception ex) {
        try {
            return ex+" ["+StringUtil.join(filterStacktrace(ex.getStackTrace()), ",")+"]";
        }
        catch (Exception ex2) {
            LOGGER.warning("Error occured trying to get stacktrace: "+ex2);
        }
        return null;
    }
    
    private static final Set<String> STACKTRACE_FILTER = new HashSet<>();
    
    static {
        STACKTRACE_FILTER.add("java.awt.EventDispatchThread");
        STACKTRACE_FILTER.add("java.awt.EventQueue");
        STACKTRACE_FILTER.add("java.awt.DefaultKeyboardFocusManager");
    }
    
    public static List<String> filterStacktrace(StackTraceElement[] st) {
        List<String> result = new ArrayList<>();
        String filtered = null;
        for (int i = 0; i < st.length; i++) {
            StackTraceElement el = st[i];
            boolean show = true;
            for (String filter : STACKTRACE_FILTER) {
                if (el.getClassName().startsWith(filter)) {
                    if (!filter.equals(filtered)) {
                        result.add("["+filter+"]");
                        filtered = filter;
                    }
                    show = false;
                }
            }
            if (show) {
                result.add(el.toString());
            }
        }
        return result;
    }
    
    public static String getStacktrace() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return StringUtil.join(st);
    }
    
    public static String getCurrenThreadInfo() {
        return getThreadInfo(Thread.currentThread());
    }
    
    public static String getThreadInfo(Thread thread) {
        StringBuilder b = new StringBuilder();
        b.append("[").append(thread.getName()).append("/").append(thread.getState()).append("]");
        b.append(StringUtil.join(thread.getStackTrace()));
        return b.toString();
    }
    
    // For testing
    public static void main(String[] args) {
        System.out.println(filterToken("-d \"G:\\chatty settings\" -set:token abc -token abc -password -password abc -connect"));
    }
    
}
