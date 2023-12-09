
package chatty.gui.components.settings;

import chatty.gui.RegexDocumentFilter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

/**
 * A JTextField that only accepts numbers.
 * 
 * @author tduva
 */
public class LongTextField extends JTextField {
    
    private String previousValue = "";
    
    public LongTextField(int size, boolean editable) {
        super(size);
        
        setEditable(editable);
        setInputVerifier(new IntegerVerifier());
        ((AbstractDocument)getDocument()).setDocumentFilter(new RegexDocumentFilter("\\D+", this));
        getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                inputChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                inputChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                inputChanged();
            }
        });
        this.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    setText(previousValue);
                }
            }
        });
    }
    
    private void inputChanged() {
        if (!getText().isEmpty()) {
            previousValue = getText();
        }
    }
    
    class IntegerVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent input) {
            JTextField component = (JTextField)input;
            try {
                Long.parseLong(component.getText());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        
    }
    
}
