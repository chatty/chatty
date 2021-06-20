
package chatty.util;

import chatty.Chatty;
import chatty.Helper;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

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
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (!bean.isSynchronizerUsageSupported()) {
            LOGGER.warning("Unable to find deadlocks, synchronizer usage is not supported");
            return;
        }
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
        else {
            LOGGER.info("No deadlocks detected");
        }
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
    
    private static final AtomicInteger EDT_LOCK_STATE = new AtomicInteger();
    private static final ElapsedTime EDT_LOCK_LAST_LOGGED = new ElapsedTime();
    
    /**
     * Tries to set a value through the EDT regularly in order to detect when it
     * may have locked up (and logs the current thread info if so).
     */
    public static void startEdtLockDetection() {
        Thread thread = new Thread(() -> {
            int state = 0;
            while (true) {
                try {
                    Thread.sleep(30*1000);
                    // Change value through EDT
                    state++;
                    int toSet = state;
                    SwingUtilities.invokeLater(() -> {
                        EDT_LOCK_STATE.set(toSet);
                    });
                    Thread.sleep(1000);
                    // Should be set by now, otherwise the EDT is really slow
                    if (EDT_LOCK_STATE.get() != state && EDT_LOCK_LAST_LOGGED.secondsElapsed(600)) {
                        EDT_LOCK_LAST_LOGGED.set();
                        // No more often than every 10 minutes, log if EDT is slow
                        LOGGER.warning("EDT may be slow");
                        logThreadInfo();
                    }
                }
                catch (InterruptedException ex) {
                    return;
                }
            }
        }, "EdtLockDetection");
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Log a thread dump and try to detect deadlocked threads.
     */
    public static void logThreadInfo() {
        LOGGER.info("Thread Dump:\n"+threadDump());
        logDeadlocks();
    }
    
    public static String threadDump() {
        StringBuilder b = new StringBuilder();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfo = threads.dumpAllThreads(threads.isObjectMonitorUsageSupported(), threads.isSynchronizerUsageSupported());
        for (ThreadInfo info : threadInfo) {
            b.append("[");
            b.append(info.getThreadName());
            b.append("/");
            b.append(info.getThreadState());
            if (info.getLockName() != null) {
                b.append("|");
                b.append(info.getLockName());
                b.append("/");
                b.append(info.getLockOwnerName());
            }
            b.append("] ");
            b.append(StringUtil.join(info.getStackTrace()));
            b.append("\n");
        }
        return b.toString();
    }
    
}
