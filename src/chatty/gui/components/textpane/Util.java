
package chatty.gui.components.textpane;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 *
 * @author tduva
 */
public class Util {
    
    public static Set<Long> getImageIds(Document doc, int startLine, int endLine) {
        Set<Long> ids = new HashSet<>();
        for (int line=startLine;
                line <= endLine && line < doc.getDefaultRootElement().getElementCount();
                line++) {
            Element e = doc.getDefaultRootElement().getElement(line);
            getImageIds(e, ids);
        }
        return ids;
    }
    
    public static void getImageIds(Element element, Set<Long> ids) {
        Long id = (Long)element.getAttributes().getAttribute(ChannelTextPane.Attribute.IMAGE_ID);
        if (id != null) {
            ids.add(id);
        }
        for (int i=0; i<element.getElementCount(); i++) {
            getImageIds(element.getElement(i), ids);
        }
    }
    
    public static String debugContents(Element element) {
        StringBuilder b = new StringBuilder();
        debugContents(element, b);
        return b.toString();
    }
    
    /**
     * Output the text of the subelements of the given element.
     *
     * @param element
     */
    public static void debugContents(Element element, StringBuilder b) {
        Document doc = element.getDocument();
        b.append("[");
        if (element.isLeaf()) {
            try {
                String text = doc.getText(
                        element.getStartOffset(),
                        element.getEndOffset() - element.getStartOffset());
                b.append("'").append(text).append("'");
            } catch (BadLocationException ex) {
                System.out.println("Bad location");
            }
        } else {
        for (int i = 0; i < element.getElementCount(); i++) {
                Element child = element.getElement(i);
                debugContents(child, b);
            }
        }
        b.append("]");
    }
    
    public static String getText(Document doc, int start, int end) {
        try {
            return doc.getText(start, end - start);
        } catch (BadLocationException ex) {
            return "-";
        }
    }
    
}
