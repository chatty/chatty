
package chatty;

import chatty.util.StringUtil;
import chatty.util.settings.SettingChangeListener;
import chatty.util.settings.Settings;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class CustomNames {
    
    private final static String SETTING_NAME = "customNames";
    
    private final Set<CustomNamesListener> listeners = new HashSet<>();
    
    private final Settings settings;
    
    public CustomNames(Settings settings) {
        this.settings = settings;
        settings.addSettingChangeListener(new SettingChangeListener() {

            @Override
            public void settingChanged(String setting, int type, Object value) {
                if (setting.equals(SETTING_NAME)) {
                    informListenersAllChanged();
                }
            }
        });
    }
    
    public void setCustomName(String nick, String customNick) {
        if (nick == null) {
            return;
        }
        nick = StringUtil.toLowerCase(nick);
        if (customNick == null) {
            settings.mapRemove(SETTING_NAME, nick);
        } else {
            settings.mapPut(SETTING_NAME, nick, customNick);
        }
        informListeners(nick, customNick);
    }
    
    public String getCustomName(String nick) {
        nick = StringUtil.toLowerCase(nick);
        return (String)settings.mapGet(SETTING_NAME, nick);
    }
    
    public synchronized String commandSetCustomName(String parameter) {
        if (parameter != null) {
            String[] split = parameter.split(" ", 2);
            if (split.length == 2) {
                String name = split[0];
                String customName = split[1];
                if (!Helper.isValidStream(name)) {
                    return "Invalid name.";
                } else {
                    setCustomName(name, customName);
                    return "Set custom name for '"+name+"' to '"+customName+"'";
                }
            }
        }
        return "Usage: /setname <name> <custom_name>";
    }
    
    public synchronized String commandResetCustomname(String parameter) {
        if (parameter != null) {
            String[] split = parameter.split(" ");
            if (split.length == 1) {
                String name = split[0];
                if (!Helper.isValidStream(name)) {
                    return "Invalid name.";
                } else {
                    setCustomName(name, null);
                    return "Removed custom name for '"+name+"'";
                }
            }
        }
        return "Usage: /resetname <name>";
    }
    
    private void informListeners(String name, String capitalizedName) {
        for (CustomNamesListener listener : listeners) {
            listener.setName(name, capitalizedName);
        }
    }
    
    private void informListenersAllChanged() {
        Map<String, String> customNames = settings.getMap(SETTING_NAME);
        for (String username : customNames.keySet()) {
            informListeners(username, customNames.get(username));
        }
    }
    
    public synchronized void addListener(CustomNamesListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Listener that can be implemented by classes that want to be informed
     * about changes in capitalization for names.
     */
    public static interface CustomNamesListener {
        
        /**
         * When the capitalization for a name changes, either because it got one
         * at all, or it changed.
         * 
         * @param name All lowercase name
         * @param customName The custom name
         */
        public void setName(String name, String customName);
    }
    
    
    
}
