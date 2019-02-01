
package chatty.gui;

import chatty.Addressbook;
import chatty.Room;
import chatty.User;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.util.Arrays;
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
public class HighlighterTest {
    
    private static final User user = new User("testUser", Room.createRegular("#testChannel"));
    private static final User user2 = new User("testUser2", Room.createRegular("#testChannel"));
    private static final User user3 = new User("testUser2", Room.createRegular("#testChannel2"));
    private static final User user4 = new User("testUser3", Room.createRegular("#testChannel2"));
    private static Highlighter highlighter;
    private static Addressbook ab;
    
    public HighlighterTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        highlighter = new Highlighter();
        Settings settings = new Settings("");
        settings.addBoolean("abSaveOnChange", false);
        ab = new Addressbook(null, null, settings);
        user.setAddressbook(ab);
        user2.setAddressbook(ab);
        ab.add("testUser", "testCat");
    }
    
    private void update(String... items) {
        highlighter.update(Arrays.asList(items));
    }
    
    private void updateBlacklist(String... items) {
        highlighter.updateBlacklist(Arrays.asList(items));
    }

    @Test
    public void test() {
        updateBlacklist();
        
        // Regular
        assertFalse(highlighter.check(user, "test message"));
        
        update("test");
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "abc"));
        
        update("");
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user3, ""));
        
        update("mäh");
        assertTrue(highlighter.check(user, "mäh"));
        assertTrue(highlighter.check(user, "Mäh"));
        assertTrue(highlighter.check(user, "MÄH"));
        
        // cs:
        update("cs:Test");
        assertTrue(highlighter.check(user, " Test "));
        assertFalse(highlighter.check(user, "testi"));
        
        // start:
        update("start:Test");
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "message test"));
        
        update("start:!bet");
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, " !bet test"));
        assertTrue(highlighter.check(user, "!bett"));
        assertTrue(highlighter.check(user3, "!bet abc"));
        
        update("start:!bet ");
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, " !bet test"));
        assertTrue(highlighter.check(user, "!bett"));
        assertTrue(highlighter.check(user3, "!bet abc"));
        
        // w:
        update("w:Test");
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "testmessage"));

        // wcs:
        update("wcs:Test");
        assertTrue(highlighter.check(user, "Test message"));
        assertTrue(highlighter.check(user, "!Test message"));
        assertFalse(highlighter.check(user, "Testmessage"));
        assertFalse(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "testmessage"));
        
        update("wcs:Test|Test2");
        assertTrue(highlighter.check(user, "Test|Test2"));
        assertFalse(highlighter.check(user, "Test"));
        
        // reg: / re*:
        update("reg:dumdi|dum");
        assertTrue(highlighter.check(user, "dumdi"));
        assertTrue(highlighter.check(user, "dum"));
        assertTrue(highlighter.check(user, "hmm dum dum dumdidum"));
        assertFalse(highlighter.check(user, "Dum"));
        
        update("re*:dumdi|dum");
        assertTrue(highlighter.check(user, "dumdi"));
        assertTrue(highlighter.check(user, "dum"));
        assertTrue(highlighter.check(user, "hmm dum dum dumdidum"));
        assertFalse(highlighter.check(user, "Dum"));
        
        // regi:
        update("reg:dumdi|dum");
        assertTrue(highlighter.check(user, "didadumdidum"));
        assertTrue(highlighter.check(user, "dumdi"));
        assertTrue(highlighter.check(user, "dum"));
        assertTrue(highlighter.check(user, "hmm dum dum dumdidum"));
        assertTrue(highlighter.check(user, "dum"));
        assertFalse(highlighter.check(user, "test"));
        
        // regm: / re:
        update("regm:dumdi|dum");
        assertTrue(highlighter.check(user, "dumdi"));
        assertTrue(highlighter.check(user, "dum"));
        assertFalse(highlighter.check(user, "hmm dum dum dumdidum"));
        assertFalse(highlighter.check(user, "Dum"));
        
        update("re:dumdi|dum");
        assertTrue(highlighter.check(user, "dumdi"));
        assertTrue(highlighter.check(user, "dum"));
        assertFalse(highlighter.check(user, "hmm dum dum dumdidum"));
        assertFalse(highlighter.check(user, "Dum"));
        
        // regmi:
        update("regmi:dumdi|dum");
        assertTrue(highlighter.check(user, "dumdi"));
        assertTrue(highlighter.check(user, "dumDi"));
        assertTrue(highlighter.check(user, "dum"));
        assertFalse(highlighter.check(user, "hmm dum dum dumdidum"));
        assertTrue(highlighter.check(user, "Dum"));
        assertFalse(highlighter.check(user, "didadumdidum"));
        
        // regw:
        update("regw:word1|word2");
        assertTrue(highlighter.check(user, "word1"));
        assertTrue(highlighter.check(user, "hmm word1!"));
        assertTrue(highlighter.check(user, "hmm word2!"));
        assertTrue(highlighter.check(user, "word1!"));
        assertTrue(highlighter.check(user, "word2!"));
        assertFalse(highlighter.check(user, "wword1"));
        assertFalse(highlighter.check(user, "wword2"));
        assertFalse(highlighter.check(user, "word11"));
        assertFalse(highlighter.check(user, "word22"));
        
        // regwi:
        update("regwi:word1|word2");
        assertTrue(highlighter.check(user, "Word1"));
        assertTrue(highlighter.check(user, "hmm word1!"));
        assertTrue(highlighter.check(user, "hmm Word2!"));
        assertTrue(highlighter.check(user, "WORD1!"));
        assertTrue(highlighter.check(user, "WORD2!"));
        assertFalse(highlighter.check(user, "Wword1"));
        assertFalse(highlighter.check(user, "wword2"));
        assertFalse(highlighter.check(user, "word11"));
        assertFalse(highlighter.check(user, "Word22"));
        
        // Several
        update("user:testUser start:Test");
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "message test"));
        update("user:testUser test");
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "abc"));
        assertFalse(highlighter.check(user2, "test message"));
        
        update("wcs:Test", "re:.*(abc|dumdidum).*");
        assertTrue(highlighter.check(user, "Test message"));
        assertFalse(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "Testmessage"));
        assertTrue(highlighter.check(user, "Test abc message"));
        assertTrue(highlighter.check(user, "abc message"));
        assertTrue(highlighter.check(user, "j90awipfkdumdidumifwaef"));
        assertFalse(highlighter.check(user, "test"));
        
        update("wcs:S", "user:testuser");
        assertTrue(highlighter.check(user, "Hello S!"));
        assertTrue(highlighter.check(user, "Hello SSSsss!"));
        assertFalse(highlighter.check(user2, "Hello SSSsss!"));
        
        update("cat:testCat chan:testChannel");
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user2, "test"));
        ab.add("testUser2", "testCat");
        assertTrue(highlighter.check(user2, "test"));
        
        update("!chan:testChannel2 test");
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, "mäh"));
        assertFalse(highlighter.check(user3, "test"));

        // reuser:
        update("reuser:test.*");
        assertTrue(highlighter.check(user, "whatever"));
        assertTrue(highlighter.check(user2, "whatever"));
        assertTrue(highlighter.check(user3, "whatever"));
        
        update("reuser:");
        assertFalse(highlighter.check(user, "whatever"));
        assertFalse(highlighter.check(user2, "whatever"));
        assertFalse(highlighter.check(user3, "whatever"));
        
        update("reuser:test.*[0-2]");
        assertFalse(highlighter.check(user, "whatever"));
        assertTrue(highlighter.check(user2, "whatever"));
        assertFalse(highlighter.check(user4, "whatever"));
        
        // Color
        update("color:red testi", "test");
        assertTrue(highlighter.check(user, "test"));
        assertEquals(highlighter.getLastMatchColor(), null);
        assertTrue(highlighter.check(user, "testi"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED);
        assertFalse(highlighter.check(user2, "abc"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED); // Not reset because not matched again
        assertTrue(highlighter.check(user2, "test"));
        assertEquals(highlighter.getLastMatchColor(), null);
        
        // Highlight follow-up messages
        update("Test");
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, "mäh"));
        highlighter.setHighlightNextMessages(true);
        assertTrue(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user, "mäh"));
        highlighter.setHighlightNextMessages(false);
        
        update("color:red testi", "test");
        highlighter.setHighlightNextMessages(true);
        assertTrue(highlighter.check(user, "testi"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED);
        assertTrue(highlighter.check(user, "asdas"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED);
        assertTrue(highlighter.check(user2, "test"));
        assertEquals(highlighter.getLastMatchColor(), null);
        assertTrue(highlighter.check(user, "asdas"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED);
        assertTrue(highlighter.check(user2, "asdas"));
        assertEquals(highlighter.getLastMatchColor(), null);
        assertTrue(highlighter.check(user2, "testi"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED);
        assertTrue(highlighter.check(user2, "asdas"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED);
        highlighter.setHighlightNextMessages(false);
        
        // Highlight username
        update("");
        highlighter.setHighlightUsername(true);
        highlighter.setUsername("username");
        assertTrue(highlighter.check(user, "username"));
        assertTrue(highlighter.check(user, "hi username :)"));
        assertTrue(highlighter.check(user, "hi, username!"));
        assertTrue(highlighter.check(user, "Username!"));
        assertFalse(highlighter.check(user, "usernamee"));
        highlighter.setHighlightUsername(false);
        assertFalse(highlighter.check(user, "username"));
        highlighter.setHighlightUsername(true);
        assertTrue(highlighter.check(user, "username"));
        
        update("color:red testi", "test");
        highlighter.setHighlightNextMessages(true);
        assertTrue(highlighter.check(user, "username"));
        assertEquals(highlighter.getLastMatchColor(), null);
        assertTrue(highlighter.check(user, "asdas"));
        assertEquals(highlighter.getLastMatchColor(), null);
        assertTrue(highlighter.check(user2, "testi"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED);
        assertTrue(highlighter.check(user, "asdas"));
        assertEquals(highlighter.getLastMatchColor(), null);
        highlighter.setHighlightNextMessages(false);
    }
    
    @Test
    public void testStatusReq() {
        updateBlacklist();
        
        User broadcaster = new User("test", Room.createRegular("#test"));
        broadcaster.setBroadcaster(true);
        
        User normal = new User("test2", Room.createRegular("#test"));
        
        User modTurbo = new User("test3", Room.createRegular("#test"));
        modTurbo.setModerator(true);
        modTurbo.setTurbo(true);
        
        User admin = new User("test4", Room.createRegular("#test"));
        admin.setAdmin(true);
        
        User adminBroadcasterTurbo = new User("test5", Room.createRegular("#test"));
        adminBroadcasterTurbo.setAdmin(true);
        adminBroadcasterTurbo.setBroadcaster(true);
        adminBroadcasterTurbo.setTurbo(true);
        
        User staff = new User("test6", Room.createRegular("#test"));
        staff.setStaff(true);
        
        User subscriber = new User("test7", Room.createRegular("#test"));
        subscriber.setSubscriber(true);
        
        update("status:b");
        assertTrue(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertTrue(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        update("!status:b");
        assertFalse(highlighter.check(broadcaster, ""));
        assertTrue(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, ""));
        assertTrue(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertTrue(highlighter.check(staff, ""));
        assertTrue(highlighter.check(subscriber, ""));
        
        update("status:m");
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        update("status:abmf");
        assertTrue(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, ""));
        assertTrue(highlighter.check(admin, ""));
        assertTrue(highlighter.check(adminBroadcasterTurbo, ""));
        assertTrue(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        update("!status:bmaf");
        assertFalse(highlighter.check(broadcaster, ""));
        assertTrue(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertTrue(highlighter.check(subscriber, ""));
        
        update("status:a !status:b");
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertTrue(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        update("status:m !status:m");
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        update("status:t");
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertTrue(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        update("!status:t");
        assertTrue(highlighter.check(broadcaster, ""));
        assertTrue(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertTrue(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertTrue(highlighter.check(staff, ""));
        assertTrue(highlighter.check(subscriber, ""));
        
        update("status:s");
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertTrue(highlighter.check(subscriber, ""));
        
        // Test if it still works in combination with text
        update("status:smb test");
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        update("status:smb test");
        assertTrue(highlighter.check(broadcaster, "test"));
        assertFalse(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, "test"));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, "hello"));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        // Highlighter shouldn't take empty items
        update("");
        assertFalse(highlighter.check(broadcaster, "test"));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, "test"));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, "hello"));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        update();
        assertFalse(highlighter.check(broadcaster, "test"));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, "test"));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, "hello"));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        // Although HighlightItem alone would match everything on empty item
        Highlighter.HighlightItem item = new Highlighter.HighlightItem("");
        assertTrue(item.matchesAny("", null));
        assertTrue(item.matchesAny("abc", null));
        assertTrue(item.matches(Highlighter.HighlightItem.Type.REGULAR, "", broadcaster));
        assertTrue(item.matches(Highlighter.HighlightItem.Type.REGULAR, "", normal));
        assertFalse(item.matches(Highlighter.HighlightItem.Type.INFO, "", normal));
    }
    
    @Test
    public void testBlacklist() {
        updateBlacklist();
        update("test");
        assertTrue(highlighter.check(user, "Hello testi"));
        updateBlacklist("testi");
        assertFalse(highlighter.check(user, "Hello testi"));
        
        updateBlacklist();
        update("w:ROM");
        assertTrue(highlighter.check(user, "Heard of that nice ROM hack?"));
        assertFalse(highlighter.check(user, "Heard of that nice ROMhack?"));
        updateBlacklist("rom hack");
        assertFalse(highlighter.check(user, "Heard of that nice ROM hack?"));
        
        updateBlacklist();
        update("w:Prom");
        assertTrue(highlighter.check(user, "Heard of that nice Prom Hack?"));
        updateBlacklist("rom hack");
        assertTrue(highlighter.check(user, "Heard of that nice Prom Hack?"));
        
        updateBlacklist();
        update("josh");
        assertTrue(highlighter.check(user, "joshBarksAtKitty joshimuz"));
        assertTrue(highlighter.check(user, "joshBarksAtKitty joshBarksAtKitty"));
        updateBlacklist("joshBarksAtKitty");
        assertTrue(highlighter.check(user, "joshBarksAtKitty joshimuz"));
        assertFalse(highlighter.check(user, "joshBarksAtKitty joshBarksAtKitty"));
        updateBlacklist("start:joshBarksAtKitty");
        assertTrue(highlighter.check(user, "joshBarksAtKitty joshimuz"));
        assertTrue(highlighter.check(user, "joshBarksAtKitty joshBarksAtKitty"));
        
        update("josh");
        updateBlacklist("chan:testChannel joshBarksAtKitty");
        assertFalse(highlighter.check(user, "joshBarksAtKitty"));
        assertTrue(highlighter.check(user3, "joshBarksAtKitty"));
        
        updateBlacklist("chan:testChannel");
        assertFalse(highlighter.check(user, "joshBarksAtKitty"));
        assertTrue(highlighter.check(user3, "joshBarksAtKitty"));
        
        updateBlacklist("chan:testChannel reg:.*");
        assertFalse(highlighter.check(user, "joshBarksAtKitty"));
        assertTrue(highlighter.check(user3, "joshBarksAtKitty"));
        
        // Highlight username
        update();
        updateBlacklist();
        highlighter.setHighlightUsername(true);
        highlighter.setUsername("username");
        assertTrue(highlighter.check(user, "username"));
        assertTrue(highlighter.check(user, "hi username :)"));
        assertTrue(highlighter.check(user, "hi, username!"));
        assertTrue(highlighter.check(user, "Username!"));
        assertFalse(highlighter.check(user, "usernamee"));
        
        updateBlacklist("username!");
        assertTrue(highlighter.check(user, "username"));
        assertTrue(highlighter.check(user, "hi username :)"));
        assertFalse(highlighter.check(user, "hi, username!"));
        assertFalse(highlighter.check(user, "Username!"));
        assertFalse(highlighter.check(user, "usernamee"));
        
        updateBlacklist("username");
        assertFalse(highlighter.check(user, "username"));
        assertFalse(highlighter.check(user, "hi username :)"));
        assertFalse(highlighter.check(user, "hi, username!"));
        assertFalse(highlighter.check(user, "Username!"));
        assertFalse(highlighter.check(user, "usernamee"));
        
        updateBlacklist("user");
        assertTrue(highlighter.check(user, "username"));
        assertTrue(highlighter.check(user, "hi username :)"));
        assertTrue(highlighter.check(user, "hi, username!"));
        assertTrue(highlighter.check(user, "Username!"));
        assertFalse(highlighter.check(user, "usernamee"));
    }
    
    @Test
    public void testNew() {
        update();
        updateBlacklist();
        
        update("config:info abc");
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.INFO, "abc", null, ab, null));
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.REGULAR, "abc", null, ab, null));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", null, ab, null));
        update("abc");
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.INFO, "abc", null, ab, null));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", null, ab, null));
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.ANY, "", null, ab, null));
        
        update("config:info chan:joshimuz");
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", "#joshimuz", ab, null));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.INFO, "abc", "#joshimuz", ab, null));
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.REGULAR, "abc", "#joshimuz", ab, null));
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", "joshimuz", ab, null));
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", "#somechannel", ab, null));
        
        update("chan:testchannel");
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.REGULAR, "abc", "#testchannel", ab, null));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.REGULAR, "abc", "#testchannel", ab, user));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.REGULAR, "abc", null, ab, user));
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.REGULAR, "abc", "#somechannel", ab, user));
        
        update("chanCat:subonly");
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", "#testchannel", ab, null));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", null, null, null));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", null, ab, null));
        ab.add("#testchannel", "subonly");
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", "#testchannel", ab, null));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", null, null, user));
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", "#somechannel", null, user));
        
        update("config:firstmsg");
        assertTrue(highlighter.check(user, "abc"));
        user.addMessage("abc", true, null);
        assertFalse(highlighter.check(user, "abc"));
        assertTrue(highlighter.check(user2, "abc"));
        update("config:info,firstmsg");
        assertFalse(highlighter.check(user2, "abc"));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", null, ab, user2));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.INFO, "abc", null, ab, user2));
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.REGULAR, "abc", null, ab, user2));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", null, null, null));
        assertFalse(highlighter.check(Highlighter.HighlightItem.Type.ANY, "abc", null, ab, user));
        
        update("config:any");
        assertTrue(highlighter.check(user, "abc"));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.ANY, "", null, ab, user2));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.INFO, "", null, ab, user2));
        assertTrue(highlighter.check(Highlighter.HighlightItem.Type.REGULAR, "", null, ab, user2));
    }
    
}
