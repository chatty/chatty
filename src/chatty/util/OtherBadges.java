
package chatty.util;

import chatty.Chatty;
import chatty.util.api.CachedManager;
import chatty.util.api.usericons.Usericon;
import chatty.util.api.usericons.UsericonFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class OtherBadges {
    
    private static final Logger LOGGER = Logger.getLogger(OtherBadges.class.getName());
    
    private static final String CACHE_FILE = Chatty.getCacheDirectory()+"other_badges";
    public static final int CACHE_EXPIRES_AFTER = 60*60*24;
    
    public static void requestBadges(OtherBadgesListener listener, boolean forcedRefresh) {
        CachedManager cache = new CachedManager(CACHE_FILE, CACHE_EXPIRES_AFTER, "Other Badges") {
            
            @Override
            public boolean handleData(String data) {
                if (data != null) {
                    List<Usericon> icons = parseUsericons(data);
                    if (icons != null) {
                        listener.received(icons);
                        return true;
                    }
                }
                return false;
            }
        };
        /**
         * Always run in separate thread, since loading from cache would run
         * badge image loading in GUI thread.
         *
         * A solution similar to emotes might be better, where the images load
         * on demand, but this will do for now.
         */
        new Thread(() -> {
            if (forcedRefresh || !cache.load()) {
                String url = "https://tduva.com/res/badges";
                //url = "http://127.0.0.1/twitch/badges/badges";
                UrlRequest request = new UrlRequest(url);
                request.setLabel("Other Badges");
                request.async((result, responseCode) -> {
                    cache.dataReceived(result, forcedRefresh);
                });
            }
        }).start();
    }
    
    private static List<Usericon> parseUsericons(String json) {
        try {
            List<Usericon> result = new ArrayList<>();
            JSONParser parser = new JSONParser();
            JSONArray data = (JSONArray)parser.parse(json);
            for (Object obj : data) {
                JSONObject item = (JSONObject)obj;
                Usericon icon = parseUsericon(item);
                if (icon != null) {
                    result.add(icon);
                }
            }
            return result;
        } catch (Exception ex) {
            LOGGER.warning("Error parsing third-party badges: "+ex);
        }
        return null;
    }
    
    private static Usericon parseUsericon(JSONObject data) {
        try {
            String title = (String) data.get("meta_title");
            String id = (String) data.get("id");
            String version = (String) data.get("version");
            String url = (String) data.get("image_url");
            String url2 = (String) data.get("image_url_2");
            String color = (String) data.get("color");
            String metaUrl = (String) data.get("meta_url");
            String position = (String) data.get("position");
            Set<String> usernames = new HashSet<>();
            for (Object obj : (JSONArray) data.get("usernames")) {
                usernames.add((String) obj);
            }
            Set<String> userids = new HashSet<>();
            if (data.containsKey("userids")) {
                for (Object obj : (JSONArray) data.get("userids")) {
                    userids.add((String) obj);
                }
            }
            if (Chatty.DEBUG) {
//                usernames.add("tduva");
//                userids.add("36194025");
            }

            Usericon icon = UsericonFactory.createThirdParty(id, version, url, url2, title, metaUrl, color, usernames, userids, position);
            return icon;
        } catch (Exception ex) {
            LOGGER.warning("Error parsing third-party badge: " + ex);
        }
        return null;
    }
    
    public interface OtherBadgesListener {
        public void received(List<Usericon> badges);
    }
    
}
