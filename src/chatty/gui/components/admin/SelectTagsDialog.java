
package chatty.gui.components.admin;

import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.lang.Language;
import chatty.util.ElapsedTime;
import chatty.util.api.StreamTagManager.StreamTag;
import chatty.util.api.TwitchApi;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Select a game by manually entering it, searching for it on Twitch or
 * selecting one from the favorites.
 * 
 * @author tduva
 */
public class SelectTagsDialog extends JDialog {

    private static final String INFO = "<html><body style='width:340px'>"
            + Language.getString("admin.tags.info");

    private static final ImageIcon ADD_ICON = new ImageIcon(SelectTagsDialog.class.getResource("list-add.png"));
    private static final ImageIcon REMOVE_ICON = new ImageIcon(SelectTagsDialog.class.getResource("list-remove.png"));
    
    private final MainGui main;
    private final TwitchApi api;

    // General Buttons
    private final JButton ok = new JButton(Language.getString("dialog.button.save"));
    private final JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
    
    // Search/fav buttons
    private final JButton clearFilterButton = new JButton(Language.getString("admin.tags.button.clearFilter"));
    private final JButton addToFavoritesButton = new JButton(Language.getString("admin.tags.button.favorite"));
    private final JButton removeFromFavoritesButton = new JButton(Language.getString("admin.tags.button.unfavorite"));
    private final JCheckBox showAllTags = new JCheckBox(Language.getString("admin.tags.button.showAll"));

    // Current info elements
    private final JLabel listInfo = new JLabel();
    private final JTextField input = new JTextField();
    private final MyList list = new MyList();
    private final DefaultListModel<StreamTag> listData = new DefaultListModel<>();
    private final JTextArea description = new JTextArea();
    
    // Currently selected tags
    private final JPanel currentPanel = new JPanel();
    private final JButton addButton = new JButton(Language.getString("admin.tags.button.addSelected"));
    
    private static final int MAX_TAGS = 5;
    
    // Current tags data separate from GUI
    private final Set<StreamTag> favorites = new TreeSet<>();
    private final Set<StreamTag> allTags = new TreeSet<>();
    
    private StreamTag selected;
    private final List<StreamTag> current = new ArrayList<>();
    private final List<StreamTag> preset = new ArrayList<>();
    private final Timer timer;
    private ElapsedTime lastSelectionET = new ElapsedTime();
    private boolean loadingInfo;
    private String loadingAllTagsInfo;
    private StreamTag shouldMaybeRequest;
    
    // Whether to use the current tags
    private boolean save;
    
    public SelectTagsDialog(MainGui main, TwitchApi api) {
        super(main, Language.getString("admin.tags.title", MAX_TAGS), true);
        setResizable(false);
        
        this.main = main;
        this.api = api;
        
        setLayout(new GridBagLayout());
        list.setModel(listData);
        list.setCellRenderer(new ListRenderer());
        list.setFixedCellWidth(200);
        list.setVisibleRowCount(14);
        // Add dummy element to calculate normal cell height
        listData.addElement(new StreamTag("dummy", "dummy"));
        list.initCellHeight();
        
        GridBagConstraints gbc;
        
        Action doneAction = new DoneAction();
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "useSelectedGame");
        list.getActionMap().put("useSelectedGame", doneAction);
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, ALT_DOWN_MASK), "toggleFavorite");
        list.getActionMap().put("toggleFavorite", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFavorite();
            }
        });
        list.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                openListContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                openListContextMenu(e);
            }
        });
        
        timer = new Timer(100, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (lastSelectionET.millisElapsed(500)) {
                    loadCurrentInfo();
                }
            }
        });
        timer.setRepeats(true);
        
        //========
        // Layout
        //========
        
        //-------------
        // Top Section
        //-------------
        gbc = makeGbc(0,0,5,1);
        add(new JLabel(INFO), gbc);
        
        gbc = makeGbc(0,1,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(input, gbc);
        
        gbc = makeGbc(2,1,1,1);
        clearFilterButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(clearFilterButton, gbc);
        
        gbc = makeGbc(0,2,3,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,4,5,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(listInfo, gbc);
        
        gbc = makeGbc(2, 2, 2, 1);
        gbc.insets = new Insets(2,4,4,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        showAllTags.setSelected(true);
        add(showAllTags, gbc);
        
        //--------------
        // List Section
        //--------------
        
        gbc = makeGbc(0, 4, 2, 2);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        add(new JScrollPane(list), gbc);
        
        gbc = makeGbc(2, 4, 3, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setRows(4);
        description.setMargin(new Insets(2, 2, 2, 2));
        add(new JScrollPane(description), gbc);
        
        gbc = makeGbc(2, 5, 3, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        currentPanel.setLayout(new GridBagLayout());
        add(currentPanel, gbc);
 
        gbc = makeGbc(0,6,1,1);
        addToFavoritesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        addToFavoritesButton.setMnemonic(KeyEvent.VK_F);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(addToFavoritesButton, gbc);
        
        removeFromFavoritesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        addToFavoritesButton.setMnemonic(KeyEvent.VK_F);
        gbc = makeGbc(1,6,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(removeFromFavoritesButton, gbc);
        
        //--------------------
        // Save/close buttons
        //--------------------
        
        ok.setMnemonic(KeyEvent.VK_S);
        gbc = makeGbc(0,8,3,1);
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(ok, gbc);
        
        cancel.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(3,8,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(cancel, gbc);

        ActionListener actionListener = new MyActionListener();
        clearFilterButton.addActionListener(actionListener);
        input.addActionListener(actionListener);
        ok.addActionListener(actionListener);
        cancel.addActionListener(actionListener);
        list.addListSelectionListener(new MyListSelectionListener());
        list.addMouseListener(new ListClickListener());
        addToFavoritesButton.addActionListener(actionListener);
        removeFromFavoritesButton.addActionListener(actionListener);
        showAllTags.addActionListener(actionListener);
        
        input.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateList();
            }
        });
        
        // Button for currentPanel
        addButton.setMargin(new Insets(0, 3, 0, 4));
        addButton.addActionListener(e -> {
            addSelected();
        });
        addButton.setIcon(ADD_ICON);
        
        updateFavoriteButtons();
        
        pack();
    }
    
    /**
     * Open the dialog with the given tags preset.
     * 
     * @param preset
     * @return The list of tags to use, or {@code null} if they should not be
     * changed
     */
    public List<StreamTag> open(List<StreamTag> preset) {
        timer.start();
        this.preset.clear();
        this.preset.addAll(preset);
        setCurrent(preset);
        loadFavorites();
        updateList();
        save = false;
        setVisible(true);

        // Blocking dialog, so stuff can change in the meantime
        timer.stop();
        if (save) {
            return current;
        }
        return null;
    }

    /**
     * Closes the dialog, using the current tags.
     */
    private void useCurrentTagsAndClose() {
        save = true;
        setVisible(false);
    }
    
    private void setCurrent(List<StreamTag> data) {
        current.clear();
        current.addAll(data);
        updateCurrent();
    }
    
    /**
     * Update the display of the currently chosen tags, this means adding the
     * labels and removal buttons.
     */
    private void updateCurrent() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 5, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        currentPanel.removeAll();
        gbc.insets = new Insets(2, 2, 2, 2);
        for (int i = 0; i < MAX_TAGS; i++) {
            StreamTag tag;
            if (current.size() > i) {
                tag = current.get(i);
            } else {
                tag = new StreamTag(null, "empty");
            }
            gbc.gridy++;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            JLabel label = new JLabel(tag.toString());
            label.setEnabled(tag.isValid());
            currentPanel.add(label, gbc);

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 0;
            currentPanel.add(createRemoveTagButton(tag), gbc);
        }
        gbc.insets = new Insets(5, 2, 2, 2);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        // Only add "Add" button if less then max Tags are chosen
        if (current.size() < MAX_TAGS) {
            currentPanel.add(addButton, gbc);
        }
        currentPanel.invalidate();
        pack();
        repaint();
        updateAddButton();
        updateOkButton();
    }
    
    private JButton createRemoveTagButton(StreamTag tag) {
        JButton removeButton = new JButton(REMOVE_ICON);
        removeButton.setToolTipText(Language.getString("admin.tags.button.remove.tip", tag.toString()));
        removeButton.setMargin(new Insets(0, 0, 0, 0));
        removeButton.setSize(10, 10);
        removeButton.addActionListener(e -> {
            current.remove(tag);
            updateCurrent();
        });
        removeButton.setEnabled(tag.isValid());
        return removeButton;
    }
    
    /**
     * Add the tags currently selected in the list.
     */
    private void addSelected() {
        for (StreamTag c : list.getSelectedValuesList()) {
            if (canAddTag(c)) {
                current.add(c);
            } else {
                current.remove(c);
            }
            updateCurrent();
        }
    }
    
    /**
     * Check if any of the currently selected tags can be added.
     * 
     * @return 
     */
    private boolean canAddSomething() {
        for (StreamTag c : list.getSelectedValuesList()) {
            if (canAddTag(c)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if the given Tag can be added, so if any can be added and if this
     * one isn't added yet.
     * 
     * @param c
     * @return 
     */
    private boolean canAddTag(StreamTag c) {
        return c != null && c != StreamTag.EMPTY
                && current.size() < MAX_TAGS
                && !current.contains(c);
    }
   
    private void updateAddButton() {
        addButton.setEnabled(canAddSomething());
    }
    
    /**
     * An entry has been selected in the list.
     * 
     * @param c 
     */
    private void setSelected(StreamTag c) {
        if (c == null) {
            selected = null;
        } else {
            selected = c;
        }
        updateInfo();
        updateAddButton();
    }
    
    private void updateInfo() {
        if (selected == null || !selected.isValid()) {
            description.setText("Nothing to see here.");
            return;
        }
        
        StreamTag maybe = api.getCachedOnlyTagInfo(selected.getId());
        if (maybe != null) {
            description.setText(maybe.getSummary());
            description.setCaretPosition(0);
        } else {
            description.setText(Language.getString("status.loading"));
            lastSelectionET.set();
            shouldMaybeRequest = selected;
        }
    }
    
    private void loadCurrentInfo() {
        if (!loadingInfo && selected != null && selected == shouldMaybeRequest) {
            final StreamTag forRequest = selected;
            // This should only be done if cached info could not be found
            loadingInfo = true;
            shouldMaybeRequest = null;
            // The request will also add it to cached infos, so we don't need
            // to retrieve the result directly
            api.getStreamTagById(forRequest.getId(), (r, e) -> {
                SwingUtilities.invokeLater(() -> {
                    updateInfo();
                    
                    // In case same one was selected again, reset
                    if (forRequest.equals(shouldMaybeRequest)) {
                        shouldMaybeRequest = null;
                    }
                    loadingInfo = false;
                    updateName(r);
                });
            });
        }
    }
    
    private void updateName(StreamTag c) {
        if (c != null && favorites.remove(c)) {
            favorites.add(c);
            saveFavorites();
        }
    }
    
    /**
     * Clear the list and fill it with the current search result and favorites.
     * Also update the status text.
     */
    private void updateList() {
        listData.clear();
        String search = input.getText();
        if (search == null || search.trim().isEmpty()) {
            search = "";
        }
        search = search.toLowerCase();
        int addedCount = 0;
        if (!showAllTags.isSelected()) {
            for (StreamTag c : allTags) {
                if (c.getDisplayName().toLowerCase().contains(search)) {
                    listData.addElement(c);
                    addedCount++;
                }
            }
        } else {
            for (StreamTag c : current) {
                if (c.getDisplayName().toLowerCase().contains(search)) {
                    listData.addElement(c);
                    addedCount++;
                }
            }
        }
        if (!listData.isEmpty() && !favorites.isEmpty()) {
            listData.addElement(StreamTag.EMPTY);
        }
        int addedCountFavs = 0;
        for (StreamTag c : favorites) {
            if (c.getDisplayName().toLowerCase().contains(search)) {
                listData.addElement(c);
                addedCountFavs++;
            }
        }
        listInfo.setText(Language.getString("admin.tags.listInfo",
                addedCount,
                !showAllTags.isSelected() ? allTags.size() : current.size(),
                addedCountFavs, favorites.size())
            + (loadingAllTagsInfo != null ? " ("+loadingAllTagsInfo+")": ""));
        list.setSelectedValue(selected, false);
        pack();
    }
    
    private void showAllTags() {
        /**
         * Only request if allTags is currently empty. As soon as one request
         * succeeded that will be no longer the case. If on the other hand an
         * error occured without receiving any tags, another attempt may be
         * possible.
         */
        if (!showAllTags.isSelected() && allTags.isEmpty()) {
            loadingAllTagsInfo = Language.getString("status.loading");
            api.requestAllTags((r,e) -> {
                SwingUtilities.invokeLater(() -> {
                    if (r != null) {
                        for (StreamTag t : r) {
                            if (!t.isAuto()) {
                                allTags.add(t);
                            }
                        }
                    }
                    if (e != null) {
                        loadingAllTagsInfo = e;
                    } else if (r == null && e == null) {
                        loadingAllTagsInfo = null;
                    }
                    updateList();
                });
            });
            listInfo.setText(Language.getString("status.loading"));
        }
    }
    
    /**
     * Adds the currently selected tags to the favorites.
     */
    private void addToFavorites() {
        for (StreamTag tag : list.getSelectedValuesList()) {
            if (tag.isValid()) {
                favorites.add(tag);
            }
        }
        saveFavorites();
        updateList();
    }

    /**
     * Removes the currently selected tags from the favorites.
     */
    private void removeFromFavorites() {
        for (StreamTag tag : list.getSelectedValuesList()) {
            favorites.remove(tag);
        }
        saveFavorites();
        updateList();
    }
    
    /**
     * Removes all selected favorites and adds all selected non-favorites as
     * favorites.
     */
    private void toggleFavorite() {
        for (StreamTag tag : list.getSelectedValuesList()) {
            if (favorites.contains(tag) || !tag.isValid()) {
                favorites.remove(tag);
            } else {
                favorites.add(tag);
            }
        }
        saveFavorites();
        updateList();
    }
    
    /**
     * Stores the current list of favorites in the settings.
     */
    private void saveFavorites() {
        Map<String, String> favs = new HashMap<>();
        for (StreamTag c : favorites) {
            favs.put(c.getId(), c.getDisplayName());
        }
        main.setStreamTagFavorites(favs);
    }
    
    /**
     * Loads the current list of favorites from the settings.
     */
    private void loadFavorites() {
        favorites.clear();
        
        Map<String, String> favs = main.getStreamTagFavorites();
        for (String id : favs.keySet()) {
            StreamTag c = new StreamTag(id, favs.get(id));
            favorites.add(c);
        }
    }
    
    /**
     * Sets the state of the favorites buttons depending on the current
     * selection.
     */
    private void updateFavoriteButtons() {
        boolean favoriteSelected = false;
        boolean nonFavoriteSelected = false;
        for (StreamTag c : list.getSelectedValuesList()) {
            if (c.isValid()) {
                if (favorites.contains(c)) {
                    favoriteSelected = true;
                } else {
                    nonFavoriteSelected = true;
                }
            }
        }
        addToFavoritesButton.setEnabled(nonFavoriteSelected);
        removeFromFavoritesButton.setEnabled(favoriteSelected);
    }
    
    private void updateOkButton() {
        ok.setEnabled(!preset.equals(current));
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(4,4,4,4);
        return gbc;
    }
    
    /**
     * Change the game to be used to the one currently selected in the given
     * JList.
     * 
     * @param list 
     */
    private void updateGameFromSelection() {
        StreamTag selected = list.getSelectedValue();
        if (selected != null) {
            setSelected(selected);
        }
    }
    
    /**
     * Called when an item is selected either by changing the selected item
     * or clicking an already selected item.
     * 
     * @param source 
     */
    private void itemSelected() {
        updateGameFromSelection();
    }
    
    private void openListContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            selectClicked(e, false);

            List<StreamTag> listSelected = list.getSelectedValuesList();
            JPopupMenu menu = new JPopupMenu();
            menu.add(new AbstractAction(Language.getString("admin.tags.cm.replaceAll")) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    setCurrent(listSelected);
                    updateCurrent();
                }
            });

            if (listSelected.size() == 1
                    && !current.isEmpty()
                    && !current.contains(list.getSelectedValue())) {
                menu.addSeparator();
                for (StreamTag c : current) {
                    menu.add(new AbstractAction(Language.getString("admin.tags.cm.replace", c)) {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            StreamTag replaceWith = list.getSelectedValue();
                            current.replaceAll(item -> {
                                if (item.equals(c)) {
                                    return replaceWith;
                                }
                                return item;
                            });
                            updateCurrent();
                        }
                    });
                }
            }
            menu.show(list, e.getX(), e.getY());
        }
    }
    
    private void selectClicked(MouseEvent e, boolean onlyOutside) {
        int index = list.locationToIndex(e.getPoint());
        Rectangle bounds = list.getCellBounds(index, index);
        if (bounds != null && bounds.contains(e.getPoint())) {
            if (!onlyOutside) {
                if (list.isSelectedIndex(index)) {
                    list.addSelectionInterval(index, index);
                } else {
                    list.setSelectedIndex(index);
                }
            }
        } else {
            list.clearSelection();
        }
    }
    
    private class MyActionListener implements ActionListener {
    
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == ok) {
                useCurrentTagsAndClose();
            }
            if (e.getSource() == cancel) {
                save = false;
                setVisible(false);
            }
            if (e.getSource() == addToFavoritesButton) {
                addToFavorites();
            }
            if (e.getSource() == removeFromFavoritesButton) {
                removeFromFavorites();
            }
            if (e.getSource() == clearFilterButton) {
                input.setText(null);
            }
            if (e.getSource() == showAllTags) {
                showAllTags();
                updateList();
            }
        }
    }
    
    private class MyListSelectionListener implements ListSelectionListener {
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            itemSelected();
            updateFavoriteButtons();
        }
    }

    /**
     * Use game by double-click.
     */
    private class ListClickListener extends MouseAdapter {
        
        @Override
        public void mouseClicked(MouseEvent e) {
            itemSelected();
            if (e.getClickCount() == 2) {
                addSelected();
            }
        }
    }
    
    private class DoneAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            addSelected();
        }
        
    }
    
    /**
     * Due to how the default getPreferredScrollableViewportSize() is
     * implemented, it only calculates the height correctly (when based on
     * visible rows) if there is at least one element in the list (since it
     * takes the height of the first element).
     * 
     * This sets the default cellHeight once and then uses that for height
     * calculation. It also only bases the size on fixed width and visible rows,
     * so setVisibleRowCount() and setFixedCellWidth() should be used.
     */
    private static class MyList extends JList<StreamTag> {
        
        private int cellHeight = 16;
        
        /**
         * Sets the default cell height using the height of the first cell, so
         * at least one element of regular size (e.g. with text) has to be in
         * the list. The correct renderer should also already be set.
         */
        public void initCellHeight() {
            Rectangle r = getCellBounds(0, 0);
            if (r != null) {
                cellHeight = r.height;
            }
        }
        
        /**
         * Calculate the preferred size only based on the fixed cell width and
         * visible row count, using the cellHeight set with initCellHeight (or a
         * default).
         * 
         * @return 
         */
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            Insets insets = getInsets();
            int dx = insets.left + insets.right;
            int dy = insets.top + insets.bottom;

            int visibleRowCount = getVisibleRowCount();
            int fixedCellWidth = getFixedCellWidth();

            int width = fixedCellWidth + dx;
            int height = cellHeight * visibleRowCount + dy;
            
            return new Dimension(width, height);
        }
        
    }
    
    /**
     * Custom list item renderer, showing the star icon for favorites and adding
     * a seperating line.
     */
    private class ListRenderer extends DefaultListCellRenderer {
        
        private final ImageIcon icon = new ImageIcon(MainGui.class.getResource("star.png"));
        private final Border seperatorBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY);
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) {
                return label;
            }
            String text = value.toString();
            if (text == null || text.isEmpty()) {
                label.setText(null);
                label.setBorder(seperatorBorder);
            }
            if (favorites.contains(value)) {
                label.setIcon(icon);
            }
            if (current.contains(value)) {
                label.setEnabled(false);
            }
            return label;
        }
    }
    
}
