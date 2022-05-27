
package chatty.gui.components.settings;

import chatty.gui.components.LinkLabel;
import chatty.gui.components.LiveStreamsDialog;
import chatty.gui.components.LiveStreamsDialog.OpenAction;
import chatty.lang.Language;
import chatty.util.commands.CustomCommand;
import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class LiveStreamsSettings extends SettingsPanel {
    
    private final LinkLabel userReadPermission;
    private final JCheckBox requestFollowedStreams;

    public LiveStreamsSettings(SettingsDialog d) {
        
        JPanel listSettings = addTitledPanel(Language.getString("settings.section.liveStreams"), 0, false);
        JPanel gameFavorites = addTitledPanel(Language.getString("settings.section.gameFavorites"), 1, false);
        JPanel follows = addTitledPanel("Followed Streams", 2);
        
        GridBagConstraints gbc;
        
        //--------------------------
        // Streams Dialog
        //--------------------------
        Map<String, String> openActionOptions = new LinkedHashMap<>();
        for (LiveStreamsDialog.OpenAction a : LiveStreamsDialog.OpenAction.values()) {
            openActionOptions.put(a.key, a.getLabel());
        }
        ComboStringSetting streamsAction = d.addComboStringSetting("liveStreamsAction", 10, false, openActionOptions);
        SettingsUtil.addLabeledComponent(listSettings, "liveStreamsAction", 0, 0, 1, GridBagConstraints.EAST, streamsAction);
        
        EditorStringSetting openCommand = d.addEditorStringSetting("liveStreamsCommand", 20, true, "Custom Command", false,
                "<code>$1-</code> contains all selected streams ([help-commands: Custom Commands Help], [url:https://www.youtube.com/watch?v=nU5AS8e9dLw Video Tutorial])",
                (parent, component, x, y, value) -> {
                    CustomCommand parsedCommand = CustomCommand.parse(value);
                    CommandSettings.showCommandInfoPopup(component, parsedCommand);
                    return null;
                });
        openCommand.setLinkLabelListener(d.getLinkLabelListener());
        SettingsUtil.addLabeledComponent(listSettings, "liveStreamsCommand", 0, 1, 1, GridBagConstraints.EAST, openCommand, true);
        
        SettingsUtil.addSubsettings(streamsAction, (t) -> {
            return t.equals(OpenAction.COMMAND.key);
        }, openCommand);
        
        SimpleBooleanSetting notificationAction = d.addSimpleBooleanSetting("liveStreamsNotificationAction");
        listSettings.add(notificationAction,
                SettingsDialog.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(streamsAction, (t) -> {
            return !t.equals(OpenAction.INFO.key);
        }, notificationAction);
        
        listSettings.add(d.addSimpleBooleanSetting("liveStreamsChatIcon"),
                SettingsDialog.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));
        
        listSettings.add(new JLabel(SettingConstants.HTML_PREFIX
            +"More options are available in the Live Streams list context menu."),
            SettingsDialog.makeGbc(0, 4, 2, 1));
        
        //--------------------------
        // Game Favorites
        //--------------------------
        gameFavorites.add(d.addListSetting("gameFavorites", Language.getString("settings.section.gameFavorites"), 250, 200, false, true),
                SettingsDialog.makeGbc(0, 0, 1, 1));
        
        gameFavorites.add(new JLabel("<html><body style='width:200px'>"
                + "The Live Streams list shows an icon for favorited games and "
                + "optionally allows them to be sorted first.<br /><br />"
                + "Games can also be added/removed through the Live Streams "
                + "list context menu, which ensures the correct game name is "
                + "added."),
                SettingsDialog.makeGbc(1, 0, 1, 1, GridBagConstraints.NORTHEAST));
        
        //--------------------------
        // Followed Streams
        //--------------------------
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        requestFollowedStreams = d.addSimpleBooleanSetting("requestFollowedStreams",
                "Request followed streams", "Allows Chatty to know "
                        + "about live streams you follow to notify you and "
                        + "display a list of them");
        follows.add(requestFollowedStreams, gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        userReadPermission = new LinkLabel("", d.getLinkLabelListener());
        follows.add(userReadPermission, gbc);
    }
    
    protected void setUserReadPermission(boolean enabled) {
        if (enabled) {
            userReadPermission.setText("Required access available. ([help:followed ?])");
        } else {
            userReadPermission.setText("Followed streams access required. ([help:followed ?])");
        }
    }
    
}
