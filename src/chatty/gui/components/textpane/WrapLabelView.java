
package chatty.gui.components.textpane;

import java.awt.Shape;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Always wrap long words.
 * 
 * Contains parts of https://stackoverflow.com/a/14230668/2375667 to fix a line
 * breaking bug in JTextPane.
 * 
 * @author tduva
 */
public class WrapLabelView extends LabelView {

    private boolean isResettingBreakSpots = false;
    
    public WrapLabelView(Element elem) {
        super(elem);
        //System.out.println(elem);
    }

    /**
     * Always return 0 for the X_AXIS of the minimum span, so long words are
     * always wrapped.
     * 
     * @param axis
     * @return 
     */
    @Override
    public float getMinimumSpan(int axis) {
        switch (axis) {
            case View.X_AXIS:
                return 0;
            case View.Y_AXIS:
                return super.getMinimumSpan(axis);
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
        }
    }
    
    @Override
    public View breakView(int axis, int p0, float pos, float len) {
        if (axis == View.X_AXIS) {
            resetBreakSpots();
        }
        return super.breakView(axis, p0, pos, len);
    }

    public void resetBreakSpots() {
        isResettingBreakSpots = true;
        removeUpdate(null, null, null);
        isResettingBreakSpots = false;
   }

    @Override
    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.removeUpdate(e, a, f);
    }

    @Override
    public void preferenceChanged(View child, boolean width, boolean height) {
        if (!isResettingBreakSpots) {
            // Prevent this call when merely resetting the break spots cache
            super.preferenceChanged(child, width, height);
        }
    }
}

