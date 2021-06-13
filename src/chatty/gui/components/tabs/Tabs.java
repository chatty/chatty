
package chatty.gui.components.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 * A JPanel that can have one or more Components added to it, and it will show
 * a JTabbedPane depending on that.
 */
public class Tabs extends JPanel {
    
    public enum TabOrder {
        /**
         * Inserts added tabs at the end.
         */
        INSERTION,
        
        /**
         * Inserts new tabs in alphabetic order based on the name of the added
         * Component. If tabs have been reordered manually, then tabs are
         * inserted before the first tab whose name would be greater than the
         * new one.
         * 
         * <p>Ordering is done ignoring case.</p>
         */
        ALPHABETIC
    }
    
    private final JTabbedPane tabs = new DraggableTabbedPane();
    private Component firstComp;
    
    private boolean mouseWheelScrolling = true;
    private boolean mouseWheelScrollingAnywhere = false;
    
    private TabOrder order = TabOrder.INSERTION;
    
    private JPopupMenu popupMenu;
    
    public Tabs() {
        setLayout(new BorderLayout());
        tabs.setOpaque(false);
        tabs.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (mouseWheelScrolling) {
                    // Only scroll if actually on tabs area
                    int index = tabs.indexAtLocation(e.getX(), e.getY());
                    if (mouseWheelScrollingAnywhere || index != -1
                            || isNearLastTab(e.getPoint())) {
                        if (e.getWheelRotation() < 0) {
                            setSelectedPrevious();
                        } else if (e.getWheelRotation() > 0) {
                            setSelectedNext();
                        }
                    }
                }
            }
        });
        
        tabs.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                openPopupMenu(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                openPopupMenu(e);
            }
        });
    }
    
    /**
     * Open context menu manually instead of relying on the JTabbedPane, so we
     * can check if it's the currently selected tab (since the context menu will
     * trigger actions based on the currently selected tab).
     * 
     * @param e 
     */
    private void openPopupMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        if (popupMenu == null) {
            return;
        }
        final int index = tabs.indexAtLocation(e.getX(), e.getY());
        if (tabs.getSelectedIndex() == index) {
            popupMenu.show(tabs, e.getX(), e.getY());
        }
    }
    
    private boolean isNearLastTab(Point p) {
        Rectangle bounds = tabs.getBoundsAt(tabs.getTabCount() - 1);
        bounds.width += 99999;
        return bounds.contains(p);
    }
    
    public void setMouseWheelScrollingEnabled(boolean enabled) {
        mouseWheelScrolling = enabled;
    }
    
    public void setMouseWheelScrollingAnywhereEnabled(boolean enabled) {
        mouseWheelScrollingAnywhere = enabled;
    }
    
    public void setTabPlacement(String location) {
        tabs.setTabPlacement(getTabPlacementValue(location));
    }
        
    private int getTabPlacementValue(String location) {
        switch(location) {
            case "top": return JTabbedPane.TOP;
            case "bottom": return JTabbedPane.BOTTOM;
            case "left": return JTabbedPane.LEFT;
            case "right": return JTabbedPane.RIGHT;
        }
        return JTabbedPane.TOP;
    }
    
    public void setTabLayoutPolicy(String type) {
        tabs.setTabLayoutPolicy(getTabLayoutPolicyValue(type));
    }
    
    private int getTabLayoutPolicyValue(String type) {
        switch(type) {
            case "wrap": return JTabbedPane.WRAP_TAB_LAYOUT;
            case "scroll": return JTabbedPane.SCROLL_TAB_LAYOUT;
        }
        return JTabbedPane.WRAP_TAB_LAYOUT;
    }
    
    public void setPopupMenu(JPopupMenu menu) {
        popupMenu = menu;
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
        if (order == TabOrder.ALPHABETIC) {
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
    
    public void setTitleForComponent(Component comp, String title, String tooltip) {
        int index = tabs.indexOfComponent(comp);
        if (index != -1) {
            tabs.setTitleAt(index, title);
            tabs.setToolTipTextAt(index, tooltip);
            if (title.startsWith("#")) {
                tabs.getAccessibleContext().setAccessibleName("Channel "+title.substring(1));
                tabs.getAccessibleContext().setAccessibleDescription("");
            }
        }
    }
    
    public void setOrder(TabOrder order) {
        this.order = order;
    }
    
    /**
     * Get all added components relative to the given Component, in a certain
     * direction. If the given Component is not a tab, then an empty list is
     * returned.
     * 
     * @param c The Component used as the center
     * @param direction -1 for tabs to the left, 1 for tabs to the right and 0
     * for tabs in both directions
     * @return List of components in the given direction, except the given
     * center one, or empty if no components are found
     */
    public Collection<Component> getComponents(Component c, int direction) {
        List<Component> result = new ArrayList<>();
        int index = tabs.indexOfComponent(c);
        if (index != -1) {
            if (direction == 1 || direction == 0) {
                for (int i = index+1; i < getTabCount(); i++) {
                    result.add(tabs.getComponentAt(i));
                }
            }
            if (direction == -1 || direction == 0) {
                for (int i = index-1; i >= 0; i--) {
                    result.add(tabs.getComponentAt(i));
                }
            }
        }
        return result;
    }
    
    public Collection<Component> getAllComponents() {
        List<Component> result = new ArrayList<>();
        if (firstComp != null) {
            result.add(firstComp);
        } else {
            for (int i = 0; i < getTabCount(); i++) {
                result.add(tabs.getComponentAt(i));
            }
        }
        return result;
    }
    
}
