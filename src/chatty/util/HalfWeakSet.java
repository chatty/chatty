
package chatty.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

/**
 *
 * @author tduva
 */
public class HalfWeakSet<T> implements Set<T> {

    private final Set<T> strong = new HashSet<>();
    private final Set<T> weak = Collections.newSetFromMap(
        new WeakHashMap<T, Boolean>());
    
    @Override
    public int size() {
        return strong.size() + weak.size();
    }

    @Override
    public boolean isEmpty() {
        return strong.isEmpty() && weak.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return strong.contains(o) || weak.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new MyIterator();
    }
    
    public Iterator<T> strongIterator() {
        return strong.iterator();
    }

    @Override
    public Object[] toArray() {
        return null;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(T e) {
        if (!weak.contains(e)) {
            return strong.add(e);
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return strong.remove(o) || weak.remove(o);
    }
    
    public void markWeak(T o) {
        if (strong.remove(o)) {
            weak.add(o);
        }
    }
    
    public void markStrong(T o) {
        if (weak.remove(o)) {
            strong.add(o);
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        strong.clear();
        weak.clear();
    }
    
    @Override
    public String toString() {
        return String.format("Strong%s Weak%s",
                strong, weak);
    }
    
    private class MyIterator implements Iterator<T> {

        private final Iterator<T> strongIt = strong.iterator();
        private final Iterator<T> weakIt = weak.iterator();
        
        @Override
        public boolean hasNext() {
            return strongIt.hasNext() || weakIt.hasNext();
        }

        @Override
        public T next() {
            if (strongIt.hasNext()) {
                return strongIt.next();
            }
            return weakIt.next();
        }
        
    }
    
    public static void main(String[] args) throws InterruptedException {
        Object a = new Object();
        Object b = new Object();
        Object c = new Object();
        
        HalfWeakSet<Object> test = new HalfWeakSet<>();
        test.add(a);
        test.add(b);
        test.add(c);
        test.markWeak(a);
        
        a = null;
        b = null;
        c = null;
        
        for (Object item : test) {
            System.out.println(item);
        }
        
        Thread.sleep(15*1000);
        
        for (Object item : test) {
            System.out.println(item);
        }
    }
    
}
