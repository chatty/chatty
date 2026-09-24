
package chatty.util;

import chatty.util.ImageCache.ImageRequest;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class ImageCacheTest {

    @Test
    public void testImageRequest() {
        testUrlFactor(1, 0, 28, 28,
                                       new int[]{1, 2, 4},
                                       1,
                                       new int[]{1});
        
        testUrlFactor(1.1f, 0, 28, 28,
                                       new int[]{1, 2, 4},
                                       2,
                                       new int[]{2});
        
        testUrlFactor(1.1f, 0, 28, 28,
                                       new int[]{1, 4},
                                       4,
                                       new int[]{2, 3, 4});
        
        testUrlFactor(1.1f, 0, 28, 28,
                                       new int[]{1, 2},
                                       2,
                                       new int[]{2});
        
        testUrlFactor(1.1f, 20, 28, 28,
                                       new int[]{1, 2, 4},
                                       1,
                                       new int[]{1});
        
        testUrlFactor(2.1f, 0, 28, 28,
                                       new int[]{1, 2, 3, 4},
                                       3,
                                       new int[]{3});
        
        testUrlFactor(2.1f, 0, 28, 28,
                                       new int[]{1, 2, 4},
                                       4,
                                       new int[]{3, 4});
        
        testUrlFactor(0.9f, 0, 28, 28,
                                       new int[]{1, 2, 4},
                                       1,
                                       new int[]{1});
        
        testUrlFactor(0.8f, 0, 28, 28,
                                       new int[]{2, 4},
                                       2,
                                       new int[]{1, 2});
    }
    
    private static void testUrlFactor(float scaleFactor, int maxHeight, int defaultWidth, int defaultHeight, int[] availableFactors, int expectedUrlFactor, int[] expectedTries) {
        List<Integer> tries = new ArrayList<>();
        ImageRequest r = new ImageCache.ImageRequest(factor -> {
            tries.add(factor);
            for (int i = 0; i < availableFactors.length; i++) {
                if (availableFactors[i] == factor) {
                    return String.valueOf(factor);
                }
            }
            return null;
        }, scaleFactor, maxHeight, new Dimension(defaultWidth, defaultHeight), false);
        Assert.assertEquals(expectedUrlFactor, r.urlFactor);
        int[] tries2 = new int[tries.size()];
        for (int i=0;i<tries.size();i++) {
            tries2[i] = tries.get(i);
        }
        Assert.assertArrayEquals(expectedTries, tries2);
    }
    
    @Test
    public void testScaledSize() {
        testScaledSize(28, 28, 1f, -1, 28, 28);
        testScaledSize(28, 28, 0f, -1, 28, 28);
        testScaledSize(28, 28, -1f, -1, 28, 28);
        testScaledSize(28, 28, 0.00001f, -1, 1, 1);
        testScaledSize(28, 28, 1f, 1, 1, 1);
        testScaledSize(1, 2, 1f, 1, 1, 1);
        
        testScaledSize(28, 28, 1f, 20, 20, 20);
        testScaledSize(28, 28, 2f, -1, 56, 56);
        testScaledSize(28, 28, 0.5f, -1, 14, 14);
        
        testScaledSize(100, 50, 1f, 28, 56, 28);
        testScaledSize(50, 100, 1f, 28, 14, 28);
        
        testScaledSize(50, 100, 10f, 0, 175, 350); // Fallback max
        
        testScaledSize(480, 360, 1f, 0, 350, 262); // Fallback max
        testScaledSize(480, 360, 1f, 300, 350, 262); // Fallback max
        
        testScaledSize(360, 480, 1f, 0, 262, 350); // Fallback max
        testScaledSize(360, 480, 1f, 300, 225, 300);
        
        testScaledSize(480, 480, 1f, 0, 350, 350); // Fallback max
        testScaledSize(480, 480, 1f, 300, 300, 300);
    }
    
    private static void testScaledSize(int width, int height, float scaleFactor, int maxHeight, int expectedResultWidth, int expectedResultHeight) {
        Dimension result = ImageCache.ImageRequest.getScaledSize(new Dimension(width, height), scaleFactor, maxHeight);
        Assert.assertEquals(new Dimension(expectedResultWidth, expectedResultHeight), result);
    }
    
}
