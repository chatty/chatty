
package chatty.util;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class IconManagerTest {

    @Test
    public void testGetBestFit() {
        ImageIcon img18 = createTestImageIcon(18, 18);
        ImageIcon img64 = createTestImageIcon(64, 64);
        ImageIcon img128 = createTestImageIcon(128, 128);
        ImageIcon img128x64 = createTestImageIcon(128, 64);
        List<ImageIcon> test1 = Arrays.asList(new ImageIcon[]{img18, img64, img128});
        List<ImageIcon> test2 = Arrays.asList(new ImageIcon[]{img18});
        List<ImageIcon> test3 = Arrays.asList(new ImageIcon[]{img64, img18});
        List<ImageIcon> test4 = Arrays.asList(new ImageIcon[]{img128x64, img64, img18});
        System.out.println(IconManager.getBestFit(test1, 32, 32).getIconHeight());
        testBestFit(test1, 32, 32, img64);
        testBestFit(test1, 16, 16, img18);
        testBestFit(test1, 18, 18, img18);
        testBestFit(test1, 20, 20, img64);
        testBestFit(test1, 40, 20, img64);
        testBestFit(test1, 100, 20, img128);
        testBestFit(test1, 100, 80, img128);
        testBestFit(test2, 100, 20, img18);
        testBestFit(test2, 1, 1, img18);
        testBestFit(test3, 100, 20, img64);
        testBestFit(test4, 128, 64, img128x64);
        testBestFit(test4, 64, 64, img64);
        testBestFit(test4, 80, 64, img128x64);
    }
    
    private void testBestFit(Collection<ImageIcon> imgs, int width, int height, ImageIcon expected) {
        assertEquals(expected, IconManager.getBestFit(imgs, width, height));
        List<ImageIcon> shuffled = new ArrayList<>(imgs);
        Collections.shuffle(shuffled);
        assertEquals(expected, IconManager.getBestFit(imgs, width, height));
        Collections.shuffle(shuffled);
        assertEquals(expected, IconManager.getBestFit(imgs, width, height));
    }
    
    private static ImageIcon createTestImageIcon(int width, int height) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(img);
    }
    
}
