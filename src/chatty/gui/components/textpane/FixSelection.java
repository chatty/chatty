
package chatty.gui.components.textpane;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
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
        int start = c.getSelectionStart();
        int end = c.getSelectionEnd();
        if (start != end && start > e.getOffset()) {
            if (e.getType() == DocumentEvent.EventType.INSERT) {
                c.setSelectionStart(start + e.getLength());
                c.setSelectionEnd(end + e.getLength());
            } else if (e.getType() == DocumentEvent.EventType.REMOVE) {
                c.setSelectionStart(start - e.getLength());
                c.setSelectionEnd(end - e.getLength());
            }
        }
    }
    
}
