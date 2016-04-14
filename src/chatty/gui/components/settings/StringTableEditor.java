
package chatty.gui.components.settings;

import chatty.UsercolorItem;
import chatty.gui.HtmlColors;
import chatty.gui.RegexDocumentFilter;
import chatty.gui.components.settings.StringTableEditor.StringMapItem;
import static chatty.gui.components.settings.TableEditor.SORTING_MODE_MANUAL;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;

/**
 *
 * @author tduva
 */
public class StringTableEditor extends TableEditor<StringMapItem> implements MapSetting<String, String> {

    private final MyTableModel data = new MyTableModel();
    private final MyItemEditor editor;
    
    public StringTableEditor(JDialog owner) {
        super(SORTING_MODE_SORTED, false);
        setModel(data);
        editor = new MyItemEditor(owner);
        setItemEditor(editor);
    }
    
    public void setKeyFilter(String p) {
        editor.setKeyFilter(new RegexDocumentFilter(p));
    }

    @Override
    public Map<String, String> getSettingValue() {
        Map<String, String> map = new HashMap<>();
        for (StringMapItem item : data.getData()) {
            map.put(item.key, item.value);
        }
        return map;
    }

    @Override
    public void setSettingValue(Map<String, String> values) {
        data.clear();
        Collection<StringMapItem> items = new ArrayList<>();
        for (String key : values.keySet()) {
            String value = values.get(key);
            items.add(new StringMapItem(key, value));
        }
        data.setData(items);
    }

    private boolean hasKey(String key) {
        for (StringMapItem item : data.getData()) {
            if (item.key.equals(key)) {
                return true;
            }
        }
        return false;
    }
    
    static class StringMapItem {
        private final String key;
        private final String value;
        
        public StringMapItem(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
    
    

    private static class MyTableModel extends ListTableModel<StringMapItem> {

        public MyTableModel() {
            super(new String[]{"Key", "Value"});
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return get(rowIndex).key;
            } else {
                return get(rowIndex).value;
            }
        }
    }

    private class MyItemEditor implements ItemEditor<StringMapItem> {

        private final JDialog dialog;
        private final JTextField key = new JTextField();
        private final JTextField value = new JTextField();
        
        private final JButton ok = new JButton("Done");
        private final JButton cancel = new JButton("Cancel");
        
        private boolean save;
        
        public MyItemEditor(JDialog owner) {
            dialog = new JDialog(owner);
            dialog.setTitle("Edit Item");
            dialog.setModal(true);
            
            DocumentListener documentListener = new DocumentListener() {

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
            };
            key.getDocument().addDocumentListener(documentListener);
            value.getDocument().addDocumentListener(documentListener);
            
            ActionListener listener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == ok) {
                        if (hasKey(getKey())) {
                            JOptionPane.showMessageDialog(dialog,
                                    "An item with the key '"+getKey()+"' already exists");
                        } else {
                            dialog.setVisible(false);
                            save = true;
                        }
                    } else if (e.getSource() == cancel) {
                        dialog.setVisible(false);
                    }
                }
            };
            ok.addActionListener(listener);
            cancel.addActionListener(listener);
            
            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            gbc.insets = new Insets(3, 3, 3, 3);
            gbc.gridwidth = 2;
            dialog.add(key, gbc);
            gbc.gridx = 0;
            gbc.gridy = 1;
            dialog.add(value, gbc);
            gbc.gridwidth = 1;
            gbc.gridx = 2;
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.insets = new Insets(7, 2, 4, 2);
            dialog.add(ok, gbc);
            gbc.gridx = 1;
            dialog.add(cancel, gbc);
            dialog.pack();
            dialog.setResizable(false);
        }
        
        @Override
        public StringMapItem showEditor(StringMapItem preset, Component c, boolean edit) {
            save = false;
            if (edit) {
                dialog.setTitle("Edit item");
            } else {
                dialog.setTitle("Add item");
            }
            dialog.setLocationRelativeTo(c);
            if (preset != null) {
                key.setText(preset.key);
                value.setText(preset.value);
            } else {
                key.setText(null);
                value.setText(null);
            }
            updateButtons();
            key.requestFocusInWindow();
            dialog.setVisible(true);
            if (save) {
                return new StringMapItem(getKey(), value.getText());
            }
            return null;
        }
        
        private String getKey() {
            return key.getText();
        }
        
        private void updateButtons() {
            boolean enabled = !getKey().isEmpty() && !value.getText().isEmpty();
            ok.setEnabled(enabled);
        }
        
        public void setKeyFilter(DocumentFilter keyFilter) {
            ((AbstractDocument)key.getDocument()).setDocumentFilter(keyFilter);
        }
    }
}
