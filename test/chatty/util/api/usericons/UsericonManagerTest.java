
package chatty.util.api.usericons;

import chatty.Room;
import chatty.User;
import chatty.gui.MainGui;
import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class UsericonManagerTest {
    
    @Test
    public void test() {
        Settings settings = new Settings(null, null);
        settings.addBoolean("ffzModIcon", false);
        settings.addBoolean("customUsericonsEnabled", false);
        settings.addList("customUsericons", new ArrayList<>(), Setting.LIST);
        UsericonManager m = new UsericonManager(settings);
        Set<String> usernames = new HashSet<>();
        Set<String> userids = new HashSet<>();
        User user = new User("test", Room.EMPTY);
        
        //                                         id/name  id    name    -
        // Empty names/ids
        testThirdParty(m, usernames, userids, user, false, false, false, true);
        
        // Only name
        usernames.add("test");
        testThirdParty(m, usernames, userids, user, true, false, true, true);
        
        // Also id
        userids.add("abc");
        testThirdParty(m, usernames, userids, user, true, false, true, true);
        
        // Wrong id
        user.setId("123");
        testThirdParty(m, usernames, userids, user, true, false, true, true);
        
        // Correct id
        user.setId("abc");
        testThirdParty(m, usernames, userids, user, true, true, true, true);
        
        // No names, but id
        usernames.clear();
        testThirdParty(m, usernames, userids, user, true, true, false, true);
    }
    
    private void testThirdParty(UsericonManager m, Set<String> usernames, Set<String> userids, User user, boolean... results) {
        List<Usericon> thirdParty = setThirdParty(m, usernames, userids);
        List<Usericon> badges = m.getBadges(new HashMap<>(), user, false, false, false);
        for (int i=0;i<results.length;i++) {
            if (results[i]) {
                assertTrue("Badge "+i, badges.contains(thirdParty.get(i)));
            }
            else {
                assertFalse("Badge "+i, badges.contains(thirdParty.get(i)));
            }
        }
    }
    
    private List<Usericon> setThirdParty(UsericonManager m, Set<String> usernames, Set<String> userids) {
        String url = MainGui.class.getResource("star.png").toString();
        List<Usericon> thirdParty = new ArrayList<>();
        thirdParty.add(UsericonFactory.createThirdParty("test", "1", url, "Title", null, null, usernames, userids, ""));
        thirdParty.add(UsericonFactory.createThirdParty("test", "2", url, "Title", null, null, null, userids, ""));
        thirdParty.add(UsericonFactory.createThirdParty("test", "3", url, "Title", null, null, usernames, null, ""));
        thirdParty.add(UsericonFactory.createThirdParty("test", "4", url, "Title", null, null, null, null, ""));
        m.setThirdPartyIcons(thirdParty);
        return thirdParty;
    }
    
}
