
package chatty.util.hotkeys;

import javax.swing.Action;

/**
 * An action to be performed by a hotkey, with an id, label and the actual
 * action to be performed.
 * 
 * @author tduva
 */
public class HotkeyAction {
    
    public final String id;
    public final String label;
    public final Action action;
    
    public HotkeyAction(String id, String label, Action action) {
        this.id = id;
        this.label = label;
        this.action = action;
    }
    
}
