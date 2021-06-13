
package chatty.util.irc;

import chatty.Helper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decodes and encodes IRCv3 message tags and provides convenience methods to
 * access them.
 * 
 * The data of IrcMsgTags objects is read-only. Use the factory methods to
 * create new instances.
 *
 * @author tduva
 */
public class IrcMsgTags {
    
    private static final Map<String, String> EMPTY_TAGS = new HashMap<>();
    
    /**
     * Empty IrcMsgTags object. Can be used if no tags should be provided, to still
     * have a IrcMsgTags object.
     */
    public static final IrcMsgTags EMPTY = new IrcMsgTags(null);

    private final Map<String, String> tags;
    
    protected IrcMsgTags(Map<String, String> tags) {
        if (tags == null) {
            this.tags = EMPTY_TAGS;
        } else {
            this.tags = tags;
        }
    }
    
    /**
     * Returns true if the given key is contained in these tags.
     * 
     * @param key The key to look for
     * @return true if the key is in the tags, false otherwise
     */
    public boolean containsKey(String key) {
        return tags.containsKey(key);
    }
    
    /**
     * Returns a copy of all keys.
     * 
     * @return 
     */
    public Set<String> keys() {
        return new HashSet<>(tags.keySet());
    }
    
    /**
     * Adds all key/value pairs to the given map.
     * 
     * @param map 
     */
    public void fill(Map<String, String> map) {
        map.putAll(tags);
    }
    
    /**
     * Returns true if there are no key/value pairs.
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return tags.isEmpty();
    }
    
    /**
     * Returns true if the given key in the tags is equal to "1", false
     * otherwise.
     *
     * @param key The key to check
     * @return True if equal to 1, false otherwise
     */
    public boolean isTrue(String key) {
        return "1".equals(tags.get(key));
    }
    
    public boolean isValue(String key, String value) {
        return value.equals(tags.get(key));
    }
    
    public boolean isValueOf(String key, String... values) {
        for (String value : values) {
            if (value.equals(tags.get(key))) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasValue(String key) {
        String value = tags.get(key);
        return value != null && !value.isEmpty();
    }
    
    /**
     * Returns the String associated with key, or null if the key doesn't exist.
     * 
     * @param key The key to look up in the tags
     * @return String associated with this key, or null
     */
    public String get(String key) {
        return get(key, null);
    }
    
    /**
     * Returns the String associated with key, or the defaultValue if the key
     * doesn't exist.
     * 
     * @param key The key to look up in the tags
     * @param defaultValue The default value to return if key isn't in tags
     * @return String associated with this key, or defaultValue
     */
    public String get(String key, String defaultValue) {
        if (tags.containsKey(key)) {
            return tags.get(key);
        }
        return defaultValue;
    }
    
    /**
     * Returns the integer associated with the given key, or the defaultValue if
     * no integer was found for that key.
     * 
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if the given key doesn't
     * point to an integer value
     * @return The integer associated with the key, or defaultValue
     */
    public int getInteger(String key, int defaultValue) {
        if (tags.get(key) != null) {
            try {
                return Integer.parseInt(tags.get(key));
            } catch (NumberFormatException ex) {
                // Just go to default value
            }
        }
        return defaultValue;
    }
    
    public boolean hasInteger(String key) {
        try {
            Integer.parseInt(tags.get(key));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Returns the long associated with the given key, or the defaultValue if no
     * long was found for that key.
     * 
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if the given key doesn't
     * point to a long value
     * @return The long associated with the key, or defaultValue
     */
    public long getLong(String key, long defaultValue) {
        if (tags.get(key) != null) {
            try {
                return Long.parseLong(tags.get(key));
            } catch (NumberFormatException ex) {
                // Just go to default value
            }
        }
        return defaultValue;
    }
    
    /**
     * Build a IRCv3 tags String for this tags object (no leading @).
     * 
     * @return The tags string (may be empty if this tags object is empty)
     */
    public String toTagsString() {
        StringBuilder b = new StringBuilder();
        Iterator<String> it = tags.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = tags.get(key);
            if (isValidKey(key)) {
                b.append(key);
                if (isValidValue(value)) {
                    b.append("=").append(escapeValue(value));
                }
                if (it.hasNext()) {
                    b.append(";");
                }
            }
        }
        return b.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IrcMsgTags other = (IrcMsgTags) obj;
        if (!Objects.equals(this.tags, other.tags)) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.tags);
        return hash;
    }

    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9-]+", Pattern.CASE_INSENSITIVE);
    
    private static boolean isValidKey(String key) {
        return KEY_PATTERN.matcher(key).matches();
    }
    
    private static boolean isValidValue(String value) {
        return value != null;
    }
    
    private static String escapeValue(String value) {
        return Helper.tagsvalue_encode(value);
    }
    
    @Override
    public String toString() {
        return tags.toString();
    }
    
    //================
    // Factory Methods
    //================
    
    public static Map<String, String> createTags(String... args) {
        Map<String, String> tags = new HashMap<>();
        Iterator<String> it = Arrays.asList(args).iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (it.hasNext()) {
                tags.put(key, it.next());
            } else {
                tags.put(key, null);
            }
        }
        return tags;
    }
    
    public static Map<String, String> parseTags(String data) {
        if (data == null) {
            return null;
        }
        String[] tags = data.split(";");
        if (tags.length > 0) {
            Map<String, String> result = new HashMap<>();
            for (String tag : tags) {
                String[] keyValue = tag.split("=",2);
                if (keyValue.length == 2) {
                    result.put(keyValue[0], Helper.tagsvalue_decode(keyValue[1]));
                } else if (!keyValue[0].isEmpty()) {
                    result.put(keyValue[0], null);
                }
            }
            return result;
        }
        return null;
    }
    
}
