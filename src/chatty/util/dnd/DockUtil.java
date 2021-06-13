
package chatty.util.dnd;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Window;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

/**
 *
 * @author tduva
 */
public class DockUtil {
    
    public static DockSplit createSplit(DockDropInfo info, DockChild prev, DockChild added) {
        DockSplit result = null;
        switch (info.location) {
            case LEFT:
                result = new DockSplit(JSplitPane.HORIZONTAL_SPLIT, added, prev);
                break;
            case RIGHT:
                result = new DockSplit(JSplitPane.HORIZONTAL_SPLIT, prev, added);
                break;
            case BOTTOM:
                result = new DockSplit(JSplitPane.VERTICAL_SPLIT, prev, added);
                break;
            case TOP:
                result = new DockSplit(JSplitPane.VERTICAL_SPLIT, added, prev);
                break;
        }
        return result;
    }
    
    public static boolean isMouseOverWindow() {
        Point p = MouseInfo.getPointerInfo().getLocation();
        for (Window w : Window.getWindows()) {
            if (w.isVisible() && w.getBounds().contains(p)) {
                return true;
            }
        }
        return false;
    }
    
    public static DockTransferable getTransferable(TransferHandler.TransferSupport info) {
        try {
            return (DockTransferable) info.getTransferable().getTransferData(DockTransferable.FLAVOR);
        }
        catch (IOException | UnsupportedFlavorException ex) {
            System.out.println(ex);
            return null;
        }
    }
    
    public static DockTransferable getTransferable(Transferable t) {
        try {
            return (DockTransferable) t.getTransferData(DockTransferable.FLAVOR);
        }
        catch (IOException | UnsupportedFlavorException ex) {
            System.out.println(ex);
            return null;
        }
    }
    
    /**
     * When a split is removed, choose a tab from the remaining child as active
     * (child may be another split or tabs directly).
     * 
     * @param child The remaining child from the split
     * @param base The base to inform
     */
    public static void activeTabAfterJoin(DockChild child, DockBase base) {
        if (base == null) {
            return;
        }
        if (child instanceof DockTabsContainer) {
            DockTabsContainer tabs = (DockTabsContainer)child;
            base.tabChanged(null, tabs.getCurrentContent());
        }
        else if (child instanceof DockSplit) {
            // Keep looking for tabs in the left child of the split
            activeTabAfterJoin(((DockSplit)child).getLeftChild(), base);
        }
    }
    
    public static DockPathEntry getNext(DockContent content, DockChild current) {
        DockPath target = content.getTargetPath();
        if (target != null) {
            return target.getNext(current.getPath());
        }
        return null;
    }
    
    /**
     * Exchanging a component can reset the divider location (seems affected by
     * resize weight), so set previous location again.
     * 
     * @param split 
     */
    public static void preserveDividerLocation(JSplitPane split) {
        int location = split.getDividerLocation();
        SwingUtilities.invokeLater(() -> split.setDividerLocation(location));
    }
    
    public static List<String> getContentIds(List<DockContent> contents) {
        List<String> ids = new ArrayList<>();
        for (DockContent c : contents) {
            if (c.getId() != null) {
                ids.add(c.getId());
            }
        }
        return ids;
    }
    
    /**
     * Get the DockContent with the given id from the Collection.
     * 
     * @param contents The Collection to get the DockContent from
     * @param id The id of the DockContent to find
     * @return The DockContent, or null if none could be found
     */
    public static DockContent getContentById(Collection<DockContent> contents, String id) {
        for (DockContent content : contents) {
            if (content.getId().equals(id)) {
                return content;
            }
        }
        return null;
    }
    
    /**
     * Return a new list containing only the content that has an id starting
     * with the given prefix.
     * 
     * @param input The input list (not modified)
     * @param prefix The prefix
     * @return The new list (could be empty, never null)
     */
    public static List<DockContent> getContentsByPrefix(List<DockContent> input, String prefix) {
        List<DockContent> result = new ArrayList<>();
        for (DockContent content : input) {
            if (content.getId().startsWith(prefix)) {
                result.add(content);
            }
        }
        return result;
    }
    
}
