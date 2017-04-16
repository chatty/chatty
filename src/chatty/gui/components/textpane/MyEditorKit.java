
package chatty.gui.components.textpane;

import javax.swing.text.AbstractDocument;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Replaces some Views by custom ones to change display behaviour.
 * 
 * @author tduva
 */
class MyEditorKit extends StyledEditorKit {

    private final ViewFactory factory;
    
    public MyEditorKit(boolean startAtBottom) {
        this.factory = new StyledViewFactory(startAtBottom);
    }
    
    @Override
    public ViewFactory getViewFactory() {
        return factory;
    }
 
    static class StyledViewFactory implements ViewFactory {

        private final boolean startAtBottom;
        
        StyledViewFactory(boolean startAtBottom) {
            this.startAtBottom = startAtBottom;
        }

        @Override
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new WrapLabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new MyParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new ChatBoxView(elem, View.Y_AXIS, startAtBottom);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new MyIconView(elem);
                }
            }
            return new LabelView(elem);
        }

    }
}
