
package chatty.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class UniqueLimitedRingBuffer<T> {

    private final LinkedList<T> list = new LinkedList<>();
    private final Set<T> set = new HashSet<>();
    private final int capacity;
    
    public UniqueLimitedRingBuffer(int capacity) {
        this.capacity = capacity;
    }
    
    public void append(T e) {
        if (set.add(e)) {
            list.addLast(e);
        }
        else {
            list.remove(e);
            list.addLast(e);
        }
        if (list.size() > capacity) {
            list.removeFirst();
        }
    }

    public T pollLast() {
        T removed = list.pollLast();
        set.remove(removed);
        return removed;
    }
    
    public void remove(T o) {
        list.remove(o);
        set.remove(o);
    }

}
