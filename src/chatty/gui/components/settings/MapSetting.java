
package chatty.gui.components.settings;

import java.util.Map;

/**
 *
 * @author tduva
 */
public interface MapSetting<K, V> {

        public Map<K, V> getSettingValue();

        public void setSettingValue(Map<K,V> value);
}
