
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import static chatty.gui.components.settings.SettingConstants.HTML_PREFIX;
import chatty.lang.Language;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class ColorSettings extends SettingsPanel {
    
    private final SettingsDialog d;
    private final Map<String, ColorSetting> colorSettings = new HashMap<>();
    private final ColorChooser colorChooser;
    private final JPanel mainPanel;
    private final JPanel colorsPanel;
    private final ColorTemplates presets;
    
    public ColorSettings(SettingsDialog d, Settings settings) {
        this.d = d;
        colorChooser = new ColorChooser(d);

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
        backgroundColor2.setEnabled(false);
        
        ColorSetting separatorColor = addColorSetting(
                "separatorColor",
                ColorSetting.FOREGROUND,
                "backgroundColor",
                "Message Separator",
                3, 0);
        separatorColor.setEnabled(false);

        ColorSetting highlightBackgroundColor = addColorSetting(
                "highlightBackgroundColor",
                ColorSetting.BACKGROUND,
                "highlightColor",
                Language.getString("settings.colors.highlightBackground"),
                9, 0);
        highlightBackgroundColor.setEnabled(false);
        
        ColorSetting highlightColor = addColorSetting(
                "highlightColor",
                ColorSetting.FOREGROUND,
                "backgroundColor",
                Language.getString("settings.colors.highlight"),
                9, 1);
        
        //------------------------------------------------
        // Boolean settings that require special handling
        //------------------------------------------------
        SimpleBooleanSetting alternateBackground = d.addSimpleBooleanSetting("alternateBackground");
        SimpleBooleanSetting messageSeparator = d.addSimpleBooleanSetting("messageSeparator");
        SimpleBooleanSetting highlightBackground = d.addSimpleBooleanSetting("highlightBackground");

        //=====================
        // Template definition
        //=====================
        // The order the settings are added in matters for presets
        presets = new ColorTemplates(settings, "colorPresets",
                new ColorSetting[]{
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
                            12, 0),
                    addColorSetting(
                            "searchResultColor2",
                            ColorSetting.BACKGROUND,
                            "foregroundColor",
                            Language.getString("settings.colors.searchResult2"),
                            12, 1),
                    backgroundColor2,
                    highlightBackgroundColor,
                    separatorColor
                },
                new BooleanSetting[]{
                    alternateBackground, messageSeparator, highlightBackground
                }
        );
        
        //-------------------
        // Hardcoded Presets
        //-------------------
        presets.addPreset(Language.getString("settings.colorPresets.option.default"),
                new String[]{
                    settings.getStringDefault("backgroundColor"),
                    settings.getStringDefault("foregroundColor"),
                    settings.getStringDefault("infoColor"),
                    settings.getStringDefault("compactColor"),
                    settings.getStringDefault("highlightColor"),
                    settings.getStringDefault("inputBackgroundColor"),
                    settings.getStringDefault("inputForegroundColor"),
                    settings.getStringDefault("searchResultColor"),
                    settings.getStringDefault("searchResultColor2"),
                    settings.getStringDefault("backgroundColor2"),
                    settings.getStringDefault("highlightBackgroundColor"),
                    settings.getStringDefault("separatorColor")},
                new Boolean[]{
                    settings.getBooleanDefault("alternateBackground"),
                    settings.getBooleanDefault("messageSeparator"),
                    settings.getBooleanDefault("highlightBackground")
                });
            
        presets.addPreset(Language.getString("settings.colorPresets.option.dark"),
                new String[]{
                    "#111111",          // backgroundColor
                    "LightGrey",        // foregroundColor
                    "DeepSkyBlue",      // infoColor
                    "#A0A0A0",          // compactColor
                    "#DDDDDD",          // highlightColor
                    "#222222",          // inputBackgroundColor
                    "White",            // inputForegroundColor
                    "DarkSlateBlue",    // searchResultColor
                    "SlateBlue",        // searchResultColor2
                    "#2D2D2D",          // backgroundColor2
                    "#7A0000",          // highlightBackgroundColor
                    "#383838"},         // separatorColor
                new Boolean[]{
                    false, // alternateBackground
                    false, // messageSeparator
                    true   // highlightBackground
                });
        
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
                    "#7A4B00"}, // separatorColor
                new Boolean[]{
                    false, // alternateBackground
                    false, // messageSeparator
                    false  // highlightBackground
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
                    "#C6C6C6"}, // separatorColor
                new Boolean[]{
                    false, // alternateBackground
                    false, // messageSeparator
                    true  // highlightBackground
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
                    "#7A4B00"}, // separatorColor
                new Boolean[]{
                    true, // alternateBackground
                    false, // messageSeparator
                    true  // highlightBackground
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
                    "#2D2D2D"}, // separatorColor
                new Boolean[]{
                    false, // alternateBackground
                    false, // messageSeparator
                    false  // highlightBackground
                });
        
        presets.addPreset("Dark Smooth",
                new String[]{
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
                    "#DFDFDF"}, // separatorColor
                new Boolean[]{
                    true, // alternateBackground
                    false, // messageSeparator
                    true  // highlightBackground
                });
        
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
        alternateBackground.addItemListener(e -> {
            backgroundColor2.setEnabled(alternateBackground.isSelected());
        });
        colorsPanel.add(alternateBackground, gbc);
        
        // Message Separator boolean setting
        gbc = d.makeGbc(0, 4, 2, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(-1,10,0,0);
        messageSeparator.addItemListener(e -> {
            separatorColor.setEnabled(messageSeparator.isSelected());
        });
        colorsPanel.add(messageSeparator, gbc);
        
        // Highlight Background boolean setting
        gbc = d.makeGbc(0, 10, 2, 1);
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
        
        gbc = d.makeGbc(0, 8, 2, 1);
        gbc.insets = new Insets(10, 0, 2, 0);
        colorsPanel.add(new JLabel(Language.getString("settings.colors.heading.highlights")), gbc);
        
        gbc = d.makeGbc(0, 11, 2, 1);
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
        ColorSetting colorSetting = new ColorSetting(type, baseSetting, extendedName, colorDescription, colorChooser);
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
