
package chatty.gui.components.settings;

import chatty.gui.components.LinkLabel;
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author tduva
 */
public class NotificationSettings extends SettingsPanel {
    
    private final LinkLabel userReadPermission;
    private final JCheckBox requestFollowedStreams;
    
    private final JCheckBox useCustom;
    private final ComboLongSetting nScreen;
    private final ComboLongSetting nPosition;
    private final DurationSetting nDisplayTime;
    private final DurationSetting nMaxDisplayTime;
    private final JCheckBox userActivity;
    
    public NotificationSettings(SettingsDialog d) {
        
        GridBagConstraints gbc;
        
        JPanel main = addTitledPanel("Notification Type / Options", 1);
        
        gbc = d.makeGbc(0, 0, 4, 1, GridBagConstraints.WEST);
        useCustom = d.addSimpleBooleanSetting("useCustomNotifications",
                "Use Chatty Notifications (Tray Notifications otherwise)",
                "Select this to use more flexible notifications instead of the "
                        + "default system tray notifications.");
        useCustom.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                updateSettingsState();
            }
        });
        main.add(useCustom, gbc);
        
        main.add(new JLabel("Position:"), d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        
        Map<Long, String> nPositionOptions = new LinkedHashMap<>();
        nPositionOptions.put(Long.valueOf(0), "Top-Left");
        nPositionOptions.put(Long.valueOf(1), "Top-Right");
        nPositionOptions.put(Long.valueOf(2), "Bottom-Left");
        nPositionOptions.put(Long.valueOf(3), "Bottom-Right");
        nPosition = new ComboLongSetting(nPositionOptions);
        d.addLongSetting("nPosition", nPosition);
        gbc = d.makeGbc(1, 1, 1, 1);
        main.add(nPosition, gbc);
        
        main.add(new JLabel("Screen:"), d.makeGbc(0, 2, 1, 1, GridBagConstraints.EAST));
        
        Map<Long, String> nScreenOptions = new LinkedHashMap<>();
        nScreenOptions.put(Long.valueOf(-1), "Auto");
        nScreenOptions.put(Long.valueOf(0), "1");
        nScreenOptions.put(Long.valueOf(1), "2");
        nScreenOptions.put(Long.valueOf(2), "3");
        nScreenOptions.put(Long.valueOf(3), "4");
        nScreen = new ComboLongSetting(nScreenOptions);
        d.addLongSetting("nScreen", nScreen);
        gbc = d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST);
        main.add(nScreen, gbc);

        main.add(new JLabel("Display Time:"), d.makeGbc(2, 1, 1, 1, GridBagConstraints.EAST));
        
        nDisplayTime = new DurationSetting(3, true);
        d.addLongSetting("nDisplayTime", nDisplayTime);
        main.add(nDisplayTime, d.makeGbc(3, 1, 1, 1, GridBagConstraints.WEST));
        
        userActivity = d.addSimpleBooleanSetting("nActivity", "No User Activity:",
                "Display longer unless the mouse was recently moved");
        main.add(userActivity, d.makeGbc(2, 2, 1, 1, GridBagConstraints.EAST));
        //main.add(new JLabel("Max Display Time:"), d.makeGbc(2, 2, 1, 1, GridBagConstraints.EAST));
        
        nMaxDisplayTime = new DurationSetting(3, true);
        d.addLongSetting("nMaxDisplayTime", nMaxDisplayTime);
        main.add(nMaxDisplayTime, d.makeGbc(3, 2, 1, 1, GridBagConstraints.WEST));
        
        /**
         * Types
         */
        JPanel types = addTitledPanel("Notifications", 0);

        gbc = d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST);
        types.add(new JLabel("Highlight:"), gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1);
        ComboStringSetting hl = new ComboStringSetting(SettingConstants.requirementOptions);
        d.addStringSetting("highlightNotification", hl);
        types.add(hl, gbc);
        
        gbc = d.makeGbc(0, 2, 1, 1, GridBagConstraints.EAST);
        types.add(new JLabel("Stream Status:"), gbc);
        
        gbc = d.makeGbc(1, 2, 1, 1);
        ComboStringSetting status = new ComboStringSetting(SettingConstants.requirementOptions);
        d.addStringSetting("statusNotification", status);
        types.add(status, gbc);
        
        gbc = d.makeGbc(1, 3, 1, 1);
        types.add(d.addSimpleBooleanSetting("ignoreOfflineNotifications",
                "Don't notify about \"Stream offline\"",
                "Don't show notifications about streams going offline"), gbc);
        
        JPanel follows = addTitledPanel("Followed Streams", 2);
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        requestFollowedStreams = d.addSimpleBooleanSetting("requestFollowedStreams",
                "Request followed streams", "Allows Chatty to know "
                        + "about live streams you follow to notify you and "
                        + "display a list of them");
        follows.add(requestFollowedStreams, gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        userReadPermission = new LinkLabel("", d.getLinkLabelListener());
        follows.add(userReadPermission, gbc);
        
        
        updateSettingsState();
    }
    
    protected void setUserReadPermission(boolean enabled) {
        if (enabled) {
            userReadPermission.setText("Required access available. ([help:followed ?])");
        } else {
            userReadPermission.setText("User read access required. ([help:followed ?])");
        }
        requestFollowedStreams.setEnabled(enabled);
        
    }
    
    private void updateSettingsState() {
        boolean enabled = useCustom.isSelected();
        nPosition.setEnabled(enabled);
        nScreen.setEnabled(enabled);
        nDisplayTime.setEnabled(enabled);
        nMaxDisplayTime.setEnabled(enabled);
        userActivity.setEnabled(enabled);
    }
    
    
}
