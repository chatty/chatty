
package chatty.util.colors;

import chatty.gui.NamedColor;
import chatty.util.Debugging;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines some HTML color names and provides several methods about Colors.
 * 
 * @author tduva
 */
public class HtmlColors {
    
    private static volatile Color defaultColor = Color.BLACK;
    private static final ArrayList<NamedColor> colors = new ArrayList<>();
    
    
    static {
        
        // HTML 4.01
        add("White", 255, 255, 255);
        add("Silver", 192, 192, 192);
        add(128, 128, 128, "Grey"," Gray");
        add("Black", 0, 0, 0);
        add("Red", 255, 0, 0);
        add("Maroon", 128, 0, 0);
        add("Yellow", 255, 255, 0);
        add("Olive", 128, 128, 0);
        add("Lime", 0, 255, 0);
        add("Green", 0, 128, 0);
        add(0, 255, 255, "Aqua", "Cyan");
        add("Teal", 0, 128, 128);
        add("Blue", 0, 0, 255);
        add("Navy", 0, 0, 128);
        add(255, 0, 255, "Fuchsia", "Magenta");
        add("Purple", 128, 0, 128);
        
        // Pink colors
        add("Pink", 255,192,203);
        add("LightPink", 255,182,193);
        add("HotPink", 255,105,180);
        add("DeepPink", 255,20,147);
        add("PaleVioletRed", 219,112,147);
        add("MediumVioletRed", 199,21,133);
        
        // Red colors
        add("LightSalmon", 255, 160, 122);
        add("Salmon", 250, 128, 114);
        add("DarkSalmon", 233, 150, 122);
        add("LightCoral", 240, 128, 128);
        add("IndianRed", 205, 92, 92);
        add("Crimson", 220, 20, 60);
        add("Firebrick", 178, 34, 34);
        add("DarkRed", 139, 0, 0);
        add("Red", 255, 0, 0);
        
        // Orange colors
        add("OrangeRed", 255, 69, 0);
        add("Tomato", 255, 99, 71);
        add("Coral", 255, 127, 80);
        add("DarkOrange", 255, 140, 0);
        add("Orange", 255, 165, 0);
        add("Gold", 255, 215, 0);
        
        // Yellow colors
        add("LightYellow", 255, 255, 224);
        add("LemonChiffon", 255, 250, 205);
        add("LightGoldenrodYellow", 250, 250, 210);
        add("PapayaWhip", 255, 239, 213);
        add("Moccasin", 255, 228, 181);
        add("PeachPuff", 255, 218, 185);
        add("PaleGoldenrod", 238, 232, 170);
        add("Khaki", 240, 230, 140);
        add("DarkKahki", 189, 183, 107);
        
        // Brown colors
        add("Cornsilk", 255, 248, 220);
        add("BlanchedAlmond", 255, 235, 205);
        add("Bisque", 255, 228, 196);
        add("NavajoWhite", 255, 222, 173);
        add("Wheat", 245, 222, 179);
        add("BurlyWood", 222, 184, 135);
        add("Tan", 210, 180, 140);
        add("Rosybrown", 188, 143, 143);
        add("SandyBrown", 244, 164, 96);
        add("Goldenrod", 218, 165, 32);
        add("DarkGoldenrod", 184, 134, 11);
        add("Peru", 205, 133, 63);
        add("Chocolate", 210, 105, 30);
        add("SaddleBrown", 139, 69, 19);
        add("Sienna", 160, 82, 45);
        add("Brown", 165, 42, 42);
        add("Maroon", 128, 0, 0);
        
        // Green colors
        add("DarkOliveGreen", 85, 107, 47);
        add("OlveDrab", 107, 142, 35);
        add("YellowGreen", 154, 205, 50);
        add("LimeGreen", 50, 205, 50);
        add("LawnGreen", 124, 252, 0);
        add("Chartreuse", 127, 255, 0);
        add("GreenYellow", 173, 255, 47);
        add("SpringGreen", 0, 255, 127);
        add("MediumSpringGreen", 0, 250, 154);
        add("LightGreen", 144, 238, 144);
        add("PaleGreen", 152, 251, 152);
        add("DarkSeaGreen", 143, 188, 143);
        add("MediumSeaGreen", 60, 179, 113);
        add("SeaGreen", 46, 139, 87);
        add("ForestGreen", 34, 139, 34);
        add("DarkGreen", 0, 100, 0);
        
        // Cyan colors
        add("MediumAquamarine", 102, 205, 170);
        add("Cyan", 0, 255, 255);
        add("LightCyan", 224, 255, 255);
        add("PaleTurquoise", 175, 238, 238);
        add("Aquamarine", 127, 255, 212);
        add("Turquoise", 64, 224, 208);
        add("MediumTurquoise", 72, 209, 204);
        add("DarkTurquoise", 0, 206, 209);
        add("LightSeaGreen", 32, 178, 170);
        add("CadetBlue", 95, 158, 160);
        add("DarkCyan", 0, 139, 139);

        // Blue colors
        add("LightSteelBlue", 176, 196, 222);
        add("PowderBlue", 176, 224, 230);
        add("LightBlue", 173, 216, 230);
        add("SkyBlue", 135, 206, 235);
        add("LightSkyBlue", 135, 206, 250);
        add("DeepSkyBlue", 0, 191, 255);
        add("DodgerBlue", 30, 144, 255);
        add("CornflowerBlue", 100, 149, 237);
        add("SteelBlue", 70, 130, 180);
        add("RoyalBlue", 65, 105, 225);
        add("MediumBlue", 0, 0, 205);
        add("DarkBlue", 0, 0, 139);
        add("MidnightBlue", 25, 25, 112);
        
        // Purple colors
        add("Lavender",230,230,250);
        add("Thistle",216,191,216);
        add("Plum",221,160,221);
        add("Violet", 238, 130, 238);
        add("Orchid", 218, 112, 214);
        add("Magenta", 255, 0, 255);
        add("MediumOrchid", 186, 85, 211);
        add("MediumPurple", 147, 112, 219);
        add("BlueViolet", 138, 43, 226);
        add("DarkViolet", 148, 0, 211);
        add("DarkMagenta", 139, 0, 139);
        add("Indigo", 75, 0, 130);
        add("DarkSlateBlue", 72, 61, 139);
        add("SlateBlue", 106, 90, 205);
        add("MediumSlateBlue", 123, 104, 238);
        
        // White/Gray/Grey colors
        add("Snow", 255, 250, 250);
        add("Honeydew", 240, 255, 240);
        add("MintCream", 245, 255, 250);
        add("Azure", 240, 255, 255);
        add("AliceBlue", 240, 248, 255);
        add("GhostWhite", 248, 248, 255);
        add("WhiteSmoke", 245, 245, 245);
        add("Seashell", 255, 245, 238);
        add("Beige", 245, 245, 220);
        add("OldLace", 253, 245, 230);
        add("FloralWhite", 255, 250, 240);
        add("Ivory", 255, 255, 240);
        add("AntiqueWhite", 250, 235, 215);
        add("Linen", 250, 240, 230);
        add("LavenderBlush", 255, 240, 245);
        add("MistyRose", 255, 228, 225);
        add("Gainsboro", 220, 220, 220);
        add(211, 211, 211, "LightGrey", "LightGray");
        add(169, 169, 169, "DarkGrey", "DarkGray");
        add(105, 105, 105, "DimGrey", "DimGray");
        add(119, 136, 153, "LightSlateGrey", "LightSlateGray");
        add(112, 128, 144, "SlateGrey", "SlateGray");
        add(47, 79, 79, "DarkSlateGrey", "DarkSlateGray");
    }
    
    public static String getNamedColorString(Color color) {
        return getNamedColorString(color, false);
    }
    
    public static String getNamedColorString(Color color, boolean verbose) {
        if (color == null) {
            return null;
        }
        for (NamedColor c : colors) {
            if (c.equals(color)) {
                if (verbose) {
                    return String.format("%s (%s)",
                                         c.getName(), getColorString(color));
                }
                else {
                    return c.getName();
                }
            }
        }
        return getColorString(color);
    }
    
    private static void add(String name, int r, int g, int b) {
        colors.add(new NamedColor(name, r, g, b));
    }
    
    private static void add(int r, int g, int b, String... names) {
        colors.add(new NamedColor(names, r, g, b));
    }

    public static NamedColor getNamedColor(String name) {
        for (NamedColor color : colors) {
            if (color.hasName(name)) {
                return color;
            }
        }
        return null;
    }
    
    public static List<NamedColor> getNamedColors() {
        return new ArrayList<>(colors);
    }
    
    /**
     * Decodes an HTML color and turns it into a Color object.
     * 
     * Supports some named colors and String formats that are supported by
     * Color.decode(). If no color can be decoded, returns the defaultColor
     * specified by the caller.
     * 
     * @param color The color as a String
     * @param defaultColor The color that should be returned if no color could
     *                      be decoded
     * @return The color that was decoded, or the given {@code defaultColor}
     */
    public static Color decode(String color, Color defaultColor) {
        if (color == null) {
            return defaultColor;
        }
        Color c = getNamedColor(color);
        if (c == null) {
            // No recognized named color
            try {
                c = Color.decode(color);
            } catch (NumberFormatException ex) {
                // If neither color, return default
                return defaultColor;
            }
        }
        return c;
    }
    
    /**
     * Returns a color for the given String, which should either be a Html Color
     * Code (e.g. #0000FF) or a Color name (e.g. Blue).
     * 
     * If no color could be found, then the default color is returned.
     * 
     * @param color The String with the color.
     * @return The color that was found for the String or the default color.
     * @see decode(String, Color)
     */
    public static Color decode(String color) {
        return decode(color, defaultColor);
    }
    
    /**
     * Set the default color.
     * 
     * @param color 
     */
    public static void setDefaultColor(Color color) {
        defaultColor = color;
    }
    
    public static String getColorString(Color color) {
        if (color == null) {
            return null;
        }
        return String.format("#%02x%02x%02x",
                color.getRed(), color.getGreen(), color.getBlue()).toUpperCase();
    }
    
}
