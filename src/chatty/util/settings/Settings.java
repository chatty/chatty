
package chatty.util.settings;

import chatty.Logging;
import chatty.util.MiscUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Manage (add/change/get/save/load) settings.
 * 
 * Reading and writing values should hopefully maybe be thread-safe. Adding
 * settings and checking type isn't synchronized, but if settings are only
 * added once at the beginning this shouldn't be a problem.
 * 
 * @author tduva
 */
public class Settings {
    
    private final Object LOCK = new Object();
    
    /**
     * Holds all settings of different Types. TreeMap to have setting names
     * lookup case-insenstive while still retaining the case for display.
     */
    private final Map<String,Setting> settings = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    private final Set<SettingChangeListener> listeners = new HashSet<>();
    private final Set<SettingsListener> settingsListeners = new HashSet<>();
    private final String defaultFile;
    private final Set<String> files = new HashSet<>();
    
    private static final Logger LOGGER = Logger.getLogger(Settings.class.getName());
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    
    private boolean saved;
    
    public Settings(String path) {
        this.defaultFile = path;
    }
    
    public void addSettingChangeListener(SettingChangeListener listener) {
        listeners.add(listener);
    }
    
    private void settingChanged(String setting, int type, Object value) {
        for (SettingChangeListener listener : listeners) {
            listener.settingChanged(setting, type, value);
        }
    }
    
    public void addSettingsListener(SettingsListener listener) {
        settingsListeners.add(listener);
    }
    
    private void aboutToSaveSettings() {
        for (SettingsListener listener : settingsListeners) {
            listener.aboutToSaveSettings(this);
        }
    }
    
    public void addFile(String fileName) {
        files.add(fileName);
    }
    
    public void setFile(String settingName, String fileName) {
        if (!isSetting(settingName)) {
            throw new SettingNotFoundException("Could not find setting: "+settingName);
        }
        if (!files.contains(fileName)) {
            throw new SettingFileNotFoundException("Could not find setting file: "+fileName);
        }
        Setting setting = settings.get(settingName);
        setting.setFile(fileName);
    }
    
    private boolean isSetting(String settingName) {
        if (getType(settingName) != Setting.UNDEFINED) {
            return true;
        }
        return false;
    }
    
    /**
     * Checks if the setting with the given name is of the given type.
     * 
     * Returns true if the settings exists and is of the given type, false
     * otherwise.
     * 
     * @param settingName The name of the setting to check
     * @param type
     * @return 
     */
    private boolean isOfType(String settingName, int type) {
        Setting obj = settings.get(settingName);
        if (obj != null && obj.isOfType(type)) {
            return true;
        }
        return false;
    }
    
    private boolean isOfSubtype(String settingName, int type) {
        Setting obj = settings.get(settingName);
        if (obj != null && obj instanceof SubtypeSetting) {
            return ((SubtypeSetting)obj).isOfSubType(type);
        }
        return false;
    }
    
    private int getType(String settingName) {
        Setting obj = settings.get(settingName);
        if (obj != null) {
            return obj.getType();
        }
        return Setting.UNDEFINED;
    }
    
    public boolean isBooleanSetting(String settingName) {
        return isOfType(settingName, Setting.BOOLEAN);
    }
    
    public boolean isStringSetting(String settingName) {
        return isOfType(settingName, Setting.STRING);
    }
    
    public boolean isLongSetting(String settingName) {
        return isOfType(settingName, Setting.LONG);
    }
    
    public boolean isMapSetting(String settingName) {
        return isOfType(settingName, Setting.MAP);
    }
    
    public boolean isListSetting(String settingName) {
        return isOfType(settingName, Setting.LIST);
    }

    public void addBoolean(String settingName, boolean value, boolean save) {
        add(settingName, value, Setting.BOOLEAN, Setting.UNDEFINED, save);
    }

    public void addString(String settingName, String value, boolean save) {
        add(settingName, value, Setting.STRING, Setting.UNDEFINED, save);
    }

    public void addLong(String settingName, long value, boolean save) {
        add(settingName, value, Setting.LONG, Setting.UNDEFINED, save);
    }
    
    public void addBoolean(String settingName, boolean value) {
        addBoolean(settingName, value, true);
    }
    
    public void addString(String settingName, String value) {
        addString(settingName, value, true);
    }
    
    public void addLong(String settingName, long value) {
        addLong(settingName, value, true);
    }
    
    public void addMap(String settingName, Map value, int type) {
        add(settingName, value, Setting.MAP, type, true);
    }
    
    public void addList(String settingName, Collection value, int type) {
        add(settingName, value, Setting.LIST, type, true);
    }
    
    public int setString(String settingName, String value) {
        return set(settingName, value, Setting.STRING);
    }
    
    public int setBoolean(String settingName, boolean value) {
        return set(settingName, value, Setting.BOOLEAN);
    }
    
    public int setLong(String settingName, long value) {
        return set(settingName, value, Setting.LONG);
    }
    

    /**
     * Adds a setting, overwrites existing setting. A setting must be added
     * before changing the value or getting the setting is possible.
     * 
     * @param settingName
     * @param value
     * @param type
     * @param save 
     */
    private void add(String settingName, Object value, int type, int subType, boolean save) {
        if (type == Setting.MAP || type == Setting.LIST) {
            settings.put(settingName, new SubtypeSetting(value, type, subType, save, defaultFile));
        }
        else {
            settings.put(settingName, new Setting(value, type, save, defaultFile));
        }
    }
    
    /**
     * Sets the given setting to a new value. The type must match the type
     * of the given value.
     * 
     * @param settingName
     * @param value
     * @param type 
     */
    private int set(String settingName, Object value, int type) {
        boolean changed = false;
        synchronized(LOCK) {
            if (!isOfType(settingName, type)) {
                throw new SettingNotFoundException("Could not find setting: " + settingName);
            }
            Setting setting = settings.get(settingName);

            if (value == null) {
                changed = setting.setToDefault();
                value = setting.getValue();
            } else {
                changed = setting.setValue(value);
            }
        }
        if (changed) {
            settingChanged(settingName,type,value);
            return Setting.CHANGED;
        }
        return Setting.NOT_CHANGED;
    }
    
    /**
     * Gets the value (as Object, but containing the type specified by the type
     * parameter) of the setting with the given name and type, or throws an
     * exception if the setting wasn't found or wasn't of the specified type.
     * 
     * @param settingName The name of the setting.
     * @param type The type of the setting.
     * @return The Object value, which is actually of 'type'.
     */
    private Object get(String settingName, int type, boolean getDefault) {
        synchronized(LOCK) {
            if (!isOfType(settingName, type)) {
                throw new SettingNotFoundException("Could not find setting: " + settingName);
            }
            if (getDefault) {
                return settings.get(settingName).getDefault();
            }
            return settings.get(settingName).getValue();
        }
    }
    
    private Object get(String settingName, int type) {
        return get(settingName, type, false);
    }
    
    private Object get(String settingName) {
        synchronized(LOCK) {
            return settings.get(settingName).getValue();
        }
    }
    
    private Setting getSetting(String settingName) {
        Setting setting = settings.get(settingName);
        if (setting == null) {
            throw new SettingNotFoundException("Could not find setting: " + settingName);
        }
        return setting;
    }
    
    public boolean hasDefaultValue(String settingName) {
        synchronized(LOCK) {
            return getSetting(settingName).hasDefaultValue();
        }
    }
    
    public boolean isValueSet(String settingName) {
        synchronized(LOCK) {
            return getSetting(settingName).isValueSet();
        }
    }
    
    public boolean getBoolean(String settingName) {
        return (Boolean)get(settingName, Setting.BOOLEAN);
    }
    
    public boolean getBooleanDefault(String settingName) {
        return (Boolean)get(settingName, Setting.BOOLEAN, true);
    }
    
    /**
     * Gets a String setting.
     * 
     * @param setting
     * @return 
     */
    public String getString(String setting) {
        return (String)get(setting, Setting.STRING);
    }
    
    public String getStringDefault(String setting) {
        return (String)get(setting, Setting.STRING, true);
    }

    public long getLong(String setting) {
        return ((Number)(get(setting, Setting.LONG))).longValue();
    }


    /**
     * Return a {@code HashMap} with a copy of the data of this setting.
     * 
     * @param settingName The name of the setting
     * @return The {@code HashMap} with the data of this setting.
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a Map setting
     */
    public Map getMap(String settingName) {
        synchronized(LOCK) {
            return new HashMap(getMapInternal(settingName));
        }
    }
    
    /**
     * Copies the current data of the Map of this setting into the given Map,
     * then returns it for convenience.
     *
     * @param settingName The name of the setting
     * @param map The map to put the data into
     * @return The given Map, that now contains a copy of the data of this
     * setting.
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a Map setting
     */
    public Map getMap(String settingName, Map map) {
        synchronized (LOCK) {
            map.putAll(getMapInternal(settingName));
            return map;
        }
    }
    
    /**
     * Stores a copy of the data from <tt>map</tt> into the setting with the
     * given name. Clears the setting Map first, so only the given data will be
     * in there.
     *
     * @param settingName The name of the setting
     * @param map The Map of data to put into the settings
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a Map setting
     */
    public boolean putMap(String settingName, Map map) {
        synchronized (LOCK) {
            Map settingMap = getMapInternal(settingName);
            boolean changed = !settingMap.equals(map);
            settingMap.clear();
            settingMap.putAll(map);
            return changed;
        }
    }
    
    public Object mapGet(String settingName, Object key) {
        synchronized(LOCK) {
            return getMapInternal(settingName).get(key);
        }
    }

    
    /**
     * Puts a {@code key}-{@code value} pair into the {@code Map} with the name
     * {@code settingName}.
     * 
     * @param settingName The name of the setting
     * @param key The key to put into the map
     * @param value The value to put into the map
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a Map setting
     */
    public void mapPut(String settingName, Object key, Object value) {
        synchronized(LOCK) {
            getMapInternal(settingName).put(key, value);
        }
    }
    
    
    
    /**
     * Clears the Map of the setting with the given name.
     * 
     * @param settingName 
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a Map setting
     */
    public void mapClear(String settingName) {
        synchronized(LOCK) {
            getMapInternal(settingName).clear();
        }
    }
    

    
    /**
     * Removes {@code key} from the {@code Map} of the setting with
     * {@code settingName}.
     *
     * @param settingName The name of the setting
     * @param key The key to remove
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a Map setting
     */
    public void mapRemove(String settingName, Object key) {
        synchronized (LOCK) {
            getMapInternal(settingName).remove(key);
        }
    }

    /**
     * Returns an {@code ArrayList} containing a copy of the data in this
     * setting.
     * 
     * @param settingName The name of the setting
     * @return The {@code ArrayList} containing a copy of the data
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a List setting.
     */
    public List getList(String settingName) {
        synchronized (LOCK) {
            return new ArrayList(getListInternal(settingName));
        }
    }
    
    /**
     * Stores a shallow copy of the data from the setting <tt>settingName</tt>
     * into the given list.
     *
     * @param settingName
     * @param list
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a List setting.
     */
    public void getList(String settingName, List list) {
        synchronized (LOCK) {
            list.addAll((Collection) get(settingName, Setting.LIST));
        }
    }
    
    /**
     * Saves a shallow copy of the given list into the settings. The given list
     * should not be modified concurrently during this.
     *
     * @param settingName
     * @param list
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a List setting.
     */
    public void putList(String settingName, Collection list) {
        synchronized (LOCK) {
            Collection settingList = (Collection) get(settingName, Setting.LIST);
            settingList.clear();
            settingList.addAll(list);
        }
    }
    
    /**
     * Checks if the List for the given setting contains the given {@code value}.
     * 
     * @param settingName The name of the {@code List} setting
     * @param value The value to check
     * @return {@code true} if the value is contained in the {@code List}, {@code false} otherwise
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a {@code List} setting.
     */
    public boolean listContains(String settingName, Object value) {
        synchronized(LOCK) {
            return getListInternal(settingName).contains(value);
        }
    }

    /**
     * Removes {@code value} from the List setting with the name
     * {@code settingName}.
     * 
     * @param settingName The name of the setting
     * @param value The value to remove
     * @throws SettingNotFoundException if a setting with this name doesn't
     * exist or isn't a List setting.
     */
    public boolean listRemove(String settingName, Object value) {
        synchronized(LOCK) {
            return getListInternal(settingName).remove(value);
        }
    }
    
    public void listAdd(String settingName, Object value) {
        synchronized(LOCK) {
            getListInternal(settingName).add(value);
        }
    }
    
    public void listClear(String settingName) {
        synchronized(LOCK) {
            getListInternal(settingName).clear();
        }
    }
    
    /**
     * Adds the given <tt>Object</tt> to this <tt>List</tt> setting, if it
     * wasn't already in there.
     * 
     * @param settingName
     * @param value 
     * @throws SettingNotFoundException if a setting with the given name doesn't
     * exist or isn't a List-setting.
     */
    public boolean setAdd(String settingName, Object value) {
        synchronized(LOCK) {
            Collection settingList = getListInternal(settingName);
            if (!settingList.contains(value)) {
                settingList.add(value);
                return true;
            }
            return false;
        }
    }
    
        
    /**
     * Gets the Map of the setting with this name, that can be directly modified
     * (if synchronized on LOCK).
     * 
     * @param settingName The name of the setting
     * @return The Map for this setting
     */
    private Map getMapInternal(String settingName) {
        synchronized (LOCK) {
            return (Map) get(settingName, Setting.MAP);
        }
    }
        
    /**
     * Returns the actual List for this setting, that can directly be modified
     * (if synchronized on <tt>LOCK</tt>).
     * 
     * @param settingName
     * @return 
     */
    private Collection getListInternal(String settingName) {
        return (Collection) get(settingName, Setting.LIST);
    }
    
    /**
     * Manually set a List or Map setting as changed, since those can't properly
     * detect it themselves.
     * 
     * @param settingName 
     */
    public void setSettingChanged(String settingName) {
        if (isListSetting(settingName)) {
            settingChanged(settingName, Setting.LIST, getList(settingName));
        } else if (isMapSetting(settingName)) {
            settingChanged(settingName, Setting.MAP, getMap(settingName));
        }
    }
    
    public String settingValueToString(String setting) {
        synchronized(LOCK) {
            return settings.get(setting).toString();
        }
    }
    
    public String settingToString(String setting) {
        if (isBooleanSetting(setting)) {
            return "Setting '"+setting+"' is "+getBoolean(setting)+".";
        }
        if (isLongSetting(setting)) {
            return "Setting '"+setting+"' is "+getLong(setting)+".";
        }
        if (isStringSetting(setting)) {
            return "Setting '"+setting+"' is '"+getString(setting)+"'.";
        }
        if (isMapSetting(setting)) {
            return "Setting '"+setting+"' is '"+getMap(setting)+"'.";
        }
        if (isListSetting(setting)) {
            return "Setting '"+setting+"' is '"+getList(setting)+"'.";
        }
        return null;
    }
    
    public String addTextual(String text) {
        if (text == null || text.isEmpty()) {
            return "Usage: /add <setting> <value>";
        }
        String[] split = text.trim().split(" ", 2);
        
        if (split.length < 2) {
            return "Usage: /add <setting> <value>";
        }
        String setting = split[0];
        String parameter = split[1];
        if (isListSetting(setting)) {
            if (isOfSubtype(setting, Setting.STRING)) {
                listAdd(setting, parameter);
                setSettingChanged(setting);
            } else if (isOfSubtype(setting, Setting.LONG)) {
                try {
                    listAdd(setting, Long.parseLong(parameter));
                    setSettingChanged(setting);
                } catch (NumberFormatException ex) {
                    return settingInvalidMessage(setting);
                }
            } else {
                return settingInvalidMessage(setting);
            }
            return "Setting '"+setting+"' (List): Added '"+parameter+"', now: "+getList(setting);
        }
        return settingInvalidMessage(setting);
    }
    
    public String removeTextual(String text) {
        if (text == null || text.isEmpty()) {
            return "Usage: /remove <setting> <value>";
        }
        String[] split = text.trim().split(" ", 2);
        
        if (split.length < 2) {
            return "Usage: /remove <setting> <value>";
        }
        String setting = split[0];
        String parameter = split[1];
        if (isListSetting(setting)) {
            if (isOfSubtype(setting, Setting.STRING)) {
                listRemove(setting, parameter);
                setSettingChanged(setting);
            } else if (isOfSubtype(setting, Setting.LONG)) {
                try {
                    listRemove(setting, Long.parseLong(parameter));
                    setSettingChanged(setting);
                } catch (NumberFormatException ex) {
                    return settingInvalidMessage(setting);
                }
            } else {
                return settingInvalidMessage(setting);
            }
            return "Setting '"+setting+"' (List): Removed '"+parameter+"', now: "+getList(setting);
        } else if (isMapSetting(setting)) {
            mapRemove(setting, parameter);
            setSettingChanged(setting);
            return "Setting '"+setting+"' (Map): Removed '"+parameter+"', now: "+getMap(setting);
        }
        return settingInvalidMessage(setting);
    }
    
    public String setTextual(String text) {
        if (text == null || text.isEmpty()) {
            return "Usage: /set <setting> <value>";
        }
        String[] split = text.split(" ", 2);
        
        if (split.length < 2) {
            return "Usage: /set <setting> <value>";
        }
        String setting = split[0];
        String parameter = split[1];
        if (isBooleanSetting(setting)) {
            boolean value = false;
            if (parameter.equals("1") || parameter.equals("true") ||
                    parameter.equals("on")) {
                value = true;
            }
            if (parameter.equals("!")) {
                value = !getBoolean(setting);
            }
            setBoolean(setting,value);
            return "Setting '"+setting+"' set to "+value+".";
        }
        else if (isStringSetting(setting)) {
            setString(setting,parameter);
            return "Setting '"+setting+"' set to '"+parameter+"'.";
        }
        else if (isLongSetting(setting)) {
            long value = 0;
            try {
                value = Long.parseLong(parameter);
            } catch (NumberFormatException ex) {
                return "Invalid value (must be numeric for this setting).";
            }
            setLong(setting,value);
            return "Setting '"+setting+"' set to '"+parameter+"'.";
        }
        else if (isListSetting(setting) && isOfSubtype(setting, Setting.STRING)) {
            listClear(setting);
            listAdd(setting, parameter);
            setSettingChanged(setting);
            return "Setting '"+setting+"' (List) set to "+getList(setting);
        }
        else if (isMapSetting(setting) && isOfSubtype(setting, Setting.STRING)) {
            String[] mapParameters = parameter.split(" ", 2);
            if (mapParameters.length != 2) {
                return "Invalid number of parameters to set map value.";
            }
            mapPut(setting, mapParameters[0], mapParameters[1]);
            setSettingChanged(setting);
            return "Setting '"+setting+"' (Map) set to "+getMap(setting);
        }
        return settingInvalidMessage(setting);
    }
    
    public String resetTextual(String text) {
        if (text == null || text.isEmpty()) {
            return "Usage: /reset <setting>";
        }
        String[] split = text.split(" ", 2);
        String settingName = split[0];
        
        if (getType(settingName) != Setting.UNDEFINED) {
            int result = set(settingName, null, getType(settingName));
            Object value = get(settingName);
            if (result == Setting.CHANGED) {
                return "Setting '"+settingName+"' reset to default ("+value.toString()+")";
            } else {
                return "Setting '"+settingName+"' already default ("+value.toString()+")";
            }
        }
        return "Setting does not exist.";
    }
    
    public String getTextual(String text) {
        if (text == null || text.isEmpty()) {
            return "Usage: /get <setting>";
        }
        String[] split = text.split(" ");
        String setting = split[0];
        String output = settingToString(setting);
        if (output != null) {
            return output;
        }
        return "Setting does not exist.";
    }
    
    /**
     * Sets the setting with the specified name to an empty value. This is only
     * possible with Strings.
     * 
     * @param text
     * @return 
     */
    public String clearTextual(String text) {
        if (text == null || text.isEmpty()) {
            return "Usage: /clearSetting <setting>";
        }
        String[] split = text.split(" ");
        String setting = split[0];
        if (isBooleanSetting(setting)) {
            return "Boolean settings can't be cleared.";
        }
        if (isLongSetting(setting)) {
            return "Numeric settings can't be cleared.";
        }
        if (isStringSetting(setting)) {
            setString(setting, "");
            return "Setting '"+setting+"' set to empty string.";
        }
        if (isListSetting(setting)) {
            listClear(setting);
            return "Setting '"+setting+"' is now empty.";
        }
        if (isMapSetting(setting)) {
            mapClear(setting);
            return "Setting '"+setting+"' is now empty.";
        }
        return settingInvalidMessage(setting);
    }
    
    private String settingInvalidMessage(String setting) {
        if (isSetting(setting)) {
            return "Invalid setting: '"+setting+"' (can't change with this command).";
        }
        return "Invalid setting: '"+setting+"'.";
    }
    
    /**
     * Turns all settings into a JSON String.
     * 
     * @return The JSON
     */
    private String settingsToJson(String file) {
        JSONObject obj = new JSONObject();
        
        Set<Map.Entry<String,Setting>> set = settings.entrySet();
        for (Entry<String,Setting> entry : set) {
            Setting setting = entry.getValue();
            if (setting.allowedToSave() && setting.getFile().equals(file)) {
                String key = entry.getKey();
                Object value = setting.getValue();
                
                // JSON Simple only supports List in this version
                if (value instanceof Collection) {
                    value = new ArrayList((Collection)value);
                }
                obj.put(key, value);
            }
        }
        
        if (obj.isEmpty()) {
            return null;
        }
        return obj.toJSONString();
    }
    
    /**
     * Parses the settings from a JSON String and adds them to the settings
     * data. Only loads settings that were peviously defined and that can be
     * saved.
     * 
     * @param json
     * @throws ParseException 
     */
    private void settingsFromJson(String json) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject)parser.parse(json);

        for (Entry<String,Setting> entry : settings.entrySet()) {
            // For every defined setting
            Setting setting = entry.getValue();
            String settingName = entry.getKey();
            Object obj = root.get(settingName);
            if (setting.allowedToSave()) {
                int objType = getTypeFromObject(obj);
                //System.out.println(settingName+" "+objType);
                if (objType == setting.getType()) {
                    //System.out.println("Loading setting: "+settingName+" "+obj);
                    if (objType == Setting.MAP) {
                        mapFromJson((Map)obj, (SubtypeSetting)setting);
                    }
                    else if (objType == Setting.LIST) {
                        listFromJson((List)obj, (SubtypeSetting)setting);
                    }
                    else {
                        setting.setValue(obj);
                    }
                }
            }
        }
    }
    
    
    private void mapFromJson(Map map, SubtypeSetting setting) {
        Map settingMap = (Map)setting.getValue();
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (getTypeFromObject(value) == setting.getSubType()) {
                settingMap.put(key, value);
            }
        }
    }
    
    private void listFromJson(List list, SubtypeSetting setting) {
        Collection settingList = (Collection)setting.getValue();
        settingList.clear();
        for (Object value : list) {
            if (getTypeFromObject(value) == setting.getSubType()) {
                settingList.add(value);
            }
        }
    }
    
    
    private int getTypeFromObject(Object obj) {
        if (obj instanceof Number) {
            return Setting.LONG;
        }
        else if (obj instanceof String) {
            return Setting.STRING;
        }
        else if (obj instanceof Boolean) {
            return Setting.BOOLEAN;
        }
        else if (obj instanceof Map) {
            return Setting.MAP;
        }
        else if (obj instanceof List) {
            return Setting.LIST;
        }
        return Setting.UNDEFINED;
    }
    
    /**
     * Saves the settings to a file as JSON.
     * 
     * @throws IOException 
     */
    public void saveSettingsToJson() {
        aboutToSaveSettings();
        synchronized(LOCK) {
            System.out.println("Saving settings to JSON.");
            saveSettingsToJson(defaultFile);
            for (String fileName : files) {
                saveSettingsToJson(fileName);
            }
        }
    }
    
    private void saveSettingsToJson(String fileName) {
        String json = settingsToJson(fileName);
        Path file = Paths.get(fileName);
        if (json == null) {
            try {
                if (Files.exists(file)) {
                    LOGGER.info("Removing unused file: "+fileName);
                    Files.delete(file);
                }
            } catch (NoSuchFileException ex) {
                // Don't need to remove non-existing file
            } catch (IOException ex) {
                LOGGER.warning("Error removing unused file: "+ex);
            }
        } else {
            LOGGER.info("Saving settings to file: "+fileName);
            try {
                Path tempFile = Paths.get(fileName + "-temp");
                try (BufferedWriter writer = Files.newBufferedWriter(tempFile, CHARSET)) {
                    writer.write(json);
                }
                MiscUtil.moveFile(tempFile, file);
            } catch (IOException ex) {
                LOGGER.warning("Error saving settings to file: " + ex);
                System.out.println("Error saving settings to file: " + ex);
            }
        }
    }

    /**
     * Loads the settings from a JSON file.
     */
    public void loadSettingsFromJson() {
        synchronized(LOCK) {
            loadSettingsFromJson(defaultFile);
            for (String fileName : files) {
                loadSettingsFromJson(fileName);
            }
        }
    }
    
    private void loadSettingsFromJson(String fileName) {
        LOGGER.info("Loading settings from file: "+fileName);
        Path file = Paths.get(fileName);
        
        try (BufferedReader reader = Files.newBufferedReader(file, CHARSET)) {
            String input = reader.readLine();
            if (input != null) {
                try {
                    settingsFromJson(input);
                } catch (ParseException ex) {
                    logParseError(fileName, input, ex);
                }
            } else {
                LOGGER.warning("Settings file empty: "+fileName);
                LOGGER.log(Logging.USERINFO, "Settings file empty, using default settings ("+fileName+")");
            }
        } catch (IOException ex) {
            LOGGER.warning("Error loading settings from file: "+ex);
        }
    }
    
    private static void logParseError(String fileName, String input, ParseException ex) {
        int pos = ex.getPosition();
        int start = pos - 10;
        int end = pos + 10;
        start = start < 0 ? 0 : start;
        end = end > input.length() ? input.length() : end;
        String excerpt = input.substring(start, pos) + "@" + input.substring(pos, end);
        LOGGER.warning("Error parsing settings: " + ex + "[" + excerpt + "]");
        LOGGER.log(Logging.USERINFO, String.format("Settings file corrupt, using default settings (%s) [%s]",
                fileName,
                excerpt));
    }
    
    public Collection<String> getSettingNames() {
        synchronized(LOCK) {
            return new HashSet<>(settings.keySet());
        }
    }
    
}