
package chatty.gui.components.textpane;

import javax.swing.text.Element;
import javax.swing.text.FlowView;
import javax.swing.text.ParagraphView;
import javax.swing.text.View;

/**
 * Changes the FlowStrategy to increase performance when i18n is enabled in the
 * Document. Not quite sure why this works.. ;) (This may work because by
 * default the strategy is a singleton shared by all instances, which may reduce
 * performance if all instances have to use a i18n stragety when one character
 * that requires it is inserted.)
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
}
