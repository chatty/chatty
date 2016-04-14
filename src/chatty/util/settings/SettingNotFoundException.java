
package chatty.util.settings;

/**
 * Thrown when a setting was accessed that didn't exist or was of a different
 * type.
 * 
 * @author tduva
 */
public class SettingNotFoundException extends RuntimeException {

    /**
     * Creates a new instance of
     * <code>SettingNotFoundException</code> without detail message.
     */
    public SettingNotFoundException() {
    }

    /**
     * Constructs an instance of
     * <code>SettingNotFoundException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public SettingNotFoundException(String msg) {
        super(msg);
    }
}
