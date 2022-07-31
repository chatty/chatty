
package chatty.gui.defaults;

import chatty.gui.GuiUtil;
import chatty.gui.components.settings.ColorSettings;
import chatty.gui.components.settings.NotificationSettings;
import chatty.lang.Language;
import chatty.splash.Splash;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;

/**
 *
 * @author tduva
 */
public class DefaultsPanel extends JPanel {
    
    /**
     * Used by the DefaultsPanel to set the selected setting values or get
     * default values, in a way where it doesn't necessarily always have to
     * interact with a Settings object (e.g. set values in the Settings Dialog
     * instead).
     * 
     * Also affects what is displayed/selected in the dialog and allows
     * something to be run after all changes have been applied.
     */
    public interface DefaultsHelper {
        public void setString(String setting, String value);
        public void setLong(String setting, long value);
        public void setBoolean(String setting, boolean value);
        
        public String getStringDefault(String setting);
        public boolean getBooleanDefault(String setting);
        
        public boolean getEnabled(String option);
        
        public void applied();
    }
    
    private final DefaultsHelper helper;
    
    private final JLabel styleImage;
    private final JToggleButton style1Button;
    private final JToggleButton style2Button;
    private final JToggleButton style3Button;
    private final JToggleButton style4Button;
    private final JCheckBox userlist;
    private final JTextPane fontPreview;
    private final JToggleButton font1Button;
    private final JToggleButton font2Button;
    private final JToggleButton fontSkipButton;
    private final JCheckBox notifications;
    
    private final String fontPreviewText = Splash.getThing(20, 80);
    
    public DefaultsPanel(String introduction, DefaultsHelper helper) {
        JPanel main = new JPanel(new GridBagLayout());
        
        this.helper = helper;
        
        GridBagConstraints gbc;
        
        //--------------------------
        // Style
        //--------------------------
        JPanel stylePanel = new JPanel(new GridBagLayout());
        stylePanel.setBorder(BorderFactory.createTitledBorder(Language.getString("defaults.style")));
        
        style1Button = new JToggleButton("Metal");
        style2Button = new JToggleButton("Fast");
        style3Button = new JToggleButton("Smooth");
        style4Button = new JToggleButton("Custom");
        
        ButtonGroup styleButtonGroup = new ButtonGroup();
        styleButtonGroup.add(style1Button);
        styleButtonGroup.add(style2Button);
        styleButtonGroup.add(style3Button);
        styleButtonGroup.add(style4Button);
        
        style1Button.addItemListener(e -> updateStyle());
        style2Button.addItemListener(e -> updateStyle());
        style3Button.addItemListener(e -> updateStyle());
        style4Button.addItemListener(e -> updateStyle());
        
        styleImage = new JLabel();
        
        gbc = GuiUtil.makeGbc(0, 0, 2, 1, GridBagConstraints.CENTER);
        gbc.insets = new Insets(10, 10, 5, 10);
        stylePanel.add(styleImage, gbc);
        
        JPanel styleButtonPanel = new JPanel(new GridBagLayout());
        gbc = GuiUtil.makeGbc(0, 0, 2, 1, GridBagConstraints.CENTER);
        styleButtonPanel.add(new JLabel("-- "+Language.getString("defaults.lightThemes")+" --"), gbc);
        gbc = GuiUtil.makeGbc(2, 0, 2, 1, GridBagConstraints.CENTER);
        styleButtonPanel.add(new JLabel("-- "+Language.getString("defaults.darkThemes")+" --"), gbc);
        gbc = GuiUtil.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 0);
        styleButtonPanel.add(style1Button, gbc);
        gbc = GuiUtil.makeGbc(1, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 5);
        styleButtonPanel.add(style2Button, gbc);
        gbc = GuiUtil.makeGbc(2, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 0);
        styleButtonPanel.add(style3Button, gbc);
        gbc = GuiUtil.makeGbc(3, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 5);
        styleButtonPanel.add(style4Button, gbc);
        
        gbc = GuiUtil.makeGbc(0, 2, 2, 1, GridBagConstraints.CENTER);
        gbc.weightx = 1;
        stylePanel.add(styleButtonPanel, gbc);
        
        userlist = new JCheckBox(Language.getString("defaults.option.showUserlist"));
        userlist.setSelected(helper.getEnabled("userlist"));
        userlist.setToolTipText(Language.getString("defaults.option.showUserlist.tip"));
        gbc = GuiUtil.makeGbc(0, 3, 2, 1, GridBagConstraints.CENTER);
        stylePanel.add(userlist, gbc);
        
        gbc = GuiUtil.makeGbc(0, 1, 2, 1);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        main.add(stylePanel, gbc);
        
        userlist.addItemListener(e -> updateStyle());
        
        style1Button.setSelected(true);
        
        //--------------------------
        // Font
        //--------------------------
        JPanel fontPanel = new JPanel(new GridBagLayout());
        fontPanel.setBorder(BorderFactory.createTitledBorder(Language.getString("defaults.font")));
        
        fontPreview = new JTextPane();
        gbc = GuiUtil.makeGbc(0, 5, 2, 1, GridBagConstraints.CENTER);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        fontPanel.add(new JScrollPane(fontPreview), gbc);
        
        font1Button = new JToggleButton("Dialog");
        font2Button = new JToggleButton("Classic");
        fontSkipButton = new JToggleButton("Don't change");
        
        font1Button.addItemListener(e -> updateFont());
        font2Button.addItemListener(e -> updateFont());
        fontSkipButton.addItemListener(e -> updateFont());
        
        ButtonGroup fontButtonGroup = new ButtonGroup();
        fontButtonGroup.add(font1Button);
        fontButtonGroup.add(font2Button);
        fontButtonGroup.add(fontSkipButton);
        
        JPanel fontButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        fontButtonPanel.add(font1Button);
        fontButtonPanel.add(font2Button);
        // Set font for sizing
        font1Button.setSelected(true);
        updateFont();
        if (helper.getEnabled("skip")) {
            fontButtonPanel.add(fontSkipButton);
            fontSkipButton.setSelected(true);
        }
        else {
            font1Button.setSelected(true);
        }
        
        gbc = GuiUtil.makeGbc(0, 6, 2, 1, GridBagConstraints.CENTER);
        fontPanel.add(fontButtonPanel, gbc);
        
        gbc = GuiUtil.makeGbc(0, 6, 2, 1, GridBagConstraints.CENTER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        main.add(fontPanel, gbc);
        
        updateFont();
        
        //--------------------------
        // Other Settings
        //--------------------------
        JPanel otherPanel = new JPanel(new GridBagLayout());
        otherPanel.setBorder(BorderFactory.createTitledBorder("Other"));
        gbc = GuiUtil.makeGbc(0, 0, 1, 1, GridBagConstraints.NORTHWEST);
        gbc.weighty = 1;
        notifications = new JCheckBox(Language.getString("defaults.option.notifications"));
        notifications.setSelected(helper.getEnabled("notifications"));
        notifications.setBorder(BorderFactory.createEmptyBorder());
        otherPanel.add(notifications, gbc);
        gbc = GuiUtil.makeGbc(1, 0, 1, 1, GridBagConstraints.NORTHWEST);
        otherPanel.add(new JLabel("<html><body style='width:300px;'>"+Language.getString("defaults.option.notifications.info")), gbc);
        
        gbc = GuiUtil.makeGbc(0, 7, 2, 1, GridBagConstraints.CENTER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        main.add(otherPanel, gbc);
        
        //--------------------------
        // Other
        //--------------------------
        gbc = GuiUtil.makeGbc(0, 0, 2, 1, GridBagConstraints.CENTER);
        main.add(new JLabel(introduction), gbc);
        
        setLayout(new GridBagLayout());
        gbc = GuiUtil.makeGbc(0, 0, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(main, gbc);
        
        JButton applyButton = new JButton(Language.getString("defaults.button.apply"));
        gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.CENTER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(applyButton, gbc);
        applyButton.addActionListener(e -> {
            applySettings();
            helper.applied();
        });
    }
    
    private static float RESIZE_FACTOR = 1.4f;
    
    private void updateStyle() {
        String name;
        if (style1Button.isSelected()) {
            name = "default";
        }
        else if (style2Button.isSelected()) {
            name = "fast";
        }
        else if (style3Button.isSelected()) {
            name = "hifi_soft";
        }
        else if (style4Button.isSelected()) {
            name = "custom";
        }
        else {
            return;
        }
        if (userlist.isSelected()) {
            name += "_userlist";
        }
        name += ".png";
        ImageIcon icon = getImage(name);
        icon.setImage(icon.getImage().getScaledInstance((int)(icon.getIconWidth() / RESIZE_FACTOR), (int)(icon.getIconHeight() / RESIZE_FACTOR), Image.SCALE_SMOOTH));
        styleImage.setIcon(icon);
    }
    
    private Font currentFont;
    
    private void updateFont() {
        if (fontSkipButton.isSelected()) {
            fontPreview.setText(Language.getString("defaults.font.skip"));
        }
        else {
            fontPreview.setText(fontPreviewText);
        }
        Font font;
        if (font1Button.isSelected()) {
            font = Font.decode("Dialog 14");
        }
        else if (font2Button.isSelected()) {
            font = Font.decode("Consolas 14");
        }
        else {
            return;
        }
        currentFont = font;
        fontPreview.setFont(font);
    }
    
    private void applySettings() {
        //--------------------------
        // LaF
        //--------------------------
        if (style1Button.isSelected()) {
            helper.setString("laf", "default");
        }
        else if (style2Button.isSelected()) {
            helper.setString("laf", "fast");
            helper.setString("lafTheme", "Default");
        }
        else if (style3Button.isSelected()) {
            helper.setString("laf", "hifi2");
            helper.setString("lafTheme", "Default");
        }
        else {
            helper.setString("laf", "hifiCustom");
            helper.setString("lafTheme", "Default");
            helper.setString("lafForeground", "#E5E5E5");
            helper.setString("lafBackground", "#2D2D2D");
            helper.setLong("lafGradient", 25);
            helper.setLong("lafVariant", 0);
            helper.setString("lafStyle", "classicStrong");
        }
        helper.setBoolean("lafNativeWindow", true);
        //--------------------------
        // Userlist
        //--------------------------
        helper.setBoolean("userlistEnabled", userlist.isSelected());
        //--------------------------
        // Chat colors
        //--------------------------
        for (int i = 0; i < ColorSettings.PRESET_SETTINGS.length; i++) {
            String key = ColorSettings.PRESET_SETTINGS[i];
            String value = helper.getStringDefault(key);
            if (style2Button.isSelected()) {
                value = ColorSettings.LIGHT_FAST[i];
            }
            else if (style3Button.isSelected()) {
                value = ColorSettings.DARK_SMOOTH[i];
            }
            else if (style4Button.isSelected()) {
                value = ColorSettings.DARK[i];
            }
            helper.setString(key, value);
        }
        for (int i = 0; i < ColorSettings.PRESET_SETTINGS_BOOLEAN.length; i++) {
            String key = ColorSettings.PRESET_SETTINGS_BOOLEAN[i];
            boolean value = helper.getBooleanDefault(key);
            if (style2Button.isSelected()) {
                value = ColorSettings.LIGHT_FAST_BOOLEAN[i];
            }
            else if (style3Button.isSelected()) {
                value = ColorSettings.DARK_SMOOTH_BOOLEAN[i];
            }
            else if (style4Button.isSelected()) {
                value = ColorSettings.DARK_BOOLEAN[i];
            }
            helper.setBoolean(key, value);
        }
        //--------------------------
        // Font
        //--------------------------
        if (!fontSkipButton.isSelected()) {
            helper.setString("font", currentFont.getFamily());
            helper.setLong("fontSize", currentFont.getSize());
            helper.setBoolean("timestampFontEnabled", style1Button.isSelected());
            helper.setString("timestampFont", currentFont.deriveFont(12f).getFamily() + " 12");
        }
        //--------------------------
        // Notifications
        //--------------------------
        helper.setLong("nType", notifications.isSelected()
                                    ? NotificationSettings.NOTIFICATION_TYPE_CUSTOM
                                    : NotificationSettings.NOTIFICATION_TYPE_OFF);
    }
    
    public static ImageIcon getImage(String name) {
        return new ImageIcon(DefaultsDialog.class.getResource(name));
    }
    
}
