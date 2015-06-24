
package chatty.gui.components;

import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.TabContextMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 * A Tabpane that shows a simple JPanel when only one Tab is added.
 */
public class Tabs extends JPanel {
    
    public static final int ADD_ORDER = 0;
    public static final int ALPHABETIC_ORDER = 1;
    
    private final JTabbedPane tabs = new JTabbedPane();
    private Component firstComp;
    
    private int order = ADD_ORDER;
    
    private final ContextMenuListener contextMenuListener;
    
    public Tabs(ContextMenuListener contextMenuListener) {
        this.contextMenuListener = contextMenuListener;
        setLayout(new BorderLayout());
        tabs.setOpaque(false);
        tabs.setComponentPopupMenu(new TabContextMenu(contextMenuListener));
        //tabs.setTabPlacement(JTabbedPane.LEFT);
    }
    
    /**
     * Adds a Component to the TabbedPane. If there is no component yet,
     * add it as the first component, otherwise show the TabbedPane and add
     * both to that.
     * 
     * @param comp 
     */
    public void addTab(Component comp) {
        if (tabs.getTabCount() == 0) {
            // Nothing yet in the tab pane
            
            if (firstComp == null) {
                add(comp, BorderLayout.CENTER);
                firstComp = comp;
                // Added because it wouldn't show changes after removing the
                // last element/adding a new one
                this.validate();
                this.repaint();
            }
            else {
                // Add the JTabbedPane and add the added Component and the
                // first Component.
                remove(firstComp);
                add(tabs, BorderLayout.CENTER);
                
                appendTab(firstComp);
                appendTab(comp);
                
                firstComp = null;
            }
        }
        else {
            appendTab(comp);
        }
    }
    
        
    /**
     * Removes a Component. If after removing the Component there is only one
     * left, remove the JTabbedPane and add the remaining Component directly
     * to the JPanel.
     * 
     * @param comp 
     */
    public void removeTab(Component comp) {
        if (tabs.getTabCount() == 0) {
            if (firstComp == null || firstComp != comp) {
                return;
            }
            super.remove(firstComp);
            
            firstComp = null;
            // Added these lines because it wasn't showing any change after removing
            // the last element
            this.validate();
            this.repaint();
            return;
        }
        
        tabs.remove(comp);
        if (tabs.getTabCount() == 1) {
            // Only one Component remaining, so remove JTabbedPane and
            // add it directly to the JPanel.
            if (firstComp != tabs.getSelectedComponent()) {
                firstComp = tabs.getSelectedComponent();
            }
            tabs.remove(firstComp);
            add(firstComp,BorderLayout.CENTER);
            super.remove(tabs);
        }
        
    }
    
    /**
     * Gets the number of Components added.
     * 
     * @return 
     */
    public int getTabCount() {
        if (tabs.getTabCount() > 0) {
            return tabs.getTabCount();
        }
        else if (firstComp != null) {
            return 1;
        }
        return 0;
    }
    
    /**
     * Adds a Component to the end of the JTabbedPane.
     * 
     * @param comp 
     */
    private void appendTab(Component comp) {
        String title = comp.getName();
        int insertAt = findInsertPosition(title);
        tabs.insertTab(title,null,comp,title,insertAt);
    }

    /**
     * Returns the index this tab should be added at, depending on the current
     * order setting.
     * 
     * @param newTabName
     * @return 
     */
    private int findInsertPosition(String newTabName) {
        if (order == ALPHABETIC_ORDER) {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (newTabName.compareToIgnoreCase(tabs.getTitleAt(i)) < 0) {
                    return i;
                }
            }
        }
        return tabs.getTabCount();
    }

    public void addChangeListener(ChangeListener listener) {
        tabs.addChangeListener(listener);
    }
    
    /**
     * Gets the currently selected Component.
     * 
     * @return 
     */
    public Component getSelectedComponent() {
        if (firstComp != null) {
            return firstComp;
        }
        return tabs.getSelectedComponent();
    }
    
    public int getSelectedIndex() {
        return tabs.getSelectedIndex();
    }
    
    /**
     * Sets the given component as selected in the JTabbedPane.
     * 
     * @param c 
     */
    public void setSelectedComponent(Component c) {
        if (tabs.indexOfComponent(c) != -1) {
            tabs.setSelectedComponent(c);
        }
    }
    
    /**
     * Switches to the next TAB, if available, or starts from the beginning.
     */
    public void setSelectedNext() {
        int index = tabs.getSelectedIndex();
        int count = tabs.getTabCount();
        if (index+1 < count) {
            tabs.setSelectedIndex(index+1);
        } else if (count > 0) {
            tabs.setSelectedIndex(0);
        }
    }
    
    public void setSelectedPrevious() {
        int index = tabs.getSelectedIndex();
        int count = tabs.getTabCount();
        if (count > 0) {
            if (index-1 >= 0) {
                tabs.setSelectedIndex(index-1);
            } else {
                tabs.setSelectedIndex(count -1);
            }
        }
    }
    
    public void setForegroundAt(int index, Color foreground) {
        tabs.setForegroundAt(index, foreground);
    }
    
    public void setForegroundForComponent(Component comp, Color foreground) {
        int index = tabs.indexOfComponent(comp);
        if (index != -1) {
            tabs.setForegroundAt(index,foreground);
        }
    }
    
    public void setTitleForComponent(Component comp, String title) {
        int index = tabs.indexOfComponent(comp);
        if (index != -1) {
            tabs.setTitleAt(index, title);
        }
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
}
