
package chatty.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Attaches components to one owner component, which makes them move along with
 * the owner.
 * 
 * The components are only moved as long as they are at least half on screen
 * (currently only horizontally, vertically the upper left corner has to be on
 * screen).
 * 
 * @author tduva
 */
public class AttachedWindowManager {
    
    private final Set<Component> components = new HashSet<>();
    private final Component owner;
    private final Set<Point> ignoreOnce = new HashSet<>();
    
    private int width;
    private int height;
    private int x;
    private int y;
    
    private boolean enabled;
    
    private final Point temp = new Point();
    private final Point temp2 = new Point();
    
    /**
     * Create a new instance which attaches a listener to listen for changes in
     * position on the owner Component.
     * 
     * @param owner The Component that the other Components are attached to
     */
    public AttachedWindowManager(Component owner) {
        owner.addComponentListener(new ComponentAdapter() {
            
            @Override
            public void componentMoved(ComponentEvent e) {
                int newWidth = e.getComponent().getWidth();
                int newHeight = e.getComponent().getHeight();
                boolean sizeChanged = newWidth != width || newHeight != height;
                width = newWidth;
                height = newHeight;
                
                int newX = e.getComponent().getX();
                int newY = e.getComponent().getY();
                int movedX = newX - x;
                int movedY = newY - y;
                x = newX;
                y = newY;
                
                if (ignoreOnce.contains(e.getComponent().getLocation(temp))) {
                    ignoreOnce.remove(temp);
                    return;
                }
                if (sizeChanged) {
                    return;
                }
                if (!enabled) {
                    return;
                }
                for (Component comp : components) {
                    temp2.x = comp.getX() + movedX;
                    temp2.y = comp.getY() + movedY;
                    if (GuiUtil.isPointOnScreen(temp2, comp.getWidth() / 2, 10)) {
                        comp.setLocation(temp2);
                    }
                }
            }
        });
        
        this.owner = owner;
    }
    
    /**
     * Attach the given Component to the owner of this instance.
     * 
     * @param comp The Component to attach, does nothing if you try to attach
     * the owner itself
     */
    public void attach(Component comp) {
        if (comp != owner && !components.contains(comp)) {
            components.add(comp);
        }
    }
    
    public void detach(Component comp) {
        components.remove(comp);
    }
    
    /**
     * Enable or disable the movement of the attached components.
     * 
     * @param enable true to enable the movement, false to disable it
     */
    public void setEnabled(boolean enable) {
        enabled = enable;
    }
    
    /**
     * Doesn't move the attached components once if the owner was moved to the
     * given location.
     * 
     * @param location The owner location to be ignored once
     */
    public void ignoreLocationOnce(Point location) {
        ignoreOnce.add(location);
    }
    
}
