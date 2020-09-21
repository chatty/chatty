
package chatty.gui.components.textpane;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * Move the dot/mark (selection or not) when text is added/removed, so it stays
 * on the same content.
 * 
 * @author tduva
 */
public class FixSelection implements DocumentListener {

    private final JTextComponent c;
    
    public static void install(JTextComponent c) {
        c.getDocument().addDocumentListener(new FixSelection(c));
    }
    
    public FixSelection(JTextComponent c) {
        this.c = c;
    }
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        fix(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        fix(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }
    
    private void fix(DocumentEvent e) {
        int dot = c.getCaret().getDot();
        int mark = c.getCaret().getMark();
        int docLength = e.getDocument().getLength();
        
        int change = 0;
        if (e.getType() == DocumentEvent.EventType.INSERT) {
            change = e.getLength();
        }
        else if (e.getType() == DocumentEvent.EventType.REMOVE) {
            change = -e.getLength();
        }
        
        // Check separately, something may be changed within the selection
        if (dot > e.getOffset()) {
            dot = dot + change;
        }
        if (mark > e.getOffset()) {
            mark = mark + change;
        }
        
        mark = checkBounds(mark, docLength);
        dot = checkBounds(dot, docLength);
        
        // Set the mark first, so the dot (caret) can then be moved to create a
        // selection (and if they're the same it doesn't matter anyway)
        c.getCaret().setDot(mark);
        if (mark != dot) {
            c.getCaret().moveDot(dot);
        }
    }
    
    private int checkBounds(int pos, int length) {
        if (pos < 0) {
            return 0;
        }
        if (pos > length) {
            return length;
        }
        return pos;
    }
    
}
