
package chatty.util.hotkeys;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 * A hotkey containing an actionId referring to the action to be performed, a
 * key combination and some more meta data.
 * 
 * @author tduva
 */
public class Hotkey {
    
    /**
     * The type of the hotkey.
     */
    public enum Type {
        REGULAR(0, "Regular"),
        APPLICATION(1, "App"),
        GLOBAL(2, "Global"),
        UNDEFINED(-1, "undefined");
        
        public int id;
        public String name;
        
        Type(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public static Type getTypeFromId(int typeId) {
            for (Type type : values()) {
                if (type.id == typeId) {
                    return type;
                }
            }
            return UNDEFINED;
        }
    }
    
    public final String actionId;
    public final String custom;
    public final KeyStroke keyStroke;
    public final Type type;
    public final int delay;
    
    /**
     * The time of the last execution of the action for this hotkey, caused by
     * this hotkey. Based on calls to the shouldExecuteAction() method.
     */
    private long lastActionExecuted;
    
    public Hotkey(String actionId, KeyStroke hotkey, Type type, String custom, int delay) {
        this.actionId = actionId;
        this.keyStroke = hotkey;
        this.type = type;
        this.custom = custom;
        this.delay = delay;
    }
    
    /**
     * Returns a properly readable String for the key combination of this
     * Hotkey.
     *
     * @return 
     */
    public String getHotkeyText() {
        return keyStrokeToText(keyStroke);
    }
    
    /**
     * Checks whether an action should currently be executed, based on the
     * required delay. Also assumes that an action was executed if this returns
     * true and sets the time of the last action to the current time.
     * 
     * @return true if an action is allowed to be executed, false otherwise
     */
    public boolean shouldExecuteAction() {
        if (delay <= 0) {
            return true;
        }
        long timePassed = System.currentTimeMillis() - lastActionExecuted;
        if (timePassed > delay*100) {
            lastActionExecuted = System.currentTimeMillis();
            return true;
        }
        return false;
    }
    
    public boolean hasValidCode() {
        return keyStroke.getKeyCode() != KeyEvent.VK_UNDEFINED;
    }
    
    /**
     * Turns a KeyStroke into a readable String.
     * 
     * @param keyStroke The KeyStroke to turn into a String
     * @return The String
     */
    public static String keyStrokeToText(KeyStroke keyStroke) {
        String mod = KeyEvent.getKeyModifiersText(keyStroke.getModifiers());
        String key = KeyEvent.getKeyText(keyStroke.getKeyCode());
        if (mod.isEmpty() || mod.equals(key)) {
            return key;
        }
        return mod+"+"+key;
    }
    
    /**
     * Somewhat readable summary of this object for debugging.
     * 
     * @return 
     */
    @Override
    public String toString() {
        if (custom != null && !custom.isEmpty()) {
            return String.format("%s(%s)[%s][%s/%d]",
                    actionId,
                    custom,
                    keyStrokeToText(keyStroke),
                    type,
                    delay);
        }
        return String.format("%s[%s][%s/%d]",
                    actionId,
                    keyStrokeToText(keyStroke),
                    type,
                    delay);
    }
    
}
