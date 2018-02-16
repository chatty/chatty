
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.lang.Language;
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

        JPanel timeoutSettingsPanel = addTitledPanel(Language.getString("settings.section.deletedMessages"), 0);
        JPanel otherSettingsPanel = addTitledPanel(Language.getString("settings.section.otherMessageSettings"), 1);

        //==========================
        // Deleted Messages (Panel)
        //==========================
        DeletedMessagesModeSetting deletedMessagesModeSetting = new DeletedMessagesModeSetting(d);
        timeoutSettingsPanel.add(deletedMessagesModeSetting,
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));

        timeoutSettingsPanel.add(d.addSimpleBooleanSetting("banDurationAppended"),
                d.makeGbcSub(0, 1, 1, 1, GridBagConstraints.WEST));

        timeoutSettingsPanel.add(d.addSimpleBooleanSetting("banReasonAppended"),
                d.makeGbcSub(1, 1, 1, 1, GridBagConstraints.WEST));

        final JCheckBox timeoutMessages = d.addSimpleBooleanSetting("showBanMessages");
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
                "banDurationMessage");
        timeoutMessageSettings.add(banDuration);
        timeoutSettingsPanel.add(banDuration,
                d.makeGbcSub(0, 4, 1, 1, GridBagConstraints.WEST));
        
        JCheckBox banReason = d.addSimpleBooleanSetting(
                "banReasonMessage");
        timeoutMessageSettings.add(banReason);
        timeoutSettingsPanel.add(banReason,
                d.makeGbcSub(1, 4, 1, 1, GridBagConstraints.WEST));
        
        JCheckBox timeoutsCombine = d.addSimpleBooleanSetting(
                "combineBanMessages");
        timeoutMessageSettings.add(timeoutsCombine);
        timeoutSettingsPanel.add(timeoutsCombine,
                d.makeGbcSub(0, 5, 1, 1, GridBagConstraints.WEST));

        timeoutSettingsPanel.add(d.addSimpleBooleanSetting(
                "clearChatOnChannelCleared"),
                d.makeGbc(0, 6, 2, 1, GridBagConstraints.WEST));
        
        
        for (JCheckBox cb : timeoutMessageSettings) {
            cb.setEnabled(false);
        }
        
        //========================
        // Other Settings (Panel)
        //========================
        // Timestamp
        otherSettingsPanel.add(new JLabel(Language.getString("settings.otherMessageSettings.timestamp")),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));

        final Map<String,String> timestampOptions = new LinkedHashMap<>();
        List<String> dateFormats = Arrays.asList(new String[]{"","YYYY-MM-dd ",
            "MMM d ", "d MMM ", "dd.MM. "});
        for (String dateFormat : dateFormats) {
            timestampOptions.put("off", "Off");
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
                d.makeGbc(1, 0, 2, 1, GridBagConstraints.EAST));

        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "showModMessages"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));

        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "showJoinsParts"),
                d.makeGbc(2, 1, 2, 1, GridBagConstraints.WEST));

        // Combining Characters
        otherSettingsPanel.add(new JLabel("Filter combining chars:"),
                d.makeGbc(0, 2, 1, 1));

        Map<Long, String> filterSetting = new LinkedHashMap<>();
        filterSetting.put(Long.valueOf(Helper.FILTER_COMBINING_CHARACTERS_OFF), "Off");
        filterSetting.put(Long.valueOf(Helper.FILTER_COMBINING_CHARACTERS_LENIENT), "Lenient");
        filterSetting.put(Long.valueOf(Helper.FILTER_COMBINING_CHARACTERS_STRICT), "Strict");
        ComboLongSetting filterCombiningCharacters = new ComboLongSetting(filterSetting);
        d.addLongSetting("filterCombiningCharacters", filterCombiningCharacters);

        otherSettingsPanel.add(filterCombiningCharacters,
                d.makeGbc(1, 2, 1, 1));
        
        
        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "printStreamStatus"),
                d.makeGbc(0, 3, 4, 1, GridBagConstraints.WEST));

        
    }
    
    public static void addTimestampFormat(Map<String, String> timestampOptions, String format) {
        String label;
        int hour = DateTime.currentHour12Hour();
        if (hour > 0 && hour < 10) {
            label = DateTime.currentTime(format);
        } else {
            label = DateTime.format(System.currentTimeMillis() - 4 * 60 * 60 * 1000, new SimpleDateFormat(format));
        }
        timestampOptions.put(format, label);
    }
}
