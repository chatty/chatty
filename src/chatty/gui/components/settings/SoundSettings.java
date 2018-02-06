
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;

/**
 *
 * @author tduva
 */
class SoundSettings extends SettingsPanel {

    public SoundSettings(SettingsDialog d) {
        add(new JLabel("Sounds are integrated into 'Notifications'."),
                GuiUtil.makeGbc(0, 0, 1, 1, GridBagConstraints.CENTER));
    }
    
}
