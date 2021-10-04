
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.lang.Language;
import chatty.util.DateTime;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.EAST;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author tduva
 */
public class MessageSettings extends SettingsPanel {
    
    
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
        timeoutSettingsPanel.add(timeoutMessages,
                d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));
        
        JCheckBox banDuration = d.addSimpleBooleanSetting(
                "banDurationMessage");
        timeoutSettingsPanel.add(banDuration,
                d.makeGbcSub(0, 4, 1, 1, GridBagConstraints.WEST));
        
        JCheckBox banReason = d.addSimpleBooleanSetting(
                "banReasonMessage");
        timeoutSettingsPanel.add(banReason,
                d.makeGbcSub(1, 4, 1, 1, GridBagConstraints.WEST));
        
        JCheckBox timeoutsCombine = d.addSimpleBooleanSetting(
                "combineBanMessages");
        timeoutSettingsPanel.add(timeoutsCombine,
                d.makeGbcSub(0, 5, 1, 1, GridBagConstraints.WEST));

        timeoutSettingsPanel.add(d.addSimpleBooleanSetting(
                "clearChatOnChannelCleared"),
                d.makeGbc(0, 6, 2, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(timeoutMessages, banDuration, banReason, timeoutsCombine);
        
        //========================
        // Other Settings (Panel)
        //========================
        // Timestamp
        otherSettingsPanel.add(createTimestampPanel(d, "timestamp"), d.makeGbc(0, 0, 4, 1, GridBagConstraints.WEST));
        
        // More
        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "showModMessages"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));

        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "showJoinsParts"),
                d.makeGbc(2, 1, 2, 1, GridBagConstraints.WEST));

        // Combining Characters
        JLabel combiningCharsLabel = new JLabel("Filter combining chars:");
        otherSettingsPanel.add(combiningCharsLabel,
                d.makeGbc(0, 2, 1, 1));

        Map<Long, String> filterSetting = new LinkedHashMap<>();
        filterSetting.put(Long.valueOf(Helper.FILTER_COMBINING_CHARACTERS_OFF), "Off");
        filterSetting.put(Long.valueOf(Helper.FILTER_COMBINING_CHARACTERS_LENIENT), "Lenient");
        filterSetting.put(Long.valueOf(Helper.FILTER_COMBINING_CHARACTERS_STRICT), "Strict");
        ComboLongSetting filterCombiningCharacters = new ComboLongSetting(filterSetting);
        combiningCharsLabel.setLabelFor(filterCombiningCharacters);
        d.addLongSetting("filterCombiningCharacters", filterCombiningCharacters);

        otherSettingsPanel.add(filterCombiningCharacters,
                d.makeGbc(1, 2, 1, 1));
        
        
        otherSettingsPanel.add(d.addSimpleBooleanSetting(
                "printStreamStatus"),
                d.makeGbc(0, 3, 4, 1, GridBagConstraints.WEST));
    }
    
    public static JPanel createTimestampPanel(SettingsDialog d, String setting) {
        final Map<String,String> timestampOptions = new LinkedHashMap<>();
        timestampOptions.put("off", "Off");
        timestampOptions.put("", "Empty (Space)");
        addTimestampFormat(timestampOptions, "[HH:mm:ss]");
        addTimestampFormat(timestampOptions, "[HH:mm]");
        addTimestampFormat(timestampOptions, "HH:mm:ss");
        addTimestampFormat(timestampOptions, "HH:mm");
        ComboStringSetting combo = new ComboStringSetting(timestampOptions);
        combo.setEditable(false);
        d.addStringSetting(setting, combo);
        
        JButton editTimestampButton = new JButton(Language.getString("dialog.button.customize"));
        editTimestampButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        GuiUtil.matchHeight(editTimestampButton, combo);
        editTimestampButton.addActionListener(e -> {
            TimestampEditor editor = new TimestampEditor(d);
            String preset = combo.getSettingValue();
            if (preset.equals("off") || preset.isEmpty()) {
                preset = "[HH:mm:ss]";
            }
            String result = editor.showDialog(preset);
            if (result != null) {
                combo.setSettingValue(result);
            }
        });
        editTimestampButton.getAccessibleContext().setAccessibleName(Language.getString("settings.otherMessageSettings.customizeTimestamp"));
        
        JPanel timestampPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        timestampPanel.add(combo);
        timestampPanel.add(Box.createHorizontalStrut(5));
        timestampPanel.add(editTimestampButton);
        return timestampPanel;
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
    
    private static class TimestampEditor extends JDialog {
        
        private static final List<String> TIME_FORMATS = Arrays.asList(new String[]{
            "HH:mm:ss", "HH:mm", "hh:mm:ss a", "hh:mm a", "h:mm a", "hh:mm:ssa", "hh:mma", "h:mma", ""
        });
        
        private static final List<String> DATE_FORMATS = Arrays.asList(new String[]{
            "YYYY-MM-dd", "MMM d", "d MMM", "dd.MM.", ""
        });
        
        private static final List<String> BEFORE = Arrays.asList(new String[]{
            "[", " [", " "
        });
        
        private static final List<String> AFTER = Arrays.asList(new String[]{
            "]", "] ", " "
        });
        
        private final JTextField value = new JTextField(10);
        private final JTextField preview = new JTextField(10);
        private final ComboStringSetting before;
        private final ComboStringSetting after;
        private final ComboStringSetting time;
        private final ComboStringSetting date;
        private final JButton saveButton = new JButton(Language.getString("dialog.button.save"));
        private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
        
        private boolean fillInProgress;
        private boolean save;
        
        TimestampEditor(Window owner) {
            super(owner);
            setModal(true);
            setTitle(Language.getString("settings.otherMessageSettings.customizeTimestamp"));
            
            // Time
            Map<String, String> timeFormatOptions = new LinkedHashMap<>();
            timeFormatOptions.put("", "<none>");
            for (String format : TIME_FORMATS) {
                if (!format.isEmpty()) {
                    addTimestampFormat(timeFormatOptions, format);
                }
            }
            time = new ComboStringSetting(timeFormatOptions);
            
            // Date
            Map<String, String> dateFormatOptions = new LinkedHashMap<>();
            dateFormatOptions.put("", "<none>");
            for (String format : DATE_FORMATS) {
                if (!format.isEmpty()) {
                    addTimestampFormat(dateFormatOptions, format);
                }
            }
            date = new ComboStringSetting(dateFormatOptions);
            
            // Before
            Map<String, String> beforeOptions = new LinkedHashMap<>();
            beforeOptions.put("", "<none>");
            for (String option : BEFORE) {
                beforeOptions.put(option, option.replace(" ", "<space>"));
            }
            before = new ComboStringSetting(beforeOptions);
            
            // After
            Map<String, String> afterOptions = new LinkedHashMap<>();
            afterOptions.put("", "<none>");
            for (String option : AFTER) {
                afterOptions.put(option, option.replace(" ", "<space>"));
            }
            after = new ComboStringSetting(afterOptions);
            
            date.addActionListener(e -> formChanged());
            time.addActionListener(e -> formChanged());
            before.addActionListener(e -> formChanged());
            after.addActionListener(e -> formChanged());
            saveButton.addActionListener(e -> {
                save = true;
                setVisible(false);
            });
            cancelButton.addActionListener(e -> {
                setVisible(false);
            });
            
            preview.setEditable(false);
            
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            gbc = GuiUtil.makeGbc(0, 0, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(value, gbc);
            gbc = GuiUtil.makeGbc(2, 0, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(preview, gbc);
            
            gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.CENTER);
            gbc.insets = new Insets(5,0,0,0);
            add(new JLabel("Prefix"), gbc);
            gbc.gridx++;
            add(new JLabel("Date"), gbc);
            gbc.gridx++;
            add(new JLabel("Time"), gbc);
            gbc.gridx++;
            add(new JLabel("Suffix"), gbc);
            add(before, GuiUtil.makeGbc(0, 2, 1, 1));
            add(date, GuiUtil.makeGbc(1, 2, 1, 1));
            add(time, GuiUtil.makeGbc(2, 2, 1, 1));
            gbc = GuiUtil.makeGbc(3, 2, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(saveButton, gbc);
            add(after, gbc);
            add(new JLabel(SettingConstants.HTML_PREFIX
                    +Language.getString("settings.otherMessageSettings.customizeTimestamp.info")
                    +"<br /><br />Append <code>'a:AM/PM'</code> (including quotes) to customize AM/PM."),
                    GuiUtil.makeGbc(0, 3, 4, 1, GridBagConstraints.CENTER));
            gbc = GuiUtil.makeGbc(0, 4, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(saveButton, gbc);
            gbc = GuiUtil.makeGbc(3, 4, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(cancelButton, gbc);
            
            value.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    valueChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    valueChanged();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    valueChanged();
                }
            });
            
            pack();
        }
        
        public String showDialog(String preset) {
            value.setText(preset);
            save = false;
            setLocationRelativeTo(getParent());
            setVisible(true);
            if (save) {
                return value.getText();
            }
            return null;
        }
        
        private void valueChanged() {
            fillFromValue();
            updatePreview();
            pack();
        }
        
        private void formChanged() {
            if (!fillInProgress) {
                buildValue();
            }
        }
        
        private void updatePreview() {
            try {
                SimpleDateFormat format = DateTime.createSdfAmPm(value.getText());
                preview.setText(DateTime.format(System.currentTimeMillis(), format));
            } catch (Exception ex) {
                preview.setText("Invalid format");
            }
        }
        
        private void fillFromValue() {
            fillInProgress = true;
            String v = value.getText();
            Matcher found = find(v);
//            System.out.println(String.format("Left: %s Date: %s Middle: %s Time: %s Right: %s",
//                    found.group(1), found.group(2), found.group(3), found.group(4), found.group(5)));
            
            String foundDate = found.group(2);
            String foundTime = found.group(4);
            String foundBefore = found.group(1);
            String foundAfter = found.group(5);
            
            time.setSettingValue(foundTime);
            date.setSettingValue(foundDate);
            before.setSettingValue(foundBefore);
            after.setSettingValue(foundAfter);
            fillInProgress = false;
        }
        
        private void buildValue() {
            String m = "";
            if (!date.getSettingValue().isEmpty() && !time.getSettingValue().isEmpty()) {
                m = " ";
            }
            value.setText(before.getSettingValue()
                    +date.getSettingValue()
                    +m
                    +time.getSettingValue()
                    +after.getSettingValue());
        }
        
        private Matcher find(String haystack) {
            for (String f : sortFormats(DATE_FORMATS)) {
                for (String f2 : sortFormats(TIME_FORMATS)) {
                    Matcher m = Pattern.compile(String.format("(.*)(%s)( ?)(%s)(.*)",
                            Pattern.quote(f),
                            Pattern.quote(f2))).matcher(haystack);
                    if (m.matches()) {
                        return m;
                    }
                }
            }
            return null;
        }
        
        private List<String> sortFormats(List<String> formats) {
            List<String> sortedFormats = new ArrayList<>(formats);
            Collections.sort(sortedFormats, new Comparator<String>() {

                @Override
                public int compare(String o1, String o2) {
                    return -Integer.compare(o1.length(), o2.length());
                }
                
            });
            return sortedFormats;
        }
        
    }
    
    public static void main(String[] args) {
        TimestampEditor s = new TimestampEditor(null);
        s.showDialog("[YYYY-MM-dd HH:mm]");
        System.exit(0);
    }
    
}
