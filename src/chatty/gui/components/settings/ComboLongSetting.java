
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
    
}
