
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
        
        JPanel usericons = addTitledPanel(Language.getString("settings.section.usericons"), 0, false);
        JPanel custom = addTitledPanel(Language.getString("settings.section.customUsericons"), 1, true);

        JCheckBox usericonsEnabled = d.addSimpleBooleanSetting("usericonsEnabled");
        JCheckBox botBadgeEnabled = d.addSimpleBooleanSetting("botBadgeEnabled");
        JCheckBox customUsericonsEnabled = d.addSimpleBooleanSetting("customUsericonsEnabled");
        
        //==================
        // General Settings
        //==================
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        usericons.add(usericonsEnabled, gbc);

        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        usericons.add(botBadgeEnabled, gbc);
        
        gbc = d.makeGbc(0, 1, 2, 1, GridBagConstraints.CENTER);
        usericons.add(new JLabel(Language.getString("settings.ffzBadgesInfo")), gbc);
        
        //==================
        // Custom Usericons
        //==================
        gbc = d.makeGbcSub(0, 1, 1, 1, GridBagConstraints.WEST);
        custom.add(customUsericonsEnabled, gbc);
        
        usericonsData = new UsericonEditor(d, d.getLinkLabelListener());
        usericonsData.setPreferredSize(new Dimension(150, 250));
        gbc = d.makeGbc(0, 2, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        custom.add(usericonsData, gbc);
        
        gbc = d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST);
        custom.add(new JLabel(Language.getString("settings.customUsericons.info")), gbc);
        
        SettingsUtil.addSubsettings(usericonsEnabled, customUsericonsEnabled, botBadgeEnabled);
        SettingsUtil.addSubsettings(customUsericonsEnabled, usericonsData);
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
