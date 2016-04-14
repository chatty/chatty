
package chatty;

import java.util.Objects;

/**
 * Immutable object presenting a single entry of a status preset, containing
 * title and game, when it was last touched, how often it was used and whether
 * it is a favorite.
 * 
 * @author tduva
 */
public class StatusHistoryEntry {
    
    public final String title;
    public final String game;
    public final long lastActivity;
    public final int timesUsed;
    public final boolean favorite;
    
    public StatusHistoryEntry(String title, String game, long lastSet, int timesUsed, boolean favorite) {
        this.title = title;
        this.game = game;
        this.lastActivity = lastSet;
        this.timesUsed = timesUsed;
        this.favorite = favorite;
    }
    
    /**
     * Returns a new identical {@code StatusHistoryEntry}, except that the times
     * used is increased by one and the last activity attribute is updated with
     * the current time.
     * 
     * @return The new {@code StatusHistoryEntry}.
     */
    public StatusHistoryEntry increaseUsed() {
        return new StatusHistoryEntry(title, game, System.currentTimeMillis(), timesUsed+1, favorite);
    }
    
    /**
     * Returns a new identical {@code StatusHistoryEntry} that has the
     * {@code favorite} attribute set to the given value and the last activity
     * attribute update with the current time.
     *
     * @param favorite Whether this entry should be favorited
     * @return The new {@code StatusHistoryEntry}
     */
    public StatusHistoryEntry setFavorite(boolean favorite) {
        return new StatusHistoryEntry(title, game, lastActivity, timesUsed, favorite);
    }
    
    /**
     * Objects should be equal only if both the title and game are equal.
     * 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StatusHistoryEntry) {
            StatusHistoryEntry entry = (StatusHistoryEntry)obj;
            if (entry.title.equals(title) && entry.game.equals(game)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.title);
        hash = 23 * hash + Objects.hashCode(this.game);
        return hash;
    }
    
    /**
     * A simple String representation, mainly for debugging.
     * 
     * @return 
     */
    @Override
    public String toString() {
        return title+" "+game+" "+lastActivity+" "+timesUsed+" "+favorite;
    }
    
}
