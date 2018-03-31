
package chatty.gui.components.textpane;

import java.awt.Shape;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Element;
import javax.swing.text.FlowView;
import javax.swing.text.ParagraphView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Changes the FlowStrategy to increase performance when i18n is enabled in the
 * Document. Not quite sure why this works.. ;) (This may work because by
 * default the strategy is a singleton shared by all instances, which may reduce
 * performance if all instances have to use a i18n stragety when one character
 * that requires it is inserted.)
 * 
 * Contains parts of https://stackoverflow.com/a/14230668/2375667 to fix a line
 * breaking bug in JTextPane.
 *
 * @author tduva
 */
class MyParagraphView extends ParagraphView {

    public static int MAX_VIEW_SIZE = 50;

    public MyParagraphView(Element elem) {
        super(elem);
        strategy = new MyParagraphView.MyFlowStrategy();
    }

    public static class MyFlowStrategy extends FlowStrategy {

        @Override
        protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
            View res = super.createView(fv, startOffset, spanLeft, rowIndex);

            if (res.getEndOffset() - res.getStartOffset() > MAX_VIEW_SIZE) {
                //res = res.createFragment(startOffset, startOffset + MAX_VIEW_SIZE);
            }
            return res;
        }
    }
    
    @Override
    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.removeUpdate(e, a, f);
        resetBreakSpots();
    }
    
    @Override
    public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.insertUpdate(e, a, f);
        resetBreakSpots();
    }

    private void resetBreakSpots() {
        for (int i = 0; i < layoutPool.getViewCount(); i++) {
            View v = layoutPool.getView(i);
            if (v instanceof WrapLabelView) {
                ((WrapLabelView) v).resetBreakSpots();
            }
        }
    }
    
}
