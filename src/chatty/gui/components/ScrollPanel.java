
package chatty.gui.components;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Scrollable;

/**
 *
 * @author tduva
 */
public class ScrollPanel extends JPanel implements Scrollable {
    
    public ScrollPanel() {
        
    }
    
    public ScrollPanel(LayoutManager layoutManager) {
        super(layoutManager);
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    /**
     * Still resize horizontally despite this panel being in a scrollpane.
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
    
}
