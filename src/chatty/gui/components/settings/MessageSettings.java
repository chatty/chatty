
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.util.DateTime;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class MessageSettings extends SettingsPanel {
    
    
    private final Set<JCheckBox> timeoutMessageSettings = new HashSet<>();
    
    public MessageSettings(final SettingsDialog d) {

        GridBagConstraints gbc;

        JPanel timeoutSettingsPanel = addTitledPanel("Deleted Messages (Timeouts/Bans)", 0);
        JPanel otherSettingsPanel = addTitledPanel("Other", 1);

        /*
         * Other settings (Panel)
         */
        // Timestamp
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        otherSettingsPanel.add(new JLabel("Timestamp: "), gbc);

        gbc = d.makeGbc(1, 0, 1, 1);
//        gbc.anchor = GridBagConstraints.WEST;
//        String[] options = new String[]{"off", "[HH:mm:ss]", "[HH:mm]"};
//        otherSettingsPanel.add(
//                d.addComboStringSetting("timestamp", 15, false, options),
//                gbc);
        
        final Map<String,String> timestampOptions = new LinkedHashMap<>();
        addTimestampFormat(timestampOptions, "off");
        addTimestampFormat(timestampOptions, "[HH:mm:ss]");
        addTimestampFormat(timestampOptions, "[HH:mm]");
        addTimestampFormat(timestampOptions, "[hh:mm:ss a]");
        addTimestampFormat(timestampOptions, "[hh:mm a]");
        addTimestampFormat(timestampOptions, "[h:mm a]");
        addTimestampFormat(timestampOptions, "[hh:mm:ssa]");
        addTimestampFormat(timestampOptions, "[hh:mma]");
        addTimestampFormat(timestampOptions, "[h:mma]");
        ComboStringSetting combo = new ComboStringSetting(timestampOptions);
        combo.setEditable(false);
        d.addStringSetting("timestamp", combo);
        otherSettingsPanel.add(combo, gbc);
        
        final JDialog capitalizedNamesSettingsDialog = new CapitalizedNamesSettings(d);
        JButton openCapitalizedNamesSettingsDialogButton = new JButton("Name Capitalization Settings..");
        openCapitalizedNamesSettingsDialogButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        openCapitalizedNamesSettingsDialogButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                capitalizedNamesSettingsDialog.setLocationRelativeTo(d);
                capitalizedNamesSettingsDialog.setVisible(true);
            }
        });
        otherSettingsPanel.add(openCapitalizedNamesSettingsDialogButton, d.makeGbc(2, 0, 2, 1));
        

//        gbc = d.makeGbc(0, 3, 2, 1);
//        gbc.anchor = GridBagConstraints.WEST;
//        otherSettingsPanel.add(
//                ,
//                gbc);
        
//        gbc = d.makeGbc(2, 3, 2, 1);
//        gbc.anchor = GridBagConstraints.WEST;
//        otherSettingsPanel.add(
//                ,
//                gbc);

        gbc = d.makeGbc(0, 1, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        otherSettingsPanel.add(
                d.addSimpleBooleanSetting("showModMessages", "Show mod/unmod messages",
                        "Whether to show when someone was modded/unmodded or a "
                                + "mod joined/left the channel."),
                gbc);
        
        gbc = d.makeGbc(2, 1, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        otherSettingsPanel.add(
                d.addSimpleBooleanSetting("showJoinsParts", "Show joins/parts",
                        "Show users joining/parting the channel (only with "
                                + "Userlist Connection enabled, see Advanced "
                                + "settings)"),
                gbc);

        
        
                        
        gbc = d.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST);
        otherSettingsPanel.add(d.addSimpleBooleanSetting("actionColored", "/me messages colored",
                "If enabled, action messages (/me) have the same color as the nick"), gbc);
        
//                gbc = d.makeGbc(2, 2, 2, 1, GridBagConstraints.WEST);
//        otherSettingsPanel.add(d.addSimpleBooleanSetting("filterCombiningCharacters", "Filter combining characters",
//                "Tries to filter out combining characters that are used to create vertical text in some languages (may prevent errors)"), gbc);
        
        otherSettingsPanel.add(new JLabel("Filter combining chars:"),
                d.makeGbc(2, 2, 1, 1));

        Map<Long, String> filterSetting = new LinkedHashMap<>();
        filterSetting.put(Long.valueOf(Helper.FILTER_COMBINING_CHARACTERS_OFF), "Off");
        filterSetting.put(Long.valueOf(Helper.FILTER_COMBINING_CHARACTERS_LENIENT), "Lenient");
        filterSetting.put(Long.valueOf(Helper.FILTER_COMBINING_CHARACTERS_STRICT), "Strict");
        ComboLongSetting filterCombiningCharacters = new ComboLongSetting(filterSetting);
        d.addLongSetting("filterCombiningCharacters", filterCombiningCharacters);

        otherSettingsPanel.add(filterCombiningCharacters,
                d.makeGbc(3, 2, 1, 1));
        
        gbc = d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST);
        otherSettingsPanel.add(d.addSimpleBooleanSetting("printStreamStatus", "Show stream status in chat",
                "Output stream status when you join a channel and when it changes"), gbc);
        
        gbc = d.makeGbc(2, 3, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        otherSettingsPanel.add(
                d.addSimpleBooleanSetting("colorCorrection", "Correct readability of usercolors",
                        "If enabled, changes some usercolors to make them more readable on the current background"),
                gbc);
        
        /**
         * Timeout settings
         */
        gbc = d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST);
        DeletedMessagesModeSetting deletedMessagesModeSetting = new DeletedMessagesModeSetting(d);
        timeoutSettingsPanel.add(deletedMessagesModeSetting, gbc);
        
        gbc = d.makeGbcSub(0, 1, 1, 1, GridBagConstraints.WEST);
        timeoutSettingsPanel.add(
                d.addSimpleBooleanSetting(
                        "banDurationAppended",
                        "Show ban duration",
                        "Shows the duration in seconds for timeouts behind the latest deleted message [BETA]"),
                gbc);
        
        gbc = d.makeGbcSub(1, 1, 1, 1, GridBagConstraints.WEST);
        gbc.anchor = GridBagConstraints.WEST;
        timeoutSettingsPanel.add(
                d.addSimpleBooleanSetting("banReasonAppended", "Show ban reason (mod only)",
                        "Shows the reason of a ban behind the latest deleted message (mod only, except for your own bans) [BETA]"),
                gbc);

        final JCheckBox timeoutMessages = d.addSimpleBooleanSetting("showBanMessages", "Show separate ban messages, with following options:",
                        "Shows separate '<user> has been banned from talking' messages for bans/timeouts");
        timeoutMessages.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                for (JCheckBox cb : timeoutMessageSettings) {
                    cb.setEnabled(timeoutMessages.isSelected());
                }
            }
        });
        gbc = d.makeGbc(0, 3, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        timeoutSettingsPanel.add(timeoutMessages, gbc);
        
        JCheckBox banDuration = d.addSimpleBooleanSetting("banDurationMessage", "Show ban duration",
                        "Shows the duration in seconds for timeouts in separate ban messages [BETA]");
        timeoutMessageSettings.add(banDuration);
        gbc = d.makeGbcSub(0, 4, 1, 1, GridBagConstraints.WEST);
        timeoutSettingsPanel.add(banDuration, gbc);
        
        JCheckBox banReason = d.addSimpleBooleanSetting("banReasonMessage", "Show ban reason (mod only)",
                        "Shows the reason of a ban in separate ban messages (mod only, except for your own bans) [BETA]");
        timeoutMessageSettings.add(banReason);
        gbc = d.makeGbcSub(1, 4, 1, 1, GridBagConstraints.WEST);
        gbc.anchor = GridBagConstraints.WEST;
        timeoutSettingsPanel.add(banReason, gbc);
        
        JCheckBox timeoutsCombine = d.addSimpleBooleanSetting("combineBanMessages", "Combine ban messages",
                        "Combines similiar ban messages into one, appending the number of bans");
        timeoutMessageSettings.add(timeoutsCombine);
        gbc = d.makeGbcSub(0, 5, 1, 1, GridBagConstraints.WEST);
        timeoutSettingsPanel.add(timeoutsCombine, gbc);
        
        gbc = d.makeGbc(0, 6, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        timeoutSettingsPanel.add(
                d.addSimpleBooleanSetting("clearChatOnChannelCleared", "Clear chat when cleared by a moderator",
                        "If enabled, removes all text from the channel when a moderator clears the channel."),
                gbc);
        
        
        for (JCheckBox cb : timeoutMessageSettings) {
            cb.setEnabled(false);
        }
        
    }
    
    public static void addTimestampFormat(Map<String, String> timestampOptions, String format) {
        String label = format;
        if (!format.equals("off")) {
            int hour = DateTime.currentHour12Hour();
            if (hour > 0 && hour < 10) {
                label = DateTime.currentTime(format);
            } else {
                label = DateTime.format(System.currentTimeMillis() - 4*60*60*1000, new SimpleDateFormat(format));
            }
        }
        timestampOptions.put(format, label);
    }
    
    private static class CapitalizedNamesSettings extends JDialog {

        private static final String INFO0 = "<html><body style='width:300px;'>"
                + "Names in Twitch Chat are send all-lowercase by default, "
                + "the following options can be used to change capitalization:";
        
        private static final String INFO1 = "<html><body style='width:280px;'>"
                + "If enabled, simply makes the first letter of the name uppercase by "
                + "default.";
        
        private static final String INFO2 = "<html><body style='width:280px;'>"
                + "If enabled, it uses the display name send by Twitch Chat in "
                + "the IRCv3 tag.<br /><br />As opposed to the previous method which "
                + "requested the names from Twitch API this has much less impact "
                + "and thus should be enabled unless you don't want correctly "
                + "capitalized names.";
        
        public CapitalizedNamesSettings(SettingsDialog d) {
            super(d);
            
            setDefaultCloseOperation(HIDE_ON_CLOSE);
            setTitle("Name Capitalization Settings");
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            gbc = d.makeGbc(0, 0, 1, 1);
            gbc.insets = new Insets(5, 5, 5, 5);
            add(new JLabel(INFO0), gbc);
            
            add(d.addSimpleBooleanSetting("capitalizedNames", "Capitalized Names (First Letter only)",
                        "Requires a restart of Chatty to have any effect."),
                    d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
            
            gbc = d.makeGbc(0, 2, 1, 1);
            gbc.insets = new Insets(0, 30, 5, 10);
            add(new JLabel(INFO1), gbc);
            
            add(d.addSimpleBooleanSetting("ircv3CapitalizedNames", "Correctly Capitalized Names (IRCv3 tags)",
                        "Requires a restart of Chatty to have full effect."),
                    d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
            
            gbc = d.makeGbc(0, 4, 1, 1);
            gbc.insets = new Insets(0, 30, 5, 10);
            add(new LinkLabel(INFO2, d.getSettingsHelpLinkLabelListener()), gbc);
            
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
            gbc = d.makeGbc(0, 5, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(5, 5, 5, 5);
            add(closeButton, gbc);
            
            pack();
        }
        
    }
    
}
