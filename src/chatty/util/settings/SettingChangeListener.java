
package chatty.util.settings;

/**
 * Called when a setting was changed.
 * 
 * @author tduva
 */
public interface SettingChangeListener {
    
    public void settingChanged(String setting, int type, Object value);
}
