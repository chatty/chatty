
package chatty.util;

import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
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
     * @param errorValue Value to return when there is no value for the key or
     * it's not a String (or it's null)
     * @return The String value, or errorValue if there is no value for this key
     * or it's not a String
     */
    public static String getString(JSONObject data, Object key, String errorValue) {
        Object value = data.get(key);
        if (value != null && value instanceof String) {
            return (String)value;
        }
        return errorValue;
    }
    
    /**
     * Gets the String value for the given key.
     * 
     * @param data The JSONObject
     * @param key The key
     * @return The String value, or null if there is no value for this key or
     * it's not a String
     */
    public static String getString(JSONObject data, Object key) {
        return getString(data, key, null);
    }
    
    public static List<String> getStringList(JSONObject data, Object key) {
        Object value = data.get(key);
        if (value != null && value instanceof JSONArray) {
            List<String> result = new ArrayList<>();
            for (Object item : (JSONArray)value) {
                if (item instanceof String) {
                    result.add((String)item);
                }
            }
            return result;
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
    
    public static int parseInteger(JSONObject data, Object key, int errorValue) {
        try {
            return Integer.parseInt((String)data.get(key));
        } catch (Exception ex) {
            return errorValue;
        }
    }
    
    public static long parseLong(JSONObject data, Object key, long errorValue) {
        try {
            return Long.parseLong((String)data.get(key));
        } catch (Exception ex) {
            return errorValue;
        }
    }
    
    public static long getLong(JSONObject data, Object key, long errorValue) {
        Object value = data.get(key);
        if (value != null && value instanceof Number) {
            return ((Number)value).longValue();
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
    
    public static String listToJSON(Object... args) {
        JSONArray o = new JSONArray();
        for (Object a : args) {
            o.add(a);
        }
        return o.toJSONString();
    }
    
    public static long getDatetime(JSONObject data, Object key, long errorValue) {
        Object value = data.get(key);
        if (value != null && value instanceof String) {
            try {
                return DateTime.parseDatetime((String) value);
            }
            catch (IllegalArgumentException ex) {
                return errorValue;
            }
        }
        return errorValue;
    }
    
}
