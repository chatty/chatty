
package chatty.gui;

import java.awt.Color;
import java.awt.Insets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;

/**
 * Functions about setting Look And Feel defaults loaded from a user defined
 * setting.
 * 
 * @author tduva
 */
public class LaFCustomDefaults {

    private static final Pattern PARSE_UIDEFAULT = Pattern.compile("(\\w+)\\((.+)\\)");

    /**
     * Input in the format "type(values)", e.g. "border(1 2 1 2,255 0 0)".
     * 
     * @param input
     * @return 
     */
    public static Object fromString(String input) {
        Matcher m = PARSE_UIDEFAULT.matcher(input);
        if (m.find()) {
            String type = m.group(1);
            String value = m.group(2);
            switch (type) {
                case "border":
                    return createDefaultBorder(value);
                case "int":
                    return Integer.parseInt(value);
                case "color":
                    return LaFUtil.parseColor(value, null);
                case "insets":
                    return createDefaultInsets(value);
            }
        }
        return null;
    }
    
    private static Border createDefaultBorder(String value) {
        String[] split = value.split(",");
        if (value.isEmpty()) {
            return EMPTY_BORDER;
        }
        Insets insets = createDefaultInsets(split[0]);
        Color color = null;
        if (split.length == 2) {
            color = LaFUtil.parseColor(split[1], null);
        }
        if (color != null) {
            return new UIResourceMatteBorder(insets.top, insets.left, insets.bottom, insets.right, color);
        }
        return new UIResourceEmptyBorder(insets.top, insets.left, insets.bottom, insets.right);
    }
    
    private static InsetsUIResource createDefaultInsets(String value) {
        String[] sizeSplit = value.split(" ");
        int top = 1;
        int left = 1;
        int bottom = 1;
        int right = 1;
        try {
            if (sizeSplit.length == 1) {
                top = Integer.parseInt(sizeSplit[0]);
                left = Integer.parseInt(sizeSplit[0]);
                bottom = Integer.parseInt(sizeSplit[0]);
                right = Integer.parseInt(sizeSplit[0]);
            }
            else {
                top = Integer.parseInt(sizeSplit[0]);
                left = Integer.parseInt(sizeSplit[1]);
                bottom = Integer.parseInt(sizeSplit[2]);
                right = Integer.parseInt(sizeSplit[3]);
            }
        }
        catch (Exception ex) {
            // Use defaults
        }
        return new InsetsUIResource(top, left, bottom, right);
    }
    
    public static final UIResourceEmptyBorder EMPTY_BORDER = new UIResourceEmptyBorder(0, 0, 0, 0);
    
    /**
     * Implementing UIResource so when installing defaults it is clear that this
     * wasn't set through a setBorder() method and is properly replaced with the
     * current value.
     */
    public static class UIResourceEmptyBorder extends EmptyBorder implements UIResource {

        public UIResourceEmptyBorder(int top, int left, int bottom, int right) {
            super(top, left, bottom, right);
        }
        
    }
    
    public static class UIResourceMatteBorder extends MatteBorder implements UIResource {

        public UIResourceMatteBorder(int top, int left, int bottom, int right, Color matteColor) {
            super(top, left, bottom, right, matteColor);
        }
        
    }
    
}
