
package chatty.gui.components.menus;

import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A Popup Menu with convenience methods to add items as well as items in
 * submenus.
 * 
 * @author tduva
 */
public abstract class ContextMenu extends JPopupMenu implements ActionListener {
    
    private final Map<String, JMenu> subMenus = new HashMap<>();
    private final Set<ContextMenuListener> listeners = new HashSet<>();
    
    private JMenuItem makeItem(String action, String text) {
        JMenuItem item = new JMenuItem(text);
        item.setActionCommand(action);
        item.addActionListener(this);
        return item;
    }
    
    private JMenuItem makeCheckboxItem(String action, String text, boolean selected) {
        JMenuItem item = new JCheckBoxMenuItem(text, selected);
        item.setActionCommand(action);
        item.addActionListener(this);
        return item;
    }
    
    /**
     * Adds an item to the context menu, after any previously added items.
     * 
     * @param action The action of the menu item, which will be send to the
     * listener
     * @param text The label of the menu item
     */
    protected void addItem(String action, String text) {
        add(makeItem(action, text));
    }
    
    /**
     * Adds an entry to the submenu with the name {@code parent}, after any
     * previously added items in that submenu.
     * 
     * @param action The action of the menu item, which will be send to the
     * listener
     * @param text The label of the menu item
     * @param parent The name of the submenu, can be {@code null} in which case
     * it isn't added to a submenu
     * @see addItem(String, String)
     */
    protected void addItem(String action, String text, String parent) {
        if (parent != null) {
            getSubmenu(parent).add(makeItem(action, text));
        } else {
            addItem(action, text);
        }
    }
    
    protected void addCheckboxItem(String action, String text, boolean selected) {
        add(makeCheckboxItem(action, text, selected));
    }
    
    protected void addCheckboxItem(String action, String text, String parent, boolean selected) {
        if (parent != null) {
            getSubmenu(parent).add(makeCheckboxItem(action, text, selected));
        } else {
            addCheckboxItem(action, text, selected);
        }
    }
    
    /**
     * Adds a seperator to the submenu with the given name, or adds a seperator
     * in the main menu if this submenu doesn't exist yet.
     * 
     * @param parent 
     */
    protected void addSeparator(String parent) {
        if (parent != null && isSubmenu(parent)) {
            getSubmenu(parent).addSeparator();
        } else {
            addSeparator();
        }
    }
    
    public void addContextMenuListener(ContextMenuListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    protected Set<ContextMenuListener> getContextMenuListeners() {
        return listeners;
    }
    
    private boolean isSubmenu(String name) {
        return subMenus.containsKey(name);
    }
    
    private JMenu getSubmenu(String name) {
        if (subMenus.get(name) == null) {
            JMenu menu = new JMenu(name);
            add(menu);
            subMenus.put(name, menu);
        }
        return subMenus.get(name);
    }
    
}