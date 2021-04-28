
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.RegexDocumentFilter;
import chatty.gui.components.settings.SimpleTableEditor.MapItem;
import chatty.lang.Language;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
public abstract class SimpleTableEditor<T> extends TableEditor<MapItem<T>> implements MapSetting<String, T> {

    private final MyTableModel<T> data;
    private final MyItemEditor editor;
    
    public SimpleTableEditor(JDialog owner, Class<T> valueClass) {
        super(SORTING_MODE_SORTED, false);
        data = new MyTableModel<>(valueClass);
        setModel(data);
        editor = new MyItemEditor(owner);
        setItemEditor(editor);
    }
    
    public void edit(String item) {
        MapItem preset = new MapItem(item, "");
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
    
    public void setValueDocumentFilter(DocumentFilter filter) {
        editor.setValueFilter(filter);
    }

    @Override
    public Map<String, T> getSettingValue() {
        Map<String, T> map = new HashMap<>();
        for (MapItem<T> item : data.getData()) {
            map.put(item.key, item.value);
        }
        return map;
    }

    @Override
    public void setSettingValue(Map<String, T> values) {
        data.clear();
        List<MapItem<T>> items = new ArrayList<>();
        for (String key : values.keySet()) {
            items.add(new MapItem<>(key, values.get(key)));
        }
        setData(items);
    }

    private boolean hasKey(String key) {
        for (MapItem item : data.getData()) {
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
    static class MapItem<T> {
        public final String key;
        public final T value;
        
        public MapItem(String key, T value) {
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
            final MapItem other = (MapItem) obj;
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
    
    

    private static class MyTableModel<T> extends ListTableModel<MapItem<T>> {

        private Class<T> valueClass;
        
        public MyTableModel(Class<T> valueClass) {
            super(new String[]{"Key", "Value"});
            this.valueClass = valueClass;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return get(rowIndex).key;
            } else {
                return get(rowIndex).value;
            }
        }
        
        @Override
        public Class getColumnClass(int column) {
            if (column == 0) {
                return String.class;
            }
            return valueClass;
        }
    }

    private class MyItemEditor implements ItemEditor<MapItem<T>> {

        private final JDialog dialog;
        private final JTextField key = new JTextField(20);
        private final JTextField value = new JTextField();
        
        private final JButton ok = new JButton(Language.getString("dialog.button.save"));
        private final JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
        
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
            GuiUtil.installEscapeCloseOperation(dialog);
            key.getDocument().addDocumentListener(documentListener);
            value.getDocument().addDocumentListener(documentListener);
            key.addActionListener(e -> save());
            value.addActionListener(e -> save());
            
            ActionListener listener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == ok) {
                        save();
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
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(ok, gbc);
            ok.setMnemonic(KeyEvent.VK_S);
            gbc = GuiUtil.makeGbc(2, 2, 1, 1);
            dialog.add(cancel, gbc);
            
            dialog.pack();
            dialog.setResizable(false);
        }
        
        private void save() {
            if (!getKey().equals(presetKey) && hasKey(getKey())) {
                JOptionPane.showMessageDialog(dialog,
                        "An item with the key '" + getKey() + "' already exists");
            }
            else {
                dialog.setVisible(false);
                save = true;
            }
        }
        
        @Override
        public MapItem<T> showEditor(MapItem<T> preset, Component c, boolean edit, int column) {
            save = false;
            if (edit) {
                dialog.setTitle("Edit item");
            } else {
                dialog.setTitle("Add item");
            }
            dialog.setLocationRelativeTo(c);
            if (preset != null) {
                key.setText(preset.key);
                value.setText(String.valueOf(preset.value));
                presetKey = preset.key;
            } else {
                key.setText(null);
                value.setText(null);
                presetKey = null;
            }
            if (column == 0 || preset == null) {
                key.requestFocusInWindow();
            }
            else if (column == 1) {
                value.requestFocusInWindow();
            }
            updateButtons();
            dialog.setVisible(true);
            if (save) {
                return new MapItem<>(getKey(), valueFromString(value.getText()));
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
