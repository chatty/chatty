
package chatty.util.colors;

import chatty.util.Debugging;
import java.awt.Color;

/**
 *
 * @author tduva
 */
public class ColorCorrection {
    
    /**
     * Gets the perceived brightness of the given Color.
     *
     * @param color
     * @return
     */
    public static int getBrightness(Color color) {
        return (color.getRed() * 299
                + color.getGreen() * 587
                + color.getBlue() * 114) / 1000;
    }
    
    public static boolean isDarkColor(Color color) {
        return getBrightness(color) < 128;
    }
    
    public static boolean isLightColor(Color color) {
        return getBrightness(color) >= 128;
    }
    
    /**
     * Gets the difference between the perceived brightness between colors.
     * 
     * @param c1
     * @param c2
     * @return 
     */
    public static int getBrightnessDifference(Color c1, Color c2) {
        int b1 = getBrightness(c1);
        int b2 = getBrightness(c2);
        return b1 - b2;
    }
    
    /**
     * Tries to change the color (if necessary) so it it better readable on
     * the given background color.
     * 
     * This works for a few colors, but is far from perfect. For example
     * some colors have a high enough brightness difference, but they are
     * still not very well readable, e.g. springgreen on light grey.
     * 
     * @param foreground
     * @param background
     * @return 
     */
    public static Color correctReadability(Color foreground, Color background) {
        int bd = getBrightnessDifference(foreground, background);
        if (Math.abs(bd) < 50) {
            if (getBrightness(background) > 180) {
                foreground = makeDarker(foreground,0.5f);
            }
            else {
                foreground = makeBrighter(foreground,0.5f);
            }
        }
        return foreground;
    }
    
    /**
     * Tries to make the given color brighter. Might not work very well with
     * colors like 0000FF because hsb[2] is already 1.0.
     * 
     * @param color
     * @param factor
     * @return 
     */
    public static Color makeBrighter(Color color, float factor) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        if (hsb[2] == 1) {
            red += 150;
            green += 150;
            blue += 150;
            if (blue > 255) {
                blue = 255;
            }
            if (red > 255) {
                red = 255;
            }
            if (green > 255) {
                green = 255;
            }
            hsb = Color.RGBtoHSB(red, green, blue, null);
        }
        return Color.getHSBColor(hsb[0], hsb[1], factor * (1f + hsb[2]));
    }
    
    public static Color makeDarker(Color color, float factor) {
        float hsb[] = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1], factor * hsb[2]);
    }
    
}
