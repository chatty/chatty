
package chatty;

import chatty.util.settings.Settings;
import java.util.*;

/**
 * Manages channel favorites and history. Reads and writes directly from the
 * Settings and performs actions like add items, remove items, remove old from
 * history etc.
 * 
 * @author tduva
 */
public class ChannelFavorites {
    
    private static final int DAY = 1000*60*60*24;
    
    private static final String FAVORITES_SETTING = "channelFavorites";
    private static final String HISTORY_SETTING = "channelHistory";
    
    /**
     * The Settings object used throughout the class.
     */
    private final Settings settings;

    /**
     * Construct a new object, requires the Settings object to work on. Deletes
     * old history entries when constructed.
     * 
     * @param settings The Settings object that contains the favorites and
     *  history
     */
    public ChannelFavorites(Settings settings) {
        this.settings = settings;
        if (settings.getBoolean("historyClear")) {
            removeOld();
        }
    }
    
    /**
     * Remove this Set of channels from favorites and history.
     * 
     * @param channels 
     */
    public synchronized void removeChannels(Set<String> channels) {
        removeChannelsFromHistory(channels);
        removeChannelsFromFavorites(channels);
    }
    
    /**
     * Remove this Set of channels from history.
     * 
     * @param channels 
     */
    public synchronized void removeChannelsFromHistory(Set<String> channels) {
        for (String channel : channels) {
            removeChannelFromHistory(channel);
        }
        settings.setSettingChanged("channelHistory");
    }
    
    /**
     * Remove a single channel from history.
     * 
     * @param channel 
     */
    private synchronized void removeChannelFromHistory(String channel) {
        channel = prepareChannel(channel);
        if (channel != null) {
            settings.mapRemove(HISTORY_SETTING, channel);
        }
    }
    
    /**
     * Removes any channels from the history, that are older than the number
     * of days specified in the settings.
     */
    private synchronized void removeOld() {
        long days = settings.getLong("channelHistoryKeepDays");
        Map<String, Long> h = settings.getMap(HISTORY_SETTING);
        long keepAfter = System.currentTimeMillis() - days * DAY;
        Iterator<String> it = h.keySet().iterator();
        while (it.hasNext()) {
            String channel = it.next();
            long time = h.get(channel);
            if (time < keepAfter) {
                it.remove();
            }
        }
        settings.putMap(HISTORY_SETTING, h);
        settings.setSettingChanged("channelHistory");
    }
    
    public synchronized void clearHistory() {
        settings.mapClear(HISTORY_SETTING);
        settings.setSettingChanged("channelHistory");
    }
    
    /**
     * Adds a single channel to history, with the current time.
     * 
     * @param channel 
     */
    public synchronized void addChannelToHistory(String channel) {
        if (!settings.getBoolean("saveChannelHistory")) {
            return;
        }
        channel = prepareChannel(channel);
        if (channel == null) {
            return;
        }
        settings.mapPut(HISTORY_SETTING, channel, System.currentTimeMillis());
        settings.setSettingChanged("channelHistory");
    }
    
    /**
     * Checks if a channel is null or empty and removes any '#' in front of
     * it if necessary.
     * 
     * @param channel
     * @return The channel or null if it was null or empty.
     */
    private synchronized String prepareChannel(String channel) {
        if (channel == null || channel.isEmpty()) {
            return null;
        }
        if (channel.startsWith("#")) {
            channel = channel.substring(1);
            if (channel.isEmpty()) {
                return null;
            }
        }
        return channel;
    }
    
    /**
     * Adds the given Set of channels to favorites.
     * 
     * @param channels 
     */
    public synchronized void addChannelsToFavorites(Set<String> channels) {
        for (String channel : channels) {
            addChannelToFavorites(channel);
        }
        settings.setSettingChanged(FAVORITES_SETTING);
    }
    
    /**
     * Adds the given channel to favorites.
     * 
     * @param channel 
     */
    private void addChannelToFavorites(String channel) {
        channel = prepareChannel(channel);
        if (channel != null) {
            settings.setAdd(FAVORITES_SETTING, channel);
        }
    }
    
    /**
     * Remove the given Set of channels from the favorites.
     * 
     * @param channels 
     */
    public synchronized void removeChannelsFromFavorites(Set<String> channels) {
        for (String channel : channels) {
            removeChannelFromFavorites(channel);
        }
        settings.setSettingChanged(FAVORITES_SETTING);
    }
    
    /**
     * Remove a single channel from favorites.
     * 
     * @param channel 
     */
    private void removeChannelFromFavorites(String channel) {
        channel = prepareChannel(channel);
        if (channel != null) {
            settings.listRemove(FAVORITES_SETTING, channel);
        }
    }

    /**
     * Returns a copy of the current history. A copy, so changes to the history
     * won't affect this Map (thread safety) and it can't be changed from
     * outside.
     * 
     * @return 
     */
    public synchronized Map<String, Long> getHistory() {
        return settings.getMap(HISTORY_SETTING);
    }
    
    /**
     * Returns a copy of the current favorites. A copy, so changes to the
     * favorites won't affect this Set (thread safety) and it can't be changed
     * from outside.
     * 
     * @return 
     */
    public synchronized Set<String> getFavorites() {
        return new HashSet<>(settings.getList(FAVORITES_SETTING));
    }
    
}
