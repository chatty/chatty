
package chatty.util.api.eventsub;

import chatty.util.ElapsedTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author tduva
 */
public abstract class Topic {
    
    private int cost = 0;
    private int errorCount = 0;
    private ElapsedTime lastError;
    
    public int getCost() {
        return cost;
    }
    
    public void setCost(int cost) {
        this.cost = cost;
    }
    
    private String id;
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public boolean shouldRequest() {
        if (lastError == null) {
            return true;
        }
        int delaySeconds = Math.min(3600, (int) Math.pow(10, errorCount)) + ThreadLocalRandom.current().nextInt(10);
        return lastError.secondsElapsed(delaySeconds);
    }
    
    public void increaseErrorCount() {
        errorCount++;
        if (lastError == null) {
            lastError = new ElapsedTime(true);
        }
        else {
            lastError.set();
        }
    }
    
    public abstract Topic copy();

    /**
     * Create the full topic name, including any data required for it.
     *
     * @param sessionId
     * @return The full topic name, or null if data is missing
     */
    public abstract String make(String sessionId);

    public abstract boolean isReady();

    /**
     * Request any data for this topic.
     */
    public abstract void request();

    public abstract int getExpectedCost();
}
