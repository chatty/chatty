
package chatty.gui.components;

import chatty.gui.laf.LaF;
import chatty.gui.components.completion.AutoCompletion;
import chatty.gui.components.completion.AutoCompletionServer;
import chatty.util.colors.ColorCorrectionNew;
import java.awt.Color;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;

/**
 * Input box that supports auto-completion, input history and automatically
 * grows to several lines for long text.
 * 
 * @author tduva
 */
public class ChannelEditBox extends JTextArea implements KeyListener,
        DocumentListener {
    
    // History
    private final List<String> history = new ArrayList<>();
    private int historyPosition = 0;
    private boolean historyTextEdited = false;
    
    private boolean historyRequireCtrlMultirow = false;
    
    private boolean completionEnabled = true;
    
    private final Set<ActionListener> listeners = new HashSet<>();
    
    // Auto completion
    private final AutoCompletion autoCompletion;
    
    public ChannelEditBox() {
        autoCompletion = new AutoCompletion(this);
        autoCompletion.setFont(getFont());
        this.addKeyListener(this);
        setLineWrap(true);
        setWrapStyleWord(true);
        getDocument().putProperty("filterNewlines", true);
        this.setFocusTraversalKeysEnabled(false);
        getDocument().addDocumentListener(this);
        
        // Prevent automatic selection of text when tabbing back into the window
        addFocusListener(new FocusListener() {

            private boolean selection;
            
            @Override
            public void focusGained(FocusEvent e) {
                if (!selection) {
                    setCaretPosition(getCaretPosition());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                selection = getSelectedText() != null;
            }
        });
    }
    
    /**
     * Need to update for setting LaF, since this text area should always use
     * a text field type border.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        if (LaF.getInputBorder() != null) {
            setBorder(LaF.getInputBorder());
        }
        else {
            setBorder(UIManager.getBorder("TextField.border"));
        }
    }
    
    @Override
    public Point getLocationOnScreen() {
        synchronized (getTreeLock()) {
            if (isShowing()) {
                return super.getLocationOnScreen();
            }
            // Workaround that probably breaks ALL the things, since I can't
            // figure out what actually causes this error
            return new Point(0,0);
        }
    }
    
    public void setCompletionEnabled(boolean enabled) {
        this.completionEnabled = enabled;
        autoCompletion.setEnabled(enabled);
    }
    
    public void setCompletionMaxItemsShown(int max) {
        autoCompletion.setMaxResultsShown(max);
    }
    
    public void setCompletionShowPopup(boolean show) {
        autoCompletion.setShowPopup(show);
    }
    
    public void setCompleteToCommonPrefix(boolean value) {
        autoCompletion.setCompleteToCommonPrefix(value);
    }
    
    public boolean getCompleteToCommonPrefix() {
        return autoCompletion.getCompleteToCommonPrefix();
    }
    
    public void setCompletionCellSize(int width, int height) {
        autoCompletion.setCellSize(width, height);
    }
    
    public void setCompletionAppendSpace(boolean append) {
        autoCompletion.setAppendSpace(append);
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (autoCompletion != null) {
            autoCompletion.setFont(font);
        }
    }
    
    @Override
    public void setForeground(Color color) {
        super.setForeground(color);
        if (autoCompletion != null) {
            autoCompletion.setForegroundColor(color);
        }
    }
    
    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        if (autoCompletion != null) {
            autoCompletion.setBackgroundColor(ColorCorrectionNew.offset(color, 0.9f));
            autoCompletion.setHighlightColor(ColorCorrectionNew.offset(color, 0.8f));
        }
    }
    
    public void setHistoryRequireCtrlMultirow(boolean require) {
        this.historyRequireCtrlMultirow = require;
    }
    
    public void addActionListener(ActionListener actionListener) {
        if (actionListener != null) {
            listeners.add(actionListener);
        }
    }
    
    /**
     * Inserts the given text at the current caret position, adding a space in
     * front and after the inserted text, if {@code withSpace} is true and if
     * there is actually other text in front/after respectively.
     * 
     * Sets the caret position to after the inserted text (after the space, if
     * one was added).
     * 
     * @param text The text to insert
     * @param withSpace Whether to add spaces
     */
    public void insertAtCaret(String text, boolean withSpace) {
        int pos = getCaretPosition();
        String current = getText();
        String before = current.substring(0, pos);
        String after = current.substring(pos);
        
        // Add space before and after inserted text, if requested
        if (withSpace) {
            if (!before.isEmpty() && !before.endsWith(" ")) {
                text = " "+text;
            }
            if (!after.isEmpty() && !after.startsWith(" ")) {
                text = text+" ";
            }
        }
        setText(before+text+after);
        
        // Set caret to position after inserted text
        setCaretPosition(pos+text.length());
    }
    
    /**
     * Set to end of text if caret pos is greater than length of text.
     * 
     * @param pos 
     */
    @Override
    public void setCaretPosition(int pos) {
        if (pos > getText().length()) {
            pos = getText().length();
        }
        super.setCaretPosition(pos);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!historyRequireCtrlMultirow || e.isControlDown() || isSingleRow()) {
            if (e.getKeyCode() == KeyEvent.VK_UP
                    && (e.isControlDown() || isCaretInFirstRow())) {
                historyBack();
                e.consume();
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN
                    && (e.isControlDown() || isCaretInLastRow())) {
                historyForward();
                e.consume();
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            if (!completionEnabled || getText().isEmpty()) {
                if (e.getModifiers() == 0) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
                }
                else if (e.getModifiers() == KeyEvent.SHIFT_MASK) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
                }
            }
            e.consume();
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            submit();
            e.consume();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB && completionEnabled) {
            if (e.isControlDown() || e.isAltDown() || e.isAltGraphDown()) {
                return;
            }
            if (e.isShiftDown()) {
                if (autoCompletion.inCompletion()) {
                    autoCompletion.manual(-1, null);
                } else {
                    autoCompletion.manual(1, "special");
                }
            } else {
                autoCompletion.manual(1, null);
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            autoCompletion.cancel();
        }
    }
    
    private boolean isCaretInFirstRow() {
        return getRow(getCaretPosition()) == 0;
    }
    
    private boolean isCaretInLastRow() {
        return getRow(getCaretPosition()) == getRow(getText().length());
    }
    
    private boolean isSingleRow() {
        return getRow(getText().length()) == 0;
    }
    
    private int getRow(int pos) {
        int row = (pos == 0) ? 0 : -1;
        try {
            int offset = pos;
            while (offset > 0) {
                offset = Utilities.getRowStart(this, offset) - 1;
                row++;
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return row;
    }
    
    //###############
    //### History ###
    //###############
    
    /**
     * Move back in the history, changing the text.
     */
    private void historyBack() { 
        if (historyPosition > 0 && historyPosition <= history.size()) {
            historyPosition--;
            String text = history.get(historyPosition);
            if (text != null) {
                setText(text);
                historyTextEdited = false;
            }
        }
    }
    
    private void historyForward() {
        
        historyAddChanged();
        
        historyPosition++;
        if (historyPosition >= history.size()) {
            // If further than the latest history entry, clear input
            setText("");
            historyTextEdited = false;
            historyPosition = history.size();
        }
        else if (historyPosition >= 0) {
            // If still in the history range set to next history position
            String text = history.get(historyPosition);
            if (text != null) {
                setText(text);
                historyTextEdited = false;
            }
        }
    }
    
    /**
     * Adds the current text to the history, if it was changed.
     */
    private void historyAddChanged() {
        if (historyTextEdited) {
            historyAdd(getText());
            historyTextEdited = false;
            historyPosition = history.size();
        }
    }
    
    /**
     * Adds the given text to the history. Mainly used when text is send. The
     * text is removed first, so it only occurs once. Only text that is not
     * empty is added.
     * 
     * @param text 
     */
    private void historyAdd(String text) {
        if (!text.isEmpty()) {
            history.remove(text);
            history.add(text);
        }
    }
    
    private void submit() {
        for (ActionListener l : listeners) {
            l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_FIRST, getText()));
        }
        historyAdd(getText());
        historyPosition = history.size();
        setText("");
        historyTextEdited = false;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        historyTextEdited = true;
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        historyTextEdited = true;
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }
    
    public void setCompletionServer(AutoCompletionServer server) {
        autoCompletion.setCompletionServer(server);
    }
    
    public void cleanUp() {
        autoCompletion.cleanUp();
    }

    public static void main(String[] args) {
        List<String> l = new ArrayList<>();
        
        l.add("joshimuz");
        l.add("joshua");
        l.add("josh");
        l.add("jo");
        //System.out.println(findStartCommonToAll(l));
    }
}
