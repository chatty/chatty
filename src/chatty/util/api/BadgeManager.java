
package chatty.util.api;

import chatty.Helper;
import chatty.util.Debugging;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.usericons.Usericon;
import chatty.util.api.usericons.UsericonFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
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
            JSONArray badges = (JSONArray)root.get("data");
            if (badges != null) {
                for (Object entry : badges) {
                    JSONObject data = (JSONObject) entry;
                    
                    String id = JSONUtil.getString(data, "set_id");
                    JSONArray versions = (JSONArray)data.get("versions");
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
                JSONArray data, String room, String id) {
        for (Object entry : data) {
            JSONObject versionData = (JSONObject) entry;

            String version = JSONUtil.getString(versionData, "id");
            String url = JSONUtil.getString(versionData, "image_url_1x");
            String url2 = JSONUtil.getString(versionData, "image_url_2x");
            String clickUrl = JSONUtil.getString(versionData, "click_url", "");
            String title;
            String description = "";
            /**
             * Even after title/description fields were added to the API these
             * custom created ones seem fine (for subscribers it even seems
             * better because it includes the Tier, assuming that works
             * correcly).
             */
            switch (id) {
                case "subscriber":
                    title = makeSubscriberTitle(version);
                    break;
                case "bits":
                    title = makeBitsTitle(version);
                    break;
                case "sub-gifter":
                    title = makeSubGifterTitle(version);
                    break;
                default:
                    title = JSONUtil.getString(versionData, "title");
                    description = JSONUtil.getString(versionData, "description");
            }
            if (Objects.equals(title, description)) {
                description = "";
            }

            if (id != null && version != null && url != null) {
                Usericon icon = UsericonFactory.createTwitchBadge(id, version, 
                        url, url2, room, title, description, clickUrl);
                if (icon != null) {
                    result.add(icon);
                    Debugging.println("badgetitles", "%s/%s %s", id, version, title);
                }
            }
        }
    }
    
    public static String makeSubscriberTitle(String version) {
        String title = "Subscriber";
        String tier = "0";
        String months = version;
        if (version.length() == 4) {
            tier = version.substring(0, 1);
            months = version.substring(1);
        }
        try {
            int monthsNum = Integer.parseInt(months);
            int tierNum = Integer.parseInt(tier);
            if (tierNum > 1) {
                title = "Tier " + tierNum + " " + title;
            }
            if (monthsNum > 1) {
                title = monthsNum + "-Month " + title;
            }
        }
        catch (NumberFormatException ex) {
            // Don't expand title
        }
        return title;
    }
    
    private static String makeBitsTitle(String version) {
        String title = "Bits";
        try {
            int amount = Integer.parseInt(version);
            if (amount > 1) {
                if (amount < 1000) {
                    title = amount+" Bits";
                }
                else {
                    title = (amount / 1000)+"k Bits";
                }
            }
        }
        catch (NumberFormatException ex) {
            // Don't expand title
        }
        return title;
    }
    
    private static String makeSubGifterTitle(String version) {
        String title = "Sub Gifter";
        try {
            int amount = Integer.parseInt(version);
            if (amount > 1) {
                title = amount+" Gift Subs";
            }
        }
        catch (NumberFormatException ex) {
            // Don't expand title
        }
        return title;
    }
    
}
