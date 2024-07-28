
package chatty;

import chatty.Chatty.PathType;
import chatty.gui.components.updating.Version;
import chatty.util.colors.HtmlColors;
import chatty.gui.WindowStateManager;
import chatty.gui.components.eventlog.EventLog;
import chatty.gui.components.settings.NotificationSettings;
import chatty.gui.notifications.Notification;
import chatty.util.DateTime;
import chatty.util.ElapsedTime;
import chatty.util.StringUtil;
import chatty.util.colors.ColorCorrection;
import chatty.util.hotkeys.Hotkey;
import chatty.util.settings.FileManager;
import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class SettingsManager {
    
    private static final Logger LOGGER = Logger.getLogger(SettingsManager.class.getName());
    
    public final Settings settings;
    public final FileManager fileManager;
    
    private final List<DefaultHotkey> defaultHotkeys = new ArrayList<>();
    
    public static final long DISPLAY_NAMES_MODE_BOTH = 0;
    public static final long DISPLAY_NAMES_MODE_CAPITALIZED = 1;
    public static final long DISPLAY_NAMES_MODE_LOCALIZED = 2;
    public static final long DISPLAY_NAMES_MODE_USERNAME = 3;

    private final String[] debugSettings = {
        "server",
        "port",
        "serverDefault",
        "portDefault",
        "membershipEnabled",
        "whisperEnabled",
        "groupChatServer",
        "groupChatPort",
        "imageCache",
        "dontSaveSettings",
        "usePassword",
        "debugLogIrc",
        "showJoinsParts",
        "saveChannelHistory",
        "historyClear",
        "autoScroll",
        "bufferSize"
    };
    
    public SettingsManager() {
        fileManager = new FileManager(
                Chatty.getPathCreate(PathType.SETTINGS),
                Chatty.getPathCreate(PathType.BACKUP));
        FileManager.FileContentInfoProvider fileInfoProvider = new FileManager.FileContentInfoProvider() {

            @Override
            public FileManager.FileContentInfo getInfo(String content) {
                try {
                    JSONParser parser = new JSONParser();
                    JSONObject root = (JSONObject)parser.parse(content);
                    JSONArray ab = (JSONArray)root.get("abEntries");
                    return new FileManager.FileContentInfo(true, String.format(Locale.ROOT, "%d settings, %d addressbook entries",
                            root.size(), ab != null ? ab.size() : 0));
                }
                catch (Exception ex) {
                    return new FileManager.FileContentInfo(false, "Error parsing: "+ex.toString());
                }
            }
        };
        fileManager.add("settings", "settings", true, fileInfoProvider);
        fileManager.add("login", "login", false, fileInfoProvider);
        fileManager.add("favoritesAndHistory", "favoritesAndHistory", false, fileInfoProvider);
        fileManager.add("statusPresets", "statusPresets", false, fileInfoProvider);
        this.settings = new Settings("settings", fileManager);
    }
    
    /**
     * Defines what settings there are and their default values.
     */
    public void defineSettings() {
        
        // Additional files (in addition to the default file)
        String loginFile = "login";
        String historyFile = "favoritesAndHistory";
        String statusPresetsFile = "statusPresets";
        
        settings.addFile(loginFile);
        settings.addFile(historyFile);
        settings.addFile(statusPresetsFile);
        
        //========
        // General
        //========

        settings.addBoolean("dontSaveSettings", false);
        settings.addLong("autoSaveSettings", 15);
        settings.addBoolean("debugLogIrc", false);
        settings.addBoolean("debugLogIrcFile", false);
        settings.addString("ignoreError", "");
        settings.addBoolean("autoRequestMods", false);
        
        // Backup
        settings.addLong("backupDelay", 1);
        settings.addLong("backupCount", 10);

        // Version/News
        settings.addLong("versionLastChecked", 0);
        settings.addString("updateAvailable", "");
        settings.addBoolean("checkNewVersion", true);
        settings.addBoolean("checkNewBeta", false);
        settings.addBoolean("updateJar", false);
        settings.addBoolean("newsAutoRequest", true);
        settings.addLong("newsLastRead", 0);
        settings.addString("currentVersion", "");

        // Hotkeys
        addDefaultHotkeyAppWide("0.7.3", "dialog.streams", "ctrl L");
        addDefaultHotkeyAppWide("0.7.3", "dialog.toggleEmotes", "ctrl E");
        addDefaultHotkey("0.7.3", "dialog.search", "ctrl F");
        addDefaultHotkey("0.7.3", "dialog.joinChannel", "ctrl J");
        addDefaultHotkeyAppWide("0.7.3", "window.toggleUserlist", "F9");
        addDefaultHotkeyAppWide("0.7.3", "window.toggleInput", "ctrl F10");
        addDefaultHotkeyAppWide("0.7.3", "window.toggleCompact", "F10");
        addDefaultHotkeyAppWide("0.7.3", "window.toggleCompactMaximized", "F11");
        addDefaultHotkey("0.7.3", "tabs.close", "ctrl W");
        addDefaultHotkeyAppWide("0.7.3", "tabs.next", "ctrl TAB");
        addDefaultHotkeyAppWide("0.7.3", "tabs.previous", "ctrl shift TAB");
        addDefaultHotkey("0.7.3", "selection.toggle", "ctrl SPACE");
        addDefaultHotkey("0.7.3", "selection.toggle", "ctrl S");
        addDefaultHotkeyAppWide("0.9b1", "about", "F1");
        addDefaultHotkeyAppWide("0.26-b3", "scroll.pageUp", "PAGE_UP");
        addDefaultHotkeyAppWide("0.26-b3", "scroll.pageDown", "PAGE_DOWN");
        settings.addList("hotkeys", getDefaultHotkeySettingValue(), Setting.LIST);
        settings.addBoolean("globalHotkeysEnabled", true);
        settings.addBoolean("inputHistoryMultirowRequireCtrl", true);
        
        settings.addString("cachePath", "");
        settings.addString("imgPath", "");
        settings.addString("exportPath", "");

        //===========
        // Connecting
        //===========

        settings.addString("serverDefault", "irc.chat.twitch.tv");
        settings.addString("portDefault", "6697,443");

        // Separate settings for commandline/temp so others can be saved
        settings.addString("server", "", false);
        settings.addString("port", "", false);
        
        settings.addList("securedPorts", new LinkedHashSet<>(Arrays.asList((long)6697, (long)443)), Setting.LONG);
        settings.addBoolean("membershipEnabled", true);
        settings.addString("pubsub", "wss://pubsub-edge.twitch.tv");
        
        settings.addLong("maxReconnectionAttempts", -1);

        // Auto-join channels
        settings.addString("channel", "");

        // Login Data
        settings.addString("username", "");
        settings.setFile("username", loginFile);
        settings.addString("userid", "");
        settings.setFile("userid", loginFile);
        settings.addString("password", "", false);
        settings.addBoolean("connectOnStartup", false, false);
        settings.addLong("onStart", 3);
        settings.addBoolean("connectDialogIfMissing", true);
        settings.addLong("minimizeOnStart", 0);
        settings.addString("autojoinChannel", "");
        settings.addString("previousChannel", "");
        settings.addString("token","");
        settings.setFile("token", loginFile);
        settings.addBoolean("allowTokenOverride", false);
        settings.addBoolean("foreignToken", false);
        // Don't save setting, login with password isn't possible anymore
        settings.addBoolean("usePassword", false, false);
        settings.addList("scopes", new HashSet<>(), Setting.STRING);
        settings.setFile("scopes", loginFile);

        //=================
        // Appearance / GUI
        //=================

        settings.addBoolean("ontop", false);
        settings.addString("laf","default");
        settings.addString("lafTheme","Default");
        settings.addMap("lafCustomTheme", new HashMap<>(), Setting.STRING);
        settings.addLong("lafFontScale", 100);
        settings.addString("lafForeground", "#B4BEB9");
        settings.addString("lafBackground", "#323433");
        settings.addLong("lafGradient", 5);
        settings.addLong("lafVariant", 0);
        settings.addString("lafStyle", "regular");
        settings.addString("lafScroll", "default");
        settings.addBoolean("lafNativeWindow", true);
        settings.addBoolean("lafFlatStyledWindow", true);
        settings.addBoolean("lafFlatEmbeddedMenu", false);
        settings.addString("lafFlatProperties", "");
        settings.addLong("lafFlatTabs", 3);
        settings.addBoolean("lafErrorSound", false);
        settings.addString("language", "");
        settings.addString("locale", "");
        settings.addString("timezone", "");
        
        settings.addBoolean("inputLimitsEnabled", true);
        
        settings.addBoolean("macScreenMenuBar", true);
        settings.addBoolean("macSystemAppearance", true);
        
        settings.addLong("dialogFontSize", -1);

        // Chat Appearance
        settings.addString("font","Dialog");
        settings.addLong("fontSize",14);
        settings.addBoolean("timestampFontEnabled", false);
        settings.addString("timestampFont", "Dialog 12");
        settings.addString("inputFont", "Dialog 14");
        settings.addString("userlistFont", "Dialog 12");
        settings.addLong("lineSpacing", 2);
        settings.addLong("paragraphSpacing", 8);
        settings.addLong("bottomMargin", -1);
        settings.addString("timestamp","HH:mm");
        settings.addString("timestampTimezone", "");
        settings.addBoolean("capitalizedNames", true);
        settings.addBoolean("correctlyCapitalizedNames", false);
        settings.addMap("customNames", new HashMap<>(), Setting.STRING);
        settings.addBoolean("actionColored", false);
        settings.addLong("displayNamesMode", DISPLAY_NAMES_MODE_BOTH);
        settings.addLong("displayNamesModeUserlist", DISPLAY_NAMES_MODE_CAPITALIZED);
        settings.addBoolean("showImageTooltips", true);
        settings.addBoolean("showTooltipImages", true);
        settings.addLong("mentions", 3);
        settings.addLong("mentionsInfo", 3);
        settings.addLong("markHoveredUser", chatty.gui.components.textpane.SettingConstants.USER_HOVER_HL_MENTIONS);
        settings.addLong("mentionMessages", 0);

        // Badges/Emotes
        settings.addBoolean("emoticonsEnabled",true);
        settings.addLong("emoteMaxHeight", 0);
        settings.addLong("emoteScale", 100);
        settings.addLong("emoteScaleDialog", 100);
        settings.addLong("emoteScaleGigantified", 200);
        settings.addList("emoteHiddenSets", new ArrayList<>(), Setting.STRING);
        settings.addBoolean("closeEmoteDialogOnDoubleClick", false);
        settings.addBoolean("ffz", true);
        settings.addBoolean("ffzEvent", true);
        settings.addBoolean("ffzModIcon", true);
        settings.addBoolean("bttvEmotes", true);
        settings.addBoolean("seventv", true);
        settings.addBoolean("animatedEmotes", true);
        settings.addLong("animationPause", 2);
        settings.addLong("animationPauseFrame", 2);
        settings.addBoolean("legacyAnimations", false);
        settings.addList("ignoredEmotes", new ArrayList(), Setting.STRING);
        settings.addList("favoriteEmotes", new ArrayList(), Setting.LIST);
        settings.addLong("smilies", 10);
        settings.addList("localEmotes", new ArrayList(), Setting.LIST);
        settings.addBoolean("webp", true);
        
        settings.addString("emoji", "twemoji");
        settings.addBoolean("emojiReplace", true);
        settings.addLong("emojiZWJ", 2);
        settings.addString("cheersType", "animated");

        settings.addBoolean("usericonsEnabled", true);
        settings.addLong("usericonScale", 100);
        settings.addLong("customUsericonScaleMode", 0);
        settings.addList("customUsericons", new ArrayList(), Setting.LIST);
        settings.addBoolean("customUsericonsEnabled", false);
        
        settings.addList("hiddenUsericons", new ArrayList(), Setting.LIST);
        
        settings.addBoolean("botBadgeEnabled", true);
        settings.addBoolean("botNamesBTTV", true);
        settings.addBoolean("botNamesFFZ", true);
        settings.addList("botNames", new LinkedHashSet<>(Arrays.asList(
                "nightbot", "moobot", "kabukibot", "slowton2", "xanbot")),
                Setting.STRING);
        
        settings.addBoolean("imageCache", true);
        
        // Colors
        settings.addString("foregroundColor","#111111");
        settings.addString("backgroundColor","#FAFAFA");
        settings.addBoolean("alternateBackground", false);
        settings.addString("backgroundColor2","#EAEAEA");
        settings.addBoolean("messageSeparator", false);
        settings.addString("separatorColor", "#DFDFDF");
        settings.addBoolean("timestampColorEnabled", false);
        settings.addString("timestampColorInherit", "off");
        settings.addString("timestampColor", "#111111");
        settings.addString("infoColor","#001480");
        settings.addString("compactColor","#A0A0A0");
        settings.addString("inputBackgroundColor","White");
        settings.addString("inputForegroundColor","Black");
        settings.addString("highlightColor","#D10000");
        settings.addBoolean("highlightBackground", true);
        settings.addString("highlightBackgroundColor", "#FFFFEA");
        settings.addString("searchResultColor", "LightYellow");
        settings.addString("searchResultColor2", "#FFFF80");
        settings.addBoolean("colorCorrection", true);
        settings.addString("nickColorCorrection", "normal");
        settings.addLong("nickColorBackground", 1);
        settings.addList("colorPresets", new ArrayList<>(), Setting.LIST);
        settings.addBoolean("displayColoredNamesInUserlist", false);

        // Message Colors
        settings.addBoolean("msgColorsEnabled", false);
        settings.addList("msgColors", new LinkedList(), Setting.STRING);
        settings.addBoolean("msgColorsPrefer", false);
        settings.addBoolean("msgColorsLinks", true);
        
        // Usercolors
        settings.addBoolean("customUsercolors", false);
        settings.addList("usercolors", new LinkedList(), Setting.STRING);
        
        // Transparency
        settings.addLong("transparencyBackground", 50);
        settings.addBoolean("transparencyClickThrough", true);
        settings.addString("transparencyCurrentId", "");

        //====================
        // Other Customization
        //====================

        // Addressbook
        settings.addString("abCommandsChannel", "");
        settings.addString("abCommands", "add,set,remove");
        settings.addString("abUniqueCats", "");
        settings.addBoolean("abAutoImport", false);
        settings.addString("abSubMonthsChan", "");
        settings.addList("abSubMonths", new TreeSet(), Setting.LONG);
        settings.addBoolean("abSaveOnChange", false);
        settings.addList("abEntries", new ArrayList(), Setting.LIST);

        // Custom Commands
        List<String> commandsDefault = new ArrayList<>();
        commandsDefault.add("/slap /me slaps $$1- around a bit with a large trout");
        commandsDefault.add("/permit !permit $$1");
        commandsDefault.add("/j /join $$1-");
        settings.addList("commands", commandsDefault, Setting.STRING);
        settings.addMap("var", new HashMap(), Setting.STRING);
        settings.addList("timers", new ArrayList<>(), Setting.LIST);

        // Menu Entries
        settings.addString("timeoutButtons","/Ban[B], /Unban[U], 5s[1], 2m[2], 10m[3], 30m[4], /ModUnmod"
                + "\n\n"
                + "@AutoMod\n"
                + ".Approve=/Automod_approve\n"
                + ".Deny=/Automod_deny\n"
                + "\n"
                + "Delete=/delete $$(msg-id)");
        settings.addString("banReasons", "Spam\nPosting Bad Links\nBan Evasion\n"
                                + "Hate / Harassment\nSpoilers / Backseat Gaming");
        settings.addString("banReasonsHotkey", "");
        settings.addString("userContextMenu", "");
        settings.addString("channelContextMenu", "");
        settings.addString("streamsContextMenu", "");
        settings.addString("textContextMenu", "-\n" +
                "Translate=/openUrlPrompt https://translate.google.com/#view=home&op=translate&sl=auto&tl=en&text=$$urlencode($(msg))");
        settings.addString("adminContextMenu", "!title=!title $(title)\n!game=!game $(game)");
        settings.addBoolean("menuCommandLabels", false);
        settings.addBoolean("menuRestrictions", false);
        
        settings.addBoolean("closeUserDialogOnAction", true);
        settings.addBoolean("openUserDialogByMouse", true);
        settings.addBoolean("reuseUserDialog", false);
        settings.addString("userDialogTimestamp", "[HH:mm:ss]");
        settings.addLong("clearUserMessages", 12);
        settings.addLong("userMessagesHighlight", 15);
        settings.addMap("userNotes", new HashMap(), Setting.STRING);
        settings.addMap("userNotesChat", new HashMap(), Setting.STRING);
        settings.addLong("userDialogMessageLimit", 100);

        // History / Favorites
        settings.addMap("channelHistory",new TreeMap(), Setting.LONG);
        //settings.setFile("channelHistory", historyFile);
        settings.addList("channelFavorites", new ArrayList(), Setting.STRING);
        //settings.setFile("channelFavorites", historyFile);
        settings.addMap("roomFavorites", new HashMap(), Setting.LIST);
        //settings.setFile("roomFavorites", historyFile);
        settings.addLong("channelHistoryKeepDays", 30);
        settings.addBoolean("saveChannelHistory", true);
        settings.addBoolean("historyClear", true);
        settings.addLong("favoritesSorting", 20);
        
        settings.addList("gameFavorites", new ArrayList(), Setting.STRING);

        settings.addBoolean("historyServiceEnabled", false);
        settings.addLong("historyServiceLimit", 30);
        settings.addList("historyServiceExcluded", new ArrayList(), Setting.STRING);
        settings.addBoolean("historyMessageHighlight", false);
        settings.addBoolean("historyMessageMsgColors", false);
        settings.addBoolean("historyMessageIgnore", true);
        settings.addBoolean("historyMessageRouting", false);
        settings.addBoolean("historyMessageNotifications", false);

        //=======================
        // Channel Admin Features
        //=======================

        // Game Presets
        settings.addList("gamesFavorites",new ArrayList(), Setting.STRING);
        // New format for saving id and name
        settings.addList("gamesFavorites2", new ArrayList(), Setting.LIST);
        
        // Tags Presets
        settings.addMap("tagsFavorites", new HashMap(), Setting.STRING);

        // Stream Status Presets
        settings.addList("statusPresets", new ArrayList(), Setting.LIST);
        //settings.setFile("statusPresets", statusPresetsFile);

        settings.addBoolean("saveStatusHistory", true);
        settings.addBoolean("statusHistoryClear", true);
        settings.addLong("statusHistoryKeepDays", 30);
        settings.addString("statusHistorySorting", "");
        
        // Commercials
        settings.addString("commercialHotkey","");
        settings.addBoolean("adDelay", false);
        settings.addLong("adDelayLength", 300);
        
        // Moderation Presets
        settings.addString("slowmodeDurations", "3s\n5s\n10s\n20s\n30s\n60s\n120s");
        settings.addString("followeronlyDurations", "0m\n10m\n30m\n1h\n1d\n7d\n30d\n90d");

        //=======
        // Window
        //=======

        // Open URLs
        settings.addBoolean("urlPrompt", true);
        settings.addBoolean("urlCommandEnabled", false);
        settings.addString("urlCommand", "");

        // Main Window
        settings.addBoolean("attachedWindows", false);
        settings.addBoolean("maximized", false);
        settings.addBoolean("nod3d", true);
        settings.addBoolean("noddraw", false);
        settings.addLong("uiScale", 0);
        settings.addBoolean("bufferStrategy1", false);
        settings.addBoolean("mainResizable", true);
        settings.addBoolean("splash", true);
        settings.addBoolean("hideStreamsOnMinimize", true);
        settings.addLong("inputFocus", 0);
        settings.addList("icons", new ArrayList<>(), Setting.STRING);
        
        // Tray
        settings.addBoolean("closeToTray", false);
        settings.addBoolean("minimizeToTray", false);
        settings.addBoolean("hidePopoutsIfTray", true);
        settings.addBoolean("trayIconAlways", false);
        settings.addBoolean("singleClickTrayOpen", true);
        
        // Window State
        settings.addMap("windows", new HashMap<>(), Setting.STRING);
        settings.addLong("restoreMode", WindowStateManager.RESTORE_ON_START);
        settings.addBoolean("restoreOnlyIfOnScreen", true);
        settings.addMap("dock", new HashMap<>(), Setting.LONG);
        settings.addMap("layouts", new HashMap<>(), Setting.LIST);
        settings.addLong("layoutsOptions", 3);
        settings.addBoolean("restoreLayout", true);
        settings.addBoolean("restoreLayoutWhisper", false);
        settings.addBoolean("initSettingsDialog", false);

        // Popouts
        settings.addBoolean("popoutSaveAttributes", true);
        settings.addBoolean("popoutCloseLastChannel", false);
        settings.addList("popoutAttributes", new ArrayList(), Setting.STRING);
        settings.addString("popoutClose", "ask");
        
        // Titlebar
        settings.addBoolean("simpleTitle", false);
        settings.addBoolean("titleShowUptime", true);
        settings.addBoolean("titleLongerUptime", true);
        settings.addBoolean("titleShowViewerCount", true);
        settings.addBoolean("titleShowChannelState", true);
        settings.addBoolean("titleConnections", true);
        settings.addString("titleAddition", "");

        // Tabs
        settings.addString("tabOrder", "normal");
        Map<String, Long> tabsPos = new HashMap<>();
        tabsPos.put("-nochannel-", -2L);
        tabsPos.put("#", -1L);
        tabsPos.put("-", 1L);
        settings.addMap("tabsPos", tabsPos, Setting.LONG);
        settings.addBoolean("tabsAutoSort", true);
        settings.addString("tabsOpen", "activeChan");
        settings.addBoolean("tabsMwheelScrolling", false);
        settings.addBoolean("tabsMwheelScrollingAnywhere", true);
        settings.addBoolean("tabsCloseMMB", true);
        settings.addBoolean("tabsCloseSwitchToPrev", true);
        settings.addString("tabsPlacement", "top");
        settings.addString("tabsLayout", "wrap");
        settings.addBoolean("tabsHideIfSingle", true);
        settings.addLong("tabsLive", 16);
        settings.addLong("tabsMessage", 4);
        settings.addLong("tabsHighlight", 8);
        settings.addLong("tabsStatus", 32);
        settings.addLong("tabsActive", 128);
        settings.addLong("tabsPopoutDrag", 2);
        settings.addLong("tabsMaxWidth", 200);
        settings.addBoolean("tabsCloseEmpty", true);
        settings.addBoolean("closeTabsSameType", true);
        settings.addBoolean("tabsChanTitles", false);
        
        // Chat Window
        settings.addBoolean("chatScrollbarAlways", false);
        settings.addLong("userlistWidth", 120);
        settings.addLong("userlistMinWidth", 0);
        settings.addBoolean("userlistEnabled", true);
        settings.addBoolean("inputEnabled", true);
        settings.addLong("bufferSize", 500);
        settings.addMap("bufferSizes", new HashMap<>(), Setting.LONG);

        settings.addString("liveStreamsSorting", "recent");
        settings.addBoolean("liveStreamsSortingFav", true);
        settings.addString("liveStreamsAction", "info");
        settings.addString("liveStreamsCommand", "");
        settings.addBoolean("liveStreamsChatIcon", true);
        settings.addBoolean("liveStreamsNotificationAction", false);
        settings.addBoolean("liveStreamsFavsOnly", false);
        settings.addLong("historyRange", 0);
        settings.addBoolean("historyVerticalZoom", false);
        
        settings.addBoolean("followersCompact", false);
        settings.addBoolean("followersReg", true);

        //=======
        // Sounds
        //=======
        settings.addBoolean("sounds", true);
        settings.addString("soundsPath", "");
        settings.addString("soundDevice", "");
        settings.addString("highlightSound", "off");
        settings.addString("highlightSoundFile", "ding.wav");
        settings.addLong("highlightSoundDelay", 15);
        settings.addLong("soundDelay", 15);
        settings.addLong("highlightSoundVolume", 100);
        settings.addString("statusSound","off");
        settings.addString("statusSoundFile","dingdong.wav");
        settings.addLong("statusSoundVolume",100);
        settings.addLong("statusSoundDelay", 15);
        settings.addString("messageSound","off");
        settings.addString("messageSoundFile","dingdong.wav");
        settings.addLong("messageSoundVolume",100);
        settings.addLong("messageSoundDelay", 5);
        settings.addString("joinPartSound","off");
        settings.addString("joinPartSoundFile","dingdong.wav");
        settings.addLong("joinPartSoundVolume",100);
        settings.addLong("joinPartSoundDelay", 10);
        settings.addString("followerSound","off");
        settings.addString("followerSoundFile","dingdong.wav");
        settings.addLong("followerSoundVolume",100);
        settings.addLong("followerSoundDelay", 10);
        
        //==============
        // Notifications
        //==============
        settings.addBoolean("requestFollowedStreams", true);
        
        settings.addLong("nType", NotificationSettings.NOTIFICATION_TYPE_CUSTOM);
        settings.addLong("nScreen", -1);
        settings.addLong("nPosition", 3);
        settings.addLong("nDisplayTime", 10);
        settings.addLong("nMaxDisplayTime", 60*30);
        settings.addBoolean("nKeepOpenOnHover", true);
        settings.addLong("nMaxDisplayed", 4);
        settings.addLong("nMaxQueueSize", 4);
        settings.addBoolean("nActivity", false);
        settings.addLong("nActivityTime", 10);
        settings.addString("nCommand", "");
        settings.addBoolean("nHideOnStart", false);
        settings.addBoolean("nInfoMsgEnabled", false);
        settings.addString("nInfoMsgTarget", "Notifications");

        settings.addList("notifications", getDefaultNotificationSettingValue(), Setting.LIST);
        settings.addList("nColorPresets", new ArrayList<>(), Setting.LIST);

        settings.addBoolean("tips", true);
        settings.addLong("lastTip", 0);
        
        settings.addList("readEvents", new ArrayList<>(), Setting.STRING);

        //=====================
        // Basic Chat Behaviour
        //=====================

        settings.addString("spamProtection", "18/30");

        settings.addBoolean("autoScroll", true);
        settings.addLong("autoScrollTimeout", 30);
        settings.addBoolean("pauseChatOnMouseMove", false);
        settings.addBoolean("pauseChatOnMouseMoveCtrlRequired", false);
        settings.addString("commandOnCtrlClick", "");
        settings.addString("commandOnMiddleClick", "");
        settings.addString("commandOnCtrlMiddleClick", "");

        // Not really used anymore, kept for compatability
        settings.addBoolean("ignoreJoinsParts",false);

        // Message Types
        settings.addBoolean("showJoinsParts", false);
        settings.addBoolean("showModMessages", false);
        settings.addBoolean("twitchnotifyAsInfo", true);
        settings.addBoolean("printStreamStatus", true);
        settings.addBoolean("showModActions", true);
        settings.addBoolean("showModActionsRestrict", true);
        settings.addBoolean("showActionBy", true);
        settings.addBoolean("showAutoMod", true);

        // Timeouts/Bans
        settings.addBoolean("showBanMessages", false);
        settings.addBoolean("banDurationAppended", true);
        settings.addBoolean("banReasonAppended", true);
        settings.addBoolean("banDurationMessage", true);
        settings.addBoolean("banReasonMessage", true);
        settings.addBoolean("combineBanMessages", true);
        settings.addBoolean("deleteMessages", false);
        settings.addString("deletedMessagesMode", "keepShortened");
        settings.addLong("deletedMessagesMaxLength", 50);
        settings.addBoolean("clearChatOnChannelCleared", false);

        // Message filtering
        settings.addLong("filterCombiningCharacters", Helper.FILTER_COMBINING_CHARACTERS_LENIENT);
        
        // Low-trust/restricted messages
        settings.addBoolean("showLowTrustInfo", false);
        settings.addBoolean("showRestrictedMessages", false);


        //==============
        // Chat Features
        //==============

        // Highlight
        settings.addList("highlight",new ArrayList(), Setting.STRING);
        settings.addBoolean("highlightEnabled", true);
        settings.addBoolean("highlightUsername", true);
        settings.addBoolean("highlightOwnText", false);
        settings.addBoolean("highlightNextMessages", false);
        settings.addBoolean("highlightIgnored", false);
        settings.addBoolean("highlightOverrideIgnored", false);
        settings.addList("noHighlightUsers", new ArrayList(), Setting.STRING);
        settings.addList("highlightBlacklist", new ArrayList(), Setting.STRING);
        settings.addBoolean("highlightMatches", true);
        settings.addBoolean("highlightMatchesAll", true);
        settings.addBoolean("highlightMatchesAllEntries", false);
        settings.addBoolean("highlightByPoints", true);

        // Ignore
        settings.addList("ignore", new ArrayList(), Setting.STRING);
        settings.addBoolean("ignoreEnabled", false);
        settings.addBoolean("ignoreOwnText", false);
        settings.addLong("ignoreMode", 0);
        settings.addBoolean("ignoreShowNotDialog", false);
        settings.addList("ignoredUsers", new ArrayList(), Setting.STRING);
        settings.addList("ignoredUsersWhisper", new ArrayList(), Setting.STRING);
        settings.addBoolean("ignoredUsersHideInGUI", true);
        settings.addList("ignoreBlacklist", new ArrayList(), Setting.STRING);
        
        // Filter
        settings.addList("filter", new ArrayList(), Setting.STRING);
        settings.addBoolean("filterEnabled", true);
        settings.addBoolean("filterOwnText", true);
        
        // Routing
        settings.addList("routingTargets", new ArrayList(), Setting.LIST);
        settings.addList("routing", new ArrayList(), Setting.STRING);
        settings.addBoolean("routingMulti", true);
        
        // Matching
        List<String> matchingPresetsDefault = new ArrayList<>();
        matchingPresetsDefault.add("# _custom replaces \\! with [\\W_]*? (matches non-word characters and underscore 0 or more times)");
        matchingPresetsDefault.add("_custom $replace($1-,$\"\\\\!\",$\"[\\W_]*?\",reg)");
        matchingPresetsDefault.add("# _special replaces every letter of words surrounded by ~ with: (<letter>[\\W_]*?)+");
        matchingPresetsDefault.add("_special $replace($1-,$\"~([^~]+)~\",$replace($(g1),$\"(\\w)\",$\"($1[\\\\W_]*?)+\",regRef),regCustom)");
        settings.addList("matchingPresets", matchingPresetsDefault, Setting.STRING);
        settings.addList("matchingSubstitutes", new ArrayList(), Setting.STRING);
        settings.addBoolean("matchingSubstitutesEnabled", false);
        
        // Repeated Messages
        settings.addBoolean("repeatMsg", false);
        settings.addLong("repeatMsgSim", 80);
        settings.addLong("repeatMsgRep", 2);
        settings.addLong("repeatMsgLen", 0);
        settings.addLong("repeatMsgTime", 3600);
        settings.addLong("repeatMsgMethod", 1);
        settings.addString("repeatMsgIgnored", "");
        settings.addString("repeatMsgMatch", "!status:M");

        // Chat Logging
        settings.addString("logMode", "always");
        settings.addBoolean("logMessage", true);
        settings.addString("logMessageTemplate", "$if(timestamp,$(timestamp) )<$(full-nick2)>$if(action,*) $(msg)");
        settings.addBoolean("logMod", false);
        settings.addBoolean("logJoinPart", false);
        settings.addBoolean("logBan", true);
        settings.addBoolean("logDeleted", true);
        settings.addBoolean("logSystem", false);
        settings.addBoolean("logInfo", true);
        settings.addBoolean("logViewerstats", true);
        settings.addBoolean("logViewercount", false);
        settings.addBoolean("logModAction", true);
        settings.addBoolean("logIgnored", true);
        settings.addBoolean("logBits", true);
        settings.addList("logWhitelist",new ArrayList(), Setting.STRING);
        settings.addList("logBlacklist",new ArrayList(), Setting.STRING);
        settings.addBoolean("logHighlighted2", false);
        settings.addBoolean("logIgnored2", false);
        settings.addString("logPath", "");
        settings.addString("logSplit", "never");
        settings.addBoolean("logSubdirectories", false);
        settings.addString("logTimestamp", "[yyyy-MM-dd HH:mm:ss]");
        settings.addBoolean("logLockFiles", true);
        
        // TAB Completion
        settings.addBoolean("completionEnabled", true);
        settings.addMap("customCompletion", new HashMap(), Setting.STRING);
        settings.addLong("completionMaxItemsShown", 5);
        settings.addBoolean("completionShowPopup", true);
        settings.addBoolean("completionCommonPrefix", false);
        settings.addString("completionSorting", "predictive");
        settings.addBoolean("completionAllNameTypes", true);
        settings.addBoolean("completionPreferUsernames", true);
        settings.addBoolean("completionAllNameTypesRestriction", true);
        settings.addString("completionTab", "both");
        settings.addString("completionTab2", "emotes");
        settings.addString("completionSearch", "words");
        settings.addBoolean("completionAuto", true);
        settings.addString("completionEmotePrefix", ":");
        settings.addLong("completionMixed", 0);
        settings.addBoolean("completionSpace", false);
        settings.addBoolean("completionFavEmotesFirst", true);
        
        // Replying
        settings.addBoolean("mentionReplyRestricted", false);

        // Stream Chat
        settings.addLong("streamChatMessageTimeout", -1);
        settings.addList("streamChatChannels", new ArrayList(), Setting.STRING);
        settings.addBoolean("streamChatBottom", true);
        settings.addBoolean("streamChatResizable", true);
        // Size (String in case <width>x<height> needs to be added later or something)
        settings.addString("streamChatLogos", "22");

        // Whispering
        settings.addBoolean("whisperEnabled", false);
        settings.addBoolean("whisperWhitelist", false);
        settings.addLong("whisperDisplayMode", WhisperManager.DISPLAY_PER_USER);
        settings.addString("groupChatServer", "");
        settings.addString("groupChatPort", "");
        settings.addBoolean("whisperAutoRespond", false);
        settings.addString("whisperAutoRespondCustom", "");
        settings.addBoolean("whisperApi", false);
        
        // Copy Messages
        settings.addBoolean("cmEnabled", false);
        settings.addString("cmChannel", "");
        settings.addString("cmTemplate", "{user}: {message}");
        settings.addBoolean("cmHighlightedOnly", false);

        // Chat rules API removed, but keep this for now
        settings.addBoolean("rulesAutoShow", true);
        settings.addList("rulesShown", new HashSet(), Setting.STRING);

        //===============
        // Other Features
        //===============

        // Livestreamer
        settings.addBoolean("livestreamer", false);
        settings.addString("livestreamerQualities", "Best, Worst, Select");
        settings.addString("livestreamerCommand", "livestreamer");
        settings.addBoolean("livestreamerShowDialog", true);
        settings.addBoolean("livestreamerAutoCloseDialog", true);

        // Stream Highlights
        settings.addString("streamHighlightCommand", "!highlight");
        settings.addString("streamHighlightMatch", "status:bm");
        settings.addString("streamHighlightChannel", "");
        settings.addBoolean("streamHighlightChannelRespond", false);
        settings.addString("streamHighlightResponseMsg", "Added stream $(added) for $(chan) [$(uptime)] $(comment)");
        settings.addBoolean("streamHighlightMarker", true);
        settings.addBoolean("streamHighlightCustomEnabled", false);
        settings.addString("streamHighlightCustom", "$(timestamp),$(chan),$(uptime),$(streamgame),$(chatuser),$quote($(rawcomment))");
        settings.addBoolean("streamHighlightExtra", true);
        settings.addLong("streamHighlightCooldown", 0);

        // Stream Status Writer
        settings.addBoolean("enableStatusWriter", false);
        settings.addString("statusWriter", "");
        
        settings.addMap("rewards", new HashMap(), Setting.STRING);
        
        settings.addBoolean("pronouns", false);
        settings.addBoolean("pronounsChat", false);
    }
    
    private boolean loadSuccess;
    
    /**
     * Tries to load the settings from file.
     * 
     * @return 
     */
    public boolean loadSettingsFromFile() {
        loadSuccess = settings.loadSettingsFromJson();
        return loadSuccess;
    }
    
    public boolean getLoadSuccess() {
        return loadSuccess;
    }
    
    public boolean wasMainFileLoaded() {
        return settings.wasFileLoaded("settings");
    }
    
    /**
     * Perform backup, specifying the minimum delay the backup should be
     * performed in (seconds). The backup manager will decide whether to
     * actually make a backup.
     */
    public void backupFiles() {
        long backupDelay = DateTime.DAY * settings.getLong("backupDelay");
        try {
            fileManager.backup(backupDelay, (int)settings.getLong("backupCount"));
        }
        catch (IOException ex) {
            LOGGER.warning("Backup failed: "+ex);
        }
    }
    
    /**
     * Goes through the commandline options and sets the settings accordingly.
     * 
     * Commandline options consist of key=value pairs, although empty values
     * are possible.
     * 
     * @param args Map with commandline settings, key=value pairs
     */
    public void loadCommandLineSettings(Map<String, String> args) {
        for (String key : args.keySet()) {
            // Go through all commandline options
            String value = args.get(key);
            if (key == null) {
                continue;
            }
            switch (key) {
                case "user":
                    settings.setString("username", value);
                    break;
                case "channel":
                    settings.setString("channel", value);
                    break;
                case "connect":
                    settings.setBoolean("connectOnStartup", true);
                    break;
                case "token":
                    if (!value.isEmpty()) {
                        if (settings.getBoolean("allowTokenOverride")
                                || settings.getString("token").isEmpty()
                                || settings.getBoolean("foreignToken")) {
                            settings.setString("token", value);
                            settings.setBoolean("foreignToken", true);
                        }
                    }
                    settings.setBoolean("usePassword", false);
                    break;
                case "password":
                    settings.setString("password", value);
                    settings.setBoolean("usePassword", true);
                    break;
                case "ds":
                    settings.setBoolean("dontSaveSettings", true);
                    break;
                case "server":
                    settings.setString("server", value);
                    break;
                case "port":
                    settings.setString("port", value);
                    break;
                case "single":
                    LOGGER.info("Single instance (port: "+(value.isEmpty() ? "default" : value)+")");
                    break;
            }
            if (key.startsWith("set:") && key.length() > 4) {
                LOGGER.info("Setting commandline setting: "+settings.setTextual(key.substring(4)+" "+value, true));
            }
        }
    }
    
    private static String previousVersion;
    
    /**
     * Override some now unused settings or change settings on version change.
     */
    public void overrideSettings() {
        previousVersion = settings.getString("currentVersion");
        if (!Chatty.VERSION.equals(previousVersion)) {
            LOGGER.info("Changed from version "+previousVersion);
        }
        
        settings.setBoolean("ignoreJoinsParts", false);
        if (switchedFromVersionBefore("0.7.2")) {
            String value = settings.getString("timeoutButtons");
            if (value.equals("5,2m,10m,30m")) {
                /**
                 * Setting is equal to the old default value, so it probably
                 * wasn't customized, so just reset it to the new default value.
                 */
                settings.setString("timeoutButtons", null);
                LOGGER.warning("Updated timeoutButtons setting to new default");
            } else if (!StringUtil.toLowerCase(value).contains("/ban") &&
                    !StringUtil.toLowerCase(value).contains("/unban")) {
                /**
                 * Setting wasn't on the old default value, but it doesn't
                 * contain /Ban or /Unban, so add those to the current
                 * (customized) value.
                 */
                String newValue = "/Ban[B], /Unban[U], "+value;
                settings.setString("timeoutButtons", newValue);
                LOGGER.warning("Added /Ban,/Unban to timeoutButtons setting, now: "+newValue);
            }
        }
        if (switchedFromVersionBefore("0.8.1")) {
            if (settings.getString("portDefault").equals("6667,80")) {
                settings.setString("portDefault", "6667,443");
            }
        }
        if (switchedFromVersionBefore("0.8.2")) {
            if (settings.getString("serverDefault").equals("irc.twitch.tv")) {
                settings.setString("serverDefault", "irc.chat.twitch.tv");
            }
            if (settings.getString("portDefault").equals("6667,443")) {
                settings.setString("portDefault", "6697,6667,443,80");
            }
            settings.setAdd("securedPorts", (long)443);
        }
        if (switchedFromVersionBefore("0.8.5b4")) {
            String currentValue = settings.getString("timeoutButtons");
            if (!StringUtil.toLowerCase(currentValue).contains("/modunmod")) {
                settings.setString("timeoutButtons", currentValue+"\n/ModUnmod");
            }
        }
        if (switchedFromVersionBefore("0.8.6b3")) {
            settings.putList("notifications", getDefaultNotificationSettingValue());
        }
        if (switchedFromVersionBefore("0.8.7b1")) {
            String currentValue = settings.getString("timeoutButtons");
            if (!StringUtil.toLowerCase(currentValue).contains("/automod_approve")) {
                settings.setString("timeoutButtons", currentValue + "\n\n"
                        + "@AutoMod\n"
                        + ".Approve=/Automod_approve\n"
                        + ".Deny=/Automod_deny");
            }
        }
        if (switchedFromVersionBefore("0.9.3")) {
            String currentValue = settings.getString("timeoutButtons");
            if (!StringUtil.toLowerCase(currentValue).contains("/delete")) {
                settings.setString("timeoutButtons", currentValue + "\n\n"
                        + "Delete=/delete $$(msg-id)");
            }
        }
        if (switchedFromVersionBefore("0.9.1b3")) {
            /**
             * Migrate both favorites and history, but only channels that don't
             * have a value in the new setting yet.
             * 
             * This won't turn an already existing entry into a favorite even if
             * it was a favorite before, however this usually shouldn't be an
             * issue (except in some cases where 0.9.1b2 was used before, where
             * not all favorites were migrated correctly).
             */
            LOGGER.info("Migrating Favorites/History");
            @SuppressWarnings("unchecked") // Setting
            List<String> favs = settings.getList("channelFavorites");
            @SuppressWarnings("unchecked") // Setting
            Map<String, Long> history = settings.getMap("channelHistory");
            @SuppressWarnings("unchecked") // Setting
            Map<String, List> data = settings.getMap("roomFavorites");
            // Migrate history
            for (String stream : history.keySet()) {
                boolean isFavorite = favs.contains(stream);
                long lastJoined = history.get(stream);
                String channel = Helper.toChannel(stream);
                if (!data.containsKey(channel)) {
                    data.put(channel, new ChannelFavorites.Favorite(
                            Room.createRegular(channel), lastJoined, isFavorite).toList());
                }
            }
            // Migrate favorites
            for (String fav : favs) {
                String channel = Helper.toChannel(fav);
                if (!data.containsKey(channel)) {
                    data.put(channel, new ChannelFavorites.Favorite(
                            Room.createRegular(channel), -1, true).toList());
                }
            }
            settings.putMap("roomFavorites", data);
        }
        
        // Turn off Highlight Background if using dark background (if not loaded
        // from the settings yet)
        Color bgColor = HtmlColors.decode(settings.getString("backgroundColor"));
        if (ColorCorrection.isDarkColor(bgColor) && !settings.isValueSet("highlightBackground")) {
            settings.setBoolean("highlightBackground", false);
        }
        
        if (switchedFromVersionBefore("0.9.3-b5")) {
            if (!settings.getBoolean("colorCorrection")) {
                settings.setString("nickColorCorrection", "off");
            }
        }
        
        if (switchedFromVersionBefore("0.9.7-b4")) {
            settings.setLong("mentionsInfo", settings.getLong("mentions"));
        }
        if (switchedFromVersionBefore("0.16")) {
            // Y is "Week year", which may not be what is expected sometimes
            for (String setting : new String[]{"timestamp", "logTimestamp", "userDialogTimestamp"}) {
                settings.setString(setting, settings.getString(setting).replace("Y", "y"));
            }
        }
        if (switchedFromVersionBefore("0.21-b3")) {
            settings.setBoolean("ffzEvent", false);
        }
        
        overrideHotkeySettings();
    }
    
    /**
     * Checks whether the current version (the version saved in the
     * "currentVersion" setting, which is overwritten with Chatty.VERSION on
     * every start) is smaller than the given one.
     * 
     * This basicially means that if the user last used a version smaller than
     * the given one, this returns true. Usually the current version would be
     * used if a new condition is added, to change a setting only on the switch
     * to the given version.
     * 
     * This can be used after the settings have been properly loaded.
     *
     * @param version The version to check against
     * @return true if the given version is greater than the current version
     */
    public static boolean switchedFromVersionBefore(String version) {
        if (previousVersion != null) {
            return Version.compareVersions(previousVersion, version) == 1;
        }
        return false;
    }
    
    public void debugSettings() {
//        String invalidPath = Chatty.getInvalidCustomPath(PathType.SETTINGS);
//        if (invalidPath != null) {
//            LOGGER.warning("Invalid -d dir: "+invalidPath);
//        }
//        StringBuilder result = new StringBuilder("Settings: ");
//        boolean first = true;
//        for (String setting : debugSettings) {
//            if (!first) {
//                result.append(", ");
//            } else {
//                first = false;
//            }
//            result.append(setting);
//            result.append(":");
//            result.append(settings.settingValueToString(setting));
//        }
//        LOGGER.info(result.toString());
    }
    
    public boolean checkSettingsDir() {
        return Files.isDirectory(Chatty.getPath(PathType.SETTINGS));
    }
    
    private void addDefaultHotkey(String version, String id, String hotkey) {
        defaultHotkeys.add(new DefaultHotkey(version,
                Arrays.asList(new Object[]{id, hotkey})));
    }
    
    private void addDefaultHotkeyAppWide(String version, String id, String hotkey) {
        defaultHotkeys.add(new DefaultHotkey(version,
                Arrays.asList(new Object[]{id, hotkey, Hotkey.Type.APPLICATION.id})));
    }
    
    private List<List> getDefaultHotkeySettingValue() {
        List<List> data = new ArrayList<>();
        for (DefaultHotkey hotkey : defaultHotkeys) {
            data.add(hotkey.data);
        }
        return data;
    }
    
    /**
     * When settings have already been loaded from file, add some more default
     * ones if previous version older than specified with the hotkey.
     */
    private void overrideHotkeySettings() {
        for (DefaultHotkey hotkey : defaultHotkeys) {
            // Check version of when the default hotkey was added
            if (switchedFromVersionBefore(hotkey.version)) {
                @SuppressWarnings("unchecked") // Setting
                List<List> setting = settings.getList("hotkeys");
                Iterator<List> it = setting.iterator();
                // Remove hotkey if already in setting
                while (it.hasNext()) {
                    // Compare hotkey ids
                    if (it.next().get(0).equals(hotkey.data.get(0))) {
                        it.remove();
                    }
                }
                // Add hotkey with default settings
                setting.add(hotkey.data);
                LOGGER.info("Overriding hotkey setting: "+hotkey.data);
                settings.putList("hotkeys", setting);
            }
        }
    }
    
    private final ElapsedTime lastAutoSaved = new ElapsedTime(true);

    void startAutoSave(TwitchClient c) {
        Timer timer = new Timer("AutoSaveSettings", false);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                int delay = (int)settings.getLong("autoSaveSettings")*60;
                //System.out.println(lastAutoSaved.secondsElapsedSync()+" "+(int)settings.getLong("autoSaveSettings")*60);
                if (delay > 0 && lastAutoSaved.secondsElapsedSync(delay)) {
                    lastAutoSaved.setSync();
                    List<FileManager.SaveResult> results = c.saveSettings(false, false);
                    if (results == null) {
                        // Saving settings not currently enabled
                        return;
                    }
                    for (FileManager.SaveResult r : results) {
                        if (r.writeError != null) {
                            String msg = "["+r.id+"] "+Helper.getErrorMessageCompact(r.writeError)
                                    +" (You can save manually under 'Main - Save..' to check if the issue persists.)";
                            if (r.backupWritten && r.backupError == null) {
                                msg += "\nBackup was successfully written to: "+r.backupPath;
                            }
                            EventLog.addSystemEvent("session.settings.writeError", msg);
                        }
                    }
                }
            }
        }, 30*1000, 30*1000);
    }
    
    private static class DefaultHotkey {
        
        String version;
        List data;

        DefaultHotkey(String version, List data) {
            this.version = version;
            this.data = data;
        }
        
    }
    
    /**
     * Used both for creating a default (as the method name would suggest) and
     * setting a value once on version change based on previous settings (note
     * that settings are loaded after the default is set, so that will in turn
     * use the defaults of the referenced settings).
     * 
     * @return 
     */
    private List<List> getDefaultNotificationSettingValue() {
        String hl = "either";
        String st = "either";
        
        Notification.Builder hlNew = new Notification.Builder(Notification.Type.HIGHLIGHT);
        hlNew.setForeground(Color.BLACK);
        hlNew.setBackground(HtmlColors.decode("#FFFF79"));
        hlNew.setDesktopEnabled(convertOldState(hl));
        
        Notification.Builder stNew = new Notification.Builder(Notification.Type.STREAM_STATUS);
        stNew.setForeground(Color.BLACK);
        stNew.setBackground(HtmlColors.decode("#FFFFF0"));
        stNew.setDesktopEnabled(convertOldState(st));
        
        List<List> result = new ArrayList<>();
        result.add(new Notification(hlNew).toList());
        result.add(new Notification(stNew).toList());
        return result;
    }
    
    private static Notification.State convertOldState(String input) {
        switch (input) {
            case "off": return Notification.State.OFF;
            case "both": return Notification.State.CHANNEL_AND_APP_NOT_ACTIVE;
            case "either": return Notification.State.CHANNEL_OR_APP_NOT_ACTIVE;
            case "app": return Notification.State.APP_NOT_ACTIVE;
            case "channel": return Notification.State.CHANNEL_NOT_ACTIVE;
            case "channelActive": return Notification.State.CHANNEL_ACTIVE;
            case "always": return Notification.State.ALWAYS;
        }
        return Notification.State.OFF;
    }
    
}
