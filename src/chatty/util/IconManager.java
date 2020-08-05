
package chatty.util;

import chatty.Chatty;
import chatty.gui.MainGui;
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
            
    private static List<ImageIcon> customIcons;
    
    public static void setCustomIcons(List<String> icons) {
        List<ImageIcon> result = new ArrayList<>();
        for (String path : icons) {
            Path absolutePath = Chatty.toAbsolutePathWdir(Paths.get(path));
            if (absolutePath.toFile().isFile()) {
                try {
                    ImageIcon image = new ImageIcon(absolutePath.toString());
                    result.add(image);
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
            for (ImageIcon icon : result) {
                if (b.length() > 0) {
                    b.append("/");
                }
                b.append(icon.getIconWidth()).append("x").append(icon.getIconHeight());
            }
            LOGGER.info(String.format("Created %s custom icons (%s)",
                    result.size(),
                    b.toString()));
        }
    }
    
    private static Image createDefaultImage(String name) {
        return Toolkit.getDefaultToolkit().createImage(MainGui.class.getResource(name));
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
    
    /**
     * Get the images defined in the "icons" setting. Must only be called when
     * custom icons have actually been successfully set (customIcons not null).
     * 
     * @return 
     */
    private static List<Image> getCustomIcons() {
        List<Image> result = new ArrayList<>();
        for (ImageIcon icon : customIcons) {
            result.add(icon.getImage());
        }
        return result;
    }
    
    public static ImageIcon getNotificationIcon() {
        if (customIcons != null) {
            for (ImageIcon icon : customIcons) {
                if (icon.getIconWidth() == 16 && icon.getIconHeight() == 16) {
                    return icon;
                }
            }
        }
        return new ImageIcon(MainGui.class.getResource("app_main_16.png"));
    }
    
    /**
     * Get a main icon or custom image for the given size. Assumes width is
     * equal to height. Most icons should probably be like that, but if it this
     * causes issues it should be improved.
     * 
     * @param size
     * @return 
     */
    public static Image getMainIcon(int size) {
        if (customIcons != null) {
            ImageIcon bestFit = getBestFit(customIcons, size, size);
            if (bestFit.getIconWidth() != size || bestFit.getIconHeight() != size) {
                return bestFit.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            }
            return bestFit.getImage();
        }
        else {
            if (size <= 16) {
                return createScaledDefaultImage("app_main_16.png", size);
            }
            else {
                return createScaledDefaultImage("app_main_64.png", size);
            }
        }
    }
    
    /**
     * Find the image that best fits the given size, preferring larger images so
     * they can be scaled down better. Not perfect, but should do for now.
     * 
     * @param imgs
     * @param width
     * @param height
     * @return 
     */
    public static ImageIcon getBestFit(Collection<ImageIcon> imgs, int width, int height) {
        ImageIcon bestFit = null;
        for (ImageIcon icon : imgs) {
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
    
    public static List<Image> getMainIcons() {
        if (customIcons != null) {
            return getCustomIcons();
        }
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createDefaultImage("app_main_16.png"));
        windowIcons.add(createDefaultImage("app_main_64.png"));
        windowIcons.add(createDefaultImage("app_main_128.png"));
        return windowIcons;
    }
    
    public static List<Image> getLiveIcons() {
        if (customIcons != null) {
            return getCustomIcons();
        }
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createDefaultImage("app_live_16.png"));
        windowIcons.add(createDefaultImage("app_live_64.png"));
        windowIcons.add(createDefaultImage("app_live_128.png"));
        return windowIcons;
    }
    
    public static List<Image> getHelpIcons() {
        if (customIcons != null) {
            return getCustomIcons();
        }
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createDefaultImage("app_help_16.png"));
        windowIcons.add(createDefaultImage("app_help_64.png"));
        windowIcons.add(createDefaultImage("app_help_128.png"));
        return windowIcons;
    }
    
    public static List<Image> getDebugIcons() {
        if (customIcons != null) {
            return getCustomIcons();
        }
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createDefaultImage("app_debug_16.png"));
        windowIcons.add(createDefaultImage("app_debug_64.png"));
        windowIcons.add(createDefaultImage("app_debug_128.png"));
        return windowIcons;
    }
    
}
