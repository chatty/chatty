
package chatty.gui.components.settings;

import chatty.gui.components.settings.GenericComboSetting.Entry;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
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

    private final Set<Consumer<GenericComboSetting>> listeners = new HashSet<>();
    
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
        return getSettingValue(null);
    }
    
    public E getSettingValue(E def) {
        Entry<E> selected = (Entry<E>)getSelectedItem();
        if (selected == null) {
            return def;
        }
        return selected.value;
    }
    
    public Entry<E> getEntry(E value) {
        for (int i=0;i<getItemCount();i++) {
            if (Objects.equals(getItemAt(i).value, value)) {
                return getItemAt(i);
            }
        }
        return null;
    }
    
    public boolean containsValue(E value) {
        return getEntry(value) != null;
    }

    public void setSettingValue(E value) {
        Entry<E> entry = getEntry(value);
        if (entry == null) {
            // If not already in the list, create default one
            entry = new Entry<>(value, value == null ? "" : String.valueOf(value));
            if (!isEditable()) {
                // So it doesn't get lost, if editable it can be in the editor
                addItem(entry);
            }
        }
        setSelectedItem(entry);
        informSettingChangeListeners();
    }

    /**
     * Adds a value and uses the String representation of that value as label
     * for display.
     * 
     * @param value The value to add to the list
     */
    public void add(E value) {
        Entry<E> entry = new Entry<>(value, String.valueOf(value));
        addItem(entry);
    }
    
    /**
     * Adds a value and uses the given label for display.
     *
     * @param value The value to add to the list
     * @param label The label to display for this value
     */
    public void add(E value, String label) {
        Entry entry = new Entry<>(value, label);
        addItem(entry);
    }
    
    public void insert(E value, String label, int pos) {
        Entry entry = new Entry<>(value, label);
        insertItemAt(entry, pos);
    }
    
    public void remove(E value) {
        Entry entry = new Entry<>(value, String.valueOf(value));
        removeItem(entry);
    }
    
    public void replace(E search, E replace) {
        Entry searchEntry = new Entry<>(search, String.valueOf(search));
        Entry replaceEntry = new Entry<>(replace, String.valueOf(replace));
        for (int i=0; i<getItemCount(); i++) {
            if (getItemAt(i).equals(searchEntry)) {
                removeItemAt(i);
                insertItemAt(replaceEntry, i);
                return;
            }
        }
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
    
    public void addSettingChangeListener(Consumer<GenericComboSetting> listener) {
        if (listener != null) {
            if (listeners.isEmpty()) {
                addItemListener(e -> {
                    informSettingChangeListeners();
                });
            }
            listeners.add(listener);
        }
    }

    public void informSettingChangeListeners() {
        for (Consumer<GenericComboSetting> listener : listeners) {
            listener.accept(this);
        }
    }
    
    public static class Entry<E> {
        public final E value;
        public final String label;
        
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
