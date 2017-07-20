
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.Highlighter.HighlightItem;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Dialog to check if the entered pattern matches the test text. This can be
 * used for developing patterns for the Highlight and Ignore lists.
 * 
 * @author tduva
 */
public class HighlighterTester extends JDialog {
    
    private final static String INFO = "<html><body style='width: 200px'>"
            + "Test if the Item pattern matches the "
            + "Test text. Disregards non-text related prefixes.";
    
    private final JTextField itemValue = new JTextField(30);
    private final JTextField testInput = new JTextField(30);
    private final JLabel testResult = new JLabel("Abc");
    
    private final String initialValue;
    
    private HighlightItem item;
    
    public HighlighterTester(Window owner, String value) {
        super(owner);
        
        setTitle("Test and Edit");
        
        this.initialValue = value;
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        add(new JLabel(INFO),
                GuiUtil.makeGbc(0, 0, 2, 1, GridBagConstraints.CENTER));
        
        add(new JLabel("Item:"),
                GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        
        gbc = GuiUtil.makeGbc(1, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        itemValue.setFont(Font.decode(Font.MONOSPACED));
        add(itemValue,
                gbc);
        
        add(new JLabel("Test:"),
                GuiUtil.makeGbc(0, 2, 1, 1));
        
        gbc = GuiUtil.makeGbc(1, 2, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        testInput.setFont(Font.decode(Font.MONOSPACED));
        add(testInput,
                gbc);
        
        add(testResult,
                GuiUtil.makeGbc(1, 3, 1, 1, GridBagConstraints.WEST));
        
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(owner);
        
        testInput.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
        
        itemValue.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFromItemValue();
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFromItemValue();
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFromItemValue();
                update();
            }
        });
        
        itemValue.setText(value);
        updateFromItemValue();
    }
    
    private void updateFromItemValue() {
        String value = itemValue.getText();
        if (value.isEmpty()) {
            item = null;
        } else {
            item = new HighlightItem(value);
        }
        update();
    }
    
    private void update() {
        if (item == null) {
            testResult.setText("No pattern.");
        } else if (item.matches(testInput.getText())) {
            if (item.hasError()) {
                testResult.setText("Matched due to invalid pattern.");
            } else {
                testResult.setText("Matched!");
            }
        } else {
            testResult.setText("No match.");
        }
    }
    
    public String test() {
        testInput.requestFocusInWindow();
        setVisible(true);
        if (!itemValue.getText().equals(initialValue)) {
            return itemValue.getText();
        }
        return null;
    }
    
}
