
package chatty.util.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class UserIDs {
    
    private static final Logger LOGGER = Logger.getLogger(UserIDs.class.getName());
    
    private static final int CHECK_PENDING_DELAY = 60*1000;
    private static final int MAX_REQUEST_ATTEMPTS = 4;
    
    private final Object LOCK = new Object();
    
    /**
     * Stores already known userIDs.
     */
    private final Map<String, Long> userIDs = Collections.synchronizedMap(new HashMap<String, Long>());
    
    private final Map<String, Set<UserIDListener>> listeners = new HashMap<>();
    
    /**
     * Pending usernames that still require a userId. These are ones that a user
     * registered a listener for. Once the listener has been informed the
     * username is removed from this.
     * 
     * This counts up how often the userId for this username has been requested,
     * so it doesn't just keep requesting it for ever in case of errors.
     */
    private final Map<String, Integer> pending = new HashMap<>();
    
    private final TwitchApi api;
    
    public UserIDs(TwitchApi api) {
        this.api = api;
        
        Timer checkPending = new Timer("UserIDPending", true);
        checkPending.schedule(new TimerTask() {

            @Override
            public void run() {
                checkPending();
            }
        }, CHECK_PENDING_DELAY, CHECK_PENDING_DELAY);
    }
    
    /**
     * Get the Twitch User ID for the given username.
     * 
     * <p>
     * If the userID is not known yet it has to first be requested from the
     * Twitch API. For this case you can optionally register a listener in order
     * to be informed about the userID being returned. If requesting the userID
     * fails too often, the request may be cancelled and the listener discarded.
     * </p>
     * 
     * @param username A valid Twitch username
     * @param listener Optional listener, can be null
     * @return The userID, or -1 if no userID is known yet
     */
    public long getUserId(String username, UserIDListener listener) {
        username = username.toLowerCase(Locale.ENGLISH);
        
        // Check if userId is already cached
        if (userIDs.containsKey(username)) {
            return userIDs.get(username);
        }
        
        // Request ChannelInfo which contains userId
        ChannelInfo info = api.getCachedChannelInfo(username);
        if (info != null && info.id != -1) {
            // ChannelInfo was already cached, so got userId immediately
            setUserId(info.name, info.id);
            return info.id;
        }
        
        // User wants to be informed when the userId is available
        if (listener != null) {
            synchronized(LOCK) {
                if (!listeners.containsKey(username)) {
                    listeners.put(username, new HashSet<UserIDListener>());
                }
                listeners.get(username).add(listener);
                if (!pending.containsKey(username)) {
                    pending.put(username, 0);
                }
            }
        }
        return -1;
    }
    
    public void channelInfoReceived(ChannelInfo info) {
        if (info != null) {
            setUserId(info.name, info.id);
        }
    }
    
    protected void setUserId(String username, long userId) {
        username = username.toLowerCase(Locale.ENGLISH);
        if (userId != -1) {
            userIDs.put(username, userId);
            Set<UserIDListener> toInform = getListenersAndRemovePending(username);
            if (toInform != null) {
                for (UserIDListener listener : toInform) {
                    listener.setUserId(username, userId);
                }
            }
        }
    }
    
    private Set<UserIDListener> getListenersAndRemovePending(String username) {
        synchronized(LOCK) {
            if (listeners.containsKey(username)) {
                Set<UserIDListener> listenersForUsername = listeners.get(username);
                if (listenersForUsername.isEmpty()) {
                    listeners.remove(username);
                    return null;
                }
                Set<UserIDListener> result = new HashSet<>(listenersForUsername);
                listeners.remove(username);
                pending.remove(username);
                return result;
            }
            return null;
        }
    }
    
    private void checkPending() {
        String pendingUsername = getOnePendingUsername();
        if (pendingUsername != null) {
            getUserId(pendingUsername, null);
        }
    }
    
    /**
     * Returns one username that still needs the userId. There may be more than
     * this one left. Usernames that got returned too often (meaning they've
     * been requested several times already without success) are thrown away and
     * their associated listeners removed.
     * 
     * @return 
     */
    private String getOnePendingUsername() {
        synchronized(LOCK) {
            if (pending.isEmpty()) {
                return null;
            }
            Iterator<Map.Entry<String, Integer>> it = pending.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = it.next();
                if (entry.getValue() > MAX_REQUEST_ATTEMPTS) {
                    it.remove();
                    listeners.remove(entry.getKey());
                    LOGGER.warning("Gave up getting userId for "+entry.getKey());
                }
                else {
                    entry.setValue(entry.getValue() + 1);
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public interface UserIDListener {
        public void setUserId(String username, long userId);
    }
    
}
