
package chatty.util.api;

import chatty.Helper;
import chatty.User;
import chatty.util.ImageCache.ImageResult;
import chatty.util.StringUtil;
import chatty.util.api.CachedImage.ImageType;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import chatty.util.api.CachedImage.CachedImageUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    
    public static final String SET_GLOBAL = "0";
    
    /**
     * Undefined means an emoteset is not defined for this emote at all.
     */
    public static final String SET_NONE = null;
    
    /**
     * Unknown means that an emoteset may be required, but it's not known.
     */
    public static final String SET_UNKNOWN = "";
    
    /**
     * Note that the declaration order is relevant for sorting by Type.
     */
    public static enum Type {
        TWITCH("twitch", "Twitch", TypeCategory.OFFICIAL),
        CUSTOM2("chattylocal", "Custom2", TypeCategory.OFFICIAL),
        FFZ("ffz", "FFZ", TypeCategory.THIRD_PARTY),
        BTTV("bttv", "BTTV", TypeCategory.THIRD_PARTY),
        SEVENTV("7tv", "7TV", TypeCategory.THIRD_PARTY),
        CUSTOM("custom", "Custom", TypeCategory.OTHER),
        EMOJI("emoji", "Emoji", TypeCategory.OTHER),
        NOT_FOUND_FAVORITE("fav", "NotFoundFavorite", TypeCategory.OTHER);
        
        // Must not be changed
        public String id;
        // For display
        public String label;
        public TypeCategory category;
        
        Type(String id, String label, TypeCategory category) {
            this.id = id;
            this.label = label;
            this.category = category;
        }
        
        public static Type fromId(String id) {
            for (Type type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return null;
        }
        
    }
    
    public static enum TypeCategory {
        OFFICIAL, THIRD_PARTY, OTHER
    }
    
    public static enum SubType {
        REGULAR, FEATURE_FRIDAY, EVENT, CHEER, FOLLOWER
    }
    
    // Assumed width/height if there is none given
    private static final int DEFAULT_WIDTH = 28;
    private static final int DEFAULT_HEIGHT = 28;
    
    private static final float MAX_WIDTH = 100;
    private static final float MAX_HEIGHT = 50;
    
    
    
    public final Type type;
    public final SubType subType;
    public final String code;
    public final String regex;
    public final String emoteset;
    private final Set<String> streamRestrictions;
    public final String url;
    public final boolean literal;
    public final String stringId;
    public final String stringIdAlias;
    public final String urlX2;
    public final String creator;
    
    private String stream;
    private ArrayList<String> infos;
    private String emotesetInfo;
    private boolean isAnimated;
    
    private volatile int width;
    private volatile int height;

    private Matcher matcher;
    
    private CachedImageManager<Emoticon> images;

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
        
        private String regex;
        private SubType subtype;
        private String urlX2;
        private int width = -1;
        private int height = -1;
        private boolean literal = false;
        private String stream;
        private String emotesetInfo;
        private Set<String> streamRestrictions;
        private ArrayList<String> infos;
        private String emoteset = SET_NONE;
        private String stringId = null;
        private String stringIdAlias = null;
        private String creator;
        private boolean isAnimated = false;
        
        public Builder(Type type, String search, String url) {
            this.type = type;
            this.search = search;
            this.url = url;
        }
        
        public Builder setRegex(String regex) {
            this.regex = regex;
            return this;
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
        
        public Builder setEmoteset(String emoteset) {
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
        
        public Builder setStringId(String id) {
            this.stringId = id;
            return this;
        }
        
        public Builder setStringIdAlias(String id) {
            this.stringIdAlias = id;
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
                    infos = new ArrayList<>(1);
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
     * Get the built URL for this emote if applicable to the emote type. It's
     * important that the returned URL matches the URL factor, so that the
     * ImageRequest can correctly calculate the base size. It will attempt to
     * get factor 1 if 2 returns null.
     * 
     * @param factor The size factor (1 or 2)
     * @param imageType
     * @return The URL as a String or null if none could created or not of an
     * applicable type
     */
    public String getEmoteUrl(int factor, ImageType imageType) {
        if (type == Type.TWITCH || type == Type.CUSTOM2) {
            if (stringId != null) {
                return getTwitchEmoteUrlById(stringId, factor, imageType);
            }
        } else if (type == Type.BTTV && stringId != null) {
            return getBttvEmoteUrl(stringId, factor);
        } else if (type == Type.SEVENTV) {
            return getSevenTVEmoteUrl(stringId, factor);
        } else if (type == Type.FFZ) {
            return getFFZUrl(factor);
        } else if (factor == 1) {
            return url;
        }
        return null;
    }
    
    public static ImageType makeImageType(boolean animated) {
        return animated ? ImageType.ANIMATED_DARK : ImageType.STATIC;
    }
    
    public static String getTwitchEmoteUrlById(String id, int factor, ImageType imageType) {
        switch (imageType) {
            case STATIC:
                return String.format(Locale.ROOT, "https://static-cdn.jtvnw.net/emoticons/v2/%s/static/dark/%d.0", id, factor);
            case ANIMATED_DARK:
                return String.format(Locale.ROOT, "https://static-cdn.jtvnw.net/emoticons/v2/%s/default/dark/%d.0", id, factor);
            case ANIMATED_LIGHT:
                return String.format(Locale.ROOT, "https://static-cdn.jtvnw.net/emoticons/v2/%s/default/light/%d.0", id, factor);
        }
        return null;
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
    
    public String getSevenTVEmoteUrl(String id, int factor) {
        if (StringUtil.isNullOrEmpty(id) || factor > 4) {
            return null;
        }
        return String.format("https://cdn.7tv.app/emote/%s/%dx", id, factor);
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
        
        this.regex = builder.regex;

        this.type = builder.type;
        this.emoteset = builder.emoteset;
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
        this.stringId = builder.stringId;
        this.stringIdAlias = builder.stringIdAlias;
        this.creator = builder.creator;
        this.infos = builder.infos;
        if (this.infos != null) {
            this.infos.trimToSize();
        }
        this.isAnimated = builder.isAnimated;
        this.subType = builder.subtype;
    }
    
    /**
     * Some emote codes contain regex characters, while not being intended to be
     * interpreted as regex (and others still consist of an actual regex).
     * 
     * This serves as a backup in case some aren't turned back into regex.
     */
    private static final Set<String> LITERAL = new HashSet<>(Arrays.asList(new String[]{
        "8-)", ":|", ";)", ">(", ":\\", ":)", ":-)", "R)", ":(", ":-(", "B)", "B-)"
    }));
    
    private void createMatcher() {
        if (matcher == null) {
            // Use separate regex if available (e.g. for smilies)
            String search = !StringUtil.isNullOrEmpty(regex) ? regex : code;
            int flags = 0;
            
            if (type == Type.EMOJI) {
                /**
                 * Match variation selectors for text and emoji style, if
                 * present, so it's included in the Emoji image and not visible.
                 * If \uFE0E (text style) is at the end of the match, it should
                 * not be turned into an image (although not sure how often that
                 * actually occurs).
                 * 
                 * http://mts.io/2015/04/21/unicode-symbol-render-text-emoji/
                 */
                search = Pattern.quote(search)+"[\uFE0E\uFE0F]?";
            } else {
                if (search.length() < 4) {
                    // Turn some of the "smiley" emotes back into regex (they
                    // still seem to be parsed with the regex serverside)
                    // This is only the fallback for smilies that still come
                    // from the Twitch API for now
                    search = Emoticons.toRegex(search);
                }
                // Any regular emotes should be separated by spaces
                if (literal || LITERAL.contains(search)) {
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
                LOGGER.warning(String.format("Error compiling emote pattern: '%s' (id: %s, type: %s) [%s]",
                        search, stringId, type, ex.getLocalizedMessage()));
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
    public synchronized String getStream() {
        return stream;
    }
    
    /**
     * Whether a stream has been set.
     * 
     * @return true if a stream name has been set, false otherwise
     * @see getStream()
     */
    public synchronized boolean hasStreamSet() {
        return stream != null;
    }
    
    public synchronized void addInfos(Set<String> infosToAdd) {
        if (infos == null) {
            infos = new ArrayList<>();
        }
        infos.addAll(infosToAdd);
        infos.trimToSize();
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
        return new TreeSet<String>(infos);
    }
    
    public synchronized boolean isAnimated() {
        return isAnimated;
    }
    
    protected synchronized void setAnimated(boolean isAnimated) {
        this.isAnimated = isAnimated;
    }
    
    public boolean hasGlobalEmoteset() {
        return isGlobalEmoteset(emoteset);
    }
    
    public static boolean isGlobalEmoteset(String emoteset) {
        return emoteset == null || emoteset.equals(SET_GLOBAL);
    }
    
    /**
     * Sets the name of the stream associated with this emote. This is only for
     * display/joining the channel (e.g. showing a subemenu with options to join
     * etc.).
     *
     * @param stream The name of the stream
     * @see getStream()
     */
    public synchronized void setStream(String stream) {
        this.stream = stream;
    }
    
    public synchronized void setEmotesetInfo(String info) {
        this.emotesetInfo = info;
    }
    
    public synchronized String getEmotesetInfo() {
        return emotesetInfo;
    }
    
    public synchronized boolean hasEmotesetInfo() {
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
     * Get a scaled image for this Emoticon. Should only be called from the EDT.
     *
     * @param scaleFactor Scale Factor, default (no scaling) should be 1
     * @param maxHeight Maximum height in pixels, default (no max height) should
     * be 0
     * @param imageType
     * @param user
     * @return
     */
    public CachedImage<Emoticon> getIcon(float scaleFactor, int maxHeight, ImageType imageType, CachedImageUser user) {
        if (images == null) {
            images = new CachedImageManager<>(this, new CachedImage.CachedImageRequester() {
                @Override
                public String getImageUrl(int scale, CachedImage.ImageType type) {
                    return getEmoteUrl(scale, type);
                }

                @Override
                public Dimension getBaseSize() {
                    return getDefaultSize();
                }
                
                @Override
                public boolean forceBaseSize() {
                    return width != -1 && height != -1;
                }

                @Override
                public void imageLoaded(ImageResult result) {
                    if (width == -1 || height == -1) {
                        width = result.actualBaseSize.width;
                        height = result.actualBaseSize.height;
                        if (width != DEFAULT_WIDTH || height != DEFAULT_HEIGHT) {
                            // setCachedSize checks for type
                            setCachedSize(width, height);
                        }
                        else {
                            /**
                             * In case an invalid/different size was cached
                             * before, but now it's the default size.
                             */
                            removeCachedSize();
                        }
                    }
                }

                @Override
                public boolean loadImage() {
                    return true;
                }
            }, ("emote_" + type).intern());
        }
        return images.getIcon(scaleFactor, maxHeight, null, imageType, user);
    }
    
    /**
     * Removes all currently cached images. Should probably be called from the
     * EDT.
     */
    public void clearImages() {
        if (images != null) {
            images.clearImages();
        }
    }
    
    /**
     * Set unused image objects to be garbage collected. Should only be called
     * from the EDT.
     *
     * @param imageExpireMinutes
     * @return
     */
    public int clearOldImages(int imageExpireMinutes) {
        if (images != null) {
            return images.clearOldImages(imageExpireMinutes);
        }
        return 0;
    }
    
    /**
     * Requests an ImageIcon to be loaded, returns the default icon at first,
     * but starts a SwingWorker to get the actual image. Should be called from
     * the EDT.
     * 
     * @param user
     * @return 
     */
    public CachedImage<Emoticon> getIcon(CachedImageUser user) {
        return getIcon(1, 0, ImageType.STATIC, user);
    }
    
    /**
     * Creates a unique identifier for this emote for use with the size cache.
     * 
     * @return A id String for this emote
     */
    private String getCachedSizeId() {
        if (type == Type.TWITCH) {
            return type+"."+stringId;
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
        if ((type == Type.TWITCH && stringId != null)
                || (type == Type.BTTV && stringId != null)) {
            EmoticonSizeCache.setSize(getCachedSizeId(), w, h);
        }
    }
    
    private void removeCachedSize() {
        EmoticonSizeCache.removeSize(getCachedSizeId());
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
    
    public boolean matchesUser(User user, Set<String> accessToSets) {
        if (user == null) {
            return true;
        }
        if (!hasGlobalEmoteset()
                && (accessToSets == null || !accessToSets.contains(emoteset))) {
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
        if (!Objects.equals(this.emoteset, other.emoteset)) {
            return false;
        }
        if (!Objects.equals(this.streamRestrictions, other.streamRestrictions)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.type);
        hash = 17 * hash + Objects.hashCode(this.subType);
        hash = 17 * hash + Objects.hashCode(this.code);
        hash = 17 * hash + Objects.hashCode(this.emoteset);
        hash = 17 * hash + Objects.hashCode(this.streamRestrictions);
        return hash;
    }

}
