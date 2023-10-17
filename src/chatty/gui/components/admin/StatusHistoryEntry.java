
package chatty.gui.components.admin;

import chatty.util.api.ChannelStatus.StreamTag;
import chatty.util.api.StreamCategory;
import chatty.util.api.StreamLabels.StreamLabel;
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
    public final StreamCategory game;
    
    /**
     * A list of StreamTag objects. May be null (meaning tags should be entirely
     * disregarded) or empty (meaning no tags set).
     */
    public final List<StreamTag> tags;
    public final List<StreamLabel> labels;
    public final long lastActivity;
    public final int timesUsed;
    public final boolean favorite;
    
    public StatusHistoryEntry(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels, long lastSet, int timesUsed, boolean favorite) {
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
        if (labels == null) {
            this.labels = null;
        } else {
            this.labels = new ArrayList<>(labels);
        }
    }
    
    public StatusHistoryEntry(String title, StreamCategory game, List<StreamTag> tags, List<StreamLabel> labels) {
        this(title, game, tags, labels, -1, -1, false);
    }
    
    /**
     * Returns a new identical {@code StatusHistoryEntry}, except that the times
     * used is increased by one and the last activity attribute is updated with
     * the current time.
     * 
     * @return The new {@code StatusHistoryEntry}.
     */
    public StatusHistoryEntry increaseUsed() {
        return new StatusHistoryEntry(title, game, tags, labels, System.currentTimeMillis(), timesUsed+1, favorite);
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
        return new StatusHistoryEntry(title, game, tags, labels, lastActivity, timesUsed, favorite);
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
                    return new StatusHistoryEntry(title, game, newTags, labels, lastActivity, timesUsed, favorite);
                }
            }
        }
        return this;
    }
    
    /**
     * Update the game (category) id or name, if the given category matches and
     * a change is needed.
     * 
     * @param updatedCategory The up-to-date category, probably from the API
     * @return A new entry with the category changed, or the same entry if
     * nothing changed
     */
    public StatusHistoryEntry updateCategory(StreamCategory updatedCategory) {
        if (game == null || updatedCategory == null) {
            return this;
        }
        // Add id
        if (!game.hasId() && updatedCategory.nameMatches(game)) {
            return new StatusHistoryEntry(title, updatedCategory, tags, labels, lastActivity, timesUsed, favorite);
        }
        // Change name
        if (game.hasId() && updatedCategory.id.equals(game.id) && !updatedCategory.name.equals(game.name)) {
            return new StatusHistoryEntry(title, updatedCategory, tags, labels, lastActivity, timesUsed, favorite);
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
        if (!Objects.equals(this.labels, other.labels)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.title);
        hash = 59 * hash + Objects.hashCode(this.game);
        hash = 59 * hash + Objects.hashCode(this.tags);
        hash = 59 * hash + Objects.hashCode(this.labels);
        return hash;
    }

    /**
     * A simple String representation, mainly for debugging.
     * 
     * @return 
     */
    @Override
    public String toString() {
        return title+" "+game+" "+lastActivity+" "+timesUsed+" "+favorite+" "+tags+" "+labels;
    }
    
}
