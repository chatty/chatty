
package chatty.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple history with a fixed length, which allows you to add elements to it
 * and to move backwards and forwards through the history.
 * 
 * <p>
 * Fixed lenght means you provide a maximum size in the constructor and if that
 * number of elements is reached, the oldest element will be removed once
 * another element is added.
 * </p>
 * 
 * @author tduva
 */
public class WrapHistory<E> {
    
    /**
     * The data
     */
    private final List<E> data;
    
    /**
     * The maximum number of items that can be held at any time
     */
    private final int size;
    
    /**
     * The current position, either the last added item or the position when
     * moving backwards/forwards in the history
     */
    private int pos = -1;
    
    /**
     * Points to the oldest item
     */
    private int start = -1;
    
    /**
     * Points to the last added item
     */
    private int end;
    
    public WrapHistory(int size) {
        data = new ArrayList<>();
        this.size = size;
    }
    
    /**
     * Adds the element using {@code add(E)}, but only if the element at the
     * current position isn't equal to the one to add.
     * 
     * @param item The element to add if the condition is met
     * @see add(E)
     */
    public void addIfNew(E item) {
        if (!item.equals(current())) {
            add(item);
        }
    }
    
    /**
     * Moves the position forward and adds an element at that position, removing
     * any elements after it (if present). If the history is full, it replaces
     * the oldest element.
     * 
     * @param item The element to add
     */
    public void add(E item) {
        pos = (pos + 1) % size;
        if (data.size() <= pos) {
            data.add(item);
        } else {
            data.set(pos, item);
        }
        if (start == -1) {
            start = 0;
        }
        else if (pos == start) {
            // If the position where this element was added was the current
            // oldest element, then move the start one foward
            start = (start+1) % size;
        }
        // The latest added element is always the newest one (this won't be
        // equal anymore once forward()/backward() are being used which may
        // change pos)
        end = pos;
    }
    
    /**
     * Return the next element (if present), without moving the position in the
     * history.
     * 
     * @return The next element or {@code null} if none exists
     */
    public E peekForward() {
        int nextPos = (pos + 1) % size;
        if (nextPos >= data.size() || pos == end) {
            // Any index should never be greater than the data size, and if the
            // current position is already the latest added element, the next
            // one can't be a newer one
            return null;
        }
        return data.get(nextPos);
    }
    
    /**
     * Return the previous element (if present), without moving the position in
     * the history.
     * 
     * @return The element on the previous position, or {@code null} if none
     * exists
     */
    public E peekBackwards() {
        // Add size first to prevent -1
        int prevPos = (pos + size - 1) % size;
        if (prevPos >= data.size() || pos == start) {
            // Any index should never be greater than the data size, and if the
            // current position is already the oldest element, the previous one
            // can't be an older one
            return null;
        }
        return data.get(prevPos);
    }

    /**
     * Return the next element if present, while moving the position in the
     * history as well.
     * 
     * @return The next element, or {@code null} if none exists
     */
    public E forward() {
        E nextItem = peekForward();
        if (nextItem == null) {
            return null;
        }
        pos = (pos + 1) % size;
        //System.out.println("pos moved forward: "+pos);
        return nextItem;
    }
    
    /**
     * Return the previous element if present, while moving the position in the
     * history as well.
     * 
     * @return The previous element, or {@code null} if none exists
     */
    public E backward() {
        E prevItem = peekBackwards();
        if (prevItem == null) {
            return null;
        }
        pos = (pos + size - 1) % size;
        //System.out.println("pos moved backwards: "+pos);
        return prevItem;
    }
    
    /**
     * Return the element at the current position, if present.
     * 
     * @return The element on the current position or {@code null} if none
     * exists.
     */
    public E current() {
        if (pos < 0 || pos >= data.size()) {
            return null;
        }
        return data.get(pos);
    }
    
    /**
     * Check if there is an element after the current position.
     *
     * @return {@code true} if there is an element, {@code false} otherwise
     * @see peekForward()
     */
    public boolean hasNext() {
        return peekForward() != null;
    }

    /**
     * Check if there is an element before the current position.
     *
     * @return {@code true} if there is an element, {@code false} otherwise
     * @see peekBackwards()
     */
    public boolean hasPrevious() {
        return peekBackwards() != null;
    }
    
    /**
     * Returns a {@code String} with some debugging information.
     *
     * @return The debug {@code String}
     */
    public String debug() {
        return "Pos: "+pos+" First/Last: "+start+"/"+end+" Data: "+data;
    }
    
    /**
     * For easier testing.
     * 
     * @param args 
     */
    public static void main(String[] args) {
        WrapHistory<String> h = new WrapHistory<>(3);
        System.out.println("peekForward:"+h.peekForward());
        System.out.println("peekBackwards:"+h.peekBackwards());
        
        h.add("test1");
        System.out.println(h.debug());
        System.out.println("peekForward:"+h.peekForward());
        System.out.println("peekBackwards:"+h.peekBackwards());
        System.out.println(h.hasPrevious());
        h.add("test2");
        System.out.println(h.debug());
        System.out.println("peekForward:"+h.peekForward());
        System.out.println("forward:"+h.forward());
        System.out.println("peekBackwards:"+h.peekBackwards());
        h.add("test3");
        System.out.println(h.debug());
        h.add("test4");
        System.out.println(h.debug());
        System.out.println("peekBackwards:"+h.peekBackwards());
        System.out.println("backwards:"+h.backward());
        System.out.println("backwards:"+h.backward());
        //System.out.println("forward:"+h.forward());
        h.add("test5");
        System.out.println(h.debug());
        System.out.println("peekForward:"+h.peekForward());
        System.out.println("backwards:"+h.backward());
        System.out.println(h.debug());
        System.out.println("peekForward:"+h.peekForward());
        System.out.println("forward:"+h.forward());
        System.out.println("forward:"+h.forward());
        System.out.println("backwards:"+h.backward());
        System.out.println(h.debug());
        System.out.println("backwards:"+h.backward());
        System.out.println(h.debug());
        System.out.println("backwards:"+h.backward());
        System.out.println(h.debug());
        System.out.println("forward:"+h.forward());
        System.out.println(h.debug());
        System.out.println("backwards:"+h.backward());
        System.out.println(h.debug());
    }
    
}
