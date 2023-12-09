
package chatty.gui.components;

import chatty.Addressbook;
import chatty.AddressbookEntry;
import chatty.gui.RegexDocumentFilter;
import chatty.gui.components.settings.ListTableModel;
import chatty.gui.components.settings.TableEditor;
import chatty.util.StringUtil;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

/**
 *
 * @author tduva
 */
public class AddressbookEditor extends TableEditor<AddressbookEntry> {
    
    private final MyTableModel data = new MyTableModel();
    
    public AddressbookEditor(JDialog parent, TableEditorListener<AddressbookEntry> listener) {
        super(SORTING_MODE_SORTED, true);
        setModel(data);
        setTableEditorListener(listener);
        setItemEditor(() -> new MyEditor(parent));
        //this.setItemEditor(null);
    }
    
    public void edit(String name) {
        AddressbookEntry preset = new AddressbookEntry(name, new HashSet<String>());
        int index = data.indexOf(preset);
        if (index == -1) {
            addItem(preset);
        } else {
            editItem(index);
        }
    }

    public void setContextMenu(TableContextMenu menu) {
        setPopupMenu(menu);
    }
    
    public void setEntry(AddressbookEntry entry) {
        int index = data.indexOf(entry);
        if (index != -1) {
            data.set(index, entry);
        }
    }
    
    private static class MyTableModel extends ListTableModel<AddressbookEntry> {
        
        public MyTableModel() {
            super(new String[]{"Name", "Categories"});
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return get(rowIndex).getName();
            } else {
                return StringUtil.join(get(rowIndex).getCategories(), ",");
            }
        }
        
        @Override
        public Class getColumnClass(int c) {
            if (c == 0) {
                return String.class;
            } else {
                return String.class;
            }
        }
        
    }
    
    
    private static class MyEditor extends JDialog implements TableEditor.ItemEditor<AddressbookEntry> {

        private final JTextField name = new JTextField(15);
        private final JTextField categories = new JTextField(15);
        
        private final JButton ok = new JButton("Done");
        private final JButton cancel = new JButton("Cancel");
        
        private boolean save;
        
        public MyEditor(JDialog owner) {
            super(owner);
            setModal(true);
            setResizable(false);
            
            // Action Listener
            ActionListener listener = new ButtonListener();
            ok.addActionListener(listener);
            cancel.addActionListener(listener);
            name.addActionListener(listener);
            categories.addActionListener(listener);
            
            // Document Listener
            DocumentListener documentListener = new TextFieldListener();
            name.getDocument().addDocumentListener(documentListener);
            categories.getDocument().addDocumentListener(documentListener);
            
            // Prevents any whitespace from being entered in the name field
            ((AbstractDocument)name.getDocument()).setDocumentFilter(new RegexDocumentFilter("\\s+", this));
            
            // Layout
            setLayout(new GridBagLayout());
            GridBagConstraints gbc;
            
            JLabel nameLabel = new JLabel("Name: ");
            JLabel categoriesLabel = new JLabel("Categories: ");

            nameLabel.setLabelFor(name);
            gbc = makeGbc(0, 1, 1, 1, GridBagConstraints.EAST);
            add(nameLabel, gbc);

            gbc = makeGbc(1, 1, 2, 1, GridBagConstraints.CENTER);
            add(name, gbc);

            gbc = makeGbc(0, 2, 1, 1, GridBagConstraints.EAST);
            add(categoriesLabel, gbc);

            gbc = makeGbc(1,2, 2, 1, GridBagConstraints.CENTER);
            add(categories, gbc);

            gbc = makeGbc(1, 3, 1, 1, GridBagConstraints.CENTER);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            add(ok, gbc);

            gbc = makeGbc(2, 3, 1, 1, GridBagConstraints.CENTER);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            add(cancel, gbc);
            
            pack();
        }
        
        private GridBagConstraints makeGbc(int x, int y, int w, int h, int anchor) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = x;
            gbc.gridy = y;
            gbc.gridwidth = w;
            gbc.gridheight = h;
            gbc.insets = new Insets(5,3,5,3);
            gbc.anchor = anchor;
            return gbc;
        }
        
        @Override
        public AddressbookEntry showEditor(AddressbookEntry preset, Component c, boolean edit, int column) {
            setLocationRelativeTo(c);
            updateButtons();
            setData(preset);
            if (edit) {
                setTitle("Edit entry");
            } else {
                setTitle("Add entry");
            }
            name.requestFocusInWindow();
            setVisible(true);
            if (save && valid()) {
                return loadData();
            }
            return null;
        }
        
        private void setData(AddressbookEntry preset) {
            if (preset == null) {
                name.setText("");
                categories.setText("");
            } else {
                name.setText(preset.getName());
                categories.setText(StringUtil.join(preset.getCategories(), ","));
            }
        }
        
        private AddressbookEntry loadData() {
            String name = this.name.getText();
            Set<String> categories =
                    Addressbook.getCategoriesFromString(this.categories.getText());
            return new AddressbookEntry(name, categories);
        }
        
        private void updateButtons() {
            boolean enabled = valid();
            ok.setEnabled(enabled);
        }
        
        private boolean valid() {
            return !name.getText().isEmpty();
        }
        
        private class ButtonListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == ok) {
                    save = true;
                    setVisible(false);
                } else if (e.getSource() == cancel) {
                    save = false;
                    setVisible(false);
                }
                if (e.getSource() == name || e.getSource() == categories) {
                    if (valid()) {
                        save = true;
                        setVisible(false);
                    }
                }
            }
        }
        
        
        private class TextFieldListener implements DocumentListener {

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
            
        }
        
    }
    
}
