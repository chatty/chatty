
package chatty.util.api;

import chatty.gui.MainGui;
import chatty.gui.components.eventlog.EventLog;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores current emotesets and requests user emotes, if possible.
 * 
 * @author tduva
 */
public class EmotesetManager {
    
    private final TwitchApi api;
    private final MainGui g;
    private final Settings settings;
    
    private final Map<String, Set<String>> ircEmotesets = new HashMap<>();
    private final Set<String> userEmotesets = new HashSet<>();
    private final Set<String> emotesets = new HashSet<>();
    
    private boolean userEmotesRequested = false;
    
    public EmotesetManager(TwitchApi api, MainGui g, Settings settings) {
        this.api = api;
        this.g = g;
        this.settings = settings;
    }
    
    /**
     * Set emotesets received from IRC, which aren't enough to actually request
     * all emotes, but can be used as an indication when emotesets have changed.
     * 
     * If the required access to request user emotes isn't available, then still
     * just use these.
     * 
     * @param channel
     * @param newSets 
     */
    public void setIrcEmotesets(String channel, Set<String> newSets) {
        boolean changed = false;
        synchronized(this) {
            if (newSets == null) {
                newSets = new HashSet<>();
            }
            Set<String> prevSets = ircEmotesets.get(channel);
            /**
             * Only check length, since emotesets may cycle through, without
             * anything actually having really changed. This is a Twitch Chat bug.
             */
            if ((prevSets != null && prevSets.size() != newSets.size()) || !userEmotesRequested) {
                changed = true;
                userEmotesRequested = true;
            }
            if (prevSets == null) {
                ircEmotesets.put(channel, new HashSet<>());
            }
            ircEmotesets.get(channel).clear();
            ircEmotesets.get(channel).addAll(newSets);
        }
        if (changed) {
            requestUserEmotes();
        }
        newSets.remove("0");
        // Filter out emotesets in the new format for now
        newSets.removeIf(s -> s.contains("-"));
        api.getEmotesBySets(newSets);
        updateEmotesets();
    }
    
    /**
     * Request user emotes, if possible.
     * 
     * @return true if the formal requirements for the request have been met,
     * the request may still have failed
     */
    public boolean requestUserEmotes() {
        String userId = settings.getString("userid");
        if (StringUtil.isNullOrEmpty(userId)) {
            return false;
        }
        if (!settings.listContains("scopes", "user_subscriptions")) {
            EventLog.addSystemEvent("access.myemotes");
            return false;
        }
        api.getUserEmotes(userId);
        return true;
    }
    
    public void setUserEmotesets(Set<String> newEmotesets) {
        synchronized(this) {
            userEmotesets.clear();
            userEmotesets.addAll(newEmotesets);
        }
        updateEmotesets();
    }
    
    /**
     * Update the emotesets for the local user and update stuff if necessary.
     */
    private void updateEmotesets() {
        boolean changed = false;
        Set<String> all = new HashSet<>();
        synchronized(this) {
            // Both IRC and GetUserEmotes sets as long as both don't contain all
            all.addAll(userEmotesets);
            // Add all emotesets from all channels
            for (Set<String> sets : ircEmotesets.values()) {
                all.addAll(sets);
            }
            if (!emotesets.equals(all)) {
                changed = true;
            }
            emotesets.clear();
            emotesets.addAll(all);
        }
        if (changed) {
            g.updateEmotesDialog(all);
            g.updateEmoteNames(all);
        }
    }

    public synchronized Set<String> getEmotesets() {
        return new HashSet<>(emotesets);
    }
    
}
