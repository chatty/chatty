
package chatty.util;

import chatty.util.api.Emoticon;
import chatty.util.api.CachedImage;
import chatty.util.api.CachedImage.ImageType;
import java.awt.Image;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import chatty.util.api.CachedImage.CachedImageUser;
import java.util.ArrayList;
import javax.swing.Timer;

/**
 * This does currently not support proper cleanup of images (e.g. references in
 * "alreadyMade" or "emotes"), but there shouldn't be a lot of these created
 * anyway overall.
 *
 * @author tduva
 */
public class CombinedEmoticon extends Emoticon {
    
    private static final Logger LOGGER = Logger.getLogger(CombinedEmoticon.class.getName());
    
    private final List<Emoticon> emotes;
    private final ImageType imageType;
    
    /**
     * Store for which EmoticonImage (as in, scale etc.) the combined image has
     * already been created, to prevent it being created more than once.
     */
    private final Set<CachedImage<Emoticon>> alreadyMade = new HashSet<>();
    
    public static String getCode(List<Emoticon> emotes) {
        StringBuilder result = new StringBuilder();
        for (Emoticon emote : emotes) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(emote.code);
        }
        return result.toString();
    }
    
    /**
     * Create a new emote based on the given emotes.
     * 
     * @param emotes List of emotes, must not be modified, must contain at least
     * one value
     * @param code
     * @param imageType
     * @return 
     */
    public static CombinedEmoticon create(List<Emoticon> emotes, String code, ImageType imageType) {
        Debugging.println("combinedemotes", "Create: %s", emotes);
        Emoticon base = emotes.get(0);
        Emoticon.Builder b = new Emoticon.Builder(base.type, code, base.url);
        b.setStringId(base.stringId);
        b.setStringIdAlias(base.stringIdAlias);
        b.setLiteral(base.literal);
        b.setAnimated(false);
        int maxWidth = -1;
        int maxHeight = -1;
        for (Emoticon emote : emotes) {
            maxWidth = Math.max(maxWidth, emote.getWidth());
            maxHeight = Math.max(maxHeight, emote.getHeight());
            if (emote.isAnimated()) {
                b.setAnimated(true);
                break;
            }
        }
        b.setSize(maxWidth, maxHeight);
        b.setX2Url(base.urlX2);
        b.setSubType(base.subType);
        b.addInfo("Special Combined Emote");
        return new CombinedEmoticon(b, emotes, imageType);
    }
    
    private CombinedEmoticon(Builder b, List<Emoticon> emotes, ImageType imageType) {
        super(b);
        this.emotes = emotes;
        this.imageType = imageType;
    }
    
    public List<Emoticon> getEmotes() {
        return new ArrayList<>(emotes);
    }
    
    /**
     * Overrides the regular method to load all emotes to be combined and only
     * create the actual combined image when all are loaded.
     * 
     * @param scaleFactor
     * @param maxHeight
     * @param user
     * @return 
     */
    @Override
    public CachedImage<Emoticon> getIcon(float scaleFactor, int maxHeight, ImageType imageType, CachedImageUser user) {
        // Don't request actual emote image, just leave default (TEMP)
        CachedImage<Emoticon> emoteImage = super.getIcon(scaleFactor, maxHeight, ImageType.TEMP, user);
        Debugging.println("combinedemotes", "Get: %s [%s] %s", emotes, System.identityHashCode(this), alreadyMade.contains(emoteImage));
        if (alreadyMade.contains(emoteImage)) {
            return emoteImage;
        }
        /**
         * Fallback timer. Sometimes iconLoaded() is not called, not sure why.
         * Could have to do with getImageIcon() being called before already when
         * first normally adding the emotes separately, but still the user added
         * here should be informed anyway. Posting the same combined emote in
         * chat again would fix it as well, but can't really rely on that, so
         * call makeImage() again just in case. The issue could depend on timing
         * since it only happens sometimes and adding debug messages appeared to
         * change the behaviour.
         */
        Timer timer = new Timer(2000, e -> {
            makeImage(scaleFactor, maxHeight, user);
        });
        timer.setRepeats(false);
        timer.start();
        
        boolean allLoaded = true;
        for (Emoticon emote : emotes) {
            CachedImage<Emoticon> image = emote.getIcon(scaleFactor, maxHeight, imageType, new CachedImageUser() {

                @Override
                public void iconLoaded(Image oldImage, Image newImage, boolean sizeChanged) {
                    Debugging.println("combinedemotes", "iconLoaded: %s [%s]", emotes, System.identityHashCode(CombinedEmoticon.this));
                    SwingUtilities.invokeLater(() -> {
                        makeImage(scaleFactor, maxHeight, user);
                    });
                }
            });
            if (!image.isLoaded()) {
                allLoaded = false;
                image.getImageIcon();
            }
        }
        if (allLoaded) {
            makeImage(scaleFactor, maxHeight, user);
        }
        return emoteImage;
    }
    
    /**
     * Create combined image. Should only be called when emotes are already
     * loaded.
     * 
     * @param scaleFactor
     * @param maxHeight
     * @param user 
     */
    private void makeImage(float scaleFactor, int maxHeight, CachedImageUser user) {
        CachedImage<Emoticon> emoteImage = super.getIcon(scaleFactor, maxHeight, ImageType.TEMP, user);
        if (alreadyMade.contains(emoteImage)) {
            return;
        }
        Debugging.println("combinedemotes", "Make: %s [%s]", emotes, System.identityHashCode(this));
        LinkedHashMap<ImageIcon, Integer> data = new LinkedHashMap<>();
        // Build list images and offsets
        for (Emoticon emote : emotes) {
            CachedImage<Emoticon> image = emote.getIcon(scaleFactor, maxHeight, imageType, null);
            if (!image.isLoaded()) {
                LOGGER.warning(image.getObject()+" not loaded for "+emotes);
                return;
            }
            int yOffset = ChattyMisc.getCombinedEmotesInfo().getOffset(emote.code);
            data.put(image.getImageIcon(), yOffset);
        }
        // Create and change this emotes icon
        ImageIcon result;
        try {
            result = chatty.util.gif.Overlay.overlayNew(data);
        } catch (Exception ex) {
            LOGGER.warning("Error when combining "+emotes+": "+ex);
            return;
        }
        alreadyMade.add(emoteImage);
        emoteImage.setImageIcon(result, true);
    }
    
}
