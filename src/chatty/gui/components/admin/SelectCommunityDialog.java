
package chatty.gui.components.admin;

import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.UrlOpener;
import chatty.util.api.CommunitiesManager.Community;
import chatty.util.api.TwitchApi;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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

    private final MainGui main;
    private final TwitchApi api;

    // General Buttons
    private final JButton ok = new JButton("Ok");
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
    
    // Current games data seperate from GUI
    private final Set<Community> favorites = new TreeSet<>();
    private final Set<Community> searchResult = new TreeSet<>();
    
    private Community current;
    private final Timer timer;
    private long lastSelectionTime;
    private boolean loading;
    private Community shouldMaybeRequest;
    
    // Whether to use the current game
    private boolean save;
    
    public SelectCommunityDialog(MainGui main, TwitchApi api) {
        super(main, "Select community", true);
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
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "toggleFavorite");
        list.getActionMap().put("toggleFavorite", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFavorite();
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
        gbc.weightx = 0.1;
        gbc.weighty = 1;
        add(new JScrollPane(list), gbc);
        
        gbc = makeGbc(2, 4, 3, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.9;
        gbc.weighty = 1;
        description.setEditable(false);
        description.setContentType("text/html");
        add(new JScrollPane(description), gbc);
 
        gbc = makeGbc(0,5,1,1);
        addToFavoritesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(addToFavoritesButton, gbc);
        
        removeFromFavoritesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        gbc = makeGbc(1,5,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(removeFromFavoritesButton, gbc);
        
        ok.setMnemonic(KeyEvent.VK_ENTER);
        gbc = makeGbc(0,6,3,1);
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(ok, gbc);
        
        cancel.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(3,6,2,1);
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
        input.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateOkButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateOkButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateOkButton();
            }
        });
        
        description.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String url = e.getURL().toString();
                UrlOpener.openUrlPrompt(SelectCommunityDialog.this, url, true);
            }
        });
        
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
    public Community open(Community preset) {
        timer.start();
        setCurrent(preset);
        loadFavorites();
        if (preset != Community.EMPTY && !favorites.contains(preset)) {
            searchResult.add(preset);
        }
        update();
        save = false;
        setVisible(true);

        // Blocking dialog, so stuff can change in the meantime
        timer.stop();
        if (save) {
            return current != null ? current : Community.EMPTY;
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
    
    private void setCurrent(Community c) {
        if (c == null) {
            current = null;
            input.setText(null);
        } else {
            current = c;
            input.setText(c.toString());
        }
        updateInfo();
    }
    
    private void updateInfo() {
        if (current == null || current.isValid()) {
            description.setText("Nothing to see here.");
            return;
        }
        
        Community maybe = api.getCachedCommunityInfo(current.getId());
        if (maybe != null) {
            description.setText(String.format(
                    "<html><body style='font: sans-serif 9pt;padding:3px;'>%s<h2 style='font-size:12pt;border-bottom: 1px solid black'>Rules</h2>%s",
                    maybe.getSummary(),
                    maybe.getRules()));
            description.setCaretPosition(0);
        } else {
            description.setText("Loading..");
            lastSelectionTime = System.currentTimeMillis();
            shouldMaybeRequest = current;
        }
    }
    
    private void loadCurrentInfo() {
        if (!loading && current != null && current == shouldMaybeRequest) {
            final Community forRequest = current;
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
        for (Community game : searchResult) {
            listData.addElement(game);
        }
        if (!searchResult.isEmpty() && !favorites.isEmpty()) {
            listData.addElement(Community.EMPTY);
        }
        for (Community c : favorites) {
            listData.addElement(c);
        }
        searchResultInfo.setText("Search: "+searchResult.size()+" / "
            +"Favorites: "+favorites.size()+"");
        list.setSelectedValue(current, false);
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
                    setCurrent(r);
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
            favs.put(c.getId(), c.getName());
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
        boolean enabled = current != null && input.getText().equals(current.getName());
        ok.setEnabled(enabled);
        openUrl.setEnabled(enabled);
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
            setCurrent(selected);
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
                if (current != null && !current.getName().isEmpty()) {
                    UrlOpener.openUrlPrompt(main, "https://www.twitch.tv/communities/"+current.getName());
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
                useGameAndClose();
            }
        }
    }
    
    private class DoneAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            useGameAndClose();
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
