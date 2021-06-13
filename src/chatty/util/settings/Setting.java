
package chatty.util.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A single setting, holding one value of one of the specified types. The type
 * and initial (default) value can't be changed, however all other properties
 * can. The type of the value isn't checked here, the caller has to make sure
 * to expect only the types of values that were actually saved, the saved type
 * just helps with this.
 * 
 * @author tduva
 */
public class Setting {
    
    public static final int UNDEFINED = -1;
    public static final int BOOLEAN = 0;
    public static final int STRING = 1;
    public static final int LONG = 2;
    public static final int MAP = 3;
    public static final int LIST = 4;
    
    public static final int CHANGED = 5;
    public static final int NOT_CHANGED = 6;
    
    private final Object defaultValue;
    private final int type;
    
    private Object value;
    private boolean save = true;
    private String file;
    private boolean valueSet;
    
    /**
     * Creates a new Setting object with some initial values.
     * 
     * @param value The initial value and also the default value.
     * @param type The type of this setting.
     * @param save Whether this setting should be saved to a file.
     * @param file The name of the file. Usually the default file.
     */
    public Setting(Object value, int type, boolean save, String file) {
        this.value = value;
        if (type == MAP) {
            this.defaultValue = new HashMap<>((Map)value);
        } else if (type == LIST) {
            this.defaultValue = copyCollection((Collection)value);
        } else {
            this.defaultValue = value;
        }
        this.save = save;
        this.type = type;
        this.file = file;
    }
    
    /**
     * Returns whether this setting should be saved to a file.
     * 
     * @return true if it allowed to be saved, false otherwise.
     */
    public boolean allowedToSave() {
        return save;
    }
    
    /**
     * Gets the current value of this setting.
     * 
     * @return 
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * Checks if this value is of the given type. Types as specified in the
     * constants in this class.
     * 
     * @param type
     * @return 
     */
    public boolean isOfType(int type) {
        return type == this.type;
    }
    
    /**
     * Gets the type of this setting.
     * 
     * @return 
     */
    public int getType() {
        return type;
    }
    
    /**
     * Changes the value if the new one is different from the current value.
     * 
     * @param value
     * @return true when a new value was set, false otherwise
     */
    public boolean setValue(Object value) {
        valueSet = true;
        if (this.value.equals(value)) {
            return false;
        }
        this.value = value;
        return true;
    }
    
    /**
     * Reset value to default.
     * 
     * @return 
     */
    public boolean setToDefault() {
        if (type == MAP) {
            return setValue(new HashMap<>((Map)defaultValue));
        }
        if (type == LIST) {
            return setValue(copyCollection((Collection)defaultValue));
        }
        return setValue(defaultValue);
    }
    
    public Object getDefault() {
        return defaultValue;
    }
    
    public boolean hasDefaultValue() {
        return value.equals(defaultValue);
    }
    
    public boolean isValueSet() {
        return valueSet;
    }
    
    public void setValueSet() {
        this.valueSet = true;
    }
    
    /**
     * Set whether this setting should be saved to a file.
     * 
     * @param save 
     */
    public void setSave(boolean save) {
        this.save = save;
    }
    
    /**
     * Sets the name of the file this setting should be saved into.
     * 
     * @param fileName The name of the file.
     */
    public void setFile(String fileName) {
        this.file = fileName;
    }
    
    /**
     * Gets the name of the file this setting should be saved into.
     * 
     * @return The name of the file.
     */
    public String getFile() {
        return file;
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
    
    /**
     * Create a shallow copy of the collection using the same underlying class.
     * 
     * @param o
     * @return 
     */
    private static Collection copyCollection(Collection o) {
        try {
            Collection copy = null;
            copy = (Collection) o.getClass().newInstance();
            copy.addAll(o);
            return copy;
        } catch (Exception ex) {
            Logger.getLogger(Setting.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList(o);
    }
    
    private static Map copyMap(Map o) {
        try {
            Map copy = null;
            copy = (Map) o.getClass().newInstance();
            copy.putAll(o);
            return copy;
        } catch (Exception ex) {
            Logger.getLogger(Setting.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new HashMap(o);
    }
    
}
