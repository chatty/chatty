
package chatty.util.api;

import chatty.Helper;
import chatty.util.api.usericons.Usericon;
import chatty.util.api.usericons.UsericonFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class BadgeManager {
    
    private static final Logger LOGGER = Logger.getLogger(BadgeManager.class.getName());
    
    private final TwitchApi api;
    
    private boolean globalBadgesRequested = false;
    private final Set<String> roomsRequested = Collections.synchronizedSet(new HashSet<String>());
    
    public BadgeManager(TwitchApi api) {
        this.api = api;
    }
    
    public void requestGlobalBadges(boolean forceRefresh) {
        if (!globalBadgesRequested || forceRefresh) {
            globalBadgesRequested = true;
            api.requests.requestGlobalBadges();
        }
    }
    
    public void requestBadges(String channel, boolean forceRefresh) {
        final String room = Helper.toStream(channel);
        if (!roomsRequested.contains(room) || forceRefresh) {
            roomsRequested.add(room);
            
            api.waitForUserId(r -> {
                api.requests.requestRoomBadges(r.getId(room), room);
            }, room);
        }
    }
    
    public List<Usericon> handleGlobalBadgesResult(String json) {
        return parseBadges(json, null);
    }
    
    public List<Usericon> handleRoomBadgesResult(String json, String room) {
        return parseBadges(json, room);
    }
    
    private static List<Usericon> parseBadges(String json, String room) {
        List<Usericon> result = new ArrayList<>();
        if (json == null) {
            return result;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            JSONObject badges = (JSONObject)root.get("badge_sets");
            if (badges != null) {
                for (Object key : badges.keySet()) {
                    JSONObject data = (JSONObject)badges.get(key);
                    
                    String id = (String)key;
                    JSONObject versions = (JSONObject)data.get("versions");
                    if (versions != null) {
                        parseBadgeVersions(result, versions, room, id);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing badges: "+ex);
        }
        return result;
    }
    
    private static void parseBadgeVersions(List<Usericon> result,
                JSONObject data, String room, String id) {
        for (Object key : data.keySet()) {
            JSONObject versionData = (JSONObject) data.get(key);

            String version = (String) key;
            String url = (String) versionData.get("image_url_1x");
            String title = (String) versionData.get("title");
            String description = (String) versionData.get("description");
            String clickUrl = (String) versionData.get("click_url");

            if (id != null && version != null && url != null) {
                Usericon icon = UsericonFactory.createTwitchBadge(id, version, 
                        url, room, title, description, clickUrl);
                if (icon != null) {
                    result.add(icon);
                }
            }
        }
    }
    
}
