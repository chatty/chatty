
package chatty.util.dnd;

import chatty.util.dnd.DockPathEntry.Type;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Stores the path to a certain child or content. Each entry may contain data
 * required to recreate the path (for example split orientation, split size, tab
 * index), although most of it isn't used yet. Currently the path is only used
 * to add a tab to a certain tab pane that already exists.
 * 
 * @author tduva
 */
public class DockPath {
    
    private final LinkedList<DockPathEntry> list = new LinkedList<>();
    private final DockContent content;
    
    public DockPath(DockContent content) {
        this.content = content;
    }
    
    public DockPath() {
        this.content = null;
    }
    
    public void addParent(DockPathEntry parent) {
        list.addFirst(parent);
    }
    
    public String getPopoutId() {
        if (list.isEmpty() || list.getFirst().type != Type.POPOUT) {
            return null;
        }
        return list.getFirst().id;
    }
    
    /**
     * Get the entry of this path that would come after the given path, based on
     * length alone.
     * 
     * @param path
     * @return 
     */
    public DockPathEntry getNext(DockPath path) {
        if (path.list.size() >= list.size()) {
            return null;
        }
        return list.get(path.list.size());
    }
    
    public DockContent getContent() {
        return content;
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", list, content);
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
        final DockPath other = (DockPath) obj;
        if (!Objects.equals(this.list, other.list)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.list);
        return hash;
    }
    
}
