
package chatty.gui.components.settings;

import chatty.gui.components.LinkLabelListener;
import chatty.gui.components.settings.Editor.Tester;
import chatty.lang.Language;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A simple panel to edit (add/remove Strings) a list.
 * 
 * @author tduva
 */
public class ListSelector extends JPanel implements ListSetting<String> {
    
    private static final Dimension BUTTON_SIZE = new Dimension(27,27);
    
    private final JList<String> list = new JList<>();
    private final DefaultListModel<String> data = new DefaultListModel<>();
    private final JButton add = new JButton();
    private final JButton remove = new JButton();
    private final JButton change = new JButton();
    private final JButton moveUp = new JButton();
    private final JButton moveDown = new JButton();
    private final JButton sort = new JButton();
    private final JButton editAll = new JButton();
    private final JTextField input = new JTextField();
    
    private final String title;
    
    private String info;
    
    private DataFormatter<String> formatter;
    
    private StringEditor editor;
    private final Editor allEditor;
    
    private Consumer<List<String>> changeListener;

    public ListSelector(Window parent, String title, boolean manualSorting,
            boolean alphabeticSorting) {
        
        this.title = title;
        
        // Button actions
        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == add || e.getSource() == input) {
                    addItem();
                }
                else if (e.getSource() == remove) {
                    removeItem();
                }
                else if (e.getSource() == change) {
                    changeItem();
                }
                else if (e.getSource() == moveUp) {
                    moveUp();
                }
                else if (e.getSource() == moveDown) {
                    moveDown();
                }
                else if (e.getSource() == sort) {
                    sort();
                }
                else if (e.getSource() == editAll) {
                    editAll();
                }
            }
        };
        
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeItems");
        list.getActionMap().put("removeItems", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeItem();
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editItem");
        list.getActionMap().put("editItem", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                changeItem();
            }
        });
        
        // List double-click
        list.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    changeItem();
                }
            }
            
        });
        
        // List selection changes
        list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEditButtons();
            }
        });
        
        // Buttons
        configureButton(add, "list-add.png", Language.getString("settings.listSelector.button.add.tip"));
        configureButton(remove, "list-remove.png", Language.getString("settings.listSelector.button.remove.tip"));
        configureButton(change, "edit.png", Language.getString("settings.listSelector.button.edit.tip"));
        configureButton(moveUp, "go-up.png", Language.getString("settings.listSelector.button.moveUp.tip"));
        configureButton(moveDown, "go-down.png", Language.getString("settings.listSelector.button.moveDown.tip"));
        configureButton(sort, "sort.png", Language.getString("settings.listSelector.button.sort.tip"));
        configureButton(editAll, "edit-all.png", Language.getString("settings.listSelector.button.editAll.tip"));
        
        // Listeners
        add.addActionListener(buttonAction);
        remove.addActionListener(buttonAction);
        input.addActionListener(buttonAction);
        change.addActionListener(buttonAction);
        moveUp.addActionListener(buttonAction);
        moveDown.addActionListener(buttonAction);
        sort.addActionListener(buttonAction);
        editAll.addActionListener(buttonAction);
        
        list.setModel(data);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        //add(input, gbc);
        
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        add(add, gbc);
        
        gbc.weightx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        add(remove, gbc);
        
        gbc.gridy = 2;
        add(change, gbc);
        
        if (manualSorting) {
            gbc.gridy = 3;
            add(moveUp, gbc);

            gbc.gridy = 4;
            add(moveDown, gbc);
        }
        if (alphabeticSorting) {
            gbc.gridy = 5;
            add(sort, gbc);
        }
        gbc.gridy = 6;
        add(editAll, gbc);
        
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 7;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        add(new JScrollPane(list), gbc);
        
        updateEditButtons();

        editor = new Editor(parent);
        allEditor = new Editor(parent);
        allEditor.setAllowLinebreaks(true);
        allEditor.setAllowEmpty(true);
    }
    
    public void setInfo(String info) {
        this.info = info;
    }
    
    public void setInfoLinkLabelListener(LinkLabelListener listener) {
        editor.setLinkLabelListener(listener);
        allEditor.setLinkLabelListener(listener);
    }
    
    public void setTester(Tester tester) {
        if (editor instanceof Editor) {
            ((Editor)editor).setTester(tester);
        }
    }
    
    public void setEditor(StringEditor editor) {
        this.editor = editor;
    }
    
    private void configureButton(JButton button, String icon, String tooltip) {
        button.setIcon(new ImageIcon(ListSelector.class.getResource(icon)));
        button.setToolTipText(tooltip);
        button.setPreferredSize(BUTTON_SIZE);
        button.setSize(BUTTON_SIZE);
        button.setMaximumSize(BUTTON_SIZE);
        button.setMinimumSize(BUTTON_SIZE);
    }
    
    /**
     * Add the item currently in the input box, but not if it's empty or already
     * in the list.
     */
    private void addItem() {
        String item = editor.showDialog(
                Language.getString("settings.listSelector.addEntry", title), "", info);
        item = format(item);
        if (item != null && !item.isEmpty() && !data.contains(item)) {
            int selectedIndex = list.getSelectedIndex();
            if (selectedIndex != -1) {
                data.add(selectedIndex + 1, item);
                list.setSelectedValue(item, true);
            } else {
                data.addElement(item);
            }
            input.setText("");
        }
        informListener();
    }
    
    /**
     * Remove selected items.
     */
    private void removeItem() {
        int selectedIndex = list.getSelectedIndex();
        for (String item : list.getSelectedValuesList()) {
            data.removeElement(item);
        }
        if (selectedIndex > -1) {
            if (selectedIndex < data.size()) {
                list.setSelectedValue(data.get(selectedIndex), true);
            } else if (selectedIndex > 0) {
                list.setSelectedValue(data.get(selectedIndex - 1), true);
            }
        }
        informListener();
    }
    
    private void changeItem() {
        String selectedValue = list.getSelectedValue();
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex > -1) {
            String newValue = editor.showDialog(
                    Language.getString("settings.listSelector.editEntry", title), selectedValue, info);
            newValue = format(newValue);
            if (newValue != null && !newValue.isEmpty()) {
                data.set(selectedIndex, newValue);
            }
        }
        informListener();
    }
    
    private void editAll() {
        StringBuilder b = new StringBuilder();
        for (int i=0;i<data.size();i++) {
            b.append(data.get(i)).append("\n");
        }
        String result = allEditor.showDialog(
                Language.getString("settings.listSelector.editAllEntries"), b.toString(), info);
        if (result != null) {
            String[] split = result.split("\n");
            data.clear();
            for (String item : split) {
                item = format(item);
                if (item != null && !item.isEmpty()) {
                    data.addElement(item);
                }
            }
        }
        informListener();
    }

    private void moveUp() {
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex > 0) {
            swap(selectedIndex, selectedIndex -1);
            list.setSelectedValue(data.get(selectedIndex - 1), true);
        }
        informListener();
    }
    
    private void moveDown() {
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex > -1 && selectedIndex < data.size() - 1) {
            swap(selectedIndex, selectedIndex + 1);
            list.setSelectedValue(data.get(selectedIndex + 1), true);
        }
        informListener();
    }
    
    private void swap(int index1, int index2) {
        String temp = data.get(index2);
        data.set(index2, data.get(index1));
        data.set(index1, temp);
        informListener();
    }
    
    private void sort() {
        if (JOptionPane.showConfirmDialog(sort,
                Language.getString("settings.listSelector.sortEntries"),
                Language.getString("settings.listSelector.sortEntries.title"),
                JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
            List<String> sortData = getData();
            Collections.sort(sortData);
            setData(sortData);
        }
    }

    /**
     * Checks if an item is selected in the list and enables or disables the
     * edit buttons accordingly.
     */
    private void updateEditButtons() {
        boolean somethingIsSelected = list.getSelectedIndex() != -1 && isEnabled();
        boolean exactlyOneIsSelected = list.getSelectedIndices().length == 1 && isEnabled();
        remove.setEnabled(somethingIsSelected);
        change.setEnabled(exactlyOneIsSelected);
        moveUp.setEnabled(exactlyOneIsSelected);
        moveDown.setEnabled(exactlyOneIsSelected);
        add.setEnabled(isEnabled());
        sort.setEnabled(isEnabled());
        editAll.setEnabled(isEnabled());
    }
    
    /**
     * Gets the current data.
     * 
     * @return 
     */
    public List<String> getData() {
        List<String> list = new ArrayList<>();
        Enumeration<String> e = data.elements();
        while (e.hasMoreElements()) {
            list.add(e.nextElement());
        }
        return list;
    }
    
    /**
     * Fills the list with the given data.
     * 
     * @param list 
     */
    public void setData(List<String> list) {
        data.clear();
        for (String item : list) {
            data.addElement(item);
        }
        informListener();
    }

    @Override
    public List<String> getSettingValue() {
        return getData();
    }

    @Override
    public void setSettingValue(List<String> value) {
        setData(value);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        input.setEnabled(enabled);
        list.setEnabled(enabled);
        updateEditButtons();
    }
    
    public void setDataFormatter(DataFormatter<String> formatter) {
        this.formatter = formatter;
    }
    
    /**
     * Formats the given text according to the set {@code DataFormatter}, or
     * just returns the input if no formatter is set.
     * 
     * @param input The text to format
     * @return The input after formatting, or the unchanged input if no
     * formatter is set (may also be {@code null}, depending on the
     * implementation of the formatter)
     */
    private String format(String input) {
        if (formatter != null && input != null) {
            return formatter.format(input);
        }
        return input;
    }
    
    public void setSelected(String item) {
        list.setSelectedValue(item, true);
    }
    
    /**
     * Select all of the given items, if they are in the list. Tries to make all
     * selected items visible, although the earlier ones in the given items are
     * preferred.
     * 
     * @param items The items to select, can be null to remove selection
     */
    public void setSelected(Collection<String> items) {
        list.clearSelection();
        if (items != null) {
            // Reverse list so earlier entries are scrolled to last
            List<String> itemsReversed = new ArrayList<>(items);
            Collections.reverse(itemsReversed);
            for (String item : itemsReversed) {
                int index = data.indexOf(item);
                if (index != -1) {
                    list.addSelectionInterval(index, index);
                    list.ensureIndexIsVisible(index);
                }
            }
        }
    }
    
    public void setChangeListener(Consumer<List<String>> listener) {
        this.changeListener = listener;
    }
    
    private void informListener() {
        if (changeListener != null) {
            changeListener.accept(getData());
        }
    }
    
}
