
package chatty.gui.colors;

import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

/**
 * Test loading and saving of settings.
 * 
 * @author tduva
 */
public class MsgColorManagerTest {
    
    @Test
    public void testOldFormat() {
        testLoadingAndSaving(
                new String[]{
                    "cat:vip,#FF9933",
                    "start:!bet,#CC0066",
                    "message; with semicolon, and comma,#9999FF"
                },
                new MsgColorItem[]{
                    new MsgColorItem("cat:vip", new Color(255, 153, 51), true, null, false),
                    new MsgColorItem("start:!bet", new Color(204, 0, 102), true, null, false),
                    new MsgColorItem("message; with semicolon, and comma", new Color(153, 153, 255), true, null, false)
                });
    }
    
    @Test
    public void testNewFormat() {
        testLoadingAndSaving(
                new String[]{
                    "cat:vip,#FF9933/1/#FFFF00/0",
                    "cat:vip2,#FF9933/1/#FFFF00/1",
                    "//,#FF9933/1/#FFFF00/1",
                    "abc,,#CC0066/1" // Invalid
                },
                new MsgColorItem[]{
                    new MsgColorItem("cat:vip", new Color(255, 153, 51), true, new Color(255, 255, 0), false),
                    new MsgColorItem("cat:vip2", new Color(255, 153, 51), true, new Color(255, 255, 0), true),
                    new MsgColorItem("//", new Color(255, 153, 51), true, new Color(255, 255, 0), true),
                    new MsgColorItem("abc,", new Color(204, 0, 102), true, null, false)
                });
    }
    
    private void testLoadingAndSaving(String[] testData, MsgColorItem[] expectedData) {
        // Define settings
        Settings settings = new Settings(null, null);
        settings.addList("msgColors", new LinkedList(), Setting.STRING);
        settings.putList("msgColors", Arrays.asList(testData));
        
        // What data is expected
        List<MsgColorItem> target = Arrays.asList(expectedData);
        
        // Load from settings
        MsgColorManager manager = new MsgColorManager(settings);
        checkData(manager.getData(), target);
        
        // Save to settings, then load again
        manager.setData(target);
        manager = new MsgColorManager(settings);
        checkData(manager.getData(), target);
    }
    
    private void checkData(List<MsgColorItem> loadedData, List<MsgColorItem> expectedData) {
        for (int i=0;i<expectedData.size();i++) {
            MsgColorItem loaded = loadedData.get(i);
            MsgColorItem expected = expectedData.get(i);
            if (!loaded.equalsAll(expected)) {
                throw new AssertionError(loaded.toStringAll()+" != "+expected.toStringAll());
            }
        }
    }

}
