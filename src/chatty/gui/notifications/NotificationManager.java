
package chatty.gui.notifications;

import chatty.Chatty;
import chatty.Helper;
import chatty.User;
import chatty.gui.MainGui;
import chatty.gui.notifications.Notification.State;
import static chatty.gui.notifications.Notification.State.APP_NOT_ACTIVE;
import static chatty.gui.notifications.Notification.State.CHANNEL_ACTIVE;
import static chatty.gui.notifications.Notification.State.CHANNEL_AND_APP_NOT_ACTIVE;
import static chatty.gui.notifications.Notification.State.CHANNEL_NOT_ACTIVE;
import static chatty.gui.notifications.Notification.State.CHANNEL_OR_APP_NOT_ACTIVE;
import static chatty.gui.notifications.Notification.State.OFF;
import chatty.gui.notifications.Notification.Type;
import chatty.util.Sound;
import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.StreamInfo;
import chatty.util.settings.Settings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tduva
 */
public class NotificationManager {
    
    private final Settings settings;
    private final MainGui main;
    
    private final List<Notification> properties = new ArrayList<>();
    private static final String SETTING_NAME = "notifications";
    public static final String COLOR_PRESETS_SETTING_NAME = "nColorPresets";
    
    public NotificationManager(MainGui main,
            Settings settings) {
        this.settings = settings;
        this.main = main;
        loadFromSettings();
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
        List<List> entriesToSave = new ArrayList<>();
        for (Notification p : properties) {
            entriesToSave.add(p.toList());
        }
        settings.putList(SETTING_NAME, entriesToSave);
    }
    
    private synchronized void loadFromSettings() {
        List<List> entriesToLoad = settings.getList(SETTING_NAME);
        properties.clear();
        for (List l : entriesToLoad) {
            Notification p = Notification.fromList(l);
            if (p != null) {
                properties.add(p);
            }
        }
    }
    
    public void streamInfoChanged(String channel, StreamInfo info) {
        check(Type.STREAM_STATUS, "#"+info.getStream(), n -> {
            if (info.getOnline() || !n.hasOption("noOffline")) {
                return new NotificationData("[Status] "+channel, info.getFullStatus());
            } 
            return null;
        });
    }
    
    public void highlight(User user, String message, boolean noNotify,
            boolean noSound) {
        check(null, user.getChannel(), user, message, noNotify, noSound, n -> {
            if (n.type == Type.HIGHLIGHT) {
                String title = String.format("[Highlight] %s in %s",
                        getDisplayName(user), user.getChannel());
                return new NotificationData(title, message);
            } else if (n.type == Type.MESSAGE) {
                String title = String.format("[Message] %s in %s",
                        getDisplayName(user), user.getChannel());
                return new NotificationData(title, message);
            } else if (n.type == Type.WHISPER) {
                String title = String.format("[Whisper] %s",
                        getDisplayName(user));
                return new NotificationData(title, message);
            }
            return null;
        });
    }
    
    public void message(User user, String message) {
        check(Type.MESSAGE, user.getChannel(), user, message, n -> {
            String title = String.format("[Message] %s in %s",
                    getDisplayName(user), user.getChannel());
            return new NotificationData(title, message);
        });
    }
    
    public void whisper(User user, String message) {
        check(Type.WHISPER, null, user, message, n -> {
            String title = String.format("[Whisper] %s",
                    getDisplayName(user));
            return new NotificationData(title, message);
        });
    }
    
    public void userJoined(User user) {
        check(Type.JOIN, user.getChannel(), user, user.getName(), n -> {
            String title = String.format("[Join] %s in %s",
                    getDisplayName(user), user.getChannel());
            return new NotificationData(title, "");
        });
    }
    
    public void userLeft(User user) {
        check(Type.PART, user.getChannel(), user, user.getName(), n -> {
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
    
    public void newSubscriber(User user, String systemMsg, String message) {
        check(Type.SUBSCRIBER, user.getChannel(), c -> {
            String title = String.format("[Subscriber] %s in %s",
                    user.getDisplayNick(),
                    user.getChannel());
            String text = systemMsg;
            if (message != null && !message.isEmpty()) {
                text = text+" ["+message+"]";
            }
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
        check(type, channel, null, null, false, false, c);
    }
    
    private void check(Type type, String channel, User user, String message,
            NotificationChecker c) {
        check(type, channel, user, message, false, false, c);
    }
    
    private void check(Type type, String channel, User user,
            String message, boolean noNotify, boolean noSound,
            NotificationChecker c) {
        boolean shown = false;
        boolean played = false;
        for (Notification n : properties) {
            if (n.hasEnabled()
                    && (type == n.type || type == null)
                    && n.matchesChannel(channel)
                    && n.matches(user, message)) {
                
                NotificationData d = c.check(n);
                if (d != null) {
                    if (!shown
                            && !noNotify
                            && checkRequirements(n.desktopState, channel)) {
                        shown = true;
                        showNotification(n, d.title, d.message, channel, channel);
                    }
                    if (!played
                            && !noSound
                            && checkRequirements(n.soundState, channel)
                            && n.hasSound()) {
                        played = true;
                        // This may not actually play the sound, if waiting for
                        // cooldown
                        playSound(n, channel);
                    }
                    n.setMatched();
                }
            }
        }
    }
    
    public boolean showNotification(Notification n, String title, String message,
            String data, String channel) {
        main.showNotification(title, message, n.foregroundColor, n.backgroundColor, data);
        return true;
    }
    
    private boolean playSound(Notification n, String channel) {
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
        
        String soundsPath = Chatty.getSoundDirectory();
        if (!settings.getString("soundsPath").isEmpty()) {
            soundsPath = settings.getString("soundsPath");
        }
        Path path = Paths.get(soundsPath, n.soundFile);
        try {
            Sound.play(path, n.soundVolume, "id", 0);
        } catch (Exception ex) {
            // Do nothing further (already logged)
        }
        return true;
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
    
    private String getDisplayName(User user) {
        long displayNamesMode = settings.getLong("displayNamesMode");
        return Helper.makeDisplayNick(user, displayNamesMode);
    }
    
}
