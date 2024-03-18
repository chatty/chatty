
package chatty.gui.notifications;

import chatty.Addressbook;
import chatty.ChannelFavorites;
import chatty.Chatty;
import chatty.Helper;
import chatty.Room;
import chatty.User;
import chatty.gui.MainGui;
import chatty.gui.components.textpane.InfoMessage;
import chatty.gui.notifications.Notification.State;
import static chatty.gui.notifications.Notification.State.APP_NOT_ACTIVE;
import static chatty.gui.notifications.Notification.State.CHANNEL_ACTIVE;
import static chatty.gui.notifications.Notification.State.CHANNEL_AND_APP_NOT_ACTIVE;
import static chatty.gui.notifications.Notification.State.CHANNEL_NOT_ACTIVE;
import static chatty.gui.notifications.Notification.State.CHANNEL_OR_APP_NOT_ACTIVE;
import static chatty.gui.notifications.Notification.State.OFF;
import chatty.gui.notifications.Notification.Type;
import chatty.gui.notifications.Notification.TypeOption;
import chatty.util.DateTime;
import chatty.util.Sound;
import chatty.util.StringUtil;
import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.StreamInfo;
import chatty.util.history.HistoryUtil;
import chatty.util.irc.MsgTags;
import chatty.util.settings.Settings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tduva
 */
public class NotificationManager {
    
    private final Settings settings;
    private final MainGui main;
    private final Addressbook ab;
    private final ChannelFavorites channelFavorites;
    
    private final List<Notification> properties = new ArrayList<>();
    private static final String SETTING_NAME = "notifications";
    public static final String COLOR_PRESETS_SETTING_NAME = "nColorPresets";

    public NotificationManager(MainGui main,
                               Settings settings,
                               Addressbook ab,
                               ChannelFavorites channelFavorites) {
        this.settings = settings;
        this.main = main;
        this.ab = ab;
        this.channelFavorites = channelFavorites;
        settings.addSettingChangeListener((s, t, v) -> {
            if (s.equals(SETTING_NAME)) {
                loadFromSettings();
            }
        });
    }
    
    public synchronized List<Notification> getData() {
        return new ArrayList<>(properties);
    }
    
    public synchronized void setData(List<Notification> data) {
        properties.clear();
        properties.addAll(data);
        saveToSettings();
    }
    
    private void saveToSettings() {
        List<List<Object>> entriesToSave = new ArrayList<>();
        for (Notification p : properties) {
            entriesToSave.add(p.toList());
        }
        settings.putList(SETTING_NAME, entriesToSave);
    }
    
    public synchronized void loadFromSettings() {
        List<List<Object>> entriesToLoad = settings.getList(SETTING_NAME);
        properties.clear();
        for (List<Object> l : entriesToLoad) {
            Notification p = Notification.fromList(l);
            if (p != null) {
                properties.add(p);
            }
        }
    }
    
    public void streamInfoChanged(String channel, StreamInfo info) {
        check(Type.STREAM_STATUS, "#"+info.getStream(), null, null, null, n -> {
            boolean liveReq = !n.hasOption(TypeOption.LIVE) || info.getOnline();
            boolean newReq = !n.hasOption(TypeOption.NEW_STREAM) || info.getTimeStartedWithPicnicAgo() < 15*60*1000;
            boolean nowLiveReq = !n.hasOption(TypeOption.NOW_LIVE) || info.getPrevLastOnlineAgoSecs() > 15*60;
            boolean gameFavReq = !n.hasOption(TypeOption.FAV_GAME) || settings.listContains("gameFavorites", info.getGame());
            if (liveReq && newReq && nowLiveReq && gameFavReq) {
                String title;
                if (info.getOnline() && !n.hasOption(TypeOption.NO_UPTIME)) {
                    title = String.format("[Status] %2$s (%1$s)",
                            DateTime.agoUptimeCompact2(info.getTimeStartedWithPicnic()),
                            info.getCapitalizedName());
                } else {
                    title = String.format("[Status] %s",
                            info.getCapitalizedName());
                }
                MsgTags tags = MsgTags.create(
                        "title", title,
                        "game", info.getGame());
                if (n.matches(info.getFullStatus(), channel, ab, null, null, tags)) {
                    return new NotificationData(title, info.getFullStatus());
                }
            }
            return null;
        });
    }
    
    public void highlight(User user, User localUser, String message, MsgTags tags, boolean noNotify,
            boolean noSound, boolean isOwnMessage, boolean isWhisper,
            boolean hasBits) {
        check(null, user.getChannel(), user, localUser, message, tags, noNotify, noSound, n -> {
            if (isOwnMessage && !n.hasOption(TypeOption.OWN_MSG)) {
                return null;
            }
            if (!hasBits && n.hasOption(TypeOption.CONTAINS_BITS)) {
                return null;
            }
            if (n.type == Type.HIGHLIGHT) {
                String title = String.format("[Highlight] %s in %s",
                        getDisplayName(user), user.getChannel());
                return new NotificationData(title, message);
            } else if (n.type == Type.MESSAGE) {
                String title = String.format("[Message] %s in %s",
                        getDisplayName(user), user.getChannel());
                return new NotificationData(title, message);
            } else if (n.type == Type.WHISPER && isWhisper) {
                String title = String.format("[Whisper] %s",
                        getDisplayName(user));
                return new NotificationData(title, message);
            }
            return null;
        });
    }
    
    /**
     * Info highlight, although also check as normal info message.
     * 
     * @param room May be null or empty (although not currently used with null)
     * @param message
     * @param noNotify
     * @param noSound 
     */
    public void infoHighlight(Room room, String message, boolean noNotify,
            boolean noSound, User localUser) {
        String channel = room != null ? room.getChannel() : null;
        check(null, channel, null, localUser, message, MsgTags.EMPTY, noNotify, noSound,  n -> {
            boolean hasChannel = channel != null && !channel.isEmpty();
            if (n.type == Type.HIGHLIGHT) {
                String title;
                if (hasChannel) {
                    title = String.format("[Highlight] Info Message in %s",
                            channel);
                } else {
                    title = "[Highlight] Info Message";
                }
                return new NotificationData(title, message);
            } else if (n.type == Type.INFO) {
                String title;
                if (hasChannel) {
                    title = String.format("[Info] %s",
                            channel);
                } else {
                    title = "[Info]";
                }
                return new NotificationData(title, message);
            }
            return null;
        });
    }
    
    /**
     * Non-highlighted info message.
     * 
     * @param room May be null or empty
     * @param text 
     */
    public void info(Room room, String text, User localUser) {
        String channel = room != null ? room.getChannel() : null;
        check(Type.INFO, channel, null, localUser, text, n -> {
            String title;
            if (channel == null || channel.isEmpty()) {
                title = "[Info]";
            } else {
                title = "[Info] "+channel;
            }
            return new NotificationData(title, text);
        });
    }
    
    public void message(User user, User localUser, String message, MsgTags tags,
            boolean isOwnMessage, boolean hasBits) {
        check(Type.MESSAGE, user.getChannel(), user, localUser, message, tags, n -> {
            if (isOwnMessage && !n.hasOption(TypeOption.OWN_MSG)) {
                return null;
            }
            if (!hasBits && n.hasOption(TypeOption.CONTAINS_BITS)) {
                return null;
            }
            String title = String.format("[Message] %s in %s",
                    getDisplayName(user), user.getChannel());
            return new NotificationData(title, message);
        });
    }
    
    public void whisper(User user, User localUser, String message, boolean isOwnMessage) {
        check(Type.WHISPER, null, user, localUser, message, n -> {
            if (isOwnMessage && !n.hasOption(TypeOption.OWN_MSG)) {
                return null;
            }
            String title = String.format("[Whisper] %s",
                    getDisplayName(user));
            return new NotificationData(title, message);
        });
    }
    
    public void userJoined(User user) {
        check(Type.JOIN, user.getChannel(), user, null, user.getName(), n -> {
            String title = String.format("[Join] %s in %s",
                    getDisplayName(user), user.getChannel());
            return new NotificationData(title, "");
        });
    }
    
    public void userLeft(User user) {
        check(Type.PART, user.getChannel(), user, null, user.getName(), n -> {
            String title = String.format("[Part] %s in %s",
                    getDisplayName(user), user.getChannel());
            return new NotificationData(title, "");
        });
    }
    
    public void newFollowers(FollowerInfo info) {
        String channel = Helper.toChannel(info.stream);
        check(Type.NEW_FOLLOWERS, channel, c -> {
            String title = String.format("[New Followers] %s",
                    channel);
            StringBuilder b = new StringBuilder();
            for (Follower f : info.getNewFollowers()) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append(f.display_name);
            }
            return new NotificationData(title, b.toString());
        });
    }
    
    public void newSubscriber(User user, User localUser, String systemMsg, String message) {
        final String text;
        if (message != null && !message.isEmpty()) {
            text = systemMsg + " [" + message + "]";
        } else {
            text = systemMsg;
        }
        check(Type.SUBSCRIBER, user.getChannel(), user, localUser, text, c -> {
            String title = String.format("[Subscriber] %s in %s",
                    user.getDisplayNick(),
                    user.getChannel());
            return new NotificationData(title, text);
        });
    }
    
    public void autoModMessage(String channel, String username, String message) {
        check(Type.AUTOMOD, channel, c -> {
            String title = String.format("[AutoMod] %s in %s",
                    username,
                    channel);
            return new NotificationData(title, message);
        });
    }
    
    public void commandNotification(String channel, String title, String text, boolean noNotify, boolean noSound) {
        check(Type.COMMAND, channel, null, null, text, null, noNotify, noSound, (n) -> {
            if (title != null) {
                return new NotificationData(String.format(title, channel), text);
            }
            return new NotificationData(String.format("[Command] %s", channel), text);
        });
    }
    
    private static interface NotificationChecker {
        public NotificationData check(Notification n);
    }
    
    private static class NotificationData {
        public final String title;
        public final String message;
        
        public NotificationData(String title, String message) {
            this.title = title;
            this.message = message;
        }
        
    }
    
    private void check(Type type, String channel, NotificationChecker c) {
        check(type, channel, null, null, null, MsgTags.EMPTY, false, false, c);
    }
    
    private void check(Type type, String channel, User user, User localUser, String message,
            NotificationChecker c) {
        check(type, channel, user, localUser, message, MsgTags.EMPTY, false, false, c);
    }
    
    private void check(Type type, String channel, User user, User localUser, String message,
            MsgTags tags, NotificationChecker c) {
        check(type, channel, user, localUser, message, tags, false, false, c);
    }
    
    private void check(Type type, String channel, User user, User localUser,
            String message, MsgTags tags, boolean noNotify, boolean noSound,
            NotificationChecker c) {
        boolean shown = false;
        boolean played = false;
        boolean msgShown = false;
        Notification shownNotifiction = null;
        NotificationData shownData = null;
        for (Notification n : properties) {
            if (n.hasEnabled()
                    && (type == n.type || type == null)
                    && n.matchesChannel(channel)
                    && n.matches(message, channel, ab, user, localUser, tags)
                    && checkHistoricAllowMatch(tags, n)
                    && (!n.hasOption(TypeOption.FAV_CHAN) || channelFavorites.isFavorite(channel))
                    && !hideOnStart(n)) {
                
                NotificationData d = c.check(n);
                if (d != null) {
                    if (!shown
                            && !noNotify
                            && checkRequirements(n.desktopState, channel)) {
                        shown = true;
                        shownNotifiction = n;
                        shownData = d;
                        showNotification(n, d.title, d.message, channel, channel);
                    }
                    if (!played
                            && !noSound
                            && checkRequirements(n.soundState, channel)
                            && n.hasSound()) {
                        // Don't remember why it's not using the playSound()
                        // return value, but this is the behavior now
                        played = true;
                        // This may not actually play the sound, if waiting for
                        // cooldown
                        playSound(n);
                    }
                    if (!msgShown
                            && checkRequirements(n.messageState, channel)
                            && !StringUtil.isNullOrEmpty(n.messageTarget)) {
                        msgShown = true;
                        addInfoMsg(n, d, channel, n.messageTarget);
                    }
                    n.setMatched();
                }
            }
        }
        String msgTarget = settings.getString("nInfoMsgTarget");
        if (settings.getBoolean("nInfoMsgEnabled")
                && !StringUtil.isNullOrEmpty(msgTarget)
                && shownNotifiction != null
                && !shownNotifiction.messageOverrideDefault) {
            addInfoMsg(shownNotifiction, shownData, channel, msgTarget);
        }
    }
    
    public boolean showNotification(Notification n, String title, String message,
            String data, String channel) {
        main.showNotification(title, message, n.foregroundColor, n.backgroundColor, new NotificationWindowData(n, data));
        return true;
    }
    
    private boolean playSound(Notification n) {
        // Check stuff, return true only if played
        if (!settings.getBoolean("sounds")) {
            return false;
        }
        if (n.lastPlayedAgo() < n.soundCooldown*1000) {
            return false;
        }
        if (n.lastMatchedAgo() < n.soundInactiveCooldown*1000) {
            return false;
        }
        n.setSoundPlayed();
        
        Chatty.updateCustomPathFromSettings(Chatty.PathType.SOUND);
        Path soundsPath = Chatty.getPath(Chatty.PathType.SOUND);
        Path path = soundsPath.resolve(n.soundFile);
        try {
            Sound.play(path, n.soundVolume, "notification_"+n.type.toString(), 0);
        } catch (Exception ex) {
            // Do nothing further (already logged)
        }
        return true;
    }
    
    private void addInfoMsg(Notification n, NotificationData d, String channel, String targets) {
        
        MsgTags tags = MsgTags.EMPTY;
        String stream = Helper.toValidStream(channel);
        if (!StringUtil.isNullOrEmpty(stream)) {
            int start = StringUtil.toLowerCase(d.title).indexOf(stream);
            if (start != -1) {
                int end = start + stream.length() - 1;
                tags = MsgTags.createLinks(new MsgTags.Link(
                        MsgTags.Link.Type.JOIN,
                        stream,
                        start, end));
            }
        }
        
        InfoMessage msg = new InfoMessage(InfoMessage.Type.INFO, String.format("%s: %s", d.title, d.message), tags);
        msg.routingSource = n;
        if (n.messageUseColor) {
            msg.color = n.foregroundColor;
            msg.bgColor = n.backgroundColor;
            msg.colorSource = n;
        }
        for (String target : StringUtil.split(targets, ',', '"', '"', 0, 2)) {
            if (!StringUtil.isNullOrEmpty(target)) {
                main.routingManager.addNotification(target, channel, msg);
            }
        }
    }
    
    private boolean hideOnStart(Notification n) {
        boolean enabled = settings.getBoolean("nHideOnStart") || n.hasOption(TypeOption.HIDE_ON_START);
        return enabled && Chatty.uptimeSeconds() < 120;
    }

    /**
     * Checks the requirements that depend on whether the app and/or the given
     * channel is active.
     * 
     * @param setting What the requirements are
     * @param channel The channel to check the requirement against
     * @return true if the requirements are met, false otherwise
     */
    private boolean checkRequirements(State setting, String channel) {
        if (setting == null) {
            return false;
        }
        boolean appActive = main.isAppActive();
        boolean channelActive = main.isChanActive(channel);
        // These conditions check when the requirements are NOT met
        if (setting == OFF) {
            return false;
        }
        if (setting == CHANNEL_AND_APP_NOT_ACTIVE && (channelActive || appActive)) {
            return false;
        }
        if (setting == CHANNEL_NOT_ACTIVE && channelActive) {
            return false;
        }
        if (setting == APP_NOT_ACTIVE && appActive) {
            return false;
        }
        if (setting == CHANNEL_OR_APP_NOT_ACTIVE && (channelActive && appActive)) {
            return false;
        }
        if (setting == CHANNEL_ACTIVE && !channelActive) {
            return false;
        }
        return true;
    }
    
    private boolean checkHistoricAllowMatch(MsgTags tags, Notification n) {
        return HistoryUtil.checkAllowMatch(tags, "Notifications", n.getMatcher(), settings);
        
//        if (tags == null || !tags.isHistoricMsg()) {
//            return true;
//        }
//        if (settings.getBoolean("historyMsgNotifications")) {
//            return true;
//        }
//        return n.hasMatcher() && n.getMatcher().matchHistoric();
    }
    
    private String getDisplayName(User user) {
        long displayNamesMode = settings.getLong("displayNamesMode");
        return Helper.makeDisplayNick(user, displayNamesMode);
    }
    
    public static class NotificationWindowData {
        
        public final Notification notification;
        public final String channel;
        
        public NotificationWindowData(Notification notification, String channel) {
            this.notification = notification;
            this.channel = channel;
        }
        
    }
    
}
