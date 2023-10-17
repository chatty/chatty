
package chatty.gui.components.admin;

import chatty.util.StringUtil;
import chatty.util.api.ChannelStatus.StreamTag;
import chatty.util.api.StreamCategory;
import chatty.util.api.StreamLabels.StreamLabel;
import chatty.util.settings.Settings;
import chatty.util.settings.SettingsListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class StatusHistory implements SettingsListener {
    
    private static final Logger LOGGER = Logger.getLogger(StatusHistory.class.getName());
    
    private static final int DAY = 1000*60*60*24;
    
    private static final int MAX_LOADING_ERRORS = 2;
    
    /**
     * The enties, whereas key and value should be equal, although the value
     * could contain different values for some of the properties, so it should
     * be used when returning a value.
     * 
     * Using a Map to keep entries that are equal (same title/game) unique, but
     * still having the ability to pull the actual value out by a key.
     */
    private final Map<StatusHistoryEntry, StatusHistoryEntry> entries = new HashMap<>();
    
    /**
     * Reference to the Settings.
     */
    private final Settings settings;
    
    /**
     * Create a new {@code StatusHistory} object, load the entries from settings
     * and remove old ones.
     * 
     * The reference to the settings object is used to load/save the entries and
     * get the settings for removing old entries.
     * 
     * @param settings 
     */
    public StatusHistory(Settings settings) {
        this.settings = settings;
        loadFromSettings();
        removeOld();
    }
    
    /**
     * Removes entries that are old as per the settings, except favorites.
     */
    private synchronized void removeOld() {
        if (!settings.getBoolean("statusHistoryClear")) {
            return;
        }
        long days = settings.getLong("statusHistoryKeepDays");
        long keepAfter = System.currentTimeMillis() - days*DAY;
        //System.out.println(keepAfter+" "+(days*DAY));
        Iterator<Map.Entry<StatusHistoryEntry, StatusHistoryEntry>> it = entries.entrySet().iterator();
        int countRemoved = 0;
        while (it.hasNext()) {
            Map.Entry<StatusHistoryEntry, StatusHistoryEntry> entry = it.next();
            // Remove entry if it's not a favorite and older than specified in
            // the setting
            //System.out.println(entry.getValue().lastActivity);
            if (!entry.getValue().favorite && entry.getValue().lastActivity < keepAfter) {
                it.remove();
                countRemoved++;
            }
        }
        if (countRemoved > 0) {
            LOGGER.info("StatusPresets: Removed "+countRemoved+" old entries.");
        }
    }
    
    /**
     * Return the entry for this title and game.
     * 
     * @param title The title
     * @param game The game
     * @param tags
     * @param labels
     * @return 
     */
    public synchronized StatusHistoryEntry get(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels) {
        StatusHistoryEntry entry = new StatusHistoryEntry(title, game, tags, labels);
        return entries.get(entry);
    }
    
    public synchronized boolean isFavorite(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels) {
        StatusHistoryEntry entry = get(title, game, tags, labels);
        return entry != null ? entry.favorite : false;
    }
    
    private void put(StatusHistoryEntry entry) {
        entries.put(entry, entry);
    }
    
    /**
     * Add the entry with the given values.
     * 
     * @param title
     * @param game
     * @param tags
     * @param lastSet
     * @param timesUsed
     * @return 
     */
    public synchronized StatusHistoryEntry add(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels, long lastSet, int timesUsed) {
        StatusHistoryEntry entry = new StatusHistoryEntry(title, game, tags, labels, lastSet, timesUsed, false);
        put(entry);
        return entry;
    }
    
    /**
     * Removes the entry with the given title and game.
     * 
     * @param title
     * @param game 
     * @param tags 
     */
    public synchronized void remove(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels) {
        StatusHistoryEntry entry = new StatusHistoryEntry(title, game, tags, labels);
        entries.remove(entry);
    }
    
    /**
     * Removes the given entry.
     * 
     * @param entry The entry to remove
     */
    public synchronized void remove(StatusHistoryEntry entry) {
        entries.remove(entry);
    }
    
    /**
     * Adds a new entry for the given title and game or modifies the already
     * existing one, if present.
     * 
     * @param title
     * @param game
     * @param tags
     * @return 
     */
    public synchronized StatusHistoryEntry addUsed(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels) {
        StatusHistoryEntry entry = new StatusHistoryEntry(title, game, tags, labels, System.currentTimeMillis(), 1, false);
        StatusHistoryEntry present = entries.get(entry);
        if (present != null) {
            entry = present.increaseUsed();
        }
        put(entry);
        return entry;
    }
    
    /**
     * Adds the given title and game to the favorites, creating a new entry
     * if not already present.
     * 
     * @param title
     * @param game
     * @param tags
     * @param labels
     * @return 
     */
    public synchronized StatusHistoryEntry addFavorite(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels) {
        return setFavorite(title, game, tags, labels, true);
    }
    
    /**
     * Removes the given title and game from the favorites.It actually adds an
     * entry if none is yet present for this, but with the favorite property set
     * to {@code false}.
     *
     * @param title
     * @param game 
     * @param tags 
     * @param labels 
     */
    public synchronized void removeFavorite(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels) {
        setFavorite(title, game, tags, labels, false);
    }
    
    /**
     * Sets the favorite property for the entry with the given title and game.
     * 
     * @param title
     * @param game
     * @param tags
     * @param labels
     * @param favorite
     * @return 
     */
    public synchronized StatusHistoryEntry setFavorite(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels, boolean favorite) {
        StatusHistoryEntry entry = new StatusHistoryEntry(title, game, tags, labels, System.currentTimeMillis(), 0, favorite);
        return setFavorite(entry, favorite);
    }
    
    /**
     * Sets the favorite propery of the given entry. If a matching entry already
     * exists, then the given entry is only used as a key and the present entry
     * is modified, otherwise the given entry is added.
     * 
     * @param entry
     * @param favorite
     * @return 
     */
    public synchronized StatusHistoryEntry setFavorite(StatusHistoryEntry entry, boolean favorite) {
        StatusHistoryEntry present = entries.get(entry);
        if (present != null) {
            entry = present;
        }
        if (entry.favorite != favorite) {
            entry = entry.setFavorite(favorite);
        }
        put(entry);
        return entry;
    }
    
    public synchronized void updateStreamTagName(StreamTag c) {
        Set<StatusHistoryEntry> modifiedValues = new HashSet<>();
        for (StatusHistoryEntry entry : entries.values()) {
            StatusHistoryEntry modified = entry.updateTagName(c);
            if (modified != entry) {
                modifiedValues.add(modified);
            }
        }
        modifiedValues.stream().forEach(e -> entries.put(e, e));
    }
    
    public synchronized void updateCategory(StreamCategory category) {
        Map<StatusHistoryEntry, StatusHistoryEntry> modifiedValues = new HashMap<>();
        for (StatusHistoryEntry entry : entries.values()) {
            StatusHistoryEntry modified = entry.updateCategory(category);
            if (modified != entry) {
                modifiedValues.put(entry, modified);
                LOGGER.info(String.format("Status presets: Updating '%s' (%s to %s)",
                        entry,
                        entry.game.toStringVerbose(),
                        modified.game.toStringVerbose()));
            }
        }
        // After renaming StreamCategory old an new might not be equal()
        modifiedValues.entrySet().stream().forEach(e -> {
            entries.remove(e.getKey());
            entries.put(e.getValue(), e.getValue());
        });
    }

    /**
     * Returns a copy of the entries list.
     * 
     * @return 
     */
    public synchronized List<StatusHistoryEntry> getEntries() {
        return new ArrayList<>(entries.values());
    }
    
    /**
     * Turns all entries into a list and adds them to a list that is saved
     * in the settings.
     */
    private synchronized void saveToSettings() {
        List<List> entriesToSave = new ArrayList<>();
        for (StatusHistoryEntry entry : entries.values()) {
            entriesToSave.add(entryToList(entry));
        }
        settings.putList("statusPresets", entriesToSave);
    }
    
    /**
     * Goes through all entries saved in the settings (as a list of lists) and
     * tries to turn them into {@code StatusHistoryEntry} objects.
     */
    private synchronized void loadFromSettings() {
        List<List> entriesToLoad = settings.getList("statusPresets");
        entries.clear();
        int count = 0;
        int errorCount = 0;
        for (List entryToLoad : entriesToLoad) {
            StatusHistoryEntry entry = listToEntry(entryToLoad);
            if (entry != null) {
                entries.put(entry, entry);
                count++;
            } else {
                if (errorCount < MAX_LOADING_ERRORS) {
                    LOGGER.warning("StatusPresets: Couldn't load entry " + entryToLoad);
                }
                errorCount++;
            }
        }
        if (errorCount > MAX_LOADING_ERRORS) {
            LOGGER.warning("StatusPresets: "+(errorCount - MAX_LOADING_ERRORS)+" more errors.");
        }
        LOGGER.info("StatusPresets: Loaded "+count+"/"+entriesToLoad.size()+" entries.");
    }
    
    /**
     * Turns a {@code StatusHistoryEntry} into a {@code List}, so it can be
     * saved in the settings.
     * 
     * Always returns the same order of elements, so the list can be read again
     * and turned into a {@code StatusHistoryEntry} again.
     * 
     * @param entry The {@code StatusHistoryEntry}
     * @return The {@code List}
     * @see listToEntry(List)
     */
    private List entryToList(StatusHistoryEntry entry) {
        List<Object> list = new ArrayList<>();
        list.add(entry.title);
        list.add(entry.game.name);
        list.add(entry.lastActivity);
        list.add(entry.timesUsed);
        list.add(entry.favorite);
        // Add empty list in between where Communities would have been
        list.add(new ArrayList<>());
        List<List<String>> clist = new ArrayList<>();
        if (entry.tags != null && !entry.tags.isEmpty()) {
            // New format, for several Communities
            for (StreamTag c : entry.tags) {
                List<String> cdata = new ArrayList<>();
                // With new freeform tags only save the name
                cdata.add("");
                cdata.add(c.getName());
                clist.add(cdata);
            }
            /**
             * Used to be:
             * list.add(entry.community.getId());
             * list.add(entry.community.getName());
             */
        }
        list.add(clist);
        list.add(entry.game.id);
        
        List<String> labels = new ArrayList<>();
        if (entry.labels != null && !entry.labels.isEmpty()) {
            for (StreamLabel label : entry.labels) {
                labels.add(label.getId());
            }
        }
        list.add(labels);
        return list;
    }
    
    /**
     * Turns a list into a {@code StatusHistoryEntry}. The list should be:
     * {title, game, lastSet, timesUsed, favorite}
     * 
     * If the list has the wrong size or contains the wrong types no entry is
     * created.
     * 
     * @param list
     * @return The {@code StatusHistoryEntry} or {@code null} if an error
     * occured
     * @see entryToList(StatusHistoryEntry)
     */
    private StatusHistoryEntry listToEntry(List list) {
        try {
            String title = (String) list.get(0);
            String gameName = (String) list.get(1);
            Number lastSet = (Number) list.get(2);
            Number timesUsed = (Number) list.get(3);
            Boolean favorite = (Boolean) list.get(4);
            List<StreamTag> tags = new ArrayList<>();
            List<StreamLabel> labels = new ArrayList<>();
            String gameId = null;
            // Communities were at 5, Tags are now at 6 (to be able to
            // differentiate them)
            if (list.size() > 6) {
                if (list.get(6) instanceof List) {
                    // New format, for several Communities
                    List clist = (List) list.get(6);
                    for (Object obj : clist) {
                        List cdata = (List) obj;
                        // With new freefrom tags the id is not used, and spaces
                        // are not allowed
                        String tagId = (String) cdata.get(0); // Unused now
                        String tagName = (String) cdata.get(1);
                        tags.add(new StreamTag(StringUtil.removeWhitespace(tagName)));
                    }
                }
            }
            if (list.size() > 7) {
                gameId = (String) list.get(7);
            }
            if (list.size() > 8) {
                for (Object obj : (List) list.get(8)) {
                    labels.add(new StreamLabel((String) obj));
                }
            }
            if (title == null || gameName == null) {
                //LOGGER.warning("Didn't load "+list+" (Unexpected null)");
                return null;
            }
            return new StatusHistoryEntry(title, new StreamCategory(gameId, gameName), tags, labels, lastSet.longValue(), timesUsed.intValue(), favorite);
        } catch (ClassCastException | IndexOutOfBoundsException ex) {
            //LOGGER.warning("Didn't load "+list+" ("+ex.getLocalizedMessage()+")");
            return null;
        }
    }

    /**
     * Called when the settings are about to be saved to file, so save to
     * settings now.
     */
    @Override
    public void aboutToSaveSettings(Settings settings) {
        saveToSettings();
    }
    
}
