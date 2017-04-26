
package chatty.gui.components;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A TextField that supports auto-completion and a history of entered text.
 * 
 * @author tduva
 */
public class ChannelEditBox extends JTextField implements KeyListener,
        ActionListener, DocumentListener {
    
    // History
    Vector<String> history = new Vector<>();
    int historyPosition = 0;
    boolean historyTextEdited = false;
    
    // Auto completion
    private final AutoCompletion autoCompletion;
    
    public ChannelEditBox(int size) {
        super(size);
        autoCompletion = new AutoCompletion(this);
        this.addKeyListener(this);
        this.addActionListener(this);
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
    
    public void setCompletionMaxItemsShown(int max) {
        autoCompletion.setMaxResultsShown(max);
    }
    
    public void setCompletionShowPopup(boolean show) {
        autoCompletion.setShowPopup(show);
    }
    
    public void setCompleteToCommonPrefix(boolean value) {
        autoCompletion.setCompleteToCommonPrefix(value);
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
    

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            historyBack();
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            historyForward();
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            if (e.isControlDown() || e.isAltDown() || e.isAltGraphDown()) {
                return;
            }
            if (e.isShiftDown()) {
                if (autoCompletion.inCompletion()) {
                    autoCompletion.doAutoCompletion(null, false);
                } else {
                    autoCompletion.doAutoCompletion("special", true);
                }
            } else {
                autoCompletion.doAutoCompletion(null, true);
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            autoCompletion.cancelAutoCompletion();
        }
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
    
    /**
     * Adds sent text to the history, sets history position and clears the
     * input field.
     * 
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        historyAdd(getText());
        historyPosition = history.size();
        setText("");
        historyTextEdited = false;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        historyTextEdited = true;
        //hideCompletionInfoWindow();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        historyTextEdited = true;
        //hideCompletionInfoWindow();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        //hideCompletionInfoWindow();
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
