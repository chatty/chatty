
package chatty.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
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

    private static ResourceBundle strings;
    
    /**
     * Set the language to use like "de" or "de-AT". Restart of Chatty is
     * required for most changes to take effect.
     * 
     * @param language 
     */
    public synchronized static void setLanguage(String language) {
        String[] split = language.split("-");
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
        strings = ResourceBundle.getBundle("chatty.lang.Strings", locale, CONTROL);
        LOGGER.info(String.format("[Localization] Loaded locale '%s' for requested '%s'",
                strings.getLocale(), language));
    }

    /**
     * Gets the String with the given key for the current language. If the key
     * wasn't found a String consisting of a single questionmark is returned.
     * 
     * @param key The key as defined in the properties files
     * @return The language specific String, or "?" if none could be found
     */
    public synchronized static String getString(String key) {
        loadIfNecessary();
        if (!strings.containsKey(key)) {
            LOGGER.warning("Missing string key: "+key);
            return "?";
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
        loadIfNecessary();
        if (!strings.containsKey(key)) {
            LOGGER.warning("Missing string key: "+key);
            return "?";
        }
        return MessageFormat.format(strings.getString(key), arguments);
    }
    
    /**
     * If Strings have not yet been loaded, load with default locale.
     */
    private static void loadIfNecessary() {
        if (strings == null) {
            strings = ResourceBundle.getBundle("chatty.lang.Strings", CONTROL);
        }
    }
    
    /**
     * Control to load properties files using UTF-8 encoding.
     */
    private static final ResourceBundle.Control CONTROL = new ResourceBundle.Control() {
        
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
