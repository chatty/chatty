
package chatty.util.dnd;

/**
 * Info provided to the component that receives a drop. Some of that info comes
 * from the export, other info may have been created by the same component that
 * receives it.
 * 
 * @author tduva
 */
public class DockTransferInfo {
    
    public final DockDropInfo dropInfo;
    public final DockTransferable importInfo;
    
    public DockTransferInfo(DockDropInfo dropInfo, DockTransferable importInfo) {
        this.dropInfo = dropInfo;
        this.importInfo = importInfo;
    }
    
    public String toString() {
        return String.format("(%s,%s)",
                dropInfo, importInfo);
    }
    
}
