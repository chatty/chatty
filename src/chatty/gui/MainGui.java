
package chatty.gui;

import chatty.gui.components.textpane.UserMessage;
import chatty.gui.components.userinfo.UserInfo;
import chatty.gui.components.DebugWindow;
import chatty.gui.components.ChannelInfoDialog;
import chatty.gui.components.LinkLabelListener;
import chatty.gui.components.help.About;
import chatty.gui.components.HighlightedMessages;
import chatty.gui.components.TokenDialog;
import chatty.gui.components.admin.AdminDialog;
import chatty.gui.components.ConnectionDialog;
import chatty.gui.components.Channel;
import chatty.gui.components.TokenGetDialog;
import chatty.gui.components.FavoritesDialog;
import chatty.gui.components.JoinDialog;
import chatty.util.api.Emoticon;
import chatty.util.api.StreamInfo;
import chatty.util.api.TokenInfo;
import chatty.util.api.Emoticons;
import chatty.util.api.ChannelInfo;
import java.util.List;
import chatty.Chatty;
import chatty.TwitchClient;
import chatty.Helper;
import chatty.User;
import chatty.Irc;
import chatty.gui.components.admin.StatusHistory;
import chatty.UsercolorItem;
import chatty.util.api.usericons.Usericon;
import chatty.WhisperManager;
import chatty.gui.components.AddressbookDialog;
import chatty.gui.components.AutoModDialog;
import chatty.gui.components.ChatRulesDialog;
import chatty.gui.components.EmotesDialog;
import chatty.gui.components.ErrorMessage;
import chatty.gui.components.FollowersDialog;
import chatty.gui.components.LiveStreamsDialog;
import chatty.gui.components.LivestreamerDialog;
import chatty.gui.components.ModerationLog;
import chatty.gui.components.NewsDialog;
import chatty.gui.components.srl.SRL;
import chatty.gui.components.SearchDialog;
import chatty.gui.components.StreamChat;
import chatty.gui.components.UpdateMessage;
import chatty.gui.components.menus.CommandActionEvent;
import chatty.gui.components.menus.CommandMenuItems;
import chatty.gui.components.menus.ContextMenuHelper;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.gui.components.settings.NotificationSettings;
import chatty.gui.components.settings.SettingsDialog;
import chatty.gui.components.textpane.AutoModMessage;
import chatty.gui.components.textpane.SubscriberMessage;
import chatty.gui.notifications.Notification;
import chatty.gui.notifications.NotificationActionListener;
import chatty.gui.notifications.NotificationManager;
import chatty.gui.notifications.NotificationWindowManager;
import chatty.util.CopyMessages;
import chatty.util.DateTime;
import chatty.util.ImageCache;
import chatty.util.MiscUtil;
import chatty.util.MsgTags;
import chatty.util.Sound;
import chatty.util.StringUtil;
import chatty.util.api.ChatInfo;
import chatty.util.api.CheerEmoticon;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.EmoticonUpdate;
import chatty.util.api.Emoticons.TagEmotes;
import chatty.util.api.FollowerInfo;
import chatty.util.api.TwitchApi.RequestResultCode;
import chatty.util.api.pubsub.ModeratorActionData;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import chatty.util.hotkeys.HotkeyManager;
import chatty.util.settings.Setting;
import chatty.util.settings.SettingChangeListener;
import chatty.util.settings.Settings;
import chatty.util.settings.SettingsListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.LogRecord;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * The Main Hub for all GUI activity.
 * 
 * @author tduva
 */
public class MainGui extends JFrame implements Runnable { 
    
    public static final Color COLOR_NEW_MESSAGE = new Color(200,0,0);
    public static final Color COLOR_NEW_HIGHLIGHTED_MESSAGE = new Color(255,80,0);
    
    public final Emoticons emoticons = new Emoticons();
    
    // Reference back to the client to give back data etc.
    TwitchClient client = null;
    
    public volatile boolean guiCreated;
    
    // Parts of the GUI
    private Channels channels;
    private ConnectionDialog connectionDialog;
    private TokenDialog tokenDialog;
    private TokenGetDialog tokenGetDialog;
    private DebugWindow debugWindow;
    private UserInfo userInfoDialog;
    private About aboutDialog;
    private ChannelInfoDialog channelInfoDialog;
    private SettingsDialog settingsDialog;
    private AdminDialog adminDialog;
    private FavoritesDialog favoritesDialog;
    private JoinDialog joinDialog;
    private HighlightedMessages highlightedMessages;
    private HighlightedMessages ignoredMessages;
    private MainMenu menu;
    private LiveStreamsDialog liveStreamsDialog;
    private NotificationWindowManager<String> notificationWindowManager;
    private NotificationManager notificationManager;
    private ErrorMessage errorMessage;
    private AddressbookDialog addressbookDialog;
    private SRL srl;
    private LivestreamerDialog livestreamerDialog;
    private UpdateMessage updateMessage;
    private NewsDialog newsDialog;
    private EmotesDialog emotesDialog;
    private FollowersDialog followerDialog;
    private FollowersDialog subscribersDialog;
    private StreamChat streamChat;
    private ModerationLog moderationLog;
    private AutoModDialog autoModDialog;
    private ChatRulesDialog chatRulesDialog;
    
    // Helpers
    private final Highlighter highlighter = new Highlighter();
    private final Highlighter ignoreChecker = new Highlighter();
    private StyleManager styleManager;
    private TrayIconManager trayIcon;
    private final StateUpdater state = new StateUpdater();
    private WindowStateManager windowStateManager;
    private final IgnoredMessages ignoredMessagesHelper = new IgnoredMessages(this);
    public final HotkeyManager hotkeyManager = new HotkeyManager(this);

    // Listeners that need to be returned by methods
    private ActionListener actionListener;
    private final WindowListener windowListener = new MyWindowListener();
    private final UserListener userListener = new MyUserListener();
    private final LinkLabelListener linkLabelListener = new MyLinkLabelListener();
    private final ContextMenuListener contextMenuListener = new MyContextMenuListener();
    
    public MainGui(TwitchClient client) {
        this.client = client;
        SwingUtilities.invokeLater(this);
    }
    
    @Override
    public void run() {
        createGui();
    }

    
    private Image createImage(String name) {
        return Toolkit.getDefaultToolkit().createImage(getClass().getResource(name));
    }
    
    /**
     * Sets different sizes of the window icon.
     */
    private void setWindowIcons() {
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createImage("app_16.png"));
        windowIcons.add(createImage("app_64.png"));
        this.setIconImages(windowIcons);
    }
    
    private void setLiveStreamsWindowIcons() {
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createImage("app_live_16.png"));
        windowIcons.add(createImage("app_live_64.png"));
        liveStreamsDialog.setIconImages(windowIcons);
    }
    
    private void setHelpWindowIcons() {
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createImage("app_help_16.png"));
        windowIcons.add(createImage("app_help_64.png"));
        aboutDialog.setIconImages(windowIcons);
    }
    
    /**
     * Creates the gui, run in the EDT.
     */
    private void createGui() {
        
        setWindowIcons();
        
        actionListener = new MyActionListener();
        
        // Error/debug stuff
        debugWindow = new DebugWindow(new DebugCheckboxListener());
        errorMessage = new ErrorMessage(this, linkLabelListener);
        
        // Dialogs and stuff
        connectionDialog = new ConnectionDialog(this);
        GuiUtil.installEscapeCloseOperation(connectionDialog);
        tokenDialog = new TokenDialog(this);
        tokenGetDialog = new TokenGetDialog(this);
        userInfoDialog = new UserInfo(this, client.settings, contextMenuListener);
        GuiUtil.installEscapeCloseOperation(userInfoDialog);
        aboutDialog = new About();
        setHelpWindowIcons();
        channelInfoDialog = new ChannelInfoDialog(this);
        channelInfoDialog.addContextMenuListener(contextMenuListener);
        adminDialog = new AdminDialog(this, client.api);
        favoritesDialog = new FavoritesDialog(this, contextMenuListener);
        GuiUtil.installEscapeCloseOperation(favoritesDialog);
        joinDialog = new JoinDialog(this);
        GuiUtil.installEscapeCloseOperation(joinDialog);
        liveStreamsDialog = new LiveStreamsDialog(contextMenuListener);
        setLiveStreamsWindowIcons();
        //GuiUtil.installEscapeCloseOperation(liveStreamsDialog);
        EmoteContextMenu.setEmoteManager(emoticons);
        emotesDialog = new EmotesDialog(this, emoticons, this, contextMenuListener);
        GuiUtil.installEscapeCloseOperation(emotesDialog);
        followerDialog = new FollowersDialog(FollowersDialog.Type.FOLLOWERS,
                this, client.api, contextMenuListener);
        subscribersDialog = new FollowersDialog(FollowersDialog.Type.SUBSCRIBERS,
                this, client.api, contextMenuListener);
        
        // Tray/Notifications
        trayIcon = new TrayIconManager(createImage("app_16.png"));
        trayIcon.addActionListener(new TrayMenuListener());
        notificationWindowManager = new NotificationWindowManager<>(this);
        notificationWindowManager.setNotificationActionListener(new MyNotificationActionListener());
        notificationManager = new NotificationManager(this, client.settings);

        // Channels/Chat output
        styleManager = new StyleManager(client.settings);
        highlightedMessages = new HighlightedMessages(this, styleManager,
                "Highlighted Messages","Highlighted", contextMenuListener);
        ignoredMessages = new HighlightedMessages(this, styleManager,
                "Ignored Messages", "Ignored", contextMenuListener);
        channels = new Channels(this,styleManager, contextMenuListener);
        channels.getComponent().setPreferredSize(new Dimension(600,300));
        add(channels.getComponent(), BorderLayout.CENTER);
        channels.setChangeListener(new ChannelChangeListener());
        
        // Some newer stuff
        addressbookDialog = new AddressbookDialog(this, client.addressbook);
        srl = new SRL(this, client.speedrunsLive, contextMenuListener);
        livestreamerDialog = new LivestreamerDialog(this, linkLabelListener, client.settings);
        updateMessage = new UpdateMessage(this);
        newsDialog = new NewsDialog(this, client.settings);
        
        client.settings.addSettingChangeListener(new MySettingChangeListener());
        client.settings.addSettingsListener(new MySettingsListener());
        
        streamChat = new StreamChat(this, styleManager, contextMenuListener,
            client.settings.getBoolean("streamChatBottom"));
        
        moderationLog = new ModerationLog(this);
        autoModDialog = new AutoModDialog(this, client.api, client);
        
        chatRulesDialog = new ChatRulesDialog(this);
        channels.setOnceOffEditListener(chatRulesDialog);
        
        //this.getContentPane().setBackground(new Color(0,0,0,0));

        getSettingsDialog();
        
        // Main Menu
        MainMenuListener menuListener = new MainMenuListener();
        menu = new MainMenu(menuListener,menuListener, linkLabelListener);
        setJMenuBar(menu);

        state.update();
        addListeners();
        pack();
        setLocationByPlatform(true);
        
        // Load some stuff
        client.api.setUserId(client.settings.getString("username"), client.settings.getString("userid"));
        client.api.requestEmoticons(false);
        //client.api.requestCheerEmoticons(false);
        // TEST
//        client.api.getUserIdAsap(null, "m_tt");
//        client.api.getCheers("m_tt", false);
        client.twitchemotes.requestEmotesets(false);
        if (client.settings.getBoolean("bttvEmotes")) {
            client.bttvEmotes.requestEmotes("$global$", false);
        }
        
        // Window states
        windowStateManager = new WindowStateManager(this, client.settings);
        windowStateManager.addWindow(this, "main", true, true);
        windowStateManager.setPrimaryWindow(this);
        windowStateManager.addWindow(highlightedMessages, "highlights", true, true);
        windowStateManager.addWindow(ignoredMessages, "ignoredMessages", true, true);
        windowStateManager.addWindow(channelInfoDialog, "channelInfo", true, true);
        windowStateManager.addWindow(liveStreamsDialog, "liveStreams", true, true);
        windowStateManager.addWindow(adminDialog, "admin", true, true);
        windowStateManager.addWindow(addressbookDialog, "addressbook", true, true);
        windowStateManager.addWindow(emotesDialog, "emotes", true, true);
        windowStateManager.addWindow(followerDialog, "followers", true, true);
        windowStateManager.addWindow(subscribersDialog, "subscribers", true, true);
        windowStateManager.addWindow(moderationLog, "moderationLog", true, true);
        windowStateManager.addWindow(streamChat, "streamChat", true, true);
        windowStateManager.addWindow(userInfoDialog, "userInfo", true, false);
        windowStateManager.addWindow(autoModDialog, "autoMod", true, true);
        
        guiCreated = true;
    }
    
    protected void popoutCreated(JDialog popout) {
        hotkeyManager.registerPopout(popout);
    }
    
    private SettingsDialog getSettingsDialog() {
        if (settingsDialog == null) {
            settingsDialog = new SettingsDialog(this,client.settings);
        }
        return settingsDialog;
    }
    
    /**
     * Perform a command executed by a hotkey. This means that the channel
     * context is the currently active channel and the selected user name is
     * added as command parameter if present.
     *
     * @param command The name of the command, leading / is removed if necessary
     * @param parameter2 Additional parameter after the username
     * @param selectedUserRequired Whether the command should only be executed
     * if a user is currently selected
     */
    private void hotkeyCommand(String command, String parameter2,
            boolean selectedUserRequired) {
        Channel channel = channels.getLastActiveChannel();
        User selectedUser = channel.getSelectedUser();
        if (selectedUserRequired && selectedUser == null) {
            return;
        }
        String selectedUserName = selectedUser != null ? selectedUser.getName() : "";
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        String parameter = null;
        if (!selectedUserName.isEmpty() || parameter2 != null) {
            parameter = selectedUserName+(parameter2 != null ? " "+parameter2 : "");
        }
        client.command(channels.getLastActiveChannel().getName(), command, parameter);
    }
    
    /**
     * Adds an action that is also represented in the main menu.
     * 
     * @param id The action id
     * @param label The label to use for the action
     * @param menuLabel The label to use for the action in the menu
     * @param mnemonic The mnemonic for the action in the menu
     * @param action The action to perform
     */
    private void addMenuAction(String id, String label, String menuLabel, int mnemonic, Action action) {
        action.putValue(Action.NAME, menuLabel);
        action.putValue(Action.MNEMONIC_KEY, mnemonic);
        menu.setAction(id, action);
        hotkeyManager.registerAction(id, label, action);
    }
    
    private void addListeners() {
        WindowManager manager = new WindowManager(this);
        manager.addWindowOnTop(liveStreamsDialog);
        
        MainWindowListener mainWindowListener = new MainWindowListener();
        addWindowStateListener(mainWindowListener);
        addWindowListener(mainWindowListener);
        
        hotkeyManager.registerAction("custom.command", "Custom Command", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommand(e.getActionCommand(), null, false);
            }
        });

        hotkeyManager.registerAction("tabs.next", "Tabs: Switch to next tab", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.switchToNextChannel();
            }
        });
        
        hotkeyManager.registerAction("tabs.previous", "Tabs: Switch to previous tab", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.switchToPreviousChannel();
            }
        });
        
        hotkeyManager.registerAction("tabs.close", "Tabs: Close tab/popout", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                client.closeChannel(channels.getActiveChannel().getName());
            }
        });
        

        
        addMenuAction("dialog.search", "Dialog: Open Search Dialog",
                "Find text..", KeyEvent.VK_F, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openSearchDialog();
            }
        });
        
        addMenuAction("dialog.addressbook", "Dialog: Addressbook (toggle)",
                "Addressbook", KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleAddressbook();
            }
        });
        
        addMenuAction("dialog.autoModDialog", "Dialog: AutoMod Dialog (toggle)",
                "AutoMod", KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleAutoModDialog();
            }
        });
        
        addMenuAction("dialog.moderationLog", "Dialog: Moderation Log (toggle)",
                "Moderation Log", KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleModerationLog();
            }
        });
        
        addMenuAction("dialog.channelInfo", "Dialog: Channel Info Dialog (toggle)",
                "Channel Info", KeyEvent.VK_C, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleChannelInfoDialog();
            }
        });
        
        addMenuAction("dialog.channelAdmin", "Dialog: Channel Admin Dialog (toggle)",
                "Channel Admin", KeyEvent.VK_A, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleChannelAdminDialog();
            }
        });
        
        addMenuAction("dialog.chatRules", "Dialog: Chat Rules (toggle)",
                "Chat Rules", KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleChatRules();
            }
        });
        
        addMenuAction("dialog.toggleEmotes", "Dialog: Emotes Dialog (toggle)",
                "Emoticons", KeyEvent.VK_E, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleEmotesDialog();
            }
        });

        addMenuAction("dialog.highlightedMessages", "Dialog: Highlighted Messages (toggle)",
                "Highlights", KeyEvent.VK_H, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleHighlightedMessages();
            }
        });
        
        addMenuAction("dialog.ignoredMessages", "Dialog: Ignored Messages (toggle)",
                "Ignored", KeyEvent.VK_I, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleIgnoredMessages();
            }
        });
        
        addMenuAction("dialog.streams", "Dialog: Live Channels Dialog (toggle)",
                "Live Channels", KeyEvent.VK_L, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleLiveStreamsDialog();
            }
        });
        
        addMenuAction("dialog.followers", "Dialog: Followers List (toggle)",
                "Followers", KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFollowerDialog();
            }
        });
        
        addMenuAction("dialog.subscribers", "Dialog: Subscriber List (toggle)",
                "Subscribers", KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSubscriberDialog();
            }
        });

        addMenuAction("dialog.joinChannel", "Dialog: Join Channel",
                "Join Channel", KeyEvent.VK_J, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openJoinDialog();
            }
        });
        
        hotkeyManager.registerAction("selection.toggle", "User Selection: Toggle", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Channel channel = channels.getLastActiveChannel();
                channel.toggleUserSelection();
            }
        });
        
        hotkeyManager.registerAction("selection.next", "User Selection: Next", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Channel channel = channels.getLastActiveChannel();
                channel.selectNextUser();
            }
        });
        
        hotkeyManager.registerAction("selection.nextExitAtBottom", "User Selection: Next (Exit at bottom)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Channel channel = channels.getLastActiveChannel();
                channel.selectNextUserExitAtBottom();
            }
        });
        
        hotkeyManager.registerAction("selection.previous", "User Selection: Previous", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Channel channel = channels.getLastActiveChannel();
                channel.selectPreviousUser();
            }
        });
        
        hotkeyManager.registerAction("selection.exit", "User Selection: Exit", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Channel channel = channels.getLastActiveChannel();
                channel.exitUserSelection();
            }
        });
        
        hotkeyManager.registerAction("selection.timeout30", "User Selection: Timeout (30s)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommand("timeout", "30", true);
            }
        });
        
        hotkeyManager.registerAction("selection.timeout600", "User Selection: Timeout (10m)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommand("timeout", "600", true);
            }
        });
        
        hotkeyManager.registerAction("selection.timeout30m", "User Selection: Timeout (30m)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommand("timeout", "1800", true);
            }
        });
        
        hotkeyManager.registerAction("selection.timeout24h", "User Selection: Timeout (24h)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommand("timeout", "86400", true);
            }
        });
        
        hotkeyManager.registerAction("commercial.30", "Run commercial (30s)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                runCommercial(30);
            }
        });
        
        hotkeyManager.registerAction("commercial.60", "Run commercial (60s)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                runCommercial(60);
            }
        });
        
        hotkeyManager.registerAction("commercial.90", "Run commercial (90s)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                runCommercial(90);
            }
        });
        
        hotkeyManager.registerAction("stream.addhighlight", "Add Stream Highlight", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommand("addstreamhighlight", null, false);
            }
        });
        
        hotkeyManager.registerAction("window.toggleCompact", "Window: Toggle Compact Mode", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleCompact(false);
            }
        });
        
        hotkeyManager.registerAction("window.toggleCompactMaximized", "Window: Toggle Compact Mode (Maximized)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleCompact(true);
            }
        });
        
        hotkeyManager.registerAction("window.toggleInput", "Window: Toggle Input", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.getActiveChannel().toggleInput();
            }
        });
        
        hotkeyManager.registerAction("window.toggleUserlist", "Window: Toggle Userlist", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.getActiveChannel().toggleUserlist();
            }
        });
        
        hotkeyManager.registerAction("dialog.toggleHelp", "Window: Toggle Help", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleHelp();
            }
        });

        addMenuAction("dialog.streamchat", "Window: Open StreamChat",
                "Open Dialog", KeyEvent.VK_O, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openStreamChat();
            }
        });
        
        hotkeyManager.registerAction("notification.closeAll", "Close all shown/queued notifications", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                notificationWindowManager.clearAll();
            }
        });
        
        hotkeyManager.registerAction("notification.closeAllShown", "Close all shown notifications", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                notificationWindowManager.clearAllShown();
            }
        });
        
        addMenuAction("application.exit", "Exit Chatty",
                "Exit", KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });
    }
    
    public void showGui() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (!guiCreated) {
                    return;
                }
                setVisible(true);
                
                // Should be done when the main window is already visible, so
                // it can be centered on it correctly, if that is necessary
                reopenWindows();
                
                newsDialog.autoRequestNews(true);
                
                client.init();
            }
        });
    }
    
    /**
     * Toggle the main menubar. Also toggle between maximized/normal if maximize
     * is true.
     * 
     * @param maximize If true, also toggle between maximized/normal
     */
    public void toggleCompact(final boolean maximize) {
        if (!isVisible()) {
            return;
        }
        final boolean hide = menu.isVisible();

        menu.setVisible(!hide);
        if (maximize) {
            if (hide) {
                setExtendedState(MAXIMIZED_BOTH);
            } else {
                setExtendedState(NORMAL);
            }
        }
    }
    
    /**
     * Bring the main window into view by bringing it out of minimization (if
     * necessary) and bringing it to the front.
     */
    private void makeVisible() {
        // Set visible was required to show it again after being minimized to tray
        setVisible(true);
        setState(NORMAL);
        toFront();
        //cleanupAfterRestoredFromTray();
        //setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    
    /**
     * Loads settings
     */
    public void loadSettings() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (guiCreated) {
                    loadSettingsInternal();
                }
            }
        });
    }

    /**
     * Initiates the GUI with settings
     */
    private void loadSettingsInternal() {
        if (client.settings.getBoolean("bufferStrategy1")) {
            createBufferStrategy(1);
        }
        
        setAlwaysOnTop(client.settings.getBoolean("ontop"));
        setResizable(client.settings.getBoolean("mainResizable"));
        streamChat.setResizable(client.settings.getBoolean("streamChatResizable"));
        
        loadMenuSettings();
        updateConnectionDialog(null);
        userInfoDialog.setUserDefinedButtonsDef(client.settings.getString("timeoutButtons"));
        debugWindow.getLogIrcCheckBox().setSelected(client.settings.getBoolean("debugLogIrc"));
        updateLiveStreamsDialog();
        
        windowStateManager.loadWindowStates();
        windowStateManager.setAttachedWindowsEnabled(client.settings.getBoolean("attachedWindows"));
        
        // Set window maximized state
        if (client.settings.getBoolean("maximized")) {
            setExtendedState(MAXIMIZED_BOTH);
        }
        updateHighlight();
        updateIgnore();
        updateHistoryRange();
        updateNotificationSettings();
        updateChannelsSettings();
        updateHighlightNextMessages();
        
        // This should be done before updatePopoutSettings() because that method
        // will delete the attributes correctly depending on the setting
        channels.setPopoutAttributes(client.settings.getList("popoutAttributes"));
        updatePopoutSettings();
        
        loadCommercialDelaySettings();
        UrlOpener.setPrompt(client.settings.getBoolean("urlPrompt"));
        UrlOpener.setCustomCommandEnabled(client.settings.getBoolean("urlCommandEnabled"));
        UrlOpener.setCustomCommand(client.settings.getString("urlCommand"));
        channels.setTabOrder(client.settings.getString("tabOrder"));
        
        favoritesDialog.setSorting((int)client.settings.getLong("favoritesSorting"));
        
        updateCustomContextMenuEntries();
        
        emoticons.setIgnoredEmotes(client.settings.getList("ignoredEmotes"));
        emoticons.loadFavoritesFromSettings(client.settings);
        emoticons.loadCustomEmotes();
        emoticons.addEmoji(client.settings.getString("emoji"));
        emoticons.setCheerState(client.settings.getString("cheersType"));
        emoticons.setCheerBackground(HtmlColors.decode(client.settings.getString("backgroundColor")));
        client.api.setToken(client.settings.getString("token"));
        
        userInfoDialog.setFontSize(client.settings.getLong("dialogFontSize"));
        
        hotkeyManager.setGlobalHotkeysEnabled(client.settings.getBoolean("globalHotkeysEnabled"));
        hotkeyManager.loadFromSettings(client.settings);
        
        streamChat.setMessageTimeout((int)client.settings.getLong("streamChatMessageTimeout"));
        
        emotesDialog.setEmoteScale((int)client.settings.getLong("emoteScaleDialog"));
        emotesDialog.setCloseOnDoubleClick(client.settings.getBoolean("closeEmoteDialogOnDoubleClick"));
        
        adminDialog.setStatusHistorySorting(client.settings.getString("statusHistorySorting"));
        
        Sound.setDeviceName(client.settings.getString("soundDevice"));
    }
    
    private static final String[] menuBooleanSettings = new String[]{
        "showJoinsParts", "ontop", "showModMessages", "attachedWindows",
        "simpleTitle", "globalHotkeysEnabled", "mainResizable", "streamChatResizable",
        "titleShowUptime", "titleShowViewerCount", "titleShowChannelState",
        "titleLongerUptime"
    };
    
    /**
     * Initiates the Main Menu with settings
     */
    private void loadMenuSettings() {
        for (String setting : menuBooleanSettings) {
            loadMenuSetting(setting);
        }
    }
    
    /**
     * Initiates a single setting in the Main Menu
     * @param name The name of the setting
     */
    private void loadMenuSetting(String name) {
        menu.setItemState(name,client.settings.getBoolean(name));
    }
    
    /**
     * Tells the highlighter the current list of highlight-items from the settings.
     */
    private void updateHighlight() {
        highlighter.update(StringUtil.getStringList(client.settings.getList("highlight")));
    }
    
    private void updateIgnore() {
        ignoreChecker.update(StringUtil.getStringList(client.settings.getList("ignore")));
    }
    
    private void updateCustomContextMenuEntries() {
        CommandMenuItems.setCommands(CommandMenuItems.MenuType.CHANNEL, client.settings.getString("channelContextMenu"));
        CommandMenuItems.setCommands(CommandMenuItems.MenuType.USER, client.settings.getString("userContextMenu"));
        CommandMenuItems.setCommands(CommandMenuItems.MenuType.STREAMS, client.settings.getString("streamsContextMenu"));
        ContextMenuHelper.livestreamerQualities = client.settings.getString("livestreamerQualities");
        ContextMenuHelper.enableLivestreamer = client.settings.getBoolean("livestreamer");
        ContextMenuHelper.settings = client.settings;
    }
    
    private void updateChannelsSettings() {
        channels.setDefaultUserlistWidth(
                (int)client.settings.getLong("userlistWidth"),
                (int)client.settings.getLong("userlistMinWidth"));
        channels.setChatScrollbarAlways(client.settings.getBoolean("chatScrollbarAlways"));
        channels.setDefaultUserlistVisibleState(client.settings.getBoolean("userlistEnabled"));
    }
    
    /**
     * Tells the highlighter the current username and whether it should be used
     * for highlight. Used to initialize on connect, when the username is fixed
     * for the duration of the connection.
     * 
     * @param username The current username.
     */
    public void updateHighlightSetUsername(String username) {
        highlighter.setUsername(username);
        highlighter.setHighlightUsername(client.settings.getBoolean("highlightUsername"));
    }
    
    /**
     * Tells the highlighter whether the current username should be used for
     * highlight. Used to set the setting when the setting is changed.
     * 
     * @param highlight 
     */
    private void updateHighlightSetUsernameHighlighted(boolean highlight) {
        highlighter.setHighlightUsername(highlight);
    }
    
    private void updateHighlightNextMessages() {
        highlighter.setHighlightNextMessages(client.settings.getBoolean("highlightNextMessages"));
    }
    
    private void updateNotificationSettings() {
        notificationWindowManager.setDisplayTime((int)client.settings.getLong("nDisplayTime"));
        notificationWindowManager.setMaxDisplayTime((int)client.settings.getLong("nMaxDisplayTime"));
        notificationWindowManager.setMaxDisplayItems((int)client.settings.getLong("nMaxDisplayed"));
        notificationWindowManager.setMaxQueueSize((int)client.settings.getLong("nMaxQueueSize"));
        int activityTime = client.settings.getBoolean("nActivity")
                ? (int)client.settings.getLong("nActivityTime") : -1;
        notificationWindowManager.setActivityTime(activityTime);
        notificationWindowManager.clearAll();
        notificationWindowManager.setScreen((int)client.settings.getLong("nScreen"));
        notificationWindowManager.setPosition((int)client.settings.getLong(("nPosition")));
    }
    
    private void updatePopoutSettings() {
        channels.setSavePopoutAttributes(client.settings.getBoolean("popoutSaveAttributes"));
        channels.setCloseLastChannelPopout(client.settings.getBoolean("popoutCloseLastChannel"));
    }
    
    /**
     * Puts the updated state of the windows/dialogs/popouts into the settings.
     */
    public void saveWindowStates() {
        windowStateManager.saveWindowStates();
        client.settings.putList("popoutAttributes", channels.getPopoutAttributes());
    }
    
    /**
     * Reopen some windows if enabled.
     */
    private void reopenWindows() {
        for (Window window : windowStateManager.getWindows()) {
            reopenWindow(window);
        }
    }
    
    /**
     * Open the given Component if enabled and if it was open before.
     * 
     * @param window 
     */
    private void reopenWindow(Window window) {
        if (windowStateManager.shouldReopen(window)) {
            if (window == liveStreamsDialog) {
                openLiveStreamsDialog();
            } else if (window == highlightedMessages) {
                openHighlightedMessages();
            } else if (window == ignoredMessages) {
                openIgnoredMessages();
            } else if (window == channelInfoDialog) {
                openChannelInfoDialog();
            } else if (window == addressbookDialog) {
                openAddressbook(null);
            } else if (window == adminDialog) {
                openChannelAdminDialog();
            } else if (window == emotesDialog) {
                openEmotesDialog();
            } else if (window == followerDialog) {
                openFollowerDialog();
            } else if (window == subscribersDialog) {
                openSubscriberDialog();
            } else if (window == moderationLog) {
                openModerationLog();
            } else if (window == streamChat) {
                openStreamChat();
            } else if (window == autoModDialog) {
                openAutoModDialog();
            }
        }
    }
    
    /**
     * Saves whether the window is currently maximized.
     */
    private void saveState(Component c) {
        if (c == this) {
            client.settings.setBoolean("maximized", isMaximized());
        }
    }
    
    /**
     * Returns if the window is currently maximized.
     * 
     * @return true if the window is maximized, false otherwise
     */
    private boolean isMaximized() {
        return (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
    }
    
    /**
     * Updates the connection dialog with current settings
     */
    private void updateConnectionDialog(String channelPreset) {
        connectionDialog.setUsername(client.settings.getString("username"));
        if (channelPreset != null) {
            connectionDialog.setChannel(channelPreset);
        } else {
            connectionDialog.setChannel(client.settings.getString("channel"));
        }

        String password = client.settings.getString("password");
        String token = client.settings.getString("token");
        boolean usePassword = client.settings.getBoolean("usePassword");
        connectionDialog.update(password, token, usePassword);
        connectionDialog.setAreChannelsOpen(channels.getChannelCount() > 0);
    }
    
    private void updateChannelInfoDialog() {
        String stream = channels.getLastActiveChannel().getStreamName();
        StreamInfo streamInfo = getStreamInfo(stream);
        channelInfoDialog.set(streamInfo);
    }
    
    private void updateTokenDialog() {
        String username = client.settings.getString("username");
        String token = client.settings.getString("token");
        tokenDialog.update(username, token);
        tokenDialog.setForeignToken(client.settings.getBoolean("foreignToken"));
    }
    
    private void updateFavoritesDialog() {
        Set<String> favorites = client.channelFavorites.getFavorites();
        Map<String, Long> history = client.channelFavorites.getHistory();
        favoritesDialog.setData(favorites, history);
    }
    
    private void updateFavoritesDialogWhenVisible() {
        if (favoritesDialog.isVisible()) {
            updateFavoritesDialog();
        }
    }
    
    public void updateUserinfo(final User user) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                updateUserInfoDialog(user);
            }
        });
    }
    
    private void updateUserInfoDialog(User user) {
        userInfoDialog.update(user, client.getUsername());
    }
    
    private void updateLiveStreamsDialog() {
        liveStreamsDialog.setSorting(client.settings.getString("liveStreamsSorting"));
    }
    
    private void updateHistoryRange() {
        int range = (int)client.settings.getLong("historyRange");
        channelInfoDialog.setHistoryRange(range);
        liveStreamsDialog.setHistoryRange(range);
    }
    
    private void openTokenDialog() {
        updateTokenDialog();
        updateTokenScopes();
        if (connectionDialog.isVisible()) {
            tokenDialog.setLocationRelativeTo(connectionDialog);
        } else {
            tokenDialog.setLocationRelativeTo(this);
        }
        tokenDialog.setVisible(true);
    }
    
    public void addStreamInfo(final StreamInfo streamInfo) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                liveStreamsDialog.addStream(streamInfo);
            }
        });
    }
    
    public ActionListener getActionListener() {
        return actionListener;
    }
    
    public StatusHistory getStatusHistory() {
        return client.statusHistory;
    }
    
    public boolean getSaveStatusHistorySetting() {
        return client.settings.getBoolean("saveStatusHistory");
    }
    
    class MyActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            // text input
            Channel chan = channels.getChannelFromInput(event.getSource());
            if (chan != null) {
                client.textInput(chan.getName(), chan.getInputText());
            }

            Object source = event.getSource();
            //---------------------------
            // Connection Dialog actions
            //---------------------------

            if (source == connectionDialog.getCancelButton()) {
                connectionDialog.setVisible(false);
                channels.setInitialFocus();
            } else if (source == connectionDialog.getConnectButton()
                    || source == connectionDialog.getChannelInput()) {
                String password = connectionDialog.getPassword();
                String channel = connectionDialog.getChannel();
                //client.settings.setString("username",name);
                client.settings.setString("password", password);
                client.settings.setString("channel", channel);
                if (client.prepareConnection(connectionDialog.rejoinOpenChannels())) {
                    connectionDialog.setVisible(false);
                    channels.setInitialFocus();
                }
            } else if (event.getSource() == connectionDialog.getGetTokenButton()) {
                openTokenDialog();
            } else if (event.getSource() == connectionDialog.getFavoritesButton()) {
                openFavoritesDialogFromConnectionDialog(connectionDialog.getChannel());
            } //---------------------------
            // Token Dialog actions
            //---------------------------
            else if (event.getSource() == tokenDialog.getDeleteTokenButton()) {
                int result = JOptionPane.showConfirmDialog(tokenDialog,
                        "<html><body style='width:400px'>"
                        + "This removes the login token from Chatty.<br><br>"
                        + "It does not revoke access for the token, which "
                        + "usually is no problem if the token isn't saved "
                        + "anywhere else. If you suspect it may still be stored "
                        + "in other places (or even compromised) you have to go "
                        + "to <code>twitch.tv/settings/connections</code> and "
                        + "click 'Disconnect' next to Chatty to revoke access.",
                        "Save Settings to file",
                        JOptionPane.OK_CANCEL_OPTION);
                if (result == 0) {
                    client.settings.setString("token", "");
                    client.settings.setBoolean("foreignToken", false);
                    client.settings.setString("username", "");
                    client.settings.setString("userid", "");
                    resetTokenScopes();
                    updateConnectionDialog(null);
                    tokenDialog.update("", "");
                    updateTokenScopes();
                }
            } else if (event.getSource() == tokenDialog.getRequestTokenButton()) {
                tokenGetDialog.setLocationRelativeTo(tokenDialog);
                tokenGetDialog.reset();
                client.startWebserver();
                tokenGetDialog.setVisible(true);

            } else if (event.getSource() == tokenDialog.getDoneButton()) {
                tokenDialog.setVisible(false);
            } else if (event.getSource() == tokenDialog.getVerifyTokenButton()) {
                verifyToken(client.settings.getString("token"));
            } // Get token Dialog
            else if (event.getSource() == tokenGetDialog.getCloseButton()) {
                tokenGetDialogClosed();
            } //-----------------
            // Userinfo Dialog
            //-----------------
            else if (userInfoDialog.getCommand(source) != null) {
                CustomCommand command = userInfoDialog.getCommand(source);
                User user = userInfoDialog.getUser();
                String nick = user.getName();
                String channel = userInfoDialog.getChannel();
                String reason = userInfoDialog.getBanReason();
                if (!reason.isEmpty()) {
                    reason = " "+reason;
                }
                Parameters parameters = Parameters.create(nick+reason);
                parameters.put("msg-id", userInfoDialog.getMsgId());
                parameters.put("target-msg-id", userInfoDialog.getTargetMsgId());
                parameters.put("automod-msg-id", userInfoDialog.getAutoModMsgId());
                client.anonCustomCommand(channel, command, parameters);
            // Favorites Dialog
            } else if (favoritesDialog.getAction(source) == FavoritesDialog.BUTTON_ADD_FAVORITES) {
                Set<String> channels = favoritesDialog.getChannels();
                client.channelFavorites.addChannelsToFavorites(channels);
            } else if (favoritesDialog.getAction(source) == FavoritesDialog.BUTTON_REMOVE_FAVORITES) {
                Set<String> channels = favoritesDialog.getSelectedChannels();
                client.channelFavorites.removeChannelsFromFavorites(channels);
            } else if (favoritesDialog.getAction(source) == FavoritesDialog.BUTTON_REMOVE) {
                Set<String> channels = favoritesDialog.getSelectedChannels();
                client.channelFavorites.removeChannels(channels);
            }
        }
        
    }

    private class DebugCheckboxListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            boolean state = e.getStateChange() == ItemEvent.SELECTED;
            if (e.getSource() == debugWindow.getLogIrcCheckBox()) {
                client.settings.setBoolean("debugLogIrc", state);
            }
        }
    }
    

    private class MyLinkLabelListener implements LinkLabelListener {
        @Override
        public void linkClicked(String type, String ref) {
            if (type.equals("help")) {
                openHelp(ref);
            } else if (type.equals("help-settings")) {
                openHelp("help-settings.html", ref);
            } else if (type.equals("help-commands")) {
                openHelp("help-custom_commands.html", ref);
            } else if (type.equals("help-admin")) {
                openHelp("help-admin.html", ref);
            } else if (type.equals("help-livestreamer")) {
                openHelp("help-livestreamer.html", ref);
            } else if (type.equals("help-whisper")) {
                openHelp("help-whisper.html", ref);
            } else if (type.equals("url")) {
                UrlOpener.openUrlPrompt(MainGui.this, ref);
            } else if (type.equals("update")) {
                if (ref.equals("show")) {
                    openUpdateDialog();
                }
            } else if (type.equals("announcement")) {
                if (ref.equals("show")) {
                    newsDialog.showDialog();
                }
            }
        }
    }
    
    public LinkLabelListener getLinkLabelListener() {
        return linkLabelListener;
    }
    
    public void clearHistory() {
        client.channelFavorites.clearHistory();
    }
    
    private class TrayMenuListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd == null || cmd.equals("show")) {
                makeVisible();
            }
            else if (cmd.equals("exit")) {
                exit();
            }
        }
        
    }
    
    /**
     * Listener for the Main Menu
     */
    private class MainMenuListener implements ItemListener, ActionListener, MenuListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            String setting = menu.getSettingByMenuItem(e.getSource());
            boolean state = e.getStateChange() == ItemEvent.SELECTED;

            if (setting != null) {
                client.settings.setBoolean(setting, state);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("debug")) {
                if (!debugWindow.isShowing()) {
                    debugWindow.setLocationByPlatform(true);
                    debugWindow.setPreferredSize(new Dimension(500, 400));
                }
                debugWindow.setVisible(true);
            } else if (cmd.equals("connect")) {
                openConnectDialogInternal(null);
            } else if (cmd.equals("disconnect")) {
                client.disconnect();
            } else if (cmd.equals("about")) {
                openHelp("");
            } else if (cmd.equals("news")) {
                newsDialog.showDialog();
            } else if (cmd.equals("settings")) {
                getSettingsDialog().showSettings();
            } else if (cmd.equals("saveSettings")) {
                int result = JOptionPane.showConfirmDialog(MainGui.this,
                        "This manually saves settings to file.\n" +
                        "Settings are also automatically saved when you exit Chatty.",
                        "Save Settings to file",
                        JOptionPane.OK_CANCEL_OPTION);
                if (result == 0) {
                    client.saveSettings(false);
                }
            } else if (cmd.equals("website")) {
                UrlOpener.openUrlPrompt(MainGui.this, Chatty.WEBSITE, true);
            } else if (cmd.equals("favoritesDialog")) {
                openFavoritesDialogToJoin("");
            } else if (cmd.equals("unhandledException")) {
                String[] array = new String[0];
                String a = array[1];
            } else if (cmd.equals("addressbook")) {
                openAddressbook(null);
            } else if (cmd.equals("srlRaces")) {
                openSrlRaces();
            } else if (cmd.equals("srlRaceActive")) {
                srl.searchRaceWithEntrant(channels.getActiveTab().getStreamName());
            } else if (cmd.startsWith("srlRace4")) {
                String stream = cmd.substring(8);
                if (!stream.isEmpty()) {
                    srl.searchRaceWithEntrant(stream);
                }
            } else if (cmd.equals("livestreamer")) {
                livestreamerDialog.open(null, null);
            } else if (cmd.equals("configureLogin")) {
                openTokenDialog();
            } else if (cmd.equals("addStreamHighlight")) {
                client.commandAddStreamHighlight(channels.getActiveChannel().getName(), null);
            } else if (cmd.equals("openStreamHighlights")) {
                client.commandOpenStreamHighlights(channels.getActiveChannel().getName());
            } else if (cmd.equals("srcOpen")) {
                client.speedruncom.openCurrentGame(channels.getActiveChannel());
            }
        }

        @Override
        public void menuSelected(MenuEvent e) {
            if (e.getSource() == menu.srlStreams) {
                ArrayList<String> popoutStreams = new ArrayList<>();
                for (Channel channel : channels.getPopoutChannels().keySet()) {
                    popoutStreams.add(channel.getStreamName());
                }
                menu.updateSrlStreams(channels.getActiveTab().getStreamName(), popoutStreams);
            } else if (e.getSource() == menu.view) {
                menu.updateCount(highlightedMessages.getNewCount(),
                        highlightedMessages.getDisplayedCount(),
                        ignoredMessages.getNewCount(),
                        ignoredMessages.getDisplayedCount());
            }
        }

        @Override
        public void menuDeselected(MenuEvent e) {
        }

        @Override
        public void menuCanceled(MenuEvent e) {
        }
    
    }
    
    /**
     * Listener for all kind of context menu events
     */
    class MyContextMenuListener implements ContextMenuListener {

        /**
         * User context menu event.
         * 
         * @param e
         * @param user 
         */
        @Override
        public void userMenuItemClicked(ActionEvent e, User user, String autoModMsgId) {
            String cmd = e.getActionCommand();
            if (cmd.equals("userinfo")) {
                openUserInfoDialog(user, null, autoModMsgId);
            }
            else if (cmd.equals("addressbookEdit")) {
                openAddressbook(user.getName());
            }
            else if (cmd.equals("addressbookRemove")) {
                client.addressbook.remove(user.getName());
                updateUserInfoDialog(user);
            }
            else if (cmd.startsWith("cat")) {
                if (e.getSource() instanceof JCheckBoxMenuItem) {
                    boolean selected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                    String catName = cmd.substring(3);
                    if (selected) {
                        client.addressbook.add(user.getName(), catName);
                    } else {
                        client.addressbook.remove(user.getName(), catName);
                    }
                }
                updateUserInfoDialog(user);
            }
            else if (cmd.equals("setcolor")) {
                setColor(user.getName());
            }
            else if (cmd.equals("setname")) {
                setCustomName(user.getName());
            }
            else if (cmd.startsWith("command")) {
                customCommand(user.getChannel(), e, user.getRegularDisplayNick());
            } else if (cmd.equals("copyNick")) {
                MiscUtil.copyToClipboard(user.getName());
            } else if (cmd.equals("copyDisplayNick")) {
                MiscUtil.copyToClipboard(user.getDisplayNick());
            } else if (cmd.equals("ignore")) {
                client.commandSetIgnored(user.getName(), "chat", true);
            } else  if (cmd.equals("ignoreWhisper")) {
                client.commandSetIgnored(user.getName(), "whisper", true);
            } else  if (cmd.equals("unignore")) {
                client.commandSetIgnored(user.getName(), "chat", false);
            } else  if (cmd.equals("unignoreWhisper")) {
                client.commandSetIgnored(user.getName(), "whisper", false);
            } else if (cmd.equals("autoModApprove")) {
                client.api.autoModApprove(autoModMsgId);
            } else if (cmd.equals("autoModDeny")) {
                client.api.autoModDeny(autoModMsgId);
            } else {
                nameBasedStuff(e, user.getName());
            }
        }
        
        /**
         * Event of an URL context menu.
         * 
         * @param e
         * @param url 
         */
        @Override
        public void urlMenuItemClicked(ActionEvent e, String url) {
            String cmd = e.getActionCommand();
            if (cmd.equals("open")) {
                UrlOpener.openUrlPrompt(MainGui.this, url);
            }
            else if (cmd.equals("copy")) {
                MiscUtil.copyToClipboard(url);
            }
            else if (cmd.equals("join")) {
                client.commandJoinChannel(url);
            }
        }

        /**
         * Context menu event without any channel context, which means it just
         * uses the active one or performs some other action that doesn't
         * immediately require one.
         * 
         * @param e 
         */
        @Override
        public void menuItemClicked(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("channelInfo")) {
                openChannelInfoDialog();
            }
            else if (cmd.equals("channelAdmin")) {
                openChannelAdminDialog();
            }
            else if (cmd.equals("chatRules")) {
                openChatRules();
            }
            else if (cmd.equals("closeChannel")) {
                client.closeChannel(channels.getActiveChannel().getName());
            }
            else if (cmd.startsWith("closeAllTabs")) {
                Collection<Channel> chans = null;
                if (cmd.equals("closeAllTabsButCurrent")) {
                    chans = channels.getTabsRelativeToCurrent(0);
                } else if (cmd.equals("closeAllTabsToLeft")) {
                    chans = channels.getTabsRelativeToCurrent(-1);
                } else if (cmd.equals("closeAllTabsToRight")) {
                    chans = channels.getTabsRelativeToCurrent(1);
                } else if (cmd.equals("closeAllTabs")) {
                    chans = channels.getTabs();
                }
                if (chans != null) {
                    for (Channel c : chans) {
                        client.closeChannel(c.getName());
                    }
                }
            }
            else if (cmd.equals("joinHostedChannel")) {
                String chan = client.getHostedChannel(channels.getActiveChannel().getName());
                if (chan == null) {
                    printLine("No channel is currently being hosted.");
                } else {
                    client.joinChannel(chan);
                }
            }
            else if (cmd.equals("srcOpen")) {
                client.speedruncom.openCurrentGame(channels.getActiveChannel());
            }
            else if (cmd.equals("popoutChannel")) {
                channels.popoutActiveChannel();
            }
            else if (cmd.startsWith("command")) {
                customCommand(channels.getActiveChannel().getName(), e, channels.getActiveChannel().getStreamName());
            }
            else if (cmd.startsWith("range")) {
                int range = -1;
                switch (cmd) {
                    case "range1h":
                        range = 60;
                        break;
                    case "range2h":
                        range = 120;
                        break;
                    case "range4h":
                        range = 240;
                        break;
                    case "range8h":
                        range = 480;
                        break;
                    case "range12h":
                        range = 720;
                        break;
                }
                // Change here as well, because even if it's the same value,
                // update may be needed. This will make it update twice often.
                updateHistoryRange();
                client.settings.setLong("historyRange", range);
            } else {
                nameBasedStuff(e, channels.getActiveChannel().getStreamName());
            }
        }

        /**
         * Context menu event associated with a list of stream or channel names.
         * 
         * @param e
         * @param streams 
         */
        @Override
        public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams) {
            streamStuff(e, streams);
        }

        /**
         * Goes through the {@code StreamInfo} objects and adds the stream names
         * into a list, so it can be used by the more generic method.
         * 
         * @param e The event
         * @param streamInfos The list of {@code StreamInfo} objects associated
         * with this event
         * @see streamsMenuItemClicked(ActionEvent, Collection)
         */
        @Override
        public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos) {
            String cmd = e.getActionCommand();
            String sorting = null;
            if (cmd.equals("sortName")) {
                sorting = "name";
            } else if (cmd.equals("sortGame")) {
                sorting = "game";
            } else if (cmd.equals("sortRecent")) {
                sorting = "recent";
            } else if (cmd.equals("sortViewers")) {
                sorting = "viewers";
            }
            if (sorting != null) {
                client.settings.setString("liveStreamsSorting", sorting);
            } else {
                Collection<String> streams = new ArrayList<>();
                for (StreamInfo info : streamInfos) {
                    streams.add(info.getCapitalizedName());
                }
                streamsMenuItemClicked(e, streams);
            }
            if (cmd.equals("manualRefreshStreams")) {
                client.api.manualRefreshStreams();
                state.update(true);
            }
        }
        
        /**
         * Handles context menu events with a single name (stream/channel). Just
         * packs it into a list for use in another method.
         * 
         * @param cmd
         * @param name 
         */
        private void nameBasedStuff(ActionEvent e, String name) {
            Collection<String> list = new ArrayList<>();
            list.add(name);
            streamStuff(e, list);
        }
        
        /**
         * Any commands that are equal to these Strings is supposed to have a
         * stream parameter.
         */
        private final Set<String> streamCmds = new HashSet<>(
                Arrays.asList("profile", "join", "hostchannel"));
        
        /**
         * Any commands starting with these Strings is supposed to have a stream
         * parameter.
         */
        private final Set<String> streamCmdsPrefix = new HashSet<>(
                Arrays.asList("stream", "livestreamer"));
        
        /**
         * Check if this command requires at least one stream/channel parameter.
         * 
         * @param cmd
         * @return 
         */
        private boolean cmdRequiresStream(String cmd) {
            for (String prefix : streamCmdsPrefix) {
                if (cmd.startsWith(prefix)) {
                    return true;
                }
            }
            return streamCmds.contains(cmd);
        }
        
        /**
         * Handles context menu events that can be applied to one or more
         * streams or channels. Checks if any valid stream parameters are
         * present and outputs an error otherwise. Since this can also be called
         * if it's not one of the commands that actually require a stream (other
         * listeners may be registered), it also checks if it's actually one of
         * the commands it handles.
         * 
         * @param cmd The command
         * @param streams The list of stream or channel names
         */
        private void streamStuff(ActionEvent e, Collection<String> streams) {
            String cmd = e.getActionCommand();
            TwitchUrl.removeInvalidStreams(streams);
            if (streams.isEmpty() && cmdRequiresStream(cmd)) {
                JOptionPane.showMessageDialog(getActiveWindow(), "Can't perform action: No stream/channel.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String firstStream = null;
            if (!streams.isEmpty()) {
                firstStream = streams.iterator().next();
            }
            if (cmd.equals("stream") || cmd.equals("streamPopout")
                    || cmd.equals("streamPopoutOld") || cmd.equals("profile")) {
                List<String> urls = new ArrayList<>();
                for (String stream : streams) {
                    String url;
                    switch (cmd) {
                        case "stream":
                            url = TwitchUrl.makeTwitchStreamUrl(stream, false);
                            break;
                        case "profile":
                            url = TwitchUrl.makeTwitchProfileUrl(stream);
                            break;
                        case "streamPopout":
                            url = TwitchUrl.makeTwitchPlayerUrl(stream);
                            break;
                        default:
                            url = TwitchUrl.makeTwitchStreamUrl(stream, true);
                            break;
                    }
                    urls.add(url);
                }
                UrlOpener.openUrlsPrompt(getActiveWindow(), urls, true);
            } else if (cmd.equals("join")) {
                Set<String> channels = new HashSet<>();
                for (String stream : streams) {
                    channels.add(stream);
                }
                makeVisible();
                client.joinChannels(channels);
            } else if (cmd.startsWith("streams")) {
                ArrayList<String> streams2 = new ArrayList<>();
                for (String stream : streams) {
                    streams2.add(stream);
                }
                String type = TwitchUrl.MULTITWITCH;
                switch (cmd) {
                    case "streamsSpeedruntv":
                        type = TwitchUrl.SPEEDRUNTV;
                        break;
                    case "streamsKadgar":
                        type = TwitchUrl.KADGAR;
                        break;
                }
                TwitchUrl.openMultitwitch(streams2, getActiveWindow(), type);
            } else if (cmd.startsWith("livestreamer")) {
                // quality null means select
                String quality = null;
                if (cmd.startsWith("livestreamerQ")) {
                    quality = StringUtil.toLowerCase(cmd.substring(13));
                    if (quality.equalsIgnoreCase("select")) {
                        quality = null;
                    }
                }
                for (String stream : streams) {
                    livestreamerDialog.open(stream.toLowerCase(), quality);
                }
            } else if (cmd.equals("showChannelEmotes")) {
                if (firstStream != null) {
                    openEmotesDialogChannelEmotes(firstStream.toLowerCase());
                }
            } else if (cmd.equals("hostchannel")) {
                if (firstStream != null && streams.size() == 1) {
                    client.command(null, "host2", firstStream.toLowerCase());
                } else {
                    printLine("Can't host more than one channel.");
                }
            } else if (cmd.equals("follow")) {
                for (String stream : streams) {
                    client.commandFollow(null, stream);
                }
            } else if (cmd.equals("unfollow")) {
                for (String stream : streams) {
                    client.commandUnfollow(null, stream);
                }
            } else if (cmd.equals("copy") && !streams.isEmpty()) {
                MiscUtil.copyToClipboard(StringUtil.join(streams, ", "));
            } else if (cmd.startsWith("command")) {
                customCommand(channels.getLastActiveChannel().getName(), e, StringUtil.join(streams, " "));
            }
        }

        @Override
        public void emoteMenuItemClicked(ActionEvent e, EmoticonImage emoteImage) {
            Emoticon emote = emoteImage.getEmoticon();
            String url = null;
            if (e.getActionCommand().equals("code")) {
                channels.getActiveChannel().insertText(emote.code, true);
            } else if (e.getActionCommand().equals("cheer")) {
                url = "http://help.twitch.tv/customer/portal/articles/2449458";
            } else if (e.getActionCommand().equals("emoteImage")) {
                url = emoteImage.getLoadedFrom();
            } else if (e.getActionCommand().equals("ffzlink")) {
                url = TwitchUrl.makeFFZUrl();
            } else if (e.getActionCommand().equals("emoteId")) {
                if (emote.type == Emoticon.Type.FFZ) {
                    url = TwitchUrl.makeFFZUrl(emote.numericId);
                } else if (emote.type == Emoticon.Type.TWITCH) {
                    url = TwitchUrl.makeTwitchemotesUrl(emote.numericId);
                }
            } else if (e.getActionCommand().equals("emoteCreator")) {
                if (emote.type == Emoticon.Type.FFZ) {
                    url = TwitchUrl.makeFFZUrl(emote.creator);
                }
            } else if (e.getActionCommand().equals("twitchturbolink")) {
                url = TwitchUrl.makeTwitchTurboUrl();
            } else if (e.getActionCommand().equals("bttvlink")) {
                url = TwitchUrl.makeBttvUrl();
            } else if (e.getActionCommand().equals("emoteDetails")) {
                openEmotesDialogEmoteDetails(emote);
            }
            else if (e.getActionCommand().equals("ignoreEmote")) {
                String code = emote.code;
                if (emote instanceof CheerEmoticon) {
                    code = ((CheerEmoticon)emote).getSimpleCode();
                }
                int result = JOptionPane.showConfirmDialog(getActiveWindow(),
                          "<html><body style='width:200px'>Ignoring an emote "
                        + "means showing just the code instead of turning "
                        + "it into an image. The list of ignored emotes can be edited in "
                        + "the Settings under 'Emoticons'.\n\nDo you want to "
                        + "ignore '"+code+"' from now on?",
                        "Ignore Emote", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    emoticons.addIgnoredEmote(code);
                    client.settings.setAdd("ignoredEmotes", code);
                }
            }
            else if (e.getActionCommand().equals("favoriteEmote")) {
                emoticons.addFavorite(emote);
                client.settings.setAdd("favoriteEmotes", emote.code);
                emotesDialog.favoritesUpdated();
            }
            else if (e.getActionCommand().equals("unfavoriteEmote")) {
                emoticons.removeFavorite(emote);
                client.settings.listRemove("favoriteEmotes", emote.code);
                emotesDialog.favoritesUpdated();
            }
            if (emote.hasStreamSet()) {
                nameBasedStuff(e, emote.getStream());
            }
            if (url != null) {
                UrlOpener.openUrlPrompt(getActiveWindow(), url, true);
            }
        }

        @Override
        public void usericonMenuItemClicked(ActionEvent e, Usericon usericon) {
            if (e.getActionCommand().equals("usericonUrl")) {
                if (!usericon.metaUrl.isEmpty()) {
                    UrlOpener.openUrlPrompt(MainGui.this, usericon.metaUrl);
                }
            }
            else if (e.getActionCommand().equals("copyBadgeType")) {
                MiscUtil.copyToClipboard(usericon.badgeType.toString());
            }
            else if (e.getActionCommand().equals("addUsericonOfBadgeType")) {
                getSettingsDialog().showSettings("addUsericonOfBadgeType", usericon.badgeType.toString());
            }
            else if (e.getActionCommand().equals("addUsericonOfBadgeTypeId")) {
                getSettingsDialog().showSettings("addUsericonOfBadgeType", usericon.badgeType.id);
            }
            else if (e.getActionCommand().equals("badgeImage")) {
                UrlOpener.openUrlPrompt(getActiveWindow(), usericon.url.toString(), true);
            }
        }
        
        private void customCommand(String channel, ActionEvent e, String args) {
            CommandActionEvent ce = (CommandActionEvent)e;
            CustomCommand command = ce.getCommand();
            client.anonCustomCommand(channel, command, Parameters.create(args));
        }
        
    }

    private class ChannelChangeListener implements ChangeListener {
        
        private boolean openedFirstChannel = false;
        
        /**
         * When the focus changes to a different channel (either by changing
         * a tab in the main window or changing focus to a different popout
         * dialog).
         *
         * @param e
         */
        @Override
        public void stateChanged(ChangeEvent e) {
            state.update(true);
            updateChannelInfoDialog();
            emotesDialog.updateStream(channels.getLastActiveChannel().getStreamName());
            moderationLog.setChannel(channels.getLastActiveChannel().getStreamName());
            autoModDialog.setChannel(channels.getLastActiveChannel().getStreamName());
            if (!openedFirstChannel
                    && channels.getLastActiveChannel().getType() == Channel.Type.CHANNEL) {
                openedFirstChannel = true;
                if (adminDialog.isVisible()) {
                    openChannelAdminDialog();
                }
                if (followerDialog.isVisible()) {
                    openFollowerDialog();
                }
            }
        }
    }
    
    private class MyUserListener implements UserListener {
        
        @Override
        public void userClicked(User user, String msgId, String autoModMsgId, MouseEvent e) {
            if (e == null || (!e.isControlDown() && !e.isAltDown())) {
                openUserInfoDialog(user, msgId, autoModMsgId);
                return;
            }
            String command = client.settings.getString("commandOnCtrlClick");
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (e.isControlDown() && !command.isEmpty()) {
                client.command(user.getChannel(), command, user.getRegularDisplayNick());
            } else if (!e.isAltDown()) {
                openUserInfoDialog(user, msgId, autoModMsgId);
            }
        }

        @Override
        public void emoteClicked(Emoticon emote, MouseEvent e) {
            openEmotesDialogEmoteDetails(emote);
        }

        @Override
        public void usericonClicked(Usericon usericon, MouseEvent e) {
            if (!usericon.metaUrl.isEmpty()) {
                UrlOpener.openUrlPrompt(MainGui.this, usericon.metaUrl);
            }
        }
    }
    
    private class MyNotificationActionListener implements NotificationActionListener<String> {

        /**
         * Right-clicked on a notification.
         * 
         * @param data 
         */
        @Override
        public void notificationAction(String data) {
            if (data != null) {
                makeVisible();
                client.joinChannel(data);
            }
        }
    }
    
    public UserListener getUserListener() {
        return userListener;
    }
    
    public java.util.List<UsercolorItem> getUsercolorData() {
        return client.usercolorManager.getData();
    }
    
    public void setUsercolorData(java.util.List<UsercolorItem> data) {
        client.usercolorManager.setData(data);
    }
    
    public java.util.List<Usericon> getUsericonData() {
        return client.usericonManager.getData();
    }
    
    public Set<String> getTwitchBadgeTypes() {
        return client.usericonManager.getTwitchBadgeTypes();
    }
    
    public void setUsericonData(java.util.List<Usericon> data) {
        client.usericonManager.setData(data);
    }
    
    public java.util.List<Notification> getNotificationData() {
        return notificationManager.getData();
    }
    
    public void setNotificationData(java.util.List<Notification> data) {
        notificationManager.setData(data);
    }
    
    /**
     * Should only be called out of EDT. All commands have to be defined
     * lowercase, because they are made lowercase when entered.
     * 
     * @param command
     * @param parameter
     * @return 
     */
    public boolean commandGui(String channel, String command, String parameter) {
        if (command.equals("settings")) {
            getSettingsDialog().showSettings();
        } else if (command.equals("customemotes")) {
            printLine(emoticons.getCustomEmotesInfo());
        } else if (command.equals("reloadcustomemotes")) {
            printLine("Reloading custom emotes from file..");
            emoticons.loadCustomEmotes();
            printLine(emoticons.getCustomEmotesInfo());
        } else if (command.equals("livestreams")) {
            openLiveStreamsDialog();
        } else if (command.equals("channeladmin")) {
            openChannelAdminDialog();
        } else if (command.equals("channelinfo")) {
            openChannelInfoDialog();
        } else if (command.equals("userinfo")) {
            User user = client.getExistingUser(channel, parameter);
            if (user != null) {
                openUserInfoDialog(user, null, null);
            } else {
                printSystem(String.format("User %s in %s not found",
                        parameter,
                        channel));
            }
        } else if (command.equals("search")) {
            openSearchDialog();
        } else if (command.equals("insert")) {
            insert(parameter, false);
        } else if (command.equals("insertword")) {
            insert(parameter, true);
        } else if (command.equals("openurl")) {
            if (!UrlOpener.openUrl(parameter)) {
                printLine("Failed to open URL (none specified or invalid).");
            }
        } else if (command.equals("openurlprompt")) {
            // Could do in invokeLater() so command isn't visible in input box
            // while the dialog is open, but probably doesn't matter since this
            // is mainly for custom commands put in a context menu anyway.
            if (!UrlOpener.openUrlPrompt(getActiveWindow(), parameter, true)) {
                printLine("Failed to open URL (none specified or invalid).");
            }
        } else if (command.equals("openfollowers")) {
            openFollowerDialog();
        } else if (command.equals("opensubscribers")) {
            openSubscriberDialog();
        } else if (command.equals("openrules")) {
            if (parameter != null) {
                openChatRules("#"+parameter);
            } else {
                openChatRules();
            }
        } else if (command.equals("openstreamchat")) {
            openStreamChat();
        } else if (command.equals("clearstreamchat")) {
            streamChat.clear();
        } else if (command.equals("streamchattest")) {
            String message = "A bit longer chat message with emotes and stuff "
                    + "FrankerZ ZreknarF MiniK ("+(int)(Math.random()*10)+")";
            if (parameter != null && !parameter.isEmpty()) {
                message = parameter;
            }
            UserMessage m = new UserMessage(client.getSpecialUser(), message, null, null, 0);
            streamChat.printMessage(m);
        } else if (command.equals("livestreamer")) {
            String stream = null;
            String quality = null;
            if (parameter != null && !parameter.trim().isEmpty()) {
                String[] split = parameter.trim().split(" ");
                stream = split[0];
                if (stream.equals("$active")) {
                    stream = channels.getActiveChannel().getStreamName();
                    if (stream == null) {
                        printLine("Livestreamer: No channel open.");
                        return true;
                    }
                }
                if (split.length > 1) {
                    quality = split[1];
                }
            }
            printLine("Livestreamer: Opening stream..");
            livestreamerDialog.open(stream, quality);
        } else if (command.equals("help")) {
            openHelp(null);
        } else if (command.equals("setstreamchatsize")) {
            Dimension size = Helper.getDimensionFromParameter(parameter);
            if (size != null) {
                setStreamChatSize(size.width, size.height);
                printSystem("Set StreamChat size to " + size.width + "x" + size.height);
                return true;
            }
            printSystem("Invalid parameters.");
        } else if (command.equals("getstreamchatsize")) {
            Dimension d = streamChat.getSize();
            printSystem("StreamChat size: "+d.width+"x"+d.height);
        } else if (command.equals("setsize")) {
            Dimension size = Helper.getDimensionFromParameter(parameter);
            if (size != null) {
                setSize(size);
                printSystem(String.format("Set Window size to %dx%d", size.width, size.height));
                return true;
            }
            printSystem("Invalid parameters.");
        } else {
            return false;
        }
        return true;
    }
    
    public void insert(final String text, final boolean spaces) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (text != null) {
                    channels.getLastActiveChannel().insertText(text, spaces);
                }
            }
        });
    }
    
    private void openLiveStreamsDialog() {
        windowStateManager.setWindowPosition(liveStreamsDialog);
        liveStreamsDialog.setAlwaysOnTop(client.settings.getBoolean("ontop"));
        liveStreamsDialog.setState(JFrame.NORMAL);
        liveStreamsDialog.setVisible(true);
    }
    
    private void toggleLiveStreamsDialog() {
        if (liveStreamsDialog.isVisible()) {
            liveStreamsDialog.setVisible(false);
        } else {
            openLiveStreamsDialog();
        }
    }
    
    /**
     * Only call out of the EDT.
     * 
     * @param user
     * @param msgId 
     */
    public void openUserInfoDialog(User user, String msgId, String autoModMsgId) {
        windowStateManager.setWindowPosition(userInfoDialog, getActiveWindow());
        userInfoDialog.show(getActiveWindow(), user, msgId, autoModMsgId, client.getUsername());
    }
    
    private void openChannelInfoDialog() {
        windowStateManager.setWindowPosition(channelInfoDialog, getActiveWindow());
        channelInfoDialog.setVisible(true);
    }
    
    private void toggleChannelInfoDialog() {
        if (!closeDialog(channelInfoDialog)) {
            openChannelInfoDialog();
        }
    }
    
    private boolean closeDialog(JDialog dialog) {
        if (dialog.isVisible()) {
            dialog.setVisible(false);
            return true;
        }
        return false;
    }
    
    private void openChannelAdminDialog() {
        windowStateManager.setWindowPosition(adminDialog, getActiveWindow());
        updateTokenScopes();
        String stream = channels.getActiveChannel().getStreamName();
        if (stream == null) {
            stream = client.settings.getString("username");
        }
        adminDialog.open(stream);
    }
    
    private void toggleChannelAdminDialog() {
        if (!closeDialog(adminDialog)) {
            openChannelAdminDialog();
        }
    }
    
    private void openHelp(String ref) {
        openHelp(null, ref, false);
    }
    
    public void openHelp(String page, String ref) {
        openHelp(page, ref, false);
    }
    
    public void openHelp(String page, String ref, boolean keepPage) {
        if (!aboutDialog.isVisible()) {
            aboutDialog.setLocationRelativeTo(this);
        }
        if (!keepPage) {
            aboutDialog.open(page, ref);
        }
        // Set ontop setting, so it won't be hidden behind the main window
        aboutDialog.setAlwaysOnTop(client.settings.getBoolean("ontop"));
        aboutDialog.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        aboutDialog.toFront();
        aboutDialog.setState(NORMAL);
        aboutDialog.setVisible(true);
    }
    
    private void toggleHelp() {
        if (aboutDialog.isVisible()) {
            aboutDialog.setVisible(false);
        } else {
            openHelp(null, null, true);
        }
    }
    
    /**
     * Opens the release info in the help.
     */
    public void openReleaseInfo() {
        /**
         * Use invokeLater() twice to run definitely when everything is ready so it
         * jumps to the correct reference (#latest). Otherwise it didn't really
         * seem to work. This may also help to open it after other stuff (like
         * the Connection Dialog) is opened.
         * 
         * This was previously implicitly achieved by having it in showGui(),
         * which already runs in invokeLater(), and then calling this which also
         * ran in invokeLater().
         */
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        openHelp("help-releases.html", "latest");
                    }
                });
            }
        });
    }
    
    protected void openSearchDialog() {
//        searchDialog.setLocationRelativeTo(getActiveWindow());
//        searchDialog.setVisible(true);
        SearchDialog.showSearchDialog(channels.getActiveChannel(), this, getActiveWindow());
    }
    
    private void openEmotesDialog() {
        openEmotesDialog(channels.getLastActiveChannel().getStreamName());
    }
    
    private void openEmotesDialog(String channel) {
        windowStateManager.setWindowPosition(emotesDialog, getActiveWindow());
        emotesDialog.showDialog(client.getSpecialUser().getEmoteSet(), channel);
    }
    
    private void openEmotesDialogChannelEmotes(String channel) {
        client.requestChannelEmotes(channel);
        openEmotesDialog(channel);
        emotesDialog.showChannelEmotes();
    }
    
    private void openEmotesDialogEmoteDetails(Emoticon emote) {
        openEmotesDialog(null);
        emotesDialog.showEmoteDetails(emote);
    }
    
    protected void toggleEmotesDialog() {
        if (!closeDialog(emotesDialog)) {
            openEmotesDialog();
        }
    }
    
    private void openFollowerDialog() {
        windowStateManager.setWindowPosition(followerDialog);
        String stream = channels.getLastActiveChannel().getStreamName();
        if (stream == null || stream.isEmpty()) {
            stream = client.settings.getString("username");
        }
        if (stream != null && !stream.isEmpty()) {
            followerDialog.showDialog(stream);
        }
    }
    
    private void toggleFollowerDialog() {
        if (!closeDialog(followerDialog)) {
            openFollowerDialog();
        }
    }
    
    private void openSubscriberDialog() {
        windowStateManager.setWindowPosition(subscribersDialog);
        String stream = client.settings.getString("username");
        if (stream != null && !stream.isEmpty()) {
            subscribersDialog.showDialog(stream);
        }
    }
    
    private void toggleSubscriberDialog() {
        if (!closeDialog(subscribersDialog)) {
            openSubscriberDialog();
        }
    }
    
    private void openModerationLog() {
        windowStateManager.setWindowPosition(moderationLog);
        moderationLog.showDialog();
    }
    
    private void toggleModerationLog() {
        if (!closeDialog(moderationLog)) {
            openModerationLog();
        }
    }
    
    private void openAutoModDialog() {
        windowStateManager.setWindowPosition(autoModDialog);
        autoModDialog.showDialog();
    }
    
    private void toggleAutoModDialog() {
        if (!closeDialog(autoModDialog)) {
            openAutoModDialog();
        }
    }
    
    private void openChatRules() {
        openChatRules(channels.getLastActiveChannel().getName());
    }
    
    private void openChatRules(String channel) {
        chatRulesDialog.showRules(channel);
    }
    
    private void toggleChatRules() {
        if (!closeDialog(chatRulesDialog)) {
            openChatRules();
        }
    }
    
    private void openUpdateDialog() {
        updateMessage.setLocationRelativeTo(this);
        updateMessage.showDialog();
    }
    
    private void openFavoritesDialogFromConnectionDialog(String channel) {
        Set<String> channels = chooseFavorites(this, channel);
        if (!channels.isEmpty()) {
            connectionDialog.setChannel(Helper.buildStreamsString(channels));
        }
    }
    
    public Set<String> chooseFavorites(Component owner, String channel) {
        updateFavoritesDialog();
        favoritesDialog.setLocationRelativeTo(owner);
        int result = favoritesDialog.showDialog(channel, "Use chosen channels",
                "Use chosen channel");
        if (result == FavoritesDialog.ACTION_DONE) {
            return favoritesDialog.getChannels();
        }
        return new HashSet<>();
    }
    
    private void openFavoritesDialogToJoin(String channel) {
        updateFavoritesDialog();
        favoritesDialog.setLocationRelativeTo(this);
        int result = favoritesDialog.showDialog(channel, "Join chosen channels",
                "Join chosen channel");
        if (result == FavoritesDialog.ACTION_DONE) {
            Set<String> selectedChannels = favoritesDialog.getChannels();
            client.joinChannels(selectedChannels);
        }
    }
    
    private void openJoinDialog() {
        joinDialog.setLocationRelativeTo(this);
        Set<String> chans = joinDialog.showDialog();
        client.joinChannels(chans);
    }
    
    private void openHighlightedMessages() {
        windowStateManager.setWindowPosition(highlightedMessages);
        highlightedMessages.setVisible(true);
    }
    
    private void toggleHighlightedMessages() {
        if (!closeDialog(highlightedMessages)) {
            openHighlightedMessages();
        }
    }
    
    private void openIgnoredMessages() {
        windowStateManager.setWindowPosition(ignoredMessages);
        ignoredMessages.setVisible(true);
    }
    
    private void toggleIgnoredMessages() {
        if (!closeDialog(ignoredMessages)) {
            openIgnoredMessages();
        }
    }
    
    /**
     * Opens the addressbook, opening an edit dialog for the given name if it
     * is non-null.
     * 
     * @param name The name to edit or null.
     */
    private void openAddressbook(String name) {
        if (!addressbookDialog.isVisible()) {
            windowStateManager.setWindowPosition(addressbookDialog);
        }
        addressbookDialog.showDialog(name);
    }
    
    private void toggleAddressbook() {
        if (!closeDialog(addressbookDialog)) {
            openAddressbook(null);
        }
    }
    
    private void openSrlRaces() {
        srl.openRaceList();
    }
    
    /*
     * Channel Management
     */
    
    public void removeChannel(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.removeChannel(channel);
                state.update();
            }
        });
    }
    
    public void switchToChannel(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.switchToChannel(channel);
            }
        });
    }
    
    public void userJoined(final User user) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                notificationManager.userJoined(user);
            }
        });
    }
    
    public void userLeft(final User user) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                notificationManager.userLeft(user);
            }
        });
    }
    
    public void newFollowers(final FollowerInfo info) {
        SwingUtilities.invokeLater(() -> {
            notificationManager.newFollowers(info);
        });
    }
    
    public void setChannelNewStatus(final String channel, final String newStatus) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.setChannelNewStatus(channels.getChannel(channel));
            }
        });
    }
    
    public void statusNotification(final String channel, final StreamInfo info) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                notificationManager.streamInfoChanged(channel, info);
            }
        });
    }
    
    public void showNotification(String title, String message, Color foreground, Color background, String channel) {
        if (client.settings.getLong("nType") == NotificationSettings.NOTIFICATION_TYPE_CUSTOM) {
            notificationWindowManager.showMessage(title, message, foreground, background, channel);
        } else {
            trayIcon.displayInfo(title, message);
        }
    }
    
    public void showTestNotification(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (client.settings.getString("username").equalsIgnoreCase("joshimuz")) {
                    showNotification("[Test] It works!",
                            "Now you have your notifications Josh.. Kappa",
                            Color.BLACK, Color.WHITE, channel);
                } else if (channel == null) {
                    showNotification("[Test] It works!",
                            "This is where the text goes.",
                            Color.BLACK, Color.WHITE, null);
                } else {
                    showNotification("[Status] "+Helper.toValidChannel(channel),
                            "Test Notification (this would pop up when a stream status changes)",
                            Color.BLACK, Color.WHITE, channel);
            }
            }
        });
    }
    
    public boolean isAppActive() {
        for (Window frame : Window.getWindows()) {
            if (frame.isActive()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isChanActive(String channel) {
        return channels.getLastActiveChannel().getName().equals(channel);
    }
    
    public Window getActiveWindow() {
        for (Window frame : Window.getWindows()) {
            if (frame.isActive()) {
                return frame;
            }
        }
        return this;
    }
    
    /* ############
     * # Messages #
     */
    
    public void printMessage(String toChan, User user, String text, boolean action,
            String emotes, int bits) {
        printMessage(toChan, user, text, action, emotes, bits, null);
    }
    
    public void printMessage(final String toChan, final User user,
            final String text, final boolean action, final String emotes,
            final int origBits, final String id) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel chan;
                String channel = toChan;
                boolean whisper = false;
                
                // Disable Cheer emotes altogether if disabled in the settings
                int bits = origBits;
                if (client.settings.getString("cheersType").equals("none")) {
                    bits = 0;
                }
                
                /**
                 * Check if special channel and change target according to
                 * settings
                 */
                if (channel.equals(WhisperManager.WHISPER_CHANNEL)) {
                    int whisperSetting = (int)client.settings.getLong("whisperDisplayMode");
                    if (whisperSetting == WhisperManager.DISPLAY_ONE_WINDOW) {
                        chan = channels.getChannel(channel);
                    } else if (whisperSetting == WhisperManager.DISPLAY_PER_USER) {
                        if (!userIgnored(user, true)) {
                            chan = channels.getChannel("$"+user.getName());
                        } else {
                            chan = channels.getActiveChannel();
                        }
                    } else {
                        chan = channels.getActiveChannel();
                    }
                    whisper = true;
                } else {
                    chan = channels.getChannel(channel);
                }
                // If channel was changed from the given one, change accordingly
                channel = chan.getName();
                client.chatLog.message(chan.getName(), user, text, action);
                
                boolean isOwnMessage = isOwnUsername(user.getName()) || (whisper && action);
                boolean ignored = checkHighlight(user, text, ignoreChecker, "ignore", isOwnMessage)
                        || (userIgnored(user, whisper) && !isOwnMessage);
                
                boolean highlighted = false;
                if ((client.settings.getBoolean("highlightIgnored") || !ignored)
                        && !client.settings.listContains("noHighlightUsers", user.getName())) {
                    highlighted = checkHighlight(user, text, highlighter, "highlight", isOwnMessage);
                }
                
                TagEmotes tagEmotes = Emoticons.parseEmotesTag(emotes);
                
                // Do stuff if highlighted, without printing message
                if (highlighted) {
                    highlightedMessages.addMessage(channel, user, text, action,
                            tagEmotes, bits, whisper);
                    if (!highlighter.getLastMatchNoNotification()) {
                        channels.setChannelHighlighted(chan);
                    } else {
                        channels.setChannelNewMessage(chan);
                    }
                    notificationManager.highlight(user, text,
                            highlighter.getLastMatchNoNotification(),
                            highlighter.getLastMatchNoSound(),
                            isOwnMessage, whisper, origBits > 0);
                } else if (!ignored) {
                    if (whisper) {
                        notificationManager.whisper(user, text, isOwnMessage);
                    } else {
                        notificationManager.message(user, text, isOwnMessage,
                                origBits > 0);
                    }
                    if (!isOwnMessage) {
                        channels.setChannelNewMessage(chan);
                    }
                }
                
                // Do stuff if ignored, without printing message
                if (ignored) {
                    ignoredMessages.addMessage(channel, user, text, action,
                            tagEmotes, bits, whisper);
                    ignoredMessagesHelper.ignoredMessage(channel);
                }
                long ignoreMode = client.settings.getLong("ignoreMode");
                
                // Print or don't print depending on ignore
                if (ignored && (ignoreMode <= IgnoredMessages.MODE_COUNT || 
                        !showIgnoredInfo())) {
                    // Don't print message
                    if (isOwnMessage && ignoreMode != IgnoredMessages.MODE_HIDE) {
                        printLine(channel, "Own message ignored.");
                    }
                } else {
                    // Print message, but determine how exactly
                    UserMessage message = new UserMessage(user, text, tagEmotes, id, bits);
                    message.color = highlighter.getLastMatchColor();
                    message.whisper = whisper;
                    message.action = action;
                    if (highlighted) {
                        message.highlighted = highlighted;
                    } else if (ignored && ignoreMode == IgnoredMessages.MODE_COMPACT) {
                        message.ignored_compact = true;
                    }
                    chan.printMessage(message);
                    if (client.settings.listContains("streamChatChannels", channel)) {
                        streamChat.printMessage(message);
                    }
                }
                
                CopyMessages.copyMessage(client.settings, user, text, highlighted);
                
                // Stuff independent of highlight/ignore
                user.addMessage(processMessage(text), action, id);
                if (highlighted) {
                    user.setHighlighted();
                }
                updateUserInfoDialog(user);
            }
        });
    }
    
    public void printSubscriberMessage(final String channel, final User user,
            final String text, final String message, final int months,
            final String emotes) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                // Prepare, check ignore
                String fullMessage;
                if (StringUtil.isNullOrEmpty(message)) {
                    fullMessage = "[Notification] "+text;
                } else {
                    fullMessage = String.format("[Notification] %s [%s]", text, message);
                }
                
                // Chat window
                if (!checkInfoIgnore(fullMessage)) {
                    Emoticons.TagEmotes tagEmotes = Emoticons.parseEmotesTag(emotes);
                    SubscriberMessage m = new SubscriberMessage(user, text, message, months, tagEmotes, null);
                    channels.getChannel(channel).printMessage(m);
                    notificationManager.newSubscriber(user, text, message);
                } else {
                    ignoredMessages.addInfoMessage(channel, fullMessage);
                }

                // Chatlog/User Info
                client.chatLog.info(channel, fullMessage);
                if (!user.getName().isEmpty()) {
                    user.addSub(message != null ? processMessage(message) : "", text);
                    updateUserInfoDialog(user);
                }
            }
        });
    }
    
    /**
     * Checks the dedicated user ignore list. The regular ignore list may still
     * ignore the user.
     * 
     * @param user
     * @param whisper
     * @return 
     */
    private boolean userIgnored(User user, boolean whisper) {
        String setting = whisper ? "ignoredUsersWhisper" : "ignoredUsers";
        return client.settings.listContains(setting, user.getName());
    }
    
    private String processMessage(String text) {
        int mode = (int)client.settings.getLong("filterCombiningCharacters");
        return Helper.filterCombiningCharacters(text, "****", mode);
    }
    
    private boolean checkHighlight(User user, String text, Highlighter hl, String setting, boolean isOwnMessage) {
        if (client.settings.getBoolean(setting + "Enabled")) {
            if (client.settings.getBoolean(setting + "OwnText") ||
                    !isOwnMessage) {
                return hl.check(user, text);
            }
        }
        return false;
    }
    
    private boolean checkInfoIgnore(String text) {
        return checkHighlight(null, text, ignoreChecker, "ignore", false);
    }
    
    protected void ignoredMessagesCount(String channel, String message) {
        if (client.settings.getLong("ignoreMode") == IgnoredMessages.MODE_COUNT
                && showIgnoredInfo()) {
            if (channels.isChannel(channel)) {
                channels.getChannel(channel).printLine(message);
            }
        }
    }
    
    private boolean showIgnoredInfo() {
        return !client.settings.getBoolean("ignoreShowNotDialog") ||
                !ignoredMessages.isVisible();
    }
    
    private boolean isOwnUsername(String name) {
        String ownUsername = client.getUsername();
        return ownUsername != null && ownUsername.equalsIgnoreCase(name);
    }
    
    public void userBanned(final User user, final long duration, final String reason, final String id) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.getChannel(user.getChannel()).userBanned(user, duration, reason, id);
                user.addBan(duration, reason, id);
                updateUserInfoDialog(user);
                if (client.settings.listContains("streamChatChannels", user.getChannel())) {
                    streamChat.userBanned(user, duration, reason, id);
                }
            }
        });
    }

    public void clearChat() {
        clearChat(null);
    }
    
    public void clearChat(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel;
                if (channel == null) {
                    panel = channels.getActiveChannel();
                } else {
                    panel = channels.getChannel(channel);
                    if (client.settings.listContains("streamChatChannels", channel)) {
                        streamChat.clear();
                    }
                }
                if (panel != null) {
                    panel.clearChat();
                }
            }
        });
    }
    
    public void testHotkey() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel = channels.getLastActiveChannel();
                if (panel != null) {
                    panel.selectPreviousUser();
                }
            }
        });
    }
    
    public void printLine(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel = channels.getLastActiveChannel();
                if (panel != null) {
                    printInfo(panel, line);
                    client.chatLog.info(panel.getName(), line);
                }
            }
        });
    }
    
    public void printSystem(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel = channels.getActiveChannel();
                if (panel != null) {
                    printInfo(panel, line);
                    client.chatLog.system(panel.getName(), line);
                }
            }
        });
    }

    public void printLine(final String channel, final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (channel == null) {
                    printLine(line);
                } else {
                    printInfo(channels.getChannel(channel), line);
                    client.chatLog.info(channel, line);
                }
            }
        });
    }
    
    public void printLineAll(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //client.chatLog.info(null, line);
                if (channels.getChannelCount() == 0) {
                    Channel panel = channels.getActiveChannel();
                    if (panel != null) {
                        printInfo(panel, line);
                    }
                    return;
                }
                for (Channel channel : channels.channels()) {
                    printInfo(channel, line);
                    client.chatLog.info(channel.getName(), line);
                }
            }
        });
    }
    
    private void printInfo(Channel channel, String line) {
        if (!checkInfoIgnore(line)) {
            channel.printLine(line);
            if (channel.getType() == Channel.Type.SPECIAL) {
                channels.setChannelNewMessage(channel);
            }
        } else {
            ignoredMessages.addInfoMessage(channel.getName(), line);
        }
    }
    
    
    /**
     * Calls the appropriate method from the given channel
     * 
     * @param channel The channel this even has happened in.
     * @param type The type of event.
     * @param user The User object of who was the target of this event (mod/..).
     */
    public void printCompact(final String channel, final String type, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channels.getChannel(channel).printCompact(type, user);
            }
        });
    }
    
    /**
     * Perform search in the currently selected channel. Should only be called
     * from the EDT.
     * 
     * @param window
     * @param searchText 
     * @return  
     */
    public boolean search(final Window window, final String searchText) {
        Channel chan = channels.getChannelFromWindow(window);
        if (chan == null) {
            return false;
        }
        return chan.search(searchText);
    }
    
    public void resetSearch(final Window window) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel chan = channels.getChannelFromWindow(window);
                if (chan != null) {
                    chan.resetSearch();
                }
            }
        });
    }
    
    public void showMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (connectionDialog.isVisible()) {
                    JOptionPane.showMessageDialog(connectionDialog, message);
                }
                else {
                    printLine(message);
                }
            }
        });
    }
    
    /**
     * Outputs a line to the debug window
     * 
     * @param line 
     */
    public void printDebug(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                debugWindow.printLine(line);
            }
        });
    }
    
    public void printDebugFFZ(final String line) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                debugWindow.printLineFFZ(line);
            }
        });
    }
    
    public void printDebugPubSub(final String line) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                debugWindow.printLinePubSub(line);
            }
        });
    }
    
    public void printModerationAction(final ModeratorActionData data,
            final boolean ownAction) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                moderationLog.add(data);
                autoModDialog.addData(data);
                
                String channel = Helper.toValidChannel(data.stream);
                if (channels.isChannel(channel)) {
                    // Output directly to chat (if enabled)
                    if (data.type == ModeratorActionData.Type.AUTOMOD_REJECTED
                            && data.args.size() > 1) {
                        // Automod
                        String username = data.args.get(0);
                        String message = data.args.get(1);
                        if (client.settings.getBoolean("showAutoMod")) {
                            User user = client.getUser(channel, username);
                            channels.getChannel(channel).printMessage(new AutoModMessage(user, message, data.msgId));
                        }
                        notificationManager.autoModMessage(channel, username, message);
                    } else if (!ownAction && client.settings.getBoolean("showModActions")) {
                        // Other Mod Actions
                        channels.getChannel(channel).printLine(String.format("[ModAction] %s: /%s %s",
                                data.created_by,
                                data.moderation_action,
                                StringUtil.join(data.args, " ")));
                    }
                }
            }
        });
    }
    
    public void autoModRequestResult(final String result, final String msgId) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                autoModDialog.requestResult(result, msgId);
            }
            
        });
    }
    
    /**
     * Outputs a line to the debug window
     * 
     * @param line 
     */
    public void printDebugIrc(final String line) {
        if (SwingUtilities.isEventDispatchThread()) {
            debugWindow.printLineIrc(line);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    debugWindow.printLineIrc(line);
                }
            });
        }
    }
    
    // User stuff
    
    /**
     * Adds a user to a channel, adding to the userlist
     * 
     * @param channel
     * @param user 
     */
    public void addUser(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!shouldUpdateUser(user)) return;
                Channel c = channels.getChannel(channel);
                c.addUser(user);
                if (channels.getActiveChannel() == c) {
                    state.update();
                }
            }
        });
    }
    
    /**
     * Removes a user from a channel, removing from the userlist
     * 
     * @param channel
     * @param user 
     */
    public void removeUser(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!shouldUpdateUser(user)) return;
                Channel c = channels.getChannel(channel);
                c.removeUser(user);
                if (channels.getActiveChannel() == c) {
                    state.update();
                }
            }
        });
    }
    
    /**
     * Updates a user.
     * 
     * @param user 
     */
    public void updateUser(final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!shouldUpdateUser(user)) return;
                channels.getChannel(user.getChannel()).updateUser(user);
                state.update();
            }
        });
    }
    
    private boolean shouldUpdateUser(User user) {
        return !user.getChannel().equals(WhisperManager.WHISPER_CHANNEL)
            || channels.isChannel(WhisperManager.WHISPER_CHANNEL);
    }
    
    /**
     * Resort users in the userlist of the given channel.
     * 
     * @param channel
     */
    public void resortUsers(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channels.getChannel(channel).resortUserlist();
            }
        });
    }
    
    /**
     * Clears the userlist on all channels.
     */
    public void clearUsers(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (channel != null) {
                    Channel c = channels.get(channel);
                    if (c != null) {
                        c.clearUsers();
                    }
                } else {
                    for (Channel channel : channels.channels()) {
                        channel.clearUsers();
                    }
                }
            }
        });
    }
    
    public void reconnect() {
        client.commandReconnect();
    }
    
    public void setUpdateAvailable(final String newVersion) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                menu.setUpdateNotification(true);
                updateMessage.setNewVersion(newVersion);
            }
        });
    }
    
    public void setAnnouncementAvailable(boolean enabled) {
        menu.setAnnouncementNotification(enabled);
    }
    
    public void showSettings() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                getSettingsDialog().showSettings();
            }
        });
    }
    
    public void setColor(final String item) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                getSettingsDialog().showSettings("editUsercolorItem", item);
            }
        });
    }
    
    public void setCustomName(final String item) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                getSettingsDialog().showSettings("editCustomNameItem", item);
            }
        });
    }

    public void updateChannelInfo() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateChannelInfoDialog();
           }
        });
    }
    
    public void updateState() {
        updateState(false);
    }
    
    public void updateState(final boolean forced) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                state.update(forced);
                //client.testHotkey();
            }
        });
    }
    
    /**
     * Manages updating the current state, mainly the titles and menus.
     */
    private class StateUpdater {
        
        /**
         * Saves when the state was last setd, so the delay can be measured.
         */
        private long stateLastUpdated = 0;
        
        /**
         * Update state no faster than this amount of milliseconds.
         */
        private static final int UPDATE_STATE_DELAY = 500;

        /**
         * Update the title and other things based on the current state and
         * stream/channel information. This is a convenience method that doesn't
         * force the update.
         * 
         * @see update(boolean)
         */
        protected void update() {
            update(false);
        }
        
        /**
         * Update the title and other things based on the current state and
         * stream/channel information.
         * 
         * <p>The update is only performed once every {@literal UPDATE_STATE_DELAY}
         * milliseconds, unless {@literal forced} is {@literal true}. This is meant
         * to prevent flickering of the titlebar when a lot of updates would
         * happen, for example when a lot of joins/parts happen at once.</p>
         * 
         * <p>Of course this means that the info might not be always up-to-date:
         * The chance is pretty high that the last update is skipped because it
         * came to close to the previous. The UpdateTimer updates every 10s so
         * it shouldn't take too long to be corrected. This also mainly affects
         * the chatter count because it gets updated in many small steps when
         * joins/parts happen (it also already isn't very up-to-date anyway from
         * Twitch's side though).</p>
         * 
         * @param forced If {@literal true} the update is performed with every call
         */
        protected void update(boolean forced) {
            if (!forced && System.currentTimeMillis() - stateLastUpdated < UPDATE_STATE_DELAY) {
                return;
            }
            stateLastUpdated = System.currentTimeMillis();

            int state = client.getState();

            requestFollowedStreams();
            updateMenuState(state);
            updateTitles(state);
        }

        /**
         * Disables/enables menu items based on the current state.
         *
         * @param state
         */
        private void updateMenuState(int state) {
            if (state > Irc.STATE_OFFLINE || state == Irc.STATE_RECONNECTING) {
                menu.getMenuItem("connect").setEnabled(false);
            } else {
                menu.getMenuItem("connect").setEnabled(true);
            }

            if (state > Irc.STATE_CONNECTING || state == Irc.STATE_RECONNECTING) {
                menu.getMenuItem("disconnect").setEnabled(true);
            } else {
                menu.getMenuItem("disconnect").setEnabled(false);
            }
        }
        
        /**
         * Updates the titles of both the main window and popout dialogs.
         * 
         * @param state 
         */
        private void updateTitles(int state) {
            // May be necessary to make the title either way, because it also
            // requests stream info
            String mainTitle = makeTitle(channels.getActiveTab(), state);
            String trayTooltip = makeTitle(channels.getLastActiveChannel(), state);
            trayIcon.setTooltipText(trayTooltip);
            if (client.settings.getBoolean("simpleTitle")) {
                setTitle("Chatty");
            } else {
                setTitle(mainTitle);
            }
            Map<Channel, JDialog> popoutChannels = channels.getPopoutChannels();
            for (Channel channel : popoutChannels.keySet()) {
                String title = makeTitle(channel, state);
                popoutChannels.get(channel).setTitle(title);
            }
        }

        /**
         * Assembles the title of the window based on the current state and chat
         * and stream info.
         *
         * @param channel The {@code Channel} object to create the title for
         * @param state The current state
         * @return The created title
         */
        private String makeTitle(Channel channel, int state) {
            String channelName = channel.getName();

            // Current state
            String stateText = "";

            if (state == Irc.STATE_CONNECTING) {
                stateText = "Connecting..";
            } else if (state == Irc.STATE_CONNECTED) {
                stateText = "Connecting...";
            } else if (state == Irc.STATE_REGISTERED) {
                if (channelName.isEmpty()) {
                    stateText = "Connected";
                }
            } else if (state == Irc.STATE_OFFLINE) {
                stateText = "Not connected";
            } else if (state == Irc.STATE_RECONNECTING) {
                stateText = "Reconnecting..";
            }

            String title = stateText;

            // Stream Info
            if (!channelName.isEmpty()) {
                boolean hideCounts = !client.settings.getBoolean("titleShowViewerCount");
                String chanNameText = channelName;
                if (client.isWhisperAvailable()) {
                    chanNameText += " [W]";
                }
                if (!title.isEmpty()) {
                    title += " - ";
                }
                String numUsers = Helper.formatViewerCount(channel.getNumUsers());
                if (!client.isUserlistLoaded(channelName)) {
                    numUsers += "*";
                }
                if (hideCounts) {
                    numUsers = "";
                }
                
                String chanState = "";
                if (client.settings.getBoolean("titleShowChannelState")) {
                    chanState = client.getChannelState(channelName).getInfo();
                }
                if (!chanState.isEmpty()) {
                    chanState = " "+chanState;
                }

                StreamInfo streamInfo = getStreamInfo(channel.getStreamName());
                if (streamInfo.isValidEnough()) {
                    if (streamInfo.getOnline()) {
                        
                        String uptime = "";
                        if (client.settings.getBoolean("titleShowUptime")) {
                            if (client.settings.getBoolean("titleLongerUptime")) {
                                uptime = DateTime.agoUptimeCompact2(
                                    streamInfo.getTimeStartedWithPicnic());
                            } else {
                                uptime = DateTime.agoUptimeCompact(
                                    streamInfo.getTimeStartedWithPicnic());
                            }
                        }
                        String numViewers = "|"+Helper.formatViewerCount(streamInfo.getViewers());
                        if (!client.settings.getBoolean("titleShowViewerCount")) {
                            numViewers = "";
                        } else if (!uptime.isEmpty()) {
                            uptime = "|"+uptime;
                        }
                        title += chanNameText + " [" + numUsers + numViewers + uptime + "]";
                    } else {
                        title += chanNameText;
                        if (!hideCounts) {
                            title += " [" + numUsers + "]";
                        }
                    }
                    title += chanState+" - " + streamInfo.getFullStatus();
                } else {
                    title += chanNameText;
                    if (!hideCounts) {
                        title += " [" + numUsers + "]";
                    }
                    title += chanState;
                }
            } else if (client.isWhisperAvailable()) {
                title += " [W]";
            }

            title += " - Chatty";
            
            String addition = client.settings.getString("titleAddition");
            if (!addition.isEmpty()) {
                title = addition+" "+title;
            }
            
            return title;
        }
    }

    public void openConnectDialog(final String channelPreset) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                openConnectDialogInternal(channelPreset);
            }
        });
    }
    
    private void openConnectDialogInternal(String channelPreset) {
        updateConnectionDialog(channelPreset);
        connectionDialog.setLocationRelativeTo(this);
        connectionDialog.setVisible(true);
    }
    
    private void openStreamChat() {
        windowStateManager.setWindowPosition(streamChat);
        streamChat.setVisible(true);
    }
    
    private void setStreamChatSize(int width, int height) {
        streamChat.setSize(width, height);
    }
    
    public void updateEmotesDialog() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emotesDialog.updateEmotesets(client.getSpecialUser().getEmoteSet());
            }
        });
    }
    
    public void updateEmoticons(final EmoticonUpdate update) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.updateEmoticons(update);
                emotesDialog.update();
            }
        });
    }
    
    public void addEmoticons(final Set<Emoticon> emotes) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.addEmoticons(emotes);
                emotesDialog.update();
            }
        });
    }
    
    public void setCheerEmotes(final Set<CheerEmoticon> emotes) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.setCheerEmotes(emotes);
            }
        });
    }
    
    public void setEmotesets(final Map<Integer, String> emotesets) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.addEmotesetStreams(emotesets);
            }
        });
    }

    /* ###############
     * Get token stuff
     */
    
    public void webserverStarted() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (tokenGetDialog.isVisible()) {
                    tokenGetDialog.ready();
                }
            }
        });
    }
    
    public void webserverError(final String error) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (tokenGetDialog.isVisible()) {
                    tokenGetDialog.error(error);
                }
            }
        });
    }
    
    public void webserverTokenReceived(final String token) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tokenReceived(token);
            }
        });
    }
    
    private void tokenGetDialogClosed() {
        tokenGetDialog.setVisible(false);
        client.stopWebserver();
    }
    
    /**
     * Token received from the webserver.
     * 
     * @param token 
     */
    private void tokenReceived(String token) {
        client.settings.setString("token", token);
        client.settings.setBoolean("foreignToken", false);
        if (tokenGetDialog.isVisible()) {
            tokenGetDialog.tokenReceived();
        }
        tokenDialog.update("",token);
        updateConnectionDialog(null);
        verifyToken(token);
    }
    
    /**
     * Verify the given Token. This sends a request to the TwitchAPI.
     * 
     * @param token 
     */
    private void verifyToken(String token) {
        client.api.verifyToken(token);
        tokenDialog.verifyingToken();
    }
    
    public void tokenVerified(final String token, final TokenInfo tokenInfo) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tokenVerifiedInternal(token, tokenInfo);
            }
        });
    }
    
    private String manuallyChangedToken = null;
    
    public void changeToken(final String token) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (token == null || token.isEmpty()) {
                    printSystem("You have to supply a token.");
                } else if (manuallyChangedToken != null) {
                    printSystem("You already have changed the token, please wait..");
                } else if (token.equals(client.settings.getString("token"))) {
                    printSystem("The token you entered is already set.");
                } else {
                    printSystem("Setting new token. Please wait..");
                    client.settings.setString("username", null);
                    manuallyChangedToken = token;
                    tokenReceived(token);
                }
            }
        });
    }
    
    /**
     * This does the main work when a response for verifying the token is
     * received from the Twitch API.
     * 
     * A Token can be verified manually by pressing the button or automatically
     * when a new Token was received by the webserver. So when this is called
     * the original source can be both.
     * 
     * The tokenGetDialog is closed if necessary.
     * 
     * @param token The token that was verified
     * @param username The usernamed that was received for this token. If this
     *      is null then an error occured, if it is empty then the token was
     *      invalid.
     */
    private void tokenVerifiedInternal(String token, TokenInfo tokenInfo) {
        // Stopping the webserver here, because it allows the /tokenreceived/
        // page to be delievered, because of the delay of verifying the token.
        // This should probably be solved better.
        client.stopWebserver();
        
        String result;
        String currentUsername = client.settings.getString("username");
        // Check if a new token was requested (the get token dialog should still
        // be open at this point) If this is wrong, it just displays the wrong
        // text, this shouldn't be used for something critical.
        boolean getNewLogin = tokenGetDialog.isVisible();
        boolean showInDialog = tokenDialog.isVisible();
        boolean changedTokenResponse = token == null
                ? manuallyChangedToken == null : token.equals(manuallyChangedToken);
        boolean valid = false;
        if (tokenInfo == null) {
            // An error occured when verifying the token
            if (getNewLogin) {
                result = "An error occured completing getting login data.";
            }
            else {
                result = "An error occured verifying login data.";
            }
        }
        else if (!tokenInfo.valid) {
            // There was an answer when verifying the token, but it was invalid
            if (getNewLogin) {
                result = "Invalid token received when getting login data. Please "
                    + "try again.";
                client.settings.setString("token", "");
            }
            else if (changedTokenResponse) {
                result = "Invalid token entered. Please try again.";
                client.settings.setString("token", "");
            }
            else {
                result = "Login data invalid. [help:login-invalid What does this mean?]";
            }
            if (!showInDialog && !changedTokenResponse) {
                showTokenWarning();
            }
        }
        else if (!tokenInfo.chat_access) {
            result = "No chat access (required) with token.";
        }
        else {
            // Everything is fine, so save username and token
            valid = true;
            String username = tokenInfo.name;
            client.settings.setString("username", username);
            client.settings.setString("userid", tokenInfo.userId);
            client.settings.setString("token", token);
            tokenDialog.update(username, token);
            updateConnectionDialog(null);
            if (!currentUsername.isEmpty() && !username.equals(currentUsername)) {
                result = "Login verified and ready to connect (replaced '" +
                        currentUsername + "' with '" + username + "').";
            }
            else {
                result = "Login verified and ready to connect.";
            }
        }
        if (changedTokenResponse) {
            printLine(result);
            manuallyChangedToken = null;
        }
        setTokenScopes(tokenInfo);
        // Always close the get token dialog, if it's not open, nevermind ;)
        tokenGetDialog.setVisible(false);
        // Show result in the token dialog
        tokenDialog.tokenVerified(valid, result);
    }
    
    /**
     * Sets the token scopes in the settings based on the given TokenInfo.
     * 
     * @param info 
     */
    private void setTokenScopes(TokenInfo info) {
        if (info == null) {
            return;
        }
        if (info.valid) {
            client.settings.setBoolean("token_chat", info.chat_access);
            client.settings.setBoolean("token_editor", info.channel_editor);
            client.settings.setBoolean("token_commercials", info.channel_commercials);
            client.settings.setBoolean("token_user", info.user_read);
            client.settings.setBoolean("token_subs", info.channel_subscriptions);
            client.settings.setBoolean("token_follow", info.user_follows_edit);
        }
        else {
            resetTokenScopes();
        }
        updateTokenScopes();
    }
    
    /**
     * Updates the token scopes in the GUI based on the settings.
     */
    private void updateTokenScopes() {
        boolean chat = client.settings.getBoolean("token_chat");
        boolean commercials = client.settings.getBoolean("token_commercials");
        boolean editor = client.settings.getBoolean("token_editor");
        boolean user = client.settings.getBoolean("token_user");
        boolean subscriptions = client.settings.getBoolean("token_subs");
        boolean follow = client.settings.getBoolean("token_follow");
        tokenDialog.updateAccess(chat, editor, commercials, user, subscriptions, follow);
        adminDialog.updateAccess(editor, commercials);
    }
    
    private void resetTokenScopes() {
        client.settings.setBoolean("token_chat", false);
        client.settings.setBoolean("token_commercials", false);
        client.settings.setBoolean("token_editor", false);
        client.settings.setBoolean("token_user", false);
        client.settings.setBoolean("token_subs", false);
        client.settings.setBoolean("token_follow", false);
    }
    
    public void showTokenWarning() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                String message = "<html><body style='width:400px;'>Login data was determined "
                        + "invalid, which means you may have to request it again before "
                        + "you can connect to chat or do authorized actions (like "
                        + "getting notified about streams you follow, edit stream title..).";
                String[] options = new String[]{"Close / Configure login","Just Close"};
                int result = GuiUtil.showNonAutoFocusOptionPane(MainGui.this, "Error",
                        message, JOptionPane.ERROR_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, options);
                if (result == 0) {
                    openTokenDialog();
                }
            }
        });
    }
    
    public void setSubscriberInfo(final FollowerInfo info) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                subscribersDialog.setFollowerInfo(info);
            }
        });
    }
    
    public void setFollowerInfo(final FollowerInfo info) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                followerDialog.setFollowerInfo(info);
            }
        });
    }
    
    public void setChannelInfo(final String channel, final ChannelInfo info, final RequestResultCode result) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.setChannelInfo(channel, info, result);
                userInfoDialog.setChannelInfo(info);
            }
        });
    }
    
    public void putChannelInfo(ChannelInfo info) {
        client.api.putChannelInfo(info);
    }
    
    public void getChannelInfo(String channel) {
        client.api.getChannelInfo(channel);
    }
    
    public ChannelInfo getCachedChannelInfo(String channel, String id) {
        return client.api.getCachedChannelInfo(channel, id);
    }
    
    public void getChatInfo(String stream) {
        client.api.getChatInfo(stream);
    }
    
    /**
     * 
     * @param info Can be null in an error occured
     */
    public void setChatInfo(final ChatInfo info) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                chatRulesDialog.setChatInfo(info);
            }
        });
    }
    
    public String getActiveStream() {
        return channels.getActiveChannel().getStreamName();
    }
    
    /**
     * Saves the Set game favorites to the settings.
     * 
     * @param favorites 
     */
    public void setGameFavorites(Set<String> favorites) {
        client.settings.putList("gamesFavorites", new ArrayList(favorites));
    }
    
    public void setCommunityFavorites(Map<String, String> favorites) {
        client.settings.putMap("communityFavorites", favorites);
    }
    
    /**
     * Returns a Set of game favorites retrieved from the settings.
     * 
     * @return 
     */
    public Set<String> getGameFavorites() {
        return new HashSet<>(client.settings.getList("gamesFavorites"));
    }
    
    public Map<String, String> getCommunityFavorites() {
        return client.settings.getMap("communityFavorites");
    }
    
    public void putChannelInfoResult(final RequestResultCode result) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.setPutResult(result);
            }
        });
    }
    
    public void saveCommercialDelaySettings(boolean enabled, long delay) {
        client.settings.setBoolean("adDelay", enabled);
        client.settings.setLong("adDelayLength", delay);
    }
    
    private void loadCommercialDelaySettings() {
        boolean enabled = client.settings.getBoolean("adDelay");
        long length = client.settings.getLong("adDelayLength");
        adminDialog.updateCommercialDelaySettings(enabled, length);
    }
    
    public void runCommercial(String stream, int length) {
        client.runCommercial(stream, length);
    }
    
    private void runCommercial(int length) {
        if (adminDialog.isCommercialsTabVisible()) {
            adminDialog.commercialHotkey(length);
        } else {
            runCommercial(getActiveStream(), length);
        }
    }
    
    public void commercialResult(final String stream, final String text, final RequestResultCode result) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.commercialResult(stream, text, result);
            }
        });
    }
    
    /**
     * Returns a list of open channels in the order the tabs are open (popouts
     * at the end if present).
     * 
     * Should probably only be used out of the EDT.
     * 
     * @return 
     */
    public List<String> getOpenChannels() {
        List<String> result = new ArrayList<>();
        for (Channel chan : channels.getChannelsOfType(Channel.Type.CHANNEL)) {
            result.add(chan.getName());
        }
        return result;
    }

    /**
     * Get StreamInfo for the given stream, but also request it for all open
     * channels.
     * 
     * @param stream
     * @return 
     */
    public StreamInfo getStreamInfo(String stream) {
        Set<String> streams = new HashSet<>();
        for (Channel chan : channels.getChannelsOfType(Channel.Type.CHANNEL)) {
            streams.add(chan.getStreamName());
        }
        return client.api.getStreamInfo(stream, streams);
    }
    
    /**
     * Outputs the full title if the StreamInfo for this channel is valid.
     * 
     * @param channel 
     */
    public void printStreamInfo(final String channel) {
        final String stream = channel.replace("#", "");
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (client.settings.getBoolean("printStreamStatus")) {
                    StreamInfo info = getStreamInfo(stream);
                    if (info.isValid()) {
                        printLine(channel, "~" + info.getFullStatus() + "~");
                    }
                }
            }
        });
    }
    
    /**
     * Possibly request followed streams from the API, if enabled and access
     * was granted.
     */
    private void requestFollowedStreams() {
        if (client.settings.getBoolean("requestFollowedStreams") &&
                client.settings.getBoolean("token_user")) {
            client.api.getFollowedStreams(client.settings.getString("token"));
        }
    }

    private class MySettingChangeListener implements SettingChangeListener {
        /**
         * Since this can also be called from other threads, run in EDT if
         * necessary.
         *
         * @param setting
         * @param type
         * @param value
         */
        @Override
        public void settingChanged(final String setting, final int type, final Object value) {
            if (SwingUtilities.isEventDispatchThread()) {
                settingChangedInternal(setting, type, value);
            } else {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        settingChangedInternal(setting, type, value);
                    }
                });
            }
        }
        
        private void settingChangedInternal(String setting, int type, Object value) {
            if (type == Setting.BOOLEAN) {
                boolean bool = (Boolean)value;
                if (setting.equals("ontop")) {
                    setAlwaysOnTop((Boolean) value);
                } else if (setting.equals("highlightUsername")) {
                    updateHighlightSetUsernameHighlighted((Boolean) value);
                } else if (setting.equals("highlightNextMessages")) {
                    updateHighlightNextMessages();
                } else if (setting.equals("popoutSaveAttributes") || setting.equals("popoutCloseLastChannel")) {
                    updatePopoutSettings();
                } else if (setting.equals("livestreamer")) {
                    ContextMenuHelper.enableLivestreamer = (Boolean)value;
                } else if (setting.equals("attachedWindows")) {
                    windowStateManager.setAttachedWindowsEnabled((Boolean)value);
                } else if (setting.equals("globalHotkeysEnabled")) {
                    hotkeyManager.setGlobalHotkeysEnabled((Boolean)value);
                } else if (setting.equals("imageCache")) {
                    ImageCache.setCachingEnabled(bool);
                } else if (setting.equals("mainResizable")) {
                    setResizable(bool);
                } else if (setting.equals("streamChatResizable")) {
                    streamChat.setResizable(bool);
                } else if (setting.equals("closeEmoteDialogOnDoubleClick")) {
                    emotesDialog.setCloseOnDoubleClick(bool);
                } else if (setting.equals("foreignToken")) {
                    tokenDialog.setForeignToken(bool);
                }
                if (setting.startsWith("title")) {
                    updateState(true);
                }
                loadMenuSetting(setting);
            }

            if (StyleManager.settingNames.contains(setting)) {
                styleManager.refresh();
                channels.refreshStyles();
                highlightedMessages.refreshStyles();
                ignoredMessages.refreshStyles();
                streamChat.refreshStyles();
                //menu.setForeground(styleManager.getColor("foreground"));
                //menu.setBackground(styleManager.getColor("background"));
            }
            if (setting.equals("displayNamesModeUserlist")) {
                channels.updateUserlistSettings();
            }
            if (type == Setting.STRING) {
                if (setting.equals("timeoutButtons")) {
                    userInfoDialog.setUserDefinedButtonsDef((String) value);
                } else if (setting.equals("token")) {
                    client.api.setToken((String)value);
                } else if (setting.equals("laf")) {
                    GuiUtil.setLookAndFeel((String)value);
                    GuiUtil.updateLookAndFeel();
                } else if (setting.equals("emoji")) {
                    emoticons.addEmoji((String)value);
                } else if (setting.equals("cheersType")) {
                    emoticons.setCheerState((String)value);
                } else if (setting.equals("backgroundColor")) {
                    emoticons.setCheerBackground(HtmlColors.decode((String)value));
                } else if (setting.equals("soundDevice")) {
                    Sound.setDeviceName((String)value);
                }
            }
            if (type == Setting.LIST) {
                if (setting.equals("highlight")) {
                    updateHighlight();
                } else if (setting.equals("ignore")) {
                    updateIgnore();
                } else if (setting.equals("hotkeys")) {
                    hotkeyManager.loadFromSettings(client.settings);
                }
            }
            if (type == Setting.LONG) {
                if (setting.equals("dialogFontSize")) {
                    userInfoDialog.setFontSize((Long)value);
                } else if (setting.equals("streamChatMessageTimeout")) {
                    streamChat.setMessageTimeout(((Long)value).intValue());
                } else if (setting.equals("emoteScaleDialog")) {
                    emotesDialog.setEmoteScale(((Long)value).intValue());
                }
            }
            if (setting.equals("channelFavorites") || setting.equals("channelHistory")) {
                // TOCONSIDER: This means that it is updated twice in a row when an action
                // requires both settings to be changed
                updateFavoritesDialogWhenVisible();
            }
            if (setting.equals("liveStreamsSorting")) {
                updateLiveStreamsDialog();
            }
            if (setting.equals("historyRange")) {
                updateHistoryRange();
            }
            Set<String> notificationSettings = new HashSet<>(Arrays.asList(
                "nScreen", "nPosition", "nDisplayTime", "nMaxDisplayTime",
                "nMaxDisplayed", "nMaxQueueSize", "nActivity", "nActivityTime"));
            if (notificationSettings.contains(setting)) {
                updateNotificationSettings();
            }
            if (setting.equals("spamProtection")) {
                client.setLinesPerSeconds((String)value);
            }
            if (setting.equals("urlPrompt")) {
                UrlOpener.setPrompt((Boolean)value);
            }
            if (setting.equals("urlCommandEnabled")) {
                UrlOpener.setCustomCommandEnabled((Boolean)value);
            }
            if (setting.equals("urlCommand")) {
                UrlOpener.setCustomCommand((String)value);
            }
            if (setting.equals("abUniqueCats")) {
                client.addressbook.setSomewhatUniqueCategories((String)value);
            }
            if (setting.equals("commands")) {
                client.customCommands.loadFromSettings();
            }
            if (setting.equals("channelContextMenu")
                    || setting.equals("userContextMenu")
                    || setting.equals("livestreamerQualities")
                    || setting.equals("streamsContextMenu")) {
                updateCustomContextMenuEntries();
            }
            else if (setting.equals("chatScrollbarAlways") || setting.equals("userlistWidth")) {
                updateChannelsSettings();
            }
            else if (setting.equals("ignoredEmotes")) {
                emoticons.setIgnoredEmotes(client.settings.getList("ignoredEmotes"));
            }
        }
    }
    
    private class MySettingsListener implements SettingsListener {

        @Override
        public void aboutToSaveSettings(Settings settings) {
            if (SwingUtilities.isEventDispatchThread()) {
                System.out.println("Saving GUI settings.");
                client.settings.setLong("favoritesSorting", favoritesDialog.getSorting());
                emoticons.saveFavoritesToSettings(settings);
                client.settings.setString("statusHistorySorting", adminDialog.getStatusHistorySorting());
            }
        }
        
    }
    
    public Settings getSettings() {
        return client.settings;
    }
    
    public Collection<String> getSettingNames() {
        return client.settings.getSettingNames();
    }
    
    public Collection<String> getEmoteNames() {
        return emoticons.getEmoteNames();
    }
    
    public Collection<String> getEmoteNamesPerStream(String stream) {
        return emoticons.getEmotesNamesByStream(stream);
    }
    
    public String getCustomCompletionItem(String key) {
        return (String)client.settings.mapGet("customCompletion", key);
    }
    
    public void updateEmoteNames() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.updateEmoteNames(client.getSpecialUser().getEmoteSet());
            }
        });
    }
    
    public WindowListener getWindowListener() {
        return windowListener;
    }
    
    private class MyWindowListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (e.getSource() == tokenGetDialog) {
                tokenGetDialogClosed();
            }
        }
    }
    
    private class MainWindowListener extends WindowAdapter {
        
        @Override
        public void windowStateChanged(WindowEvent e) {
            if (e.getComponent() == MainGui.this) {
                saveState(e.getComponent());
                if (isMinimized()) {
                    if (client.settings.getBoolean("minimizeToTray")) {
                        minimizeToTray();
                    }
                } else {
                    // Only cleanup from tray if not minimized, when minimized
                    // cleanup should never be done
                    cleanupAfterRestoredFromTray();
                }
            }
        }

        @Override
        public void windowClosing(WindowEvent evt) {
            if (evt.getComponent() == MainGui.this) {
                if (client.settings.getBoolean("closeToTray")) {
                    minimizeToTray();
                } else {
                    exit();
                }
            }
        }
    }
    
    /**
     * Checks if the main window is currently minimized.
     * 
     * @return true if minimized, false otherwise
     */
    private boolean isMinimized() {
        return (getExtendedState() & ICONIFIED) == ICONIFIED;
    }
    
    /**
     * Minimize window to tray.
     */
    private void minimizeToTray() {
        if (!isMinimized()) {
            setExtendedState(getExtendedState() | ICONIFIED);
        }
        trayIcon.setIconVisible(true);
        // Set visible to false, so it is removed from the taskbar
        setVisible(false);
        //trayIcon.displayInfo("Minimized to tray", "Double-click icon to show again..");
    }
    
    /**
     * Remove tray icon if applicable.
     */
    private void cleanupAfterRestoredFromTray() {
        if (client.settings.getLong("nType") != NotificationSettings.NOTIFICATION_TYPE_TRAY) {
            trayIcon.setIconVisible(false);
        }
    }
    
    /**
     * Display an error dialog with the option to quit or continue the program
     * and to report the error.
     *
     * @param error The error as a LogRecord
     * @param previous Some previous debug messages as LogRecord, to provide
     * context
     */
    public void error(final LogRecord error, final LinkedList<LogRecord> previous) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                int result = errorMessage.show(error, previous);
                if (result == ErrorMessage.QUIT) {
                    exit();
                }
            }
        });
    }
    
    /**
     * Exit the program.
     */
    private void exit() {
        client.exit();
    }
    
    public void cleanUp() {
        if (SwingUtilities.isEventDispatchThread()) {
            hotkeyManager.cleanUp();
            setVisible(false);
            dispose();
        }
    }
    
}
