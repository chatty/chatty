
package chatty.gui.components.settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import javax.swing.JComboBox;

/**
 *
 * @author tduva
 */
public class GenericComboSetting<E> extends JComboBox<String> {

    private final Map<E, String> items;
    private final Map<String, String> commands = new HashMap<>();
    private ActionListener actionListener;
    private int lastSelected;
    
    public GenericComboSetting(E[] initialItems) {
        items = new HashMap<>();
        for (E item : initialItems) {
            add(String.valueOf(item));
            items.put(item, String.valueOf(item));
        }
        if (initialItems.length > 0) {
            setSelectedIndex(0);
        }
        //addListener();
    }
    
    public GenericComboSetting(Map<E, String> items) {
        super(new Vector<>(items.values()));
        this.items = items;
        //addListener();
    }
    
    public E getSettingValue() {
        String selected = (String)getSelectedItem();
        if (selected == null) {
            return null;
        }
        E value = this.getKeyFromItem(selected);
        if (value != null) {
            return value;
        }
        return null;
    }

    public void setSettingValue(E value) {
        if (!items.containsKey(value)) {
            items.put(value, String.valueOf(value));
            add(String.valueOf(value));
        }
        setSelectedItem(items.get(value));
    }
    
    public void addCommand(String command, String label) {
        commands.put(command, label);
        addItem(label);
    }

    public void add(E item) {
        items.put(item, String.valueOf(item));
        add(String.valueOf(item));
    }
    
    /**
     * Removes all items from the list.
     */
    public void clear() {
        items.clear();
        removeAllItems();
    }
    
    /**
     * Removes all items from the list and replaces them with the given ones.
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
    
    /**
     * Adds an item and adds the label to the list.
     * 
     * @param item
     * @param label 
     */
    public void add(E item, String label) {
        items.put(item, label);
        add(label);
    }
    
    /**
     * Adds an item to the list, before any commands.
     * 
     * @param item 
     */
    private void add(String item) {
        int insertAt = getItemCount() - commands.size();
        if (insertAt < 0) {
            insertAt = 0;
        }
        insertItemAt(item, insertAt);
    }

    private E getKeyFromItem(String item) {
        for (Entry<E, String> entry : items.entrySet()) {
            if (item.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    private String getCommandFromItem(String item) {
        for (Entry<String, String> entry : commands.entrySet()) {
            if (item.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    private boolean contains(E value) {
        for (int i=0; i < getItemCount(); i++) {
            if (getItemAt(i).equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    public void setActionListener(ActionListener listener) {
        actionListener = listener;
    }
    
    private void addListener() {
        this.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }
                String selected = (String) getSelectedItem();
                if (selected != null) {
                    String command = getCommandFromItem(selected);
                    if (command != null) {
                        if (getSelectedIndex() != lastSelected) {
                            setSelectedIndex(lastSelected);
                        }
                        if (actionListener != null) {
                            actionListener.actionPerformed(new ActionEvent(this, 0, command));
                        }
                    } else {
                        lastSelected = getSelectedIndex();
                    }
                }
            }
        });
    }
}
