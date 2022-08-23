
package chatty.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class SpecialMapTest {

    @Test
    public void testSet() {
        Map<String, Set<String>> regularMap = new HashMap<>();
        SpecialMap<String, Set<String>> map = new SpecialMap<>(regularMap, () -> new HashSet<>());
        map.getPut("a").add("1");
        assertEquals(new HashSet<>(Arrays.asList("1")), regularMap.get("a"));
        assertEquals(1, map.subSize());
        map.getPut("a").add("2");
        assertEquals(new HashSet<>(Arrays.asList("1", "2")), regularMap.get("a"));
        assertEquals(2, map.subSize());
        map.getOptional("a").add("3");
        assertEquals(new HashSet<>(Arrays.asList("1", "2", "3")), regularMap.get("a"));
        assertEquals(3, map.subSize());
        map.getOptional("b").add("3");
        assertEquals(new HashSet<>(Arrays.asList("1", "2", "3")), regularMap.get("a"));
        assertEquals(3, map.subSize());
        assertFalse(regularMap.containsKey("b"));
        map.getPut("c");
        assertEquals(3, map.subSize());
        map.getPut("c").add("4");
        assertEquals(new HashSet<>(Arrays.asList("1", "2", "3")), regularMap.get("a"));
        assertEquals(new HashSet<>(Arrays.asList("4")), regularMap.get("c"));
        assertEquals(4, map.subSize());
        
        assertEquals(new HashSet<>(Arrays.asList("a", "c")), map.keySet());
        assertEquals(new HashSet<>(Arrays.asList("a", "c")), regularMap.keySet());
        
        assertEquals(map.get("a"), regularMap.get("a"));
        assertEquals(map.entrySet(), regularMap.entrySet());
        assertEquals(map.isEmpty(), regularMap.isEmpty());
        assertEquals(map.values(), regularMap.values());
        assertEquals(map.size(), regularMap.size());
        
        map.subRemoveValue("3");
        assertEquals(new HashSet<>(Arrays.asList("1", "2")), regularMap.get("a"));
        assertEquals(new HashSet<>(Arrays.asList("4")), regularMap.get("c"));
        assertEquals(3, map.subSize());
    }
    
    @Test
    public void testMap() {
        Map<String, Map<Integer, String>> regularMap = new HashMap<>();
        SpecialMap<String, Map<Integer, String>> map = new SpecialMap<>(regularMap, () -> new HashMap<>());
        
        map.getPut("a").put(1, "1");
        assertEquals("1", regularMap.get("a").get(1));
        assertEquals(1, map.subSize());
        map.getPut("a").put(2, "2");
        assertEquals(2, map.subSize());
        map.getOptional("b");
        assertFalse(regularMap.containsKey("b"));
        assertEquals("1", map.getOptional("a").get(1));
        assertEquals("2", map.getOptional("a").get(2));
        map.subRemoveValue("2");
        assertEquals("1", map.getOptional("a").get(1));
        assertEquals(null, map.getOptional("a").get(2));
        assertEquals("1", regularMap.get("a").get(1));
        assertEquals(null, regularMap.get("a").get(2));
        assertEquals(1, map.subSize());
        regularMap.put("c", new HashMap<>());
        assertEquals(1, map.subSize());
        Map<Integer, String> subMap = new HashMap<>();
        subMap.put(1, "1");
        regularMap.put("d", subMap);
        assertEquals(2, map.subSize());
        map.subRemoveValue("1");
        assertEquals(0, map.subSize());
        assertTrue(map.subIsEmpty());
        assertFalse(map.isEmpty());
    }
    
}
