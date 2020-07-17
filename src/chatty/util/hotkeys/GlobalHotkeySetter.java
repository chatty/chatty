
package chatty.util.hotkeys;

import com.tulskiy.keymaster.common.Provider;
import java.util.logging.Logger;
import javax.swing.KeyStroke;

/**
 * JKeymaster (JNA) global hotkey handling.
 * 
 * @author tduva
 */
public class GlobalHotkeySetter {
    
    private static final Logger LOGGER = Logger.getLogger(GlobalHotkeySetter.class.getName());
    
    private final GlobalHotkeySetter.GlobalHotkeyListener listener;
    
    private String error = "Global hotkey error";
    private Provider hotkeys;
    private boolean anyRegistered;
    
    public GlobalHotkeySetter(GlobalHotkeySetter.GlobalHotkeyListener listener) {
        this.listener = listener;
        try {
            hotkeys = Provider.getCurrentProvider(true);
            if (hotkeys == null) {
                error = "Global hotkeys: Platform not supported";
            }
        } catch (Throwable ex) {
            LOGGER.warning("Global hotkey error: "+ex);
            error = "Global hotkey error: "+ex;
        }
    }
    
    /**
     * Returns an error message if an error occured while initializing.
     * 
     * @return The error message, or null if no error occured
     */
    public String getError() {
        return error;
    }
    
    /**
     * Whether the global hotkey provider has been initialized and is still
     * active. If false, then more information may be retrieved with
     * {@link #getError() getError}, unless the provider was intentionally
     * stopped.
     * 
     * @return true if global hotkeys can be added, false otherwise
     */
    public boolean isActive() {
        return hotkeys != null && hotkeys.isRunning();
    }
    
    /**
     * Sets a hotkey with the given id.
     * 
     * @param hotkeyId Used in the listener to notify about a hotkey press
     * @param keyStroke The hotkey to register (some key codes may not work
     * depending on the system)
     */
    public void registerHotkey(Object hotkeyId, KeyStroke keyStroke) {
        if (!isActive()) {
            return;
        }
        try {
            LOGGER.info("[Global Hotkeys] Trying to register hotkey: " + hotkeyId);
            hotkeys.register(keyStroke, h -> {
                listener.onHotkey(hotkeyId);
            });
            anyRegistered = true;
        } catch (Throwable ex) {
            // I don't think there can be an exception, but just in case
            LOGGER.info("[Global Hotkeys] Error registering hotkey: " + ex);
        }
    }
    
    /**
     * Removes all registered hotkeys.
     */
    public void unregisterAllHotkeys() {
        if (!isActive() || !anyRegistered) {
            return;
        }
        try {
            hotkeys.reset();
            anyRegistered = false;
        }
        catch (Throwable ex) {
            // I don't think there can be an exception, but just in case
            LOGGER.warning("[Global Hotkeys] Error resetting: " + ex);
        }
    }
    
    /**
     * Free up any ressources if necessary. This object can no longer be used
     * afterwards.
     */
    public void cleanUp() {
        if (!isActive()) {
            return;
        }
        try {
            hotkeys.close();
        }
        catch (Throwable ex) {
            // I don't think there can be an exception, but just in case
            LOGGER.warning("[Global Hotkeys] Error closing: " + ex);
        }
    }
    
    public interface GlobalHotkeyListener {
        public void onHotkey(Object hotkeyId);
    }
    
}
