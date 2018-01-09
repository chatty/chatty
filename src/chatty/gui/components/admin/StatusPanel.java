
package chatty.gui.components.admin;

import chatty.gui.MainGui;
import static chatty.gui.components.admin.AdminDialog.SMALL_BUTTON_INSETS;
import static chatty.gui.components.admin.AdminDialog.hideableLabel;
import static chatty.gui.components.admin.AdminDialog.makeGbc;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.ChannelInfo;
import chatty.util.api.CommunitiesManager;
import chatty.util.api.CommunitiesManager.Community;
import chatty.util.api.TwitchApi;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author tduva
 */
public class StatusPanel extends JPanel {
    
    /**
     * Set to not updating the channel info after this time (re-enable buttons).
     * This is also done when channel info is received, so when it was set
     * successfully it will be set to not updating immediately. This basicially
     * is only for the case of error.
     */
    private static final int PUT_RESULT_DELAY = 5000;
    
    private final JTextArea status = new JTextArea();
    private final JTextField game = new JTextField(20);
    private final JTextArea community = new JTextArea();
    private final JButton update = new JButton(Language.getString("admin.button.update"));
    private final JLabel updated = new JLabel("No info loaded");
    private final JLabel putResult = new JLabel("...");
    private final JButton selectGame = new JButton(Language.getString("admin.button.selectGame"));
    private final JButton removeGame = new JButton(Language.getString("admin.button.removeGame"));
    private final JButton selectCommunity = new JButton(Language.getString("admin.button.selectCommunity"));
    private final JButton removeCommunity = new JButton(Language.getString("admin.button.removeCommunity"));
    private final JButton reloadButton = new JButton(Language.getString("admin.button.reload"));
    private final JButton historyButton = new JButton(Language.getString("admin.button.presets"));
    private final JButton addToHistoryButton = new JButton(Language.getString("admin.button.fav"));
    private final SelectGameDialog selectGameDialog;
    private final SelectCommunityDialog selectCommunityDialog;
    private final StatusHistoryDialog statusHistoryDialog;
    
    private final AdminDialog parent;
    private final MainGui main;
    private final TwitchApi api;
    
    private String currentChannel;
    private boolean statusEdited;
    private final List<Community> currentCommunities = new ArrayList<>();
    private long infoLastLoaded;
    
    private boolean loading;
    private boolean loadingStatus;
    private boolean loadingCommunity;
    private String statusLoadError;
    private String communityLoadError;
    private String statusPutResult;
    private String communityPutResult;
    private long lastPutResult = -1;
    
    public StatusPanel(AdminDialog parent, MainGui main, TwitchApi api) {
        
        this.parent = parent;
        this.main = main;
        this.api = api;
        
        selectGameDialog = new SelectGameDialog(main, api);
        selectCommunityDialog = new SelectCommunityDialog(main, api);
        statusHistoryDialog = new StatusHistoryDialog(parent, main.getStatusHistory());
        
        GridBagConstraints gbc;

        setLayout(new GridBagLayout());
        
        JPanel presetPanel = new JPanel();
        presetPanel.setLayout(new GridBagLayout());

        historyButton.setMargin(SMALL_BUTTON_INSETS);
        historyButton.setToolTipText("Open status presets containing favorites "
                + "and status history");
        historyButton.setMnemonic(KeyEvent.VK_P);
        gbc = makeGbc(2, 0, 1, 1);
        gbc.insets = new Insets(5, 5, 5, -1);
        gbc.anchor = GridBagConstraints.EAST;
        presetPanel.add(historyButton, gbc);
        
        addToHistoryButton.setMargin(SMALL_BUTTON_INSETS);
        addToHistoryButton.setToolTipText("Add current status to favorites");
        addToHistoryButton.setMnemonic(KeyEvent.VK_F);
        gbc = makeGbc(3, 0, 1, 1);
        gbc.insets = new Insets(5, 0, 5, 5);
        gbc.anchor = GridBagConstraints.EAST;
        presetPanel.add(addToHistoryButton, gbc);

        updated.setHorizontalAlignment(JLabel.CENTER);
        gbc = makeGbc(1,0,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(5, 10, 5, 5);
        presetPanel.add(updated, gbc);
        
        reloadButton.setMargin(SMALL_BUTTON_INSETS);
        reloadButton.setIcon(new ImageIcon(AdminDialog.class.getResource("view-refresh.png")));
        reloadButton.setMnemonic(KeyEvent.VK_R);
        gbc = makeGbc(0,0,1,1);
        gbc.anchor = GridBagConstraints.EAST;
        presetPanel.add(reloadButton, gbc);
        
        gbc = makeGbc(0, 1, 3, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0,0,0,0);
        add(presetPanel, gbc);
        
        
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setRows(2);
        status.setMargin(new Insets(2,3,3,2));
        status.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                statusEdited();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                statusEdited();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                statusEdited();
            }
        });
        gbc = makeGbc(0,2,3,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(new JScrollPane(status), gbc);
        
        game.setEditable(false);
        gbc = makeGbc(0,3,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(game, gbc);
        
        selectGame.setMargin(SMALL_BUTTON_INSETS);
        selectGame.setMnemonic(KeyEvent.VK_G);
        gbc = makeGbc(1,3,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(selectGame, gbc);

        removeGame.setMargin(SMALL_BUTTON_INSETS);
        gbc = makeGbc(2,3,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(removeGame,gbc);
        
        community.setEditable(false);
        community.setBackground(game.getBackground());
        community.setBorder(game.getBorder());
        community.setLineWrap(true);
        community.setWrapStyleWord(true);
        gbc = makeGbc(0,4,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(community, gbc);
        
        selectCommunity.setMargin(SMALL_BUTTON_INSETS);
        gbc = makeGbc(1,4,1,1);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(selectCommunity, gbc);
        
        removeCommunity.setMargin(SMALL_BUTTON_INSETS);
        gbc = makeGbc(2,4,1,1);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(removeCommunity, gbc);
        
        gbc = makeGbc(0,5,3,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        update.setMnemonic(KeyEvent.VK_U);
        add(update, gbc);
        
        gbc = makeGbc(0,6,3,1);
        add(putResult,gbc);
        
        ActionListener actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == update) {
                    if (currentChannel != null && !currentChannel.isEmpty()) {
                        loadingStatus = true;
                        loadingCommunity = true;
                        setLoading(true);
                        ChannelInfo info = new ChannelInfo(currentChannel, status.getText(), game.getText());
                        main.putChannelInfo(info);
                        putCommunity();
                        addCurrentToHistory();
                    }
                } else if (e.getSource() == reloadButton) {
                    getChannelInfo();
                } else if (e.getSource() == selectGame) {
                    selectGameDialog.setLocationRelativeTo(StatusPanel.this);
                    String result = selectGameDialog.open(game.getText());
                    if (result != null) {
                        game.setText(result);
                        statusEdited();
                    }
                } else if (e.getSource() == removeGame) {
                    game.setText("");
                    statusEdited();
                } else if (e.getSource() == selectCommunity) {
                    selectCommunityDialog.setLocationRelativeTo(StatusPanel.this);
                    List<CommunitiesManager.Community> result = selectCommunityDialog.open(currentCommunities);
                    if (result != null) {
                        setCommunities(result);
                        statusEdited();
                    }
                } else if (e.getSource() == removeCommunity) {
                    setCommunities(null);
                    statusEdited();
                } else if (e.getSource() == historyButton) {
                    StatusHistoryEntry result = statusHistoryDialog.showDialog(game.getText());
                    if (result != null) {
                        // A null value means that value shouldn't be used to
                        // change the info, it would be empty otherwise
                        if (result.title != null) {
                            status.setText(result.title);
                        }
                        if (result.game != null) {
                            game.setText(result.game);
                        }
                        if (result.communities != null) {
                            setCommunities(result.communities);
                        }
                    }
                } else if (e.getSource() == addToHistoryButton) {
                    addCurrentToFavorites();
                }
            }
        };
        
        reloadButton.addActionListener(actionListener);
        selectGame.addActionListener(actionListener);
        removeGame.addActionListener(actionListener);
        selectCommunity.addActionListener(actionListener);
        removeCommunity.addActionListener(actionListener);
        historyButton.addActionListener(actionListener);
        addToHistoryButton.addActionListener(actionListener);
        update.addActionListener(actionListener);
    }
    
    public void changeChannel(String channel) {
        currentChannel = channel;
        status.setText("");
        game.setText("");
        setCommunities(null);
        
        // This will reset last loaded anyway
        getChannelInfo();
    }
    
    private void setCommunities(List<Community> c) {
        currentCommunities.clear();
        if (c == null) {
            community.setText(null);
        } else {
            currentCommunities.addAll(c);
            community.setText(StringUtil.join(c, ", "));
        }
        parent.pack();
    }
    
    private void putCommunity() {
        final String channel = currentChannel;
        api.setCommunities(currentChannel, currentCommunities, error -> {
            if (currentChannel.equals(channel)) {
                if (error != null) {
                    communityPutResult = "Failed setting community.";
                } else {
                    communityPutResult = "Community updated.";
                }
                loadingCommunity = false;
                checkLoadingDone();
            }
        });
    }

    /**
     * Channel Info received, which happens when Channel Info is requested
     * or when a new status was successfully set.
     * 
     * @param stream Then stream the info is for
     * @param info The channel info
     */
    public void setChannelInfo(String stream, ChannelInfo info, TwitchApi.RequestResultCode result) {
        if (stream.equals(this.currentChannel)) {
            if (result == TwitchApi.RequestResultCode.SUCCESS) {
                status.setText(info.getStatus());
                game.setText(info.getGame());
            } else {
                infoLastLoaded = -1;
                if (result == TwitchApi.RequestResultCode.NOT_FOUND) {
                    statusLoadError = "Channel not found";
                } else {
                    statusLoadError = "";
                }
            }
            loadingStatus = false;
            checkLoadingDone();
        }
    }

    /**
     * Sets the result text of a attempted status update, but doesn't set it
     * back to "not loading" state, which is done when the channel info is
     * returned (which is also contained in the response for this action)
     * 
     * @param result 
     */
    public void setPutResult(TwitchApi.RequestResultCode result) {
        if (result == TwitchApi.RequestResultCode.SUCCESS) {
            statusPutResult = Language.getString("admin.infoUpdated");
        } else {
            if (result == TwitchApi.RequestResultCode.ACCESS_DENIED) {
                statusPutResult = "Changing info: Access denied";
                updated.setText("Error: Access denied");
            } else if (result == TwitchApi.RequestResultCode.FAILED) {
                statusPutResult = "Changing info: Unknown error";
                updated.setText("Error: Unknown error");
            } else if (result == TwitchApi.RequestResultCode.NOT_FOUND) {
                statusPutResult = "Changing info: Channel not found.";
                updated.setText("Error: Channel not found.");
            } else if (result == TwitchApi.RequestResultCode.INVALID_STREAM_STATUS) {
                statusPutResult = "Changing info: Invalid title/game (possibly bad language)";
                updated.setText("Error: Invalid title/game");
            }
        }
        lastPutResult = System.currentTimeMillis();
        loadingStatus = false;
        checkLoadingDone();
    }
    
    /**
     * Changes the text of the putResult label.
     * 
     * @param result 
     */
    protected void setPutResult(String result) {
        hideableLabel(putResult, result);
    }
    
    /**
     * Request Channel Info from the API.
     */
    private void getChannelInfo() {
        loadingStatus = true;
        loadingCommunity = true;
        statusLoadError = null;
        communityLoadError = null;
        
        setLoading(true);
        main.getChannelInfo(currentChannel);
        final String channel = currentChannel;
        api.getCommunitiesForChannel(currentChannel, (r, e) -> {
            if (currentChannel.equals(channel)) {
                if (r == null) {
                    communityLoadError = e == null ? "" : e;
                } else {
                    setCommunities(r);
                }
                loadingCommunity = false;
                checkLoadingDone();
            }
            if (r != null) {
                for (Community c : r) {
                    updateCommunityName(c);
                }
            }
        });
    }
    
    private void checkLoadingDone() {
        if (!loadingStatus && !loadingCommunity) {
            statusEdited = false;
            updated.setText(Language.getString("admin.infoLoaded.now"));
            if (statusPutResult != null || communityPutResult != null) {
                setPutResult(statusPutResult+" / "+communityPutResult);
                statusPutResult = null;
                communityPutResult = null;
            }
            if (statusLoadError != null || communityLoadError != null) {
                infoLastLoaded = -1;
                String error = getError(statusLoadError, "Status");
                error = StringUtil.append(error, ", ", getError(communityLoadError, "Community"));
                if (error.isEmpty()) {
                    error = "Unkonwn Error";
                }
                updated.setText("Loading failed: "+error);
                statusLoadError = null;
                communityLoadError = null;
            } else {
                infoLastLoaded = System.currentTimeMillis();
            }
            setLoading(false);
        }
    }
    
    private static String getError(String message, String type) {
        if (message != null) {
            if (!message.isEmpty()) {
                return message;
            }
        }
        return "";
    }
    
    /**
     * Set the dialog loading state, enabling or disabling controls.
     * 
     * @param loading 
     */
    private void setLoading(boolean loading) {
        if (loading) {
            updated.setText(Language.getString("admin.loading"));
            lastPutResult = -1;
        }
        update.setEnabled(!loading);
        selectGame.setEnabled(!loading);
        removeGame.setEnabled(!loading);
        selectCommunity.setEnabled(!loading);
        removeCommunity.setEnabled(!loading);
        reloadButton.setEnabled(!loading);
        historyButton.setEnabled(!loading);
        addToHistoryButton.setEnabled(!loading);
        this.loading = loading;
    }
    
    public void update() {
        if (!loading && infoLastLoaded > 0) {
            long timePassed = System.currentTimeMillis() - infoLastLoaded;
            if (statusEdited) {
                updated.setText(Language.getString("admin.infoLoaded.edited", DateTime.duration(timePassed, 1, 0)));
            } else {
                updated.setText(Language.getString("admin.infoLoaded", DateTime.duration(timePassed, 1, 0)));
            }
        }
        if (loading && lastPutResult > 0) {
            long ago = System.currentTimeMillis() - lastPutResult;
            if (ago > PUT_RESULT_DELAY) {
                setLoading(false);
            }
        }
    }
    
    /**
     * This should be done from an up-to-date source, like a direct response
     * from the API. It might not be necessary, but just in case a Community
     * gets renamed at some point, at least this way it's possible to change it
     * for existing status/favorite entries.
     * 
     * @param c 
     */
    protected void updateCommunityName(Community c) {
        if (c == null) {
            return;
        }
        main.getStatusHistory().updateCommunityName(c);
        Map<String, String> communities = main.getCommunityFavorites();
        if (communities.containsKey(c.getId())) {
            communities.put(c.getId(), c.getCapitalizedName());
            main.setCommunityFavorites(communities);
        }
    }
    
    private void statusEdited() {
        statusEdited = true;
    }

    public String getStatusHistorySorting() {
        return statusHistoryDialog.getSortOrder();
    }
    
    public void setStatusHistorySorting(String order) {
        statusHistoryDialog.setSortOrder(order);
    }
    
    /**
     * Adds the current status to the preset history
     */
    private void addCurrentToHistory() {
        String currentTitle = status.getText().trim();
        String currentGame = game.getText();
        if (main.getSaveStatusHistorySetting()
                || main.getStatusHistory().isFavorite(currentTitle, currentGame, currentCommunities)) {
            main.getStatusHistory().addUsed(currentTitle, currentGame, currentCommunities);
        }
    }
    
    /**
     * Adds the current status to the preset favorites
     */
    private void addCurrentToFavorites() {
        main.getStatusHistory().addFavorite(status.getText().trim(), game.getText(), currentCommunities);
    }
    
}
