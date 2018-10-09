
package chatty.gui.components.settings;

import chatty.util.StringUtil;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * A table containing one element per row, with editing features.
 * 
 * @author tduva
 */
public class TableEditor<T> extends JPanel {
    
    private static final Dimension BUTTON_SIZE = new Dimension(27,27);
    
    public static final int SORTING_MODE_MANUAL = 0;
    public static final int SORTING_MODE_SORTED = 1;
    
    private final ButtonAction buttonActionListener = new ButtonAction();
    
    // Table State
    private final JTable table;
    private ListTableModel<T> data;
    private ItemEditor<T> editor;
    private TableRowSorter<ListTableModel<T>> sorter;
    private int sortingMode;
    private boolean currentlyFiltering;
    
    private String search = "";
    private long searchTime = 0;
    private int searchColumn;
    private final Timer searchTimer;
    
    /**
     * Edit buttons
     */
    private final JButton add = new JButton();
    private final JButton remove = new JButton();
    private final JButton edit = new JButton();
    private final JButton moveUp = new JButton();
    private final JButton moveDown = new JButton();
    private final JButton refresh = new JButton();
    
    private final JTextField filterInput = new JTextField();
    
    private TableEditorListener listener;
    private TableContextMenu contextMenu;

    /**
     * 
     * The {@code sortingMode} determines the sorting features this table provides:
     * <ul>
     * <li>{@code SORTING_MODE_MANUAL} means the user can/has to order the entries
     * manually, which may be required for some applications</li>
     * <li> {@code SORTING_MODE_SORTED} means the table can sort the entries by their
     * natural order and items can also be filtered</li>
     * </ul>
     * 
     * @param sortingMode The sorting mode
     * @param refreshButton Whether this table should have a reload button,
     * which may only be applicable for some uses
     */
    public TableEditor(int sortingMode, boolean refreshButton) {
        this.sortingMode = sortingMode;
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        
        // Selection Listener to update buttons
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateButtons();
            }
        });
        
        // Mouse Listener to edit items and open context menu
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedItem();
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                selectRowAt(e.getPoint());
                popupMenu(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                popupMenu(e);
            }
        });
        
        // Delete key
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeItems");
        table.getActionMap().put("removeItems", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelected();
            }
        });
        
        table.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                search(e.getKeyChar());
            }
        });
        searchTimer = new Timer(500, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                checkResetSearch();
            }
        });
        searchTimer.setRepeats(true);

        // Buttons Configuration
        configureButton(add, "list-add.png", "Add new item (after selected)");
        configureButton(edit, "edit.png", "Edit selected item (double-click)");
        configureButton(remove, "list-remove.png", "Remove selected item");
        configureButton(moveUp, "go-up.png", "Move selected item up");
        configureButton(moveDown, "go-down.png", "Move selected item down");
        configureButton(refresh, "view-refresh.png", "Refresh data");
        
        // Layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = makeGbc(0, 0, 2, 7);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(new JScrollPane(table), gbc);
        
        // Filter
        
        if (sortingMode == SORTING_MODE_SORTED) {
            gbc = makeGbc(0, 7, 1, 1);
            gbc.insets = new Insets(0,2,0,1);
            JLabel filterInputLabel = new JLabel("Filter: ");
            filterInputLabel.setLabelFor(filterInput);
            add(filterInputLabel, gbc);
            
            gbc = makeGbc(1, 7, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(filterInput, gbc);
            filterInput.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateFiltering();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateFiltering();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateFiltering();
                }
            });
        }
        
        // Buttons
        gbc = makeGbc(2, 0, 1, 1);
        add(add, gbc);
        
        gbc = makeGbc(2, 1, 1, 1);
        add(remove, gbc);
        
        gbc = makeGbc(2, 2, 1, 1);
        add(edit, gbc);
        
        if (sortingMode == SORTING_MODE_MANUAL) {
            gbc = makeGbc(2, 3, 1, 1);
            add(moveUp, gbc);

            gbc = makeGbc(2, 4, 1, 1);
            add(moveDown, gbc);
        }
        
        if (refreshButton) {
            gbc = makeGbc(2, 5, 1, 1);
            add(refresh, gbc);
        }
        
        updateButtons();
    }
    
    /**
     * Set the model for this table, which must be done before it is used.
     *
     * @param model
     */
    protected final void setModel(ListTableModel<T> model) {
        data = model;
        table.setModel(model);
        if (sortingMode == SORTING_MODE_SORTED) {
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            sorter.toggleSortOrder(0);
        }
    }
    
    /**
     * Allows to set custom renderers for certain classes.
     * 
     * @param cellClass
     * @param renderer 
     */
    protected final void setDefaultRenderer(Class cellClass, TableCellRenderer renderer) {
        table.setDefaultRenderer(cellClass, renderer);
    }
    
    protected final void setRendererForColumn(int column, TableCellRenderer renderer) {
        table.getColumnModel().getColumn(column).setCellRenderer(renderer);
    }
    
    protected final void setFixedColumnWidth(int column, int size) {
        table.getColumnModel().getColumn(column).setMaxWidth(size);
        table.getColumnModel().getColumn(column).setMinWidth(size);
    }
    
    protected final void setColumnWidth(int column, int size) {
        table.getColumnModel().getColumn(column).setPreferredWidth(size);
    }

    /**
     * Set the data for this table.
     * 
     * @param data 
     */
    public void setData(List<T> data) {
        this.data.setData(data);
        updateButtons();
    }
    
    /**
     * Returns the (possibly edited by the user) data of this table.
     * 
     * @return 
     */
    public List<T> getData() {
        return this.data.getData();
    }
    
    /**
     * Sets the item editor, which must be done before stuff can be edited.
     * 
     * @param editor 
     */
    public void setItemEditor(ItemEditor<T> editor) {
        this.editor = editor;
    }
    
    /**
     * Sets the context menu for this.
     * 
     * @param menu 
     */
    public final void setPopupMenu(TableContextMenu<T> menu) {
        contextMenu = menu;
    }
    
    /**
     * Sets the {@code TableEditorListener}. Only one listener can be set at a
     * time.
     * 
     * @param listener The listener to set
     */
    public final void setTableEditorListener(TableEditorListener<T> listener) {
        this.listener = listener;
    }
    
    /**
     * Opens the context menu if this MouseEvent was a popup trigger and a menu
     * is set.
     * 
     * @param e The MouseEvent
     */
    private void popupMenu(MouseEvent e) {
        if (contextMenu != null && e.isPopupTrigger()) {
            int modelIndex = indexToModel(table.getSelectedRow());
            if (modelIndex != -1) {
                T entry = data.get(modelIndex);
                contextMenu.showMenu(entry, table, e.getX(), e.getY());
            }
        }
    }
    
    /**
     * Select the row at the given coordinates.
     * 
     * @param p The {@code Point} containing the coordinates
     */
    private void selectRowAt(Point p) {
        int row = table.rowAtPoint(p);
        if (row != -1) {
            setRowSelected(row);
        }
    }
    
    /**
     * Convenience method to create {@code GridBagConstraints}.
     * 
     * @param x The x coordinate in the grid
     * @param y The y coordinate in the grid
     * @param w The width in the grid
     * @param h The height in the grid
     * @return {@code GridBagConstraints} with the given values
     */
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        return gbc;
    }

    
    private void updateFiltering() {
        String filterText = filterInput.getText();
        RowFilter<ListTableModel<T>, Object> rf = null;
        try {
            rf = RowFilter.regexFilter(filterText, 0);
        } catch (PatternSyntaxException ex) {
            return;
        }
        currentlyFiltering = rf != null && !filterText.isEmpty();
        sorter.setRowFilter(rf);
        scrollToSelection();
        updateButtons();
        
    }
    

    
    /**
     * Sets the size, icon and tooltip of a button and adds the ActionListener.
     * 
     * @param button
     * @param icon
     * @param tooltip 
     */
    private void configureButton(JButton button, String icon, String tooltip) {
        button.setIcon(new ImageIcon(ListSelector.class.getResource(icon)));
        button.setToolTipText(tooltip);
        button.setPreferredSize(BUTTON_SIZE);
        button.setSize(BUTTON_SIZE);
        button.setMaximumSize(BUTTON_SIZE);
        button.setMinimumSize(BUTTON_SIZE);
        button.addActionListener(buttonActionListener);
    }
    
    /**
     * Update the enabled-state of the buttons.
     */
    private void updateButtons() {
        boolean enabled = table.getSelectedRowCount() == 1;
        add.setEnabled(true);
        remove.setEnabled(enabled);
        edit.setEnabled(enabled);
        moveUp.setEnabled(enabled);
        moveDown.setEnabled(enabled);
        if (currentlyFiltering) {
            add.setEnabled(false);
            edit.setEnabled(false);
            moveUp.setEnabled(false);
            moveDown.setEnabled(false);
        }
    }
    
    /**
     * Sets the given row as selected and scrolls to it if necessary.
     * 
     * @param viewIndex 
     */
    private void setRowSelected(int viewIndex) {
        table.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
        scrollToRow(viewIndex);
    }
    
    private void scrollToSelection() {
        int index = table.getSelectedRow();
        scrollToRow(index);
    }
    
    private void scrollToRow(int index) {
        if (index != -1) {
            table.scrollRectToVisible(table.getCellRect(index, 0, true));
//            System.out.println(table.getVisibleRect()+" "+);
//            Rectangle row = table.getCellRect(index, 0, true);
//            int visibleHeight = table.getVisibleRect().height;
//            int rowHeight = row.height;
//            if (visibleHeight > rowHeight*4) {
//                
//            }
        }
    }
    
    /**
     * Open the edit dialog with the given {@code preset} already filling in
     * the data it contains. If the edit dialog isn't canceled, the resulting
     * entry is added after checking for duplicates. It is added at the selected
     * position or at the beginning of the table if nothing is selected.
     * 
     * @param preset The entry used to fill out some data in the edit dialog
     */
    protected void addItem(T preset) {
        T result = editor.showEditor(preset, this, false, -1);
        // If the user didn't cancel the dialog, work with the result.
        if (result != null) {
            // Check if the resulting entry is already in the table.
            if (data.contains(result)) {
                String[] options = new String[]{"Don't save", "Edit again"};
                int r = JOptionPane.showOptionDialog(this, "Another item with the same name"
                        + " is already in the list.", "Duplicate item", 
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
                if (r == 1) {
                    addItem(result);
                }
            } else {
                // Insert at the selected position or at the beginning of the
                // table if nothing is selected.
                int selectedIndex = table.getSelectedRow();
                int modelIndex = indexToModel(selectedIndex);
                if (modelIndex != -1) {
                    data.insert(modelIndex, result);
                    setRowSelected(indexToView(modelIndex));
                } else {
                    data.insert(0, result);
                    setRowSelected(indexToView(0));
                }
                if (listener != null) {
                    listener.itemAdded(result);
                }
            }
        }
    }
    
    /**
     * Edit the currently selected item.
     * 
     * @see editItem(int modelIndex, T preset)
     */
    private void editSelectedItem() {
        editItem(-1, null);
    }

    /**
     * Edit the entry at the given {@code modelIndex}.
     * 
     * @param modelIndex The index
     * @see editItem(int modelIndex, T preset)
     */
    protected void editItem(int modelIndex) {
        editItem(modelIndex, null);
    }
    
    /**
     * Open an edit dialog for the entry at the given {@code modelIndex}.
     *
     * @param modelIndex The model index. If this is -1 then the currently
     * selected entry is edited. If no entry is selected, then nothing is done.
     * @param preset The preset is used to fill out the dialog with the data it
     * contains, if it is {@code null}, then the edited entry is used as preset
     */
    protected void editItem(int modelIndex, T preset) {
        if (modelIndex == -1) {
            modelIndex = indexToModel(table.getSelectedRow());
            if (modelIndex == -1) {
                return;
            }
        }
        setRowSelected(indexToView(modelIndex));
        if (preset == null) {
            preset = data.get(modelIndex);
        }
        T result = editor.showEditor(preset, this, true, table.getSelectedColumn());
        
        // Done editing in the dialog, work with the result if the user didn't
        // cancel the dialog.
        if (result != null) {
            // Check if the resulting entry is already in the data, but is not
            // the one being edited, which means it would be a duplicate.
            int present = data.indexOf(result);
            if (present != -1 && present != modelIndex) {
                String[] options = new String[]{"Don't save", "Edit again"};
                int r = JOptionPane.showOptionDialog(this, "Another item with the same name"
                        + " is already in the list.", "Duplicate item", 
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, "Replace");
                if (r == 1) {
                    editItem(modelIndex, result);
                }
            } else {
                data.set(modelIndex, result);
                if (listener != null) {
                    listener.itemEdited(preset, result);
                }
            }
        }
        setRowSelected(indexToView(modelIndex));
    }
    
    /**
     * Remove the selected entry. If no entry is selected, nothing is done.
     * After removing, an appropriate remaining entry is selected.
     */
    protected void removeSelected() {
        // If table is empty, nothing can be selected to remove
        if (table.getRowCount() == 0) {
            return;
        }
        
        // Get selected entry and remove it if present
        int viewIndex = table.getSelectedRow();
        int modelIndex = indexToModel(viewIndex);
        if (modelIndex == -1) {
            return;
        }
        T removedItem = data.remove(modelIndex);
        
        // Select appropriate row after removing
        if (table.getRowCount() > viewIndex) {
            setRowSelected(viewIndex);
        } else if (viewIndex-1 >= 0 && table.getRowCount() > viewIndex-1) {
            setRowSelected(viewIndex-1);
        }
        
        // Update buttons state and inform listener
        updateButtons();
        if (listener != null) {
            listener.itemRemoved(removedItem);
        }
    }
    
    /**
     * Moves the selected item up in the model (and table). This can behave
     * kind of odd when the table is filtered or sorted automatically, so it
     * should not be used then.
     */
    protected void moveUpSelected() {
        int selectedIndex = table.getSelectedRow();
        if (selectedIndex > -1) {
            int index = data.moveUp(indexToModel(selectedIndex));
            setRowSelected(indexToView(index));
        }
    }
    
    /**
     * Moves the selected item down in the model (and table). This can behave
     * kind of odd when the table is filtered or sorted automatically, so it
     * should not be used then.
     */
    protected void moveDownSelected() {
        int selectedIndex = table.getSelectedRow();
        if (selectedIndex > -1) {
            int index = data.moveDown(indexToModel(selectedIndex));
            setRowSelected(indexToView(index));
        }
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
        return table.convertRowIndexToModel(index);
    }
    
    /**
     * Convert a model index to view index.
     * 
     * @param index The index to convert
     * @return The corresponding index of the view, or {@code -1} if the row
     * isn't visible
     */
    private int indexToView(int index) {
        return table.convertRowIndexToView(index);
    }

    /**
     * Receives events from the buttons and calls the appropriate table methods.
     */
    private class ButtonAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == moveUp) {
                moveUpSelected();
            } else if (e.getSource() == moveDown) {
                moveDownSelected();
            } else if (e.getSource() == remove) {
                removeSelected();
            } else if (e.getSource() == edit) {
                editSelectedItem();
            } else if (e.getSource() == add) {
                addItem(null);
            } else if (e.getSource() == refresh) {
                if (listener != null) {
                    listener.refreshData();
                }
            }
        }
    }
    
    
    /**
     * A context menu that in addition to the invoker and coordinates, also
     * receives the item it was opened on, so it can build the menu accordingly.
     *
     * @param <T> The type of the item
     */
    public static abstract class TableContextMenu<T> extends JPopupMenu {

        /**
         * The menu should open itself at the given coordinates. It can
         * customize itself based on which {@code item} it was opened for.
         *
         * @param item The item it was opened for (usually by right-clicking
         * on it)
         * @param invoker The Component it was opened on
         * @param x The x-coordinate where it should be opened
         * @param y The y-coordinate where it should be opened
         */
        public abstract void showMenu(T item, Component invoker, int x, int y);

    }
    

    /**
     * An item editor is opened with the item to edit, the parent component
     * and whether the item is being edited or added. The implementation can
     * then build the GUI accordingly. When finished, the editor should give
     * the edited item back, or null if the action was canceled.
     * 
     * @param <T> The type of the item to edit
     */
    public static interface ItemEditor<T> {

        /**
         * Opens the editor, which the user can use to add or change an item.
         *
         * @param preset The item to fill the GUI with initially, can be
         * {@code null}
         * @param c The parent component
         * @param edit Whether this item is edited or added (might set the title
         * accordingly for example)
         * @return The changed or added item, or {@code null} if the action was
         * canceled
         */
        public T showEditor(T preset, Component c, boolean edit, int column);
        
    }
    
    /**
     * Users of the TableEditor can register a listener of this type to be
     * informed about edits to the table. This is one of the main ways to
     * actually change the data that is edited in this table elsewhere.
     * 
     * @param <T> The type of the items to be edited
     */
    public static interface TableEditorListener<T> {
        
        /**
         * Called when an item has been added to the table. The table should
         * not allow for duplicates to be added, but it is prudent to not rely
         * on that.
         * 
         * @param item The item that was added
         */
        public void itemAdded(T item);
        
        /**
         * Called when an item has been removed in the table.
         * 
         * @param item The item that was removed
         */
        public void itemRemoved(T item);
        
        /**
         * Called when an item was edited in the table. The {@code oldItem}
         * contains the item before editing, the {@code newItem} contains the
         * changed item, so it can also be determined what changed.
         * 
         * @param oldItem The item before editing
         * @param newItem The item after editing
         */
        public void itemEdited(T oldItem, T newItem);
        
        /**
         * Called when the user requested the data in the table to be refreshed.
         */
        public void refreshData();
    }
    
    private void search(char input) {
        // Reset search on backspace
        if (input == '\b') {
            resetSearch();
            return;
        }
        
        // Reset search if searching on other column
        int column = data.getSearchColumn(table.getSelectedColumn());
        if (column == -1) {
            // No search column, so don't do any search
            return;
        }
        if (column != searchColumn) {
            resetSearch();
        }
        
        // Update state
        String pressed = String.valueOf(input);
        search += StringUtil.toLowerCase(pressed);
        searchColumn = column;
        searchTime = System.currentTimeMillis();

        //System.out.println("'" + search + "'");

        // Rename header to current search
        table.getColumnModel().getColumn(column).setHeaderValue("[" + search + "]");
        table.getTableHeader().repaint();
        
        // Start timer to reset search
        if (!searchTimer.isRunning()) {
            searchTimer.start();
        }
        
        // Perform search and select first result
        for (int i = 0; i < data.getRowCount(); i++) {
            String item = StringUtil.toLowerCase(data.getSearchValueAt(i, column));
            if (item.startsWith(search)) {
                setRowSelected(indexToView(i));
                return;
            }
            if (item.contains(search)) {
                setRowSelected(indexToView(i));
            }
        }
    }
    
    private void checkResetSearch() {
        if (System.currentTimeMillis() - searchTime > 3000) {
            resetSearch();
        }
    }
    
    private void resetSearch() {
        if (searchColumn != -1) {
            String originalValue = data.getColumnName(searchColumn);
            table.getColumnModel().getColumn(searchColumn).setHeaderValue(originalValue);
            table.getTableHeader().repaint();
        }
        searchColumn = -1;
        search = "";
        searchTimer.stop();
    }
    
}
