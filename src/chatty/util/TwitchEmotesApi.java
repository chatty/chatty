
package chatty.util;

import chatty.util.api.Emoticon;
import chatty.util.api.Emoticons;
import chatty.util.api.TwitchApi;
import java.util.Objects;
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
    
    private static final String[] MODIFIERS = new String[]{"_BW", "_HF", "_SG", "_SQ", "_TK"};
    
    /**
     * Returns true if the emote has a string id that consists of more than a
     * single number.
     * 
     * @param emote
     * @return 
     */
    public static boolean isModified(Emoticon emote) {
        if (emote.type == Emoticon.Type.TWITCH && emote.stringId != null) {
            for (String suffix : MODIFIERS) {
                if (emote.stringId.endsWith(suffix)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Get a description of the emote type for the given Emoticon, using the
     * EmotesetInfo as well.
     * 
     * @param emote The Emoticon
     * @return A non-null string
     */
    public static String getEmoteType(Emoticon emote) {
        String emoteset = emote.emoteset;
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
        } else {
            return "Unknown Emote"+modified;
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
