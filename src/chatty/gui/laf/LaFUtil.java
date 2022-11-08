
package chatty.gui.laf;

import java.awt.Color;
import java.awt.Window;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * General Look And Feel utility functions.
 * 
 * @author tduva
 */
public class LaFUtil {
    
    private static final Set<Object> setKeys = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * Change existing Look And Feel defaults. The given BiFunction must be
     * implemented to return the changed value for the given key and value, or
     * null if no change should occur.
     * <p>
     * The changes made here can be reset by calling {@link resetDefaults()}.
     *
     * @param modify 
     */
    public static void modifyDefaults(BiFunction<Object, Object, Object> modify) {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        // Make a copy to prevent concurrent modification bug
        // https://bugs.openjdk.java.net/browse/JDK-6893623
        Set<Object> keys = new HashSet<>(defaults.keySet());
        for (Object key : keys) {
            Object value = defaults.get(key);
            Object result = modify.apply(key, value);
            if (result != null) {
                putDefault(key, result);
            }
        }
    }
    
    /**
     * Put an entry into the developer defaults, which take precedence over LAF
     * and system defaults.
     * 
     * @param key
     * @param value 
     */
    public static void putDefault(Object key, Object value) {
        UIManager.getDefaults().put(key, value);
        setKeys.add(key);
    }
    
    /**
     * Set developer defaults to null that have been set through the
     * {@link putDefault(Object, Object)} or {@link modifyDefaults(BiFunction)}
     * method (since this was last called).
     */
    public static void resetDefaults() {
        for (Object key : getSetKeys()) {
            UIManager.getDefaults().put(key, null);
        }
        setKeys.clear();
    }
    
    private static Set<Object> getSetKeys() {
        synchronized(setKeys) {
            return new HashSet<>(setKeys);
        }
    }
    
    public static Color parseColor(String value, Color defaultValue) {
        try {
            String[] split = value.split(" ");
            return new Color(
                    Integer.parseInt(split[0]),
                    Integer.parseInt(split[1]),
                    Integer.parseInt(split[2]));
        }
        catch (Exception ex) {
            // Invalid format, just don't do anything
        }
        return defaultValue;
    }
    
    public static void updateLookAndFeel() {
        for (Window w : Window.getWindows()) {
            if (w.isDisplayable()) {
                SwingUtilities.updateComponentTreeUI(w);
            }
        }
    }
    
}
