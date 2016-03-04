
package chatty;

import chatty.gui.WindowStateManager;
import chatty.util.BackupManager;
import chatty.util.DateTime;
import chatty.util.hotkeys.Hotkey;
import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class SettingsManager {
    
    private static final Logger LOGGER = Logger.getLogger(SettingsManager.class.getName());
    
    private final Settings settings;
    private final BackupManager backup;
    
    private final List<DefaultHotkey> hotkeys = new ArrayList<>();
    
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
    
    public SettingsManager(Settings settings) {
        this.settings = settings;
        backup = new BackupManager(Paths.get(Chatty.getBackupDirectory()),
            Paths.get(Chatty.getUserDataDirectory()));
    }
    
    /**
     * Defines what settings there are and their default values.
     */
    void defineSettings() {
        
        // Additional files (in addition to the default file)
        String loginFile = Chatty.getUserDataDirectory()+"login";
        String historyFile = Chatty.getUserDataDirectory()+"favoritesAndHistory";
        String statusPresetsFile = Chatty.getUserDataDirectory()+"statusPresets";
        
        backup.addFile("settings");
        backup.addFile(historyFile);
        backup.addFile(statusPresetsFile);
        backup.addFile("addressbook");
        
        settings.addFile(loginFile);
        settings.addFile(historyFile);
        settings.addFile(statusPresetsFile);
        
        // Global
        settings.addBoolean("ontop", false);
        settings.addBoolean("dontSaveSettings",false);
        settings.addBoolean("debugCommands", false, false);
        settings.addString("laf","default");
        
        addDefaultHotkeyAppWide("0.7.3", "dialog.streams", "ctrl L");
        addDefaultHotkey("0.7.3", "dialog.toggleEmotes", "ctrl E");
        addDefaultHotkey("0.7.3", "dialog.search", "ctrl F");
        addDefaultHotkey("0.7.3", "dialog.joinChannel", "ctrl J");
        addDefaultHotkey("0.7.3", "window.toggleUserlist", "shift F10");
        addDefaultHotkey("0.7.3", "window.toggleInput", "ctrl F10");
        addDefaultHotkey("0.7.3", "window.toggleCompact", "F10");
        addDefaultHotkey("0.7.3", "window.toggleCompactMaximized", "F11");
        addDefaultHotkey("0.7.3", "tabs.close", "ctrl W");
        addDefaultHotkeyAppWide("0.7.3", "tabs.next", "ctrl TAB");
        addDefaultHotkeyAppWide("0.7.3", "tabs.previous", "ctrl shift TAB");
        addDefaultHotkey("0.7.3", "selection.toggle", "ctrl SPACE");
        addDefaultHotkey("0.7.3", "selection.toggle", "ctrl S");
        settings.addList("hotkeys", getDefaultHotkeySettingValue(), Setting.LIST);
        settings.addBoolean("globalHotkeysEnabled", true);
        
        // Connecting/Login data
        settings.addString("serverDefault", "irc.twitch.tv");
        settings.addString("portDefault", "6667,443");
        // Seperate settings for commandline/temp so others can be saved
        settings.addString("server", "", false);
        settings.addString("port", "", false);
        settings.addList("securedPorts", new ArrayList<>(Arrays.asList((long)6697)), Setting.LONG);
        settings.addList("securedPortsWhisper", new ArrayList<>(Arrays.asList()), Setting.LONG);
        
        settings.addBoolean("userlistConnection", true);
        settings.addList("userlistConnectionBlacklist", new ArrayList(), Setting.STRING);
        settings.addBoolean("membershipEnabled", true);
        
        settings.addBoolean("botBadgeEnabled", true);
        settings.addBoolean("botNamesBTTV", true);
        settings.addBoolean("botNamesFFZ", true);
        settings.addList("botNames", new ArrayList<>(Arrays.asList("nightbot", "moobot", "kabukibot", "slowton2", "xanbot")), Setting.STRING);
        
        settings.addBoolean("debugLogIrcFile", false);
        
        settings.addBoolean("tc3", false);
        
        settings.addString("channel", "");
        settings.addString("username", "");
        settings.setFile("username", loginFile);
        settings.addString("password", "", false);
        settings.addBoolean("connectOnStartup", false, false);
        settings.addLong("onStart", 1);
        settings.addString("autojoinChannel", "");
        settings.addString("previousChannel", "");
        settings.addString("token","");
        settings.setFile("token", loginFile);
        // Don't save setting, login with password isn't possible anymore
        settings.addBoolean("usePassword", false, false);
        // Scopes
        settings.addBoolean("token_editor", false);
        settings.setFile("token_editor", loginFile);
        settings.addBoolean("token_commercials", false);
        settings.setFile("token_commercials", loginFile);
        settings.addBoolean("token_user", false);
        settings.setFile("token_user", loginFile);
        settings.addBoolean("token_subs", false);
        settings.setFile("token_subs", loginFile);
        settings.addBoolean("token_chat", false);
        settings.setFile("token_chat", loginFile);
        
        // Chat messages
        // Not really used anymore, kept for compatability
        settings.addBoolean("ignoreJoinsParts",false);
        settings.addBoolean("showJoinsParts", false);
        settings.addBoolean("showModMessages", false);
        settings.addBoolean("debugLogIrc", false);
        settings.addBoolean("showBanMessages", false);
        settings.addBoolean("combineBanMessages", true);
        settings.addBoolean("deleteMessages", false);
        settings.addString("deletedMessagesMode", "keepShortened");
        settings.addLong("deletedMessagesMaxLength", 50);
        settings.addBoolean("clearChatOnChannelCleared", false);
        settings.addLong("bufferSize", 500);
        settings.addBoolean("twitchnotifyAsInfo", true);
        settings.addBoolean("printStreamStatus", true);
        settings.addLong("filterCombiningCharacters", Helper.FILTER_COMBINING_CHARACTERS_LENIENT);
        
        // Chat appearance
        settings.addBoolean("emoticonsEnabled",true);
        settings.addLong("emoteMaxHeight", 0);
        settings.addLong("emoteScale", 100);
        settings.addLong("emoteScaleDialog", 100);
        settings.addBoolean("closeEmoteDialogOnDoubleClick", false);
        settings.addBoolean("usericonsEnabled",true);
        settings.addString("font","Consolas");
        settings.addLong("fontSize",14);
        settings.addString("inputFont", "Dialog 14");
        settings.addLong("lineSpacing", 3);
        settings.addString("timestamp","[HH:mm]");
        settings.addString("timestampTimezone", "");
        settings.addBoolean("capitalizedNames", true);
        settings.addBoolean("ircv3CapitalizedNames", true);
        settings.addBoolean("correctlyCapitalizedNames", false);
        settings.addMap("customNames", new HashMap<>(), Setting.STRING);
        settings.addBoolean("ffz", true);
        settings.addBoolean("ffzModIcon", true);
        settings.addBoolean("bttvEmotes", true);
        settings.addBoolean("showAnimatedEmotes", false);
        settings.addList("ignoredEmotes", new ArrayList(), Setting.STRING);
        settings.addList("favoriteEmotes", new ArrayList(), Setting.LIST);
        settings.addString("tabOrder", "normal");
        settings.addBoolean("tabsMwheelScrolling", false);
        settings.addBoolean("tabsMwheelScrollingAnywhere", false);
        settings.addBoolean("actionColored", false);
        
        settings.addBoolean("customUsercolors", false);
        settings.addList("usercolors", new LinkedList(), Setting.STRING);
        
        settings.addList("customUsericons", new ArrayList(), Setting.LIST);
        settings.addBoolean("customUsericonsEnabled", false);
        
        settings.addLong("dialogFontSize", -1);
        
        settings.addBoolean("imageCache", true);
        
        // Colors
        settings.addString("foregroundColor","#111111");
        settings.addString("backgroundColor","#FAFAFA");
        settings.addString("infoColor","#001480");
        settings.addString("compactColor","#A0A0A0");
        settings.addString("inputBackgroundColor","#FFFFFF");
        settings.addString("inputForegroundColor","#000000");
        settings.addString("highlightColor","#FF0000");
        settings.addString("searchResultColor", "LightYellow");
        settings.addString("searchResultColor2", "#FFFF80");
        settings.addBoolean("colorCorrection", true);
        
        
        // History and Favorites
        settings.addMap("channelHistory",new TreeMap(), Setting.LONG);
        settings.setFile("channelHistory", historyFile);
        settings.addList("channelFavorites", new ArrayList(), Setting.STRING);
        settings.setFile("channelFavorites", historyFile);
        settings.addList("gamesFavorites",new ArrayList(), Setting.STRING);
        settings.setFile("gamesFavorites", historyFile);
        settings.addLong("channelHistoryKeepDays", 30);
        settings.addBoolean("saveChannelHistory", true);
        settings.addBoolean("historyClear", true);
        settings.addLong("favoritesSorting", 20);
        
        settings.addList("statusPresets", new ArrayList(), Setting.LIST);
        settings.setFile("statusPresets", statusPresetsFile);

        settings.addBoolean("saveStatusHistory", true);
        settings.addBoolean("statusHistoryClear", true);
        settings.addLong("statusHistoryKeepDays", 30);
        settings.addString("statusHistorySorting", "");
        
        // Commercials
        settings.addString("commercialHotkey","");
        settings.addBoolean("adDelay", false);
        settings.addLong("adDelayLength", 300);
        
        // Other
        settings.addBoolean("channelsWarning", true);
        settings.addBoolean("autoScroll", true);
        settings.addLong("autoScrollTimeout", 30);
        settings.addBoolean("pauseChatOnMouseMove", false);
        settings.addBoolean("pauseChatOnMouseMoveCtrlRequired", false);
        settings.addString("commandOnCtrlClick", "");
        settings.addLong("versionLastChecked", 0);
        settings.addString("updateAvailable", "");
        settings.addBoolean("checkNewVersion", true);
        settings.addString("liveStreamsSorting", "recent");
        settings.addLong("historyRange", 0);
        settings.addString("spamProtection", "18/30");

        settings.addString("currentVersion", "");
        
        settings.addBoolean("urlPrompt", true);

        // Window
        settings.addBoolean("attachedWindows", false);
        settings.addBoolean("maximized", false);
        settings.addBoolean("nod3d", true);
        settings.addBoolean("noddraw", false);
        settings.addBoolean("bufferStrategy1", false);
        settings.addBoolean("mainResizable", true);
        
        settings.addBoolean("closeToTray", false);
        settings.addBoolean("minimizeToTray", false);
        
        settings.addMap("windows", new HashMap<>(), Setting.STRING);
        settings.addLong("restoreMode", WindowStateManager.RESTORE_ON_START);
        
        settings.addBoolean("popoutSaveAttributes", true);
        settings.addBoolean("popoutCloseLastChannel", true);
        settings.addList("popoutAttributes", new ArrayList(), Setting.STRING);
        
        settings.addBoolean("simpleTitle", false);
        settings.addBoolean("titleShowUptime", true);
        settings.addBoolean("titleShowViewerCount", true);
        settings.addBoolean("titleShowChannelState", true);
        settings.addBoolean("chatScrollbarAlways", false);
        settings.addLong("userlistWidth", 120);
        settings.addLong("userlistMinWidth", 0);

        settings.addBoolean("statusBar", false);
        
        // Highlight
        settings.addList("highlight",new ArrayList(), Setting.STRING);
        settings.addBoolean("highlightEnabled", true);
        settings.addBoolean("highlightUsername", true);
        settings.addBoolean("highlightOwnText", false);
        settings.addBoolean("highlightNextMessages", false);
        settings.addBoolean("highlightIgnored", false);
        settings.addList("noHighlightUsers", new ArrayList(), Setting.STRING);
        
        // Ignore
        settings.addList("ignore", new ArrayList(), Setting.STRING);
        settings.addBoolean("ignoreEnabled", false);
        settings.addBoolean("ignoreOwnText", false);
        settings.addLong("ignoreMode", 1);
        settings.addBoolean("ignoreShowNotDialog", false);
        settings.addList("ignoredUsers", new ArrayList(), Setting.STRING);
        settings.addList("ignoredUsersWhisper", new ArrayList(), Setting.STRING);
        
        // Sounds
        settings.addBoolean("sounds", false);
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
        
        // Notifications
        settings.addString("highlightNotification", "either");
        settings.addString("statusNotification", "either");
        settings.addBoolean("ignoreOfflineNotifications", false);
        settings.addBoolean("requestFollowedStreams", true);
        
        settings.addBoolean("useCustomNotifications", true);
        
        settings.addLong("nType", 0);
        settings.addLong("nScreen", -1);
        settings.addLong("nPosition", 3);
        settings.addLong("nDisplayTime", 10);
        settings.addLong("nMaxDisplayTime", 60*30);
        settings.addLong("nMaxDisplayed", 4);
        settings.addLong("nMaxQueueSize", 4);
        settings.addBoolean("nActivity", false);
        settings.addLong("nActivityTime", 10);
        
        settings.addList("notificationProperties", new ArrayList<>(), Setting.LIST);
        
        settings.addLong("v0.5", 0);
        settings.addBoolean("tips", true);
        settings.addLong("lastTip", 0);
        
        // Logging
        settings.addString("logMode", "always");
        settings.addBoolean("logMod", true);
        settings.addBoolean("logJoinPart", false);
        settings.addBoolean("logBan", true);
        settings.addBoolean("logSystem", false);
        settings.addBoolean("logInfo", true);
        settings.addBoolean("logViewerstats", true);
        settings.addBoolean("logViewercount", false);
        settings.addList("logWhitelist",new ArrayList(), Setting.STRING);
        settings.addList("logBlacklist",new ArrayList(), Setting.STRING);
        settings.addString("logPath", "");
        settings.addString("logTimestamp", "[HH:mm:ss]");

        // Addressbook
        settings.addString("abCommandsChannel", "");
        settings.addString("abCommands", "add,set,remove");
        settings.addString("abUniqueCats", "");
        settings.addBoolean("abAutoImport", false);

        // Backup
        settings.addLong("backupDelay", 1);
        settings.addLong("backupCount", 5);
        
        // Livestreamer
        settings.addBoolean("livestreamer", false);
        settings.addString("livestreamerQualities", "Best, Worst, Select");
        settings.addString("livestreamerCommand", "livestreamer");
        settings.addBoolean("livestreamerUseAuth", false);
        settings.addBoolean("livestreamerShowDialog", true);
        
        // Custom Commands and Menu Customization
        settings.addList("commands", new ArrayList(), Setting.STRING);
        // Default entries, will only be set if setting is not loaded from file
        settings.setAdd("commands", "/slap /me slaps $$1- around a bit with a large trout");
        settings.setAdd("commands", "/permit !permit $$1");
        
        settings.addString("timeoutButtons","/Ban[B], /Unban[U], 5s[1], 2m[2], 10m[3], 30m[4]");
        settings.addString("userContextMenu", "");
        settings.addString("channelContextMenu", "");
        
        settings.addBoolean("enableStatusWriter", false);
        settings.addString("statusWriter", "");
        
        settings.addBoolean("autoRequestMods", false);
        
        settings.addLong("streamChatMessageTimeout", -1);
        settings.addList("streamChatChannels", new ArrayList(), Setting.STRING);
        settings.addBoolean("streamChatBottom", true);
        settings.addBoolean("streamChatResizable", true);
        
        settings.addString("streamHighlightCommand", "!addstreamhighlight");
        settings.addString("streamHighlightChannel", "");
        settings.addBoolean("streamHighlightChannelRespond", false);
        
        settings.addMap("customCompletion", new HashMap(), Setting.STRING);
        settings.addLong("completionMaxItemsShown", 5);
        settings.addBoolean("completionShowPopup", true);
        settings.addBoolean("completionCommonPrefix", false);
        settings.addString("completionSorting", "predictive");
        
        settings.addBoolean("whisperEnabled", false);
        settings.addBoolean("whisperWhitelist", false);
        settings.addLong("whisperDisplayMode", WhisperConnection.DISPLAY_IN_CHAT);
        settings.addString("groupChatServer", "");
        settings.addString("groupChatPort", "");
        settings.addBoolean("whisperAutoRespond", false);
        
        settings.addBoolean("autoUnhost", false);
        settings.addList("autoUnhostStreams", new ArrayList(), Setting.STRING);
    }
    
    /**
     * Tries to load the settings from file.
     */
    void loadSettingsFromFile() {
        settings.loadSettingsFromJson();
    }
    
    /**
     * Perform backup, specifying the minimum delay the backup should be
     * performed in (seconds). The backup manager will decide whether to
     * actually make a backup.
     */
    void backupFiles() {
        long backupDelay = DateTime.DAY * settings.getLong("backupDelay");
        backup.performBackup((int)backupDelay, (int)settings.getLong("backupCount"));
    }
    
    /**
     * Goes through the commandline options and sets the settings accordingly.
     * 
     * Commandline options consist of key=value pairs, although empty values
     * are possible.
     * 
     * @param args Map with commandline settings, key=value pairs
     */
    void loadCommandLineSettings(Map<String, String> args) {
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
                        settings.setString("token", value);
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
                LOGGER.info("Setting commandline setting: "+settings.setTextual(key.substring(4)+" "+value));
            }
        }
    }
    
    /**
     * Override some now unused settings.
     */
    void overrideSettings() {
        settings.setBoolean("ignoreJoinsParts", false);
        if (beforeVersion("0.7.2")) {
            String value = settings.getString("timeoutButtons");
            if (value.equals("5,2m,10m,30m")) {
                /**
                 * Setting is equal to the old default value, so it probably
                 * wasn't customized, so just reset it to the new default value.
                 */
                settings.setString("timeoutButtons", null);
                LOGGER.warning("Updated timeoutButtons setting to new default");
            } else if (!value.toLowerCase(Locale.ENGLISH).contains("/ban") &&
                    !value.toLowerCase(Locale.ENGLISH).contains("/unban")) {
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
        if (beforeVersion("0.8.1")) {
            if (settings.getString("portDefault").equals("6667,80")) {
                settings.setString("portDefault", "6667,443");
            }
        }
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
     * @param version The version to check against
     * @return true if the given version is greater than the current version
     */
    private boolean beforeVersion(String version) {
        return Version.compareVersions(settings.getString("currentVersion"), version) == 1;
    }
    
    public void debugSettings() {
        StringBuilder result = new StringBuilder("Settings: ");
        boolean first = true;
        for (String setting : debugSettings) {
            if (!first) {
                result.append(", ");
            } else {
                first = false;
            }
            result.append(setting);
            result.append(":");
            result.append(settings.settingValueToString(setting));
        }
        LOGGER.info(result.toString());
    }
    
    private void addDefaultHotkey(String version, String id, String hotkey) {
        hotkeys.add(new DefaultHotkey(version,
                Arrays.asList(new Object[]{id, hotkey})));
    }
    
    private void addDefaultHotkeyAppWide(String version, String id, String hotkey) {
        hotkeys.add(new DefaultHotkey(version,
                Arrays.asList(new Object[]{id, hotkey, Hotkey.Type.APPLICATION.id})));
    }
    
    private List<List> getDefaultHotkeySettingValue() {
        List<List> data = new ArrayList<>();
        for (DefaultHotkey hotkey : hotkeys) {
            data.add(hotkey.data);
        }
        return data;
    }
    
    private static class DefaultHotkey {
        
        String version;
        List data;

        DefaultHotkey(String version, List data) {
            this.version = version;
            this.data = data;
        }
        
    }
    
}
