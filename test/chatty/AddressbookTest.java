
package chatty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class AddressbookTest {
    
    private Addressbook ab;
    
    public AddressbookTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        ab = new Addressbook("addressbookTest", "addressbookTestImport");
    }
    
    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    @Test
    public void testAdd() {
        ab.add("abc", "123");
        List<AddressbookEntry> desiredResult = new ArrayList<>();
        Set<String> categories = new HashSet<>();
        categories.add("123");
        desiredResult.add(new AddressbookEntry("Abc", categories));
        assertEquals(ab.getEntries(), desiredResult);
        assertEquals(ab.get("abc").getCategories(), categories);
        assertEquals(ab.getEntries().size(), 1);
    }
}
