
package chatty;

import chatty.gui.HtmlColors;
import chatty.util.ImageCache;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
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
    private static final int CACHE_TIME = 60*60*24*3;
    
    private static final Set<String> statusDef = new HashSet<>(Arrays.asList(
            "$mod", "$sub", "$admin", "$staff", "$turbo", "$broadcaster", "$bot",
            "$globalmod", "$anymod"));
    
    /**
     * The type determines whether it should replace any of the default icons
     * (which also assumes they are mainly requested if the user is actually
     * mod, turbo, etc.) or if it should be shown in addition to the default
     * icons (addon).
     */
    public enum Type {
        
        MOD(0, "Moderator", "MOD", HtmlColors.decode("#34ae0a")),
        TURBO(1, "Turbo", "TRB", HtmlColors.decode("#6441a5")),
        BROADCASTER(2, "Broadcaster", "BRC", HtmlColors.decode("#e71818")),
        STAFF(3, "Staff", "STA", HtmlColors.decode("#200f33")),
        ADMIN(4, "Admin", "ADM", HtmlColors.decode("#faaf19")),
        SUB(5, "Subscriber", "SUB", null),
        ADDON(6, "Addon", "ADD", null),
        GLOBAL_MOD(7, "Global Moderator", "GLM", HtmlColors.decode("#0c6f20")),
        BOT(8, "Bot", "BOT", null),
        UNDEFINED(-1, "Undefined", "UDF", null);
        
        public Color color;
        public String label;
        public String shortLabel;
        public int id;
        
        Type(int id, String label, String shortLabel, Color color) {
            this.color = color;
            this.label = label;
            this.shortLabel = shortLabel;
            this.id = id;
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
        CATEGORY, UNDEFINED, ALL, STATUS, NAME, COLOR
    }
    
    /**
     * The type of icon based on the source.
     */
    public static final int SOURCE_FALLBACK = 0;
    public static final int SOURCE_TWITCH = 5;
    public static final int SOURCE_FFZ = 10;
    public static final int SOURCE_CUSTOM = 20;
    
    /**
     * Fields directly saved from the constructor arguments (or only slightly
     * modified).
     */
    /**
     * Which kind of icon (replacing mod, sub, turbo, .. or addon).
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
     */
    public final String channelRestriction;
    
    /**
     * The URL the image is loaded from
     */
    public final URL url;
    
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
     * Data that is derived from other fields and not directly saved from
     * constructor arguments.
     */
    /**
     * The image loaded from the given {@literal url}
     */
    public final ImageIcon image;
    
    /**
     * The match type is derived from {@literal id}, to make it easier to check
     * what to match later.
     */
    public final MatchType matchType;
    
    /**
     * The addressbook category to match (if given) in {@literal id}.
     */
    public final String category;
    
    /**
     * The actual channel from the channel restriction. If no or an invalid
     * channel is specified in the channel restriction, then this is empty.
     */
    public final String channel;
    
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
    
    public final boolean stop;
    public final boolean first;
    
    
    /**
     * Creates a new Icon from the Twitch API, which the appropriate default
     * values for the stuff that isn't specified in the arguments.
     * 
     * @param type
     * @param channel
     * @param urlString
     * @return 
     */
    public static Usericon createTwitchIcon(Type type, String channel, String urlString) {
        //return createTwitchLikeIcon(type, channel, urlString, SOURCE_TWITCH);
        return createIconFromUrl(type, channel, urlString, SOURCE_TWITCH, null);
    }
    
    /**
     * Creates a new icon with the given values, with appropriate default values
     * for the stuff that isn't specified in the arguments. It determines the
     * background color based on the default Twitch settings, so it should only
     * be used for icons that should match that behaviour.
     * 
     * @param type
     * @param channel
     * @param urlString
     * @param source
     * @return 
     */
    public static Usericon createTwitchLikeIcon(Type type, String channel,
            String urlString, int source) {
        return createIconFromUrl(type, channel, urlString, source,
                getColorFromType(type));
    }
    
    public static Usericon createIconFromUrl(Type type, String channel,
            String urlString, int source, Color color) {
        try {
            URL url = new URL(Helper.checkHttpUrl(urlString));
            Usericon icon = new Usericon(type, channel, url, color, source);
            return icon;
        } catch (MalformedURLException ex) {
            LOGGER.warning("Invalid icon url: " + urlString);
        }
        return null;
    }
    
    /**
     * Creates an icon based on a filename, which is resolved with the image
     * directory (if necessary). It also takes a restriction parameter and
     * stuff and sets the other values to appropriate values for custom icons.
     * 
     * @param type
     * @param restriction
     * @param fileName
     * @param channel
     * @return 
     */
    public static Usericon createCustomIcon(Type type, String restriction, String fileName, String channel) {
        if (fileName == null) {
            return null;
        }
        try {
            URL url;
            if (fileName.startsWith("http")) {
                url = new URL(fileName);
            } else {
                Path path = Paths.get(Chatty.getImageDirectory()).resolve(Paths.get(fileName));
                url = path.toUri().toURL();
            }
            Usericon icon = new Usericon(type, channel, url, null, SOURCE_CUSTOM, restriction, fileName);
            return icon;
        } catch (MalformedURLException | InvalidPathException ex) {
            LOGGER.warning("Invalid icon file: " + fileName);
        }
        return null;
    }

    public static Usericon createFallbackIcon(Type type, URL url) {
        Usericon icon = new Usericon(type, null, url, getColorFromType(type), SOURCE_FALLBACK);
        return icon;
    }
    
    /**
     * Convenience constructor which simply omits the two arguments mainly used
     * for custom icons.
     * 
     * @param type
     * @param channel
     * @param url
     * @param color
     * @param source 
     */
    public Usericon(Type type, String channel, URL url, Color color, int source) {
        this(type, channel, url, color, source, null, null);
    }
    
    /**
     * Creates a new {@literal Userimage}, which will try to load the image from
     * the given URL. If the loading fails, the {@literal image} field will be
     * {@literal null}.
     * 
     * @param type The type of userimage (Addon, Mod, Sub, etc.)
     * @param channel The channelRestriction the image applies to
     * @param url The url to load the image from
     * @param color The color to use as background
     * @param source The source of the image (like Twitch, Custom, FFZ)
     * @param restriction Additional restrictions (like $mod, $sub, $cat)
     * @param fileName The name of the file to load the icon from (this is used
     * for further reference, probably only for custom icons)
     */
    public Usericon(Type type, String channel, URL url, Color color, int source,
            String restriction, String fileName) {
        this.type = type;
        this.fileName = fileName;
        this.source = source;

        // Channel Restriction
        if (channel != null) {
            channel = channel.trim();
            channelRestriction = channel;
            if (channel.startsWith("!")) {
                channelInverse = true;
                channel = channel.substring(1);
            } else {
                channelInverse = false;
            }
        } else {
            channelRestriction = "";
            channelInverse = false;
        }
        channel = Helper.toValidChannel(channel);
        if (channel == null) {
            channel = "";
        }
        this.channel = channel;
        
        
        this.url = url;
        
        if (fileName != null && fileName.startsWith("$")) {
            image = null;
        } else {
            image = addColor(getIcon(url), color);
        }
        
        // Restriction
        if (restriction != null) {
            restriction = restriction.trim();
            this.restriction = restriction;
            if (restriction.contains("$stop")) {
                restriction = restriction.replace("$stop", "").trim();
                stop = true;
            } else {
                stop = false;
            }
            if (restriction.contains("$first")) {
                restriction = restriction.replace("$first", "").trim();
                first = true;
            } else {
                first = false;
            }
            restrictionValue = restriction;
            
            // Check if a category was specified as id
            if (restriction.startsWith("$cat:") && restriction.length() > 5) {
                category = restriction.substring(5);
            } else {
                category = null;
            }
            
            if (restriction.startsWith("#") && restriction.length() == 7) {
                colorRestriction = HtmlColors.decode(restriction, null);
            } else if (restriction.startsWith("$color:") && restriction.length() > 7) {
                colorRestriction = HtmlColors.decode(restriction.substring(7), null);
            } else {
                colorRestriction = null;
            }
            
            // Save the type
            if (restriction.startsWith("$cat:") && restriction.length() > 5) {
                matchType = MatchType.CATEGORY;
            } else if (colorRestriction != null) {
                matchType = MatchType.COLOR;
            } else if (statusDef.contains(restriction)) {
                matchType = MatchType.STATUS;
            } else if (Helper.validateStream(restriction)) {
                matchType = MatchType.NAME;
            } else if (restriction.equals("$all") || restriction.isEmpty()) {
                matchType = MatchType.ALL;
            } else {
                matchType = MatchType.UNDEFINED;
            }
        } else {
            matchType = MatchType.UNDEFINED;
            category = null;
            this.restriction = null;
            restrictionValue = null;
            colorRestriction = null;
            stop = false;
            first = false;
        }
    }
    
    /**
     * Loads the icon from the given url.
     * 
     * @param url The URL to load the icon from
     * @return The loaded icon or {@literal null} if no URL was specified or the
     * icon couldn't be loaded
     */
    private ImageIcon getIcon(URL url) {
        if (url == null) {
            return null;
        }
        //ImageIcon icon = new ImageIcon(url);
        //ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(url));
        ImageIcon icon = ImageCache.getImage(url, "usericon", CACHE_TIME);
        if (icon != null) {
            return icon;
        } else {
            LOGGER.warning("Could not load icon: " + url);
        }
        return null;
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
        if (icon == null || color == null) {
            return icon;
        }
        BufferedImage image = new BufferedImage(icon.getIconWidth(),
                icon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setColor(color);
        g.drawImage(icon.getImage(), 0, 0, color, null);
        g.dispose();
        return new ImageIcon(image);
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
            if (this.image == null && icon.image == null) {
                return 0;
            } else if (this.image == null) {
                return 1;
            } else if (icon.image == null) {
                return -1;
            } else if (icon.source > source) {
                return 1;
            } else if (icon.source < source) {
                return -1;
            } else if (icon.type != type) {
                return icon.type.compareTo(type);
            } else {
                return icon.channelRestriction.compareTo(channelRestriction);
            }
        }
        return 0;
    }
    
    @Override
    public String toString() {
        return typeToString(type)+"/"+source+"/"+channelRestriction+"/"+restriction+"("+(image != null ? "L" : "E")+")";
    }
    
    public static String typeToString(Type type) {
        return type.shortLabel;
//        switch (type) {
//            case MOD: return "MOD";
//            case ADDON: return "ADD";
//            case ADMIN: return "ADM";
//            case BROADCASTER: return "BRC";
//            case STAFF: return "STA";
//            case SUB: return "SUB";
//            case TURBO: return "TRB";
//        }
//        return "UDF";
    }
    
    public static Color getColorFromType(Type type) {
        return type.color;
//        switch (type) {
//            case TYPE_MOD:
//                return TWITCH_MOD_COLOR;
//            case TYPE_TURBO:
//                return TWITCH_TURBO_COLOR;
//            case TYPE_ADMIN:
//                return TWITCH_ADMIN_COLOR;
//            case TYPE_BROADCASTER:
//                return TWITCH_BROADCASTER_COLOR;
//            case TYPE_STAFF:
//                return TWITCH_STAFF_COLOR;
//        }
//        return null;
    }
}
