
package chatty.gui.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import javax.swing.AbstractListModel;

/**
 * A List Model that sorts the entries either by a Comparator that can be set
 * or their natural ordering (must be Comparable if no Comparator is set).
 * 
 * @author tduva
 * @param <E>
 */
public class SortedListModel<E> extends AbstractListModel<E> implements Iterable<E> {

    private final ArrayList<E> data = new ArrayList<>();
    private Comparator<? super E> comparator = null;

    /**
     * Set a new Comparator, which also automatically resorts the items.
     * 
     * @param c The Comparator to use from now on.
     */
    public void setComparator(Comparator<? super E> c) {
        comparator = c;
    }
    
    public void resort() {
        if (comparator != null) {
            Collections.sort(data, comparator);
            super.fireContentsChanged(this, 0, data.size());
        }
    }

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public E getElementAt(int index) {
        return data.get(index);
    }

    public void add(E item) {
        int insertionPoint = findInsertionPoint(item);
        data.add(insertionPoint, item);
        super.fireIntervalAdded(this, insertionPoint, insertionPoint);
    }

    public boolean contains(E item) {
        return data.contains(item);
    }

    public void remove(E item) {
        int index = data.indexOf(item);
        if (index == -1) {
            return;
        }
        data.remove(index);
        super.fireIntervalRemoved(this, index, index);
    }

    private int findInsertionPoint(E item) {
        int insertionPoint = Collections.binarySearch(data, item, comparator);
        if (insertionPoint < 0) {
            insertionPoint = -(insertionPoint + 1);
        }
        return insertionPoint;
    }

    public void clear() {
        if (!data.isEmpty()) {
            super.fireIntervalRemoved(this, 0, data.size() - 1);
            data.clear();
        }
    }

    /**
     * Removing entries using this iterator may not update the list.
     * 
     * @return 
     */
    @Override
    public Iterator iterator() {
        return data.iterator();
    }
}
