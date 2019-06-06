
package chatty.gui.colors;

import java.awt.Color;
import java.util.Objects;

/**
 *
 * @author tduva
 */
public class ColorItem {
    
    private final String id;
    private final Color foreground;
    private final Color background;
    private final boolean foregroundEnabled;
    private final boolean backgroundEnabled;
    
    public ColorItem(String id,
            Color foreground, boolean foregroundEnabled,
            Color background, boolean backgroundEnabled) {
        this.id = id;
        this.foreground = foreground;
        this.foregroundEnabled = foregroundEnabled;
        this.background = background;
        this.backgroundEnabled = backgroundEnabled;
    }
    
    public String getId() {
        return id;
    }
    
    public Color getForeground() {
        return foreground;
    }
    
    public Color getBackground() {
        return background;
    }
    
    public Color getForegroundIfEnabled() {
        return foregroundEnabled ? foreground : null;
    }
    
    public Color getBackgroundIfEnabled() {
        return backgroundEnabled ? background : null;
    }
    
    public boolean getForegroundEnabled() {
        return foregroundEnabled;
    }
    
    public boolean getBackgroundEnabled() {
        return backgroundEnabled;
    }
    
    public boolean isEmpty() {
        return foreground == null && background == null;
    }
    
    @Override
    public String toString() {
        return getId();
    }
    
    public String toStringAll() {
        return String.format("%s,%s/%s/%s/%s",
                getId(),
                getForeground(),
                getForegroundEnabled(),
                getBackground(),
                getBackgroundEnabled());
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

    public boolean equalsAll(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ColorItem other = (ColorItem) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.foreground, other.foreground)) {
            return false;
        }
        if (!Objects.equals(this.background, other.background)) {
            return false;
        }
        if (this.foregroundEnabled != other.foregroundEnabled) {
            return false;
        }
        if (this.backgroundEnabled != other.backgroundEnabled) {
            return false;
        }
        return true;
    }
    
}
