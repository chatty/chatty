
package chatty.gui.components.admin;

import chatty.util.api.CommunitiesManager.Community;
import java.util.ArrayList;
import java.util.List;
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
    public final List<Community> communities;
    public final long lastActivity;
    public final int timesUsed;
    public final boolean favorite;
    
    public StatusHistoryEntry(String title, String game, List<Community> communities, long lastSet, int timesUsed, boolean favorite) {
        this.title = title;
        this.game = game;
        this.lastActivity = lastSet;
        this.timesUsed = timesUsed;
        this.favorite = favorite;
        this.communities = new ArrayList<>(communities);
    }
    
    public StatusHistoryEntry(String title, String game, List<Community> communities) {
        this(title, game, communities, -1, -1, false);
    }
    
    /**
     * Returns a new identical {@code StatusHistoryEntry}, except that the times
     * used is increased by one and the last activity attribute is updated with
     * the current time.
     * 
     * @return The new {@code StatusHistoryEntry}.
     */
    public StatusHistoryEntry increaseUsed() {
        return new StatusHistoryEntry(title, game, communities, System.currentTimeMillis(), timesUsed+1, favorite);
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
        return new StatusHistoryEntry(title, game, communities, lastActivity, timesUsed, favorite);
    }
    
    public StatusHistoryEntry updateCommunityName(Community c) {
        if (communities != null) {
            for (Community community : communities) {
                if (community.equals(c)
                        && !Objects.equals(c.getCapitalizedName(), community.getCapitalizedName())) {
                    List<Community> newCommunities = new ArrayList<>(communities);
                    newCommunities.replaceAll(e -> {
                        if (c.equals(e)) {
                            return c;
                        }
                        return e;
                    });
                    return new StatusHistoryEntry(title, game, newCommunities, lastActivity, timesUsed, favorite);
                }
            }
        }
        return this;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StatusHistoryEntry other = (StatusHistoryEntry) obj;
        if (!Objects.equals(this.title, other.title)) {
            return false;
        }
        if (!Objects.equals(this.game, other.game)) {
            return false;
        }
        if (!Objects.equals(this.communities, other.communities)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + Objects.hashCode(this.title);
        hash = 73 * hash + Objects.hashCode(this.game);
        hash = 73 * hash + Objects.hashCode(this.communities);
        return hash;
    }

    /**
     * A simple String representation, mainly for debugging.
     * 
     * @return 
     */
    @Override
    public String toString() {
        return title+" "+game+" "+lastActivity+" "+timesUsed+" "+favorite+" "+communities;
    }
    
}
