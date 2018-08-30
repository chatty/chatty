
package chatty.gui.components.textpane;

import chatty.util.Debugging;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

/**
 * Changes the position of the icon slightly, so it overlaps with following text
 * as to not take as much space. Not perfect still, but ok.
 * 
 * @author tduva
 */
class MyIconView extends IconView {
    
    private static final int BOTTOM_MARGIN = 2;
    
    public MyIconView(Element elem) {
        super(elem);
    }
    
    @Override
    public float getAlignment(int axis) {
        if (axis == View.Y_AXIS && Debugging.isEnabled("oldiconplace")) {
            return 1f;
        }
        if (axis == View.Y_AXIS) {
            int fontHeight = MyStyleConstants.getFontHeight(getAttributes());
            float lineHeight = fontHeight * 0.7f;
            float actualHeight = super.getPreferredSpan(View.Y_AXIS);
            float fakedHeight = getPreferredSpan(View.Y_AXIS);
            float height = fakedHeight - (actualHeight - fakedHeight);
            float shouldMoveUp = (height - lineHeight) / 2.0f;
            float result = (fakedHeight - shouldMoveUp) / fakedHeight;
//            System.out.println(height + " " + diff + " " + lineHeight + " " + result);
            return result;
        }
        return super.getAlignment(axis);
    }
    
    @Override
    public float getPreferredSpan(int axis) {
        if (axis == View.Y_AXIS && Debugging.isEnabled("oldiconplace")) {
            float height = super.getPreferredSpan(axis);
            return height * 0.7f;
        }
        if (axis == View.Y_AXIS) {
//            int fontHeight = (int)getAttributes().getAttribute(Attribute.FONT_HEIGHT);
//            float height = super.getPreferredSpan(axis);
//            if (fontHeight >= height) {
//                return height;
//            }
//            float bottomSpacing = StyleConstants.getLineSpacing(getAttributes()) * fontHeight + StyleConstants.getSpaceBelow(getAttributes());
//            bottomSpacing = Math.max(bottomSpacing - 2, 0);
//            float result = Math.max(fontHeight / height, 0.8f);
//            float diff = height - result*height;
//            return height - Math.min(diff, bottomSpacing);
            
            int fontHeight = MyStyleConstants.getFontHeight(getAttributes());
            float height = super.getPreferredSpan(axis);
            if (fontHeight >= height) {
                return height;
            }
            float bottomSpacing = StyleConstants.getLineSpacing(getAttributes()) * fontHeight + StyleConstants.getSpaceBelow(getAttributes());
            return height - Math.min(height - fontHeight, Math.max(bottomSpacing - BOTTOM_MARGIN, 0));
        }
        return super.getPreferredSpan(axis);
    }
    
//    @Override
//    public void paint(Graphics g, Shape a) {
//        //System.out.println(a.getBounds()+" "+g.getClipBounds());
//        Rectangle r = new Rectangle(a.getBounds());
//        r.height = (int)super.getPreferredSpan(View.Y_AXIS);
//        super.paint(g, r);
//    }
    
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
