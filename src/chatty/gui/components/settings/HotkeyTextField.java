
package chatty.gui.components.settings;

import chatty.util.hotkeys.Hotkey;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Text field allowing the user to enter a KeyStroke.
 * 
 * @author tduva
 */
public class HotkeyTextField extends JTextField implements StringSetting {
    
    private final HotkeyEditListener listener;
    
    private KeyStroke hotkey;

    /**
     * Create a new instance with the given size and listener.
     * 
     * @param size The number of columns of the JTextField
     * @param listener Listener to be informed of hotkey changes (optional)
     */
    public HotkeyTextField(int size, final HotkeyEditListener listener) {
        super(size);
        this.listener = listener;
        addKeyListener(new KeyListener() {
            
            @Override
            public void keyPressed(KeyEvent e) {
                KeyStroke newHotkey = KeyStroke.getKeyStrokeForEvent(e);
                setHotkey(newHotkey);
                if (listener != null) {
                    listener.hotkeyEntered(newHotkey);
                }
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
        
        // Clear hotkey option
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem("Clear");
        item.addActionListener(e -> { setHotkey(null); });
        menu.add(item);
        setComponentPopupMenu(menu);
        setToolTipText("Open context menu for options");
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
        if (listener != null) {
            listener.hotkeyChanged(hotkey);
        }
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

    @Override
    public String getSettingValue() {
        if (hotkey == null) {
            return "";
        }
        return hotkey.toString();
    }

    @Override
    public void setSettingValue(String value) {
        setHotkey(KeyStroke.getKeyStroke(value));
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
