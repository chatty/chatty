
package chatty.util;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows checking the pressed status of a key.
 * 
 * @author tduva
 */
public class KeyChecker {

    private static final Object LOCK = new Object();
    private static Map<Integer, Boolean> watching;
    
    /**
     * Start watching a key. The {@link isPressed(int)} method will only detect key presses that
     * occur for the watched keycode after this method was called.
     * 
     * @param keyCode A KeyEvent keycode
     */
    public static void watch(int keyCode) {
        synchronized (LOCK) {
            init();
            watching.putIfAbsent(keyCode, Boolean.FALSE);
        }
    }
    
    /**
     * Check whether a key is currently being pressed. Will only work for keys that were registered
     * to be watched via the {@link watch(int)} method.
     * 
     * @param keyCode A KeyEvent keycode
     * @return true if the key is being pressed, false otherwise or if the key wasn't registered
     */
    public static boolean isPressed(int keyCode) {
        synchronized (LOCK) {
            return watching.getOrDefault(keyCode, Boolean.FALSE);
        }
    }
    
    /**
     * Add the listener to start tracking keys. Called in the lock.
     */
    private static void init() {
        if (watching == null) {
            watching = new HashMap<>();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

                @Override
                public boolean dispatchKeyEvent(KeyEvent e) {
                    synchronized (LOCK) {
                        if (watching.containsKey(e.getKeyCode())) {
                            switch (e.getID()) {
                                case KeyEvent.KEY_PRESSED:
                                    watching.put(e.getKeyCode(), true);
                                    break;
                                case KeyEvent.KEY_RELEASED:
                                    watching.put(e.getKeyCode(), false);
                                    break;
                            }
                        }
                        return false;
                    }
                }
            });
        }
    }
    
}
