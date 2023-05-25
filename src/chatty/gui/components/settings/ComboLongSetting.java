
package chatty.gui.components.settings;

import java.util.Map;

/**
 *
 * @author tduva
 */
public class ComboLongSetting extends GenericComboSetting<Long> implements LongSetting {

    public ComboLongSetting(Map<Long, String> items) {
        super(items);
    }
    
    @Override
    public Long getSettingValue(Long def) {
        // When combobox is set as editable the input may not be a number
        try {
            return super.getSettingValue(def);
        }
        catch (ClassCastException ex) {
            try {
                Object v = ((Entry) getSelectedItem()).value;
                if (v instanceof String) {
                    return Long.valueOf((String) v);
                }
            }
            catch (NumberFormatException ex2) {
            }
        }
        return 0L;
    }
    
}
