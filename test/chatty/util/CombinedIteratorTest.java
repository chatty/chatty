
package chatty.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class CombinedIteratorTest {

    @Test
    public void testIterator() {
        List<String> a = new ArrayList<>();
        a.add("a1");
        a.add("a2");
        
        Collection<String> b = Arrays.asList("b1", "b2");
        
        testExpected(new CombinedIterator<>(a, b), "a1", "a2", "b1", "b2");
        a.add("a3");
        testExpected(new CombinedIterator<>(a, b), "a1", "a2", "a3", "b1", "b2");
        a.clear();
        testExpected(new CombinedIterator<>(a, b), "b1", "b2");
        b = Arrays.asList();
        testExpected(new CombinedIterator<>(a, b));
        a.add("a1");
        testExpected(new CombinedIterator<>(a, b), "a1");
    }
    
    private static void testExpected(Iterator<String> it, String... expected) {
        int i = 0;
        while (it.hasNext()) {
            String item = it.next();
            assertEquals(expected[i], item);
            i++;
        }
        assertEquals(expected.length, i);
    }
    
    @Test
    public void testIteratorRemove() {
        List<String> a = new ArrayList<>();
        a.add("a1");
        a.add("a2");
        
        List<String> b = new ArrayList<>();
        b.add("b1");
        b.add("b2");
        
        testExpectedWithRemove(new CombinedIterator<>(a, b), "a2", "a1", "a2", "b1", "b2");
        
        assertEquals(Arrays.asList("a1"), a);
        assertEquals(Arrays.asList("b1", "b2"), b);
        
        testExpectedWithRemove(new CombinedIterator<>(a, b), ".*", "a1", "b1", "b2");
        
        assertTrue(a.isEmpty());
        assertTrue(b.isEmpty());
    }
    
    private static void testExpectedWithRemove(Iterator<String> it, String remove, String... expected) {
        int i = 0;
        while (it.hasNext()) {
            String item = it.next();
            assertEquals(expected[i], item);
            if (item.matches(remove)) {
                it.remove();
            }
            i++;
        }
        assertEquals(expected.length, i);
    }
    
    @Test
    public void testIteratorRemove2() {
        List<String> a = new ArrayList<>();
        a.add("1");
        a.add("2");
        
        List<String> b = new ArrayList<>();
        b.add("1");
        b.add("2");
        
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 0, "1", "2", "1", "2");
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 0, "2", "1", "2");
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 0, "1", "2");
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 0, "2");
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 0);
        
        a.add("1");
        a.add("2");
        b.add("1");
        b.add("2");
        
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 3, "1", "2", "1", "2");
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 2, "1", "2", "1");
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 1, "1", "2");
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 0, "1");
        testExpectedWithRemoveIndex(new CombinedIterator<>(a, b), 0);
    }
    
    private static void testExpectedWithRemoveIndex(Iterator<String> it, int removeIndex, String... expected) {
        int i = 0;
        while (it.hasNext()) {
            String item = it.next();
            assertEquals(expected[i], item);
            if (i == removeIndex) {
                it.remove();
            }
            i++;
        }
        assertEquals(expected.length, i);
    }
    
}
