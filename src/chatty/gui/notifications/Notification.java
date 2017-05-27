
package chatty.gui.notifications;

import chatty.Helper;
import chatty.User;
import chatty.gui.Highlighter;
import chatty.gui.HtmlColors;
import chatty.util.StringUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Settings for a single notification type/entry.
 * 
 * @author tduva
 */
public class Notification {

    private static final Logger LOGGER = Logger.getLogger(Notification.class.getName());

    public enum Type {
        
        STREAM_STATUS("Stream Status",createStreamStatusSubtypes()),
        HIGHLIGHT("Highlights"),
        MESSAGE("Chat Message"),
        WHISPER("Whisper"),
        JOIN("User Joined"),
        PART("User Left"),
        NEW_FOLLOWERS("New Followers"),
        SUBSCRIBER("Subscriber Notification"),
        AUTOMOD("AutoMod Message");
        
        public final String label;
        public final Map<String, String> subTypes;
        
        Type(String name, Map<String, String> subTypes) {
            this.label = name;
            this.subTypes = Collections.unmodifiableMap(subTypes);
        }
        
        Type(String name) {
            this(name, new HashMap<>());
        }
        
        private static Map<String, String> createStreamStatusSubtypes() {
            Map<String, String> result = new LinkedHashMap<>();
            result.put("noOffline", "Not when: 'Stream offline'");
            return result;
        }
    }
    
    public enum State {
        ALWAYS(1, "Enabled"),
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
        private String channel;
        
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
        
        public Builder setChannel(String channel) {
            this.channel = channel;
            return this;
        }
        
        public Builder setMatcher(String matcher) {
            this.matcher = matcher;
            return this;
        }
        
    }
    
    public final Type type;
    public final List<String> options;
    public final String channel;
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
    public int soundFileDelay;
    
    // State
    private long lastMatched;
    private long lastSoundPlayed;

    public Notification(Builder builder) {
        
        // Both
        type = builder.type;
        this.options = builder.options;
        String tempChannel = StringUtil.trim(builder.channel);
        this.channel = tempChannel == null || tempChannel.isEmpty() ? null : Helper.toChannel(tempChannel);
        this.matcher = StringUtil.trim(builder.matcher);
        if (matcher != null && !matcher.isEmpty()) {
            this.matcherItem = new Highlighter.HighlightItem(matcher);
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
        if (this.channel == null) {
            return true;
        }
        return channel.equalsIgnoreCase(this.channel);
    }
    
    public boolean matches(User user, String text) {
        if (matcherItem == null || text == null) {
            return true;
        }
        return matcherItem.matches(user, text, StringUtil.toLowerCase(text));
    }
    
    public boolean hasChannel() {
        return channel != null;
    }

    public boolean hasOption(String option) {
        return options.contains(option);
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
    
    public List toList() {
        List result = new ArrayList<>();
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
        result.add(channel);
        result.add(matcher);
        return result;
    }
    
    public static Notification fromList(List list) {
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
            String channel = (String)list.get(11);
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
            b.setChannel(channel);
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
    
}
