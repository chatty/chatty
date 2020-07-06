
package chatty.util;

import chatty.gui.MainGui;
import java.awt.Image;
import java.awt.Toolkit;
import java.nio.file.Paths;
import java.util.ArrayList;
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
            if (Paths.get(path).toFile().isFile()) {
                try {
                    ImageIcon image = new ImageIcon(path);
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
