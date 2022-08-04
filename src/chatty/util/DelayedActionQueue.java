
package chatty.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * A queue that items can be added to, that are send to the registered action
 * listener on a fixed delay (in between each item). The queue is FIFO.
 * 
 * @author tduva
 * @param <E> The type of the items
 */
public class DelayedActionQueue<E> {
    
    private static final Logger LOGGER = Logger.getLogger(DelayedActionQueue.class.getName());
    
    private final DelayedActionListener<E> listener;
    private final long delay;
    private final BlockingQueue<E> q = new LinkedBlockingQueue<>();

    /**
     * Create a new queue object and start it.
     * 
     * @param <T>
     * @param listener
     * @param delay
     * @return 
     */
    public static<T> DelayedActionQueue<T> create(DelayedActionListener<T> listener, long delay) {
        DelayedActionQueue<T> q = new DelayedActionQueue<>(listener, delay);
        q.start();
        return q;
    }
    
    /**
     * Creates a new instance.
     * 
     * @param listener The listener to send the items to (can't be {@code null})
     * @param delay The delay between items in milliseconds
     */
    private DelayedActionQueue(DelayedActionListener<E> listener, long delay) {
        this.listener = listener;
        this.delay = delay;
    }
    
    /**
     * Start a new reader thread. This should only be called once per instance.
     */
    private void start() {
        new Reader().start();
    }
    
    /**
     * Adds an item to the queue.
     * 
     * @param item 
     */
    public void add(E item) {
        q.add(item);
    }
    
    /**
     * Clears all elements from the queue.
     */
    public void clear() {
        q.clear();
    }
    
    /**
     * Thread that reads an item (or blocks if none is available) sends it to
     * the listener and then waits for the specified delay.
     */
    private class Reader extends Thread {
        
        private Reader() {
            super("DelayedActionQueue");
        }
        
        @Override
        public void run() {
            while (true) {
                try {
                    E item = q.take();
                    listener.actionPerformed(item);
                    sleep(delay);
                } catch (InterruptedException ex) {
                    LOGGER.warning("Reader Thread interrupted.");
                    break;
                }
            }
        }
        
    }

    public static interface DelayedActionListener<E> {
        public void actionPerformed(E item);
    }
    
}
