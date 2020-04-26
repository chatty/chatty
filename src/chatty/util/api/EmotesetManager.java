
package chatty.util.api;

import chatty.gui.MainGui;
import chatty.gui.components.eventlog.EventLog;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.util.HashSet;
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
    
    private final Set<String> ircEmotesets = new HashSet<>();
    private final Set<String> emotesets = new HashSet<>();
    
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
     * @param emotesets 
     */
    public void setIrcEmotesets(Set<String> emotesets) {
        boolean changed = false;
        synchronized(this) {
            if (emotesets == null) {
                emotesets = new HashSet<>();
            }
            /**
             * Only check length, since emotesets may cycle through, without
             * anything actually having really changed. This is a Twitch Chat bug.
             */
            if (ircEmotesets.size() != emotesets.size()) {
                changed = true;
            }
            ircEmotesets.clear();
            ircEmotesets.addAll(emotesets);
        }
        if (changed) {
            if (!requestUserEmotes()) {
                /**
                 * Request not possible for some reason, so just use IRC sets
                 * directly, requesting them as usual.
                 */
                setEmotesets(emotesets);
                api.getEmotesBySets(emotesets);
            }
        }
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
    
    /**
     * Set the emotesets for the local user and update stuff if necessary.
     * 
     * @param newEmotesets Must not be null
     */
    public void setEmotesets(Set<String> newEmotesets) {
        boolean changed = false;
        synchronized(this) {
            if (!emotesets.equals(newEmotesets)) {
                changed = true;
            }
            emotesets.clear();
            emotesets.addAll(newEmotesets);
        }
        if (changed) {
            g.updateEmotesDialog(newEmotesets);
            g.updateEmoteNames(newEmotesets);
        }
    }

    public synchronized Set<String> getEmotesets() {
        return new HashSet<>(emotesets);
    }
    
}
