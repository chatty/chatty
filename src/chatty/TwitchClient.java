
package chatty;

import chatty.gui.components.updating.Version;
import chatty.ChannelFavorites.Favorite;
import chatty.lang.Language;
import chatty.gui.colors.UsercolorManager;
import chatty.gui.components.admin.StatusHistory;
import chatty.util.commands.CustomCommands;
import chatty.util.api.usericons.Usericon;
import chatty.util.api.usericons.UsericonManager;
import chatty.ChannelStateManager.ChannelStateListener;
import chatty.util.api.TwitchApiResultListener;
import chatty.util.api.Emoticon;
import chatty.util.api.StreamInfoListener;
import chatty.util.api.TokenInfo;
import chatty.util.api.StreamInfo;
import chatty.util.api.ChannelInfo;
import chatty.util.api.TwitchApi;
import chatty.WhisperManager.WhisperListener;
import chatty.gui.GuiUtil;
import chatty.gui.LaF;
import chatty.gui.LaF.LaFSettings;
import chatty.gui.MainGui;
import chatty.gui.components.eventlog.EventLog;
import chatty.gui.components.menus.UserContextMenu;
import chatty.gui.components.textpane.ModLogInfo;
import chatty.gui.components.updating.Stuff;
import chatty.splash.Splash;
import chatty.util.BTTVEmotes;
import chatty.util.BotNameManager;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.EmoticonListener;
import chatty.util.ffz.FrankerFaceZ;
import chatty.util.ffz.FrankerFaceZListener;
import chatty.util.ImageCache;
import chatty.util.LogUtil;
import chatty.util.MiscUtil;
import chatty.util.OtherBadges;
import chatty.util.ProcessManager;
import chatty.util.RawMessageTest;
import chatty.util.Speedruncom;
import chatty.util.StreamHighlightHelper;
import chatty.util.StreamStatusWriter;
import chatty.util.StringUtil;
import chatty.util.TwitchEmotesApi;
import chatty.util.UserRoom;
import chatty.util.Webserver;
import chatty.util.api.AutoModCommandHelper;
import chatty.util.api.CheerEmoticon;
import chatty.util.api.EmotesetManager;
import chatty.util.api.EmoticonSizeCache;
import chatty.util.api.EmoticonUpdate;
import chatty.util.api.Emoticons;
import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.StreamInfo.StreamType;
import chatty.util.api.StreamInfo.ViewerStats;
import chatty.util.api.StreamTagManager.StreamTag;
import chatty.util.api.TwitchApi.RequestResultCode;
import chatty.util.api.pubsub.UserinfoMessageData;
import chatty.util.api.pubsub.Message;
import chatty.util.api.pubsub.ModeratorActionData;
import chatty.util.api.pubsub.PubSubListener;
import chatty.util.chatlog.ChatLog;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import chatty.util.irc.MsgTags;
import chatty.util.settings.FileManager;
import chatty.util.settings.Settings;
import chatty.util.settings.SettingsListener;
import chatty.util.srl.SpeedrunsLive;
import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

	private final TwitchConnection twitchconnection;

	/**
	 * Holds the TwitchApi object, which is used to make API requests
	 */
	public final TwitchApi twitchapi;

	public final chatty.util.api.pubsub.Manager pubsub;
	private final PubSubResults pubsubListener = new PubSubResults();

	public final EmotesetManager emotesetManager;

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
	protected MainGui maingui;

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

	private final WhisperManager whispermanager;
	private final IrcLogger ircLogger;

	private boolean fixServer = false;

	public TwitchClient(Map<String, String> args) {
		// Logging
		new Logging(this);
		Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler());

		LOGGER.info("### Log start ("+DateTime.fullDateTime()+")");
		LOGGER.info(Chatty.chattyVersion());
		LOGGER.info(Helper.systemInfo());
		LOGGER.info("[Working Directory] "+System.getProperty("user.dir")
		+" [Settings Directory] "+Chatty.getUserDataDirectory()
		+" [Classpath] "+System.getProperty("java.class.path"));

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

		Helper.setDefaultTimezone(settings.getString("timezone"));

		addressbook = new Addressbook(Chatty.getUserDataDirectory()+"addressbook",
				Chatty.getUserDataDirectory()+"addressbookImport.txt", settings);
		if (!addressbook.loadFromSettings()) {
			addressbook.loadFromFile();
		}
		addressbook.setSomewhatUniqueCategories(settings.getString("abUniqueCats"));
		if (settings.getBoolean("abAutoImport")) {
			addressbook.enableAutoImport();
		}

		initDxSettings();

		if (settings.getBoolean("splash")) {
			Splash.initSplashScreen(Splash.getLocation((String)settings.mapGet("windows", "main")));
		}

		// Create after Logging is created, since that resets some stuff
		ircLogger = new IrcLogger();

		createTestUser("tduva", "");

		twitchapi = new TwitchApi(new TwitchApiResults(), new MyStreamInfoListener());
		bttvEmotes = new BTTVEmotes(new EmoteListener(), twitchapi);
		TwitchEmotesApi.api.setTwitchApi(twitchapi);

		Language.setLanguage(settings.getString("language"));

		pubsub = new chatty.util.api.pubsub.Manager(
				settings.getString("pubsub"), pubsubListener, twitchapi);

		frankerFaceZ = new FrankerFaceZ(new EmoticonsListener(), settings, twitchapi);

		ImageCache.setDefaultPath(Paths.get(Chatty.getCacheDirectory()+"img"));
		ImageCache.setCachingEnabled(settings.getBoolean("imageCache"));
		ImageCache.deleteExpiredFiles();
		EmoticonSizeCache.loadFromFile();

		usercolorManager = new UsercolorManager(settings);
		usericonManager = new UsericonManager(settings);
		customCommands = new CustomCommands(settings, twitchapi, this);
		customCommands.loadFromSettings();
		botNameManager = new BotNameManager(settings);
		settings.addSettingsListener(new SettingSaveListener());

		streamHighlights = new StreamHighlightHelper(settings, twitchapi);

		customNames = new CustomNames(settings);

		chatLog = new ChatLog(settings);
		chatLog.start();

		testUser.setUsericonManager(usericonManager);
		testUser.setUsercolorManager(usercolorManager);
		testUser.setAddressbook(addressbook);

		speedrunsLive = new SpeedrunsLive();
		speedruncom = new Speedruncom(twitchapi);

		statusHistory = new StatusHistory(settings);
		settings.addSettingsListener(statusHistory);

		spamProtection = new SpamProtection();
		spamProtection.setLinesPerSeconds(settings.getString("spamProtection"));

		roomManager = new RoomManager(new MyRoomUpdatedListener());
		channelFavorites = new ChannelFavorites(settings, roomManager);

		twitchconnection = new TwitchConnection(new Messages(), settings, "main", roomManager);
		twitchconnection.setAddressbook(addressbook);
		twitchconnection.setCustomNamesManager(customNames);
		twitchconnection.setUsercolorManager(usercolorManager);
		twitchconnection.setUsericonManager(usericonManager);
		twitchconnection.setBotNameManager(botNameManager);
		twitchconnection.addChannelStateListener(new ChannelStateUpdater());
		twitchconnection.setMaxReconnectionAttempts(settings.getLong("maxReconnectionAttempts"));

		whispermanager = new WhisperManager(new MyWhisperListener(), settings, twitchconnection);

		streamStatusWriter = new StreamStatusWriter(Chatty.getUserDataDirectory(), twitchapi);
		streamStatusWriter.setSetting(settings.getString("statusWriter"));
		streamStatusWriter.setEnabled(settings.getBoolean("enableStatusWriter"));
		settings.addSettingChangeListener(streamStatusWriter);

		LaF.setLookAndFeel(LaFSettings.fromSettings(settings));
		GuiUtil.addMacKeyboardActions();

		// Create GUI
		LOGGER.info("Create GUI..");
		maingui = new MainGui(this);
		maingui.loadSettings();
		emotesetManager = new EmotesetManager(twitchapi, maingui, settings);
		maingui.showGui();

		autoModCommandHelper = new AutoModCommandHelper(maingui, twitchapi);

		if (Chatty.DEBUG) {
			Room testRoom =  Room.createRegular("");
			maingui.addUser(new User("josh", testRoom));
			maingui.addUser(new User("joshua", testRoom));
			User josh = new User("joshimuz", "Joshimuz", testRoom);
			for (int i=0;i<99;i++) {
				josh.addMessage("abc", false, null);
			}
			josh.addMessage("abc", false, null);
			josh.setDisplayNick("Joshimoose");
			josh.setTurbo(true);
			josh.setVip(true);
			maingui.addUser(josh);
			maingui.addUser(new User("jolzi", testRoom));
			maingui.addUser(new User("john", testRoom));
			User tduva = new User("tduva", testRoom);
			for (int i=0;i<100;i++) {
				tduva.addMessage("abc", false, null);
			}
			tduva.setModerator(true);
			maingui.addUser(tduva);
			User kb = new User("kabukibot", "Kabukibot", testRoom);
			for (int i=0;i<80;i++) {
				kb.addMessage("abc", false, null);
			}
			kb.clearMessagesIfInactive(0);
			kb.addMessage("abc", false, null);
			kb.setDisplayNick("reallyLongDisplayNickAndStuffBlahNeedsToBeLonger");
			kb.setBot(true);
			maingui.addUser(kb);
			User lots = new User("lotsofs", "LotsOfS", testRoom);
			for (int i=0;i<120;i++) {
				lots.addMessage("abc", false, null);
			}
			lots.clearMessagesIfInactive(0);
			for (int i=0;i<100;i++) {
				lots.addMessage("abc", false, null);
			}
			lots.addMessage("abc", false, null);
			lots.setSubscriber(true);
			maingui.addUser(lots);
			User anders = new User("anders", testRoom);
			for (int i=0;i<120;i++) {
				anders.addMessage("abc", false, null);
			}
			anders.setSubscriber(true);
			anders.setVip(true);
			maingui.addUser(anders);
			maingui.addUser(new User("apex1", testRoom));
			maingui.addUser(new User("xfwefawf32q4543t5greger", testRoom));
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

		// Request some stuff
		twitchapi.getEmotesBySets("0");

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
				maingui.openConnectDialog(null);
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
	}


	/**
	 * Based on the current renametings, rename the system properties to disable
	 * Direct3D and/or DirectDraw.
	 */
	private void initDxSettings() {
		try {
			Boolean d3d = !settings.getBoolean("nod3d");
			Boolean ddraw = settings.getBoolean("noddraw");
			LOGGER.info(String.format("d3d: %s (%s) / noddraw: %s (%s) / opengl: (%s) / retina: %s",
					d3d, System.getProperty("sun.java2d.d3d"),
					ddraw, System.getProperty("sun.java2d.noddraw"),
					System.getProperty("sun.java2d.opengl"),
					GuiUtil.hasRetinaDisplay()));
			System.setProperty("sun.java2d.d3d", d3d.toString());
			System.setProperty("sun.java2d.noddraw", ddraw.toString());
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
			if (settingsManager.getLoadSuccess()) {
				// Don't bother user if settings were probably corrupted
				maingui.openReleaseInfo();
			}
		}
	}

	/**
	 * Checks for a new version if the last check was long enough ago.
	 */
	private void checkNewVersion() {
		Version.check(settings, (newVersion,releases) -> {
			if (newVersion != null) {
				maingui.setUpdateAvailable(newVersion, releases);
			} else {
				maingui.printSystem("You already have the newest version.");
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
		//testUser.setModerator(true);
		//testUser.setSubscriber(true);
		//testUser.setAdmin(true);
		//testUser.setStaff(true);
		//testUser.setBroadcaster(true);
		LinkedHashMap<String, String> badgesTest = new LinkedHashMap<>();
		//        badgesTest.put("global_mod", "1");
		//        badgesTest.put("moderator", "1");
		//        badgesTest.put("premium", "1");
		//        badgesTest.put("bits", "1000000");
		testUser.setTwitchBadges(badgesTest);
	}

	/**
	 * Close all channels except the ones in the given Array.
	 * 
	 * @param except 
	 */
	private void closeAllChannelsExcept(String[] except) {
		Set<String> copy = twitchconnection.getOpenChannels();
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
		if (twitchconnection.onChannel(channel)) {
			twitchconnection.partChannel(channel);
		}
		else { // Always remove channel (or try to), so it can be closed even if it bugged out
			Room room = roomManager.getRoom(channel);
			logViewerstats(channel);
			twitchconnection.closeChannel(channel);
			closeChannelStuff(room);
			maingui.removeChannel(channel);
			chatLog.closeChannel(room.getFilename());
		}
	}

	private void closeChannelStuff(Room room) {
		// Check if not on any associated channel anymore
		if (!twitchconnection.onOwnerChannel(room.getOwnerChannel())) {
			frankerFaceZ.left(room.getOwnerChannel());
			pubsub.unlistenModLog(room.getStream());
			pubsub.unlistenPoints(room.getStream());
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
				maingui.printSystem("[Ab/Mod] "+addressbook.command(commandText));
			}
		}
	}

	public String getUsername() {
		return twitchconnection.getUsername();
	}

	public User getUser(String channel, String name) {
		return twitchconnection.getUser(channel, name);
	}

	public User getExistingUser(String channel, String name) {
		return twitchconnection.getExistingUser(channel, name);
	}

	public User getLocalUser(String channel) {
		return twitchconnection.getExistingUser(channel, twitchconnection.getUsername());
	}

	public void clearUserList() {
		twitchconnection.setAllOffline();
		maingui.clearUsers(null);
	}

	public String isLong(int len, String Temp, String Default)
	{
		if (len > 0)
			return Temp;
		else
			return Default;
	}
	private String getServer() {
		String serverDefault = settings.getString("serverDefault");
		String serverTemp = settings.getString("server");
		String ret = isLong(serverTemp.length(), serverTemp, serverDefault);
		return ret;
	}

	private String getPorts() {
		String portDefault = settings.getString("portDefault");
		String portTemp = settings.getString("port");
		String ret = isLong(portTemp.length(), portTemp, portDefault);
		return ret;
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
		if (twitchconnection.getOpenChannels().isEmpty()) {
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

		if (twitchconnection.getState() > Irc.STATE_OFFLINE) {
			maingui.showMessage("Cannot connect: Already connected.");
			return false;
		}

		if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
			maingui.showMessage("Cannot connect: Incomplete login data.");
			return false;
		}

		String[] autojoin;
		Set<String> openChannels = twitchconnection.getOpenChannels();
		if (channel == null) {
			autojoin = new String[openChannels.size()];
			openChannels.toArray(autojoin);
		} else {
			autojoin = Helper.parseChannels(channel);
		}
		if (autojoin.length == 0) {
			maingui.showMessage("A channel to join has to be specified.");
			return false;
		}

		if (server == null || server.isEmpty()) {
			maingui.showMessage("Invalid server specified.");
			return false;
		}

		closeAllChannelsExcept(autojoin);

		settings.setString("username", name);
		if (channel != null) {
			settings.setString("channel", channel);
		}
		twitchapi.requestUserId(Helper.toStream(autojoin));
		//        api.getEmotesByStreams(Helper.toStream(autojoin)); // Removed
		twitchconnection.connect(server, ports, name, password, autojoin);
		return true;
	}

	public boolean disconnect() {
		return twitchconnection.disconnect();
	}

	public void joinChannels(Set<String> channels) {
		twitchconnection.joinChannels(channels);
	}

	public void joinChannel(String channels) {
		twitchconnection.joinChannel(channels);
	}

	public int getState() {
		return twitchconnection.getState();
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
		text = maingui.replaceEmojiCodes(text);
		String channel = room.getChannel();
		if (text.startsWith("//")) {
			anonCustomCommand(room, text.substring(1), commandParameters);
		}
		else if (text.startsWith("/")) {
			commandInput(room, text, commandParameters);
		}
		else {
			if (twitchconnection.onChannel(channel)) {
				sendMessage(channel, text, true);
			}
			else if (channel.startsWith("$")) {
				whispermanager.whisperChannel(channel, text);
			}
			else if (channel.startsWith("*")) {
				twitchconnection.sendCommandMessage(channel, text, "> "+text);
			}
			else {
				// For testing:
				// (Also creates a channel with an empty string)
				if (Chatty.DEBUG) {
					User user = twitchconnection.getUser(room.getChannel(), "test");
					if (testUser.getRoom().equals(room)) {
						user = testUser;
					}
					maingui.printMessage(user,text,false);
				} else {
					maingui.printLine("Not in a channel");
				}
			}
		}     
	}

	private void sendMessage(String channel, String text) {
		sendMessage(channel, text, false);
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
		if (twitchconnection.sendSpamProtectedMessage(channel, text, false)) {
			User user = twitchconnection.localUserJoined(channel);
			maingui.printMessage(user, text, false);
			if (allowCommandMessageLocally) {
				modCommandAddStreamHighlight(user, text, MsgTags.EMPTY);
			}
		} else {
			maingui.printLine("# Message not sent to prevent ban: " + text);
		}
	}

	/**
	 * Checks if the given channel should be open.
	 * 
	 * @param channel The channel name
	 * @return 
	 */
	public boolean isChannelOpen(String channel) {
		return twitchconnection.isChannelOpen(channel);
	}

	public boolean isUserlistLoaded(String channel) {
		return twitchconnection.isUserlistLoaded(channel);
	}

	public String getHostedChannel(String channel) {
		return twitchconnection.getChannelState(channel).getHosting();
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


		switch(command)
		{

		//---------------
		// Connection/IRC
		//---------------
		case "quit":
			twitchconnection.quit();
			break;
		case "server":
			commandServer(parameter);
			break;
		case "reconnect":
			commandReconnect();
			break;
		case "connection":
			maingui.printLine(room, twitchconnection.getConnectionInfo());
			break;
		case "join":
			commandJoinChannel(parameter);
			break;
		case "part":
			commandJoinChannel(parameter);
			break;
		case "joinhosted":
			String hostedChan = getHostedChannel(channel);
			if (hostedChan == null) {
				maingui.printLine("No channel is currently being hosted.");
			} else {
				joinChannel(hostedChan);
			}
			break;
		case "raw":
			if (parameter != null) {
				twitchconnection.sendRaw(parameter);
			}
			break;
		case "me":
			commandActionMessage(channel, parameter);
			break;
		case "msg":
			commandCustomMessage(parameter);
			break;
		case "w":
			whispermanager.whisperCommand(parameter, false);
			break;
		case "changetoken":
			maingui.changeToken(parameter);
			break;

			//------------
			// System/Util
			//------------
		case "dir":
			maingui.printSystem("Settings directory: '"+Chatty.getUserDataDirectory()+"'");
			break;
		case "wdir":
			maingui.printSystem("Working directory: '"+Chatty.getWorkingDirectory()+"'");
			break;
		case "opendir":
			MiscUtil.openFolder(new File(Chatty.getUserDataDirectory()), maingui);
			break;
		case "openwdir":
			MiscUtil.openFolder(new File(Chatty.getWorkingDirectory()), maingui);
			break;
		case "showbackupdir":
			maingui.printSystem("Backup directory: "+Chatty.getBackupDirectory());
			break;
		case "openbackupdir":
			MiscUtil.openFolder(new File(Chatty.getBackupDirectory()), maingui);
			break;

		case "showtempdir" : 
			maingui.printSystem("System Temp directory: "+Chatty.getTempDirectory());
			break;
		case "opentempdir":
			MiscUtil.openFolder(new File(Chatty.getTempDirectory()), maingui);
			break;
		case "showdebugdir":
			maingui.printSystem("Debug Log Directory: "+Chatty.getDebugLogDirectory());
			break;
		case "opendebugdir":
			maingui.printSystem("Debug Log Directory: "+Chatty.getDebugLogDirectory());
			break;
		case "showlogdir":
			if (chatLog.getPath() != null) {
				maingui.printSystem("Chat Log Directory: "+chatLog.getPath().toAbsolutePath().toString());
			}
			else {
				maingui.printSystem("Invalid Chat Log Directory");
			}
			break;
		case "openlogdir":
			if (chatLog.getPath() != null) {
				MiscUtil.openFolder(chatLog.getPath().toAbsolutePath().toFile(), maingui);
			}
			else {
				maingui.printSystem("Invalid Chat Log Directory");
			}
			break;
		case "showjavadir":
			maingui.printSystem("JRE directory: "+System.getProperty("java.home"));
			break;
		case "openjavdir":
			maingui.printSystem("JRE directory: "+System.getProperty("java.home"));
			break;
		case "showfallbackfontdir":
			Path path = Paths.get(System.getProperty("java.home"), "lib", "fonts", "fallback");
			maingui.printSystem("Fallback font directory (may not exist yet): "+path);
			break;
		case "openfallbackfontdir":
			path = Paths.get(System.getProperty("java.home"), "lib", "fonts", "fallback");
			if (Files.exists(path)) {
				MiscUtil.openFolder(path.toFile(), maingui);
			} else {
				path = path.getParent();
				maingui.showPopupMessage("Fallback font folder does not exist. Create a folder called 'fallback' in '"+path+"'.");
				MiscUtil.openFolder(path.toFile(), maingui);
			}
			break;
		case "copy":
			MiscUtil.copyToClipboard(parameter);
			break;
		case "releaseinfo":
			maingui.openReleaseInfo();
			break;
		case "echo" :
			if (parameter != null) {
				maingui.printLine(room, parameter);
			} else {
				maingui.printLine(room, "Invalid parameters: /echo <message>");
			}
			break;
		case "echoall":
			if (parameter != null) {
				maingui.printLineAll(parameter);
			} else {
				maingui.printLine("Invalid parameters: /echoall <message>");
			}
			break;
		case "uptime":
			maingui.printSystem("Chatty has been running for "+Chatty.uptime());
			break;
		case "appinfo" :
			maingui.printSystem(LogUtil.getAppInfo()+" [Connection] "+twitchconnection.getConnectionInfo());
			break;

			//-----------------------
			// Settings/Customization
			//-----------------------

		case "set" :
			maingui.printSystem(settings.setTextual(parameter));
			break;
		case "get" : 
			maingui.printSystem(settings.getTextual(parameter));
			break;
		case "clearsetting":
			maingui.printSystem(settings.clearTextual(parameter));
			break;
		case "reset" :
			maingui.printSystem(settings.resetTextual(parameter));
			break;
		case "add" :
			maingui.printSystem(settings.addTextual(parameter));
			break;
		case "remove" :
			maingui.printSystem(settings.removeTextual(parameter));
			break;
		case "setcolor":
			if (parameter != null) {
				maingui.setColor(parameter);
			}
			break;
		case "setname" :
			maingui.printLine(customNames.commandSetCustomName(parameter));
			break;
		case "resetname" :
			maingui.printLine(customNames.commandResetCustomname(parameter));
			break;
		case "customcompletion":
			commandCustomCompletion(parameter);
			break;
		case "user": case "ab":
			maingui.printSystem("[Addressbook] "
					+addressbook.command(parameter != null ? parameter : ""));
			break;
		case "abimport":
			maingui.printSystem("[Addressbook] Importing from file..");
			addressbook.importFromFile();
			break;

			//-------
			// Ignore
			//-------
		case "ignore":
			commandSetIgnored(parameter, null, true);
			break;
		case "unignore":
			commandSetIgnored(parameter, null, false);
			break;
		case "ignorechat":
			commandSetIgnored(parameter, "chat", true);
			break;
		case "unignorechat":
			commandSetIgnored(parameter, "chat", false);
			break;
		case "ignorewhisper":
			commandSetIgnored(parameter, "whisper", true);
			break;
		case "unignorewhisper":
			commandSetIgnored(parameter, "whisper", false);
			break;
			//--------------
			// Emotes/Images
			//--------------
		case "myemotes":
			commandMyEmotes();
			break;
		case "emoteinfo":
			maingui.printSystem(maingui.emoticons.getEmoteInfo(parameter));
			break;
		case "ffz":
			if (parameter != null && parameter.startsWith("following")) {
				commandFFZFollowing(room.getOwnerChannel(), parameter);
			} else {
				commandFFZ(room.getOwnerChannel());
			}
			break;
		case "ffzglobal":
			commandFFZ(null);
			break;
		case "ffzws":
			maingui.printSystem("[FFZ-WS] Status: "+frankerFaceZ.getWsStatus());
			break;
		case "pubsubstatus":
			maingui.printSystem("[PubSub] Status: "+pubsub.getStatus());
			break;
		case "refresh":
			commandRefresh(room.getOwnerChannel(), parameter);
			break;
		case "clearimagecache":
			maingui.printLine("Clearing image cache (this can take a few seconds)");
			int result = ImageCache.clearCache(null);
			if (result == -1) {
				maingui.printLine("Failed clearing image cache.");
			} else {
				maingui.printLine(String.format("Deleted %d image cache files",
						result));
			}
			break;
		case "clearemotecache":
			maingui.printLine("Clearing Emoticon image cache for type "+parameter+".");
			result = ImageCache.clearCache("emote_"+parameter);
			if (result == -1) {
				maingui.printLine("Failed clearing image cache.");
			} else {
				maingui.printLine(String.format("Deleted %d image cache files",
						result));
			}
			break;
			//------
			// Other
			//------

		case "follow":
			commandFollow(channel, parameter);
			break;
		case "unfollow":
			commandUnfollow(channel, parameter);
			break;
		case "favorite":
			Favorite favoriteresult;
			if (parameter == null) {
				favoriteresult = channelFavorites.addFavorite(channel);
			} else {
				favoriteresult = channelFavorites.addFavorite(parameter);
			}
			if (favoriteresult != null) {
				maingui.printSystem("Added '"+favoriteresult+"' to favorites");
			} else {
				maingui.printSystem("Failed adding favorite");
			}
			break;
		case "unfavorite":
			if (parameter == null) {
				favoriteresult = channelFavorites.removeFavorite(channel);
			} else {
				favoriteresult = channelFavorites.removeFavorite(parameter);
			}
			if (favoriteresult != null) {
				maingui.printSystem("Removed '"+favoriteresult+"' from favorites");
			} else {
				maingui.printSystem("Failed removing favorite");
			}
			break;
		case "automod_approve":
			autoModCommandHelper.approve(channel, parameter);
			break;
		case "automod_deny":
			autoModCommandHelper.deny(channel, parameter);
			break;
		case "marker":
			commandAddStreamMarker(room, parameter);
			break;
		case "addstreamhighlight":
			commandAddStreamHighlight(room, parameter);
			break;
		case "openstreamhighlights":
			commandOpenStreamHighlights(room);
			break;
		case "testnotification":
			if (parameter == null) {
				parameter = "";
			}
			String[] split = parameter.split("\\|\\|", 2);
			if (split.length == 2) {
				maingui.showTestNotification(null, split[0], split[1]);
			}
			else {
				maingui.showTestNotification(parameter, null, null);
			}
			break;
		case "clearchat":
			maingui.clearChat();
			break;
		case "resortuserlist":
			maingui.resortUsers(room);
			break;
		case "proc":
			maingui.printSystem("[Proc] "+ProcessManager.command(parameter));
			break;
		case "chain":
			List<String> commands = Helper.getChainedCommands(parameter);
			if (commands.isEmpty()) {
				maingui.printSystem("No valid commands");
			}
			for (String chainedCommand : commands) {
				textInput(room, chainedCommand, parameters);
			}
			break;
			// Has to be tested last, so regular commands with the same name take
			// precedence
		case "debug":
			split = parameter.split(" ", 2);
			String actualCommand = split[0];
			String actualParamter = null;
			if (split.length == 2) {
				actualParamter = split[1];
			}
			testCommands(room, actualCommand, actualParamter);
			break;
		default :
			break;
		}
		if (twitchconnection.command(channel, command, parameter, null)) {
			// Already done if true
		}

		else if (maingui.commandGui(channel, command, parameter)) {
			// Already done if true :P
		}


		else if (customCommands.containsCommand(command, room)) {
			customCommand(room, command, parameters);
		}

		else if (Chatty.DEBUG || settings.getBoolean("debugCommands")) {
			testCommands(room, command, parameter);
		}
		//----------------------

		else {
			maingui.printLine(Language.getString("chat.unknownCommand", command));
			return false;
		}
		return true;
	}

	private void testCommands(Room room, String command, String parameter) {
		String channel = room.getChannel();

		switch(channel)
		{
		case "addchans":
			String[] splitSpace = parameter.split(" ");
			String[] split2 = splitSpace[0].split(",");
			for (String chan : split2) {
				maingui.printLine(twitchconnection.getUser(chan, "test").getRoom(), "test");
			}
			break;
		case "settestuser":
			String[] split = parameter.split(" ");
			createTestUser(split[0], split[1]);
			break;
		case "getemoteset":
			maingui.printLine(maingui.emoticons.getEmoticonsBySet(parameter).toString());
			break;
		case "testcolor":
			testUser.setColor(parameter);
			break;
		case "testupdatenotification":
			maingui.setUpdateAvailable("[test]", null);
			break;
		case "testnewevent":
			maingui.setSystemEventCount(Integer.valueOf(parameter));
			break;
		case "addevent":
			split = parameter.split(" ", 3);
			EventLog.addSystemEvent(split[0], split[1], split[2]);
			break;
		case "removechan":
			maingui.removeChannel(parameter);
			break;
		case "tt":
			split = parameter.split(" ", 3);
			int repeats = Integer.parseInt(split[0]);
			int delay = Integer.parseInt(split[1]);
			String c = split[2];
			TestTimer.testTimer(this, room, c, repeats, delay);
			break;
		case "bantest":
			int duration = -1;
			String reason = "";
			if (parameter != null) {
				split = parameter.split(" ", 2);
				duration = Integer.parseInt(split[0]);
				if (split.length > 1) {
					reason = split[1];
				}
			}
			maingui.userBanned(testUser, duration, reason, null);
			break;
		case "ban":
			split = parameter.split(" ", 3);
			duration = -1;
			if (split.length > 1) {
				duration = Integer.parseInt(split[1]);
			}
			reason = "";
			if (split.length > 2) {
				reason = split[2];
			}
			maingui.userBanned(twitchconnection.getUser(channel, split[0]), duration, reason, null);
			break;
			// case "usertest":
			//		System.out.println(users.getChannelsAndUsersByUserName(parameter));
			//      break;
			// case "insert2":
			//       g.printLine("\u0E07");
			//       break;

		case "userjoined":
			twitchconnection.userJoined("#test", parameter);
			break;
		case "echomessage":
			String[] parts = parameter.split(" ");
			//          g.printMessage(parts[0], testUser, parts[1], false, null, 0);
			break;
		case "loadffz":
			frankerFaceZ.requestEmotes(parameter, true);
			break;
		case "testtw":
			maingui.showTokenWarning();
			break;
		case "tsonline":
			testStreamInfo.set(parameter, "Game", 123, -1, StreamType.LIVE);
			maingui.addStreamInfo(testStreamInfo);
			break;
		case "tsoffline":
			testStreamInfo.setOffline();
			maingui.addStreamInfo(testStreamInfo);
			break;
		case "testspam":
			maingui.printLine("test" + spamProtection.getAllowance() + spamProtection.tryMessage());
			break;
		case "spamprotectioninfo":
			maingui.printSystem("Spam Protection: "+spamProtection);
			break;
		case "tsv":
			testStreamInfo.set("Title", "Game", Integer.parseInt(parameter), -1, StreamType.LIVE);
			break;
		case "tsvs":
			System.out.println(testStreamInfo.getViewerStats(true));
			break;
		case "tsaoff":
			StreamInfo info = twitchapi.getStreamInfo(maingui.getActiveStream(), null);
			info.setOffline();
			break;
		case "tsaon":
			info = twitchapi.getStreamInfo(maingui.getActiveStream(), null);
			info.set("Test", "Game", 12, System.currentTimeMillis() - 1000, StreamType.LIVE);
			break;
		case "tss":
			info = twitchapi.getStreamInfo(parameter, null);
			info.set("Test", "Game", 12, System.currentTimeMillis() - 1000, StreamType.LIVE);
			break;
		case "tston":
			int viewers = 12;
			try {
				viewers = Integer.parseInt(parameter);
			} catch (NumberFormatException ex) { }
			info = twitchapi.getStreamInfo("tduva", null);
			info.set("Test 2", "Game", viewers, System.currentTimeMillis() - 1000, StreamType.LIVE);
			break;
		case "refreshstreams":
			twitchapi.manualRefreshStreams();
			break;
		case "usericonsinfo":
			usericonManager.debug();
			break;
		case "userlisttest":
			//          g.printMessage("test1", testUser, "short message", false, null, 0);
			//          g.printMessage("test2", testUser, "short message2", false, null, 0);
			//          g.printCompact("test3", "MOD", testUser);
			//          g.printCompact("test3", "MOD", testUser);
			//          g.printCompact("test3", "MOD", testUser);
			//          g.printCompact("test3", "MOD", testUser);
			//          g.printCompact("test3", "MOD", testUser);
			//          g.printCompact("test3", "MOD", testUser);
			//          g.printCompact("test3", "MOD", testUser);
			//          g.printMessage("test3", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
			//                  + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false, null, 0);
			//          g.printMessage("test3", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
			//                  + "fjwfjfwjefjwefjwoeifjwefiowejfef wefjoiwefj", false, null, 0);
			//          g.printMessage("test3", testUser, "longer wef wef wefwe fwe ewfwe fwef wwefwef"
			//                  + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false, null, 0);
			//          g.printCompact("test4", "MOD", testUser);
			//          g.printCompact("test5", "MOD", testUser);
			//          g.printCompact("test6", "MOD", testUser);
			//          g.printCompact("test7", "MOD", testUser);
			//          g.printCompact("test8", "MOD", testUser);
			//          g.printCompact("test9", "MOD", testUser);
			//          g.printMessage("test10", testUser, "longer message abc hmm fwef wef wef wefwe fwe ewfwe fwef wwefwef"
			//                  + "fjwfjfwjefjwefjwef wfejfkwlefjwoefjwf wfjwoeifjwefiowejfef wefjoiwefj", false, null, 0);
			break;
		case "requestfollower":
			twitchapi.getFollowers(parameter);
			break;
		case "simulate2":
			twitchconnection.simulate(parameter);
			break;
		case "simulate":
			if (parameter.equals("bits")) {
				parameter = "bits "+maingui.emoticons.getCheerEmotesString(null);
			} else if (parameter.equals("bitslocal")) {
				parameter = "bits "+maingui.emoticons.getCheerEmotesString(Helper.toStream(channel));
			} else if (parameter.startsWith("bits ")) {
				parameter = "bits "+parameter.substring("bits ".length());
			} else if (parameter.startsWith("emoji ")) {
				int num = Integer.parseInt(parameter.substring("emoji ".length()));
				StringBuilder b = new StringBuilder();
				for (Emoticon emote : maingui.emoticons.getEmoji()) {
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
					twitchconnection.simulate(raw);
				}
				return;
			}
			String raw = RawMessageTest.simulateIRC(channel, parameter, twitchconnection.getUsername());
			if (raw != null) {
				twitchconnection.simulate(raw);
			}
			break;
		case "lb":
			split = parameter.split("&");
			String message = "";
			for (int i=0;i<split.length;i++) {
				if (!message.isEmpty()) {
					message += "\r";
				}
				message += split[i];
			}
			sendMessage(channel, message);
			break;
		case "c1":
			sendMessage(channel, (char)1+parameter);
			break;
		case "gc":
			Runtime.getRuntime().gc();
			LogUtil.logMemoryUsage();
			break;
		case "wsconnect":
			frankerFaceZ.connectWs();
			break;
		case "wsdisconnect":
			frankerFaceZ.disconnectWs();
			break;
		case "posconnect":
			//          pubsub.connect();
			break;
		case "psdisconnect":
			pubsub.disconnect();
			break;
		case "psreconnect":
			pubsub.reconnect();
			break;
		case "modaction":
			String by = "Blahfasel";
			String action = "timeout";
			List<String> args = new ArrayList<>();
			if (parameter != null && !parameter.isEmpty()) {
				split = parameter.split(" ");
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
			break;
		case "automod":
			args = new ArrayList<>();
			args.add("tduva");
			if (parameter != null) {
				if (parameter.contains(",")) {
					split = parameter.split(",", 2);
					args.add(split[1]);
					args.add(split[0]);
				} else {
					args.add(parameter);
				}
			} else {
				args.add("fuck and stuff like that, rather long message and whatnot Kappa b "+Debugging.count(channel));
			}
			maingui.printModerationAction(new ModeratorActionData("", "", "", room.getStream(), "twitchbot_rejected", args, "twitchbot", "TEST"), false);
			break;
		case "automod2":
			args = new ArrayList<>();
			args.add("tduva");
			data = new ModeratorActionData("", "", "", room.getStream(), "denied_automod_message", args, "asdas", "TEST");
			maingui.printModerationAction(data, false);
			break;
		case "repeat":
			split = parameter.split(" ", 2);
			int count = Integer.parseInt(split[0]);
			for (int i=0;i<count;i++) {
				commandInput(room, "/"+split[1]);
			}
			break;
		case "chain":
			split = parameter.split("\\|");
			for (String part : split) {
				System.out.println("Command: "+part.trim());
				commandInput(room, "/"+part.trim());
			}
			break;
		case "modactiontest3":
			args = new ArrayList<>();
			args.add("tduva");
			maingui.printModerationAction(new ModeratorActionData("", "", "", "tduvatest", "approved_twitchbot_message", args, "tduvatest", "TEST"+Math.random()), false);
			break;
		case "loadsoferros":
			for (int i=0;i<10000;i++) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Helper.unhandledException();
					}
				});
			}
			break;
		case "getuserid":
			if (parameter == null) {
				maingui.printSystem("Parameter required.");
			} else {
				twitchapi.getUserIdAsap(r -> {
					String result = r.getData().toString();
					if (r.hasError()) {
						result += " Error: "+r.getError();
					}
					maingui.printSystem(result);
				}, parameter.split("[ ,]"));
			}
			break;
		case "getuserids2":
			twitchapi.getUserIDsTest2(parameter);
			break;
		case "getuserid3":
			twitchapi.getUserIDsTest3(parameter);
			break;
		case "clearoldsetups":
			Stuff.init();
			Stuff.clearOldSetups();
			break;
		case "tags":
			Set<StreamTag> tags = new HashSet<>();
			tags.add(new StreamTag("id", "name", "summary", false));
			if (parameter != null) {
				tags.add(new StreamTag(parameter, "name2", "summary", false));
			}
			twitchapi.getInvalidStreamTags(tags, (t,e) -> {
				System.out.println(t+" "+e);
			});
			break;
		case "-":
			maingui.printSystem(Debugging.command(parameter));
			break;
		case "connection":
			twitchconnection.debugConnection();
			break;
		case "clearoldcachefiles":
			ImageCache.deleteExpiredFiles();
			break;
		} 
	}

	private void anonCustomCommand(Room room, String text, Parameters parameters) {
		CustomCommand command = CustomCommand.parse(text);
		if (parameters == null) {
			parameters = Parameters.create(null);
		}
		anonCustomCommand(room, command, parameters);
	}

	public void anonCustomCommand(Room room, CustomCommand command, Parameters parameters) {
		if (command.hasError()) {
			maingui.printLine("Parse error: "+command.getSingleLineError());
			return;
		}
		if (room == null) {
			maingui.printLine("Custom command: Not on a channel");
			return;
		}
		String result = customCommands.command(command, parameters, room);
		if (result == null) {
			maingui.printLine("Custom command: Insufficient parameters/data");
		} else if (result.isEmpty()) {
			maingui.printLine("Custom command: No action specified");
		} else {
			textInput(room, result, parameters);
		}
	}

	public void customCommand(Room room, String command, Parameters parameters) {
		if (room == null) {
			maingui.printLine("Custom command: Not on a channel");
			return;
		}
		if (!customCommands.containsCommand(command, room)) {
			maingui.printLine("Custom command not found: "+command);
			return;
		}
		String result = customCommands.command(command, parameters, room);
		if (result == null) {
			maingui.printLine("Custom command '"+command+"': Insufficient parameters/data");
		} else if (result.isEmpty()) {
			// This shouldn't actually happen if edited through the settings,
			// which should trim() out whitespace, so that the command won't
			// have a result if it's empty and thus won't be added as a command.
			// Although it can also happen if the command just contains a \
			// (which is interpreted as an escape character).
			maingui.printLine("Custom command '"+command+"': No action specified");
		} else {
			// Check what command is called in the result of this command
			String[] resultSplit = result.split(" ", 2);
			String resultCommand = resultSplit[0];
			if (resultCommand.startsWith("/")
					&& customCommands.containsCommand(resultCommand.substring(1), room)) {
				maingui.printLine("Custom command '"+command+"': Calling another custom "
						+ "command ('"+resultCommand.substring(1)+"') is not allowed");
			} else {
				textInput(room, result, parameters);
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
					maingui.printSystem(String.format("Ignore: '%s' now ignored %s",
							name, message));
				} else {
					maingui.printSystem(String.format("Ignore: '%s' no longer ignored %s",
							name, message));
				}
			} else {
				if (ignore) {
					maingui.printSystem(String.format("Ignore: '%s' already ignored %s",
							name, message));
				} else {
					maingui.printSystem(String.format("Ignore: '%s' not ignored %s",
							name, message));
				}
			}
		} else {
			maingui.printSystem("Ignore: Invalid name");
		}
	}

	private void commandServer(String parameter) {
		if (parameter == null) {
			maingui.printLine("Usage: /server <address>[:port]");
			return;
		}
		String[] split = parameter.split(":");
		if (split.length == 1) {
			prepareConnectionAnyChannel(split[0], getPorts());
		} else if (split.length == 2) {
			prepareConnectionAnyChannel(split[0], split[1]);
		} else {
			maingui.printLine("Invalid format. Usage: /server <address>[:port]");
		}
	}

	/**
	 * Command to join channel entered.
	 * 
	 * @param channel 
	 */
	public void commandJoinChannel(String channelString) {
		if (channelString == null) {
			maingui.printLine("A channel to join needs to be specified.");
		} else {
			String[] channelList = channelString.split(" ");
			for (String channel: channelList)
			{
				channel = StringUtil.toLowerCase(channel.trim());
				twitchconnection.joinChannel(channel);
			}

		}
	}

	/**
	 * Command to part channel entered.
	 * 
	 * @param channel 
	 */
	private void commandPartChannel(String channel) {
		if (channel == null || channel.isEmpty()) {
			maingui.printLine("No channel to leave.");
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
			maingui.printLine("Usage: /me <message>");
		}
	}

	public void sendActionMessage(String channel, String message) {
		if (twitchconnection.onChannel(channel, true)) {
			if (twitchconnection.sendSpamProtectedMessage(channel, message, true)) {
				maingui.printMessage(twitchconnection.localUserJoined(channel), message, true);
			} else {
				maingui.printLine("# Action Message not sent to prevent ban: " + message);
			}
		}
	}

	private void commandCustomMessage(String parameter) {
		if (parameter != null && !parameter.isEmpty()) {
			String[] split = parameter.split(" ", 2);
			if (split.length == 2) {
				String to = split[0];
				String message = split[1];
				twitchconnection.sendSpamProtectedMessage(to, message, false);
				maingui.printLine(String.format("-> %s: %s", to, message));
				return;
			}
		}
		maingui.printSystem("Invalid parameters.");
	}

	public void commandReconnect() {
		if (twitchconnection.disconnect()) {
			twitchconnection.reconnect();
		} else {
			maingui.printLine("Could not reconnect.");
		}
	}

	private void commandCustomCompletion(String parameter) {
		String usage = "Usage: /customCompletion <add/set/remove> <item> <value>";
		if (parameter == null) {
			maingui.printLine(usage);
			return;
		}
		String[] split = parameter.split(" ", 3);
		if (split.length < 2) {
			maingui.printLine(usage);
		} else {
			String type = split[0];
			String key = split[1];
			if (type.equals("add") || type.equals("set")) {
				if (split.length < 3) {
					maingui.printLine("Invalid number of parameters for adding completion item.");
				} else {
					String value = split[2];
					if (!type.equals("set") && settings.mapGet("customCompletion", key) != null) {
						maingui.printLine("Completion item '"+key+"' already exists, use '/customCompletion set <key> <value>' to overwrite");
					} else {
						settings.mapPut("customCompletion", key, value);
						maingui.printLine("Set custom completion '"+key+"' to '"+value+"'");
					}
				}
			} else if (type.equals("remove")) {
				settings.mapRemove("customCompletion", key);
				maingui.printLine("Removed '"+key+"' from custom completion");
			} else {
				maingui.printLine(usage);
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
		String user = settings.getString("username");
		String target = Helper.toStream(channel);
		if (parameter != null && !parameter.isEmpty()) {
			target = Helper.toStream(parameter.trim());
		}
		if (!Helper.isValidStream(target)) {
			maingui.printSystem("No valid channel to follow.");
			return;
		}
		if (!Helper.isValidStream(user)) {
			maingui.printSystem("No valid username.");
			return;
		}
		twitchapi.followChannel(user, target);
	}

	public void commandUnfollow(String channel, String parameter) {
		String user = settings.getString("username");
		String target = Helper.toStream(channel);
		if (parameter != null && !parameter.isEmpty()) {
			target = Helper.toStream(parameter.trim());
		}
		if (!Helper.isValidStream(target)) {
			maingui.printSystem("No valid channel to unfollow.");
			return;
		}
		if (!Helper.isValidStream(user)) {
			maingui.printSystem("No valid username.");
			return;
		}
		twitchapi.unfollowChannel(user, target);
	}

	public void commandAddStreamHighlight(Room room, String parameter) {
		maingui.printLine(room, streamHighlights.addHighlight(room.getOwnerChannel(), parameter, null));
	}

	public void commandOpenStreamHighlights(Room room) {
		maingui.printLine(room, streamHighlights.openFile());
	}

	public void modCommandAddStreamHighlight(User user, String message, MsgTags tags) {
		// Stream Highlights
		String result = streamHighlights.modCommand(user, message, tags);
		if (result != null) {
			result = user.getDisplayNick() + ": " + result;
			if (settings.getBoolean("streamHighlightChannelRespond")) {
				sendMessage(user.getChannel(), result);
			} else {
				maingui.printLine(user.getRoom(), result);
			}
		}
	}

	public void commandAddStreamMarker(Room room, String description) {
		twitchapi.createStreamMarker(room.getStream(), description, error -> {
			String info = StringUtil.aEmptyb(description, "no description", "'%s'");
			if (error == null) {
				maingui.printLine("Stream marker created ("+info+")");
			} else {
				maingui.printLine("Failed to create stream marker ("+info+"): "+error);
			}
		});
	}

	private void commandRefresh(String channel, String parameter) {
		if (!Helper.isRegularChannel(channel)) {
			channel = null;
		}
		switch(parameter)
		{
		case "emoticons":
			maingui.printLine("Refreshing emoticons.. (this can take a few seconds)");
			refreshRequests.add("emoticons");
			//Emoticons.clearCache(Emoticon.Type.TWITCH);
			twitchapi.refreshEmotes();
			break;
		case "bits":
			maingui.printLine("Refreshing bits..");
			refreshRequests.add("bits");
			twitchapi.getCheers(channel, true);
			break;
		case "badges":
			if (!Helper.isValidChannel(channel)) {
				maingui.printLine("Must be on a channel to use this.");
			} else {
				maingui.printLine("Refreshing badges for " + channel + "..");
				refreshRequests.add("badges");
				twitchapi.getGlobalBadges(true);
				twitchapi.getRoomBadges(Helper.toStream(channel), true);
				OtherBadges.requestBadges(r -> usericonManager.setThirdPartyIcons(r), true);
			}
			break;
		case "ffz":
			if (channel == null || channel.isEmpty()) {
				maingui.printLine("Must be on a channel to use this.");
			} else {
				maingui.printLine("Refreshing FFZ emotes for "+channel+"..");
				refreshRequests.add("ffz");
				frankerFaceZ.requestEmotes(channel, true);
			}
			break;
		case "ffzglobal":
			maingui.printLine("Refreshing global FFZ emotes..");
			refreshRequests.add("ffzglobal");
			frankerFaceZ.requestGlobalEmotes(true);
			break;
		case "bttvemotes":
			maingui.printLine("Refreshing BTTV emotes..");
			refreshRequests.add("bttvemotes");
			bttvEmotes.requestEmotes("$global$", true);
			bttvEmotes.requestEmotes(channel, true);
			break;
		case "" :
			maingui.printLine("Usage: /refresh <type> (see help)");
			break;
		default :
			maingui.printLine("Usage: /refresh <type> (invalid type, see help)");
		}
	}

	public User getSpecialUser() {
		return twitchconnection.getSpecialUser();
	}

	public Set<String> getEmotesets() {
		return emotesetManager.getEmotesets();
	}

	/**
	 * Outputs the emotesets for the local user. This might not work correctly
	 * if the user is changed or the emotesets change during the session.
	 */
	private void commandMyEmotes() {
		Set<String> emotesets = getEmotesets();
		if (emotesets.isEmpty()) {
			maingui.printLine("No subscriber emotes found. (Only works if you joined"
					+ " any channel before.)");
		} else {
			StringBuilder stringbuilder = new StringBuilder("Your subemotes: ");
			String sep = "";
			for (String emoteset : emotesets) {
				stringbuilder.append(sep);
				if (Emoticons.isTurboEmoteset(emoteset)) {
					stringbuilder.append("Turbo/Prime emotes");
				} else {
					String sep2 = "";
					for (Emoticon emote : maingui.emoticons.getEmoticonsBySet(emoteset)) {
						stringbuilder.append(sep2);
						stringbuilder.append(emote.code);
						sep2 = ", ";
					}
				}
				sep = " / ";
			}
			maingui.printLine(stringbuilder.toString());
		}
	}

	private void commandFFZ(String channel) {
		Set<Emoticon> output;
		StringBuilder stringbuilder = new StringBuilder();
		if (channel == null) {
			stringbuilder.append("Global FFZ emotes: ");
			output = Emoticons.filterByType(maingui.emoticons.getGlobalTwitchEmotes(), Emoticon.Type.FFZ);
		} else {
			stringbuilder.append("This channel's FFZ emotes: ");
			Set<Emoticon> emotes = maingui.emoticons.getEmoticonsByStream(Helper.toStream(channel));
			output = Emoticons.filterByType(emotes, Emoticon.Type.FFZ);
		}
		if (output.isEmpty()) {
			stringbuilder.append("None found.");
		}
		String sep = "";
		for (Emoticon emote : output) {
			stringbuilder.append(sep);
			stringbuilder.append(emote.code);
			sep = ", ";
		}
		maingui.printLine(roomManager.getRoom(channel), stringbuilder.toString());
	}

	private void commandFFZFollowing(String channel, String parameter) {
		String stream = Helper.toStream(channel);
		if (stream == null) {
			maingui.printSystem("FFZ: No valid channel.");
		} else if (!twitchconnection.isRegistered()) {
			maingui.printSystem("FFZ: You have to be connected to use this command.");
		} else {
			if (!stream.equals(twitchconnection.getUsername())) {
				maingui.printSystem("FFZ: You may only be able to run this command on your own channel.");
			}
			parameter = parameter.substring("following".length()).trim();
			frankerFaceZ.setFollowing(twitchconnection.getUsername(), stream, parameter);
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
			if (maingui == null) {
				cachedDebugMessages.add("["+DateTime.currentTimeExact()+"] "+line);
			} else {
				if (!cachedDebugMessages.isEmpty()) {
					maingui.printDebug("[Start of cached messages]");
					for (String cachedLine : cachedDebugMessages) {
						maingui.printDebug(cachedLine);
					}
					maingui.printDebug("[End of cached messages]");
					// No longer used
					cachedDebugMessages.clear();
				}
				maingui.printDebug(line);
			}
		}
	}

	public void debugFFZ(String line) {
		if (shuttingDown || maingui == null) {
			return;
		}
		maingui.printDebugFFZ(line);
	}

	public void debugPubSub(String line) {
		if (shuttingDown || maingui == null) {
			return;
		}
		maingui.printDebugPubSub(line);
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
			if (maingui == null) {
				cachedWarningMessages.add(line);
			} else {
				if (!cachedWarningMessages.isEmpty()) {
					for (String cachedLine : cachedWarningMessages) {
						maingui.printLine(cachedLine);
					}
					cachedWarningMessages.clear();
				}
				if (line != null) {
					maingui.printLine(line);
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
					if (data.stream != null) {
						String channel = Helper.toChannel(data.stream);
						maingui.printModerationAction(data, data.created_by.equals(twitchconnection.getUsername()));
						chatLog.modAction(data);

						User modUser = twitchconnection.getUser(channel, data.created_by);
						modUser.addModAction(data);
						maingui.updateUserinfo(modUser);

						String bannedUsername = ModLogInfo.getBannedUsername(data);
						if (bannedUsername != null) {
							// If this is actually a ban, add info to banned user
							User bannedUser = twitchconnection.getUser(channel, bannedUsername);
							bannedUser.addBanInfo(data);
							maingui.updateUserinfo(bannedUser);
						}
						String unbannedUsername = ModLogInfo.getUnbannedUsername(data);
						if (unbannedUsername != null) {
							// Add info to unbanned user
							User unbannedUser = twitchconnection.getUser(channel, unbannedUsername);
							int type = User.UnbanMessage.getType(data.moderation_action);
							unbannedUser.addUnban(type, data.created_by);
							maingui.updateUserinfo(unbannedUser);
						}
					}
				}
				else if (message.data instanceof UserinfoMessageData) {
					UserinfoMessageData data = (UserinfoMessageData) message.data;
					User user = twitchconnection.getUser(Helper.toChannel(data.stream), data.username);
					maingui.printPointsNotice(user, data.msg, data.attached_msg, MsgTags.create("chatty-source", "pubsub"));
				}
			}
		}

		@Override
		public void info(String info) {
			maingui.printDebugPubSub(info);
		}

	}

	/**
	 * Redirects request results from the API.
	 */
	private class TwitchApiResults implements TwitchApiResultListener {

		@Override
		public void receivedEmoticons(EmoticonUpdate update) {
			maingui.updateEmoticons(update);

			// After adding emotes, update sets
			if (update.source == EmoticonUpdate.Source.USER_EMOTES
					&& update.setsToRemove != null) {
				// setsToRemove contains all sets (only for USER_EMOTES)
				// This may also update EmoteDialog etc.
				emotesetManager.setEmotesets(update.setsToRemove);
			}

			// Other stuff
			if (refreshRequests.contains("emoticons")) {
				maingui.printLine("Emoticons list updated.");
				refreshRequests.remove("emoticons");
			}
		}

		@Override
		public void tokenVerified(String token, TokenInfo tokenInfo) {
			maingui.tokenVerified(token, tokenInfo);
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
			maingui.setChannelInfo(stream, info, result);
		}

		@Override
		public void putChannelInfoResult(RequestResultCode result) {
			maingui.putChannelInfoResult(result);
		}

		@Override
		public void accessDenied() {
			twitchapi.checkToken();
		}

		@Override
		public void receivedUsericons(List<Usericon> icons) {
			usericonManager.addDefaultIcons(icons);
			if (refreshRequests.contains("badges2")) {
				maingui.printLine("Badges2 updated.");
				refreshRequests.remove("badges2");
			}
			if (refreshRequests.contains("badges")) {
				maingui.printLine("Badges updated.");
				refreshRequests.remove("badges");
			}
		}

		@Override
		public void receivedFollowers(FollowerInfo followerInfo) {
			maingui.setFollowerInfo(followerInfo);
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
			StreamInfo streamInfo = twitchapi.getStreamInfo(followerInfo.stream, null);
			boolean changed = false;
			if (followerInfo.type == Follower.Type.SUBSCRIBER) {
				changed = streamInfo.setSubscriberCount(followerInfo.total);
			} else if (followerInfo.type == Follower.Type.FOLLOWER) {
				changed = streamInfo.setFollowerCount(followerInfo.total);
			}
			if (changed && streamInfo.isValid()) {
				streamStatusWriter.streamStatus(streamInfo);
			}
		}

		@Override
		public void newFollowers(FollowerInfo followerInfo) {
			maingui.newFollowers(followerInfo);
		}

		@Override
		public void receivedSubscribers(FollowerInfo info) {
			maingui.setSubscriberInfo(info);
			followerInfoNames(info);
			receivedFollowerOrSubscriberCount(info);
		}

		private void followerInfoNames(FollowerInfo info) {

		}

		@Override
		public void receivedFollower(String stream, String username, RequestResultCode result, Follower follower) {
			maingui.setFollowInfo(stream, username, result, follower);
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
				twitchconnection.disconnect();
				prepareConnectionAnyChannel(s, String.valueOf(p));
			}
			else {
				//g.printLine(channel, "An error occured requesting server info.");
			}
		}

		@Override
		public void followResult(String message) {
			maingui.printSystem(message);
		}

		@Override
		public void autoModResult(String result, String msgId) {
			maingui.autoModRequestResult(result, msgId);
			autoModCommandHelper.requestResult(result, msgId);
		}

		@Override
		public void receivedCheerEmoticons(Set<CheerEmoticon> emoticons) {
			if (refreshRequests.contains("bits")) {
				maingui.printLine("Bits received.");
				refreshRequests.remove("bits");
			}
			maingui.setCheerEmotes(emoticons);
		}


	}

	private class MyRoomUpdatedListener implements RoomManager.RoomUpdatedListener {

		@Override
		public void roomUpdated(Room room) {
			if (twitchconnection != null) {
				twitchconnection.updateRoom(room);
			}
			if (maingui != null) {
				maingui.updateRoom(room);
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
			maingui.webserverStarted();
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
			maingui.webserverStarted();
		}

		@Override
		public void webserverStopped() {
			webserver = null;
		}

		@Override
		public void webserverError(String error) {
			maingui.webserverError(error);
			webserver = null;
		}

		@Override
		public void webserverTokenReceived(String token) {
			maingui.webserverTokenReceived(token);
		}
	};

	/**
	 * Update the logo for all current Stream Chat channels, based on already
	 * available StreamInfo.
	 */
	public void updateStreamChatLogos() {
		for (Object chanObject : settings.getList("streamChatChannels")) {
			String channel = (String) chanObject;
			updateStreamChatLogo(channel, twitchapi.getCachedStreamInfo(Helper.toStream(channel)));
		}
	}

	/**
	 * Update the Stream Chat logo for the given channel.
	 * 
	 * @param channel The channel
	 * @param info The StreamInfo to get the logo from, may be null
	 */
	public void updateStreamChatLogo(String channel, StreamInfo info) {
		if (info != null && info.getLogo() != null && settings.listContains("streamChatChannels", channel)) {
			usericonManager.updateChannelLogo(channel, info.getLogo(), settings.getString("streamChatLogos"));
		}
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
			maingui.updateState(true);
			maingui.updateChannelInfo(info);
			maingui.addStreamInfo(info);
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
				maingui.printLine("** This channel doesn't seem to exist on Twitch. "
						+ "You may not be able to join this channel, but trying"
						+ " anyways. **");
			}
			updateStreamChatLogo(channel, info);
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
					maingui.printLineByOwnerChannel(channel, "~" + newStatus + "~");
				}
				maingui.setChannelNewStatus(channel, newStatus);

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
							&& twitchconnection.onChannel(channel)
							&& (
									info.stream.equals(twitchconnection.getUsername())
									|| settings.listContains("autoUnhostStreams", info.stream)
									)) {
						twitchconnection.sendCommandMessage(channel, ".unhost", "Trying to turn off host mode.. (Auto-Unhost)");
					} else {
						maingui.printLine(roomManager.getRoom(channel), "** Still hosting another channel while streaming. **");
					}
				}
			}
			maingui.statusNotification(channel, info);
		}
	}

	/**
	 * Log viewerstats for any open channels, which can be used to log any
	 * remaining data on all channels when the program is closed.
	 */
	private void logAllViewerstats() {
		for (String channel : twitchconnection.getOpenChannels()) {
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
			ViewerStats stats = twitchapi.getStreamInfo(Helper.toStream(channel), null).getViewerStats(true);
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
		maingui.showTokenWarning();
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
			commercialResult(stream, "Can't run commercial, not on a channel.", TwitchApi.RequestResultCode.FAILED);
		}
		else {
			String channel = "#"+stream;
			if (isChannelOpen(channel)) {
				maingui.printLine(roomManager.getRoom(channel), "Trying to run "+length+"s commercial..");
			} else {
				maingui.printLine("Trying to run "+length+"s commercial.. ("+stream+")");
			}
			twitchapi.runCommercial(stream, length);
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
			maingui.printLine(roomManager.getRoom(channel), text);
		} else {
			maingui.printLine(text+" ("+stream+")");
		}
		maingui.commercialResult(stream, text, result);
	}

	/**
	 * Receive FrankerFaceZ emoticons and icons.
	 */
	private class EmoticonsListener implements FrankerFaceZListener {

		@Override
		public void channelEmoticonsReceived(EmoticonUpdate emotes) {
			maingui.updateEmoticons(emotes);
			if (refreshRequests.contains("ffz")) {
				maingui.printLine("FFZ emotes updated.");
				refreshRequests.remove("ffz");
			}
			if (refreshRequests.contains("ffzglobal")) {
				maingui.printLine("Global FFZ emotes updated.");
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

		@Override
		public void wsInfo(String info) {
			maingui.printDebugFFZ(info);
		}

		@Override
		public void authorizeUser(String code) {
			twitchconnection.sendSpamProtectedMessage("#frankerfacezauthorizer", "AUTH "+code, false);
		}

		@Override
		public void wsUserInfo(String info) {
			maingui.printSystem("FFZ: "+info);
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
		//        api.getEmotesByStreams(Helper.toStream(channel)); // Removed
	}

	private class EmoteListener implements EmoticonListener {

		@Override
		public void receivedEmoticons(Set<Emoticon> emoticons) {
			maingui.addEmoticons(emoticons);
			if (refreshRequests.contains("bttvemotes")) {
				maingui.printLine("BTTV emotes updated.");
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

	/**
	 * Only used for testing. You have to restart Chatty for the spam protection
	 * in the connectin to change.
	 * 
	 * @param value 
	 */
	public void setLinesPerSeconds(String value) {
		spamProtection.setLinesPerSeconds(value);
		twitchconnection.setSpamProtection(value);
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
		twitchconnection.disconnect();
		frankerFaceZ.disconnectWs();
		pubsub.disconnect();
		maingui.cleanUp();
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
		if (maingui != null && maingui.guiCreated) {
			maingui.saveWindowStates();
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
			Collection<String> openChans;
			if (SwingUtilities.isEventDispatchThread()) {
				openChans = maingui.getOpenChannels();
			} else {
				openChans = twitchconnection.getOpenChannels();
			}
			settings.setString("previousChannel", Helper.buildStreamsString(openChans));
			EmoticonSizeCache.saveToFile();
		}

	}

	private class Messages implements TwitchConnection.ConnectionListener {

		private void checkModLogListen(User user) {
			Debugging.println("pubsub", "%s/%s==%s/%s",
					user.hasChannelModeratorRights(),
					user.getName(),
					twitchconnection.getUsername(),
					user.getStream());
			if (user.hasChannelModeratorRights()
					&& user.getName().equals(twitchconnection.getUsername())
					&& user.getStream() != null) {
				if (settings.listContains("scopes", TokenInfo.Scope.CHAN_MOD.scope)) {
					Debugging.println("pubsub", "Listen");
					pubsub.setLocalUsername(twitchconnection.getUsername());
					pubsub.listenModLog(user.getStream(), settings.getString("token"));
				}
				else {
					EventLog.addSystemEvent("access.modlog");
				}
			}
		}

		private void checkPointsListen(User user) {
			if (settings.listContains("scopes", TokenInfo.Scope.POINTS.scope)
					&& user.getName().equals(twitchconnection.getUsername())
					&& user.getStream().equals(twitchconnection.getUsername())
					&& user.getStream() != null) {
				pubsub.listenPoints(user.getStream(), settings.getString("token"));
			}
		}

		@Override
		public void onChannelJoined(User user) {
			channelFavorites.addJoined(user.getRoom());

			maingui.printLine(user.getRoom(), Language.getString("chat.joined", user.getRoom()));
			if (user.getRoom().hasTopic()) {
				maingui.printLine(user.getRoom(), user.getRoom().getTopicText());
			}

			// Icons and FFZ/BTTV Emotes
			//api.requestChatIcons(Helper.toStream(channel), false);
			twitchapi.getGlobalBadges(false);
			String stream = user.getStream();
			if (Helper.isValidStream(stream)) {
				twitchapi.getRoomBadges(stream, false);
				twitchapi.getCheers(stream, false);
				requestChannelEmotes(stream);
				frankerFaceZ.joined(stream);
				checkModLogListen(user);
				checkPointsListen(user);
			}
		}

		@Override
		public void onChannelLeft(Room room) {
			chatLog.info(room.getFilename(), "You have left "+room.getDisplayName());
			closeChannel(room.getChannel());
		}

		@Override
		public void onJoin(User user) {
			if (settings.getBoolean("showJoinsParts") && showUserInGui(user)) {
				maingui.printCompact("JOIN", user);
			}
			maingui.userJoined(user);
			chatLog.compact(user.getRoom().getFilename(), "JOIN", user.getRegularDisplayNick());
		}

		@Override
		public void onPart(User user) {
			if (settings.getBoolean("showJoinsParts") && showUserInGui(user)) {
				maingui.printCompact("PART", user);
			}
			chatLog.compact(user.getRoom().getFilename(), "PART", user.getRegularDisplayNick());
			maingui.userLeft(user);
		}

		@Override
		public void onUserUpdated(User user) {
			if (showUserInGui(user)) {
				maingui.updateUser(user);
			}
			maingui.updateUserinfo(user);
			checkModLogListen(user);
		}

		@Override
		public void onChannelMessage(User user, String text, boolean action, MsgTags tags) {
			if (tags.isCustomReward()) {
				String rewardInfo = (String)settings.mapGet("rewards", tags.getCustomRewardId());
				String info = String.format("%s redeemed a custom reward (%s)",
						user.getDisplayNick(),
						rewardInfo != null ? rewardInfo : "unknown");
				maingui.printPointsNotice(user, info, text, tags);
			}
			else {
				maingui.printMessage(user, text, action, tags);
				if (!action) {
					addressbookCommands(user.getChannel(), user, text);
					modCommandAddStreamHighlight(user, text, tags);
				}
			}
		}

		@Override
		public void onNotice(String message) {
			maingui.printLine("[Notice] "+message);
		}

		@Override
		public void onInfo(Room room, String infoMessage, MsgTags tags) {
			maingui.printInfo(room, infoMessage, tags);
		}

		@Override
		public void onInfo(String message) {
			maingui.printLine(message);
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
				maingui.printStreamInfo(room);
			}
			maingui.printLine(room, Language.getString("chat.joining", room));
		}

		@Override
		public void onUserAdded(User user) {
			if (showUserInGui(user)) {
				maingui.addUser(user);
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
			maingui.removeUser(user);
		}

		@Override
		public void onBan(User user, long duration, String reason, String targetMsgId) {
			User localUser = twitchconnection.getLocalUser(user.getChannel());
			if (localUser != user && !localUser.hasModeratorRights()) {
				// Remove reason if not the affected user and not a mod, to be
				// consistent with other applications
				reason = "";
			}
			maingui.userBanned(user, duration, reason, targetMsgId);
			ChannelInfo channelInfo = twitchapi.getOnlyCachedChannelInfo(user.getName());
			chatLog.userBanned(user.getRoom().getFilename(), user.getRegularDisplayNick(),
					duration, reason, channelInfo);
		}

		@Override
		public void onMsgDeleted(User user, String targetMsgId, String msg) {
			User localUser = twitchconnection.getLocalUser(user.getChannel());
			if (localUser == user) {
				maingui.printLine(user.getRoom(), "Your message was deleted: "+msg);
			} else {
				maingui.msgDeleted(user, targetMsgId, msg);
			}
			chatLog.msgDeleted(user, msg);
		}

		@Override
		public void onRegistered() {
			maingui.updateHighlightSetUsername(twitchconnection.getUsername());
			//pubsub.listenModLog(c.getUsername(), settings.getString("token"));
		}

		@Override
		public void onMod(User user) {
			boolean modMessagesEnabled = settings.getBoolean("showModMessages");
			if (modMessagesEnabled && showUserInGui(user)) {
				maingui.printCompact("MOD", user);
			}
			chatLog.compact(user.getRoom().getFilename(), "MOD", user.getRegularDisplayNick());
		}

		@Override
		public void onUnmod(User user) {
			boolean modMessagesEnabled = settings.getBoolean("showModMessages");
			if (modMessagesEnabled && showUserInGui(user)) {
				maingui.printCompact("UNMOD", user);
			}
			chatLog.compact(user.getRoom().getFilename(), "UNMOD", user.getRegularDisplayNick());
		}

		@Override
		public void onDisconnect(int reason, String reasonMessage) {
			//g.clearUsers();
			if (reason == Irc.ERROR_REGISTRATION_FAILED) {
				twitchapi.checkToken();
			}
			if (reason == Irc.ERROR_CONNECTION_CLOSED) {
				pubsub.checkConnection();
			}
		}

		@Override
		public void onConnectionStateChanged(int state) {
			maingui.updateState(true);
		}

		@Override
		public void onEmotesets(Set<String> emotesets) {
			emotesetManager.setIrcEmotesets(emotesets);
		}

		@Override
		public void onConnectError(String message) {
			maingui.printLine(message);
		}

		@Override
		public void onJoinError(Set<String> toJoin, String errorChannel, TwitchConnection.JoinError error) {
			if (error == TwitchConnection.JoinError.NOT_REGISTERED) {
				String validChannels = Helper.buildStreamsString(toJoin);
				if (twitchconnection.isOffline()) {
					maingui.openConnectDialog(validChannels);
				}
				maingui.printLine(Language.getString("chat.joinError.notConnected", validChannels));
			} else if (error == TwitchConnection.JoinError.ALREADY_JOINED) {
				if (toJoin.size() == 1) {
					maingui.switchToChannel(errorChannel);
				} else {
					maingui.printLine(Language.getString("chat.joinError.alreadyJoined", errorChannel));
				}
			} else if (error == TwitchConnection.JoinError.INVALID_NAME) {
				maingui.printLine(Language.getString("chat.joinError.invalid", errorChannel));
			} else if (error == TwitchConnection.JoinError.ROOM) {
				maingui.printLine(Language.getString("chat.joinError.rooms", errorChannel));
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
			maingui.printLineAll(message);
		}

		@Override
		public void onUserlistCleared(String channel) {
			maingui.clearUsers(channel);
		}

		@Override
		public void onHost(Room room, String target) {
		}

		@Override
		public void onChannelCleared(Room room) {
			if (room != null) {
				if (settings.getBoolean("clearChatOnChannelCleared")) {
					maingui.clearChat(room);
				}
				maingui.printLine(room, "Channel was cleared by a moderator.");
			} else {
				maingui.printLine("One of the channels you joined was cleared by a moderator.");
			}
		}

		@Override
		public void onWhisper(User user, String message, String emotes) {
			whispermanager.whisperReceived(user, message, emotes);
		}

		@Override
		public void onSubscriberNotification(User user, String text, String message, int months, MsgTags tags) {
			maingui.printSubscriberMessage(user, text, message, tags);

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
			maingui.printUsernotice(type, user, text, message, tags);
		}

		@Override
		public void onSpecialMessage(String name, String message) {
			maingui.printLine(roomManager.getRoom(name), message);
		}

		@Override
		public void onRoomId(String channel, String id) {
			if (Helper.isRegularChannel(channel)) {
				twitchapi.setUserId(Helper.toStream(channel), id);
			}
		}

	}

	private class IrcLogger {

		private final Logger IRC_LOGGER = Logger.getLogger(TwitchClient.IrcLogger.class.getName());

		IrcLogger() {
			IRC_LOGGER.setUseParentHandlers(false);
			IRC_LOGGER.addHandler(Logging.getIrcFileHandler());
		}

		public void onRawReceived(String text) {
			if (settings.getBoolean("debugLogIrc")) {
				maingui.printDebugIrc("--> " + text);
			}
			if (settings.getBoolean("debugLogIrcFile")) {
				IRC_LOGGER.info("--> " + text);
			}
		}

		public void onRawSent(String text) {
			if (settings.getBoolean("debugLogIrc")) {
				maingui.printDebugIrc("<-- " + text);
			}
			if (settings.getBoolean("debugLogIrcFile")) {
				IRC_LOGGER.info("<-- " + text);
			}
		}

	}

	public ChannelState getChannelState(String channel) {
		return twitchconnection.getChannelState(channel);
	}

	public Collection<String> getOpenChannels() {
		return twitchconnection.getOpenChannels();
	}

	public Collection<Room> getOpenRooms() {
		return twitchconnection.getOpenRooms();
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
			User roomUser = twitchconnection.getExistingUser(room.getChannel(), user.getName());
			result.add(new UserRoom(room, roomUser));
		}
		return result;
	}

	private class ChannelStateUpdater implements ChannelStateListener {

		@Override
		public void channelStateUpdated(ChannelState state) {
			maingui.updateState(true);
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
			maingui.printMessage(user, message, false, MsgTags.create("emotes", emotes));
			maingui.updateUser(user);
		}

		@Override
		public void info(String message) {
			maingui.printLine(message);
		}

		@Override
		public void whisperSent(User to, String message) {
			maingui.printMessage(to, message, true);
		}
	}

}
