
package chatty.gui.components.tabs;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Allow tabs to be dragged to be repositioned.
 * 
 * This probably won't work very well if the tabs aren't oriented horizontally
 * and ok in most cases but not quite perfectly if they are placed in more than
 * one row.
 *
 * Based on: https://stackoverflow.com/a/60306/2375667 (but extended quite a bit)
 * 
 * @author tduva
 */
public class DraggableTabbedPane extends JTabbedPane {
    
    /**
     * How long after the drag started should the dragging become visible and
     * take effect. This is to prevent accidently dragging while just clicking
     * on the tab while moving the mouse slightly, which can lead to the
     * hovering tab show up for a fraction of a second.
     * 
     * It is only the painting of the hovering tab and the actual drop that is
     * prevented when the drag is not long enough, the drag is still started in
     * the background in case it continues, so it already has started on the
     * correct tab and not the one the mouse was over when the threshold was
     * satisfied.
     */
    private static final int DRAG_STARTED_THRESHOLD = 100;
    
    private boolean dragging;
    private int draggedTabIndex;
    private Image draggedTabImage;
    private int draggedTabImageHeight;
    private Point mouseLocation;
    private boolean canDrag;
    private long dragStarted;
    
    public DraggableTabbedPane() {
        super();
        
        addMouseMotionListener(new MouseMotionAdapter() {
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!canDrag) {
                    return;
                }
                if (dragStarted == 0) {
                    dragStarted = System.currentTimeMillis();
                }
                
                mouseLocation = e.getPoint();
                if (!dragging) {
                    int tabIndex = getIndexForPoint(e.getPoint());
                    if (tabIndex >= 0) {
                        draggedTabIndex = tabIndex;
                        
                        Rectangle bounds = getBoundsAt(tabIndex);
                        
                        Image totalImage = new BufferedImage(getWidth(),
                                getHeight(), BufferedImage.TYPE_INT_ARGB);
                        Graphics totalGraphics = totalImage.getGraphics();
                        totalGraphics.setClip(bounds);
                        setDoubleBuffered(false);
                        paintComponent(totalGraphics);
                        
                        draggedTabImageHeight = bounds.height;
                        draggedTabImage = new BufferedImage(bounds.width,
                                bounds.height, BufferedImage.TYPE_INT_ARGB);
                        Graphics g = draggedTabImage.getGraphics();
                        g.drawImage(totalImage, 0, 0,
                                bounds.width, bounds.height,
                                bounds.x, bounds.y,
                                bounds.x+bounds.width, bounds.y+bounds.height,
                                DraggableTabbedPane.this);
                        
                        dragging = true;
                    }
                }
                repaint();
            }
        });
        
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                dragStarted = 0;
                /**
                 * Only allow a drag to start using the left mouse button. Can't
                 * use isPopupTrigger() to prevent it from dragging when opening
                 * a context menu, because the popup trigger isn't necessarily
                 * available in mousePressed().
                 */
                canDrag = SwingUtilities.isLeftMouseButton(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging && dragStartedThreshold()) {
                    int dropOnIndex = getDropIndexForPoint(e.getPoint());
                    if (dropOnIndex >= 0) {
                        moveTab(draggedTabIndex, dropOnIndex);
                    }
                }
                dragging = false;
                draggedTabImage = null;
                dragStarted = 0;
                repaint();
            }
        });
        
        addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                dragging = false;
                dragStarted = 0;
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (dragging && mouseLocation != null && draggedTabImage != null && dragStartedThreshold()) {
            // Draw floating tab image
            g.drawImage(draggedTabImage, mouseLocation.x, mouseLocation.y - draggedTabImageHeight / 2, this);
            
            /**
             * Draw indicator where the tab would be dropped onto. This might
             * not work very well if the tabs are layed out in more than one
             * row or are not placed horizontally.
             */
            int tabIndex = getDropIndexForPoint(mouseLocation);
            if (tabIndex >= 0 && tabIndex != draggedTabIndex
                    && tabIndex != draggedTabIndex+1) {
                boolean drawBeforeTab = true;
                if (tabIndex >= getTabCount()) {
                    // If after last tab, then draw behind the last tab
                    tabIndex = getTabCount() - 1;
                    drawBeforeTab = false;
                }
                Rectangle bounds = getUI().getTabBounds(DraggableTabbedPane.this, tabIndex);
                if (drawBeforeTab) {
                    g.fillRect(bounds.x, bounds.y, 3, bounds.height);
                } else {
                    g.fillRect(bounds.x+bounds.width-3, bounds.y, 3, bounds.height);
                }
            }
        }
    }
    
    /**
     * Returns the time in milliseconds how long ago the current drag started.
     * 
     * @return The number of milliseconds since the current drag started, or -1
     * if no drag is currently in progress
     */
    private long dragStartedAgo() {
        if (dragStarted == 0) {
            return -1;
        }
        return System.currentTimeMillis() - dragStarted;
    }
    
    /**
     * Check if the current drag has exceeded the DRAG_STARTED_THRESHOLD, which
     * prevents accidental dragging.
     * 
     * @return true if the current drag should be considered as actually
     * started, false otherwise or if no drag is currently in progress
     */
    private boolean dragStartedThreshold() {
        return dragStartedAgo() > DRAG_STARTED_THRESHOLD;
    }
    
    /**
     * Move a tab from a given index to another.
     * 
     * @param from The index of the tab to move
     * @param to The index to move the tab to
     * @throws IndexOutOfBoundsException if from or to are not in the range of
     * tab indices
     */
    private void moveTab(int from, int to) {
        if (from == to) {
            // Nothing to do here
            return;
        }
        Component comp = getComponentAt(from);
        String title = getTitleAt(from);
        String toolTip = getToolTipTextAt(from);
        Icon icon = getIconAt(from);
        Component tabComp = getTabComponentAt(from);
        removeTabAt(from);
        
        /**
         * If the source index (the tab that is moved) is in front of the target
         * index, then removing the source index will have shifted the target
         * index back by one, so adjust for that.
         */
        if (from < to) {
            to--;
        }
        insertTab(title, icon, comp, toolTip, to);
        setTabComponentAt(to, tabComp);
        setSelectedComponent(comp);
    }
    
    private int getIndexForPoint(Point p) {
        return indexAtLocation(p.x, p.y);
    }
    
    /**
     * Get the drop index for the given location. The drop index is the location
     * between the tabs to insert the dragged tab into. Basicially it's the
     * index of the tab to insert the dragged tab in front of, or the last tab
     * index + 1 if it should be inserted after the last tab.
     * 
     * @param p The location to find the drop index for
     * @return The drop index (> 0 and &lt= tab count), or -1 if none could be
     * found
     */
    private int getDropIndexForPoint(Point p) {
        int index = getIndexForPoint(p);
        if (index >= 0) {
            Rectangle bounds = getBoundsAt(index);
            if (p.x < bounds.x + bounds.width / 2) {
                return index;
            } else {
                return index+1;
            }
        } else {
            /**
             * Basicially making the last tab wider to have more leeway with
             * dropping it after the last tab.
             */
            Rectangle bounds = getBoundsAt(getTabCount() - 1);
            bounds.width += 30;
            if (bounds.contains(p)) {
                return getTabCount();
            }
        }
        return -1;
    }
    
}
