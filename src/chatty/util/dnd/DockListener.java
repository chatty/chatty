
package chatty.util.dnd;

import java.util.List;

/**
 * 
 * 
 * @author tduva
 */
public interface DockListener {
    
    /**
     * The active content changed, probably due to a focus change.
     * 
     * @param popout The popout, or null if in main DockBase
     * @param content The content (never null)
     */
    public void activeContentChanged(DockPopout popout, DockContent content, boolean focusChange);
    public void popoutOpened(DockPopout popout, DockContent content);
    public void popoutClosed(DockPopout popout, List<DockContent> content);
    
}
