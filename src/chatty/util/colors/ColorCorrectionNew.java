
package chatty.util.colors;

import java.awt.Color;

/**
 *
 * @author tduva
 */
public class ColorCorrectionNew {
    
    /**
     * Calculate the lightness of the given color (sRGB Luma).
     * 
     * @param c
     * @return An integer between 0 and 254 (inclusive)
     */
    public static int getLightness(Color c) {
        return (int)(c.getRed() * 0.2126 + c.getGreen() * 0.7152 + c.getBlue() * 0.0722);
    }

    /**
     * Gets the difference between the perceived brightness between colors.
     *
     * @param c1
     * @param c2
     * @return
     */
    public static int getLightnessDifference(Color c1, Color c2) {
        int b1 = getLightness(c1);
        int b2 = getLightness(c2);
        return b1 - b2;
    }
    
    /**
     * Gets the difference between the perceived brightness between colors.
     *
     * @param c1
     * @param c2
     * @return
     */
    public static int getLightnessDifferenceAbs(Color c1, Color c2) {
        return Math.abs(getLightnessDifference(c1, c2));
    }
    
    /**
     * Tries to change the color (if necessary) so it it better readable on the
     * given background color.
     *
     * @param foreground
     * @param background
     * @param threshold
     * @return
     */
    public static Color correctReadability(Color foreground, Color background, int threshold) {
        boolean darkBg = getLightness(background) < 180;
        int diff = getLightnessDifference(foreground, background);
        if (!darkBg) {
            diff *= -1;
            // Light colors seem to be a bit worse readable, so adjust some more
            threshold += 10;
        }
        diff = Math.max(0, diff);
        if (diff < threshold) {
            // Find adjustment factor to roughly get to threshold diff
            float factor = (1 - diff / (float)threshold) / 2;
            if (!darkBg) {
                foreground = makeDarker(foreground, 1 - factor);
            }
            else {
                foreground = makeBrighter(foreground, factor);
            }
        }
        return foreground;
    }
    
    public static Color makeBrighter(Color c, float factor) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        
        // Remaining to max value
        int rr = 255 - r;
        int rg = 255 - g;
        int rb = 255 - b;
        
        // Add a factor of remaining
        int cr = r + (int)(rr * factor);
        int cg = g + (int)(rg * factor);
        int cb = b + (int)(rb * factor);
        
        return new Color(cr, cg, cb);
    }
    
    public static Color makeDarker(Color c, float factor) {
        return new Color((int)(c.getRed() * factor), (int)(c.getGreen() * factor), (int)(c.getBlue() * factor));
    }
    
    public static Color offset(Color c, float factor) {
        if (getLightness(c) < 180) {
            return makeBrighter(c, 1 - factor);
        }
        return makeDarker(c, factor);
    }
    
    /**
     * Change a Color to a certain lightness (as determined by
     * {@link getLightness(Color)}). The resulting lightness may be a bit off.
     * 
     * @param c The Color to change
     * @param target The target lightness (0-255), too high numbers may throw
     * an error
     * @return A new modified Color, or the same if the lightness already mtches
     */
    public static Color toLightness(Color c, int target) {
        float current = getLightness(c);
        if (target == current) {
            return c;
        }
        if (target > current) {
            float factor = (target - current) / (255 - current);
            return makeBrighter(c, factor);
        }
        return makeDarker(c, target / current);
    }
    
    /**
     * Change the lightness of the given Color to the lightness of the
     * reference.
     * 
     * @param toModify The color to change (a new Color object will be created)
     * @param reference The color to match the brightness on
     * @param factor How much to match the brigthness, between 0 and 1.0
     * @return A new Color, or the given one if no change is necessary
     */
    public static Color matchLightness(Color toModify, Color reference, float factor) {
        if (factor == 0) {
            return toModify;
        }
        int lightness = getLightness(toModify);
        int lightnessRef = getLightness(reference);
        int diff = (int)((lightness - lightnessRef) * factor);
        lightness -= diff;
        return toLightness(toModify, lightness);
    }
    
    public static void main(String[] args) {
//        Color c = Color.BLACK;
//        System.out.println(makeBrighter(Color.BLACK, 0.2f));
//        System.out.println(makeDarker(Color.WHITE, 0.2f));
        
//        Color result = toLightness(Color.BLACK, 6);
//        System.out.println(result);
//        System.out.println(getLightness(result));
//        System.out.println("---");
//        Color c = new Color(140, 140, 140);
//        System.out.println(getLightness(c)+" "+getLightness(makeDarker(c, 0.5f)));
//        System.out.println(getLightness(c)+" "+getLightness(makeBrighter(c, 0.24f)));
    }
    
}
