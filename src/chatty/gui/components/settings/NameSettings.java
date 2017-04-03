
package chatty.gui.components.settings;

import chatty.SettingsManager;
import java.awt.GridBagConstraints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class NameSettings extends SettingsPanel {
    
    private final SimpleTableEditor customNamesEditor;
    
    public NameSettings(SettingsDialog d) {
        
        GridBagConstraints gbc;
        
        JPanel main = addTitledPanel("Names / Localized Names", 0);
        
        Map<Long, String> displayNamesModeSettings = new LinkedHashMap<>();
        displayNamesModeSettings.put(SettingsManager.DISPLAY_NAMES_MODE_USERNAME, "Username Only");
        displayNamesModeSettings.put(SettingsManager.DISPLAY_NAMES_MODE_CAPITALIZED, "Capitalized Only");
        displayNamesModeSettings.put(SettingsManager.DISPLAY_NAMES_MODE_LOCALIZED, "Localized Only");
        displayNamesModeSettings.put(SettingsManager.DISPLAY_NAMES_MODE_BOTH, "Localized+Username");
        
        ComboLongSetting displayNamesMode = new ComboLongSetting(displayNamesModeSettings);
        d.addLongSetting("displayNamesMode", displayNamesMode);

        main.add(new JLabel("Chat:"),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST));
        main.add(displayNamesMode,
                d.makeGbc(1, 0, 2, 1, GridBagConstraints.WEST));
        
        ComboLongSetting displayNamesModeUserlist = new ComboLongSetting(displayNamesModeSettings);
        d.addLongSetting("displayNamesModeUserlist", displayNamesModeUserlist);
        
        
        main.add(new JLabel("Userlist:"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        main.add(displayNamesModeUserlist,
                d.makeGbc(1, 1, 2, 1, GridBagConstraints.WEST));
        
        main.add(new JLabel("<html><body style=\"width:300px\">Note: "
                + "Localized/Capitalized names in the Userlist are only "
                + "available if that user said something in chat."),
                d.makeGbc(0, 2, 3, 1));
        
        
        main.add(d.addSimpleBooleanSetting("capitalizedNames", "Capitalize First Letter if no display name available",
                "Requires a restart of Chatty to have any effect."),
                d.makeGbc(0, 3, 3, 1, GridBagConstraints.WEST));

        
        JPanel custom = addTitledPanel("Custom Names", 1, true);
        customNamesEditor = d.addStringMapSetting("customNames", 270, 200);
        customNamesEditor.setKeyFilter("[^\\w]");
        
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        custom.add(customNamesEditor, gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1);
        gbc.anchor = GridBagConstraints.NORTH;
        custom.add(new JLabel("<html><body style='width:100px'>"
                + "Define custom names for individual users which are displayed "
                + "in the chat and userlist.<br /><br />"
                + "You can also open these settings through the User Context"
                + "Menu (right-click on name in chat, <code>Miscellaneous -> Set name</code>)."), gbc);
    }
    
    public void editCustomName(String item) {
        customNamesEditor.edit(item);
    }
    
}
