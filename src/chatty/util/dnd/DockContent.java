
package chatty.util.dnd;

import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 * This holds the component that is the actually visible content and provides
 * various meta information and methods related to the content.
 * 
 * @author tduva
 */
public interface DockContent {
    
    /**
     * The component that will be added to the layout.
     * 
     * @return 
     */
    public JComponent getComponent();
    
    /**
     * The title (used e.g. for tab names). Should be rather short and usually
     * not change (but it can).
     * 
     * @return 
     */
    public String getTitle();
    
    public String getLongTitle();
    
    public void setLongTitle(String title);
    
    public DockPath getPath();
    
    public String getId();
    
    public void setId(String id);
    
    public void setTargetPath(DockPath path);
    
    public DockPath getTargetPath();
    
    public void setDockParent(DockChild parent);
    
    /**
     * The context menu for the tab.
     * 
     * @return The menu, can be null to show no menu
     */
    public JPopupMenu getContextMenu();
    
    /**
     * Provides a custom tab component.
     * 
     * @return The tab component, can be null to use the default
     */
    public DockTabComponent getTabComponent();
    
    /**
     * This can be called to remove the content. Was exactly is performed may
     * depend on the component, but commonly this should call the DockManager to
     * remove the content.
     */
    public void remove();
    public void addListener(DockContentPropertyListener listener);
    public void removeListener(DockContentPropertyListener listener);
    public Color getForegroundColor();
    public boolean canPopout();
    
    public interface DockContentPropertyListener {
        
        public enum Property {
            TITLE, LONG_TITLE, FOREGROUND
        }
        
        public void propertyChanged(Property property, DockContent content);
    }
    
}
