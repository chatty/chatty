
package chatty.util;

import chatty.Chatty;
import chatty.gui.MainGui;
import chatty.util.IconManager.CustomIcon.Type;
import java.awt.Image;
import java.awt.Toolkit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/**
 * Allows setting custom window/notification icons.
 * 
 * @author tduva
 */
public class IconManager {
    
    private static final Logger LOGGER = Logger.getLogger(IconManager.class.getName());
    
    private static List<CustomIcon> customIcons;
    
    //==========================
    // Custom Icons
    //==========================
    
    /**
     * Set custom window icons, straight from the setting.
     * 
     * @param icons 
     */
    public static void setCustomIcons(List<String> icons) {
        List<CustomIcon> result = new ArrayList<>();
        for (String path : icons) {
            CustomIcon.Type type = CustomIcon.Type.ALL;
            for (CustomIcon.Type t : CustomIcon.Type.values()) {
                String prefix = "["+t.id+"]";
                if (path.startsWith(prefix)) {
                    path = path.substring(prefix.length());
                    type = t;
                }
            }
            Path absolutePath = Chatty.toAbsolutePathWdir(Paths.get(path));
            if (absolutePath.toFile().isFile()) {
                try {
                    ImageIcon image = new ImageIcon(absolutePath.toString());
                    result.add(new CustomIcon(type,image));
                } catch (Exception ex) {
                    LOGGER.warning(String.format("Error creating custom icon: %s [%s]", path, ex));
                }
            } else {
                LOGGER.warning(String.format("Error creating custom icon: %s [not a file]", path));
            }
        }
        if (!result.isEmpty()) {
            // Set result and make debug output
            customIcons = result;
            StringBuilder b = new StringBuilder();
            for (CustomIcon ci : result) {
                if (b.length() > 0) {
                    b.append("/");
                }
                b.append("[").append(ci.type).append("]");
                b.append(ci.icon.getIconWidth()).append("x").append(ci.icon.getIconHeight());
            }
            LOGGER.info(String.format("Created %s custom icons (%s)",
                    result.size(),
                    b.toString()));
        }
    }
    
    public static class CustomIcon {

        public enum Type {

            ALL("all"), MAIN("main"), TRAY("tray"), NOTIFICATION("notif"),
            LIVE("live"), HELP("help"), DEBUG("debug"), POPOUT("popout");
            
            public final String id;

            Type(String id) {
                this.id = id;
            }

        }

        public final Type type;
        public final ImageIcon icon;

        public CustomIcon(Type type, ImageIcon icon) {
            this.icon = icon;
            this.type = type;
        }

    }
    
    //==========================
    // Single Icon
    //==========================
    public static ImageIcon getNotificationIcon() {
        return new ImageIcon(getSingleIcon(Type.NOTIFICATION, 16));
    }
    
    public static Image getTrayIcon(int size) {
        return getSingleIcon(Type.TRAY, size);
    }
    
    /**
     * Get a single custom icon (or default main icon as backup) of the given
     * type and size. Assumes width is equal to height. Most icons should
     * probably be like that, but if it this causes issues it should be
     * improved.
     * 
     * @param type The type to prefer, if none found icons of the type ALL will
     * be tried as well
     * @param size 
     * @return A fitting custom icon, or a default icon
     */
    public static Image getSingleIcon(Type type, int size) {
        if (customIcons != null) {
            // Prefer exact type
            ImageIcon bestFit = getBestFit(customIcons, type, size, size);
            if (bestFit == null) {
                bestFit = getBestFit(customIcons, Type.ALL, size, size);
            }
            if (bestFit != null) {
                if (bestFit.getIconWidth() != size || bestFit.getIconHeight() != size) {
                    return bestFit.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                }
                return bestFit.getImage();
            }
        }
        // If no icon was returned yet (even if customIcons is there, type may
        // not match), then use defaults
        if (size <= 16) {
            return createScaledDefaultImage("app_main_16.png", size);
        }
        else {
            return createScaledDefaultImage("app_main_64.png", size);
        }
    }
    
    /**
     * Find the image that best fits the given size, preferring larger images so
     * they can be scaled down better. Not perfect, but should do for now.
     * 
     * @param imgs The custom icons to get the image from
     * @param type Restrict custom icon to this type
     * @param width The target width
     * @param height The target height
     * @return The image with the best size and matching the type, or null if
     * none was found
     */
    public static ImageIcon getBestFit(Collection<CustomIcon> imgs, Type type, int width, int height) {
        ImageIcon bestFit = null;
        for (CustomIcon ci : imgs) {
            if (ci.type != type) {
                continue;
            }
            ImageIcon icon = ci.icon;
            if ((bestFit == null)
                    || (icon.getIconWidth() == width && icon.getIconHeight() == height)) {
                bestFit = icon;
            }
            else {
                int wDist = icon.getIconWidth() - width;
                int hDist = icon.getIconHeight() - height;
                int wDistB = bestFit.getIconWidth() - width;
                int hDistB = bestFit.getIconHeight() - height;
//                System.out.println(String.format("%dx%d [%dx%d] wDist: %d hDist: %d",
//                        icon.getIconWidth(),
//                        icon.getIconHeight(),
//                        bestFit.getIconWidth(),
//                        bestFit.getIconHeight(),
//                        wDist,
//                        hDist));
                if ((wDist > 0 && hDist > 0) && (wDistB < 0 || hDistB < 0)) {
                    bestFit = icon;
                }
                else if (wDistB < 0 || hDistB < 0) {
                    if (Math.abs(wDist) + Math.abs(hDist) < Math.abs(wDistB) + Math.abs(hDistB)) {
                        bestFit = icon;
                    }
                }
            }
        }
        return bestFit;
    }
    
    /**
     * Load and resize image. Assumes width == height.
     *
     * @param name
     * @param size
     * @return
     */
    private static Image createScaledDefaultImage(String name, int size) {
        ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(MainGui.class.getResource(name)));
        if (icon.getIconWidth() != size) {
            return icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        }
        return icon.getImage();
    }
    
    //==========================
    // Icon Lists
    //==========================
    public static List<Image> getMainIcons() {
        List<Image> result = getCustomIcons(Type.MAIN);
        if (!result.isEmpty()) {
            return result;
        }
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createDefaultImage("app_main_16.png"));
        windowIcons.add(createDefaultImage("app_main_64.png"));
        windowIcons.add(createDefaultImage("app_main_128.png"));
        return windowIcons;
    }
    
    public static List<Image> getLiveIcons() {
        List<Image> result = getCustomIcons(Type.LIVE);
        if (!result.isEmpty()) {
            return result;
        }
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createDefaultImage("app_live_16.png"));
        windowIcons.add(createDefaultImage("app_live_64.png"));
        windowIcons.add(createDefaultImage("app_live_128.png"));
        return windowIcons;
    }
    
    public static List<Image> getHelpIcons() {
        List<Image> result = getCustomIcons(Type.HELP);
        if (!result.isEmpty()) {
            return result;
        }
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createDefaultImage("app_help_16.png"));
        windowIcons.add(createDefaultImage("app_help_64.png"));
        windowIcons.add(createDefaultImage("app_help_128.png"));
        return windowIcons;
    }
    
    public static List<Image> getDebugIcons() {
        List<Image> result = getCustomIcons(Type.DEBUG);
        if (!result.isEmpty()) {
            return result;
        }
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createDefaultImage("app_debug_16.png"));
        windowIcons.add(createDefaultImage("app_debug_64.png"));
        windowIcons.add(createDefaultImage("app_debug_128.png"));
        return windowIcons;
    }
    
    public static List<Image> getPopoutIcons() {
        List<Image> result = getCustomIcons(Type.POPOUT);
        if (!result.isEmpty()) {
            return result;
        }
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createDefaultImage("app_popout_16.png"));
        windowIcons.add(createDefaultImage("app_popout_64.png"));
        windowIcons.add(createDefaultImage("app_popout_128.png"));
        return windowIcons;
    }
    
    /**
     * Get the images defined in the icons setting, preferring the exact type,
     * but if no images were found falling back to the ALL type.
     *
     * @param type
     * @return A list with images, or an empty list if no matches found
     */
    private static List<Image> getCustomIcons(Type type) {
        // Prefer exact type
        List<Image> result = getCustomIcons2(type);
        if (result.isEmpty()) {
            result = getCustomIcons2(Type.ALL);
        }
        return result;
    }
    
    private static List<Image> getCustomIcons2(Type type) {
        List<Image> result = new ArrayList<>();
        if (customIcons == null) {
            return result;
        }
        for (CustomIcon ci : customIcons) {
            if (ci.type == type) {
                result.add(ci.icon.getImage());
            }
        }
        return result;
    }
    
    private static Image createDefaultImage(String name) {
        return Toolkit.getDefaultToolkit().createImage(MainGui.class.getResource(name));
    }
    
}
