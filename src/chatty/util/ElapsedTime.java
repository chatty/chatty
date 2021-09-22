
package chatty.util;

import java.util.concurrent.TimeUnit;

/**
 * Measure elapsed time. Using System.nanoTime() to be independent of system
 * clock.
 * 
 * @author tduva
 */
public class ElapsedTime {
    
    private final Object LOCK = new Object();
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
     * Set to current time, so elapsed time is measured from this point on.
     */
    public void set() {
        this.time = ems();
    }
    
    /**
     * Identical to {@link set()}, except synchronized.
     */
    public void setSync() {
        synchronized(LOCK) {
            set();
        }
    }
    
    /**
     * Set to "not set", which means a really big number is returned as elapsed
     * time.
     */
    public void reset() {
        this.time = -1;
    }
    
    /**
     * Identical to {@link reset()}, except synchronized.
     */
    public void resetSync() {
        synchronized(LOCK) {
            reset();
        }
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
     * Identical to {@link isSet()}, except synchronized.
     * 
     * @return 
     */
    public boolean isSetSync() {
        synchronized(LOCK) {
            return isSet();
        }
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
     * Identical to {@link millisElapsed()}, except synchronized.
     * 
     * @return 
     */
    public long millisElapsedSync() {
        synchronized(LOCK) {
            return millisElapsed();
        }
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
     * Identical to {@link secondsElapsed()}, except synchronized.
     * 
     * @return 
     */
    public long secondsElapsedSync() {
        synchronized(LOCK) {
            return secondsElapsed();
        }
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
     * Identical to {@link millisElapsed(long)}, except synchronized.
     * 
     * @param milliseconds
     * @return 
     */
    public boolean millisElapsedSync(long milliseconds) {
        synchronized(LOCK) {
            return millisElapsed(milliseconds);
        }
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
     * Identical to {@link secondsElapsed(int)}, except synchronized.
     * 
     * @param seconds
     * @return 
     */
    public boolean secondsElapsedSync(int seconds) {
        synchronized(LOCK) {
            return secondsElapsed(seconds);
        }
    }
    
    /**
     * Checks if at least the given time in seconds has elapsed since the
     * starting point has been set.
     * 
     * @param seconds
     * @return true if a starting point is currently set and at least the given
     * time has elapsed, false otherwise
     */
    public boolean isSetAndSecondsElapsed(int seconds) {
        return isSet() && secondsElapsed(seconds);
    }
    
    /**
     * Identical to {@link isSetAndSecondsElapsed(int)}, except synchronized.
     *
     * @param seconds
     * @return 
     */
    public boolean isSetAndSecondsElapsedSync(int seconds) {
        synchronized(LOCK) {
            return isSet() && secondsElapsed(seconds);
        }
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
