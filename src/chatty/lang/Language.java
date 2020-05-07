
package chatty.lang;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Create a ResourceBundle for a specific language and provide static functions
 * to retrieve strings from it.
 * 
 * @author tduva
 */
public class Language {
    
    private static final Logger LOGGER = Logger.getLogger(Language.class.getName());

    private static final boolean DEBUG = false;
    
    private static ResourceBundle strings;
    
    /**
     * Set the language to use like "de" or "de-AT". Restart of Chatty is
     * required for most changes to take effect.
     * 
     * @param language 
     */
    public synchronized static void setLanguage(String language) {
        strings = getBundleForLanguage(language);
        LOGGER.info(String.format("[Localization] Loaded locale '%s' for requested '%s'",
                strings.getLocale(), language));
    }
    
    public static ResourceBundle getBundleForLanguage(String language) {
        Locale locale = parseLanguage(language);
        try {
            return ResourceBundle.getBundle("chatty.lang.Strings", locale, CONTROL);
        }
        catch (UnsupportedOperationException ex) {
            // If this exception occurs, it should mean that it's a named module
            // and thus would be read as UTF-8 by default anyway
            return ResourceBundle.getBundle("chatty.lang.Strings", locale);
        }
    }
    
    public static Locale parseLanguage(String language) {
        String[] split = language.split("_");
        Locale locale;
        if (language.trim().isEmpty()) {
            locale = Locale.getDefault();
        }
        else if (split.length == 2) {
            locale = new Locale(split[0], split[1]);
        }
        else {
            locale = new Locale(language);
        }
        return locale;
    }
    
    /**
     * Gets the String with the given key for the current language. If the key
     * wasn't found a String consisting of a single questionmark is returned.
     * 
     * @param key The key as defined in the properties files
     * @return The language specific String, or "?" if none could be found
     */
    public static String getString(String key) {
        return getString(key, true);
    }

    /**
     * Gets the String with the given key for the current language.
     * 
     * <p>
     * Optionally either null or "?" is returned if the given key wasn't found.
     * Getting only non-null values can be useful where a key is expected to
     * exist, but should not cause an error if it's not there (however "?"
     * displayed in the GUI would still indicate an issue). On the other hand,
     * getting a null value can e.g. be useful when checking for the existence
     * of a (probably dynamically built) key, and then falling back on another
     * string if it doesn't exist.
     *
     * @param key The key as defined in the properties files
     * @param nonNull If true, only non-null values are returned ("?" for
     * missing keys)
     * @return The language specific String, or null or "?" if none could be
     * found
     */
    public synchronized static String getString(String key, boolean nonNull) {
        loadIfNecessary();
        if (!strings.containsKey(key)) {
            if (nonNull) {
                // Only output warning if non-null is expected, since otherwise
                // the program doesn't necessarily expect the key to exist
                LOGGER.warning("Missing string key: "+key);
                return "?";
            }
            if (DEBUG) {
                return "{"+key+"}";
            }
            return null;
        }
        if (DEBUG) {
            return "["+strings.getString(key)+"]";
        }
        return strings.getString(key);
    }
    
    /**
     * Gets the String with the given key for the current language. Provide
     * arguments for any replacements present in the String.
     * 
     * @param key The key as defined in the properties files
     * @param arguments One or more arguments, depending on the String value
     * @return The language specific String, or "?" if none could be found
     */
    public synchronized static String getString(String key, Object... arguments) {
        return getString(key, true, arguments);
    }
    
    /**
     * Gets the String with the given key for the current language. This "Null"
     * variant of the method returns null for invalid keys.
     * 
     * @param key The key as defined in the properties files
     * @return The language specific String, or null if none could be found
     */
    public synchronized static String getStringNull(String key) {
        return getString(key, false);
    }
    
    /**
     * Gets the String with the given key for the current language. Provide
     * arguments for any replacements present in the String. This "Null" variant
     * of the method returns null for invalid keys.
     * 
     * @param key The key as defined in the properties files
     * @param arguments One or more arguments, depending on the String value
     * @return The language specific String, or null if none could be found
     */
    public synchronized static String getStringNull(String key, Object... arguments) {
        return getString(key, false, arguments);
    }
    
    /**
     * Gets the String with the given key for the current language. Provide
     * arguments for any replacements present in the String.
     * 
     * <p>
     * Optionally either null or "?" is returned if the given key wasn't found.
     * Getting only non-null values can be useful where a key is expected to
     * exist, but should not cause an error if it's not there (however "?"
     * displayed in the GUI would still indicate an issue). On the other hand,
     * getting a null value can e.g. be useful when checking for the existence
     * of a (probably dynamically built) key, and then falling back on another
     * string if it doesn't exist.
     * 
     * @param key The key as defined in the properties files
     * @param nonNull If true, only non-null values are returned ("?" for
     * missing keys)
     * @param arguments One or more arguments, depending on the String value
     * @return The language specific String, or "?" if none could be found
     */
    public synchronized static String getString(String key, boolean nonNull, Object... arguments) {
        loadIfNecessary();
        if (!strings.containsKey(key)) {
            if (nonNull) {
                LOGGER.warning("Missing string key: "+key);
                return "?";
            }
            if (DEBUG) {
                return "{"+key+"}";
            }
            return null;
        }
        if (DEBUG) {
            return "["+MessageFormat.format(strings.getString(key), arguments)+"]";
        }
        return MessageFormat.format(strings.getString(key), arguments);
    }
    
    /**
     * If Strings have not yet been loaded, load with default locale.
     */
    private static void loadIfNecessary() {
        if (strings == null) {
            try {
                strings = ResourceBundle.getBundle("chatty.lang.Strings", CONTROL);
            }
            catch (UnsupportedOperationException ex) {
                strings = ResourceBundle.getBundle("chatty.lang.Strings");
            }
        }
    }
    
    /**
     * Control to load properties files using UTF-8 encoding.
     * 
     * Might be caching, but shouldn't matter as long as stuff is only loaded
     * once on start.
     */
    public static final ResourceBundle.Control CONTROL = new ResourceBundle.Control() {
        
        @Override
        public List<String> getFormats(String name) {
            return FORMAT_PROPERTIES;
        }
        
        @Override
        public ResourceBundle newBundle(String baseName,
                                Locale locale,
                                String format,
                                ClassLoader loader,
                                boolean reload)
                         throws IllegalAccessException,
                                InstantiationException,
                                IOException
        {
            //System.out.println(baseName+" "+locale+" "+format+" "+reload);
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            try (InputStreamReader stream = new InputStreamReader(
                    loader.getResourceAsStream(resourceName), "UTF-8")) {
                // Load properties files as UTF-8
                bundle = new PropertyResourceBundle(stream);
            }
            return bundle;
        }
    };
    
}
