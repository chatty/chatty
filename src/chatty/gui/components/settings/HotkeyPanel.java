
package chatty.gui.components.settings;

import chatty.lang.Language;
import chatty.util.hotkeys.Hotkey;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 *
 * @author tduva
 */
public class HotkeyPanel extends JPanel {

    private final JTextField hotkeyField = new JTextField(20);
    
    private final JButton editButton = new JButton("Edit");
    
    private final JButton removeButton = new JButton("Remove");
    
    private Hotkey currentHotkey;
    
    private Map<String, String> actions;
    
    public HotkeyPanel(JDialog owner, String actionId, Hotkey.Type type, Function<KeyStroke, Hotkey> getExistingHotkey, HotkeyHelperListener listener) {
        this.currentHotkey = new Hotkey(actionId, null, type, null, 1);
        
        add(hotkeyField);
        add(editButton);
        add(removeButton);
        
        hotkeyField.setEditable(false);
        
        editButton.addActionListener(e -> {
            HotkeyEditor.MyItemEditor editor = new HotkeyEditor.MyItemEditor(owner, getExistingHotkey);
            editor.setActions(actions);
            editor.setFixedAction();
            Hotkey edited = editor.showEditor(currentHotkey, hotkeyField, true, 0);
            if (edited != null) {
                listener.changeHotkey(currentHotkey, edited);
                updateHotkey(currentHotkey, edited);
            }
        });
        
        removeButton.addActionListener(e -> {
            if (currentHotkey.keyStroke != null) {
                listener.deleteHotkey(currentHotkey);
                setCurrentHotkey(null);
            }
        });
        
        updateState();
    }
    
    public void updateHotkey(Hotkey current, Hotkey changed) {
        if (current == null) {
            if (changed.actionId.equals(currentHotkey.actionId)) {
                setCurrentHotkey(changed);
            }
        }
        else if (current.actionId.equals(currentHotkey.actionId)) {
            if (currentHotkey.keyStroke == null) {
                setCurrentHotkey(changed);
            }
            else if (currentHotkey.keyStroke.equals(current.keyStroke)) {
                setCurrentHotkey(changed);
            }
        }
    }
    
    public void setActions(Map<String, String> actions) {
        this.actions = actions;
    }
    
    private void setCurrentHotkey(Hotkey hotkey) {
        if (hotkey == null) {
            currentHotkey = new Hotkey(currentHotkey.actionId, null, currentHotkey.type, null, currentHotkey.delay);
        }
        else {
            currentHotkey = hotkey;
        }
        updateState();
    }
    
    private void updateState() {
        if (currentHotkey == null || currentHotkey.keyStroke == null) {
            hotkeyField.setText(Language.getString("settings.hotkeys.key.empty"));
            removeButton.setEnabled(false);
        }
        else {
            hotkeyField.setText(currentHotkey.getHotkeyText());
            removeButton.setEnabled(true);
        }
    }
    
    public static interface HotkeyHelperListener {
        
        public void changeHotkey(Hotkey current, Hotkey changed);
        public void deleteHotkey(Hotkey current);
        
    }
    
}
