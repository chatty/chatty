
package chatty.util.api.usericons;

import chatty.Chatty;
import chatty.Helper;
import chatty.util.colors.HtmlColors;
import static chatty.util.api.usericons.Usericon.SOURCE_CUSTOM;
import static chatty.util.api.usericons.Usericon.SOURCE_FALLBACK;
import static chatty.util.api.usericons.Usericon.SOURCE_OTHER;
import static chatty.util.api.usericons.Usericon.SOURCE_TWITCH;
import static chatty.util.api.usericons.Usericon.SOURCE_TWITCH2;
import static chatty.util.api.usericons.Usericon.getColorFromType;
import java.awt.Color;
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
    public static Usericon createTwitchIcon(Usericon.Type type, String channel, String urlString, String title) {
        //return createTwitchLikeIcon(type, channel, urlString, SOURCE_TWITCH);
        return createIconFromUrl(type, channel, urlString, SOURCE_TWITCH, null, title);
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
            String urlString, int source, String title) {
        return createIconFromUrl(type, channel, urlString, source,
                getColorFromType(type), title);
    }
    
    public static Usericon createIconFromUrl(Usericon.Type type, String channel,
            String urlString, int source, Color color, String title) {
        try {
            URL url = new URL(Helper.checkHttpUrl(urlString));
            Usericon.Builder b = new Usericon.Builder(type, source);
            b.setChannel(channel);
            b.setUrl(url);
            b.setColor(color);
            b.setMetaTitle(title);
            return b.build();
        } catch (MalformedURLException ex) {
            LOGGER.warning("Invalid icon url: " + urlString);
        }
        return null;
    }
    
    public static Usericon createTwitchBadge(String id, String version,
            String urlString, String channel, String title, String description,
            String clickUrl) {
        try {
            URL url = new URL(Helper.checkHttpUrl(urlString));
            Usericon.Builder b = new Usericon.Builder(Usericon.Type.TWITCH, SOURCE_TWITCH2);
            b.setChannel(channel);
            b.setBadgeType(id, version);
            b.setUrl(url);
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
            String urlString, String title, String clickUrl, String color,
            Set<String> usernames, Set<String> userids, String position) {
        try {
            URL url = new URL(Helper.checkHttpUrl(urlString));
            Usericon.Builder b = new Usericon.Builder(Usericon.Type.OTHER, SOURCE_OTHER);
            b.setBadgeType(id, version);
            b.setUrl(url);
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
                    Path path = Paths.get(Chatty.getImageDirectory()).resolve(Paths.get(fileName));
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
        return b.build();
    }
    
    
}
