
package chatty.util.api;

import chatty.Helper;
import chatty.gui.MainGui;
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
    
    private final Map<String, Set<String>> ircEmotesets = new HashMap<>();
    private final Set<String> latestIrcEmotesets = new HashSet<>();
    
    /**
     * Emotesets both from the API and latest from IRC. May not contain follower
     * emote sets, but subemotes sets should be up-to-date.
     */
    private final Set<String> emotesets = new HashSet<>();
    
    public EmotesetManager(TwitchApi api, MainGui g, Settings settings) {
        this.api = api;
        this.g = g;
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
            latestIrcEmotesets.clear();
            latestIrcEmotesets.addAll(newSets);
            
            Set<String> prevSets = ircEmotesets.get(channel);
            /**
             * Only check length, since emotesets may cycle through, without
             * anything actually having really changed. This is a Twitch Chat bug.
             */
            if (prevSets == null || prevSets.size() != newSets.size()) {
                changed = true;
            }
            if (prevSets == null) {
                ircEmotesets.put(channel, new HashSet<>());
            }
            ircEmotesets.get(channel).clear();
            ircEmotesets.get(channel).addAll(newSets);
        }
        if (changed) {
            g.updateEmotesDialog(Helper.toStream(channel), newSets);
            g.updateEmoteNames(newSets);
        }
        api.getEmotesBySets(newSets);
    }

    public synchronized Set<String> getEmotesetsByChannel(String channel) {
        if (!ircEmotesets.containsKey(channel)) {
            return new HashSet<>();
        }
        return new HashSet<>(ircEmotesets.get(channel));
    }
    
}
