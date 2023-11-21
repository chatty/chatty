
package chatty;

import chatty.util.DateTime;
import chatty.util.StringUtil;

/**
 * Holds the state for a single channel, like slowmode, submode and so on.
 * 
 * @author tduva
 */
public class ChannelState {
    
    public static final int SLOWMODE_ON_INVALID = -2;
    
    /**
     * The name of the channel this refers to
     */
    private final String channel;
    
    private boolean subMode;
    
    /**
     * The length of the slodemode. Values smaller than 0 mean slowmode is
     * disabled.
     */
    private int slowMode;
    
    private boolean r9kMode;
    
    private boolean emoteOnly;
    
    private String lang;
    
    // Should be -1, since 0 means on as well
    private int followersOnly = -1;
    
    private boolean shieldMode;
    
    /**
     * Cached info text based on the current state.
     */
    private String info = "";
    
    public ChannelState(String channel) {
        this.channel = channel;
    }
    
    public String getChannel() {
        return channel;
    }
    
    /**
     * Set all state back to their default value (off/disabled).
     * 
     * @return 
     */
    public synchronized boolean reset() {
        boolean changed = false;
        if (setSubMode(false)) {
            changed = true;
        }
        if (setSlowMode(-1)) {
            changed = true;
        }
        if (setR9kMode(false)) {
            changed = true;
        }
        if (setEmoteOnly(false)) {
            changed = true;
        }
        if (setFollowersOnly(-1)) {
            changed = true;
        }
        if (setShieldMode(false)) {
            changed = true;
        }
        return changed;
    }
    
    /**
     * Set sub only mode enabled/disabled.
     * 
     * @param enabled Whether sub only mode is enabled
     * @return true if setting this value changed the state, false otherwise
     */
    public synchronized boolean setSubMode(boolean enabled) {
        if (subMode != enabled) {
            this.subMode = enabled;
            updateInfo();
            return true;
        }
        return false;
    }
    
    /**
     * Gets the current sub only mode state.
     * 
     * @return true if submode is enabled, false otherwise
     */
    public synchronized boolean subMode() {
        return subMode;
    }
    
    /**
     * Set slowmode to the given length. Values smaller than 0 indicate slowmode
     * is disabled.
     * 
     * @param length The length in seconds or -1 or 0 to indicate slowmode is
     * disabled, -2 to indicate slowmode is enabled but too long to fit into an
     * Integer
     * @return true if setting this value changed the state, false otherwise
     */
    public synchronized boolean setSlowMode(int length) {
        if (slowMode != length) {
            this.slowMode = length;
            updateInfo();
            return true;
        }
        return false;
    }
    
    /**
     * Gets the current length for the slowmode.
     * 
     * @return The length of the slowmode in seconds, or a value smaller than 0
     * if no slowmode is enabled (usually -1)
     */
    public synchronized int slowMode() {
        return slowMode;
    }
    
    public synchronized boolean setR9kMode(boolean enabled) {
        if (r9kMode != enabled) {
            r9kMode = enabled;
            updateInfo();
            return true;
        }
        return false;
    }
    
    public synchronized boolean r9kMode() {
        return r9kMode;
    }
    
    public synchronized boolean setEmoteOnly(boolean enabled) {
        if (emoteOnly != enabled) {
            emoteOnly = enabled;
            updateInfo();
            return true;
        }
        return false;
    }
    
    public synchronized boolean emoteOnly() {
        return emoteOnly;
    }
    
    public synchronized boolean setShieldMode(boolean enabled) {
        if (shieldMode != enabled) {
            shieldMode = enabled;
            updateInfo();
            return true;
        }
        return false;
    }
    
    public synchronized boolean shieldMode() {
        return shieldMode;
    }
    
    public synchronized boolean setLang(String lang) {
        if ((this.lang == null && lang != null)
                || (this.lang != null && !this.lang.equals(lang))) {
            this.lang = lang;
            updateInfo();
            return true;
        }
        return false;
    }
    
    public synchronized boolean setFollowersOnly(int minutes) {
        if (followersOnly != minutes) {
            this.followersOnly = minutes;
            updateInfo();
            return true;
        }
        return false;
    }
    
    /**
     * Get the info text based on the current state.
     * 
     * @return The info text, empty if no state is enabled
     */
    public String getInfo() {
        return info;
    }
    
    /**
     * Update the info text once a state has been updated.
     */
    private void updateInfo() {
        StringBuilder resultBuilder = new StringBuilder();

        appendShieldMode(resultBuilder);
        appendSlowMode(resultBuilder);
        appendSubMode(resultBuilder);
        appendFollowersOnly(resultBuilder);
        appendR9kMode(resultBuilder);
        appendEmoteOnly(resultBuilder);
        appendLanguage(resultBuilder);

        String result = resultBuilder.toString();
        if (!result.isEmpty()) {
            info = "[" + result + "]";
        } else {
            info = ""; // Reset info if result is empty
        }
    }

    private void appendShieldMode(StringBuilder resultBuilder) {
        if (shieldMode) {
            StringUtil.append(String.valueOf(resultBuilder), "|", "Shield");
        }
    }

    private void appendSlowMode(StringBuilder resultBuilder) {
        String sep = "|";
        if (slowMode == SLOWMODE_ON_INVALID || slowMode > 86400) {
            StringUtil.append(String.valueOf(resultBuilder), sep, "Slow: >day");
        } else if (slowMode > 999) {
            StringUtil.append(String.valueOf(resultBuilder), sep, "Slow: " + DateTime.duration(slowMode * 1000, 1, 0));
        } else if (slowMode > 0) {
            StringUtil.append(String.valueOf(resultBuilder), sep, "Slow: " + slowMode);
        }
    }

    private void appendSubMode(StringBuilder resultBuilder) {
        if (subMode) {
            StringUtil.append(String.valueOf(resultBuilder), "|", "Sub");
        }
    }

    private void appendFollowersOnly(StringBuilder resultBuilder) {
        String sep = "|";
        if (followersOnly == SLOWMODE_ON_INVALID) {
            StringUtil.append(String.valueOf(resultBuilder), sep, "Followers: ?");
        } else if (followersOnly > 0) {
            StringUtil.append(String.valueOf(resultBuilder), sep, "Followers: " +
                    DateTime.duration((long) followersOnly * 60 * 1000, 1, DateTime.S, DateTime.Formatting.COMPACT));
        } else if (followersOnly == 0) {
            StringUtil.append(String.valueOf(resultBuilder), sep, "Followers");
        }
    }

    private void appendR9kMode(StringBuilder resultBuilder) {
        if (r9kMode) {
            StringUtil.append(String.valueOf(resultBuilder), "|", "r9k");
        }
    }

    private void appendEmoteOnly(StringBuilder resultBuilder) {
        if (emoteOnly) {
            StringUtil.append(String.valueOf(resultBuilder), "|", "EmoteOnly");
        }
    }

    private void appendLanguage(StringBuilder resultBuilder) {
        if (lang != null && !lang.isEmpty()) {
            StringUtil.append(String.valueOf(resultBuilder), "|", lang);
        }
    }



}





