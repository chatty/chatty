
package chatty;

import chatty.gui.components.updating.Version;
import chatty.ChannelFavorites.Favorite;
import chatty.lang.Language;
import chatty.gui.colors.UsercolorManager;
import chatty.gui.components.admin.StatusHistory;
import chatty.util.api.pubsub.*;
import chatty.util.commands.CustomCommands;
import chatty.util.api.usericons.Usericon;
import chatty.util.api.usericons.UsericonManager;
import chatty.ChannelStateManager.ChannelStateListener;
import chatty.Chatty.PathType;
import chatty.Commands.CommandParsedArgs;
import chatty.util.api.TwitchApiResultListener;
import chatty.util.api.Emoticon;
import chatty.util.api.StreamInfoListener;
import chatty.util.api.TokenInfo;
import chatty.util.api.StreamInfo;
import chatty.util.api.ChannelInfo;
import chatty.util.api.TwitchApi;
import chatty.WhisperManager.WhisperListener;
import chatty.gui.GuiUtil;
import chatty.gui.laf.LaF;
import chatty.gui.laf.LaF.LaFSettings;
import chatty.gui.MainGui;
import chatty.gui.components.SelectReplyMessage;
import chatty.gui.components.SelectReplyMessage.SelectReplyMessageResult;
import chatty.gui.components.eventlog.EventLog;
import chatty.gui.components.menus.UserContextMenu;
import chatty.gui.components.textpane.ModLogInfo;
import chatty.gui.components.updating.Stuff;
import chatty.gui.defaults.DefaultsDialog;
import chatty.splash.Splash;
import chatty.util.BTTVEmotes;
import chatty.util.BotNameManager;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.EmoticonListener;
import chatty.util.IconManager;
import chatty.util.ffz.FrankerFaceZ;
import chatty.util.ffz.FrankerFaceZListener;
import chatty.util.ImageCache;
import chatty.util.LogUtil;
import chatty.util.MiscUtil;
import chatty.util.OtherBadges;
import chatty.util.ProcessManager;
import chatty.util.Pronouns;
import chatty.util.RawMessageTest;
import chatty.util.ReplyManager;
import chatty.util.Speedruncom;
import chatty.util.StreamHighlightHelper;
import chatty.util.StreamStatusWriter;
import chatty.util.StringUtil;
import chatty.util.Timestamp;
import chatty.util.TimerCommand;
import chatty.util.TimerCommand.TimerResult;
import chatty.util.TwitchEmotesApi;
import chatty.util.UserRoom;
import chatty.util.Webserver;
import chatty.util.api.AutoModCommandHelper;
import chatty.util.api.ChannelStatus;
import chatty.util.api.CheerEmoticon;
import chatty.util.api.EmotesetManager;
import chatty.util.api.EmoticonSizeCache;
import chatty.util.api.EmoticonUpdate;
import chatty.util.api.Emoticons;
import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.ResultManager;
import chatty.util.api.StreamCategory;
import chatty.util.api.StreamInfo.StreamType;
import chatty.util.api.StreamInfo.ViewerStats;
import chatty.util.api.TwitchApi.RequestResultCode;
import chatty.util.api.UserInfo;
import chatty.util.api.eventsub.EventSubListener;
import chatty.util.api.eventsub.EventSubManager;
import chatty.util.api.eventsub.payloads.PollPayload;
import chatty.util.api.eventsub.payloads.RaidPayload;
import chatty.util.api.eventsub.payloads.ShieldModePayload;
import chatty.util.api.eventsub.payloads.ShoutoutPayload;
import chatty.util.api.pubsub.RewardRedeemedMessageData;
import chatty.util.api.pubsub.Message;
import chatty.util.api.pubsub.ModeratorActionData;
import chatty.util.api.pubsub.PubSubListener;
import chatty.util.api.pubsub.UserModerationMessageData;
import chatty.util.chatlog.ChatLog;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import chatty.util.irc.MsgTags;
import chatty.util.settings.FileManager;
import chatty.util.settings.Settings;
import chatty.util.settings.SettingsListener;
import chatty.util.seventv.SevenTV;
import chatty.util.srl.SpeedrunsLive;
import java.awt.Color;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * The main client class, responsible for managing most parts of the program.
 * 
 * @author tduva
 */
public class TwitchClient {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchClient.class.getName());
    
    private volatile boolean shuttingDown = false;
    private volatile boolean settingsAlreadySavedOnExit = false;
    
    /**
     * The URL to get a token. Needs to end with the scopes so other ones can be
     * added.
     */
    public static final String REQUEST_TOKEN_URL = ""
            + "https://id.twitch.tv/oauth2/authorize"
            + "?response_type=token"
            + "&client_id="+Chatty.CLIENT_ID
            + "&redirect_uri="+Chatty.REDIRECT_URI
            + "&force_verify=true"
            + "&scope=";

    /**
     * Holds the Settings object, which is used to store and retrieve renametings
     */
    public final Settings settings;
    
    public final ChatLog chatLog;
    
    private final TwitchConnection c;
    
    /**
     * Holds the TwitchApi object, which is used to make API requests
     */
    public final TwitchApi api;
    
    public final chatty.util.api.pubsub.Manager pubsub;
    private final PubSubResults pubsubListener = new PubSubResults();
    
    public final EventSubManager eventSub;
    
    public final EmotesetManager emotesetManager;
    
    public final BTTVEmotes bttvEmotes;
    
    public final FrankerFaceZ frankerFaceZ;
    
    public final SevenTV sevenTV;
    
    public final ChannelFavorites channelFavorites;
    
    public final UsercolorManager usercolorManager;
    
    public final UsericonManager usericonManager;
    
    public final Addressbook addressbook;
    
    public final SpeedrunsLive speedrunsLive;
    
    public final Speedruncom speedruncom;
    
    public final StatusHistory statusHistory;
    
    public final StreamStatusWriter streamStatusWriter;
    
    protected final BotNameManager botNameManager;
    
    protected final CustomNames customNames;
    
    private final AutoModCommandHelper autoModCommandHelper;
    
    public final RoomManager roomManager;
    
    /**
     * Holds the UserManager instance, which manages all the user objects.
     */
    //protected UserManager users = new UserManager();
    
    /**
     * A reference to the Main Gui.
     */
    protected MainGui g;
    
    private final List<String> cachedDebugMessages = new ArrayList<>();
    private final List<String> cachedWarningMessages = new ArrayList<>();
    
    /**
     * User used for testing without connecting.
     */
    private User testUser;
    private final StreamInfo testStreamInfo = new StreamInfo("testStreamInfo", null);
    
    private Webserver webserver;
    private final SettingsManager settingsManager;
    private final SpamProtection spamProtection;
    public final CustomCommands customCommands;
    public final Commands commands = new Commands();
    private final TimerCommand timerCommand;
    
    private final StreamHighlightHelper streamHighlights;
    
    private final Set<String> refreshRequests = Collections.synchronizedSet(new HashSet<String>());
    
    private final WhisperManager w;
    private final IrcLogger ircLogger;
    
    private boolean fixServer = false;
    private String launchCommand;
    
    public TwitchClient(Map<String, String> args) {
        // Logging
        new Logging(this);
        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler());
        
        LOGGER.info("### Log start ("+DateTime.fullDateTime()+")");
        LOGGER.info(Chatty.chattyVersion());
        LOGGER.info(Helper.systemInfo());
        LOGGER.info("[Working Directory] "+System.getProperty("user.dir")
                +" [Settings Directory] "+Chatty.getPath(PathType.SETTINGS)
                +" [Classpath] "+System.getProperty("java.class.path")
                +" [Launch Options] "+ManagementFactory.getRuntimeMXBean().getInputArguments());
        
        Helper.checkSLF4JBinding();
        
        // Settings
        settingsManager = new SettingsManager();
        settings = settingsManager.settings;
        settingsManager.defineSettings();
        settingsManager.loadSettingsFromFile();
        settingsManager.loadCommandLineSettings(args);
        settingsManager.overrideSettings();
        settingsManager.debugSettings();
        settingsManager.backupFiles();
        settingsManager.startAutoSave(this);
        
        Chatty.setSettings(settings);
        
        Language.setLanguage(settings.getString("language"));
        /**
         * Not sure how much there is that doesn't get affected by a locale
         * change at this point, but it's pretty much as early as it can be when
         * loaded from the settings.
         */
        Helper.setDefaultLocale(settings.getString("locale"));
        Helper.setDefaultTimezone(settings.getString("timezone"));
        
        String pathDebug = Chatty.getPathDebug();
        if (!StringUtil.isNullOrEmpty(pathDebug)) {
            LOGGER.info(pathDebug);
        }
        
        launchCommand = args.get("cc");
        
        addressbook = new Addressbook(Chatty.getPath(PathType.SETTINGS).resolve("addressbook").toString(),
            Chatty.getPath(PathType.SETTINGS).resolve("addressbookImport.txt").toString(), settings);
        if (!addressbook.loadFromSettings()) {
            addressbook.loadFromFile();
        }
        addressbook.setSomewhatUniqueCategories(settings.getString("abUniqueCats"));
        if (settings.getBoolean("abAutoImport")) {
            addressbook.enableAutoImport();
        }
        
        initDxSettings();
        /**
         * Set a proper user agent. The default Java user agent may be rejected
         * by some servers.
         */
        System.setProperty("http.agent", "Chatty "+Chatty.VERSION);
        System.setProperty("jna.debug_load", "true");
        
        // After graphic settings (what is changed here shouldn't affect stuff before this
        if (!settingsManager.wasMainFileLoaded()) {
            DefaultsDialog.showAndWait(settings);
        }
        
        IconManager.setCustomIcons(settings.getList("icons"));
        if (settings.getBoolean("splash")) {
            Splash.initSplashScreen(Splash.getLocation((String)settings.mapGet("windows", "main")));
        }

        // Create after Logging is created, since that resets some stuff
        ircLogger = new IrcLogger();

        createTestUser("tduva", "");
        
        api = new TwitchApi(new TwitchApiResults(), new MyStreamInfoListener());
        addTwitchApiResultListeners();
        bttvEmotes = new BTTVEmotes(new EmoteListener(), api);
        TwitchEmotesApi.api.setTwitchApi(api);
        Timestamp.setTwitchApi(api);
        
        pubsub = new chatty.util.api.pubsub.Manager(
                settings.getString("pubsub"), pubsubListener, api);
        eventSub = new EventSubManager("wss://eventsub-beta.wss.twitch.tv/ws", new EventSubResults(), api);
//        eventSub = new EventSubManager("ws://localhost:8080/eventsub", new EventSubResults(), api);
        
        frankerFaceZ = new FrankerFaceZ(new EmoticonsListener(), settings, api);
        sevenTV = new SevenTV(new EmoteListener(), api);
        
        ImageCache.setDefaultPath(Chatty.getPathCreate(PathType.CACHE).resolve("img"));
        ImageCache.setCachingEnabled(settings.getBoolean("imageCache"));
        ImageCache.deleteExpiredFiles();
        EmoticonSizeCache.loadFromFile();

        usercolorManager = new UsercolorManager(settings);
        usericonManager = new UsericonManager(settings);
        customCommands = new CustomCommands(settings, api, this);
        botNameManager = new BotNameManager(settings);
        settings.addSettingsListener(new SettingSaveListener());

        streamHighlights = new StreamHighlightHelper(settings, api);
        
        customNames = new CustomNames(settings);
        
        chatLog = new ChatLog(settings);
        chatLog.start();
        
        testUser.setUserSettings(new User.UserSettings(100, usercolorManager, addressbook, usericonManager));
        
        speedrunsLive = new SpeedrunsLive();
        speedruncom = new Speedruncom(api);
        
        statusHistory = new StatusHistory(settings);
        settings.addSettingsListener(statusHistory);
        
        spamProtection = new SpamProtection();
        spamProtection.setLinesPerSeconds(settings.getString("spamProtection"));
        
        roomManager = new RoomManager(new MyRoomUpdatedListener());
        channelFavorites = new ChannelFavorites(settings, roomManager);
        
        c = new TwitchConnection(new Messages(), settings, "main", roomManager);
        c.setUserSettings(new User.UserSettings(
                settings.getInt("userDialogMessageLimit"),
                usercolorManager, addressbook, usericonManager));
        c.setCustomNamesManager(customNames);
        c.setBotNameManager(botNameManager);
        c.addChannelStateListener(new ChannelStateUpdater());
        c.setMaxReconnectionAttempts(settings.getLong("maxReconnectionAttempts"));
        
        w = new WhisperManager(new MyWhisperListener(), settings, c, this);
        
        streamStatusWriter = new StreamStatusWriter(Chatty.getPath(PathType.EXPORT), api);
        streamStatusWriter.setSetting(settings.getString("statusWriter"));
        streamStatusWriter.setEnabled(settings.getBoolean("enableStatusWriter"));
        settings.addSettingChangeListener(streamStatusWriter);
        
        LaF.setLookAndFeel(LaFSettings.fromSettings(settings));
        GuiUtil.addMacKeyboardActions();
        
        // Create GUI
        LOGGER.info("Create GUI..");
        g = new MainGui(this);
        g.loadSettings();
        emotesetManager = new EmotesetManager(api, g, settings);
        g.showGui();
        
        autoModCommandHelper = new AutoModCommandHelper(g, api);
        
        timerCommand = new TimerCommand(settings, new TimerCommand.TimerAction() {
            @Override
            public void performAction(String command, String chan, Parameters parameters, Set<TimerCommand.Option> options) {
                if (isChannelOpen(chan)) {
                    textInput(c.getRoomByChannel(chan), command, parameters);
                }
                else if (options.contains(TimerCommand.Option.CHANNEL_LENIENT)) {
                    parameters.put(TimerCommand.TIMER_PARAMETERS_KEY_CHANGED_CHANNEL, "true");
                    textInput(g.getActiveRoom(), command, parameters);
                }
                else {
                    g.printSystem("Timed command not run, channel " + chan + " not open or not valid");
                    g.printTimerLog("Timed command not run, channel " + chan + " not open or not valid: " + command);
                }
            }

            @Override
            public void log(String line) {
                g.printTimerLog(line);
            }
        });
        
        if (Chatty.DEBUG) {
            Room testRoom =  Room.createRegular("");
            g.addUser(new User("josh", testRoom));
            g.addUser(new User("joshua", testRoom));
            User j = new User("joshimuz", "Joshimuz", testRoom);
            for (int i=0;i<99;i++) {
                j.addMessage("abc", false, null);
            }
            j.addMessage("abc", false, "abc-id");
            j.addMessage("blah", true, "blah-id");
            j.setDisplayNick("Joshimoose");
            j.setTurbo(true);
            j.setVip(true);
            g.addUser(j);
            g.addUser(new User("jolzi", testRoom));
            g.addUser(new User("john", testRoom));
            User t = new User("tduva", testRoom);
            for (int i=0;i<100;i++) {
                t.addMessage("abc", false, null);
            }
            t.setModerator(true);
            g.addUser(t);
            User kb = new User("kabukibot", "Kabukibot", testRoom);
            for (int i=0;i<80;i++) {
                kb.addMessage("abc", false, null);
            }
            kb.clearLinesIfInactive(0);
            kb.addMessage("abc", false, null);
            kb.setDisplayNick("reallyLongDisplayNickAndStuffBlahNeedsToBeLonger");
            kb.setBot(true);
            g.addUser(kb);
            User l = new User("lotsofs", "LotsOfS", testRoom);
            for (int i=0;i<120;i++) {
                l.addMessage("abc", false, null);
            }
            l.clearLinesIfInactive(0);
            for (int i=0;i<100;i++) {
                l.addMessage("abc", false, null);
            }
            l.addMessage("abc", false, null);
            l.setSubscriber(true);
            g.addUser(l);
            User a = new User("anders", testRoom);
            for (int i=0;i<120;i++) {
                a.addMessage("abc", false, null);
            }
            a.setSubscriber(true);
            a.setVip(true);
            g.addUser(a);
            g.addUser(new User("apex1", testRoom));
            g.addUser(new User("xfwefawf32q4543t5greger", testRoom));
            User af = new User("applefan", testRoom);
            for (int i=0;i<101;i++) {
                af.addMessage("abc", false, null);
            }
//            Map<String, String> badges = new LinkedHashMap<>();
//            badges.put("bits", "100");
//            af.setTwitchBadges(badges);
//            g.addUser("", af);
//            g.addUser("", new User("austrian_", ""));
//            g.addUser("", new User("adam_ak", ""));
//            g.addUser("", new User("astroman", ""));
//            g.addUser("", new User("xxxandre369xxx", ""));
//            g.addUser("", new User("all_that_stuff_", ""));
//            g.addUser("", new User("adam_ak_stole_my_bike", ""));
//            g.addUser("", new User("bikelover", ""));
//            g.addUser("", new User("bicyclefan", ""));
//            g.addUser("", new User("botnak", "Botnak", ""));
//            g.addUser("", new User("brett", ""));
//            g.addUser("", new User("bll", ""));
//            g.addUser("", new User("bzp______________", ""));
//            g.addUser("", new User("7_dm", ""));
            
            String[] chans = new String[]{"europeanspeedsterassembly","esamarathon2","heinki","joshimuz","lotsofs","test","a","b","c"};
            for (String chan : chans) {
                //g.printLine(chan, "test");
            }
        }
    }
    
    public void init() {
        LOGGER.info("GUI shown");
        Splash.closeSplashScreen();
        
        // Output any cached warning messages
        warning(null);
        
        if (!settingsManager.checkSettingsDir()) {
            warning("The settings directory could not be created, so Chatty"
                    + " will not function correctly. Make sure that "+Chatty.getPath(PathType.SETTINGS)
                    + " is accessible or change it using launch options.");
            return;
        }
        
        addCommands();
        g.addGuiCommands();
        updateCustomCommands();
        
        // Request some stuff
        // Don't request for now, since new API is a bit weird with :) emotes
//        api.getEmotesBySets("0");
        
        // Before checkNewVersion(), so "updateAvailable" is already updated
        checkForVersionChange();
        // Check version, if enabled in this build
        if (Chatty.VERSION_CHECK_ENABLED) {
            checkNewVersion();
        }
        
        Helper.parseChannelHelper = new Helper.ParseChannelHelper() {
            @Override
            public Collection<String> getFavorites() {
                return channelFavorites.getFavorites();
            }

            @Override
            public Collection<String> getNamesByCategory(String category) {
                return addressbook.getNamesByCategory(category);
            }

            @Override
            public boolean isStreamLive(String stream) {
                StreamInfo info = api.getCachedStreamInfo(stream);
                return info != null && info.isValidEnough() && info.getOnline();
            }
        };
        
        // Connect or open connect dialog
        if (settings.getBoolean("connectOnStartup")) {
            prepareConnection();
        } else {
            switch ((int)settings.getLong("onStart")) {
                case 1:
                    g.openConnectDialog(null);
                    break;
                case 2:
                    prepareConnectionWithChannel(settings.getString("autojoinChannel"));
                    break;
                case 3:
                    prepareConnectionWithChannel(settings.getString("previousChannel"));
                    break;
                case 4:
                    prepareConnectionWithChannel(Helper.buildStreamsString(channelFavorites.getFavorites()));
                    break;
            }
            
        }
        
        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown(this)));
        
        if (Chatty.DEBUG) {
            //textInput(Room.EMPTY, "/test3");
        }
        
        UserContextMenu.client = this;
        
        customCommandLaunch(launchCommand);
        launchCommand = null;
        
        String timerCommandLoadResult = timerCommand.loadFromSettings(settings);
        if (timerCommandLoadResult != null) {
            g.printSystem(timerCommandLoadResult);
        }
        
        String customPathsWarning = Chatty.getInvalidPathInfo();
        if (!customPathsWarning.isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(g, customPathsWarning));
        }
    }
    

    /**
     * Based on the current renametings, rename the system properties to disable
     * Direct3D and/or DirectDraw.
     */
    private void initDxSettings() {
        try {
            Boolean d3d = !settings.getBoolean("nod3d");
            Boolean ddraw = settings.getBoolean("noddraw");
            String uiScale = null;
            if (settings.getLong("uiScale") > 0) {
                uiScale = String.valueOf(settings.getLong("uiScale") / 100.0);
            }
            LOGGER.info(String.format("d3d: %s (%s) / noddraw: %s (%s) / opengl: (%s) / retina: %s / uiScale: %s",
                    d3d, System.getProperty("sun.java2d.d3d"),
                    ddraw, System.getProperty("sun.java2d.noddraw"),
                    System.getProperty("sun.java2d.opengl"),
                    GuiUtil.hasRetinaDisplay(),
                    uiScale));
            System.setProperty("sun.java2d.d3d", d3d.toString());
            System.setProperty("sun.java2d.noddraw", ddraw.toString());
            if (uiScale != null) {
                System.setProperty("sun.java2d.uiScale", uiScale);
            }
        } catch (SecurityException ex) {
            LOGGER.warning("Error setting drawing settings: "+ex.getLocalizedMessage());
        }
    }
    
    public void updateCustomCommands() {
        customCommands.update(commands);
    }
    
    /**
     * Check if the current version (Chatty.VERSION) is different from the
     * "currentVersion" in the settings, which means a different version is
     * being run compared to the last time.
     * 
     * If a new version is detected, updates the "currentVersion" setting,
     * clears the "updateAvailable" setting and opens the release info.
     */
    private void checkForVersionChange() {
        String currentVersion = settings.getString("currentVersion");
        if (!currentVersion.equals(Chatty.VERSION)) {
            settings.setString("currentVersion", Chatty.VERSION);
            // Changed version, so should check for update properly again
            settings.setString("updateAvailable", "");
            if (settingsManager.wasMainFileLoaded()) {
                // Don't bother user if settings were corrupted or new install
                g.openReleaseInfo();
            }
        }
    }
    
    /**
     * Checks for a new version if the last check was long enough ago.
     */
    private void checkNewVersion() {
        Version.check(settings, (newVersion,releases) -> {
            if (newVersion != null) {
                g.setUpdateAvailable(newVersion, releases);
            } else {
                g.printSystem("You already have the newest version.");
            }
        });
    }
    
    /**
     * Creates the test user, also allowing it to be recreated with another name
     * or channel for testing while the programm is running.
     * 
     * @param name
     * @param channel 
     */
    private void createTestUser(String name, String channel) {
        testUser = new User(name, name, Room.createRegular(channel));
        testUser.setColor(new Color(94, 0, 211));
        // Force color correction for longer userinfo color label
        testUser.setColor(new Color(255, 255, 255));
        //testUser.setColor(new Color(0,216,107));
        //testUser.setBot(true);
        //testUser.setTurbo(true);
        testUser.setModerator(true);
        testUser.setSubscriber(true);
        //testUser.setAdmin(true);
        //testUser.setStaff(true);
        //testUser.setBroadcaster(true);
//        LinkedHashMap<String, String> badgesTest = new LinkedHashMap<>();
//        badgesTest.put("global_mod", "1");
//        badgesTest.put("moderator", "1");
//        badgesTest.put("premium", "1");
//        badgesTest.put("bits", "1000000");
//        testUser.setTwitchBadges(badgesTest);
    }
    
    /**
     * Close all channels except the ones in the given Array.
     * 
     * @param except 
     */
    private void closeAllChannelsExcept(String[] except) {
        Set<String> copy = c.getOpenChannels();
        for (String channel : copy) {
            if (!Arrays.asList(except).contains(channel)) {
                closeChannel(channel);
            }
        }
    }
    
    /**
     * Close a channel by either parting it if it is currently joined or
     * just closing the tab.
     * 
     * @param channel 
     */
    public void closeChannel(String channel) {
        if (c.onChannel(channel)) {
            c.partChannel(channel);
        }
        else { // Always remove channel (or try to), so it can be closed even if it bugged out
            Room room = roomManager.getRoom(channel);
            logViewerstats(channel);
            c.closeChannel(channel);
            closeChannelStuff(room);
            g.removeChannel(channel);
            chatLog.closeChannel(room.getFilename());
            updateStreamInfoChannelOpen(channel);
        }
    }
    
    private void closeChannelStuff(Room room) {
        // Check if not on any associated channel anymore
        if (!c.onOwnerChannel(room.getOwnerChannel())) {
            frankerFaceZ.left(room.getOwnerChannel());
            pubsub.unlistenModLog(room.getStream());
            pubsub.unlistenUserModeration(room.getStream());
            pubsub.unlistenPoints(room.getStream());
            eventSub.unlistenRaid(room.getStream());
            eventSub.unlistenPoll(room.getStream());
            eventSub.unlistenShield(room.getStream());
            eventSub.unlistenShoutouts(room.getStream());
        }
    }
    
    private void addressbookCommands(String channel, User user, String text) {
        if (settings.getString("abCommandsChannel").equalsIgnoreCase(channel)
                && user.isModerator()) {
            text = text.trim();
            if (text.length() < 2) {
                return;
            }
            String command = text.split(" ")[0].substring(1);
            List<String> activatedCommands =
                    Arrays.asList(settings.getString("abCommands").split(","));
            if (activatedCommands.contains(command)) {
                String commandText = text.substring(1);
                g.printSystem("[Ab/Mod] "+addressbook.command(commandText));
            }
        }
    }

    public String getUsername() {
        return c.getUsername();
    }
    
    public User getUser(String channel, String name) {
        return c.getUser(channel, name);
    }
    
    public User getExistingUser(String channel, String name) {
        return c.getExistingUser(channel, name);
    }
    
    public User getLocalUser(String channel) {
        return c.getExistingUser(channel, c.getUsername());
    }
    
    public void clearUserList() {
        c.setAllOffline();
        g.clearUsers(null);
    }
    
    private String getServer() {
        String serverDefault = settings.getString("serverDefault");
        String serverTemp = settings.getString("server");
        return serverTemp.length() > 0 ? serverTemp : serverDefault;
    }
    
    private String getPorts() {
        String portDefault = settings.getString("portDefault");
        String portTemp = settings.getString("port");
        return portTemp.length() > 0 ? portTemp : portDefault;
    }
    
    /**
     * Prepare connection using renametings and default server.
     * 
     * @return 
     */
    public final boolean prepareConnection() {
        return prepareConnection(getServer(), getPorts());
    }
    
    public boolean prepareConnection(boolean rejoinOpenChannels) {
        if (rejoinOpenChannels) {
            return prepareConnection(getServer(), getPorts(), null);
        } else {
            return prepareConnection();
        }
    }
    
    public final boolean prepareConnectionWithChannel(String channel) {
        return prepareConnection(getServer(), getPorts(), channel);
    }
    
    public boolean prepareConnection(String server, String ports) {
        return prepareConnection(server, ports, settings.getString("channel"));
    }
    
    public final boolean prepareConnectionAnyChannel(String server, String ports) {
        String channel = null;
        if (c.getOpenChannels().isEmpty()) {
            channel = settings.getString("channel");
        }
        return prepareConnection(server, ports, channel);
    }
    
    /**
     * Prepares the connection while getting everything from the renametings,
     * except the server/port.
     *
     * @param server
     * @param ports
     * @return
     */
    public boolean prepareConnection(String server, String ports, String channel) {
        String username = settings.getString("username");
        String password = settings.getString("password");
        boolean usePassword = settings.getBoolean("usePassword");
        String token = settings.getString("token");
        
        String login = "oauth:"+token;
        if (token.isEmpty()) {
            login = "";
        }
        
        if (usePassword) {
            login = password;
            LOGGER.info("Using password instead of token.");
        }
        
        return prepareConnection(username,login,channel,server, ports);
    }
    
    /**
     * Prepare connection using given credentials and channel, but use default
     * server.
     * 
     * @param name
     * @param password
     * @param channel
     * @return 
     */
//    public boolean prepareConnection(String name, String password, String channel) {
//        return prepareConnection(name, password, channel, getServer(), getPorts());
//    }
    
    /**
     * Prepares the connection to the given channel with the given credentials.
     * 
     * This does stuff that should only be done once, unless the given parameters
     * change. So this shouldn't be repeated for just reconnecting.
     * 
     * @param name The username to use for connecting.
     * @param password The password to connect with.
     * @param channel The channel(s) to join after connecting, if this is null
     * then it rejoins the currently open channels (if any)
     * @param server The server to connect to.
     * @param ports The port to connect to.
     * @return true if no formal error occured, false otherwise
     */
    public boolean prepareConnection(String name, String password,
            String channel, String server, String ports) {
        
        fixServer = false;
        
        if (c.getState() > Irc.STATE_OFFLINE) {
            g.showMessage("Cannot connect: Already connected.");
            return false;
        }
        
        if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
            g.showMessage(Language.getString("connect.error.noLogin"));
            showConnectDialogIfMissing();
            return false;
        }
        
        String[] autojoin;
        Set<String> openChannels = c.getOpenChannels();
        if (channel == null) {
            autojoin = new String[openChannels.size()];
            openChannels.toArray(autojoin);
        } else {
            autojoin = Helper.parseChannels(channel);
        }
        if (autojoin.length == 0) {
            g.showMessage(Language.getString("connect.error.noChannel"));
            showConnectDialogIfMissing();
            return false;
        }
        
        if (server == null || server.isEmpty()) {
            g.showMessage("Invalid server specified.");
            return false;
        }
        
        closeAllChannelsExcept(autojoin);
        
        settings.setString("username", name);
        if (channel != null) {
            settings.setString("channel", channel);
        }
        api.requestUserId(Helper.toStream(autojoin));
//        api.getEmotesByStreams(Helper.toStream(autojoin)); // Removed
        c.connect(server, ports, name, password, autojoin);
        return true;
    }
    
    private void showConnectDialogIfMissing() {
        if (settings.getBoolean("connectDialogIfMissing")) {
            g.openConnectDialog(null);
        }
    }
    
    public boolean disconnect() {
        return c.disconnect();
    }
    
    public void joinChannels(Set<String> channels) {
        c.joinChannels(channels);
    }
    
    public void joinChannel(String channels) {
        c.joinChannel(channels);
    }
    
    public int getState() {
        return c.getState();
    }

    /**
     * Directly entered into the input box or entered by Custom Commands.
     * 
     * This must be safe input (i.e. input directly by the local user) because
     * this can execute all kind of commands.
     * 
     * @param room
     * @param commandParameters
     * @param text 
     */
    public void textInput(Room room, String text, Parameters commandParameters) {
        if (text.isEmpty()) {
            return;
        }
        Debugging.println("textinput", "'%s' in %s (%s)", text, room, commandParameters);
        text = g.replaceEmojiCodes(text);
        String channel = room.getChannel();
        if (text.startsWith("//")) {
            anonCustomCommand(room, text.substring(1), commandParameters);
        }
        else if (text.startsWith("/")) {
            commandInput(room, text, commandParameters);
        }
        else if (!checkRejectTimedMessage(room, commandParameters)) {
            if (c.onChannel(channel)) {
                sendMessage(channel, text, true);
            }
            else if (channel.startsWith("$")) {
                w.whisperChannel(channel, text);
            }
            else if (channel.startsWith("*")) {
                c.sendCommandMessage(channel, text, "> "+text);
            }
            else {
                // For testing:
                // (Also creates a channel with an empty string)
                if (Chatty.DEBUG) {
                    User user = c.getUser(room.getChannel(), "test");
                    if (testUser.getRoom().equals(room)) {
                        user = testUser;
                    }
                    g.printMessage(user,text,false);
                } else {
                    g.printLine("Not in a channel");
                }
            }
        }     
    }
    
    private void sendMessage(String channel, String text) {
        if (c.onChannel(channel, true)) {
            sendMessage(channel, text, false);
        }
    }
    
    /**
     * 
     * @param channel
     * @param text
     * @param allowCommandMessageLocally Commands like !highlight, which
     * normally only working for received messages, will be triggered when
     * sending a message as well
     */
    private void sendMessage(String channel, String text, boolean allowCommandMessageLocally) {
        if (sendAsReply(channel, text)) {
            return;
        }
        if (c.sendSpamProtectedMessage(channel, text, false)) {
            User user = c.localUserJoined(channel);
            g.printMessage(user, text, false);
            if (allowCommandMessageLocally) {
                modCommandAddStreamHighlight(user, text, MsgTags.EMPTY);
            }
        } else {
            g.printLine("# Message not sent to prevent ban: " + text);
        }
    }
    
    /**
     * Check if the message should be sent as a reply.
     * 
     * @param channel The channel to send the message to (not null)
     * @param text The text to send (not null)
     * @return true if the message was handled by this method, false if it
     * should be sent normally
     */
    private boolean sendAsReply(String channel, String text) {
        boolean restricted = settings.getBoolean("mentionReplyRestricted");
        boolean doubleAt = text.startsWith("@@");
        if (doubleAt || (!restricted && text.startsWith("@"))) {
            String[] split = text.split(" ", 2);
            // Min username length may be 1 or 2, depending on @@ or @
            if (split.length == 2 && split[0].length() > 2 && split[1].length() > 0) {
                String username = split[0].substring(doubleAt ? 2 : 1);
                String actualMsg = split[1];
                User user = c.getExistingUser(channel, username);
                if (user != null) {
                    SelectReplyMessage.settings = settings;
                    SelectReplyMessageResult result = SelectReplyMessage.show(user);
                    if (result.action != SelectReplyMessageResult.Action.SEND_NORMALLY) {
                        // Should not send normally, so return true
                        if (result.action == SelectReplyMessageResult.Action.REPLY) {
                            // If changed to parent msg-id, atMsg will be null
                            sendReply(channel, actualMsg, username, result.atMsgId, result.atMsg);
                        }
                        else {
                            g.insert(text, false);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Send a reply.
     * 
     * @param channel The channel to send to (not null)
     * @param text The text to send (not null)
     * @param atUsername The username to address (not null)
     * @param atMsgId The msg-id to use as reply thread parent (not null)
     * @param atMsg The parent msg text (may be null)
     */
    private void sendReply(String channel, String text, String atUsername, String atMsgId, String atMsg) {
        MsgTags tags = MsgTags.create("reply-parent-msg-id", atMsgId);
        if (c.sendSpamProtectedMessage(channel, text, false, tags)) {
            User user = c.localUserJoined(channel);
            String localOutputText = text;
            if (!text.startsWith("@")) {
                localOutputText = String.format("@%s %s",
                        atUsername, text);
            }
            ReplyManager.addReply(atMsgId, null,
                    String.format("<%s> %s", user.getName(), localOutputText),
                    atMsg != null ? String.format("<%s> %s", atUsername, atMsg) : null);
            g.printMessage(user, localOutputText, false, tags);
        }
        else {
            g.printLine("# Message not sent to prevent ban: " + text);
        }
    }
    
    private boolean checkRejectTimedMessage(Room room, Parameters parameters) {
        User localUser = getLocalUser(room.getChannel());
        boolean isTimed = parameters != null
                            && parameters.hasKey(TimerCommand.TIMER_PARAMETERS_KEY);
        boolean rejected = isTimed &&
                            (
                                !Helper.isRegularChannelStrict(room.getChannel())
                                || localUser == null
                                || !localUser.hasModeratorRights()
                                || parameters.hasKey(TimerCommand.TIMER_PARAMETERS_KEY_CHANGED_CHANNEL)
                            );
        if (rejected) {
            g.printSystem(room, "Could not send timed message (not a moderator or invalid channel)");
        }
        return rejected;
    }
    
    private boolean rejectTimedMessage(String command, Room room, Parameters parameters) {
        boolean rejected = parameters != null && parameters.hasKey(TimerCommand.TIMER_PARAMETERS_KEY);
        if (rejected) {
            g.printSystem(room, "Could not send timed message (command /"+command+" not allowed)");
            g.printTimerLog("Could not send timed message (command /"+command+" not allowed): "+parameters.getArgs());
        }
        return rejected;
    }
    
    /**
     * Checks if the given channel should be open.
     * 
     * @param channel The channel name
     * @return 
     */
    public boolean isChannelOpen(String channel) {
        return c.isChannelOpen(channel);
    }
    
    public boolean isChannelJoined(String channel) {
        return c.onChannel(channel, false);
    }
    
    public void updateStreamInfoChannelOpen(String channel) {
        StreamInfo streamInfo = api.getCachedStreamInfo(Helper.toStream(channel));
        if (streamInfo != null) {
            streamInfo.setIsOpen(c.isChannelOpen(channel));
        }
    }
    
    public boolean isUserlistLoaded(String channel) {
        return c.isUserlistLoaded(channel);
    }
    
    /**
     * Execute a command from input, which means the text starts with a '/',
     * followed by the command name and comma-separated arguments.
     * 
     * Use {@link #commandInput(Room, String, Parameters)} to carry over extra
     * parameters.
     * 
     * @param room The room context
     * @param text The raw text
     * @return 
     */
    public boolean commandInput(Room room, String text) {
        return commandInput(room, text, null);
    }
    
    /**
     * Execute a command from input, which means the text starts with a '/',
     * followed by the command name and comma-separated arguments.
     * 
     * @param room The room context
     * @param text The raw text
     * @param parameters The parameters to carry over (args will be overwritten)
     * @return 
     */
    public boolean commandInput(Room room, String text, Parameters parameters) {
        String[] split = text.split(" ", 2);
        String command = split[0].substring(1);
        String args = null;
        if (split.length == 2) {
            args = split[1];
        }
        
        // Overwrite args in Parameters with current
        if (parameters == null) {
            parameters = Parameters.create(args);
        } else {
            parameters.putArgs(args);
        }
        return command(room, command, parameters);
    }
    
    /**
     * Executes the command with the given name, which can be a built-in or
     * Custom command, with no parameters.
     * 
     * @param room The room context
     * @param command The command name (no leading /)
     * @return 
     */
    public boolean command(Room room, String command) {
        return command(room, command, Parameters.create(null));
    }
    
    /**
     * Executes the command with the given name, which can be a built-in or
     * Custom Command.
     * 
     * @param room The room context
     * @param command The command name (no leading /)
     * @param parameter The parameter, can be null
     * @return 
     */
    public boolean command(Room room, String command, String parameter) {
        return command(room, command, Parameters.create(parameter));
    }
    

    private void addCommands() {
        
        //---------------
        // Connection/IRC
        //---------------
        commands.add("quit", p -> c.quit());
        commands.add("server", p -> {
            commandServer(p.getArgs());
        });
        commands.add("reconnect", p -> commandReconnect());
        commands.add("connection", p -> {
            g.printLine(p.getRoom(), c.getConnectionInfo());
        });
        commands.add("join", p -> {
            commandJoinChannel(p.getArgs());
        });
        commands.add("part", p -> {
            commandPartChannel(p.getChannel());
        }, "close");
        commands.add("rejoin", p -> {
            commandRejoinChannel(p.getChannel());
        });
        commands.add("raw", p -> {
            if (rejectTimedMessage("raw", p.getRoom(), p.getParameters())) {
                return;
            }
            if (p.hasArgs()) {
                c.sendRaw(p.getArgs());
            }
        });
        commands.add("me", p -> {
            if (checkRejectTimedMessage(p.getRoom(), p.getParameters())) {
                return;
            }
            commandActionMessage(p.getChannel(), p.getArgs());
        });
        commands.add("say", p -> {
            if (checkRejectTimedMessage(p.getRoom(), p.getParameters())) {
                return;
            }
            if (p.hasArgs()) {
                sendMessage(p.getChannel(), p.getArgs());
            }
            else {
                g.printLine(p.getRoom(), "Usage: /say <message>");
            }
        });
        commands.add("msg", p -> {
            if (rejectTimedMessage("msg", p.getRoom(), p.getParameters())) {
                return;
            }
            commandCustomMessage(p.getArgs());
        });
        commands.add("msgreply", p -> {
            if (checkRejectTimedMessage(p.getRoom(), p.getParameters())) {
                return;
            }
            if (p.getParameters().notEmpty("nick", "msg-id", "msg") && p.hasArgs()) {
                    String atUsername = p.getParameters().get("nick");
                    String atMsgId = p.getParameters().get("msg-id");
                    String atMsg = p.getParameters().get("msg");
                    String msg = p.getArgs();
                    sendReply(p.getChannel(), msg, atUsername, atMsgId, atMsg);
            }
            else {
                g.printLine("Invalid reply parameters");
            }
        });
        commands.add("w", p -> {
            if (rejectTimedMessage("w", p.getRoom(), p.getParameters())) {
                return;
            }
            w.whisperCommand(p.getArgs(), false);
        });
        commands.add("changeToken", p -> {
            g.changeToken(p.getArgs());
        });
        //------------
        // System/Util
        //------------
        commands.add("dir", p -> {
            g.printSystem("Settings directory: "+Chatty.getPathInfo(PathType.SETTINGS));
        });
        commands.add("wdir", p -> {
            g.printSystem("Working directory: "+Chatty.getPathInfo(PathType.WORKING));
        });
        commands.add("opendir", p -> {
            MiscUtil.openFile(Chatty.getPath(PathType.SETTINGS), g);
        });
        commands.add("openwdir", p -> {
            MiscUtil.openFile(Chatty.getPath(PathType.WORKING), g);
        });
        commands.add("showJarDir", p -> {
            Path path = Stuff.determineJarPath();
            if (path != null) {
                g.printSystem("JAR directory: "+path.getParent());
            }
            else {
                g.printSystem("JAR directory unknown");
            }
        });
        commands.add("openJarDir", p -> {
            Path path = Stuff.determineJarPath();
            if (path != null) {
                MiscUtil.openFile(path.getParent().toFile(), g);
            }
            else {
                g.printSystem("JAR directory unknown");
            }
        });
        commands.add("showBackupDir", p -> {
            g.printSystem("Backup directory: "+Chatty.getPathInfo(PathType.BACKUP));
        });
        commands.add("openBackupDir", p -> {
            MiscUtil.openFile(Chatty.getPathCreate(PathType.BACKUP), g);
        });
        commands.add("showTempDir", p -> {
            g.printSystem("System Temp directory: "+Chatty.getTempDirectory());
        });
        commands.add("openTempDir", p -> {
            MiscUtil.openFile(new File(Chatty.getTempDirectory()), g);
        });
        commands.add("showDebugDir", p -> {
            g.printSystem("Debug Log Directory: "+Chatty.getPathInfo(PathType.DEBUG));
        });
        commands.add("openDebugDir", p -> {
            MiscUtil.openFile(Chatty.getPath(PathType.DEBUG), g);
        });
        commands.add("showLogDir", p -> {
            g.printSystem("Chat Log Directory: " + Chatty.getPathInfo(PathType.LOGS));
        });
        commands.add("openLogDir", p -> {
            if (chatLog.getPath() != null) {
                MiscUtil.openFile(chatLog.getPath().toAbsolutePath().toFile(), g);
            }
            else {
                g.printSystem("Invalid Chat Log Directory");
            }
        });
        commands.add("showJavaDir", p -> {
            g.printSystem("JRE directory: "+System.getProperty("java.home"));
        });
        commands.add("openJavaDir", p -> {
            MiscUtil.openFile(new File(System.getProperty("java.home")), g);
        });
        commands.add("showFallbackFontDir", p -> {
            Path path = Paths.get(System.getProperty("java.home"), "lib", "fonts", "fallback");
            g.printSystem("Fallback font directory (may not exist yet): "+path);
        });
        commands.add("openFallbackFontDir", p -> {
            Path path = Paths.get(System.getProperty("java.home"), "lib", "fonts", "fallback");
            if (Files.exists(path)) {
                MiscUtil.openFile(path.toFile(), g);
            } else {
                path = path.getParent();
                g.showPopupMessage("Fallback font folder does not exist. Create a folder called 'fallback' in '"+path+"'.");
                MiscUtil.openFile(path.toFile(), g);
            }
        });
        commands.add("copy", p -> {
            MiscUtil.copyToClipboard(p.getArgs());
        });
        commands.add("releaseInfo", p -> {
            g.openReleaseInfo();
        });
        commands.add("echo", p -> {
            if (p.hasArgs()) {
                g.printLine(p.getRoom(), p.getArgs());
            } else {
                g.printLine(p.getRoom(), "Invalid parameters: /echo <message>");
            }
        });
        commands.add("echoall", p -> {
            if (p.hasArgs()) {
                g.printLineAll(p.getArgs());
            } else {
                g.printLine("Invalid parameters: /echoall <message>");
            }
        });
        commands.add("uptime", p -> {
            g.printSystem("Chatty has been running for "+Chatty.uptime());
        });
        commands.add("appinfo", p -> {
            g.printSystem(LogUtil.getAppInfo()+" [Connection] "+c.getConnectionInfo());
        });
        commands.add("timer", p -> {
            TimerResult result = timerCommand.command(p.getArgs(), p.getRoom(), p.getParameters());
            if (result.message != null) {
                g.printSystemMultline(null, result.message);
                g.printTimerLog(result.message);
            }
        });
        commands.add("exportText", p -> {
            CommandParsedArgs args = p.parsedArgs(2);
            if (args != null) {
                String file = args.get(0);
                String text = args.hasOption("n")
                            ? args.get(1).replace("\\n", "\n")
                            : args.get(1);
                boolean append = args.hasOption("a");
                if (args.hasOption("A")) {
                    text += "\n";
                    append = true;
                }
                if (args.hasOption("L")) {
                    text = "\n"+text;
                    append = true;
                }
                boolean success = MiscUtil.exportText(file, text, append);
                if (success) {
                    if (!args.hasOption("s")) {
                        g.printSystem("File written successfully.");
                    }
                }
                else {
                    g.printSystem("Failed to write file, see debug log.");
                }
            }
            else {
                g.printSystem("Usage: /exportText <fileName> <text>");
            }
        });
        commands.add("clearUserMessages", p -> {
            CommandParsedArgs a = p.parsedArgs(0);
            String chan = p.getChannel();
            boolean numOnly = false;
            if (a != null) {
                chan = a.hasOption("a") ? null : p.getChannel();
                numOnly = a.hasOption("n");
            }
            int result = c.clearLines(chan, numOnly);
            g.printSystem(p.getRoom(), String.format("%s of %d users in %s",
                    numOnly ? "Reset number of messages" : "Cleared message history",
                    result,
                    chan != null ? chan : "all channels"));
        });
        
        //-----------------------
        // Settings/Customization
        //-----------------------
        commands.add("set", p -> {
            g.printSystem(settings.setTextual(p.getArgs(), true));
        });
        commands.add("set2", p -> {
            g.printSystem(settings.setTextual(p.getArgs(), false));
        });
        commands.add("setSwitch", p -> {
            g.printSystem(settings.setSwitchTextual(p.getArgs(), true));
        });
        commands.add("setSwitch2", p -> {
            g.printSystem(settings.setSwitchTextual(p.getArgs(), false));
        });
        commands.add("setList", p -> {
            g.printSystem(settings.setListTextual(p.getArgs()));
        });
        commands.add("get", p -> {
            g.printSystem(settings.getTextual(p.getArgs()));
        });
        commands.add("clearsetting", p -> {
            g.printSystem(settings.clearTextual(p.getArgs()));
        });
        commands.add("reset", p -> {
            g.printSystem(settings.resetTextual(p.getArgs()));
        });
        commands.add("add", p -> {
            g.printSystem(settings.addTextual(p.getArgs(), true));
        });
        commands.add("add2", p -> {
            g.printSystem(settings.addTextual(p.getArgs(), false));
        });
        commands.add("addUnique", p -> {
            g.printSystem(settings.addUniqueTextual(p.getArgs(), true));
        });
        commands.add("addUnique2", p -> {
            g.printSystem(settings.addUniqueTextual(p.getArgs(), false));
        });
        commands.add("remove", p -> {
            g.printSystem(settings.removeTextual(p.getArgs(), true));
        });
        commands.add("remove2", p -> {
            g.printSystem(settings.removeTextual(p.getArgs(), false));
        });
        commands.add("setcolor", p -> {
            if (p.hasArgs()) {
                g.setColor(p.getArgs());
            }
        });
        commands.add("setname", p -> {
            g.printLine(customNames.commandSetCustomName(p.getArgs()));
        });
        commands.add("resetname", p -> {
            g.printLine(customNames.commandResetCustomname(p.getArgs()));
        });
        commands.add("customCompletion", p -> {
            commandCustomCompletion(p.getArgs());
        });
        commands.add("ab", p -> {
            g.printSystem("[Addressbook] "
                    +addressbook.command(p.hasArgs() ? p.getArgs() : ""));
        }, "users");
        commands.add("abimport", p -> {
            g.printSystem("[Addressbook] Importing from file..");
            addressbook.importFromFile();
        });
        
        //-------
        // Ignore
        //-------
        commands.add("ignore", p -> {
            commandSetIgnored(p.getArgs(), null, true);
        });
        commands.add("unIgnore", p -> {
            commandSetIgnored(p.getArgs(), null, false);
        });
        commands.add("ignoreChat", p -> {
            commandSetIgnored(p.getArgs(), "chat", true);
        });
        commands.add("unignoreChat", p -> {
            commandSetIgnored(p.getArgs(), "chat", false);
        });
        commands.add("ignoreWhisper", p -> {
            commandSetIgnored(p.getArgs(), "whisper", true);
        });
        commands.add("unignoreWhisper", p -> {
            commandSetIgnored(p.getArgs(), "whisper", false);
        });
        //--------------
        // Emotes/Images
        //--------------
        commands.add("ffz", p -> {
            if (p.hasArgs() && p.getArgs().startsWith("following")) {
                commandFFZFollowing(p.getRoom().getOwnerChannel(), p.getArgs());
            } else {
                commandFFZ(p.getRoom().getOwnerChannel());
            }
        });
        commands.add("ffzws", p -> {
            g.printSystem("[FFZ-WS] Status: "+frankerFaceZ.getWsStatus());
        });
        commands.add("pubsubstatus", p -> {
            g.printSystem("[PubSub] Status: "+pubsub.getStatus());
        });
        commands.add("refresh", p -> {
            commandRefresh(p.getRoom().getOwnerChannel(), p.getArgs());
        });
        commands.add("clearimagecache", p -> {
            g.printLine("Clearing image cache (this can take a few seconds)");
            int result = ImageCache.clearCache(null);
            if (result == -1) {
                g.printLine("Failed clearing image cache.");
            } else {
                g.printLine(String.format("Deleted %d image cache files",
                        result));
            }
        });
        commands.add("clearemotecache", p -> {
            g.printLine("Clearing Emoticon image cache for type "+p.getArgs()+".");
            int result = ImageCache.clearCache("emote_"+p.getArgs());
            if (result == -1) {
                g.printLine("Failed clearing image cache.");
            } else {
                g.printLine(String.format("Deleted %d image cache files",
                        result));
            }
        });
        
        //------
        // Other
        //------
        commands.add("follow", p -> {
            commandFollow(p.getChannel(), p.getArgs());
        });
        commands.add("unfollow", p -> {
            commandUnfollow(p.getChannel(), p.getArgs());
        });
        commands.add("favorite", p -> {
            Favorite result;
            if (!p.hasArgs()) {
                result = channelFavorites.addFavorite(p.getChannel());
            } else {
                result = channelFavorites.addFavorite(p.getArgs());
            }
            if (result != null) {
                g.printSystem("Added '"+result+"' to favorites");
            } else {
                g.printSystem("Failed adding favorite");
            }
        });
        commands.add("unfavorite", p -> {
            Favorite result;
            if (!p.hasArgs()) {
                result = channelFavorites.removeFavorite(p.getChannel());
            } else {
                result = channelFavorites.removeFavorite(p.getArgs());
            }
            if (result != null) {
                g.printSystem("Removed '"+result+"' from favorites");
            } else {
                g.printSystem("Failed removing favorite");
            }
        });
        commands.add("automod_approve", p -> {
            autoModCommandHelper.approve(p.getChannel(), p.getArgs());
        });
        commands.add("automod_deny", p -> {
            autoModCommandHelper.deny(p.getChannel(), p.getArgs());
        });
        commands.add("marker", p -> {
            commandAddStreamMarker(p.getRoom(), p.getArgs());
        });
        c.addNewCommands(commands, this);
        commands.add("addStreamHighlight", p -> {
            commandAddStreamHighlight(p.getRoom(), p.getArgs());
        });
        commands.add("openStreamHighlights", p -> {
            commandOpenStreamHighlights(p.getRoom());
        });
        commands.add("triggerNotification", p -> {
            CommandParsedArgs args = p.parsedArgs(1);
            if (args != null) {
                String title = null;
                String text = args.get(0);
                if (args.hasOption("t")) {
                    List<String> split = StringUtil.split(text, ' ', '"', '"', 2, 1);
                    if (split.size() == 2) {
                        title = split.get(0);
                        text = split.get(1);
                    }
                }
                g.triggerCommandNotification(p.getChannel(), title, text,
                        args.hasOption("h"), args.hasOption("m"));
            }
            else {
                g.printSystem("Usage: /triggerNotification [-hmt] <text>");
            }
        });
        commands.add("testNotification", p -> {
            String args = p.getArgs();
            if (args == null) {
                args = "";
            }
            String[] split = args.split("\\|\\|", 2);
            if (split.length == 2) {
                g.showTestNotification(null, split[0], split[1]);
            }
            else {
                g.showTestNotification(args, null, null);
            }
        });
        commands.add("clearChat", p -> {
            g.clearChat();
        });
        commands.add("resortUserlist", p -> {
            g.resortUsers(p.getRoom());
        });
        commands.add("proc", p -> {
            g.printSystem("[Proc] "+ProcessManager.command(p.getArgs(),
                    s -> g.printSystem("[ProcOutput] "+s)));
        });
        commands.add("chain", p -> {
            List<String> commands = Helper.getChainedCommands(p.getArgs());
            if (commands.isEmpty()) {
                g.printSystem("No valid commands");
            }
            for (String chainedCommand : commands) {
                // Copy parameters so changing args in commandInput() doesn't
                // affect the following commands
                textInput(p.getRoom(), chainedCommand, p.getParameters().copy());
            }
        });
        commands.add("foreach", p -> {
            if (p.hasArgs()) {
                String[] split = Helper.getForeachParams(p.getArgs());
                if (split[0] == null) {
                    g.printSystem("No list specified for foreach");
                }
                else if (split[1] == null) {
                    g.printSystem("No command specified for foreach");
                }
                else {
                    String list = split[0];
                    String command = split[1];
                    String[] splitList = list.split(" ");
                    CustomCommand customCommand = CustomCommand.parse(command);
                    if (customCommand.hasError()) {
                        g.printSystem("Command specified for foreach is invalid");
                    }
                    else {
                        for (String item : splitList) {
                            Parameters param = Parameters.create(item);
                            /**
                             * Transfer this one if present. Not sure why not
                             * just all are copied (except for args), but
                             * keeping it like this at least doesn't change
                             * behaviour.
                             */
                            if (p.getParameters().hasKey(TimerCommand.TIMER_PARAMETERS_KEY)) {
                                param.put(TimerCommand.TIMER_PARAMETERS_KEY, p.getParameters().get(TimerCommand.TIMER_PARAMETERS_KEY));
                            }
                            Debugging.println("foreach", "Foreach command: %s Param: %s", customCommand, param);
                            anonCustomCommand(p.getRoom(), customCommand, param);
                        }
                    }
                }
            }
            else {
                g.printSystem("Usage: /foreach [list] > [command]");
            }
        });
        commands.add("runin", p -> {
            String[] split;
            if (!p.hasArgs() || (split = p.getArgs().split(" ", 2)).length != 2) {
                g.printSystem("Usage: /runin [channel] [command]");
                return;
            }
            String chan = split[0];
            String command = split[1];
            if (Helper.isValidStream(chan)) {
                chan = Helper.toChannel(chan);
            }
            chan = StringUtil.toLowerCase(chan);
            /**
             * Whisper channels ($) don't count as "open" in the same way
             * regular channels do.
             */
            if (isChannelOpen(chan) || Helper.isValidWhisperChannel(chan)) {
                textInput(c.getRoomByChannel(chan), command, p.getParameters().copy());
            }
            else {
                g.printSystem("Invalid channel: " + chan);
            }
        });
        commands.add("debug", p -> {
            if (rejectTimedMessage("debug", p.getRoom(), p.getParameters())) {
                return;
            }
            String[] split = p.getArgs().split(" ", 2);
            String actualCommand = split[0];
            String actualParamter = null;
            if (split.length == 2) {
                actualParamter = split[1];
            }
            testCommands(p.getRoom(), actualCommand, actualParamter);
        });
    }
    
    /**
     * Executes the command with the given name, which can be a built-in or
     * Custom Command.
     *
     * @param room The room context
     * @param command The command name (no leading /)
     * @param parameters The parameters, can not be null
     * @return
     */
    public boolean command(Room room, String command, Parameters parameters) {
        String channel = room.getChannel();
        // Args could be null
        String parameter = parameters.getArgs();
        command = StringUtil.toLowerCase(command);
        
        if (commands.performCommand(command, room, parameters)) {
            // Already done if true
        }
        
        else if (TwitchCommands.isCommand(command)) {
            if (!checkRejectTimedMessage(room, parameters)) {
                c.command(channel, command, parameter, null);
            }
        }
        
        // Has to be tested last, so regular commands with the same name take
        // precedence
        else if (customCommands.containsCommand(command, room)) {
            customCommand(room, command, parameters);
        }
        
        else {
            g.printLine(Language.getString("chat.unknownCommand", command));
            return false;
        }
        return true;
    }
    
    private void testCommands(Room room, String command, String parameter) {
        String channel = room.getChannel();
        if (command.equals("addchans")) {
            String[] splitSpace = parameter.split(" ");
            String[] split2 = splitSpace[0].split(",");
            for (String chan : split2) {
                g.printLine(c.getUser(chan, "test").getRoom(), "test");
            }
        } else if (command.equals("switchchan")) {
            g.switchToChannel(parameter);
        } else if (command.equals("settestuser")) {
            String[] split = parameter.split(" ");
            createTestUser(split[0], split[1]);
        } else if (command.equals("getemoteset")) {
            g.printLine(g.emoticons.getEmoticonsBySet(parameter).toString());
        } else if (command.equals("testcolor")) {
            testUser.setColor(parameter);
        } else if (command.equals("testupdatenotification")) {
            g.setUpdateAvailable("[test]", null);
        } else if (command.equals("testnewevent")) {
            g.setSystemEventCount(Integer.valueOf(parameter));
        } else if (command.equals("addevent")) {
            String[] split = parameter.split(" ", 3);
            EventLog.addSystemEvent(split[0], split[1], split[2]);
        } else if (command.equals("addevent2")) {
            String[] split = parameter.split(" ", 3);
            new Thread(() -> EventLog.addSystemEvent(split[0], split[1], split[2])).start();
        } else if (command.equals("removechan")) {
            g.removeChannel(parameter);
        } else if (command.equals("tt")) {
            String[] split = parameter.split(" ", 3);
            int repeats = Integer.parseInt(split[0]);
            int delay = Integer.parseInt(split[1]);
            String c = split[2];
            TestTimer.testTimer(this, room, c, repeats, delay);
        } //        else if (command.equals("usertest")) {
        //            System.out.println(users.getChannelsAndUsersByUserName(parameter));
        //        }
        //        else if (command.equals("insert2")) {
        //            g.printLine("\u0E07");
        //        }
        else if (command.equals("bantest")) {
            int duration = -1;
            String reason = "";
            if (parameter != null) {
                String[] split = parameter.split(" ", 2);
                duration = Integer.parseInt(split[0]);
                if (split.length > 1) {
                    reason = split[1];
                }
            }
            g.userBanned(testUser, duration, reason, null);
        } else if (command.equals("ban")) {
            String[] split = parameter.split(" ", 3);
            int duration = -1;
            if (split.length > 1) {
                duration = Integer.parseInt(split[1]);
            }
            String reason = "";
            if (split.length > 2) {
                reason = split[2];
            }
            g.userBanned(c.getUser(channel, split[0]), duration, reason, null);
        } else if (command.equals("userjoined")) {
            c.userJoined("#test", parameter);
        } else if (command.equals("echomessage")) {
            String[] parts = parameter.split(" ");
//            g.printMessage(parts[0], testUser, parts[1], false, null, 0);
        } else if (command.equals("loadffz")) {
            frankerFaceZ.requestEmotes(parameter, true);
        } else if (command.equals("testtw")) {
            g.showTokenWarning();
        } else if (command.equals("tsonline")) {
            testStreamInfo.set(parameter, new StreamCategory(null, "Game"), 123, -1, StreamType.LIVE);
            g.addStreamInfo(testStreamInfo);
        } else if (command.equals("tsoffline")) {
            testStreamInfo.setOffline();
            g.addStreamInfo(testStreamInfo);
        } else if (command.equals("testspam")) {
            g.printLine("test" + spamProtection.getAllowance() + spamProtection.tryMessage());
        } else if (command.equals("spamprotectioninfo")) {
            g.printSystem("Spam Protection: "+spamProtection);
        } else if (command.equals("tsv")) {
            testStreamInfo.set("Title", new StreamCategory(null, "Game"), Integer.parseInt(parameter), -1, StreamType.LIVE);
        } else if (command.equals("tsvs")) {
            System.out.println(testStreamInfo.getViewerStats(true));
        } else if (command.equals("tsaoff")) {
            StreamInfo info = api.getStreamInfo(g.getActiveStream(), null);
            info.setOffline();
        } else if (command.equals("tsaon")) {
            StreamInfo info = api.getStreamInfo(g.getActiveStream(), null);
            info.set("Test", new StreamCategory(null, "Game"), 12, System.currentTimeMillis() - 1000, StreamType.LIVE);
        } else if (command.equals("tss")) {
            StreamInfo info = api.getStreamInfo(parameter, null);
            info.set("Test", new StreamCategory(null, "Game"), 12, System.currentTimeMillis() - 1000, StreamType.LIVE);
        } else if (command.equals("tston")) {
            int viewers = 12;
            try {
                viewers = Integer.parseInt(parameter);
            } catch (NumberFormatException ex) { }
            StreamInfo info = api.getStreamInfo("tduva", null);
            info.set("Test 2", new StreamCategory(null, "Game"), viewers, System.currentTimeMillis() - 1000, StreamType.LIVE);
        } else if (command.equals("newstatus")) {
            g.setChannelNewStatus(parameter, "");
        } else if (command.equals("refreshstreams")) {
            api.manualRefreshStreams();
        } else if (command.equals("usericonsinfo")) {
            usericonManager.debug();
        } else if (command.equals("userlisttest")) {
//            g.printMessage("test1", testUser, "short message", false, null, 0);
//            g.printMessage("test2", testUser, "short message2", false, null, 0);
//            g.printCompact("test3", "MOD", testUser);
//            g.printCompact("test3", "MOD", testUser);
//            g.printCompact("test3", "MOD", testUser);
//            g.printCompact("test3", "MOD", testUser);
//            g.printCompact("test3", "MOD", testUser);
//            g.printCompact("test3", "MOD", testUser);
//            g.printCompact("test3", "MOD", testUser);
//            g.printMessage("test3", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
//                    + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false, null, 0);
//            g.printMessage("test3", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
//                    + "fjwfjfwjefjwefjwoeifjwefiowejfef wefjoiwefj", false, null, 0);
//            g.printMessage("test3", testUser, "longer wef wef wefwe fwe ewfwe fwef wwefwef"
//                    + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false, null, 0);
//            g.printCompact("test4", "MOD", testUser);
//            g.printCompact("test5", "MOD", testUser);
//            g.printCompact("test6", "MOD", testUser);
//            g.printCompact("test7", "MOD", testUser);
//            g.printCompact("test8", "MOD", testUser);
//            g.printCompact("test9", "MOD", testUser);
//            g.printMessage("test10", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
//                    + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false, null, 0);
        } else if (command.equals("requestfollowers")) {
            api.getFollowers(parameter);
        } else if (command.equals("simulate2")) {
            c.simulate(parameter);
        } else if (command.equals("simulate")) {
            if (parameter.equals("bits")) {
                parameter = "bits "+g.emoticons.getCheerEmotesString(null);
            } else if (parameter.equals("bitslocal")) {
                parameter = "bits "+g.emoticons.getCheerEmotesString(Helper.toStream(channel));
            } else if (parameter.startsWith("bits ")) {
                parameter = "bits "+parameter.substring("bits ".length());
            } else if (parameter.startsWith("emoji ")) {
                int num = Integer.parseInt(parameter.substring("emoji ".length()));
                StringBuilder b = new StringBuilder();
                for (Emoticon emote : g.emoticons.getEmoji()) {
                    b.append(emote.code);
                    if (--num == 0) {
                        break;
                    }
                }
                parameter = "message "+b.toString();
            } else if (parameter.startsWith("subbomb")) {
                String gifter = parameter.equals("subbomb") ? "Gifter" : "Gifter2";
                String secondParam = parameter.substring("subbomb".length());
                int amount = 10;
                try {
                    amount = Integer.parseInt(secondParam.trim());
                } catch (NumberFormatException ex) { }
                for (int i=0;i<amount;i++) {
                    String raw = RawMessageTest.simulateIRC(channel, "subbomb recipient"+i, gifter);
                    c.simulate(raw);
                }
                return;
            }
            String raw = RawMessageTest.simulateIRC(channel, parameter, c.getUsername());
            if (raw != null) {
                c.simulate(raw);
            }
        } else if (command.equals("c1")) {
            sendMessage(channel, (char)1+parameter);
        } else if (command.equals("gc")) {
            Runtime.getRuntime().gc();
            LogUtil.logMemoryUsage();
        } else if (command.equals("wsconnect")) {
            frankerFaceZ.connectWs();
        } else if (command.equals("wsdisconnect")) {
            frankerFaceZ.disconnectWs();
        } else if (command.equals("psconnect")) {
//            pubsub.connect();
        } else if (command.equals("psdisconnect")) {
            pubsub.disconnect();
        } else if (command.equals("psreconnect")) {
            pubsub.reconnect();
        } else if (command.equals("eventsubreconnect")) {
            eventSub.reconnect();
        } else if (command.equals("modaction")) {
            String by = "Blahfasel";
            String action = "timeout";
            List<String> args = new ArrayList<>();
            if (parameter != null && !parameter.isEmpty()) {
                String[] split = parameter.split(" ");
                by = split[0];
                action = split[1];
                for (int i=2;i<split.length;i++) {
                    args.add(split[i]);
                }
            } else {
                args.add("tduvatest");
                args.add("5");
            }
            ModeratorActionData data = new ModeratorActionData("", "", "", room.getStream(), action, args, by, "");
            //args.add("still not using LiveSplit Autosplitter D:");
            //g.printModerationAction(new ModeratorActionData("", "", "", room.getStream(), action, args, "Blahfasel", ""), false);
            pubsubListener.messageReceived(new Message(null, null, data, null));
        } else if (command.equals("automod")) {
            List<String> args = new ArrayList<>();
            args.add("tduva");
            if (parameter != null) {
                if (parameter.contains(",")) {
                    String[] split = parameter.split(",", 2);
                    args.add(split[1]);
                    args.add(split[0]);
                } else {
                    args.add(parameter);
                }
            } else {
                args.add("fuck and stuff like that, rather long message and whatnot Kappa b "+Debugging.count(channel));
            }
            g.printModerationAction(new ModeratorActionData("", "", "", room.getStream(), "twitchbot_rejected", args, "twitchbot", "TEST"), false);
        } else if (command.equals("automod2")) {
            List<String> args = new ArrayList<>();
            args.add("tduva");
            ModeratorActionData data = new ModeratorActionData("", "", "", room.getStream(), "denied_automod_message", args, "asdas", "TEST");
            g.printModerationAction(data, false);
        } else if (command.equals("simulatepubsub")) {
            pubsub.simulate(parameter);
        } else if (command.equals("simulateeventsub")) {
            eventSub.simulate(parameter);
        } else if (command.equals("repeat")) {
            String[] split = parameter.split(" ", 2);
            int count = Integer.parseInt(split[0]);
            for (int i=0;i<count;i++) {
//                commandInput(room, "/"+split[1]);
            }
        } else if (command.equals("modactiontest3")) {
            List<String> args = new ArrayList<>();
            args.add("tduva");
            g.printModerationAction(new ModeratorActionData("", "", "", "tduvatest", "approved_twitchbot_message", args, "tduvatest", "TEST"+Math.random()), false);
        } else if (command.equals("loadsoferrors")) {
            for (int i=0;i<10000;i++) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        Helper.unhandledException();
                    }
                });
            }
        } else if (command.equals("getuserid")) {
            if (parameter == null) {
                g.printSystem("Parameter required.");
            } else {
                api.getUserIdAsap(r -> {
                    String result = r.getData().toString();
                    if (r.hasError()) {
                        result += " Error: "+r.getError();
                    }
                    g.printSystem(result);
                }, parameter.split("[ ,]"));
            }
        } else if (command.equals("getuserids2")) {
            api.getUserIDsTest2(parameter);
        } else if (command.equals("getuserids3")) {
            api.getUserIDsTest3(parameter);
        } else if (command.equals("clearoldsetups")) {
            Stuff.init();
            Stuff.clearOldSetups();
        } else if (command.equals("-")) {
            g.printSystem(Debugging.command(parameter));
        } else if (command.equals("connection")) {
            c.debugConnection();
        } else if (command.equals("clearoldcachefiles")) {
            ImageCache.deleteExpiredFiles();
        } else if (command.equals("sha1")) {
            g.printSystem(ImageCache.sha1(parameter));
        } else if (command.equals("letstakeabreak")) {
            try {
                Thread.sleep(Integer.parseInt(parameter));
            }
            catch (InterruptedException ex) {
                Logger.getLogger(TwitchClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (command.equals("infiniteloop")) {
            while (true) {
            }
        } else if (command.equals("threadinfo")) {
            LogUtil.logThreadInfo();
        } else if (command.equals("addusers")) {
            String[] split = parameter.split(" ", 2);
            int amount = Integer.parseInt(split[0]);
            int messageAmount = Integer.parseInt(split[1]);
            for (int i=0;i<amount;i++) {
                User user = c.getUser(channel, "user"+i);
                for (int m=0;m<messageAmount;m++) {
                    user.addMessage("abc"+i+" "+m, false, "abc id"+i+" "+m);
                }
            }
        } else if (command.equals("testr")) {
            api.test();
        } else if (command.equals("joinlink")) {
            MsgTags tags = MsgTags.create("chatty-channel-join", Helper.toChannel("twitch"));
            g.printInfo(c.getRoomByChannel(channel), "Join link:", tags);
        }
    }
    
    public void anonCustomCommand(Room room, String text, Parameters parameters) {
        CustomCommand command = CustomCommand.parse(text);
        if (parameters == null) {
            parameters = Parameters.create(null);
        }
        anonCustomCommand(room, command, parameters);
    }
    
    public void anonCustomCommand(Room room, CustomCommand command, Parameters parameters) {
        if (command.hasError()) {
            g.printLine("Parse error: "+command.getSingleLineError());
            return;
        }
        if (room == null) {
            g.printLine("Custom command: Not on a channel");
            return;
        }
        customCommands.command(command, parameters, room, result -> {
            if (result == null) {
                g.printLine("Custom command: Insufficient parameters/data");
            }
            else if (result.isEmpty()) {
                g.printLine("Custom command: No action specified");
            }
            else {
                textInput(room, result, parameters);
            }
        });
    }
    
    public void customCommandLaunch(String commandAndParameters) {
        if (StringUtil.isNullOrEmpty(commandAndParameters)) {
            return;
        }
        LOGGER.info("Running launch command: "+commandAndParameters);
        String[] split = commandAndParameters.split(" ", 2);
        String commandName = split[0];
        Parameters p;
        if (split.length == 2) {
            p = Parameters.create(split[1]);
        }
        else {
            p = Parameters.create(null);
        }
        p.put("-cc", "true");
        // Probably safer, since it access state from the GUI
        SwingUtilities.invokeLater(() -> customCommand(g.getActiveRoom(), commandName, p));
    }
    
    public void customCommand(Room room, String command, Parameters parameters) {
        if (room == null) {
            g.printLine("Custom command: Not on a channel");
            return;
        }
        if (!customCommands.containsCommand(command, room)) {
            g.printLine("Custom command not found: "+command);
            return;
        }
        if (CustomCommands.getCustomCommandCount(parameters) > 2) {
            g.printLine(String.format("Stopped executing '%s' (too many nested Custom Commands)", command));
            return;
        }
        customCommands.command(command, parameters, room, result -> {
            if (result == null) {
                g.printLine("Custom command '" + command + "': Insufficient parameters/data");
            }
            else if (result.isEmpty()) {
                // This shouldn't actually happen if edited through the settings,
                // which should trim() out whitespace, so that the command won't
                // have a result if it's empty and thus won't be added as a command.
                // Although it can also happen if the command just contains a \
                // (which is interpreted as an escape character).
                g.printLine("Custom command '" + command + "': No action specified");
            }
            else {
                textInput(room, result, parameters);
            }
        });
    }
    
    /**
     * Adds or removes the name given in the parameter on the ignore list. This
     * is done for either regular ignores (chat) and/or whisper ignores
     * depending on the given type.
     * 
     * Outputs a message with the new state depending on whether any change (at
     * least one list was changed) occured or not.
     *
     * @param parameter The first word is used as name to ignore/unignore
     * @param type Can be "chat", "whisper" or null to affect both lists
     * @param ignore true to ignore, false to unignore
     */
    public void commandSetIgnored(String parameter, String type, boolean ignore) {
        if (parameter != null && !parameter.isEmpty()) {
            String[] split = parameter.split(" ");
            String name = StringUtil.toLowerCase(split[0]);
            String message = "";
            List<String> setting = new ArrayList<>();
            if (type == null || type.equals("chat")) {
                message = "in chat";
                setting.add("ignoredUsers");
            }
            if (type == null || type.equals("whisper")) {
                message = StringUtil.append(message, "/", "from whispering you");
                setting.add("ignoredUsersWhisper");
            }
            boolean changed = false;
            for (String s : setting) {
                if (ignore) {
                    if (settings.setAdd(s, name)) {
                        changed = true;
                    }
                } else {
                    if (settings.listRemove(s, name)) {
                        changed = true;
                    }
                }
            }
            if (changed) {
                if (ignore) {
                    g.printSystem(String.format("Ignore: '%s' now ignored %s",
                            name, message));
                } else {
                    g.printSystem(String.format("Ignore: '%s' no longer ignored %s",
                            name, message));
                }
            } else {
                if (ignore) {
                    g.printSystem(String.format("Ignore: '%s' already ignored %s",
                            name, message));
                } else {
                    g.printSystem(String.format("Ignore: '%s' not ignored %s",
                            name, message));
                }
            }
        } else {
            g.printSystem("Ignore: Invalid name");
        }
    }
    
    private void commandServer(String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /server <address>[:port]");
            return;
        }
        String[] split = parameter.split(":");
        if (split.length == 1) {
            prepareConnectionAnyChannel(split[0], getPorts());
        } else if (split.length == 2) {
            prepareConnectionAnyChannel(split[0], split[1]);
        } else {
            g.printLine("Invalid format. Usage: /server <address>[:port]");
        }
    }
    
    /**
     * Command to join channel entered.
     * 
     * @param channelString 
     */
    public void commandJoinChannel(String channelString) {
        if (channelString == null) {
            channelString = "";
        }
        String[] channelList = Helper.parseChannels(channelString);
        if (channelList.length == 0) {
            g.printLine("No valid channel specified.");
        }
        else {
            c.joinChannels(new LinkedHashSet<>(Arrays.asList(channelList)));
        }
    }
    
    /**
     * Command to part channel entered.
     * 
     * @param channel 
     */
    private void commandPartChannel(String channel) {
        if (channel == null || channel.isEmpty()) {
            g.printLine("No channel to leave.");
        } else {
            closeChannel(channel);
        }
    }
    
    private void commandRejoinChannel(String channel) {
        if (channel == null || channel.isEmpty()) {
            g.printLine("No channel to rejoin.");
        } else {
            c.rejoinChannel(channel);
        }
    }

    /**
     * React to action message (/me) command and send message if on channel.
     * 
     * @param channel The channel to send the message to
     * @param message The message to send
     */
    private void commandActionMessage(String channel, String message) {
        if (message != null) {
            sendActionMessage(channel, message);
        } else {
            g.printLine("Usage: /me <message>");
        }
    }
    
    public void sendActionMessage(String channel, String message) {
        if (c.onChannel(channel, true)) {
            if (c.sendSpamProtectedMessage(channel, message, true)) {
                g.printMessage(c.localUserJoined(channel), message, true);
            } else {
                g.printLine("# Action Message not sent to prevent ban: " + message);
            }
        }
    }
    
    private void commandCustomMessage(String parameter) {
        if (parameter != null && !parameter.isEmpty()) {
            String[] split = parameter.split(" ", 2);
            if (split.length == 2) {
                String to = split[0];
                String message = split[1];
                c.sendSpamProtectedMessage(to, message, false);
                g.printLine(String.format("-> %s: %s", to, message));
                return;
            }
        }
        g.printSystem("Invalid parameters.");
    }
    
    public void commandReconnect() {
        if (c.disconnect()) {
            c.reconnect();
        } else {
            g.printLine("Could not reconnect.");
        }
    }
    
    public void updateLogin() {
        String username = settings.getString("username");
        String token = settings.getString("token");
        c.setLogin(username, "oauth:"+token);
        
        /**
         * Not sure if this makes sense, since topics may not be sent in the
         * first place when the required scopes weren't there.
         * 
         * Reconnecting IRC, which should now be automatic when changing token,
         * may sent those topics though.
         */
//        pubsub.updateToken(token);

        // Removed topics have a chance to reconnect
        eventSub.tokenUpdated();
    }
    
    private void commandCustomCompletion(String parameter) {
        String usage = "Usage: /customCompletion <add/set/remove> <item> <value>";
        if (parameter == null) {
            g.printLine(usage);
            return;
        }
        String[] split = parameter.split(" ", 3);
        if (split.length < 2) {
            g.printLine(usage);
        } else {
            String type = split[0];
            String key = split[1];
            if (type.equals("add") || type.equals("set")) {
                if (split.length < 3) {
                    g.printLine("Invalid number of parameters for adding completion item.");
                } else {
                    String value = split[2];
                    if (!type.equals("set") && settings.mapGet("customCompletion", key) != null) {
                        g.printLine("Completion item '"+key+"' already exists, use '/customCompletion set <key> <value>' to overwrite");
                    } else {
                        settings.mapPut("customCompletion", key, value);
                        g.printLine("Set custom completion '"+key+"' to '"+value+"'");
                    }
                }
            } else if (type.equals("remove")) {
                settings.mapRemove("customCompletion", key);
                g.printLine("Removed '"+key+"' from custom completion");
            } else {
                g.printLine(usage);
            }
        }
    }
    
    /**
     * Follows the stream given in the parameter, or the channel if no parameter
     * is given.
     * 
     * @param channel
     * @param parameter 
     */
    public void commandFollow(String channel, String parameter) {
        g.printSystem("Following/unfollowing has been removed from the Twitch API");
    }
    
    public void commandUnfollow(String channel, String parameter) {
        g.printSystem("Following/unfollowing has been removed from the Twitch API");
    }
    
    public void commandAddStreamHighlight(Room room, String parameter) {
        g.printLine(room, streamHighlights.addHighlight(room.getOwnerChannel(), parameter, null));
    }
    
    public void commandOpenStreamHighlights(Room room) {
        g.printLine(room, streamHighlights.openFile());
    }
    
    public void modCommandAddStreamHighlight(User user, String message, MsgTags tags) {
        // Stream Highlights
        String result = streamHighlights.modCommand(user, message, tags);
        if (result != null) {
            result = user.getDisplayNick() + ": " + result;
            if (settings.getBoolean("streamHighlightChannelRespond")) {
                sendMessage(user.getChannel(), result);
            } else {
                g.printLine(user.getRoom(), result);
            }
        }
    }
    
    public void commandAddStreamMarker(Room room, String description) {
        api.createStreamMarker(room.getStream(), description, error -> {
            String info = StringUtil.aEmptyb(description, "no description", "'%s'");
            if (error == null) {
                g.printLine("Stream marker created ("+info+")");
            } else {
                g.printLine("Failed to create stream marker ("+info+"): "+error);
            }
        });
    }
    
    private void commandRefresh(String channel, String parameter) {
        if (!Helper.isRegularChannel(channel)) {
            channel = null;
        }
        if (parameter == null) {
            g.printLine("Usage: /refresh <type> (see help)");
        } else if (parameter.equals("emoticons")) {
            g.printLine("Refreshing emoticons.. (this can take a few seconds)");
            refreshRequests.add("emoticons");
            //Emoticons.clearCache(Emoticon.Type.TWITCH);
            api.refreshEmotes();
        } else if (parameter.equals("bits")) {
            g.printLine("Refreshing bits..");
            refreshRequests.add("bits");
            api.getCheers(channel, true);
        } else if (parameter.equals("badges")) {
            if (!Helper.isValidChannel(channel)) {
                g.printLine("Must be on a channel to use this.");
            } else {
                g.printLine("Refreshing badges for " + channel + "..");
                refreshRequests.add("badges");
                api.getGlobalBadges(true);
                api.getRoomBadges(Helper.toStream(channel), true);
                OtherBadges.requestBadges(r -> usericonManager.setThirdPartyIcons(r), true);
            }
        } else if (parameter.equals("ffz")) {
            if (channel == null || channel.isEmpty()) {
                g.printLine("Must be on a channel to use this.");
            } else {
                g.printLine("Refreshing FFZ emotes for "+channel+"..");
                refreshRequests.add("ffz");
                frankerFaceZ.requestEmotes(channel, true);
            }
        } else if (parameter.equals("ffzglobal")) {
            g.printLine("Refreshing global FFZ emotes..");
            refreshRequests.add("ffzglobal");
            frankerFaceZ.requestGlobalEmotes(true);
        } else if (parameter.equals("bttvemotes")) {
            g.printLine("Refreshing BTTV emotes..");
            refreshRequests.add("bttvemotes");
            bttvEmotes.requestEmotes(BTTVEmotes.GLOBAL, true);
            bttvEmotes.requestEmotes(channel, true);
        } else if (parameter.equals("7tv")) {
            g.printSystem("Refreshing 7TV emotes..");
            refreshRequests.add("seventv");
            if (!StringUtil.isNullOrEmpty(channel)) {
                sevenTV.requestEmotes(channel, true);
            }
            sevenTV.requestEmotes(null, true);
        } else {
            g.printLine("Usage: /refresh <type> (invalid type, see help)");
        }
    }
    
    public User getSpecialUser() {
        return c.getSpecialUser();
    }
    
    public Set<String> getEmotesets() {
        return emotesetManager.getEmotesets();
    }
    
    private void commandFFZ(String channel) {
        Set<Emoticon> output;
        StringBuilder b = new StringBuilder();
        if (channel == null) {
            b.append("Global FFZ emotes: ");
            output = Emoticons.filterByType(g.emoticons.getGlobalTwitchEmotes(), Emoticon.Type.FFZ);
        } else {
            b.append("This channel's FFZ emotes: ");
            Set<Emoticon> emotes = g.emoticons.getEmoticonsByStream(Helper.toStream(channel));
            output = Emoticons.filterByType(emotes, Emoticon.Type.FFZ);
        }
        if (output.isEmpty()) {
            b.append("None found.");
        }
        String sep = "";
        for (Emoticon emote : output) {
            b.append(sep);
            b.append(emote.code);
            sep = ", ";
        }
        g.printLine(roomManager.getRoom(channel), b.toString());
    }
    
    private void commandFFZFollowing(String channel, String parameter) {
        String stream = Helper.toStream(channel);
        if (stream == null) {
            g.printSystem("FFZ: No valid channel.");
        } else if (!c.isRegistered()) {
            g.printSystem("FFZ: You have to be connected to use this command.");
        } else {
            if (!stream.equals(c.getUsername())) {
                g.printSystem("FFZ: You may only be able to run this command on your own channel.");
            }
            parameter = parameter.substring("following".length()).trim();
            frankerFaceZ.setFollowing(c.getUsername(), stream, parameter);
        }
    }

    /**
     * Add a debugmessage to the GUI. If the GUI wasn't created yet, add it
     * to a cache that is send to the GUI once it is created. This is done
     * automatically when a debugmessage is added after the GUI was created.
     * 
     * @param line 
     */
    public void debug(String line) {
        if (shuttingDown) {
            return;
        }
        synchronized(cachedDebugMessages) {
            if (g == null) {
                cachedDebugMessages.add("["+DateTime.currentTimeExact()+"] "+line);
            } else {
                if (!cachedDebugMessages.isEmpty()) {
                    g.printDebug("[Start of cached messages]");
                    for (String cachedLine : cachedDebugMessages) {
                        g.printDebug(cachedLine);
                    }
                    g.printDebug("[End of cached messages]");
                    // No longer used
                    cachedDebugMessages.clear();
                }
                g.printDebug(line);
            }
        }
    }
    
    public void debugFFZ(String line) {
        if (shuttingDown || g == null) {
            return;
        }
        g.printDebugFFZ(line);
    }
    
    public void debugPubSub(String line) {
        if (shuttingDown || g == null) {
            return;
        }
        g.printDebugPubSub(line);
    }
    
    public void debugEventSub(String line) {
        if (shuttingDown || g == null) {
            return;
        }
        g.printDebugEventSub(line);
    }
    
    /**
     * Output a warning to the user, instead of the debug window.
     * 
     * @param line 
     */
    public final void warning(String line) {
        if (shuttingDown) {
            return;
        }
        synchronized(cachedWarningMessages) {
            if (g == null) {
                cachedWarningMessages.add(line);
            } else {
                if (!cachedWarningMessages.isEmpty()) {
                    for (String cachedLine : cachedWarningMessages) {
                        g.printLine(cachedLine);
                    }
                    cachedWarningMessages.clear();
                }
                if (line != null) {
                    g.printLine(line);
                }
            }
        }
    }
    
    private class PubSubResults implements PubSubListener {

        @Override
        public void messageReceived(Message message) {
            if (message.data != null) {
                if (message.data instanceof ModeratorActionData) {
                    ModeratorActionData data = (ModeratorActionData) message.data;
                    /**
                     * PubSub topics get unlistened to when the channel is
                     * closed, however there may be edgecases where a message
                     * still comes in and causes unwanted effects (like
                     * reopening the channel, although that may only happen for
                     * other reward and user moderation info messages).
                     */
                    if (c.isChannelOpen(Helper.toChannel(data.stream))) {
                        handleModAction(data);
                    }
                }
                else if (message.data instanceof RewardRedeemedMessageData) {
                    RewardRedeemedMessageData data = (RewardRedeemedMessageData) message.data;
                    if (c.isChannelOpen(Helper.toChannel(data.stream))) {
                        handleReward(data);
                    }
                }
                else if (message.data instanceof UserModerationMessageData) {
                    UserModerationMessageData data = (UserModerationMessageData) message.data;
                    if (c.isChannelOpen(Helper.toChannel(data.stream))) {
                        handleUserModeration(data);
                    }
                }
                else if (message.data instanceof LowTrustUserMessageData) {
                    LowTrustUserMessageData data = (LowTrustUserMessageData) message.data;
                    if (c.isChannelOpen(Helper.toChannel(data.stream))) {
                        handleLowTrustUser(data);
                    }
                }
            }
        }

        @Override
        public void info(String info) {
            g.printDebugPubSub(info);
        }
        
        private void handleReward(RewardRedeemedMessageData data) {
            User user = c.getUser(Helper.toChannel(data.stream), data.username);
            // Uses added source and reward id for merging
            g.printPointsNotice(user, data.msg, data.attached_msg,
                    MsgTags.create("chatty-source", "pubsub",
                            "custom-reward-id", data.reward_id));
        }
        
        private void handleUserModeration(UserModerationMessageData data) {
            g.printLine(c.getRoomByChannel(Helper.toChannel(data.stream)), data.info);
        }

        private void handleLowTrustUser(LowTrustUserMessageData data) {
            String channel = Helper.toChannel(data.stream);
            
            g.printLowTrustUserInfo(c.getUser(channel, data.username), data);
        }
        
    }
    
    private void handleModAction(ModeratorActionData data) {
        // A regular mod action that doesn't contain a mod action should be ignored
        boolean empty = data.type == ModeratorActionData.Type.OTHER && data.moderation_action.isEmpty() && data.args.isEmpty();
        if (data.stream != null && !empty) {
            String channel = Helper.toChannel(data.stream);
            g.printModerationAction(data, data.created_by.equals(c.getUsername()));
            chatLog.modAction(data);

            User modUser = c.getUser(channel, data.created_by);
            modUser.addModAction(data);
            g.updateUserinfo(modUser);

            String bannedUsername = ModLogInfo.getBannedUsername(data);
            if (bannedUsername != null) {
                // If this is actually a ban, add info to banned user
                User bannedUser = c.getUser(channel, bannedUsername);
                bannedUser.addBanInfo(data);
                g.updateUserinfo(bannedUser);
            }
            String unbannedUsername = ModLogInfo.getUnbannedUsername(data);
            if (unbannedUsername != null) {
                // Add info to unbanned user
                User unbannedUser = c.getUser(channel, unbannedUsername);
                int type = User.UnbanMessage.getType(data.moderation_action);
                unbannedUser.addUnban(type, data.created_by);
                g.updateUserinfo(unbannedUser);
            }
        }
    }
    
    private class EventSubResults implements EventSubListener {

        @Override
        public void messageReceived(chatty.util.api.eventsub.Message message) {
            if (message.data instanceof RaidPayload) {
                RaidPayload raid = (RaidPayload) message.data;
                String channel = Helper.toChannel(raid.fromLogin);
                String text = String.format("[Raid] Now raiding %s with %d viewers.",
                        raid.toLogin, raid.viewers);
                MsgTags tags = MsgTags.create("chatty-channel-join", Helper.toChannel(raid.toLogin));
                g.printInfo(c.getRoomByChannel(channel), text, tags);
            }
            String pollMessage = PollPayload.getPollMessage(message);
            if (pollMessage != null) {
                PollPayload poll = (PollPayload) message.data;
                g.printInfo(c.getRoomByChannel(Helper.toChannel(poll.stream)), pollMessage, MsgTags.EMPTY);
            }
            if (message.data instanceof ShieldModePayload) {
                ShieldModePayload mode = (ShieldModePayload) message.data;
                String channel = Helper.toChannel(mode.stream);
                ChannelState state = c.getChannelState(channel);
                state.setShieldMode(mode.enabled);
                String infoText = String.format("[Info] Shield mode turned %s (@%s)",
                        mode.enabled ? "on" : "off",
                        mode.moderatorLogin);
                g.printInfo(c.getRoomByChannel(channel), infoText, MsgTags.EMPTY);
                handleModAction(new ModeratorActionData(
                        "", "chat_moderator_actions", "",
                        mode.stream, mode.enabled ? "shieldMode" : "shieldModeOff",
                        new ArrayList<>(),
                        mode.moderatorLogin,
                        null));
            }
            if (message.data instanceof ShoutoutPayload) {
                ShoutoutPayload shoutout = (ShoutoutPayload) message.data;
                String channel = Helper.toChannel(shoutout.stream);
                // Message
                String infoText = String.format("[Shoutout] Was given to %s (@%s)",
                        shoutout.target_name,
                        shoutout.moderator_login);
                MsgTags tags = MsgTags.create("chatty-channel-join", Helper.toChannel(shoutout.target_login));
                g.printInfo(c.getRoomByChannel(channel), infoText, tags);
                // Mod Action
                List<String> args = new ArrayList<>();
                args.add(shoutout.target_login);
                handleModAction(new ModeratorActionData(
                        "", "chat_moderator_actions", "",
                        shoutout.stream,
                        "shoutout",
                        args,
                        shoutout.moderator_login,
                        null));
            }
        }

        @Override
        public void info(String info) {
            g.printDebugEventSub(info);
        }
        
    }
    
    /**
     * Redirects request results from the API.
     */
    private class TwitchApiResults implements TwitchApiResultListener {
        
        @Override
        public void receivedEmoticons(EmoticonUpdate update) {
            g.updateEmoticons(update);
            
            // After adding emotes, update sets
            if (update.source == EmoticonUpdate.Source.USER_EMOTES
                    && update.setsAdded != null) {
                // setsAdded contains all sets (for USER_EMOTES)
                // This may also update EmoteDialog etc.
                emotesetManager.setUserEmotesets(update.setsAdded);
            }
            
            // Other stuff
            if (refreshRequests.contains("emoticons")) {
                g.printLine("Emoticons list updated.");
                refreshRequests.remove("emoticons");
            }
        }
        
        @Override
        public void tokenVerified(String token, TokenInfo tokenInfo) {
            g.tokenVerified(token, tokenInfo);
        }
        
        @Override
        public void tokenRevoked(String error) {
            // TODO
        }
        
        @Override
        public void runCommercialResult(String stream, String text, RequestResultCode result) {
            commercialResult(stream, text, result);
        }
        
        @Override
        public void receivedChannelInfo(String stream, ChannelInfo info, RequestResultCode result) {
            
        }
        
        @Override
        public void receivedChannelStatus(ChannelStatus status, RequestResultCode resultCode) {
            g.channelStatusReceived(status, resultCode);
        }
    
        @Override
        public void putChannelInfoResult(RequestResultCode result, String error) {
            g.putChannelInfoResult(result, error);
        }

        @Override
        public void accessDenied() {
            api.checkToken();
        }

        @Override
        public void receivedUsericons(List<Usericon> icons) {
            usericonManager.addDefaultIcons(icons);
            if (refreshRequests.contains("badges2")) {
                g.printLine("Badges2 updated.");
                refreshRequests.remove("badges2");
            }
            if (refreshRequests.contains("badges")) {
                g.printLine("Badges updated.");
                refreshRequests.remove("badges");
            }
        }

        @Override
        public void receivedFollowers(FollowerInfo followerInfo) {
            g.setFollowerInfo(followerInfo);
            followerInfoNames(followerInfo);
            receivedFollowerOrSubscriberCount(followerInfo);
        }
        
        /**
         * Set follower/subscriber count in StreamInfo and send to Stream Status
         * Writer.
         * 
         * @param followerInfo 
         */
        private void receivedFollowerOrSubscriberCount(FollowerInfo followerInfo) {
            if (followerInfo.requestError) {
                return;
            }
            StreamInfo streamInfo = api.getCachedStreamInfo(followerInfo.stream);
            if (streamInfo != null) {
                boolean changed = false;
                if (followerInfo.type == Follower.Type.SUBSCRIBER) {
                    changed = streamInfo.setSubscriberCount(followerInfo.total);
                }
                else if (followerInfo.type == Follower.Type.FOLLOWER) {
                    changed = streamInfo.setFollowerCount(followerInfo.total);
                }
                if (changed && streamInfo.isValid()) {
                    streamStatusWriter.streamStatus(streamInfo);
                }
            }
        }

        @Override
        public void newFollowers(FollowerInfo followerInfo) {
            g.newFollowers(followerInfo);
        }

        @Override
        public void receivedSubscribers(FollowerInfo info) {
            g.setSubscriberInfo(info);
            followerInfoNames(info);
            receivedFollowerOrSubscriberCount(info);
        }

        private void followerInfoNames(FollowerInfo info) {
            
        }
        
        @Override
        public void receivedFollower(String stream, String username, RequestResultCode result, Follower follower) {
            g.setFollowInfo(stream, username, result, follower);
        }

        @Override
        public void receivedDisplayName(String name, String displayName) {
            
        }

        @Override
        public void receivedServer(String channel, String server) {
            LOGGER.info("Received server info: "+channel+"/"+server);
            if (fixServer && server != null) {
                String s = Helper.getServer(server);
                int p = Helper.getPort(server);
                c.disconnect();
                prepareConnectionAnyChannel(s, String.valueOf(p));
            }
            else {
                //g.printLine(channel, "An error occured requesting server info.");
            }
        }

        @Override
        public void followResult(String message) {
            g.printSystem(message);
        }

        @Override
        public void autoModResult(TwitchApi.AutoModAction action, String msgId, TwitchApi.AutoModActionResult result) {
            g.autoModRequestResult(action, msgId, result);
            autoModCommandHelper.requestResult(action, msgId, result);
        }

        @Override
        public void receivedCheerEmoticons(Set<CheerEmoticon> emoticons) {
            if (refreshRequests.contains("bits")) {
                g.printLine("Bits received.");
                refreshRequests.remove("bits");
            }
            g.setCheerEmotes(emoticons);
        }

        @Override
        public void errorMessage(String error) {
            g.printLine(error);
        }
        
    }
    
    private void addTwitchApiResultListeners() {
        api.subscribe(ResultManager.Type.SHIELD_MODE_RESULT, (ResultManager.ShieldModeResult) (stream, enabled) -> {
            String channel = Helper.toChannel(stream);
            getChannelState(channel).setShieldMode(enabled);
        });
    }

    private class MyRoomUpdatedListener implements RoomManager.RoomUpdatedListener {

        @Override
        public void roomUpdated(Room room) {
            if (c != null) {
                c.updateRoom(room);
            }
            if (g != null) {
                g.updateRoom(room);
            }
        }
        
    }
    
    // Webserver
    
    public void startWebserver() {
        if (webserver == null) {
            webserver = new Webserver(new WebserverListener());
            new Thread(webserver).start();
        }
        else {
            LOGGER.warning("Webserver already running");
            // When webserver is already running, it should be started
            g.webserverStarted();
        }
    }
    
    public void stopWebserver() {
        if (webserver != null) {
            webserver.stop();
        }
        else {
            LOGGER.info("No webserver running, can't stop it");
        }
    }
    
    private class WebserverListener implements Webserver.WebserverListener {

        @Override
        public void webserverStarted() {
            g.webserverStarted();
        }

        @Override
        public void webserverStopped() {
            webserver = null;
        }

        @Override
        public void webserverError(String error) {
            g.webserverError(error);
            webserver = null;
        }

        @Override
        public void webserverTokenReceived(String token) {
            g.webserverTokenReceived(token);
        }
    };
    
    /**
     * Update the logo for all current Stream Chat channels, based on already
     * available StreamInfo.
     */
    public void updateStreamChatLogos() {
        List<String> logins = new ArrayList<>();
        for (String channel : (List<String>) settings.getList("streamChatChannels")) {
            if (Helper.isRegularChannel(channel)) {
                logins.add(Helper.toStream(channel));
            }
        }
        api.getCachedUserInfo(logins, (result) -> {
            for (Map.Entry<String, UserInfo> entry : result.entrySet()) {
                UserInfo info = entry.getValue();
                if (info != null && !StringUtil.isNullOrEmpty(info.profileImageUrl)) {
                    usericonManager.updateChannelLogo(Helper.toChannel(info.login), info.profileImageUrl, settings.getString("streamChatLogos"));
                }
            }
        });
    }

    private class MyStreamInfoListener implements StreamInfoListener {
        
        private final ConcurrentMap<StreamInfo, Object> notFoundInfoDone
                = new ConcurrentHashMap<>();
        
        /**
         * The StreamInfo has been updated with new data from the API.
         * 
         * This may still hold a lock from the StreamInfoManager.
         * 
         * @param info 
         */
        @Override
        public void streamInfoUpdated(StreamInfo info) {
            g.updateState(true);
            g.updateChannelInfo(info);
            g.updateStreamLive(info);
            g.addStreamInfo(info);
            String channel = "#"+info.getStream();
            if (isChannelOpen(channel)) {
                // Log viewerstats if channel is still open and thus a log
                // is being written
                chatLog.viewerstats(channel, info.getViewerStats(false));
                if (info.getOnline() && info.isValid()) {
                    chatLog.viewercount(channel, info.getViewers());
                }
            }
            streamStatusWriter.streamStatus(info);
            if (info.isNotFound() && notFoundInfoDone.putIfAbsent(info, info) == null) {
                g.printLine("** This channel doesn't seem to exist on Twitch. "
                        + "You may not be able to join this channel, but trying"
                        + " anyways. **");
            }
        }

        /**
         * Displays the new stream status. Prints to the channel of the stream,
         * which should usually be open because only current channels get stream
         * data requested, but check if its still open (request response is
         * delayed and it could have been closed in the meantime).
         * 
         * This may still hold a lock from the StreamInfoManager.
         *
         * @param info
         * @param newStatus
         */
        @Override
        public void streamInfoStatusChanged(StreamInfo info, String newStatus) {
            String channel = "#" + info.getStream();
            if (isChannelOpen(channel)) {
                if (settings.getBoolean("printStreamStatus")) {
                    g.printLineByOwnerChannel(channel, "~" + newStatus + "~");
                }
                g.setChannelNewStatus(channel, newStatus);
            }
            g.statusNotification(channel, info);
        }
    }
    
    /**
     * Log viewerstats for any open channels, which can be used to log any
     * remaining data on all channels when the program is closed.
     */
    private void logAllViewerstats() {
        for (String channel : c.getOpenChannels()) {
            logViewerstats(channel);
        }
    }
    
    /**
     * Gets the viewerstats for the given channel and logs them. This can be
     * used to log any remaining data when a channel is closed or the program is
     * exited.
     *
     * @param channel
     */
    private void logViewerstats(String channel) {
        if (Helper.isRegularChannelStrict(channel)) {
            ViewerStats stats = api.getStreamInfo(Helper.toStream(channel), null).getViewerStats(true);
            chatLog.viewerstats(channel, stats);
        }
    }

    /**
     * For testing. This requires Chatty.DEBUG and Chatty.HOTKEY to be enabled.
     * 
     * If enabled, AltGr+T can be used to trigger this method.
    */
    public void testHotkey() {
//        g.printMessage("", testUser, "abc 1", false);
        //Helper.unhandledException();
        //g.showTokenWarning();
        //g.showTestNotification();
        //logViewerstats("test");
        g.showTokenWarning();
        //g.testHotkey();
    }
    
    /**
     * Tries to run a commercial on the given stream with the given length.
     * 
     * Outputs a message about it in the appropriate channel.
     * 
     * @param stream The stream to run the commercial in
     * @param length The length of the commercial in seconds
     */
    public void runCommercial(String stream, int length) {
        String channel = Helper.toChannel(stream);
        if (stream == null || stream.isEmpty()) {
            commercialResult(stream, "Can't run commercial, not on a channel.", TwitchApi.RequestResultCode.FAILED);
        }
        else if (stream.equals(settings.getString("username"))) {
            // Broadcaster can use API
            if (isChannelOpen(channel)) {
                g.printLine(roomManager.getRoom(channel), Language.getString("chat.twitchcommands.commercial", length));
            }
            else {
                g.printLine(Language.getString("chat.twitchcommands.commercial", length)+" (" + stream + ")");
            }
            api.runCommercial(stream, length);
        }
        else {
            // Editor must use command
            if (isChannelOpen(channel)) {
                // Call this directly instead so the command added to "commands" is not called
                c.command(channel, "commercial", String.valueOf(length), null);
            }
            else {
                commercialResult(stream, "Can't run commercial, not in the channel.", TwitchApi.RequestResultCode.FAILED);
            }
        }
    }
    
    /**
     * Work with the result on trying to run a commercial, which mostly is
     * returned by the Twitch API, but may also be immediately called if
     * something is formally wrong (like no or empty stream name specified).
     * 
     * Outputs an info text about the result to the appropriate channel and
     * tells the GUI so a message can be displayed in the admin dialog.
     * 
     * @param stream
     * @param text
     * @param result 
     */
    private void commercialResult(String stream, String text, RequestResultCode result) {
        String channel = "#"+stream;
        if (isChannelOpen(channel)) {
            g.printLine(roomManager.getRoom(channel), text);
        } else {
            g.printLine(text+" ("+stream+")");
        }
        g.commercialResult(stream, text, result);
    }
    
    /**
     * Receive FrankerFaceZ emoticons and icons.
     */
    private class EmoticonsListener implements FrankerFaceZListener {

        @Override
        public void channelEmoticonsReceived(EmoticonUpdate emotes) {
            g.updateEmoticons(emotes);
            if (refreshRequests.contains("ffz")) {
                g.printLine("FFZ emotes updated.");
                refreshRequests.remove("ffz");
            }
            if (refreshRequests.contains("ffzglobal")) {
                g.printLine("Global FFZ emotes updated.");
                refreshRequests.remove("ffzglobal");
            }
        }

        @Override
        public void usericonsReceived(List<Usericon> icons) {
            usericonManager.addDefaultIcons(icons);
        }

        @Override
        public void botNamesReceived(String stream, Set<String> botNames) {
            if (settings.getBoolean("botNamesFFZ")) {
                String channel = Helper.toValidChannel(stream);
                botNameManager.addBotNames(channel, botNames);
            }
        }

        @Override
        public void wsInfo(String info) {
            g.printDebugFFZ(info);
        }

        @Override
        public void authorizeUser(String code) {
            c.sendSpamProtectedMessage("#frankerfacezauthorizer", "AUTH "+code, false);
        }

        @Override
        public void wsUserInfo(String info) {
            g.printSystem("FFZ: "+info);
        }
    }
    
    /**
     * Requests the third-party emotes for the channel, if enabled.
     * 
     * @param channel The name of the channel (can be stream or channel)
     */
    public void requestChannelEmotes(String channel) {
        if (settings.getBoolean("ffz")) {
            frankerFaceZ.requestEmotes(channel, false);
            frankerFaceZ.autoUpdateFeatureFridayEmotes();
        }
        if (settings.getBoolean("bttvEmotes")) {
            bttvEmotes.requestEmotes(channel, false);
        }
        if (settings.getBoolean("seventv")) {
            sevenTV.requestEmotes(channel, false);
            sevenTV.requestEmotes(null, false);
        }
//        api.getEmotesByStreams(Helper.toStream(channel)); // Removed
    }
    
    private class EmoteListener implements EmoticonListener {

        @Override
        public void receivedEmoticons(EmoticonUpdate emoticons) {
            g.updateEmoticons(emoticons);
            if (refreshRequests.contains("bttvemotes")) {
                g.printLine("BTTV emotes updated.");
                refreshRequests.remove("bttvemotes");
            }
            else if (refreshRequests.contains("seventv")) {
                g.printLine("7TV emotes updated.");
                refreshRequests.remove("seventv");
            }
        }

        @Override
        public void receivedBotNames(String stream, Set<String> names) {
            if (settings.getBoolean("botNamesBTTV")) {
                String channel = Helper.toValidChannel(stream);
                botNameManager.addBotNames(channel, names);
            }
        }
        
    }
    
    /**
     * Only used for testing. You have to restart Chatty for the spam protection
     * in the connectin to change.
     * 
     * @param value 
     */
    public void setLinesPerSeconds(String value) {
        spamProtection.setLinesPerSeconds(value);
        c.setSpamProtection(value);
    }
    
    /**
     * Exit the program. Do some cleanup first and save stuff to file (settings,
     * addressbook, chatlogs).
     * 
     * Should run in EDT.
     */
    public void exit() {
        shuttingDown = true;
        saveSettings(true, false);
        logAllViewerstats();
        Pronouns.instance().saveCache();
        c.disconnect();
        frankerFaceZ.disconnectWs();
        pubsub.disconnect();
        eventSub.disconnect();
        g.cleanUp();
        chatLog.close();
        System.exit(0);
    }
    
    /**
     * Save all settings to file.
     * 
     * @param onExit If true, this will save the settings only if they haven't
     * already been saved with this being true before
     */
    public List<FileManager.SaveResult> saveSettings(boolean onExit, boolean force) {
        if (onExit) {
            if (settingsAlreadySavedOnExit) {
                return null;
            }
            settingsAlreadySavedOnExit = true;
        }
        
        // Prepare saving settings
        if (g != null && g.guiCreated) {
            // Run in EDT just to be safe
            GuiUtil.edtAndWait(() -> g.saveWindowStates(), "Save Window States");
        }
        // Actually write settings to file
        if (force || !settings.getBoolean("dontSaveSettings")) {
            LOGGER.info("Saving settings..");
            System.out.println("Saving settings..");
            return settings.saveSettingsToJson(force);
        }
        else {
            LOGGER.info("Not saving settings (disabled)");
        }
        return null;
    }
    
    public List<FileManager.SaveResult> manualBackup() {
        return settingsManager.fileManager.manualBackup();
    }
    
    private class SettingSaveListener implements SettingsListener {

        @Override
        public void aboutToSaveSettings(Settings settings) {
            GuiUtil.edtAndWait(() ->
                    settings.setString("previousChannel", Helper.buildStreamsString(g.getOpenChannels())),
                    "Save previous channels");
            EmoticonSizeCache.saveToFile();
        }
    }
    
    private class Messages implements TwitchConnection.ConnectionListener {

        private void checkModLogListen(User user) {
            Debugging.println("pubsub", "%s/%s==%s/%s",
                    user.hasChannelModeratorRights(),
                    user.getName(),
                    c.getUsername(),
                    user.getStream());
            if (user.getName().equals(c.getUsername())
                    && user.getStream() != null) {
                pubsub.setLocalUsername(c.getUsername());
                if (user.hasChannelModeratorRights()) {
                    if (settings.listContains("scopes", TokenInfo.Scope.CHAN_MOD.scope)) {
                        Debugging.println("pubsub", "Listen");
                        pubsub.listenModLog(user.getStream(), settings.getString("token"));
                    }
                    else {
                        EventLog.addSystemEvent("access.modlog");
                    }
                    pubsub.unlistenUserModeration(user.getStream());
                }
                else {
                    if (settings.listContains("scopes", TokenInfo.Scope.CHAT_EDIT.scope)) {
                        pubsub.listenUserModeration(user.getStream(), settings.getString("token"));
                    }
                    else {
                        EventLog.addSystemEvent("access.chat");
                    }
                    pubsub.unlistenModLog(user.getStream());
                }
            }
        }
        
        private void checkPointsListen(User user) {
            if (settings.listContains("scopes", TokenInfo.Scope.POINTS.scope)
                    && user.getName().equals(c.getUsername())
                    && user.getStream() != null) {
                pubsub.listenPoints(user.getStream(), settings.getString("token"));
            }
        }
        
        private void checkEventSubListen(User user) {
            // Is user the local user (can be on any channel though)
            if (!user.getName().equals(c.getUsername())
                    || user.getStream() == null) {
                return;
            }
            eventSub.setLocalUsername(c.getUsername());
            eventSub.listenRaid(user.getStream());
            if (settings.listContains("scopes", TokenInfo.Scope.MANAGE_POLLS.scope)
                    && user.isBroadcaster()) {
                eventSub.listenPoll(user.getStream());
            }
            if (settings.listContains("scopes", TokenInfo.Scope.MANAGE_SHIELD.scope)
                    && (user.isModerator() || user.isBroadcaster())) {
                eventSub.listenShield(user.getStream());
                api.getShieldMode(user.getRoom(), true);
            }
            if (settings.listContains("scopes", TokenInfo.Scope.MANAGE_SHOUTOUTS.scope)
                    && (user.isModerator() || user.isBroadcaster())) {
                eventSub.listenShoutouts(user.getStream());
            }
        }
        
        @Override
        public void onChannelJoined(User user) {
            channelFavorites.addJoined(user.getRoom());
            
            g.printLine(user.getRoom(), Language.getString("chat.joined", user.getRoom()));
            if (user.getRoom().hasTopic()) {
                g.printLine(user.getRoom(), user.getRoom().getTopicText());
            }
            
            // Icons and FFZ/BTTV Emotes
            //api.requestChatIcons(Helper.toStream(channel), false);
            api.getGlobalBadges(false);
            String stream = user.getStream();
            if (Helper.isValidStream(stream)) {
                api.getRoomBadges(stream, false);
                api.getCheers(stream, false);
                api.getEmotesByChannelId(stream, null, false);
                requestChannelEmotes(stream);
                frankerFaceZ.joined(stream);
                checkModLogListen(user);
                checkPointsListen(user);
                api.removeShieldModeCache(user.getRoom());
                checkEventSubListen(user);
                updateStreamInfoChannelOpen(user.getChannel());
            }
        }

        @Override
        public void onChannelLeft(Room room, boolean closeChannel) {
            chatLog.info(room.getFilename(), "You have left "+room.getDisplayName(), null);
            if (closeChannel) {
                closeChannel(room.getChannel());
            }
            else {
                g.printLine(room, Language.getString("chat.left", room));
            }
        }

        @Override
        public void onJoin(User user) {
            if (settings.getBoolean("showJoinsParts") && showUserInGui(user)) {
                g.printCompact("JOIN", user);
            }
            g.userJoined(user);
            chatLog.compact(user.getRoom().getFilename(), "JOIN", user.getRegularDisplayNick());
        }

        @Override
        public void onPart(User user) {
            if (settings.getBoolean("showJoinsParts") && showUserInGui(user)) {
                g.printCompact("PART", user);
            }
            chatLog.compact(user.getRoom().getFilename(), "PART", user.getRegularDisplayNick());
            g.userLeft(user);
        }

        @Override
        public void onUserUpdated(User user) {
            if (showUserInGui(user)) {
                g.updateUser(user);
            }
            g.updateUserinfo(user);
            checkModLogListen(user);
            checkEventSubListen(user);
        }

        @Override
        public void onChannelMessage(User user, String text, boolean action, MsgTags tags) {
            if (tags.isCustomReward()) {
                String rewardInfo = (String)settings.mapGet("rewards", tags.getCustomRewardId());
                String info = String.format("%s redeemed a custom reward (%s)",
                                            user.getDisplayNick(),
                                            rewardInfo != null ? rewardInfo : "unknown");
                g.printPointsNotice(user, info, text, tags);
            }
            else {
                g.printMessage(user, text, action, tags);
                if (tags.isReply() && tags.hasReplyUserMsg() && tags.hasId()) {
                    ReplyManager.addReply(tags.getReplyParentMsgId(), tags.getId(), String.format("<%s> %s", user.getName(), text), tags.getReplyUserMsg());
                }
                if (!action) {
                    addressbookCommands(user.getChannel(), user, text);
                    modCommandAddStreamHighlight(user, text, tags);
                }
            }
        }

        @Override
        public void onNotice(String message) {
            g.printLine("[Notice] "+message);
        }

        @Override
        public void onInfo(Room room, String infoMessage, MsgTags tags) {
            if (tags != null) {
                if (tags.isValue("msg-id", "commercial_success"))  {
                    g.commercialResult(room.getStream(), StringUtil.shortenTo(infoMessage, 60), RequestResultCode.SUCCESS);
                }
                else if (tags.isValue("msg-id", "bad_commercial_error")) {
                    g.commercialResult(room.getStream(), StringUtil.shortenTo(infoMessage, 60), RequestResultCode.UNKNOWN);
                }
                // Some error responses, like no access, don't appear to have
                // commercial-specific ids, so they are not forwarded
            }
            g.printInfo(room, infoMessage, tags);
        }

        @Override
        public void onInfo(String message) {
            g.printLine(message);
        }
        
        @Override
        public void onJoinScheduled(Collection<String> channels) {
            boolean joiningStreamChatChannel = false;
            for (String channel : channels) {
                g.joinScheduled(channel);
                if (settings.listContains("streamChatChannels", channel)) {
                    joiningStreamChatChannel = true;
                }
            }
            if (joiningStreamChatChannel) {
                updateStreamChatLogos();
            }
            // Try to request stream info for all, so it doesn't do it one by one
            api.getStreamInfo(null, new HashSet<>(Helper.toStream(channels)));
        }

        @Override
        public void onJoinAttempt(Room room) {
            /**
             * This should be the event where the channel is first opened, and
             * the stream info should be output then. If the stream info is
             * already valid, then it is output now, otherwise it is requested
             * by this and output once it is received. Doing this later, like
             * onJoin, won't work because opening the channel will always
             * request stream info, so it might be output twice (once onJoin, a
             * second time because it is new).
             */
            if (!isChannelOpen(room.getChannel())) {
                g.printStreamInfo(room);
            }
            g.printLine(room, Language.getString("chat.joining", room));
        }

        @Override
        public void onUserAdded(User user) {
            if (showUserInGui(user)) {
                g.addUser(user);
            }
        }
        
        private boolean showUserInGui(User user) {
            if (!settings.getBoolean("ignoredUsersHideInGUI")) {
                return true;
            }
            return !settings.listContains("ignoredUsers", user.getName());
        }

        @Override
        public void onUserRemoved(User user) {
            g.removeUser(user);
        }

        @Override
        public void onBan(User user, long duration, String reason, String targetMsgId) {
            User localUser = c.getLocalUser(user.getChannel());
            if (localUser != user && !localUser.hasModeratorRights()) {
                // Remove reason if not the affected user and not a mod, to be
                // consistent with other applications
                reason = "";
            }
            g.userBanned(user, duration, reason, targetMsgId);
            UserInfo userInfo = api.getCachedOnlyUserInfo(user.getName());
            chatLog.userBanned(user.getRoom().getFilename(), user.getRegularDisplayNick(),
                    duration, reason, userInfo);
        }
        
        @Override
        public void onMsgDeleted(User user, String targetMsgId, String msg) {
            User localUser = c.getLocalUser(user.getChannel());
            if (localUser == user) {
                g.printLine(user.getRoom(), "Your message was deleted: "+msg);
            } else {
                g.msgDeleted(user, targetMsgId, msg);
            }
            chatLog.msgDeleted(user, msg);
        }
        
        private Object connectAttemptMsgId;
        
        @Override
        public void onConnectionPrepare(String server) {
            g.updateState(true);
            connectAttemptMsgId = g.printLineAll(Language.getString("chat.connecting2"));
            g.printLineAllAppend(server + "..", connectAttemptMsgId);
        }
        
        @Override
        public void onConnectAttempt(String server, int port, boolean secured) {
            if (server != null) {
                if (connectAttemptMsgId != null) {
                    String text = String.format("%s:%d..%s",
                            server,
                            port,
                            secured ? " (" + Language.getString("chat.secured") + ")" : "");
                    g.printLineAllAppend(text, connectAttemptMsgId);
                    connectAttemptMsgId = null;
                }
            } else {
                g.printLineAll("Failed to connect (server or port invalid)");
            }
        }
        
        @Override
        public void onRegistered() {
            g.updateHighlightSetUsername(c.getUsername());
            //pubsub.listenModLog(c.getUsername(), settings.getString("token"));
        }

        @Override
        public void onMod(User user) {
            boolean modMessagesEnabled = settings.getBoolean("showModMessages");
            if (modMessagesEnabled && showUserInGui(user)) {
                g.printCompact("MOD", user);
            }
            chatLog.compact(user.getRoom().getFilename(), "MOD", user.getRegularDisplayNick());
        }

        @Override
        public void onUnmod(User user) {
            boolean modMessagesEnabled = settings.getBoolean("showModMessages");
            if (modMessagesEnabled && showUserInGui(user)) {
                g.printCompact("UNMOD", user);
            }
            chatLog.compact(user.getRoom().getFilename(), "UNMOD", user.getRegularDisplayNick());
        }

        @Override
        public void onDisconnect(int reason, String reasonMessage) {
            //g.clearUsers();
            if (reason == Irc.ERROR_REGISTRATION_FAILED) {
                api.checkToken();
            }
            if (reason == Irc.ERROR_CONNECTION_CLOSED) {
                pubsub.checkConnection();
            }
        }

        @Override
        public void onConnectionStateChanged(int state) {
            g.updateState(true);
        }
        
        @Override
        public void onEmotesets(String channel, Set<String> emotesets) {
            emotesetManager.setIrcEmotesets(channel, emotesets);
        }

        @Override
        public void onConnectError(String message) {
            g.printLine(message);
        }

        @Override
        public void onJoinError(Set<String> toJoin, String errorChannel, TwitchConnection.JoinError error) {
            if (error == TwitchConnection.JoinError.NOT_REGISTERED) {
                String validChannels = Helper.buildStreamsString(toJoin);
                if (c.isOffline()) {
                    prepareConnectionWithChannel(validChannels);
                }
                else {
                    g.printLine(Language.getString("chat.joinError.notConnected", validChannels));
                }
            } else if (error == TwitchConnection.JoinError.ALREADY_JOINED) {
                if (toJoin.size() == 1) {
                    g.switchToChannel(errorChannel);
                } else {
                    g.printLine(Language.getString("chat.joinError.alreadyJoined", errorChannel));
                }
            } else if (error == TwitchConnection.JoinError.INVALID_NAME) {
                g.printLine(Language.getString("chat.joinError.invalid", errorChannel));
            } else if (error == TwitchConnection.JoinError.ROOM) {
                g.printLine(Language.getString("chat.joinError.rooms", errorChannel));
            }
        }

        @Override
        public void onRawReceived(String text) {
            ircLogger.onRawReceived(text);
        }

        @Override
        public void onRawSent(String text) {
            ircLogger.onRawSent(text);
        }

        @Override
        public void onGlobalInfo(String message) {
            g.printLineAll(message);
        }

        @Override
        public void onUserlistCleared(String channel) {
            g.clearUsers(channel);
        }
        
        @Override
        public void onChannelCleared(Room room) {
            if (room != null) {
                if (settings.getBoolean("clearChatOnChannelCleared")) {
                    g.clearChat(room);
                }
                g.printLine(room, "Channel was cleared by a moderator.");
            } else {
                g.printLine("One of the channels you joined was cleared by a moderator.");
            }
        }

        @Override
        public void onWhisper(User user, String message, String emotes) {
            w.whisperReceived(user, message, emotes);
        }

        @Override
        public void onSubscriberNotification(User user, String text, String message, int months, MsgTags tags) {
            g.printSubscriberMessage(user, text, message, tags);
            
            // May be using dummy User if from twitchnotify that doesn't contain a propery name tag
            if (user.getName().isEmpty()) {
                return;
            }
            String name = user.getName();
            if (!settings.getString("abSubMonthsChan").equalsIgnoreCase(user.getChannel())) {
                return;
            }
            List<Long> monthsDef = new ArrayList<>();
            settings.getList("abSubMonths", monthsDef);
            long max = 0;
            for (long entry : monthsDef) {
                if (months >= entry && entry > max) {
                    max = entry;
                }
            }
            if (name != null && max > 0) {
                String cat = max+"months";
                addressbook.add(name, cat);
                LOGGER.info(String.format("[Subscriber] Added '%s' with category '%s'",
                        name, cat));
            }
        }
        
        @Override
        public void onUsernotice(String type, User user, String text, String message, MsgTags tags) {
            g.printUsernotice(type, user, text, message, tags);
        }

        @Override
        public void onSpecialMessage(String name, String message) {
            g.printLine(roomManager.getRoom(name), message);
        }

        @Override
        public void onRoomId(String channel, String id) {
            if (Helper.isRegularChannel(channel)) {
                api.setUserId(Helper.toStream(channel), id);
            }
        }
        
    }
    
    private class IrcLogger {
        
        private final Logger IRC_LOGGER = Logger.getLogger(TwitchClient.IrcLogger.class.getName());
        
        IrcLogger() {
            IRC_LOGGER.setUseParentHandlers(false);
            FileHandler handler = Logging.getIrcFileHandler();
            if (handler != null) {
                IRC_LOGGER.addHandler(handler);
            }
        }
        
        public void onRawReceived(String text) {
            if (settings.getBoolean("debugLogIrc")) {
                g.printDebugIrc("--> " + text);
            }
            if (settings.getBoolean("debugLogIrcFile")) {
                IRC_LOGGER.info("--> " + text);
            }
        }
        
        public void onRawSent(String text) {
            if (settings.getBoolean("debugLogIrc")) {
                g.printDebugIrc("<-- " + text);
            }
            if (settings.getBoolean("debugLogIrcFile")) {
                IRC_LOGGER.info("<-- " + text);
            }
        }
        
    }
    
    public ChannelState getChannelState(String channel) {
        return c.getChannelState(channel);
    }
    
    public Collection<String> getOpenChannels() {
        return c.getOpenChannels();
    }
    
    public Collection<Room> getOpenRooms() {
        return c.getOpenRooms();
    }
    
    /**
     * Get the currently open rooms, with a User object of the same username
     * attached to each room, if it already exists.
     *
     * @param user
     * @return A new List containing UserRoom objects of currently open rooms
     */
    public List<UserRoom> getOpenUserRooms(User user) {
        List<UserRoom> result = new ArrayList<>();
        Collection<Room> rooms = getOpenRooms();
        for (Room room : rooms) {
            User roomUser = c.getExistingUser(room.getChannel(), user.getName());
            result.add(new UserRoom(room, roomUser));
        }
        return result;
    }
    
    private class ChannelStateUpdater implements ChannelStateListener {

        @Override
        public void channelStateUpdated(ChannelState state) {
            g.updateState(true);
        }

    }
    
    public String getSecondaryConnectionsStatus() {
        return String.format("%s%s",
                frankerFaceZ.isWsConnected() ? "F" : "",
                pubsub.isConnected() ? "M" : "");
    }
    
    private class MyWhisperListener implements WhisperListener {

        @Override
        public void whisperReceived(User user, String message, String emotes) {
            g.printMessage(user, message, false, MsgTags.create("emotes", emotes));
            g.updateUser(user);
        }

        @Override
        public void info(String message) {
            g.printLine(message);
        }

        @Override
        public void whisperSent(User to, String message) {
            g.printMessage(to, message, true);
        }
    }
    
}
