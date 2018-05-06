
package chatty.gui.components.textpane;

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
//    private int layouts = 0;
//    
//    @Override
//    protected void layout(int width, int height) {
//        long start=System.currentTimeMillis();
//        super.layout(width, height);
//        long duration =System.currentTimeMillis() - start;
//        if (duration > 1) {
//            layouts++;
//            System.out.println(duration+" "+layouts);
//        }
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
            //System.out.println(offset);
            for (int i = 0; i < offsets.length; i++) {
                offsets[i] += offset;
            }
        }
    }
}   