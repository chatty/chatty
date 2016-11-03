
package chatty.gui.components;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.StreamsContextMenu;
import chatty.util.BitEncoder;
import chatty.util.DateTime;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author tduva
 */
public class FavoritesDialog extends JDialog {
    
    private final JTable table;
    private final MyTableModel data;
    private final CustomSorter sorter;
    
    private final JTextField input = new JTextField(30);
    
    private final JButton addToFavoritesButton = new JButton("Add to favorites");
    private final JButton removeFromFavoritesButton = new JButton("Remove selected from favorites");
    private final JButton removeButton = new JButton("Remove selected");
    private final JButton doneButton = new JButton("Use chosen channels");
    private final JButton cancelButton = new JButton("Cancel");
    
    private String doneButtonText = "";
    private String doneButtonTextOneChannel = "";
 
    public static final int ACTION_CANCEL = 0;
    public static final int ACTION_DONE = 1;
    public static final int BUTTON_ADD_FAVORITES = 2;
    public static final int BUTTON_REMOVE_FAVORITES = 3;
    public static final int BUTTON_REMOVE = 4;
    
    private static final int COLUMN_FAV = 0;
    private static final int COLUMN_CHANNEL = 1;
    private static final int COLUMN_AGO = 2;
    
    private static final int FAV_COLUMN_WIDTH = 50;
    private static final int TIME_COLUMN_WIDTH = 100;
    
    private int result = -1;
    
    private final ContextMenuListener contextMenuListener;
    
    public FavoritesDialog(MainGui main, ContextMenuListener contextMenuListener) {
        super(main);
        setTitle("Favorites / History");
        setModal(true);
        
        this.contextMenuListener = contextMenuListener;
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        
        input.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                channelsChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                channelsChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                channelsChanged();
            }
        });
        
        // Table
        data = new MyTableModel();
        table = new JTable();
        table.setModel(data);
        TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(rowSorter);
        sorter = new CustomSorter(rowSorter);
        setupTable();
        
        
        gbc = makeGbc(0,0,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(input, gbc);
        
        gbc = makeGbc(2,0,1,1);
        addToFavoritesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        add(addToFavoritesButton, gbc);
        
        gbc = makeGbc(0,2,3,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        add(new JScrollPane(table), gbc);
        
        gbc = makeGbc(0,3,1,1);
        gbc.insets = new Insets(2, 5, 5, 5);
        removeFromFavoritesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        add(removeFromFavoritesButton, gbc);
        
        gbc = makeGbc(1,3,1,1);
        gbc.insets = new Insets(2, 5, 5, 5);
        removeButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        add(removeButton, gbc);

        doneButton.setMnemonic(KeyEvent.VK_J);
        gbc = makeGbc(0,4,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(doneButton, gbc);
        
        cancelButton.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(2,4,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(cancelButton, gbc);

        removeFromFavoritesButton.setToolTipText("Remove selected channel(s) "
                + "from favorites only");
        removeButton.setToolTipText("Remove selected channel(s) from favorites "
                + "and history");
        
        
        // Button Listeners
        ActionListener listener = new FavoritesActionListener();
        cancelButton.addActionListener(listener);
        doneButton.addActionListener(listener);
        
        ActionListener actionListener = main.getActionListener();
        addToFavoritesButton.addActionListener(actionListener);
        removeFromFavoritesButton.addActionListener(actionListener);
        removeButton.addActionListener(actionListener);
        input.addActionListener(actionListener);
        
        channelsChanged();
        
        pack();
        setMinimumSize(getSize());
    }
    
    private void setupTable() {
        table.setShowGrid(false);
        table.setDefaultRenderer(Boolean.class, new TestRenderer());
        table.setDefaultRenderer(Long.class, new TimeRenderer());
        
        table.getTableHeader().addMouseListener(sorter);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new MyDefaultTableHeaderCellRenderer());
        
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                selectionChanged();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    doneButton.doClick();
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                openContextMenu(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                openContextMenu(e);
            }
        
        });
        
        // Shortcuts
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0), "done");
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "done");
        table.getActionMap().put("done", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doneButton.doClick();
            }
        });
        
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        table.getActionMap().put("delete", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeButton.doClick();
            }
        });
        
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "addFav");
        table.getActionMap().put("addFav", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addToFavoritesButton.doClick();
            }
        });
        
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0), "unFav");
        table.getActionMap().put("unFav", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeFromFavoritesButton.doClick();
            }
        });
        
        // Column Sizes
        TableColumn favColumn = table.getColumnModel().getColumn(COLUMN_FAV);
        favColumn.setMaxWidth(FAV_COLUMN_WIDTH);
        favColumn.setMinWidth(FAV_COLUMN_WIDTH);
        TableColumn timeColumn = table.getColumnModel().getColumn(COLUMN_AGO);
        timeColumn.setMaxWidth(TIME_COLUMN_WIDTH);
        timeColumn.setMinWidth(TIME_COLUMN_WIDTH);
    }
    
    /**
     * Shows the dialog with the given channel as preset and the given action
     * as button description.
     * 
     * @param channelPreset
     * @param action The description of the done-button
     * @param actionOneChannel The secondary description, to be used for one channel
     * @return 
     */
    public int showDialog(String channelPreset, String action, String actionOneChannel) {
        doneButton.setText(action);
        doneButtonText = action;
        doneButtonTextOneChannel = actionOneChannel;
        setChannel(channelPreset);
        result = -1;
        updateEditButtons();
        setVisible(true);
        return result;
    }
    
    public int showDialog(String channelPreset, String action) {
        return showDialog(channelPreset, action, null);
    }
    

    /**
     * Gets the action associated with this Object (should be JButton).
     * 
     * @param source
     * @return 
     */
    public int getAction(Object source) {
        if (source == addToFavoritesButton || source == input) {
            return BUTTON_ADD_FAVORITES;
        }
        else if (source == removeFromFavoritesButton) {
            return BUTTON_REMOVE_FAVORITES;
        }
        else if (source == cancelButton) {
            return ACTION_CANCEL;
        }
        else if (source == doneButton) {
            return ACTION_DONE;
        }
        else if (source == removeButton) {
            return BUTTON_REMOVE;
        }
        return -1;
    }
    
    /**
     * Sets the chosen channel(s) to the input box.
     * 
     * @param channel 
     */
    public void setChannel(String channel) {
        if (channel != null) {
            input.setText(channel);
        }
    }
    
    /**
     * When the chosen channels changed. Update the done button.
     */
    private void channelsChanged() {
        boolean channelsPresent = !getChannels().isEmpty();
        doneButton.setEnabled(channelsPresent);
        int count = getChannels().size();
        if (count == 1 && doneButtonTextOneChannel != null) {
            doneButton.setText(doneButtonTextOneChannel);
        } else {
            doneButton.setText(doneButtonText);
        }
    }
    
    /**
     * Sets input to the currently selected channels.
     */
    private void selectionChanged() {
        updateChosenFromSelected();
        updateEditButtons();
    }
    
    private void updateChosenFromSelected() {
        String selectedChannels = Helper.buildStreamsString(getSelectedChannels());
        input.setText(selectedChannels);
    }
    
    private void updateEditButtons() {
        boolean selected = !getSelectedChannels().isEmpty();
        removeButton.setEnabled(selected);
        removeFromFavoritesButton.setEnabled(selected);
    }
    
    public Set<String> getSelectedChannels() {
        Set<String> selectedChannels = new HashSet<>();
        int[] selected = table.getSelectedRows();
        for (int i=0;i<selected.length;i++) {
            int row = selected[i];
            Object channel = table.getValueAt(row, 1);
            if (channel != null) {
                selectedChannels.add((String)channel);
            }
        }
        return selectedChannels;
    }
    
    /**
     * Gets the current chosen channels. This means the channels in the input
     * box, either entered manually or preset by the caller or by selecting
     * channels in the list.
     * 
     * @return 
     */
    public Set<String> getChannels() {
        String channels = input.getText();
        return Helper.parseChannelsFromString(channels, false);
    }
    
    public void setData(Set<String> favorites, Map<String, Long> history) {
        Map<String, Long> favoritesWithHistory = new HashMap<>();
        for (String channel : favorites) {
            favoritesWithHistory.put(channel, history.get(channel));
        }
        //favoritesWithHistory = MapUtil.sortByValue(favoritesWithHistory);
        //history = MapUtil.sortByValue(history);
        data.setData(favoritesWithHistory, history);
    }
    
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            int clickedRow = table.rowAtPoint(e.getPoint());
            if (clickedRow != -1) {
                if (!Helper.arrayContainsInt(table.getSelectedRows(), clickedRow)) {
                    table.setRowSelectionInterval(clickedRow, clickedRow);
                }
            }
            if (table.getSelectedRow() != -1) {
                Set<String> selected = getSelectedChannels();
                ContextMenu m = new StreamsContextMenu(selected, contextMenuListener);
                m.show(table, e.getX(), e.getY());
            }
        }
    }
    
    /**
     * The column id may not be associated with the same column forever, in case
     * the columns are changed.
     * 
     * @param column 
     */
    public void setSorting(int column) {
        sorter.setSorting(column);
    }
    
    /**
     * The returned value may not be associated with the same column forever, in
     * case the columns are changed.
     * 
     * @return 
     */
    public int getSorting() {
        return sorter.getSorting();
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(5,5,5,5);
        return gbc;
    }
    
    static class TestRenderer extends DefaultTableCellRenderer {
        
        ImageIcon icon = new ImageIcon(getClass().getResource("/chatty/gui/star.png"));
        
        @Override
        public void setValue(Object value) {
            if (value instanceof Boolean) {
                if ((Boolean)value) {
                    this.setIcon(icon);
                }
                else {
                    this.setIcon(null);
                }
                JLabel label = (JLabel)this;
                label.setHorizontalAlignment(JLabel.CENTER);
                //this.setText(value.toString());
            }
        }
        
    }
    
    static class TimeRenderer extends DefaultTableCellRenderer {
        
        @Override
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            long time = (Long)value;
            if (time == -1) {
                setText("-");
            }
            else {
                setText(DateTime.agoText(time));
            }
            JLabel label = (JLabel)this;
            label.setHorizontalAlignment(JLabel.CENTER);
        }
        
    }

    /**
     * Table Header renderer that always uses the second SortKey to determine
     * which column is sorted, because the first one is always the favorites
     * row, to keep the favorites at the top.
     */
    private static class MyDefaultTableHeaderCellRenderer extends ExtendableDefaultTableHeaderCellRenderer {

        @Override
        protected RowSorter.SortKey getSortKey(JTable table, int column) {
            RowSorter rowSorter = table.getRowSorter();
            if (rowSorter == null) {
                return null;
            }

            List sortedColumns = rowSorter.getSortKeys();
            if (sortedColumns.size() > 1) {
                return (RowSorter.SortKey) sortedColumns.get(1);
            } else if (sortedColumns.size() == 1) {
                return (RowSorter.SortKey) sortedColumns.get(0);
            }
            return null;
        }
    }
    
    class MyTableModel extends AbstractTableModel {

        Object[][] data = {
            {true,"joshimuz",new Integer(1000)},
            {true,"ninkortek",new Integer(1234)},
            {false,"abc",new Integer(1)}
        };
        
        /**
         * The columns. Changing this requires adjustments of column stuff in
         * some places (e.g. sorter).
         */
        private final String[] columns = {"Fav","Channel","Last joined"};
        
        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data[rowIndex][columnIndex];
        }
        
        @Override
        public Class getColumnClass(int c) {
            if (data.length == 0) {
                return Object.class;
            }
            return getValueAt(0, c).getClass();
        }
        
        @Override
        public String getColumnName(int col) {
            return columns[col];
        }
        
        
        public void setData(Map<String, Long> favorites, Map<String, Long> history) {
            int count = 0;
            for (String entry : favorites.keySet()) {
                if (!history.containsKey(entry)) {
                    count++;
                }
            }
            count += history.size();
            
            data = new Object[count][columns.length];
            int i = 0;
            
            for (String entry : favorites.keySet()) {
                
                addEntry(i, true, entry, history.get(entry));
                i++;
            }
            
            for (String channel : history.keySet()) {
                Long time = history.get(channel);
                if (!favorites.containsKey(channel)) {
                    
                    addEntry(i, false, channel, time);
                    i++;
                }
            }
            fireTableDataChanged();
        }
        
        private void addEntry(int i, boolean favorite, String channel, Long time) {
            if (time == null) {
                time = -1l;
            }
            //System.out.println(i+" "+channel);
            data[i] = new Object[3];
            data[i][0] = favorite;
            data[i][1] = channel;
            data[i][2] = time;
        }
    }
    
    private class FavoritesActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == doneButton) {
                result = ACTION_DONE;
                setVisible(false);
            }
            else if (e.getSource() == cancelButton) {
                result = ACTION_CANCEL;
                setVisible(false);
            }
        }
        
    }
    
    /**
     * Custom sorter, that sorts the column that is clicked on, using the
     * TableRowSorter added to the table.
     */
    private final class CustomSorter extends MouseAdapter {
        
        private final BitEncoder encoder = new BitEncoder(new int[]{COLUMN_FAV,
            COLUMN_CHANNEL, COLUMN_AGO}, new int[]{0,1});
        
        /**
         * How often the current column was sorted
         */
        private int sortCount;
        
        /**
         * Which column is currently sorted
         */
        private int sortedColumn;
        
        private final TableRowSorter<TableModel> sorter;
        
        CustomSorter(TableRowSorter<TableModel> sorter) {
            this.sorter = sorter;
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            int columnIndex = table.getColumnModel().getColumnIndexAtX(e.getX());
            sortColumn(columnIndex);
        }
        
        /**
         * Sorts the given column, either starting with the default sort order
         * if this column wasn't sorted in the last request, or switching
         * between sort orders.
         * 
         * @param columnIndex 
         */
        public void sortColumn(int columnIndex) {
            
            sortCount++;
            
            // Start counting everytime another column is sorted
            if (sortedColumn != columnIndex) {
                sortCount = 0;
                // Use another starting order for the 3rd column (time ago)
                if (columnIndex == COLUMN_AGO) {
                    sortCount = 1;
                }
            }
            
            sort(columnIndex, sortCount);
        }
 
        /**
         * Sorts the given column with the given sortCount, which indicates
         * the sort order.
         * 
         * @param columnIndex
         * @param sortCount 
         */
        private void sort(int columnIndex, int sortCount) {
            SortOrder order = SortOrder.values()[sortCount % 2];

            List<RowSorter.SortKey> sortKeys = new ArrayList<>();
            
            // Always have column 0 (favorites) sorted first, so the favorites
            // stay at the top
            if (columnIndex != 0) {
                sortKeys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
            }
            sortKeys.add(new RowSorter.SortKey(columnIndex, order));
            sorter.setSortKeys(sortKeys);
            
            this.sortCount = sortCount;
            sortedColumn = columnIndex;
        }
        
        public int getSorting() {
            encoder.setValue(0, sortedColumn);
            encoder.setValue(1, sortCount % 2);
            return (int)encoder.encode();
        }
        
        public void setSorting(int sorting) {
            encoder.decode(sorting);
            sort(encoder.getValue(0), encoder.getValue(1));
        }
        
    }
    
}

