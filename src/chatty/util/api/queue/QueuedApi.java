
package chatty.util.api.queue;

import chatty.util.Debugging;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class QueuedApi {
    
    private static final Logger LOGGER = Logger.getLogger(QueuedApi.class.getName());
    
    /**
     * Queued requests are removed when they are taken from the queue in order
     * to perform the request.
     */
    private final PriorityBlockingQueue<Entry> queue;
    
    /**
     * Pending requests are removed after the request is completed, so if the
     * server has answered or a timeout occured.
     */
    private final Set<Entry> requestPending = new HashSet<>();
    
    /**
     * Rate limit remaining from the most recent request response, or -1 if no
     * data is available yet.
     */
    private volatile int ratelimitRemaining = -1;
    
    /**
     * Limits the number of active requests. This is different to active threads
     * which could be more, if a thread e.g. still keeps going after the actual
     * API request (e.g. download badge images).
     * 
     * This is so that there aren't too many concurrent API requests that could
     * immediatelly use up all the ratelimit tokens (although this is quite
     * unlikely with the number of usual requests anyway).
     */
    private final Semaphore activeRequests = new Semaphore(10);
    
    public QueuedApi() {
        ExecutorService executor = Executors.newCachedThreadPool();
        queue = new PriorityBlockingQueue<>();
        
        Thread thread = new Thread(new Runnable() {
            
            @Override
            public void run() {
                while (true) {
                    try {
                        if (ratelimitRemaining != -1 && ratelimitRemaining < 60) {
                            LOGGER.info("Waiting..");
                            Thread.sleep(10*1000);
                        }
                        activeRequests.acquire();
                        //System.out.println("Waiting for entry.. Permits: "+activeRequests.availablePermits());
                        Entry entry = queue.take();
                        //System.out.println("Entry taken: "+entry.request+" Permits: "+activeRequests.availablePermits());
                        entry.request.setResultListener((result, responseCode, ratelimitRemaining) -> {
                            /**
                             * Executed in an executor thread.
                             */
                            // Get some data from the response and forward to external listener
                            QueuedApi.this.ratelimitRemaining = ratelimitRemaining;
                            activeRequests.release();
                            if (Debugging.isEnabled("requestresponse") && result != null) {
                                LOGGER.info(result);
                            }
                            // This may run a while (e.g. loading images etc.)
                            entry.listener.result(result, responseCode);
                            removePending(entry);
                            //System.out.println("Entry done: "+entry.request+" Permits: "+activeRequests.availablePermits());
                        });
                        executor.execute(entry.request);
                        
                    } catch (InterruptedException ex) {
                        // To stop the thread (currently not used)
                        LOGGER.warning("QueuedApi Thread interrupted");
                        break;
                    }
                }
            }
        }, "QueuedApi");
        thread.start();
    }
    
    /**
     * Perform a request.
     * 
     * @param url
     * @param requestMethod
     * @param token
     * @param listener 
     */
    public void add(String url, String requestMethod, String token,
            ResultListener listener) {
        Request request = new Request(url);
        request.setToken(token);
        request.setRequestType(requestMethod);
        addRequest(1, request, listener);
    }
    
    /**
     * Perform a request with JSON data.
     * 
     * @param url
     * @param requestMethod Only set if jsonData is not null
     * @param jsonData
     * @param token
     * @param listener 
     */
    public void add(String url, String requestMethod, String jsonData,
            String token, ResultListener listener) {
        Request request = new Request(url);
        request.setToken(token);
        if (jsonData != null) {
            request.setJSONData(requestMethod, jsonData);
        }
        addRequest(1, request, listener);
    }
    
    public void add(String url, String requestMethod, Map<String, String> data,
            String token, ResultListener listener) {
        Request request = new Request(url);
        request.setToken(token);
        if (data != null) {
            request.setJSONData(requestMethod, data);
        }
        addRequest(1, request, listener);
    }
    
    private void addRequest(int priority, Request request, ResultListener listener) {
        Entry entry = new Entry(priority, request, listener);
        if (checkPending(entry)) {
            queue.add(entry);
        } else {
            System.out.println("Duped "+request);
        }
    }
    
    private boolean checkPending(Entry entry) {
        synchronized(requestPending) {
            if (!requestPending.contains(entry)) {
                requestPending.add(entry);
                return true;
            }
            return false;
        }
    }
    
    private void removePending(Entry entry) {
        synchronized(requestPending) {
            requestPending.remove(entry);
        }
    }
    
    public static void main(String[] args) {
        QueuedApi api = new QueuedApi();
        ResultListener listener = new ResultListener() {

            @Override
            public void result(String result, int responseCode) {
                System.out.println("Result: "+responseCode+" "+result);
            }
        };
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
//        api.add("https://api.twitch.tv/helix/streams", null, null, listener);
    }

}
