
package chatty.gui.components.settings;

/**
 * Default numeric setting consisting of a simple text field that only accepts
 * Long values.
 * 
 * @author tduva
 */
public class SimpleLongSetting extends LongTextField implements LongSetting {

    public SimpleLongSetting(int size, boolean editable) {
        super(size, editable);
    }
    
    @Override
    public Long getSettingValue() {
        return getSettingValue(null);
    }
    
    @Override
    public Long getSettingValue(Long def) {
        try {
            return Long.parseLong(getText());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    @Override
    public void setSettingValue(Long setting) {
        setText(setting.toString());
    }
    
}
