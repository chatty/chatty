
package chatty.util.api.queue;

/**
 *
 * @author tduva
 */
public interface RequestResultListener {
    
    public void requestResult(String result, int responseCode, int ratelimitRemaining);
    
}
