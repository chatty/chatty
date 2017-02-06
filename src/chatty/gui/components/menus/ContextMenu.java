
package chatty.gui.components.menus;

import chatty.util.commands.CustomCommand;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
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
    
    private final ActionListener listener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            ContextMenu.this.actionPerformed(getCommandActionEvent(e));
        }
    };
    private final Map<String, JMenu> subMenus = new HashMap<>();
    private final Set<ContextMenuListener> listeners = new HashSet<>();
    private final Map<String, CustomCommand> commands = new HashMap<>();
    
    private JMenuItem makeItem(String action, String text, ImageIcon icon) {
        JMenuItem item = new JMenuItem(text);
        item.setActionCommand(action);
        item.addActionListener(listener);
        if (icon != null) {
            item.setIcon(icon);
        }
        return item;
    }
    
    private JMenuItem makeCheckboxItem(String action, String text, boolean selected) {
        JMenuItem item = new JCheckBoxMenuItem(text, selected);
        item.setActionCommand(action);
        item.addActionListener(listener);
        return item;
    }
    
    /**
     * Adds an item to the context menu, after any previously added items.
     * 
     * @param action The action of the menu item, which will be send to the
     * listener
     * @param text The label of the menu item
     */
    protected JMenuItem addItem(String action, String text, ImageIcon icon) {
        JMenuItem item = makeItem(action, text, icon);
        add(item);
        return item;
    }
    
    protected JMenuItem addItem(String action, String text) {
        return addItem(action, text, null);
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
     * @param icon The icon to display for the menu item
     * @see addItem(String, String, ImageIcon)
     */
    protected JMenuItem addSubItem(String action, String text, String parent, ImageIcon icon) {
        if (parent != null) {
            JMenuItem item = makeItem(action, text, icon);
            getSubmenu(parent).add(item);
            return item;
        } else {
            return addItem(action, text, icon);
        }
    }
    
    protected JMenuItem addSubItem(String action, String text, String parent) {
        return addSubItem(action, text, parent, null);
    }
    
    public JMenuItem addCommandItem(CommandMenuItem item) {
        if (item.getCommand() == null && item.getLabel() == null) {
            addSeparator(item.getParent());
        } else if (item.getCommand() == null) {
            JMenu menu = getSubmenu(item.getLabel());
            addKey(item, menu);
        } else {
            commands.put(item.getId(), item.getCommand());
            JMenuItem mItem = addSubItem(item.getId(), item.getLabel(), item.getParent());
            addKey(item, mItem);
        }
        return null;
    }
    
    private void addKey(CommandMenuItem item, JMenuItem mItem) {
        if (item.hasKey()) {
            mItem.setMnemonic(KeyEvent.getExtendedKeyCodeForChar(item.getKey().toLowerCase().charAt(0)));
        }
    }
    
    private ActionEvent getCommandActionEvent(ActionEvent e) {
        CustomCommand command = commands.get(e.getActionCommand());
        if (command != null) {
            return new CommandActionEvent(e, command);
        }
        return e;
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
    
    protected void setSubMenuIcon(String name, ImageIcon icon) {
        getSubmenu(name).setIcon(icon);
    }
 
}