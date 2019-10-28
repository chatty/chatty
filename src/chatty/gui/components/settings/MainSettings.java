
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import chatty.util.MiscUtil;
import java.awt.GridBagConstraints;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author tduva
 */
public class MainSettings extends SettingsPanel {

    public MainSettings(SettingsDialog d) {

        JPanel startSettingsPanel = addTitledPanel(Language.getString("settings.section.startup"), 0);
        JPanel languagePanel = addTitledPanel(Language.getString("settings.section.language"), 1);
        JPanel dirPanel = addTitledPanel(Language.getString("settings.section.directory"), 2);
        
        GridBagConstraints gbc;
        
        //=========
        // Startup
        //=========
        gbc = d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST);
        startSettingsPanel.add(d.addSimpleBooleanSetting("splash"), gbc);
        
        gbc = d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST);
        startSettingsPanel.add(new JLabel(Language.getString("settings.startup.onStart")), gbc);
        
        Map<Long, String> onStartDef = new LinkedHashMap<>();
        onStartDef.put((long)0, Language.getString("settings.startup.option.doNothing"));
        onStartDef.put((long)1, Language.getString("settings.startup.option.openConnect"));
        onStartDef.put((long)2, Language.getString("settings.startup.option.connectJoinSpecified"));
        onStartDef.put((long)3, Language.getString("settings.startup.option.connectJoinPrevious"));
        onStartDef.put((long)4, Language.getString("settings.startup.option.connectJoinFavorites"));
        ComboLongSetting onStart = new ComboLongSetting(onStartDef);
        d.addLongSetting("onStart", onStart);
        gbc = d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST);
        startSettingsPanel.add(onStart, gbc);
        
        gbc = d.makeGbc(0, 2, 1, 1, GridBagConstraints.EAST);
        startSettingsPanel.add(new JLabel(Language.getString("settings.startup.channels")), gbc);
        
        gbc = d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST);
        JTextField channels = d.addSimpleStringSetting("autojoinChannel", 25, true);
        GuiUtil.installLengthLimitDocumentFilter(channels, 8000, false);
        startSettingsPanel.add(channels, gbc);
        
        onStart.addActionListener(e -> {
            boolean channelsEnabled = onStart.getSettingValue().equals(Long.valueOf(2));
            channels.setEnabled(channelsEnabled);
        });
        
        //==========
        // Language
        //==========
        languagePanel.add(new JLabel(Language.getString("settings.language.language")),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST));
        ComboStringSetting languageSetting = d.addComboStringSetting(
                "language", 0, false, getLanguageOptions());
        languagePanel.add(languageSetting,
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        languagePanel.add(new LinkLabel(SettingConstants.HTML_PREFIX
                +Language.getString("settings.language.info")
                + "<br /><br />"
                + "If you would like to help with translations, check "
                + "[url:https://chatty.github.io/localization.html the website].",
                d.getLinkLabelListener()),
                d.makeGbc(0, 1, 2, 1));
        
        //==========================
        // Directory
        //==========================
        String dirInfo = Language.getString("settings.directory.default");
        if (Chatty.getSettingsDirectoryInfo() != null) {
            dirInfo = Language.getString("settings.directory.argument", Chatty.getSettingsDirectoryInfo());
        }
        
        dirPanel.add(new JLabel(Language.getString("settings.directory.info", dirInfo)),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        
        JTextField dir = new JTextField(Chatty.getUserDataDirectory(), 30);
        dir.setEditable(false);
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        dirPanel.add(dir, gbc);
        
        JButton openDirButton = new JButton(Language.getString("settings.chooseFolder.button.open"));
        openDirButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        openDirButton.addActionListener(e -> {
            MiscUtil.openFolder(new File(Chatty.getUserDataDirectory()), this);
        });
        dirPanel.add(openDirButton, d.makeGbc(1, 1, 1, 1));
        
        if (Chatty.getInvalidSettingsDirectory() != null) {
            dirPanel.add(new JLabel(Language.getString("settings.directory.invalid")),
                    d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
            
            JTextField invalidDir = new JTextField(Chatty.getInvalidSettingsDirectory(), 30);
            invalidDir.setEditable(false);
            gbc = d.makeGbc(0, 3, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dirPanel.add(invalidDir, gbc);
        }
    }
    
    public static Map<String, String> getLanguageOptions() {
        Map<String, String> languageOptions = new LinkedHashMap<>();
        languageOptions.put("", Language.getString("settings.language.option.defaultLanguage"));
        languageOptions.put("zh_TW", "Chinese (traditional)");
        languageOptions.put("nl", "Dutch / Nederlands");
        languageOptions.put("en_US", "English (US)");
        languageOptions.put("en_GB", "English (UK)");
        languageOptions.put("fr", "French / Français");
        languageOptions.put("de", "German / Deutsch");
        languageOptions.put("it", "Italian / Italiano");
        languageOptions.put("ja", "Japanese / 日本語");
        languageOptions.put("ko", "Korean / 한국어");
        languageOptions.put("pl", "Polish / Polski");
        languageOptions.put("ru", "Russian / Русский");
        languageOptions.put("es", "Spanish / Español");
        languageOptions.put("tr", "Turkish / Türk");
        return languageOptions;
    }
  
}
