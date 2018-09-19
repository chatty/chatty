
package chatty.gui.components.textpane;

import chatty.Chatty;
import chatty.util.Debugging;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import static javax.swing.text.View.Y_AXIS;

/**
 * Changes the position of the icon slightly, so it overlaps with following text
 * as to not take as much space. Not perfect still, but ok.
 * 
 * @author tduva
 */
class MyIconView extends IconView {
    
    private static final int MARGIN = 2;
    
    public MyIconView(Element elem) {
        super(elem);
    }
    
    @Override
    public float getAlignment(int axis) {
        if (axis == View.Y_AXIS && Debugging.isEnabled("iconold1")) {
            return 1f;
        }
        if (axis == View.Y_AXIS) {
            int fontHeight = MyStyleConstants.getFontHeight(getAttributes());
            float lineHeight = fontHeight * 0.6f;
            float actualHeight = super.getPreferredSpan(View.Y_AXIS);
            float fakedHeight = getPreferredSpan(View.Y_AXIS);
            float height = fakedHeight - (actualHeight - fakedHeight);
            float moveDownBy = (height - lineHeight) / 2.0f;
            moveDownBy += moveUpBy();
            float result = (fakedHeight - moveDownBy) / fakedHeight;
//            System.out.println(height + " " + moveDownBy + " " + lineHeight + " " + result);
            return result;
        }
        return super.getAlignment(axis);
    }
    
    @Override
    public float getPreferredSpan(int axis) {
        if (axis == View.Y_AXIS && Debugging.isEnabled("iconold1")) {
            float height = super.getPreferredSpan(axis);
            return height * 0.7f;
        }
        if (axis == View.Y_AXIS) {
            int fontHeight = MyStyleConstants.getFontHeight(getAttributes());
            float height = super.getPreferredSpan(axis);
            if (fontHeight >= height) {
                return height;
            }
            float spacing = StyleConstants.getLineSpacing(getAttributes()) * fontHeight + StyleConstants.getSpaceBelow(getAttributes());
            if (!Debugging.isEnabled("iconold2")) {
                spacing += Math.max(StyleConstants.getSpaceAbove(getAttributes()) - 1 - MARGIN, 0);
            }
            float toFontHeight = Math.max(height - fontHeight - MARGIN, 0);
            float availableSpace = Math.max(spacing - MARGIN, 0);
//            Chatty.println(toFontHeight+" "+availableSpace);
            return height - Math.min(toFontHeight, availableSpace);
        }
        return super.getPreferredSpan(axis);
    }
    
    private final Rectangle tempRect = new Rectangle();
    
    @Override
    public void paint(Graphics g, Shape s) {
        int moveUpBy = moveUpBy();
        if (moveUpBy > 0) {
            Rectangle r;
            if (s instanceof Rectangle) {
                r = (Rectangle) s;
            } else {
                r = s.getBounds();
            }
            tempRect.x = r.x;
            tempRect.y = r.y;
            tempRect.width = r.width;
            tempRect.height = r.height;
            tempRect.translate(0, -moveUpBy);
            s = tempRect;
        }
        super.paint(g, s);
    }
    
    private int moveUpBy() {
        if (!Debugging.isEnabled("iconold2")) {
            float height = super.getPreferredSpan(Y_AXIS);
            float fakedHeight = getPreferredSpan(Y_AXIS);
            int moveUp = ((int) (height - fakedHeight) / 2);
            if (moveUp > 0) {
                //return moveUp - 1;
                return moveUp;
            }
        }
        return 0;
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
