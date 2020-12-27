
package chatty.util.dnd;

import chatty.util.dnd.DockDropInfo.DropType;

/**
 *
 * @author tduva
 */
public class DockPathEntry {

    public enum Type {
        SPLIT, TAB, POPOUT
    }
    
    public final Type type;
    public final DropType location;
    public final int index;
    public final String id;
    
    private DockPathEntry(DropType location) {
        this.type = Type.SPLIT;
        this.location = location;
        this.index = -1;
        this.id = null;
    }
    
    private DockPathEntry(int index) {
        this.type = Type.TAB;
        this.location = null;
        this.index = index;
        this.id = null;
    }
    
    private DockPathEntry(String id) {
        this.type = Type.POPOUT;
        this.location = null;
        this.index = -1;
        this.id = id;
    }
    
    public static DockPathEntry createSplit(DropType location) {
        return new DockPathEntry(location);
    }
    
    public static DockPathEntry createTab(int index) {
        return new DockPathEntry(index);
    }
    
    public static DockPathEntry createPopout(String popoutId) {
        return new DockPathEntry(popoutId);
    }
    
    @Override
    public String toString() {
        switch (type) {
            case POPOUT: return String.format("%s (%s)", type, id);
            case SPLIT: return String.format("%s (%s)", type, location);
            case TAB: return String.format("%s (%s)", type, index);
        }
        return "?";
    }
    
}
