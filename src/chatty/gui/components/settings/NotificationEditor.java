
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.HtmlColors;
import chatty.gui.notifications.Notification;
import chatty.gui.notifications.Notification.State;
import chatty.gui.notifications.Notification.Type;
import chatty.util.Sound;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.swing.BorderFactory;
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
import javax.swing.table.TableCellRenderer;

/**
 * Table to add/remove/edit notification events.
 * 
 * @author tduva
 */
class NotificationEditor extends TableEditor<Notification> {
    
    private static final Map<Notification.Type, String> typeNames;
    private static final Map<Notification.State, String> status;
    
    private final MyItemEditor editor;
    
    public NotificationEditor(JDialog owner) {
        super(SORTING_MODE_MANUAL, false);
        
        editor = new MyItemEditor(owner);
        
        setModel(new MyTableModel());
        setItemEditor(editor);
        setRendererForColumn(0, new MyRenderer());
        setRendererForColumn(1, new MyRenderer());
        setRendererForColumn(2, new MyRenderer());
    }
    
    public void setSoundFiles(Path path, String[] fileNames) {
        editor.setSoundFiles(path, fileNames);
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
            super(new String[]{"Event","Notification","Sound"});
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
                setText("");
                setToolTipText("error");
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
                String channel = n.hasChannel() ? " ("+n.channel+")" : "";
                String matcher = n.hasMatcher() ? "&nbsp;"+n.getMatcherString() : "";
                text = String.format("%s%s\n%s",
                        n.type.label,
                        channel,
                        matcher);
            } else if (column == 1) {
                text = String.format("%s", n.getDesktopState());
            } else {
                text = String.format("%s\n%s", n.getSoundState(), n.soundFile);
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

    /**
     * The editor for a single usericon, which does the most work here, having
     * to load a list of icons for use, creating the icon when selected,
     * updating the preview and so on.
     */
    private static class MyItemEditor implements ItemEditor<Notification> {
        
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
        private final SimpleStringSetting channel;
        private final SimpleStringSetting matcher;
        private final ColorSetting foregroundColor;
        private final ColorSetting backgroundColor;
        private final ColorChooser colorChooser;
        private final DurationSetting soundCooldown;
        private final DurationSetting soundInactiveCooldown;
        private final ComboStringSetting soundFile;
        private final SliderLongSetting volumeSlider;
        private final JButton playSound;

        // State
        private Notification current;
        private Path soundsPath;
        private boolean save;
        
        public MyItemEditor(Window owner) {
            dialog = new JDialog(owner);
            dialog.setLayout(new GridBagLayout());
            dialog.setResizable(false);
            dialog.setModal(true);
            
            type = new GenericComboSetting<>(typeNames);
            type.setToolTipText("Choosing a type other than Addon replaces the corresponding default icon.");
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
            
            desktopState.addItemListener(e -> {
                if (desktopState.getSettingValue() == State.OFF) {
                    tabs.setTitleAt(0, "Notification (Off)");
                } else {
                    tabs.setTitleAt(0, "Notification");
                }
            });
            
            soundState.addItemListener(e -> {
                if (soundState.getSettingValue() == State.OFF) {
                    tabs.setTitleAt(1, "Sound (Off)");
                } else {
                    tabs.setTitleAt(1, "Sound");
                }
                updateSoundSettings();
            });
            
            JPanel desktop = new JPanel(new GridBagLayout());
            
            JPanel sound = new JPanel(new GridBagLayout());
            
            JPanel optionsPanel = new JPanel(new GridBagLayout());
            //optionsPanel.setBorder(BorderFactory.createTitledBorder("Event"));
            
            options = new JPanel();
            options.setLayout(new BoxLayout(options, BoxLayout.PAGE_AXIS));
            optionsAssoc = new HashMap<>();
            
            channel = new SimpleStringSetting(20, true);
            matcher = new SimpleStringSetting(20, true);
            
            optionsPanel.add(new JLabel("Channel:"), GuiUtil.makeGbc(0, 1, 1, 1));
            optionsPanel.add(channel, GuiUtil.makeGbc(1, 1, 1, 1));
            optionsPanel.add(new JLabel("Match:"), GuiUtil.makeGbc(0, 2, 1, 1));
            optionsPanel.add(matcher, GuiUtil.makeGbc(1, 2, 1, 1));
            optionsPanel.add(options, GuiUtil.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));
            
            colorChooser = new ColorChooser(dialog);
            foregroundColor = new ColorSetting(ColorSetting.FOREGROUND, null, "Foreground", "Foreground", colorChooser);
            backgroundColor = new ColorSetting(ColorSetting.BACKGROUND, null, "Background", "Background", colorChooser);
            ColorSettingListener colorChangeListener = new ColorSettingListener() {

                @Override
                public void colorUpdated() {
                    foregroundColor.update(backgroundColor.getSettingValue());
                    backgroundColor.update(foregroundColor.getSettingValue());
                }
            };
            foregroundColor.setListener(colorChangeListener);
            backgroundColor.setListener(colorChangeListener);
            
            
            soundFile = new ComboStringSetting(new String[]{});
            
            volumeSlider = new SliderLongSetting(JSlider.HORIZONTAL, VOLUME_MIN, VOLUME_MAX, 0);
            volumeSlider.setMajorTickSpacing(10);
            volumeSlider.setMinorTickSpacing(5);
            volumeSlider.setPaintTicks(true);
            
            
            soundCooldown = new DurationSetting(3, true);
            soundInactiveCooldown = new DurationSetting(3, true);
            

            GridBagConstraints gbc;

            //### Basic Settings Panel ###
            gbc = GuiUtil.makeGbc(0, 0, 1, 1);
            gbc.anchor = GridBagConstraints.WEST;
            optionsPanel.add(new JLabel("Type:"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 0, 2, 1);
            gbc.anchor = GridBagConstraints.WEST;
            optionsPanel.add(type, gbc);
            //-----------------------
            // Notification Settings
            //-----------------------
            gbc = GuiUtil.makeGbc(0, 1, 1, 1);
            desktop.add(new JLabel("Status:"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 1, 2, 1);
            gbc.anchor = GridBagConstraints.WEST;
            desktop.add(desktopState, gbc);
            
            gbc = GuiUtil.makeGbc(0, 2, 3, 1);
            desktop.add(foregroundColor, gbc);
            
            gbc = GuiUtil.makeGbc(0, 3, 3, 1);
            desktop.add(backgroundColor, gbc);
            
            //----------------
            // Sound Settings
            //----------------
            gbc = GuiUtil.makeGbc(0, 1, 1, 1);
            sound.add(new JLabel("Status:"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 1, 3, 1);
            gbc.anchor = GridBagConstraints.WEST;
            sound.add(soundState, gbc);
            
            gbc = GuiUtil.makeGbc(0, 2, 1, 1);
            sound.add(new JLabel("File:"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 2, 3, 1, GridBagConstraints.WEST);
            sound.add(soundFile, gbc);
            
            gbc = GuiUtil.makeGbc(0, 3, 1, 1);
            sound.add(new JLabel("Volume:"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 3, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            sound.add(volumeSlider, gbc);
            
            gbc = GuiUtil.makeGbc(0, 4, 1, 1);
            sound.add(new JLabel("Cooldown:"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 4, 1, 1);
            gbc.anchor = GridBagConstraints.WEST;
            sound.add(soundCooldown, gbc);
            
            gbc = GuiUtil.makeGbc(2, 4, 1, 1);
            sound.add(new JLabel("Passive Cooldown:"), gbc);
            
            gbc = GuiUtil.makeGbc(3, 4, 1, 1);
            gbc.anchor = GridBagConstraints.WEST;
            sound.add(soundInactiveCooldown, gbc);
            
            playSound = new JButton("Test Sound");
            playSound.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        String file = soundFile.getSettingValue();
                        long volume = volumeSlider.getSettingValue();
                        Sound.play(soundsPath.resolve(file), volume, "test", -1);
                    } catch (Exception ex) {
                        GuiUtil.showNonModalMessage(dialog, "Error Playing Sound",
                                ex.toString(),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            gbc = GuiUtil.makeGbc(2, 5, 2, 1);
            sound.add(playSound, gbc);
            
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
            
            tabs.addTab("Notification", GuiUtil.northWrap(desktop));
            tabs.addTab("Sound", GuiUtil.northWrap(sound));
            
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
        
        private void updateOkButton() {
            okButton.setEnabled(type.getSettingValue() != null);
        }
        
        private void updateSize() {
            dialog.pack();
        }
        
        private void updateSubTypes() {
            Type t = this.type.getSettingValue();
            options.removeAll();
            optionsAssoc.clear();
            for (String type : t.subTypes.keySet()) {
                JCheckBox checkbox = new JCheckBox(t.subTypes.get(type));
                checkbox.setName(type);
                System.out.println(current);
                if (current != null) {
                    checkbox.setSelected(current.options.contains(type));
                }
                options.add(checkbox);
                optionsAssoc.put(type, checkbox);
            }
            options.setVisible(!t.subTypes.isEmpty());
            updateSize();
        }
        
        private Collection<String> getSubTypes() {
            Collection<String> result = new LinkedList<>();
            for (String type : optionsAssoc.keySet()) {
                JCheckBox cb = optionsAssoc.get(type);
                if (cb.isSelected()) {
                    result.add(type);
                }
            }
            return result;
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
        
        @Override
        public Notification showEditor(Notification preset, Component c, boolean edit, int column) {
            if (edit) {
                dialog.setTitle("Edit notification settings");
            } else {
                dialog.setTitle("Add notification");
            }
            if (preset != null) {
                current = preset;
                
                type.setSettingValue(preset.type);
                channel.setSettingValue(preset.channel);
                matcher.setSettingValue(preset.matcher);
                desktopState.setSettingValue(preset.desktopState);
                foregroundColor.setSettingValue(HtmlColors.getColorString(preset.foregroundColor));
                backgroundColor.setSettingValue(HtmlColors.getColorString(preset.backgroundColor));

                // Sound
                soundState.setSettingValue(preset.soundState);
                soundFile.setSettingValue(preset.soundFile);
                System.out.println(preset.soundFile);
                soundCooldown.setSettingValue(Long.valueOf(preset.soundCooldown));
                soundInactiveCooldown.setSettingValue(Long.valueOf(preset.soundInactiveCooldown));
                volumeSlider.setSettingValue(preset.soundVolume);
                updateSubTypes();
            } else {
                current = null;
                
                type.setSelectedIndex(0);
                channel.setSettingValue(null);
                matcher.setSettingValue(null);
                desktopState.setSettingValue(Notification.State.ALWAYS);
                foregroundColor.setSettingValue("black");
                backgroundColor.setSettingValue(HtmlColors.getColorString(new Color(255, 255, 240)));
                
                // Sound
                soundState.setSettingValue(Notification.State.OFF);
                soundFile.setSelectedIndex(0);
                volumeSlider.setSettingValue(Long.valueOf(100));
                soundCooldown.setSettingValue(Long.valueOf(0));
                soundInactiveCooldown.setSettingValue(Long.valueOf(0));
                
                updateSubTypes();
                create();
            }
            
            // Default selected tab based on which table column was clicked on
            if (column == 2) {
                tabs.setSelectedIndex(1);
            } else {
                tabs.setSelectedIndex(0);
            }
            
            save = false;
            
            dialog.setLocationRelativeTo(c);
            dialog.setVisible(true);
            // Modal dialog, so blocks here and stuff can be changed via the GUI
            // until the dialog is closed
            
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
            b.setForeground(foreground);
            b.setBackground(background);
            b.setSoundFile(soundFile.getSettingValue());
            System.out.println(soundFile.getSettingValue());
            b.setVolume(volumeSlider.getSettingValue());
            b.setSoundCooldown(soundCooldown.getSettingValue(0L).intValue());
            b.setSoundInactiveCooldown(soundInactiveCooldown.getSettingValue(0L).intValue());
            b.setChannel(channel.getSettingValue());
            b.setMatcher(matcher.getSettingValue());
            b.setOptions(getSubTypes());
            
            current = new Notification(b);
            return current;
        }
        
        public void setSoundFiles(Path path, String[] names) {
            soundFile.removeAllItems();
            for (String name : names) {
                soundFile.add(name);
            }
            this.soundsPath = path;
        }
        
    }
    
}
