
package chatty.gui.notifications;

import chatty.Addressbook;
import chatty.Helper;
import chatty.User;
import chatty.gui.Highlighter;
import chatty.util.colors.HtmlColors;
import chatty.util.StringUtil;
import chatty.util.irc.MsgTags;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Settings for a single notification type/entry.
 * 
 * @author tduva
 */
public class Notification {

    private static final Logger LOGGER = Logger.getLogger(Notification.class.getName());

    public enum Type {
        
        // Type constants must not be renamed, since they are used in setting
        STREAM_STATUS("Stream Status", createStreamStatusSubtypes()),
        HIGHLIGHT("Highlights", createMessageSubtypes()),
        MESSAGE("Chat Message", createMessageSubtypes()),
        INFO("Info Message"),
        WHISPER("Whisper", createMessageSubtypes()),
        JOIN("User Joined"),
        PART("User Left"),
        NEW_FOLLOWERS("New Followers"),
        SUBSCRIBER("Subscriber Notification"),
        AUTOMOD("AutoMod Message"),
        COMMAND("Triggered by command");
        
        public final String label;
        public final List<TypeOption> options;
        
        Type(String name, List<TypeOption> options) {
            this.label = name;
            options.add(TypeOption.HIDE_ON_START);
            this.options = Collections.unmodifiableList(options);
        }
        
        Type(String name) {
            this(name, new ArrayList<>());
        }
        
        private static List<TypeOption> createStreamStatusSubtypes() {
            return new ArrayList<>(Arrays.asList(
                    TypeOption.NOW_LIVE,
                    TypeOption.NEW_STREAM,
                    TypeOption.LIVE,
                    TypeOption.NO_UPTIME,
                    TypeOption.FAV_CHAN,
                    TypeOption.FAV_GAME));
        }
        
        private static List<TypeOption> createMessageSubtypes() {
            return new ArrayList<>(Arrays.asList(
                    TypeOption.OWN_MSG,
                    TypeOption.CONTAINS_BITS,
                    TypeOption.FAV_CHAN));
        }
    }
    
    public enum TypeOption {
        
        // Ids must not be renamed, since they are used in setting
        NO_UPTIME("noUptime"),
        NEW_STREAM("justLive"),
        NOW_LIVE("nowLive"),
        HIDE_ON_START("hideOnStart"),
        LIVE("noOffline"),
        OWN_MSG("own"),
        FAV_CHAN("fav"),
        FAV_GAME("favGame"),
        CONTAINS_BITS("bits");
        
        public String id;
        
        TypeOption(String id) {
            this.id = id;
        }
        
    }
    
    public enum State {
        ALWAYS(1, "Always enabled"),
        OFF(0, "Off"),
        CHANNEL_ACTIVE(2, "Chan focused"),
        CHANNEL_NOT_ACTIVE(3, "Chan not focused"),
        APP_NOT_ACTIVE(4, "App not focused"),
        CHANNEL_OR_APP_NOT_ACTIVE(5, "Chan or app not focused"),
        CHANNEL_AND_APP_NOT_ACTIVE(6, "Chan/app not focused");
        
        
        public String label;
        public int id;
        
        State(int id, String label) {
            this.label = label;
            this.id = id;
        }
        
        public static State getTypeFromId(int triggerStateId) {
            for (State type : values()) {
                if (type.id == triggerStateId) {
                    return type;
                }
            }
            return OFF;
        }
    }
    
    public static class Builder {
        
        private final Type type;
        
        private State soundState = State.OFF;
        private State desktopState = State.OFF;
        private String matcher;
        
        private Color foregroundColor = Color.BLACK;
        private Color backgroundColor = Color.WHITE;
        private int fontSize;
        private String soundFile;
        private long soundVolume = 20;
        private int soundCooldown;
        private int soundInactiveCooldown;
        private List<String> options = new ArrayList<>();
        private String channels;
        
        public Builder(Type type) {
            this.type = type;
        }
        
        public Builder setForeground(Color c) {
            this.foregroundColor = c;
            return this;
        }
        
        public Builder setBackground(Color c) {
            this.backgroundColor = c;
            return this;
        }
        
        public Builder setFontSize(int fontSize) {
            this.fontSize = fontSize;
            return this;
        }
        
        public Builder setVolume(long volume) {
            this.soundVolume = volume;
            return this;
        }
        
        public Builder setSoundCooldown(int cooldown) {
            this.soundCooldown = cooldown;
            return this;
        }
        
        public Builder setSoundInactiveCooldown(int cooldown) {
            this.soundInactiveCooldown = cooldown;
            return this;
        }
        
        public Builder setSoundFile(String file) {
            this.soundFile = file;
            return this;
        }
        
        public Builder setSoundEnabled(State enabled) {
            this.soundState = enabled;
            return this;
        }
        
        public Builder setDesktopEnabled(State enabled) {
            this.desktopState = enabled;
            return this;
        }
        
        public Builder setOptions(List<String> options) {
            this.options = options;
            return this;
        }
        
        public Builder setChannels(String channels) {
            this.channels = channels;
            return this;
        }
        
        public Builder setMatcher(String matcher) {
            this.matcher = matcher;
            return this;
        }
        
    }
    
    public final Type type;
    public final List<String> options;
    private final Set<String> channels;
    public final String matcher;
    private final Highlighter.HighlightItem matcherItem;
    
    // Desktop Notification
    public final State desktopState;
    public final Color foregroundColor;
    public final Color backgroundColor;
    public final int fontSize;
    
    // Sounds Notification
    public final State soundState;
    public final String soundFile;
    public final long soundVolume;
    public final int soundCooldown;
    public final int soundInactiveCooldown;

    // State
    private long lastMatched;
    private long lastSoundPlayed;

    public Notification(Builder builder) {
        
        // Both
        type = builder.type;
        this.options = builder.options;
        channels = parseChannels(builder.channels);
        this.matcher = StringUtil.trim(builder.matcher);
        if (matcher != null && !matcher.isEmpty()) {
            this.matcherItem = new Highlighter.HighlightItem(matcher, "notification");
        } else {
            this.matcherItem = null;
        }
        
        // Desktop
        this.desktopState = builder.desktopState;
        this.foregroundColor = builder.foregroundColor;
        this.backgroundColor = builder.backgroundColor;
        this.fontSize = builder.fontSize;
        
        // Sound
        this.soundState = builder.soundState;
        this.soundFile = builder.soundFile;
        this.soundVolume = builder.soundVolume;
        this.soundCooldown = builder.soundCooldown;
        this.soundInactiveCooldown = builder.soundInactiveCooldown;
    }

    private static Set<String> parseChannels(String channels) {
        if (channels == null) {
            return null;
        }
        Set<String> parsedChannels = Helper.parseChannelsFromString(channels, true);
        if (!parsedChannels.isEmpty()) {
            Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            result.addAll(parsedChannels);
            return result;
        } else {
            return null;
        }
    }

    public String getDesktopState() {
        return desktopState.label;
    }
    
    public String getSoundState() {
        return soundState.label;
    }
    
    public boolean hasEnabled() {
        return desktopState != State.OFF || soundState != State.OFF;
    }

    public void setMatched() {
        this.lastMatched = System.currentTimeMillis();
    }
    
    public void setSoundPlayed() {
        this.lastSoundPlayed = System.currentTimeMillis();
    }
    
    public long lastMatchedAgo() {
        return System.currentTimeMillis() - lastMatched;
    }
    
    public long lastPlayedAgo() {
        return System.currentTimeMillis() - lastSoundPlayed;
    }
    
    public boolean matchesChannel(String channel) {
        if (channel == null) {
            return true;
        }
        if (channels == null) {
            return true;
        }
        return channels.contains(Helper.toChannel(channel));
    }
    
    public boolean matches(String text, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
        if (matcherItem == null || text == null) {
            return true;
        }
        return matcherItem.matches(Highlighter.HighlightItem.Type.ANY, text, -1, -1, null, channel, ab, user, localUser, tags);
    }
    
    public boolean hasChannels() {
        return channels != null && !channels.isEmpty();
    }

    public String serializeChannels() {
        return channels == null ? "" : Helper.buildStreamsString(channels);
    }

    public boolean hasOption(TypeOption option) {
        return options.contains(option.id);
    }
    
    public boolean hasMatcher() {
        return matcherItem != null;
    }
    
    public boolean hasSound() {
        return soundFile != null && !soundFile.isEmpty();
    }
    
    public String getMatcherString() {
        return matcher == null ? "" : matcher;
    }
    
    public List<Object> toList() {
        List<Object> result = new ArrayList<>();
        result.add(type.name());
        result.add(desktopState.id);
        result.add(soundState.id);
        result.add(HtmlColors.getColorString(foregroundColor));
        result.add(HtmlColors.getColorString(backgroundColor));
        result.add(fontSize);
        result.add(options);
        result.add(soundFile);
        result.add(soundVolume);
        result.add(soundCooldown);
        result.add(soundInactiveCooldown);
        result.add(serializeChannels());
        result.add(matcher);
        return result;
    }
    
    public static Notification fromList(List<Object> list) {
        try {
            Type type = Type.valueOf((String)list.get(0));
            State desktopState = State.getTypeFromId(((Number)list.get(1)).intValue());
            State soundState = State.getTypeFromId(((Number)list.get(2)).intValue());
            Color foregroundColor = HtmlColors.decode((String)list.get(3));
            Color backgroundColor = HtmlColors.decode((String)list.get(4));
            int fontSize = ((Number)list.get(5)).intValue();
            List<String> options = getStringList(list.get(6));
            String soundFile = (String)list.get(7);
            long volume = ((Number)list.get(8)).longValue();
            int soundCooldown = ((Number)list.get(9)).intValue();
            int soundInactiveCooldown = ((Number)list.get(10)).intValue();
            String channels = (String)list.get(11);
            String matcher = (String)list.get(12);
            
            Builder b = new Builder(type);
            b.setDesktopEnabled(desktopState);
            b.setSoundEnabled(soundState);
            b.setForeground(foregroundColor);
            b.setBackground(backgroundColor);
            b.setFontSize(fontSize);
            b.setOptions(options);
            b.setSoundFile(soundFile);
            b.setVolume(volume);
            b.setSoundCooldown(soundCooldown);
            b.setSoundInactiveCooldown(soundInactiveCooldown);
            b.setChannels(channels);
            b.setMatcher(matcher);
            return new Notification(b);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing NotificationSettings: "+ex);
        }
        return null;
    }
    
    private static List<String> getStringList(Object o) {
        List<String> result = new ArrayList<>();
        if (o instanceof Collection) {
            for (Object item : (Collection)o) {
                if (item instanceof String) {
                    result.add((String)item);
                }
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "Event "+type.label+", Desktop Notification "+getDesktopState()+", Sound "+getSoundState();
    }
    
}
