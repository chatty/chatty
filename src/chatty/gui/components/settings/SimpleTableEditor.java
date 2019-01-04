
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.RegexDocumentFilter;
import chatty.gui.components.settings.SimpleTableEditor.StringMapItem;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;

/**
 * A map setting with a String as key/value. Implement the abstract method to
 * parse the value into the desired type.
 * 
 * @author tduva
 */
public abstract class SimpleTableEditor<T> extends TableEditor<StringMapItem> implements MapSetting<String, T> {

    private final MyTableModel data = new MyTableModel();
    private final MyItemEditor editor;
    
    public SimpleTableEditor(JDialog owner) {
        super(SORTING_MODE_SORTED, false);
        setModel(data);
        editor = new MyItemEditor(owner);
        setItemEditor(editor);
    }
    
    public void edit(String item) {
        StringMapItem preset = new StringMapItem(item, "");
        int index = data.indexOf(preset);
        if (index == -1) {
            addItem(preset);
        } else {
            editItem(index);
        }
    }
    
    protected abstract T valueFromString(String input);
    
    public void setKeyFilter(String p) {
        editor.setKeyFilter(new RegexDocumentFilter(p));
    }
    
    public void setValueFilter(String p) {
        editor.setValueFilter(new RegexDocumentFilter(p));
    }

    @Override
    public Map<String, T> getSettingValue() {
        Map<String, T> map = new HashMap<>();
        for (StringMapItem item : data.getData()) {
            map.put(item.key, valueFromString(item.value));
        }
        return map;
    }

    @Override
    public void setSettingValue(Map<String, T> values) {
        data.clear();
        Collection<StringMapItem> items = new ArrayList<>();
        for (String key : values.keySet()) {
            String value = String.valueOf(values.get(key));
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
    
    /**
     * Simple key/value String pair. Counts as equal if the key is equal
     * (intended to be used in the context of a table where each key should only
     * occur once).
     */
    static class StringMapItem {
        private final String key;
        private final String value;
        
        public StringMapItem(String key, String value) {
            this.key = key;
            this.value = value;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StringMapItem other = (StringMapItem) obj;
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + Objects.hashCode(this.key);
            return hash;
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
        
        private String presetKey;
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
                        if (!getKey().equals(presetKey) && hasKey(getKey())) {
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
            GridBagConstraints gbc;
            
            gbc = GuiUtil.makeGbc(0, 0, 1, 1);
            dialog.add(new JLabel("Key:"), gbc);
            gbc = GuiUtil.makeGbc(1, 0, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            dialog.add(key, gbc);
            
            gbc = GuiUtil.makeGbc(0, 1, 1, 1);
            dialog.add(new JLabel("Value:"), gbc);
            gbc = GuiUtil.makeGbc(1, 1, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            dialog.add(value, gbc);
            
            gbc = GuiUtil.makeGbc(1, 2, 1, 1);
            dialog.add(ok, gbc);
            gbc = GuiUtil.makeGbc(2, 2, 1, 1);
            dialog.add(cancel, gbc);
            
            dialog.pack();
            dialog.setResizable(false);
        }
        
        @Override
        public StringMapItem showEditor(StringMapItem preset, Component c, boolean edit, int column) {
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
                presetKey = preset.key;
            } else {
                key.setText(null);
                value.setText(null);
                presetKey = null;
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
        
        public void setValueFilter(DocumentFilter keyFilter) {
            ((AbstractDocument)value.getDocument()).setDocumentFilter(keyFilter);
        }
    }
}
