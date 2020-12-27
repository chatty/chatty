
package chatty.util.dnd;

import java.util.List;
import javax.swing.JComponent;

/**
 * Any kind of component that is part of the hierarchy that holds content.
 * 
 * @author tduva
 */
public interface DockChild {
    
    /**
     * The actual component that should be part of the layout.
     * 
     * @return 
     */
    public JComponent getComponent();
    
    /**
     * Split up into two components, whereas the new one will contain the
     * given content.
     * 
     * @param info Contains info on where the new component should be added
     * @param content The content to add
     */
    public void split(DockDropInfo info, DockContent content);
    
    /**
     * Replace the given child with another one. This is commonly used to change
     * what a split pane contains.
     * 
     * @param old What to replace (required)
     * @param replacement What to replace it with (can be null to replace it
     * with nothing)
     */
    public void replace(DockChild old, DockChild replacement);
    
    /**
     * Add content to the component. If the component itself does not hold
     * content, it can be referred to a child component.
     * 
     * @param content The content to add
     */
    public void addContent(DockContent content);
    
    /**
     * Remove content from the component. If the component itself does not hold
     * content, it can be referred to a child component.
     * 
     * @param content The content to remove
     */
    public void removeContent(DockContent content);
    
    public void setActiveContent(DockContent content);
    
    public boolean isContentVisible(DockContent content);
    
    /**
     * Change the current base of this component.
     * 
     * @param base 
     */
    public void setBase(DockBase base);
    
    /**
     * Determine whether a drop can occur at the given location. If this
     * component does not provide drop points, it should be referred any child
     * components that may be able to receive drops.
     * 
     * This will be called a lot as the mouse is being moved around during a
     * drag and drop movement.
     * 
     * @param info Information on the potential drop
     * @return A DockDropInfo if this component has an opinion on what drop
     * should occur, null otherwise
     */
    public DockDropInfo findDrop(DockImportInfo info);
    
    /**
     * A drop has occured on this component and should be acted on accordingly.
     * The info contains things like what content was dropped, where it comes
     * from and how it should be added.
     * 
     * When adding the content, it should first be removed from where it comes
     * from.
     * 
     * @param info 
     */
    public void drop(DockTransferInfo info);
    
    /**
     * Whether this component contains any content.
     * 
     * @return true if the component contains no content, false otherwise
     */
    public boolean isEmpty();
    
    /**
     * Change the current parent of this component. A change can e.g. happen
     * because the component was moved to or from a split pane.
     * 
     * @param parent 
     */
    public void setDockParent(DockChild parent);
    
    /**
     * The component above in the docking structure.
     * 
     * @return The DockChild, can be null
     */
    public DockChild getDockParent();
    
    /**
     * Get the path to this component (not including the component itself).
     * 
     * @return 
     */
    public DockPath getPath();
    
    /**
     * Add the current component to the path (if applicable) and continue with
     * the parent, so that the path is build up from the bottom up.
     * 
     * @param path The path so far
     * @param child The child component
     * @return 
     */
    public DockPath buildPath(DockPath path, DockChild child);
    
    /**
     * All content contained in this component (should be ordered the way it is
     * displayed).
     * 
     * @return The content, possibly and empty collection when there is none
     * (never null)
     */
    public List<DockContent> getContents();
    
    /**
     * Get all contents from the same tab pane relative to the given content.
     * 
     * @param content The content that defines the starting position
     * @param direction -1 for left, 1 for right, 0 for both directions
     * @return The content, possibly and empty collection when there is none
     * (never null)
     */
    public List<DockContent> getContentsRelativeTo(DockContent content, int direction);
    
    /**
     * Set a setting. The setting value should not be modified, since the
     * unmodified values are stored in DockManager to be applied later directly
     * to a child, so it might not work correctly.
     * 
     * @param setting
     * @param value 
     */
    public void setSetting(DockSetting.Type setting, Object value);
    
}
