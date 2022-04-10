
package chatty.util.dnd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author tduva
 */
public class DockLayoutSplit implements DockLayoutElement {
    
    public final DockLayoutElement left;
    public final DockLayoutElement right;
    public final int dividerLocation;
    public final int orientation;
    
    public DockLayoutSplit(DockLayoutElement left, DockLayoutElement right,
                           int dividerLocation, int orientation) {
        this.left = left;
        this.right = right;
        this.dividerLocation = dividerLocation;
        this.orientation = orientation;
    }
    
    @Override
    public String toString() {
        return String.format("(%s|%d|%s)", left, dividerLocation, right);
    }

    @Override
    public List<Object> toList() {
        List<Object> result = new ArrayList<>();
        result.add("s");
        result.add(dividerLocation); // 1
        result.add(orientation);     // 2
        result.add(left.toList());   // 3
        result.add(right.toList());  // 4
        return result;
    }

    public static DockLayoutElement fromList(List<Object> list) {
        if (!Objects.equals(list.get(0), "s")) {
            return null;
        }
        int dividerLocation = ((Number) list.get(1)).intValue();
        int orientiation = ((Number) list.get(2)).intValue();
        DockLayoutElement left = DockLayoutElement.fromList((List) list.get(3));
        DockLayoutElement right = DockLayoutElement.fromList((List) list.get(4));
        return new DockLayoutSplit(left, right, dividerLocation, orientiation);
    }

    @Override
    public List<String> getContentIds() {
        List<String> result = new ArrayList<>();
        result.addAll(left.getContentIds());
        result.addAll(right.getContentIds());
        return result;
    }
    
    @Override
    public List<String> getActiveContentIds() {
        List<String> result = new ArrayList<>();
        result.addAll(left.getActiveContentIds());
        result.addAll(right.getActiveContentIds());
        return result;
    }
    
}
