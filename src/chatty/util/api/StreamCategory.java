
package chatty.util.api;

import chatty.util.StringUtil;
import java.util.Objects;

/**
 * A stream category (often a game) should have an id and a name, although an
 * id may not be available if it was saved before ids were used.
 * 
 * @author tduva
 */
public class StreamCategory implements Comparable<StreamCategory> {

    public static final StreamCategory EMPTY = new StreamCategory("", "");
    
    /**
     * The id of the category, may be null if no id is available. Empty for "no
     * category".
     */
    public final String id;
    
    /**
     * The name of the category, must not be null. Empty for "no category".
     */
    public final String name;
    
    public StreamCategory(String id, String name) {
        if (StringUtil.isNullOrEmpty(name)) {
            // No game is specified, so id should not be null (missing)
            this.id = "";
            this.name = "";
        }
        else {
            this.id = id;
            this.name = name;
        }
    }
    
    /**
     * Check if this entry has a category id set (not null), although it may
     * still be empty (no category).
     * 
     * @return 
     */
    public boolean hasId() {
        return id != null;
    }
    
    public boolean isEmpty() {
        return name.isEmpty();
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(StreamCategory o) {
        return name.compareToIgnoreCase(o.name);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.name);
        return hash;
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
        final StreamCategory other = (StreamCategory) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }
    
}
