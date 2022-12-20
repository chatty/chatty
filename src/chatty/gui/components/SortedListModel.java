
package chatty.gui.components;

import chatty.util.CombinedIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.AbstractListModel;

/**
 * A List Model that sorts the entries either by a Comparator that can be set
 * or their natural ordering (must be Comparable if no Comparator is set).
 * 
 * Also allows a filter to be set, which automatically hides items from the
 * list.
 *
 * @author tduva
 * @param <E>
 */
public class SortedListModel<E> extends AbstractListModel<E> implements Iterable<E> {

    private final ArrayList<E> data = new ArrayList<>();
    private Comparator<? super E> comparator = null;
    private Predicate<E> filter;
    private final List<E> removedByFilter = new ArrayList<>();

    /**
     * Set a new Comparator, which also automatically resorts the items.
     * 
     * @param c The Comparator to use from now on.
     */
    public void setComparator(Comparator<? super E> c) {
        comparator = c;
    }
    
    /**
     * Set a filter. Return false from the filter predicate for items that
     * should not appear in the list. Automatically updates the filtered items.
     * 
     * @param filter The filter, or null for no filtering
     */
    public void setFilter(Predicate<E> filter) {
        this.filter = filter;
        updateFiltering();
    }
    
    /**
     * It may be necessary to call this when the items contained in the list
     * change any properties that would get them shown or hidden.
     */
    public void updateFiltering() {
        if (filter == null) {
            removedByFilter.forEach(item -> addInternal(item));
            removedByFilter.clear();
        }
        else {
            List<E> toAdd = filterList(removedByFilter, true);
            removedByFilter.removeAll(toAdd);

            List<E> toRemove = filterList(data, false);
            for (E item : toRemove) {
                removeInternal(item);
            }
            removedByFilter.addAll(toRemove);
            for (E item : toAdd) {
                addInternal(item);
            }
        }
    }
    
    private List<E> filterList(List<E> list, boolean testValue) {
        List<E> result = new ArrayList<>();
        for (E item : list) {
            if (filter.test(item) == testValue) {
                result.add(item);
            }
        }
        return result;
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
    
    /**
     * The total number of entries, including filtered out ones. getSize() is
     * used by the list, so it should only return the number that is actually
     * added to the list.
     * 
     * @return 
     */
    public int getTotalSize() {
        return data.size() + removedByFilter.size();
    }

    @Override
    public E getElementAt(int index) {
        return data.get(index);
    }

    public void add(E item) {
        if (filter != null && !filter.test(item)) {
            removedByFilter.add(item);
        }
        else {
            addInternal(item);
        }
    }
    
    private void addInternal(E item) {
        int insertionPoint = findInsertionPoint(item);
        data.add(insertionPoint, item);
        super.fireIntervalAdded(this, insertionPoint, insertionPoint);
    }

    public boolean contains(E item) {
        return data.contains(item) || removedByFilter.contains(item);
    }

    public void remove(E item) {
        removedByFilter.remove(item);
        removeInternal(item);
    }
    
    private void removeInternal(E item) {
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
        removedByFilter.clear();
    }

    /**
     * Removing entries using this iterator may not update the list.
     * 
     * @return 
     */
    @Override
    public Iterator<E> iterator() {
        return new CombinedIterator<>(data, removedByFilter);
    }
}
