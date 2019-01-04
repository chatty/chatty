
package chatty.gui.components.settings;

import chatty.util.hotkeys.Hotkey;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

/**
 * 
 * @author tduva
 */
public class HotkeySettings extends SettingsPanel {
    
    private final SettingsDialog d;
    private final HotkeyEditor data;
    
    public HotkeySettings(SettingsDialog d) {
        super(true);
        
        this.d = d;
        
        JPanel main = addTitledPanel("Hotkeys", 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        main.add(d.addSimpleBooleanSetting("globalHotkeysEnabled",
                "Enable global hotkeys",
                "Enable or disable currently defined global hotkeys"), gbc);
        
        data = new HotkeyEditor(d);
        data.setPreferredSize(new Dimension(1,270));
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        main.add(data, gbc);
    }
    
    public void setData(Map<String, String> actions, List<Hotkey> hotkeys,
            boolean globalHotkeysAvailable) {
        data.setData(actions, hotkeys, globalHotkeysAvailable);
    }
    
    public List<Hotkey> getData() {
        return data.getData();
    }
    
}
