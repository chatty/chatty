
package chatty.gui.components.settings;

import javax.swing.JTextField;

/**
 * Simple StringSetting that just consists of a JTextField.
 * 
 * @author tduva
 */
public class SimpleStringSetting extends JTextField implements StringSetting {
    
    private final DataFormatter<String> formatter;
    
    public SimpleStringSetting(int size, boolean editable) {
        this(size, editable, null);
    }
    
    public SimpleStringSetting(int size, boolean editable, DataFormatter<String> formatter) {
        super(size);
        setEditable(editable);
        this.formatter = formatter;
    }
    
    @Override
    public String getSettingValue() {
        String value = getText();
        if (formatter != null) {
            value = formatter.format(value);
        }
        return value;
    }

    @Override
    public void setSettingValue(String value) {
        if (formatter != null) {
            value = formatter.format(value);
        }
        setText(value);
    }
    
}
