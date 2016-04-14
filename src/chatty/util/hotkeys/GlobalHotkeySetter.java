
package chatty.util.hotkeys;

import chatty.*;
import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.JIntellitype;
import com.melloware.jintellitype.JIntellitypeException;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Actually initialize JIntellitype and register the hotkeys.
 * 
 * @author tduva
 */
public class GlobalHotkeySetter implements HotkeyListener {
    
    private static final Logger LOGGER = Logger.getLogger(GlobalHotkeySetter.class.getName());
    
    private boolean initialized = false;
    private final GlobalHotkeyListener listener;

    /**
     * Save an association between id (Integer) that JIntelliType uses and the
     * id that can be referred to by the caller (any Object).
     */
    private final Map<Integer, Object> hotkeys = new HashMap<>();
    
    /**
     * Increasing number to get ids off of for JIntelliType to use.
     */
    private int idCount;
    
    public GlobalHotkeySetter(GlobalHotkeyListener listener) {
        this.listener = listener;
        initialize();
    }
    
    /**
     * Adds a hotkey listener.
     */
    public final void initialize() {
        try {
            JIntellitype.getInstance().addHotKeyListener(this);
            initialized = true;
        } catch (JIntellitypeException ex) {
            String info = "Failed adding HotKeyListener: "+ex.getLocalizedMessage();
            LOGGER.log(Logging.USERINFO, info+" (if you don't use global "
                    + "hotkeys you can just ignore this)");
            LOGGER.warning(info);
        }
    }
    
    public boolean isInitalized() {
        return initialized;
    }
    
    /**
     * Called when global hotkey is pressed. This is run in the EDT.
     * 
     * @param i 
     */
    @Override
    public void onHotKey(int i) {
        Object hotkeyId = hotkeys.get(i);
        if (hotkeyId != null) {
            listener.onHotkey(hotkeyId);
        }
    }
    
    /**
     * Sets a hotkey with the given id. The hotkey consists of modifiers
     * (c == ctrl, s == shift, ..) and the keyCode.
     * 
     * @param hotkeyId
     * @param modifiers
     * @param keyCode
     */
    public void registerHotkey(Object hotkeyId, int modifiers, int keyCode) {
        if (!initialized) {
            return;
        }
        try {
            int id = getId(hotkeyId);
            int mod = getModFromModifiers(modifiers);
            // Remove previously under this id registered hotkey (if there is
            // any)
            JIntellitype.getInstance().unregisterHotKey(id);
            LOGGER.info("[Global Hotkeys] Trying to register hotkey: " + id + "/" + mod + "/" + keyCode);
            JIntellitype.getInstance().registerHotKey(id, mod, keyCode);
            hotkeys.put(id, hotkeyId);
        } catch (JIntellitypeException ex) {
            LOGGER.info("[Global Hotkeys] Couldn't register hotkey: " + ex);
        }
    }
    
    /**
     * Removes all registered hotkeys.
     */
    public void unregisterAllHotkeys() {
        if (!initialized) {
            return;
        }
        if (!hotkeys.isEmpty()) {
            LOGGER.info("[Global Hotkeys] Unregistering "+hotkeys.size()+" global hotkeys.");
        }
        for (Integer id : hotkeys.keySet()) {
            try {
                JIntellitype.getInstance().unregisterHotKey(id);
            } catch (JIntellitypeException ex) {
                LOGGER.info("[Global Hotkeys] Couldn't unregister hotkey: " + ex);
            }
        }
        hotkeys.clear();
        idCount = 0;
    }
    
    private int getModFromModifiers(int modifiers) {
        int mod = 0;
        if (checkModifier(modifiers, KeyEvent.CTRL_DOWN_MASK)) {
            mod += JIntellitype.MOD_CONTROL;
        }
        if (checkModifier(modifiers, KeyEvent.ALT_DOWN_MASK)) {
            mod += JIntellitype.MOD_ALT;
        }
        if (checkModifier(modifiers, KeyEvent.SHIFT_DOWN_MASK)) {
            mod += JIntellitype.MOD_SHIFT;
        }
        return mod;
    }
    
    private static boolean checkModifier(int modifiers, int modifier) {
        return (modifiers & modifier) == modifier;
    }
    
    public void cleanUp() {
        if (initialized) {
            JIntellitype.getInstance().cleanUp();
        }
    }
    
    /**
     * Find if this hotkey has an id and if not, assign a new one.
     * 
     * @param hotkeyId
     * @return 
     */
    private int getId(Object hotkeyId) {
        for (Integer id : hotkeys.keySet()) {
            if (hotkeys.get(id) == hotkeyId) {
                return id;
            }
        }
        return idCount++;
    }
    
    public interface GlobalHotkeyListener {
        public void onHotkey(Object hotkeyId);
    }

}
