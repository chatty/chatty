
package chatty.util.api.usericons;

import chatty.Chatty;
import chatty.Chatty.PathType;
import chatty.Helper;
import chatty.util.ImageCache;
import chatty.util.ImageCache.ImageResult;
import chatty.util.colors.HtmlColors;
import static chatty.util.api.usericons.Usericon.SOURCE_CUSTOM;
import static chatty.util.api.usericons.Usericon.SOURCE_FALLBACK;
import static chatty.util.api.usericons.Usericon.SOURCE_OTHER;
import static chatty.util.api.usericons.Usericon.SOURCE_TWITCH;
import static chatty.util.api.usericons.Usericon.SOURCE_TWITCH2;
import static chatty.util.api.usericons.Usericon.getColorFromType;
import java.awt.Color;
import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class UsericonFactory {
    
    private static final Logger LOGGER = Logger.getLogger(UsericonFactory.class.getName());
    
    /**
     * Creates a new Icon from the Twitch API, which the appropriate default
     * values for the stuff that isn't specified in the arguments.
     * 
     * @param type
     * @param channel
     * @param urlString
     * @return 
     */
    public static Usericon createTwitchIcon(Usericon.Type type, String channel, String urlString, String urlString2, String title) {
        //return createTwitchLikeIcon(type, channel, urlString, SOURCE_TWITCH);
        return createIconFromUrl(type, channel, urlString, urlString2, SOURCE_TWITCH, null, title);
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
    public static Usericon createTwitchLikeIcon(Usericon.Type type, String channel,
            String urlString, String urlString2, int source, String title) {
        return createIconFromUrl(type, channel, urlString, urlString2, source,
                getColorFromType(type), title);
    }
    
    public static Usericon createIconFromUrl(Usericon.Type type, String channel,
            String urlString, String url2String, int source, Color color,
            String title) {
        try {
            URL url = new URL(Helper.checkHttpUrl(urlString));
            URL url2 = Helper.createUrlNoError(Helper.checkHttpUrl(url2String));
            Usericon.Builder b = new Usericon.Builder(type, source);
            b.setChannel(channel);
            b.setUrl(url);
            b.setUrl2(url2);
            b.setColor(color);
            b.setMetaTitle(title);
            return b.build();
        } catch (MalformedURLException ex) {
            LOGGER.warning("Invalid icon url: " + urlString);
        }
        return null;
    }
    
    public static Usericon createTwitchBadge(String id, String version,
            String urlString, String url2String, String channel, String title,
            String description, String clickUrl) {
        try {
            URL url = new URL(Helper.checkHttpUrl(urlString));
            URL url2 = Helper.createUrlNoError(Helper.checkHttpUrl(url2String));
            Usericon.Builder b = new Usericon.Builder(Usericon.Type.TWITCH, SOURCE_TWITCH2);
            b.setChannel(channel);
            b.setBadgeType(id, version);
            b.setUrl(url);
            b.setUrl2(url2);
            b.setMetaTitle(title);
            b.setMetaDescription(description);
            b.setMetaUrl(clickUrl);
            return b.build();
        } catch (MalformedURLException ex) {
            LOGGER.warning("Invalid icon url: " + urlString);
        }
        return null;
    }
    
    public static Usericon createThirdParty(String id, String version,
            String urlString, String url2String, String title, String clickUrl, String color,
            Set<String> usernames, Set<String> userids, String position) {
        try {
            URL url = new URL(Helper.checkHttpUrl(urlString));
            URL url2 = Helper.createUrlNoError(Helper.checkHttpUrl(url2String));
            Usericon.Builder b = new Usericon.Builder(Usericon.Type.OTHER, SOURCE_OTHER);
            b.setBadgeType(id, version);
            b.setUrl(url);
            b.setUrl2(url2);
            b.setMetaTitle(title);
            b.setMetaUrl(clickUrl);
            b.setUsernames(usernames);
            b.setUserids(userids);
            b.setPosition(position);
            if (color != null) {
                b.setColor(HtmlColors.decode(color));
            }
            return b.build();
        } catch (MalformedURLException ex) {
            LOGGER.warning("Invalid icon url: " + urlString);
        }
        return null;
    }
    
    public static Usericon createChannelLogo(String channel, String url, int size) {
        try {
            Usericon.Builder b = new Usericon.Builder(Usericon.Type.CHANNEL_LOGO, Usericon.SOURCE_OTHER);
            b.setChannel(channel);
            b.setUrl(new URL(url));
            b.setTargetImageSize(size, size);
            return b.build();
        }
        catch (MalformedURLException ex) {
            LOGGER.warning("Invalid icon url: " + url);
        }
        return null;
    }
    
    /**
     * Creates an icon based on a filename, which is resolved with the image
     * directory (if necessary). It also takes a restriction parameter and
     * stuff and sets the other values to appropriate values for custom icons.
     * 
     * @param type
     * @param idVersion
     * @param restriction
     * @param fileName
     * @param channel
     * @return 
     */
    public static Usericon createCustomIcon(Usericon.Type type, String idVersion,
            String restriction, String fileName, String channel, String position) {
        try {
            URL url = null;
            if (fileName != null) {
                if (fileName.startsWith("http")) {
                    url = new URL(fileName);
                } else if (!fileName.trim().isEmpty()) {
                    Path path = Chatty.getPathCreate(PathType.IMAGE).resolve(Paths.get(fileName));
                    url = path.toUri().toURL();
                }
            }

            Usericon.Builder b = new Usericon.Builder(type, SOURCE_CUSTOM);
            b.setChannel(channel);
            b.setUrl(url);
            b.setRestriction(restriction);
            b.setFileName(fileName);
            b.setBadgeType(BadgeType.parse(idVersion));
            b.setPosition(position);
            if (url != null && !fileName.startsWith("$")) {
                Dimension size = getSize(url);
                if (size == null) {
                    // If image could not be loaded, invalid icon
                    LOGGER.warning("Invalid icon file (size): " + fileName);
                    return null;
                }
                /**
                 * Set the size since custom icons can have a non-standard size
                 * and this ensures that the default placeholder image has the
                 * correctly size already and chat doesn't move around when the
                 * actual image is loaded.
                 */
                b.setBaseImageSize(size.width, size.height);
            }
            return b.build();
        } catch (MalformedURLException | InvalidPathException ex) {
            LOGGER.warning("Invalid icon file: " + fileName);
        }
        return null;
    }

    public static Usericon createFallbackIcon(Usericon.Type type, URL url) {
        Usericon.Builder b = new Usericon.Builder(type, SOURCE_FALLBACK);
        b.setUrl(url);
        b.setColor(getColorFromType(type));
        Dimension size = getSize(url);
        if (size != null) {
            /**
             * Set the size so that non-standard icons such as for the first
             * message in the channel are displayed correctly.
             */
            b.setBaseImageSize(size.width, size.height);
        }
        return b.build();
    }
    
    public static Usericon createSpecialOtherIcon(String id, String version, URL url, URL url2, String metatitle) {
        Usericon.Builder b = new Usericon.Builder(Usericon.Type.OTHER, SOURCE_OTHER);
        b.setUrl(url);
        b.setUrl2(url2);
        b.setBadgeType(id, version);
        b.setMetaTitle(metatitle);
        Dimension size = getSize(url);
        if (size != null) {
            /**
             * Set the size so that non-standard icons such as for the first
             * message in the channel are displayed correctly.
             */
            b.setBaseImageSize(size.width, size.height);
        }
        return b.build();
    }
    
    /**
     * Get the size of the image of the given URL.
     * 
     * @param url
     * @return The size, or null if the URL or image is invalid
     */
    private static Dimension getSize(URL url) {
        ImageResult result = ImageCache.getImage(new ImageCache.ImageRequest(url), "usericon", Usericon.CACHE_TIME);
        if (result != null && result.isValidImage()) {
            return result.actualBaseSize;
        }
        return null;
    }
    
}
