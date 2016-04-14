
package chatty.gui.components.settings;

import java.util.List;

/**
 *
 * @author tduva
 * @param <T>
 */
public interface ListSetting<T> {
    public List<T> getSettingValue();
    public void setSettingValue(List<T> value);
}
