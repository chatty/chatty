
package chatty.gui.components;

import chatty.gui.MainGui;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.pubsub.ModeratorActionData;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    private final Map<String, List<String>> cache = new HashMap<>();
    
    private String currentChannel;

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
    
    public void setChannel(String channel) {
        if (channel != null && !channel.equals(currentChannel)) {
            currentChannel = channel;
            setTitle("Moderation Actions ("+channel+")");
            
            List<String> cached = cache.get(channel);
            if (cached != null) {
                StringBuilder b = new StringBuilder();
                for (String line : cached) {
                    b.append(line);
                    b.append("\n");
                }
                log.setText(b.toString());
                scrollDown();
            } else {
                log.setText(null);
            }
        }
    }
    
    public void add(ModeratorActionData data) {
        if (data.stream == null) {
            return;
        }
        String channel = data.stream;
        
        String line = String.format("[%s] %s: /%s %s",
                DateTime.currentTime(),
                data.created_by,
                data.moderation_action,
                StringUtil.join(data.args," "));
        
        if (channel.equals(currentChannel)) {
            printLine(log, line);
        }
        
        if (!cache.containsKey(channel)) {
            cache.put(channel, new ArrayList<String>());
        }
        cache.get(channel).add(line);
    }
    
    private void printLine(JTextArea text, String line) {
        try {
            Document doc = text.getDocument();
            String linebreak = "\n";
            doc.insertString(doc.getLength(), line+linebreak, null);
            JScrollBar bar = scroll.getVerticalScrollBar();
            boolean scrollDown = bar.getValue() == bar.getMaximum() - bar.getVisibleAmount();
            if (scrollDown) {
                scrollDown();
            }
            clearSomeChat(doc);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void scrollDown() {
        log.setCaretPosition(log.getDocument().getLength());
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
    private void removeFirstLines(Document doc, int amount) {
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
