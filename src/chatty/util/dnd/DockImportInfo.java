
package chatty.util.dnd;

import java.awt.Component;
import java.awt.Point;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

/**
 * Information for a potential import, which helps decide a component if a drop
 * can occur. It combines the transferable (containing info from the component
 * where the drag movement started) and the support info (which is provided by
 * the drag&drop system and contains info like the potential drop coordinates).
 * 
 * @author tduva
 */
public class DockImportInfo {
    
    public final TransferHandler.TransferSupport info;
    public final DockTransferable tf;
    
    public DockImportInfo(TransferHandler.TransferSupport info, DockTransferable tf) {
        this.tf = tf;
        this.info = info;
    }
    
    /**
     * Gets the drop location relative to the given component.
     * 
     * @param comp
     * @return 
     */
    public Point getLocation(Component comp) {
        return SwingUtilities.convertPoint(info.getComponent(), info.getDropLocation().getDropPoint(), comp);
    }
    
    public String toString() {
        return String.format("DIF(%s,%s)",
                info, tf);
    }
    
}
