
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import static chatty.gui.components.settings.SettingConstants.HTML_PREFIX;
import chatty.lang.Language;
import chatty.util.settings.Settings;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class ColorSettings extends SettingsPanel {
    
    public static final String[] PRESET_SETTINGS = new String[]{
        "backgroundColor",
        "foregroundColor",
        "infoColor",
        "compactColor",
        "highlightColor",
        "inputBackgroundColor",
        "inputForegroundColor",
        "searchResultColor",
        "searchResultColor2",
        "backgroundColor2",
        "highlightBackgroundColor",
        "separatorColor",
        "timestampColor",
        "timestampColorInherit"
    };
    
    public static final String[] PRESET_SETTINGS_BOOLEAN = new String[]{
        "alternateBackground",
        "messageSeparator",
        "highlightBackground",
        "timestampColorEnabled"
    };
    
    public static final String[] DARK = new String[]{
        "#111111", // backgroundColor
        "LightGrey", // foregroundColor
        "DeepSkyBlue", // infoColor
        "#A0A0A0", // compactColor
        "#DDDDDD", // highlightColor
        "#222222", // inputBackgroundColor
        "White", // inputForegroundColor
        "DarkSlateBlue", // searchResultColor
        "SlateBlue", // searchResultColor2
        "#2D2D2D", // backgroundColor2
        "#7A0000", // highlightBackgroundColor
        "#383838", // separatorColor
        "LightGrey", // timestampColor
        "off", // timestampColorInherit
    };
    
    public static final Boolean[] DARK_BOOLEAN = new Boolean[]{
        false, // alternateBackground
        false, // messageSeparator
        true, // highlightBackground
        false, // timestampColorEnabled
    };
    
    public static final String[] DARK_SMOOTH = new String[]{
        "#323232", // backgroundColor
        "LightGrey", // foregroundColor
        "Aquamarine", // infoColor
        "#A0A0A0", // compactColor
        "#FFFFFF", // highlightColor
        "#222222", // inputBackgroundColor
        "#FFFFFF", // inputForegroundColor
        "DarkSlateBlue", // searchResultColor
        "SlateBlue", // searchResultColor2
        "#3B3B3B", // backgroundColor2
        "#5C0000", // highlightBackgroundColor
        "#DFDFDF", // separatorColor
        "#C5C5C5", // timestampColor
        "40", // timestampColorInherit
    };
    
    public static final Boolean[] DARK_SMOOTH_BOOLEAN = new Boolean[]{
        true, // alternateBackground
        false, // messageSeparator
        true, // highlightBackground
        true, // timestampColorEnabled
    };
    
    public static final String[] LIGHT_FAST = new String[]{
        "#FAFAFA", // backgroundColor
        "#111111", // foregroundColor
        "#001480", // infoColor
        "#A0A0A0", // compactColor
        "#D10000", // highlightColor
        "White", // inputBackgroundColor
        "Black", // inputForegroundColor
        "LightYellow", // searchResultColor
        "#FFFF80", // searchResultColor2
        "#EAEAEA", // backgroundColor2
        "#FFFFEA", // highlightBackgroundColor
        "#DFDFDF", // separatorColor
        "#6E6779", // timestampColor
        "30", // timestampColorInherit
    };
    
    public static final Boolean[] LIGHT_FAST_BOOLEAN = new Boolean[]{
        false, // alternateBackground
        false, // messageSeparator
        true, // highlightBackground
        true, // timestampColorEnabled
    };
    
    private final SettingsDialog d;
    private final Map<String, ColorSetting> colorSettings = new HashMap<>();
    private ColorChooser colorChooser;
    private final JPanel mainPanel;
    private final JPanel colorsPanel;
    private final ColorTemplates presets;
    
    public ColorSettings(SettingsDialog d, Settings settings) {
        this.d = d;

        mainPanel = addTitledPanel(Language.getString("settings.section.colors"), 0);
        colorsPanel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        //----------------------------------------------
        // Color settings that require special handling
        //----------------------------------------------
        ColorSetting backgroundColor = addColorSetting(
                "backgroundColor",
                ColorSetting.BACKGROUND,
                "foregroundColor",
                Language.getString("settings.colors.background"),
                0, 0);
        
        ColorSetting backgroundColor2 = addColorSetting(
                "backgroundColor2",
                ColorSetting.BACKGROUND,
                "foregroundColor",
                Language.getString("settings.colors.background2"),
                1, 0);
        
        ColorSetting separatorColor = addColorSetting(
                "separatorColor",
                ColorSetting.FOREGROUND,
                "backgroundColor",
                Language.getString("settings.colors.messageSeparator"),
                3, 0);

        ColorSetting highlightBackgroundColor = addColorSetting(
                "highlightBackgroundColor",
                ColorSetting.BACKGROUND,
                "highlightColor",
                Language.getString("settings.colors.highlightBackground"),
                11, 0);
        highlightBackgroundColor.setEnabled(false);
        
        ColorSetting highlightColor = addColorSetting(
                "highlightColor",
                ColorSetting.FOREGROUND,
                "backgroundColor",
                Language.getString("settings.colors.highlight"),
                11, 1);
        
        ColorSetting timestampColor = addColorSetting(
                "timestampColor",
                ColorSetting.FOREGROUND,
                "backgroundColor",
                Language.getString("settings.colors.timestamp"),
                8, 0);
        switchOnHover(highlightBackgroundColor, "backgroundColor", "highlightBackgroundColor", "highlightBackground", timestampColor);
        
        Map<String, String> timestampInheritOptions = new LinkedHashMap<>();
        timestampInheritOptions.put("off", "Off");
        for (int i=10;i<=100;i+=10) {
            timestampInheritOptions.put(String.valueOf(i), i+"%");
        }
        ComboStringSetting timestampInheritSelection = new ComboStringSetting(timestampInheritOptions);
        d.addStringSetting("timestampColorInherit", timestampInheritSelection);
        
        //------------------------------------------------
        // Boolean settings that require special handling
        //------------------------------------------------
        SimpleBooleanSetting alternateBackground = d.addSimpleBooleanSetting("alternateBackground");
        SimpleBooleanSetting messageSeparator = d.addSimpleBooleanSetting("messageSeparator");
        SimpleBooleanSetting highlightBackground = d.addSimpleBooleanSetting("highlightBackground");
        SimpleBooleanSetting timestampColorEnabled = d.addSimpleBooleanSetting("timestampColorEnabled");

        //=====================
        // Template definition
        //=====================
        // The order the settings are added in matters for presets
        presets = new ColorTemplates(settings, "colorPresets",
                new StringSetting[]{
                    backgroundColor,
                    addColorSetting(
                            "foregroundColor",
                            ColorSetting.FOREGROUND,
                            "backgroundColor",
                            Language.getString("settings.colors.foreground"),
                            0, 1),
                    addColorSetting(
                            "infoColor",
                            ColorSetting.FOREGROUND,
                            "backgroundColor",
                            Language.getString("settings.colors.info"),
                            6, 0),
                    addColorSetting(
                            "compactColor",
                            ColorSetting.FOREGROUND,
                            "backgroundColor",
                            Language.getString("settings.colors.compact"),
                            6, 1),
                    highlightColor,
                    addColorSetting(
                            "inputBackgroundColor",
                            ColorSetting.BACKGROUND,
                            "inputForegroundColor",
                            Language.getString("settings.colors.inputBackground"),
                            7, 0),
                    addColorSetting(
                            "inputForegroundColor",
                            ColorSetting.FOREGROUND,
                            "inputBackgroundColor",
                            Language.getString("settings.colors.inputForeground"),
                            7, 1),
                    addColorSetting(
                            "searchResultColor",
                            ColorSetting.BACKGROUND,
                            "foregroundColor",
                            Language.getString("settings.colors.searchResult"),
                            14, 0),
                    addColorSetting(
                            "searchResultColor2",
                            ColorSetting.BACKGROUND,
                            "foregroundColor",
                            Language.getString("settings.colors.searchResult2"),
                            14, 1),
                    backgroundColor2,
                    highlightBackgroundColor,
                    separatorColor,
                    timestampColor,
                    timestampInheritSelection
                },
                new BooleanSetting[]{
                    alternateBackground, messageSeparator, highlightBackground,
                    timestampColorEnabled
                }
        );
        
        //-------------------
        // Hardcoded Presets
        //-------------------
        String[] defaultValues = new String[PRESET_SETTINGS.length];
        for (int i = 0; i < PRESET_SETTINGS.length; i++) {
            defaultValues[i] = settings.getStringDefault(PRESET_SETTINGS[i]);
        }
        Boolean[] defaultValuesBoolean = new Boolean[PRESET_SETTINGS_BOOLEAN.length];
        for (int i = 0; i < PRESET_SETTINGS_BOOLEAN.length; i++) {
            defaultValuesBoolean[i] = settings.getBooleanDefault(PRESET_SETTINGS_BOOLEAN[i]);
        }
        presets.addPreset(Language.getString("settings.colorPresets.option.default"),
                defaultValues,
                defaultValuesBoolean);
            
        presets.addPreset(Language.getString("settings.colorPresets.option.dark"),
                DARK,
                DARK_BOOLEAN
        );
        
        presets.addPreset(Language.getString("settings.colorPresets.option.dark2"),
                new String[]{
                    "Black", // backgroundColor
                    "White", // foregroundColor
                    "#FF9900", // infoColor
                    "#FFCC00", // compactColor
                    "#66FF66", // highlightColor
                    "#FFFFFF", // inputBackgroundColor
                    "#000000", // inputForegroundColor
                    "#333333", // searchResultColor
                    "#555555", // searchResultColor2
                    "#1E1E1E", // backgroundColor2
                    "#660000", // highlightBackgroundColor
                    "#7A4B00", // separatorColor
                    "White",  // timestampColor
                    "off", // timestampColorInherit
                },
                new Boolean[]{
                    false, // alternateBackground
                    false, // messageSeparator
                    false, // highlightBackground
                    false, // timestampColorEnabled
                });
        
        presets.addPreset("Twitch",
                new String[]{
                    "#EFEEF1", // backgroundColor
                    "#111111", // foregroundColor
                    "#001480", // infoColor
                    "#A0A0A0", // compactColor
                    "#111111", // highlightColor
                    "White", // inputBackgroundColor
                    "#111111", // inputForegroundColor
                    "LightYellow", // searchResultColor
                    "#FFFF80", // searchResultColor2
                    "#DBDBDB", // backgroundColor2
                    "#F0A5B0", // highlightBackgroundColor
                    "#C6C6C6", // separatorColor
                    "#6e6779",  // timestampColor
                    "100", // timestampColorInherit
                },
                new Boolean[]{
                    false, // alternateBackground
                    false, // messageSeparator
                    true,  // highlightBackground
                    true, // timestampColorEnabled
                });
        
        presets.addPreset("Twitch Dark",
                new String[]{
                    "#17141A", // backgroundColor
                    "#CFC8CD", // foregroundColor
                    "White", // infoColor
                    "#A0A0A0", // compactColor
                    "#D6D0D4", // highlightColor
                    "#17141A", // inputBackgroundColor
                    "#CFC8CD", // inputForegroundColor
                    "#333333", // searchResultColor
                    "#555555", // searchResultColor2
                    "#241F29", // backgroundColor2
                    "#590E1A", // highlightBackgroundColor
                    "#7A4B00", // separatorColor
                    "#898395",  // timestampColor
                    "70", // timestampColorInherit
                }, 
                new Boolean[]{
                    true,  // alternateBackground
                    false, // messageSeparator
                    true,  // highlightBackground
                    true, // timestampColorEnabled
                });
        
        presets.addPreset("Theater",
                new String[]{
                    "#0E0C13", // backgroundColor
                    "#DAD8DE", // foregroundColor
                    "#A0A0A0", // infoColor
                    "#6A7559", // compactColor
                    "#D88A35", // highlightColor
                    "#0E0C13", // inputBackgroundColor
                    "#DAD8DE", // inputForegroundColor
                    "#31362F", // searchResultColor
                    "#444B42", // searchResultColor2
                    "#191522", // backgroundColor2
                    "#660000", // highlightBackgroundColor
                    "#2D2D2D", // separatorColor
                    "#898395",  // timestampColor
                    "100", // timestampColorInherit
                }, 
                new Boolean[]{
                    false, // alternateBackground
                    false, // messageSeparator
                    false, // highlightBackground
                    true, // timestampColorEnabled
                });
        
        presets.addPreset("Dark Smooth",
                DARK_SMOOTH,
                DARK_SMOOTH_BOOLEAN
        );
        
        presets.init();
        
        //========
        // Layout
        //========
        
        //------------------
        // Boolean Settings
        //------------------
        // Alternating Backgrounds boolean setting
        gbc = d.makeGbc(0, 2, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(-1,10,0,0);
        SettingsUtil.addSubsettings(alternateBackground, backgroundColor2);
        colorsPanel.add(alternateBackground, gbc);
        
        // Message Separator boolean setting
        gbc = d.makeGbc(0, 4, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(-1,10,0,0);
        SettingsUtil.addSubsettings(messageSeparator, separatorColor);
        colorsPanel.add(messageSeparator, gbc);
        
        // Highlight Background boolean setting
        gbc = d.makeGbc(0, 12, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(-1,10,0,0);
        highlightBackground.addItemListener(e -> {
            if (highlightBackground.isSelected()) {
                highlightColor.setBaseColorSetting("highlightBackgroundColor");
                updated("highlightBackgroundColor");
            } else {
                highlightColor.setBaseColorSetting("backgroundColor");
                updated("backgroundColor");
            }
            highlightBackgroundColor.setEnabled(highlightBackground.isSelected());
        });
        colorsPanel.add(highlightBackground, gbc);
        
        // Timestamp boolean setting
        gbc = d.makeGbc(0, 9, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(-1,10,0,0);
        SettingsUtil.addSubsettings(timestampColorEnabled, timestampColor, timestampInheritSelection);
        colorsPanel.add(timestampColorEnabled, gbc);
        
        gbc = d.makeGbc(1, 9, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(-1,10,0,0);
        colorsPanel.add(SettingsUtil.createPanel("timestampColorInherit", timestampInheritSelection), gbc);
        
        //--------------------------
        // Background Switch Button
        //--------------------------
        gbc = d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST);
        JButton switchButton = new JButton(Language.getString("settings.colors.button.switchBackgrounds"));
        switchButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        switchButton.addActionListener(e -> {
            String bg = backgroundColor.getSettingValue();
            backgroundColor.setSettingValue(backgroundColor2.getSettingValue());
            backgroundColor2.setSettingValue(bg);
        });
        colorsPanel.add(switchButton, gbc);

        //----------------------
        // Color Panel Headings
        //----------------------
        gbc = d.makeGbc(0, 5, 2, 1);
        gbc.insets = new Insets(10, 0, 2, 0);
        colorsPanel.add(new JLabel(Language.getString("settings.colors.heading.misc")), gbc);
        
        gbc = d.makeGbc(0, 10, 2, 1);
        gbc.insets = new Insets(10, 0, 2, 0);
        colorsPanel.add(new JLabel(Language.getString("settings.colors.heading.highlights")), gbc);
        
        gbc = d.makeGbc(0, 13, 2, 1);
        gbc.insets = new Insets(10, 0, 2, 0);
        colorsPanel.add(new JLabel(Language.getString("settings.colors.heading.searchResult")), gbc);

        //------------
        // Main Panel
        //------------
        gbc = d.makeGbc(0, 0, 1, 1);
        mainPanel.add(presets, gbc);
        
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(colorsPanel, gbc);

        gbc = d.makeGbc(0, 20, 1, 1);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 5, 5, 5);
        mainPanel.add(new JLabel(HTML_PREFIX+Language.getString("settings.colors.lookandfeel")), gbc);
    }
    
    
    private ColorSetting addColorSetting(String setting, int type,
            String baseSetting, String colorDescription, int row, int column) {
        String colorType = type == ColorSetting.FOREGROUND
                ? Language.getString("settings.colors.general.foregroundColor")
                : Language.getString("settings.colors.general.backgroundColor");
        String extendedName = colorDescription+" ["+colorType+"]";
        ColorSetting colorSetting = new ColorSetting(type, baseSetting, extendedName, colorDescription,
                () -> {
                    if (colorChooser == null) {
                        colorChooser = new ColorChooser(d);
                    }
                    return colorChooser;
                });
        colorSetting.addListener(new MyColorSettingListener(setting));
        d.addStringSetting(setting, colorSetting);
        colorSettings.put(setting, colorSetting);
        GridBagConstraints gbc = d.makeGbc(column, row, 1, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(3,4,2,4);
        colorsPanel.add(colorSetting, gbc);
        return colorSetting;
    }
    
    /**
     * A single setting was updated, so tell all settings that have the updated
     * color as base (background) to update their preview.
     * 
     * @param setting 
     */
    private void updated(String setting) {
        //System.out.println(setting);
        String newColor = colorSettings.get(setting).getSettingValue();
        //updatedSetting.updated();
        for (ColorSetting colorSetting : colorSettings.values()) {
            if (colorSetting.hasBase(setting)) {
                colorSetting.setBaseColor(newColor);
            }
        }
        d.updateBackgroundColor();
    }
    
    /**
     * Switch the base color of the given settings to tempSetting if the mouse
     * is hovered over hoverSetting.
     * 
     * @param hoverSetting Setting to hover over
     * @param normalSetting Name of the default color setting
     * @param tempSetting Name of the color setting to switch to on hover
     * @param onlyIf Name of a boolean setting, only switch if true (optional)
     * @param settings One or several color settings to switch the base color on
     */
    private void switchOnHover(ColorSetting hoverSetting, String normalSetting, String tempSetting, String onlyIf, ColorSetting... settings) {
        hoverSetting.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                if (onlyIf != null && !d.getBooleanSetting(onlyIf)) {
                    return;
                }
                for (ColorSetting setting : settings) {
                    setting.setBaseColorSetting(tempSetting);
                }
                updated(tempSetting);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                for (ColorSetting setting : settings) {
                    setting.setBaseColorSetting(normalSetting);
                }
                updated(normalSetting);
            }

        });
    }
    
    /**
     * Listen for a color setting to be updated. Save the setting name so it's
     * clear which setting it was.
     */
    class MyColorSettingListener implements ColorSettingListener {

        private final String setting;
        
        MyColorSettingListener(String setting) {
            this.setting = setting;
        }
        
        @Override
        public void colorUpdated() {
            updated(setting);
        }
        
    }
    
}
