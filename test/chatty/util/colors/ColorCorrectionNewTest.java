
package chatty.util.colors;

import java.awt.Color;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class ColorCorrectionNewTest {
    
    /** 
     * If this is small, the test can take *very* long. Tested STEP = 1 once to
     * make sure all colors work, but it shouldn't be set to that normally.
     */
    private static final int STEP = 10;
    
    @Test
    public void test() {
        for (int r = 0; r < 256; r += STEP) {
            for (int g = 0; g < 256; g += STEP) {
                for (int b = 0; b < 256; b += STEP) {
                    testColor(new Color(r, g, b));
                }
            }
        }
        // Always test extremes
        testColor(Color.BLACK);
        testColor(Color.WHITE);
    }
    
    /**
     * Test various stuff on a single color.
     * 
     * @param c 
     */
    private static void testColor(Color c) {
        testToLightness(c);
        
        // Create a second color for some tests (but fewer, for speed)
        for (int r = 0; r < 256; r += STEP*10) {
            for (int g = 0; g < 256; g += STEP*10) {
                for (int b = 0; b < 256; b += STEP*10) {
                    /**
                     * Only test 1.0 and 0.0 (to test the result of other
                     * factors would basicially mean copying the calculations
                     * that set matchLightness() and toLightness() apart).
                     */
                    testMatchLightness(new Color(r, g, b), c, 1.0f);
                    testMatchLightness(new Color(r, g, b), c, 0.0f);
                }
            }
        }
        testMatchLightness(Color.BLACK, c, 1.0f);
        testMatchLightness(Color.WHITE, c, 0.0f);
    }
    
    private static void testToLightness(Color c) {
        for (int target = 0; target < 256; target++) {
            Color result = ColorCorrectionNew.toLightness(c, target);
            int resultLightness = ColorCorrectionNew.getLightness(result);
            int diff = Math.abs(resultLightness - target);
            if (diff > 2) {
                throw new AssertionError("Diff: " + diff + " " + c + " " + target);
            }
        }
    }
    
    /**
     * Only supports testing 1.0 and 0.0.
     * 
     * @param m
     * @param r
     * @param factor 
     */
    private static void testMatchLightness(Color m, Color r, float factor) {
        int modifyL = ColorCorrectionNew.getLightness(m);
        int referenceL = ColorCorrectionNew.getLightness(r);
        int resultL = ColorCorrectionNew.getLightness(ColorCorrectionNew.matchLightness(m, r, factor));
        int diff = factor == 1 ? Math.abs(resultL - referenceL) : Math.abs(resultL - modifyL);
        if (diff > 2) {
            throw new AssertionError("MatchDiff: " + diff + " " + m + " -> " + r + " " + factor);
        }
    }
    
}
