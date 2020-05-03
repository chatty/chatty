
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.Helper;
import chatty.Room;
import chatty.User;
import chatty.gui.GuiUtil;
import static chatty.gui.components.settings.MessageSettings.addTimestampFormat;
import chatty.lang.Language;
import chatty.util.chatlog.ChatLog;
import chatty.util.commands.CustomCommand;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
        super(true); // Expand
        
        GridBagConstraints gbc;
        
        JPanel modePanel = createTitledPanel(Language.getString("settings.log.section.channels"));
        
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST);
        gbc.weightx = 0.4;
        modePanel.add(new JLabel(Language.getString("settings.log.loggingMode")), gbc);
        
        Map<String, String> logModeOptions = new HashMap<>();
        logModeOptions.put("always", Language.getString("settings.option.logMode.always"));
        logModeOptions.put("blacklist", Language.getString("settings.option.logMode.blacklist"));
        logModeOptions.put("whitelist", Language.getString("settings.option.logMode.whitelist"));
        logModeOptions.put("off", Language.getString("settings.option.logMode.off"));
        modeSetting = d.addComboStringSetting("logMode", 1, false, logModeOptions);
        modeSetting.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                update();
            }
        });
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        gbc.weightx = 0.6;
        modePanel.add(modeSetting, gbc);
        
        // Lists
        cardManager = new CardLayout();
        cards = new JPanel(cardManager);
        cards.setPreferredSize(new Dimension(220,130));
        
        final ChannelFormatter formatter = new ChannelFormatter();
        ListSelector whitelist = d.addListSetting("logWhitelist", "Chatlog Whitelist", 1, 1, true, true);
        whitelist.setDataFormatter(formatter);
        ListSelector blacklist = d.addListSetting("logBlacklist", "Chatlog Blacklist", 1, 1, true, true);
        blacklist.setDataFormatter(formatter);
        
        cards.add(whitelist, "whitelist");
        cards.add(blacklist, "blacklist");
        JPanel empty = new JPanel(new GridBagLayout());
        JLabel emptyLabel = new JLabel(Language.getString("settings.log.noList"));
        emptyLabel.setForeground(Color.gray);
        empty.add(emptyLabel, d.makeGbc(0,0,1,1));
        cards.add(empty, "none");
        
        gbc = d.makeGbc(0, 1, 2, 1);
        gbc.insets = new Insets(5,10,5,5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        modePanel.add(cards, gbc);
        
        // Info Text
        info = new JLabel();
        gbc = d.makeGbc(0, 2, 3, 1);
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        modePanel.add(info, gbc);
        
        JPanel typesPanel = createTitledPanel("Message Types");
        
        JCheckBox logMessages = d.addSimpleBooleanSetting("logMessage");
        JCheckBox logIgnored = d.addSimpleBooleanSetting("logIgnored");
        EditorStringSetting messageTemplate = d.addEditorStringSetting("logMessageTemplate", -1, true,
                Language.getString("settings.boolean.logMessage.template"),
                false,
                SettingConstants.HTML_PREFIX+SettingsUtil.getInfo("info-logmessagetemplate.html", null),
                new Editor.Tester() {

            @Override
            public String test(Window parent, Component component, int x, int y, String value) {
                CustomCommand command = CustomCommand.parse(value);
                if (command.hasError()) {
                    CommandSettings.showCommandInfoPopup(component, command);
                }
                else {
                    // Regular
                    User user = new User("testuser", "テストユーザー", Room.createRegular("#testchannel"));
                    user.setId("123456");
                    user.setSubscriber(true);
                    Map<String, String> badges = new LinkedHashMap<>();
                    badges.put("subscriber", "12");
                    user.setTwitchBadges(badges);
                    String normalResult = command.replace(ChatLog.messageParam(
                            user,
                            "Hello, good day! :)",
                            false,
                            d.settings,
                            "[12:34:56]"));
                    
                    // Action
                    String actionResult = command.replace(ChatLog.messageParam(
                            user,
                            "has arrived! ;)",
                            true,
                            d.settings,
                            "[12:34:56]"));
                    
                    // More badges
                    badges.clear();
                    badges.put("vip", "1");
                    badges.put("founder", "0");
                    badges.put("premium", "1");
                    user = new User("TestName", Room.createRegular("#testchannel"));
                    user.setId("123457");
                    user.setTwitchBadges(badges);
                    user.setVip(true);
                    user.setSubscriber(true);
                    user.setTurbo(true);
                    String badgesResult = command.replace(ChatLog.messageParam(
                            user,
                            "HeyGuys",
                            false,
                            d.settings,
                            "[12:34:56]"));
                    
                    // No badges
                    user = new User("testname", "TestName", Room.createRegular("#testchannel"));
                    user.setId("123457");
                    String noBadgesResult = command.replace(ChatLog.messageParam(
                            user,
                            "HeyGuys",
                            false,
                            d.settings,
                            "[12:34:56]"));
                    
                    GuiUtil.showNonModalMessage(parent, "Example",
                            String.format("Regular message:<br />%s<br /><br />"
                                    + "Action message:<br />%s<br /><br />"
                                    + "More badges, no localized name:<br />%s<br /><br />"
                                    + "No badges:<br />%s<br /><br />"
                                    + "(The timestamp may not represent your current log timestamp setting.)",
                                    Helper.htmlspecialchars_encode(normalResult),
                                    Helper.htmlspecialchars_encode(actionResult),
                                    Helper.htmlspecialchars_encode(badgesResult),
                                    Helper.htmlspecialchars_encode(noBadgesResult)),
                            JOptionPane.INFORMATION_MESSAGE, true);
                }
                return null;
            }
        });
        
        SettingsUtil.addSubsettings(logMessages, logIgnored, messageTemplate);
        
        typesPanel.add(logMessages,
                d.makeGbcCloser(0, 0, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(messageTemplate,
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(logIgnored,
                d.makeGbcCloser(0, 2, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logInfo"),
                d.makeGbcCloser(0, 3, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logBan"),
                d.makeGbcCloser(0, 4, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logDeleted"),
                d.makeGbcCloser(0, 5, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logMod"),
                d.makeGbcCloser(0, 6, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logJoinPart"),
                d.makeGbcCloser(0, 7, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logSystem"),
                d.makeGbcCloser(0, 8, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logViewerstats"),
                d.makeGbcCloser(0, 9, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logViewercount"),
                d.makeGbcCloser(0, 10, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logModAction"),
                d.makeGbcCloser(0, 11, 1, 1, GridBagConstraints.WEST));
        typesPanel.add(d.addSimpleBooleanSetting("logBits"),
                d.makeGbcCloser(0, 12, 1, 1, GridBagConstraints.WEST));

        JPanel otherSettings = createTitledPanel(Language.getString("settings.log.section.other"));
        
        PathSetting logPath = new PathSetting(d, Chatty.getUserDataDirectory()+"logs");
        d.addStringSetting("logPath", logPath);
        otherSettings.add(new JLabel(Language.getString("settings.log.folder")),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.NORTHWEST));
        gbc = d.makeGbc(1, 0, 2, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.9;
        gbc.insets.bottom += 4;
        otherSettings.add(logPath, gbc);

        final Map<String,String> organizationOptions = new LinkedHashMap<>();
        organizationOptions.put("never", Language.getString("settings.option.logSplit.never"));
        organizationOptions.put("daily", Language.getString("settings.option.logSplit.daily"));
        organizationOptions.put("weekly", Language.getString("settings.option.logSplit.weekly"));
        organizationOptions.put("monthly", Language.getString("settings.option.logSplit.monthly"));
        ComboStringSetting organizationCombo = new ComboStringSetting(organizationOptions);
        organizationCombo.setEditable(false);
        d.addStringSetting("logSplit", organizationCombo);

        otherSettings.add(new JLabel(Language.getString("settings.log.splitLogs")), d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        otherSettings.add(organizationCombo,
                d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));

        otherSettings.add(d.addSimpleBooleanSetting("logSubdirectories"),
                d.makeGbcCloser(2, 1, 1, 1, GridBagConstraints.WEST));

        final Map<String,String> timestampOptions = new LinkedHashMap<>();
        timestampOptions.put("off", Language.getString("settings.option.logTimestamp.off"));
        addTimestampFormat(timestampOptions, "[HH:mm:ss]");
        addTimestampFormat(timestampOptions, "[hh:mm:ss a]");
        addTimestampFormat(timestampOptions, "[hh:mm:ssa]");
        addTimestampFormat(timestampOptions, "[yyyy-MM-dd HH:mm:ss]");
        addTimestampFormat(timestampOptions, "[yyyy-MM-dd hh:mm:ss a]");
        addTimestampFormat(timestampOptions, "[yyyy-MM-dd hh:mm:ssa]");
        ComboStringSetting timestampCombo = new ComboStringSetting(timestampOptions);
        timestampCombo.setEditable(false);
        d.addStringSetting("logTimestamp", timestampCombo);

        otherSettings.add(new JLabel(Language.getString("settings.log.timestamp")),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        otherSettings.add(timestampCombo,
                d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));
        
        otherSettings.add(d.addSimpleBooleanSetting("logLockFiles"),
                d.makeGbcCloser(2, 2, 1, 1, GridBagConstraints.WEST));
        
        /**
         * Add panels to the dialog
         */
        gbc = getGbc(0);
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.BOTH;
        addPanel(modePanel, gbc);
        
        gbc = getGbc(0);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridx = 1;
        addPanel(typesPanel, gbc);
        
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
                infoText = Language.getString("settings.log.offInfo");
                switchTo = "none";
                break;
            case "always":
                infoText = Language.getString("settings.log.alwaysInfo");
                switchTo = "none";
                break;
            case "blacklist":
                infoText = Language.getString("settings.log.blacklistInfo");
                switchTo = "blacklist";
                break;
            case "whitelist":
                infoText = Language.getString("settings.log.whitelistInfo");
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
