
package chatty.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * Iterate over two different Collections. Iterator.remove() also works, when
 * the underlying Collections/Iterators support it.
 * 
 * @author tduva
 */
public class CombinedIterator<T> implements Iterator<T> {
    
    private final Iterator<T> itA;
    private final Iterator<T> itB;
    private Iterator<T> lastUsed;
    
    /**
     * Iterator over Collection a, then over Collection b.
     * 
     * @param a Must not be null
     * @param b Must not be null
     */
    public CombinedIterator(Collection<T> a, Collection<T> b) {
        itA = a.iterator();
        itB = b.iterator();
    }
    
    @Override
    public boolean hasNext() {
        return itA.hasNext() || itB.hasNext();
    }

    @Override
    public T next() {
        if (itA.hasNext()) {
            lastUsed = itA;
            return itA.next();
        }
        lastUsed = itB;
        return itB.next();
    }
    
    @Override
    public void remove() {
        lastUsed.remove();
    }
    
}
