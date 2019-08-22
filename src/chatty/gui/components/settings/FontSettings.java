
package chatty.gui.components.settings;

import static chatty.gui.components.settings.SettingConstants.HTML_PREFIX;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class FontSettings extends SettingsPanel {
    
    public FontSettings(SettingsDialog d) {
        
        JPanel chatFontsPanel = addTitledPanel(Language.getString("settings.section.chatFonts"), 0);
        JPanel chatMarginsPanel = addTitledPanel(Language.getString("settings.section.chatSpacings"), 1);
        JPanel notePanel = (JPanel)addPanel(new JPanel(new GridBagLayout()), getGbc(2));
        
        GridBagConstraints gbc;
        
        //============
        // Chat Fonts
        //============
        
        //---------------
        // Messages Font
        //---------------
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST);
        chatFontsPanel.add(new JLabel(Language.getString("settings.chatFont.chatFont")), gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        FontSetting chatFont = new FontSetting(d,
                FontSetting.SettingType.STRING_INT,
                FontSetting.NO_STYLE_SELECTION);
        d.addStringSetting("font", chatFont.getFontSetting());
        d.addLongSetting("fontSize", chatFont.getFontSizeSetting());
        chatFontsPanel.add(chatFont, gbc);
        
        //------------
        // Input Font
        //------------
        gbc = d.makeGbc(0, 20, 1, 1, GridBagConstraints.EAST);
        chatFontsPanel.add(new JLabel(Language.getString("settings.otherFonts.inputFont")), gbc);

        FontSetting inputFont = new FontSetting(d,
                FontSetting.SettingType.STRING,
                FontSetting.RESTRICTED_FONTS);
        d.addStringSetting("inputFont", inputFont.getFontSetting());
        gbc = d.makeGbc(1, 20, 1, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        chatFontsPanel.add(inputFont, gbc);
        
        //---------------
        // Userlist Font
        //---------------
        gbc = d.makeGbc(0, 21, 1, 1, GridBagConstraints.EAST);
        chatFontsPanel.add(new JLabel(Language.getString("settings.otherFonts.userlistFont")), gbc);
        
        FontSetting userlistFont = new FontSetting(d,
                FontSetting.SettingType.STRING,
                FontSetting.RESTRICTED_FONTS);
        d.addStringSetting("userlistFont", userlistFont.getFontSetting());
        gbc = d.makeGbc(1, 21, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        chatFontsPanel.add(userlistFont, gbc);
        
        //----------------
        // Timestamp Font
        //----------------
        // Boolean
        SimpleBooleanSetting customTimestampFont = d.addSimpleBooleanSetting("timestampFontEnabled");
        gbc = d.makeGbc(0, 30, 2, 1, GridBagConstraints.WEST);
        chatFontsPanel.add(customTimestampFont, gbc);
        
        // Font
        gbc = d.makeGbcSub(0, 31, 1, 1, GridBagConstraints.EAST);
        chatFontsPanel.add(new JLabel(Language.getString("settings.chatFont.timestampFont")), gbc);
        
        gbc = d.makeGbc(1, 31, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        FontSetting timestampFont = new FontSetting(d,
                FontSetting.SettingType.STRING);
        d.addStringSetting("timestampFont", timestampFont.getFontSetting());
        chatFontsPanel.add(timestampFont, gbc);
        
        gbc = d.makeGbc(0, 32, 2, 1);
        chatFontsPanel.add(new JLabel(SettingConstants.HTML_PREFIX
                +Language.getString("settings.chatFont.timestampFont.info")), gbc);
        
        SettingsUtil.addSubsettings(customTimestampFont, timestampFont);
        
        
        //===============
        // Chat Spacings
        //===============
        
        //--------------
        // Line Spacing
        //--------------
        gbc = d.makeGbc(0, 3, 1, 1, GridBagConstraints.EAST);
        chatMarginsPanel.add(new JLabel(Language.getString("settings.chatFont.lineSpacing")), gbc);
        
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
        gbc = d.makeGbc(1, 3, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        chatMarginsPanel.add(lineSpacing, gbc);
        
        //-----------------
        // Message Spacing
        //-----------------
        chatMarginsPanel.add(new JLabel(Language.getString("settings.chatFont.messageSpacing")),
                d.makeGbc(0, 4, 1, 1, GridBagConstraints.EAST));
        
        Map<Long, String> paragraphSpacingDef = new LinkedHashMap<>();
        for (int i=0;i<=20;i+=2) {
            paragraphSpacingDef.put((long)i, String.valueOf(i)+" px");
        }
        ComboLongSetting paragraphSpacing = new ComboLongSetting(paragraphSpacingDef);
        d.addLongSetting("paragraphSpacing", paragraphSpacing);
        gbc = d.makeGbc(1, 4, 1, 1, GridBagConstraints.WEST);
        chatMarginsPanel.add(paragraphSpacing, gbc);
        
        //---------------
        // Bottom Margin
        //---------------
        chatMarginsPanel.add(new JLabel(Language.getString("settings.chatFont.bottomMargin")),
            d.makeGbc(0, 5, 1, 1, GridBagConstraints.EAST));
        
        Map<Long, String> bottomMarginDef = new LinkedHashMap<>();
        bottomMarginDef.put((long)-1, "Auto");
        for (int i=0;i<=20;i+=1) {
            bottomMarginDef.put((long)i, String.valueOf(i)+" px");
        }
        ComboLongSetting bottomMargin = new ComboLongSetting(bottomMarginDef);
        d.addLongSetting("bottomMargin", bottomMargin);
        gbc = d.makeGbc(1, 5, 1, 1, GridBagConstraints.WEST);
        chatMarginsPanel.add(bottomMargin, gbc);
        
        //============
        // Note Panel
        //============
        gbc = d.makeGbc(0, 0, 1, 1);
        notePanel.add(new JLabel(HTML_PREFIX+Language.getString("settings.otherFonts.info")), gbc);
    }
    
}
