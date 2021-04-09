
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import chatty.util.MiscUtil;
import chatty.util.StringUtil;
import chatty.util.settings.FileManager;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.EAST;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 *
 * @author tduva
 */
public class MainSettings extends SettingsPanel {

    public MainSettings(SettingsDialog d) {

        JPanel startSettingsPanel = addTitledPanel(Language.getString("settings.section.startup"), 0);
        JPanel languagePanel = addTitledPanel(Language.getString("settings.section.language"), 1);
        JPanel dirPanel = addTitledPanel(Language.getString("settings.section.settings"), 2);
        
        GridBagConstraints gbc;
        
        //=========
        // Startup
        //=========
        gbc = d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST);
        startSettingsPanel.add(d.addSimpleBooleanSetting("splash"), gbc);
        
        Map<Long, String> onStartDef = new LinkedHashMap<>();
        onStartDef.put((long)0, Language.getString("settings.startup.option.doNothing"));
        onStartDef.put((long)1, Language.getString("settings.startup.option.openConnect"));
        onStartDef.put((long)2, Language.getString("settings.startup.option.connectJoinSpecified"));
        onStartDef.put((long)3, Language.getString("settings.startup.option.connectJoinPrevious"));
        onStartDef.put((long)4, Language.getString("settings.startup.option.connectJoinFavorites"));
        ComboLongSetting onStart = new ComboLongSetting(onStartDef);
        d.addLongSetting("onStart", onStart);
        SettingsUtil.addLabeledComponent(startSettingsPanel, "settings.startup.onStart", 0, 1, 1, EAST, onStart);
        
        JTextField channels = d.addSimpleStringSetting("autojoinChannel", 25, true);
        GuiUtil.installLengthLimitDocumentFilter(channels, 8000, false);
        SettingsUtil.addLabeledComponent(startSettingsPanel, "settings.startup.channels", 0, 2, 1, EAST, channels);
        
        onStart.addActionListener(e -> {
            boolean channelsEnabled = onStart.getSettingValue().equals(Long.valueOf(2));
            channels.setEnabled(channelsEnabled);
        });
        
        gbc = d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST);
        startSettingsPanel.add(d.addSimpleBooleanSetting("restoreLayout"), gbc);
        
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
        
        JLabel timezoneLabel = SettingsUtil.createLabel("timezone");
        languagePanel.add(timezoneLabel,
                d.makeGbc(0, 2, 1, 1));
        TimezoneSetting timezoneSetting = new TimezoneSetting(d);
        timezoneLabel.setLabelFor(timezoneSetting);
        d.addStringSetting("timezone", timezoneSetting);
        gbc = d.makeGbc(1, 2, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        languagePanel.add(timezoneSetting,
                gbc);
        
        //==========================
        // Settings
        //==========================
        String dirInfo = Language.getString("settings.directory.default");
        if (Chatty.getSettingsDirectoryInfo() != null) {
            dirInfo = Language.getString("settings.directory.argument", Chatty.getSettingsDirectoryInfo());
        }
        
        JLabel dirLabel = new JLabel(Language.getString("settings.directory.info", dirInfo));
        dirPanel.add(dirLabel,
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        
        JTextField dir = new JTextField(Chatty.getUserDataDirectory(), 30);
        dirLabel.setLabelFor(dir);
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

        JButton openBackupButton = new JButton(Language.getString("settings.backup.button.open"));
        openBackupButton.addActionListener(e -> {
            FileManager fm = d.settings.getFileManager();
            BackupManager mg = new BackupManager(d, fm);
            mg.setModal(true);
            mg.pack();
            mg.setLocationRelativeTo(d);
            /**
             * Pause saving because session backup shouldn't be changed while
             * viewing backups
             */
            fm.setSavingPaused(true);
            mg.open();
            fm.setSavingPaused(false);
        });
        gbc = d.makeGbc(0, 4, 2, 1);
        dirPanel.add(openBackupButton, gbc);
    }
    
    public static Map<String, String> getLanguageOptions() {
        Map<String, String> languageOptions = new LinkedHashMap<>();
        languageOptions.put("", Language.getString("settings.language.option.defaultLanguage"));
        languageOptions.put("zh_TW", "Chinese (traditional)");
        languageOptions.put("cs", "Czech / Čeština");
        languageOptions.put("nl", "Dutch / Nederlands");
        languageOptions.put("en_US", "English (US)");
        languageOptions.put("en_GB", "English (UK)");
        languageOptions.put("fr", "French / Français");
        languageOptions.put("de", "German / Deutsch");
        languageOptions.put("in", "Indonesian");
        languageOptions.put("it", "Italian / Italiano");
        languageOptions.put("ja", "Japanese / 日本語");
        languageOptions.put("ko", "Korean / 한국어");
        languageOptions.put("pl", "Polish / Polski");
        languageOptions.put("pt_BR", "Portuguese (BR)");
        languageOptions.put("ru", "Russian / Русский");
        languageOptions.put("es", "Spanish / Español");
        languageOptions.put("tr", "Turkish / Türk");
        return languageOptions;
    }
    
    /**
     * Unchanged default, should be set before it is being changed.
     */
    public static TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();
    
    private static class TimezoneSetting extends JPanel implements StringSetting {
        
        private final JTextField display;
        
        private String value;
        
        TimezoneSetting(Window parent) {
            setLayout(new GridBagLayout());
            display = new JTextField(20);
            display.setEditable(false);
            
            JButton changeButton = new JButton(Language.getString("dialog.button.change"));
            changeButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            changeButton.addActionListener(e -> {
                change(parent);
            });
            
            GridBagConstraints gbc = GuiUtil.makeGbc(0, 0, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(display, gbc);
            gbc = GuiUtil.makeGbc(1, 0, 1, 1);
            add(changeButton, gbc);
        }
        
        private void change(Window parent) {
            JDialog dialog = new JDialog(parent);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setModal(true);
            dialog.setLayout(new GridBagLayout());
            dialog.setResizable(false);
            Map<String, String> options = new LinkedHashMap<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date now = new Date();
            List<TimeZone> timezones = new ArrayList<>();
            for (String id : TimeZone.getAvailableIDs()) {
                TimeZone tz = TimeZone.getTimeZone(id);
                timezones.add(tz);
            }
            Collections.sort(timezones, new Comparator<TimeZone>() {

                @Override
                public int compare(TimeZone o1, TimeZone o2) {
                    return o1.getOffset(System.currentTimeMillis()) - o2.getOffset(System.currentTimeMillis());
                }
            });
            options.put("", String.format("%s [%s]",
                     format(DEFAULT_TIMEZONE, sdf, now),
                     Language.getString("status.default")));
            for (TimeZone tz : timezones) {
                options.put(tz.getID(), format(tz, sdf, now));
            }
            ComboStringSetting list = new ComboStringSetting(options);
            list.setSettingValue(value);
            
            JButton save = new JButton(Language.getString("dialog.button.save"));
            save.addActionListener(e -> {
                setSettingValue(list.getSettingValue());
                dialog.setVisible(false);
            });
            
            JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
            cancel.addActionListener(e -> {
                dialog.setVisible(false);
            });
            
            dialog.add(list, GuiUtil.makeGbc(0, 0, 2, 1));
            dialog.add(save, GuiUtil.makeGbc(0, 1, 1, 1));
            dialog.add(cancel, GuiUtil.makeGbc(1, 1, 1, 1));
            
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
        
        private String format(TimeZone tz, SimpleDateFormat sdf, Date date) {
            sdf.setTimeZone(tz);
            return String.format("[%s] %s (%s)",
                        sdf.format(date),
                        tz.getID(),
                        tz.getDisplayName(false, TimeZone.SHORT));
        }

        @Override
        public String getSettingValue() {
            return value;
        }

        @Override
        public void setSettingValue(String value) {
            this.value = value;
            TimeZone tz = DEFAULT_TIMEZONE;
            String def = " ["+Language.getString("status.default")+"]";
            if (!StringUtil.isNullOrEmpty(value)) {
                tz = TimeZone.getTimeZone(value);
                def = "";
            }
            display.setText(String.format("%s (%s)%s",
                        tz.getID(),
                        tz.getDisplayName(false, TimeZone.SHORT),
                        def));
        }
        
    }
  
}
