
package chatty.gui.components.settings;

/**
 *
 * @author tduva
 */
public interface LongSetting {
    public Long getSettingValue();
    public Long getSettingValue(Long def);
    public void setSettingValue(Long setting);
}
