
package chatty.gui.components;

import chatty.Chatty;
import chatty.StatusHistoryEntry;
import chatty.gui.MainGui;
import chatty.gui.components.settings.DurationSetting;
import chatty.util.DateTime;
import chatty.util.api.ChannelInfo;
import chatty.util.api.TwitchApi;
import chatty.util.api.TwitchApi.RequestResultCode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Dialog to change stream info and run commercials.
 * 
 * @author tduva
 */
public class AdminDialog extends JDialog {
    
    private final static String EDITOR_TEXT = "[help-admin:status Help]";
    private final static String EDITOR_TEXT_NO_ACCESS
            = "No Editor Access available. [help-admin:top More information..]";
    private final static String COMMERCIALS_TEXT = "[help-admin:commercials Help]";
    private final static String COMMERCIALS_TEXT_NO_ACCESS
            = "No Commercial Access available. [help-admin:top More information..]";
    
    // Colors for hideable labels
    private static final Color LABEL_INVISIBLE = new Color(0, 0, 0, 0);
    private static final Color LABEL_VISIBLE = new Color(120, 150, 150);
    
    // Insets for smaller kind of buttons
    public static final Insets SMALL_BUTTON_INSETS = new Insets(-1,15,-1,15);
    // How often to call update() which updates times and runs commercials.
    private static final int UPDATE_DELAY = 4000;
    
    /**
     * Set to not updating the channel info after this time (re-enable buttons).
     * This is also done when channel info is received, so when it was set
     * successfully it will be set to not updating immediately. This basicially
     * is only for the case of error.
     */
    private static final int PUT_RESULT_DELAY = 5000;
    
    /**
     * After a commercial was attempted to run/the result was returned wait for
     * this long before displaying new data of a scheduled commercial.
     */
    private static final int COMMERCIAL_RUN_ATTEMPT_DELAY = 5*1000;
    
    private final MainGui main;
    
    private final ActionListener actionListener = new MyActionListener();
    
    // Status Tab
    private final JTextArea status = new JTextArea();
    private final JTextField game = new JTextField(20);
    private final JButton update = new JButton("Update");
    private final JLabel updated = new JLabel("No info loaded");
    private final JLabel putResult = new JLabel("...");
    private final JButton selectGame = new JButton("Select game");
    private final JButton removeGame = new JButton("Remove game");
    private final JButton reloadButton = new JButton("reload");
    private final JButton historyButton = new JButton("Presets");
    private final JButton addToHistoryButton = new JButton("Fav");
    private final SelectGameDialog selectGameDialog;
    private final StatusHistoryDialog statusHistoryDialog;

    // Commercials Tab
    private final static int[] commercialButtonsDef = {30,60,90,120,180};
    private final Map<Integer, JToggleButton> commercialButtons = new LinkedHashMap<>();
    private final JLabel commercialResult;
    private final JLabel lastCommercialInfo = new JLabel("");
    private final JCheckBox useCommercialDelay = new JCheckBox("Use delay: ");
    private final JCheckBox repeatCommercial = new JCheckBox("Repeat");
    private final DurationSetting commercialDelay = new DurationSetting(3, true);

    // Shared
    private final JTabbedPane tabs;
    private final JButton close = new JButton("Close");
    private final LinkLabel infoText;
    
    // Current state/settings (currentChannel specific)
    private String currentChannel;
    private long infoLastLoaded;
    private boolean statusEdited;
    private boolean loading;
    private long lastPutResult = -1;
    private long lastCommercialRun;
    private long scheduledCommercialTime;
    private int scheduledCommercialLength;
    private long lastCommercialRunAttempt;
    
    // Saves last commercial run time for several channels (although it's
    // probably unncessary, at least this way another currentChannel can be opened in
    // the dialog and it's still saved)
    private final Map<String, Long> lastCommercial = new HashMap<>();
    
    // Current access (not currentChannel specific)
    private boolean commercialAccess;
    private boolean editorAccess;

    public AdminDialog(MainGui main) {
        super(main);
        setTitle("Channel Admin - No Channel");
        this.main = main;
        setResizable(false);
        addWindowListener(new WindowClosingListener());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // Special objects
        infoText = new LinkLabel("Test",main.getLinkLabelListener());
        selectGameDialog = new SelectGameDialog(main);
        
        statusHistoryDialog = new StatusHistoryDialog(this, main.getStatusHistory());
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;

        
        // Status Panel
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new GridBagLayout());
        
        
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
        statusPanel.add(presetPanel, gbc);
        
        
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
        statusPanel.add(new JScrollPane(status), gbc);
        
        game.setEditable(false);
        gbc = makeGbc(0,3,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        statusPanel.add(game, gbc);
        
        selectGame.setMargin(SMALL_BUTTON_INSETS);
        selectGame.setMnemonic(KeyEvent.VK_G);
        gbc = makeGbc(1,3,1,1);
        statusPanel.add(selectGame, gbc);

        removeGame.setMargin(SMALL_BUTTON_INSETS);
        gbc = makeGbc(2,3,1,1);
        statusPanel.add(removeGame,gbc);
        
        gbc = makeGbc(0,4,3,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        update.setMnemonic(KeyEvent.VK_U);
        statusPanel.add(update, gbc);
        
        gbc = makeGbc(0,5,3,1);
        statusPanel.add(putResult,gbc);
        
        
        // Commercials Panel
        JPanel commercialPanel = new JPanel();
        commercialPanel.setLayout(new GridBagLayout());
        
        gbc = makeGbc(0,1,1,1);
        commercialPanel.add(new JLabel("Run commercial: "), gbc);
        gbc = makeGbc(1,1,4,1);
        gbc.insets = new Insets(5,5,0,5);
        commercialPanel.add(createCommercialButtons(), gbc);
        
        gbc = makeGbc(1,2,1,1);
        gbc.insets = new Insets(0,5,5,5);
        commercialPanel.add(useCommercialDelay, gbc);
        gbc = makeGbc(2,2,1,1);
        gbc.insets = new Insets(0,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        commercialPanel.add(commercialDelay, gbc);
        
        gbc = makeGbc(3,2,1,1);
        gbc.insets = new Insets(0,5,5,5);
        commercialPanel.add(repeatCommercial, gbc);
        
        gbc = makeGbc(4,2,1,1);
        gbc.insets = new Insets(0,10,5,5);
        commercialPanel.add(lastCommercialInfo, gbc);

        commercialResult = new JLabel("...");
        gbc = makeGbc(0,3,5,1);
        gbc.insets = new Insets(3,5,15,5);
        commercialPanel.add(commercialResult, gbc);
        
        // Add to tab pane
        tabs = new JTabbedPane();
        tabs.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                updateInfoText();
            }
        });
        tabs.addTab("Status",statusPanel);
        tabs.addTab("Commercial",commercialPanel);
        gbc = makeGbc(0,0,2,1);
        gbc.insets = new Insets(0,0,0,0);
        add(tabs, gbc);
        
        
        gbc = makeGbc(0,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(infoText, gbc);
        
        gbc = makeGbc(1,1,1,1);
        //gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        close.setMnemonic(KeyEvent.VK_C);
        add(close,gbc);
        
        update.addActionListener(actionListener);
        close.addActionListener(actionListener);
        selectGame.addActionListener(actionListener);
        reloadButton.addActionListener(actionListener);
        removeGame.addActionListener(actionListener);
        historyButton.addActionListener(actionListener);
        addToHistoryButton.addActionListener(actionListener);
        
        setCommercialResult("");
        
        finishDialog();
        
        startUpdateTimer();
    }
    
    /**
     * Starts the timer that updates the dialog on the set delay (texts, running
     * commercials)
     */
    private void startUpdateTimer() {
        Timer timer = new Timer(UPDATE_DELAY, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });
        timer.start();
    }
    
    private void finishDialog() {
        pack();
        //setMinimumSize(getSize());
    }
    
    /**
     * Updates times and runs scheduled commercials.
     */
    private void update() {
        if (isVisible()) {
            if (!loading && infoLastLoaded > 0) {
                long timePassed = System.currentTimeMillis() - infoLastLoaded;
                updated.setText("Info last loaded: "
                        +DateTime.duration(timePassed, 1, 0)+" ago"
                        +(statusEdited ? " (edited)" : ""));
            }
            if (lastCommercialRun > 0) {
                long ago = System.currentTimeMillis() - lastCommercialRun;
                lastCommercialInfo.setText("Last run: "+DateTime.duration(ago, 1, 0)+" ago");
                lastCommercialInfo.setToolTipText("Last run: "+DateTime.formatFullDatetime(lastCommercialRun));
            }
            if (loading && lastPutResult > 0) {
                long ago = System.currentTimeMillis() - lastPutResult;
                if (ago > PUT_RESULT_DELAY) {
                    setLoading(false);
                }
            }
            // In case the text is going to be too big, breaking the dialog
            finishDialog();
        }
        if (scheduledCommercialTime > 0) {
            long timeLeft = scheduledCommercialTime - System.currentTimeMillis();
            if (timeLeft <= 0) {
                runCommercialNow(scheduledCommercialLength);
            } else if (System.currentTimeMillis() - lastCommercialRunAttempt
                    > COMMERCIAL_RUN_ATTEMPT_DELAY) {
                setCommercialResult(
                        String.format("Commercial (%ds) scheduled to run in %s",
                                scheduledCommercialLength,
                                DateTime.duration(timeLeft, 2, 0))
                );
            }
        }
    }
    
    /**
     * Update the info text based on the current tab and access.
     */
    private void updateInfoText() {
        if (tabs.getSelectedIndex() == 0) {
            if (editorAccess) {
                infoText.setText(EDITOR_TEXT);
            } else {
                infoText.setText(EDITOR_TEXT_NO_ACCESS);
            }
        } else {
            if (commercialAccess) {
                infoText.setText(COMMERCIALS_TEXT);
            } else {
                infoText.setText(COMMERCIALS_TEXT_NO_ACCESS);
            }
        }
    }
    
    /**
     * Set the kind of access the current token grants. To be able to show
     * appropriate info texts.
     * 
     * @param editor
     * @param commercials 
     */
    public void updateAccess(boolean editor, boolean commercials) {
        this.editorAccess = editor;
        this.commercialAccess = commercials;
        updateInfoText();
    }
    
    private void statusEdited() {
        statusEdited = true;
    }
    
    /**
     * Opens the dialog with the given currentChannel, positioning it correctly.
     * 
     * @param channel 
     */
    public void open(String channel) {
        if (channel == null || channel.isEmpty()) {
            JOptionPane.showMessageDialog(main, "No channel specified. Can't"
                    + " open admin dialog.");
            if (!Chatty.DEBUG) {
                return;
            }
        }
        setPutResult("");
        setChannel(channel);
        
        setVisible(true);
    }
    
    public void commercialHotkey(int length) {
        if (commercialButtons.containsKey(length)) {
            commercialButtons.get(length).doClick();
        } else {
            commercialButtons.get(30).doClick();
        }
    }
    
    public boolean isCommercialsTabVisible() {
        return isVisible() && tabs.getSelectedIndex() == 1;
    }
    
    /**
     * Sets the currentChannel, performing some actions only if it was changed.
     * Asks the user if it should be changed if a commercial is scheduled.
     * 
     * @param channel 
     */
    private void setChannel(String channel) {
        if (channel != null && !channel.equals(currentChannel)) {
            if (scheduledCommercialTime == 0 || confirmChannelChange(channel)) {
                changeChannel(channel);
            }
        }
        if (channel != null && !channel.isEmpty()) {
            //update.setEnabled(true);
        }
        setTitle("Channel Admin - "+currentChannel);
    }
    
    /**
     * Dialog to confirm whether the currentChannel should be switched when a
     * commercial is currently scheduled on currentChannel.
     * 
     * @param channel
     * @return 
     */
    private boolean confirmChannelChange(String channel) {
        String message = "<html><body style='width:240'>"
                + "There is currently a commercial scheduled on '"+currentChannel
                +"'. Changing channel to '"+channel+"' will cancel that.";
        String[] options = new String[]{"Change channel", "Don't change"};
        int result = JOptionPane.showOptionDialog(main, message, 
                "Changing channel will cancel commercial", JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE, null,
                options, options[1]);
        return result == 0;
    }
    
    /**
     * Change the currentChannel this dialog is about, so some loading/resetting
     * of currentChannel specific stuff is required.
     *
     * @param channel
     */
    private void changeChannel(String channel) {
        status.setText("");
        game.setText("");
        this.currentChannel = channel;
        
        // Load/reset last commercial run time
        Long lastCommercialTemp = lastCommercial.get(channel);
        if (lastCommercialTemp != null) {
            lastCommercialRun = lastCommercialTemp;
        } else {
            lastCommercialRun = 0;
            lastCommercialInfo.setText(null);
        }
        
        // Clear scheduled commercial
        clearScheduledCommercial();
        
        // This will reset last loaded anyway
        getChannelInfo();
        
        // Reset result
        setCommercialResult("");
        
        update();
    }
    
    /**
     * Set the dialog loading state, enabling or disabling controls.
     * 
     * @param loading 
     */
    private void setLoading(boolean loading) {
        if (loading) {
            updated.setText("Loading..");
            lastPutResult = -1;
        }
        update.setEnabled(!loading);
        selectGame.setEnabled(!loading);
        removeGame.setEnabled(!loading);
        reloadButton.setEnabled(!loading);
        historyButton.setEnabled(!loading);
        addToHistoryButton.setEnabled(!loading);
        this.loading = loading;
    }
    
    /**
     * Channel Info received, which happens when Channel Info is requested
     * or when a new status was successfully set.
     * 
     * @param stream Then stream the info is for
     * @param info The channel info
     */
    public void setChannelInfo(String stream, ChannelInfo info, RequestResultCode result) {
        if (stream.equals(this.currentChannel)) {
            if (result == TwitchApi.RequestResultCode.SUCCESS) {
                status.setText(info.getStatus());
                game.setText(info.getGame());
                updated.setText("Info last loaded: just now");
                infoLastLoaded = info.time;
                statusEdited = false;
            } else {
                infoLastLoaded = -1;
                if (result == TwitchApi.RequestResultCode.NOT_FOUND) {
                    updated.setText("Error loading info: Channel not found.");
                } else {
                    updated.setText("Error loading info.");
                }
            }
        }
        setLoading(false);
        //finishDialog();
    }
    
    /**
     * Sets the result text of a attempted status update, but doesn't set it
     * back to "not loading" state, which is done when the channel info is
     * returned (which is also contained in the response for this action)
     * 
     * @param result 
     */
    public void setPutResult(RequestResultCode result) {
        if (result == TwitchApi.RequestResultCode.SUCCESS) {
            setPutResult("Info successfully updated.");
        } else {
            if (result == TwitchApi.RequestResultCode.ACCESS_DENIED) {
                setPutResult("Changing info failed: Access denied");
                updated.setText("Error: Access denied");
            } else if (result == TwitchApi.RequestResultCode.FAILED) {
                setPutResult("Changing info failed: Unknown error");
                updated.setText("Error: Unknown error");
            } else if (result == TwitchApi.RequestResultCode.NOT_FOUND) {
                setPutResult("Changing info failed: Channel not found.");
                updated.setText("Error: Channel not found.");
            } else if (result == TwitchApi.RequestResultCode.INVALID_STREAM_STATUS) {
                setPutResult("Changing info failed: Invalid title/game (possibly bad language)");
                updated.setText("Error: Invalid title/game");
            }
        }
        lastPutResult = System.currentTimeMillis();
    }
    
    /**
     * Changes the text of the putResult label.
     * 
     * @param result 
     */
    private void setPutResult(String result) {
        hideableLabel(putResult, result);
    }
    
    /**
     * Updates the selectGameDialog with the search result returned from the
     * API.
     *
     * @param games
     */
    public void gameSearchResult(Set<String> games) {
        selectGameDialog.setSearchResult(games);
    }

    /**
     * Request Channel Info from the API.
     */
    private void getChannelInfo() {
        main.getChannelInfo(currentChannel);
        setLoading(true);
    }
    
    /**
     * Save commercial settings.
     */
    private void saveSettings() {
        boolean enabled = useCommercialDelay.isSelected();
        long length = commercialDelay.getSettingValue();
        main.saveCommercialDelaySettings(enabled, length);
    }
    
    /**
     * Creates buttons for the defined commercial lengths, adds them to a JPanel,
     * a ButtonGroup and a Map for further reference.
     * 
     * @return The JPanel to be added to the GUI.
     */
    private JPanel createCommercialButtons() {
        ButtonGroup g = new NoneSelectedButtonGroup();
        JPanel panel = new JPanel();
        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                commercialButtonClicked(e.getActionCommand());
            }
        };
        for (int i : commercialButtonsDef) {
            JToggleButton b = new JToggleButton(i+"s");
            b.setActionCommand(String.valueOf(i));
            g.add(b);
            commercialButtons.put(i, b);
            panel.add(b);
            b.addActionListener(listener);
        }
        return panel;
    }
    
    /**
     * One of the commercial buttons was clicked. Find out which one by the
     * action command and check if it was selected/unselected.
     * 
     * @param command 
     */
    private void commercialButtonClicked(String command) {
        int length = Integer.parseInt(command);
        JToggleButton button = commercialButtons.get(length);
        if (button.isSelected()) {
            // If commercial is already scheduled, just change the length.
            if (scheduledCommercialTime == 0) {
                runCommercial(length);
            } else {
                scheduledCommercialLength = length;
                update();
            }
        } else {
            scheduledCommercialTime = 0;
            setCommercialResult("Canceled scheduled commercial.");
        }
    }
    
    /**
     * Unselect all run commercial buttons.
     */
    private void clearCommercialButtonsSelection() {
        for (JToggleButton b : commercialButtons.values()) {
            b.setSelected(false);
        }
    }
    
    /**
     * A run commercial button was pressed. Either play commercial immediately
     * or schedule it for the given time if that is enabled.
     * 
     * @param length 
     */
    private void runCommercial(int length) {
        saveSettings();
        if (useCommercialDelay.isSelected()) {
            scheduleCommercial(length);
        } else {
            runCommercialNow(length);
        }
    }
    
    /**
     * Schedules a commercial of the given length to be run after the delay
     * currently set in the delay input field.
     * 
     * @param length 
     */
    private void scheduleCommercial(int length) {
        Long delay = commercialDelay.getSettingValue();
        if (delay == null) {
            setCommercialResult("Invalid delay specified.");
            clearCommercialButtonsSelection();
        } else {
            scheduledCommercialTime = System.currentTimeMillis() + delay * 1000;
            scheduledCommercialLength = length;
            update();
        }
    }
    
    /**
     * Immediately run commercial of the given length.
     * 
     * @param length 
     */
    private void runCommercialNow(int length) {
        lastCommercialRunAttempt = System.currentTimeMillis();
        if (repeatCommercial.isSelected()) {
            scheduleCommercial(length);
        } else {
            clearScheduledCommercial();
        }
        main.runCommercial(currentChannel, length);
        setLoadingCommercial(true);
    }
    
    /**
     * Unschedules a scheduled commercial.
     */
    private void clearScheduledCommercial() {
        clearCommercialButtonsSelection();
        scheduledCommercialTime = 0;
        setCommercialResult("");
    }
    
    /**
     * Changes the status to waiting for commercial to run.
     * 
     * @param loading 
     */
    private void setLoadingCommercial(boolean loading) {
        for (JToggleButton b : commercialButtons.values()) {
            b.setEnabled(!loading);
        }
        if (loading) {
            setCommercialResult("Trying to run commercial..");
        }
    }
    
    /**
     * Sets the text of the commercialResult label.
     * 
     * @param result 
     */
    private void setCommercialResult(String result) {
        hideableLabel(commercialResult, result);
    }
    
    /**
     * The commercial result as returned from the API. Updates the result text
     * and when the commercial was last run, also sets it to "not loading" 
     * state.
     * 
     * @param stream
     * @param resultText
     * @param result 
     */
    public void commercialResult(String stream, String resultText, RequestResultCode result) {
        setCommercialResult(DateTime.currentTime()+" "+resultText);
        lastCommercialRunAttempt = System.currentTimeMillis();
        setLoadingCommercial(false);
        if (result == TwitchApi.RequestResultCode.RUNNING_COMMERCIAL) {
            lastCommercial.put(stream, System.currentTimeMillis());
            if (stream != null && stream.equals(currentChannel)) {
                lastCommercialRun = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Allows the commercial settings to be set.
     * 
     * @param enabled
     * @param length 
     */
    public void updateCommercialDelaySettings(boolean enabled, long length) {
        useCommercialDelay.setSelected(enabled);
        commercialDelay.setSettingValue(length);
    }
    
    /**
     * Changes the text on the given label, making it invisible if the text is
     * empty or visible again otherwise.
     * 
     * @param label
     * @param text 
     */
    private void hideableLabel(JLabel label, String text) {
        if (text.isEmpty()) {
            label.setForeground(LABEL_INVISIBLE);
        } else {
            label.setForeground(LABEL_VISIBLE);
            label.setText(text);
        }
    }

    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(5,5,5,5);
        gbc.weighty = 0;
        return gbc;
    }
    
    /**
     * Confirm dialog when a commercial is still scheduled when the dialog
     * is closed.
     * 
     * @return 
     */
    private int confirmContinueScheduledOnClose() {
        String message = "<html><body style='width:240'>"
                + "There is currently a commercial scheduled to be run. It can also"
                + " be run if you close this. What do you want to do?";
        String[] options = new String[]{"Run on schedule", "Cancel commercial"};
        int result = JOptionPane.showOptionDialog(main, message, 
                "Closing Admin Dialog while commercial is scheduled", JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE, null,
                options, options[0]);
        return result;
    }

    /**
     * Closes the dialog, unless a commercial is scheduled, then ask the user
     * whether it should be continued or canceled. Closing the prompt keeps the
     * dialog open so the user can check the commercial again.
     */
    private void close() {
        saveSettings();
        if (scheduledCommercialTime != 0) {
            int result = confirmContinueScheduledOnClose();
            if (result != JOptionPane.CLOSED_OPTION) {
                if (result == 1) {
                    clearScheduledCommercial();
                }
                super.setVisible(false);
            }
        } else {
            super.setVisible(false);
        }
    }
    
    @Override
    public void setVisible(boolean state) {
        if (state) {
            super.setVisible(true);
        } else {
            close();
        }
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
                || main.getStatusHistory().isFavorite(currentTitle, currentGame)) {
            main.getStatusHistory().addUsed(currentTitle, currentGame);
        }
    }
    
    /**
     * Adds the current status to the preset favorites
     */
    private void addCurrentToFavorites() {
        main.getStatusHistory().addFavorite(status.getText().trim(), game.getText());
    }
    
    /**
     * Main listener for button actions, commercial buttons have their own
     * listener which is created where the buttons are created.
     */
    private class MyActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == close) {
                close();
            }
            if (e.getSource() == update) {
                if (currentChannel != null && !currentChannel.isEmpty()) {
                    ChannelInfo info = new ChannelInfo(currentChannel, status.getText(), game.getText());
                    main.putChannelInfo(info);
                    setLoading(true);
                    addCurrentToHistory();
                }
            } else if (e.getSource() == reloadButton) {
                getChannelInfo();
            } else if (e.getSource() == selectGame) {
                selectGameDialog.setLocationRelativeTo(AdminDialog.this);
                String result = selectGameDialog.open(game.getText());
                if (result != null) {
                    game.setText(result);
                    statusEdited();
                }
            } else if (e.getSource() == removeGame) {
                game.setText("");
                statusEdited();
            } else if (e.getSource() == historyButton) {
                StatusHistoryEntry result = statusHistoryDialog.showDialog(game.getText());
                if (result != null) {
                    if (result.title != null) {
                        status.setText(result.title);
                    }
                    if (result.game != null) {
                        game.setText(result.game);
                    }
                }
            } else if (e.getSource() == addToHistoryButton) {
                addCurrentToFavorites();
            }
        }
    }

    /**
     * To get when the dialog is closed by the default (x) button and then close
     * it properly.
     */
    private class WindowClosingListener extends WindowAdapter {
        
        @Override
        public void windowClosing(WindowEvent e) {
            close();
        }
    }
    
    /**
     * Allows to deselect a button on a ButtonGroup, instead of always having
     * one selected.
     * 
     * Source: http://blog.frankel.ch/unselect-all-toggle-buttons-of-a-group
     */
    public static class NoneSelectedButtonGroup extends ButtonGroup {

        @Override
        public void setSelected(ButtonModel model, boolean selected) {
            if (selected) {
                super.setSelected(model, selected);
            } else {
                clearSelection();
            }
        }
    }
    
}
