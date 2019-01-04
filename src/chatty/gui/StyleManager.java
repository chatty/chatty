
package chatty.gui;

import chatty.util.colors.HtmlColors;
import chatty.gui.components.textpane.ChannelTextPane.Attribute;
import chatty.gui.components.textpane.ChannelTextPane.Setting;
import chatty.gui.components.textpane.MyStyleConstants;
import chatty.util.colors.ColorCorrectionNew;
import chatty.util.colors.ColorCorrector;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.*;

/**
 * Provides style information to other objects based on the settings.
 * 
 * @author tduva
 */
public class StyleManager implements StyleServer {
    
    private final static Logger LOGGER = Logger.getLogger(StyleManager.class.getName());
    
    public static final Set<String> settingNames = new HashSet<>(Arrays.asList(
            "font", "fontSize", "timestampEnabled", "emoticonsEnabled",
            "foregroundColor","infoColor","compactColor","backgroundColor",
            "backgroundColor2", "alternateBackground", "messageSeparator",
            "separatorColor", "bottomMargin",
            "inputBackgroundColor","inputForegroundColor","usericonsEnabled",
            "timestamp","highlightColor","highlightBackgroundColor",
            "highlightBackground", "showBanMessages","autoScroll",
            "deletedMessagesMode", "deletedMessagesMaxLength","searchResultColor",
            "lineSpacing", "bufferSize", "actionColored","combineBanMessages",
            "timestampTimezone", "autoScrollTimeout", "searchResultColor2",
            "inputFont","emoteScale", "emoteMaxHeight", "botBadgeEnabled",
            "filterCombiningCharacters", "pauseChatOnMouseMove",
            "pauseChatOnMouseMoveCtrlRequired", "showAnimatedEmotes",
            "banReasonAppended", "banDurationAppended",
            "banDurationMessage", "banReasonMessage", "displayNamesMode",
            "paragraphSpacing", "bufferSizes", "userlistFont",
            "showImageTooltips", "highlightMatches", "nickColorCorrection",
            "inputHistoryMultirowRequireCtrl" // Not delievered through this
            ));
    
    private MutableAttributeSet baseStyle;
    private MutableAttributeSet standardStyle;
    private MutableAttributeSet specialStyle;
    private MutableAttributeSet infoStyle;
    private MutableAttributeSet paragraphStyle;
    private MutableAttributeSet other;
    private MutableAttributeSet highlightStyle;
    private Font inputFont;
    private Font userlistFont;
    private Color backgroundColor;
    private Color foregroundColor;
    private Color inputBackgroundColor;
    private Color inputForegroundColor;
    private Color highlightColor;
    private Color searchResultColor;
    private Color searchResultColor2;
    private Color infoColor;
    
    private ColorCorrector colorCorrector;
    
    private final Settings settings;
    private final Component dummyComponent = new JDialog();
    
    public StyleManager(Settings settings) {
        this.settings = settings;
        makeStyles();
    }
    
    /**
     * Remakes the styles, usually when a setting was changed.
     */
    public void refresh() {
        LOGGER.info("Refreshing styles..");
        makeStyles();
    }
    
    private void makeStyles() {

        inputFont = Font.decode(settings.getString("inputFont"));
        userlistFont = Font.decode(settings.getString("userlistFont"));
        //font = new Font("Comic Sans MS", Font.PLAIN, 12);
        
        foregroundColor = makeColor("foregroundColor", Color.BLACK);
        HtmlColors.setDefaultColor(foregroundColor);
        backgroundColor = makeColor("backgroundColor");
        inputBackgroundColor = makeColor("inputBackgroundColor");
        inputForegroundColor = makeColor("inputForegroundColor");
        highlightColor = makeColor("highlightColor");
        searchResultColor = makeColor("searchResultColor");
        searchResultColor2 = makeColor("searchResultColor2");
        infoColor = makeColor("infoColor");
        
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        String fontFamily = settings.getString("font");
        int fontSize = (int)settings.getLong("fontSize");
        baseStyle = new SimpleAttributeSet(defaultStyle);
        StyleConstants.setFontFamily(baseStyle, fontFamily);
        StyleConstants.setFontSize(baseStyle, fontSize);
        //StyleConstants.setBackground(baseStyle, new Color(20,20,20));
        
        standardStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(standardStyle, makeColor("foregroundColor"));
        
        highlightStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(highlightStyle, highlightColor);
        
        specialStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(specialStyle, makeColor("compactColor"));

        infoStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(infoStyle, makeColor("infoColor"));
        
        paragraphStyle = new SimpleAttributeSet();
        // Divide by 10 so integer values can be used for this setting
        float spacing = settings.getLong("lineSpacing") / (float)10.0;
        StyleConstants.setLineSpacing(paragraphStyle, spacing);
        paragraphStyle.addAttribute(Attribute.PARAGRAPH_SPACING, settings.getLong("paragraphSpacing"));
        
        MyStyleConstants.setBackground2(paragraphStyle,
                settings.getBoolean("alternateBackground") ? makeColor("backgroundColor2", null) : null);
        MyStyleConstants.setHighlightBackground(paragraphStyle,
                settings.getBoolean("highlightBackground") ? makeColor("highlightBackgroundColor", null) : null);
        MyStyleConstants.setSeparatorColor(paragraphStyle,
                settings.getBoolean("messageSeparator") ? makeColor("separatorColor", null) : null);
        MyStyleConstants.setFontHeight(paragraphStyle, dummyComponent.getFontMetrics(new Font(fontFamily, Font.PLAIN, fontSize)).getHeight());
        MyStyleConstants.setHighlightMatchesEnabled(paragraphStyle, settings.getBoolean("highlightMatches"));
        
        other = new SimpleAttributeSet();
        addBooleanSetting(Setting.EMOTICONS_ENABLED, "emoticonsEnabled");
        addLongSetting(Setting.EMOTICON_SCALE_FACTOR, "emoteScale");
        addLongSetting(Setting.EMOTICON_MAX_HEIGHT, "emoteMaxHeight");
        addBooleanSetting(Setting.USERICONS_ENABLED, "usericonsEnabled");
        addBooleanSetting(Setting.SHOW_BANMESSAGES, "showBanMessages");
        addBooleanSetting(Setting.AUTO_SCROLL, "autoScroll");
        addLongSetting(Setting.AUTO_SCROLL_TIME, "autoScrollTimeout");
        addBooleanSetting(Setting.ACTION_COLORED, "actionColored");
        addLongSetting(Setting.BUFFER_SIZE, "bufferSize");
        addBooleanSetting(Setting.COMBINE_BAN_MESSAGES, "combineBanMessages");
        addBooleanSetting(Setting.BAN_DURATION_APPENDED, "banDurationAppended");
        addBooleanSetting(Setting.BAN_REASON_APPENDED, "banReasonAppended");
        addBooleanSetting(Setting.BAN_DURATION_MESSAGE, "banDurationMessage");
        addBooleanSetting(Setting.BAN_REASON_MESSAGE, "banReasonMessage");
        addBooleanSetting(Setting.BOT_BADGE_ENABLED, "botBadgeEnabled");
        addBooleanSetting(Setting.SHOW_TOOLTIPS, "showImageTooltips");
        addLongSetting(Setting.FILTER_COMBINING_CHARACTERS, "filterCombiningCharacters");
        addBooleanSetting(Setting.PAUSE_ON_MOUSEMOVE, "pauseChatOnMouseMove");
        addBooleanSetting(Setting.PAUSE_ON_MOUSEMOVE_CTRL_REQUIRED, "pauseChatOnMouseMoveCtrlRequired");
        addBooleanSetting(Setting.EMOTICONS_SHOW_ANIMATED, "showAnimatedEmotes");
        addLongSetting(Setting.BOTTOM_MARGIN, "bottomMargin");
        // Deleted Messages Settings
        String deletedMessagesMode = settings.getString("deletedMessagesMode");
        long deletedMessagesModeNumeric = 0;
        if (deletedMessagesMode.equals("delete")) {
            deletedMessagesModeNumeric = -1;
        } else if (deletedMessagesMode.equals("keepShortened")) {
            deletedMessagesModeNumeric = settings.getLong("deletedMessagesMaxLength");
        }
        other.addAttribute(Setting.DELETED_MESSAGES_MODE, deletedMessagesModeNumeric);
        addLongSetting(Setting.DISPLAY_NAMES_MODE, "displayNamesMode");
        
        colorCorrector = ColorCorrector.get(settings.getString("nickColorCorrection"));
    }
    
    private void addBooleanSetting(Setting key, String name) {
        other.addAttribute(key, settings.getBoolean(name));
    }
    
    private void addLongSetting(Setting key, String name) {
        other.addAttribute(key, settings.getLong(name));
    }
    
    private Color makeColor(String setting) {
        return makeColor(setting, foregroundColor);
    }
    
    private Color makeColor(String setting, Color defaultColor) {
        return HtmlColors.decode(settings.getString(setting),defaultColor);
    }

    @Override
    public MutableAttributeSet getStyle() {
        return getStyle("regular");
    }

    @Override
    public MutableAttributeSet getStyle(String type) {
        switch (type) {
            case "special":
                return specialStyle;
            case "standard":
                return standardStyle;
            case "info":
                return infoStyle;
            case "highlight":
                return highlightStyle;
            case "paragraph":
                return paragraphStyle;
            case "settings":
                return other;
        }
        return baseStyle;
    }

    @Override
    public Font getFont(String type) {
        switch (type) {
            case "input":
                return inputFont;
            case "userlist":
                return userlistFont;
        }
        return null;
    }

    @Override
    public Color getColor(String type) {
        switch (type) {
            case "foreground":
                return foregroundColor;
            case "background":
                return backgroundColor;
            case "inputBackground":
                return inputBackgroundColor;
            case "inputForeground":
                return inputForegroundColor;
            case "searchResult":
                return searchResultColor;
            case "searchResult2":
                return searchResultColor2;
            case "info":
                return infoColor;
        }
        return foregroundColor;
    }
    
    @Override
    public SimpleDateFormat getTimestampFormat() {
        String timestamp = settings.getString("timestamp");
        String timezone = settings.getString("timestampTimezone");
        if (!timestamp.equals("off")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(timestamp);
                if (!timezone.isEmpty() && !timezone.equalsIgnoreCase("local")) {
                    sdf.setTimeZone(TimeZone.getTimeZone(timezone));
                }
                return sdf;
            } catch (IllegalArgumentException ex) {
                LOGGER.warning("Invalid timestamp: "+timestamp);
            }
        }
        return null;
    }

    @Override
    public ColorCorrector getColorCorrector() {
        return colorCorrector;
    }
    
}
