
package chatty.gui.components.admin;

import chatty.gui.GuiUtil;
import static chatty.gui.components.admin.AdminDialog.SMALL_BUTTON_INSETS;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.settings.Editor;
import chatty.gui.components.settings.ListTableModel;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.api.BlockedTermsManager;
import chatty.util.api.BlockedTermsManager.BlockedTerm;
import chatty.util.api.BlockedTermsManager.BlockedTerms;
import chatty.util.api.TwitchApi;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author tduva
 */
public class BlockedTermsPanel extends JPanel {
    
    private static final Logger LOGGER = Logger.getLogger(BlockedTermsPanel.class.getName());
    
    private static final int BULK_EDIT_DELAY = 500;

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
    private final Map<String, Boolean> edited = new HashMap<>();
    
    private final TableRowSorter sorter;
    
    public BlockedTermsPanel(AdminDialog dialog, TwitchApi api) {
        this.api = api;
        
        setLayout(new GridBagLayout());
        
        //--------------------------
        // Table
        //--------------------------
        data = new Model(new String[]{
            "Text",
            "Created",
            "Expires"
        });
        table = new JTable();
        table.setModel(data);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(1).setCellRenderer(new CreatedRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new ExpiresRenderer());
        sorter = new TableRowSorter(data);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING)); // Created At
        sorter.setSortKeys(sortKeys);
        table.setRowSorter(sorter);
        table.setTransferHandler(new MyTransferHandler());
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(1).setMaxWidth(GuiUtil.getTableColumnHeaderWidth(table, 1)*2);
        table.getColumnModel().getColumn(2).setMaxWidth(GuiUtil.getTableColumnHeaderWidth(table, 2)*2);
        
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
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editEntry();
                }
            }
            
        });
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeItems");
        table.getActionMap().put("removeItems", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteEntries();
            }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editItem");
        table.getActionMap().put("editItem", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                editEntry();
            }
        });
        
        //--------------------------
        // Other Elements
        //--------------------------
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
                    else if (term.streamLogin.equals(currentStream)) {
                        data.add(term);
                        if (term.text.equals(input.getText())) {
                            // Only clear if successfully added and still same
                            input.setText(null);
                        }
                        setEdited(true);
                    }
                });
            });
        }
    }
    
    private void editEntry() {
        int selected = table.getSelectedRow();
        if (selected != -1) {
            BlockedTerm selectedTerm = data.get(table.convertRowIndexToModel(selected));
            Editor editor = new Editor(SwingUtilities.getWindowAncestor(this));
            String result = editor.showDialog("Edit (Removes the term and adds the edited one)", selectedTerm.text, null);
            if (result != null && !result.equals(selectedTerm.text)) {
                api.removeBlockedTerm(selectedTerm, removed -> {
                    SwingUtilities.invokeLater(() -> {
                        if (removed == null) {
                            statusLabel.setText("An error occured removing term");
                        }
                        else if (removed.streamLogin.equals(currentStream)) {
                            data.remove(removed);
                            addEntry(result);
                            setEdited(true);
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
            Thread thread = new Thread("deleteTerms") {
                
                @Override
                public void run() {
                    for (BlockedTerm term : toDelete) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(BULK_EDIT_DELAY);
                            SwingUtilities.invokeLater(() -> {
                                api.removeBlockedTerm(term, removed -> {
                                    SwingUtilities.invokeLater(() -> {
                                        if (removed == null) {
                                            statusLabel.setText("An error occured removing item");
                                        }
                                        else if (removed.streamLogin.equals(currentStream)) {
                                            data.remove(removed);
                                            setEdited(true);
                                        }
                                    });
                                });
                            });
                        }
                        catch (InterruptedException ex) {
                            Logger.getLogger(BlockedTermsPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                
            };
            thread.start();
        }
    }
    
    private void importData(String preset) {
        Editor editor = new Editor(SwingUtilities.getWindowAncestor(this));
        editor.setAllowLinebreaks(true);
        String result = editor.showDialog("Import entries (one entry per line)", preset, "Can also copy&paste in table to trigger export/import.");
        if (result != null) {
            String[] split = result.split("\n");
            Thread thread = new Thread("importTerms") {

                @Override
                public void run() {
                    for (String item : split) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(BULK_EDIT_DELAY);
                        }
                        catch (InterruptedException ex) {
                            Logger.getLogger(BlockedTermsPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        SwingUtilities.invokeLater(() -> {
                            addEntry(item);
                        });
                    }
                }

            };
            thread.start();
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
            if (isEdited()) {
                refreshData();
            }
            else {
                loadData();
            }
        }
    }
    
    private void setEdited(boolean newValue) {
        edited.put(currentStream, newValue);
    }
    
    private boolean isEdited() {
        return edited.containsKey(currentStream) && edited.get(currentStream);
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
        api.getBlockedTerms(currentStream, true, t -> {
            setEdited(false);
            setData(t);
        });
    }
    
    public void update() {
        if (currentTerms != null) {
            if (currentTerms.hasError()) {
                statusLabel.setText(currentTerms.error);
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
        table.repaint();
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
                        case "import":
                            importData("");
                            break;
                    }
                }
            };
            menu.addItem("edit", "Edit");
            menu.addItem("delete", "Delete");
            menu.addSeparator();
            menu.addItem("import", "Import");
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
    
    private static class CreatedRenderer extends DefaultTableCellRenderer {
        
        @Override
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Long lastActivity = (Long)value;
            setText(DateTime.agoSingleCompact(lastActivity)+" ago");
            JLabel label = (JLabel)this;
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setToolTipText("Created or updated at "+DateTime.formatFullDatetime(lastActivity));
        }
        
    }
    
    private static class ExpiresRenderer extends DefaultTableCellRenderer {
        
        @Override
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Long lastActivity = (Long)value;
            if (lastActivity.longValue() <= 0) {
                setText("");
            }
            else {
                setText(DateTime.duration(lastActivity - System.currentTimeMillis(), 0, 1, 0));
                JLabel label = (JLabel) this;
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setToolTipText("Expires at "+DateTime.formatFullDatetime(lastActivity));
            }
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
                    if (entry.expiresAt != -1) {
                        return entry.updatedAt;
                    }
                    return entry.createdAt;
                case 2:
                    return entry.expiresAt;
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
                case 2:
                    return Long.class;
            }
            return String.class;
        }

    }
    
    /**
     * Only use first row value (terms text) when copying from table and show
     * import dialog when pasting something.
     */
    private class MyTransferHandler extends TransferHandler {

        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof JTable) {
                JTable table = (JTable) c;
                StringBuilder b = new StringBuilder();
                int[] selected = table.getSelectedRows();
                for (int row=0;row<selected.length;row++) {
                    Object obj = table.getValueAt(selected[row], 0);
                    b.append(String.valueOf(obj)).append("\n");
                }
                b.deleteCharAt(b.length() - 1);
                return new StringSelection(b.toString());
            }
            return null;
        }
        
        @Override
        public boolean importData(TransferSupport support) {
            try {
                String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                BlockedTermsPanel.this.importData(data);
                return true;
            }
            catch (UnsupportedFlavorException | IOException ex) {
                LOGGER.warning("Error importing table data: "+ex);
            }
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

    }
    
}
