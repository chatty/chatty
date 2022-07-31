
package chatty.gui.components.settings;

import chatty.gui.defaults.DefaultsPanel;
import java.awt.GridBagConstraints;
import javax.swing.JOptionPane;

/**
 *
 * @author tduva
 */
public class SimpleSettings extends SettingsPanel {

    public SimpleSettings(SettingsDialog d) {
        DefaultsPanel panel = new DefaultsPanel(SettingConstants.HTML_PREFIX+"Loads selected presets for various settings when you click 'Apply'.",
                new DefaultsPanel.DefaultsHelper() {
            @Override
            public void setString(String setting, String value) {
                d.setStringSetting(setting, value);
            }

            @Override
            public void setLong(String setting, long value) {
                d.setLongSetting(setting, value);
            }

            @Override
            public void setBoolean(String setting, boolean value) {
                d.setBooleanSetting(setting, value);
            }

            @Override
            public String getStringDefault(String setting) {
                return d.settings.getStringDefault(setting);
            }

            @Override
            public boolean getBooleanDefault(String setting) {
                return d.settings.getBooleanDefault(setting);
            }

            @Override
            public boolean getEnabled(String option) {
                switch (option) {
                    case "notifications":
                        return d.settings.getLong("nType") == NotificationSettings.NOTIFICATION_TYPE_CUSTOM;
                    case "userlist":
                        return d.settings.getBoolean("userlistEnabled");
                    case "skip":
                        return true;
                }
                return false;
            }

            @Override
            public void applied() {
                JOptionPane.showMessageDialog(d, "Changes applied. You may 'Save' on the Settings Dialog now or review settings further.");
            }
        });
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        addPanel(panel, gbc);
    }
    
}
