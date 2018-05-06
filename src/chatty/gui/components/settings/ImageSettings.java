
package chatty.gui.components.settings;

import chatty.lang.Language;
import chatty.util.api.usericons.Usericon;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.List;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class ImageSettings extends SettingsPanel {
    
    private final UsericonEditor usericonsData;
    
    public ImageSettings(SettingsDialog d) {
        super(true);
        
        GridBagConstraints gbc;
        
        JPanel usericons = addTitledPanel(Language.getString("settings.section.usericons"), 0, true);

        JCheckBox usericonsEnabled = d.addSimpleBooleanSetting("usericonsEnabled");
        JCheckBox botBadgeEnabled = d.addSimpleBooleanSetting("botBadgeEnabled");
        JCheckBox customUsericonsEnabled = d.addSimpleBooleanSetting("customUsericonsEnabled",
                "Enable Custom Usericons (table)", "");
        
        //==================
        // General Settings
        //==================
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        usericons.add(usericonsEnabled, gbc);

        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        usericons.add(botBadgeEnabled, gbc);
        
        //==================
        // Custom Usericons
        //==================
        gbc = d.makeGbcSub(0, 1, 1, 1, GridBagConstraints.WEST);
        usericons.add(customUsericonsEnabled, gbc);
        
        usericonsData = new UsericonEditor(d, d.getLinkLabelListener());
        usericonsData.setPreferredSize(new Dimension(150, 250));
        gbc = d.makeGbc(0, 2, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        usericons.add(usericonsData, gbc);
        
        gbc = d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST);
        usericons.add(new JLabel("Tip: Add a Usericon with no image to hide badges of that type"), gbc);
        
        // Usericons enabled state
        customUsericonsEnabled.setEnabled(false);
        botBadgeEnabled.setEnabled(false);
        usericonsEnabled.addItemListener(e -> {
            customUsericonsEnabled.setEnabled(usericonsEnabled.isSelected());
            botBadgeEnabled.setEnabled(usericonsEnabled.isSelected());
        });
    }
    
    public void setData(List<Usericon> data) {
        usericonsData.setData(data);
    }
    
    public void setTwitchBadgeTypes(Set<String> data) {
        usericonsData.setTwitchBadgeTypes(data);
    }
    
    public List<Usericon> getData() {
        return usericonsData.getData();
    }
    
    public void addUsericonOfBadgeType(Usericon.Type type, String idVersion) {
        usericonsData.addUsericonOfBadgeType(type, idVersion);
    }
    
}
