
package chatty.util;

import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class JSONUtil {
    
    /**
     * Gets the String value for the given key.
     * 
     * @param data The JSONObject
     * @param key The key
     * @return The String value, or null if there is no value for this key or
     * it's not a String
     */
    public static String getString(JSONObject data, Object key) {
        Object value = data.get(key);
        if (value != null && value instanceof String) {
            return (String)value;
        }
        return null;
    }
    
    /**
     * Gets the integer value for the given key.
     * 
     * @param data The JSONObject
     * @param key The key
     * @param errorValue The value to return if no integer was found for this
     * key
     * @return The integer value, or the given errorValue if no integer was
     * found for this key
     */
    public static int getInteger(JSONObject data, Object key, int errorValue) {
        Object value = data.get(key);
        if (value != null && value instanceof Number) {
            return ((Number)value).intValue();
        }
        return errorValue;
    }
    
    public static boolean getBoolean(JSONObject data, Object key, boolean errorValue) {
        Object value = data.get(key);
        if (value != null && value instanceof Boolean) {
            return (Boolean)value;
        }
        return errorValue;
    }
    
}
