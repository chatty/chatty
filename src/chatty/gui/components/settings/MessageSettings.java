
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.util.DateTime;
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class MessageSettings extends SettingsPanel {
    
    
    private final Set<JCheckBox> timeoutMessageSettings = new HashSet<>();
    
    public MessageSettings(final SettingsDialog d) {

        JPanel timeoutSettingsPanel = addTitledPanel("Deleted Messages (Timeouts/Bans)", 0);
        JPanel otherSettingsPanel = addTitledPanel("Other", 1);

        /*
         * Other settings (Panel)
         */
        // Timestamp
        otherSettingsPanel.add(new JLabel("Timestamp: "),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));

        final Map<String,String> timestampOptions = new LinkedHashMap<>();
        List<String> dateFormats = Arrays.asList(new String[]{"","YYYY-MM-dd ",
            "MMM d ", "d MMM ", "dd.MM. "});
        for (String dateFormat : dateFormats) {
            addTimestampFormat(timestampOptions, "off");
            addTimestampFormat(timestampOptions, "[" + dateFormat + "HH:mm:ss]");
            addTimestampFormat(timestampOptions, "[" + dateFormat + "HH:mm]");
            addTimestampFormat(timestampOptions, "[" + dateFormat + "hh:mm:ss a]");
            addTimestampFormat(timestampOptions, "[" + dateFormat + "hh:mm a]");
            addTimestampFormat(timestampOptions, "[" + dateFormat + "h:mm a]");
            addTimestampFormat(timestampOptions, "[" + dateFormat + "hh:mm:ssa]");
            addTimestampFormat(timestampOptions, "[" + dateFormat + "hh:mma]");
            addTimestampFormat(timestampOptions, "[" + dateFormat + "h:mma]");
        }
        ComboStringSetting combo = new ComboStringSetting(timestampOptions);
        combo.setEditable(false);
        d.addStringSetting("timestamp", combo);
        otherSettingsPanel.add(combo,
                d.makeGbc(1, 0, 2, 1, GridBagConstraints.WEST));

        
        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "showModMessages",
                "Show mod/unmod (unreliable)",
                "Whether to show when someone was modded/unmodded or a mod "
                        + "joined/left the channel. Twitch Chat is not very "
                        + "reliable in reporting these events correctly."),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));

        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "showJoinsParts", "Show joins/parts (unreliable)",
                "Show users joining/parting the channel (only with "
                                + "Userlist Connection enabled, see Advanced "
                                + "settings)."),
                d.makeGbc(2, 1, 2, 1, GridBagConstraints.WEST));

        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "actionColored",
                "/me messages colored",
                "If enabled, action messages (/me) have the same color as the nick"),
                d.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST));
        
        
        // Combining Characters
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
        
        
        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "printStreamStatus",
                "Show stream status in chat",
                "Output stream status when you join a channel and when it changes"),
                d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));

        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "colorCorrection",
                "Correct readability of usercolors",
                "If enabled, changes some usercolors to make them more readable on the current background"),
                d.makeGbc(2, 3, 2, 1, GridBagConstraints.WEST));
        


        
        /**
         * Deleted Messages settings
         */
        DeletedMessagesModeSetting deletedMessagesModeSetting = new DeletedMessagesModeSetting(d);
        timeoutSettingsPanel.add(deletedMessagesModeSetting,
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));

        timeoutSettingsPanel.add(d.addSimpleBooleanSetting(
                "banDurationAppended",
                "Show ban duration",
                "Shows the duration in seconds for timeouts behind the latest deleted message [BETA]"),
                d.makeGbcSub(0, 1, 1, 1, GridBagConstraints.WEST));

        timeoutSettingsPanel.add(d.addSimpleBooleanSetting(
                "banReasonAppended",
                "Show ban reason (mod only)",
                "Shows the reason of a ban behind the latest deleted message (mod only, except for your own bans) [BETA]"),
                d.makeGbcSub(1, 1, 1, 1, GridBagConstraints.WEST));

        final JCheckBox timeoutMessages = d.addSimpleBooleanSetting(
                "showBanMessages",
                "Show separate ban messages, with following options:",
                "Shows separate '<user> has been banned from talking' messages for bans/timeouts");
        timeoutMessages.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                for (JCheckBox cb : timeoutMessageSettings) {
                    cb.setEnabled(timeoutMessages.isSelected());
                }
            }
        });
        timeoutSettingsPanel.add(timeoutMessages,
                d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));
        
        JCheckBox banDuration = d.addSimpleBooleanSetting(
                "banDurationMessage",
                "Show ban duration",
                "Shows the duration in seconds for timeouts in separate ban messages [BETA]");
        timeoutMessageSettings.add(banDuration);
        timeoutSettingsPanel.add(banDuration,
                d.makeGbcSub(0, 4, 1, 1, GridBagConstraints.WEST));
        
        JCheckBox banReason = d.addSimpleBooleanSetting(
                "banReasonMessage",
                "Show ban reason (mod only)",
                "Shows the reason of a ban in separate ban messages (mod only, except for your own bans) [BETA]");
        timeoutMessageSettings.add(banReason);
        timeoutSettingsPanel.add(banReason,
                d.makeGbcSub(1, 4, 1, 1, GridBagConstraints.WEST));
        
        JCheckBox timeoutsCombine = d.addSimpleBooleanSetting(
                "combineBanMessages",
                "Combine ban messages",
                "Combines similiar ban messages into one, appending the number of bans");
        timeoutMessageSettings.add(timeoutsCombine);
        timeoutSettingsPanel.add(timeoutsCombine,
                d.makeGbcSub(0, 5, 1, 1, GridBagConstraints.WEST));

        timeoutSettingsPanel.add(d.addSimpleBooleanSetting(
                "clearChatOnChannelCleared",
                "Clear chat when cleared by a moderator",
                "If enabled, removes all text from the channel when a moderator clears the channel."),
                d.makeGbc(0, 6, 2, 1, GridBagConstraints.WEST));
        
        
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
}
