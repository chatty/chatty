
package chatty.util.dnd;

import java.awt.Window;

/**
 *
 * @author tduva
 */
public interface DockPopout {

    public Window getWindow();
    
    public DockBase getBase();
    
    public void setTitle(String title);
    
    public String getId();
    
    public void setId(String id);
    
}
