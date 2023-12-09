
package chatty.gui.components.settings;

import chatty.gui.RegexDocumentFilter;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;

/**
 *
 * @author tduva
 */
public class DurationSetting extends JTextField implements LongSetting {
    
    public DurationSetting(int size, boolean editable) {
        super(size);
        setEditable(editable);
        ((AbstractDocument)getDocument()).setDocumentFilter(new RegexDocumentFilter("[^\\dms]+", this));
    }
    
    @Override
    public Long getSettingValue() {
        return getSettingValue(null);
    }

    @Override
    public Long getSettingValue(Long def) {
        String value = getText();
        Long number = null;
        try {
            number = Long.parseLong(value.replaceAll("[^0-9]*", ""));
        } catch (NumberFormatException ex) {
            return def;
        }
        if (value.endsWith("m")) {
            number *= 60;
        }
        return number;
    }

    @Override
    public void setSettingValue(Long setting) {
        if (setting % 60 != 0 || setting == 0) {
            setText(setting+"s");
        } else {
            setText(setting / 60+"m");
        }
    }

}
