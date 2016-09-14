
package chatty.util.api.pubsub;

import chatty.util.StringUtil;
import chatty.util.api.TwitchApi;
import chatty.util.api.UserIDs;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class Manager {
    
    private final static Logger LOGGER = Logger.getLogger(Manager.class.getName());
    
    private final TwitchApi api;
    
    private final Client c;
    private final String server;

    private final Map<Long, String> userIds = Collections.synchronizedMap(new HashMap<Long, String>());
    private final Map<String, Long> modLogListen = Collections.synchronizedMap(new  HashMap<String, Long>());
    
    private volatile Timer pingTimer;
    private volatile String token;
    private volatile long localUserId = -1;
    private volatile String localUsername;
    
    public Manager(String server, final PubSubListener listener, TwitchApi api) {
        this.api = api;
        this.server = server;
        c = new Client(new Client.MessageHandler() {

            @Override
            public void handleReceived(String received) {
                listener.info("<< "+StringUtil.trim(received));
                Message message = Message.fromJson(received, userIds);
                if (message.type.equals("MESSAGE")) {
                    listener.messageReceived(message);
                }
                if (message.error != null && !message.error.isEmpty()) {
                    LOGGER.warning("[PubSub] Errror: "+message);
                }
            }

            @Override
            public void handleSent(String sent) {
                listener.info(">> "+Helper.removeToken(token, sent));
            }

            @Override
            public void handleConnect() {
                startPinging();
                sendAllTopics();
            }

            @Override
            public void handleDisconnect() {
                
            }
        });
    }
    
    private void sendAllTopics() {
        Set<Long> listenTo = new HashSet<>();
        synchronized (modLogListen) {
            for (Long userId : modLogListen.values()) {
                if (userId != -1) {
                    listenTo.add(userId);
                }
            }
        }
        for (Long userId : listenTo) {
            sendListenModLog(userId, true);
        }
    }
    
    /**
     * Get a textual representation of the connection status for output to the
     * user.
     * 
     * @return 
     */
    public String getStatus() {
        return c.getStatus();
    }
    
    public void setLocalUsername(String username) {
        if (localUsername == null || !localUsername.equals(username)) {
            this.localUsername = username;
            this.localUserId = getUserId(username);
        }
    }
    
    public void listenModLog(String username, String token) {
        if (!hasServer()) {
            return;
        }
        // Already listening, so don't do anything
        if (modLogListen.containsKey(username)) {
            return;
        }
        this.token = token;
        long userId = getUserId(username);
        modLogListen.put(username, userId);
        LOGGER.info("[PubSub] LISTEN ModLog "+username+" "+userId);
        if (userId != -1) {
            sendListenModLog(userId, true);
        }
    }
    
    public void unlistenModLog(String username) {
        synchronized(modLogListen) {
            if (modLogListen.containsKey(username)) {
                if (modLogListen.get(username) != -1) {
                    sendListenModLog(modLogListen.get(username), false);
                }
                modLogListen.remove(username);
            }
        }
    }
    
    private long getUserId(String username) {
        long userId = api.getUserId(username, new UserIDs.UserIDListener() {

            @Override
            public void setUserId(String username, long userId) {
                Manager.this.setUserId(username, userId);
            }
        });
        if (userId != -1) {
            userIds.put(userId, username);
            return userId;
        }
        return -1;
    }

    /**
     * The given userId is now known, so act on it if necessary.
     * 
     * @param username
     * @param userId 
     */
    private void setUserId(String username, long userId) {
        userIds.put(userId, username);
        
        // Topics to still request
        if (modLogListen.containsKey(username) && modLogListen.get(username) == -1) {
            modLogListen.put(username, userId);
            sendListenModLog(userId, true);
        }
        
        // If local userId hasn't been set yet, request everything now
        if (localUserId == -1 && username.equals(localUsername)) {
            localUserId = userId;
            sendAllTopics();
        }
    }
    
    private void sendListenModLog(Long userId, boolean listen) {
        if (localUserId == -1) {
            return;
        }
        
        JSONArray topics = new JSONArray();
        topics.add("chat_moderator_actions."+localUserId+"."+userId);
        
        JSONObject data = new JSONObject();
        data.put("topics", topics);
        data.put("auth_token", token);
        connect();
        c.send(Helper.createOutgoingMessage(listen ? "LISTEN" : "UNLISTEN", "", data));
        LOGGER.info("[PubSub] "+(listen ? "LISTEN" : "UNLISTEN")+" ModLog "+userId);
    }
    
    private void startPinging() {
        if (pingTimer == null) {
            pingTimer = new Timer("PubSubPing", true);
            schedulePing();
        }
    }
    
    private void schedulePing() {
        pingTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                sendPing();
                schedulePing();
                pingTimer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        if (System.currentTimeMillis() - c.getLastMessageReceived() > 15*1000) {
                            /**
                             * Checking 10s after PING was send if there was a
                             * message received in the last 15s.
                             */
                            c.reconnect();
                        }
                    }
                }, 10*1000);
            }
        }, 280*1000+(new Random()).nextInt(5000)); // Random Jitter
    }
    
    private void sendPing() {
        c.send(Helper.createOutgoingMessage("PING", null, null));
    }
    
    private boolean hasServer() {
        return server != null && !server.isEmpty();
    }
    
    public void connect() {
        if (hasServer()) {
            c.connect(server);
        }
    }
    
    public void disconnect() {
        c.disconnect();
    }
    
    public void checkConnection() {
        sendPing();
    }
    
}
