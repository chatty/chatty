
package chatty.gui.colors;

import java.awt.Color;
import java.util.Objects;

/**
 *
 * @author tduva
 */
public abstract class ColorItem {
    
    abstract public String getId();
    abstract public Color getColor();
    
    @Override
    public String toString() {
        return getId();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ColorItem) {
            return getId().equals(((ColorItem)o).getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(getId());
        return hash;
    }
    
}
