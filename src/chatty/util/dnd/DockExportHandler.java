
package chatty.util.dnd;

import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.MOVE;

/**
 *
 * @author tduva
 */
public class DockExportHandler extends TransferHandler {
    
    private final DockTabs tabs;
    private Transferable transferable;
    
    public DockExportHandler(DockTabs tabs) {
        this.tabs = tabs;
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public Transferable createTransferable(JComponent c)
    {
        return transferable;
    }
    
    public void drag(int index, MouseEvent e) {
        transferable = new DockTransferable(tabs.getContent(index), tabs, index, tabs.createScreenshot(index));
        exportAsDrag(tabs.getComponent(), e, TransferHandler.MOVE);
    }

    @Override
    public void exportDone(JComponent c, Transferable t, int action)
    {
//        System.out.println("EXPORT DONE:"+action+" "+MouseInfo.getPointerInfo().getLocation());
        tabs.requestStopDrag(DockUtil.getTransferable(t));
    }
//    
//    @Override
//    public boolean canImport(TransferHandler.TransferSupport info) {
//        System.out.println("Tabs:"+info.getDropLocation());
//        return true;
//    }
//
//    @Override
//    public boolean importData(TransferHandler.TransferSupport info) {
//        System.out.println("Tabs:"+info.getComponent());
//        return true;
//    }
    
}
