
package chatty.util.dnd;

import java.util.List;

/**
 *
 * @author tduva
 */
public interface DockLayoutElement {
    
    public List<Object> toList();
    
    /**
     * Get all content ids contained in this element or children of the element.
     * 
     * @return A list of content ids, may be empty, never null
     */
    public List<String> getContentIds();
    
    public List<String> getActiveContentIds();
    
    public static DockLayoutElement fromList(List<Object> list) {
        DockLayoutElement e = DockLayoutSplit.fromList(list);
        if (e != null) {
            return e;
        }
        e = DockLayoutTabs.fromList(list);
        if (e != null) {
            return e;
        }
        return null;
    }
    
}
