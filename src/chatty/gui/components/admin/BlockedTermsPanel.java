
package chatty.gui.components.admin;

import chatty.gui.GuiUtil;
import static chatty.gui.components.admin.AdminDialog.SMALL_BUTTON_INSETS;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.settings.Editor;
import chatty.gui.components.settings.ListTableModel;
import chatty.gui.components.settings.TableEditor;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.api.BlockedTermsManager;
import chatty.util.api.BlockedTermsManager.BlockedTerm;
import chatty.util.api.BlockedTermsManager.BlockedTerms;
import chatty.util.api.TwitchApi;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author tduva
 */
public class BlockedTermsPanel extends JPanel {

    private final JLabel statusLabel;
    private final JButton refreshButton;
    private final JTable table;
    private final Model data;
    private final JTextField input;
    private final JButton addButton;
    private final JCheckBox filterEnabled;
    
    private final TwitchApi api;
    
    private String currentStream;
    private BlockedTerms currentTerms;
    private boolean loading;
    private final Editor editor;
    
    private final TableRowSorter sorter;
    
    public BlockedTermsPanel(AdminDialog dialog, TwitchApi api) {
        this.api = api;
        
        setLayout(new GridBagLayout());
        
        //--------------------------
        // Table
        //--------------------------
        data = new Model(new String[]{
            "Text",
            "Created"
        });
        table = new JTable();
        table.setModel(data);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(1).setCellRenderer(new LastActivityRenderer());
        sorter = new TableRowSorter(data);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING)); // Created At
        sorter.setSortKeys(sortKeys);
        table.setRowSorter(sorter);
        
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isControlDown() && !e.isShiftDown() && table.getSelectedRowCount() < 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        setRowSelected(row);
                    }
                    else if (table.getRowCount() > 0) {
                        setRowSelected(table.getRowCount() - 1);
                    }
                }
                popupMenu(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                popupMenu(e);
            }
        });
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        //--------------------------
        // Other Elements
        //--------------------------
        editor = new Editor(dialog);
        
        statusLabel = new JLabel();
        refreshButton = new JButton(Language.getString("admin.button.reload"));
        refreshButton.setMargin(SMALL_BUTTON_INSETS);
        refreshButton.setIcon(new ImageIcon(AdminDialog.class.getResource("view-refresh.png")));
        refreshButton.setMnemonic(KeyEvent.VK_R);
        refreshButton.addActionListener(e -> refreshData());
        
        filterEnabled = new JCheckBox("Filter");
        filterEnabled.addItemListener((e) -> {
            updateFilter();
        });
        
        input = new JTextField();
        input.addActionListener(e -> {
            addInputEntry();
        });
        GuiUtil.addChangeListener(input.getDocument(), e -> {
            updateFilter();
        });
        
        addButton = new JButton("Add");
        addButton.setMargin(SMALL_BUTTON_INSETS);
        addButton.addActionListener(e -> {
            addInputEntry();
        });
        
        //--------------------------
        // Layout
        //--------------------------
        GridBagConstraints gbc;
        
        gbc = GuiUtil.makeGbc(0, 0, 1, 1);
        add(refreshButton, gbc);
        
        gbc = GuiUtil.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        add(statusLabel, gbc);
        
        gbc = GuiUtil.makeGbc(2, 0, 1, 1, GridBagConstraints.WEST);
        add(filterEnabled, gbc);
        
        gbc = GuiUtil.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(input, gbc);
        
        gbc = GuiUtil.makeGbc(2, 1, 1, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addButton, gbc);
        
        gbc = GuiUtil.makeGbc(0, 2, 3, 1);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(new JScrollPane(table), gbc);
    }
    
    private void updateFilter() {
        addButton.setEnabled(!input.getText().isEmpty());
        if (filterEnabled.isSelected()) {
            filter(input.getText());
        }
        else {
            resetFilter();
        }
    }
    
    private void addInputEntry() {
        addEntry(input.getText());
    }
    
    private void addEntry(String text) {
        if (!text.isEmpty()) {
            api.addBlockedTerm(currentStream, text, term -> {
                SwingUtilities.invokeLater(() -> {
                    if (term == null) {
                        statusLabel.setText("An error occured adding term");
                    }
                    else {
                        data.add(term);
                        if (term.text.equals(input.getText())) {
                            // Only clear if successfully added and still same
                            input.setText(null);
                        }
                    }
                });
            });
        }
    }
    
    private void editEntry() {
        int selected = table.getSelectedRow();
        if (selected != -1) {
            BlockedTerm selectedTerm = data.get(table.convertRowIndexToModel(selected));
            String result = editor.showDialog("Edit (Removes re-adds the term)", selectedTerm.text, "");
            if (result != null && !result.equals(selectedTerm.text)) {
                api.removeBlockedTerm(selectedTerm, removed -> {
                    SwingUtilities.invokeLater(() -> {
                        if (removed == null) {
                            statusLabel.setText("An error occured removing term");
                        }
                        else {
                            data.remove(removed);
                            addEntry(result);
                        }
                    });
                });
            }
        }
    }
    
    private void deleteEntries() {
        List<BlockedTerm> toDelete = new ArrayList<>();
        int[] selected = table.getSelectedRows();
        for (int i=0;i<selected.length;i++) {
            BlockedTerm term = data.get(table.convertRowIndexToModel(selected[i]));
            toDelete.add(term);
        }
        if (toDelete.size() == 1 || JOptionPane.showConfirmDialog(table, "Delete "+toDelete.size()+" items?", "Delete items", JOptionPane.OK_CANCEL_OPTION) == 0) {
            for (BlockedTerm term : toDelete) {
                api.removeBlockedTerm(term, removed -> {
                    SwingUtilities.invokeLater(() -> {
                        if (removed == null) {
                            statusLabel.setText("An error occured removing item");
                        }
                        else {
                            data.remove(removed);
                        }
                    });
                });
            }
        }
    }
    
    public void resetFilter() {
        sorter.setRowFilter(null);
    }
    
    public void filter(String text) {
        if (text.isEmpty()) {
            resetFilter();
        } else {
            sorter.setRowFilter(new RowFilter<Model, Integer>() {

                @Override
                public boolean include(RowFilter.Entry<? extends Model, ? extends Integer> entry) {
                    Model model = entry.getModel();
                    BlockedTerm term = model.get(entry.getIdentifier());
                    return term.text.contains(text);
                }
            });
        }
    }
    
    public void changeStream(String stream) {
        if (stream != null && !stream.equals(currentStream)) {
            currentStream = stream;
            loadData();
        }
    }
    
    private void setData(BlockedTerms terms) {
        SwingUtilities.invokeLater(() -> {
            loading = false;
            if (terms.streamName != null && terms.streamName.equals(currentStream)) {
                currentTerms = terms;
                data.setData(terms.data);
                update();
            }
        });
    }
    
    private void loadData() {
        loading = true;
        update();
        api.getBlockedTerms(currentStream, false, t -> setData(t));
    }
    
    private void refreshData() {
        loading = true;
        update();
        api.getBlockedTerms(currentStream, true, t -> setData(t));
    }
    
    public void update() {
        if (currentTerms != null) {
            if (currentTerms.hasError()) {
                statusLabel.setText("An error occured getting terms");
            }
            else {
                statusLabel.setText(Language.getString("admin.termsLoaded",
                        data.getRowCount(),
                        DateTime.duration(System.currentTimeMillis() - currentTerms.createdAt, 1, 0)));
                if (currentTerms.data.size() == BlockedTermsManager.MAX_RESULTS) {
                    statusLabel.setText(statusLabel.getText()+" (request limit reached)");
                }
            }
        }
        refreshButton.setEnabled(!loading);
    }
    
    private void popupMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            ContextMenu menu = new ContextMenu() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switch (e.getActionCommand()) {
                        case "edit":
                            editEntry();
                            break;
                        case "delete":
                            deleteEntries();
                            break;
                    }
                }
            };
            menu.addItem("edit", "Edit");
            menu.addItem("delete", "Delete");
            menu.show(table, e.getX(), e.getY());
        }
    }
    
    private void setRowSelected(int viewIndex) {
        table.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
        scrollToRow(viewIndex);
    }
    
    private void scrollToRow(int index) {
        if (index != -1) {
            scrollRectToVisible(table.getCellRect(index, 0, true));
        }
    }
    
    private static class LastActivityRenderer extends DefaultTableCellRenderer {
        
        @Override
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Long lastActivity = (Long)value;
            setText(DateTime.agoText(lastActivity));
            JLabel label = (JLabel)this;
            label.setHorizontalAlignment(JLabel.CENTER);
        }
        
    }
    
    private static class Model extends ListTableModel<BlockedTerm> {

        Model(String[] columns) {
            super(columns);
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BlockedTerm entry = get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return entry.text;
                case 1:
                    return entry.createdAt;
            }
            return null;
        }
        
        /**
         * Return classes so the table does the correct sorting etc.
         * 
         * @param columnIndex
         * @return 
         */
        @Override
        public Class getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 1:
                    return Long.class;
            }
            return String.class;
        }

    }
    
}
