
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.Room;
import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.Timestamp;
import chatty.util.api.StreamInfo;
import chatty.util.api.StreamInfoHistoryItem;
import java.awt.FlowLayout;
import java.awt.Font;
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
import java.util.concurrent.TimeUnit;
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
        SettingsUtil.addLabeledComponent(otherSettingsPanel,
                "settings.otherMessageSettings.timestamp",
                0, 0, 3, GridBagConstraints.WEST,
                createTimestampPanel(d, "timestamp"));
        
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
            TimestampEditor editor = new TimestampEditor(d, d.getLinkLabelListener());
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
            "yyyy-MM-dd", "MMM d", "d MMM", "dd.MM.", ""
        });
        
        // Only for regex matching
        private static final List<String> UPTIME_FORMATS = Arrays.asList(new String[]{
            Timestamp.UPTIME_NO_CAPTURE.toString(), ""
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
        private final ComboStringSetting uptime;
        private final ComboLongSetting streamStatus;
        private final JButton saveButton = new JButton(Language.getString("dialog.button.save"));
        private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
        
        private boolean fillInProgress;
        private boolean save;
        
        TimestampEditor(Window owner, LinkLabelListener linkLabelListener) {
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
            
            // Uptime
            Map<String, String> uptimeFormatOptions = new LinkedHashMap<>();
            uptimeFormatOptions.put("", "<none>");
            uptimeFormatOptions.put("'{|,uptime}'", "With space");
            uptimeFormatOptions.put("'{|,uptime:t}'", "Without space");
            uptimeFormatOptions.put("'{|,uptime:p}'", "With space (with Picnic)");
            uptimeFormatOptions.put("'{|,uptime:tp}'", "Without space (with Picnic)");
            uptimeFormatOptions.put("'{|,uptime:c}'", "Clock style");
            uptime = new ComboStringSetting(uptimeFormatOptions);
            
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
            
            // Stream Status
            Map<Long, String> streamStatusOptions = new LinkedHashMap<>();
            streamStatusOptions.put(-1L, "Stream Offline");
            streamStatusOptions.put(TimeUnit.MINUTES.toMillis(8), "Live 8 minutes");
            streamStatusOptions.put(TimeUnit.MINUTES.toMillis(50), "Live 50 minutes");
            streamStatusOptions.put(TimeUnit.MINUTES.toMillis(62), "Live 62 minutes");
            streamStatusOptions.put(TimeUnit.MINUTES.toMillis(70), "Live 70 minutes");
            streamStatusOptions.put(TimeUnit.HOURS.toMillis(40), "Live 40 hours");
            streamStatus = new ComboLongSetting(streamStatusOptions);
            streamStatus.setSettingValue(TimeUnit.MINUTES.toMillis(70));
            
            date.addActionListener(e -> formChanged());
            time.addActionListener(e -> formChanged());
            uptime.addActionListener(e -> formChanged());
            before.addActionListener(e -> formChanged());
            after.addActionListener(e -> formChanged());
            streamStatus.addActionListener(e -> updatePreview());
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
            JPanel topPanel = new JPanel(new GridBagLayout());
            gbc = GuiUtil.makeGbc(0, 0, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            value.setFont(Font.decode(Font.MONOSPACED));
            topPanel.add(value, gbc);
            gbc = GuiUtil.makeGbc(2, 0, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            topPanel.add(preview, gbc);
            
            gbc = GuiUtil.makeGbc(0, 0, 4, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 0, 0);
            add(topPanel, gbc);
            
            gbc = GuiUtil.makeGbc(4, 0, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(streamStatus, gbc);
            
            // Labels
            gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.CENTER);
            gbc.insets = new Insets(5,0,0,0);
            add(new JLabel("Prefix"), gbc);
            gbc.gridx++;
            add(new JLabel("Date"), gbc);
            gbc.gridx++;
            add(new JLabel("Time"), gbc);
            gbc.gridx++;
            add(new JLabel("Stream Uptime"), gbc);
            gbc.gridx++;
            add(new JLabel("Suffix"), gbc);
            
            // Selection
            gbc = GuiUtil.makeGbc(0, 2, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(before, gbc);
            gbc = GuiUtil.makeGbc(1, 2, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(date, gbc);
            gbc = GuiUtil.makeGbc(2, 2, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(time, gbc);
            gbc = GuiUtil.makeGbc(3, 2, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(uptime, gbc);
            gbc = GuiUtil.makeGbc(4, 2, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(after, gbc);
            
            // Info
            add(new LinkLabel(SettingConstants.HTML_PREFIX
                    +SettingsUtil.getInfo("info-timestamp.html", null),
                    linkLabelListener),
                    GuiUtil.makeGbc(0, 3, 5, 1, GridBagConstraints.CENTER));
            
            // Buttons
            gbc = GuiUtil.makeGbc(0, 4, 4, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(saveButton, gbc);
            gbc = GuiUtil.makeGbc(4, 4, 1, 1);
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
        }
        
        private void formChanged() {
            if (!fillInProgress) {
                buildValue();
            }
        }
        
        private void updatePreview() {
            try {
                Timestamp format = new Timestamp(value.getText(), "");
                
                // Create test StreamInfo
                StreamInfo info = new StreamInfo("test", null);
                long uptimeDuration = streamStatus.getSettingValue();
                if (uptimeDuration > 0) {
                    /**
                     * Using history item so a stream start with Picnic can be
                     * simulated.
                     */
                    long testTime = System.currentTimeMillis() - 1;
                    long startTime = testTime - uptimeDuration;
                    long startTimePicnic = startTime - 1000*60*30;
                    if (uptimeDuration < 10*60*1000) {
                        startTimePicnic = startTime;
                    }
                    StreamInfoHistoryItem item = new StreamInfoHistoryItem(testTime, 0, null, null, StreamInfo.StreamType.LIVE, null, startTime, startTimePicnic);
                    LinkedHashMap<Long, StreamInfoHistoryItem> history = new LinkedHashMap<>();
                    history.put(testTime, item);
                    info.setHistory(history);
//                    info.set("test", StreamCategory.EMPTY, 0, System.currentTimeMillis() - uptimeDuration, StreamInfo.StreamType.LIVE);
                }
                
                preview.setText(format.make2(System.currentTimeMillis(), info));
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
            String foundUptime = found.group(5);
            String foundBefore = found.group(1);
            String foundAfter = found.group(6);
            
            time.setSettingValue(foundTime);
            date.setSettingValue(foundDate);
            uptime.setSettingValue(foundUptime);
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
                    +uptime.getSettingValue()
                    +after.getSettingValue());
        }
        
        private Matcher find(String haystack) {
            for (String f : sortFormats(DATE_FORMATS)) {
                for (String f2 : sortFormats(TIME_FORMATS)) {
                    for (String f3 : sortFormats(UPTIME_FORMATS)) {
                        // f3 is a valid regex pattern
                        Matcher m = Pattern.compile(String.format("(.*)(%s)( ?)(%s)(%s)(.*)",
                                Pattern.quote(f),
                                Pattern.quote(f2),
                                f3)).matcher(haystack);
                        if (m.matches()) {
                            return m;
                        }
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
        TimestampEditor s = new TimestampEditor(null, new LinkLabelListener() {
            @Override
            public void linkClicked(String type, String ref) {
                
            }
        });
        s.showDialog("[yyyy-MM-dd HH:mm]");
        System.exit(0);
    }
    
}
