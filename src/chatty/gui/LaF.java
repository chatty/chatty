
package chatty.gui;

import chatty.gui.LaFCustomDefaults.UIResourceMatteBorder;
import chatty.gui.components.settings.SettingsDialog;
import chatty.util.StringUtil;
import chatty.util.colors.ColorCorrectionNew;
import chatty.util.colors.HtmlColors;
import chatty.util.settings.Settings;
import com.jtattoo.plaf.aero.AeroLookAndFeel;
import com.jtattoo.plaf.fast.FastLookAndFeel;
import com.jtattoo.plaf.graphite.GraphiteLookAndFeel;
import com.jtattoo.plaf.hifi.HiFiLookAndFeel;
import com.jtattoo.plaf.luna.LunaLookAndFeel;
import com.jtattoo.plaf.mint.MintLookAndFeel;
import com.jtattoo.plaf.noire.NoireLookAndFeel;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

/**
 *
 * @author tduva
 */
public class LaF {
    
    private static final Logger LOGGER = Logger.getLogger(LaF.class.getName());
    
    private static LaFSettings settings;
    private static String linkColor = "#0000FF";
    private static boolean isDarkTheme;
    private static String lafClass;
    private static Color tabForegroundUnread = new Color(200,0,0);
    private static Color tabForegroundHighlight = new Color(255,80,0);
    private static Border inputBorder;
    
    public static String getLinkColor() {
        return linkColor;
    }
    
    public static Color getTabForegroundUnread() {
        return tabForegroundUnread;
    }
    
    public static Color getTabForegroundHighlight() {
        return tabForegroundHighlight;
    }
    
    public static boolean isDarkTheme() {
        return isDarkTheme;
    }
    
    public static Border getInputBorder() {
        return inputBorder;
    }
    
    public static boolean shouldUpdate(String settingName) {
        List<String> settingNames = Arrays.asList(new String[]{
            "laf", "lafTheme", "lafScroll", "lafForeground", "lafBackground",
            "lafStyle", "lafCustomTheme", "lafGradient", "lafVariant"});
        return settingNames.contains(settingName);
    }
    
    public static class LaFSettings {
        
        public final String lafCode;
        public final String theme;
        public final int fontScale;
        public final Map<String, String> custom;
        public final Color bg;
        public final Color fg;
        public final String style;
        public final int gradient;
        public final String scroll;
        public final int variant;
        
        public LaFSettings(String lafCode, String theme, int fontScale,
                           Map<String, String> custom, Color fg, Color bg,
                           String style, int gradient, String scroll, int variant) {
            this.lafCode = lafCode;
            this.theme = theme;
            this.fontScale = fontScale;
            this.custom = custom;
            this.fg = fg;
            this.bg = bg;
            this.style = style;
            this.gradient = gradient;
            this.scroll = scroll;
            this.variant = variant;
        }
        
        public static LaFSettings fromSettings(Settings settings) {
            String lafCode = settings.getString("laf");
            String lafTheme = settings.getString("lafTheme");
            int lafFontScale = (int)settings.getLong("lafFontScale");
            Color bg = HtmlColors.decode(settings.getString("lafBackground"), Color.BLACK);
            Color fg = HtmlColors.decode(settings.getString("lafForeground"), Color.WHITE);
            String style = settings.getString("lafStyle");
            int gradient = (int)(settings.getLong("lafGradient"));
            int variant = (int)(settings.getLong("lafVariant"));
            String scroll = settings.getString("lafScroll");
            Map<String, String> custom = settings.getMap("lafCustomTheme");
            return new LaFSettings(lafCode, lafTheme, lafFontScale, custom, fg, bg, style, gradient, scroll, variant);
        }
        
        public static LaFSettings fromSettingsDialog(SettingsDialog d, Settings settings) {
            String lafCode = d.getStringSetting("laf");
            String lafTheme = d.getStringSetting("lafTheme");
            int lafFontScale = ((Number)d.getLongSetting("lafFontScale")).intValue();
            Color bg = HtmlColors.decode(d.getStringSetting("lafBackground"), Color.BLACK);
            Color fg = HtmlColors.decode(d.getStringSetting("lafForeground"), Color.WHITE);
            String style = d.getStringSetting("lafStyle");
            int gradient = ((Number)(d.getLongSetting("lafGradient"))).intValue();
            int variant = ((Number)(d.getLongSetting("lafVariant"))).intValue();
            String scroll = d.getStringSetting("lafScroll");
            Map<String, String> custom = settings.getMap("lafCustomTheme");
            return new LaFSettings(lafCode, lafTheme, lafFontScale, custom, fg, bg, style, gradient, scroll, variant);
        }
        
    }
    
    public static void setLookAndFeel(LaFSettings settings) {
        LaFUtil.resetDefaults();
        inputBorder = null;
        LaF.settings = settings;
        String lafCode = settings.lafCode;
        String theme = settings.theme;
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
                        Properties p1 = prepareTheme(HiFiLookAndFeel.getThemeProperties(theme));
                        // Default selection background color isn't really well
                        // visible with "Dark" preset input box colors, a better
                        // color than this could probably be chosen, it has to
                        // fit in other place as well though (see hifi2 as well)
                        p1.put("selectionBackgroundColor", "65 65 65");
                        HiFiLookAndFeel.setCurrentTheme(addCustom(p1));
                        break;
                    case "hifi2":
                        laf = "com.jtattoo.plaf.hifi.HiFiLookAndFeel";
                        Properties p2 = prepareTheme(HiFiLookAndFeel.getThemeProperties(theme));
                        //p.put("backgroundColor", "50 54 52");
                        p2.put("backgroundColor", "50 52 51");
                        p2.put("foregroundColor", "180 190 185");
                        p2.put("menuForegroundColor", "200 210 204");
                        p2.put("menuBackgroundColor", "40 42 40");
                        p2.put("inputForegroundColor", "190 200 195");
                        p2.put("inputBackgroundColor", "60 64 62");
                        p2.put("buttonForegroundColor", "210 220 215");
                        p2.put("buttonColorLight", "96 96 96");
                        p2.put("buttonColorDark", "44 44 44");
                        // See "hifi" comment
                        p2.put("selectionBackgroundColor", "65 65 65");
                        //p.put("backgroundColorDark", "255 0 0");
                        //p.put("backgroundColorLight", "0 0 0");
                        HiFiLookAndFeel.setCurrentTheme(addCustom(p2));
                        break;
                    case "hifiCustom":
                        laf = "com.jtattoo.plaf.hifi.HiFiLookAndFeel";
                        Properties p4 = prepareTheme(HiFiLookAndFeel.getThemeProperties(theme));
                        customColors(p4);
                        HiFiLookAndFeel.setCurrentTheme(addCustom(p4));
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
                    case "nimbus":
                        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                            if ("Nimbus".equals(info.getName())) {
                                laf = info.getClassName();
                                break;
                            }
                        }
                        break;
                    default:
                        laf = UIManager.getCrossPlatformLookAndFeelClassName();
                        MetalLookAndFeel.setCurrentTheme(new OceanTheme());
                }
            }
            
            LOGGER.info("[LAF] Set " + lafCode + "/" + theme + " [" + laf + "]");
            UIManager.setLookAndFeel(laf);
            lafClass = laf;
            modifyDefaults();
        } catch (Exception ex) {
            LOGGER.warning("[LAF] Failed setting LAF: "+ex);
        }
        
        isDarkTheme = determineDarkTheme();
        // Set some settings not directly used by the LAF, but based on LAF.
        if (isDarkTheme) {
            linkColor = "#EEEEEE";
            tabForegroundHighlight = new Color(255,180,40);
            tabForegroundUnread = new Color(255,80,80);
        } else {
            linkColor = "#0000FF";
        }
        loadOtherCustom();
    }
    
    private static boolean determineDarkTheme() {
        Color color = UIManager.getColor("Panel.background");
        if (color != null && ColorCorrectionNew.getLightness(color) < 128) {
            return true;
        }
        return false;
    }
    
    private static void modifyDefaults() {
        //--------------------------
        // Simple overrides
        //--------------------------
        // Tab rows not overlaying eachother
        LaFUtil.putDefault("TabbedPane.tabRunOverlay", 0);
        
        //--------------------------
        // Special font override
        //--------------------------
        try {
            if (lafClass.equals(UIManager.getSystemLookAndFeelClassName())) {
                Object font = UIManager.getLookAndFeelDefaults().get("TextField.font");
                UIManager.getLookAndFeelDefaults().put("TextArea.font", font);
                UIManager.getDefaults().put("TextArea.font", font);
                LOGGER.info("[LAF] Changed TextArea.font to "+font);
            }
        } catch (Exception ex) {
            LOGGER.warning("[LAF] Failed to change TextArea.font: "+ex);
        }
        
        //--------------------------
        // Font size
        //--------------------------
        int fontScale = settings.fontScale;
        if (fontScale != 100 && fontScale >= 10 && fontScale <= 200) {
            LOGGER.info("[LAF] Applying font scale "+fontScale);
            LaFUtil.modifyDefaults((k, v) -> {
                if (v instanceof FontUIResource) {
                    FontUIResource font = (FontUIResource) v;
                    int step = (fontScale - 100) / 10;
                    int fontSize = font.getSize() + step;
//                    System.out.println(font.getSize()+" => "+fontSize);
                    return new FontUIResource(font.getFamily(), font.getStyle(), fontSize);
                }
                return null;
            });
        }
        
        // Maybe add an option for this sometime
//            modifyDefaults((k, v) -> {
//                if (v instanceof FontUIResource) {
//                    FontUIResource font = (FontUIResource) v;
//                    return new FontUIResource(font.getFamily(), Font.PLAIN, font.getSize());
//                }
//                return null;
//            });
        
        //--------------------------
        // Scrollbar
        //--------------------------
        if (!StringUtil.isNullOrEmpty(settings.scroll)) {
            switch (settings.scroll) {
                case "tiny":
                    LaFUtil.putDefault("ScrollBar.width", 8);
                    break;
                case "smaller":
                    LaFUtil.putDefault("ScrollBar.width", 10);
                    break;
                case "small":
                    LaFUtil.putDefault("ScrollBar.width", 15);
                    break;
            }
        }
        
        //--------------------------
        // Custom
        //--------------------------
        // User-defined overrides last
        for (Map.Entry<String, String> entry : settings.custom.entrySet()) {
            if (entry.getKey().startsWith("$")) {
                String key = entry.getKey().substring(1);
                try {
                    Object value = LaFCustomDefaults.fromString(entry.getValue());
                    if (!key.isEmpty() && value != null) {
                        LaFUtil.putDefault(key, value);
                    }
                }
                catch (Exception ex) {
                    LOGGER.warning("[LAF] Invalid custom: " + entry);
                }
            }
        }
    }
        
    public static void updateLookAndFeel() {
        LaFUtil.updateLookAndFeel();
    }
    
    /**
     * Prepare a JTattoo theme.
     * 
     * @param properties
     * @return 
     */
    private static Properties prepareTheme(Properties properties) {
        if (properties == null) {
            properties = new Properties();
        } else {
            properties = new Properties(properties);
        }
        properties.put("logoString", "");
        properties.put("backgroundPattern", "off");
        if (!StringUtil.isNullOrEmpty(settings.scroll)) {
            switch (settings.scroll) {
                case "tiny":
                case "smaller":
                    properties.put("macStyleScrollBar", "on");
                    break;
            }
        }
        return properties;
    }
    
    /**
     * Add custom properties to a JTattoo theme.
     * 
     * @param properties
     * @return 
     */
    private static Properties addCustom(Properties properties) {
        if (!settings.custom.isEmpty()) {
            properties.putAll(settings.custom);
            LOGGER.info("[LAF] Set Custom: " + settings.custom);
        }
        return properties;
    }
    
    /**
     * Load custom properties that aren't directly used by a LAF.
     */
    private static void loadOtherCustom() {
        if (settings != null) {
            tabForegroundHighlight = loadCustomColor("cTabForegroundHighlight", tabForegroundHighlight);
            tabForegroundUnread = loadCustomColor("cTabForegroundUnread", tabForegroundUnread);
            if (settings.custom.containsKey("cInputBorder")) {
                Object border = LaFCustomDefaults.fromString(settings.custom.get("cInputBorder"));
                if (border instanceof Border) {
                    inputBorder = (Border)border;
                }
            }
        }
    }
    
    /**
     * Get a color from custom properties.
     * 
     * @param properties
     * @param key
     * @param defaultValue
     * @return 
     */
    private static Color loadCustomColor(String key, Color defaultValue) {
        String value = settings.custom.get(key);
        if (value != null) {
            return LaFUtil.parseColor(value, defaultValue);
        }
        return defaultValue;
    }
    
    /**
     * Colors for "hifiCustom" JTattoo LaF.
     * 
     * @param p 
     */
    private static void customColors(Properties p) {
        if (settings == null) {
            return;
        }
        Color bg = settings.bg;
        Color fg = settings.fg;
        float gradient = (float)(settings.gradient / 100.0);
        
        float contrast = 1f;
        switch (settings.variant) {
            case 1:
                contrast = 0.5f;
                break;
            case 2:
                contrast = 1.5f;
                break;
            case 3:
                contrast = 2f;
                break;
            case 4:
                contrast = 2.5f;
                break;
        }
        float contrastLight = (float)(contrast + (1 - contrast)*0.5);
        float contrastLighter = (float)(contrast + (1 - contrast)*0.7);
        
        float titleBgC = 0;
        float inactiveTitleBgC = 0;
        float activeGradient = 0;
        float frameFgC = -0.63f;
        float frame2FgC = -0.45f;
        float controlBgC = 0.03f;
        float buttonBgC = 0.03f;
        float menuBgC = 0f;
        int tabSeparatorStyle = 0;
        
        boolean minimalistic = false;
        
        Color lighterFg = changeColor(fg, 0.3f * contrastLight);
        Color activeTitleFg = changeColor(lighterFg, 0f);
        Color inactiveTitleFg = changeColor(fg, -0.3f);
        
        switch (settings.style) {
            case "classic":
                titleBgC = -0.05f;
                inactiveTitleBgC = -0.12f;
                activeGradient = 0.12f;
                inactiveTitleFg = fg;
                menuBgC = -0.1f;
                activeTitleFg = changeColor(fg, 0.7f);
                controlBgC = -0.1f;
                buttonBgC = 0f;
                tabSeparatorStyle = 1;
                break;
            case "classicStrong":
                titleBgC = -0.2f;
                inactiveTitleBgC = -0.40f;
                activeGradient = 0.12f;
                inactiveTitleFg = fg;
                menuBgC = -0.18f;
                activeTitleFg = changeColor(fg, 0.7f);
                frameFgC = -0.7f;
                frame2FgC = -0.6f;
                controlBgC = -0.3f;
                buttonBgC = -0.05f;
                break;
            case "regular":
                titleBgC = -0.1f;
                inactiveTitleBgC = -0.1f;
                if (settings.variant > 2) {
                    controlBgC = -0.02f;
                    contrast -= 0.5f;
                }
                break;
            case "regularStrong":
                titleBgC = -0.2f;
                inactiveTitleBgC = -0.2f;
                controlBgC = -0.1f;
                break;
            case "simple":
                buttonBgC = 0f;
                break;
            case "sleek":
                buttonBgC = 0f;
                menuBgC = 0.05f;
                minimalistic = true;
                break;
            case "minimal":
                buttonBgC = 0f;
                minimalistic = true;
                break;
        }
        
        titleBgC *= contrast;
        inactiveTitleBgC *= contrast;
        activeGradient *= contrast;
        frameFgC *= contrastLight;
        frame2FgC *= contrastLight;
        controlBgC *= contrastLight;
        buttonBgC *= contrast;
        menuBgC *= contrastLight;
        
        // Window
        Color titleBg = changeColor(bg, titleBgC);
        Color inactiveTitleBg = changeColor(bg, inactiveTitleBgC);
        setColor(p, "windowInactiveTitleForegroundColor", inactiveTitleFg);
        setColorG(p, "windowInactiveTitleColor", inactiveTitleBg, 0, gradient);
        setColor(p, "windowTitleForegroundColor", activeTitleFg);
        setColorG(p, "windowTitleColor", titleBg, 0, gradient + activeGradient);
        if (minimalistic) {
            setColor(p, "windowInnerBorderColor", bg);
            setColor(p, "windowInactiveInnerBorderColor", bg);
        }
        setColor(p, "windowIconColor", inactiveTitleFg);
        setColor(p, "windowIconRolloverColor", activeTitleFg);
        
        // Window Border
        setColor(p, "windowBorderColor", bg, -0.4f * contrastLight);
        setColor(p, "windowInactiveBorderColor", bg, -0.4f * contrastLight);
        
        // Some frames (like scrollpane, tabpane and more)
        if (minimalistic) {
            LaFUtil.putDefault("ScrollPane.border", LaFCustomDefaults.EMPTY_BORDER);
            inputBorder = (Border)LaFCustomDefaults.fromString("border(1)");
        }
        if (settings.style.equals("simple")) {
            inputBorder = new UIResourceMatteBorder(0, 1, 1, 1, changeColor(bg, frameFgC));
        }
        setColor(p, "frameColor", bg, frameFgC);
        setColor(p, "frameColor2", bg, frame2FgC);
        
        // General
        setColor(p, "foregroundColor", fg, 0);
        setColor(p, "backgroundColor", bg);
        
        // "Greyed out" GUI elements
        setColor(p, "disabledBackgroundColor", bg, -0.04f);
        setColor(p, "disabledForegroundColor", fg, -0.4f);
        
        // Menu
        setColor(p, "menuForegroundColor", lighterFg);
        setColor(p, "menuBackgroundColor", bg, menuBgC);
        setColor(p, "menuSelectionForegroundColor", lighterFg, 0.4f * contrastLight);
        setColor(p, "menuSelectionBackgroundColor", bg, 0.2f * contrastLight);
        
        Color controlColor = changeColor(bg, controlBgC);
        Color buttonColor = changeColor(bg, buttonBgC);
        // Buttons
        setColorG(p, "buttonColor", buttonColor, 0, gradient);
        setColor(p, "buttonForegroundColor", lighterFg);
        setColorG(p, "pressedBackgroundColor", buttonColor, 0.05f, gradient);
        
        // Buttons/Tabs
        setColor(p, "rolloverForegroundColor", lighterFg, 0.4f * contrastLight);
        setColorG(p, "rolloverColor", controlColor, 0.1f * contrastLight, gradient*1.4f);
        
        // Input
        setColor(p, "inputForegroundColor", fg, 0f);
        setColor(p, "selectionBackgroundColor", bg, 0.2f);
        Color inputBg = changeColor(bg, 0.07f * contrastLight);
        setColor(p, "inputBackgroundColor", inputBg);
        setColor(p, "focusBackgroundColor", inputBg);
        
        // Tabs and other
        setColor(p, "controlForegroundColor", lighterFg);
        setColorG(p, "controlColor", controlColor, 0, gradient*1.05f);
        setColorG(p, "inactiveColor", controlColor, 0, gradient*1.05f);
        setColor(p, "tabAreaBackgroundColor", bg);
        setColorG(p, "selectionBackgroundColor", changeColor(controlColor, 0.15f * contrastLighter), 0, gradient*1.7f);
        p.put("tabSeparatorStyle", String.valueOf(tabSeparatorStyle));
    }
    
    private static void setColor(Properties p, String property, Color base, float offset) {
        setColor(p, property, changeColor(base, offset));
    }
    
    private static void setColorG(Properties p, String property, Color base, float offset, float offsetG) {
        Color offsetBase = changeColor(base, offset);
        // For gradient, make color both a bit darker and lighter
        setColorG(p, property,
                changeColor(offsetBase, offsetG * 0.55f),
                changeColor(offsetBase, -offsetG * 0.4f));
        // For testing
//        setColorG(p, property, ColorCorrectionNew.makeBrighter(offsetBase, offsetG), offsetBase);
    }
    
    private static void setColor(Properties p, String property, Color color) {
        p.put(property, String.format("%d %d %d", color.getRed(), color.getGreen(), color.getBlue()));
    }
    
    private static void setColorG(Properties p, String property, Color cl, Color cd) {
        p.put(property+"Light", String.format("%d %d %d", cl.getRed(), cl.getGreen(), cl.getBlue()));
        p.put(property+"Dark", String.format("%d %d %d", cd.getRed(), cd.getGreen(), cd.getBlue()));
    }
    
    /**
     * An explicit -change (darker) or +change (brighter), independant of what
     * the color currently is.
     * 
     * @param color
     * @param change
     * @return 
     */
    private static Color changeColor(Color color, float change) {
        if (change > 1) {
            change = 1;
        }
        if (change < -1) {
            change = -1;
        }
        if (change < 0) {
            return ColorCorrectionNew.makeDarker(color, 1 - Math.abs(change));
        }
        return ColorCorrectionNew.makeBrighter(color, change);
    }
    
}
