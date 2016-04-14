
package chatty.gui.components;

import chatty.Addressbook;
import chatty.AddressbookEntry;
import chatty.gui.components.settings.TableEditor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class AddressbookDialog extends JDialog {
    
    private final AddressbookEditor table;
    private final JFrame owner;
    private final Addressbook addressbook;
    
    public AddressbookDialog(JFrame owner, Addressbook addressbook) {
        super(owner);
        setTitle("Addressbook");
        this.addressbook = addressbook;
        this.owner = owner;
        table = new AddressbookEditor(this, new TableListener());
        table.setContextMenu(new ContextMenu());
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
        add(table, gbc);
        
        setPreferredSize(new Dimension(300,400));
        
        pack();
    }
    
    private class ContextMenu extends TableEditor.TableContextMenu<AddressbookEntry> {

        private final ActionListener actionListener;
        private AddressbookEntry currentEntry;
        
        public ContextMenu() {
            actionListener = new Action();
        }
        
        @Override
        public void showMenu(AddressbookEntry entry, Component invoker, int x, int y) {
            currentEntry = entry;
            removeAll();
            List<String> presetCategories = addressbook.getCategories();
            Set<String> categories = entry.getCategories();
            for (String cat : presetCategories) {
                boolean selected = categories.contains(cat);
                JCheckBoxMenuItem item = new JCheckBoxMenuItem(cat, selected);
                item.addActionListener(actionListener);
                item.setActionCommand(cat);
                add(item);
            }
            this.show(invoker, x, y);
        }
        
        private class Action implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                String category = e.getActionCommand();
                AddressbookEntry entry = currentEntry;
                boolean selected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                AddressbookEntry changedEntry;
                if (selected) {
                    changedEntry = addressbook.add(entry.getName(), category);
                } else {
                    changedEntry = addressbook.remove(entry.getName(), category);
                }
                table.setEntry(changedEntry);
            }
            
        }
        
    }
    
        
    private class TableListener implements TableEditor.TableEditorListener<AddressbookEntry> {

        @Override
        public void itemAdded(AddressbookEntry item) {
            addressbook.set(item);
        }

        @Override
        public void itemRemoved(AddressbookEntry item) {
            addressbook.remove(item);
        }

        @Override
        public void itemEdited(AddressbookEntry oldItem, AddressbookEntry newItem) {
            if (oldItem.getName().equals(newItem.getName())) {
                addressbook.set(newItem);
            } else {
                addressbook.rename(oldItem.getName(), newItem);
            }
        }

        @Override
        public void refreshData() {
            loadData();
        }
        
    }
    
    private void loadData() {
        table.setData(addressbook.getEntries());
    }
    
    public void showDialog(final String name) {
        if (!isVisible()) {
            loadData();
            setVisible(true);
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (name != null) {
                    table.edit(name);
                }
            }
        });
    }
    
    
}
