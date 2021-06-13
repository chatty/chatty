
package chatty.util.dnd;

import chatty.util.dnd.DockDropInfo.DropType;
import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DockPathEntry other = (DockPathEntry) obj;
        if (this.index != other.index) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        if (this.location != other.location) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.type);
        hash = 89 * hash + Objects.hashCode(this.location);
        hash = 89 * hash + this.index;
        hash = 89 * hash + Objects.hashCode(this.id);
        return hash;
    }
    
}
