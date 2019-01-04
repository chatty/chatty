
package chatty.util.api;

import chatty.Helper;
import chatty.User;
import chatty.util.ImageCache;
import chatty.util.StringUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

/**
 * A single emoticon, that contains a pattern, an URL to the image and
 * a width/height.
 * 
 * It also includes a facility to load the image in a separate thread once
 * it is needed.
 * 
 * @author tduva
 */
public class Emoticon {
    
    private static final Logger LOGGER = Logger.getLogger(Emoticon.class.getName());
    
    public static final int SET_GLOBAL = 0;
    public static final int SET_UNDEFINED = -1;
    public static final int SET_UNKNOWN = -2;
    public static final int ID_UNDEFINED = -1;
    
    public static enum Type {
        TWITCH("Twitch"), FFZ("FFZ"), BTTV("BTTV"), CUSTOM("Custom"),
        EMOJI("Emoji"), NOT_FOUND_FAVORITE("NotFoundFavorite");
        
        public String label;
        
        Type(String label) {
            this.label = label;
        }
    }
    
    public static enum SubType {
        FEATURE_FRIDAY, EVENT, CHEER
    }
    
    /**
     * Try loading the image these many times, which will be tried if an error
     * occurs.
     */
    private static final int MAX_LOADING_ATTEMPTS = 3;
    
    /**
     * How much time (milliseconds) has to pass in between loading attempts,
     * which will only happen if an error occured.
     */
    private static final int LOADING_ATTEMPT_DELAY = 30*1000;
    
    /**
     * Number of seconds emote images are supposed to be cached for before they
     * are refreshed.
     */
    private static final int CACHE_TIME = 60*60*24*7;
    
    // Assumed width/height if there is none given
    private static final int DEFAULT_WIDTH = 28;
    private static final int DEFAULT_HEIGHT = 28;
    
    private static final float MAX_WIDTH = 100;
    private static final float MAX_HEIGHT = 50;
    
    private static final int MAX_SCALED_WIDTH = 250;
    private static final int MAX_SCALED_HEIGHT = 150;
    
    public final Type type;
    public final SubType subType;
    public final String code;
    public final int emoteSet;
    private final Set<String> streamRestrictions;
    public final String url;
    public final boolean literal;
    public final int numericId;
    public final String stringId;
    public final String urlX2;
    public final String creator;
    
    private String stream;
    private Set<String> infos;
    private String emotesetInfo;
    private boolean isAnimated;
    
    private volatile int width;
    private volatile int height;

    private Matcher matcher;
    private Set<EmoticonImage> images;
    

    /**
     * Set required values in contructor and optional values via methods, then
     * construct the Emoticon object with a private constructor. This ensures
     * that the Emoticon object is fully build when it is used (while not having
     * to pass all values to one constructor).
     */
    public static class Builder {
        
        private final Type type;
        private final String search;
        private final String url;
        
        private SubType subtype;
        private String urlX2;
        private int width = -1;
        private int height = -1;
        private boolean literal = false;
        private String stream;
        private String emotesetInfo;
        private Set<String> streamRestrictions;
        private Set<String> infos;
        private int emoteset = SET_UNDEFINED;
        private int numericId = ID_UNDEFINED;
        private String stringId = null;
        private String creator;
        private boolean isAnimated = false;
        
        public Builder(Type type, String search, String url) {
            this.type = type;
            this.search = search;
            this.url = url;
        }
        
        public Builder setSize(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }
        
        public Builder addStreamRestriction(String stream) {
            if (stream != null) {
                if (streamRestrictions == null) {
                    streamRestrictions = new HashSet<>();
                }
                streamRestrictions.add(StringUtil.toLowerCase(stream));
            }
            return this;
        }
        
        public Builder setEmoteset(int emoteset) {
            this.emoteset = emoteset;
            return this;
        }
        
        public Builder setStream(String stream) {
            this.stream = stream;
            return this;
        }
        
        public Builder setEmotesetInfo(String info) {
            this.emotesetInfo = info;
            return this;
        }
        
        public Builder setLiteral(boolean literal) {
            this.literal = literal;
            return this;
        }
        
        public Builder setNumericId(int id) {
            this.numericId = id;
            return this;
        }
        
        public Builder setStringId(String id) {
            this.stringId = id;
            return this;
        }
        
        public Builder setX2Url(String url) {
            this.urlX2 = url;
            return this;
        }
        
        public Builder setCreator(String creator) {
            this.creator = creator;
            return this;
        }
        
        public Builder addInfo(String info) {
            if (info != null) {
                if (infos == null) {
                    infos = new HashSet<>();
                }
                infos.add(info);
            }
            return this;
        }
        
        public Builder setAnimated(boolean isAnimated) {
            this.isAnimated = isAnimated;
            return this;
        }
        
        public Builder setSubType(SubType subtype) {
            this.subtype = subtype;
            return this;
        }
        
        public Emoticon build() {
            return new Emoticon(this);
        }
    }
    
    /**
     * Get the built URL for this emote if applicable to the emote type.
     * 
     * @param factor The size factor (1 or 2)
     * @return The URL as a String or null if none could created or not of an
     * applicable type
     */
    protected String getEmoteUrl(int factor) {
        if (type == Type.TWITCH) {
            if (numericId != ID_UNDEFINED) {
                return getTwitchEmoteUrlById(numericId, factor);
            }
        } else if (type == Type.BTTV && stringId != null) {
            return getBttvEmoteUrl(stringId, factor);
        } else if (type == Type.FFZ) {
            return getFFZUrl(factor);
        } else if (type == Type.EMOJI) {
            return url;
        }
        return null;
    }
    
    public static String getTwitchEmoteUrlById(int id, int factor) {
        return "https://static-cdn.jtvnw.net/emoticons/v1/"+id+"/"+factor+".0";
    }
    
    public String getBttvEmoteUrl(String id, int factor) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        String result = url;
        result = result.replace("{{id}}", id);
        result = result.replace("{{image}}", factor + "x");
        return result;
    }
    
    public String getFFZUrl(int factor) {
        if (factor == 2 && urlX2 != null) {
            return urlX2;
        }
        return url;
    }

    /**
     * Private constructor specifically for use with the Builder.
     * 
     * @param builder The Emoticon.Builder object containing the values to
     * construct this object with
     */
    protected Emoticon(Builder builder) {
        
        String code = builder.search;
        
        // Replace some HTML entities (Twitch matches on HTML, we do not)
        code = code.replace("\\&lt\\;", "<");
        code = code.replace("\\&gt\\;", ">");

        // Save before adding word boundary matching
        this.code = code;

        this.type = builder.type;
        this.emoteSet = builder.emoteset;
        this.url = Helper.checkHttpUrl(builder.url);
        this.urlX2 = Helper.checkHttpUrl(builder.urlX2);
        
        int width = builder.width;
        int height = builder.height;
        if (width != -1 && height != -1) {
            if (width > MAX_WIDTH) {
                height = (int) (height / (width / MAX_WIDTH));
                width = (int) MAX_WIDTH;
            }
            if (height > MAX_HEIGHT) {
                width = (int) (width / (height / MAX_HEIGHT));
                height = (int) MAX_HEIGHT;
            }
            if (width <= 0) {
                width = 1;
            }
            if (height <= 0) {
                height = 1;
            }
            if (builder.width != width || builder.height != height) {
                LOGGER.warning("Changed '" + code + "'/" + type + " emote size: " + builder.width + "x"
                        + builder.height + " -> " + width + "x" + height);
            }
        }
        this.width = width;
        this.height = height;
        this.streamRestrictions = builder.streamRestrictions;
        this.stream = builder.stream;
        this.emotesetInfo = builder.emotesetInfo;
        this.literal = builder.literal;
        this.numericId = builder.numericId;
        this.stringId = builder.stringId;
        this.creator = builder.creator;
        this.infos = builder.infos;
        this.isAnimated = builder.isAnimated;
        this.subType = builder.subtype;
    }
    
    private void createMatcher() {
        if (matcher == null) {
            String search = code;
            int flags = 0;
            
            if (type == Type.EMOJI) {
                // Some Emoji seemed to not compile without this, although not
                // sure why, but just do it just in case
                flags = Pattern.LITERAL;
            } else {
                // Any regular emotes should be separated by spaces
                if (literal) {
                    // Literal emotes come from a source that doesn't provide
                    // regex, but may contain regex special characters
                    search = Pattern.quote(search);
                }
                search = "(?<=^|\\s)"+search+"(?=$|\\s)";
            }
            
            // Compile the prepared Pattern
            try {
                matcher = Pattern.compile(search, flags).matcher("");
            } catch (PatternSyntaxException ex) {
                LOGGER.warning("Error compiling pattern for '" + search + "' [" + ex.getLocalizedMessage() + "]");
                // Compile a pattern that doesn't match anything, so a Matcher
                // is still available
                matcher = Pattern.compile("(?!)").matcher("");
            }
        }
    }
    
    /**
     * Gets the stream restrictions set for this Emoticon.
     * 
     * @return A copy of the restrictions (defensive copying) or null if no
     * restrictions were set.
     */
    public synchronized Set<String> getStreamRestrictions() {
        if (streamRestrictions == null) {
            return null;
        }
        return new HashSet<>(streamRestrictions);
    }
    
    public synchronized boolean hasStreamRestrictions() {
        return streamRestrictions != null;
    }
    
    public synchronized boolean streamRestrictionContains(String stream) {
        return streamRestrictions != null && streamRestrictions.contains(stream);
    }
    
    /**
     * The name of the stream associated with this emoticon. For Twitch emotes
     * this will be the name of the stream the subemote is from. For FFZ emotes
     * the name of the stream the emote is from for Feature Friday. Used for
     * display purposes, so it isn't necessarily lowercase.
     *
     * @return The name of the stream, or null if none is set
     * @see hasStreamSet()
     */
    public String getStream() {
        return stream;
    }
    
    /**
     * Whether a stream has been set.
     * 
     * @return true if a stream name has been set, false otherwise
     * @see getStream()
     */
    public boolean hasStreamSet() {
        return stream != null;
    }
    
    public synchronized void addInfos(Set<String> infosToAdd) {
        if (infos == null) {
            infos = new HashSet<>();
        }
        infos.addAll(infosToAdd);
    }
    
    public synchronized void addInfo(String info) {
        if (infos == null) {
            infos = new HashSet<>();
        }
        infos.add(info);
    }
    
    /**
     * Creates a copy of the info strings.
     * 
     * <p>Info strings are intended to be displayed to the user along with other
     * information about the emote. This is probably mainly going to be used for
     * FFZ/BTTV emotes.</p>
     * 
     * @return A TreeSet of info strings (defensive copying), or an empty Set if
     * no info strings are available
     */
    public synchronized Set<String> getInfos() {
        if (infos == null) {
            return new TreeSet<>();
        }
        return new TreeSet<>(infos);
    }
    
    public synchronized boolean isAnimated() {
        return isAnimated;
    }
    
    protected synchronized void setAnimated(boolean isAnimated) {
        this.isAnimated = isAnimated;
    }
    
    public boolean hasGlobalEmoteset() {
        return this.emoteSet == SET_GLOBAL || this.emoteSet == SET_UNDEFINED;
    }
    
    /**
     * Sets the name of the stream associated with this emote. This is only for
     * display/joining the channel (e.g. showing a subemenu with options to join
     * etc.).
     *
     * @param stream The name of the stream
     * @see getStream()
     */
    public void setStream(String stream) {
        this.stream = stream;
    }
    
    public void setEmotesetInfo(String info) {
        this.emotesetInfo = info;
    }
    
    public String getEmotesetInfo() {
        return emotesetInfo;
    }
    
    public boolean hasEmotesetInfo() {
        return emotesetInfo != null;
    }
    
    /**
     * Gets the matcher that can be used to find this emoticon in the given
     * text. Should probably only be used out of the EDT.
     * 
     * @param text
     * @return 
     */
    public Matcher getMatcher(String text) {
        createMatcher();
        return matcher.reset(text);
    }
    
    /**
     * Get a scaled EmoticonImage for this Emoticon.
     *
     * @param scaleFactor Scale Factor, default (no scaling) should be 1
     * @param maxHeight Maximum height in pixels, default (no max height) should
     * be 0
     * @param user
     * @return
     */
    public EmoticonImage getIcon(float scaleFactor, int maxHeight, EmoticonUser user) {
        if (images == null) {
            images = new HashSet<>();
        }
        EmoticonImage resultImage = null;
        for (EmoticonImage image : images) {
            if (image.scaleFactor == scaleFactor && image.maxHeight == maxHeight) {
                resultImage = image;
            }
        }
        if (resultImage == null) {
            resultImage = new EmoticonImage(scaleFactor, maxHeight);
        }
        resultImage.addUser(user);
        images.add(resultImage);
        return resultImage;
    }
    
    /**
     * Removes all currently cached images.
     */
    public void clearImages() {
        if (images != null) {
            images.clear();
        }
    }
    
    /**
     * Requests an ImageIcon to be loaded, returns the default icon at first,
     * but starts a SwingWorker to get the actual image.
     * 
     * @param user
     * @return 
     */
    public EmoticonImage getIcon(EmoticonUser user) {
        return getIcon(1, 0, user);
    }
    
    /**
     * Creates a unique identifier for this emote for use with the size cache.
     * 
     * @return A id String for this emote
     */
    private String getCachedSizeId() {
        if (type == Type.TWITCH) {
            return type+"."+numericId;
        }
        if (type == Type.BTTV) {
            return type+"."+stringId;
        }
        return null;
    }
    
    /**
     * Looks up the cached size for this emote.
     * 
     * @return The cache size for this emote or null if none exists
     */
    private Dimension getCachedSize() {
        return EmoticonSizeCache.getSize(getCachedSizeId());
    }
    
    /**
     * Sets the cached size for this emote. This should be the actual size taken
     * from the image.
     * 
     * @param w The width
     * @param h The height
     */
    private void setCachedSize(int w, int h) {
        if ((type == Type.TWITCH && numericId != ID_UNDEFINED)
                || (type == Type.BTTV && stringId != null)) {
            EmoticonSizeCache.setSize(getCachedSizeId(), w, h);
        }
    }
    
    /**
     * Get the size of this emoticon without scaling. If no size was given when
     * creating the Emoticon, the size is looked up in the size cache or if that
     * yields no result, the default width and height constants are used
     * (28x28).
     *
     * @return The default size of this emoticon
     */
    private Dimension getDefaultSize() {
        if (this.width == -1 || this.height == -1) {
            Dimension d = getCachedSize();
            if (d != null) {
                return d;
            } else {
                return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            }
        }
        return new Dimension(width, height);
    }
    
    /**
     * Scale the given Dimension based on the given settings. Also checks if the
     * resulting size is within reasonable boundaries.
     *
     * @param d The dimension to modify
     * @param scaleFactor The scale factor, values smaller or equal to 0 are
     * ignored
     * @param maxHeight The maximum height, values smaller or equal to 0 are
     * ignored
     * @return A new Dimension that has been scaled accordingly
     */
    private static Dimension getScaledSize(Dimension d, float scaleFactor, int maxHeight) {
        float scaledWidth = d.width;
        float scaledHeight = d.height;
        
        if (scaleFactor > 0) {
            scaledWidth *= scaleFactor;
            scaledHeight *= scaleFactor;
        }
        
        if (maxHeight > 0 && scaledHeight > maxHeight) {
            scaledWidth = scaledWidth / (scaledHeight / maxHeight);
            scaledHeight = maxHeight;
        }
        
        /**
         * Convert into int before checking
         */
        int resultWidth = (int)scaledWidth;
        int resultHeight = (int)scaledHeight;
        if (resultWidth < 1) {
            resultWidth = 1;
        }
        if (resultHeight < 1) {
            resultHeight = 1;
        }
        
        /**
         * This shouldn't really happen, but just in case, so no ridicously
         * huge (default) image is created.
         */
        if (resultWidth > MAX_SCALED_WIDTH) {
            resultWidth = MAX_SCALED_WIDTH;
        }
        if (resultHeight > MAX_SCALED_HEIGHT) {
            resultHeight = MAX_SCALED_HEIGHT;
        }
        return new Dimension(resultWidth, resultHeight);
    }
    
    /**
     * Gets the size from the image, including correcting for the given URL
     * factor for Twitch emotes.
     * 
     * @param icon The ImageIcon to get the size from
     * @param urlFactor Only 1 and 2 should be valid at the moment
     * @return The size of the given image
     */
    private Dimension getSizeFromImage(ImageIcon icon, int urlFactor) {
        int imageWidth = icon.getIconWidth();
        int imageHeight = icon.getIconHeight();
        if (urlFactor > 1) {
            imageWidth /= urlFactor;
            imageHeight /= urlFactor;
        }
        return new Dimension(imageWidth, imageHeight);
    }
    
    private Image getScaledImage(Image img, int w, int h) {
        return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }
    

    /**
     * A Worker class to load the Icon. Not doing this in it's own thread
     * can lead to lag when a lot of new icons are being loaded.
     */
    private class IconLoader extends SwingWorker<ImageIcon,Object> {

        private final EmoticonImage image;
        
        public IconLoader(EmoticonImage image) {
            this.image = image;
        }
        
        @Override
        protected ImageIcon doInBackground() throws Exception {
            
            // Get the assumed size or size loaded from the size cache
            Dimension defaultSize = getDefaultSize();
            Dimension scaledSize = getScaledSize(defaultSize, image.scaleFactor,
                    image.maxHeight);
            
            //System.out.println(defaultSize+" "+scaledSize+" "+image);
            
            // Determine which URL to load the image from
            String url = Emoticon.this.url;
            int urlFactor = 1;
            if (type == Type.TWITCH || type == Type.BTTV || type == Type.FFZ || type == Type.EMOJI) {
                if (scaledSize.width > defaultSize.width) {
                    urlFactor = 2;
                    if (isAnimated() && (float)scaledSize.width / defaultSize.width < 1.6) {
                        // For animated emotes, which currently are not resized,
                        // only load the 2x version if scale is high enough,
                        // otherwise it just looks ridiculous
                        urlFactor = 1;
                    }
                }
                String builtUrl = getEmoteUrl(urlFactor);
                if (builtUrl == null) {
                    LOGGER.warning("Couldn't build URL for "+type+"/"+code);
                    //return null;
                } else {
                    url = builtUrl;
                }
            }

            ImageIcon icon = loadEmote(url);
            image.setLoadedFrom(url);
            
            /**
             * If an error occured loading the image, return null.
             */
            if (icon == null) {
                return null;
            }
            
            /**
             * Only doing this on ERRORED, waiting for COMPLETE would not allow
             * animated GIFs to load
             */
            if (icon.getImageLoadStatus() == MediaTracker.ERRORED) {
                icon.getImage().flush();
                return null;
            }
            
            /**
             * Determine the size to actually scale the image to. If a size has
             * already been specified (when importing the emote from an API or
             * when no size was given but the actual image size was set before)
             * then use the scaledSize as calculated before. Otherwise use the
             * actual size from the image to calculate the scaled targetSize.
             */
            Dimension actualImageSize = getSizeFromImage(icon, urlFactor);
            Dimension targetSize;
            if (width == -1 || height == -1) {
                targetSize = getScaledSize(actualImageSize, image.scaleFactor,
                    image.maxHeight);
            } else {
                targetSize = scaledSize;
            }
            
            /**
             * Scale to targetSize, unless image is already the correct size.
             * Don't resize files ending on .gif, which should only apply to
             * some BTTV emotes which are animated. Filename is not necessarily
             * available though, it also seems like animated GIFs are not fully
             * loaded according to the MediaTracker though, so check that as
             * well. Not quite sure what that means exactly though.
             */
            if ((icon.getIconWidth() != targetSize.width
                    || icon.getIconHeight() != targetSize.height)
                    && (icon.getDescription() == null || !icon.getDescription().startsWith("GIF"))) {
                Image scaled = getScaledImage(icon.getImage(), targetSize.width,
                        targetSize.height);
                icon.setImage(scaled);
            }

            /**
             * If no size is set for the image, use the actual image size (and
             * save it for later if not the default emote size).
             */
            if (width == -1 || height == -1) {
                width = actualImageSize.width;
                height = actualImageSize.height;
                if (width != DEFAULT_WIDTH || height != DEFAULT_HEIGHT) {
                    // setCachedSize checks for type
                    setCachedSize(width, height);
                }
            }
            return icon;
        }
        
        private ImageIcon loadEmote(String url) {
            try {
                return ImageCache.getImage(new URL(url), "emote_" + type, CACHE_TIME);
            } catch (MalformedURLException ex) {
                LOGGER.warning("Invalid url for " + code + ": " + url);
                return null;
            }
        }
        
        /**
         * The image should be done loading, replace the defaulticon with the
         * actual loaded icon and tell the user that it's loaded.
         */
        @Override
        protected void done() {
            try {
                // This is null if an error occured
                ImageIcon loadedIcon = get();
                if (loadedIcon == null) {
                    image.setLoadingError();
                } else {
                    image.setImageIcon(loadedIcon);
                }
                image.setLoadingDone();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.warning("Unexpected error when loading emoticon: "+ex);
            }
        }
    }
    
    @Override
    public String toString() {
        return code;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public boolean matchesUser(User user) {
        if (user == null) {
            return true;
        }
        if (emoteSet > Emoticon.SET_GLOBAL
                && !user.getEmoteSet().contains(emoteSet)) {
            return false;
        }
        if (hasStreamRestrictions()
                && !streamRestrictionContains(user.getStream())) {
            return false;
        }
        return true;
    }
    
    public boolean allowedForStream(String stream) {
        return !hasStreamRestrictions() || streamRestrictionContains(stream);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Emoticon other = (Emoticon) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.subType != other.subType) {
            return false;
        }
        if (!Objects.equals(this.code, other.code)) {
            return false;
        }
        if (this.emoteSet != other.emoteSet) {
            return false;
        }
        if (!Objects.equals(this.streamRestrictions, other.streamRestrictions)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.type);
        hash = 59 * hash + Objects.hashCode(this.subType);
        hash = 59 * hash + Objects.hashCode(this.code);
        hash = 59 * hash + this.emoteSet;
        hash = 59 * hash + Objects.hashCode(this.streamRestrictions);
        return hash;
    }

    public static interface EmoticonUser {

        void iconLoaded(Image oldImage, Image newImage, boolean sizeChanged);
    }
    
    /**
     * A single image for this emoticon which may be scaled. Each emote can have
     * more than one image, when images with different scaling have been
     * requested.
     * 
     * <p>
     * The actual image is loaded asynchronous once {@link getImageIcon()} is
     * called.
     * </p>
     */
    public class EmoticonImage {
        
        private ImageIcon icon;
        public final float scaleFactor;
        public final int maxHeight;
        
        private Set<EmoticonUser> users;
      
        /**
         * The source the image was loaded from. This may not be the same for
         * all images, because image providers may have different URLs for
         * different image sizes.
         */
        private String loadedFrom;
        
        private boolean loading = false;
        private boolean loadingError = false;
        private volatile int loadingAttempts = 0;
        private long lastLoadingAttempt;
        
        public EmoticonImage(float scaleFactor, int maxHeight) {
            this.scaleFactor = scaleFactor;
            this.maxHeight = maxHeight;
        }
        
        /**
         * Gets the ImageIcon for this EmoticonImage. When this is called for
         * the first time, the image will be loaded asynchronously and a
         * temporary default image of the same size is returned (if the size is
         * known, otherwise it is based on the default size).
         *
         * @return 
         */
        public ImageIcon getImageIcon() {
            if (icon == null) {
                /**
                 * Note: The temporary image (as well as the actual image) are
                 * used as a key for GIF handling in ChannelTextPane, so it is
                 * important not to reuse the same temporary image across
                 * different emotes.
                 */
                icon = getDefaultIcon();
                if (type != Type.NOT_FOUND_FAVORITE) {
                    loadImage();
                }
            } else if (loadingError) {
                if (loadImage()) {
                    LOGGER.warning("Trying to load " + code + " again (" + loadedFrom + ")");
                }
            }
            return icon;
        }
        
        /**
         * Get the Emoticon object this EmoticonImage is a part of.
         *
         * @return
         */
        public Emoticon getEmoticon() {
            return Emoticon.this;
        }

        /**
         * Gets the URL in String form where the image was loaded from.
         * 
         * @return The URL or null if the image hasn't been loaded yet
         */
        public String getLoadedFrom() {
            return loadedFrom;
        }
        
        /**
         * Try to load the image, if it's not already loading and if the max
         * loading attempts are not exceeded.
         *
         * @return true if the image will be attempted to be loaded, false
         * otherwise
         */
        private boolean loadImage() {
            if (!loading && loadingAttempts < MAX_LOADING_ATTEMPTS
                    && System.currentTimeMillis() - lastLoadingAttempt > LOADING_ATTEMPT_DELAY) {
                loading = true;
                loadingError = false;
                loadingAttempts++;
                lastLoadingAttempt = System.currentTimeMillis();
                (new IconLoader(this)).execute();
                return true;
            }
            return false;
        }
        
        private void setLoadingError() {
            setImageIcon(new ImageIcon(getDefaultImage(true)));
            loadingError = true;
        }
        
        private void setImageIcon(ImageIcon newIcon) {
            boolean sizeChanged = icon.getIconWidth() != newIcon.getIconWidth()
                    || icon.getIconHeight() != newIcon.getIconHeight();
            Image oldImage = icon.getImage();
            icon.setImage(newIcon.getImage());
            icon.setDescription(newIcon.getDescription());
            informUsers(oldImage, newIcon.getImage(), sizeChanged);
        }
        
        private void setLoadedFrom(String url) {
            this.loadedFrom = url;
        }
        

        
        /**
         * Either error or successfully loaded.
         */
        private void setLoadingDone() {
            loading = false;
        }
        
        private void addUser(EmoticonUser user) {
            if (users == null) {
                users = Collections.newSetFromMap(
                        new WeakHashMap<EmoticonUser, Boolean>());
            }
            users.add(user);
        }
        
        private void informUsers(Image oldImage, Image newImage, boolean sizeChanged) {
            for (EmoticonUser user : users) {
                user.iconLoaded(oldImage, newImage, sizeChanged);
            }
        }
        
        /**
         * Construct a default icon based on the size of this emoticon.
         *
         * @return
         */
        private ImageIcon getDefaultIcon() {
            return new ImageIcon(getDefaultImage(false));
        }
        
        /**
         * Construct a default image based on the size of this emoticon.
         *
         * @param error If true, uses red color to indicate an error.
         * @return
         */
        private Image getDefaultImage(boolean error) {
            // Determine the assumed size of the emote, for the placerholder image
            Dimension d = getScaledSize(getDefaultSize(), scaleFactor, maxHeight);
            int width = d.width;
            int height = d.height;

            BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Graphics g = res.getGraphics();
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(Color.LIGHT_GRAY);
            if (error) {
                g.setColor(Color.red);
            }
            int sWidth = g.getFontMetrics().stringWidth("[x]");
            g.drawString("[x]", width / 2 - sWidth / 2, height / 2);

            g.dispose();
            return res;
        }
        
        

        @Override
        public String toString() {
            return scaleFactor+"/"+maxHeight+"/"+icon;
        }
        
        /**
         * Builds a String of the size of this image, including the original
         * size if there is a scaled size, meant for display.
         * 
         * @return The String containing the width and height
         */
        public String getSizeString() {
            int scaledWidth = icon.getIconWidth();
            int scaledHeight = icon.getIconHeight();
            if (width == -1 || height == -1) {
                return scaledWidth+"x"+scaledHeight;
            }
            if (scaledWidth != width || scaledHeight != height) {
                return scaledWidth + "x" + scaledHeight + " (" + width + "x" + height + ")";
            }
            return width + "x" + height;
        }
    }
}
