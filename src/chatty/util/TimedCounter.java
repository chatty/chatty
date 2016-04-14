
package chatty.util;

import java.util.LinkedList;

/**
 * Counts occurences in a given sliding time window.
 * 
 * @author tduva
 */
public class TimedCounter {

    private final LinkedList<Long> data = new LinkedList<>();
    private long interval;
    private final long accuracy;
    private long lastAdded;
    private int count;
    
    /**
     * Create a new counter with the given interval, that defines the sliding
     * time window for which these occurences should be counted (e.g. last
     * 2 minutes) and the given accuracy, which makes any occurences in this
     * time be counted as one.
     * 
     * The interval and accuracy also determine the maximum possible number of
     * the counter.
     * 
     * @param interval The length of the interval that should be saved in
     *  milliseconds.
     * @param accuracy For how long any occurences should be counted as one in
     *  milliseconds.
     */
    public TimedCounter(long interval, long accuracy) {
        this.interval = interval;
        this.accuracy = accuracy;
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
        if (accuracy > 0) {
            long timePassed = System.currentTimeMillis() - lastAdded;
            if (timePassed > accuracy) {
                clearUp();
                if (count > 0) {
                    data.addFirst(lastAdded + accuracy);
                }
                lastAdded = System.currentTimeMillis();
                count = 0;
            }
            count++;
        } else {
            clearUp();
            data.addFirst(System.currentTimeMillis());
        }
    }
    
    /**
     * Removes old elements that are no longer in the time window.
     */
    private void clearUp() {
        long limit = System.currentTimeMillis() - interval;
        while (!data.isEmpty() && data.getLast() < limit) {
            data.removeLast();
        }
    }
    
    /**
     * Changes the current interval.
     * 
     * @param interval Time in milliseconds.
     */
    public synchronized void setInterval(long interval) {
        this.interval = interval;
        clearUp();
    }
}
