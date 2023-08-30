
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.routing.RoutingEntry;
import static chatty.gui.components.settings.TableEditor.SORTING_MODE_MANUAL;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author tduva
 * @param <T>
 */
public class RoutingSettingsTable<T extends RoutingEntry> extends TableEditor<RoutingEntry> {

    private final MyTableModel<T> data;
    private MyItemEditor<T> editor;
    
    private static final Map<Long, String> openOnMessageValues = new HashMap<>();
    
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
        
        openOnMessageValues.put(0L, "Don't open on message");
        openOnMessageValues.put(1L, "Open on any message");
        openOnMessageValues.put(2L, "Open on regular chat message");
        openOnMessageValues.put(3L, "Open on info message");
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
                case 1: return openOnMessageValues.get((long) entry.openOnMessage);
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
        private final ComboLongSetting openOnMessage;
        private final JCheckBox exclusive;
        private final JButton ok = new JButton("Done");
        private final JButton cancel = new JButton("Cancel");
        
        boolean save;
        
        public MyItemEditor(JDialog owner, Component info) {
            dialog = new JDialog(owner);
            dialog.setTitle("Edit Item");
            dialog.setModal(true);
            
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
            
            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc;
            
            dialog.add(new JLabel("Name:"),
                    GuiUtil.makeGbc(0, 0, 1, 1));
            gbc = GuiUtil.makeGbc(1, 0, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(name, gbc);
            
            
            openOnMessage = new ComboLongSetting(openOnMessageValues);
            exclusive = new JCheckBox("Exclusive");
            
            dialog.add(openOnMessage,
                    GuiUtil.makeGbc(1, 1, 2, 1, GridBagConstraints.WEST));
//            dialog.add(exclusive,
//                    GuiUtil.makeGbc(1, 4, 2, 1, GridBagConstraints.WEST));
            
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
            } else {
                name.setText(null);
                openOnMessage.setSettingValue(1L);
                exclusive.setSelected(false);
            }
            name.requestFocusInWindow();
            updateButtons();
            
            // Save will be set to true when pressing the "OK" button
            save = false;
            dialog.setVisible(true);
            if (!name.getText().isEmpty() && save) {
                return new RoutingEntry(name.getText(), openOnMessage.getSettingValue().intValue(), exclusive.isSelected());
            }
            return null;
        }
        
        private void updateButtons() {
            boolean enabled = !name.getText().isEmpty();
            ok.setEnabled(enabled);
        }
        
    }
    
}
