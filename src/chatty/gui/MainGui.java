
package chatty.gui;

import chatty.gui.transparency.TransparencyManager;
import chatty.gui.laf.LaF;
import chatty.util.api.pubsub.LowTrustUserMessageData;
import chatty.util.colors.HtmlColors;
import chatty.Addressbook;
import chatty.gui.components.textpane.UserMessage;
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
import chatty.util.*;
import chatty.util.api.*;

import java.util.List;
import chatty.Chatty;
import chatty.TwitchClient;
import chatty.Helper;
import chatty.User;
import chatty.Irc;
import chatty.Room;
import chatty.SettingsManager;
import chatty.gui.components.admin.StatusHistory;
import chatty.gui.colors.UsercolorItem;
import chatty.util.api.usericons.Usericon;
import chatty.WhisperManager;
import chatty.gui.Highlighter.HighlightItem;
import chatty.gui.Highlighter.Match;
import chatty.gui.laf.LaF.LaFSettings;
import chatty.gui.colors.ColorItem;
import chatty.gui.colors.MsgColorItem;
import chatty.gui.colors.MsgColorManager;
import chatty.gui.components.AddressbookDialog;
import chatty.gui.components.AutoModDialog;
import chatty.gui.components.EmotesDialog;
import chatty.gui.components.ErrorMessage;
import chatty.gui.components.eventlog.EventLog;
import chatty.gui.components.FollowersDialog;
import chatty.gui.components.LiveStreamsDialog;
import chatty.gui.components.LivestreamerDialog;
import chatty.gui.components.ModerationLog;
import chatty.gui.components.srl.SRL;
import chatty.gui.components.SearchDialog;
import chatty.gui.components.StreamChat;
import chatty.gui.transparency.TransparencyDialog;
import chatty.gui.components.admin.SelectGameDialog;
import chatty.gui.components.updating.UpdateDialog;
import chatty.gui.components.menus.CommandActionEvent;
import chatty.gui.components.menus.CommandMenuItems;
import chatty.gui.components.menus.ContextMenuHelper;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.gui.components.menus.StreamChatContextMenu;
import chatty.gui.components.menus.TextSelectionMenu;
import chatty.gui.components.settings.EmoteSettings;
import chatty.gui.components.settings.NotificationSettings;
import chatty.gui.components.settings.SettingsDialog;
import chatty.gui.components.textpane.AutoModMessage;
import chatty.gui.components.textpane.InfoMessage;
import chatty.gui.components.textpane.ModLogInfo;
import chatty.gui.components.textpane.SubscriberMessage;
import chatty.gui.components.textpane.UserNotice;
import chatty.gui.components.userinfo.UserInfoManager;
import chatty.gui.components.userinfo.UserNotes;
import chatty.gui.emoji.EmojiUtil;
import chatty.gui.notifications.Notification;
import chatty.gui.notifications.NotificationActionListener;
import chatty.gui.notifications.NotificationManager;
import chatty.gui.notifications.NotificationManager.NotificationWindowData;
import chatty.gui.notifications.NotificationWindowManager;
import chatty.lang.Language;
import chatty.util.api.Emoticons.TagEmotes;
import chatty.util.api.TwitchApi.RequestResultCode;
import chatty.util.api.pubsub.ModeratorActionData;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockLayout;
import chatty.util.hotkeys.HotkeyManager;
import chatty.util.irc.MsgTags;
import chatty.util.settings.FileManager;
import chatty.util.settings.Setting;
import chatty.util.settings.SettingChangeListener;
import chatty.util.settings.Settings;
import chatty.util.settings.SettingsListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import chatty.util.dnd.DockPopout;
import chatty.util.gif.FocusUpdates;
import chatty.util.gif.GifUtil;
import java.util.function.Consumer;
import org.json.simple.JSONValue;

/**
 * The Main Hub for all GUI activity.
 * 
 * @author tduva
 */
public class MainGui extends JFrame implements Runnable { 
    
    private static final Logger LOGGER = Logger.getLogger(MainGui.class.getName());
    
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
    private UserInfoManager userInfoDialog;
    private About aboutDialog;
    private ChannelInfoDialog channelInfoDialog;
    private AdminDialog adminDialog;
    private FavoritesDialog favoritesDialog;
    private JoinDialog joinDialog;
    private HighlightedMessages highlightedMessages;
    private HighlightedMessages ignoredMessages;
    private MainMenu menu;
    private LiveStreamsDialog liveStreamsDialog;
    private NotificationWindowManager<NotificationWindowData> notificationWindowManager;
    private NotificationManager notificationManager;
    private ErrorMessage errorMessage;
    private AddressbookDialog addressbookDialog;
    private SRL srl;
    private LivestreamerDialog livestreamerDialog;
    private UpdateDialog updateDialog;
    //private NewsDialog newsDialog;
    private EmotesDialog emotesDialog;
    private FollowersDialog followerDialog;
    private FollowersDialog subscribersDialog;
    private StreamChat streamChat;
    private ModerationLog moderationLog;
    private AutoModDialog autoModDialog;
    private EventLog eventLog;
    
    // Helpers
    private final Highlighter highlighter = new Highlighter("highlight");
    private final Highlighter ignoreList = new Highlighter("ignore");
    private final Highlighter filter = new Highlighter("filter");
    public final RepeatMsgHelper repeatMsg;
    private final MsgColorManager msgColorManager;
    private StyleManager styleManager;
    private TrayIconManager trayIcon;
    private final StateUpdater state = new StateUpdater();
    private WindowStateManager windowStateManager;
    protected DockedDialogManager dockedDialogs;
    private final IgnoredMessages ignoredMessagesHelper = new IgnoredMessages(this);
    public final HotkeyManager hotkeyManager = new HotkeyManager(this);
    public final LocalEmotesSetting localEmotes;

    // Listeners that need to be returned by methods
    private ActionListener actionListener;
    private final WindowListener windowListener = new MyWindowListener();
    private final UserListener userListener = new MyUserListener();
    private final LinkLabelListener linkLabelListener = new MyLinkLabelListener();
    protected final ContextMenuListener contextMenuListener = new MyContextMenuListener();
    
    public MainGui(TwitchClient client) {
        this.client = client;
        msgColorManager = new MsgColorManager(client.settings);
        localEmotes = new LocalEmotesSetting(client.settings, this);
        repeatMsg = new RepeatMsgHelper(client.settings);
        SwingUtilities.invokeLater(this);
    }
    
    @Override
    public void run() {
        createGui();
    }

    /**
     * Sets different sizes of the window icon.
     */
    private void setWindowIcons() {
        setIconImages(IconManager.getMainIcons());
    }
    
    private void setLiveStreamsWindowIcons() {
        liveStreamsDialog.setIconImages(IconManager.getLiveIcons());
    }
    
    private void setHelpWindowIcons() {
        aboutDialog.setIconImages(IconManager.getHelpIcons());
    }
    
    private void setDebugWindowIcons() {
        debugWindow.setIconImages(IconManager.getDebugIcons());
    }
    
    /**
     * Creates the gui, run in the EDT.
     */
    private void createGui() {
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setWindowIcons();
        
        actionListener = new MyActionListener();
        TextSelectionMenu.listener = contextMenuListener;
        
        // Error/debug stuff
        debugWindow = new DebugWindow(new DebugCheckboxListener());
        setDebugWindowIcons();
        errorMessage = new ErrorMessage(this, linkLabelListener);
        
        // Dialogs and stuff
        connectionDialog = new ConnectionDialog(this);
        GuiUtil.installEscapeCloseOperation(connectionDialog);
        tokenDialog = new TokenDialog(this);
        tokenGetDialog = new TokenGetDialog(this);
        userInfoDialog = new UserInfoManager(this, client.settings, contextMenuListener, client.api);
        aboutDialog = new About();
        setHelpWindowIcons();
        favoritesDialog = new FavoritesDialog(this, client.channelFavorites, contextMenuListener);
        GuiUtil.installEscapeCloseOperation(favoritesDialog);
        joinDialog = new JoinDialog(this);
        GuiUtil.installEscapeCloseOperation(joinDialog);
        EmoteContextMenu.setEmoteManager(emoticons);
        emotesDialog = new EmotesDialog(this, emoticons, this, contextMenuListener);
        GuiUtil.installEscapeCloseOperation(emotesDialog);
        
        // Tray/Notifications
        trayIcon = new TrayIconManager();
        trayIcon.addActionListener(new TrayMenuListener());
        if (client.settings.getBoolean("trayIconAlways")) {
            trayIcon.setIconVisible(true);
        }
        notificationWindowManager = new NotificationWindowManager<>(this);
        notificationWindowManager.setNotificationActionListener(new MyNotificationActionListener());
        notificationManager = new NotificationManager(this, client.settings, client.addressbook, client.channelFavorites);

        // Channels/Chat output
        styleManager = new StyleManager(client.settings);
        channels = new Channels(this,styleManager, contextMenuListener);
        channels.getComponent().setPreferredSize(new Dimension(600,300));
        add(channels.getComponent(), BorderLayout.CENTER);
        channels.setChangeListener(new ChannelChangeListener());
        
        dockedDialogs = new DockedDialogManager(this, channels, client.settings);
        
        highlightedMessages = new HighlightedMessages(this, styleManager,
                Language.getString("highlightedDialog.title"),
                Language.getString("menubar.dialog.highlightedMessages"),
                Language.getString("highlightedDialog.info"),
                contextMenuListener, dockedDialogs, "highlightDock");
        ignoredMessages = new HighlightedMessages(this, styleManager,
                Language.getString("ignoredDialog.title"),
                Language.getString("menubar.dialog.ignoredMessages"),
                Language.getString("ignoredDialog.info"),
                contextMenuListener, dockedDialogs, "ignoreDock");
        
        followerDialog = new FollowersDialog(FollowersDialog.Type.FOLLOWERS,
                this, client.api, contextMenuListener, dockedDialogs);
        subscribersDialog = new FollowersDialog(FollowersDialog.Type.SUBSCRIBERS,
                this, client.api, contextMenuListener, dockedDialogs);
        channelInfoDialog = new ChannelInfoDialog(this, dockedDialogs);
        channelInfoDialog.addContextMenuListener(contextMenuListener);
        adminDialog = new AdminDialog(this, client.api, dockedDialogs);
        liveStreamsDialog = new LiveStreamsDialog(this, contextMenuListener, client.channelFavorites, client.settings, dockedDialogs);
        setLiveStreamsWindowIcons();
        
        // Some newer stuff
        addressbookDialog = new AddressbookDialog(this, client.addressbook);
        srl = new SRL(this, client.speedrunsLive, contextMenuListener);
        livestreamerDialog = new LivestreamerDialog(this, linkLabelListener, client.settings);
        updateDialog = new UpdateDialog(this, linkLabelListener, client.settings,() -> exit());
        //newsDialog = new NewsDialog(this, client.settings);
        
        client.settings.addSettingChangeListener(new MySettingChangeListener());
        client.settings.addSettingsListener(new MySettingsListener());
        
        streamChat = new StreamChat(this, styleManager, contextMenuListener,
            client.settings.getBoolean("streamChatBottom"), dockedDialogs);
        StreamChatContextMenu.client = client;
        
        moderationLog = new ModerationLog(this, dockedDialogs);
        autoModDialog = new AutoModDialog(this, client.api, client, dockedDialogs);
        eventLog = new EventLog(this);
        EventLog.setMain(eventLog);
        
        //this.getContentPane().setBackground(new Color(0,0,0,0));

        if (client.settings.getBoolean("initSettingsDialog")) {
            getSettingsDialog(null);
        }
        
        // Main Menu
        MainMenuListener menuListener = new MainMenuListener();
        menu = new MainMenu(menuListener, menuListener);
        setJMenuBar(menu);

        addListeners();
        pack();
        
        // Load some stuff
        client.api.setUserId(client.settings.getString("username"), client.settings.getString("userid"));
        //client.api.requestCheerEmoticons(false);
        // TEST
//        client.api.getUserIdAsap(null, "m_tt");
//        client.api.getCheers("m_tt", false);
        if (client.settings.getBoolean("bttvEmotes")) {
            client.bttvEmotes.requestEmotes(BTTVEmotes.GLOBAL, false);
        }
        OtherBadges.requestBadges(r -> client.usericonManager.setThirdPartyIcons(r), false);
        ChattyMisc.request(() -> {
            SwingUtilities.invokeLater(() -> {
                updateSmilies();
            });
        });
        
        // Window states
        windowStateManager = new WindowStateManager(this, client.settings);
        windowStateManager.addWindow(this, "main", true, true);
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
        windowStateManager.addWindow(userInfoDialog.getDummyWindow(), "userInfo", true, false);
        windowStateManager.addWindow(autoModDialog, "autoMod", true, true);
        windowStateManager.addWindow(eventLog, "eventLog", true, true);
        
        if (System.getProperty("java.version").equals("1.8.0_161")
                || System.getProperty("java.version").equals("1.8.0_162")) {
            GuiUtil.installTextComponentFocusWorkaround();
        }
        
        ToolTipManager.sharedInstance().setInitialDelay(555);
        ToolTipManager.sharedInstance().setDismissDelay(20*1000);
        
        TransparencyManager.init(channels);
        
        guiCreated = true;
    }
    
    public void setWindowAttached(Window window, boolean attached) {
        windowStateManager.setWindowAttached(window, attached);
    }
    
    protected void popoutCreated(Window popout) {
        hotkeyManager.registerPopout(popout);
    }
    
    public void getSettingsDialog(Consumer<SettingsDialog> action) {
        GuiUtil.edt(() -> {
            SettingsDialog.get(this, action);
        });
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
        client.command(channels.getLastActiveChannel().getRoom(), command, parameter);
    }
    
    private void hotkeyCommandAnon(String command) {
        Channel channel = channels.getActiveChannel();
        User selectedUser = channel.getSelectedUser();
        String selectedUserName = selectedUser != null ? selectedUser.getName() : "";
        Parameters p = Parameters.create(null);
        p.put("selectedUserName", selectedUserName);
        client.anonCustomCommand(channel.getRoom(), command, p);
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
    private void addMenuAction(String id, String label, int mnemonic, Action action) {
        action.putValue(Action.NAME, Language.getString("menubar."+id));
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
        addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (client.settings.getLong("inputFocus") == 1) {
                    channels.setInitialFocus();
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });
        
        hotkeyManager.registerAction("custom.command", "Custom Command",
                "Enter the name of a Custom Command (only the name), as added to the Custom Commands list under \"Commands\". This is useful when you have it added to the Custom Commands list already anyway (for example if you want to use it in another context as well). Otherwise the \"Anon. Custom Command\" may be more convenient.", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommand(e.getActionCommand(), null, false);
            }
        });
        
        hotkeyManager.registerAction("custom.anonCommand", "Anon. Custom Command",
                "Enter a Custom Command directly (for example \"/echo $datetime()\" to output the current time as an info message). This is an anonymous Custom Command because it is not added to the Custom Commands list and thus no name is specified for it. It can only be executed via this hotkey.", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommandAnon(e.getActionCommand());
            }
        });

        hotkeyManager.registerAction("tabs.next", "Tabs: Switch to next tab", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.switchToNextTab();
            }
        });
        
        hotkeyManager.registerAction("tabs.previous", "Tabs: Switch to previous tab", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.switchToPreviousTab();
            }
        });
        
        hotkeyManager.registerAction("tabs.switch", "Tabs: Switch to tab (specify index or #chan)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String target = e.getActionCommand();
                try {
                    int index = Integer.parseInt(target);
                    channels.switchToTabIndex(index - 1);
                }
                catch (NumberFormatException ex) {
                    channels.switchToTabId(target);
                }
            }
        });
        
        hotkeyManager.registerAction("tabs.close", "Tabs: Close tab/popout", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.getActiveContent().remove();
            }
        });
        

        
        addMenuAction("dialog.search", "Dialog: Open Search Dialog",
                KeyEvent.VK_F, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openSearchDialog();
            }
        });
        
        addMenuAction("dialog.addressbook", "Dialog: Addressbook (toggle)",
                KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleAddressbook();
            }
        });
        
        addMenuAction("dialog.autoModDialog", "Dialog: AutoMod Dialog (toggle)",
                KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleAutoModDialog();
            }
        });
        
        addMenuAction("dialog.moderationLog", "Dialog: Moderation Log (toggle)",
                KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleModerationLog();
            }
        });
        
        addMenuAction("dialog.channelInfo", "Dialog: Channel Info Dialog (toggle)",
                KeyEvent.VK_C, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleChannelInfoDialog();
            }
        });
        
        addMenuAction("dialog.channelAdmin", "Dialog: Channel Admin Dialog (toggle)",
                KeyEvent.VK_A, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleChannelAdminDialog();
            }
        });
        
        addMenuAction("dialog.eventLog", "Dialog: Event log (toggle)",
                KeyEvent.VK_A, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleEventLog();
            }
        });
        
        addMenuAction("dialog.toggleEmotes", "Dialog: Emotes Dialog (toggle)",
                KeyEvent.VK_E, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleEmotesDialog();
            }
        });

        addMenuAction("dialog.highlightedMessages", "Dialog: Highlighted Messages (toggle)",
                KeyEvent.VK_H, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleHighlightedMessages();
            }
        });
        
        addMenuAction("dialog.ignoredMessages", "Dialog: Ignored Messages (toggle)",
                KeyEvent.VK_I, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleIgnoredMessages();
            }
        });
        
        addMenuAction("dialog.streams", "Dialog: Live Channels Dialog (toggle)",
                KeyEvent.VK_L, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleLiveStreamsDialog();
            }
        });
        
        addMenuAction("dialog.followers", "Dialog: Followers List (toggle)",
                KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFollowerDialog();
            }
        });
        
        addMenuAction("dialog.subscribers", "Dialog: Subscriber List (toggle)",
                KeyEvent.VK_UNDEFINED, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSubscriberDialog();
            }
        });
        
        addMenuAction("dialog.livestreamer", "Dialog: "+Language.getString("menubar.dialog.livestreamer")+" (toggle)", KeyEvent.VK_L, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (livestreamerDialog.isVisible()) {
                    livestreamerDialog.close();
                }
                else {
                    livestreamerDialog.open(null, null);
                }
            }
        });

        addMenuAction("dialog.joinChannel", "Dialog: Join Channel",
                KeyEvent.VK_J, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openJoinDialog();
            }
        });
        
        addMenuAction("dialog.favorites", "Dialog: Favorites / History (toggle)",
                KeyEvent.VK_F, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFavoritesDialog();
            }
        });
        
        addMenuAction("dialog.updates", "Dialog: Updates",
                KeyEvent.VK_U, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openUpdateDialog();
            }
        });
        
        addMenuAction("about", "Open Help", KeyEvent.VK_H,
                new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openHelp("");
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
        
        hotkeyManager.registerAction("commercial.120", "Run commercial (120s)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                runCommercial(120);
            }
        });
        
        hotkeyManager.registerAction("commercial.180", "Run commercial (180s)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                runCommercial(180);
            }
        });

        addMenuAction("stream.addhighlight", "Stream: Add Stream Highlight", KeyEvent.VK_A, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommand("addstreamhighlight", null, false);
            }
        });
        
        hotkeyManager.registerAction("stream.addmarker", "Stream: Add Stream Marker", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                hotkeyCommand("marker", null, false);
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
        
        addMenuAction("window.toggleUserlist", "Window: Toggle Userlist", KeyEvent.VK_U, new AbstractAction() {

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
                KeyEvent.VK_O, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openStreamChat();
            }
        });
        
        hotkeyManager.registerAction("dialog.toggleTransparency", "Window: Toggle Transparency", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TransparencyManager.toggleTransparent();
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
                KeyEvent.VK_UNDEFINED, new AbstractAction() {

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
                
                startUpdatingState();
                
                windowStateManager.loadWindowStates();
                windowStateManager.setWindowPosition(MainGui.this);
                setVisible(true);
                
                // If not invokeLater() seemed to move dialogs on start when
                // maximized and not restoring location due to off-screen
                SwingUtilities.invokeLater(() -> {
                    windowStateManager.setAttachedWindowsEnabled(client.settings.getBoolean("attachedWindows"));
                });
                
                /**
                 * Init channels and load previous layout after main window is
                 * visible, so that popouts from the layout open afterwards and
                 * have the correct order in the taskbar.
                 */
                channels.init();
                /**
                 * Don't update titles before channels is properly initialized.
                 */
                state.init();

                // Should be done when the main window is already visible, so
                // it can be centered on it correctly, if that is necessary
                reopenWindows();
                
                //newsDialog.autoRequestNews(true);
                
                client.init();
                
                /**
                 * Do it here as well as with invokeLater to minimize possible
                 * issues, for example with positioning windows that depend on
                 * the main window showing (like connect dialog, but probably
                 * others as well).
                 */
                switch ((int) client.settings.getLong("minimizeOnStart")) {
                    case 1:
                        SwingUtilities.invokeLater(() -> {
                            minimize(true);
                        });
                        break;
                    case 2:
                        SwingUtilities.invokeLater(() -> {
                            minimizeToTray(true);
                        });
                        break;
                }
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
        final boolean hide = getJMenuBar() != null;
        
        //menu.setVisible(!hide);
        if (hide) {
            setJMenuBar(null);
        } else {
            setJMenuBar(menu);
            /**
             * Seems like adding the menubar adds the default F10 hotkey again
             * (that opens the menu), so refresh custom hotkeys in case one of
             * them uses F10.
             */
            hotkeyManager.refreshHotkeys(getRootPane());
        }
        revalidate();
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
        channels.getDock().showHiddenWindows();
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
        updateFollowerDialogs();
        
        // Set window maximized state
        if (client.settings.getBoolean("maximized")) {
            setExtendedState(MAXIMIZED_BOTH);
        }
        // Anything using highlighting format should be loaded after this
        updateMatchingPresets();
        updateHistoryRange();
        updateHistoryVerticalZoom();
        updateNotificationSettings();
        updateChannelsSettings();
        updateHighlightNextMessages();
        updateHighlightIncludeAllMatches();
        repeatMsg.loadSettings();
        
        msgColorManager.loadFromSettings();
        notificationManager.loadFromSettings();
        client.usericonManager.init();
        
        // This should be done before updatePopoutSettings() because that method
        // will delete the attributes correctly depending on the setting
        channels.setPopoutAttributes(client.settings.getList("popoutAttributes"));
        updatePopoutSettings();
        
        loadCommercialDelaySettings();
        UrlOpener.setSettings(client.settings);
        UrlOpener.setCustomCommandEnabled(client.settings.getBoolean("urlCommandEnabled"));
        UrlOpener.setCustomCommand(client.settings.getString("urlCommand"));
        
        favoritesDialog.setSorting((int)client.settings.getLong("favoritesSorting"));
        
        updateCustomContextMenuEntries();
        
        client.api.setToken(client.settings.getString("token"));
        client.api.setLocalUserId(client.settings.getString("userid"));
        if (client.settings.getList("scopes").isEmpty()) {
            client.api.checkToken();
        }
        
        localEmotes.init();
        emoticons.setLocalEmotes(localEmotes.getData());
        emoticons.setIgnoredEmotes(client.settings.getList("ignoredEmotes"));
        emoticons.loadFavoritesFromSettings(client.settings);
        client.api.getEmotesBySets(emoticons.getFavoritesNonGlobalEmotesets());
        emoticons.loadCustomEmotes();
        emoticons.addEmoji(client.settings.getString("emoji"));
        emoticons.setCheerState(client.settings.getString("cheersType"));
        emoticons.setCheerBackground(HtmlColors.decode(client.settings.getString("backgroundColor")));
        
        userInfoDialog.setTimestampFormat(styleManager.makeTimestampFormat("userDialogTimestamp"));
        userInfoDialog.setFontSize(client.settings.getLong("dialogFontSize"));
        UserNotes.init(client.api, client.settings);
        
        hotkeyManager.setGlobalHotkeysEnabled(client.settings.getBoolean("globalHotkeysEnabled"));
        hotkeyManager.loadFromSettings(client.settings);
        
        streamChat.setMessageTimeout((int)client.settings.getLong("streamChatMessageTimeout"));
        
        emotesDialog.setEmoteScale((int)client.settings.getLong("emoteScaleDialog"));
        emotesDialog.setEmoteImageType(Emoticon.makeImageType(client.settings.getBoolean("animatedEmotes")));
        emotesDialog.setHiddenEmotesets(client.settings.getList("emoteHiddenSets"));
        emotesDialog.setCloseOnDoubleClick(client.settings.getBoolean("closeEmoteDialogOnDoubleClick"));
        
        adminDialog.setStatusHistorySorting(client.settings.getString("statusHistorySorting"));
        
        Sound.setDeviceName(client.settings.getString("soundDevice"));
        
        dockedDialogs.loadSettings();
        
        updateTokenScopes();
        
        FocusUpdates.set(client.settings);
        GifUtil.setSettings(client.settings);
        TransparencyManager.loadSettings(client.settings);
    }
    
    private static final String[] menuBooleanSettings = new String[]{
        "showJoinsParts", "ontop", "showModMessages", "attachedWindows",
        "simpleTitle", "globalHotkeysEnabled", "mainResizable", "streamChatResizable",
        "titleShowUptime", "titleShowViewerCount", "titleShowChannelState",
        "titleLongerUptime", "titleConnections"
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
        BatchAction.queue(highlighter, () -> {
            highlighter.update(StringUtil.getStringList(client.settings.getList("highlight")));
            highlighter.updateBlacklist(StringUtil.getStringList(client.settings.getList("highlightBlacklist")));

            @SuppressWarnings("unchecked") // Setting
            List<String> substitutesValue = client.settings.getList("matchingSubstitutes");
            highlighter.updateSubstitutes(Replacer2.create(substitutesValue));
            highlighter.setSubstitutitesDefault(client.settings.getBoolean("matchingSubstitutesEnabled"));
        });
    }
    
    private void updateIgnore() {
        BatchAction.queue(ignoreList, () -> {
            ignoreList.update(StringUtil.getStringList(client.settings.getList("ignore")));
            ignoreList.updateBlacklist(StringUtil.getStringList(client.settings.getList("ignoreBlacklist")));
        });
    }
    
    private void updateMatchingPresets() {
        @SuppressWarnings("unchecked") // Setting
        List<String> settingValue = client.settings.getList("matchingPresets");
        Map<String, CustomCommand> presets = Highlighter.HighlightItem.makePresets(settingValue);
        if (SettingsManager.switchedFromVersionBefore("0.18")) {
            for (String name : presets.keySet()) {
                if (name.equals("_global_highlight") || name.equals("_global_ignore")) {
                    EventLog.addSystemEvent("matching.presetsBlacklist");
                }
            }
        }
        Highlighter.HighlightItem.setGlobalPresets(presets);
        Highlighter.HighlightItem.setTwitchApi(client.api);
        updateHighlight();
        updateIgnore();
        updateFilter();
    }
    
    private void updateFilter() {
        BatchAction.queue(filter, () -> {
            filter.update(StringUtil.getStringList(client.settings.getList("filter")));
        });
    }
    
    private void updateCustomContextMenuEntries() {
        CommandMenuItems.customCommands = client.customCommands;
        CommandMenuItems.setCommands(CommandMenuItems.MenuType.CHANNEL, client.settings.getString("channelContextMenu"));
        CommandMenuItems.setCommands(CommandMenuItems.MenuType.USER, client.settings.getString("userContextMenu"));
        CommandMenuItems.setCommands(CommandMenuItems.MenuType.STREAMS, client.settings.getString("streamsContextMenu"));
        CommandMenuItems.setCommands(CommandMenuItems.MenuType.TEXT, client.settings.getString("textContextMenu"));
        CommandMenuItems.setCommands(CommandMenuItems.MenuType.ADMIN, client.settings.getString("adminContextMenu"));
        TextSelectionMenu.update();
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
    
    private void updateHighlightIncludeAllMatches() {
        highlighter.setIncludeAllTextMatches(client.settings.getBoolean("highlightMatchesAllEntries"));
    }
    
    private void updateNotificationSettings() {
        notificationWindowManager.setDisplayTime((int)client.settings.getLong("nDisplayTime"));
        notificationWindowManager.setMaxDisplayTime((int)client.settings.getLong("nMaxDisplayTime"));
        notificationWindowManager.keepOpenOnHover(client.settings.getBoolean("nKeepOpenOnHover"));
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
        userInfoDialog.aboutToSaveSettings();
        windowStateManager.saveWindowStates();
        client.settings.putList("popoutAttributes", channels.getPopoutAttributes());
        channels.saveLayout();
        TransparencyManager.saveSettings(client.settings);
    }
    
    public void setWindowPosition(Window window) {
        windowStateManager.setWindowPosition(window, getActiveWindow());
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
            openWindow(window);
        }
    }
    
    public void openWindow(Window window) {
        if (window == liveStreamsDialog) {
            openLiveStreamsDialog();
        }
        else if (window == highlightedMessages) {
            openHighlightedMessages(false);
        }
        else if (window == ignoredMessages) {
            openIgnoredMessages(false);
        }
        else if (window == channelInfoDialog) {
            openChannelInfoDialog();
        }
        else if (window == addressbookDialog) {
            openAddressbook(null);
        }
        else if (window == adminDialog) {
            openChannelAdminDialog();
        }
        else if (window == emotesDialog) {
            openEmotesDialog();
        }
        else if (window == followerDialog) {
            openFollowerDialog();
        }
        else if (window == subscribersDialog) {
            openSubscriberDialog();
        }
        else if (window == moderationLog) {
            openModerationLog();
        }
        else if (window == streamChat) {
            openStreamChat();
        }
        else if (window == autoModDialog) {
            openAutoModDialog();
        }
    }
    
    private void addLayout(String name) {
        if (StringUtil.isNullOrEmpty(name)) {
            name = JOptionPane.showInputDialog(rootPane, "Enter name to save the current layout:");
        }
        if (!StringUtil.isNullOrEmpty(name)) {
            boolean save = true;
            if (client.settings.mapGet("layouts", name) != null) {
                int result = JOptionPane.showConfirmDialog(rootPane, "Layout "+name+" already exists. Overwrite?", "Add layout", JOptionPane.YES_NO_OPTION);
                save = result == 0;
            }
            if (save) {
                client.settings.mapPut("layouts", name, channels.getDock().getLayout().toList());
                printSystem("Saved current layout as "+name+".");
            }
        }
        else {
            printSystem("Invalid layout name, didn't save layout.");
        }
    }
    
    private void removeLayout(String layoutName, boolean ask) {
        boolean remove = true;
        if (ask) {
            int result = JOptionPane.showConfirmDialog(rootPane, "Remove layout "+layoutName+"?", "Remove layout", JOptionPane.YES_NO_OPTION);
            remove = result == 0;
        }
        if (!layoutName.isEmpty()) {
            if (remove) {
                client.settings.mapRemove("layouts", layoutName);
                printSystem("Layout "+layoutName+" removed.");
            }
        }
        else {
            printSystem("Invalid layout name.");
        }
    }
    
    private void loadLayout(String layoutName, int options) {
        DockLayout layout = DockLayout.fromList((List) client.settings.mapGet("layouts", layoutName));
        if (layout != null) {
            channels.changeLayout(layout, options);
        }
        else {
            printSystem("Layout not found: "+layoutName);
        }
    }
    
    private void saveLayout(String layoutName, boolean ask) {
        if (!StringUtil.isNullOrEmpty(layoutName)) {
            boolean save = true;
            if (ask && client.settings.mapGet("layouts", layoutName) != null) {
                int result = JOptionPane.showConfirmDialog(rootPane, "Overwrite layout "+layoutName+"?", "Save layout", JOptionPane.YES_NO_OPTION);
                save = result == 0;
            }
            if (save) {
                client.settings.mapPut("layouts", layoutName, channels.getDock().getLayout().toList());
                printSystem("Saved current layout as "+layoutName+".");
            }
        }
        else {
            printSystem("Invalid layout name.");
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
    
    private void setChannelInfoDialogChannel(Channel channel) {
        channelInfoDialog.set(getStreamInfo(channel.getStreamName()));
    }
    
    private void updateChannelInfoDialog(StreamInfo info) {
        if (info == null) {
            setChannelInfoDialogChannel(channels.getLastActiveChannel());
        } else {
            channelInfoDialog.update(info);
        }
    }
    
    private void updateTokenDialog() {
        String username = client.settings.getString("username");
        String token = client.settings.getString("token");
        tokenDialog.update(username, token);
        tokenDialog.setForeignToken(client.settings.getBoolean("foreignToken"));
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
        liveStreamsDialog.setSorting(
                client.settings.getString("liveStreamsSorting"),
                client.settings.getBoolean("liveStreamsSortingFav")
        );
        liveStreamsDialog.setFiltering(client.settings.getBoolean("liveStreamsFavsOnly"));
    }
    
    private void updateFollowerDialogs() {
        followerDialog.setCompactMode(client.settings.getBoolean("followersCompact"));
        subscribersDialog.setCompactMode(client.settings.getBoolean("followersCompact"));
        followerDialog.setShowRegistered(client.settings.getBoolean("followersReg"));
        subscribersDialog.setShowRegistered(client.settings.getBoolean("followersReg"));
    }
    
    private void updateHistoryRange() {
        int range = (int)client.settings.getLong("historyRange");
        channelInfoDialog.setHistoryRange(range);
        liveStreamsDialog.setHistoryRange(range);
    }
    
    private void updateHistoryVerticalZoom() {
        boolean zoom = client.settings.getBoolean("historyVerticalZoom");
        channelInfoDialog.setHistoryVerticalZoom(zoom);
        liveStreamsDialog.setHistoryVerticalZoom(zoom);
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
    
    /**
     * Should be thread-safe.
     * 
     * @param input
     * @return 
     */
    public String replaceEmojiCodes(String input) {
        if (client.settings.getBoolean("emojiReplace")) {
            return emoticons.emojiReplace(input);
        }
        return input;
    }
    
    class MyActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            // text input
            Channel chan = channels.getChannelFromInput(event.getSource());
            if (chan != null) {
                client.textInput(chan.getRoom(), chan.getInputText(), null);
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
                int result = JOptionPane.showOptionDialog(tokenDialog,
                        "<html><body style='width:400px'>"
                                + Language.getString("login.removeLogin")
                                + "<ul>"
                                + "<li>"+Language.getString("login.removeLogin.revoke")
                                + "<li>"+Language.getString("login.removeLogin.remove")
                                + "</ul>"
                                + Language.getString("login.removeLogin.note"),
                        Language.getString("login.removeLogin.title"),
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{Language.getString("login.removeLogin.button.revoke"),
                            Language.getString("login.removeLogin.button.remove"),
                            Language.getString("dialog.button.cancel")},
                        Language.getString("login.removeLogin.button.revoke"));
                if (result == 0) {
                    client.api.revokeToken(client.settings.getString("token"));
                }
                if (result == 0 || result == 1) {
                    client.settings.setString("token", "");
                    client.settings.setBoolean("foreignToken", false);
                    client.settings.setString("username", "");
                    client.settings.setString("userid", "");
                    client.settings.listClear("scopes");
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
            }
        }
        
    }
    
    public void anonCustomCommand(Room room, CustomCommand command, Parameters parameters) {
        client.anonCustomCommand(room, command, parameters);
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
            switch (type) {
                case "help":
                    openHelp(ref);
                    break;
                case "help-settings":
                    openHelp("help-settings.html", ref);
                    break;
                case "help-commands":
                    openHelp("help-custom_commands.html", ref);
                    break;
                case "help-admin":
                    openHelp("help-admin.html", ref);
                    break;
                case "help-livestreamer":
                    openHelp("help-livestreamer.html", ref);
                    break;
                case "help-whisper":
                    openHelp("help-whisper.html", ref);
                    break;
                case "help-laf":
                    openHelp("help-laf.html", ref);
                    break;
                case "help-guide2":
                    openHelp("help-guide2.html", ref);
                    break;
                case "help-releases":
                    openHelp("help-releases.html", ref);
                    break;
                case "url":
                    UrlOpener.openUrlPrompt(MainGui.this, ref);
                    break;
                case "update":
                    if (ref.equals("show")) {
                        openUpdateDialog();
                    }
                    break;
                case "announcement":
                    if (ref.equals("show")) {
                        //newsDialog.showDialog();
                    }
                    break;
                case "settings":
                    getSettingsDialog(s -> {
                        if (!s.isVisible()) {
                            s.showSettings("show", ref);
                        }
                        else {
                            s.showPage(ref);
                        }
                    });
                    break;
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

        private final ElapsedTime lastEvent = new ElapsedTime();
        
        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd == null
                    || cmd.equals("show")
                    || cmd.equals("doubleClick")
                    || (cmd.equals("singleClick") && client.settings.getBoolean("singleClickTrayOpen"))) {
                /**
                 * Prevent hiding/showing too quickly, for example when both the
                 * mouse listener and the action listener fire (could also be
                 * platform dependent).
                 */
                if (lastEvent.millisElapsed(80)) {
                    lastEvent.set();
                    if (isMinimized()) {
                        makeVisible();
                    }
                    else {
                        minimizeToTray();
                    }
                }
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
                //newsDialog.showDialog();
            } else if (cmd.equals("settings")) {
                getSettingsDialog(s -> s.showSettings());
            } else if (cmd.equals("saveSettings")) {
                int result = JOptionPane.showOptionDialog(MainGui.this,
                        Language.getString("saveSettings.text")+"\n\n"+Language.getString("saveSettings.textBackup"),
                        Language.getString("saveSettings.title"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null,
                        new String[]{
                            Language.getString("dialog.button.save"),
                            Language.getString("saveSettings.saveAndBackup"),
                            Language.getString("dialog.button.cancel")
                        }, null);
                if (result == 0) {
                    List<FileManager.SaveResult> saveResult = client.saveSettings(false, true);
                    JOptionPane.showMessageDialog(MainGui.this,
                            Helper.makeSaveResultInfo(saveResult),
                            Language.getString("saveSettings.title"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
                else if (result == 1) {
                    List<FileManager.SaveResult> saveResult = client.saveSettings(false, true);
                    List<FileManager.SaveResult> backupResult = client.manualBackup();
                    JOptionPane.showMessageDialog(MainGui.this,
                            Helper.makeSaveResultInfo(saveResult)+"\nManual Backup:\n"+Helper.makeSaveResultInfo(backupResult),
                            Language.getString("saveSettings.title"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else if (cmd.equals("website")) {
                UrlOpener.openUrlPrompt(MainGui.this, Chatty.WEBSITE, true);
            } else if (cmd.equals("unhandledException")) {
                String[] array = new String[0];
                String a = array[1];
            } else if (cmd.equals("errorTest")) {
                Logger.getLogger(MainGui.class.getName()).log(Level.SEVERE, null, new ArrayIndexOutOfBoundsException(2));
            } else if (cmd.equals("addressbook")) {
                openAddressbook(null);
            } else if (cmd.equals("srlRaces")) {
                openSrlRaces();
            } else if (cmd.equals("srlRaceActive")) {
                srl.searchRaceWithEntrant(channels.getActiveChannel().getStreamName());
            } else if (cmd.startsWith("srlRace4")) {
                String stream = cmd.substring(8);
                if (!stream.isEmpty()) {
                    srl.searchRaceWithEntrant(stream);
                }
            } else if (cmd.equals("configureLogin")) {
                openTokenDialog();
            } else if (cmd.equals("addStreamHighlight")) {
                client.commandAddStreamHighlight(channels.getActiveChannel().getRoom(), null);
            } else if (cmd.equals("openStreamHighlights")) {
                client.commandOpenStreamHighlights(channels.getActiveChannel().getRoom());
            } else if (cmd.equals("srcOpen")) {
                client.speedruncom.openCurrentGame(channels.getActiveChannel());
            } else if (cmd.startsWith("room:")) {
                String channel = cmd.substring("room:".length());
                client.joinChannel(channel);
            } else if (cmd.equals("dialog.chattyInfo")) {
                openEventLog(1);
            } else if (cmd.equals("layouts.add")) {
                addLayout(null);
            } else if (cmd.startsWith("layouts.load.")) {
                String layoutName = cmd.substring("layouts.load.".length());
                loadLayout(layoutName, -1);
            } else if (cmd.startsWith("layouts.save.")) {
                String layoutName = cmd.substring("layouts.save.".length());
                saveLayout(layoutName, true);
            } else if (cmd.startsWith("layouts.remove.")) {
                String layoutName = cmd.substring("layouts.remove.".length());
                removeLayout(layoutName, true);
            } else if (cmd.equals("transparency")) {
                TransparencyDialog dialog = TransparencyDialog.instance(MainGui.this);
                dialog.setLocationRelativeTo(MainGui.this);
                dialog.setVisible(true);
                dialog.refresh();
            }
        }

        @Override
        public void menuSelected(MenuEvent e) {
            if (e.getSource() == menu.srlStreams) {
                ArrayList<String> popoutStreams = new ArrayList<>();
                String activeStream = channels.getActiveChannel().getStreamName();
                for (DockContent c : channels.getDock().getPopoutContents()) {
                    if (c instanceof Channels.DockChannelContainer) {
                        Channel channel = ((Channels.DockChannelContainer) c).getContent();
                        if (channel.getStreamName() != null
                                && !channel.getStreamName().equals(activeStream)) {
                            popoutStreams.add(channel.getStreamName());
                        }
                    }
                }
                menu.updateSrlStreams(activeStream, popoutStreams);
            } else if (e.getSource() == menu.view) {
                menu.updateCount(highlightedMessages.getNewCount(),
                        highlightedMessages.getDisplayedCount(),
                        ignoredMessages.getNewCount(),
                        ignoredMessages.getDisplayedCount());
            }
            else if (e.getSource() == menu.layoutsMenu) {
                menu.updateLayouts(Helper.getLayoutsFromSettings(client.settings));
            }
        }

        @Override
        public void menuDeselected(MenuEvent e) {
        }

        @Override
        public void menuCanceled(MenuEvent e) {
        }
    
    }
    
    public void updateRoom(Room room) {
        SwingUtilities.invokeLater(() -> {
            channels.updateRoom(room);
        });
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
        public void userMenuItemClicked(ActionEvent e, User user, String msgId, String autoModMsgId) {
            String cmd = e.getActionCommand();
            if (cmd.equals("userinfo")) {
                openUserInfoDialog(user, msgId, autoModMsgId);
            }
            else if (cmd.startsWith("userinfo.")) {
                String chan = cmd.substring(9);
                openUserInfoDialog(client.getUser(chan, user.getName()), null, null, true);
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
            else if (cmd.equals("notes")) {
                UserNotes.instance().showDialog(user, MainGui.this, null);
            }
            else if (cmd.startsWith("command")) {
                Parameters parameters = Parameters.create(user.getRegularDisplayNick());
                Helper.addUserParameters(user, msgId, autoModMsgId, parameters);
                customCommand(user.getRoom(), e, parameters);
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
                client.command(user.getRoom(), "automod_approve", autoModMsgId);
            } else if (cmd.equals("autoModDeny")) {
                client.command(user.getRoom(), "automod_deny", autoModMsgId);
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
         */
        @Override
        public void menuItemClicked(ActionEvent e) {
            Debugging.println("cmchan", "[cm] tab: %s chan: %s lastchan: %s",
                    channels.getMainActiveChannel(), channels.getActiveChannel(), channels.getLastActiveChannel());
            
            String cmd = e.getActionCommand();
            if (cmd.startsWith("historyRange")) {
                int range = Integer.parseInt(cmd.substring("historyRange".length()));
                // Change here as well, because even if it's the same value,
                // update may be needed. This will make it update twice often.
                //updateHistoryRange();
                client.settings.setLong("historyRange", range);
            }
            else if (cmd.startsWith("toggleVerticalZoom")) {
                boolean selected = ((JMenuItem) e.getSource()).isSelected();
                client.settings.setBoolean("historyVerticalZoom", selected);
            }
            else if (cmd.startsWith("highlightSource.")) {
                getSettingsDialog(s -> s.showSettings("selectHighlight", JSONValue.parse(cmd.substring("highlightSource.".length()))));
            }
            else if (cmd.startsWith("ignoreSource.")) {
                getSettingsDialog(s -> s.showSettings("selectIgnore", JSONValue.parse(cmd.substring("ignoreSource.".length()))));
            }
            else if (cmd.startsWith("msgColorSource.")) {
                getSettingsDialog(s -> s.showSettings("selectMsgColor", cmd.substring("msgColorSource.".length())));
            }
            else {
                nameBasedStuff(e, channels.getActiveChannel().getStreamName());
            }
        }
        
        @Override
        public void tabMenuItemClicked(ActionEvent e, DockContent content) {
            String cmd = e.getActionCommand();
            if (cmd.equals("closeChannel")) {
                content.remove();
            }
            else if (cmd.startsWith("tabsPosTab")) {
                long pos = Long.parseLong(cmd.substring("tabsPosTab".length()));
                getSettings().mapPut("tabsPos", content.getId(), pos);
                getSettings().setSettingChanged("tabsPos");
            }
            else if (cmd.startsWith("tabsPosType")) {
                long pos = Long.parseLong(cmd.substring("tabsPosType".length()));
                getSettings().mapPut("tabsPos", content.getId().substring(0, 1), pos);
                getSettings().setSettingChanged("tabsPos");
            }
            else if (cmd.equals("tabsSort")) {
                channels.sortContent(null);
            }
            else if (cmd.equals("tabsAutoSort")) {
                client.settings.toggleBoolean("tabsAutoSort");
            }
            else if (cmd.equals("popoutChannel")) {
                // TabContextMenu
                channels.popout(content, false);
            }
            else if (cmd.equals("popoutChannelWindow")) {
                // TabContextMenu
                channels.popout(content, true);
            }
            else if (cmd.startsWith("closeAllTabs")) {
                // TabContextMenu
                Collection<DockContent> chans = Channels.getCloseTabs(channels, content, client.settings.getBoolean("closeTabsSameType")).get(cmd);
                if (chans != null) {
                    for (DockContent c : chans) {
                        c.remove();
                    }
                }
            }
            else if (cmd.equals("tabsCloseSameType")) {
                client.settings.toggleBoolean("closeTabsSameType");
            }
            else {
                Channel channel = content instanceof Channels.DockChannelContainer
                                  ? ((Channels.DockChannelContainer)content).getContent()
                                  : null;
                if (channel != null) {
                    if (cmd.startsWith("command")) {
                        customCommand(channel.getRoom(), e,
                                Parameters.create(channel.getStreamName()));
                    }
                    else {
                        nameBasedStuff(e, channel.getStreamName());
                    }
                }
            }
        }
        
        /**
         * ChannelContextMenu, with Channel context.
         */
        @Override
        public void channelMenuItemClicked(ActionEvent e, Channel channel) {
            Debugging.println("cmchan", "[channelcm] tab: %s chan: %s lastchan: %s",
                    channels.getMainActiveChannel(), channels.getActiveChannel(), channels.getLastActiveChannel());
            
            String cmd = e.getActionCommand();
            if (cmd.equals("channelInfo")) {
                setChannelInfoDialogChannel(channel);
                openChannelInfoDialog();
            }
            else if (cmd.equals("channelAdmin")) {
                openChannelAdminDialog(channel.getStreamName());
            }
            else if (cmd.equals("closeChannel")) {
                client.closeChannel(channel.getChannel());
            }
            else if (cmd.equals("srcOpen")) {
                client.speedruncom.openCurrentGame(channel);
            }
            else if (cmd.startsWith("command")) {
                customCommand(channel.getRoom(), e,
                        Parameters.create(channel.getStreamName()));
            }
            else {
                nameBasedStuff(e, channel.getStreamName());
            }
        }

        @Override
        public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams) {
            streamStuff(e, streams);
            channelStuff(e, streams);
        }

        /**
         * Context menu event associated with a list of stream or channel names,
         * used in the channel favorites dialog.
         */
        @Override
        public void roomsMenuItemClicked(ActionEvent e, Collection<Room> rooms) {
            roomsStuff(e, rooms);
        }

        /**
         * Goes through the {@code StreamInfo} objects and adds the stream names
         * into a list, so it can be used by the more generic method.
         * 
         * @param e The event
         * @param streamInfos The list of {@code StreamInfo} objects associated
         * with this event
         * @see #roomsMenuItemClicked(ActionEvent, Collection)
         */
        @Override
        public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos) {
            String cmd = e.getActionCommand();
            String sorting = null;
            if (cmd.startsWith("sort_")) {
                sorting = cmd.substring("sort_".length());
            }
            if (sorting != null) {
                client.settings.setString("liveStreamsSorting", sorting);
            } else {
                Collection<String> streams = new ArrayList<>();
                for (StreamInfo info : streamInfos) {
                    streams.add(info.getCapitalizedName());
                }
                streamStuff(e, streams);
                channelStuff(e, streams);
            }
            if (cmd.equals("manualRefreshStreams")) {
                client.api.manualRefreshStreams();
                state.update(true);
            }
            if (cmd.equals("liveStreamsSettings")) {
                getSettingsDialog(s -> s.showSettings("show", "LIVE_STREAMS"));
            }
            if (cmd.equals("sortOption_favFirst")) {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
                client.settings.setBoolean("liveStreamsSortingFav", item.isSelected());
            }
            if (cmd.equals("filterOption_favsOnly")) {
                client.settings.setBoolean("liveStreamsFavsOnly", ((JCheckBoxMenuItem)e.getSource()).isSelected());
            }
            if (cmd.equals("favoriteGame")) {
                for (StreamInfo info : streamInfos) {
                    if (!StringUtil.isNullOrEmpty(info.getGame())) {
                        client.settings.setAdd("gameFavorites", info.getGame());
                    }
                }
                client.settings.setSettingChanged("gameFavorites");
            }
            if (cmd.equals("unfavoriteGame")) {
                for (StreamInfo info : streamInfos) {
                    if (!StringUtil.isNullOrEmpty(info.getGame())) {
                        client.settings.listRemove("gameFavorites", info.getGame());
                    }
                }
                client.settings.setSettingChanged("gameFavorites");
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
            channelStuff(e, list);
        }
        
        /**
         * Any commands that are equal to these Strings is supposed to have a
         * stream parameter.
         */
        private final Set<String> streamCmds = new HashSet<>(
                Arrays.asList("profile", "join", "raidchannel"));
        
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
        private void roomsStuff(ActionEvent e, Collection<Room> rooms) {
            Collection<String> channels = new ArrayList<>();
            Collection<String> streams = new ArrayList<>();
            for (Room room : rooms) {
                channels.add(room.getChannel());
                if (room.hasStream()) {
                    streams.add(room.getStream());
                }
            }
            channelStuff(e, channels);
            streamStuff(e, streams);
        }
        
        private void channelStuff(ActionEvent e, Collection<String> channels) {
            String cmd = e.getActionCommand();
            TwitchUrl.removeInvalidStreams(channels);
            if (cmd.equals("join")) {
                makeVisible();
                client.joinChannels(new HashSet<>(channels));
            }
            else if (cmd.equals("favoriteChannel")) {
                for (String chan : channels) {
                    client.channelFavorites.addFavorite(chan);
                }
                /**
                 * Manually update data when changed from the outside, instead
                 * of just using ChannelFavorites change listener (since changes
                 * through the favoritesDialog itself get handled differently
                 * but would also cause additional updates).
                 */
                favoritesDialog.updateData();
            }
            else if (cmd.equals("unfavoriteChannel")) {
                for (String chan : channels) {
                    client.channelFavorites.removeFavorite(chan);
                }
                favoritesDialog.updateData();
            }
        }
        
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
                    || cmd.equals("streamPopoutOld") || cmd.equals("profile")
                    || cmd.equals("streamChat")) {
                List<String> urls = new ArrayList<>();
                for (String stream : streams) {
                    String url;
                    switch (cmd) {
                        case "stream":
                            url = TwitchUrl.makeTwitchStreamUrl(stream);
                            break;
                        case "profile":
                            url = TwitchUrl.makeTwitchProfileUrl(stream);
                            break;
                        case "streamPopout":
                            url = TwitchUrl.makeTwitchPlayerUrl(stream);
                            break;
                        case "streamChat":
                            url = TwitchUrl.makeTwitchChatUrl(stream);
                            break;
                        default:
                            url = TwitchUrl.makeTwitchStreamUrl(stream);
                            break;
                    }
                    urls.add(url);
                }
                if (urls.size() > 1) {
                    UrlOpener.openUrlsPrompt(getActiveWindow(), urls, true);
                }
                else {
                    UrlOpener.openUrlsPrompt(getActiveWindow(), urls);
                }
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
                    livestreamerDialog.open(StringUtil.toLowerCase(stream), quality);
                }
            } else if (cmd.equals("showChannelEmotes")) {
                if (firstStream != null) {
                    // Should add the stream to be requested
                    openEmotesDialogChannelEmotes(StringUtil.toLowerCase(firstStream));
                    // Request immediately in this case
                    client.api.requestEmotesNow();
                }
            } else if (cmd.equals("raidchannel")) {
                if (firstStream != null && streams.size() == 1) {
                    String ownChannel = "#"+client.getUsername();
                    Channel chan = channels.getExistingChannel(ownChannel);
                    if (chan == null) {
                        printSystem("Must have your own channel open to raid.");
                    }
                    else {
                        client.command(chan.getRoom(), "raid", StringUtil.toLowerCase(firstStream));
                        printSystem(String.format("Trying to raid %s from %s..",
                                firstStream, ownChannel));
                    }
                } else {
                    printSystem("Can't raid more than one channel.");
                }
            } else if (cmd.equals("copy") && !streams.isEmpty()) {
                MiscUtil.copyToClipboard(StringUtil.join(streams, ", "));
            } else if (cmd.startsWith("command")) {
                /**
                 * For example for Live Streams/Favorites context menu, so it
                 * makes sense to use the last active channel, since this might
                 * be triggered from a Chatty window that doesn't represent a
                 * single joined room.
                 */
                customCommand(channels.getLastActiveChannel().getRoom(), e, Parameters.create(StringUtil.join(streams, " ")));
            } else if (cmd.startsWith("toggleBoolean_")) {
                // Added here so it can be triggered from various menus
                String setting = cmd.substring("toggleBoolean_".length());
                client.settings.setBoolean(setting, !client.settings.getBoolean(setting));
            }
        }

        @Override
        public void emoteMenuItemClicked(ActionEvent e, CachedImage<Emoticon> emoteImage) {
            Emoticon emote = emoteImage.getObject();
            String url = null;
            if (e.getActionCommand().equals("code")) {
                channels.getActiveChannel().insertText(emote.code, true);
            } else if (e.getActionCommand().equals("codeEmoji")) {
                channels.getActiveChannel().insertText(emote.stringId, true);
            } else if (e.getActionCommand().equals("cheer")) {
                url = "https://help.twitch.tv/customer/portal/articles/2449458";
            } else if (e.getActionCommand().equals("emoteImage")) {
                url = emoteImage.getLoadedFrom();
            } else if (e.getActionCommand().equals("ffzlink")) {
                url = TwitchUrl.makeFFZUrl();
            } else if (e.getActionCommand().equals("emoteId")) {
                url = TwitchUrl.makeEmoteUrl(emote.type, emote.stringId);
            } else if (e.getActionCommand().equals("emoteCreator")) {
                if (emote.type == Emoticon.Type.FFZ) {
                    url = TwitchUrl.makeFFZUserUrl(emote.creator);
                } else if (emote.creator.equals("Twemoji")) {
                    url = "https://github.com/twitter/twemoji";
                }
            } else if (e.getActionCommand().equals("twitchturbolink")) {
                url = TwitchUrl.makeTwitchTurboUrl();
            } else if (e.getActionCommand().equals("bttvlink")) {
                url = TwitchUrl.makeBttvUrl();
            } else if (e.getActionCommand().equals("seventvlink")) {
                url = "https://7tv.app/";
            } else if (e.getActionCommand().equals("emoteDetails")) {
                openEmotesDialogEmoteDetails(emote);
            }
            else if (e.getActionCommand().equals("ignoreEmote")) {
                IgnoredEmotes.Item item = EmoteSettings.showIgnoredEmoteEditDialog(
                        MainGui.this, getActiveWindow(), emote, emoticons.getIgnoredEmoteMatches(emote));
                if (item != null) {
                    emoticons.setEmoteIgnored(emote, item.context, client.settings);
                    emotesDialog.update();
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
            else if (e.getActionCommand().equals("addCustomLocalEmote")) {
                localEmotes.add(emote);
            }
            else if (e.getActionCommand().equals("removeCustomLocalEmote")) {
                localEmotes.remove(emote);
            }
            if (emote.hasStreamSet()) {
                nameBasedStuff(e, emote.getStream());
            }
            if (url != null) {
                UrlOpener.openUrlPrompt(getActiveWindow(), url, true);
            }
        }

        @Override
        public void usericonMenuItemClicked(ActionEvent e, CachedImage<Usericon> usericonImage) {
            Usericon usericon = usericonImage.getObject();
            if (e.getActionCommand().equals("usericonUrl")) {
                if (!usericon.metaUrl.isEmpty()) {
                    UrlOpener.openUrlPrompt(MainGui.this, usericon.metaUrl);
                }
            }
            else if (e.getActionCommand().equals("copyBadgeType")) {
                MiscUtil.copyToClipboard(usericon.badgeType.toString());
            }
            else if (e.getActionCommand().startsWith("addUsericonOfBadgeType")) {
                getSettingsDialog(s -> s.showSettings(e.getActionCommand(), usericon));
            }
            else if (e.getActionCommand().startsWith("hideUsericonOfBadgeType")) {
                if (client.usericonManager.hideBadge(usericon)) {
                    JOptionPane.showMessageDialog(rootPane, "Badges of type '"+usericon.readableLenientType()+"' will not show up in new messages.\n\nSee: <Main - Settings - Badges - View Hidden Badges>");
                }
                else {
                    JOptionPane.showMessageDialog(rootPane, "Badges of type '"+usericon.readableLenientType()+"' are already hidden.\n\nSee: <Main - Settings - Badges - View Hidden Badges>");
                }
            }
            else if (e.getActionCommand().equals("badgeImage")) {
                UrlOpener.openUrlPrompt(getActiveWindow(), usericonImage.getLoadedFrom(), true);
            }
        }
        
        @Override
        public void textMenuItemClick(ActionEvent e, String selected) {
            if (e.getActionCommand().startsWith("command")) {
                Room room = channels.getLastActiveChannel().getRoom();
                Parameters parameters = Parameters.create(selected);
                parameters.put("msg", selected);
                customCommand(room, e, parameters);
            }
        }
        
        private void customCommand(Room room, ActionEvent e, Parameters parameters) {
            CommandActionEvent ce = (CommandActionEvent)e;
            CustomCommand command = ce.getCommand();
            client.anonCustomCommand(room, command, parameters);
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
            updateChannelInfoDialog(null);
            emotesDialog.updateStream(channels.getLastActiveChannel().getStreamName());
            moderationLog.setChannel(channels.getLastActiveChannel().getStreamName());
            if (!openedFirstChannel
                    && channels.getLastActiveChannel().getType() == Channel.Type.CHANNEL) {
                openedFirstChannel = true;
                if (adminDialog.isVisible()) {
                    openChannelAdminDialog();
                }
                autoModDialog.setStream(channels.getActiveChannel().getStreamName());
                if (followerDialog.isVisible()) {
                    openFollowerDialog();
                }
            }
            dockedDialogs.activeContentChanged();
        }
    }
    
    private class MyUserListener implements UserListener {
        
        @Override
        public void userClicked(User user, String msgId, String autoModMsgId, MouseEvent e) {
            if (e == null || (!e.isControlDown() && !e.isAltDown())) {
                openUserInfoDialog(user, msgId, autoModMsgId);
                return;
            }
            String command = client.settings.getString("commandOnCtrlClick").trim();
            if (e.isControlDown() && command.length() > 1) {
                CustomCommand customCommand;
                Parameters parameters = Parameters.create(user.getRegularDisplayNick());
                Helper.addUserParameters(user, msgId, autoModMsgId, parameters);
                if (command.contains(" ")) {
                    // Assume that something containing a space is direct Custom Command
                    customCommand = CustomCommand.parse(command);
                } else {
                    // Just a command name (old format)
                    if (!command.startsWith("/")) {
                        // Need to add since not calling client.command(), so
                        // it would just be output to chat (but could just be
                        // a command name)
                        command = "/"+command;
                    }
                    customCommand = CustomCommand.createDefault(command);
                }
                client.anonCustomCommand(user.getRoom(), customCommand, parameters);
            } else if (!e.isAltDown()) {
                openUserInfoDialog(user, msgId, autoModMsgId);
            }
        }

        @Override
        public void emoteClicked(Emoticon emote, MouseEvent e) {
            if (e.isControlDown()) {
                insert(Emoticons.toWriteable(emote.code), true);
            } else {
                openEmotesDialogEmoteDetails(emote);
            }
        }

        @Override
        public void usericonClicked(Usericon usericon, MouseEvent e) {
            if (!usericon.metaUrl.isEmpty()) {
                UrlOpener.openUrlPrompt(MainGui.this, usericon.metaUrl);
            }
        }

        @Override
        public void linkClicked(Channel channel, String link) {
            if (link.startsWith("join.")) {
                String c = link.substring("join.".length());
                client.joinChannel(c);
            }
        }
        
    }
    
    private class MyNotificationActionListener implements NotificationActionListener<NotificationWindowData> {

        /**
         * Right-clicked on a notification.
         * 
         * @param data 
         */
        @Override
        public void notificationAction(NotificationWindowData data) {
            if (data != null) {
                Notification notification = data.notification;
                String channel = data.channel;
                if (client.settings.getBoolean("liveStreamsNotificationAction")
                        && notification != null
                        && notification.type == Notification.Type.STREAM_STATUS) {
                    StreamInfo status = client.api.getCachedStreamInfo(Helper.toStream(channel));
                    if (status != null) {
                        liveStreamsDialog.handleStreamsAction(Arrays.asList(new StreamInfo[]{status}), true);
                    }
                }
                else {
                    makeVisible();
                    client.joinChannel(channel);
                }
            }
        }
    }
    
    public UserListener getUserListener() {
        return userListener;
    }
    
    public List<UsercolorItem> getUsercolorData() {
        return client.usercolorManager.getData();
    }

    public void setUsercolorData(List<UsercolorItem> data) {
        client.usercolorManager.setData(data);
    }
    
    public List<MsgColorItem> getMsgColorData() {
        return msgColorManager.getData();
    }
    
    public void setMsgColorData(List<MsgColorItem> data) {
        msgColorManager.setData(data);
    }
    
    public List<Usericon> getUsericonData() {
        return client.usericonManager.getCustomData();
    }
    
    public List<Usericon> getHiddenBadgesData() {
        return client.usericonManager.getHiddenBadgesData();
    }
    
    public Set<String> getTwitchBadgeTypes() {
        return client.usericonManager.getTwitchBadgeTypes();
    }
    
    public void setUsericonData(List<Usericon> data) {
        client.usericonManager.setCustomData(data);
    }
    
    public void setHiddenBadgesData(List<Usericon> data) {
        client.usericonManager.setHiddenBadgesData(data);
    }
    
    public List<Notification> getNotificationData() {
        return notificationManager.getData();
    }
    
    public void setNotificationData(List<Notification> data) {
        notificationManager.setData(data);
    }
    
    /**
     * Add commands related to the GUI.
     */
    public void addGuiCommands() {
        client.commands.addEdt("settings", p -> {
            getSettingsDialog(s -> s.showSettings());
        });
        client.commands.addEdt("customEmotes", p -> {
            printLine(emoticons.getCustomEmotesInfo());
        });
        client.commands.addEdt("reloadCustomEmotes", p -> {
            printLine("Reloading custom emotes from file..");
            emoticons.loadCustomEmotes();
            printLine(emoticons.getCustomEmotesInfo());
        });
        client.commands.addEdt("livestreams", p -> {
            openLiveStreamsDialog();
        });
        client.commands.addEdt("channelAdmin", p -> {
            openChannelAdminDialog();
        });
        client.commands.addEdt("channelInfo", p -> {
            openChannelInfoDialog();
        });
        client.commands.addEdt("userinfo", p -> {
            String channel = p.getChannel();
            String parameter = StringUtil.trim(p.getArgs());
            if (StringUtil.isNullOrEmpty(parameter)) {
                parameter = client.settings.getString("username");
            }
            String[] split = parameter.split(" ");
            String username = split[0];
            if (split.length > 1) {
                channel = Helper.toChannel(split[1]);
            }
            if (!Helper.isValidChannelStrict(channel)) {
                printSystem("Invalid channel: "+channel);
            }
            else if (!Helper.isValidStream(username)) {
                printSystem("Invalid username: "+username);
            }
            else {
                User user = client.getUser(channel, username);
                openUserInfoDialog(user, null, null);
            }
        });
        client.commands.addEdt("search", p -> {
            openSearchDialog();
        });
        client.commands.addEdt("insert", p -> {
            insert(p.getArgs(), false);
        });
        client.commands.addEdt("insertWord", p -> {
            insert(p.getArgs(), true);
        });
        client.commands.addEdt("openUrl", p -> {
            if (!UrlOpener.openUrl(p.getArgs())) {
                printLine("Failed to open URL (none specified or invalid).");
            }
        });
        client.commands.addEdt("openUrlPrompt", p -> {
            // Could do in invokeLater() so command isn't visible in input box
            // while the dialog is open, but probably doesn't matter since this
            // is mainly for custom commands put in a context menu anyway.
            if (!UrlOpener.openUrlPrompt(getActiveWindow(), p.getArgs(), true)) {
                printLine("Failed to open URL (none specified or invalid).");
            }
        });
        client.commands.addEdt("openFile", p -> {
            MiscUtil.openFile(p.getArgs(), getActiveWindow());
        });
        client.commands.addEdt("openFilePrompt", p -> {
            MiscUtil.openFilePrompt(p.getArgs(), getActiveWindow());
        });
        client.commands.addEdt("openFollowers", p -> {
            openFollowerDialog();
        });
        client.commands.addEdt("openSubscribers", p -> {
            openSubscriberDialog();
        });
        client.commands.addEdt("openRules", p -> {
            // Chat rules API removed, but keep this for now
        });
        client.commands.addEdt("openStreamChat", p -> {
            openStreamChat();
        });
        client.commands.addEdt("clearStreamChat", p -> {
            streamChat.clear();
        });
        client.commands.addEdt("streamChatTest", p -> {
            String message = "A bit longer chat message with emotes and stuff "
                    + "FrankerZ ZreknarF MiniK ("+(int)(Math.random()*10)+")";
            if (p.hasArgs()) {
                message = p.getArgs();
            }
            UserMessage m = new UserMessage(client.getSpecialUser(), message, null, null, 0, null, null, null, MsgTags.EMPTY);
            streamChat.printMessage(m);
        });
        client.commands.addEdt("livestreamer", p -> {
            String stream = null;
            String quality = null;
            String parameter = StringUtil.trim(p.getArgs());
            if (parameter != null && !parameter.isEmpty()) {
                String[] split = parameter.split(" ");
                stream = split[0];
                if (stream.equals("$active")) {
                    stream = channels.getActiveChannel().getStreamName();
                    if (stream == null) {
                        printLine("Streamlink: No channel open.");
                        return;
                    }
                }
                if (split.length > 1) {
                    quality = split[1];
                }
            }
            printLine("Streamlink: Opening stream..");
            livestreamerDialog.open(stream, quality);
        });
        client.commands.addEdt("help", p -> {
            openHelp(null);
        });
        client.commands.addEdt("setStreamChatSize", p -> {
            Dimension size = Helper.getDimensionFromParameter(p.getArgs());
            if (size != null) {
                setStreamChatSize(size.width, size.height);
                printSystem("Set StreamChat size to " + size.width + "x" + size.height);
            }
            else {
                printSystem("Invalid parameters.");
            }
        });
        client.commands.addEdt("getStreamChatSize", p -> {
            Dimension d = streamChat.getSize();
            printSystem("StreamChat size: "+d.width+"x"+d.height);
        });
        client.commands.addEdt("setSize", p -> {
            Dimension size = Helper.getDimensionFromParameter(p.getArgs());
            if (size != null) {
                setSize(size);
                printSystem(String.format("Set Window size to %dx%d", size.width, size.height));
            }
            else {
                printSystem("Invalid parameters.");
            }
        });
        client.commands.addEdt("popoutChannel", p -> {
            channels.popout(channels.getActiveContent(), false);
        });
        client.commands.addEdt("popoutChannelWindow", p -> {
            channels.popout(channels.getActiveContent(), true);
        });
        client.commands.addEdt("layouts", p -> {
            if (p.hasArgs()) {
                String args = p.getArgs().trim();
                String command = null;
                String layoutName = null;
                int options = -1;
                String[] split = args.split(" ", 2);
                if (split.length == 2) {
                    command = split[0];
                    if (split[1].startsWith("-") && command.equals("load")) {
                        String[] optionsSplit = split[1].split(" ");
                        if (optionsSplit.length == 2) {
                            if (!optionsSplit[0].equals("--")) {
                                options = Channels.makeLoadLayoutOptions(
                                        optionsSplit[0].contains("c"),
                                        optionsSplit[0].contains("l"),
                                        optionsSplit[0].contains("m"));
                            }
                            layoutName = optionsSplit[1];
                        }
                    }
                    else {
                        layoutName = split[1];
                    }
                }
                if (command == null || layoutName == null) {
                    printSystem("Invalid parameters.");
                }
                else {
                    switch (command) {
                        case "add":
                            addLayout(layoutName);
                            break;
                        case "save":
                            saveLayout(layoutName, false);
                            break;
                        case "load":
                            loadLayout(layoutName, options);
                            break;
                        case "remove":
                            removeLayout(layoutName, false);
                            break;
                    }
                }
            }
        });
    }
    
    public void insert(final String text, final boolean spaces) {
        insert(text, spaces, false);
    }
    
    public void insert(final String text, final boolean spaces, boolean focus) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (text != null) {
                    channels.getLastActiveChannel().insertText(text, spaces);
                    if (focus) {
                        channels.getLastActiveChannel().requestFocus();
                        channels.getLastActiveChannel().getInput().requestFocusInWindow();
                    }
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
        openUserInfoDialog(user, msgId, autoModMsgId, false);
    }
    
    /**
     * Only call out of the EDT.
     * 
     * @param user
     * @param msgId 
     */
    public void openUserInfoDialog(User user, String msgId, String autoModMsgId, boolean keepPosition) {
        windowStateManager.setWindowPosition(userInfoDialog.getDummyWindow(), getActiveWindow());
        userInfoDialog.show(getActiveWindow(), user, msgId, autoModMsgId, client.getUsername(), keepPosition);
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
        openChannelAdminDialog(channels.getActiveChannel().getStreamName());
    }
    
    private void openChannelAdminDialog(String stream) {
        windowStateManager.setWindowPosition(adminDialog, getActiveWindow());
        updateTokenScopes();
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
    
    private void openEventLog(int tab) {
        windowStateManager.setWindowPosition(eventLog);
        eventLog.setVisible(true);
        if (tab > -1) {
            eventLog.setTab(tab);
        }
    }
    
    private void toggleEventLog() {
        if (!closeDialog(eventLog)) {
            openEventLog(-1);
        }
    }
    
    public void setSystemEventCount(int count) {
        menu.setSystemEventCount(count);
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
        emotesDialog.showDialog(client.getEmotesets(), channel);
        // Focus inputbox to be able to keep writing
        channels.setInitialFocus();
    }
    
    private void openEmotesDialogChannelEmotes(String channel) {
        client.requestChannelEmotes(channel);
        openEmotesDialog();
        emotesDialog.setTempStream(channel);
        emotesDialog.showChannelEmotes();
    }
    
    private void openEmotesDialogEmoteDetails(Emoticon emote) {
        openEmotesDialog();
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
    
    private void openUpdateDialog() {
        updateDialog.setLocationRelativeTo(this);
        updateDialog.showDialog();
    }
    
    private void openFavoritesDialogFromConnectionDialog(String channel) {
        Set<String> channels = chooseFavorites(this, channel);
        if (!channels.isEmpty()) {
            connectionDialog.setChannel(Helper.buildStreamsString(channels));
        }
    }
    
    public Set<String> chooseFavorites(Component owner, String channel) {
        favoritesDialog.setLocationRelativeTo(owner);
        int result = favoritesDialog.showDialog(channel);
        if (result == FavoritesDialog.ACTION_DONE) {
            return favoritesDialog.getChannels();
        }
        return new HashSet<>();
    }
    
    private void toggleFavoritesDialog() {
        if (favoritesDialog.isVisible()) {
            favoritesDialog.setVisible(false);
        } else {
            openFavoritesDialogToJoin("");
        }
    }
    
    private void openFavoritesDialogToJoin(String channel) {
        favoritesDialog.setLocationRelativeTo(this);
        int result = favoritesDialog.showDialog(channel);
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
    
    private void openHighlightedMessages(boolean switchTo) {
        windowStateManager.setWindowPosition(highlightedMessages);
        highlightedMessages.setVisible(true, switchTo);
    }
    
    private void toggleHighlightedMessages() {
        if (!closeDialog(highlightedMessages)) {
            openHighlightedMessages(true);
        }
    }
    
    private void openIgnoredMessages(boolean switchTo) {
        windowStateManager.setWindowPosition(ignoredMessages);
        ignoredMessages.setVisible(true, switchTo);
    }
    
    private void toggleIgnoredMessages() {
        if (!closeDialog(ignoredMessages)) {
            openIgnoredMessages(true);
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
    
    public void setChannelNewStatus(final String ownerChannel, final String newStatus) {
        SwingUtilities.invokeLater(() -> {
            channels.setChannelNewStatus(ownerChannel);
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
    
    public void triggerCommandNotification(String channel, String title, String text, boolean noNotify, boolean noSound) {
        GuiUtil.edt(() -> {
            notificationManager.commandNotification(channel, title, text, noNotify, noSound);
        });
    }
    
    public void showNotification(String title, String message, Color foreground, Color background, NotificationWindowData data) {
        long setting = client.settings.getLong("nType");
        if (setting == NotificationSettings.NOTIFICATION_TYPE_CUSTOM) {
            notificationWindowManager.showMessage(title, message, foreground, background, data);
        } else if (setting == NotificationSettings.NOTIFICATION_TYPE_TRAY) {
            trayIcon.displayInfo(title, message);
        } else if (setting == NotificationSettings.NOTIFICATION_TYPE_COMMAND) {
            GuiUtil.showCommandNotification(client.settings.getString("nCommand"),
                    title, message, data.channel);
        }
        eventLog.add(new chatty.gui.components.eventlog.Event(
                chatty.gui.components.eventlog.Event.Type.NOTIFICATION,
                null, title, message, foreground, background));
    }
    
    public void showTestNotification(final String channel, String title, String text) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (client.settings.getString("username").equalsIgnoreCase("joshimuz")) {
                    showNotification("[Test] It works!",
                            "Now you have your notifications Josh.. Kappa",
                            Color.BLACK, Color.WHITE, new NotificationWindowData(null, channel));
                } else if (title != null && text != null) {
                    showNotification(title, text, Color.BLACK, Color.WHITE, null);
                } else if (StringUtil.isNullOrEmpty(channel)) {
                    showNotification("[Test] It works!",
                            "This is where the text goes.",
                            Color.BLACK, Color.WHITE, null);
                } else {
                    showNotification("[Status] "+Helper.toValidChannel(channel),
                            "Test Notification (this would pop up when a stream status changes)",
                            Color.BLACK, Color.WHITE, new NotificationWindowData(null, channel));
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
        return channels.getLastActiveChannel().getChannel().equals(channel);
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
    
    public void printMessage(User user, String text, boolean action) {
        printMessage(user, text, action, MsgTags.EMPTY);
    }
    
    public void printMessage(User user, String text2, boolean action, MsgTags tags0) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                /**
                 * Replace the ZWF replacement (which consists of two chars)
                 * with the ZFW before anything that relies on character
                 * position (like Highlights or Emote parsing) is performed.
                 * Twitch emote indices work with codepoint counts, so it's
                 * fine.
                 */
                boolean decodeZWF = client.settings.getLong("emojiZWJ") > 0;
                String text = decodeZWF ? EmojiUtil.decodeZWJ(text2) : text2;
                
                MsgTags tags = tags0;
                Channel chan;
                String channel = user.getChannel();
                boolean whisper = false;
                int bitsAmount = tags.getBits();
                User localUser = client.getLocalUser(channel);
                
                // Disable Cheer emotes altogether if disabled in the settings
                int bitsForEmotes = bitsAmount;
                if (client.settings.getString("cheersType").equals("none")) {
                    bitsForEmotes = 0;
                }
                
                /**
                 * Check if special channel and change target according to
                 * settings
                 */
                if (channel.equals(WhisperManager.WHISPER_CHANNEL)) {
                    int whisperSetting = (int)client.settings.getLong("whisperDisplayMode");
                    if (whisperSetting == WhisperManager.DISPLAY_ONE_WINDOW) {
                        chan = channels.getChannel(client.roomManager.getRoom(channel));
                    } else if (whisperSetting == WhisperManager.DISPLAY_PER_USER) {
                        if (!userIgnored(user, true)) {
                            chan = channels.getChannel(client.roomManager.getRoom("$"+user.getName()));
                        } else {
                            chan = channels.getActiveChannel();
                        }
                    } else {
                        chan = channels.getActiveChannel();
                    }
                    whisper = true;
                } else {
                    chan = channels.getChannel(user.getRoom());
                }
                // If channel was changed from the given one, change accordingly
                channel = chan.getChannel();
                
                // Adds a tag if repeated msg is detected according to settings
                tags = repeatMsg.check(user, localUser, text, tags);
                if (Chatty.DEBUG && !tags.hasValue("id")) {
                    /**
                     * Could be weird to add for non-testing since the message
                     * can't actually be deleted or whatever.
                     */
                    tags = MsgTags.addTag(tags, "id", String.valueOf(User.MSG_ID++));
                }
                
                boolean isOwnMessage = isOwnUsername(user.getName()) || (whisper && action);
                boolean ignoredUser = (userIgnored(user, whisper) && !isOwnMessage);
                boolean ignored = checkMsg(ignoreList, "ignore", text, -2, -2, user, localUser, tags, isOwnMessage, false) || ignoredUser;
                
                boolean highlighted = false;
                List<Match> highlightMatches = null;
                if ((client.settings.getBoolean("highlightIgnored")
                        || client.settings.getBoolean("highlightOverrideIgnored")
                        || highlighter.hasOverrideIgnored()
                        || !ignored)
                        && !client.settings.listContains("noHighlightUsers", user.getName())) {
                    boolean rejectIgnoredWithoutPrefix = client.settings.getBoolean("highlightOverrideIgnored")
                                              || client.settings.getBoolean("highlightIgnored")
                                              ? false : ignored;
                    highlighted = checkMsg(highlighter, "highlight", text, -2, -2, user, localUser, tags, isOwnMessage, rejectIgnoredWithoutPrefix);
                    if (highlighted) {
                        if (client.settings.getBoolean("highlightOverrideIgnored")
                                || highlighter.getLastMatchItem().overrideIgnored()) {
                            ignored = false;
                        }
                    }
                }
                
                if (!ignored || client.settings.getBoolean("logIgnored")) {
                    client.chatLog.bits(chan.getFilename(), user, bitsAmount);
                    client.chatLog.message(chan.getFilename(), user, text, action, null);
                }
                
                TagEmotes tagEmotes = Emoticons.parseEmotesTag(tags.getRawEmotes());
                
                // Do stuff if highlighted, without printing message
                if (highlighted) {
                    highlightMatches = highlighter.getLastTextMatches();
                    if (!highlighter.getLastMatchNoNotification()) {
                        channels.setChannelHighlighted(chan);
                    } else {
                        channels.setChannelNewMessage(chan);
                    }
                    notificationManager.highlight(user, localUser, text, tags,
                            highlighter.getLastMatchNoNotification(),
                            highlighter.getLastMatchNoSound(),
                            isOwnMessage, whisper, bitsAmount > 0);
                } else if (!ignored) {
                    if (whisper) {
                        notificationManager.whisper(user, localUser, text, isOwnMessage);
                    } else {
                        notificationManager.message(user, localUser, text, tags, isOwnMessage,
                                bitsAmount > 0);
                    }
                    if (!isOwnMessage) {
                        channels.setChannelNewMessage(chan);
                    }
                }
                
                // Do stuff if ignored, without printing message
                if (ignored) {
                    List<Match> ignoreMatches = null;
                    Object ignoreSource = null;
                    if (!ignoredUser) {
                        // Text matches might not be valid if ignore was through
                        // ignored users list
                        ignoreMatches = ignoreList.getLastTextMatches();
                        ignoreSource = ignoreList.getLastMatchItems();
                    }
                    ignoredMessages.addMessage(channel, user, text, action,
                            tagEmotes, bitsForEmotes, whisper, ignoreMatches,
                            ignoreSource, tags);
                    client.chatLog.message("ignored", user, text, action, channel);
                    ignoredMessagesHelper.ignoredMessage(channel);
                }
                long ignoreMode = client.settings.getLong("ignoreMode");
                
                // Print or don't print depending on ignore
                if (ignored && (ignoreMode <= IgnoredMessages.MODE_COUNT || 
                        !showIgnoredInfo())) {
                    // Don't print message
                    if (isOwnMessage && channels.isChannel(channel)) {
                        // Don't log to file
                        printInfo(chan, InfoMessage.createInfo("Own message ignored."));
                    }
                } else {
                    boolean hasReplacements = checkMsg(filter, "filter", text, -2, -2, user, localUser, tags, isOwnMessage, false);

                    // Print message, but determine how exactly
                    UserMessage message = new UserMessage(user, text, tagEmotes, tags.getId(), bitsForEmotes,
                            highlightMatches,
                            hasReplacements ? filter.getLastTextMatches() : null,
                            hasReplacements ? filter.getLastReplacement() : null,
                            tags);
                    message.localUser = localUser;
                    
                    // Custom color
                    boolean hlByPoints = tags.isHighlightedMessage() && client.settings.getBoolean("highlightByPoints");
                    if (highlighted) {
                        message.color = highlighter.getLastMatchColor();
                        message.backgroundColor = highlighter.getLastMatchBackgroundColor();
                        message.colorSource = highlighter.getColorSource();
                        message.highlightSource = highlighter.getLastMatchItems();
                    }
                    if (!(highlighted || hlByPoints) || client.settings.getBoolean("msgColorsPrefer")) {
                        ColorItem colorItem = msgColorManager.getMsgColor(user, localUser, text, -2, -2, tags);
                        if (!colorItem.isEmpty()) {
                            message.color = colorItem.getForegroundIfEnabled();
                            message.backgroundColor = colorItem.getBackgroundIfEnabled();
                            message.colorSource = colorItem;
                        }
                    }
                    
                    message.whisper = whisper;
                    message.action = action;
                    if (highlighted || hlByPoints) {
                        // Only set message.highlighted instead of highlighted
                        // if hlByPoints, since that would affect other stuff as
                        // well
                        message.highlighted = true;
                    } else if (ignored && ignoreMode == IgnoredMessages.MODE_COMPACT) {
                        message.ignored_compact = true;
                    }
                    chan.printMessage(message);
                    if (highlighted) {
                        highlightedMessages.addMessage(channel, message);
                        client.chatLog.message("highlighted", user, text, action, channel);
                    }
                    if (client.settings.listContains("streamChatChannels", channel)) {
                        streamChat.printMessage(message);
                    }
                }
                
                CopyMessages.copyMessage(client.settings, user, text, highlighted);
                
                // Update User
                user.addMessage(processMessage(text), action, tags.getId());
                if (highlighted) {
                    user.setHighlighted();
                }
                updateUserInfoDialog(user);
            }
        });
    }
    
    public void printSubscriberMessage(final User user, final String text,
            final String message, final MsgTags tags) {
        SwingUtilities.invokeLater(() -> {
            SubscriberMessage m = new SubscriberMessage(user, text, message, tags);

            boolean printed = printUsernotice(m);
            if (printed) {
                notificationManager.newSubscriber(user, client.getLocalUser(user.getChannel()), text, message);
            }
        });
    }
    
    /**
     * Will attempt to merge Points notices with an attached message between IRC
     * and PubSub. Data is shared between both sources accordingly when a merge
     * occurs, which is when both sources have called this method for the same
     * message. When no merge occurs after a second, a backup timer will output
     * the message anyway.
     * 
     * Notices without an attached messages will be directly output.
     * 
     * @param user
     * @param text
     * @param message
     * @param tags 
     */
    public void printPointsNotice(final User user, final String text, final String message, final MsgTags tags) {
        SwingUtilities.invokeLater(() -> {
            UserNotice m = new UserNotice("Points", user, text, message, tags);
            if (message != null) {
                Helper.pointsMerge(m, this);
            }
            else {
                printUsernotice(m);
            }
        });
    }
    
    public void printUsernotice(final String type, final User user, final String text,
            final String message, final MsgTags tags) {
        SwingUtilities.invokeLater(() -> {
            UserNotice m = new UserNotice(type, user, text, message, tags);
            printUsernotice(m);
        });
    }
    
    private boolean printUsernotice(UserNotice m) {
        boolean notIgnored = printInfo(channels.getChannel(m.user.getRoom()), m);
        
        // Only add if not dummy user (dummy user possibly not used anymore)
        if (!m.user.getName().isEmpty()) {
            String message = m.attachedMessage != null ? processMessage(m.attachedMessage) : "";
            String text = m.infoText;
            if (m instanceof SubscriberMessage) {
                m.user.addSub(message, text);
            } else {
                m.user.addInfo(message, m.text);
            }
            updateUserInfoDialog(m.user);
        }
        return notIgnored;
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
    
    private boolean checkHighlight(HighlightItem.Type type, String text, int msgStart, int msgEnd,
            String channel, Addressbook ab, User user, User localUser, MsgTags tags, Highlighter hl,
            String setting, boolean isOwnMessage, boolean ignored) {
        if (client.settings.getBoolean(setting + "Enabled")) {
            if (client.settings.getBoolean(setting + "OwnText") ||
                    !isOwnMessage) {
                return hl.check(type, text, msgStart, msgEnd, channel, ab, user, localUser, tags, ignored);
            }
        }
        return false;
    }
    
    private boolean checkMsg(Highlighter hl, String setting, String text, int msgStart, int msgEnd,
            User user, User localUser, MsgTags tags, boolean isOwnMessage,
            boolean ignored) {
        return checkHighlight(HighlightItem.Type.REGULAR, text, msgStart, msgEnd, null, null,
                user, localUser, tags, hl, setting, isOwnMessage, ignored);
    }
    
    private boolean checkInfoMsg(Highlighter hl, String setting, String text, int msgStart, int msgEnd,
            User user, MsgTags tags, String channel, Addressbook ab,
            boolean ignored) {
        return checkHighlight(HighlightItem.Type.INFO, text, msgStart, msgEnd, channel, ab,
                user, client.getLocalUser(channel), tags, hl, setting, false,
                ignored);
    }
    
    protected void ignoredMessagesCount(String channel, String message) {
        if (client.settings.getLong("ignoreMode") == IgnoredMessages.MODE_COUNT
                && showIgnoredInfo()) {
            if (channels.isChannel(channel)) {
                channels.getExistingChannel(channel).printLine(message);
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
                channels.getChannel(user.getRoom()).userBanned(user, duration, reason, id);
                user.addBan(duration, reason, id);
                updateUserInfoDialog(user);
                if (client.settings.listContains("streamChatChannels", user.getChannel())) {
                    streamChat.userBanned(user, duration, reason, id);
                }
                highlightedMessages.addBan(user, duration, reason, id);
                ignoredMessages.addBan(user, duration, reason, id);
            }
        });
    }
    
    public void msgDeleted(final User user, String targetMsgId, String msg) {
        SwingUtilities.invokeLater(() -> {
            channels.getChannel(user.getRoom()).userBanned(user, -2, null, targetMsgId);
            user.addMsgDeleted(targetMsgId, msg);
            updateUserInfoDialog(user);
            if (client.settings.listContains("streamChatChannels", user.getChannel())) {
                streamChat.userBanned(user, -2, null, targetMsgId);
            }
            highlightedMessages.addBan(user, -2, null, targetMsgId);
            ignoredMessages.addBan(user, -2, null, targetMsgId);
        });
    }

    public void clearChat() {
        clearChat(null);
    }
    
    public void clearChat(final Room room) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel;
                if (room == null) {
                    panel = channels.getActiveChannel();
                } else {
                    panel = channels.getChannel(room);
                    if (client.settings.listContains("streamChatChannels", room.getChannel())) {
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
        GuiUtil.edt(() -> {
            Channel panel = channels.getLastActiveChannel();
            if (panel != null) {
                panel.selectPreviousUser();
            }
        });
    }
    
    public void joinScheduled(String channel) {
        GuiUtil.edt(() -> {
            channels.joinScheduled(channel);
        });
    }
    
    public void printLine(final String line) {
        GuiUtil.edt(() -> {
            Channel panel = channels.getLastActiveChannel();
            if (panel != null) {
                printInfo(panel, InfoMessage.createInfo(line));
            }
        });
    }
    
    public void addToLine(final Room room, Object objectId, String text) {
        GuiUtil.edt(() -> {
            channels.getChannel(room).printInfoMessage(InfoMessage.createAppend(objectId, text));
        });
    }
    
    public void printSystem(final String line) {
        printSystem(null, line);
    }
    
    public void printSystem(final Room room, final String line) {
        GuiUtil.edt(() -> {
            Channel channel;
            if (room == null || room == Room.EMPTY) {
                channel = channels.getActiveChannel();
            }
            else {
                channel = channels.getChannel(room);
            }
            if (channel != null) {
                printInfo(channel, InfoMessage.createSystem(line));
            }
        });
    }
    
    public void printSystemMultline(final Room room, final String text) {
        GuiUtil.edt(() -> {
            String[] lines = text.split("\n");
            for (String line : lines) {
                printSystem(room, line);
            }
        });
    }

    public Object printLine(final Room room, final String line) {
        return printInfo(room, line, null);
    }
    
    public Object printInfo(final Room room, final String line, MsgTags tags) {
        Object objectId = new Object();
        GuiUtil.edt(() -> {
            if (room == null || room == Room.EMPTY) {
                printLine(line);
            } else {
                InfoMessage m = InfoMessage.createInfo(line, tags);
                m.objectId = objectId;
                printInfo(channels.getChannel(room), m);
            }
        });
        return objectId;
    }
    
    public Object printLineAll(final String line) {
        Object objectId = new Object();
        SwingUtilities.invokeLater(() -> {
            for (Channel channel : channels.allChannels()) {
                // Separate for each channel, since it could be modified based
                // on channel
                InfoMessage m = InfoMessage.createInfo(line);
                m.objectId = objectId;
                printInfo(channel, m);
            }
        });
        return objectId;
    }
    
    public void printLineAllAppend(String text, Object objectId) {
        GuiUtil.edt(() -> {
            for (Channel channel : channels.allChannels()) {
                channel.printInfoMessage(InfoMessage.createAppend(objectId, text));
            }
        });
    }
    
    public void printLineByOwnerChannel(final String channel, final String text) {
        SwingUtilities.invokeLater(() -> {
            for (Channel chan : channels.getExistingChannelsByOwner(channel)) {
                printInfo(chan, InfoMessage.createInfo(text));
            }
        });
    }
    
    /**
     * Central method for printing info messages. Each message is intended for
     * a single channel, so for printing to e.g. all channels at once, this is
     * called once each for all channels.
     * 
     * @param channel
     * @param message
     * @return 
     */
    private boolean printInfo(Channel channel, InfoMessage message) {
        User user = null;
        if (message instanceof UserNotice) {
            user = ((UserNotice)message).user;
        }
        MsgTags tags = message.tags;
        boolean ignored = checkInfoMsg(ignoreList, "ignore", message.text, message.getMsgStart(), message.getMsgEnd(), user, tags, channel.getChannel(), client.addressbook, false);
        boolean highlighted = false;
        boolean ignoreCheck = !ignored
                || highlighter.hasOverrideIgnored()
                || client.settings.getBoolean("highlightOverrideIgnored");
        if (ignoreCheck && !message.isHidden()) {
            boolean rejectIgnoredWithoutPrefix = client.settings.getBoolean("highlightOverrideIgnored") ? false : ignored;
            highlighted = checkInfoMsg(highlighter, "highlight", message.text, message.getMsgStart(), message.getMsgEnd(), user, tags, channel.getChannel(), client.addressbook, rejectIgnoredWithoutPrefix);
            if (highlighted) {
                if (client.settings.getBoolean("highlightOverrideIgnored")
                        || highlighter.getLastMatchItem().overrideIgnored()) {
                    ignored = false;
                }
            }
        }
        if (!ignored) {
            //----------------
            // Output Message
            //----------------
            if (!message.isHidden()) {
                User localUser = client.getLocalUser(channel.getChannel());
                if (highlighted) {
                    message.highlighted = true;
                    message.highlightMatches = highlighter.getLastTextMatches();
                    message.color = highlighter.getLastMatchColor();
                    message.bgColor = highlighter.getLastMatchBackgroundColor();
                    message.colorSource = highlighter.getColorSource();
                    message.highlightSource = highlighter.getLastMatchItems();

                    if (!highlighter.getLastMatchNoNotification()) {
                        channels.setChannelHighlighted(channel);
                    } else {
                        channels.setChannelNewMessage(channel);
                    }
                    notificationManager.infoHighlight(channel.getRoom(), message.text,
                            highlighter.getLastMatchNoNotification(),
                            highlighter.getLastMatchNoSound(), localUser);
                } else {
                    notificationManager.info(channel.getRoom(), message.text, localUser);
                }
                if (!highlighted || client.settings.getBoolean("msgColorsPrefer")) {
                    ColorItem colorItem = msgColorManager.getInfoColor(
                            message.text, message.getMsgStart(), message.getMsgEnd(), channel.getChannel(), client.addressbook, user, localUser, tags);
                    if (!colorItem.isEmpty()) {
                        message.color = colorItem.getForegroundIfEnabled();
                        message.bgColor = colorItem.getBackgroundIfEnabled();
                        message.colorSource = colorItem;
                    }
                }
                // After colors and everything is set
                if (highlighted) {
                    highlightedMessages.addInfoMessage(channel.getChannel(), message);
                    client.chatLog.info("highlighted", message.text, channel.getChannel());
                }
            }
            channel.printInfoMessage(message);
            if (channel.getType() == Channel.Type.SPECIAL) {
                channels.setChannelNewMessage(channel);
            }
        } else if (!message.isHidden()) {
            ignoredMessages.addInfoMessage(channel.getChannel(), message.text,
                    ignoreList.getLastTextMatches(), ignoreList.getLastMatchItems());
            client.chatLog.info("ignored", message.text, channel.getChannel());
        }
        
        //----------
        // Chat Log
        //----------
        if (message.isSystemMsg()) {
            client.chatLog.system(channel.getFilename(), message.text);
        } else if (!message.text.startsWith("[ModAction]")) {
            // ModLog message could be ModLogInfo or generic ModInfo (e.g. for
            // abandoned messages), so just checking the text instead of type or
            // something (ModActions are logged separately)
            client.chatLog.info(channel.getFilename(), message.text, null);
        }
        return !ignored;
    }
    
    /**
     * Calls the appropriate method from the given channel
     * 
     * @param channel The channel this even has happened in.
     * @param type The type of event.
     * @param user The User object of who was the target of this event (mod/..).
     */
    public void printCompact(final String type, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channels.getChannel(user.getRoom()).printCompact(type, user);
            }
        });
    }
    
    /**
     * Perform search in the currently selected channel. Should only be called
     * from the EDT.
     * 
     * @param chan
     * @param searchText 
     * @return  
     */
    public boolean search(Channel chan, final String searchText) {
        if (chan == null) {
            return false;
        }
        return chan.search(searchText);
    }
    
    public void resetSearch(Channel chan) {
        SwingUtilities.invokeLater(() -> {
            chan.resetSearch();
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
    
    public void printDebugEventSub(String line) {
        SwingUtilities.invokeLater(() -> debugWindow.printLineEventSub(line));
    }
    
    public void printTimerLog(String line) {
        GuiUtil.edt(() -> debugWindow.printTimerLog(line));
    }
    
    public void printModerationAction(final ModeratorActionData data,
            final boolean ownAction) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                moderationLog.add(data);
                autoModDialog.addData(data);
                
                String channel = Helper.toValidChannel(data.stream);
                
                // AutoMod only seems to work in Stream Chat
                if (channels.isChannel(channel)) {
                    Channel chan = channels.getExistingChannel(channel);
                    if (data.type == ModeratorActionData.Type.AUTOMOD_REJECTED
                            && data.args.size() > 1) {
                        // Automod
                        String username = data.args.get(0);
                        String message = data.args.get(1);
                        if (client.settings.getBoolean("showAutoMod")) {
                            User user = client.getUser(channel, username);
                            printInfo(chan, new AutoModMessage(user, message, data.msgId));
                        }
                        notificationManager.autoModMessage(channel, username, message);
                    }
                }
                
                // Moderator Actions apparently apply to all rooms
                Collection<Channel> chans = channels.getExistingChannelsByOwner(channel);
                if (!chans.isEmpty()
                        && data.type != ModeratorActionData.Type.AUTOMOD_REJECTED
                        && data.type != ModeratorActionData.Type.UNMODDED) {
                    boolean showActions = client.settings.getBoolean("showModActions");
                    boolean showActionsRestrict = client.settings.getBoolean("showModActionsRestrict");
                    boolean showMessage =
                               showActions
                            && (!ownAction || ModLogInfo.isIndirectAction(data))
                            && !(showActionsRestrict && ModLogInfo.isAssociated(data));
                    boolean showActionby = client.settings.getBoolean("showActionBy");
                    for (Channel chan : chans) {
                        // Create for each channel, just in case (since they get
                        // modified)
                        // TODO: Output that output of reason or by isn't affected by ignore etc.
                        ModLogInfo infoMessage = new ModLogInfo(chan, data, showActionby, ownAction);
                        infoMessage.setHidden(!showMessage);
                        printInfo(chan, infoMessage);
                    }
                }
            }
        });
    }

    public void printLowTrustUserInfo(User user, final LowTrustUserMessageData data) {
        String channel = Helper.toValidChannel(data.stream);
        if (channels.isChannel(channel)) {
            data.fetchUserInfoForBannedChannels(client.api, o -> SwingUtilities.invokeLater(() -> {
                Channel chan = channels.getExistingChannel(channel);
                if (data.treatment == LowTrustUserMessageData.Treatment.RESTRICTED &&
                    client.settings.getBoolean("showRestrictedMessagesInChat")) {
                    // message is not being posted to actual chat, display it here anyway
                    HashMap<String, String> map = new HashMap<>();
                    map.put("id", data.aboutMessageId);
                    MsgTags tags = new MsgTags(map);

                    printMessage(user, data.text, false, tags);
                }
                
                chan.printLowTrustUpdate(user, data);
            }));
        }
    }

    /**
     * If not matching message was found for the ModAction to append the @mod,
     * then output anyway.
     *
     * @param info
     */
    public void printAbandonedModLogInfo(ModLogInfo info) {
        boolean showActions = client.settings.getBoolean("showModActions");
        if (showActions && !info.ownAction) {
            printInfo(info.chan, InfoMessage.createInfo(info.text));
        }
    }
    
    public void autoModRequestResult(TwitchApi.AutoModAction action, String msgId, TwitchApi.AutoModActionResult result) {
        SwingUtilities.invokeLater(() -> {
            autoModDialog.requestResult(action, msgId, result);
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
    public void addUser(final User user) {
        SwingUtilities.invokeLater(() -> {
            if (shouldUpdateUser(user)) {
                Channel c = channels.getChannel(user.getRoom());
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
    public void removeUser(final User user) {
        SwingUtilities.invokeLater(() -> {
            if (shouldUpdateUser(user)) {
                Channel c = channels.getChannel(user.getRoom());
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
        SwingUtilities.invokeLater(() -> {
            if (shouldUpdateUser(user)) {
                channels.getChannel(user.getRoom()).updateUser(user);
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
     * @param room
     */
    public void resortUsers(final Room room) {
        SwingUtilities.invokeLater(() -> {
            channels.getChannel(room).resortUserlist();
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
                    Channel c = channels.getExistingChannel(channel);
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
    
    public User getUser(String channel, String name) {
        return client.getUser(channel, name);
    }
    
    public void reconnect() {
        client.commandReconnect();
    }
    
    public void setUpdateAvailable(final String newVersion, final GitHub.Releases releases) {
        SwingUtilities.invokeLater(() -> {
            menu.setUpdateNotification(true);
            //updateMessage.setNewVersion(newVersion);
            updateDialog.setInfo(releases);
            if (releases != null) {
                // From actual request
                updateDialog.showDialog();
            }
        });
    }
    
    public void setColor(final String item) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                getSettingsDialog(s -> s.showSettings("editUsercolorItem", item));
            }
        });
    }
    
    public void setCustomName(final String item) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                getSettingsDialog(s -> s.showSettings("editCustomNameItem", item));
            }
        });
    }

    public void updateChannelInfo(StreamInfo info) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateChannelInfoDialog(info);
           }
        });
    }
    
    public void updateStreamLive(StreamInfo info) {
        SwingUtilities.invokeLater(() -> {
            channels.setStreamLive(info.stream, info.isValidEnough() && info.getOnline());
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
    
    public void startUpdatingState() {
        state.update(false);
        javax.swing.Timer timer = new javax.swing.Timer(5000, e -> {
            state.update(false);
        });
        timer.setRepeats(true);
        timer.start();
    }
    
    /**
     * Manages updating the current state, mainly the titles and menus.
     */
    private class StateUpdater {
        
        /**
         * Saves when the state was last updated, so the delay can be measured.
         */
        private final ElapsedTime lastUpdatedET = new ElapsedTime();
        
        /**
         * Update state no faster than this amount of milliseconds.
         */
        private static final int UPDATE_STATE_DELAY = 500;
        
        private boolean init = false;
        
        protected void init() {
            init = true;
            update();
        }
        
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
            if (!guiCreated || !init) {
                return;
            }
            if (!forced && !lastUpdatedET.millisElapsed(UPDATE_STATE_DELAY)) {
                return;
            }
            lastUpdatedET.set();

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
            menu.getMenuItem("connect").setEnabled(!(state > Irc.STATE_OFFLINE || state == Irc.STATE_RECONNECTING));
            menu.getMenuItem("disconnect").setEnabled(state > Irc.STATE_OFFLINE || state == Irc.STATE_RECONNECTING);
        }
        
        /**
         * Updates the titles of both the main window and popout dialogs.
         * 
         * @param state 
         */
        private void updateTitles(int state) {
            // May be necessary to make the title either way, because it also
            // requests stream info
            String mainTitle = makeTitle(channels.getMainActiveContent(), state);
            String trayTooltip = makeTitle(channels.getLastActiveChannel().getDockContent(), state);
            trayIcon.setTooltipText(trayTooltip);
            if (client.settings.getBoolean("simpleTitle")) {
                setTitle("Chatty");
            } else {
                setTitle(mainTitle);
            }
            for (Map.Entry<DockPopout, DockContent> entry : channels.getActivePopoutContent().entrySet()) {
                DockPopout popout = entry.getKey();
                DockContent active = entry.getValue();
                if (active == null) {
                    popout.setTitle(appendToTitle("Empty"));
                }
                else {
                    String title = makeTitle(active, state);
                    popout.setTitle(title);
                }
            }
            if (client.settings.getBoolean("tabsChanTitles")) {
                // Set extra title for channels
                for (DockContent c : channels.getDock().getContents()) {
                    if (c.getComponent() instanceof Channel) {
                        Channel chan = (Channel) c.getComponent();
                        if (chan.getName().startsWith("#")) {
                            c.setLongTitle(makeChannelInfo(chan));
                        }
                    }
                }
            }
            else {
                // Disable extra title for channels
                for (DockContent c : channels.getDock().getContents()) {
                    if (c.getComponent() instanceof Channel) {
                        c.setLongTitle(null);
                    }
                }
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
        private String makeTitle(DockContent content, int state) {
            Channel channel = null;
            if (content instanceof Channels.DockChannelContainer) {
                channel = ((Channels.DockChannelContainer)content).getContent();
            }
            if (channel == null) {
                //--------------------------
                // Non-channel
                //--------------------------
                String title = content.getLongTitle();
                if (StringUtil.isNullOrEmpty(title)) {
                    title = content.getTitle();
                }
                return appendToTitle(title);
            }
            //--------------------------
            // Channel
            //--------------------------
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

            String secondaryConnectionsStatus = "";
            if (client.settings.getBoolean("titleConnections")) {
                secondaryConnectionsStatus = client.getSecondaryConnectionsStatus();
            }
            if (!secondaryConnectionsStatus.isEmpty()) {
                secondaryConnectionsStatus = " [" + secondaryConnectionsStatus + "]";
            }
            
            // Stream Info
            if (!channelName.isEmpty()) {
                String chanNameText = channelName;
                chanNameText += secondaryConnectionsStatus;
                if (!title.isEmpty()) {
                    title += " - ";
                }
                title += chanNameText + " " + makeChannelInfo(channel);
            } else {
                title += secondaryConnectionsStatus;
            }
            return appendToTitle(title);
        }
        
        private String makeChannelInfo(Channel channel) {
            String chan = channel.getChannel();
            String title = "";
            boolean hideCounts = !client.settings.getBoolean("titleShowViewerCount");
            
            String numUsers = Helper.formatViewerCount(channel.getNumUsers());
            if (!client.isUserlistLoaded(chan)) {
                numUsers += "*";
            }
            if (hideCounts) {
                numUsers = "";
            }

            String chanState = "";
            if (client.settings.getBoolean("titleShowChannelState")) {
                chanState = client.getChannelState(chan).getInfo();
            }
            if (!chanState.isEmpty()) {
                chanState = " " + chanState;
            }
            String topic = "";
            if (channel.getRoom().hasTopic()) {
                topic = "[" + channel.getRoom().getTopic() + "]";
            }

            StreamInfo streamInfo = getStreamInfo(channel.getStreamName());
            if (streamInfo.isValidEnough()) {
                if (streamInfo.getOnline()) {

                    String uptime = "";
                    if (client.settings.getBoolean("titleShowUptime")
                            && streamInfo.getTimeStartedWithPicnic() != -1) {
                        if (client.settings.getBoolean("titleLongerUptime")) {
                            uptime = DateTime.agoUptimeCompact2(
                                    streamInfo.getTimeStartedWithPicnic());
                        }
                        else {
                            uptime = DateTime.agoUptimeCompact(
                                    streamInfo.getTimeStartedWithPicnic());
                        }
                    }
                    String numViewers = "|" + Helper.formatViewerCount(streamInfo.getViewers());
                    if (!client.settings.getBoolean("titleShowViewerCount")) {
                        numViewers = "";
                    }
                    else if (!uptime.isEmpty()) {
                        uptime = "|" + uptime;
                    }
                    title += " [" + numUsers + numViewers + uptime + "]";
                }
                else {
                    if (!hideCounts) {
                        title += " [" + numUsers + "]";
                    }
                }
                title += chanState + topic + " - " + streamInfo.getFullStatus();
            }
            else {
                if (!hideCounts) {
                    title += " [" + numUsers + "]";
                }
                title += chanState;
                title += topic;
            }
            return title.trim();
        }
        
        private String appendToTitle(String title) {
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
    
    public void updateEmotesDialog(Set<String> emotesets) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emotesDialog.updateEmotesets(emotesets);
            }
        });
    }
    
    public void refreshEmotes(String type, String stream) {
        if (type.equals("user")) {
            client.api.refreshEmotes();
        }
        else if (type.equals("channel") && !StringUtil.isNullOrEmpty(stream)) {
            client.api.getEmotesByChannelId(stream, null, true);
            if (client.settings.getBoolean("ffz")) {
                client.frankerFaceZ.requestEmotes(stream, true);
            }
            if (client.settings.getBoolean("bttvEmotes")) {
                client.bttvEmotes.requestEmotes(stream, true);
            }
            if (client.settings.getBoolean("seventv")) {
                client.sevenTV.requestEmotes(stream, true);
            }
        }
        else if (type.equals("globaltwitch")) {
            client.api.refreshSets(new HashSet<>(Arrays.asList(new String[]{"0"})));
        }
        else if (type.equals("globalother")) {
            if (client.settings.getBoolean("ffz")) {
                client.frankerFaceZ.requestGlobalEmotes(true);
            }
            if (client.settings.getBoolean("bttvEmotes")) {
                client.bttvEmotes.requestEmotes(BTTVEmotes.GLOBAL, true);
            }
            if (client.settings.getBoolean("seventv")) {
                client.sevenTV.requestEmotes(null, true);
            }
        }
    }
    
    public void updateEmoticons(final EmoticonUpdate update) {
        SwingUtilities.invokeLater(() -> {
            emoticons.updateEmoticons(update);
            emotesDialog.update();
            autoSetSmilies(update);
        });
    }
    
    private void updateSmilies() {
        GuiUtil.edt(() -> {
            Map<String, Set<Emoticon>> smilies = ChattyMisc.getSmilies();
            if (smilies != null) {
                switch ((int) client.settings.getLong("smilies")) {
                    case 1:
                    case 10:
                        emoticons.setSmilies(smilies.get("robot"));
                        break;
                    case 2:
                    case 20:
                        emoticons.setSmilies(smilies.get("glitch"));
                        break;
                    case 3:
                    case 30:
                        emoticons.setSmilies(smilies.get("monkey"));
                        break;
                    default:
                        emoticons.setSmilies(null);
                }
                emotesDialog.update();
            }
        });
    }
    
    /**
     * The old user emotes API will contain at least most of the smilies the
     * user has selected in their Twitch settings, so that can be used to set
     * the setting before the API is removed.
     * 
     * Additionally, it can also look for emotesets in new API request results,
     * although only the set 42 (monkey emotes) appear to actually be in the IRC
     * emotesets message tag.
     * 
     * The smilies in the new API appear to be a bit messy, because instead of
     * regex (and maybe specifying in an option that it's regex) it has an emote
     * for several variations (but the same image). Plus the glitch emotes seem
     * to not be available at all (at least without coding in the emoteset
     * manually somehow, if it still exists). This is why the smilies are loaded
     * separately from another API, whereas the "smilies" setting determines
     * which set is loaded.
     *
     * @param update 
     */
    private void autoSetSmilies(EmoticonUpdate update) {
        if (client.settings.getLong("smilies") < 10) {
            // Manually set to not be automatically changed
            return;
        }
        if (update.setsAdded != null) {
            // User Emotes request should have setsAdded filled
            String type = null;
            for (Emoticon emote : update.emotesToAdd) {
                type = ChattyMisc.getTypeByEmoteId(emote.stringId);
                if (type != null) {
                    LOGGER.info("Set smilies type to "+type+" (by id "+emote.stringId+")");
                    break;
                }
            }
            if (type == null) {
                for (String set : update.setsAdded) {
                    type = ChattyMisc.getTypeByEmoteSet(set);
                    if (type != null) {
                        LOGGER.info("Set smilies type to " + type + " (by set " + set + ")");
                        break;
                    }
                }
            }
            if (type != null) {
                switch (type) {
                    case "robot":
                        client.settings.setLong("smilies", 10);
                        break;
                    case "glitch":
                        client.settings.setLong("smilies", 20);
                        break;
                    case "monkey":
                        client.settings.setLong("smilies", 30);
                        break;
                }
            }
        }
    }
    
    public void setCheerEmotes(final Set<CheerEmoticon> emotes) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.setCheerEmotes(emotes);
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
                result = "Login data invalid. This may only be temporary. If the issue persists, remove login and connect Twitch account again.";
            }
            if (!showInDialog && !changedTokenResponse) {
                showTokenWarning();
            }
        }
        else if (!tokenInfo.hasChatAccess()) {
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
            if (!Chatty.CLIENT_ID.equals(tokenInfo.clientId)) {
                result += " ClientID does not match, please generate token through Chatty.";
            }
            client.updateLogin();
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
            client.settings.putList("scopes", info.scopes);
        } else {
            client.settings.listClear("scopes");
        }
        updateTokenScopes();
    }
    
    /**
     * Updates the token scopes in the GUI based on the settings.
     */
    private void updateTokenScopes() {
        Collection<String> scopes = client.settings.getList("scopes");
        tokenDialog.updateAccess(scopes);
        adminDialog.updateAccess(
                scopes.contains(TokenInfo.Scope.EDITOR.scope),
                scopes.contains(TokenInfo.Scope.EDIT_BROADCAST.scope),
                scopes.contains(TokenInfo.Scope.COMMERICALS.scope),
                scopes.contains(TokenInfo.Scope.BLOCKED_READ.scope)
                        && scopes.contains(TokenInfo.Scope.BLOCKED_MANAGE.scope));
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
                userInfoDialog.setFollowerInfo(info);
            }
        });
    }

    public void setFollowInfo(final String stream, final String user, RequestResultCode result, Follower follower) {
        SwingUtilities.invokeLater(() -> userInfoDialog.setFollowInfo(stream, user, result, follower));
    }
    
    public void channelStatusReceived(ChannelStatus status, RequestResultCode result) {
        SwingUtilities.invokeLater(() -> {
            adminDialog.channelStatusReceived(status, result);
        });
    }
    
    public void putChannelInfo(ChannelStatus info) {
        client.api.putChannelInfoNew(info);
    }

    public String getActiveStream() {
        return channels.getActiveChannel().getStreamName();
    }
    
    public Room getActiveRoom() {
        return channels.getActiveChannel().getRoom();
    }
    
    /**
     * Saves game favorites to the settings. Save in new and old format for
     * backwards compatibility.
     * 
     * @param favorites 
     */
    public void setGameFavorites(Set<StreamCategory> favorites) {
        client.settings.putList("gamesFavorites", new ArrayList(SelectGameDialog.favoritesToSettingsOld(favorites)));
        client.settings.putList("gamesFavorites2", new ArrayList(SelectGameDialog.favoritesToSettings(favorites)));
    }
    
    public void setStreamTagFavorites(Map<String, String> favorites) {
        client.settings.putMap("tagsFavorites", favorites);
    }
    
    /**
     * Gets the game favorites from the settings. Loads from the old format if
     * new format doesn't have any data for backwards compatibility.
     * 
     * @return 
     */
    public Collection<StreamCategory> getGameFavorites() {
        Collection<StreamCategory> result = SelectGameDialog.settingsToFavorites(client.settings.getList("gamesFavorites2"));
        if (result.isEmpty()) {
            // If new settings format has never been saved yet, load old one
            result = SelectGameDialog.settingsToFavoritesOld(client.settings.getList("gamesFavorites"));
        }
        return result;
    }
    
    public Map<String, String> getStreamTagFavorites() {
        return client.settings.getMap("tagsFavorites");
    }
    
    public void putChannelInfoResult(final RequestResultCode result, String error) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.setPutResult(result, error);
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
            result.add(chan.getChannel());
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
            if (chan.getStreamName() != null) {
                streams.add(chan.getStreamName());
            }
        }
        return client.api.getStreamInfo(stream, streams);
    }
    
    /**
     * Outputs the full title if the StreamInfo for this channel is valid.
     * 
     * @param channel 
     */
    public void printStreamInfo(final Room room) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (client.settings.getBoolean("printStreamStatus")) {
                    StreamInfo info = getStreamInfo(room.getStream());
                    if (info.isValid()) {
                        printLine(room, "~" + info.getFullStatus() + "~");
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
        if (client.settings.getBoolean("requestFollowedStreams")) {
            if (client.settings.getList("scopes").contains(TokenInfo.Scope.FOLLOWS.scope)) {
                client.api.getFollowedStreams(client.settings.getString("token"));
            }
            else {
                EventLog.addSystemEvent("access.follows");
            }
        }
    }
    
    private void updateLaF() {
        LaF.setLookAndFeel(LaFSettings.fromSettings(client.settings));
        LaF.updateLookAndFeel();
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
                } else if (setting.equals("highlightMatchesAllEntries")) {
                    updateHighlightIncludeAllMatches();
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
                } else if (setting.equals("completionEnabled")) {
                    channels.setCompletionEnabled(bool);
                } else if (setting.equals("animatedEmotes")) {
                    emotesDialog.setEmoteImageType(Emoticon.makeImageType(bool));
                } else if (setting.equals("matchingSubstitutesEnabled")) {
                    updateHighlight();
                }
                if (setting.startsWith("title") || setting.equals("tabsChanTitles")) {
                    updateState(true);
                }
                loadMenuSetting(setting);
            }

            if (StyleManager.settingNames.contains(setting)) {
                BatchAction.queue(styleManager, () -> {
                    styleManager.refresh();
                    channels.refreshStyles();
                    highlightedMessages.refreshStyles();
                    ignoredMessages.refreshStyles();
                    streamChat.refreshStyles();
                    //menu.setForeground(styleManager.getColor("foreground"));
                    //menu.setBackground(styleManager.getColor("background"));
                });
            }
            if (setting.equals("displayNamesModeUserlist")) {
                channels.updateUserlistSettings();
            }
            if (type == Setting.STRING) {
                if (setting.equals("timeoutButtons") || setting.equals("banReasonsHotkey")) {
                    userInfoDialog.setUserDefinedButtonsDef(client.settings.getString("timeoutButtons"));
                } else if (setting.equals("token")) {
                    client.api.setToken((String)value);
                } else if (setting.equals("userid")) {
                    client.api.setLocalUserId((String)value);
                } else if (setting.equals("emoji")) {
                    emoticons.addEmoji((String)value);
                } else if (setting.equals("cheersType")) {
                    emoticons.setCheerState((String)value);
                } else if (setting.equals("backgroundColor")) {
                    emoticons.setCheerBackground(HtmlColors.decode((String)value));
                } else if (setting.equals("soundDevice")) {
                    Sound.setDeviceName((String)value);
                } else if (setting.equals("userDialogTimestamp")) {
                    userInfoDialog.setTimestampFormat(styleManager.makeTimestampFormat("userDialogTimestamp"));
                } else if (setting.equals("streamChatLogos")) {
                    client.updateStreamChatLogos();
                }
            }
            if (type == Setting.LIST) {
                if (setting.equals("highlight") || setting.equals("highlightBlacklist") || setting.equals("matchingSubstitutes")) {
                    updateHighlight();
                } else if (setting.equals("ignore") || setting.equals("ignoreBlacklist")) {
                    updateIgnore();
                } else if (setting.equals("matchingPresets")) {
                    updateMatchingPresets();
                } else if (setting.equals("filter")) {
                    updateFilter();
                } else if (setting.equals("hotkeys")) {
                    hotkeyManager.loadFromSettings(client.settings);
                } else if (setting.equals("streamChatChannels")) {
                    client.updateStreamChatLogos();
                } else if (setting.equals("localEmotes")) {
                    emoticons.setLocalEmotes(localEmotes.getData());
                    emotesDialog.update();
                }
            }
            if (type == Setting.LONG) {
                if (setting.equals("dialogFontSize")) {
                    userInfoDialog.setFontSize((Long)value);
                } else if (setting.equals("streamChatMessageTimeout")) {
                    streamChat.setMessageTimeout(((Long)value).intValue());
                } else if (setting.equals("emoteScaleDialog")) {
                    emotesDialog.setEmoteScale(((Long)value).intValue());
                } else if (setting.equals("smilies")) {
                    updateSmilies();
                }
            }
            if (setting.equals("liveStreamsSorting")
                    || setting.equals("liveStreamsSortingFav")
                    || setting.equals("liveStreamsFavsOnly")) {
                updateLiveStreamsDialog();
            }
            if (setting.equals("followersCompact") || setting.equals("followersReg")) {
                updateFollowerDialogs();
            }
            if (setting.equals("historyRange")) {
                updateHistoryRange();
            }
            if (setting.equals("historyVerticalZoom")) {
                updateHistoryVerticalZoom();
            }
            Set<String> notificationSettings = new HashSet<>(Arrays.asList(
                "nScreen", "nPosition", "nDisplayTime", "nMaxDisplayTime",
                "nMaxDisplayed", "nMaxQueueSize", "nActivity", "nActivityTime",
                "nKeepOpenOnHover"));
            if (notificationSettings.contains(setting)) {
                updateNotificationSettings();
            }
            if (setting.equals("spamProtection")) {
                client.setLinesPerSeconds((String)value);
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
                client.updateCustomCommands();
            }
            if (setting.equals("channelContextMenu")
                    || setting.equals("userContextMenu")
                    || setting.equals("livestreamerQualities")
                    || setting.equals("streamsContextMenu")
                    || setting.equals("textContextMenu")
                    || setting.equals("adminContextMenu")) {
                updateCustomContextMenuEntries();
            }
            else if (setting.equals("chatScrollbarAlways") || setting.equals("userlistWidth")) {
                updateChannelsSettings();
            }
            else if (setting.equals("ignoredEmotes")) {
                @SuppressWarnings("unchecked") // Setting
                List<String> data = client.settings.getList("ignoredEmotes");
                emoticons.setIgnoredEmotes(data);
                emotesDialog.update();
            }
            else if (LaF.shouldUpdate(setting)) {
                updateLaF();
            }
            else if (setting.equals("tabsPos")) {
                if (client.settings.getBoolean("tabsAutoSort")) {
                    channels.sortContent(null);
                }
            }
        }
    }
    
    private class MySettingsListener implements SettingsListener {

        @Override
        public void aboutToSaveSettings(Settings settings) {
            GuiUtil.edtAndWait(() -> {
                System.out.println("Saving GUI settings.");
                client.settings.setLong("favoritesSorting", favoritesDialog.getSorting());
                emoticons.saveFavoritesToSettings(settings);
                client.settings.setString("statusHistorySorting", adminDialog.getStatusHistorySorting());
                client.settings.putList("emoteHiddenSets", emotesDialog.getHiddenEmotesets());
            }, "Save GUI settings");
        }
        
    }
    
    public Settings getSettings() {
        return client.settings;
    }
    
    public Collection<String> getSettingNames() {
        return client.settings.getSettingNames();
    }
    
    public String getCustomCompletionItem(String key) {
        return (String)client.settings.mapGet("customCompletion", key);
    }
    
    public boolean isEmoteIgnored(Emoticon emote, int in) {
        return emoticons.isEmoteIgnored(emote, in);
    }
    
    public Collection<String> getCustomCommandNames() {
        return client.customCommands.getCommandNames();
    }
    
    public Collection<String> getCommandNames() {
        return client.commands.getCommandNames();
    }
    
    public void updateEmoteNames(Set<String> emotesets, Set<String> allEmotesets) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.updateLocalEmotes(emotesets, allEmotesets);
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
        
        private boolean liveStreamsHidden;
        
        @Override
        public void windowStateChanged(WindowEvent e) {
            if (e.getComponent() == MainGui.this) {
                saveState(e.getComponent());
                if (isMinimized()) {
                    if (liveStreamsDialog.isVisible()
                            && client.settings.getBoolean("hideStreamsOnMinimize")
                            && !liveStreamsDialog.isDocked()) {
                        liveStreamsDialog.setVisible(false);
                        liveStreamsHidden = true;
                    }
                    if (client.settings.getBoolean("minimizeToTray")) {
                        minimizeToTray();
                    }
                } else {
                    // Only cleanup from tray if not minimized, when minimized
                    // cleanup should never be done
                    cleanupAfterRestoredFromTray();
                    if (liveStreamsHidden) {
                        liveStreamsDialog.setVisible(true);
                        liveStreamsHidden = false;
                    }
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
    
    private void minimizeToTray() {
        minimizeToTray(client.settings.getBoolean("hidePopoutsIfTray"));
    }
    
    /**
     * Minimize window to tray.
     */
    private void minimizeToTray(boolean includePopouts) {
        //trayIcon.displayInfo("Minimized to tray", "Double-click icon to show again..");
        
        trayIcon.setIconVisible(true);
        if (!isMinimized()) {
            setExtendedState(getExtendedState() | ICONIFIED);
        }
        if (trayIcon.isAvailable()) {
            // Set visible to false, so it is removed from the taskbar, but only
            // if tray icon is actually added
            setVisible(false);
            if (includePopouts) {
                channels.getDock().minimizeWindows();
                channels.getDock().hideWindows();
            }
        }
        else {
            if (includePopouts) {
                channels.getDock().minimizeWindows();
            }
        }
    }
    
    private void minimize(boolean includePopouts) {
        setExtendedState(getExtendedState() | ICONIFIED);
        if (includePopouts) {
            channels.getDock().minimizeWindows();
        }
    }
    
    /**
     * Remove tray icon if applicable.
     */
    private void cleanupAfterRestoredFromTray() {
        if (client.settings.getLong("nType") != NotificationSettings.NOTIFICATION_TYPE_TRAY
                && !client.settings.getBoolean("trayIconAlways")) {
            trayIcon.setIconVisible(false);
        }
    }
    
    public void showPopupMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, text);
        });
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
                int result = errorMessage.show(error, previous, client.getOpenChannels().size());
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
