
package chatty.util;

import java.util.LinkedList;

/**
 *
 * @author tduva
 * @param <T>
 */
public class RingBuffer<T> {
    
    private final int capacity;
    private final LinkedList<T> data;
    
    public RingBuffer(int capacity) {
        this.capacity = capacity;
        this.data = new LinkedList<>();
    }
    
    public synchronized void add(T item) {
        data.addLast(item);
        if (data.size() > capacity) {
            data.removeFirst();
        }
    }
    
    public synchronized LinkedList<T> getItems() {
        return new LinkedList<>(data);
    }
    
    @Override
    public synchronized String toString() {
        return String.format("[%d]%s", capacity, data);
    }
    
}
