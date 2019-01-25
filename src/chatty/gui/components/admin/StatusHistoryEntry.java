
package chatty.gui.components.admin;

import chatty.util.api.StreamTagManager.StreamTag;
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
    
    /**
     * A list of StreamTag objects. May be null (meaning tags should be entirely
     * disregarded) or empty (meaning no tags set).
     */
    public final List<StreamTag> tags;
    public final long lastActivity;
    public final int timesUsed;
    public final boolean favorite;
    
    public StatusHistoryEntry(String title, String game, List<StreamTag> tags, long lastSet, int timesUsed, boolean favorite) {
        this.title = title;
        this.game = game;
        this.lastActivity = lastSet;
        this.timesUsed = timesUsed;
        this.favorite = favorite;
        if (tags == null) {
            this.tags = null;
        } else {
            this.tags = new ArrayList<>(tags);
        }
    }
    
    public StatusHistoryEntry(String title, String game, List<StreamTag> tags) {
        this(title, game, tags, -1, -1, false);
    }
    
    /**
     * Returns a new identical {@code StatusHistoryEntry}, except that the times
     * used is increased by one and the last activity attribute is updated with
     * the current time.
     * 
     * @return The new {@code StatusHistoryEntry}.
     */
    public StatusHistoryEntry increaseUsed() {
        return new StatusHistoryEntry(title, game, tags, System.currentTimeMillis(), timesUsed+1, favorite);
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
        return new StatusHistoryEntry(title, game, tags, lastActivity, timesUsed, favorite);
    }
    
    public StatusHistoryEntry updateTagName(StreamTag o) {
        if (tags != null) {
            for (StreamTag tag : tags) {
                if (tag.equals(o)
                        && !Objects.equals(o.getDisplayName(), tag.getDisplayName())) {
                    List<StreamTag> newTags = new ArrayList<>(tags);
                    newTags.replaceAll(e -> {
                        if (o.equals(e)) {
                            return o;
                        }
                        return e;
                    });
                    return new StatusHistoryEntry(title, game, newTags, lastActivity, timesUsed, favorite);
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
        if (!Objects.equals(this.tags, other.tags)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.title);
        hash = 17 * hash + Objects.hashCode(this.game);
        hash = 17 * hash + Objects.hashCode(this.tags);
        return hash;
    }

    /**
     * A simple String representation, mainly for debugging.
     * 
     * @return 
     */
    @Override
    public String toString() {
        return title+" "+game+" "+lastActivity+" "+timesUsed+" "+favorite+" "+tags;
    }
    
}
