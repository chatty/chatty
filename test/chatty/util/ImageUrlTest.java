
package chatty.util;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class ImageUrlTest {

    @Test
    public void test() {
        testTemplate("123",
             "1", "https://example.com/cdn/123/1",
             "2", "https://example.com/cdn/123/2",
             "3", "https://example.com/cdn/123/3");
        
        testTemplate("cdn",
             "1", "https://example.com/cdn/cdn/1",
             "2", "https://example.com/cdn/cdn/2",
             "3", "https://example.com/cdn/cdn/3");
        
        testTemplate("cdn",
             "1", "https://cdn1.example.com/cdn/cdn/1",
             "2", "https://cdn1.example.com/cdn/cdn/2",
             "3", "https://cdn1.example.com/cdn/cdn/3");
        
        testTemplate("cdn",
             "1", "https://cdn1.example.com/cdn/cdn/x1",
             "2", "https://cdn1.example.com/cdn/cdn/x2",
             "3", "https://cdn1.example.com/cdn/cdn/x3");
        
        testTemplate("cdn",
             "1", "https://cdn1.example.com/cdn/cdn/1x",
             "2", "https://cdn1.example.com/cdn/cdn/2x",
             "3", "https://cdn1.example.com/cdn/cdn/3x");
        
        testSingle("123",
             "1", "https://example.com/cdn/123/1");
        
        testMap("123",
             "1", "https://example.com/cdn/123/small",
             "2", "https://example.com/cdn/123/medium",
             "3", "https://example.com/cdn/123/big");
        
        testMap(null,
             "1", "https://example.com/cdn/123/small",
             "2", "https://example.com/cdn/123/medium",
             "3", "https://example.com/cdn/123/big");
        
//        System.out.println(ImageUrl.cacheSize());
    }
    
    private static void testTemplate(String id, String... params) {
        assertEquals(ImageUrl.TemplateImageUrl.class, test(id, params).getClass());
    }
    
    private static void testMap(String id, String... params) {
        assertEquals(ImageUrl.MapImageUrl.class, test(id, params).getClass());
    }
    
    private static void testSingle(String id, String... params) {
        assertEquals(ImageUrl.SingleImageUrl.class, test(id, params).getClass());
    }
    
    private static ImageUrl test(String id, String... params) {
        Map<Integer, String> urls = new HashMap<>();
        for (int i = 0; i < params.length; i += 2) {
            urls.put(Integer.valueOf(params[i]), params[i+1]);
        }
        ImageUrl.Builder b = ImageUrl.builder(id);
        urls.forEach((size, url) -> b.addUrl(size, url));
        ImageUrl url = b.build();
        for (Map.Entry<Integer, String> entry : urls.entrySet()) {
            assertEquals(entry.getValue(), url.getUrl(id, entry.getKey()));
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(urls.get(i), url.getUrl(id, i));
        }
        return url;
    }
    
}
