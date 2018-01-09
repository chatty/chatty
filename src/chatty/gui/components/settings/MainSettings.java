
package chatty.gui.components.settings;

import chatty.gui.LaF;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author tduva
 */
public class MainSettings extends SettingsPanel implements ActionListener {
    
    private final JButton selectFontButton = new JButton("Select font");
    private final FontChooser fontChooser;
    private final ComboLongSetting onStart;
    private final JTextField channels;
    private final SettingsDialog d;
    
    public MainSettings(SettingsDialog d) {
        
        fontChooser = new FontChooser(d);
        this.d = d;
        
        GridBagConstraints gbc;
        
        JPanel fontSettingsPanel = addTitledPanel("Chat Font", 0);
        JPanel inputFontSettingsPanel = addTitledPanel("Input / Userlist Font", 1);
        JPanel startSettingsPanel = addTitledPanel("Startup", 2);
        JPanel lafSettingsPanel = addTitledPanel("Look&Feel", 3);
        JPanel languagePanel = addTitledPanel("Language", 4);
        
        //===========
        // Chat Font
        //===========
        // Font Name
        gbc = d.makeGbc(0,0,1,1);
        fontSettingsPanel.add(new JLabel("Font Name:"),gbc);
        gbc = d.makeGbc(0,1,1,1);
        gbc.anchor = GridBagConstraints.EAST;
        fontSettingsPanel.add(new JLabel("Font Size:"),gbc);
        
        // Font Size
        gbc = d.makeGbc(1,0,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        SimpleStringSetting fontSetting = new SimpleStringSetting(15, false);
        d.addStringSetting("font", fontSetting);
        fontSettingsPanel.add(fontSetting ,gbc);
        gbc = d.makeGbc(1,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        fontSettingsPanel.add(d.addSimpleLongSetting("fontSize",7,false),gbc);
        
        // Select Font button
        selectFontButton.addActionListener(this);
        gbc = d.makeGbc(3,0,1,1);
        fontSettingsPanel.add(selectFontButton,gbc);
        
        gbc = d.makeGbc(2,1,1,1);
        fontSettingsPanel.add(new JLabel("Line Spacing:"), gbc);
        
        Map<Long, String> lineSpacingDef = new HashMap<>();
        lineSpacingDef.put((long)0, "Smallest");
        lineSpacingDef.put((long)1, "Smaller");
        lineSpacingDef.put((long)2, "Small");
        lineSpacingDef.put((long)3, "Normal");
        lineSpacingDef.put((long)4, "Big");
        lineSpacingDef.put((long)5, "Bigger");
        lineSpacingDef.put((long)6, "Biggest");
        ComboLongSetting lineSpacing = new ComboLongSetting(lineSpacingDef);
        d.addLongSetting("lineSpacing", lineSpacing);
        gbc = d.makeGbc(3,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        fontSettingsPanel.add(lineSpacing, gbc);
        
        fontSettingsPanel.add(new JLabel("Message Spacing:"),
                d.makeGbc(1, 2, 2, 1, GridBagConstraints.EAST));
        
        Map<Long, String> paragraphSpacingDef = new LinkedHashMap<>();
        for (int i=0;i<=20;i+=2) {
            paragraphSpacingDef.put((long)i, String.valueOf(i)+" px");
        }
        ComboLongSetting paragraphSpacing = new ComboLongSetting(paragraphSpacingDef);
        d.addLongSetting("paragraphSpacing", paragraphSpacing);
        gbc = d.makeGbc(3, 2, 1, 1, GridBagConstraints.WEST);
        fontSettingsPanel.add(paragraphSpacing, gbc);
        
        //=============
        // Other Fonts
        //=============
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST);
        inputFontSettingsPanel.add(new JLabel("Input:"), gbc);
        
        List<String> inputFonts = new ArrayList<>();
        for (int i=12; i<=32; i++) {
            inputFonts.add("Dialog "+i);
        }
        for (int i=12; i<=32; i++) {
            inputFonts.add("Monospaced "+i);
        }
        for (int i=12; i<=32; i++) {
            inputFonts.add("Dialog Bold "+i);
        }
        for (int i=12; i<=32; i++) {
            inputFonts.add("Monospaced Bold "+i);
        }
        ComboStringSetting inputFont = new ComboStringSetting(inputFonts);
        d.addStringSetting("inputFont", inputFont);
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        inputFontSettingsPanel.add(inputFont, gbc);
        
        gbc = d.makeGbc(2, 0, 1, 1);
        inputFontSettingsPanel.add(new JLabel("Userlist:"), gbc);
        
        ComboStringSetting userlistFont = new ComboStringSetting(inputFonts);
        d.addStringSetting("userlistFont", userlistFont);
        gbc = d.makeGbc(3, 0, 1, 1);
        inputFontSettingsPanel.add(userlistFont, gbc);
        
        //=========
        // Startup
        //=========
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST);
        startSettingsPanel.add(new JLabel("On start:"), gbc);
        
        Map<Long, String> onStartDef = new LinkedHashMap<>();
        onStartDef.put((long)0, "Do nothing");
        onStartDef.put((long)1, "Open connect dialog");
        onStartDef.put((long)2, "Connect and join specified channels");
        onStartDef.put((long)3, "Connect and join previously open channels");
        onStartDef.put((long)4, "Connect and join favorited channels");
        onStart = new ComboLongSetting(onStartDef);
        onStart.addActionListener(this);
        d.addLongSetting("onStart", onStart);
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        startSettingsPanel.add(onStart, gbc);
        
        gbc = d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST);
        startSettingsPanel.add(new JLabel("Channels:"), gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST);
        channels = d.addSimpleStringSetting("autojoinChannel", 25, true);
        startSettingsPanel.add(channels, gbc);
        
        
        //=============
        // Look & Feel
        //=============
        
        Map<String, String> lafDef = new LinkedHashMap<>();
        lafDef.put("default", "Default");
        lafDef.put("system", "System");
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
        
        Map<String, String> themeDef = new LinkedHashMap<>();
        themeDef.put("Default", "Default");
        themeDef.put("Small-Font", "Small Font");
        themeDef.put("Large-Font", "Large Font");
        themeDef.put("Giant-Font", "Giant Font");
        ComboStringSetting theme = new ComboStringSetting(themeDef);
        d.addStringSetting("lafTheme", theme);
        
        laf.addActionListener(e -> {
            String selected = laf.getSettingValue();
            theme.setEnabled(!selected.equals("default") && !selected.equals("system"));
        });
        
        JButton lafPreviewButton = new JButton("Preview");
        lafPreviewButton.addActionListener(e -> {
            LaF.setLookAndFeel(laf.getSettingValue(), theme.getSettingValue());
            LaF.updateLookAndFeel();
        });
        
        gbc = d.makeGbc(0, 0, 1, 1);
        lafSettingsPanel.add(new JLabel("Look&Feel:"), gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        lafSettingsPanel.add(laf, gbc);
        
        gbc = d.makeGbc(2, 0, 1, 1);
        lafSettingsPanel.add(new JLabel("Font:"), gbc);
        
        gbc = d.makeGbc(3, 0, 1, 1);
        lafSettingsPanel.add(theme, gbc);
        
        gbc = d.makeGbc(4, 0, 1, 1);
        lafSettingsPanel.add(lafPreviewButton, gbc);
        
        gbc = d.makeGbc(0, 1, 5, 1);
        gbc.insets = new Insets(0, 5, 7, 5);
        lafSettingsPanel.add(new JLabel("(Restart of Chatty required for all changes to take effect.)"), gbc);
        
        gbc = d.makeGbc(0, 2, 5, 1);
        lafSettingsPanel.add(new JLabel("<html><body><em>Note:</em> "
                + "Chat Colors can be changed on the 'Colors' settings page."), gbc);
        
        //==========
        // Language
        //==========
        
        Map<String, String> languageOptions = new LinkedHashMap<>();
        languageOptions.put("", "Default");
        languageOptions.put("en", "English");
        languageOptions.put("de", "German / Deutsch");
        
        languagePanel.add(new JLabel("Language:"),
                d.makeGbc(0, 0, 1, 1));
        ComboStringSetting languageSetting = d.addComboStringSetting(
                "language", 0, false, languageOptions);
        languagePanel.add(languageSetting,
                d.makeGbc(1, 0, 1, 1));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == selectFontButton) {
            String font = d.getStringSetting("font");
            int fontSize = d.getLongSetting("fontSize").intValue();
            int result = fontChooser.showDialog(font, fontSize);
            if (result == FontChooser.ACTION_OK) {
                d.setStringSetting("font", fontChooser.getFontName());
                d.setLongSetting("fontSize", fontChooser.getFontSize().longValue());
            }
        } else if (e.getSource() == onStart) {
            boolean channelsEnabled = onStart.getSettingValue().equals(Long.valueOf(2));
            channels.setEnabled(channelsEnabled);
        }
    }
    
}
