
package chatty.gui.components.textpane;

import chatty.util.Debugging;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.text.BoxView;
import javax.swing.text.Element;

/**
 * Starts adding text at the bottom instead of at the top.
 * 
 * @author tduva
 */
class ChatBoxView extends BoxView {
    
    private final boolean enabled;
    
    public ChatBoxView(Element elem, int axis, boolean enabled) {
        super(elem,axis);
        this.enabled = enabled;
    }
    
    // For testing
    private int layouts = 0;
    
    @Override
    protected void layout(int width, int height) {
        if (Debugging.isEnabled("layout")) {
            long start = System.currentTimeMillis();
            super.layout(width, height);
            long duration = System.currentTimeMillis() - start;
            if (duration > 1) {
                layouts++;
                Debugging.println("layout "+duration + " " + layouts);
            }
        } else {
            super.layout(width, height);
        }
    }

//    @Override
//    public void paint(Graphics g, Shape a) {
//        if (g.getClipBounds().width == 35) {
//            //System.out.println(g.getClip()+" "+a);
//        }
////        Rectangle c = g.getClipBounds();
////        Rectangle r = a.getBounds();
////        if (r.contains(c)) {
////            g.setColor(Color.gray);
////            //g.fillRect(c.x, c.y, c.width, c.height);
////        }
//        super.paint(g, a);
//    }
    
    @Override
    protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
        super.layoutMajorAxis(targetSpan,axis,offsets,spans);
        if (enabled) {
            int textBlockHeight = 0;
            int offset = 0;

            for (int i = 0; i < spans.length; i++) {
                textBlockHeight += spans[i];
            }
            offset = (targetSpan - textBlockHeight);
            if (offset > 0) {
                for (int i = 0; i < offsets.length; i++) {
                    offsets[i] += offset;
                }
            }
        }
    }
}
