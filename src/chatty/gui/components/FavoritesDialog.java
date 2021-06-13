
package chatty.gui.components;

import chatty.ChannelFavorites;
import chatty.ChannelFavorites.Favorite;
import chatty.Helper;
import chatty.Room;
import chatty.gui.GuiUtil;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.lang.Language;
import chatty.gui.components.menus.RoomsContextMenu;
import chatty.gui.components.menus.TextSelectionMenu;
import chatty.util.BitEncoder;
import chatty.util.DateTime;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
    private final ChannelFavorites favorites;
    
    private final JTextField input = new JTextField(30);
    
    private final JButton addToFavoritesButton = new JButton(Language.getString("favorites.button.addToFavorites"));
    private final JButton removeFromFavoritesButton = new JButton(Language.getString("favorites.button.removeFromFavorites"));
    private final JButton removeButton = new JButton(Language.getString("favorites.button.remove"));
    private final JButton doneButton = new JButton();
    private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
 
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
    
    public FavoritesDialog(Window main, ChannelFavorites favorites,
            ContextMenuListener contextMenuListener) {
        super(main);
        setTitle(Language.getString("favorites.title"));
        setModal(true);
        
        this.favorites = favorites;
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
        GuiUtil.installLengthLimitDocumentFilter(input, 8000, false);
        TextSelectionMenu.install(input);
        
        // Table
        data = new MyTableModel();
        table = new JTable();
        table.setModel(data);
        TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(rowSorter);
        sorter = new CustomSorter(rowSorter);
        setupTable();

        input.setToolTipText(Language.getString("favorites.input.tip"));
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

        removeButton.setToolTipText(Language.getString("favorites.button.remove.tip"));
        
        ActionListener actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == addToFavoritesButton) {
                    addToFavorites();
                } else if (e.getSource() == removeFromFavoritesButton) {
                    removeFromFavorites();
                } else if (e.getSource() == removeButton) {
                    remove();
                } else if (e.getSource() == input) {
                    addToFavorites();
                } else if (e.getSource() == doneButton) {
                    result = ACTION_DONE;
                    setVisible(false);
                } else if (e.getSource() == cancelButton) {
                    result = ACTION_CANCEL;
                    setVisible(false);
                }
            }
        };

        addToFavoritesButton.addActionListener(actionListener);
        removeFromFavoritesButton.addActionListener(actionListener);
        removeButton.addActionListener(actionListener);
        input.addActionListener(actionListener);
        cancelButton.addActionListener(actionListener);
        doneButton.addActionListener(actionListener);
        
        channelsChanged();
        
        pack();
        setMinimumSize(getSize());
    }
    
    /**
     * Add channels currently in the inputbox to the favorites. This allows both
     * channels selected in the list to be added as well as manually entered
     * ones.
     */
    private void addToFavorites() {
        for (String channel : getChannels()) {
            removeEntry(channel);
            Favorite r = favorites.addFavorite(channel);
            data.add(r);
        }
    }
    
    /**
     * Remove favorites selected in the list.
     */
    private void removeFromFavorites() {
        for (Favorite f : getSelected()) {
            data.remove(f);
            Favorite r = favorites.removeFavorite(f.room);
            data.add(r);
        }
    }
    
    /**
     * Remove entries selected in the list.
     */
    private void remove() {
        int index = table.getSelectedRow();
        for (Favorite f : getSelected()) {
            data.remove(f);
            favorites.remove(f);
        }
        if (table.getRowCount() > index) {
            table.setRowSelectionInterval(index, index);
        } else if (table.getRowCount() > 0) {
            index = table.getRowCount() - 1;
            table.setRowSelectionInterval(index, index);
        }
    }
    
    /**
     * Remove the entry for the given channel, if it exists.
     * 
     * @param channel The channel
     */
    private void removeEntry(String channel) {
        Favorite existing = favorites.get(channel);
        if (existing != null) {
            data.remove(existing);
        }
    }
    
    private void setupTable() {
        table.setShowGrid(false);
        table.setDefaultRenderer(Boolean.class, new FavoriteRenderer());
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
                remove();
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
     * @return 
     */
    public int showDialog(String channelPreset) {
        setChannel(channelPreset);
        result = -1;
        updateEditButtons();
        data.setData(favorites.getAll());
        setVisible(true);
        return result;
    }
    
    public void updateData() {
        if (isVisible()) {
            data.setData(favorites.getAll());
        }
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
        Set<String> currentChannels = getChannels();
        doneButton.setEnabled(!currentChannels.isEmpty());
        doneButton.setText(Language.getString("favorites.button.joinChannels",
                currentChannels.size()));
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
        for (int row : selected) {
            Favorite favorite = data.get(table.convertRowIndexToModel(row));
            selectedChannels.add(favorite.room.getChannel());
        }
        return selectedChannels;
    }
    
    public List<Favorite> getSelected() {
        List<Favorite> result = new ArrayList<>();
        for (int row : table.getSelectedRows()) {
            result.add(data.get(table.convertRowIndexToModel(row)));
        }
        return result;
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
        return Helper.parseChannelsFromString(channels, true);
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
                Collection<Room> selected = new ArrayList<>();
                for (Favorite f : getSelected()) {
                    selected.add(f.room);
                }
                ContextMenu m = new RoomsContextMenu(selected, contextMenuListener);
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
    
    /**
     * Renderer to draw the star icon for favorited entries.
     */
    private static class FavoriteRenderer extends DefaultTableCellRenderer {
        
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
    
    /**
     * Renderer to format and output the last join time.
     */
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
     * Table Header renderer that always uses the second SortKey to show which
     * column is sorted, because the first one is always the favorites row, to
     * keep the favorites at the top.
     */
    private static class MyDefaultTableHeaderCellRenderer extends ExtendableDefaultTableHeaderCellRenderer {

        @Override
        protected RowSorter.SortKey getSortKey(JTable table, int column) {
            RowSorter rowSorter = table.getRowSorter();
            if (rowSorter == null) {
                return null;
            }

            List<RowSorter.SortKey> sortedColumns = rowSorter.getSortKeys();
            for (RowSorter.SortKey key : sortedColumns) {
                if (key.getColumn() == column) {
                    return key;
                }
            }
//            if (sortedColumns.size() > 1) {
//                return (RowSorter.SortKey) sortedColumns.get(1);
//            } else if (sortedColumns.size() == 1) {
//                return (RowSorter.SortKey) sortedColumns.get(0);
//            }
            return null;
        }
    }
    
    private static class MyTableModel extends AbstractTableModel {

        private List<Favorite> data = new ArrayList<>();
        
        /**
         * The columns. Changing this requires adjustments of column stuff in
         * some places (e.g. sorter).
         */
        private final String[] columns = {
            Language.getString("favorites.column.fav"),
            Language.getString("favorites.column.channel"),
            Language.getString("favorites.column.lastJoined")
        };

        public void setData(List<Favorite> newData) {
            this.data = newData;
            fireTableDataChanged();
        }
        
        public Favorite get(int index) {
            return data.get(index);
        }
        
        public void add(Favorite f) {
            data.add(f);
            int index = data.size() - 1;
            fireTableRowsInserted(index, index);
        }
        
        public void remove(Favorite f) {
            int index = data.indexOf(f);
            if (index != -1) {
                data.remove(index);
                fireTableRowsDeleted(index, index);
            }
        }
        
        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }
        
        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Favorite f = data.get(rowIndex);
            if (columnIndex == 0) {
                return f.isFavorite;
            } else if (columnIndex == 1) {
                return Helper.toStream(f.room.getDisplayName());
            } else {
                return f.lastJoined;
            }
        }
        
        @Override
        public Class getColumnClass(int c) {
            if (data.isEmpty()) {
                return Object.class;
            }
            return getValueAt(0, c).getClass();
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

