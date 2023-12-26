
package chatty.gui.components.settings;

import chatty.gui.laf.LaF;
import chatty.gui.laf.LaF.LaFSettings;
import chatty.gui.components.LinkLabel;
import chatty.gui.laf.FlatLafUtil;
import chatty.gui.laf.LaFChanger;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 *
 * @author tduva
 */
public class LookSettings extends SettingsPanel {

    protected LookSettings(SettingsDialog d) {

        JPanel lafSettingsPanel = addTitledPanel(Language.getString("settings.section.lookandfeel"), 1);
        JPanel optionsPanel = addTitledPanel("Additional Options", 2);
        JPanel previewPanel = addTitledPanel(Language.getString("settings.section.preview"), 3);
        
        GridBagConstraints gbc;
        
        //=============
        // Look & Feel
        //=============
        
        Map<String, String> lafDef = new LinkedHashMap<>();
        lafDef.put("default", "Default");
        lafDef.put("system", "System");
        lafDef.put("flatdark", "Flat Dark");
        lafDef.put("flatlight", "Flat Light");
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
        
        JButton lafPreviewButton = new JButton("Preview");
        lafPreviewButton.addActionListener(e -> {
            LaFChanger.changeLookAndFeel(LaFSettings.fromSettingsDialog(d, d.settings), this);
            d.lafPreviewed = true;
            d.pack();
        });
        
        //==========================
        // LaF Settings
        //==========================
        ColorSetting foregroundColor = new ColorSetting(ColorSetting.FOREGROUND,
                "lafBackground",
                Language.getString("settings.general.foreground"),
                Language.getString("settings.general.foreground"),
                () -> new ColorChooser(d));
        ColorSetting backgroundColor = new ColorSetting(ColorSetting.BACKGROUND,
                "lafForeground",
                Language.getString("settings.general.background"),
                Language.getString("settings.general.background"),
                () -> new ColorChooser(d));
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
        
        JPanel generalOptions = new JPanel(new GridBagLayout());
        JPanel flatOptions = new JPanel(new GridBagLayout());
        JPanel hifiCustomOptions = new JPanel(new GridBagLayout());
        JPanel macOptions = new JPanel(new GridBagLayout());
        
        //--------------------------
        // General Options
        //--------------------------
        // Font Theme
        gbc = d.makeGbc(0, 3, 1, 1, GridBagConstraints.EAST);
        generalOptions.add(new JLabel(Language.getString("settings.laf.font")), gbc);
        
        gbc = d.makeGbc(1, 3, 1, 1, GridBagConstraints.WEST);
        generalOptions.add(theme, gbc);
        
        // Font Scale
        SettingsUtil.addLabeledComponent(generalOptions, "lafFontScale", 0, 1, 1, GridBagConstraints.WEST, fontScale);
        
        // Native window
        gbc = d.makeGbc(0, 8, 2, 1, GridBagConstraints.WEST);
        generalOptions.add(lafNativeWindow, gbc);
        
        // Scrollbar
        SettingsUtil.addLabeledComponent(generalOptions, "lafScroll", 0, 0, 1, GridBagConstraints.WEST, lafScroll);
        
        SettingsUtil.addSubsettings(laf, s -> !s.equals("default") && !s.equals("system") && !s.startsWith("flat"), theme, lafNativeWindow);
        
        generalOptions.add(d.addSimpleBooleanSetting("lafErrorSound"),
                d.makeGbc(0, 9, 2, 1, GridBagConstraints.WEST));
        
        //--------------------------
        // Flat Options
        //--------------------------
        JCheckBox styledWindow = d.addSimpleBooleanSetting("lafFlatStyledWindow");
        SettingsUtil.addStandardSetting(flatOptions, "lafFlatStyledWindow", 0, styledWindow);
        JCheckBox embeddedMenu = d.addSimpleBooleanSetting("lafFlatEmbeddedMenu");
        SettingsUtil.addStandardSubSetting(flatOptions, "lafFlatEmbeddedMenu", 1, embeddedMenu);
        EditorStringSetting flatProperties = d.addEditorStringSetting("lafFlatProperties", 10, true, "Custom Flat Look&Feel properties:", true,
                SettingConstants.HTML_PREFIX+SettingsUtil.getInfo("info-flatProperties.html", null));
        flatProperties.setLinkLabelListener(d.getLinkLabelListener());
        SettingsUtil.addStandardSetting(flatOptions, "lafFlatProperties", 3, flatProperties);
        FlatTabOptions selectedTabOptions = new FlatTabOptions("lafFlatTabs", d);
        flatOptions.add(selectedTabOptions, d.makeGbc(0, 2, 3, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(styledWindow, embeddedMenu);
        SettingsUtil.addSubsettings(laf, s -> s.startsWith("flat"), styledWindow);
        
        //--------------------------
        // HiFi Custom Options
        //--------------------------
        SettingsUtil.addStandardSetting(hifiCustomOptions, "lafStyle", 0, lafStyle);
        SettingsUtil.addStandardSetting(hifiCustomOptions, "lafVariant", 1, lafVariant);
        SettingsUtil.addStandardSetting(hifiCustomOptions, "lafGradient", 2, lafGradient);
        
        // Colors
        gbc = d.makeGbc(0, 10, 1, 1, GridBagConstraints.EAST);
        hifiCustomOptions.add(new JLabel(Language.getString("settings.laf.colors")), gbc);
        
        gbc = d.makeGbc(1, 10, 3, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        hifiCustomOptions.add(foregroundColor, gbc);
        
        gbc = d.makeGbc(1, 11, 3, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        hifiCustomOptions.add(backgroundColor, gbc);
        
        SettingsUtil.addSubsettings(laf, s -> s.equals("hifiCustom"), foregroundColor, backgroundColor, lafGradient, lafStyle, lafVariant);
        
        //--------------------------
        // Mac Options
        //--------------------------
        SettingsUtil.addStandardSetting(macOptions, "macScreenMenuBar", 0,
                d.addSimpleBooleanSetting("macScreenMenuBar"));
        SettingsUtil.addStandardSetting(macOptions, "macSystemAppearance", 1,
                d.addSimpleBooleanSetting("macSystemAppearance"));
        
        //--------------------------
        // Add tabs
        //--------------------------
        JTabbedPane optionsTabs = new JTabbedPane();
        optionsTabs.addTab("General", SettingsUtil.topAlign(generalOptions, 20));
        optionsTabs.addTab("Flat", SettingsUtil.topAlign(flatOptions, 20));
        optionsTabs.addTab("HiFi Custom", SettingsUtil.topAlign(hifiCustomOptions, 20));
        optionsTabs.addTab("MacOS", SettingsUtil.topAlign(macOptions, 20));
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        optionsPanel.add(optionsTabs, gbc);
        
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
        else if (selected.startsWith("flat")) {
            text += "Minimalistic. Customize further below.";
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
    
    private static class FlatTabOptions extends JPanel implements LongSetting {
        
        private final Map<Integer, JCheckBox> options = new HashMap<>();
        
        FlatTabOptions(String settingName, SettingsDialog settings) {
            settings.addLongSetting(settingName, this);
            setLayout(new GridBagLayout());
            add(makeOption(FlatLafUtil.TAB_SELECTED_BACKGROUND, "selectedBackground"),
                    SettingsDialog.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
            add(makeOption(FlatLafUtil.TAB_SEP, "separators"),
                    SettingsDialog.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
            add(makeOption(FlatLafUtil.TAB_SEP_FULL, "separatorsFull"),
                    SettingsDialog.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));
            update();
        }
        
        private JCheckBox makeOption(int option, String labelKey) {
            String text = Language.getString("settings.tabs.flat."+labelKey);
            String tip = Language.getString("settings.tabs.flat."+labelKey + ".tip", false);
            JCheckBox check = new JCheckBox(text);
            check.setToolTipText(SettingsUtil.addTooltipLinebreaks(tip));
            check.addItemListener(e -> update());
            options.put(option, check);
            return check;
        }
        
        private void update() {
        }

        @Override
        public Long getSettingValue() {
            long result = 0;
            for (Map.Entry<Integer, JCheckBox> entry : options.entrySet()) {
                if (entry.getValue().isSelected()) {
                    result = result | entry.getKey();
                }
            }
            return result;
        }

        @Override
        public Long getSettingValue(Long def) {
            return getSettingValue();
        }

        @Override
        public void setSettingValue(Long setting) {
            for (Map.Entry<Integer, JCheckBox> entry : options.entrySet()) {
                entry.getValue().setSelected((setting & entry.getKey()) != 0);
            }
        }

    }
    
}
