
package chatty.gui.components.updating;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class VersionTest {
    
    @Test
    public void testVersionToIntArray() {
        testVersionToIntArray("0.8.1", new int[]{0,8,1});
        testVersionToIntArray("0.8.1b", new int[]{0,8,1});
        testVersionToIntArray("0.8.1b3", new int[]{0,8,1,-1,3});
        testVersionToIntArray("0.8.1-b3", new int[]{0,8,1,-1,3});
        testVersionToIntArray("0.8.1-beta3", new int[]{0,8,1,-1,3});
        testVersionToIntArray("0.8.1-alpha3", new int[]{0,8,1,-1,3});
        testVersionToIntArray("0.8.1b10", new int[]{0,8,1,-1,10});
        testVersionToIntArray("0", new int[]{0});
        testVersionToIntArray("1.2", new int[]{1,2});
        
        // Invalid versions
        testVersionToIntArray("a", new int[]{0});
        testVersionToIntArray("a.b", new int[]{0,0});
    }
    
    private void testVersionToIntArray(String input, int[] output) {
        assertArrayEquals(Version.versionToIntArray(input), output);
    }
    
    @Test
    public void testCompareVersions() {
        assertEquals(Version.compareVersions("0.8.1", "0.8.1"), 0);
        assertEquals(Version.compareVersions("0.8.1", "0.8.2"), 1);
        assertEquals(Version.compareVersions("0.8.2", "0.8.1"), -1);
        assertEquals(Version.compareVersions("0.8.1b1", "0.8.1"), 1);
        assertEquals(Version.compareVersions("0.8.1b4", "0.8.1"), 1);
        assertEquals(Version.compareVersions("0.8.1b1", "0.8.1b1"), 0);
        assertEquals(Version.compareVersions("0.8.1b1", "0.8.1b2"), 1);
        assertEquals(Version.compareVersions("0.8.1b1", "0.8.1.1.1"), 1);
        assertEquals(Version.compareVersions("0.8.1b1", "0.8.1.1"), 1);
        assertEquals(Version.compareVersions("0.8.1b1", "0.8.1.-1.1"), 0);
        assertEquals(Version.compareVersions("0.8.1b2", "0.8.1.-1.1"), -1);
        assertEquals(Version.compareVersions("0.8.4.1b3", "0.8.4.1"), 1);
        assertEquals(Version.compareVersions("0.8.4.1-b3", "0.8.4.1"), 1);
        assertEquals(Version.compareVersions("0.8.4.1beta3", "0.8.4.1"), 1);
        assertEquals(Version.compareVersions("0.8.4.1-beta3", "0.8.4.1"), 1);
        assertEquals(Version.compareVersions("0.8.10", "0.8.9"), -1);
        assertEquals(Version.compareVersions("0.12", "0.10.9"), -1);
        assertEquals(Version.compareVersions("0.22", "0.10.9"), -1);
        assertEquals(Version.compareVersions("0.9", "0.10.9"), 1);
        assertEquals(Version.compareVersions("0.10.0", "0.10"), 0);
        
        // Invalid versions
        assertEquals(Version.compareVersions("a", "0.1"), 1);
        assertEquals(Version.compareVersions("a", "b"), 0);
    }
    
}
