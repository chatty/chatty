
package chatty.util;

import chatty.util.ffz.FrankerFaceZParsing;
import chatty.util.api.Emoticon;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class FrankerFaceZTest {

    @Test
    public void testParseEmote() throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(loadJSON("FFZ_emote_regular"));
        Emoticon emote = FrankerFaceZParsing.parseEmote(obj, null, null, null);
        assertNotNull(emote);
        assertEquals(emote.code, "joshWASTED");
        assertEquals(emote.creator, "Joshimuz");
        assertEquals(emote.getWidth(), 100);
        assertEquals(emote.getHeight(), 16);
        
        obj = (JSONObject) parser.parse(loadJSON("FFZ_emote_no_height"));
        emote = FrankerFaceZParsing.parseEmote(obj, null, null, null);
        assertNotNull(emote);
        assertEquals(emote.code, "joshWASTED");
        assertEquals(emote.creator, "Joshimuz");
        assertEquals(emote.getWidth(), 100);
        assertEquals(emote.getHeight(), -1);
        
        testParseEmoteError("FFZ_emote_id_string");
    }
    
    private void testParseEmoteError(String fileName) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(loadJSON(fileName));
        Emoticon emote = FrankerFaceZParsing.parseEmote(obj, null, null, null);
        assertNull(emote);
    }
    
    private String loadJSON(String fileName) throws Exception {
        Path path = Paths.get(this.getClass().getResource(fileName).toURI());
        System.out.println(path.toAbsolutePath());
        try (BufferedReader r = Files.newBufferedReader(path, Charset.forName("UTF-8"))) {
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                b.append(line);
            }
            return b.toString();
        } catch (IOException ex) {
            fail("Test failed: Error reading file");
        }
        return null;
    }
    
}
