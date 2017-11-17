
package chatty.gui.components.settings;

import chatty.Chatty;
import static chatty.gui.components.settings.MessageSettings.addTimestampFormat;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class LogSettings extends SettingsPanel {
    
    private final JLabel info;
    private final ComboStringSetting modeSetting;
    private final CardLayout cardManager;
    private final JPanel cards;
    
    public LogSettings(final SettingsDialog d) {
        
        GridBagConstraints gbc;
        
        JPanel mode = createTitledPanel("Channels to log to file");
        
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST);
        gbc.weightx = 0.4;
        mode.add(new JLabel("Logging Mode: "), gbc);
        
        modeSetting = d.addComboStringSetting("logMode", 1, false, new String[]{"always", "blacklist", "whitelist", "off"});
        modeSetting.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                update();
            }
        });
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        gbc.weightx = 0.6;
        mode.add(modeSetting, gbc);
        
        // Lists
        cardManager = new CardLayout();
        cards = new JPanel(cardManager);
        cards.setPreferredSize(new Dimension(220,130));
        
        final ChannelFormatter formatter = new ChannelFormatter();
        ListSelector whitelist = d.addListSetting("logWhitelist", 1, 1, true, true);
        whitelist.setDataFormatter(formatter);
        ListSelector blacklist = d.addListSetting("logBlacklist", 1, 1, true, true);
        blacklist.setDataFormatter(formatter);
        
        cards.add(whitelist, "whitelist");
        cards.add(blacklist, "blacklist");
        JPanel empty = new JPanel(new GridBagLayout());
        JLabel emptyLabel = new JLabel("<No List in this mode>");
        emptyLabel.setForeground(Color.gray);
        empty.add(emptyLabel, d.makeGbc(0,0,1,1));
        cards.add(empty, "none");
        
        gbc = d.makeGbc(0, 1, 2, 1);
        gbc.insets = new Insets(5,10,5,5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        mode.add(cards, gbc);
        
        // Info Text
        info = new JLabel();
        gbc = d.makeGbc(0, 2, 3, 1);
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        mode.add(info, gbc);
        
        JPanel types = createTitledPanel("Message Types");
        
        types.add(d.addSimpleBooleanSetting(
                "logInfo",
                "Chat Info",
                "Log infos like stream title, messages from twitch, connecting, disconnecting."),
                d.makeGbcCloser(0, 0, 1, 1, GridBagConstraints.NORTHWEST));
        types.add(d.addSimpleBooleanSetting(
                "logBan",
                "Bans/Timeouts",
                "Log Bans/Timeouts as BAN messages."),
                d.makeGbcCloser(0, 1, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting(
                "logMod",
                "Mod/Unmod",
                "Log MOD/UNMOD messages."),
                d.makeGbcCloser(0, 2, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting(
                "logJoinPart",
                "Joins/Parts",
                "Log JOIN/PART messages."),
                d.makeGbcCloser(0, 3, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting(
                "logSystem",
                "System Info",
                "Messages that concern Chatty rather than chat."),
                d.makeGbcCloser(0, 4, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting(
                "logViewerstats",
                "Viewerstats",
                "Log viewercount stats in a semi-regular interval."),
                d.makeGbcCloser(0, 5, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting(
                "logViewercount",
                "Viewercount",
                "Log the viewercount as it is updated."),
                d.makeGbcCloser(0, 6, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting(
                "logModAction",
                "Mod Actions",
                "Log who performed which command (only your own channel)."),
                d.makeGbcCloser(0, 7, 1, 1, GridBagConstraints.WEST));
        types.add(d.addSimpleBooleanSetting(
                "logIgnored",
                "Ignored Msg.",
                "Log messages ignored by the ignore list."),
                d.makeGbcCloser(0, 8, 1, 1, GridBagConstraints.WEST));


        JPanel otherSettings = createTitledPanel("Other Settings");
        
        PathSetting logPath = new PathSetting(d, Chatty.getUserDataDirectory()+"logs");
        d.addStringSetting("logPath", logPath);
        otherSettings.add(new JLabel("Folder:"),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.NORTHWEST));
        gbc = d.makeGbc(1, 0, 2, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.9;
        gbc.insets.bottom += 4;
        otherSettings.add(logPath, gbc);

        final Map<String,String> organizationOptions = new LinkedHashMap<>();
        organizationOptions.put("never", "Never");
        organizationOptions.put("daily", "Daily");
        organizationOptions.put("weekly", "Weekly");
        organizationOptions.put("monthly", "Monthly");
        ComboStringSetting organizationCombo = new ComboStringSetting(organizationOptions);
        organizationCombo.setEditable(false);
        d.addStringSetting("logSplit", organizationCombo);

        otherSettings.add(new JLabel("Split Logs:"), d.makeGbc(0, 1, 1, 1));
        otherSettings.add(organizationCombo,
                d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));

        otherSettings.add(d.addSimpleBooleanSetting(
                "logSubdirectories",
                "Channel Subdirectories",
                "Organize logs into channel subdirectories."),
                d.makeGbcCloser(2, 1, 1, 1, GridBagConstraints.WEST));

        final Map<String,String> timestampOptions = new LinkedHashMap<>();
        addTimestampFormat(timestampOptions, "off");
        addTimestampFormat(timestampOptions, "[HH:mm:ss]");
        addTimestampFormat(timestampOptions, "[hh:mm:ss a]");
        addTimestampFormat(timestampOptions, "[hh:mm:ssa]");
        addTimestampFormat(timestampOptions, "[yyyy-MM-dd HH:mm:ss]");
        addTimestampFormat(timestampOptions, "[yyyy-MM-dd hh:mm:ss a]");
        addTimestampFormat(timestampOptions, "[yyyy-MM-dd hh:mm:ssa]");
        ComboStringSetting timestampCombo = new ComboStringSetting(timestampOptions);
        timestampCombo.setEditable(false);
        d.addStringSetting("logTimestamp", timestampCombo);

        otherSettings.add(new JLabel("Timestamp:"),
                d.makeGbc(0, 2, 1, 1));
        otherSettings.add(timestampCombo,
                d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));
        
        otherSettings.add(d.addSimpleBooleanSetting(
                "logLockFiles",
                "Lock files while writing",
                "Gets exclusive access to logfiles to ensure no other program writes to it."),
                d.makeGbcCloser(2, 2, 1, 1, GridBagConstraints.WEST));
        
        /**
         * Add panels to the dialog
         */
        gbc = getGbc(0);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.BOTH;
        addPanel(mode, gbc);
        
        gbc = getGbc(0);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridx = 1;
        addPanel(types, gbc);
        
        gbc = getGbc(2);
        gbc.gridwidth = 2;
        addPanel(otherSettings, gbc);

        update();
    }
    
    private void update() {
        String mode = modeSetting.getSettingValue();
        String infoText = "";
        String switchTo = "none";
        switch (mode) {
            case "off":
                infoText = "Nothing is logged.";
                switchTo = "none";
                break;
            case "always":
                infoText = "All channels are logged.";
                switchTo = "none";
                break;
            case "blacklist":
                infoText = "All channels but those on the list are logged.";
                switchTo = "blacklist";
                break;
            case "whitelist":
                infoText = "Only the channels on the list are logged.";
                switchTo = "whitelist";
                break;
        }
        info.setText("<html><body style='width: 200px;text-align:center;'>"+infoText);
        cardManager.show(cards, switchTo);
    }
    
    private static class ChannelFormatter implements DataFormatter<String> {

        /**
         * Prepends the input with a "#" if not already present. Returns
         * {@code null} if the length after prepending is only 1, which means
         * it only consists of the "#" and is invalid.
         * 
         * @param input The input to be formatted
         * @return The formatted input, which has the "#" prepended, or
         * {@code null} or any empty String if the input was invalid
         */
        @Override
        public String format(String input) {
            if (input != null && !input.isEmpty() && !input.startsWith("#")) {
                input = "#"+input;
            }
            if (input.length() == 1) {
                input = null;
            }
            return input;
        }
        
    }
    
}
