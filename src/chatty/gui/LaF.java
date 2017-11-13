
package chatty.gui;

import chatty.util.settings.Settings;
import com.jtattoo.plaf.aero.AeroLookAndFeel;
import com.jtattoo.plaf.fast.FastLookAndFeel;
import com.jtattoo.plaf.graphite.GraphiteLookAndFeel;
import com.jtattoo.plaf.hifi.HiFiLookAndFeel;
import com.jtattoo.plaf.luna.LunaLookAndFeel;
import com.jtattoo.plaf.mint.MintLookAndFeel;
import com.jtattoo.plaf.noire.NoireLookAndFeel;
import java.awt.Window;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

/**
 *
 * @author tduva
 */
public class LaF {
    
    private static final Logger LOGGER = Logger.getLogger(LaF.class.getName());
    
    private static Settings settings;
    private static String linkColor = "#0000FF";
    private static boolean isDarkTheme;
    
    public static void setSettings(Settings settings) {
        LaF.settings = settings;
    }
    
    public static String getLinkColor() {
        return linkColor;
    }
    
    public static boolean isDarkTheme() {
        return isDarkTheme;
    }
    
    public static void setLookAndFeel(String lafCode, String theme) {
        try {
            String laf = null;
            if (lafCode.startsWith(":")) {
                laf = lafCode.substring(1);
            } else {
                switch (lafCode) {
                    case "system":
                        laf = UIManager.getSystemLookAndFeelClassName();
                        break;
                    case "hifi":
                        laf = "com.jtattoo.plaf.hifi.HiFiLookAndFeel";
                        HiFiLookAndFeel.setCurrentTheme(
                                addCustom(prepareTheme(HiFiLookAndFeel.getThemeProperties(theme)))
                        );
                        break;
                    case "hifi2":
                        laf = "com.jtattoo.plaf.hifi.HiFiLookAndFeel";
                        Properties p = prepareTheme(HiFiLookAndFeel.getThemeProperties(theme));
                        //p.put("backgroundColor", "50 54 52");
                        p.put("backgroundColor", "50 52 51");
                        p.put("foregroundColor", "180 190 185");
                        p.put("menuForegroundColor", "200 210 204");
                        p.put("menuBackgroundColor", "40 42 40");
                        p.put("inputForegroundColor", "190 200 195");
                        p.put("inputBackgroundColor", "60 64 62");
                        p.put("buttonForegroundColor", "210 220 215");
                        p.put("buttonColorLight", "96 96 96");
                        p.put("buttonColorDark", "44 44 44");
                        p.put("backgroundColorDark", "255 0 0");
                        HiFiLookAndFeel.setCurrentTheme(addCustom(p));
                        break;
                    case "mint":
                        laf = "com.jtattoo.plaf.mint.MintLookAndFeel";
                        MintLookAndFeel.setCurrentTheme(
                                addCustom(prepareTheme(MintLookAndFeel.getThemeProperties(theme))));
                        break;
                    case "noire":
                        laf = "com.jtattoo.plaf.noire.NoireLookAndFeel";
                        NoireLookAndFeel.setCurrentTheme(addCustom(prepareTheme(
                                NoireLookAndFeel.getThemeProperties(theme))));
                        break;
                    case "graphite":
                        laf = "com.jtattoo.plaf.graphite.GraphiteLookAndFeel";
                        GraphiteLookAndFeel.setCurrentTheme(addCustom(prepareTheme(
                                GraphiteLookAndFeel.getThemeProperties(theme))));
                        break;
                    case "fast":
                        laf = "com.jtattoo.plaf.fast.FastLookAndFeel";
                        FastLookAndFeel.setCurrentTheme(addCustom(prepareTheme(
                                FastLookAndFeel.getThemeProperties(theme))));
                        break;
                    case "aero":
                        laf = "com.jtattoo.plaf.aero.AeroLookAndFeel";
                        AeroLookAndFeel.setCurrentTheme(addCustom(prepareTheme(
                                AeroLookAndFeel.getThemeProperties(theme))));
                        break;
                    case "luna":
                        laf = "com.jtattoo.plaf.luna.LunaLookAndFeel";
                        LunaLookAndFeel.setCurrentTheme(addCustom(prepareTheme(
                                LunaLookAndFeel.getThemeProperties(theme))));
                        break;
                    default:
                        laf = UIManager.getCrossPlatformLookAndFeelClassName();
                        MetalLookAndFeel.setCurrentTheme(new OceanTheme());
                }
            }

            LOGGER.info("[LAF] Set " + lafCode + "/" + theme + " [" + laf + "]");
            UIManager.setLookAndFeel(laf);
        } catch (Exception ex) {
            LOGGER.warning("Failed setting LAF: "+ex);
        }
        
        if (lafCode.equals("hifi") || lafCode.equals("hifi2")
                || lafCode.equals("noire")) {
            linkColor = "#EEEEEE";
            isDarkTheme = true;
        } else {
            linkColor = "#0000FF";
            isDarkTheme = false;
        }
    }
    
    private static Properties prepareTheme(Properties properties) {
        if (properties == null) {
            properties = new Properties();
        } else {
            properties = new Properties(properties);
        }
        properties.put("logoString", "");
        properties.put("backgroundPattern", "off");
        return properties;
    }
    
    private static Properties addCustom(Properties properties) {
        if (settings != null) {
            Map<String, String> map = settings.getMap("lafCustomTheme");
            if (!map.isEmpty()) {
                properties.putAll(map);
                LOGGER.info("[LAF] Set Custom: "+map);
            }
        }
        return properties;
    }
    
    public static void updateLookAndFeel() {
        for (Window w : Window.getWindows()) {
            if (w.isDisplayable()) {
                SwingUtilities.updateComponentTreeUI(w);
            }
        }
    }
}
