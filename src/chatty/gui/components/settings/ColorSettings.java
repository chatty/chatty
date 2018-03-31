
package chatty.gui.components.settings;

import static chatty.gui.components.settings.SettingConstants.HTML_PREFIX;
import chatty.lang.Language;
import chatty.util.settings.Settings;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
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
    private final JPanel main;
    private final ColorTemplates presets;
    
    public ColorSettings(SettingsDialog d, Settings settings) {
        this.d = d;
        colorChooser = new ColorChooser(d);

        main = addTitledPanel(Language.getString("settings.section.colors"), 0);
        
        GridBagConstraints gbc;

        presets = new ColorTemplates(settings, "colorPresets",
                addColorSetting("backgroundColor", ColorSetting.BACKGROUND, "foregroundColor",
                        Language.getString("settings.colors.background"), 1),
                addColorSetting("foregroundColor", ColorSetting.FOREGROUND, "backgroundColor",
                        Language.getString("settings.colors.foreground"), 2),
                addColorSetting("infoColor", ColorSetting.FOREGROUND, "backgroundColor",
                        Language.getString("settings.colors.info"), 3),
                addColorSetting("compactColor", ColorSetting.FOREGROUND, "backgroundColor",
                        Language.getString("settings.colors.compact"), 4),
                addColorSetting("highlightColor", ColorSetting.FOREGROUND, "backgroundColor",
                        Language.getString("settings.colors.highlight"), 5),
                addColorSetting("inputBackgroundColor", ColorSetting.BACKGROUND, "inputForegroundColor",
                        Language.getString("settings.colors.inputBackground"), 6),
                addColorSetting("inputForegroundColor", ColorSetting.FOREGROUND, "inputBackgroundColor",
                        Language.getString("settings.colors.inputForeground"), 7),
                addColorSetting("searchResultColor", ColorSetting.BACKGROUND, "foregroundColor",
                        Language.getString("settings.colors.searchResult"), 8),
                addColorSetting("searchResultColor2", ColorSetting.BACKGROUND, "foregroundColor",
                        Language.getString("settings.colors.searchResult2"), 9)
        );
        
        presets.addPreset(Language.getString("settings.colorPresets.option.default"),
                settings.getStringDefault("backgroundColor"),
                settings.getStringDefault("foregroundColor"),
                settings.getStringDefault("infoColor"),
                settings.getStringDefault("compactColor"),
                settings.getStringDefault("highlightColor"),
                settings.getStringDefault("inputBackgroundColor"),
                settings.getStringDefault("inputForegroundColor"),
                settings.getStringDefault("searchResultColor"),
                settings.getStringDefault("searchResultColor2"));
            
        presets.addPreset(Language.getString("settings.colorPresets.option.dark"),
                "#111111", "LightGrey", "DeepSkyBlue", "#A0A0A0", "Red",
                "#222222", "White", "DarkSlateBlue", "SlateBlue");
        
        presets.addPreset(Language.getString("settings.colorPresets.option.dark2"),
                "Black", "White", "#FF9900", "#FFCC00", "#66FF66",
                "#FFFFFF", "#000000", "#333333", "#555555");
        
        presets.init();
        
        gbc = d.makeGbc(0, 0, 1, 1);
        main.add(presets, gbc);

        gbc = d.makeGbc(0, 10, 1, 1);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 5, 5, 5);
        main.add(new JLabel(HTML_PREFIX+Language.getString("settings.colors.lookandfeel")), gbc);
    }
    
    
    private ColorSetting addColorSetting(String setting, int type,
            String baseSetting, String colorDescription, int row) {
        ColorSetting colorSetting = new ColorSetting(type, baseSetting, colorDescription, colorDescription, colorChooser);
        colorSetting.addListener(new MyColorSettingListener(setting));
        d.addStringSetting(setting, colorSetting);
        colorSettings.put(setting, colorSetting);
        GridBagConstraints gbc = d.makeGbc(0, row, 1, 1);
        gbc.insets = new Insets(0,0,0,0);
        main.add(colorSetting, gbc);
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
                colorSetting.update(newColor);
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
    
    /**
     * Defines color presets, kind of a color theme, that can be loaded into
     * the settings by selecting it from a combo box.
     */
    class Presets extends JPanel {
        
        private final Map<String, Map<String, String>> presets = new HashMap<>();
        private final JComboBox<String> combo = new JComboBox<>();
        private final JButton loadPreset = new JButton("Load preset");
        
        Presets() {
            initPresets();
            
            combo.setEditable(false);
            
            for (String presetName : presets.keySet()) {
                combo.addItem(presetName);
            }
            
            add(combo);
            add(loadPreset);
            
            loadPreset.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String selected = combo.getItemAt(combo.getSelectedIndex());
                    loadPreset(selected);
                }
            });
        }
        
        /**
         * Load the preset with the given name. Sets all the String settings
         * defined in this preset.
         * 
         * @param presetName 
         */
        private void loadPreset(String presetName) {
            if (!presets.containsKey(presetName)) {
                return;
            }
            
            Map<String, String> loadedPreset = presets.get(presetName);
            for (String setting : loadedPreset.keySet()) {
                String color = loadedPreset.get(setting);
                d.setStringSetting(setting, color);
            }
        }
        
        /**
         * Create presets for each setting. Using the exact setting names as
         * keys for the Map.
         */
        private void initPresets() {
            Map<String, String> defaultColors = new HashMap<>();
            defaultColors.put("backgroundColor", "#FAFAFA");
            defaultColors.put("foregroundColor", "#111111");
            defaultColors.put("infoColor", "#001480");
            defaultColors.put("compactColor", "#A0A0A0");
            defaultColors.put("highlightColor", "Red");
            defaultColors.put("inputBackgroundColor", "White");
            defaultColors.put("inputForegroundColor", "Black");
            defaultColors.put("searchResultColor", "LightYellow");
            defaultColors.put("searchResultColor2", "#FFFF80");
            
            presets.put("default", defaultColors);
            
            Map<String, String> darkColors = new HashMap<>();
            darkColors.put("backgroundColor", "#111111");
            darkColors.put("foregroundColor", "LightGrey");
            darkColors.put("infoColor", "DeepSkyBlue");
            darkColors.put("compactColor", "#A0A0A0");
            darkColors.put("highlightColor", "Red");
            darkColors.put("inputBackgroundColor", "#222222");
            darkColors.put("inputForegroundColor", "White");
            darkColors.put("searchResultColor", "DarkSlateBlue");
            darkColors.put("searchResultColor2", "SlateBlue");
            
            presets.put("dark", darkColors);
            
            Map<String, String> darkColors2 = new HashMap<>();
            darkColors2.put("backgroundColor", "Black");
            darkColors2.put("foregroundColor", "White");
            darkColors2.put("infoColor", "#FF9900");
            darkColors2.put("compactColor", "#FFCC00");
            darkColors2.put("highlightColor", "#66FF66");
            darkColors2.put("inputBackgroundColor", "#FFFFFF");
            darkColors2.put("inputForegroundColor", "#000000");
            darkColors2.put("searchResultColor", "#333333");
            darkColors2.put("searchResultColor2", "#555555");
            
            presets.put("dark2", darkColors2);
        }
    }
    
    
}
