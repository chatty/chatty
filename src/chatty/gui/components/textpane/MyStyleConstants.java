
package chatty.gui.components.textpane;

import java.awt.Color;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;

/**
 *
 * @author tduva
 */
public class MyStyleConstants {
    
    enum Attribute {
        BACKGROUND2, HIGHLIGHT_BACKGROUND, SEPARATOR_COLOR, FONT_HEIGHT,
        HIGHLIGHT_MATCHES, LABEL_BACKGROUND
    }
    
    public static void setHighlightMatchesEnabled(MutableAttributeSet attr, boolean state) {
        attr.addAttribute(Attribute.HIGHLIGHT_MATCHES, state);
    }
    
    public static boolean getHighlightMatchesEnabled(AttributeSet attr) {
        return getBoolean(attr, Attribute.HIGHLIGHT_MATCHES);
    }
    
    public static void setFontHeight(MutableAttributeSet attr, int height) {
        attr.addAttribute(Attribute.FONT_HEIGHT, height);
    }
    
    public static int getFontHeight(AttributeSet attr) {
        return getInteger(attr, Attribute.FONT_HEIGHT);
    }
    
    public static void setBackground2(MutableAttributeSet attr, Color color) {
        setColor(attr, color, Attribute.BACKGROUND2);
    }
    
    public static Color getBackground2(AttributeSet attr) {
        return getColor(attr, Attribute.BACKGROUND2);
    }
    
    public static void setHighlightBackground(MutableAttributeSet attr, Color color) {
        setColor(attr, color, Attribute.HIGHLIGHT_BACKGROUND);
    }
    
    public static Color getHighlightBackground(AttributeSet attr) {
        return getColor(attr, Attribute.HIGHLIGHT_BACKGROUND);
    }
    
    public static void setSeparatorColor(MutableAttributeSet attr, Color color) {
        setColor(attr, color, Attribute.SEPARATOR_COLOR);
    }
    
    public static Color getSeparatorColor(AttributeSet attr) {
        return getColor(attr, Attribute.SEPARATOR_COLOR);
    }
    
    public static void setLabelBackground(MutableAttributeSet attr, Color color) {
        setColor(attr, color, Attribute.LABEL_BACKGROUND);
    }
    
    public static Color getLabelBackground(AttributeSet attr) {
        return getColor(attr, Attribute.LABEL_BACKGROUND);
    }
    
    private static void setColor(MutableAttributeSet attr, Color color, Attribute key) {
        attr.addAttribute(key, color != null ? color : false);
    }
    
    private static Color getColor(AttributeSet attr, Attribute key) {
        Object value = attr.getAttribute(key);
        if (value instanceof Color) {
            return (Color)value;
        }
        return null;
    }
    
    private static int getInteger(AttributeSet attr, Attribute key) {
        Object value = attr.getAttribute(key);
        if (value instanceof Integer) {
            return (Integer)value;
        }
        return -1;
    }
    
    private static boolean getBoolean(AttributeSet attr, Attribute key) {
        Object value = attr.getAttribute(key);
        if (value instanceof Boolean) {
            return (Boolean)value;
        }
        return false;
    }
}
