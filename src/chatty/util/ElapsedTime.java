
package chatty.util;

import java.util.concurrent.TimeUnit;

/**
 * Measure elapsed time. Using System.nanoTime() to be independent of system
 * clock.
 * 
 * @author tduva
 */
public class ElapsedTime {
    
    private long time = -1;
    
    public ElapsedTime(boolean initNow) {
        if (initNow) {
            time = ems();
        }
    }
    
    public ElapsedTime() {
        this(false);
    }
    
    /**
     * Measure elapsed time from this point on.
     */
    public void set() {
        this.time = ems();
    }
    
    /**
     * Set to "not set", which means a really big number is returned as elapsed
     * time.
     */
    public void reset() {
        this.time = -1;
    }
    
    /**
     * Check if a starting point for measure elapsed time has been set. If this
     * returns false, then the values returned are not really the elapsed time
     * but just a really big number.
     * 
     * @return 
     */
    public boolean isSet() {
        return time != -1;
    }
    
    /**
     * The number of milliseconds elapsed or Long.MAX_VALUE if not starting
     * point has been set.
     * 
     * @return The number of milliseconds elapsed
     */
    public long millisElapsed() {
        if (time == -1) {
            return Long.MAX_VALUE;
        }
        return ems() - time;
    }
    
    /**
     * The number of seconds elapsed or Long.MAX_VALUE if not starting point has
     * been set.
     *
     * @return The number of seconds elapsed
     */
    public long secondsElapsed() {
        if (time == -1) {
            return Long.MAX_VALUE;
        }
        return TimeUnit.MILLISECONDS.toSeconds(ems() - time);
    }
    
    /**
     * Checks if at least the given time in milliseconds has elapsed, or if no
     * starting point has been set.
     * 
     * @param milliseconds
     * @return true if at least the given time has elapsed or no starting point
     * has been set, false otherwise
     */
    public boolean millisElapsed(long milliseconds) {
        if (time == -1) {
            return true;
        }
        return millisElapsed() >= milliseconds;
    }
    
    /**
     * Checks if at least the given time in seconds has elapsed, or if no
     * starting point has been set.
     * 
     * @param seconds
     * @return true if at least the given time has elapsed or no starting point
     * has been set, false otherwise
     */
    public boolean secondsElapsed(int seconds) {
        return millisElapsed(TimeUnit.SECONDS.toMillis(seconds));
    }
    
    /**
     * Helper function, outputs the current System.nanoTime() converted to
     * milliseconds.
     * 
     * @return 
     */
    public static long ems() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
    
}
