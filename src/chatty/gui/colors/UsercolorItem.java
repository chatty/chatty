
package chatty.gui.colors;

import chatty.Helper;
import chatty.util.colors.HtmlColors;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Associates some kind of id (name, type of user, color) with a Color.
 * 
 * @author tduva
 */
public class UsercolorItem extends ColorItem {

    private static final Set<String> statusDef = new HashSet<>(Arrays.asList(
            "$mod", "$sub", "$admin", "$staff", "$turbo", "$broadcaster", "$bot",
            "$globalmod", "$anymod", "$vip"));
    
    public static final int TYPE_NAME = 0;
    public static final int TYPE_COLOR = 1;
    public static final int TYPE_STATUS = 2;
    public static final int TYPE_ALL = 3;
    public static final int TYPE_CATEGORY = 4;
    public static final int TYPE_DEFAULT_COLOR = 5;
    public static final int TYPE_UNDEFINED = -1;
    
    public final Color color;
    public final String id;
    
    public final Color idColor;
    public final int type;
    public final String category;

    public UsercolorItem(String id, Color color) {
        super(id, color, true, null, false);
        this.color = color;
        this.id = id;
        
        // Check if a color was specified as id
        if (id.startsWith("#")) {
            idColor = HtmlColors.decode(id, null);
        } else {
            if (id.startsWith("$color:") && id.length() > 7) {
                idColor = HtmlColors.decode(id.substring(7), null);
            } else {
                idColor = null;
            }
        }

        // Check if a category was specified as id
        if (id.startsWith("$cat:") && id.length() > 5) {
            category = id.substring(5);
        } else {
            category = null;
        }
        
        // Save the type
        if (idColor != null) {
            type = TYPE_COLOR;
        } else if (id.startsWith("$cat:") && id.length() > 5) {
            type = TYPE_CATEGORY;
        } else if (statusDef.contains(id)) {
            type = TYPE_STATUS;
        } else if (Helper.isValidChannel(id)) {
            type = TYPE_NAME;
        } else if (id.equals("$all")) {
            type = TYPE_ALL;
        } else if (id.toLowerCase().equals("$defaultcolor")) {
            type = TYPE_DEFAULT_COLOR;
        } else {
            type = TYPE_UNDEFINED;
        }
    }

}
