
package chatty.gui.components.textpane;

import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.View;

/**
 * Changes the position of the icon slightly, so it overlaps with following text
 * as to not take as much space. Not perfect still, but ok.
 * 
 * @author tduva
 */
class MyIconView extends IconView {
    public MyIconView(Element elem) {
        super(elem);
    }
    
    private static final int lineHeight = 20;
    
    @Override
    public float getAlignment(int axis) {
        //System.out.println(this.getElement());

        if (axis ==  View.Y_AXIS) {
            //System.out.println(this.getElement());
//            float height = super.getPreferredSpan(axis);
//            double test = 1.5 - lineHeight / height * 0.5;
//            System.out.println(height+" "+test+" "+this.getAttributes());
//            return (float)test;
            return 1f;
            
        }
        return super.getAlignment(axis);
    }
    
    @Override
    public float getPreferredSpan(int axis) {
        if (axis == View.Y_AXIS) {
            float height = super.getPreferredSpan(axis);
//            float test = lineHeight / height;
            float test = 0.7f;
            //System.out.println(test);
            height *= test;
            return height;
        }
        return super.getPreferredSpan(axis);
    }
    
    /**
     * Wrap Icon Labels as well, to prevent horizontal scrolling when a row of
     * continuous emotes (without space) is present.
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
}
