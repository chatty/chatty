
package chatty.util.api.pubsub;

import chatty.Chatty;
import chatty.gui.components.eventlog.EventLog;
import chatty.util.Debugging;
import chatty.util.StringUtil;
import chatty.util.api.TwitchApi;
import chatty.util.jws.JWSClient;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class Manager {
    
    private final static Logger LOGGER = Logger.getLogger(Manager.class.getName());
    
    private final TwitchApi api;
    
    private final Connections c;
    private final PubSubListener listener;

    /**
     * Storage of user ids for easier lookup to turn an id into a channel name.
     */
    private final Map<String, String> userIds = Collections.synchronizedMap(new HashMap<String, String>());
    
    private final Set<Topic> pendingTopics = Collections.synchronizedSet(new HashSet<Topic>());
    
    private volatile String token;
    private volatile String localUserId;
    private volatile String localUsername;
    
    public Manager(String server, final PubSubListener listener, TwitchApi api) {
        this.api = api;
        this.listener = listener;
        this.c = init(server);
    }
    
    private Connections init(String server) {
        try {
            return new Connections(new URI(server), new ConnectionsMessageHandler() {
                
                @Override
                public void handleReceived(int id, String received) {
                    listener.info(String.format("[%d(%d)/%d(%d)]--> %s",
                            id,
                            c.getNumTopics(id),
                            c.getNumConnections(),
                            c.getNumTopics(),
                            StringUtil.trim(received)));
                    Message message = Message.fromJson(received, userIds);
                    if (message != null) {
                        if (message.data instanceof ModeratorActionData) {
                            ModeratorActionData data = (ModeratorActionData) message.data;
                            if (data.type == ModeratorActionData.Type.UNMODDED) {
                                unlistenModLog(data.stream);
                            }
                        }
                        if (message.type.equals("MESSAGE")) {
                            listener.messageReceived(message);
                        }
                        if (message.error != null && !message.error.isEmpty()) {
                            LOGGER.warning("[PubSub]["+id+"] Errror: " + message);
                        }
                    }
                }
                
                @Override
                public void handleSent(int id, String sent) {
                    listener.info(String.format("[%d(%d)/%d(%d)]<-- %s",
                            id,
                            c.getNumTopics(id),
                            c.getNumConnections(),
                            c.getNumTopics(),
                            Helper.removeToken(token, sent)));
                }
                
                @Override
                public void handleDisconnect(int id) {
                    
                }
                
                @Override
                public void handleConnect(int id, JWSClient c) {
                    
                }
            });
        }
        catch (URISyntaxException ex) {
            LOGGER.warning("[PubSub] Invalid server: "+server);
            return null;
        }
    }
    
    /**
     * Only for testing. May cause issues.
     * 
     * @param input 
     */
    public void simulate(String input) {
        c.simulate(input);
    }
    
    
    //==========================
    // Topics / various
    //==========================
    
    /**
     * Set the username of this Chatty user, which is used for listening to the
     * correct mod log topic (which requires the mod user id).
     * 
     * @param username 
     */
    public void setLocalUsername(String username) {
        if (localUsername == null || !localUsername.equals(username)) {
            this.localUsername = username;
            this.localUserId = null;
            requestUserId(username);
        }
    }
    
    /**
     * Start receiving the modlog for the given channel (username). The token is
     * requires to authenticate.
     * 
     * @param username
     * @param token 
     */
    public void listenModLog(String username, String token) {
        this.token = token;
        addTopic(new ModLog(username));
        addTopic(new AutoMod(username));
    }
    
    /**
     * Stop reciving the modlog for the given channel (username).
     * 
     * @param username 
     */
    public void unlistenModLog(String username) {
        removeTopic(new ModLog(username));
        removeTopic(new AutoMod(username));
    }
    
    public void listenUserModeration(String username, String token) {
        this.token = token;
        addTopic(new UserModeration(username));
    }
    
    public void unlistenUserModeration(String username) {
        removeTopic(new UserModeration(username));
    }
    
    public void listenPoints(String username, String token) {
        this.token = token;
        addTopic(new Points(username));
    }
    
    public void unlistenPoints(String username) {
        removeTopic(new Points(username));
    }
    
    /**
     * Adds the given topic to be requested. If the topic is already being
     * listened to, it will still be added, processed and tried to listen to
     * again (which will simply do nothing).
     * 
     * If the topic doesn't require any additional data, it will be listened to
     * immediately, otherwise it will request any missing data and stay pending
     * until the data is available. This currently only refers to resolving a
     * user/stream name to an id.
     * 
     * @param topic 
     */
    private void addTopic(Topic topic) {
        if (c == null) {
            return;
        }
        boolean added = pendingTopics.add(topic);
        if (added) {
            checkPendingTopics();
            if (topic.make() == null) {
                topic.request();
            }
        }
    }
    
    private void removeTopic(Topic topic) {
        if (c == null) {
            return;
        }
        pendingTopics.remove(topic);
        String readyTopic = topic.make();
        if (readyTopic != null) {
            // Only if all required data is already there is it likely that
            // the topic already added
            c.removeTopic(readyTopic, token);
        }
    }
    
    private void checkPendingTopics() {
        Set<String> readyTopics = new HashSet<>();
        synchronized(pendingTopics) {
            Iterator<Topic> it = pendingTopics.iterator();
            while (it.hasNext()) {
                Topic topic = it.next();
                String readyTopic = topic.make();
                if (readyTopic != null) {
                    readyTopics.add(readyTopic);
                    it.remove();
                }
            }
        }
        Debugging.println("pubsub", "[PubSub] Send: %s, Pending: %s",
                readyTopics, pendingTopics);
        for (String topic : readyTopics) {
            boolean success = c.addTopic(topic, token);
            if (!success) {
                // Prefixed with "session" so it can create a notification again
                // next session, even if marked as read
                EventLog.addSystemEvent("session.pubsub.maxtopics",
                        "Too many channels",
                        "The amount of channels you have joined (especially "
                        + "where you are a moderator) will cause some features "
                                + "to not fully work, such as displaying Mod "
                                + "Actions, AutoMod and others.");
            }
        }
    }
    
    /**
     * Get the user id for the given username, or wait until it has been
     * requested and act on it then.
     * 
     * @param username A valid Twitch username
     * @return The user id, or -1 if user id still has to be requested
     */
    private void requestUserId(String username) {
        api.waitForUserId(r -> {
            Manager.this.setUserId(username, r.getId(username));
        }, username);
    }
    
    private String getUserId(String username) {
        if (StringUtil.isNullOrEmpty(username)) {
            return null;
        }
        synchronized(userIds) {
            for (Map.Entry<String, String> entry : userIds.entrySet()) {
                if (entry.getValue().equals(username)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * The given userId is now known, so act on it if necessary.
     * 
     * @param username
     * @param userId 
     */
    private void setUserId(String username, String userId) {
        userIds.put(userId, username);
        
        // If local userId hasn't been set yet, request everything now
        if (localUserId == null && username.equals(localUsername)) {
            localUserId = userId;
        }
        
        checkPendingTopics();
    }
    
    //==========================
    // Connection stuff
    //==========================
    
    /**
     * Get a textual representation of the connection status for output to the
     * user.
     *
     * @return
     */
    public String getStatus() {
        if (c == null) {
            return "Not initialized";
        }
        return c.getStatus();
    }
    
    public void disconnect() {
        if (c != null) {
            c.disconnect();
        }
    }
    
    public void checkConnection() {
        c.sendPing();
    }
    
    public boolean isConnected() {
        return c != null && c.isConnected();
    }
    
    public void reconnect() {
        if (c != null) {
            c.reconnect();
        }
    }
    
    //==========================
    // Topic Classes
    //==========================
    
    private static interface Topic {
        
        /**
         * Create the full topic name, including any data required for it.
         * 
         * @return The full topic name, or null if data is missing
         */
        public String make();
        
        /**
         * Request any data for this topic.
         */
        public void request();
    }
    
    /**
     * A topic that requires a stream id, based on a stream name.
     */
    private abstract class StreamTopic implements Topic {
        
        protected final String stream;
        
        StreamTopic(String stream) {
            this.stream = stream;
        }
        
        public void request() {
            requestUserId(stream);
        }
        
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + Objects.hashCode(this.stream);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StreamTopic other = (StreamTopic) obj;
            if (!Objects.equals(this.stream, other.stream)) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName()+" "+stream;
        }
        
    }
    
    private class ModLog extends StreamTopic {

        ModLog(String stream) {
            super(stream);
        }
        
        @Override
        public String make() {
            String userId = getUserId(stream);
            if (userId != null && localUserId != null) {
                return "chat_moderator_actions."+localUserId+"."+userId;
            }
            return null;
        }
        
    }
    
    private class AutoMod extends StreamTopic {

        AutoMod(String stream) {
            super(stream);
        }
        
        @Override
        public String make() {
            String userId = getUserId(stream);
            if (userId != null && localUserId != null) {
                return "automod-queue."+localUserId+"."+userId;
            }
            return null;
        }
        
    }
    
    private class UserModeration extends StreamTopic {

        UserModeration(String stream) {
            super(stream);
        }
        
        @Override
        public String make() {
            String userId = getUserId(stream);
            if (userId != null && localUserId != null) {
                return "user-moderation-notifications."+localUserId+"."+userId;
            }
            return null;
        }
        
    }
    
    private class Points extends StreamTopic {

        Points(String stream) {
            super(stream);
        }
        
        @Override
        public String make() {
            String userId = getUserId(stream);
            if (userId != null) {
                return "community-points-channel-v1."+userId;
            }
            return null;
        }

    }
    
}
