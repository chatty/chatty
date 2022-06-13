
package chatty.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This uses Lists to store the values instead of HashSets/Maps and is more
 * memory efficient, however it is aimed at storing only a few values or where
 * adding values doesn't happen often, since all values are iterated over to
 * check if the value has already been added. Additionally, this compares object
 * identity, not equals().
 *
 * This is consistent with storing CachedImage objects, where usually only one
 * or few different images are stored.
 * 
 * @author tduva
 */
public class HalfWeakSet2<T> implements Iterable<T> {

    private List<T> strong;
    private List<WeakReference<T>> weak;
    
    public void add(T value) {
        if (contains(weak, value)) {
            remove(weak, value);
            ensureStrongExists();
            strong.add(value);
        }
        else if (!contains(strong, value)) {
            ensureStrongExists();
            strong.add(value);
        }
    }
    
    private void ensureStrongExists() {
        if (strong == null) {
            strong = new ArrayList<>(1);
        }
    }
    
    private void ensureWeakExists() {
        if (weak == null) {
            weak = new ArrayList<>(1);
        }
    }
    
    public void markStrong(T value) {
        if (remove(weak, value)) {
            ensureStrongExists();
            strong.add(value);
        }
    }
    
    public void markWeak(T value) {
        if (remove(strong, value)) {
            ensureWeakExists();
            weak.add(new WeakReference<>(value));
        }
    }
    
    public void cleanUp() {
        if (weak != null) {
            Iterator<WeakReference<T>> it = weak.iterator();
            while (it.hasNext()) {
//                if (it.next().refersTo(null)) { // Doesn't work on Java 8
                if (it.next().get() == null) {
                    it.remove();
                }
            }
            if (weak.isEmpty()) {
                weak = null;
            }
        }
        if (strong != null && strong.isEmpty()) {
            strong = null;
        }
    }
    
    private boolean contains(List list, Object search) {
        if (list == null) {
            return false;
        }
        for (Object value : list) {
            if (list == weak && ((WeakReference)value).get() == search
                    || value == search) {
                return true;
            }
        }
        return false;
    }
    
    private boolean remove(List list, Object search) {
        if (list == null) {
            return false;
        }
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if ((list == weak && ((WeakReference)o).get() == search)
                    || (list == strong && o == search)) {
                it.remove();
                return true;
            }
        }
        return false;
    }
    
    public void clear() {
        strong = null;
        weak = null;
    }

    /**
     * Values returned by this may be {@code null} if they have been marked weak and
     * have been garbage collected while iterating.
     * 
     * @return 
     */
    @Override
    public Iterator<T> iterator() {
        cleanUp();
        return new CombinedIterator();
    }
    
    public Iterator<T> strongIterator() {
        if (strong == null) {
            return new EmptyIterator();
        }
        return strong.iterator();
    }
    
    private class CombinedIterator implements Iterator<T> {
        
        private final Iterator<T> strongIt;
        private final Iterator<WeakReference<T>> weakIt;
        
        private CombinedIterator() {
            if (strong != null) {
                strongIt = strong.iterator();
            }
            else {
                strongIt = null;
            }
            if (weak != null) {
                weakIt = weak.iterator();
            }
            else {
                weakIt = null;
            }
        }
        
        @Override
        public boolean hasNext() {
            return (strongIt != null && strongIt.hasNext())
                    || (weakIt != null && weakIt.hasNext());
        }

        @Override
        public T next() {
            if (strongIt != null && strongIt.hasNext()) {
                return strongIt.next();
            }
            return weakIt.next().get();
        }
        
    }
    
    private class EmptyIterator implements Iterator<T> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            return null;
        }
        
    }
    
    public static void main(String[] args) throws InterruptedException {
        Object a = new String("a");
        Object b = new String("b");
        Object c = new String("c");
        
        HalfWeakSet2<Object> test = new HalfWeakSet2<>();
//        test.add(a);
        test.add(b);
//        test.add(c);
        test.markWeak(a);
        test.markStrong(a);
        test.markStrong(a);
        test.markWeak(b);
//        test.add(b);
        
        for (Object item : test) {
            System.out.println(item);
        }

        a = null;
        b = null;
        c = null;
        
        for (Object item : test) {
            System.out.println(item);
        }
        
        System.gc();
//        Thread.sleep(5*1000);
        System.out.println("--");
        
        for (Object item : test) {
            System.out.println(item);
        }
    }
    
}
