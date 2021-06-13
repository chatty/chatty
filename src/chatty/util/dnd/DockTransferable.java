
package chatty.util.dnd;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * Contains information provided by the source component of a drag operation,
 * used mainly for drawing the interface and the potential drop.
 * 
 * @author tduva
 */
public class DockTransferable implements Transferable {
    
    public static final DataFlavor FLAVOR =
            new DataFlavor(DockTransferable.class, "DockTransferable");
    
    public final DockContent content;
    public final DockChild source;
    public final Image image;
    public final int sourceIndex;
    
    public DockTransferable(DockContent content, DockChild source, int index, Image image) {
        this.content = content;
        this.source = source;
        this.sourceIndex = index;
        this.image = image;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return new DataFlavor[]{FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        if (FLAVOR.equals(flavor))
        {
            return true;
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) {
        return this;
    }
    
    @Override
    public String toString() {
        return String.format("TF(%d,%s,%s)",
                sourceIndex, content, source);
    }
    
}
