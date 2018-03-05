
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
    
    /**
     * The name of the channel that is being hosted. Can be null to indicate no
     * channe is being hosted.
     */
    private String hosting;
    
    private String lang;
    
    // Should be -1, since 0 means on as well
    private int followersOnly = -1;
    
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
        if (setHosting(null)) {
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
    
    /**
     * Set the channel that is being hosted.
     * 
     * @param target The channel that is being hosted, or null to indicate no
     * channe is being hosted
     * @return true if setting this value changed the state, false otherwise
     */
    public synchronized boolean setHosting(String target) {
        if ((hosting == null && target != null)
                || (hosting != null && !hosting.equals(target))) {
            this.hosting = target;
            updateInfo();
            return true;
        }
        return false;
    }
    
    /**
     * Gets the channel name that is being hosted.
     * 
     * @return The name of the hosted channel, or null if none is being hosted
     */
    public synchronized String getHosting() {
        return hosting;
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
        String result = "";
        String sep = "|";
        if (slowMode == SLOWMODE_ON_INVALID || slowMode > 86400) {
            result += "Slow: >day";
        } else if (slowMode > 999) {
            result += "Slow: "+DateTime.duration(slowMode*1000, 1, 0);
        } else if (slowMode > 0) {
            result += "Slow: "+slowMode;
        }
        if (subMode) {
            result = StringUtil.append(result, sep, "Sub");
        }
        if (followersOnly == SLOWMODE_ON_INVALID) {
            result = StringUtil.append(result, sep, "Followers: ?");
        } else if (followersOnly > 0) {
            result = StringUtil.append(result, sep, "Followers: "+DateTime.duration((long)followersOnly*60*1000, 1, DateTime.S, DateTime.Formatting.COMPACT));
        } else if (followersOnly == 0) {
            result = StringUtil.append(result, sep, "Followers");
        }
        if (r9kMode) {
            result = StringUtil.append(result, sep, "r9k");
        }
        if (emoteOnly) {
            result = StringUtil.append(result, sep, "EmoteOnly");
        }
        if (hosting != null && !hosting.isEmpty()) {
            result = StringUtil.append(result, sep, "Hosting: "+hosting);
        }
        if (lang != null && !lang.isEmpty()) {
            result = StringUtil.append(result, sep, lang);
        }
        if (!result.isEmpty()) {
            result = "["+result+"]";
        }
        info = result;
    }
    
}
