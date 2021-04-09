
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
    
    public DockLayoutTabs(List<String> contents) {
        this.contents = contents;
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
        return new DockLayoutTabs(contents);
    }

    @Override
    public List<String> getContentIds() {
        return new ArrayList<>(contents);
    }
    
}
