
package chatty.gui.components.admin;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.HtmlColors;
import chatty.gui.MainGui;
import chatty.gui.UrlOpener;
import chatty.util.api.CommunitiesManager.Community;
import chatty.util.api.TwitchApi;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLDocument;

/**
 * Select a game by manually entering it, searching for it on Twitch or
 * selecting one from the favorites.
 * 
 * @author tduva
 */
public class SelectCommunityDialog extends JDialog {

    private static final String INFO = "<html><body style='width:340px'>"
            + "Twitch currently does not offer a search API, so enter the exact "
            + "name of a community and click 'Search' (or press Enter) to verify it's name.";

    private static final ImageIcon ADD_ICON = new ImageIcon(SelectCommunityDialog.class.getResource("list-add.png"));
    private static final ImageIcon REMOVE_ICON = new ImageIcon(SelectCommunityDialog.class.getResource("list-remove.png"));
    
    private final MainGui main;
    private final TwitchApi api;

    // General Buttons
    private final JButton ok = new JButton("Save changes");
    private final JButton cancel = new JButton("Cancel");
    
    // Game search/fav buttons
    private final JButton searchButton = new JButton("Search");
    private final JButton addToFavoritesButton = new JButton("Favorite");
    private final JButton removeFromFavoritesButton = new JButton("Unfavorite");
    private final JButton clearSearchButton = new JButton("Clear");
    private final JButton openUrl = new JButton("Open URL");
    private final JButton top100 = new JButton("Top 100");

    // Current info elements
    private final JLabel searchResultInfo = new JLabel("No search performed yet.");
    private final JTextField input = new JTextField(30);
    private final JList<Community> list = new JList<>();
    private final DefaultListModel<Community> listData = new DefaultListModel<>();
    private final JTextPane description = new JTextPane();
    
    // Currently selected communities
    private final JPanel currentPanel = new JPanel();
    private final JButton addButton = new JButton("Add selected");
    
    private static final int MAX_COMMUNITIES = 3;
    
    // Current games data seperate from GUI
    private final Set<Community> favorites = new TreeSet<>();
    private final Set<Community> searchResult = new TreeSet<>();
    
    private Community selected;
    private final List<Community> current = new ArrayList<>();
    private final List<Community> preset = new ArrayList<>();
    private final Timer timer;
    private long lastSelectionTime;
    private boolean loading;
    private Community shouldMaybeRequest;
    
    // Whether to use the current game
    private boolean save;
    
    public SelectCommunityDialog(MainGui main, TwitchApi api) {
        super(main, "Choose up to "+MAX_COMMUNITIES+" communities", true);
        setResizable(true);
        
        this.main = main;
        this.api = api;
        
        setLayout(new GridBagLayout());
        list.setModel(listData);
        list.setVisibleRowCount(12);
        list.setCellRenderer(new ListRenderer());
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
                if (System.currentTimeMillis() - lastSelectionTime > 500) {
                    loadCurrentInfo();
                }
            }
        });
        timer.setRepeats(true);
        
        gbc = makeGbc(0,0,5,1);
        add(new JLabel(INFO), gbc);
        
        gbc = makeGbc(0,1,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(input, gbc);
        
        searchButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        gbc = makeGbc(2,1,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(searchButton, gbc);
        
        gbc = makeGbc(0,2,3,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,4,4,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(searchResultInfo, gbc);
        
        gbc = makeGbc(2, 2, 1, 1);
        clearSearchButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2,4,4,4);
        add(clearSearchButton, gbc);
        
        gbc = makeGbc(4, 2, 1, 1);
        openUrl.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        gbc.insets = new Insets(2,4,4,4);
        gbc.anchor = GridBagConstraints.EAST;
        add(openUrl, gbc);
        
        gbc = makeGbc(3, 1, 1, 1);
        top100.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        gbc.insets = new Insets(2,4,4,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        add(top100, gbc);
        
        gbc = makeGbc(0, 4, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 1;
        add(new JScrollPane(list), gbc);
        
        gbc = makeGbc(2, 4, 3, 2);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        description.setEditable(false);
        description.setContentType("text/html");
        String textColor = HtmlColors.getColorString(searchResultInfo.getForeground());
        int textSize = searchResultInfo.getFont().getSize();
        ((HTMLDocument)description.getDocument()).getStyleSheet().addRule(""
                + "body { font: sans-serif; font-size: "+textSize+"pt; padding:3px; color:"+textColor+"; }"
                + "a { color:"+textColor+"; }"
                + "h2 { border-bottom: 1px solid "+textColor+"; font-size: "+(textSize+2)+"pt; }");
        add(new JScrollPane(description), gbc);
 
        gbc = makeGbc(0,5,1,1);
        addToFavoritesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        addToFavoritesButton.setMnemonic(KeyEvent.VK_F);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(addToFavoritesButton, gbc);
        
        removeFromFavoritesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        addToFavoritesButton.setMnemonic(KeyEvent.VK_F);
        gbc = makeGbc(1,5,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(removeFromFavoritesButton, gbc);
        
        gbc = makeGbc(0,6,5,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 0, 5);
        //add(new JLabel("Currently chosen Communities:"), gbc);
        
        gbc = makeGbc(0,7,5,1);
        gbc.anchor = GridBagConstraints.WEST;
        add(currentPanel, gbc);
        
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
        searchButton.addActionListener(actionListener);
        openUrl.addActionListener(actionListener);
        top100.addActionListener(actionListener);
        input.addActionListener(actionListener);
        ok.addActionListener(actionListener);
        cancel.addActionListener(actionListener);
        list.addListSelectionListener(new MyListSelectionListener());
        list.addMouseListener(new ListClickListener());
        addToFavoritesButton.addActionListener(actionListener);
        removeFromFavoritesButton.addActionListener(actionListener);
        clearSearchButton.addActionListener(actionListener);
        
        description.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String url = e.getURL().toString();
                UrlOpener.openUrlPrompt(SelectCommunityDialog.this, url, true);
            }
        });
        
        // Button for currentPanel
        addButton.setMargin(new Insets(0, 3, 0, 4));
        addButton.addActionListener(e -> {
            addSelected();
        });
        addButton.setToolTipText("Tip: Double-click on list entry to add");
        addButton.setIcon(ADD_ICON);
        
        updateFavoriteButtons();
        
        pack();
        
        setMinimumSize(getSize());
        
    }
    
    /**
     * Open the dialog with the given game preset.
     * 
     * @param preset
     * @return The name of the game to use, or {@code null} if the game should
     * not be changed
     */
    public List<Community> open(List<Community> preset) {
        timer.start();
        this.preset.clear();
        this.preset.addAll(preset);
        setCurrent(preset);
        loadFavorites();
        for (Community c : preset) {
            if (c != Community.EMPTY && !favorites.contains(c)) {
                searchResult.add(c);
            }
        }
        update();
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
     * Closes the dialog, using the current game.
     */
    private void useGameAndClose() {
        save = true;
        setVisible(false);
    }
    
    private void setCurrent(List<Community> data) {
        current.clear();
        current.addAll(data);
        updateCurrent();
    }
    
    /**
     * Update the display of the currently chosen Communities, this means
     * adding the labels and buttons to remove them.
     */
    private void updateCurrent() {
        currentPanel.removeAll();
        if (current.isEmpty()) {
            currentPanel.add(new JLabel("No Community chosen."));
        } else {
            for (Community c : current) {
                JLabel label = new JLabel(c.toString());
                currentPanel.add(label);
                JButton removeButton = new JButton(REMOVE_ICON);
                removeButton.setToolTipText("Remove '"+c.toString()+"'");
                removeButton.setMargin(new Insets(0,0,0,0));
                removeButton.setSize(10, 10);
                removeButton.addActionListener(e -> {
                    current.remove(c);
                    updateCurrent();
                });
                currentPanel.add(removeButton);
            }
        }
        // Only add "Add" button if less then max Communities are chosen
        if (current.size() < 3) {
            currentPanel.add(addButton);
        }
        revalidate();
        // Make dialog bigger if necessary
        if (currentPanel.getPreferredSize().width+20 > getWidth()) {
            setSize(currentPanel.getPreferredSize().width+20, getHeight());
        }
        updateAddButton();
        updateOkButton();
    }
    
    /**
     * Add the Communities currently selected in the list.
     */
    private void addSelected() {
        for (Community c : list.getSelectedValuesList()) {
            if (canAddCommunity(c)) {
                current.add(c);
                updateCurrent();
            }
        }
    }
    
    /**
     * Check if one of the currently selected Communities can be added.
     * 
     * @return 
     */
    private boolean canAddSomething() {
        for (Community c : list.getSelectedValuesList()) {
            if (canAddCommunity(c)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if the given Community can be added, so if any can be added and if
     * this one isn't added yet.
     * 
     * @param c
     * @return 
     */
    private boolean canAddCommunity(Community c) {
        return c != null && c != Community.EMPTY
                && current.size() < MAX_COMMUNITIES
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
    private void setSelected(Community c) {
        if (c == null) {
            selected = null;
            input.setText(null);
        } else {
            selected = c;
            input.setText(c.toString());
        }
        updateInfo();
        updateAddButton();
    }
    
    private void updateInfo() {
        if (selected == null || selected.isValid()) {
            description.setText("Nothing to see here.");
            return;
        }
        
        Community maybe = api.getCachedCommunityInfo(selected.getId());
        if (maybe != null) {
            description.setText(String.format(
                    "<html><body>%s<h2>Rules</h2>%s",
                    maybe.getSummary(),
                    maybe.getRules()));
            description.setCaretPosition(0);
        } else {
            description.setText("Loading..");
            lastSelectionTime = System.currentTimeMillis();
            shouldMaybeRequest = selected;
        }
    }
    
    private void loadCurrentInfo() {
        if (!loading && selected != null && selected == shouldMaybeRequest) {
            final Community forRequest = selected;
            // This should only be done if cached info could not be found
            loading = true;
            shouldMaybeRequest = null;
            // The request will also add it to cached infos, so we don't need
            // to retrieve the result directly
            api.getCommunityById(forRequest.getId(), (r, e) -> {
                SwingUtilities.invokeLater(() -> {
                    updateInfo();
                    
                    // In case same one was selected again, reset
                    if (forRequest.equals(shouldMaybeRequest)) {
                        shouldMaybeRequest = null;
                    }
                    loading = false;
                    updateName(r);
                });
            });
        }
    }
    
    private void updateName(Community c) {
        if (c != null && favorites.remove(c)) {
            favorites.add(c);
            saveFavorites();
        }
    }
    
    /**
     * Clear the list and fill it with the current search result and favorites.
     * Also update the status text.
     */
    private void update() {
        listData.clear();
        for (Community c : searchResult) {
            listData.addElement(c);
        }
        if (!searchResult.isEmpty() && !favorites.isEmpty()) {
            listData.addElement(Community.EMPTY);
        }
        for (Community c : favorites) {
            listData.addElement(c);
        }
        searchResultInfo.setText("Search: "+searchResult.size()+" / "
            +"Favorites: "+favorites.size()+"");
        list.setSelectedValue(selected, false);
    }
    
    private void doSearch() {
        String searchString = input.getText().trim();
        if (searchString.isEmpty()) {
            searchResultInfo.setText("Enter something to search.");
            return;
        }
        api.getCommunityByName(searchString, (r, e) -> {
            SwingUtilities.invokeLater(() -> {
                if (r == null) {
                    if (e != null) {
                        searchResultInfo.setText(e);
                    } else {
                        searchResultInfo.setText("An error occured.");
                    }
                } else {
                    setSelected(r);
                    // Update cached name, if necessary (not sure if Communities
                    // can even change name, but it's certainly not impossible).
                    // Do it here because the result from the API should be
                    // correct.
                    updateName(r);
                    
                    searchResult.clear();
                    searchResult.add(r);
                    update();
                    searchResultInfo.setText("Community found.");
                }
            });
        });
        searchResultInfo.setText("Searching..");
    }
    
    private void showTop() {
        api.getCommunityTop((r) -> {
            SwingUtilities.invokeLater(() -> {
                searchResult.clear();
                searchResult.addAll(r);
                update();
                searchResultInfo.setText("Loaded current Top 100 (alphabetical)");
            });
        });
        searchResultInfo.setText("Loading..");
    }
    
    /**
     * Adds the currently selected games to the favorites.
     */
    private void addToFavorites() {
        for (Community game : list.getSelectedValuesList()) {
            if (!game.isValid()) {
                favorites.add(game);
            }
        }
        saveFavorites();
        update();
    }

    /**
     * Removes the currently selected games from the favorites.
     */
    private void removeFromFavorites() {
        for (Community c : list.getSelectedValuesList()) {
            favorites.remove(c);
        }
        saveFavorites();
        update();
    }
    
    /**
     * Removes all selected favorites and adds all selected non-favorites as
     * favorites.
     */
    private void toggleFavorite() {
        for (Community c : list.getSelectedValuesList()) {
            if (favorites.contains(c) || c.isValid()) {
                favorites.remove(c);
            } else {
                favorites.add(c);
            }
        }
        saveFavorites();
        update();
    }
    
    private void saveFavorites() {
        Map<String, String> favs = new HashMap<>();
        for (Community c : favorites) {
            favs.put(c.getId(), c.getCapitalizedName());
        }
        main.setCommunityFavorites(favs);
    }
    
    private void loadFavorites() {
        favorites.clear();
        
        Map<String, String> favs = main.getCommunityFavorites();
        for (String id : favs.keySet()) {
            Community c = new Community(id, favs.get(id));
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
        for (Community c : list.getSelectedValuesList()) {
            if (!c.isValid()) {
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
        Community selected = list.getSelectedValue();
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

            List<Community> listSelected = list.getSelectedValuesList();
            JPopupMenu menu = new JPopupMenu();
            menu.add(new AbstractAction("Replace All") {

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
                for (Community c : current) {
                    menu.add(new AbstractAction("Replace '" + c + "'") {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Community replaceWith = list.getSelectedValue();
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
            if (e.getSource() == input || e.getSource() == searchButton) {
                doSearch();
            }
            if (e.getSource() == top100) {
                showTop();
            }
            if (e.getSource() == ok) {
                useGameAndClose();
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
            if (e.getSource() == clearSearchButton) {
                searchResult.clear();
                update();
            }
            if (e.getSource() == openUrl) {
                if (selected != null && !selected.getName().isEmpty()) {
                    UrlOpener.openUrlPrompt(main,
                            Helper.buildUrlString("https", "twitch.tv", "/communities/"+selected.getName()));
                }
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
            return label;
        }
    }
    
}
