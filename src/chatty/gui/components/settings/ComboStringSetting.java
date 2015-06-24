
package chatty.gui.components.settings;

import java.util.Map;

/**
 *
 * @author tduva
 */
public class ComboStringSetting extends GenericComboSetting<String> implements StringSetting {

    public ComboStringSetting(String[] items) {
        super(items);
    }
    
    public ComboStringSetting(Map<String, String> items) {
        super(items);
    }
    
    @Override
    public String getSettingValue() {
        return super.getSettingValue();
    }

    @Override
    public void setSettingValue(String value) {
        super.setSettingValue(value);
    }
    
}
