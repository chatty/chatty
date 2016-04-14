
package chatty.gui.components.settings;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author tduva
 */
public class ComboStringSetting extends GenericComboSetting<String> implements StringSetting {

    public ComboStringSetting(String[] items) {
        super(items);
    }
    
    public ComboStringSetting(Collection<String> items) {
        super(items.toArray(new String[0]));
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
