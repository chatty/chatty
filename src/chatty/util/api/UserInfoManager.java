
package chatty.util.api;

import chatty.util.CachedBulkManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class UserInfoManager {
    
    private static final Logger LOGGER = Logger.getLogger(UserInfoManager.class.getName());

    private final CachedBulkManager<String, UserInfo> perLogin;
    
    public UserInfoManager(TwitchApi api) {
        perLogin = new CachedBulkManager<>(new CachedBulkManager.Requester<String, UserInfo>() {
            
            @Override
            public void request(CachedBulkManager<String, UserInfo> manager, Set<String> asap, Set<String> normal, Set<String> backlog) {
                Set<String> toRequest = manager.makeAndSetRequested(asap, normal, backlog, 100);
                api.requests.requestUserInfo(toRequest);
            }
            
        }, CachedBulkManager.NONE);
    }
    
    public UserInfo getCachedOnly(String login) {
        return perLogin.get(login);
    }
    
    /**
     * Get cached user info or request if necessary.
     * 
     * @param login The username
     * @param result Receives the result if a request is necessary, UserInfo
     * could be null in case of request error
     * @return Cached UserInfo, or null if none cached for this user
     */
    public UserInfo getCached(String login, Consumer<UserInfo> result) {
        return perLogin.getOrQuerySingle(r -> {
            // Can contain null in case of request error
            result.accept(r.get(login));
        }, CachedBulkManager.ASAP, login);
    }
    
    /**
     * Get info for the given list of usernames, returned to the listener. A
     * request is performed if necessary, but cached results may be returned as
     * well.
     * 
     * @param unique Only one request per object is kept, overwriting older ones
     * (unless this is null)
     * @param logins The usernames
     * @param resultListener
     */
    public void getCached(Object unique, List<String> logins, Consumer<Map<String, UserInfo>> resultListener) {
        perLogin.query(unique, (result) -> {
            resultListener.accept(result.getResults());
        }, CachedBulkManager.ASAP, logins);
    }
    
    public void resultReceived(Set<String> requested, Collection<UserInfo> result) {
        if (result == null) {
            perLogin.setError(requested);
        }
        else {
            Set<String> notFound = new HashSet<>(requested);
            for (UserInfo info : result) {
                notFound.remove(info.login);
                perLogin.setResult(info.login, info);
            }
            perLogin.setNotFound(notFound);
        }
    }
    
    public static Collection<UserInfo> parseJSON(String json) {
        if (json == null) {
            return null;
        }
        try {
            List<UserInfo> result = new ArrayList<>();
            
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray data = (JSONArray) root.get("data");
            for (Object o : data) {
                if (o instanceof JSONObject) {
                    UserInfo info = UserInfo.create((JSONObject) o);
                    if (info != null) {
                        result.add(info);
                    }
                }
            }
            return result;
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing user info: "+ex);
        }
        return null;
    }
    
}
