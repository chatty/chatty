
package chatty.util;

import chatty.Helper;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticons;
import chatty.util.api.TwitchApi;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class TwitchEmotesApi {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchEmotesApi.class.getName());
    
    public static final TwitchEmotesApi api = new TwitchEmotesApi();
    private TwitchApi twitchApi;
    
    public TwitchEmotesApi() {
        
    }
    
    public void setTwitchApi(TwitchApi api) {
        this.twitchApi = api;
    }
    
    /**
     * Request channel emotes only if not already requested.
     * 
     * @param stream 
     */
    public void requestByStream(String stream) {
        twitchApi.getEmotesByChannelId(stream, null, false);
    }
    
    //================
    // Get Emote Info
    //================
    
    /**
     * Get info for the given Emoticon. If request errored before, this might
     * call the listener immediately with a null value.
     * 
     * @param unique A previous request with the same Object will be overwritten
     * @param listener If no cached info available, this listener will be
     * notified with the request result, possibly null if an error occured
     * (optional)
     * @param emote The Emoticon to get the info for
     * @return 
     */
    public EmotesetInfo getInfoByEmote(Object unique, Consumer<EmotesetInfo> listener, Emoticon emote) {
        return null;
    }
    
    /**
     * Get by stream for the Emote Dialog "Channel" tab. Only one request can
     * be active at a time. Requested as soon as possible with no rety on error.
     * Also requests the emotes for the resulting emotesets if necessary.
     * 
     * @param listener The listener that will be called with the result
     * @param stream The stream name to request for
     */
    public void requestByStream(Consumer<Set<EmotesetInfo>> listener, String stream) {
        return;
    }
    
    /**
     * Get by stream id for the Emote Dialog "Emote Details". Only one request
     * can be active at a time. Requested as soon as possible with no retry on
     * error.
     * 
     * @param listener The listener, receiving a Set of EmotesetInfo objects,
     * which may be null or empty
     * @param streamId A stream id
     */
    public void requestByStreamId(Consumer<Set<EmotesetInfo>> listener, String streamId) {
        
    }
    
    /**
     * Get by sets for the Emote Dialog "My Emotes" tab. Retry missing errored
     * sets. Multiple partial results may be returned in case of errors. Only
     * one request can be active at a time.
     * 
     * Emotes for the set "0" are not requested.
     * 
     * @param listener The listener, receiving the result map, where some keys
     * may return null if errors occured
     * @param emotesets
     * @return The result, if already cached, or null
     */
    public Map<String, EmotesetInfo> requestBySets(Consumer<Map<String, EmotesetInfo>> listener, Set<String> emotesets) {
        return null;
    }
    
    public EmotesetInfo getBySet(String emoteset) {
        return null;
    }
    
    /**
     * Helper function to compare a set of integer emotesets against
     * EmotesetInfo objects.
     *
     * @param accessTo
     * @param emotesets
     * @return true if all emotesets ids are in accessTo, false otherwise
     */
    public static boolean hasAccessTo(Set<String> accessTo, Set<EmotesetInfo> emotesets) {
        for (EmotesetInfo set : emotesets) {
            if (!accessTo.contains(set.emoteset_id)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the stream from the emote, or from the EmotesetInfo (if available).
     * 
     * @param emote The Emoticon
     * @param info The EmotesetInfo for the emote (optional)
     * @return The stream, or null if info not found
     */
    public static String getStream(Emoticon emote, EmotesetInfo info) {
        String stream = Helper.isValidStream(emote.getStream()) ? emote.getStream() : null;
        if (stream == null && info != null && info.stream_name != null) {
            stream = info.stream_name;
        }
        return stream;
    }
    
    /**
     * Get the emoteset from the emote, or from the EmotesetInfo (if available).
     * 
     * @param emote The Emoticon
     * @param info The EmotesetInfo for the emote (optional)
     * @return 
     */
    public static String getSet(Emoticon emote, EmotesetInfo info) {
        String emoteset = emote.emoteset;
        if (emoteset != null && emoteset.equals(Emoticon.SET_UNKNOWN) && info != null) {
            emoteset = info.emoteset_id;
        }
        return emoteset;
    }
    
    /**
     * Returns true if the emote has a string id that consists of more than a
     * single number.
     * 
     * @param emote
     * @return 
     */
    public static boolean isModified(Emoticon emote) {
        try {
            Integer.parseInt(emote.stringId);
            return false;
        } catch (NumberFormatException ex) {
            return true;
        }
    }
    
    /**
     * Get a description of the emote type for the given Emoticon, using the
     * EmotesetInfo as well.
     * 
     * @param emote The Emoticon
     * @param info The EmotesetInfo (optional)
     * @param includeStream Whether to include the stream name in the output for
     * Subemotes
     * @return A non-null string
     */
    public static String getEmoteType(Emoticon emote, EmotesetInfo info, boolean includeStream) {
        String emoteset = getSet(emote, info);
        String modified = isModified(emote) ? " [modified]" : "";
        if (emote.hasGlobalEmoteset()) {
            return "Twitch Global"+modified;
        } else if (Emoticons.isTurboEmoteset(emoteset)) {
            return "Turbo Emoticon"+modified;
        } else if (emote.getEmotesetInfo() != null) {
            if (emote.getEmotesetInfo().startsWith("Tier")) {
                return emote.getEmotesetInfo()+" Subemote";
            }
            return emote.getEmotesetInfo();
        } else if (info == null) {
            return "Unknown Emote";
        } else if (info.stream_name != null && !info.stream_name.equals("Twitch")) {
            if (includeStream) {
                return "Subemote (" + info.stream_name + ")"+modified;
            } else {
                return "Subemote"+modified;
            }
        } else {
            return "Other Twitch Emote"+modified;
        }
    }
    
    public static class EmotesetInfo {

        public final String emoteset_id;
        public final String product;
        public final String stream_name;
        public final String stream_id;

        public EmotesetInfo(String emoteset_id, String stream_name, String stream_id, String product) {
            this.emoteset_id = emoteset_id;
            this.product = product;
            this.stream_name = stream_name;
            this.stream_id = stream_id;
        }

        @Override
        public String toString() {
            return String.format("%s(%s,%s,%s)", emoteset_id, stream_name, stream_id, product);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final EmotesetInfo other = (EmotesetInfo) obj;
            if (!Objects.equals(this.emoteset_id, other.emoteset_id)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.emoteset_id);
            return hash;
        }
        
    }
    
}
