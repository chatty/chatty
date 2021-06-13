
package chatty.gui.components.settings;

import chatty.gui.LaF;
import chatty.gui.LaF.LaFSettings;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class LookSettings extends SettingsPanel {

    protected LookSettings(SettingsDialog d) {

        JPanel lafSettingsPanel = addTitledPanel(Language.getString("settings.section.lookandfeel"), 1);
        JPanel fontScalePanel = addTitledPanel("Font Scale (experimental)", 2);
        JPanel previewPanel = addTitledPanel(Language.getString("settings.section.preview"), 3);
        
        GridBagConstraints gbc;
        
        //=============
        // Look & Feel
        //=============
        
        Map<String, String> lafDef = new LinkedHashMap<>();
        lafDef.put("default", "Default");
        lafDef.put("system", "System");
        lafDef.put("hifiCustom", "HiFi Custom (Dark)");
        lafDef.put("hifi2", "HiFi Soft (Dark)");
        lafDef.put("hifi", "HiFi (Dark)");
        lafDef.put("noire", "Noire (Dark)");
        lafDef.put("mint", "Mint");
        lafDef.put("graphite", "Graphite");
        lafDef.put("aero", "Aero");
        lafDef.put("fast", "Fast");
        lafDef.put("luna", "Luna");
        ComboStringSetting laf = new ComboStringSetting(lafDef);
        d.addStringSetting("laf", laf);
        
        LinkLabel lafInfo = new LinkLabel("", d.getLinkLabelListener());
        laf.addItemListener(e -> {
            updateInfo(lafInfo, laf);
        });
        updateInfo(lafInfo, laf);
        
        Map<String, String> themeDef = new LinkedHashMap<>();
        themeDef.put("Default", Language.getString("settings.laf.option.defaultFont"));
        themeDef.put("Small-Font", Language.getString("settings.laf.option.smallFont"));
        themeDef.put("Large-Font", Language.getString("settings.laf.option.largeFont"));
        themeDef.put("Giant-Font", Language.getString("settings.laf.option.giantFont"));
        ComboStringSetting theme = new ComboStringSetting(themeDef);
        d.addStringSetting("lafTheme", theme);
        
        SettingsUtil.addSubsettings(laf, s -> !s.equals("default") && !s.equals("system"), theme);
        
        
        laf.addActionListener(e -> {
            String selected = laf.getSettingValue();
            theme.setEnabled(!selected.equals("default") && !selected.equals("system"));
        });
        
        JButton lafPreviewButton = new JButton("Preview");
        lafPreviewButton.addActionListener(e -> {
            LaF.setLookAndFeel(LaFSettings.fromSettingsDialog(d, d.settings));
            LaF.updateLookAndFeel();
            d.lafPreviewed = true;
            d.pack();
        });
        
        //==========================
        // LaF Settings
        //==========================
        ColorChooser colorChooser = new ColorChooser(d);
        ColorSetting foregroundColor = new ColorSetting(ColorSetting.FOREGROUND,
                "lafBackground",
                Language.getString("settings.general.foreground"),
                Language.getString("settings.general.foreground"),
                colorChooser);
        ColorSetting backgroundColor = new ColorSetting(ColorSetting.BACKGROUND,
                "lafForeground",
                Language.getString("settings.general.background"),
                Language.getString("settings.general.background"),
                colorChooser);
        ColorSettingListener colorChangeListener = new ColorSettingListener() {

            @Override
            public void colorUpdated() {
                foregroundColor.setBaseColor(backgroundColor.getSettingValue());
                backgroundColor.setBaseColor(foregroundColor.getSettingValue());
            }
        };
        foregroundColor.addListener(colorChangeListener);
        backgroundColor.addListener(colorChangeListener);
        d.addStringSetting("lafForeground", foregroundColor);
        d.addStringSetting("lafBackground", backgroundColor);
        
        ComboStringSetting lafScroll = d.addComboStringSetting("lafScroll", false, "default", "small", "smaller", "tiny");
        ComboLongSetting lafGradient = d.addComboLongSetting("lafGradient", 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50);
        ComboLongSetting lafVariant = d.addComboLongSetting("lafVariant", 0, 1, 2, 3, 4);
        ComboStringSetting lafStyle = d.addComboStringSetting("lafStyle", false,
                "classic", "classicStrong", "regular", "regularStrong", "simple", "sleek", "minimal");
        SimpleBooleanSetting lafNativeWindow = d.addSimpleBooleanSetting("lafNativeWindow");
        
        //--------------------------
        // Font Scale
        //--------------------------
        Map<Long, String> fontScaleOptions = new LinkedHashMap<>();
        fontScaleOptions.put((long)80, "0.8");
        fontScaleOptions.put((long)90, "0.9");
        fontScaleOptions.put((long)100, "Default");
        fontScaleOptions.put((long)110, "1.1");
        fontScaleOptions.put((long)120, "1.2");
        fontScaleOptions.put((long)130, "1.3");
        fontScaleOptions.put((long)140, "1.4");
        fontScaleOptions.put((long)150, "1.5");
        ComboLongSetting fontScale = new ComboLongSetting(fontScaleOptions);
        d.addLongSetting("lafFontScale", fontScale);

        //==========================
        // Layout
        //==========================
        JLabel info = new JLabel(SettingConstants.HTML_PREFIX
                + Language.getString("settings.laf.info"));
        gbc = d.makeGbc(0, 0, 1, 1);
        addPanel(info, gbc);
        
        // Look & Feel
        gbc = d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST);
        lafSettingsPanel.add(new JLabel(Language.getString("settings.laf.lookandfeel")), gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST);
        lafSettingsPanel.add(laf, gbc);
        
        gbc = d.makeGbc(0, 2, 4, 1);
        lafSettingsPanel.add(lafInfo, gbc);
        
        // Font Theme
        gbc = d.makeGbc(2, 1, 1, 1, GridBagConstraints.EAST);
        lafSettingsPanel.add(new JLabel(Language.getString("settings.laf.font")), gbc);
        
        gbc = d.makeGbc(3, 1, 1, 1, GridBagConstraints.WEST);
        lafSettingsPanel.add(theme, gbc);
        
        // Scrollbar
        gbc = d.makeGbc(0, 3, 1, 1, GridBagConstraints.EAST);
        lafSettingsPanel.add(SettingsUtil.createLabel("lafScroll"), gbc);
        
        gbc = d.makeGbc(1, 3, 3, 1, GridBagConstraints.WEST);
        lafSettingsPanel.add(lafScroll, gbc);
        
        // Style
        gbc = d.makeGbc(0, 8, 1, 1, GridBagConstraints.EAST);
        lafSettingsPanel.add(SettingsUtil.createLabel("lafStyle"), gbc);
        
        gbc = d.makeGbc(1, 8, 1, 1, GridBagConstraints.WEST);
        lafSettingsPanel.add(lafStyle, gbc);
        
        gbc = d.makeGbc(2, 8, 2, 1, GridBagConstraints.WEST);
        lafSettingsPanel.add(lafNativeWindow, gbc);
        
        // Colors
        gbc = d.makeGbc(0, 10, 1, 1, GridBagConstraints.EAST);
        lafSettingsPanel.add(new JLabel(Language.getString("settings.laf.colors")), gbc);
        
        gbc = d.makeGbc(1, 10, 3, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        lafSettingsPanel.add(foregroundColor, gbc);
        
        gbc = d.makeGbc(1, 11, 3, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        lafSettingsPanel.add(backgroundColor, gbc);
        
        // Variant
        gbc = d.makeGbc(0, 9, 1, 1, GridBagConstraints.EAST);
        lafSettingsPanel.add(SettingsUtil.createLabel("lafVariant"), gbc);
        
        gbc = d.makeGbc(1, 9, 1, 1, GridBagConstraints.WEST);
        lafSettingsPanel.add(lafVariant, gbc);
        
        // Gradient
        gbc = d.makeGbc(2, 9, 1, 1);
        lafSettingsPanel.add(SettingsUtil.createLabel("lafGradient"), gbc);
        
        gbc = d.makeGbc(3, 9, 1, 1, GridBagConstraints.WEST);
        lafSettingsPanel.add(lafGradient, gbc);

        SettingsUtil.addSubsettings(laf, s -> s.equals("hifiCustom"), foregroundColor, backgroundColor, lafGradient, lafStyle);
        SettingsUtil.addSubsettings(laf, s -> !s.equals("default") && !s.equals("system"), lafNativeWindow);
        
        //--------------------------
        // Font Scale
        //--------------------------
        gbc = d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST);
        fontScalePanel.add(new JLabel("Font Scale:"), gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST);
        fontScalePanel.add(fontScale, gbc);
        
        gbc = d.makeGbc(0, 2, 2, 1, GridBagConstraints.CENTER);
        fontScalePanel.add(new JLabel(SettingConstants.HTML_PREFIX
                + "Some things may not look correct. Restart of Chatty required "
                + "after changing setting."),
                gbc);
        
        //==========================
        // Preview
        //==========================
        gbc = d.makeGbc(0, 1, 1, 1);
        previewPanel.add(new JLabel(SettingConstants.HTML_PREFIX+Language.getString("settings.laf.previewInfo")), gbc);
        
        gbc = d.makeGbc(0, 4, 1, 1);
        previewPanel.add(lafPreviewButton, gbc);
    }
    
    private static void updateInfo(LinkLabel label, StringSetting setting) {
        String text = SettingConstants.HTML_PREFIX;
        String selected = setting.getSettingValue();
        if (selected.equals("default")) {
            text += "The classic cross-platform Java look.";
        }
        else if (selected.equals("system")) {
            text += "This Look&Feel differs depending what OS you are using.";
        }
        else {
            text += "No window snapping (unless you enable using the native window, see [help-laf:native-window help]). ";
            if (selected.equals("hifiCustom")) {
                text += "Allows some basic customization here. ";
            }
            text += "More properties [help-laf:custom can be set] manually.";
        }
        label.setText(text);
    }
    
}
