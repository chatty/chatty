
package chatty.util.dnd;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author tduva
 */
public class DockLayoutPopout implements DockLayoutElement {

    public final String id;
    public final Point location;
    public final Dimension size;
    public final DockLayoutElement child;
    public final int state;
    
    public DockLayoutPopout(String id, Point location, Dimension size, int state, DockLayoutElement child) {
        this.id = id;
        this.location = location;
        this.size = size;
        this.child = child;
        this.state = state;
    }
    
    @Override
    public String toString() {
        return String.format("$%s(%s,%s,%d)%s$", id, location, size, state, child);
    }
    
    public List<Object> toList() {
        List<Object> result = new ArrayList<>();
        result.add("p"); // 0
        result.add(id);  // 1
        if (location != null && size != null) { // 2
            result.add(location.x+","+location.y+";"+size.width+","+size.height);
        }
        else {
            result.add(null);
        }
        result.add(state);          // 3
        result.add(child.toList()); // 4
        return result;
    }
    
    public static DockLayoutPopout fromList(List<Object> list) {
        if (!Objects.equals(list.get(0), "p")) {
            return null;
        }
        String id = (String) list.get(1);
        int state = ((Number) list.get(3)).intValue();
        DockLayoutElement child = DockLayoutElement.fromList((List) list.get(4));
        if (list.get(2) != null) {
            String[] locationSize = ((String) list.get(2)).split(";");
            String[] location = locationSize[0].split(",");
            String[] size = locationSize[1].split(",");
            int x = Integer.parseInt(location[0]);
            int y = Integer.parseInt(location[1]);
            int w = Integer.parseInt(size[0]);
            int h = Integer.parseInt(size[1]);
            return new DockLayoutPopout(id, new Point(x, y), new Dimension(w, h), state, child);
        }
        return new DockLayoutPopout(id, null, null, state, child);
    }

    @Override
    public List<String> getContentIds() {
        return child.getContentIds();
    }
    
    @Override
    public List<String> getActiveContentIds() {
        return child.getActiveContentIds();
    }
    
    public boolean canChange(Point location, Dimension size, int state) {
        if (this.location == null || size == null || state == -1) {
            return false;
        }
        if (!this.location.equals(location)) {
            return true;
        }
        if (!this.size.equals(size)) {
            return true;
        }
        return this.state != state;
    }

}
