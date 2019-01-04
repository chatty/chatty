
package chatty.util.api.queue;

import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class QueuedApi {
    
    private static final Logger LOGGER = Logger.getLogger(QueuedApi.class.getName());
    
    private final PriorityBlockingQueue<Entry> queue;
    private int ratelimitRemaining = -1;
    
    public QueuedApi() {
        queue = new PriorityBlockingQueue<>();
        
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        if (ratelimitRemaining != -1 && ratelimitRemaining < 20) {
                            LOGGER.info("Waiting..");
                            Thread.sleep(10*1000);
                        }
                        Entry entry = queue.take();
                        entry.request.setResultListener((result, responseCode, ratelimitRemaining) -> {
                            QueuedApi.this.ratelimitRemaining = ratelimitRemaining;
                            entry.listener.result(result, responseCode);
                        });
                        entry.request.run();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(QueuedApi.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        thread.start();
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
        //if (!queue.contains(entry)) {
            queue.add(entry);
        //}
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
