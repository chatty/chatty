
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
    public void testToLightness() {
        for (int r = 0; r < 256; r += STEP) {
            for (int g = 0; g < 256; g += STEP) {
                for (int b = 0; b < 256; b += STEP) {
                    testToLightness(new Color(r, g, b));
                }
            }
        }
        // Always test extremes
        testToLightness(Color.BLACK);
        testToLightness(Color.WHITE);
    }
    
    private static void testToLightness(Color c) {
        for (int target = 0; target < 256; target++) {
            Color result = ColorCorrectionNew.toLightness(c, target);
            int resultLightness = ColorCorrectionNew.getLightness(result);
            if (Math.abs(resultLightness - target) > 2) {
                throw new AssertionError("Diff: " + Math.abs(resultLightness - target) + " " + c + " " + target);
            }
        }
    }
    
}
