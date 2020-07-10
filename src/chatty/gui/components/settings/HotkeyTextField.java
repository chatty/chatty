
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.util.hotkeys.Hotkey;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Text field allowing the user to enter a KeyStroke.
 * 
 * @author tduva
 */
public class HotkeyTextField extends JPanel implements StringSetting {
    
    private final HotkeyEditListener listener;
    private final JTextField textField;
    
    private KeyStroke hotkey;

    /**
     * Create a new instance with the given size and listener.
     * 
     * @param size The number of columns of the JTextField
     * @param listener Listener to be informed of hotkey changes (optional)
     */
    public HotkeyTextField(int size, final HotkeyEditListener listener) {
        this.listener = listener;
        
        //------------
        // Text field
        //------------
        textField = new JTextField(size);
        textField.addKeyListener(new KeyListener() {
            
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
        textField.setFocusTraversalKeysEnabled(false);
        
        //--------
        // Button
        //--------
        JButton resetButton = new JButton("x");
        resetButton.addActionListener(e -> {
            setHotkey(null);
        });
        resetButton.setMargin(GuiUtil.SMALLER_BUTTON_INSETS);
        GuiUtil.matchHeight(resetButton, textField);
        
        //--------
        // Layout
        //--------
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        add(textField);
        add(resetButton);
        
        // Clear hotkey option (removed for now, since it has become redundant
        // after adding the reset button)
//        JPopupMenu menu = new JPopupMenu();
//        JMenuItem item = new JMenuItem("Clear");
//        item.addActionListener(e -> { setHotkey(null); });
//        menu.add(item);
//        textField.setComponentPopupMenu(menu);
//        textField.setToolTipText("Open context menu for options");
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
            textField.setText(Hotkey.keyStrokeToText(hotkey));
            textField.setToolTipText(String.format("Key Code: %1$d (0x%1$X)",
                    hotkey.getKeyCode()));
        } else {
            textField.setText("No hotkey set.");
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
