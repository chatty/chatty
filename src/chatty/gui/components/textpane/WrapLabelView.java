
package chatty.gui.components.textpane;

import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.View;

/**
 * Always wrap long words.
 * 
 * @author tduva
 */
public class WrapLabelView extends LabelView {

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
    
//    public int getBreakWeight(int axis, float pos, float len) {
//        if (axis == View.X_AXIS) {
//            return View.ForcedBreakWeight;
//        }
//        return super.getBreakWeight(axis, pos, len);
//    }
}

