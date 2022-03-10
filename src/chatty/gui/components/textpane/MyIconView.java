
package chatty.gui.components.textpane;

import chatty.gui.components.textpane.ChannelTextPane.Attribute;
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
            if (Debugging.isEnabled("icond")) {
                Debugging.println(String.format("Height: %f Faked: %f Font: %d Movedown: %f",
                        actualHeight, fakedHeight, fontHeight, moveDownBy));
            }
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
            
            float spacing;
            if (getAttributes().containsAttribute(Attribute.ANIMATED, true)) {
                spacing = StyleConstants.getLineSpacing(getAttributes()) * fontHeight;
            } else {
                spacing = StyleConstants.getLineSpacing(getAttributes()) * fontHeight + StyleConstants.getSpaceBelow(getAttributes());
                if (!Debugging.isEnabled("iconold2")) {
                    spacing += Math.max(StyleConstants.getSpaceAbove(getAttributes()) - 1 - MARGIN, 0);
                }
            }
            float toFontHeight = Math.max(height - fontHeight - MARGIN, 0);
            float availableSpace = Math.max(spacing - MARGIN, 0);
            //Debugging.println(toFontHeight+" "+availableSpace);
            return height - Math.min(toFontHeight, availableSpace);
        }
        return super.getPreferredSpan(axis);
    }
    
    private static final int VISIBILITY_CHECK_COOLDOWN = 500;
    private final Rectangle tempRect = new Rectangle();
    private boolean shouldRepaint = true;
    private int movedUpBy;
    private long lastChecked;
    
    public void getRectangle(Rectangle r) {
        synchronized(tempRect) {
            r.setBounds(tempRect);
        }
    }
    
    /**
     * Whether to check this view's visibility. There is a cooldown for it as to
     * not use up too much resources on it, so even if this returns false the
     * view might still be assumed as visible.
     * 
     * @return 
     */
    public boolean shouldCheckVisibility() {
        if (getShouldRepaint()
                && System.currentTimeMillis() - lastChecked > VISIBILITY_CHECK_COOLDOWN) {
            lastChecked = System.currentTimeMillis();
            return true;
        }
        return false;
    }
    
    /**
     * Whether a repaint should be requested. If this is false, then it
     * hopefully means that the view isn't visible. Any painting sets this flag
     * to true.
     * 
     * @return 
     */
    public boolean getShouldRepaint() {
        synchronized(tempRect) {
            return shouldRepaint;
        }
    }
    
    /**
     * Set this flag to disable repainting. This doesn't prevent painting, but
     * the flag can be checked to decide whether a repaint should be requested
     * for this view. In fact, any painting enables this flag again, so it's
     * convenient to store this flag in this object.
     */
    public void setDontRepaint() {
        synchronized(tempRect) {
            shouldRepaint = false;
        }
    }
    
    /**
     * Get the height, minus the pixels the view was moved upwards, so it's
     * possible to calculate the real bottom y coordinate.
     * 
     * @return 
     */
    public int getAdjustedHeight() {
        synchronized(tempRect) {
            return tempRect.height - movedUpBy;
        }
    }
    
    @Override
    public void paint(Graphics g, Shape s) {
        int moveUpBy = moveUpBy();
        Rectangle r;
        if (s instanceof Rectangle) {
            r = (Rectangle) s;
        } else {
            r = s.getBounds();
        }
        synchronized (tempRect) {
            tempRect.x = r.x;
            tempRect.y = r.y;
            tempRect.width = (int) super.getPreferredSpan(X_AXIS);
            tempRect.height = (int) super.getPreferredSpan(Y_AXIS);
            movedUpBy = moveUpBy;
            if (moveUpBy > 0) {
                tempRect.translate(0, -moveUpBy);
            }
            shouldRepaint = true;
        }
        s = tempRect;
        super.paint(g, s);
    }
    
    private int moveUpBy() {
        if (!Debugging.isEnabled("iconold2")) {
            float height = super.getPreferredSpan(Y_AXIS);
            float fakedHeight = getPreferredSpan(Y_AXIS);
            int moveUp = ((int) (height - fakedHeight) / 2);
            int space = (int)StyleConstants.getSpaceAbove(getAttributes());
            moveUp = Math.min(moveUp, space);
            if (moveUp > 0) {
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
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }
    
}
