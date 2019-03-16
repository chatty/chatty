
package chatty.util;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Counts occurences in a given sliding time window.
 * 
 * @author tduva
 */
public class TimedCounter {

    private final LinkedList<Long> data = new LinkedList<>();
    private long interval;
    
    /**
     * Create a new counter with the given interval, that defines the sliding
     * time window for which these occurences should be counted (e.g. last
     * 2 minutes).
     * 
     * @param interval The length of the interval that should be saved in
     *  milliseconds.
     */
    public TimedCounter(long interval) {
        this.interval = TimeUnit.MILLISECONDS.toNanos(interval);
    }
    
    public int getCount() {
        return getCount(true);
    }
    
    /**
     * Gets the current count of elements.
     * 
     * @param remove Whether to remove old elements.
     * @return The number of elements currently in the counter.
     */
    public synchronized int getCount(boolean remove) {
        if (remove) {
            clearUp();
        }
        return data.size();
    }
    
    /**
     * Adds an element to the counter or caches it to be added.
     */
    public synchronized void increase() {
        clearUp();
        data.addFirst(System.nanoTime());
    }
    
    /**
     * Removes old elements that are no longer in the time window.
     */
    private void clearUp() {
        while (!data.isEmpty() && System.nanoTime() - data.getLast() > interval) {
            data.removeLast();
        }
    }
    
    /**
     * Changes the current interval.
     * 
     * @param interval Time in milliseconds.
     */
    public synchronized void setInterval(long interval) {
        this.interval = TimeUnit.MILLISECONDS.toNanos(interval);
        clearUp();
    }
}
