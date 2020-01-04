
package chatty.util;

import static chatty.util.CachedBulkManager.ASAP;
import static chatty.util.CachedBulkManager.REFRESH;
import static chatty.util.CachedBulkManager.WAIT;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import javax.swing.Timer;

/**
 *
 * @author tduva
 */
public class RetryManager {
    
    private final CachedBulkManager<Object, Boolean> manager = new CachedBulkManager<>(new CachedBulkManager.Requester<Object, Boolean>() {

        @Override
        public void request(CachedBulkManager<Object, Boolean> manager, Set<Object> asap, Set<Object> normal, Set<Object> backlog) {
            Debugging.println("retry", "request(%s, %s, %s)", asap, normal, backlog);
            Set<Object> keys = manager.makeAndSetRequested(asap, normal, backlog, 1);
            if (keys.isEmpty()) {
                return;
            }
            for (Object key : keys) {
                Consumer<Object> f = getFunction(key);
                if (f != null) {
                    f.accept(key);
                }
            }
        }
    }, "[RetryManager] ", CachedBulkManager.DAEMON | ASAP);
    
    private final Map<Object, Consumer<Object>> requests = new HashMap<>();
    
    private static final RetryManager INSTANCE = new RetryManager();
    
    public static RetryManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * For now, this is not being used but instead a request made directly in
     * case of forced refresh, which has the advantage of being pretty simple,
     * but the disadvantage of making another request even in the case when a
     * request is already pending. But when it is only manually triggered it
     * shouldn't be too bad. Still, looking into this again sometime may make
     * sense.
     */
//    /**
//     * Execute the function once, even if the same key already had a success or
//     * not found result. The function must always call one of the result methods
//     * (success, error or not found), otherwise the request will be pending forever.
//     * 
//     * @param key Each key can only appear once at a time, overwriting previous
//     * entries
//     * @param function 
//     */
//    public void forceOnce(Object key, Consumer<Object> function) {
//        synchronized(requests) {
//            Debugging.println("retry", "forceOnce(%s)", key);
//            requests.put(key, function);
//            manager.query(key, r -> clearRequest(key), REFRESH, key);
//        }
//    }
    
    /**
     * Keep retrying executing the function until success or not found. The
     * function must always call one of the result methods (success, error or
     * not found), otherwise the request will be pending forever.
     * 
     * If the same key already had a success or not found result the function is
     * not executed again.
     * 
     * @param key Each key can only appear once at a time, overwriting previous
     * entries
     * @param function The function to perform
     */
    public void retry(Object key, Consumer<Object> function) {
        synchronized(requests) {
            Debugging.println("retry", "retry(%s)", key);
            requests.put(key, function);
            /**
             * Using the key as unique means only the last query for this key is
             * kept, which means it is more consistent to what is added to
             * "requests", which is also always the last one.
             * 
             * However it also means a query can be overwritten with different
             * settings (if called from another method with different settings).
             */
            manager.query(key, r -> clearRequest(key), WAIT, key);
        }
    }
    
    private Consumer<Object> getFunction(Object key) {
        synchronized(requests) {
            return requests.get(key);
        }
    }
    
    private void clearRequest(Object key) {
        synchronized(requests) {
            if (!manager.hasQueryKey(key)) {
                requests.remove(key);
                Debugging.println("retry", "Removed %s [now: %s][%s]", key, requests, manager.debug());
            }
            else {
                Debugging.println("retry", "Didn't remove %s [now: %s]", key, requests);
            }
        }
    }
    
    public void setSuccess(Object key) {
        manager.setResult(key, Boolean.TRUE);
    }
    
    public void setNotFound(Object key) {
        manager.setNotFound(key);
    }
    
    public void setError(Object key) {
        manager.setError(key);
    }
    
    public static void main(String[] args) throws InterruptedException {
//        getInstance().retry("test", k -> {
//            System.out.println("Requesting "+k);
//            if (ThreadLocalRandom.current().nextBoolean()) {
//                System.out.println("Error");
//                getInstance().setError(k);
//            }
//            else {
//                System.out.println("Success");
//                getInstance().setSuccess(k);
//            }
//            System.out.println(getInstance().manager.debug());
//        });
        
//        getInstance().setSuccess("test");
//        getInstance().forceOnce("test", k -> {
//            System.out.println("Requesting "+k);
//            System.out.println(getInstance().manager.debug());
//            getInstance().setSuccess(k);
//            System.out.println(getInstance().manager.debug());
//        });
        
        Debugging.command("retry println cbm");
        
        getInstance().retry("test", k -> {
            Timer t = new Timer(50, e -> {
                System.out.println("Requesting A "+k);
                if (ThreadLocalRandom.current().nextBoolean()) {
                    getInstance().setError(k);
                }
                else {
                    System.out.println("A success");
                    getInstance().setSuccess(k);
                }
                System.out.println(getInstance().manager.debug());
            });
            t.setRepeats(false);
            t.start();
        });
        getInstance().retry("test", k -> {
            System.out.println("Requesting B "+k);
            System.out.println(getInstance().manager.debug());
            if (ThreadLocalRandom.current().nextBoolean()) {
                getInstance().setError(k);
            }
            else {
                System.out.println("B success");
                getInstance().setSuccess(k);
            }
            System.out.println(getInstance().manager.debug());
        });
        Thread.sleep(10*1000);
        System.out.println(getInstance().manager.debug());
        
        Thread.sleep(5000000);
    }
    
}
