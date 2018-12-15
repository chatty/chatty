
package chatty.util.colors;

import java.awt.Color;

/**
 *
 * @author tduva
 */
public class ColorCorrectionNew {
    
    /**
     * sRGB Luma
     * 
     * @param c
     * @return 
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
    
}
