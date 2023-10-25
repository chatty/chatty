
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.RegexDocumentFilter;
import chatty.gui.components.routing.RoutingEntry;
import static chatty.gui.components.settings.TableEditor.SORTING_MODE_MANUAL;
import chatty.lang.Language;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

/**
 *
 * @author tduva
 * @param <T>
 */
public class RoutingSettingsTable<T extends RoutingEntry> extends TableEditor<RoutingEntry> {

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
    }
    
    private static class MyTableModel<T extends RoutingEntry> extends ListTableModel<RoutingEntry> {
        
        public MyTableModel() {
            super(new String[]{"Name", "Settings"});
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RoutingEntry entry = get(rowIndex);
            switch (columnIndex) {
                case 0: return entry.getName();
                case 1: return entry.makeInfo();
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
    
    public static class MyItemEditor<T extends RoutingEntry> implements TableEditor.ItemEditor<RoutingEntry> {
        
        private final JDialog dialog;
        private final JTextField name = new JTextField(10);
        private final JCheckBox logEnabled = new JCheckBox(Language.getString("settings.customTabSettings.logEnabled"));
        private final JTextField logFile = new JTextField(10);
        private final ComboLongSetting openOnMessage;
        private final JCheckBox exclusive;
        private final JButton ok = new JButton("Done");
        private final JButton cancel = new JButton("Cancel");
        
        boolean save;
        
        public MyItemEditor(JDialog owner, Component info) {
            dialog = new JDialog(owner);
            dialog.setTitle("Edit Item");
            dialog.setModal(true);
            
            ((AbstractDocument) logFile.getDocument()).setDocumentFilter(new RegexDocumentFilter("[^a-zA-Z]"));
            
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
            
            
            openOnMessage = new ComboLongSetting(RoutingEntry.getOpenOnMessageValues());
            exclusive = new JCheckBox("Exclusive");
            
            dialog.add(openOnMessage,
                    GuiUtil.makeGbc(1, 1, 2, 1, GridBagConstraints.WEST));
//            dialog.add(exclusive,
//                    GuiUtil.makeGbc(1, 4, 2, 1, GridBagConstraints.WEST));
            
            // Log Panel
            JPanel logPanel = new JPanel(new GridBagLayout());
            logPanel.setBorder(BorderFactory.createTitledBorder("Log to file"));
            
            gbc = GuiUtil.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST);
            logPanel.add(logEnabled, gbc);
            
            SettingsUtil.addLabeledComponent(logPanel,
                    "settings.customTabSettings.logFile",
                    0, 1, 1, GridBagConstraints.WEST,
                    logFile, true);
            
            gbc = GuiUtil.makeGbc(2, 1, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(5, 0, 5, 5);
            logPanel.add(new JLabel(".log"), gbc);
            
            gbc = GuiUtil.makeGbc(0, 2, 3, 1, GridBagConstraints.WEST);
            logPanel.add(new JLabel(Language.getString("settings.customTabSettings.logInfo")), gbc);
            
            gbc = GuiUtil.makeGbc(0, 5, 3, 1);
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
        public RoutingEntry showEditor(RoutingEntry preset, Component c, boolean edit, int column) {
            if (edit) {
                dialog.setTitle("Edit item");
            } else {
                dialog.setTitle("Add item");
            }
            dialog.setLocationRelativeTo(c);
            if (preset != null) {
                name.setText(preset.getName());
                openOnMessage.setSettingValue((long) preset.openOnMessage);
                exclusive.setSelected(preset.exclusive);
                logEnabled.setSelected(preset.logEnabled);
                logFile.setText(preset.logFile);
            } else {
                name.setText(null);
                openOnMessage.setSettingValue(1L);
                exclusive.setSelected(false);
                logEnabled.setSelected(false);
                logFile.setText(null);
            }
            name.requestFocusInWindow();
            updateButtons();
            
            // Save will be set to true when pressing the "OK" button
            save = false;
            dialog.setVisible(true);
            if (!name.getText().isEmpty() && save) {
                return new RoutingEntry(
                        name.getText(),
                        openOnMessage.getSettingValue().intValue(),
                        exclusive.isSelected(),
                        logEnabled.isSelected(),
                        logFile.getText());
            }
            return null;
        }
        
        private void updateButtons() {
            boolean enabled = !name.getText().isEmpty();
            ok.setEnabled(enabled);
        }
        
    }
    
}
