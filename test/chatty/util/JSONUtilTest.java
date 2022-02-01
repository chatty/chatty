
package chatty.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class JSONUtilTest {

    @Test
    public void testListMapToJSON() throws ParseException {
        List<String> someList = new ArrayList<>();
        someList.add(null);
        String json = JSONUtil.listMapToJSON("list", someList, "a", "1", "b", "2", "c", null, "'blah' \"abc\"", "test 123");
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(json);
        assertTrue(root.get("list").equals(someList));
        assertTrue(root.get("a").equals("1"));
        assertTrue(root.get("b").equals("2"));
        assertNull(root.get("c"));
        assertNull(root.get("d"));
        assertTrue(root.get("'blah' \"abc\"").equals("test 123"));
    }
    
    @Test
    public void testListToJSON() throws ParseException {
        List<String> list = Arrays.asList(new String[]{"a", "b", "c", null});
        String json = JSONUtil.listToJSON("a", "b", "c", null);
        JSONParser parser = new JSONParser();
        JSONArray root = (JSONArray) parser.parse(json);
        assertEquals(root, list);
    }
    
    @Test
    public void testGetStringList() throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse("{\"list\":[\"a\", \"b\", \"c\"], \"list2\":[\"1\", [], {\"abc\": null}, null, \"2\"], \"noList\":{}}");
        
        // List with only String elements
        List<String> list = JSONUtil.getStringList(root, "list");
        List<String> expectedList = Arrays.asList(new String[]{"a", "b", "c"});
        assertEquals(expectedList, list);
        
        // List with non-String elements
        List<String> list2 = JSONUtil.getStringList(root, "list2");
        List<String> expectedList2 = Arrays.asList(new String[]{"1", "2"});
        assertEquals(expectedList2, list2);
        
        // Not a list
        assertNull(JSONUtil.getStringList(root, "noList"));
    }
    
}
