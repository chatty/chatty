
package chatty.util.dnd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author tduva
 */
public class DockLayoutTabs implements DockLayoutElement {
    
    public final List<String> contents;
    public final String activeId;
    
    public DockLayoutTabs(List<String> contents, String activeId) {
        this.contents = contents;
        this.activeId = activeId;
    }
    
    @Override
    public String toString() {
        return contents.toString();
    }

    @Override
    public List<Object> toList() {
        List<Object> result = new ArrayList<>();
        result.add("t");
        result.add(contents);
        result.add(activeId);
        return result;
    }
    
    public static DockLayoutElement fromList(List<Object> list) {
        if (!Objects.equals(list.get(0), "t")) {
            return null;
        }
        List<String> contents = new ArrayList<>();
        for (Object o : (List) list.get(1)) {
            if (o instanceof String) {
                contents.add((String) o);
            }
        }
        String activeId = null;
        if (list.size() > 2) {
            activeId = (String) list.get(2);
        }
        return new DockLayoutTabs(contents, activeId);
    }

    @Override
    public List<String> getContentIds() {
        return new ArrayList<>(contents);
    }
    
    @Override
    public List<String> getActiveContentIds() {
        List<String> result = new ArrayList<>();
        if (activeId != null) {
            result.add(activeId);
        }
        return result;
    }
    
}
