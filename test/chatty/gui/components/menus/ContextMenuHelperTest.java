
package chatty.gui.components.menus;

import chatty.gui.components.menus.ContextMenuHelper.Quality;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class ContextMenuHelperTest {
    
    @Test
    public void testParseLivestreamerQualities() {
        assertEquals(ContextMenuHelper.parseLivestreamerQualities(""),
                new ArrayList<>());
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities(" "),
                new ArrayList<>());
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("Select"),
                new ArrayList<>(Arrays.asList(new Quality("Select", "Select"))));
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("Select "),
                new ArrayList<>(Arrays.asList(new Quality("Select", "Select"))));
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("{Select quality:select}"),
                new ArrayList<>(Arrays.asList(new Quality("Select quality", "select"))));
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("Best, Worst Select |"),
                new ArrayList<>(Arrays.asList(
                        new Quality("Best", "Best"),
                        new Quality("Worst", "Worst"),
                        new Quality("Select", "Select"))));
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("Best Worst | Select"),
                new ArrayList<>(Arrays.asList(
                        new Quality("Best", "Best"),
                        new Quality("Worst", "Worst"),
                        null,
                        new Quality("Select", "Select"))));
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("{Best:1080p60,1080p,720p60,720p}, Worst | Select"),
                new ArrayList<>(Arrays.asList(
                        new Quality("Best", "1080p60,1080p,720p60,720p"),
                        new Quality("Worst", "Worst"),
                        null,
                        new Quality("Select", "Select"))));
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("{360p} {Best:1080p60,1080p,720p60,720p}"),
                new ArrayList<>(Arrays.asList(
                        new Quality("360p", "360p"),
                        new Quality("Best", "1080p60,1080p,720p60,720p"))));
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("{360p,240p}{Best:1080p60,1080p,720p60,720p}"),
                new ArrayList<>(Arrays.asList(
                        new Quality("360p,240p", "360p,240p"),
                        new Quality("Best", "1080p60,1080p,720p60,720p"))));
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("{360p,240p} {1080p60,1080p,720p60,720p}"),
                new ArrayList<>(Arrays.asList(
                        new Quality("360p,240p", "360p,240p"),
                        new Quality("1080p60,1080p,720p60,720p", "1080p60,1080p,720p60,720p"))));
        
        assertEquals(ContextMenuHelper.parseLivestreamerQualities("{High Quality:1080p60,1080p,720p60,720p} {Low Quality:360p,240p}"),
                new ArrayList<>(Arrays.asList(
                        new Quality("High Quality", "1080p60,1080p,720p60,720p"),
                        new Quality("Low Quality", "360p,240p")
                )));
    }
    
}