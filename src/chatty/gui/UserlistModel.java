
package chatty.gui;

import java.util.ArrayList;
import java.util.Collections;
import javax.swing.AbstractListModel;

/**
 * The data model behind the userlist, sorts items.
 * 
 * @author tduva
 */
public class UserlistModel<T extends Comparable<T>> extends AbstractListModel<T> {
    
    ArrayList<T> data = new ArrayList<>();

    public ArrayList<T> getData() {
        return (ArrayList) data.clone();
    }

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public T getElementAt(int index) {
        return data.get(index);
    }

    public void add(T item) {
        int insertionPoint = findInsertionPoint(item);
        data.add(insertionPoint, item);
        super.fireIntervalAdded(this, insertionPoint, insertionPoint);
    }

    public void remove(T item) {
        int index = data.indexOf(item);
        if (index == -1) {
            return;
        }
        data.remove(index);
        super.fireIntervalRemoved(this, index, index);
    }

    private int findInsertionPoint(T item) {
        int insertionPoint = Collections.binarySearch(data, item, null);
        if (insertionPoint < 0) {
            insertionPoint = -(insertionPoint + 1);
        }
        return insertionPoint;
    }

    public void updated(T item) {
        int index = data.indexOf(item);
        if (index == -1) {
            return;
        }
        super.fireContentsChanged(this, index, index);
    }

    public void clear() {
        if (!data.isEmpty()) {
            super.fireIntervalRemoved(this, 0, data.size() - 1);
            data.clear();
        }
    }
    
    /**
     * Manually sort entries. This may sometimes fix the sorting.
     */
    public void sort() {
        Collections.sort(data);
        super.fireContentsChanged(this, 0, data.size() - 1);
    }
    
    /**
     * Mark all entries as changed, so they get repainted.
     */
    public void update() {
        super.fireContentsChanged(this, 0, data.size() - 1);
    }
}
