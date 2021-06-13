
package chatty;

import chatty.util.settings.Settings;
import java.util.*;

/**
 * Manages channel favorites and history in one setting.
 * 
 * Settings are loaded once and saved when required.
 * 
 * @author tduva
 */
public class ChannelFavorites {
    
    private static final int DAY = 1000*60*60*24;
    
    private static final String SETTING = "roomFavorites";
    
    /**
     * The Settings object used throughout the class.
     */
    private final Settings settings;
    private final RoomManager roomManager;
    
    /**
     * Current data. The Favorite objects contain all necessary data, however
     * they can be looked-up by channel in this Map for easier editing.
     */
    private final Map<String, Favorite> data = new HashMap<>();
    
    private final Set<ChangeListener> listeners = new HashSet<>();

    /**
     * Construct a new object, requires the Settings object to work on. Deletes
     * old history entries when constructed.
     * 
     * @param settings The Settings object that contains the favorites and
     *  history
     * @param roomManager
     */
    public ChannelFavorites(Settings settings, RoomManager roomManager) {
        this.settings = settings;
        this.roomManager = roomManager;
        
        // Load settings
        loadFromSettings();
        if (settings.getBoolean("historyClear")) {
            removeOld();
        }
        
        // Send favorited rooms to Room Manager
        Collection<Room> rooms = new HashSet<>();
        for (Favorite f : data.values()) {
            rooms.add(f.room);
        }
        roomManager.addRoomsIfNone(rooms);
        
        // Listener for saving settings
        settings.addSettingsListener((s) -> {
            saveToSettings();
        });
    }
    
    /**
     * Remove expired entries that are not favorited.
     */
    private synchronized void removeOld() {
        long days = settings.getLong("channelHistoryKeepDays");
        long keepAfter = System.currentTimeMillis() - days * DAY;
        clearHistory(keepAfter);
    }
    
    /**
     * Remove all entries that are not favorited.
     */
    public void clearHistory() {
        // Remove all non-favorited last joined before the current time, so all
        clearHistory(System.currentTimeMillis());
    }
    
    /**
     * Remove all entries that are not favorited and are older (smaller) than
     * the "removeIfBefore" timestamp.
     * 
     * @param removeIfBefore All non-favorited entries before this point in time
     * (smaller number) are removed
     */
    private synchronized void clearHistory(long removeIfBefore) {
        Iterator<Map.Entry<String, Favorite>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Favorite f = it.next().getValue();
            if (!f.isFavorite && f.lastJoined < removeIfBefore) {
                it.remove();
            }
        }
    }
    
    //=========
    // Getting
    //=========
    
    public synchronized Favorite get(String channel) {
        return data.get(channel);
    }
    
    public synchronized List<Favorite> getAll() {
        return new ArrayList<>(data.values());
    }
    
    public synchronized Set<String> getFavorites() {
        Set<String> result = new HashSet<>();
        for (Favorite f : data.values()) {
            if (f.isFavorite) {
                result.add(f.getChannel());
            }
        }
        return result;
    }
    
    public synchronized boolean isFavorite(String channel) {
        channel = Helper.toChannel(channel);
        if (channel == null) {
            return false;
        }
        for (Favorite f : data.values()) {
            if (f.isFavorite && f.getChannel().equals(channel)) {
                return true;
            }
        }
        return false;
    }
    
    //===========
    // Favorites
    //===========
    
    public synchronized Favorite addFavorite(Favorite favorite) {
        Favorite existing = data.get(favorite.getChannel());
        if (existing != null) {
            return set(existing.setFavorite(true));
        }
        return set(favorite.setFavorite(true));
    }
    
    public synchronized Favorite addFavorite(String channel) {
        if (!Helper.isValidChannel(channel)) {
            return null;
        }
        Room room = roomManager.getRoom(Helper.toChannel(channel));
        return addFavorite(room);
    }
    
    public synchronized Favorite addFavorite(Room room) {
        return setFavorite(room, true);
    }
    
    /**
     * Remove the given Room as favorite, leaving the history entry (which may
     * be automatically removed at next start, depending on settings).
     * 
     * @param room
     * @return 
     */
    public synchronized Favorite removeFavorite(Room room) {
        return setFavorite(room, false);
    }
    
    /**
     * Remove the given channel as favorite, leaving the history entry (which
     * may be automatically removed at next start, depending on settings).
     * 
     * @param channel
     * @return 
     */
    public synchronized Favorite removeFavorite(String channel) {
        if (!Helper.isValidChannel(channel)) {
            return null;
        }
        Room room = roomManager.getRoom(Helper.toChannel(channel));
        return removeFavorite(room);
    }

    public synchronized Favorite setFavorite(Room room, boolean isFavorite) {
        Favorite existing = data.get(room.getChannel());
        if (existing != null) {
            // Change isFavorite state of existing entry
            return set(existing.setFavorite(isFavorite));
        } else if (isFavorite) {
            // Create new entry if favoriting
            return set(new Favorite(room, -1, isFavorite));
        }
        // If setFavorite(<no existing entry>, false) then don't do anything
        return null;
    }
    
    //=========
    // History
    //=========

    /**
     * Set the given Room as joined in the history.
     * 
     * @param room 
     */
    public synchronized void addJoined(Room room) {
        if (!settings.getBoolean("saveChannelHistory")) {
            return;
        }
        
        Favorite existing = data.get(room.getChannel());
        boolean isFavorite = false;
        if (existing != null) {
            isFavorite = existing.isFavorite;
        }
        set(new Favorite(room, System.currentTimeMillis(), isFavorite));
    }
    
    /**
     * Remove the entry associated with the channel from the given Favorite (it
     * doesn't have to be the same actual Favorite object).
     *
     * @param favorite
     * @return 
     */
    public synchronized Favorite remove(Favorite favorite) {
        if (data.containsKey(favorite.getChannel())) {
            Favorite removed = data.remove(favorite.getChannel());
            if (removed.isFavorite) {
                informListeners();
            }
            return removed;
        }
        return null;
    }
    
    //========
    // Helper
    //========
    
    /**
     * Store the given Favorite object in the current data, potentially
     * replacing an already existing one.
     *
     * @param fav The Favorite object to store
     * @return The Favorite object itself
     */
    private Favorite set(Favorite fav) {
        Favorite prev = data.put(fav.getChannel(), fav);
        if ((prev == null && fav.isFavorite)
                || (prev != null && fav.isFavorite != prev.isFavorite)) {
            informListeners();
        }
        return fav;
    }
    
    //==========
    // Settings
    //==========
    
    private synchronized void loadFromSettings() {
        data.clear();
        Map<String, List> entries = settings.getMap(SETTING);
        for (String channel : entries.keySet()) {
            List entryValues = entries.get(channel);
            Favorite fav = Favorite.fromList(entryValues, channel);
            if (fav != null) {
                data.put(fav.room.getChannel(), fav);
            }
        }
        informListeners();
    }
    
    private synchronized void saveToSettings() {
        Map<String, List> entries = new HashMap<>();
        for (Favorite f : data.values()) {
            List list = f.toList();
            entries.put(f.getChannel(), list);
        }
        settings.putMap(SETTING, entries);
    }
    
    //================
    // Favorite Class
    //================
    
    public static class Favorite {
        
        public final Room room;
        public final long lastJoined;
        public final boolean isFavorite;

        public Favorite(Room room, long lastJoined, boolean isFavorite) {
            this.room = room;
            this.lastJoined = lastJoined;
            this.isFavorite = isFavorite;
        }
        
        public String getChannel() {
            return room.getChannel();
        }
        
        /**
         * Turn this favorite into a list, except for the channel.
         * 
         * @param input
         * @param channel
         * @return 
         */
        public static Favorite fromList(List input, String channel) {
            long lastJoined = ((Number)input.get(0)).longValue();
            boolean isFavorite = (Boolean)input.get(1);
            if (input.size() == 2) {
                return new Favorite(Room.createRegular(channel), lastJoined, isFavorite);
            } else if (input.size() == 3) {
                String ownerId = (String)input.get(2);
                return new Favorite(Room.createRegularWithId(channel, ownerId), lastJoined, isFavorite);
            } else if (input.size() == 4) {
                String name = (String)input.get(2);
                String ownerStream = (String)input.get(3);
                Room room = Room.createFromChannel(channel, name, Helper.toChannel(ownerStream));
                if (room != null) {
                    return new Favorite(room, lastJoined, isFavorite);
                }
            }
            return null;
        }
        
        public List toList() {
            List result = new ArrayList<>();
            result.add(lastJoined);
            result.add(isFavorite);
            if (room.hasOwnerChannel() && room.hasStream() && !room.isOwner()) {
                result.add(room.getName());
                result.add(room.getStream());
            } else if (room.getStreamId() != null) {
                result.add(room.getStreamId());
            }
            return result;
        }
        
        public Favorite setFavorite(boolean isFavorite) {
            if (this.isFavorite == isFavorite) {
                return this;
            }
            return new Favorite(this.room, this.lastJoined, isFavorite);
        }
        
        public Favorite setJoined(long lastJoined) {
            return new Favorite(room, lastJoined, isFavorite);
        }
        
        @Override
        public String toString() {
            return room.toString();
        }
        
    }
    
    //===========
    // Listeners
    //===========
    
    public synchronized void addChangeListener(ChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    public synchronized void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void informListeners() {
        for (ChangeListener listener : listeners) {
            listener.favoritesChanged();
        }
    }
    
    public interface ChangeListener {
        
        /**
         * When actual favorites (not history) change (added/removed).
         * 
         * This will be run in the ChannelFavorites instance lock.
         */
        public void favoritesChanged();
        
    }
    
}
