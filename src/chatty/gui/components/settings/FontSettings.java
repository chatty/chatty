
package chatty.gui.components.settings;

import static chatty.gui.components.settings.SettingConstants.HTML_PREFIX;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class FontSettings extends SettingsPanel {
    
    public FontSettings(SettingsDialog d) {
        
        JPanel fontSettingsPanel = addTitledPanel(Language.getString("settings.section.chatFont"), 0);
        JPanel inputFontSettingsPanel = addTitledPanel(Language.getString("settings.section.otherFonts"), 1);
        JPanel notePanel = (JPanel)addPanel(new JPanel(new GridBagLayout()), getGbc(2));
        
        GridBagConstraints gbc;
        
        //===========
        // Chat Font
        //===========
        
        //---------------
        // Font Settings
        //---------------
        fontSettingsPanel.add(new JLabel(Language.getString("settings.chatFont.fontName")),
                d.makeGbc(0, 0, 1, 1,  GridBagConstraints.EAST));
        fontSettingsPanel.add(new JLabel(Language.getString("settings.chatFont.fontSize")),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        
        gbc = d.makeGbc(1,0,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        SimpleStringSetting fontSetting = new SimpleStringSetting(15, false);
        d.addStringSetting("font", fontSetting);
        fontSettingsPanel.add(fontSetting, gbc);
        
        gbc = d.makeGbc(1,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        fontSettingsPanel.add(d.addSimpleLongSetting("fontSize",7,false), gbc);
        
        //--------------------
        // Select Font Button
        //--------------------
        JButton selectFontButton = new JButton(Language.getString("settings.chatFont.button.selectFont"));
        FontChooser fontChooser = new FontChooser(d);
        selectFontButton.addActionListener(e -> {
            String font = d.getStringSetting("font");
            int fontSize = d.getLongSetting("fontSize").intValue();
            int result = fontChooser.showDialog(font, fontSize);
            if (result == FontChooser.ACTION_OK) {
                d.setStringSetting("font", fontChooser.getFontName());
                d.setLongSetting("fontSize", fontChooser.getFontSize().longValue());
            }
        });
        gbc = d.makeGbc(3,0,1,1);
        fontSettingsPanel.add(selectFontButton, gbc);
        
        //--------------
        // Line Spacing
        //--------------
        gbc = d.makeGbc(2,1,1,1);
        fontSettingsPanel.add(new JLabel(Language.getString("settings.chatFont.lineSpacing")), gbc);
        
        Map<Long, String> lineSpacingDef = new HashMap<>();
        lineSpacingDef.put((long)0, Language.getString("settings.chatFont.option.smallest"));
        lineSpacingDef.put((long)1, Language.getString("settings.chatFont.option.smaller"));
        lineSpacingDef.put((long)2, Language.getString("settings.chatFont.option.small"));
        lineSpacingDef.put((long)3, Language.getString("settings.chatFont.option.normal"));
        lineSpacingDef.put((long)4, Language.getString("settings.chatFont.option.big"));
        lineSpacingDef.put((long)5, Language.getString("settings.chatFont.option.bigger"));
        lineSpacingDef.put((long)6, Language.getString("settings.chatFont.option.biggest"));
        ComboLongSetting lineSpacing = new ComboLongSetting(lineSpacingDef);
        d.addLongSetting("lineSpacing", lineSpacing);
        gbc = d.makeGbc(3,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        fontSettingsPanel.add(lineSpacing, gbc);
        
        //-----------------
        // Message Spacing
        //-----------------
        fontSettingsPanel.add(new JLabel(Language.getString("settings.chatFont.messageSpacing")),
                d.makeGbc(1, 2, 2, 1, GridBagConstraints.EAST));
        
        Map<Long, String> paragraphSpacingDef = new LinkedHashMap<>();
        for (int i=0;i<=20;i+=2) {
            paragraphSpacingDef.put((long)i, String.valueOf(i)+" px");
        }
        ComboLongSetting paragraphSpacing = new ComboLongSetting(paragraphSpacingDef);
        d.addLongSetting("paragraphSpacing", paragraphSpacing);
        gbc = d.makeGbc(3, 2, 1, 1, GridBagConstraints.WEST);
        fontSettingsPanel.add(paragraphSpacing, gbc);
        
        //---------------
        // Bottom Margin
        //---------------
        fontSettingsPanel.add(new JLabel(Language.getString("settings.chatFont.bottomMargin")),
            d.makeGbc(1, 3, 2, 1, GridBagConstraints.EAST));
        
        Map<Long, String> bottomMarginDef = new LinkedHashMap<>();
        bottomMarginDef.put((long)-1, "Auto");
        for (int i=0;i<=20;i+=1) {
            bottomMarginDef.put((long)i, String.valueOf(i)+" px");
        }
        ComboLongSetting bottomMargin = new ComboLongSetting(bottomMarginDef);
        d.addLongSetting("bottomMargin", bottomMargin);
        gbc = d.makeGbc(3, 3, 1, 1, GridBagConstraints.WEST);
        fontSettingsPanel.add(bottomMargin, gbc);
        
        //=============
        // Other Fonts
        //=============
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST);
        inputFontSettingsPanel.add(new JLabel(Language.getString("settings.otherFonts.inputFont")), gbc);
        
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
        
        gbc = d.makeGbc(0, 1, 1, 1);
        inputFontSettingsPanel.add(new JLabel(Language.getString("settings.otherFonts.userlistFont")), gbc);
        
        ComboStringSetting userlistFont = new ComboStringSetting(inputFonts);
        d.addStringSetting("userlistFont", userlistFont);
        gbc = d.makeGbc(1, 1, 1, 1);
        inputFontSettingsPanel.add(userlistFont, gbc);
        
        //============
        // Note Panel
        //============
        gbc = d.makeGbc(0, 0, 1, 1);
        notePanel.add(new JLabel(HTML_PREFIX+Language.getString("settings.otherFonts.info")), gbc);
    }
    
}
