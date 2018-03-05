
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
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void test() {
        assertFalse(highlighter.check(user, "test message"));
        highlighter.update(Arrays.asList(new String[]{"test"}));
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "abc"));
        highlighter.update(Arrays.asList(new String[]{"cs:Test"}));
        assertTrue(highlighter.check(user, " Test "));
        assertFalse(highlighter.check(user, "testi"));
        highlighter.update(Arrays.asList(new String[]{"start:Test"}));
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "message test"));
        highlighter.update(Arrays.asList(new String[]{"user:testUser start:Test"}));
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "message test"));
        highlighter.update(Arrays.asList(new String[]{"user:testUser test"}));
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "abc"));
        assertFalse(highlighter.check(user2, "test message"));
        highlighter.update(Arrays.asList(new String[]{"w:Test"}));
        assertTrue(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "testmessage"));
        highlighter.update(Arrays.asList(new String[]{"wcs:Test", "re:.*(abc|dumdidum).*"}));
        assertTrue(highlighter.check(user, "Test message"));
        assertFalse(highlighter.check(user, "test message"));
        assertFalse(highlighter.check(user, "Testmessage"));
        assertTrue(highlighter.check(user, "Test abc message"));
        assertTrue(highlighter.check(user, "abc message"));
        assertTrue(highlighter.check(user, "j90awipfkdumdidumifwaef"));
        assertFalse(highlighter.check(user, "test"));
        
        highlighter.update(Arrays.asList(new String[]{"wcs:S", "user:testuser"}));
        assertTrue(highlighter.check(user, "Hello S!"));
        assertTrue(highlighter.check(user, "Hello SSSsss!"));
        assertFalse(highlighter.check(user2, "Hello SSSsss!"));
        
        highlighter.update(Arrays.asList(new String[]{"cat:testCat chan:testChannel"}));
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user2, "test"));
        ab.add("testUser2", "testCat");
        assertTrue(highlighter.check(user2, "test"));
        
        highlighter.update(Arrays.asList(new String[]{"!chan:testChannel2 test"}));
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, "mäh"));
        assertFalse(highlighter.check(user3, "test"));
        
        highlighter.update(Arrays.asList(new String[]{""}));
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user3, ""));
        
        highlighter.update(Arrays.asList(new String[]{"start:!bet"}));
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, " !bet test"));
        assertTrue(highlighter.check(user, "!bett"));
        assertTrue(highlighter.check(user3, "!bet abc"));
        
        highlighter.update(Arrays.asList(new String[]{"start:!bet "}));
        assertFalse(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, " !bet test"));
        assertTrue(highlighter.check(user, "!bett"));
        assertTrue(highlighter.check(user3, "!bet abc"));
        
        // Color
        highlighter.update(Arrays.asList(new String[]{"color:red testi", "test"}));
        assertTrue(highlighter.check(user, "test"));
        assertEquals(highlighter.getLastMatchColor(), null);
        assertTrue(highlighter.check(user, "testi"));
        assertEquals(highlighter.getLastMatchColor(), Color.RED);
        assertFalse(highlighter.check(user2, "abc"));
        assertEquals(highlighter.getLastMatchColor(), null);
        assertTrue(highlighter.check(user2, "test"));
        assertEquals(highlighter.getLastMatchColor(), null);
        
        highlighter.update(Arrays.asList(new String[]{"Test"}));
        assertTrue(highlighter.check(user, "test"));
        assertFalse(highlighter.check(user, "mäh"));
        highlighter.setHighlightNextMessages(true);
        assertTrue(highlighter.check(user, "mäh"));
        highlighter.setHighlightNextMessages(false);
    }
    
    @Test
    public void testStatusReq() {
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
        
        highlighter.update(Arrays.asList(new String[]{"status:b"}));
        assertTrue(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertTrue(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"!status:b"}));
        assertFalse(highlighter.check(broadcaster, ""));
        assertTrue(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, ""));
        assertTrue(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertTrue(highlighter.check(staff, ""));
        assertTrue(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"status:m"}));
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"status:abmf"}));
        assertTrue(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, ""));
        assertTrue(highlighter.check(admin, ""));
        assertTrue(highlighter.check(adminBroadcasterTurbo, ""));
        assertTrue(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"!status:bmaf"}));
        assertFalse(highlighter.check(broadcaster, ""));
        assertTrue(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertTrue(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"status:a !status:b"}));
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertTrue(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"status:m !status:m"}));
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"status:t"}));
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertTrue(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"!status:t"}));
        assertTrue(highlighter.check(broadcaster, ""));
        assertTrue(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertTrue(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertTrue(highlighter.check(staff, ""));
        assertTrue(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"status:s"}));
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertTrue(highlighter.check(subscriber, ""));
        
        // Test if it still works in combination with text
        highlighter.update(Arrays.asList(new String[]{"status:smb test"}));
        assertFalse(highlighter.check(broadcaster, ""));
        assertFalse(highlighter.check(normal, ""));
        assertFalse(highlighter.check(modTurbo, ""));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, ""));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
        
        highlighter.update(Arrays.asList(new String[]{"status:smb test"}));
        assertTrue(highlighter.check(broadcaster, "test"));
        assertFalse(highlighter.check(normal, ""));
        assertTrue(highlighter.check(modTurbo, "test"));
        assertFalse(highlighter.check(admin, ""));
        assertFalse(highlighter.check(adminBroadcasterTurbo, "hello"));
        assertFalse(highlighter.check(staff, ""));
        assertFalse(highlighter.check(subscriber, ""));
    }
    
}
