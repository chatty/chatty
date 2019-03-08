
package chatty.gui.components.textpane;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.AttributeSet;
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
    
    public static boolean hasAttributeKey(Element element, Object key) {
        return element.getAttributes().getAttribute(key) != null;
    }
    
    public static boolean hasAttributeKeyValue(Element element, Object key, Object value) {
        return element.getAttributes().containsAttribute(key, value);
    }
    
    /**
     * Get the offsets of the actual message text portion of a user message.
     * This should only be used on appropriate lines. Anything after the User
     * and before the appended info (if present) is counted as message text.
     * 
     * The message element should always start with a single space to separate
     * from the user element: [user][ actual message]
     * 
     * @param line The line element
     * @return An array containing the start and end offset, or an empty array
     * if something went wrong
     */
    public static int[] getMessageOffsets(Element line) {
        int count = line.getElementCount();
        int start = 0;
        int end = -1;
        for (int i=0;i<count;i++) {
            Element element = line.getElement(i);
            if (element.getAttributes().isDefined(ChannelTextPane.Attribute.USER)) {
                // After last User element
                start = i + 1;
            }
            if (element.getAttributes().isDefined(ChannelTextPane.Attribute.IS_APPENDED_INFO)) {
                // Stop before appended info
                end = i - 1;
                break;
            }
        }
        boolean toLastElement = false;
        if (end == -1 || end < start) {
            // Substract one from the count for the last index
            end = count - 1;
            toLastElement = true;
        }
        if (start < count) {
            int startOffset = line.getElement(start).getStartOffset();
            int endOffset = line.getElement(end).getEndOffset();
            if (toLastElement) {
                /**
                 * Adjust for linebreak character if the message reaches to the
                 * last element of the line (sometimes linebreak is in a
                 * separate element).
                 */
                endOffset--;
            }
//            System.out.println("'"+Util.getText(line.getDocument(), startOffset, endOffset)+"'");
            // +1 for separating space
            return new int[]{startOffset+1, endOffset};
        }
        return new int[0];
    }
    
}
