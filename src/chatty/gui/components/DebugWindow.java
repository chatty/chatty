
package chatty.gui.components;

import chatty.gui.components.textpane.ChannelTextPane;
import chatty.util.DateTime;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 *
 * @author tduva
 */
public class DebugWindow extends JFrame {
    
    private static final int MAX_NUMBER_LINES = 250;
    
    private final JCheckBox autoscroll = new JCheckBox("Autoscroll", true);
    private final JCheckBox logIrc = new JCheckBox("Irc log", false);
    private final JTextArea text;
    private final JTextArea textIrcLog;
    
    public DebugWindow(ItemListener listener) {
        setTitle("Debug");
        
        // Caret to prevent scrolling
        DefaultCaret caret = new DefaultCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        
        // Normal log
        text = new JTextArea();
        text.setCaret(caret);
        text.setEditable(false);
        JScrollPane scroll = new JScrollPane(text);
        
        // Caret to prevent scrolling
        caret = new DefaultCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        
        // Irc log
        textIrcLog = new JTextArea();
        textIrcLog.setEditable(false);
        textIrcLog.setCaret(caret);
        JScrollPane scrollIrcLog = new JScrollPane(textIrcLog);
        
        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Log", scroll);
        tabs.addTab("Irc log", scrollIrcLog);
        
        // Settings (Checkboxes)
        logIrc.setToolTipText("Logging IRC traffic can reduce performance");
        JPanel settingsPanel = new JPanel();
        settingsPanel.add(autoscroll);
        settingsPanel.add(logIrc);
        
        // Add everything
        add(tabs, BorderLayout.CENTER);
        add(settingsPanel, BorderLayout.SOUTH);

        pack();
        
        logIrc.addItemListener(listener);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(new Dimension(600,500));
    }
    
    public void printLine(String line) {
        printLine(text, line);
    }
    
    public void printLineIrc(String line) {
        printLine(textIrcLog, line);
    }
    
    private void printLine(JTextArea text, String line) {
        try {
            Document doc = text.getDocument();
            doc.insertString(doc.getLength(), "["+DateTime.currentTime()+"] "+line+"\n", null);
            if (autoscroll.isSelected()) {
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
            Logger.getLogger(ChannelTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
   }

    public JCheckBox getLogIrcCheckBox() {
        return logIrc;
    }
    
}
