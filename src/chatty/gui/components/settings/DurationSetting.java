
package chatty.gui.components.settings;

import javax.swing.JTextField;

/**
 *
 * @author tduva
 */
public class DurationSetting extends JTextField implements LongSetting {
    
    public DurationSetting(int size, boolean editable) {
        super(size);
        setEditable(editable);
    }

    @Override
    public Long getSettingValue() {
        String value = getText();
        Long number = null;
        try {
            number = Long.parseLong(value.replaceAll("[^0-9]*", ""));
        } catch (NumberFormatException ex) {
        }
        if (value.endsWith("m")) {
            number *= 60;
        }
        return number;
    }

    @Override
    public void setSettingValue(Long setting) {
        if (setting % 60 != 0) {
            setText(setting+"s");
        } else {
            setText(setting / 60+"m");
        }
    }
    
}
