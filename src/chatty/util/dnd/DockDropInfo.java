
package chatty.util.dnd;

import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;

/**
 * Info provided by a component in reaction to being asked whether a drop is
 * possible at a specific location.
 * 
 * @author tduva
 */
public class DockDropInfo {
    
    /**
     * What a drop should do. TOP/LEFT/BOTTOM/RIGHT creates a split in that
     * location. TAB inserts a tab into a tab pane (possibly just moving).
     * INVALID marks the location explicitly as an invalid drop, which prevents
     * additional looking for a drop for that location.
     */
    public enum DropType {
        TOP, LEFT, BOTTOM, RIGHT, CENTER, TAB, INVALID
    }
    
    public final DockChild dropComponent;
    public final DropType location;
    public final Rectangle rect;
    public final int index;
    
    public DockDropInfo(DockChild dropComponent, DropType location, Rectangle rect, int index) {
        this.dropComponent = dropComponent;
        this.rect = rect;
        this.location = location;
        this.index = index;
    }
    
    public static Rectangle makeRect(JComponent component, DropType location, int size, int max) {
        return makeRect(component, location, size, 0, max);
    }
    
    public static Rectangle makeRect(JComponent component, DropType location, int size, int otherSize, int max) {
        int width = component.getWidth();
        int height = component.getHeight();
        int w = Math.min(width * size / 100, max);
        int h = Math.min(height * size / 100, max);
        int w2 = width * otherSize / 100;
        int h2 = height * otherSize / 100;
        switch (location) {
            case TOP:
                return new Rectangle(w2, 0, width - w2*2, h);
            case LEFT:
                return new Rectangle(0, h2, w, height - h2*2);
            case RIGHT:
                return new Rectangle(width - w - 1, h2, w, height - h2*2 - 1);
            case BOTTOM:
                return new Rectangle(w2, height - h - 1, width - w2*2, h);
        }
        return new Rectangle((width - w)/2, (height - h)/2, w, h);
    }
    
    public static DropType determineLocation(JComponent component, Point point, int size, int max, int size2) {
        int width = component.getWidth();
        int height = component.getHeight();
        int w = Math.min(width * size / 100, max);
        int h = Math.min(height * size / 100, max);
        int w2 = width * size2 / 100;
        int h2 = height * size2 / 100;
        
//        System.out.println(String.format("%dx%d %d/%d %d,%d", width, height, w, h, point.x, point.y));
        
        if (point.x < w && point.y > h2 && point.y < height - h2
                // Diagonal
                && (point.y > w || point.y > point.x)
                && (point.y < height - w || point.y < height - point.x)) {
            return DropType.LEFT;
        }
        if (point.x > width - w && point.y > h2 && point.y < height - h2
                && (point.y > w || point.y > -point.x + width)
                && (point.y < height - w || point.y < height -(width - point.x))) {
            return DropType.RIGHT;
        }
        if (point.y < h && point.y >= 0 && point.x > w2 && point.x < width - w2) {
            return DropType.TOP;
        }
        if (point.y > height - h && point.y < height && point.x > w2 && point.x < width - w2) {
            return DropType.BOTTOM;
        }
        if (point.x > w && point.y > h && point.x < width - w && point.y < height - h) {
            return DropType.CENTER;
        }
        return null;
    }
    
}
