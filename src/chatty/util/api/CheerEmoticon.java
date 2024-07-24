
package chatty.util.api;

import chatty.Helper;
import chatty.util.api.CachedImage.ImageType;
import java.awt.Color;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class CheerEmoticon extends Emoticon {
    
    public static class CheerEmoticonUrl {
        
        public final String url;
        public final String background;
        public final String type;
        public final String scale;
        
        public CheerEmoticonUrl(String url, String background, String type, String scale) {
            this.url = url;
            this.background = background;
            this.type = type;
            this.scale = scale;
        }
    }
    
    public final String prefix;
    public final int min_bits;
    public final Color color;
    
    private volatile String currentBackground;
    private volatile String currentState;
    
    private final Set<CheerEmoticonUrl> urls;
    
    public static CheerEmoticon create(String prefix, int min_bits, Color color,
            Set<CheerEmoticonUrl> urls, String stream) {
        Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.TWITCH, "(?i)"+prefix+"([0-9]+)");
        b.setSubType(Emoticon.SubType.CHEER);
        b.addInfo(Helper.formatViewerCount(min_bits)+" bits");
        b.addStreamRestriction(stream);
        b.setStream(stream);
        return new CheerEmoticon(b, prefix, min_bits, color, urls);
    }
    
    public CheerEmoticon(Emoticon.Builder b, String prefix, int min_bits,
            Color color, Set<CheerEmoticonUrl> urls) {
        super(b);
        this.prefix = prefix;
        this.min_bits = min_bits;
        this.color = color;
        this.urls = urls;
    }
    
    @Override
    public String getEmoteUrl(int factor, ImageType imageType) {
        return getUrl(currentBackground, currentState, String.valueOf(factor));
    }
    
    public String getUrl(String theme, String type, String scale) {
        for (CheerEmoticonUrl url : urls) {
            if (url.background.equals(theme) && url.type.equals(type) && url.scale.equals(scale)) {
                return url.url;
            }
        }
        return null;
    }
    
    public void setSettings(String background, String state) {
        if (background == null || state == null) {
            return;
        }
        if (!background.equals(currentBackground) || !state.equals(currentState)) {
            this.currentBackground = background;
            this.currentState = state;
            setAnimated(state.equals("animated"));
            clearImages();
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.prefix);
        hash = 17 * hash + this.min_bits;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CheerEmoticon other = (CheerEmoticon) obj;
        if (!Objects.equals(this.prefix, other.prefix)) {
            return false;
        }
        if (this.min_bits != other.min_bits) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return super.toString()+"/"+min_bits;
    }
    
    public String getSimpleCode() {
        return prefix+min_bits;
    }
    
}
