
package chatty.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class BatchAction {
    
    private static final Map<Object, Runnable> queued = new HashMap<>();
    private static Timer timer;
    
    /**
     * When the id is not currently queued, the given Runnable will be executed
     * on the EDT after 10 milliseconds, with subsequent calls with the same id
     * being ignored until then, except for overwriting the Runnable (so that
     * the last one queued during the delay is executed).
     * 
     * @param id The id
     * @param runnable What to execute
     */
    public static void queue(Object id, Runnable runnable) {
        queue(id, 10, true, true, runnable);
    }
    
    /**
     * When the id is not currently queued, the given Runnable will be executed
     * after delay milliseconds, with subsequent calls with the same id being
     * ignored until then.
     * 
     * @param id The id
     * @param delay Time in milliseconds until execution
     * @param edt Whether to execute in EDT
     * @param overwrite Whether to overwrite the Runnable to execute (so the
     * last one queued during the delay is executed rather than the first)
     * @param runnable What to execute
     */
    public static void queue(Object id, long delay, boolean edt, boolean overwrite, Runnable runnable) {
        boolean scheduleTask = prepare(id, overwrite, runnable);
        if (scheduleTask) {
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    Runnable r;
                    synchronized (queued) {
                        r = queued.remove(id);
                    }
                    if (r != null) {
                        if (edt) {
                            SwingUtilities.invokeLater(() -> {
                                r.run();
                            });
                        }
                        else {
                            r.run();
                        }
                    }
                }
            }, delay);
        }
    }
    
    private static boolean prepare(Object id, boolean overwrite, Runnable runnable) {
        synchronized (queued) {
            if (!queued.containsKey(id)) {
                if (timer == null) {
                    timer = new Timer("BatchAction", true);
                }
                queued.put(id, runnable);
                return true;
            }
            else if (overwrite) {
                queued.put(id, runnable);
            }
            return false;
        }
    }
    
}
