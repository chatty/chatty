
package chatty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class ChannelStateManager {
    
    private final Map<String, ChannelState> states = new HashMap<>();
    private final Set<ChannelStateListener> listeners = new HashSet<>();
    
    public ChannelState getState(String channel) {
        synchronized(states) {
            ChannelState s = states.get(channel);
            if (s != null) {
                return s;
            }
            s = new ChannelState(channel);
            states.put(channel, s);
            return s;
        }
    }
    
    public void addListener(ChannelStateListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Tells all listeners that the channel state for this channel has been
     * updated.
     * 
     * @param channel 
     */
    private void updated(String channel) {
        ChannelState s = getState(channel);
        for (ChannelStateListener l : listeners) {
            l.channelStateUpdated(s);
        }
    }
    
    public void setSlowmode(String channel, int length) {
        if (getState(channel).setSlowMode(length)) {
            updated(channel);
        }
    }
    
    /**
     * Parses the length of the slowmode from a String. If the String is null
     * or doesn't contain exclusively one or more numbers, slowmode is
     * considered disabled. If the length is too large to fit into an Integer,
     * it is considered -2 which will display as "> day".
     * 
     * @param channel The channel to set the slowmode for
     * @param length The length as a String with numbers
     */
    public void setSlowmode(String channel, String length) {
        if (length != null && length.matches("^[0-9]+$")) {
            try {
                setSlowmode(channel, Integer.parseInt(length));
            } catch (NumberFormatException ex) {
                // Possibly too big number
                setSlowmode(channel, ChannelState.SLOWMODE_ON_INVALID);
            }
        } else {
            // If not a number, consider off
            setSlowmode(channel, -1);
        }
    }
    
    public void setFollowersOnly(String channel, int minutes) {
        if (getState(channel).setFollowersOnly(minutes)) {
            updated(channel);
        }
    }
    
    public void setFollowersOnly(String channel, String length) {
        if (length != null && length.matches("^-?[0-9]+$")) {
            try {
                setFollowersOnly(channel, Integer.parseInt(length));
            } catch (NumberFormatException ex) {
                setFollowersOnly(channel, ChannelState.SLOWMODE_ON_INVALID);
            }
        } else {
            setSlowmode(channel, -1);
        }
    }
    
    public void setShieldMode(String channel, boolean enabled) {
        if (getState(channel).setShieldMode(enabled)) {
            updated(channel);
        }
    }
    
    public void setSubmode(String channel, boolean enabled) {
        if (getState(channel).setSubMode(enabled)) {
            updated(channel);
        }
    }
    
    public void setR9kMode(String channel, boolean enabled) {
        if (getState(channel).setR9kMode(enabled)) {
            updated(channel);
        }
    }
    
    public void setEmoteOnly(String channel, boolean enabled) {
        if (getState(channel).setEmoteOnly(enabled)) {
            updated(channel);
        }
    }
    
    public void setLang(String channel, String lang) {
        if (getState(channel).setLang(lang)) {
            updated(channel);
        }
    }
    
    /**
     * Reset the channelstate for the given channel.
     * 
     * @param channel 
     */
    public void reset(String channel) {
        if (getState(channel).reset()) {
            updated(channel);
        }
    }
    
    /**
     * Reset all channelstates.
     */
    public void reset() {
        Set<ChannelState> statesCopy;
        synchronized(states) {
            statesCopy = new HashSet<>(states.values());
        }
        for (ChannelState s : statesCopy) {
            if (s.reset()) {
                updated(s.getChannel());
            }
        }
    }
    
    public static interface ChannelStateListener {
        void channelStateUpdated(ChannelState state);
    }
    
}
