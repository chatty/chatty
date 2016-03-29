
package chatty;

import chatty.ChannelStateManager.ChannelStateListener;
import chatty.util.api.TwitchApiResultListener;
import chatty.util.api.Emoticon;
import chatty.util.api.StreamInfoListener;
import chatty.util.api.TokenInfo;
import chatty.util.api.StreamInfo;
import chatty.util.api.ChannelInfo;
import chatty.util.api.TwitchApi;
import chatty.Version.VersionListener;
import chatty.WhisperConnection.WhisperListener;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.Channel;
import chatty.util.BTTVEmotes;
import chatty.util.BotNameManager;
import chatty.util.DateTime;
import chatty.util.EmoticonListener;
import chatty.util.FrankerFaceZ;
import chatty.util.FrankerFaceZListener;
import chatty.util.ImageCache;
import chatty.util.LogUtil;
import chatty.util.MiscUtil;
import chatty.util.ProcessManager;
import chatty.util.Speedruncom;
import chatty.util.StreamHighlightHelper;
import chatty.util.StreamStatusWriter;
import chatty.util.StringUtil;
import chatty.util.TwitchEmotes;
import chatty.util.TwitchEmotes.TwitchEmotesListener;
import chatty.util.Webserver;
import chatty.util.api.EmoticonSizeCache;
import chatty.util.api.EmoticonUpdate;
import chatty.util.api.Emoticons;
import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.StreamInfo.ViewerStats;
import chatty.util.api.TwitchApi.RequestResult;
import chatty.util.chatlog.ChatLog;
import chatty.util.settings.Settings;
import chatty.util.settings.SettingsListener;
import chatty.util.srl.SpeedrunsLive;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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
            + "https://api.twitch.tv/kraken/oauth2/authorize"
            + "?response_type=token"
            + "&client_id="+Chatty.CLIENT_ID
            + "&redirect_uri="+Chatty.REDIRECT_URI
            + "&force_verify=true"
            + "&scope=chat_login";
    
    /**
     * The interval to check version in (seconds)
     */
    private static final int CHECK_VERSION_INTERVAL = 60*60*24*2;

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
    
    public final TwitchEmotes twitchemotes;
    
    public final BTTVEmotes bttvEmotes;
    
    public final FrankerFaceZ frankerFaceZ;
    
    public final ChannelFavorites channelFavorites;
    
    public final UsercolorManager usercolorManager;
    
    public final UsericonManager usericonManager;
    
    public final Addressbook addressbook;
    
    public final SpeedrunsLive speedrunsLive;
    
    public final Speedruncom speedruncom;
    
    public final StatusHistory statusHistory;
    
    public final StreamStatusWriter streamStatusWriter;
    
    protected final CapitalizedNames capitalizedNames;
    
    protected final BotNameManager botNameManager;
    
    protected final CustomNames customNames;
    
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
    
    private final StreamHighlightHelper streamHighlights;
    
    private final Set<String> refreshRequests = Collections.synchronizedSet(new HashSet<String>());
    
    private final WhisperConnection w;
    private final IrcLogger ircLogger = new IrcLogger();
    
    private boolean fixServer = false;
    
    public TwitchClient(Map<String, String> args) {

        // Logging
        new Logging(this);
        Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler());

        LOGGER.info("### Log start ("+DateTime.fullDateTime()+")");
        LOGGER.info(Chatty.chattyVersion());
        LOGGER.info(Helper.systemInfo());
        LOGGER.info("Working Directory: "+System.getProperty("user.dir")
                +" Settings Directory: "+Chatty.getUserDataDirectory()
                +" Classpath: "+System.getProperty("java.class.path")
                +" Library Path: "+System.getProperty("java.library.path"));
        LOGGER.info("Retina Display: "+GuiUtil.hasRetinaDisplay());
        
        createTestUser("tduva", "#bacon_donut");
        
        settings = new Settings(Chatty.getUserDataDirectory()+"settings");
        api = new TwitchApi(new TwitchApiResults(), new MyStreamInfoListener());
        twitchemotes = new TwitchEmotes(new TwitchemotesListener());
        bttvEmotes = new BTTVEmotes(new EmoteListener());
        frankerFaceZ = new FrankerFaceZ(new EmoticonsListener());
        frankerFaceZ.autoUpdateFeatureFridayEmotes();

        // Settings
        settingsManager = new SettingsManager(settings);
        settingsManager.defineSettings();
        settingsManager.loadSettingsFromFile();
        settingsManager.backupFiles();
        settingsManager.loadCommandLineSettings(args);
        settingsManager.overrideSettings();
        settingsManager.debugSettings();
        
        ImageCache.setDefaultPath(Paths.get(Chatty.getCacheDirectory()+"img"));
        ImageCache.setCachingEnabled(settings.getBoolean("imageCache"));
        ImageCache.clearOldFiles();
        EmoticonSizeCache.loadFromFile();
        
        channelFavorites = new ChannelFavorites(settings);
        usercolorManager = new UsercolorManager(settings);
        usericonManager = new UsericonManager(settings);
        customCommands = new CustomCommands(settings);
        customCommands.loadFromSettings();
        botNameManager = new BotNameManager(settings);
        settings.addSettingsListener(new SettingSaveListener());

        streamHighlights = new StreamHighlightHelper(settings, api);
        
        if (false) {
            capitalizedNames = new CapitalizedNames(api);
        } else {
            capitalizedNames = null;
        }
        customNames = new CustomNames(settings);
        
        chatLog = new ChatLog(settings);
        chatLog.start();
        
        testUser.setUsericonManager(usericonManager);
        testUser.setUsercolorManager(usercolorManager);
        
        addressbook = new Addressbook(Chatty.getUserDataDirectory()+"addressbook",
            Chatty.getUserDataDirectory()+"addressbookImport.txt");
        addressbook.loadFromFile();
        addressbook.setSomewhatUniqueCategories(settings.getString("abUniqueCats"));
        if (settings.getBoolean("abAutoImport")) {
            addressbook.enableAutoImport();
        }
        testUser.setAddressbook(addressbook);
        
        speedrunsLive = new SpeedrunsLive();
        speedruncom = new Speedruncom(api);
        
        statusHistory = new StatusHistory(settings);
        settings.addSettingsListener(statusHistory);
        
        spamProtection = new SpamProtection();
        spamProtection.setLinesPerSeconds(settings.getString("spamProtection"));
        
        c = new TwitchConnection(new Messages(), settings, "main");
        c.setAddressbook(addressbook);
        c.setCapitalizedNamesManager(capitalizedNames);
        c.setCustomNamesManager(customNames);
        c.setUsercolorManager(usercolorManager);
        c.setUsericonManager(usericonManager);
        c.setBotNameManager(botNameManager);
        c.addChannelStateListener(new ChannelStateUpdater());
        c.setSubNotificationPattern(settings.getString("subNotificationPattern"));
        
        w = new WhisperConnection(new MyWhisperListener(), settings);
        w.setUsericonManager(usericonManager);
        w.setAddressbook(addressbook);
        w.setUsercolorManager(usercolorManager);
        
        streamStatusWriter = new StreamStatusWriter(Chatty.getUserDataDirectory(), api);
        streamStatusWriter.setSetting(settings.getString("statusWriter"));
        streamStatusWriter.setEnabled(settings.getBoolean("enableStatusWriter"));
        settings.addSettingChangeListener(streamStatusWriter);
        
        initDxSettings();
        
        GuiUtil.setLookAndFeel(settings.getString("laf"));
        
        // Create GUI
        LOGGER.info("Create GUI..");
        g = new MainGui(this);
        g.loadSettings();
        g.showGui();
        
        // Output any cached warning messages
        warning(null);
        
        // Before checkNewVersion(), so "updateAvailable" is already updated
        checkForVersionChange();
        // Check version, if enabled in this build
        if (Chatty.VERSION_CHECK_ENABLED) {
            checkNewVersion();
        }
        
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
        
        new UpdateTimer(g);
        
        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown(this)));
        
        if (Chatty.DEBUG) {
            getSpecialUser().setEmoteSets("130,4280,793,33,42");
            g.addUser("", new User("josh", ""));
            g.addUser("", new User("joshua", ""));
            User j = new User("joshimuz", "Joshimuz", "");
            j.addMessage("abc", false);
            g.addUser("", j);
            g.addUser("", new User("jolzi", ""));
            g.addUser("", new User("john", ""));
            g.addUser("", new User("tduva", ""));
            User kb = new User("kabukibot", "Kabukibot", "");
            kb.setBot(true);
            g.addUser("", kb);
            g.addUser("", new User("lotsofs", "LotsOfS", ""));
            g.addUser("", new User("anders", ""));
            g.addUser("", new User("apex1", ""));
            g.addUser("", new User("applefan", ""));
            g.addUser("", new User("austrian_", ""));
            g.addUser("", new User("adam_ak", ""));
            g.addUser("", new User("astroman", ""));
            g.addUser("", new User("xxxandre369xxx", ""));
            g.addUser("", new User("all_that_stuff_", ""));
            g.addUser("", new User("adam_ak_stole_my_bike", ""));
            g.addUser("", new User("bikelover", ""));
            g.addUser("", new User("bicyclefan", ""));
            g.addUser("", new User("botnak", "Botnak", ""));
            g.addUser("", new User("brett", ""));
            g.addUser("", new User("bll", ""));
            g.addUser("", new User("bzp______________", ""));
            
            String[] chans = new String[]{"europeanspeedsterassembly","esamarathon2","heinki","joshimuz","lotsofs","test","a","b","c"};
            for (String chan : chans) {
                //g.printLine(chan, "test");
            }
        }
    }
    

    /**
     * Based on the current renametings, rename the system properties to disable
     * Direct3D and/or DirectDraw.
     */
    private void initDxSettings() {
        try {
            Boolean d3d = !settings.getBoolean("nod3d");
            System.setProperty("sun.java2d.d3d", d3d.toString());
            Boolean ddraw = settings.getBoolean("noddraw");
            System.setProperty("sun.java2d.noddraw", ddraw.toString());
            //System.out.println(System.getProperty("sun.java2d.opengl"));
            LOGGER.info("Drawing settings: d3d: "+d3d+" / noddraw: "+ddraw);
        } catch (SecurityException ex) {
            LOGGER.warning("Error setting drawing settings: "+ex.getLocalizedMessage());
        }
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
            g.openReleaseInfo();
        }
    }
    
    /**
     * Checks for a new version if the last check was long enough ago.
     */
    private void checkNewVersion() {
        if (!settings.getBoolean("checkNewVersion")) {
            return;
        }
        /**
         * Check if enough time has passed since the last check.
         */
        long ago = System.currentTimeMillis() - settings.getLong("versionLastChecked");
        if (ago/1000 < CHECK_VERSION_INTERVAL) {
            /**
             * If not checking, check if update was detected last time.
             */
            String updateAvailable = settings.getString("updateAvailable");
            if (!updateAvailable.isEmpty()) {
                g.setUpdateAvailable(updateAvailable);
            }
            return;
        }
        settings.setLong("versionLastChecked", System.currentTimeMillis());
        g.printSystem("Checking for new version..");
        
        new Version(new VersionListener() {

            @Override
            public void versionChecked(String version, String info, boolean isNewVersion) {
                if (isNewVersion) {
                    String infoText = "";
                    if (!info.isEmpty()) {
                        infoText = "[" + info + "] ";
                    }
                    g.printSystem("New version available: "+version+" "+infoText
                            +"(Go to <Help-Website> to download)");
                    g.setUpdateAvailable(version);
                    settings.setString("updateAvailable", version);
                } else {
                    g.printSystem("You already have the newest version.");
                }
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
        testUser = new User(name, channel);
        testUser.setColor("blue");
        testUser.setGlobalMod(true);
        testUser.setBot(true);
        //testUser.setTurbo(true);
        //testUser.setModerator(true);
        //testUser.setSubscriber(true);
        testUser.setEmoteSets("4280");
        //testUser.setAdmin(true);
        //testUser.setStaff(true);
        //testUser.setBroadcaster(true);
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
            logViewerstats(channel);
            c.closeChannel(channel);
            g.removeChannel(channel);
            chatLog.closeChannel(channel);
        }
    }
    
    private void addressbookCommands(String channel, User user, String text) {
        if (settings.getString("abCommandsChannel").equals(channel)
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
            g.showMessage("Cannot connect: Incomplete login data.");
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
            g.showMessage("A channel to join has to be specified.");
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
        
        c.connect(server, ports, name, password, autojoin);
        w.connect(name, password);
        return true;
    }
    
    public boolean disconnect() {
        w.disconnect();
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
     * Anything entered in a channel input box is reacted to here.
     * 
     * @param channel
     * @param text 
     */
    public void textInput(String channel, String text) {
        if (text.isEmpty()) {
            return;
        }
        if (text.startsWith("/")) {
            commandInput(channel, text);
        }
        else {
            if (c.onChannel(channel)) {
                sendMessage(channel, text);
            }
            else if (channel.startsWith("$")) {
                w.whisperChannel(channel, text);
            }
            else {
                g.printLine("Not in a channel");
                // For testing:
                // (Also creates a channel with an empty string)
                if (Chatty.DEBUG) {
                    g.printMessage(channel,testUser,text,false,null);
                }
            }
        }     
    }
    
    private void sendMessage(String channel, String text) {
        if (c.sendSpamProtectedMessage(channel, text, false)) {
            g.printMessage(channel, c.localUserJoined(channel), text, false, null);
        } else {
            g.printLine("# Message not sent to prevent ban: " + text);
        }
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
    
    public boolean isUserlistLoaded(String channel) {
        return c.isUserlistLoaded(channel);
    }
    
    public String getHostedChannel(String channel) {
        return c.getChannelState(channel).getHosting();
    }
    
    /**
     * Text has to start with a /.
     * 
     * @param channel
     * @param text
     * @return 
     */
    public boolean commandInput(String channel, String text) {
        String[] split = text.trim().split(" ", 2);
        String command = split[0].substring(1);
        String parameter = null;
        if (split.length == 2) {
            parameter = split[1];
        }
        return command(channel, command, parameter);
    }
    
    /**
     * Reacts on all of the commands entered in the channel input box.
     * 
     * @param channel
     * @param command
     * @param parameter
     * @return 
     */
    public boolean command(String channel, String command, String parameter) {
        command = StringUtil.toLowerCase(command);
        
        // IRC Commands
        if (command.equals("quit")) {
            c.quit();
        }
        else if (command.equals("server")) {
            commandServer(parameter);
        }
        else if (command.equals("connection")) {
            if (!w.isOffline()) {
                g.printLine(c.getConnectionInfo()+" {Whisper: "+w.getConnectionInfo()+"}");
            } else {
                g.printLine(c.getConnectionInfo());
            }
        }
        else if (command.equals("join")) {
            commandJoinChannel(parameter);
        }
        else if (command.equals("part") || command.equals("close")) {
            commandPartChannel(channel);
        }
        else if (command.equals("raw")) {
            if (parameter != null) {
                c.sendRaw(parameter);
            }
        }
        else if (command.equals("me")) {
            commandActionMessage(channel,parameter);
        }
        else if (command.equals("w")) {
            w.whisperCommand(parameter, false);
        }
        else if (command.equals("clearchat")) {
            g.clearChat();
        }
        else if (command.equals("resortuserlist")) {
            g.resortUsers(channel);
        }
        
        else if (command.equals("testnotification")) {
            g.showTestNotification(parameter);
        }
        
        // Misc
        else if (command.equals("dir")) {
            g.printSystem("Settings directory: '"+Chatty.getUserDataDirectory()+"'");
        }
        else if (command.equals("wdir")) {
            g.printSystem("Working directory: '"+Chatty.getWorkingDirectory()+"'");
        }
        else if (command.equals("opendir")) {
            MiscUtil.openFolder(new File(Chatty.getUserDataDirectory()), g);
        }
        else if (command.equals("openwdir")) {
            MiscUtil.openFolder(new File(Chatty.getWorkingDirectory()), g);
        }
        else if (command.equals("openbackupdir")) {
            MiscUtil.openFolder(new File(Chatty.getBackupDirectory()), g);
        }
        
        // Settings
        else if (command.equals("set")) {
            g.printSystem(settings.setTextual(parameter));
        }
        else if (command.equals("get")) {
            g.printSystem(settings.getTextual(parameter));
        }
        else if (command.equals("clearsetting")) {
            g.printSystem(settings.clearTextual(parameter));
        }
        else if (command.equals("reset")) {
            g.printSystem(settings.resetTextual(parameter));
        }
        else if (command.equals("add")) {
            g.printSystem(settings.addTextual(parameter));
        }
        else if (command.equals("remove")) {
            g.printSystem(settings.removeTextual(parameter));
        }
        
        else if (command.equals("setcolor")) {
            if (parameter != null) {
                g.setColor(parameter);
            }
        }
        
//        else if (command.equals("userlist")) {
//            g.printSystem(addressbook.getEntries().toString());
//        }
        
        else if (command.equals("users") || command.equals("ab")) {
            g.printSystem("[Addressbook] "
                    +addressbook.command(parameter != null ? parameter : ""));
        }
        else if (command.equals("abimport")) {
            g.printSystem("[Addressbook] Importing from file..");
            addressbook.importFromFile();
        }
        else if (command.equals("proc")) {
            g.printSystem("[Proc] "+ProcessManager.command(parameter));
        }
        else if (command.equals("ignore")) {
            commandSetIgnored(parameter, null, true);
        }
        else if (command.equals("unignore")) {
            commandSetIgnored(parameter, null, false);
        }
        else if (command.equals("ignorechat")) {
            commandSetIgnored(parameter, "chat", true);
        }
        else if (command.equals("unignorechat")) {
            commandSetIgnored(parameter, "chat", false);
        }
        else if (command.equals("ignorewhisper")) {
            commandSetIgnored(parameter, "whisper", true);
        }
        else if (command.equals("unignorewhisper")) {
            commandSetIgnored(parameter, "whisper", false);
        }
        
        else if (command.equals("changetoken")) {
            g.changeToken(parameter);
        }
        
        else if (command.equals("reconnect")) {
            commandReconnect();
        }
        else if (command.equals("fixserver")) {
            commandFixServer(channel);
        }
        
        else if (command.equals("refresh")) {
            commandRefresh(channel, parameter);
        }
        
        else if (command.equals("clearimagecache")) {
            g.printLine("Clearing image cache (this can take a few seconds)");
            ImageCache.clearCache(null);
            g.printLine("Image cache cleared.");
        }
        
        else if (command.equals("clearemotecache")) {
            ImageCache.clearCache("emote_"+parameter);
            g.printLine("Emoticon image cache for type "+parameter+" cleared.");
        }
        
        else if (command.equals("refreshcase")) {
            if (capitalizedNames != null) {
                g.printLine(capitalizedNames.commandRefreshCase(parameter));
            }
        }
        else if (command.equals("setcase")) {
            if (capitalizedNames != null) {
                g.printLine(capitalizedNames.commandSetCase(parameter));
            }
        }
        else if (command.equals("getcase")) {
            if (capitalizedNames != null) {
                g.printLine(capitalizedNames.commandGetCase(parameter));
            }
        }
        else if (command.equals("setname")) {
            g.printLine(customNames.commandSetCustomName(parameter));
        }
        else if (command.equals("resetname")) {
            g.printLine(customNames.commandResetCustomname(parameter));
        }
        else if (command.equals("customcompletion")) {
            commandCustomCompletion(parameter);
        }
        else if (command.equals("copy")) {
            MiscUtil.copyToClipboard(parameter);
        }
        
        else if (command.equals("myemotes")) {
            commandMyEmotes();
        }
        else if (command.equals("emoteinfo")) {
            g.printSystem(g.emoticons.getEmoteInfo(parameter));
        }
        else if (command.equals("ffz")) {
            commandFFZ(channel);
        }
        else if (command.equals("ffzglobal")) {
            commandFFZ(null);
        }
        
        else if (command.equals("releaseinfo")) {
            g.openReleaseInfo();
        }
        
        else if (command.equals("echo")) {
            g.printLine(parameter);
        }
        
        else if (command.equals("uptime")) {
            g.printSystem("Chatty has been running for "+Chatty.uptime());
        }
        else if (command.equals("appinfo")) {
            g.printSystem(LogUtil.getMemoryUsage());
        }
        else if (command.equals("addstreamhighlight")) {
            commandAddStreamHighlight(channel, parameter);
        }
        else if (command.equals("openstreamhighlights")) {
            commandOpenStreamHighlights(channel);
        }
        
        else if (c.command(channel, command, parameter)) {
            // Already done if true
        }
        
        else if (g.commandGui(command, parameter)) {
            // Already done if true :P
        }

        // Has to be tested last, so regular commands with the same name take
        // precedence
        else if (customCommands.containsCommand(command)) {
            customCommand(channel, command, parameter);
        }
        
        //--------------------
        // Only for testing
        else if (Chatty.DEBUG || settings.getBoolean("debugCommands")) {
            testCommands(channel, command, parameter);
        }
        //----------------------
        
        else {
            g.printLine("Unknown command: "+command+" (Remember you can also "
                    + "enter Twitch Chat Commands with a point in front: \".mods\")");
            return false;
        }
        return true;
    }
    
    private void testCommands(String channel, String command, String parameter) {
        if (command.equals("addchans")) {
            String[] splitSpace = parameter.split(" ");
            String[] split2 = splitSpace[0].split(",");
            for (String chan : split2) {
                g.printLine(chan, "test");
            }
        } else if (command.equals("settestuser")) {
            String[] split = parameter.split(" ");
            createTestUser(split[0], split[1]);
        } else if (command.equals("setemoteset")) {
            testUser.setEmoteSets(parameter);
        } else if (command.equals("setemoteset2")) {
            getSpecialUser().setEmoteSets(parameter);
        } else if (command.equals("getemoteset")) {
            g.printLine(g.emoticons.getEmoticons(Integer.parseInt(parameter)).toString());
        } else if (command.equals("loadchaticons")) {
            api.requestChatIcons(parameter, true);
        } else if (command.equals("testcolor")) {
            testUser.setColor(parameter);
        } else if (command.equals("testupdatenotification")) {
            g.setUpdateAvailable("[test]");
        } else if (command.equals("testannouncement")) {
            g.setAnnouncementAvailable(Boolean.parseBoolean(parameter));
        } else if (command.equals("removechan")) {
            g.removeChannel(parameter);
        } else if (command.equals("testtimer")) {
            new Thread(new TestTimer(this, new Integer(parameter))).start();
        } //        else if (command.equals("usertest")) {
        //            System.out.println(users.getChannelsAndUsersByUserName(parameter));
        //        }
        //        else if (command.equals("insert2")) {
        //            g.printLine("\u0E07");
        //        }
        else if (command.equals("bantest")) {
            g.userBanned("", testUser);
        } else if (command.equals("bantest2")) {
            g.userBanned(channel, c.getUser(channel, parameter));
        } else if (command.equals("userjoined")) {
            c.userJoined("#test", parameter);
        } else if (command.equals("echomessage")) {
            String[] parts = parameter.split(" ");
            g.printMessage(parts[0], testUser, parts[1], false, null);
        } else if (command.equals("loadffz")) {
            frankerFaceZ.requestEmotes(parameter, true);
        } else if (command.equals("testtw")) {
            g.showTokenWarning();
        } else if (command.equals("tsonline")) {
            testStreamInfo.set(parameter, "Game", 123, -1);
            g.addStreamInfo(testStreamInfo);
        } else if (command.equals("tsoffline")) {
            testStreamInfo.setOffline();
            g.addStreamInfo(testStreamInfo);
        } else if (command.equals("testspam")) {
            g.printLine("test" + spamProtection.getAllowance() + spamProtection.tryMessage());
        } else if (command.equals("tsv")) {
            testStreamInfo.set("Title", "Game", Integer.parseInt(parameter), -1);
        } else if (command.equals("tsvs")) {
            System.out.println(testStreamInfo.getViewerStats(true));
        } else if (command.equals("tsaoff")) {
            StreamInfo info = api.getStreamInfo(g.getActiveStream(), null);
            info.setOffline();
        } else if (command.equals("tsaon")) {
            StreamInfo info = api.getStreamInfo(g.getActiveStream(), null);
            info.set("Test", "Game", 12, System.currentTimeMillis() - 1000);
        } else if (command.equals("usericonsInfo")) {
            usericonManager.debug();
        } else if (command.equals("userlistTest")) {
            g.printMessage("test1", testUser, "short message", false, null);
            g.printMessage("test2", testUser, "short message2", false, null);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printCompact("test3", "MOD", testUser);
            g.printMessage("test3", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
                    + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false, null);
            g.printMessage("test3", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
                    + "fjwfjfwjefjwefjwoeifjwefiowejfef wefjoiwefj", false, null);
            g.printMessage("test3", testUser, "longer wef wef wefwe fwe ewfwe fwef wwefwef"
                    + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false, null);
            g.printCompact("test4", "MOD", testUser);
            g.printCompact("test5", "MOD", testUser);
            g.printCompact("test6", "MOD", testUser);
            g.printCompact("test7", "MOD", testUser);
            g.printCompact("test8", "MOD", testUser);
            g.printCompact("test9", "MOD", testUser);
            g.printMessage("test10", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
                    + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false, null);
        } else if (command.equals("requestfollowers")) {
            api.getFollowers(parameter);
        } else if (command.equals("simulate")) {
            c.simulate(parameter);
        } else if (command.equals("lb")) {
            String[] split = parameter.split("&");
            String message = "";
            for (int i=0;i<split.length;i++) {
                if (!message.isEmpty()) {
                    message += "\r";
                }
                message += split[i];
            }
            sendMessage(channel, message);
        } else if (command.equals("c1")) {
            sendMessage(channel, (char)1+parameter);
        } else if (command.equals("gc")) {
            Runtime.getRuntime().gc();
            LogUtil.logMemoryUsage();
        }
    }
    
    public void customCommand(String channel, String command, String parameter) {
        if (channel == null) {
            g.printLine("Custom command: Not on a channel");
            return;
        }
        if (!customCommands.containsCommand(command)) {
            g.printLine("Custom command not found: "+command);
            return;
        }
        String result = customCommands.command(command, parameter, Helper.toStream(channel));
        if (result == null) {
            g.printLine("Custom command '"+command+"': Invalid parameters");
        } else if (result.isEmpty()) {
            // This shouldn't actually happen if edited through the settings,
            // which should trim() out whitespace, so that the command won't
            // have a result if it's empty and thus won't be added as a command.
            // Although it can also happen if the command just contains a \
            // (which is interpreted as an escape character).
            g.printLine("Custom command '"+command+"': No action specified");
        } else {
            // Check what command is called in the result of this command
            String[] resultSplit = result.trim().split(" ", 2);
            String resultCommand = resultSplit[0];
            if (resultCommand.startsWith("/")
                    && customCommands.containsCommand(resultCommand.substring(1))) {
                g.printLine("Custom command '"+command+"': Calling another custom "
                        + "command ('"+resultCommand.substring(1)+"') is not allowed");
            } else {
                textInput(channel, result);
            }
        }
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
            String name = split[0].toLowerCase();
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
     * @param channel 
     */
    public void commandJoinChannel(String channel) {
        if (channel == null) {
            g.printLine("A channel to join needs to be specified.");
        } else {
            channel = StringUtil.toLowerCase(channel.trim());
            c.joinChannel(channel);
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
                g.printMessage(channel, c.localUserJoined(channel), message, true, null);
            } else {
                g.printLine("# Action Message not sent to prevent ban: " + message);
            }
        }
    }
    
    public void commandReconnect() {
        if (c.disconnect()) {
            c.reconnect();
        } else {
            g.printLine("Could not reconnect.");
        }
    }
    
    private void commandFixServer(String channel) {
        if (!Helper.validateChannel(channel)) {
            return;
        }
        fixServer = true;
        api.getServer(channel);
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
    
    public void commandAddStreamHighlight(String channel, String parameter) {
        g.printLine(channel, streamHighlights.addHighlight(channel, parameter));
    }
    
    public void commandOpenStreamHighlights(String channel) {
        g.printLine(channel, streamHighlights.openFile());
    }
    
    private void commandRefresh(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /refresh <type> (see help)");
        } else if (parameter.equals("emoticons")) {
            g.printLine("Refreshing emoticons.. (this can take a few seconds)");
            refreshRequests.add("emoticons");
            //Emoticons.clearCache(Emoticon.Type.TWITCH);
            api.requestEmoticons(true);
        } else if (parameter.equals("badges")) {
            if (channel == null || channel.isEmpty()) {
                g.printLine("Must be on a channel to use this.");
            } else {
                g.printLine("Refreshing badges for " + channel + "..");
                refreshRequests.add("badges");
                api.requestChatIcons(Helper.toStream(channel), true);
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
            bttvEmotes.requestEmotes("$global$", true);
            bttvEmotes.requestEmotes(channel, true);
        } else if (parameter.equals("emotesets")) {
            g.printLine("Refreshing emoteset information..");
            refreshRequests.add("emotesets");
            twitchemotes.requestEmotesets(true);
        } else {
            g.printLine("Usage: /refresh <type> (invalid type, see help)");
        }
    }
    
    public User getSpecialUser() {
        return c.getSpecialUser();
    }
    
    /**
     * Outputs the emotesets for the local user. This might not work correctly
     * if the user is changed or the emotesets change during the session.
     */
    private void commandMyEmotes() {
        Set<Integer> emotesets = getSpecialUser().getEmoteSet();
        if (emotesets.isEmpty()) {
            g.printLine("No subscriber emotes found. (Only works if you joined"
                    + " any channel before.)");
        } else {
            StringBuilder b = new StringBuilder("Your subemotes: ");
            String sep = "";
            for (Integer emoteset : emotesets) {
                b.append(sep);
                if (emoteset == 33 || emoteset == 42 || emoteset == 457) {
                    b.append("Turbo emotes");
                } else {
                    String sep2 = "";
                    for (Emoticon emote : g.emoticons.getEmoticons(emoteset)) {
                        b.append(sep2);
                        b.append(emote.code);
                        sep2 = ", ";
                    }
                }
                sep = " / ";
            }
            g.printLine(b.toString());
        }
    }
    
    private void commandFFZ(String channel) {
        Set<Emoticon> output;
        StringBuilder b = new StringBuilder();
        if (channel == null) {
            b.append("Global FFZ emotes: ");
            output = Emoticons.filterByType(g.emoticons.getGlobalTwitchEmotes(), Emoticon.Type.FFZ);
        } else {
            b.append("This channel's FFZ emotes: ");
            Set<Emoticon> emotes = g.emoticons.getEmoticons(Helper.toStream(channel));
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
        g.printLine(channel, b.toString());
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
                cachedDebugMessages.add(line);
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
    
    /**
     * Output a warning.
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
    
    
    /**
     * Redirects request results from the API.
     */
    private class TwitchApiResults implements TwitchApiResultListener {
        
        @Override
        public void receivedEmoticons(Set<Emoticon> emoticons) {
            g.addEmoticons(emoticons);
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
        public void runCommercialResult(String stream, String text, RequestResult result) {
            commercialResult(stream, text, result);
        }
 
        @Override
        public void gameSearchResult(Set<String> games) {
            g.gameSearchResult(games);
        }
        
        @Override
        public void receivedChannelInfo(String channel, ChannelInfo info, RequestResult result) {
            g.setChannelInfo(channel, info, result);
        }
    
        @Override
        public void putChannelInfoResult(RequestResult result) {
            g.putChannelInfoResult(result);
        }

        @Override
        public void accessDenied() {
            checkToken();
        }

        @Override
        public void receivedUsericons(List<Usericon> icons) {
            usericonManager.addDefaultIcons(icons);
            if (refreshRequests.contains("badges")) {
                g.printLine("Badges updated.");
                refreshRequests.remove("badges");
            }
        }

        @Override
        public void receivedFollowers(FollowerInfo followerInfo) {
            g.setFollowerInfo(followerInfo);
            followerInfoNames(followerInfo);
            
        }

        @Override
        public void newFollowers(FollowerInfo followerInfo) {
            g.followerSound(Helper.toValidChannel(followerInfo.stream));
        }

        @Override
        public void receivedSubscribers(FollowerInfo info) {
            g.setSubscriberInfo(info);
            followerInfoNames(info);
        }
        
        private void followerInfoNames(FollowerInfo info) {
            if (capitalizedNames != null && info.followers != null) {
                for (Follower f : info.followers) {
                    capitalizedNames.setName(StringUtil.toLowerCase(f.name), f.name);
                }
            }
        }

        @Override
        public void receivedDisplayName(String name, String displayName) {
            capitalizedNames.setName(name, displayName);
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
                g.printLine(channel, "An error occured requesting server info.");
            }
        }
    }
    
    private void checkToken() {
        api.checkToken(settings.getString("token"));
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
            g.updateChannelInfo();
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
            if (capitalizedNames != null && info.hasDisplayName()) {
                capitalizedNames.setName(info.stream, info.getDisplayName());
            }
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
                    g.printLine(channel, "~" + newStatus + "~");
                }
                g.setChannelNewStatus(channel, newStatus);
                
                /**
                 * Only do warning/unhost stuff if stream is only at most 15
                 * minutes old. This prevents unhosting at the end of the stream
                 * when the status may change from online -> offline -> online
                 * due to cached data from the Twitch API and unhost when the
                 * streamer already hosted someone else intentionally.
                 */
                if (info.getOnline()
                        && info.getTimeStartedWithPicnicAgo() < 15*60*1000
                        && getHostedChannel(channel) != null) {
                    if (settings.getBoolean("autoUnhost")
                            && c.onChannel(channel)
                            && (
                                info.stream.equals(c.getUsername())
                                || settings.listContains("autoUnhostStreams", info.stream)
                            )) {
                        c.sendCommandMessage(channel, ".unhost", "Trying to turn off host mode.. (Auto-Unhost)");
                    } else {
                        g.printLine(channel, "** Still hosting another channel while streaming. **");
                    }
                }
            }
            if (info.getOnline() || !settings.getBoolean("ignoreOfflineNotifications")) {
                g.statusNotification(channel, newStatus);
            }
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
        if (Helper.isRegularChannel(channel)) {
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
        if (stream == null || stream.isEmpty()) {
            commercialResult(stream, "Can't run commercial, not on a channel.", TwitchApi.RequestResult.FAILED);
        }
        else {
            String channel = "#"+stream;
            if (isChannelOpen(channel)) {
                g.printLine(channel, "Trying to run "+length+"s commercial..");
            } else {
                g.printLine("Trying to run "+length+"s commercial.. ("+stream+")");
            }
            api.runCommercial(stream, settings.getString("token"), length);
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
    private void commercialResult(String stream, String text, RequestResult result) {
        String channel = "#"+stream;
        if (isChannelOpen(channel)) {
            g.printLine(channel, text);
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
        public void botNamesReceived(Set<String> botNames) {
            if (settings.getBoolean("botNamesFFZ")) {
                botNameManager.addBotNames(null, botNames);
            }
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
        }
        if (settings.getBoolean("bttvEmotes")) {
            bttvEmotes.requestEmotes(channel, false);
        }
    }
    
    private class EmoteListener implements EmoticonListener {

        @Override
        public void receivedEmoticons(Set<Emoticon> emoticons) {
            g.addEmoticons(emoticons);
            if (refreshRequests.contains("bttvemotes")) {
                g.printLine("BTTV emotes updated.");
                refreshRequests.remove("bttvemotes");
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
    
    private class TwitchemotesListener implements TwitchEmotesListener {

        @Override
        public void emotesetsReceived(Map<Integer, String> emotesetStreams) {
            if (refreshRequests.contains("emotesets")) {
                g.printLine("Emoteset information updated.");
                refreshRequests.remove("emotesets");
            }
            g.setEmotesets(emotesetStreams);
            c.setEmotesets(emotesetStreams);
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
    }
    
    /**
     * Exit the program. Do some cleanup first and save stuff to file (settings,
     * addressbook, chatlogs).
     * 
     * Should run in EDT.
     */
    public void exit() {
        shuttingDown = true;
        saveSettings(true);
        logAllViewerstats();
        c.disconnect();
        w.disconnect();
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
    public void saveSettings(boolean onExit) {
        if (onExit) {
            if (settingsAlreadySavedOnExit) {
                return;
            }
            settingsAlreadySavedOnExit = true;
        }
        
        LOGGER.info("Saving settings..");
        System.out.println("Saving settings..");
        if (capitalizedNames != null) {
            capitalizedNames.saveToFile();
        }
        
        // Prepare saving settings
        if (g != null && g.guiCreated) {
            g.saveWindowStates();
        }
        // Actually write settings to file
        if (!onExit || !settings.getBoolean("dontSaveSettings")) {
            addressbook.saveToFile();
            settings.saveSettingsToJson();
        }
    }
    
    private class SettingSaveListener implements SettingsListener {

        @Override
        public void aboutToSaveSettings(Settings settings) {
            Collection<String> openChans;
            if (SwingUtilities.isEventDispatchThread()) {
                openChans = g.getOpenChannels();
            } else {
                openChans = c.getOpenChannels();
            }
            settings.setString("previousChannel", Helper.buildStreamsString(openChans));
            EmoticonSizeCache.saveToFile();
        }
        
    }
    
    private class Messages implements TwitchConnection.ConnectionListener {

        @Override
        public void onChannelJoined(String channel) {
            channelFavorites.addChannelToHistory(channel);
            
            g.printLine(channel,"You have joined " + channel);
            
            // Icons and FFZ/BTTV Emotes
            api.requestChatIcons(Helper.toStream(channel), false);
            requestChannelEmotes(channel);
        }

        @Override
        public void onChannelLeft(String channel) {
            chatLog.info(channel, "You have left "+channel);
            closeChannel(channel);
        }

        @Override
        public void onJoin(User user) {
            String channel = user.getChannel();
            if (settings.getBoolean("showJoinsParts")) {
                g.printCompact(channel,"JOIN", user);
            }
            g.playSound("joinPart", channel);
            chatLog.compact(channel, "JOIN", user);
        }

        @Override
        public void onPart(User user) {
            if (settings.getBoolean("showJoinsParts")) {
                g.printCompact(user.getChannel(), "PART", user);
            }
            chatLog.compact(user.getChannel(), "PART", user);
            g.playSound("joinPart", user.getChannel());
        }

        @Override
        public void onUserUpdated(User user) {
            g.updateUser(user);
            g.updateUserinfo(user);
        }

        @Override
        public void onChannelMessage(User user, String message, boolean action,
                String emotes) {
            g.printMessage(user.getChannel(), user, message, action, emotes);
            if (!action) {
                addressbookCommands(user.getChannel(), user, message);
                
                // Stream Highlights
                String result = streamHighlights.modCommand(user, message);
                if (result != null) {
                    result = user.getDisplayNick()+": "+result;
                    if (settings.getBoolean("streamHighlightChannelRespond")) {
                        sendMessage(user.getChannel(), result);
                    } else {
                        g.printLine(result);
                    }
                }
            }
        }

        @Override
        public void onNotice(String message) {
            g.printLine("[Notice] "+message);
        }

        @Override
        public void onInfo(String channel, String infoMessage) {
            g.printLine(channel, infoMessage);
        }

        @Override
        public void onInfo(String message) {
            g.printLine(message);
        }

        @Override
        public void onJoinAttempt(String channel) {
            /**
             * This should be the event where the channel is first opened, and
             * the stream info should be output then. If the stream info is
             * already valid, then it is output now, otherwise it is requested
             * by this and output once it is received. Doing this later, like
             * onJoin, won't work because opening the channel will always
             * request stream info, so it might be output twice (once onJoin, a
             * second time because it is new).
             */
            if (!isChannelOpen(channel)) {
                g.printStreamInfo(channel);
            }
            g.printLine(channel, "Joining "+channel+"..");
        }

        @Override
        public void onUserAdded(User user) {
            g.addUser(user.getChannel(), user);
        }

        @Override
        public void onUserRemoved(User user) {
            g.removeUser(user.getChannel(), user);
        }

        @Override
        public void onBan(User user) {
            g.userBanned(user.getChannel(), user);
            chatLog.compact(user.getChannel(), "BAN", user);
        }
        
        @Override
        public void onRegistered() {
            g.updateHighlightSetUsername(c.getUsername());
        }

        @Override
        public void onMod(User user) {
            boolean modMessagesEnabled = settings.getBoolean("showModMessages");
            String channel = user.getChannel();
            if (modMessagesEnabled) {
                g.printCompact(channel, "MOD", user);
            }
            chatLog.compact(channel, "MOD", user);
        }

        @Override
        public void onUnmod(User user) {
            boolean modMessagesEnabled = settings.getBoolean("showModMessages");
            String channel = user.getChannel();
            if (modMessagesEnabled) {
                g.printCompact(channel, "UNMOD", user);
            }
            chatLog.compact(channel, "UNMOD", user);
        }

        @Override
        public void onDisconnect(int reason, String reasonMessage) {
            //g.clearUsers();
            if (reason == Irc.ERROR_REGISTRATION_FAILED) {
                checkToken();
            }
        }

        @Override
        public void onConnectionStateChanged(int state) {
            g.updateState(true);
        }

        @Override
        public void onSpecialUserUpdated() {
            g.updateEmotesDialog();
            g.updateEmoteNames();
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
                    g.openConnectDialog(validChannels);
                }
                g.printLine("Can't join '" + validChannels + "' (not connected)");
            } else if (error == TwitchConnection.JoinError.ALREADY_JOINED) {
                if (toJoin.size() == 1) {
                    g.switchToChannel(errorChannel);
                } else {
                    g.printLine("Can't join '" + errorChannel + "' (already joined)");
                }
            } else if (error == TwitchConnection.JoinError.INVALID_NAME) {
                g.printLine("Can't join '"+errorChannel+"' (invalid channelname)");
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
        public void onHost(String channel, String target) {
        }
        
        @Override
        public void onChannelCleared(String channel) {
            if (channel != null) {
                if (settings.getBoolean("clearChatOnChannelCleared")) {
                    g.clearChat(channel);
                }
                g.printLine(channel, "Channel was cleared by a moderator.");
            } else {
                g.printLine("One of the channels you joined was cleared by a moderator.");
            }
        }

        @Override
        public void onWhisper(User user, String message, String emotes) {
        }

        @Override
        public void onSubscriberNotification(String channel, String name, int months) {
            System.out.println(channel+" "+name+" "+months);
            if (!settings.getString("abSubMonthsChan").equalsIgnoreCase(channel)) {
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
        
    }
    
    private class IrcLogger {
        
        private final Logger IRC_LOGGER = Logger.getLogger(TwitchClient.Messages.class.getName()+"IRClog");
        
        IrcLogger() {
            IRC_LOGGER.setUseParentHandlers(false);
            IRC_LOGGER.addHandler(Logging.getIrcFileHandler());
        }
        
        public void onRawReceived(String text) {
            if (settings.getBoolean("debugLogIrc")) {
                g.printDebugIrc(">> " + text);
            }
            if (settings.getBoolean("debugLogIrcFile")) {
                IRC_LOGGER.info(">> " + text);
            }
        }
        
        public void onRawSent(String text) {
            if (settings.getBoolean("debugLogIrc")) {
                g.printDebugIrc("<<< " + text);
            }
            if (settings.getBoolean("debugLogIrcFile")) {
                IRC_LOGGER.info("SENT: " + text);
            }
        }
        
    }
    
    public ChannelState getChannelState(String channel) {
        return c.getChannelState(channel);
    }
    
    private class ChannelStateUpdater implements ChannelStateListener {

        @Override
        public void channelStateUpdated(ChannelState state) {
            g.updateState(true);
        }

    }
    
    public boolean isWhisperAvailable() {
        return w.isAvailable();
    }
    
    private class MyWhisperListener implements WhisperListener {

        @Override
        public void whisperReceived(User user, String message, String emotes) {
            g.printMessage(WhisperConnection.WHISPER_CHANNEL, user, message, false, emotes);
            if (settings.getLong("whisperDisplayMode") == WhisperConnection.DISPLAY_ONE_WINDOW) {
                g.updateUser(user);
            }
        }

        @Override
        public void info(String message) {
            g.printLine(message);
        }

        @Override
        public void whisperSent(User to, String message) {
            g.printMessage(WhisperConnection.WHISPER_CHANNEL, to, message, true, null);
        }

        @Override
        public void onRawSent(String text) {
            ircLogger.onRawSent(text);
        }

        @Override
        public void onRawReceived(String text) {
            ircLogger.onRawReceived(text);
        }
        
    }
    
}
