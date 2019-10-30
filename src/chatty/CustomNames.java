
package chatty;

import chatty.util.Debugging;
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
    
    /**
     * The current original names. Used to detect removed names when changed
     * through the settings dialog.
     */
    private final Set<String> origNames = new HashSet<>();
    
    public CustomNames(Settings settings) {
        this.settings = settings;
        updateOrigNames();
        settings.addSettingChangeListener(new SettingChangeListener() {

            @Override
            public void settingChanged(String setting, int type, Object value) {
                /**
                 * This is only called when not changed through command, since
                 * mapPut() and mapRemove() don't trigger this.
                 */
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
        updateOrigNames();
    }
    
    public String getCustomName(String nick) {
        nick = StringUtil.toLowerCase(nick);
        return (String)settings.mapGet(SETTING_NAME, nick);
    }
    
    public String commandSetCustomName(String parameter) {
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
    
    public String commandResetCustomname(String parameter) {
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
    
    private void informListeners(String name, String customName) {
        Debugging.println("customNames", "%s => %s", name, customName);
        for (CustomNamesListener listener : getListeners()) {
            listener.setName(name, customName);
        }
    }
    
    private void informListenersAllChanged() {
        @SuppressWarnings("unchecked")
        Map<String, String> customNames = settings.getMap(SETTING_NAME);
        fillRemovedNames(customNames);
        for (String username : customNames.keySet()) {
            informListeners(username, customNames.get(username));
        }
        updateOrigNames();
    }
    
    public synchronized void addListener(CustomNamesListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    private synchronized Set<CustomNamesListener> getListeners() {
        return new HashSet<>(listeners);
    }
    
    private synchronized void updateOrigNames() {
        @SuppressWarnings("unchecked")
        Map<String, String> customNames = settings.getMap(SETTING_NAME);
        origNames.clear();
        origNames.addAll(customNames.keySet());
        Debugging.println("customNames", "OrigNames: %s", origNames);
    }
    
    /**
     * Add previously available names as removed, which when removed through the
     * settings dialog wouldn't otherwise cause a notification.
     * 
     * @param names 
     */
    private synchronized void fillRemovedNames(Map<String, String> names) {
        for (String name : origNames) {
            if (!names.containsKey(name)) {
                names.put(name, null);
            }
        }
    }
    
    /**
     * Listener that can be implemented by classes that want to be informed
     * about changes in custom names.
     */
    public static interface CustomNamesListener {
        
        /**
         * When a custom name got added, changed or removed.
         * 
         * @param name All lowercase name
         * @param customName The custom name, null if custom name removed
         */
        public void setName(String name, String customName);
    }
    
    
    
}
