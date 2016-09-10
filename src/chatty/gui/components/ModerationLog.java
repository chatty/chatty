
package chatty.gui.components;

import chatty.gui.MainGui;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.pubsub.ModeratorActionData;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 *
 * @author tduva
 */
public class ModerationLog extends JDialog {
    
    private static final int MAX_NUMBER_LINES = 1000;
    
    private final JTextArea log;
    private final JScrollPane scroll;
    
    public ModerationLog(MainGui owner) {
        super(owner);
        log = createLogArea();
        log.setSize(300, 200);
        setTitle("Moderator Actions");
        
        scroll = new JScrollPane(log);
        add(scroll, BorderLayout.CENTER);
        pack();
    }

    private static JTextArea createLogArea() {
        // Caret to prevent scrolling
        DefaultCaret caret = new DefaultCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JTextArea text = new JTextArea();
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setCaret(caret);
        return text;
    }
    
    public void add(ModeratorActionData data) {
        if (data.stream != null) {
            setTitle("Moderator Actions ("+data.stream+")");
        }
        String line = String.format("%s: /%s %s",
                data.created_by,
                data.moderation_action,
                StringUtil.join(data.args," "));
        printLine(log, line);
    }
    
    private void printLine(JTextArea text, String line) {
        try {
            Document doc = text.getDocument();
            String linebreak = doc.getLength() > 0 ? "\n" : "";
            doc.insertString(doc.getLength(), linebreak+"["+DateTime.currentTime()+"] "+line, null);
            JScrollBar bar = scroll.getVerticalScrollBar();
            boolean scrollDown = bar.getValue() == bar.getMaximum() - bar.getVisibleAmount();
            if (scrollDown) {
                text.setCaretPosition(doc.getLength());
            }
            clearSomeChat(doc);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Removes some lines from the given Document so it won't exceed the maximum
     * number of lines.
     * 
     * @param doc 
     */
    public void clearSomeChat(Document doc) {
        int count = doc.getDefaultRootElement().getElementCount();
        if (count > MAX_NUMBER_LINES) {
            removeFirstLines(doc, 10);
        }
    }
    
    /**
     * Removes the given number of lines from the given Document.
     * 
     * @param doc
     * @param amount 
     */
    public void removeFirstLines(Document doc, int amount) {
        if (amount < 1) {
            amount = 1;
        }
        Element firstToRemove = doc.getDefaultRootElement().getElement(0);
        Element lastToRemove = doc.getDefaultRootElement().getElement(amount - 1);
        int startOffset = firstToRemove.getStartOffset();
        int endOffset = lastToRemove.getEndOffset();
        try {
            doc.remove(startOffset,endOffset);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
   }
    
    public void showDialog() {
        setVisible(true);
    }
}
