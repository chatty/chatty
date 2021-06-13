
package chatty.util;

import chatty.Chatty;
import chatty.Helper;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class LogUtil {
    
    private static final Logger LOGGER = Logger.getLogger(LogUtil.class.getName());
    
    public static void logMemoryUsage() {
        LOGGER.info(getMemoryUsage());
    }
    
    public static String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return String.format("[Memory] max: %2$,d / %1$,d free: %3$,d [Uptime] %4$s",
                runtime.maxMemory() / 1024,
                runtime.totalMemory() / 1024,
                runtime.freeMemory() / 1024,
                Chatty.uptime());
    }
    
    public static String getAppInfo() {
        return Chatty.chattyVersion()+" "+getMemoryUsage()+" [System] "+Helper.systemInfo();
    }
    
    /**
     * Log JVM memory information every 15 minutes.
     */
    public static void startMemoryUsageLogging() {
        Timer t = new Timer(true);
        t.schedule(new TimerTask() {

            @Override
            public void run() {
                logMemoryUsage();
            }
        }, 10*1000, 900*1000);
    }
    
    public static void logDeadlocks() {
        long start = System.currentTimeMillis();
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] threadIds = bean.findDeadlockedThreads();
        
        if (threadIds != null) {
            ThreadInfo[] infos = bean.getThreadInfo(threadIds);
            
            for (ThreadInfo info : infos) {
                StackTraceElement[] stack = info.getStackTrace();
                StringBuilder b = new StringBuilder();
                for (StackTraceElement e : stack) {
                    b.append(e);
                    b.append("\n");
                }
                LOGGER.warning("Deadlock detected: "+b.toString());
            }
        }
        System.out.println("Deadlock check took "+(System.currentTimeMillis() - start)+" milliseconds");
    }
    
    public static void startDeadlockDetection() {
        LOGGER.info("Started Thread Deadlock Detection");
        Timer t = new Timer(true);
        t.schedule(new TimerTask() {

            @Override
            public void run() {
                logDeadlocks();
            }
        }, 10*1000, 60*1000);
    }
    
}
