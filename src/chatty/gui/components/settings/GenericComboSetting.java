
package chatty.gui.components.settings;

import chatty.gui.components.settings.GenericComboSetting.Entry;
import java.util.Map;
import java.util.Objects;
import javax.swing.JComboBox;

/**
 * Warning:
 * This should only be set editable when the generic type is String, since there
 * is no way to convert what is manually entered into the generic type.
 * 
 * @author tduva
 * @param <E>
 */
public class GenericComboSetting<E> extends JComboBox<Entry<E>> {

    public GenericComboSetting() {
        // Empty list
    }
    
    public GenericComboSetting(E[] initialItems) {
        for (E item : initialItems) {
            add(item);
        }
        if (initialItems.length > 0) {
            setSelectedIndex(0);
        }
    }
    
    public GenericComboSetting(Map<E, String> items) {
        for (E value : items.keySet()) {
            String label = items.get(value);
            add(value, label);
        }
    }
    
    public E getSettingValue() {
        Entry<E> selected = (Entry<E>)getSelectedItem();
        if (selected == null) {
            return null;
        }
        return selected.value;
    }
    
    public boolean containsItem(E item) {
        for (int i=0;i<getItemCount();i++) {
            if (Objects.equals(getItemAt(i).value, item)) {
                return true;
            }
        }
        return false;
    }

    public void setSettingValue(E item) {
        Entry entry = new Entry<>(item, item == null ? "" : String.valueOf(item));
        if (!isEditable() && !containsItem(item)) {
            addItem(entry);
        }
        setSelectedItem(entry);
    }

    public void add(E item) {
        Entry entry = new Entry<>(item, String.valueOf(item));
        addItem(entry);
    }
    
    /**
     * Adds an item and adds the label to the list.
     *
     * @param item
     * @param label
     */
    public void add(E item, String label) {
        Entry entry = new Entry<>(item, label);
        addItem(entry);
    }
    
    /**
     * Removes all items from the list.
     */
    public void clear() {
        removeAllItems();
    }
    
    /**
     * Removes all items from the list and replaces them with the given ones.
     * 
     * @param data 
     */
    public void setData(Map<E, String> data) {
        clear();
        addData(data);
    }
    
    /**
     * Adds the given items to the end of the list.
     * 
     * @param data 
     */
    public void addData(Map<E, String> data) {
        if (data != null) {
            for (E key : data.keySet()) {
                add(key, data.get(key));
            }
        }
    }
    
    public static class Entry<E> {
        private final E value;
        private final String label;
        
        public Entry(E value, String label) {
            this.value = value;
            this.label = label;
        }
        
        @Override
        public String toString() {
            return label;
        }
        
        public static Object valueOf(String value) {
            return new Entry(value, value);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Entry<?> other = (Entry<?>) obj;
            if (!Objects.equals(this.value, other.value)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + Objects.hashCode(this.value);
            return hash;
        }
    }

}
