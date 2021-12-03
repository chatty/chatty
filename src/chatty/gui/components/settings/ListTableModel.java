
package chatty.gui.components.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * A table model backed by a list. This supports one object per row. The
 * {@code getValueAt(int, int)} method has to be overriden to provide the data
 * the table should use for the individual cells. It can use the methods defined
 * to retrieve the appropriate object.
 * 
 * @author tduva
 * @param <T> The type of the items contained in the table
 */
public abstract class ListTableModel<T> extends AbstractTableModel {

    private String[] columnNames;
    private List<T> data = new ArrayList<>();

    public ListTableModel(String[] columns) {
        columnNames = columns;
    }
    
    /**
     * Changing columns after the table is created may not work.
     * 
     * @param names 
     */
    public void setColumnNames(String[] names) {
        this.columnNames = names;
    }

    /**
     * Replaces the whole data with the entries in the given list.
     * 
     * @param data 
     */
    public void setData(Collection<T> data) {
        if (data == null) {
            this.data = new ArrayList<>();
        } else {
            this.data = new ArrayList<>(data);
        }
        fireTableDataChanged();
    }
    
    public void clear() {
        setData(null);
    }

    /**
     * Returns a copy of all items.
     * 
     * @return 
     */
    public List<T> getData() {
        return new ArrayList<>(data);
    }

    /**
     * Gets the item at the given {@code index}.
     * 
     * @param index
     * @return 
     */
    public T get(int index) {
        return data.get(index);
    }

    /**
     * Adds the given item to the end.
     * 
     * @param item
     * @return 
     */
    public int add(T item) {
        data.add(item);
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
        return data.size();
    }
    
    public void remove(T item) {
        int index = indexOf(item);
        if (index != -1) {
            remove(index);
        }
    }

    /**
     * Inserts an item at the given {@code index}
     * 
     * @param index
     * @param item 
     */
    public void insert(int index, T item) {
        data.add(index, item);
        fireTableRowsInserted(index, index);
    }

    /**
     * Replaces the entry at the given {@code index} with the given item.
     * 
     * @param index The index to put the entry in
     * @param item The item to put
     */
    public void set(int index, T item) {
        data.set(index, item);
        fireTableRowsUpdated(index, index);
    }

    /**
     * Checks if the data contains the given item.
     * 
     * @param item The item to check
     * @return true if the data contains the item, false otherwise
     */
    public boolean contains(T item) {
        return data.contains(item);
    }

    /**
     * 
     * @param item
     * @return 
     * @see List.indexOf(T)
     */
    public int indexOf(T item) {
        return data.indexOf(item);
    }
    
    /**
     * Replaces the first entry that equals the given entry with the given
     * entry. This can be used to update an entry that has changed attributes
     * but is considered equal (as in the {@code equals()} method).
     *
     * @param item
     */
    public void update(T item) {
        int present = indexOf(item);
        if (present != -1) {
            set(present, item);
        }
    }

    public int moveUp(int index) {
        if (index > 0) {
            swap(index, index - 1);
            fireTableRowsUpdated(index - 1, index);
            return index - 1;
        }
        return index;
    }

    public int moveDown(int index) {
        if (index < data.size() - 1) {
            swap(index, index + 1);
            fireTableRowsUpdated(index, index + 1);
            return index + 1;
        }
        return index;
    }
    
//    public int move(int from, int to) {
//        if (from > 0 && to > 0 && to != from) {
//            
//        }
//        return from;
//    }

    public T remove(int index) {
        T removed = data.remove(index);
        fireTableRowsDeleted(index, index);
        return removed;
    }

    private void swap(int index, int index2) {
        T temp = data.get(index2);
        data.set(index2, data.get(index));
        data.set(index, temp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return data.size();
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public String getColumnName(int n) {
        return columnNames[n];
    }

    public String getSearchValueAt(int row, int column) {
        return getValueAt(row, column).toString();
    }
    
    public int getSearchColumn(int column) {
        return column;
    }
    
}
