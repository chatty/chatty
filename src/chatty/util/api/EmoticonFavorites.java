
package chatty.util.api;

import chatty.util.SpecialMap;
import chatty.util.StringUtil;
import chatty.util.api.Emoticon.Type;
import chatty.util.settings.Settings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Load favorites from settings, find global emote favorites in loaded emotes.
 * Channel-specific emotes don't have their Emoticon object stored here, since
 * it would change when the channel changes, so it's being looked for on the fly
 * in the Emote Dialog.
 * 
 * @author tduva
 */
public class EmoticonFavorites {
    
    private static final Logger LOGGER = Logger.getLogger(EmoticonFavorites.class.getName());
    
    /**
     * Favorites that have not been matched with loaded emotes yet (or
     * channel-specific emotes). By emote code.
     */
    private final Map<String, Favorite> notFoundByName = new HashMap<>();
    
    /**
     * Favorites that have not been matched with loaded emotes yet (or
     * channel-specific emotes). By emote type/id.
     */
    private final SpecialMap<Type, Map<String, Favorite>> notFoundByTypeId = new SpecialMap<>(new HashMap<>(), () -> new HashMap<>());
    
    /**
     * Favorites of global emotes that have been matches with loaded emotes.
     */
    private final Map<Favorite, Emoticon> favorites = new HashMap<>();
    
    /**
     * Track if settings have been loaded.
     */
    private boolean loadedFavoritesFromSettings;
    
    /**
     * Creates a Favorite object for the given Emoticon.
     * 
     * @param emote The Emoticon to create the Favorite object for
     * @return The created Favorite object
     */
    private Favorite createFavorite(Emoticon emote) {
        if (emote.type == Emoticon.Type.EMOJI) {
            return new Favorite(emote.code, null, null, null);
        }
        return new Favorite(emote.code, emote.emoteset, emote.type, emote.stringId);
    }
    
    /**
     * Loads the favorites from the settings.
     * 
     * @param settings The Settings object
     */
    public void loadFavoritesFromSettings(Settings settings) {
        @SuppressWarnings("unchecked")
        List<List<Object>> entriesToLoad = settings.getList("favoriteEmotes");
        favorites.clear();
        notFoundByName.clear();
        notFoundByTypeId.clear();
        for (List<Object> item : entriesToLoad) {
            Favorite f = listToFavorite(item);
            if (f != null) {
                // By type/id
                if (f.type != null && f.id != null) {
                    notFoundByTypeId.getPut(f.type).put(f.id, f);
                }
                // By code
                else if (f.code != null) {
                    notFoundByName.put(f.code, f);
                }
            }
        }
        loadedFavoritesFromSettings = true;
    }

    /**
     * Saves the favorites to the settings.
     * 
     * @param settings The Settings object
     */
    public void saveFavoritesToSettings(Settings settings) {
        if (!loadedFavoritesFromSettings) {
            LOGGER.warning("Not saving favorite emotes, because they don't seem to have been loaded in the first place.");
            return;
        }
        // Found favorites (global emotes)
        List<List<Object>> entriesToSave = new ArrayList<>();
        for (Favorite f : favorites.keySet()) {
            entriesToSave.add(favoriteToList(f));
        }
        // Not found favorites (or channel-specific emotes)
        for (Favorite f : getNotFound()) {
            entriesToSave.add(favoriteToList(f));
        }
        settings.putList("favoriteEmotes", entriesToSave);
    }
    
    public Set<String> getNonGlobalEmotesets() {
        Set<String> result = new HashSet<>();
        for (Favorite f : favorites.keySet()) {
            if (!Emoticon.isGlobalEmoteset(f.emoteset) && !f.emoteset.isEmpty()) {
                result.add(f.emoteset);
            }
        }
        return result;
    }
    
    /**
     * Turns the given list into a single Favorite object. This is used to load
     * the favorites from the settings. The expected format is as detailed in
     * {@see favoriteToList(Favorite, boolean)}.
     * 
     * @param item The List to turn into a Favorite object
     * @return The created Favorite, or null if an error occured
     * @see favoriteToList(Favorite, boolean)
     */
    private Favorite listToFavorite(List<Object> item) {
        try {
            String code = (String) item.get(0);
            String emoteset;
            if (item.get(1) instanceof String || item.get(1) == null) {
                emoteset = (String)item.get(1);
            } else {
                emoteset = String.valueOf((Number)item.get(1));
            }
            if (emoteset != null) {
                if (emoteset.equals("-2")) {
                    emoteset = "";
                }
                if (emoteset.equals("-1")) {
                    emoteset = null;
                }
            }
            Emoticon.Type type = null;
            if (item.size() > 3) {
                type = Emoticon.Type.fromId((String) item.get(3));
            }
            String id = null;
            if (item.size() > 4) {
                id = (String) item.get(4);
            }
            return new Favorite(code, emoteset, type, id);
        } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
            return null;
        }
    }
    
    /**
     * Turns the given favorite into a list, so it can be saved to the settings.
     * The format is (arrayindex: value (Type)):
     * 
     * <ul>
     * <li>0: code (String),</li>
     * <li>1: emoteset (Number),</li>
     * <li>2: notFoundCount (Number)</li> (removed, may be in old settings)
     * <li>3: type (String)</li> (added later)
     * <li>4: id (String)</li> (added later)
     * </ul>
     * 
     * The notFoundCount is increased if the emote was not found during this
     * session, otherwise it is set to 0.
     * 
     * @param f The favorite to turn into a list
     * @return The created list
     * @see listToFavorite(List)
     */
    private List<Object> favoriteToList(Favorite f) {
        List<Object> list = new ArrayList<>();
        list.add(f.code);
        list.add(f.emoteset);
        list.add(0); // In case loaded in older version where this is expected
        if (f.type != null && f.id != null) {
            list.add(f.type != null ? f.type.id : null);
            list.add(f.id);
        }
        return list;
    }
    
    /**
     * If there are still Favorites not yet associated with an actual Emoticon
     * object, then search through the current emoticons. This should be done
     * everytime new emotes are added (e.g. from request or loaded from cache).
     * 
     * @param twitchEmotesById All Twitch emotes currently loaded
     * @param more
     */
    @SafeVarargs
    public final void find(Map<String, Emoticon> twitchEmotesById,
            Set<Emoticon>... more) {
        if (getNotFoundCount() == 0) {
            return;
        }
        int countBefore = notFoundByName.size() + notFoundByTypeId.subSize();
        findFavorites(twitchEmotesById.values());
        for (Set<Emoticon> emotes : more) {
            findFavorites(emotes);
        }
        if (getNotFoundCount() == 0) {
            LOGGER.info("Emoticons: Found all remaining " + countBefore + " favorites");
        } else {
            LOGGER.info(String.format("Emoticons: %d favorites still not found",
                    getNotFoundCount()));
        }
    }
    
    private boolean findFavorites(Collection<Emoticon> emotes) {
        for (Emoticon emote : emotes) {
            checkFavorite(emote);
            if (getNotFoundCount() == 0) {
                return true;
            }
        }
        return false;
    }
    
    private void checkFavorite(Emoticon emote) {
        Favorite f = getNotFoundByEmote(emote);
        /**
         * For Twitch emotes if a temp emote has been added the emoteset is
         * unknown, so for favorites that emote shouldn't be used, otherwise it
         * can't be determined whether the emote is usable for the local user.
         */
        if (f != null
                && (emote.type != Type.TWITCH
                    || !StringUtil.isNullOrEmpty(emote.emoteset))) {
            favorites.put(f, emote);
            notFoundByName.values().remove(f);
            notFoundByTypeId.subRemoveValue(f);
        }
    }
    
    /**
     * Get the Favorite that matches the given emote, either by code and set or
     * by type and id.
     * 
     * @param emote
     * @return 
     */
    private Favorite getNotFoundByEmote(Emoticon emote) {
        Favorite byName = notFoundByName.get(emote.code);
        /**
         * Allow local emotes (CUSTOM2) to match even without the emoteset check
         * (since the emoteset is not saved with them, but they may replace
         * emotes that were previously available through the API and could have
         * been added as favorites then).
         */
        if (byName != null && (Objects.equals(byName.emoteset, emote.emoteset)
                            || emote.type == Emoticon.Type.CUSTOM2)) {
            return byName;
        }
        return notFoundByTypeId.getOptional(emote.type).get(emote.stringId);
    }
    
    /**
     * Adds the given Emoticon to the favorites.
     *
     * @param emote The Emoticon to add
     */
    public void addFavorite(Emoticon emote) {
        if (emote.hasStreamRestrictions()) {
            notFoundByTypeId.getPut(emote.type).put(emote.stringId, createFavorite(emote));
        }
        else {
            favorites.put(createFavorite(emote), emote);
        }
    }
    
    /**
     * Removes the given Emoticon from the favorites.
     * 
     * @param emote 
     */
    public void removeFavorite(Emoticon emote) {
        favorites.keySet().removeIf(fav -> {
            if (fav.type != null && fav.id != null) {
                return fav.type == emote.type && fav.id.equals(emote.stringId);
            }
            return Objects.equals(fav.code, emote.code) && Objects.equals(fav.emoteset, emote.emoteset);
        });
        notFoundByName.remove(emote.code);
        notFoundByTypeId.getOptional(emote.type).remove(emote.stringId);
    }
    
    public void removeFavorites(Collection<Favorite> toRemove) {
        toRemove.forEach(fav -> favorites.remove(fav));
        notFoundByName.values().removeAll(toRemove);
        notFoundByTypeId.values().forEach(m -> m.values().removeAll(toRemove));
    }
    
    /**
     * Returns a copy of the found global emotes.
     * 
     * @return 
     */
    public Set<Emoticon> getFavorites() {
        return new HashSet<>(favorites.values());
    }
    
    /**
     * Checks whether the given Emoticon is a favorite, by comparing with both
     * found favorites' emotes and by trying to match it with not found
     * favorites.
     *
     * @param emote
     * @return 
     */
    public boolean isFavorite(Emoticon emote) {
        return favorites.containsValue(emote) || getNotFoundByEmote(emote) != null;
    }
    
    private int getNotFoundCount() {
        return notFoundByName.size() + notFoundByTypeId.subSize();
    }
    
    /**
     * Returns a copy of all not found (or channel-specific) favorites.
     * 
     * @return 
     */
    public Collection<Favorite> getNotFound() {
        List<Favorite> result = new ArrayList<>();
        result.addAll(notFoundByName.values());
        notFoundByTypeId.values().forEach(m -> result.addAll(m.values()));
        Collections.sort(result, (o1, o2) -> {
            return o1.code.compareToIgnoreCase(o2.code);
        });
        return result;
    }

    public static class Favorite {
        
        public final String code;
        public final String emoteset;
        public final Emoticon.Type type;
        public final String id;
        
        Favorite(String code, String emoteset, Emoticon.Type type, String id) {
            this.code = code;
            this.emoteset = emoteset;
            this.type = type;
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Favorite other = (Favorite) obj;
            if (!Objects.equals(this.code, other.code)) {
                return false;
            }
            if (!Objects.equals(this.emoteset, other.emoteset)) {
                return false;
            }
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            return this.type == other.type;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.code);
            hash = 97 * hash + Objects.hashCode(this.emoteset);
            hash = 97 * hash + Objects.hashCode(this.type);
            hash = 97 * hash + Objects.hashCode(this.id);
            return hash;
        }
        
        @Override
        public String toString() {
            if (id != null) {
                return String.format("%s (%s) [set:%s, id:%s]", code, type, emoteset, id);
            }
            return String.format("%s [set: %s]", code, emoteset);
        }
        
    }
    
}
