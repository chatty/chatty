
package chatty.util.api.eventsub;

import chatty.util.api.TwitchApi;
import chatty.util.jws.JWSClient;
import chatty.util.jws.MessageHandler;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A single EventSub connection, also storing some metadata.
 * 
 * @author tduva
 */
public class Connection extends JWSClient {
    
    private static final int MAX_TOPICS = 100;

    private final MessageHandler handler;
    private final Map<Topic, Topic> topics;
    private final TwitchApi api;
    
    private volatile String sessionId;
    private volatile Connection replacesConnection;
    private volatile int connectionTimeoutSeconds;
    
    public Connection(URI server, MessageHandler handler, TwitchApi api) {
        super(server);
        this.handler = handler;
        this.topics = new HashMap<>();
        this.api = api;
    }
    
    public void setConnectionTimeout(int seconds) {
        this.connectionTimeoutSeconds = seconds;
    }
    
    public void checkTimeout() {
        if (!isOpen() && getConnectionSeconds() > 30) {
            return;
        }
        if (connectionTimeoutSeconds > 0) {
            if (getLastReceivedSecondsAgo() > connectionTimeoutSeconds * 2) {
                forceReconnect();
            }
            else if (getLastReceivedSecondsAgo() > connectionTimeoutSeconds) {
                reconnect();
            }
        }
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getSessionId() {
        return this.sessionId;
    }
    
    public void setReplacedConnection(Connection c) {
        this.replacesConnection = c;
    }
    
    public Connection getReplacedConnection() {
        return replacesConnection;
    }
    
    @Override
    public void handleConnect(JWSClient c) {
        handler.handleConnect(c);
    }

    @Override
    public void handleDisconnect(int code) {
        handler.handleDisconnect(code);
    }

    @Override
    public void handleReceived(String text) {
        handler.handleReceived(text);
    }
    
    @Override
    public void handleSent(String text) {
        handler.handleSent(text);
    }
    
    public boolean addTopic(Topic topic) {
        synchronized(topics) {
            if (topics.containsKey(topic)) {
                return true;
            }
            if (topics.size() >= MAX_TOPICS) {
                return false;
            }
            topics.put(topic, topic);
        }
        return true;
    }
    
    public Topic removeTopic(Topic topic) {
        synchronized(topics) {
            return topics.remove(topic);
        }
    }
    
    public Set<Topic> getTopics() {
        synchronized (topics) {
            return new HashSet<>(topics.values());
        }
    }
    
    public Topic getTopicById(String id) {
        synchronized (topics) {
            for (Topic topic : topics.values()) {
                if (id.equals(topic.getId())) {
                    return topic;
                }
            }
        }
        return null;
    }
    
    public boolean hasTopic(Topic topic) {
        synchronized(topics) {
            return topics.containsKey(topic);
        }
    }
    
    public int numTopics() {
        synchronized(topics) {
            return topics.size();
        }
    }
    
    @Override
    public String toString() {
        return getStatus();
    }
    
}
