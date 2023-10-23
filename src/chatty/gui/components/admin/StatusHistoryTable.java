
package chatty.gui.components.admin;

import chatty.gui.components.settings.ListTableModel;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.StreamCategory;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author tduva
 */
public class StatusHistoryTable extends JTable {
    
    private final Model data = new Model(new String[]{
        Language.getString("admin.presets.column.fav"),
        Language.getString("admin.presets.column.title"),
        Language.getString("admin.presets.column.game"),
        Language.getString("admin.presets.column.tags"),
        Language.getString("admin.presets.column.labels"),
        Language.getString("admin.presets.column.lastActivity"),
        Language.getString("admin.presets.column.usage")
    });
    
    private static final int FAV_COLUMN = 0;
    private static final int TITLE_COLUMN = 1;
    private static final int GAME_COLUMN = 2;
    private static final int TAGS_COLUMN = 3;
    private static final int LABELS_COLUMN = 4;
    private static final int ACTIVITY_COLUMN = 5;
    private static final int USAGE_COLUMN = 6;
    
    private final TableRowSorter sorter;
    private final JPopupMenu contextMenu;
    
    public StatusHistoryTable(JPopupMenu contextMenu) {
        setModel(data);
        // Disable because some stuff depends on the column id (although it
        // is probably possible to do it differently)
        getTableHeader().setReorderingAllowed(false);
        setFillsViewportHeight(true);
        sorter = new TableRowSorter(data);
        this.contextMenu = contextMenu;
        setRowSorter(sorter);
        TableColumn tc = getColumnModel().getColumn(TITLE_COLUMN); // Title
        tc.setCellRenderer(new LineWrapCellRenderer(true));
        TableColumn tc2 = getColumnModel().getColumn(TAGS_COLUMN); // Tags
        tc2.setCellRenderer(new LineWrapCellRenderer(true));
        TableColumn tc3 = getColumnModel().getColumn(LABELS_COLUMN); // Labels
        tc3.setCellRenderer(new LineWrapCellRenderer(true));
        setGridColor(new Color(200,200,200));
        
        getColumnModel().getColumn(ACTIVITY_COLUMN).setCellRenderer(new LastActivityRenderer());
        getColumnModel().getColumn(FAV_COLUMN).setCellRenderer(new FavoriteRenderer());
        
        setColumnWidth(FAV_COLUMN, 30, 30, 30);  // Fav
        setColumnWidth(TITLE_COLUMN, 200, 0, 0);   // Title
        setColumnWidth(GAME_COLUMN, 120, 0, 0);   // Game
        setColumnWidth(TAGS_COLUMN, 140, 0, 0);   // Tags
        setColumnWidth(LABELS_COLUMN, 140, 0, 0);   // Labels
        setColumnWidth(ACTIVITY_COLUMN, 100, 100, 100); // Last Activity
        setColumnWidth(USAGE_COLUMN, 50, 50, 50);  // Usage
        
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(ACTIVITY_COLUMN, SortOrder.DESCENDING)); // Last Activity
        sorter.setSortKeys(sortKeys);
        
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                if (row != -1) {
                    setRowSelected(row);
                } else if (getRowCount() > 0) {
                    setRowSelected(getRowCount() - 1);
                }
                popupMenu(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                popupMenu(e);
            }
        });
        
    }
    
    private void popupMenu(MouseEvent e) {
        if (e.isPopupTrigger() && contextMenu != null) {
            contextMenu.show(this, e.getX(), e.getY());
        }
    }
    
    private void setColumnWidth(int column, int width, int minwidth, int maxwidth) {
        TableColumn tc = getColumnModel().getColumn(column);
        tc.setPreferredWidth(width);
        if (maxwidth > 0) {
            tc.setMaxWidth(maxwidth);
        }
        if (minwidth > 0) {
            tc.setMinWidth(minwidth);
        }
    }
    
    public void resetFilter() {
        sorter.setRowFilter(null);
    }
    
    public void filter(final StreamCategory game, final boolean favorites) {
        if (game == null && !favorites) {
            resetFilter();
        } else {
            sorter.setRowFilter(new RowFilter<Model, Integer>() {

                @Override
                public boolean include(RowFilter.Entry<? extends Model, ? extends Integer> entry) {
                    Model model = entry.getModel();
                    StatusHistoryEntry e = model.get(entry.getIdentifier());
                    if (game != null && !game.equals(e.game)) {
                        return false;
                    }
                    if (favorites && !e.favorite) {
                        return false;
                    }
                    return true;
                }
            });
        }
    }
    
    public void setData(List<StatusHistoryEntry> newData) {
        data.setData(newData);
    }
    
    /**
     * Returns the selected {@code StatusHistoryEntry}.
     * 
     * @return The selected {@code StatusHistoryEntry} or {@code null} if no
     * entry is selected
     */
    public StatusHistoryEntry getSelectedEntry() {
        int modelIndex = indexToModel(getSelectedRow());
        if (modelIndex == -1) {
            return null;
        }
        return data.get(modelIndex);
    }
    
    /**
     * Removes the given entry, if present in the table, and selects the next
     * appropriate entry.
     * 
     * @param entry 
     */
    public void removeEntry(StatusHistoryEntry entry) {
        int indexToRemove = data.indexOf(entry);
        if (indexToRemove != -1) {
            int viewIndex = indexToView(indexToRemove);
            data.remove(indexToRemove);
            
            // Select appropriate row after removing
            if (getRowCount() > viewIndex) {
                setRowSelected(viewIndex);
            } else if (viewIndex - 1 >= 0 && getRowCount() > viewIndex - 1) {
                setRowSelected(viewIndex - 1);
            }
        }
    }
    
    /**
     * Sets the given row as selected and scrolls to it if necessary.
     *
     * @param viewIndex
     */
    private void setRowSelected(int viewIndex) {
        getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
        scrollToRow(viewIndex);
    }
    
    /**
     * Scrolls to the given row.
     * 
     * @param index The view row index to scroll to
     */
    private void scrollToRow(int index) {
        if (index != -1) {
            scrollRectToVisible(getCellRect(index, 0, true));
        }
    }
    
    /**
     * Updates the given entry if already present in the table.
     * 
     * @param entry 
     */
    public void updateEntry(StatusHistoryEntry entry) {
        data.update(entry);
    }
    
    /**
     * Convert a view index to model index.
     *
     * @param index The index to convert
     * @return The converted index, or {@code -1} if {@code index} was
     * {@code -1}
     */
    private int indexToModel(int index) {
        if (index == -1) {
            return -1;
        }
        return convertRowIndexToModel(index);
    }
    
    /**
     * Convert a model index to view index.
     * 
     * @param index The index to convert
     * @return The corresponding index of the viliteralor {@code -1} if the row
     * isn't visible
     */
    private int indexToView(int index) {
        return convertRowIndexToView(index);
    }
    
    private static class LineWrapCellRenderer extends JTextArea implements TableCellRenderer {
        
        LineWrapCellRenderer(boolean wordWrap) {
            setWrapStyleWord(wordWrap);
            setLineWrap(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(final JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            setText((String)value);
            int cWidth = table.getTableHeader().getColumnModel().getColumn(column).getWidth();
            setSize(new Dimension(cWidth, 1000));       
            final int height = getPreferredSize().height;
            /**
             * Setting the row height triggers a repaint, so make sure to only
             * do it when necessary. This still seems a bit dodgy though.
             */
            if (table.getRowHeight(row) < height) {
                table.setRowHeight(row, height);
            }
            
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            
            return this;
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
            }
        }
    }
    
    private static class Model extends ListTableModel<StatusHistoryEntry> {

        Model(String[] columns) {
            super(columns);
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            StatusHistoryEntry entry = get(rowIndex);
            switch (columnIndex) {
                case FAV_COLUMN:
                    return entry.favorite;
                case TITLE_COLUMN:
                    return entry.title;
                case GAME_COLUMN:
                    return entry.game.name;
                case TAGS_COLUMN:
                    return StringUtil.join(entry.tags, ", ");
                case LABELS_COLUMN:
                    return StringUtil.join(entry.labels, ", ");
                case ACTIVITY_COLUMN:
                    return entry.lastActivity;
                case USAGE_COLUMN:
                    return entry.timesUsed;
                default:
                    break;
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
            if (columnIndex == FAV_COLUMN) { // Fav
                return Boolean.class;
            }
            if (columnIndex == ACTIVITY_COLUMN) { // Last Activity
                return Long.class;
            }
            if (columnIndex == USAGE_COLUMN) { // Usage
                return Integer.class;
            }
            return String.class;
        }

    }
    
}
