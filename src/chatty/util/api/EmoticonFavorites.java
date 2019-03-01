
package chatty.util.api;

import chatty.util.settings.Settings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class EmoticonFavorites {
    
    private static final Logger LOGGER = Logger.getLogger(EmoticonFavorites.class.getName());
    
    /**
     * Contains all favorites that still need to be found.
     */
    private final Map<String, Favorite> favoritesNotFound = new HashMap<>();
    
    /**
     * Contains all favorites with the associated Emoticon object. Favorites
     * that haven't been found yet (either because their metadata hasn't been
     * loaded yet or they don't exist on Twitch anymore) will have a dummy
     * Emoticon object.
     */
    private final HashMap<Favorite, Emoticon> favorites = new HashMap<>();
    
    private boolean loadedFavoritesFromSettings;
    
    
    /**
     * Adds the given Emoticon to the favorites.
     * 
     * @param emote The Emoticon to add
     */
    public void addFavorite(Emoticon emote) {
        favorites.put(createFavorite(emote), emote);
    }
    
    /**
     * Creates a Favorite object for the given Emoticon.
     * 
     * @param emote The Emoticon to create the Favorite object for
     * @return The created Favorite object
     */
    private Favorite createFavorite(Emoticon emote) {
        return new Favorite(emote.code, emote.emoteSet);
    }
    
    /**
     * Loads the favorites from the settings.
     * 
     * @param settings The Settings object
     */
    public void loadFavoritesFromSettings(Settings settings) {
        List<List> entriesToLoad = settings.getList("favoriteEmotes");
        favoritesNotFound.clear();
        favorites.clear();
        for (List item : entriesToLoad) {
            Favorite f = listToFavorite(item);
            if (f != null) {
                favoritesNotFound.put(f.code, f);
            }
        }
        loadedFavoritesFromSettings = true;
    }

    /**
     * Saves the favorites to the settings, discarding any favorites that
     * haven't been found several times already.
     * 
     * @param settings The Settings object
     */
    public void saveFavoritesToSettings(Settings settings) {
        if (!loadedFavoritesFromSettings) {
            LOGGER.warning("Not saving favorite emotes, because they don't seem to have been loaded in the first place.");
            return;
        }
        List<List> entriesToSave = new ArrayList<>();
        for (Favorite f : favorites.keySet()) {
            entriesToSave.add(favoriteToList(f));
        }
        settings.putList("favoriteEmotes", entriesToSave);
    }
    
    public Set<Integer> getEmotesets() {
        Set<Integer> result = new HashSet<>();
        for (Favorite f : favorites.keySet()) {
            if (f.emoteset > 0) {
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
    private Favorite listToFavorite(List item) {
        try {
            String code = (String) item.get(0);
            int emoteset = ((Number) item.get(1)).intValue();
            return new Favorite(code, emoteset);
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
     * </ul>
     * 
     * The notFoundCount is increased if the emote was not found during this
     * session, otherwise it is set to 0.
     * 
     * @param f The favorite to turn into a list
     * @return The created list
     * @see listToFavorite(List)
     */
    private List favoriteToList(Favorite f) {
        List list = new ArrayList();
        list.add(f.code);
        list.add(f.emoteset);
        list.add(0); // In case loaded in older version where this is expeceted
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
    public void find(Map<Integer, Emoticon> twitchEmotesById,
            Set<Emoticon>... more) {
        if (favoritesNotFound.isEmpty()) {
            return;
        }
        int count = favoritesNotFound.size();
        findFavorites(twitchEmotesById.values());
        for (Set<Emoticon> emotes : more) {
            findFavorites(emotes);
        }
        if (favoritesNotFound.isEmpty()) {
            LOGGER.info("Emoticons: Found all remaining " + count + " favorites");
        } else {
            createNotFound();
            LOGGER.info("Emoticons: "+favoritesNotFound.size()+" favorites still not found");
        }
    }
    
    /**
     * Create "dummy" Emoticon objects for not found favorites.
     */
    private void createNotFound() {
        for (Favorite f : favoritesNotFound.values()) {
            Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.NOT_FOUND_FAVORITE, f.code, null);
            b.setEmoteset(f.emoteset);
            favorites.put(f, b.build());
        }
    }
    
    private boolean findFavorites(Collection<Emoticon> emotes) {
        for (Emoticon emote : emotes) {
            checkFavorite(emote);
            if (favoritesNotFound.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    private void checkFavorite(Emoticon emote) {
        Favorite f = favoritesNotFound.get(emote.code);
        if (f != null && f.emoteset == emote.emoteSet) {
            favorites.put(f, emote);
            favoritesNotFound.remove(emote.code);
        }
    }
    
    /**
     * Removes the given Emoticon from the favorites.
     * 
     * @param emote 
     */
    public void removeFavorite(Emoticon emote) {
        favorites.remove(createFavorite(emote));
        favoritesNotFound.remove(emote.code);
    }
    
    /**
     * Returns a copy of the favorites.
     * 
     * @return 
     */
    public Set<Emoticon> getFavorites() {
        return new HashSet<>(favorites.values());
    }
    
    /**
     * Gets the number of favorites that couldn't be found.
     * 
     * @return 
     */
    public int getNumNotFoundFavorites() {
        return favoritesNotFound.size();
    }
    
    /**
     * Checks whether the given Emoticon is a favorite.
     * 
     * @param emote
     * @return 
     */
    public boolean isFavorite(Emoticon emote) {
        return favoritesNotFound.containsKey(emote.code) || favorites.containsValue(emote);
    }

    /**
     * A favorite specifying the emote code, the emoteset and how often it
     * hasn't been found. The emote code and emoteset are required to find the
     * actual Emoticon object that corresponds to it.
     */
    private static class Favorite {
        
        public final String code;
        public final int emoteset;
        
        Favorite(String code, int emoteset) {
            this.code = code;
            this.emoteset = emoteset;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 19 * hash + Objects.hashCode(this.code);
            hash = 19 * hash + this.emoteset;
            return hash;
        }

        /**
         * A Favorite is considered equal when both the emote code and emoteset
         * are equal.
         * 
         * @param obj
         * @return 
         */
        @Override
        public boolean equals(Object obj) {
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
            if (this.emoteset != other.emoteset) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return code+"["+emoteset+"]";
        }
        
    }
    
}
