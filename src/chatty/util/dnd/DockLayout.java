
package chatty.util.dnd;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tduva
 */
public class DockLayout {

    public final List<DockLayoutPopout> main;
    
    public DockLayout(List<DockLayoutPopout> main) {
        this.main = main;
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Layout:\n");
        for (DockLayoutPopout p : main) {
            b.append(p.toString()).append("\n");
        }
        return b.toString();
    }
    
    public List<Object> toList() {
        List<Object> result = new ArrayList<>();
        for (DockLayoutPopout p : main) {
            result.add(p.toList());
        }
        return result;
    }
    
    public static DockLayout fromList(List<Object> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<DockLayoutPopout> result = new ArrayList<>();
        for (Object o : list) {
            DockLayoutPopout p = DockLayoutPopout.fromList((List) o);
            result.add(p);
        }
        return new DockLayout(result);
    }
    
    public List<String> getContentIds() {
        List<String> result = new ArrayList<>();
        for (DockLayoutElement e : main) {
            result.addAll(e.getContentIds());
        }
        return result;
    }
    
    public List<String> getActiveContentIds() {
        List<String> result = new ArrayList<>();
        for (DockLayoutElement e : main) {
            result.addAll(e.getActiveContentIds());
        }
        return result;
    }
    
    /**
     * Get the path for the given content id, if it saved in this layout.
     * 
     * @param contentId The content id
     * @return The path, or null if the content id could not be found
     */
    public DockPath getPath(String contentId) {
        DockPath path = new DockPath();
        for (DockLayoutPopout p : main) {
            if (fillPath(contentId, p.child, path)) {
                path.addParent(DockPathEntry.createPopout(p.id));
            }
        }
        if (path.isEmpty()) {
            return null;
        }
        return path;
    }
    
    public DockLayoutPopout getMain() {
        for (DockLayoutPopout p : main) {
            if (p.id == null) {
                return p;
            }
        }
        return null;
    }
    
    /**
     * Recursively fill the given path to point to the given content id.
     * 
     * @param contentId
     * @param e
     * @param path
     * @return The path, may be empty if the content id could not be found
     */
    private static boolean fillPath(String contentId, DockLayoutElement e, DockPath path) {
        if (e instanceof DockLayoutTabs) {
            DockLayoutTabs tabs = (DockLayoutTabs) e;
            if (tabs.contents.contains(contentId)) {
                path.addParent(DockPathEntry.createTab(0));
                return true;
            }
        }
        else if (e instanceof DockLayoutSplit) {
            DockLayoutSplit split = (DockLayoutSplit) e;
            if (fillPath(contentId, split.left, path)) {
                path.addParent(DockPathEntry.createSplit(DockDropInfo.DropType.LEFT));
                return true;
            }
            else if (fillPath(contentId, split.right, path)) {
                path.addParent(DockPathEntry.createSplit(DockDropInfo.DropType.RIGHT));
                return true;
            }
        }
        return false;
    }
    
}
