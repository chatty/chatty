
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.lang.Language;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Shows the currently selected font name/size and provides a button to change
 * it using a FontChooser.
 * 
 * Supports two kind of settings:
 * - Font family and font size (STRING_INT)
 * - Font family, style, size in one (STRING)
 * 
 * The type has to be specified at creation and a StringSetting and LongSetting
 * can be retrieved via methods in order to add them to be loaded/saved.
 * 
 * @author tduva
 */
public class FontSetting extends JPanel {
    
    public enum SettingType {
        STRING, STRING_INT
    }
    
    public static final int NO_OPTION = 0;
    public static final int RESTRICTED_FONTS = 1;
    public static final int NO_STYLE_SELECTION = 2;
    
    private final JTextField fontField;
    private final JTextField fontSizeField;
    private final JButton editButton;
    private final StringSetting fontSetting;
    private final LongSetting fontSizeSetting;
    private final SettingType settingType;
    private FontChooser fontChooser;
    
    private String fontSettingValue;
    private int fontSizeSettingValue;
    
    private Font current = Font.decode(Font.DIALOG);
    
    public FontSetting(Dialog parent, SettingType settingType) {
        this(parent, settingType, NO_OPTION);
    }
    
    public FontSetting(Dialog parent, SettingType settingType,
            int options) {
        boolean optionRestrictedFonts = (options & RESTRICTED_FONTS) != 0;
        boolean optionNoStyle = (options & NO_STYLE_SELECTION) != 0;
        
        this.settingType = settingType;
        fontField = new JTextField(14);
        fontField.setEditable(false);
        fontSizeField = new JTextField(3);
        fontSizeField.setEditable(false);
        editButton = new JButton(Language.getString("dialog.button.change"));
        GuiUtil.smallButtonInsets(editButton);
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 2, 0, 2);
        add(fontField, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        add(fontSizeField, gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        add(editButton, gbc);
        
        String[] fonts = optionRestrictedFonts
                ? new String[]{Font.DIALOG, Font.DIALOG_INPUT, Font.MONOSPACED, Font.SERIF, Font.SANS_SERIF}
                : null;
        editButton.addActionListener(e -> {
            if (fontChooser == null) {
                fontChooser = new FontChooser(parent, fonts, !optionNoStyle);
            }
            String info = optionRestrictedFonts ? Language.getString("settings.otherFonts.restrictedInfo") : null;
            int result = fontChooser.showDialog(current, info);
            if (result == FontChooser.ACTION_OK) {
                current = fontChooser.getSelectedFont();
                saveFont();
            }
        });
        
        fontSetting = new StringSetting() {
            
            @Override
            public String getSettingValue() {
                return fontSettingValue;
            }

            @Override
            public void setSettingValue(String value) {
                fontSettingValue = value;
                loadFont();
            }
            
        };
        
        fontSizeSetting = new LongSetting() {
            
            @Override
            public Long getSettingValue() {
                return (long)fontSizeSettingValue;
            }

            @Override
            public Long getSettingValue(Long def) {
                return (long)fontSizeSettingValue;
            }

            @Override
            public void setSettingValue(Long setting) {
                fontSizeSettingValue = setting.intValue();
                loadFont();
            }
        };
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        editButton.setEnabled(enabled);
        fontField.setEnabled(enabled);
        fontSizeField.setEnabled(enabled);
        super.setEnabled(enabled);
    }
    
    private void loadFont() {
        if (settingType == SettingType.STRING) {
            current = Font.decode(fontSettingValue);
        } else {
            current = Font.decode(fontSettingValue+" "+fontSizeSettingValue);
        }
        updateDisplay();
    }
    
    private void saveFont() {
        if (settingType == SettingType.STRING) {
            fontSettingValue = makeFontName(current)+" "+current.getSize();
        }
        else {
            fontSettingValue = current.getName();
        }
        fontSizeSettingValue = current.getSize();
        updateDisplay();
    }
    
    /**
     * Create a string according to what Font.decode() expects (family and style
     * only).
     * 
     * @param font
     * @return 
     */
    private String makeFontName(Font font) {
        if (font.isBold() && font.isItalic()) {
            return font.getName()+" BoldItalic";
        }
        return String.format("%s%s%s",
                font.getName(),
                font.isBold() ? " Bold" : "",
                font.isItalic() ? " Italic" : "");
    }
    
    private void updateDisplay() {
        fontField.setText(makeFontName(current));
        fontSizeField.setText(String.valueOf(current.getSize()));
    }
    
    public StringSetting getFontSetting() {
        return fontSetting;
    }
    
    public LongSetting getFontSizeSetting() {
        return fontSizeSetting;
    }
    
}
