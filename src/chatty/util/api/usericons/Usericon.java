
package chatty.util.api.usericons;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.Highlighter;
import chatty.gui.components.textpane.ChannelTextPane;
import chatty.util.colors.HtmlColors;
import chatty.util.ImageCache;
import chatty.util.ImageCache.ImageRequest;
import chatty.util.ImageCache.ImageResult;
import chatty.util.StringUtil;
import chatty.util.api.CachedImage;
import chatty.util.api.CachedImageManager;
import chatty.util.irc.IrcBadges;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;

/**
 * A single usericon (badge) with an image and information on where (channel)
 * and for who (user properties) it should be displayed.
 * 
 * @author tduva
 */
public class Usericon implements Comparable {
    
    private static final Logger LOGGER = Logger.getLogger(Usericon.class.getName());
    
    /**
     * How long to cache the usericon images (in seconds).
     */
    public static final int CACHE_TIME = 60*60*24*3;
    
    private static final Dimension DEFAULT_ICON_SIZE = new Dimension(18, 18);
    
    private static final Set<String> statusDef = new HashSet<>(Arrays.asList(
            "$mod", "$sub", "$admin", "$staff", "$turbo", "$broadcaster", "$bot",
            "$globalmod", "$anymod", "$vip"));
    
    /**
     * The type determines whether it should replace any of the default icons
     * (which also assumes they are mainly requested if the user is actually
     * mod, turbo, etc.) or if it should be shown in addition to the default
     * icons (addon).
     */
    public enum Type {
        
        MOD(0, "Moderator", "MOD", "@", "moderator", HtmlColors.decode("#34ae0a")),
        TURBO(1, "Turbo", "TRB", "+", "turbo", HtmlColors.decode("#6441a5")),
        BROADCASTER(2, "Broadcaster", "BRC", "~", "broadcaster", HtmlColors.decode("#e71818")),
        STAFF(3, "Staff", "STA", "&", "staff", HtmlColors.decode("#200f33")),
        ADMIN(4, "Admin", "ADM", "!", "admin", HtmlColors.decode("#faaf19")),
        SUB(5, "Subscriber", "SUB", "%", "subscriber", null),
        ADDON(6, "Addon", "ADD", "'", null, null),
        GLOBAL_MOD(7, "Global Moderator", "GLM", "*", "global_mod", HtmlColors.decode("#0c6f20")),
        BOT(8, "Bot", "BOT", "^", null, null),
        TWITCH(9, "Twitch Badge", "TWB", null, null, null),
        PRIME(10, "Prime", "PRM", "+", "premium", null),
        BITS(11, "Bits", "BIT", "$", "bits", null),
        OTHER(12, "Other", "OTH", "'", null, null),
        VIP(13, "VIP", "VIP", "!", "vip", null),
        HL(14, "Highlighted by channel points", "HL", "'", null, null),
        CHANNEL_LOGO(15, "Channel Logo", "CHL", null, null, null),
        FOUNDER(16, "Founder", "FND", "%", "founder", null),
        ALL(17, "All Types", "ALL", "", null, null),
        FIRSTMSG(18, "First Message in Channel", "FMG", "'", null, null),
        UNDEFINED(-1, "Undefined", "UDF", null, null, null);
        
        public Color color;
        public String label;
        public String shortLabel;
        public int id;
        public String symbol;
        public String badgeId;
        
        Type(int id, String label, String shortLabel, String symbol,
                String badgeId, Color color) {
            this.color = color;
            this.label = label;
            this.shortLabel = shortLabel;
            this.id = id;
            this.symbol = symbol;
            this.badgeId = badgeId;
        }
        
        public static Type getTypeFromId(int typeId) {
            for (Type type : values()) {
                if (type.id == typeId) {
                    return type;
                }
            }
            return UNDEFINED;
        }
        
    }
    
    /**
     * On creation the match type is determined, which means what type of
     * restriction is set. This is done for easier handling later, so the
     * restriction doesn't have to be parsed everytime.
     */
    public enum MatchType {
        CATEGORY, UNDEFINED, ALL, STATUS, NAME, COLOR, MATCH
    }
    
    /**
     * The type of icon based on the source.
     */
    public static final int SOURCE_ANY = -1;
    public static final int SOURCE_FALLBACK = 0;
    public static final int SOURCE_TWITCH = 5;
    public static final int SOURCE_TWITCH2 = 6;
    public static final int SOURCE_FFZ = 10;
    public static final int SOURCE_OTHER = 11;
    public static final int SOURCE_CUSTOM = 20;
    
    /**
     * Which kind of icon (replacing mod, sub, turbo, .. or Addon).
     */
    public final Type type;
    
    /**
     * Where the icon comes from (Twitch, FFZ, Custom, ..).
     */
    public final int source;
    
    /**
     * The channel restriction, which determines which channel(s) the icon
     * should be used for. This doesn't necessarily only contain the channel
     * itself, but possibly also modifiers.
     * 
     * Can never be null.
     */
    public final String channelRestriction;
    
    /**
     * The actual channel from the channel restriction. If no or an invalid
     * channel is specified in the channel restriction, then this is empty.
     */
    public final String channel;
    
    /**
     * The URL the image is loaded from
     */
    public final URL url;
    public final URL url2;
    
    public final Color color;
    
    /**
     * The unscaled size, if not set the default size is used
     */
    public final Dimension baseImageSize;
    
    /**
     * If set, the image will be resized to this size
     */
    public final Dimension targetImageSize;
    
    /**
     * The restriction
     */
    public final String restriction;
    
    /**
     * The filename of a locally loaded custom emoticon. This can be used to
     * more easily load/save that setting.
     */
    public final String fileName;
    
    /**
     * Which badge id/version this Usericon should match. This is mostly used
     * when a Twitch Badge is requested, to match up the id/version.
     */
    public final BadgeType badgeType;
    
    /**
     * This is a user restriction, which checks if that user has the given
     * Twitch Badge id/version.
     */
    public final BadgeType badgeTypeRestriction;
    
    /**
     * Whether this Usericon has no image because it's supposed to remove the
     * matching badge.
     */
    public final boolean removeBadge;
    
    
    /**
     * Data that is derived from other fields and not directly saved from
     * constructor arguments.
     */
    /**
     * The image loaded from the given {@literal url}
     */
    public final boolean hasRegularImage;
    
    /**
     * The match type is derived from {@literal restriction}, to make it easier
     * to check what to match later.
     */
    public final MatchType matchType;
    
    /**
     * The addressbook category to match (if given) in {@literal id}.
     */
    public final String category;
    
    /**
     * Match user-related prefixes based on the Highlighting format.
     */
    public final Highlighter.HighlightItem match;
    
    /**
     * This is {@code true} if the channel restriction should be reversed, which
     * means all channels BUT the one specified should match.
     */
    public final boolean channelInverse;
    
    /**
     * Color restriction. Maybe be null.
     */
    public final Color colorRestriction;
    
    public final String restrictionValue;
    
    public final Set<String> usernames;
    public final Set<String> userids;
    
    public final boolean stop;
    public final boolean first;
    public final String positionValue;
    public final UsericonPosition position;
    
    public final String metaTitle;
    public final String metaDescription;
    public final String metaUrl;
    
    /**
     * Creates a new {@literal Userimage}, which will try to load the image from
     * the given URL. If the loading fails, the {@literal image} field will be
     * {@literal null}.
     * 
     * @param builder
     */
    public Usericon(Builder builder) {
        this.type = builder.type;
        this.fileName = builder.fileName;
        this.source = builder.source;
        this.badgeType = builder.badgeType != null ? builder.badgeType : BadgeType.EMPTY;
        this.metaTitle = builder.metaTitle;
        this.metaDescription = builder.metaDescription;
        this.metaUrl = builder.metaUrl;

        //---------------------
        // Channel Restriction
        //---------------------
        String chan = builder.channel;
        if (chan != null) {
            chan = chan.trim();
            channelRestriction = chan;
            if (chan.startsWith("!")) {
                channelInverse = true;
                chan = chan.substring(1);
            } else {
                channelInverse = false;
            }
        } else {
            channelRestriction = "";
            channelInverse = false;
        }
        chan = Helper.toValidChannel(chan);
        if (chan == null) {
            chan = "";
        }
        this.channel = chan;
        
        //-----------------------
        // Usernames Restriction
        //-----------------------
        this.usernames = builder.usernames;
        this.userids = builder.userids;

        //----------------------
        // Image/Image Location
        //----------------------
        this.url = builder.url;
        this.url2 = builder.url2;
        this.color = builder.color;
        this.targetImageSize = builder.targetImageSize;
        this.baseImageSize = builder.baseImageSize;
        
        // If no url is set, assume that no image is supposed to be used
        if (builder.url == null) {
            removeBadge = true;
        } else {
            removeBadge = false;
        }
        if (fileName != null && fileName.startsWith("$")) {
            hasRegularImage = false;
        } else {
            hasRegularImage = true;
        }
        
        //-------------
        // Restriction
        //-------------
        String restrict = builder.restriction;
        if (restrict != null) {
            restrict = restrict.trim();
            this.restriction = restrict;
            if (restrict.contains("$stop")) {
                restrict = restrict.replace("$stop", "").trim();
                stop = true;
            } else {
                stop = false;
            }
            if (restrict.contains("$first")) {
                restrict = restrict.replace("$first", "").trim();
                first = true;
            } else {
                first = false;
            }
            Pattern p = Pattern.compile(".*(\\$badge:([^\\s]+)).*");
            Matcher m = p.matcher(restrict);
            if (restrict.contains("$badge:") && m.matches()) {
                restrict = restrict.replace(m.group(1), "").trim();
                badgeTypeRestriction = BadgeType.parse(m.group(2));
            } else {
                badgeTypeRestriction = BadgeType.EMPTY;
            }
            
            // From this point on, the restriction itself isn't modified
            restrictionValue = restrict;
            
            // Check if a category was specified as id
            if (restrict.startsWith("$cat:") && restrict.length() > 5) {
                category = restrict.substring(5);
            } else {
                category = null;
            }
            
            if (restrict.startsWith("$m:") && restrict.length() > "$m:".length()) {
                match = new Highlighter.HighlightItem(restrict.substring("$m:".length()), "badge");
            }
            else {
                match = null;
            }
            
            if (restrict.startsWith("#") && restrict.length() == 7) {
                colorRestriction = HtmlColors.decode(restrict, null);
            } else if (restrict.startsWith("$color:") && restrict.length() > 7) {
                colorRestriction = HtmlColors.decode(restrict.substring(7), null);
            } else {
                colorRestriction = null;
            }
            
            // Save the type
            if (restrict.startsWith("$cat:") && restrict.length() > 5) {
                matchType = MatchType.CATEGORY;
            } else if (match != null) {
                matchType = MatchType.MATCH;
            } else if (colorRestriction != null) {
                matchType = MatchType.COLOR;
            } else if (statusDef.contains(restrict)) {
                matchType = MatchType.STATUS;
            } else if (Helper.isValidStream(restrict)) {
                matchType = MatchType.NAME;
            } else if (restrict.equals("$all") || restrict.isEmpty()) {
                matchType = MatchType.ALL;
            } else {
                matchType = MatchType.UNDEFINED;
            }
        } else {
            matchType = MatchType.UNDEFINED;
            category = null;
            match = null;
            this.restriction = null;
            restrictionValue = null;
            colorRestriction = null;
            stop = false;
            first = false;
            badgeTypeRestriction = BadgeType.EMPTY;
        }
        
        // Position (at the end so "first" is already set, for backwards
        // compatibility)
        this.positionValue = builder.position;
        this.position = UsericonPosition.parse(builder.position, first);
    }
    
    private CachedImageManager<Usericon> images;
    private int customScaleMode;
    
    public CachedImage<Usericon> getIcon(float scale, int customUsericonScaleMode, CachedImage.CachedImageUser user) {
        this.customScaleMode = customUsericonScaleMode;
        if (images == null) {
            images = new CachedImageManager<>(this, new CachedImage.CachedImageRequester() {
                
                @Override
                public String getImageUrl(int scale, CachedImage.ImageType type) {
                    if (scale == 2) {
                        return url2 != null ? url2.toString() : null;
                    }
                    return url.toString();
                }

                @Override
                public Dimension getBaseSize() {
                    if (source == SOURCE_CUSTOM && customScaleMode == 2) {
                        // Use field, so the updated value is available (if the setting changes)
                        return toHeight(baseImageSize, DEFAULT_ICON_SIZE.height);
                    }
                    if (baseImageSize != null) {
                        return baseImageSize;
                    }
                    if (targetImageSize != null) {
                        return targetImageSize;
                    }
                    return DEFAULT_ICON_SIZE;
                }
                
                @Override
                public boolean forceBaseSize() {
                    return true;
                }
                
                @Override
                public Image modifyImage(ImageIcon icon) {
                    icon = addColor(icon, color);
                    icon = substituteColor(icon);
                    icon = ChannelTextPane.addSpaceToIcon(icon);
                    return icon.getImage();
                }
            }, "usericon");
        }
        if (targetImageSize != null) {
            scale = 1f;
        }
        Object customKey = null;
        if (source == SOURCE_CUSTOM) {
            if (customUsericonScaleMode == 0) {
                scale = 1f;
            }
            if (customUsericonScaleMode == 2) {
                /**
                 * A custom key is used to make sure a new image is created when
                 * settings change. With this setting value the base size is
                 * modified, which otherwise would not cause an image update in
                 * the "images" cache.
                 */
                customKey = 1;
            }
        }
        return images.getIcon(scale, 0, customKey, CachedImage.ImageType.STATIC, user);
    }
    
    private static Dimension toHeight(Dimension d, int targetHeight) {
        int width = d.width;
        int height = d.height;
        width = width / (height / targetHeight);
        height = targetHeight;
        return new Dimension(width, height);
    }
    
    /**
     * Adds a background color to the given icon, if an icon and color is
     * actually given, otherwise the original icon is returned.
     * 
     * @param icon
     * @param color
     * @return 
     */
    private ImageIcon addColor(ImageIcon icon, Color color) {
        if (color == null) {
            return icon;
        }
        BufferedImage newImage = new BufferedImage(icon.getIconWidth(),
                icon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = newImage.getGraphics();
        g.setColor(color);
        g.drawImage(icon.getImage(), 0, 0, color, null);
        g.dispose();
        return new ImageIcon(newImage);
    }
    
    /**
     * Exchanges black for another color for announcement icons. The color is
     * the version of this badge.
     * 
     * @param icon
     * @return 
     */
    private ImageIcon substituteColor(ImageIcon icon) {
        if (type == Type.OTHER && badgeType.id == null) {
            LOGGER.warning("Badge of type OTHER has no id set: "+this);
            return icon;
        }
        if (type == Type.OTHER && badgeType.id.equals("announcement")) {
            Color search = Color.BLACK;
            Color target = HtmlColors.decode(badgeType.version);
            if (!search.equals(target)) {
                return GuiUtil.substituteColor(icon, search, target);
            }
        }
        return icon;
    }
    
    /**
     * Used for sorting the default icons in the {@code TreeSet}, which means no
     * two icons that should both appear in there at the same time can return 0,
     * because the {@code TreeSet} uses this to determine the order as well as
     * equality.
     * 
     * @param o The object to compare this object against
     * @return 
     */
    @Override
    public int compareTo(Object o) {
        if (o instanceof Usericon) {
            Usericon icon = (Usericon)o;
            if (icon.source > source) {
                return 1;
            } else if (icon.source < source) {
                return -1;
            } else if (icon.type != type) {
                return icon.type.compareTo(type);
            } else if (!Objects.equals(icon.badgeType, badgeType)) {
                return badgeType.compareTo(icon.badgeType);
            } else {
                return icon.channelRestriction.compareTo(channelRestriction);
            }
        }
        return 0;
    }
    
    
    
    @Override
    public String toString() {
        return String.format("%s[%s,%s]/%s/%s/%s(%s)", 
                typeToString(type),
                badgeType.id,
                badgeType.version,
                source,
                channelRestriction,
                restriction,
                hasRegularImage ? "L" : (removeBadge ? "R" : "E"));
    }
    
    public String readableLenientType() {
        Type type = this.type;
        if (type == Type.TWITCH) {
            type = typeFromBadgeId(badgeType.id);
        }
        if (type == null) {
            return badgeType.id;
        }
        if (type == Type.OTHER) {
            return "Other/"+badgeType.id;
        }
        return type.label;
    }
    
    public static String typeToString(Type type) {
        return type.shortLabel;
    }
    
    public static Color getColorFromType(Type type) {
        return type.color;
    }
    
    public String getSymbol() {
        if (type.symbol != null) {
            return type.symbol;
        }
        if (typeFromBadgeId(badgeType.id) != null) {
            return typeFromBadgeId(badgeType.id).symbol;
        }
        return "?";
    }
    
    public String getIdAndVersion() {
        return badgeType.toString();
    }
    
    public static Type typeFromBadgeId(String badgeId) {
        if (badgeId == null) {
            return null;
        }
        for (Type type : Type.values()) {
            if (badgeId.equals(type.badgeId)) {
                return type;
            }
        }
        return null;
    }
    
    public static String makeBadgeInfo(IrcBadges badgesDef) {
        StringBuilder b = new StringBuilder();
        badgesDef.forEach((id, version) -> {
            Type type = typeFromBadgeId(id);
            if (type != null) {
                if (b.length() > 0) {
                    b.append("|");
                }
                b.append(type.shortLabel);
                if (version != null && !version.equals("1")) {
                    b.append("/").append(version);
                }
            }
        });
        if (b.length() > 0) {
            b.insert(0, "[");
            b.append("]");
        }
        return b.toString();
    }
    
    
    public static class Builder {

        private final Usericon.Type type;
        private final int source;

        private String channel;
        private URL url;
        private URL url2;
        private Color color;
        private String restriction;
        private String fileName;
        private BadgeType badgeType;
        private String metaTitle = "";
        private String metaDescription = "";
        private String metaUrl = "";
        private Set<String> usernames;
        private Set<String> userids;
        private String position;
        private Dimension baseImageSize;
        private Dimension targetImageSize;

        public Builder(Usericon.Type type, int source) {
            this.type = type;
            this.source = source;
        }

        /**
         * Restrict badge to this channel.
         * 
         * @param channel
         * @return 
         */
        public Builder setChannel(String channel) {
            this.channel = channel;
            return this;
        }

        /**
         * The URL (can be a local file) used to load the image.
         * 
         * @param url
         * @return 
         */
        public Builder setUrl(URL url) {
            this.url = url;
            return this;
        }
        
        public Builder setUrl2(URL url2) {
            this.url2 = url2;
            return this;
        }

        /**
         * If set, the transparent background of the image will be filled with
         * this color.
         * 
         * @param color
         * @return 
         */
        public Builder setColor(Color color) {
            this.color = color;
            return this;
        }

        /**
         * The restriction can contain a number of different identifiers that
         * have to match the user this badge will be displayed for.
         * 
         * @param restriction
         * @return 
         */
        public Builder setRestriction(String restriction) {
            this.restriction = restriction;
            return this;
        }

        /**
         * Mostly for custom badges, this is used for saving the original
         * setting, as well as for special images like ($ffz). A url has to be
         * set as well to specify the actual image location (which won't be used
         * if the fileName starts with $).
         * 
         * @param fileName
         * @return 
         */
        public Builder setFileName(String fileName) {
            this.fileName = StringUtil.trim(fileName);
            return this;
        }

        public Builder setBadgeType(String id, String version) {
            this.badgeType = new BadgeType(id, version);
            return this;
        }

        public Builder setBadgeType(BadgeType badgeType) {
            this.badgeType = badgeType;
            return this;
        }
        
        public Builder setMetaTitle(String title) {
            if (title == null) {
                title = "";
            }
            this.metaTitle = title.trim();
            return this;
        }
        
        public Builder setMetaDescription(String description) {
            if (description == null) {
                description = "";
            }
            this.metaDescription = description.trim();
            return this;
        }
        
        public Builder setMetaUrl(String url) {
            if (url == null) {
                url = "";
            }
            this.metaUrl = url.trim();
            return this;
        }
        
        public Builder setUsernames(Collection<String> usernames) {
            if (usernames != null) {
                this.usernames = Collections.unmodifiableSet(new HashSet<>(usernames));
            }
            return this;
        }
        
        public Builder setUserids(Collection<String> userids) {
            if (userids != null) {
                this.userids = Collections.unmodifiableSet(new HashSet<>(userids));
            }
            return this;
        }
        
        public Builder setPosition(String position) {
            this.position = position;
            return this;
        }
        
        public Builder setBaseImageSize(int width, int height) {
            this.baseImageSize = new Dimension(width, height);
            return this;
        }
        
        public Builder setTargetImageSize(int width, int height) {
            this.targetImageSize = new Dimension(width, height);
            return this;
        }

        public Usericon build() {
            return new Usericon(this);
        }

    }

}
