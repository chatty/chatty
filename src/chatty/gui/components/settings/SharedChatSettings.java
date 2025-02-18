
package chatty.gui.components.settings;

import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class SharedChatSettings extends SettingsPanel {
    
    public SharedChatSettings(SettingsDialog d) {
        super(false);
        
        JPanel base = addTitledPanel(Language.getString("settings.section.sharedChat"), 0, true);
        
        SettingsUtil.addStandardSetting(base, "sharedBadges", 0,
                                                                 d.addComboLongSetting("sharedBadges", 0, 1, 2));
        
        SettingsUtil.addStandardSetting(base, "sharedLogoSize", 1,
                                                                 d.addComboLongSetting("sharedLogoSize", 0, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30));
    }
    
}
