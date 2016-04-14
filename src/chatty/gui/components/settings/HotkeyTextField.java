
package chatty.gui.components.settings;

import chatty.util.hotkeys.Hotkey;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Text field allowing the user to enter a KeyStroke.
 * 
 * @author tduva
 */
public class HotkeyTextField extends JTextField {
    
    private final HotkeyEditListener listener;
    
    private KeyStroke hotkey;

    public HotkeyTextField(int size, final HotkeyEditListener listener) {
        super(size);
        this.listener = listener;
        addKeyListener(new KeyListener() {
            
            @Override
            public void keyPressed(KeyEvent e) {
                KeyStroke newHotkey = KeyStroke.getKeyStrokeForEvent(e);
                setHotkey(newHotkey);
                listener.hotkeyEntered(newHotkey);
                e.consume();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                e.consume();
            }
        
        });
        setFocusTraversalKeysEnabled(false);
    }
    
    /**
     * Sets the given KeyStroke as the current one and updates the contents of
     * the text field accordingly.
     * 
     * @param hotkey The KeyStroke to set
     */
    public void setHotkey(KeyStroke hotkey) {
        this.hotkey = hotkey;
        if (hotkey != null) {
            setText(Hotkey.keyStrokeToText(hotkey));
        } else {
            setText("No hotkey set.");
        }
        listener.hotkeyChanged(hotkey);
    }
    
    /**
     * The current KeyStroke. This may be null if no KeyStroke was set yet and
     * the user hasn't entered one yet (or if it was set to null specifically).
     * 
     * @return The current KeyStroke or null
     */
    public KeyStroke getHotkey() {
        return hotkey;
    }
    
    public interface HotkeyEditListener {
        
        /**
         * Hotkey has been changed, either by setting it or by the user entering
         * a new one.
         * 
         * @param newHotkey The new KeyStroke
         */
        public void hotkeyChanged(KeyStroke newHotkey);
        
        /**
         * Hotkey has been changed by the user entering a new one.
         * 
         * @param newHotkey The new KeyStroke
         */
        public void hotkeyEntered(KeyStroke newHotkey);
    }
    
}
