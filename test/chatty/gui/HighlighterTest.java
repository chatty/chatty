
package chatty.gui;

import chatty.Addressbook;
import chatty.Room;
import chatty.User;
import chatty.gui.Highlighter.HighlightItem;
import chatty.gui.Highlighter.HighlightItem.Type;
import chatty.util.irc.MsgTags;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
        highlighter = new Highlighter("test");
        Settings settings = new Settings("", null);
        settings.addBoolean("abSaveOnChange", false);
        ab = new Addressbook(null, null, settings);
        user.setAddressbook(ab);
        user2.setAddressbook(ab);
        user3.setAddressbook(ab);
        user4.setAddressbook(ab);
        ab.add("testUser", "testCat,testCat2");
        ab.add("testUser3", "testCat2");
        ab.add("testUser2", "testCat3");
        Map<String, String> badges = new HashMap<>();
        badges.put("vip", "1");
        badges.put("subscriber", "24");
        user.setTwitchBadges(badges);
        Map<String, String> badges2 = new HashMap<>();
        badges2.put("subscriber", "12");
        user2.setTwitchBadges(badges2);
    }
    
    private void update(String... items) {
        highlighter.update(Arrays.asList(items));
    }
    
    private void updateBlacklist(String... items) {
        highlighter.updateBlacklist(Arrays.asList(items));
    }

    @Test
    public void test() {
        update();
        updateBlacklist();
        
        // Regular
        assertFalse(highlighter.check(user, "test message"));
        
        update("test");
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "abc"));
        
        update(" test");
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "abc"));
        
        update("");
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user3, ""));
        
        update("mäh");
        assertTrue(highlighter.check(user, "mäh"));
        assertTrue(highlighter.check(user, "Mäh"));
        assertTrue(highlighter.check(user, "MÄH"));
        
        update("   mäh       ");
        assertTrue(highlighter.check(user, "mäh"));
        
        update("\"");
        assertTrue(highlighter.check(user, "\""));
        assertTrue(highlighter.check(user, "blah\" blubb"));
        assertFalse(highlighter.check(user, "mäh"));
        
        update("abc\\blah");
        assertTrue(highlighter.check(user, "abc\\blah"));
        assertFalse(highlighter.check(user, "mäh"));
        
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
        
        update("start:\\abc");
        assertTrue(highlighter.check(user, "\\abc"));
        assertFalse(highlighter.check(user, "mäh"));
        
        // startw:
        update("startw:!bet ");
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, " !bet test"));
        assertFalse(highlighter.check(user, "!bett"));
        assertTrue(highlighter.check(user3, "!bet abc"));
        assertTrue(highlighter.check(user3, "!bet,abc"));
        assertTrue(highlighter.check(user3, "!bet.abc"));
        assertTrue(highlighter.check(user3, "!bet. abc"));
        assertFalse(highlighter.check(user3, "!bet_abc"));
        
        update("startw: !bet ");
        assertFalse(highlighter.check(user, "!bet test"));
        assertTrue(highlighter.check(user, " !bet test"));
        assertTrue(highlighter.check(user, " !bet"));
        
        update("startw:\\abc");
        assertTrue(highlighter.check(user, "\\abc"));
        assertTrue(highlighter.check(user, "\\abc d"));
        assertFalse(highlighter.check(user, "abc"));
        assertFalse(highlighter.check(user, "abcd"));
        
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
        
        update("reg:\"abc\"");
        assertTrue(highlighter.check(user, "test \"abc\" test"));
        assertFalse(highlighter.check(user, "test abc test"));
        
        update("reg:\\\\");
        assertTrue(highlighter.check(user, "test\\test"));
        assertFalse(highlighter.check(user, "test abc test"));
        
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
        
        update("cat:testCat  chan:testChannel");
        assertTrue(highlighter.check(user, "test"));
        
        update("cat:testCat,");
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user4, "test"));
        
        update("cat:testCat");
        assertFalse(highlighter.check(null, "test"));
        
        update("cat:testCat2");
        assertTrue(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user4, "test"));
        
        update("cat:testCat cat:testCat2");
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user4, "test"));
        
        update("cat:testCat,testCat2");
        assertTrue(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user4, "test"));
        
        update("!cat:abc");
        assertTrue(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user4, "test"));
        
        update("!cat:testCat");
        assertFalse(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user4, "test"));
        
        update("!cat:testCat,testCat2");
        assertFalse(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user4, "test"));
        
        update("!cat:testCat !cat:testCat2");
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user4, "test"));
        
        update("!cat:testCat,testCat2,testCat3");
        assertTrue(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user4, "test"));
        
        update("!cat:testCat2");
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user4, "test"));
        
        update("!cat:testCat2,testCat3");
        assertTrue(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user4, "test"));
        assertTrue(highlighter.check(user3, "test"));
        
        update("!cat:testCat2 !cat:testCat3");
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user4, "test"));
        assertFalse(highlighter.check(user3, "test"));
        
        update("!cat:testCat !cat:testCat3");
        assertFalse(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user4, "test"));
        assertFalse(highlighter.check(user3, "test"));
        
        update("chan:testChannel");
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user3, "test"));
        
        update("chan:testChannel,testChannel2");
        assertTrue(highlighter.check(user, "test"));
        assertTrue(highlighter.check(user3, "test"));
        
        update("!chan:testChannel,testChannel2");
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user3, "test"));
        
        update("!chan:testChannel2 test");
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, "mäh"));
        assertFalse(highlighter.check(user3, "test"));
        
        // user:
        update("user:abc");
        assertFalse(highlighter.check(user, "mäh"));
        update("user:testUser");
        assertTrue(highlighter.check(user, "mäh"));
        assertFalse(highlighter.check(null, "mäh"));
        update("config:info user:testUser");
        assertFalse(highlighter.check(Type.INFO, "abc", null, ab, null, null, MsgTags.EMPTY));

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
        assertFalse(highlighter.check(null, "whatever"));
        
        // Badge
        update("config:b|vip");
        assertTrue(highlighter.check(user, "whatever"));
        assertFalse(highlighter.check(user2, "whatever"));
        
        update("config:b|subscriber/12");
        assertFalse(highlighter.check(user, "whatever"));
        assertTrue(highlighter.check(user2, "whatever"));
        
        update("config:b|subscriber");
        assertTrue(highlighter.check(user, "whatever"));
        assertTrue(highlighter.check(user2, "whatever"));
        
        update("config:b|subscriber/12 color:red", "config:b|vip color:blue");
        assertTrue(highlighter.check(user, "whatever"));
        assertEquals(highlighter.getLastMatchColor(), Color.BLUE);
        assertTrue(highlighter.check(user2, "whatever"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED);
        
        update("config:b|subscriber/12,b|vip");
        assertTrue(highlighter.check(user, "whatever"));
        assertTrue(highlighter.check(user2, "whatever"));
        
        update("config:b|subscriber/12 config:b|vip");
        assertFalse(highlighter.check(user, "whatever"));
        assertFalse(highlighter.check(user2, "whatever"));
        
        update("config:b|subscriber/24 config:b|vip");
        assertTrue(highlighter.check(user, "whatever"));
        assertFalse(highlighter.check(user2, "whatever"));
        
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
        
        // Status with no user
        assertFalse(highlighter.check(null, "abc"));
        
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
        assertTrue(item.matches(Highlighter.HighlightItem.Type.REGULAR, "", broadcaster, null, MsgTags.EMPTY));
        assertTrue(item.matches(Highlighter.HighlightItem.Type.REGULAR, "", normal, null, MsgTags.EMPTY));
        assertFalse(item.matches(Highlighter.HighlightItem.Type.INFO, "", normal, null, MsgTags.EMPTY));
        
        // Several status prefixes
        update("status:bm status:s");
        assertFalse(highlighter.check(broadcaster, "test"));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, "test"));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, "hello"));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        User modSub = new User("abc", Room.EMPTY);
        modSub.setModerator(true);
        modSub.setSubscriber(true);
        User broadcasterSub = new User("abc", Room.createRegular("#abc"));
        broadcasterSub.setBroadcaster(true);
        broadcasterSub.setSubscriber(true);
        assertTrue(highlighter.check(modSub, ""));
        assertTrue(highlighter.check(broadcasterSub, ""));
        
        // mystatus:
        update("mystatus:b");
        assertTrue(highlighter.check(Type.REGULAR, "", null, null, normal, broadcaster, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.REGULAR, "", null, null, normal, normal, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.REGULAR, "", null, null, normal, null, MsgTags.EMPTY));
        
        update("!mystatus:b");
        assertTrue(highlighter.check(Type.REGULAR, "", null, null, broadcaster, normal, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.REGULAR, "", null, null, normal, broadcaster, MsgTags.EMPTY));
        
        update("mystatus:m");
        assertFalse(highlighter.check(Type.REGULAR, "", null, null, normal, broadcaster, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.REGULAR, "", null, null, normal, modSub, MsgTags.EMPTY));
        
        update("mystatus:M");
        assertTrue(highlighter.check(Type.REGULAR, "", null, null, normal, broadcaster, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.REGULAR, "", null, null, normal, modSub, MsgTags.EMPTY));
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
        
        // Blacklist block
        update("user:testuser");
        updateBlacklist("abc");
        assertTrue(highlighter.check(user, "abc"));
        assertTrue(highlighter.check(user, "blah abc"));
        
        update("user:testuser");
        updateBlacklist("config:block abc");
        assertFalse(highlighter.check(user, "abc"));
        assertFalse(highlighter.check(user, "blah abc"));
        
        update("user:testuser test");
        updateBlacklist("config:block abc");
        assertFalse(highlighter.check(user, "abc test"));
        assertFalse(highlighter.check(user, "blah abc test"));
        
        update("user:testuser test");
        updateBlacklist("config:block");
        assertFalse(highlighter.check(user, "abc test"));
        assertFalse(highlighter.check(user, "blah abc test"));
        
        update("user:testuser");
        updateBlacklist("config:block abcd");
        assertTrue(highlighter.check(user, "abc"));
        assertTrue(highlighter.check(user, "blah abc"));
        
        update("user:testuser", "abc");
        updateBlacklist("config:block !start:!quote");
        assertFalse(highlighter.check(user, "abc"));
        assertFalse(highlighter.check(user, "blah abc"));
        assertTrue(highlighter.check(user, "!quote blah abc"));
    }
    
    @Test
    public void testBlacklistPrefix() {
        update();
        updateBlacklist();
        
        update("blacklist:\"!bet all\" start:!bet");
        assertFalse(highlighter.check(user, "!be"));
        assertTrue(highlighter.check(user, "!bet"));
        assertTrue(highlighter.check(user, "!bet 100"));
        assertFalse(highlighter.check(user, "!bet all"));
        assertFalse(highlighter.check(user, "!bet all2"));
        assertFalse(highlighter.check(user, " !bet all"));
        assertFalse(highlighter.check(user, " !bet"));
        
        update("blacklist:regi:[a-z]+ w:cake");
        assertFalse(highlighter.check(user, "!bet"));
        assertFalse(highlighter.check(user, "cake"));
        assertFalse(highlighter.check(user, "cheesecake"));
        assertFalse(highlighter.check(user, "Cake"));
        
        update("blacklist:reg:[a-z]+ w:cake");
        assertFalse(highlighter.check(user, "!bet"));
        assertFalse(highlighter.check(user, "cake"));
        assertFalse(highlighter.check(user, "cheesecake"));
        assertTrue(highlighter.check(user, "Cake"));
        
        update("blacklist:cheesecake cake");
        assertTrue(highlighter.check(user, "cake"));
        assertFalse(highlighter.check(user, "cheesecake"));
        
        update("blacklist:applepie blacklist:cheesecake cake");
        assertTrue(highlighter.check(user, "cake"));
        assertFalse(highlighter.check(user, "cheesecake"));
        
        update("blacklist:cheesecake blacklist:applepie cake");
        assertTrue(highlighter.check(user, "cake"));
        assertFalse(highlighter.check(user, "cheesecake"));
        
        // Blacklist prefix only gets text matches, no text match means it spans all text
        update("blacklist:config:info cake");
        assertFalse(highlighter.check(user, "cake"));
        assertFalse(highlighter.check(user, "cheesecake"));
        
        // When there is no text match to blacklist, it doesn't have any effect though
        update("blacklist:config:info user:testUSER");
        assertTrue(highlighter.check(user, "cake"));
        assertTrue(highlighter.check(user, "cheesecake"));
    }
    
    @Test
    public void testNegated() {
        updateBlacklist();
        
        update("!start:!bet all");
        assertTrue(highlighter.check(user, "!bet"));
        assertFalse(highlighter.check(user, "!bet all"));
        assertFalse(highlighter.check(user, "!bet alll"));
        
        update("!w:Testi");
        assertTrue(highlighter.check(user, "Test"));
        assertFalse(highlighter.check(user, "Testi"));
        assertTrue(highlighter.check(user, "Testii"));
    }
    
    @Test
    public void testAdditional() {
        updateBlacklist();
        
        update("+wcs:Test Abc");
        assertTrue(highlighter.check(user, "Test Abc"));
        assertTrue(highlighter.check(user, "Test abc"));
        assertTrue(highlighter.check(user, "Test Afewfawef Abc"));
        assertFalse(highlighter.check(user, "Test"));
        assertFalse(highlighter.check(user, "test Abc"));
        assertTrue(highlighter.check(user, "jfpoeajwf Test iojiofawefabc"));
        
        update("+wcs:\"Test Abc\"");
        assertTrue(highlighter.check(user, "Test Abc"));
        assertFalse(highlighter.check(user, "Test abc"));
        assertFalse(highlighter.check(user, "Test Afewfawef Abc"));
        assertFalse(highlighter.check(user, "Test"));
        assertFalse(highlighter.check(user, "test Abc"));
        assertFalse(highlighter.check(user, "jfpoeajwf Test iojiofawefabc"));
        
        update("+!w:Testi w:Abc");
        assertTrue(highlighter.check(user, "Test Abc"));
        assertTrue(highlighter.check(user, "Testii Abc"));
        assertFalse(highlighter.check(user, "Testi Abc"));
        assertFalse(highlighter.check(user, "Testi"));
        assertFalse(highlighter.check(user, "Testi Abcd"));
    }
    
    @Test
    public void testNew() {
        update();
        updateBlacklist();
        
        update("config:info abc");
        assertTrue(highlighter.check(Type.INFO, "abc", null, ab, null, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.REGULAR, "abc", null, ab, null, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.ANY, "abc", null, ab, null, null, MsgTags.EMPTY));
        update("abc");
        assertFalse(highlighter.check(Type.INFO, "abc", null, ab, null, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.ANY, "abc", null, ab, null, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.ANY, "", null, ab, null, null, MsgTags.EMPTY));
        
        update("config:info chan:joshimuz");
        assertTrue(highlighter.check(Type.ANY, "abc", "#joshimuz", ab, null, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.INFO, "abc", "#joshimuz", ab, null, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.REGULAR, "abc", "#joshimuz", ab, null, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.ANY, "abc", "joshimuz", ab, null, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.ANY, "abc", "#somechannel", ab, null, null, MsgTags.EMPTY));
        
        update("chan:testchannel");
        assertTrue(highlighter.check(Type.REGULAR, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.REGULAR, "abc", "#testchannel", ab, user, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.REGULAR, "abc", null, ab, user, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.REGULAR, "abc", "#somechannel", ab, user, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.REGULAR, "abc", null, null, null, null, MsgTags.EMPTY));
        
        // Channel categories / missing user or ab
        update("chanCat:subonly");
        assertFalse(highlighter.check(Type.ANY, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        // No ab/user given
        assertFalse(highlighter.check(Type.ANY, "abc", null, null, null, null, MsgTags.EMPTY));
        // No user, but ab given
        assertFalse(highlighter.check(Type.ANY, "abc", null, ab, null, null, MsgTags.EMPTY));
        ab.add("#testchannel", "subonly");
        // Ab/channel given, but no user
        assertTrue(highlighter.check(Type.ANY, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        // Ab given, but no user/channel
        assertFalse(highlighter.check(Type.ANY, "abc", null, ab, null, null, MsgTags.EMPTY));
        // Gets ab and chan from user
        assertTrue(highlighter.check(Type.ANY, "abc", null, null, user, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.ANY, "abc", "#somechannel", null, user, null, MsgTags.EMPTY));
        
        update("!chanCat:subonly");
        assertFalse(highlighter.check(Type.ANY, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.ANY, "abc", "#testchannel2", ab, null, null, MsgTags.EMPTY));
        
        // Either of the categories
        update("chanCat:subonly,modding");
        assertTrue(highlighter.check(Type.ANY, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.ANY, "abc", "#testchannel2", ab, null, null, MsgTags.EMPTY));
        
        // Either of the categories (category added)
        ab.add("#testchannel2", "modding");
        update("chanCat:subonly,modding");
        assertTrue(highlighter.check(Type.ANY, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.ANY, "abc", "#testchannel2", ab, null, null, MsgTags.EMPTY));
        
        // Not one of the categories
        update("!chanCat:subonly,modding");
        assertTrue(highlighter.check(Type.ANY, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.ANY, "abc", "#testchannel2", ab, null, null, MsgTags.EMPTY));
        
        // Not both categories
        update("!chanCat:subonly !chanCat:modding");
        assertFalse(highlighter.check(Type.ANY, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.ANY, "abc", "#testchannel2", ab, null, null, MsgTags.EMPTY));
        
        // One has both categories
        ab.add("#testchannel2", "subonly");
        update("!chanCat:subonly,modding");
        assertTrue(highlighter.check(Type.ANY, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.ANY, "abc", "#testchannel2", ab, null, null, MsgTags.EMPTY));
        // Both have both categories
        ab.add("#testchannel", "modding");
        assertFalse(highlighter.check(Type.ANY, "abc", "#testchannel", ab, null, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.ANY, "abc", "#testchannel2", ab, null, null, MsgTags.EMPTY));
        
        update("config:firstmsg");
        assertTrue(highlighter.check(user, "abc"));
        user.addMessage("abc", true, null);
        assertFalse(highlighter.check(user, "abc"));
        assertTrue(highlighter.check(user2, "abc"));
        update("config:info,firstmsg");
        assertFalse(highlighter.check(user2, "abc"));
        assertTrue(highlighter.check(Type.ANY, "abc", null, ab, user2, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.INFO, "abc", null, ab, user2, null, MsgTags.EMPTY));
        assertFalse(highlighter.check(Type.REGULAR, "abc", null, ab, user2, null, MsgTags.EMPTY));
        // No user given
        assertFalse(highlighter.check(Type.ANY, "abc", null, null, null, null, MsgTags.EMPTY));
        // User with a message already added (checks 0 since message is normally added after checking)
        assertFalse(highlighter.check(Type.ANY, "abc", null, ab, user, null, MsgTags.EMPTY));
        
        update("config:any");
        assertTrue(highlighter.check(user, "abc"));
        assertTrue(highlighter.check(Type.ANY, "", null, ab, user2, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.INFO, "", null, ab, user2, null, MsgTags.EMPTY));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user2, null, MsgTags.EMPTY));
        
        update("config:hl");
        assertFalse(highlighter.check(user, "abc"));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message")));
        
        // Tags prefix
        update("config:t|msg-id=highlighted-message");
        assertFalse(highlighter.check(user, "abc"));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message")));
        
        // Several tags
        update("config:t|msg-id,t|subscriber=1");
        assertFalse(highlighter.check(user, "abc"));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message")));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("subscriber", "1")));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message", "subscriber", "0")));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message", "subscriber", "1")));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("subscriber", "0")));
        
        update("config:t|msg-id config:t|subscriber=1");
        assertFalse(highlighter.check(user, "abc"));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message")));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("subscriber", "1")));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message", "subscriber", "0")));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message", "subscriber", "1")));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("subscriber", "0")));
        
        update("config:t|msg-id config:t|subscriber=1,t|mod=1");
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message", "mod", "1")));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message", "mod", "0")));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message", "mod", "1", "subscriber", "1")));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("msg-id", "highlighted-message")));
        
        // Tags prefix with regex value matching
        update("config:t|color=reg:(#00FF7F|#FF00FF)");
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("color", "#00FF7F")));
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("color", "#FF00FF")));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("color", "#123456")));
        
        // New list parsing/adding space
        update("config:t|test=\"abc,lol\"");
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("test", "abc,lol")));
        update("config:t|test=\"a,b,c\",!notify");
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("test", "a,b,c")));
        update("config:t|test=\"abc lol\"");
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("test", "abc lol")));
        update("config:t|test=reg:abc\" \"lol");
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("test", "abc lol")));
        update("config:t|test=reg:\"abc \\w{2,3}\"");
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("test", "abc lol")));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("test", "abc rofl")));
        assertFalse(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("test", "abclol")));
        update("config:t|test=reg:\"abc \\w{2,3}\"");
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("test", "abc lol")));
        update("config:t|test=abc\\slol");
        assertTrue(highlighter.check(Type.REGULAR, "", null, ab, user, null, MsgTags.create("test", "abc\\slol")));
    }
    
    @Test
    public void testCC() {
        update();
        updateBlacklist();
        
        HighlightItem.setGlobalPresets(HighlightItem.makePresets(Arrays.asList(new String[]{
            "t \\Qabc$1\\E",
            "t2 _t",
            "_t abc$1",
            "_f $replace($1-,\\\\s+,_,reg)",
            "cc cc:config:silent"
        })));
        
        update("cc:regm:$(t)$(_t)");
        assertTrue(highlighter.check(user, "abc$1abc"));
        assertFalse(highlighter.check(user, "abc$1abc2"));
        
        update("cc:regm:$(t)$(_t,test)");
        assertTrue(highlighter.check(user, "abc$1abctest"));
        assertFalse(highlighter.check(user, "abc$1abc2"));
        
        update("ccf:_f|regm:abc blah");
        assertTrue(highlighter.check(user, "abc_blah"));
        assertFalse(highlighter.check(user, "abc$1abc2"));
        
        update("cc2:'|regm:$(t)\\w");
        assertTrue(highlighter.check(user, "abc$1d"));
        
        update("cc2:'§|regm:§(t)\\w", "cc2:'§|regm:\\Q$(t)\\E\\s");
        assertTrue(highlighter.check(user, "abc$1d"));
        assertFalse(highlighter.check(user, "abc$1 "));
        assertTrue(highlighter.check(user, "$(t) "));
        
        update("preset:t", "preset:_t|123");
        assertTrue(highlighter.check(user, "\\Qabc$1\\E"));
        assertTrue(highlighter.check(user, "abc123"));
        assertFalse(highlighter.check(user, "abc"));
        
        update("preset:t,_t|123");
        assertTrue(highlighter.check(user, "\\Qabc$1\\E abc123"));
        assertFalse(highlighter.check(user, "abc123"));
        assertFalse(highlighter.check(user, "abc"));
        
        update("cc:preset:$(t2)|123");
        assertTrue(highlighter.check(user, "abc123"));
        assertFalse(highlighter.check(user, "abc"));
        
        update("preset:cc regm:$(t)");
        assertTrue(highlighter.check(user, "abc$1"));
    }
    
    @Test
    public void testNote() {
        update();
        updateBlacklist();
        
        update("n:abc blah");
        assertTrue(highlighter.check(user, "blah"));
        
        update("n:\"abc blah\"");
        assertTrue(highlighter.check(user, "blah"));
        assertTrue(highlighter.check(user, "fawefeawf"));
        
        update("n:\"abc blah\" cs:ABC");
        assertTrue(highlighter.check(user, "blahABCfeawfeawf"));
        assertFalse(highlighter.check(user, "blah"));
        assertFalse(highlighter.check(user, "fawefeawf"));
        
        update("cs:ABC n:\"abc blah\"");
        assertTrue(highlighter.check(user, "blahABC n:\"abc blah\"feawfeawf"));
        assertFalse(highlighter.check(user, "blahABC n:\"abcblah\"feawfeawf"));
        assertFalse(highlighter.check(user, "blahABCfeawfeawf"));
        assertFalse(highlighter.check(user, "blah"));
        assertFalse(highlighter.check(user, "fawefeawf"));
    }
    
}
