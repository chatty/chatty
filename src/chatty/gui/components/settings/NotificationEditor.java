
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.util.colors.HtmlColors;
import chatty.gui.components.LinkLabelListener;
import static chatty.gui.components.settings.NotificationSettings.l;
import chatty.gui.notifications.Notification;
import chatty.gui.notifications.Notification.State;
import chatty.gui.notifications.Notification.TypeOption;
import chatty.gui.notifications.Notification.Type;
import chatty.gui.notifications.NotificationManager;
import chatty.gui.notifications.NotificationWindow;
import chatty.lang.Language;
import chatty.util.Sound;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.EAST;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

/**
 * Table to add/remove/edit notification events.
 * 
 * @author tduva
 */
class NotificationEditor extends TableEditor<Notification> {
    
    private static final Map<Notification.Type, String> typeNames;
    private static final Map<Notification.State, String> status;
    
    private MyItemEditor editor;
    private LinkLabelListener linkLabelListener;
    private Path soundPath;
    private String[] soundFiles;
    
    public NotificationEditor(JDialog owner, Settings settings) {
        super(SORTING_MODE_MANUAL, false);
        
        setModel(new MyTableModel());
        setItemEditor(() -> {
            if (editor == null) {
                editor = new MyItemEditor(owner, settings);
                setLinkLabelListener(linkLabelListener);
                setSoundFiles(soundPath, soundFiles);
            }
            return editor;
        });
        setRendererForColumn(0, new MyRenderer());
        setRendererForColumn(1, new MyRenderer());
        setRendererForColumn(2, new MyRenderer());
        setRendererForColumn(3, new MyRenderer());
    }
    
    public void setSoundFiles(Path path, String[] fileNames) {
        if (editor != null) {
            editor.setSoundFiles(path, fileNames);
        }
        else {
            soundPath = path;
            soundFiles = fileNames;
        }
    }
    
    public void setLinkLabelListener(LinkLabelListener listener) {
        if (editor != null) {
            editor.setLinkLabelListener(listener);
        }
        else {
            linkLabelListener = listener;
        }
    }
    
    public void setSelected(long id) {
        for (Notification n : getData()) {
            if (n.id == id) {
                super.selectItem(n);
            }
        }
    }
    
    /**
     * Names for the usericon types.
     */
    static {
        typeNames = new LinkedHashMap<>();
        for (Notification.Type s : Notification.Type.values()) {
            typeNames.put(s, s.label);
        }
        
        status = new LinkedHashMap<>();
        for (Notification.State s : Notification.State.values()) {
            status.put(s, s.label);
        }
    }
    
    /**
     * The table model defining the columns and what data is returned on which
     * column.
     */
    private static class MyTableModel extends ListTableModel<Notification> {

        public MyTableModel() {
            super(new String[]{l("column.event"), l("column.notification"), l("column.sound"), l("column.message")});
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Notification p = get(rowIndex);
            return p;
        }
        
        @Override
        public Class getColumnClass(int columnIndex) {
            return Notification.class;
        }
        
    }
    
    /**
     * Renderer for all table columns.
     */
    private static class MyRenderer extends JLabel implements TableCellRenderer {

        private final int ROW_HEIGHT = this.getFontMetrics(this.getFont()).getHeight()*2;
        private boolean rowHeightSet;
        
        public MyRenderer() {
            setFont(getFont().deriveFont(Font.PLAIN));
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            // Apparently value can be null in rare cases, even if it shouldn't
            // be
            if (value == null) {
                setText("error");
                return this;
            }
            Notification n = (Notification) value;
            
            // Color
            if (column == 0) {
                setForeground(n.foregroundColor);
                setBackground(n.backgroundColor);
            } else if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            
            // Text
            String text;
            if (column == 0) {
                String channel = n.hasChannels() ? " (" + n.serializeChannels() + ")" : "";
                String matcher = n.hasMatcher() ? "&nbsp;"+n.getMatcherString() : "";
                text = String.format("%s%s\n%s",
                        n.type.label,
                        channel,
                        matcher);
            } else if (column == 1) {
                text = String.format("%s", n.getDesktopState());
            } else if (column == 2) {
                String cooldown = "";
                if (n.soundCooldown > 0 || n.soundInactiveCooldown > 0) {
                    cooldown = String.format("[%s/%s]",
                            formatCooldown(n.soundCooldown),
                            formatCooldown(n.soundInactiveCooldown));
                }
                text = String.format("%s\n%s %s",
                        n.getSoundState(),
                        cooldown,
                        n.soundFile == null ? "No sound file" : n.soundFile);
            } else {
                text = String.format("%s%s\n%s",
                        n.messageOverrideDefault ? "[!] " : "",
                        n.getMessageState(),
                        n.messageTarget);
            }
            
            if (text.startsWith("Off")) {
                setForeground(Color.GRAY);
            }
            
            this.setText("<html><body style='overflow:hidden;width:1000;padding:1px'>"+text.replace("\n", "<br />"));
            if (!rowHeightSet) {
                table.setRowHeight(ROW_HEIGHT);
                rowHeightSet = true;
            }
            return this;
        }
        
    }
    
    private static String formatCooldown(int input) {
        return input == 0 || input % 60 != 0 ? input+"s" : input/60+"m";
    }

    /**
     * The editor for a single usericon, which does the most work here, having
     * to load a list of icons for use, creating the icon when selected,
     * updating the preview and so on.
     */
    private static class MyItemEditor implements ItemEditor<Notification> {
        
        private static final String MATCHER_HELP = "<html><body width='300px'>"
                + "The Matcher allows you to match on the text of the "
                + "notification. You can use the same format as for the "
                + "[help-settings:Highlight Highlights] list, although some "
                + "prefixes may have no effect.<br /><br />";
        
        private static final int VOLUME_MIN = 0;
        private static final int VOLUME_MAX = 100;

        // Organization
        private final JDialog dialog;
        private final JTabbedPane tabs = new JTabbedPane();
        private final JButton okButton = new JButton("Save");
        private final JButton cancelButton = new JButton("Cancel");
        
        // Settings
        private final JPanel options;
        private final Map<String, JCheckBox> optionsAssoc;
        private final GenericComboSetting<Notification.Type> type;
        private final GenericComboSetting<Notification.State> desktopState;
        private final GenericComboSetting<Notification.State> soundState;
        private final GenericComboSetting<Notification.State> messageState;
        private final SimpleStringSetting channels;
        private final EditorStringSetting matcher;
        private final ColorTemplates colorTemplates;
        private final ColorSetting foregroundColor;
        private final ColorSetting backgroundColor;
        private final JButton testColors;
        private final DurationSetting soundCooldown;
        private final DurationSetting soundInactiveCooldown;
        private final ComboStringSetting soundFile;
        private final SliderLongSetting volumeSlider;
        private final JButton playSound;
        private final SimpleStringSetting messageTarget;
        private final JCheckBox messageUseColor;
        private final JCheckBox messageOverrideDefault;

        // State
        private JLabel description;
        private Notification current;
        private Path soundsPath;
        private boolean save;
        private NotificationWindow testNotification;
        
        public MyItemEditor(Window owner, Settings settings) {
            dialog = new JDialog(owner);
            dialog.setLayout(new GridBagLayout());
            dialog.setResizable(false);
            dialog.setModal(true);
            
            type = new GenericComboSetting<>(typeNames);
            type.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    updateSubTypes();
                }
            });
            
            
            
            desktopState = new GenericComboSetting<>(status);
            desktopState.setToolTipText("abc");
            
            soundState = new GenericComboSetting<>(status);
            soundState.setToolTipText("abc");
            
            messageState = new GenericComboSetting<>(status);
            
            desktopState.addItemListener(e -> {
                tabs.setTitleAt(0, columnLabel("notification", desktopState.getSettingValue() != State.OFF));
                updateDesktopSettings();
            });
            
            soundState.addItemListener(e -> {
                tabs.setTitleAt(1, columnLabel("sound", soundState.getSettingValue() != State.OFF));
                updateSoundSettings();
            });
            
            messageState.addItemListener(e -> {
                tabs.setTitleAt(2, columnLabel("message", messageState.getSettingValue() != State.OFF));
                updateMessageSettings();
            });
            
            JPanel desktop = new JPanel(new GridBagLayout());
            JPanel sound = new JPanel(new GridBagLayout());
            JPanel message = new JPanel(new GridBagLayout());
            
            JPanel optionsPanel = new JPanel(new GridBagLayout());
            //optionsPanel.setBorder(BorderFactory.createTitledBorder("Event"));
            
            options = new JPanel();
            options.setLayout(new BoxLayout(options, BoxLayout.PAGE_AXIS));
            optionsAssoc = new HashMap<>();
            
            channels = new SimpleStringSetting(20, true, DataFormatter.TRIM);
            matcher = new EditorStringSetting(dialog, "Match Notification Text", 20, () -> {
                HighlighterTester matcherEditor = new HighlighterTester(dialog, false, "notification");
                matcherEditor.setAllowEmpty(true);
                return matcherEditor;
            });
            
            SettingsUtil.addLabeledComponent(optionsPanel, "settings.notifications.channel", 0, 2, 1, EAST, channels);
            SettingsUtil.addLabeledComponent(optionsPanel, "settings.notifications.textMatch", 0, 3, 1, EAST, matcher);
            
            optionsPanel.add(options, GuiUtil.makeGbc(0, 4, 2, 1, GridBagConstraints.WEST));
            
            foregroundColor = new ColorSetting(ColorSetting.FOREGROUND,
                    null,
                    Language.getString("settings.general.foreground"),
                    Language.getString("settings.general.foreground"),
                    () -> new ColorChooser(dialog));
            backgroundColor = new ColorSetting(ColorSetting.BACKGROUND,
                    null,
                    Language.getString("settings.general.background"),
                    Language.getString("settings.general.background"),
                    () -> new ColorChooser(dialog));
            ColorSettingListener colorChangeListener = new ColorSettingListener() {

                @Override
                public void colorUpdated() {
                    foregroundColor.setBaseColor(backgroundColor.getSettingValue());
                    backgroundColor.setBaseColor(foregroundColor.getSettingValue());
                    updateTestNotification();
                }
            };
            foregroundColor.addListener(colorChangeListener);
            backgroundColor.addListener(colorChangeListener);
            
            colorTemplates = new ColorTemplates(settings,
                    NotificationManager.COLOR_PRESETS_SETTING_NAME,
                    new ColorSetting[]{foregroundColor, backgroundColor});
            colorTemplates.addPreset("Classic",new String[]{"Black", "#FFFFF0"});
            colorTemplates.addPreset("Highlight",new String[]{"Black", "#FFFF79"});
            colorTemplates.addPreset("Black", new String[]{"White", "#333333"});
            colorTemplates.addPreset("Violet", new String[]{"White", "BlueViolet"}); // TODO: Other Chatty icon
            colorTemplates.init();
            
            testColors = new JButton("Test Colors");
            testColors.addActionListener(e -> {
                testNotification();
            });
            
            soundFile = new ComboStringSetting(new String[]{});
            
            volumeSlider = new SliderLongSetting(JSlider.HORIZONTAL, VOLUME_MIN, VOLUME_MAX, 0);
            volumeSlider.setMajorTickSpacing(10);
            volumeSlider.setMinorTickSpacing(5);
            volumeSlider.setPaintTicks(true);
            
            
            soundCooldown = new DurationSetting(3, true);
            soundInactiveCooldown = new DurationSetting(3, true);
            
            messageTarget = new SimpleStringSetting(20, true, DataFormatter.TRIM);
            messageUseColor = new JCheckBox(Language.getString("settings.notifications.messageUseColor"));
            messageOverrideDefault = new JCheckBox(Language.getString("settings.notifications.messageOverrideDefault"));

            GridBagConstraints gbc;

            //### Basic Settings Panel ###
            SettingsUtil.addLabeledComponent(optionsPanel, "settings.notifications.event", 0, 0, 2, EAST, type);
            
            description = new JLabel();
            gbc = GuiUtil.makeGbc(1, 1, 2, 1, GridBagConstraints.WEST);
            optionsPanel.add(description, gbc);
            
            //-----------------------
            // Notification Settings
            //-----------------------
            JLabel desktopStateLabel = new JLabel("Status:");
            desktopStateLabel.setLabelFor(desktopState);
            gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST);
            desktop.add(desktopStateLabel, gbc);
            
            gbc = GuiUtil.makeGbc(1, 1, 2, 1, GridBagConstraints.CENTER);
            desktop.add(desktopState, gbc);
            
            gbc = GuiUtil.makeGbc(0, 2, 3, 1, GridBagConstraints.CENTER);
            desktop.add(colorTemplates, gbc);
            
            gbc = GuiUtil.makeGbc(0, 3, 3, 1, GridBagConstraints.CENTER);
            gbc.insets = new Insets(5, 5, 0, 5);
            desktop.add(foregroundColor, gbc);
            
            gbc = GuiUtil.makeGbc(0, 4, 3, 1, GridBagConstraints.CENTER);
            desktop.add(backgroundColor, gbc);
            
            gbc = GuiUtil.makeGbc(1, 5, 2, 1, GridBagConstraints.EAST);
            desktop.add(testColors, gbc);
            
            //----------------
            // Sound Settings
            //----------------
            JLabel soundStateLabel = new JLabel("Status:");
            soundStateLabel.setLabelFor(soundState);
            gbc = GuiUtil.makeGbc(0, 1, 1, 1);
            sound.add(soundStateLabel, gbc);
            
            gbc = GuiUtil.makeGbc(1, 1, 3, 1);
            gbc.anchor = GridBagConstraints.WEST;
            sound.add(soundState, gbc);
            
            SettingsUtil.addLabeledComponent(sound, "settings.notifications.soundFile", 0, 2, 3, EAST, soundFile);
            SettingsUtil.addLabeledComponent(sound, "settings.notifications.soundVolume", 0, 3, 3, EAST, volumeSlider);
            SettingsUtil.addLabeledComponent(sound, "settings.notifications.soundCooldown", 0, 4, 1, EAST, soundCooldown);
            SettingsUtil.addLabeledComponent(sound, "settings.notifications.soundPassiveCooldown", 2, 4, 1, EAST, soundInactiveCooldown);
            
            playSound = new JButton("Test Sound");
            playSound.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        String file = soundFile.getSettingValue();
                        if (file != null && !file.isEmpty()) {
                            long volume = volumeSlider.getSettingValue();
                            Sound.play(soundsPath.resolve(file), volume, "test", -1);
                        }
                    } catch (Exception ex) {
                        GuiUtil.showNonModalMessage(dialog, "Error Playing Sound",
                                ex.toString(),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            gbc = GuiUtil.makeGbc(2, 5, 2, 1);
            sound.add(playSound, gbc);
            
            //------------------
            // Message Settings
            //------------------
            JLabel messageStateLabel = new JLabel("Status:");
            messageStateLabel.setLabelFor(messageState);
            gbc = GuiUtil.makeGbc(0, 1, 1, 1);
            message.add(messageStateLabel, gbc);
            
            gbc = GuiUtil.makeGbc(1, 1, 3, 1);
            gbc.anchor = GridBagConstraints.WEST;
            message.add(messageState, gbc);
            
            SettingsUtil.addLabeledComponent(message, "settings.notifications.messageTarget", 0, 2, 1, EAST, messageTarget);
            
            gbc = GuiUtil.makeGbc(0, 3, 4, 1);
            gbc.anchor = GridBagConstraints.WEST;
            message.add(messageUseColor, gbc);
            
            gbc = GuiUtil.makeGbc(0, 4, 4, 1);
            gbc.anchor = GridBagConstraints.WEST;
            message.add(messageOverrideDefault, gbc);
            
            //### Dialog ###
            
            gbc = GuiUtil.makeGbc(0, 2, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(tabs, gbc);
            
            gbc = GuiUtil.makeGbc(0, 0, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(optionsPanel, gbc);
            
            gbc = GuiUtil.makeGbc(1, 6, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.7;
            dialog.add(okButton, gbc);

            gbc = GuiUtil.makeGbc(2, 6, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.3;
            dialog.add(cancelButton, gbc);
            
            tabs.addTab(columnLabel("notification", true), GuiUtil.northWrap(desktop));
            tabs.addTab(columnLabel("sound", true), GuiUtil.northWrap(sound));
            tabs.addTab(columnLabel("message", true), GuiUtil.northWrap(message));
            
            ActionListener buttonAction = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == okButton) {
                        save = true;
                    }
                    if (e.getSource() == okButton || e.getSource() == cancelButton) {
                        dialog.setVisible(false);
                    }
                }
            };
            okButton.addActionListener(buttonAction);
            cancelButton.addActionListener(buttonAction);
            
            dialog.pack();
        }
        
        private static String columnLabel(String type, boolean on) {
            if (on) {
                return Language.getString("settings.notifications.column."+type);
            }
            return String.format("%s (%s)",
                    Language.getString("settings.notifications.column."+type),
                    Language.getString("settings.notifications.column.off"));
        }
        
        private void updateSize() {
            dialog.pack();
        }
        
        private void updateSubTypes() {
            updateHelp();
            
            Type t = this.type.getSettingValue();
            options.removeAll();
            optionsAssoc.clear();
            for (TypeOption option : t.options) {
                String label = Language.getString("notification.typeOption."+option.id);
                String tip = Language.getString("notification.typeOption."+option.id+".tip", false);
                JCheckBox checkbox = new JCheckBox(label);
                if (tip != null) {
                    checkbox.setToolTipText(SettingsUtil.addTooltipLinebreaks(tip));
                }
                checkbox.setName(option.id);
                if (current != null) {
                    checkbox.setSelected(current.options.contains(option.id));
                }
                options.add(checkbox);
                optionsAssoc.put(option.id, checkbox);
            }
            options.setVisible(!t.options.isEmpty());
            updateSize();
        }
        
        private void updateHelp() {
            String eventId = StringUtil.toLowerCase(type.getSettingValue().name());
            String eventDescription = Language.getString("notification.type."+eventId+".tip", false);
            if (eventDescription != null) {
                description.setVisible(true);
                description.setText("<html><body style='width:"+matcher.getPreferredSize().width+"'>"+eventDescription);
                type.setToolTipText(eventDescription);
            } else {
                description.setVisible(false);
                type.setToolTipText(null);
            }
            matcher.setInfo(SettingConstants.HTML_PREFIX
                    +SettingsUtil.getInfo("info-notification-match.html", type.getSettingValue().name()));
        }
        
        private List<String> getSubTypes() {
            List<String> result = new ArrayList<>();
            for (String option : optionsAssoc.keySet()) {
                JCheckBox cb = optionsAssoc.get(option);
                if (cb.isSelected()) {
                    result.add(option);
                }
            }
            return result;
        }
        
        private void updateDesktopSettings() {
            boolean enabled = desktopState.getSettingValue() != State.OFF;
            testColors.setEnabled(enabled);
        }
        
        /**
         * Disable sound settings if sound is not enabled, to make clearer that
         * sound is not enabled.
         */
        private void updateSoundSettings() {
            boolean enabled = soundState.getSettingValue() != State.OFF;
            soundFile.setEnabled(enabled);
            soundCooldown.setEnabled(enabled);
            soundInactiveCooldown.setEnabled(enabled);
            volumeSlider.setEnabled(enabled);
            playSound.setEnabled(enabled);
        }
        
        private void updateMessageSettings() {
            boolean enabled = messageState.getSettingValue() != State.OFF;
            messageTarget.setEnabled(enabled);
            messageUseColor.setEnabled(enabled);
        }
        
        @Override
        public Notification showEditor(Notification preset, Component c, boolean edit, int column) {
            if (edit) {
                dialog.setTitle("Edit notification/sound");
            } else {
                dialog.setTitle("Add notification/sound");
            }
            if (preset != null) {
                current = preset;
                
                type.setSettingValue(preset.type);
                channels.setSettingValue(preset.serializeChannels());
                matcher.setSettingValue(preset.matcher);
                desktopState.setSettingValue(preset.desktopState);
                foregroundColor.setSettingValue(HtmlColors.getColorString(preset.foregroundColor));
                backgroundColor.setSettingValue(HtmlColors.getColorString(preset.backgroundColor));

                // Sound
                soundState.setSettingValue(preset.soundState);
                soundFile.setSettingValue(preset.soundFile);
                soundCooldown.setSettingValue(Long.valueOf(preset.soundCooldown));
                soundInactiveCooldown.setSettingValue(Long.valueOf(preset.soundInactiveCooldown));
                volumeSlider.setSettingValue(preset.soundVolume);
                
                // Message
                messageState.setSettingValue(preset.messageState);
                messageTarget.setSettingValue(preset.messageTarget);
                messageUseColor.setSelected(preset.messageUseColor);
                messageOverrideDefault.setSelected(preset.messageOverrideDefault);
                
                updateSubTypes();
            } else {
                current = null;
                
                type.setSelectedIndex(0);
                channels.setSettingValue("");
                matcher.setSettingValue(null);
                desktopState.setSettingValue(Notification.State.ALWAYS);
                foregroundColor.setSettingValue("black");
                backgroundColor.setSettingValue(HtmlColors.getColorString(new Color(255, 255, 240)));
                
                // Sound
                soundState.setSettingValue(Notification.State.OFF);
                soundFile.setSelectedIndex(0);
                volumeSlider.setSettingValue(Long.valueOf(20));
                soundCooldown.setSettingValue(Long.valueOf(0));
                soundInactiveCooldown.setSettingValue(Long.valueOf(0));
                
                // Message
                messageState.setSettingValue(Notification.State.OFF);
                messageTarget.setSettingValue(null);
                messageUseColor.setSelected(false);
                messageOverrideDefault.setSelected(false);
                
                updateSubTypes();
                create();
            }
            
            // Default selected tab based on which table column was clicked on
            switch (column) {
                case 2:
                    tabs.setSelectedIndex(1);
                    break;
                case 3:
                    tabs.setSelectedIndex(2);
                    break;
                default:
                    tabs.setSelectedIndex(0);
                    break;
            }
            colorTemplates.selectDefault();
            
            save = false;
            
            dialog.setLocationRelativeTo(c);
            dialog.setVisible(true);
            // Modal dialog, so blocks here and stuff can be changed via the GUI
            // until the dialog is closed
            
            clearTestNotification();
            
            if (save) {
                return create();
            }
            return null;
        }
        
        private Notification create() {
            Type type = this.type.getSettingValue();
            Color foreground = HtmlColors.decode(foregroundColor.getSettingValue());
            Color background = HtmlColors.decode(backgroundColor.getSettingValue());
            
            Notification.Builder b = new Notification.Builder(type);
            b.setDesktopEnabled(desktopState.getSettingValue());
            b.setSoundEnabled(soundState.getSettingValue());
            b.setMessageEnabled(messageState.getSettingValue());
            b.setForeground(foreground);
            b.setBackground(background);
            b.setSoundFile(soundFile.getSettingValue());
            b.setVolume(volumeSlider.getSettingValue());
            b.setSoundCooldown(soundCooldown.getSettingValue(0L).intValue());
            b.setSoundInactiveCooldown(soundInactiveCooldown.getSettingValue(0L).intValue());
            b.setChannels(channels.getSettingValue());
            b.setMatcher(matcher.getSettingValue());
            b.setOptions(getSubTypes());
            b.setMessageTarget(messageTarget.getSettingValue());
            b.setMessageUseColor(messageUseColor.isSelected());
            b.setMessageOverrideDefault(messageOverrideDefault.isSelected());
            
            current = new Notification(b);
            return current;
        }
        
        public void setSoundFiles(Path path, String[] names) {
            soundFile.removeAllItems();
            soundFile.add((String)null, "<None>");
            for (String name : names) {
                soundFile.add(name);
            }
            this.soundsPath = path;
        }
        
        private void testNotification() {
            NotificationWindow w = new NotificationWindow("[Test] "+type.getSettingValue().label+" Colors",
                    "Nice color you got there, would be a shame if it was to be used for a notification.",
                    foregroundColor.getSettingValueAsColor(),
                    backgroundColor.getSettingValueAsColor(),
                    null);
            w.setLocation(dialog.getLocation());
            w.setTimeout(30*1000);
            w.show();
            if (testNotification != null) {
                final NotificationWindow toClose = testNotification;
                SwingUtilities.invokeLater(() -> {
                    toClose.close();
                });
            }
            testNotification = w;
        }
        
        private void clearTestNotification() {
            if (testNotification != null) {
                testNotification.close();
                testNotification = null;
            }
        }
        
        private void updateTestNotification() {
            if (testNotification != null && testNotification.isVisible()) {
                testNotification();
            }
        }
        
        public void setLinkLabelListener(LinkLabelListener listener) {
            matcher.setLinkLabelListener(listener);
        }
        
    }
    
}
