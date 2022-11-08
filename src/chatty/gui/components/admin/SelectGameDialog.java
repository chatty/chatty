
package chatty.gui.components.admin;

import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.lang.Language;
import chatty.util.StringUtil;
import chatty.util.api.ResultManager;
import chatty.util.api.StreamCategory;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
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
public class SelectGameDialog extends JDialog {
    
    private static final Logger LOGGER = Logger.getLogger(SelectGameDialog.class.getName());

    private static final String INFO = "<html><body style='width:280px'>"
            +Language.getString("admin.game.info");
    
    private static String s(String key) {
        return Language.getString("admin.game."+key);
    }
    
    private static final StreamCategory SEPARATOR = new StreamCategory("", "");

    private final MainGui main;
    private final TwitchApi api;

    // General Buttons
    private final JButton ok = new JButton(Language.getString("dialog.button.ok"));
    private final JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
    
    // Game search/fav buttons
    private final JButton searchButton = new JButton(s("button.search"));
    private final JButton addToFavoritesButton = new JButton(s("button.favorite"));
    private final JButton removeFromFavoritesButton = new JButton(s("button.unfavorite"));
    private final JButton clearSearchButton = new JButton(s("button.clearSearch"));

    // Current info elements
    private final JLabel searchResultInfo = new JLabel();
    private final JTextField gameInput = new JTextField(30);
    private final JList<StreamCategory> list = new JList<>();
    private final DefaultListModel<StreamCategory> listData = new DefaultListModel<>();
    
    // Current games data separate from GUI
    private final Set<StreamCategory> favorites = new TreeSet<>();
    private final Set<StreamCategory> searchResult = new TreeSet<>();
    
    private StreamCategory presetCategory;
    
    // Whether to use the current game
    private boolean save;
    
    public SelectGameDialog(MainGui main, TwitchApi api) {
        super(main, "Select game", true);
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
        
        gbc = makeGbc(0,0,4,1);
        add(new JLabel(INFO), gbc);
        
        gbc = makeGbc(0,1,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(gameInput, gbc);
        
        GuiUtil.smallButtonInsets(searchButton);
        gbc = makeGbc(2,1,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(searchButton, gbc);
        
        gbc = makeGbc(0,2,3,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2,4,4,4);
        add(searchResultInfo, gbc);
        
        gbc = makeGbc(2,2,1,1);
        GuiUtil.smallButtonInsets(clearSearchButton);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2,4,4,4);
        add(clearSearchButton, gbc);
        
        gbc = makeGbc(0,3,4,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(new JScrollPane(list), gbc);
 
        GuiUtil.smallButtonInsets(addToFavoritesButton);
        gbc = makeGbc(0,4,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(addToFavoritesButton, gbc);
        
        GuiUtil.smallButtonInsets(removeFromFavoritesButton);
        gbc = makeGbc(1,4,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(removeFromFavoritesButton, gbc);
        
        ok.setMnemonic(KeyEvent.VK_ENTER);
        gbc = makeGbc(0,6,2,1);
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(ok, gbc);
        
        cancel.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(2,6,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(cancel, gbc);

        ActionListener actionListener = new MyActionListener();
        searchButton.addActionListener(actionListener);
        gameInput.addActionListener(actionListener);
        ok.addActionListener(actionListener);
        cancel.addActionListener(actionListener);
        list.addListSelectionListener(new MyListSelectionListener());
        list.addMouseListener(new ListClickListener());
        addToFavoritesButton.addActionListener(actionListener);
        removeFromFavoritesButton.addActionListener(actionListener);
        clearSearchButton.addActionListener(actionListener);
        
        gameInput.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButtons();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButtons();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButtons();
            }
        });
        
        updateButtons();
        
        pack();
        
        setMinimumSize(getSize());
        
        api.subscribe(ResultManager.Type.CATEGORY_RESULT, (ResultManager.CategoryResult) categories -> {
            SwingUtilities.invokeLater(() -> {
                // Load favorites first, since this may be triggered while the
                // dialog has not been opened yet
                loadFavorites();
                // Needs to save old an new, in case what equals() matches is different
                Map<StreamCategory, StreamCategory> replace = new HashMap<>();
                for (StreamCategory currentCategory : favorites) {
                    for (StreamCategory updatedCategory : categories) {
                        if (!currentCategory.hasId()) {
                            if (currentCategory.nameMatches(updatedCategory)) {
                                // Has no id yet, but same name -> add id
                                replace.put(currentCategory, updatedCategory);
                            }
                        }
                        else if (currentCategory.id.equals(updatedCategory.id)) {
                            if (!currentCategory.name.equals(updatedCategory.name)) {
                                // Has id, but different name -> update name
                                replace.put(currentCategory, updatedCategory);
                            }
                        }
                    }
                }
                for (Map.Entry<StreamCategory, StreamCategory> category : replace.entrySet()) {
                    LOGGER.info(String.format("Game favorites: Updating %s to %s",
                            category.getKey().toStringVerbose(),
                            category.getValue().toStringVerbose()));
                    favorites.remove(category.getKey());
                    favorites.add(category.getValue());
                }
                if (!replace.isEmpty()) {
                    saveFavorites();
                    update();
                }
            });
        });
    }
    
    /**
     * Open the dialog with the given game preset.
     * 
     * @param gamePreset The name of the game to use as preset, can also be
     * {@code null} or empty if it should be left blank
     * @return The name of the game to use, or {@code null} if the game should
     * not be changed
     */
    public StreamCategory open(StreamCategory gamePreset) {
        presetCategory = gamePreset;
        loadFavorites();
        list.setSelectedValue(gamePreset, true);
        save = false;
        setVisible(true);

        // Blocking dialog, so stuff can change in the meantime
        if (save) {
            return list.getSelectedValue();
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
    
    /**
     * Clear the list and fill it with the current search result and favorites.
     * Also update the status text.
     */
    private void update() {
        listData.clear();
        if (presetCategory != null && !presetCategory.isEmpty() && !favorites.contains(presetCategory)) {
            searchResult.add(presetCategory);
        }
        for (StreamCategory game : searchResult) {
            listData.addElement(game);
        }
        if (!searchResult.isEmpty() && !favorites.isEmpty()) {
            listData.addElement(SEPARATOR);
        }
        for (StreamCategory game : favorites) {
            listData.addElement(game);
        }
        searchResultInfo.setText(Language.getString("admin.game.listInfo",
                searchResult.size(), favorites.size()));
        list.setSelectedValue(gameInput.getText(), false);
    }
    
    private void doSearch() {
        String searchString = gameInput.getText().trim();
        if (searchString.isEmpty()) {
            return;
        }
        api.performGameSearch(searchString, r -> {
            SwingUtilities.invokeLater(() -> {
                searchResult.clear();
                searchResult.addAll(r);
                update();
            });
        });
        searchResultInfo.setText(s("searching"));
    }
    
    /**
     * Adds the currently selected games to the favorites.
     */
    private void addToFavorites() {
        for (StreamCategory game : list.getSelectedValuesList()) {
            if (!game.equals(SEPARATOR)) {
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
        for (StreamCategory game : list.getSelectedValuesList()) {
            favorites.remove(game);
        }
        saveFavorites();
        update();
    }
    
    /**
     * Removes all selected favorites and adds all selected non-favorites as
     * favorites.
     */
    private void toggleFavorite() {
        for (StreamCategory game : list.getSelectedValuesList()) {
            if (favorites.contains(game) || game.equals(SEPARATOR)) {
                favorites.remove(game);
            } else {
                favorites.add(game);
            }
        }
        saveFavorites();
        update();
    }
    
    public static Collection<List> favoritesToSettings(Collection<StreamCategory> favorites) {
        List<List> result = new ArrayList<>();
        for (StreamCategory category : favorites) {
            if (!StringUtil.isNullOrEmpty(category.name)) {
                List item = new ArrayList<>();
                item.add(category.id);
                item.add(category.name);
                result.add(item);
            }
        }
        return result;
    }
    
    public static Collection<String> favoritesToSettingsOld(Collection<StreamCategory> favorites) {
        List<String> result = new ArrayList<>();
        for (StreamCategory category : favorites) {
            if (!StringUtil.isNullOrEmpty(category.name)) {
                result.add(category.name);
            }
        }
        return result;
    }
    
    public static Collection<StreamCategory> settingsToFavorites(List<List> favorites) {
        List<StreamCategory> result = new ArrayList<>();
        for (List item : favorites) {
            if (item != null && item.size() == 2) {
                String id = (String) item.get(0);
                String name = (String) item.get(1);
                result.add(new StreamCategory(id, name));
            }
        }
        return result;
    }
    
    public static Collection<StreamCategory> settingsToFavoritesOld(List<String> favorites) {
        List<StreamCategory> result = new ArrayList<>();
        for (String item : favorites) {
            result.add(new StreamCategory(null, item));
        }
        return result;
    }
    
    private void saveFavorites() {
        main.setGameFavorites(favorites);
    }
    
    private void loadFavorites() {
        favorites.clear();
        favorites.addAll(main.getGameFavorites());
        update();
    }
    
    /**
     * Sets the state of the favorites buttons depending on the current
     * selection.
     */
    private void updateButtons() {
        boolean favoriteSelected = false;
        boolean nonFavoriteSelected = false;
        for (StreamCategory game : list.getSelectedValuesList()) {
            if (!game.equals(SEPARATOR)) {
                if (favorites.contains(game)) {
                    favoriteSelected = true;
                } else {
                    nonFavoriteSelected = true;
                }
            }
        }
        addToFavoritesButton.setEnabled(nonFavoriteSelected);
        removeFromFavoritesButton.setEnabled(favoriteSelected);
        searchButton.setEnabled(!gameInput.getText().isEmpty());
        ok.setEnabled(list.getSelectedValue() != null && list.getSelectedValue() != SEPARATOR);
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
    
    private class MyActionListener implements ActionListener {
    
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == gameInput || e.getSource() == searchButton) {
                doSearch();
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
        }
    }
    
    private class MyListSelectionListener implements ListSelectionListener {
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateButtons();
        }
    }

    /**
     * Use game by double-click.
     */
    private class ListClickListener extends MouseAdapter {
        
        @Override
        public void mouseClicked(MouseEvent e) {
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
            if (value.equals(SEPARATOR)) {
                label.setText(null);
                label.setBorder(seperatorBorder);
            }
            if (favorites.contains((StreamCategory) value)) {
                label.setIcon(icon);
            }
            return label;
        }
    }
    
}
