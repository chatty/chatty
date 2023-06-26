
package chatty.gui.components.textpane;

import chatty.util.colors.HtmlColors;
import chatty.gui.components.textpane.ChannelTextPane.Attribute;
import chatty.util.Debugging;
import chatty.util.colors.ColorCorrection;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.StyleConstants;
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
    
    @Override
    public float getPreferredSpan(int axis) {
        boolean highlightMatchesEnabled = MyStyleConstants.getHighlightMatchesEnabled(getAttributes());
        if (axis == View.X_AXIS
                && highlightMatchesEnabled
                && getAttributes().containsAttribute(Attribute.HIGHLIGHT_WORD, true)) {
            // Make a bit wider for marking the highlighted word
            return super.getPreferredSpan(axis)+3;
        }
        return super.getPreferredSpan(axis);
    }
    
    long lastPaint = 0;
    long shortPaintCount = 0;
    
    @Override
    public void paint(Graphics g, Shape a) {
        Rectangle r = a instanceof Rectangle ? (Rectangle)a : a.getBounds();
        
        // Testing
        if (Debugging.isEnabled("gifd")) {
            long ms = Debugging.millisecondsElapsed("WrapLabelView.print");
            Debugging.println(String.format("%d %d-%d %s",
                    ms,
                    getStartOffset(), getEndOffset(),
                    Util.getText(getDocument(), getStartOffset(), getEndOffset())
            ));
        }
        if (Debugging.isEnabled("gifdd")) {
            long passed = System.currentTimeMillis() - lastPaint;
            lastPaint = System.currentTimeMillis();
            if (passed < 300) {
                shortPaintCount++;
            } else {
                shortPaintCount = 0;
            }
            if (shortPaintCount > 10) {
                g.setColor(Color.blue);
                g.drawRect(r.x, r.y, r.width, r.height);
                shortPaintCount = 10;
            }
        }
        
        boolean isHoveredUser = ChannelTextPane.hoveredUser != null &&
                (getAttributes().containsAttribute(Attribute.USER, ChannelTextPane.hoveredUser)
                || getAttributes().containsAttribute(Attribute.MENTION, ChannelTextPane.hoveredUser));
        
        boolean highlightMatchesEnabled = MyStyleConstants.getHighlightMatchesEnabled(getAttributes());
        boolean isHl = highlightMatchesEnabled && getAttributes().containsAttribute(Attribute.HIGHLIGHT_WORD, true);
        if (isHl || isHoveredUser) {
            Color c = StyleConstants.getForeground(getAttributes());
            Color c2;
            Color c3;
            
            boolean darkText = ColorCorrection.isDarkColor(c);
            if (darkText) {
                c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 200);
                c3 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 60);
            } else {
                c3 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 100);
                c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 160);
            }
            
            g.setColor(isHoveredUser ? c3 : c2);
            
            // Bottom
            g.drawLine(r.x+1, r.y+r.height, r.x+r.width, r.y+r.height);
            // Right
            g.drawLine(r.x+r.width, r.y+1, r.x+r.width, r.y+r.height-1);

            g.setColor(isHoveredUser ? c2 : c3);
            
            // Top
            g.drawLine(r.x+1, r.y, r.x+r.width, r.y);
            // Left
            g.drawLine(r.x, r.y, r.x, r.y+r.height);
            
            // Move text a bit to the right (there should probably be a better
            // way of doing this, this is a bit of a hack, not sure if it breaks
            // anything
            if (isHl) {
                r.translate(2, 0);
            }
        } else {
            // Mostly used for namecolor background
            Color bgColor = MyStyleConstants.getLabelBackground(getAttributes());
            if (bgColor != null) {
                g.setColor(bgColor);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
        }
        if (getAttributes().getAttribute(Attribute.HYPE_CHAT) != null) {
            g.setColor(StyleConstants.getForeground(getAttributes()));
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, r.height / 2, r.height / 2);
        }
        if (Debugging.isEnabled("labeloutlines")) {
            g.setColor(Color.red);
            g.drawRect(r.x, r.y, r.width, r.height);
        }
        super.paint(g, r);
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

