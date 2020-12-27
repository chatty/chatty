
package chatty.util.dnd;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/**
 * Allows a tab component to receive an update notification to update it's
 * styling when something relevant in the tab pane changed.
 * 
 * @author tduva
 */
public interface DockTabComponent {
    
    /**
     * Called when the tab pane first installs a tab component and when
     * something relevant in the tab pane changes (e.g. selected tab changes,
     * which causes a tab to have different colors) so that the tab component
     * can update accordingly.
     * 
     * @param pane The tab pane
     * @param index The tab index associated with this tab component
     */
    public void update(JTabbedPane pane, int index);
    
    /**
     * The actual tab component to install on the tab pane.
     * 
     * @return The component (may be null, in which case no custom tab component
     * is used)
     */
    public JComponent getComponent();
    
}
