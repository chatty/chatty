
package chatty.gui.components.admin;

import chatty.Chatty;
import chatty.Helper;
import chatty.gui.DockedDialogHelper;
import chatty.gui.DockedDialogManager;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import chatty.util.api.ChannelInfo;
import chatty.util.api.ChannelStatus;
import chatty.util.api.TwitchApi;
import chatty.util.api.TwitchApi.RequestResultCode;
import chatty.util.dnd.DockContent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog to change stream info and run commercials.
 * 
 * @author tduva
 */
public class AdminDialog extends JDialog {
    
    private final static String EDITOR_TEXT = "[help-admin:status Help]";
    private final static String EDITOR_TEXT_NO_ACCESS
            = "Required access not available. [help-admin:access More information..]";
    private final static String COMMERCIALS_TEXT = "[help-admin:commercials Help]";
    private final static String COMMERCIALS_TEXT_NO_ACCESS
            = "No Commercial Access available. [help-admin:access More information..]";
    private final static String BLOCKED_TERMS_TEXT = "[help-admin: Help]";
    private final static String BLOCKED_TERMS_TEXT_NO_ACCESS
            = "Required access not available. [help-admin:access More information..]";
    
    // Colors for hideable labels
    private static final Color LABEL_INVISIBLE = new Color(0, 0, 0, 0);
    private static final Color LABEL_VISIBLE = new Color(120, 150, 150);
    
    // Insets for smaller kind of buttons
    public static final Insets SMALL_BUTTON_INSETS = new Insets(-1,15,-1,15);
    // How often to call update() which updates times and runs commercials.
    private static final int UPDATE_DELAY = 4000;

    private final MainGui main;
    private final TwitchApi api;
    
    private final ActionListener actionListener = new MyActionListener();
    
    private final StatusPanel statusPanel;
    private final CommercialPanel commercialPanel;
    private final BlockedTermsPanel blockedTermsPanel;

    // Shared
    private final JTabbedPane tabs;
    private final JButton close = new JButton(Language.getString("dialog.button.close"));
    private final LinkLabel infoText;
    
    // Current state/settings (currentChannel specific)
    private String currentChannel;

    


    // Current access (not currentChannel specific)
    private boolean commercialAccess;
    private boolean editorAccess;
    private boolean blockedTermsAccess;
    
    private final DockContent content;
    protected final DockedDialogHelper helper;

    public AdminDialog(MainGui main, TwitchApi api, DockedDialogManager dockedDialogs) {
        super(main);
        setTitle("Channel Admin - No Channel");
        this.main = main;
        this.api = api;
        addWindowListener(new WindowClosingListener());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // Special objects
        infoText = new LinkLabel("Test",main.getLinkLabelListener());
        
        statusPanel = new StatusPanel(this, main, api);
        commercialPanel = new CommercialPanel(main);
        blockedTermsPanel = new BlockedTermsPanel(this, api);
        
        GridBagConstraints gbc;
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        
        // Add to tab pane
        tabs = new JTabbedPane();
        tabs.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                updateInfoText();
                if (currentChannel != null) {
                    changeChannel(currentChannel);
                }
            }
        });
        tabs.addTab(Language.getString("admin.tab.status"), statusPanel);
        tabs.addTab(Language.getString("admin.tab.commercial"), commercialPanel);
        tabs.addTab("Blocked Terms", blockedTermsPanel);
        gbc = makeGbc(0,0,2,1);
        gbc.insets = new Insets(0,0,0,0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        mainPanel.add(tabs, gbc);
        
        
        gbc = makeGbc(0,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        mainPanel.add(infoText, gbc);
        
        gbc = makeGbc(1,1,1,1);
        //gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        close.setMnemonic(KeyEvent.VK_C);
        mainPanel.add(close,gbc);
        
        
        add(mainPanel, BorderLayout.CENTER);
        
        close.addActionListener(actionListener);
        
        
        finishDialog();
        
        startUpdateTimer();
        
        GuiUtil.installEscapeCloseOperation(this);
        
        content = dockedDialogs.createContent(mainPanel, "Admin", "-admin-");
        
        helper = dockedDialogs.createHelper(new DockedDialogHelper.DockedDialog() {
            
            @Override
            public void setVisible(boolean visible) {
                AdminDialog.super.setVisible(visible);
            }

            @Override
            public boolean isVisible() {
                return AdminDialog.super.isVisible();
            }

            @Override
            public void addComponent(Component comp) {
                add(comp, BorderLayout.CENTER);
            }

            @Override
            public void removeComponent(Component comp) {
                remove(comp);
            }

            @Override
            public Window getWindow() {
                return AdminDialog.this;
            }

            @Override
            public DockContent getContent() {
                return content;
            }
            
            @Override
            public void dockedChanged() {
                updateTabTitle();
            }
            
        });
        helper.setChannelChangeListener(channel -> {
            if (isVisible()) {
                setChannel(Helper.toStream(channel));
            }
        });
        helper.installContextMenu(tabs);
        helper.installContextMenu(mainPanel);
        helper.installContextMenu(infoText);
    }
    
    @Override
    public boolean isVisible() {
        if (helper != null) {
            return helper.isVisible();
        }
        return super.isVisible();
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
    }
    
    /**
     * Updates times and runs scheduled commercials.
     */
    private void update() {
        if (isVisible()) {
            statusPanel.update();
            commercialPanel.update();
            blockedTermsPanel.update();
        }
        commercialPanel.checkScheduled();
    }
    
    /**
     * Update the info text based on the current tab and access.
     */
    private void updateInfoText() {
        if (tabs.getSelectedIndex() == 0) {
            if (editorAccess) {
                infoText.setText(EDITOR_TEXT);
            }
            else {
                infoText.setText(EDITOR_TEXT_NO_ACCESS);
            }
        }
        else if (tabs.getSelectedIndex() == 1) {
            if (commercialAccess) {
                infoText.setText(COMMERCIALS_TEXT);
            }
            else {
                infoText.setText(COMMERCIALS_TEXT_NO_ACCESS);
            }
        }
        else {
            if (blockedTermsAccess) {
                infoText.setText(BLOCKED_TERMS_TEXT);
            }
            else {
                infoText.setText(BLOCKED_TERMS_TEXT_NO_ACCESS);
            }
        }
    }
    
    /**
     * Set the kind of access the current token grants. To be able to show
     * appropriate info texts.
     * 
     * @param editor
     * @param edit_broadcast
     * @param commercials 
     */
    public void updateAccess(boolean editor, boolean edit_broadcast, boolean commercials, boolean blockedTerms) {
        this.editorAccess = editor && edit_broadcast;
        this.commercialAccess = commercials;
        this.blockedTermsAccess = blockedTerms;
        updateInfoText();
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
        statusPanel.dialogOpened();
        setChannel(channel);
        
        setVisible(true);
    }
    
    public void commercialHotkey(int length) {
        commercialPanel.commercialHotkey(length);
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
            if (commercialPanel.confirmChannelChange(channel)) {
                changeChannel(channel);
            }
        }
        if (channel != null && !channel.isEmpty()) {
            //update.setEnabled(true);
        }
        setTitle(Language.getString("admin.title", currentChannel));
        updateTabTitle();
    }
    
    private void updateTabTitle() {
        if (helper.isDocked()) {
            tabs.setTitleAt(0, currentChannel+": "+Language.getString("admin.tab.status"));
        }
        else {
            tabs.setTitleAt(0, Language.getString("admin.tab.status"));
        }
    }

    /**
     * Change the currentChannel this dialog is about, so some loading/resetting
     * of currentChannel specific stuff is required.
     *
     * @param channel
     */
    private void changeChannel(String channel) {
        this.currentChannel = channel;
        commercialPanel.changeChannel(channel);
        if (tabs.getSelectedComponent() == statusPanel) {
            statusPanel.changeChannel(channel);
        }
        if (tabs.getSelectedComponent() == blockedTermsPanel) {
            blockedTermsPanel.changeStream(channel);
        }
        update();
    }

    /**
     * Changes the text on the given label, making it invisible if the text is
     * empty or visible again otherwise.
     * 
     * @param label
     * @param text 
     */
    protected static void hideableLabel(JLabel label, String text) {
        if (text.isEmpty()) {
            label.setForeground(LABEL_INVISIBLE);
        } else {
            label.setForeground(LABEL_VISIBLE);
            label.setText(text);
        }
    }

    protected static GridBagConstraints makeGbc(int x, int y, int w, int h) {
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
     * Closes the dialog, unless a commercial is scheduled, then ask the user
     * whether it should be continued or canceled. Closing the prompt keeps the
     * dialog open so the user can check the commercial again.
     */
    private void close() {
        commercialPanel.saveSettings();
        if (commercialPanel.checkOnClose()) {
            helper.setVisible(false, false);
        }
    }
    
    @Override
    public void setVisible(boolean state) {
        if (state) {
            helper.setVisible(true, true);
        } else {
            close();
        }
    }
    
    public void channelStatusReceived(ChannelStatus status, RequestResultCode result) {
        statusPanel.channelStatusReceived(status, result);
    }
    
    public void setPutResult(RequestResultCode result) {
        statusPanel.setPutResult(result);
    }

    public void commercialResult(String stream, String text, RequestResultCode result) {
        commercialPanel.commercialResult(stream, text, result);
    }
    
    public void updateCommercialDelaySettings(boolean enabled, long length) {
        commercialPanel.updateCommercialDelaySettings(enabled, length);
    }
    
    public void setStatusHistorySorting(String string) {
        statusPanel.setStatusHistorySorting(string);
    }

    public String getStatusHistorySorting() {
        return statusPanel.getStatusHistorySorting();
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
    
}
