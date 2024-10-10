
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.RegexDocumentFilter;
import chatty.gui.components.routing.RoutingTargetSettings;
import static chatty.gui.components.settings.SettingsUtil.createLabel;
import static chatty.gui.components.settings.TableEditor.SORTING_MODE_MANUAL;
import chatty.lang.Language;
import chatty.util.FileUtil;
import chatty.util.StringUtil;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

/**
 *
 * @author tduva
 * @param <T>
 */
public class RoutingSettingsTable<T extends RoutingTargetSettings> extends TableEditor<RoutingTargetSettings> {

    private static final int NAME_COLUMN = 0;
    private static final int SETTINGS_COLUMN = 1;
    private static final int LOG_COLUMN = 2;
    
    private final MyTableModel<T> data;
    private MyItemEditor<T> editor;
    
    public RoutingSettingsTable(JDialog owner, Component info) {
        super(SORTING_MODE_MANUAL, false);
        this.data = new MyTableModel<>();
        
        setModel(data);
        setItemEditor(() -> {
            if (editor == null) {
                editor = new MyItemEditor<>(owner, info);
            }
            return editor;
        });
        
        setFixedColumnWidth(NAME_COLUMN, 180);
    }
    
    private static class MyTableModel<T extends RoutingTargetSettings> extends ListTableModel<RoutingTargetSettings> {
        
        public MyTableModel() {
            super(new String[]{"Name", "Settings", "Log file"});
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RoutingTargetSettings entry = get(rowIndex);
            switch (columnIndex) {
                case NAME_COLUMN:
                    return entry.getName();
                case SETTINGS_COLUMN:
                    return entry.makeSettingsInfo();
                case LOG_COLUMN:
                    return entry.getFullLogFilename();
            }
            return "";
        }
        
//        @Override
//        public String getSearchValueAt(int rowIndex, int columnIndex) {
//            if (columnIndex == 0) {
//                return get(rowIndex).getId();
//            } else if (columnIndex == 1) {
//                return HtmlColors.getNamedColorString(get(rowIndex).getForeground());
//            } else {
//                return HtmlColors.getNamedColorString(get(rowIndex).getBackground());
//            }
//        }
        
        @Override
        public Class getColumnClass(int c) {
            return String.class;
        }
        
    }
    
    public static class MyItemEditor<T extends RoutingTargetSettings> implements TableEditor.ItemEditor<RoutingTargetSettings> {
        
        private static final String HTML_PREFIX = "<html><body style='width:200px;'>";
        
        private final JDialog dialog;
        private final JTextField name = new JTextField(10);
        private final JCheckBox logEnabled = new JCheckBox(Language.getString("settings.customTabSettings.logEnabled"));
        private final JTextField logFile = new JTextField(10);
        private final ComboLongSetting openOnMessage;
        private final ComboLongSetting channelLogo;
        private final JCheckBox exclusive;
        private final JRadioButton multiChannelAll = new JRadioButton();
        private final JRadioButton multiChannelSep = new JRadioButton();
        private final JRadioButton multiChannelSepAndAll = new JRadioButton();
        private final JCheckBox channelFixed = new JCheckBox();
        private final JButton ok = new JButton("Done");
        private final JButton cancel = new JButton("Cancel");
        
        boolean save;
        
        public MyItemEditor(JDialog owner, Component info) {
            dialog = new JDialog(owner);
            dialog.setTitle("Edit Item");
            dialog.setModal(true);
            
            ButtonGroup multiChannelGroup = new ButtonGroup();
            multiChannelGroup.add(multiChannelAll);
            multiChannelGroup.add(multiChannelSep);
            multiChannelGroup.add(multiChannelSepAndAll);
            
            SettingsUtil.setTextAndTooltip(multiChannelAll, "settings.customTabSettings.multiChannelAll");
            SettingsUtil.setTextAndTooltip(multiChannelSep, "settings.customTabSettings.multiChannelSep");
            SettingsUtil.setTextAndTooltip(multiChannelSepAndAll, "settings.customTabSettings.multiChannelSepAndAll");
            SettingsUtil.setTextAndTooltip(channelFixed, "settings.customTabSettings.channelFixed");
            
            ((AbstractDocument) logFile.getDocument()).setDocumentFilter(new RegexDocumentFilter(FileUtil.ILLEGAL_FILENAME_CHARACTERS_PATTERN.pattern(), logFile));
            
            name.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateButtons();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateButtons();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateButtons();
                }
            });
            
            ActionListener listener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == ok) {
                        dialog.setVisible(false);
                        save = true;
                    } else if (e.getSource() == cancel) {
                        dialog.setVisible(false);
                    }
                }
            };
            ok.addActionListener(listener);
            cancel.addActionListener(listener);
            
            //--------
            // Layout
            //--------
            
            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc;
            
            dialog.add(new JLabel("Name:"),
                    GuiUtil.makeGbc(0, 0, 1, 1));
            gbc = GuiUtil.makeGbc(1, 0, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(name, gbc);
            
            
            JPanel generalPanel = new JPanel(new GridBagLayout());
            generalPanel.setBorder(BorderFactory.createTitledBorder("General"));
            
            openOnMessage = new ComboLongSetting(RoutingTargetSettings.getOpenOnMessageValues());
            channelLogo = new ComboLongSetting(makeChannelLogoValues());
            exclusive = new JCheckBox("Exclusive");
            
            generalPanel.add(openOnMessage,
                    GuiUtil.makeGbc(1, 1, 2, 1, GridBagConstraints.WEST));
            
            generalPanel.add(new JLabel("Channel Logos:"),
                    GuiUtil.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));
            
            generalPanel.add(channelLogo,
                       GuiUtil.makeGbc(2, 2, 1, 1, GridBagConstraints.WEST));
//            dialog.add(exclusive,
//                    GuiUtil.makeGbc(1, 4, 2, 1, GridBagConstraints.WEST));
            
            //--------------------
            // Multichannel Panel
            //--------------------
            JPanel multiChannelPanel = new JPanel(new GridBagLayout());
            multiChannelPanel.setBorder(BorderFactory.createTitledBorder("Show messages by channel"));
            
            gbc = SettingsDialog.makeGbcCloser(0, 0, 2, 1, GridBagConstraints.WEST);
            multiChannelPanel.add(multiChannelAll, gbc);
            
            gbc = SettingsDialog.makeGbcCloser(0, 1, 2, 1, GridBagConstraints.WEST);
            multiChannelPanel.add(multiChannelSep, gbc);
            
            gbc = SettingsDialog.makeGbcCloser(0, 2, 2, 1, GridBagConstraints.WEST);
            multiChannelPanel.add(multiChannelSepAndAll, gbc);
            
            gbc = SettingsDialog.makeGbcSub(0, 3, 2, 1, GridBagConstraints.WEST);
            multiChannelPanel.add(channelFixed, gbc);
            
            gbc = SettingsDialog.makeGbc(0, 4, 2, 1, GridBagConstraints.CENTER);
            multiChannelPanel.add(new JLabel(HTML_PREFIX+"Switch channels through the context menu. Changing this setting only applies to new messages."), gbc);
            
            SettingsUtil.addSubsettings(
                    new JRadioButton[]{multiChannelSep, multiChannelSepAndAll},
                    channelFixed);
            
            //-----------
            // Log Panel
            //-----------
            JPanel logPanel = new JPanel(new GridBagLayout());
            logPanel.setBorder(BorderFactory.createTitledBorder("Log to file"));
            
            // Log enabled
            gbc = GuiUtil.makeGbc(0, 0, 4, 1, GridBagConstraints.WEST);
            logPanel.add(logEnabled, gbc);
            
            // Log file name
            JLabel logFileLabel = createLabel("settings.customTabSettings.logFile");
            logFileLabel.setLabelFor(logFile);
            gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST);
            logPanel.add(logFileLabel, gbc);
            
            gbc = GuiUtil.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(5, 0, 5, 0);
            logPanel.add(new JLabel("customTab-"), gbc);
            
            gbc = GuiUtil.makeGbc(2, 1, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(5, 2, 5, 2);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            logPanel.add(logFile, gbc);
            
            gbc = GuiUtil.makeGbc(3, 1, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(5, 0, 5, 5);
            logPanel.add(new JLabel(".log"), gbc);
            
            // Log info
            gbc = GuiUtil.makeGbc(0, 2, 4, 1, GridBagConstraints.WEST);
            logPanel.add(new JLabel(HTML_PREFIX+Language.getString("settings.customTabSettings.logInfo")), gbc);
            
            gbc = GuiUtil.makeGbc(0, 6, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(generalPanel, gbc);
            
            gbc = GuiUtil.makeGbc(0, 7, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(multiChannelPanel, gbc);
            
            gbc = GuiUtil.makeGbc(0, 8, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(logPanel, gbc);
            
            if (info != null) {
                gbc = GuiUtil.makeGbc(0, 10, 3, 1, GridBagConstraints.CENTER);
                gbc.weightx = 1;
                dialog.add(info, gbc);
            }
            
            gbc = GuiUtil.makeGbc(1, 11, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            dialog.add(ok, gbc);
            gbc = GuiUtil.makeGbc(2, 11, 1, 1);
            dialog.add(cancel, gbc);
            
            dialog.pack();
            dialog.setResizable(false);
        }
        
        @Override
        public RoutingTargetSettings showEditor(RoutingTargetSettings preset, Component c, boolean edit, int column) {
            if (edit) {
                dialog.setTitle("Edit item");
            } else {
                dialog.setTitle("Add item");
            }
            dialog.setLocationRelativeTo(c);
            if (preset != null) {
                name.setText(preset.getName());
                openOnMessage.setSettingValue((long) preset.openOnMessage);
                channelLogo.setSettingValue((long) preset.channelLogo);
                exclusive.setSelected(preset.exclusive);
                logEnabled.setSelected(preset.logEnabled);
                logFile.setText(preset.getRawLogFilename());
                if (StringUtil.isNullOrEmpty(preset.getRawLogFilename())) {
                    logFile.setText(preset.getName());
                }
                switch (preset.multiChannel) {
                    case 0:
                        multiChannelAll.setSelected(true);
                        break;
                    case 1:
                        multiChannelSep.setSelected(true);
                        break;
                    case 2:
                        multiChannelSepAndAll.setSelected(true);
                        break;
                }
                channelFixed.setSelected(preset.channelFixed);
            } else {
                name.setText(null);
                openOnMessage.setSettingValue(1L);
                channelLogo.setSettingValue((long) RoutingTargetSettings.CHANNEL_LOGO_DEFAULT);
                exclusive.setSelected(false);
                logEnabled.setSelected(false);
                logFile.setText(null);
                multiChannelAll.setSelected(true);
                channelFixed.setSelected(false);
            }
            name.requestFocusInWindow();
            updateButtons();
            
            // Save will be set to true when pressing the "OK" button
            save = false;
            dialog.setVisible(true);
            if (!name.getText().isEmpty() && save) {
                return new RoutingTargetSettings(
                        name.getText(),
                        openOnMessage.getSettingValue().intValue(),
                        exclusive.isSelected(),
                        logEnabled.isSelected(),
                        logFile.getText(),
                        getMultiChannelValue(),
                        channelFixed.isSelected(),
                        preset != null ? preset.showAll : false,
                        channelLogo.getSettingValue().intValue());
            }
            return null;
        }
        
        private int getMultiChannelValue() {
            if (multiChannelSep.isSelected()) {
                return 1;
            }
            if (multiChannelSepAndAll.isSelected()) {
                return 2;
            }
            return 0;
        }
        
        private void updateButtons() {
            boolean enabled = !name.getText().isEmpty();
            ok.setEnabled(enabled);
        }
        
        private Map<Long, String> makeChannelLogoValues() {
            Map<Long, String> result = new LinkedHashMap<>();
            result.put(0L, "Off");
            for (int i = 12; i <= 30; i += 2) {
                result.put((long) i, i + "px");
            }
            return result;
        }
        
    }
    
}
