
package chatty.util;

import chatty.util.IconManager.CustomIcon;
import chatty.util.IconManager.CustomIcon.Type;
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
        CustomIcon img18 = createTestImageIcon(Type.ALL, 18, 18);
        CustomIcon img64 = createTestImageIcon(Type.ALL, 64, 64);
        CustomIcon img128 = createTestImageIcon(Type.ALL, 128, 128);
        CustomIcon img128x64 = createTestImageIcon(Type.ALL, 128, 64);
        CustomIcon img18tray = createTestImageIcon(Type.TRAY, 18, 18);
        
        List<CustomIcon> test1 = Arrays.asList(new CustomIcon[]{img18, img64, img128});
        List<CustomIcon> test2 = Arrays.asList(new CustomIcon[]{img18});
        List<CustomIcon> test3 = Arrays.asList(new CustomIcon[]{img64, img18});
        List<CustomIcon> test4 = Arrays.asList(new CustomIcon[]{img128x64, img64, img18});
        List<CustomIcon> test5 = Arrays.asList(new CustomIcon[]{img18tray});
        System.out.println(IconManager.getBestFit(test1, Type.ALL, 32, 32).getIconHeight());
        testBestFit(test1, Type.ALL, 32, 32, img64);
        testBestFit(test1, Type.ALL, 16, 16, img18);
        testBestFit(test1, Type.ALL, 18, 18, img18);
        testBestFit(test1, Type.ALL, 20, 20, img64);
        testBestFit(test1, Type.ALL, 40, 20, img64);
        testBestFit(test1, Type.ALL, 100, 20, img128);
        testBestFit(test1, Type.ALL, 100, 80, img128);
        testBestFit(test2, Type.ALL, 100, 20, img18);
        testBestFit(test2, Type.ALL, 1, 1, img18);
        testBestFit(test3, Type.ALL, 100, 20, img64);
        testBestFit(test4, Type.ALL, 128, 64, img128x64);
        testBestFit(test4, Type.ALL, 64, 64, img64);
        testBestFit(test4, Type.ALL, 80, 64, img128x64);
        testBestFit(test4, Type.TRAY, 80, 64, null);
        testBestFit(test5, Type.ALL, 32, 32, null);
        testBestFit(test5, Type.MAIN, 32, 32, null);
        testBestFit(test5, Type.TRAY, 32, 32, img18tray);
        testBestFit(test5, Type.TRAY, 32, 32, img18tray);
    }
    
    private void testBestFit(Collection<CustomIcon> imgs, Type type, int width, int height, CustomIcon expected) {
        assertEquals(expected != null ? expected.icon : null, IconManager.getBestFit(imgs, type, width, height));
        List<CustomIcon> shuffled = new ArrayList<>(imgs);
        Collections.shuffle(shuffled);
        assertEquals(expected != null ? expected.icon : null, IconManager.getBestFit(imgs, type, width, height));
        Collections.shuffle(shuffled);
        assertEquals(expected != null ? expected.icon : null, IconManager.getBestFit(imgs, type, width, height));
    }
    
    private static CustomIcon createTestImageIcon(Type type, int width, int height) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            return new CustomIcon(type, new ImageIcon(img));
    }
    
}
