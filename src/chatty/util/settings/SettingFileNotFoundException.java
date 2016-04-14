
package chatty.util.settings;

/**
 * Thrown when a setting is set to a file that wasn't added to the Settings.
 * 
 * @author tduva
 */
public class SettingFileNotFoundException extends RuntimeException {
 
    public SettingFileNotFoundException(String message) {
        super(message);
    }
}
