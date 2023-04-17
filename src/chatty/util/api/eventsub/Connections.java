
package chatty.util.api.eventsub;

import chatty.util.api.eventsub.payloads.SessionPayload;
import chatty.util.api.eventsub.payloads.SubscriptionPayload;
import chatty.util.api.TwitchApi;
import chatty.util.jws.JWSClient;
import chatty.util.jws.MessageHandler;
import java.net.URI;
import java.net.URISyntaxException;
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
 * Manage several connections.
 * 
 * @author tduva
 */
public class Connections {
    
    private static final Logger LOGGER = Logger.getLogger(Connections.class.getName());
    
    private static final int MAX_CONNECTIONS = 3;
    private static final int MAX_RETRY_TOPIC_COUNT = 5;
    
    private final Map<Integer, Connection> connections;
    private final URI server;
    private final ConnectionsMessageHandler handler;
    private final TwitchApi api;
    private final Timer timer = new Timer("EventSub");
    
    private final Set<Topic> errorTopics = new HashSet<>();
    private final Set<Topic> errorLimitReachedTopics = new HashSet<>();
    private final Set<Topic> removedAuthTopics = new HashSet<>();
    
    private int totalCost = 0;
    private int maxTotalCost = 0;
    
    private int connIdCounter;
    
    public Connections(URI server, ConnectionsMessageHandler handler, TwitchApi api) {
        this.connections = new HashMap<>();
        this.server = server;
        this.handler = handler;
        this.api = api;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkConnection();
                checkRetry();
            }
        }, 5000, 5000);
    }
    
    public synchronized boolean addTopic(Topic topic) {
        if (hasTopic(topic)) {
            return true;
        }
        return addTopicInternal(topic);
    }
    
    private synchronized boolean addTopicInternal(Topic topic) {
        for (Connection c : connections.values()) {
            if (c.addTopic(topic)) {
                if (c.getSessionId() != null) {
                    registerTopic(c, topic);
                }
                return true;
            }
        }
        // Not added yet, add to new connection
        if (connections.size() < MAX_CONNECTIONS) {
            Connection c = addConnection(server);
            boolean result = c.addTopic(topic);
            c.init();
            return result;
        }
        return false;
    }
    
    private synchronized Connection addConnection(URI server) {
        int connId = connIdCounter++;
        Connection c = new Connection(server, createHandler(connId), api);
        c.setDebugPrefix(String.format(Locale.ROOT, "[EventSub][%d] ", connId));
        connections.put(connId, c);
        return c;
    }
    
    /**
     * No longer listen to a topic.
     * 
     * @param topic
     * @return 
     */
    public synchronized boolean removeTopic(Topic topic) {
        return removeTopic(topic, true);
    }
    
    public synchronized boolean removeTopic(Topic topic, boolean externalRemove) {
        for (Connection c : connections.values()) {
            Topic removedTopic;
            if ((removedTopic = c.removeTopic(topic)) != null) {
                if (externalRemove) {
                    unregisterTopic(removedTopic);
                }
                if (c.numTopics() == 0) {
                    disconnect(c);
                }
                return true;
            }
        }
        if (externalRemove) {
            errorLimitReachedTopics.remove(topic);
            errorTopics.remove(topic);
            removedAuthTopics.remove(topic);
        }
        return false;
    }
    
    private void retryFailedTopic() {
        if (errorLimitReachedTopics.isEmpty()) {
            return;
        }
        Iterator<Topic> it = errorLimitReachedTopics.iterator();
        Topic topic = it.next();
        it.remove();
        LOGGER.info("[EventSub] Retrying: " + topic + " "+totalCost);
        addTopicInternal(topic);
    }
    
    /**
     * Time between retries increases for each topic as error count increases.
     * This it not cleared, so with a bad connection this might never work, but
     * one-off errors should be fine. Needs better handling of different types
     * of errors.
     */
    private synchronized void checkRetry() {
        if (!errorTopics.isEmpty()) {
            Set<Topic> retry = new HashSet<>();
            for (Topic topic : errorTopics) {
                if (topic.shouldRequest()) {
                    retry.add(topic);
                }
                if (retry.size() >= MAX_RETRY_TOPIC_COUNT) {
                    break;
                }
            }
            errorTopics.removeAll(retry);
            if (!retry.isEmpty()) {
                LOGGER.info("[EventSub] Retrying: " + retry);
                for (Topic topic : retry) {
                    addTopicInternal(topic);
                }
            }
        }
    }
    
    private synchronized Connection getConnection(int id) {
        return connections.get(id);
    }
    
    public synchronized boolean hasTopic(Topic topic) {
        if (errorTopics.contains(topic)) {
            return true;
        }
        if (errorLimitReachedTopics.contains(topic)) {
            return true;
        }
        if (removedAuthTopics.contains(topic)) {
            return true;
        }
        for (Connection c : connections.values()) {
            if (c.hasTopic(topic)) {
                return true;
            }
        }
        return false;
    }
    
    public synchronized String getStatus() {
        return connections.toString();
    }
    
    public synchronized int getNumTopics(int id) {
        if (connections.containsKey(id)) {
            return connections.get(id).numTopics();
        }
        return -1;
    }
    
    public synchronized int getNumTopics() {
        int total = 0;
        for (Connection c : connections.values()) {
            total += c.numTopics();
        }
        return total;
    }
    
    public synchronized int getNumConnections() {
        return connections.size();
    }
    
    public synchronized boolean isConnected() {
        if (connections.isEmpty()) {
            return false;
        }
        for (Connection c : connections.values()) {
            if (!c.isOpen()) {
                return false;
            }
        }
        return true;
    }
    
    private synchronized void disconnect(Connection c) {
        c.disconnect();
        connections.values().remove(c);
    }
    
    public synchronized void disconnect() {
        for (Connection c : connections.values()) {
            c.disconnect();
        }
        timer.cancel();
    }
    
    public synchronized void reconnect() {
        for (Connection c : connections.values()) {
            c.reconnect();
        }
    }
    
    public synchronized void checkConnection() {
        for (Connection c : connections.values()) {
            c.checkTimeout();
        }
    }
    
    private synchronized boolean hasReplacement(Connection c) {
        for (Connection c2 : connections.values()) {
            if (c2.getReplacedConnection() == c) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Only for testing. May cause issues.
     * 
     * @param text 
     */
    public synchronized void simulate(String text) {
        connections.values().iterator().next().handleReceived(text);
    }
    
    public synchronized Topic removeTopicById(String id) {
        for (Connection c : connections.values()) {
            Topic topic = c.getTopicById(id);
            if (topic != null) {
                removeTopic(topic, false);
                return topic;
            }
        }
        return null;
    }
    
    private MessageHandler createHandler(int id) {
        return new MessageHandler() {
            
            @Override
            public void handleReceived(String text) {
                handler.handleReceived(id, text);
                handleMessage2(id, text);
            }

            @Override
            public void handleSent(String text) {
                handler.handleSent(id, text);
            }

            @Override
            public void handleConnect(JWSClient c) {
                handler.handleConnect(id, c);
            }

            @Override
            public void handleDisconnect(int code) {
                handler.handleDisconnect(id);
                handleDisconnect2(id, code);
            }
        };
    }
    
    /**
     * Handle messages that are relevant for managing connections/topics.
     * 
     * @param id
     * @param text 
     */
    private synchronized void handleMessage2(int id, String text) {
        Connection c = getConnection(id);
        if (c == null) {
            return;
        }
        Message msg = Message.fromJson(text, null);
        if (msg == null) {
            return;
        }
        if (msg.type.equals("session_welcome")) {
            SessionPayload session = (SessionPayload) msg.data;
            c.setSessionId(session.id);
            c.setConnectionTimeout(session.keepAliveTimeout + 4);
            if (c.getReplacedConnection() != null) {
                disconnect(c.getReplacedConnection());
            }
            else {
                for (Topic topic : c.getTopics()) {
                    registerTopic(c, topic);
                }
            }
        }
        else if (msg.type.equals("session_reconnect")) {
            SessionPayload session = (SessionPayload) msg.data;
            String reconnectUrl = session.reconnectUrl;
            if (reconnectUrl != null) {
                try {
                    Connection c2 = addConnection(new URI(reconnectUrl));
                    c2.setReplacedConnection(c);
                    for (Topic topic : c.getTopics()) {
                        // Make a copy so that previous connection doesn't
                        // affect them, but retain id and cost, which should in
                        // this case still be the same
                        Topic newTopic = topic.copy();
                        newTopic.setCost(topic.getCost());
                        newTopic.setId(topic.getId());
                        c2.addTopic(newTopic);
                    }
                    c2.init();
                }
                catch (URISyntaxException ex) {
                    LOGGER.warning("[EventSub] Invalid reconnect URL: " + reconnectUrl);
                }
            }
        }
        else if (msg.type.equals("revocation")) {
            SubscriptionPayload subscription = (SubscriptionPayload) msg.data;
            Topic topic = removeTopicById(subscription.id);
            if (topic != null) {
                // Topic is no longer registered, so reset associated stuff
                topic.setId(null);
                topic.setCost(0);
                if ("authorization_revoked".equals(subscription.status)) {
                    removedAuthTopics.add(topic);
                }
                else if ("moderator_removed".equals(subscription.status)) {
                    // Just remove topic
                }
                else {
                    errorTopics.add(topic);
                }
            }
        }
    }
    
    private synchronized void handleDisconnect2(int id, int code) {
        Connection c = getConnection(id);
        if (c == null) {
            return;
        }

        c.setSessionId(null);
        if (!hasReplacement(c)) {
            for (Topic topic : c.getTopics()) {
                if (topic.getCost() > 0) {
                    totalCost = -topic.getCost();
                }
                topic.setCost(0);
                topic.setId(null);
            }
        }

        switch (code) {
            case 4007: // The reconnect URL is invalid.
                disconnect(c);
                Connection c2 = addConnection(server);
                // Previous connection may still affect topics
                c.getTopics().forEach(topic -> c2.addTopic(topic.copy()));
                c2.init();
        }
    }
    
    
    
    private void registerTopic(Connection c, Topic topic) {
        String sessionId = c.getSessionId();
        api.addEventSub(topic.make(sessionId), r -> {
            if (!r.hasError) {
                synchronized (Connections.this) {
                    // Check if connection has reconnected in the meantime
                    if (sessionId.equals(c.getSessionId())) {
                        totalCost = r.totalCost;
                        maxTotalCost = r.maxTotalCost;
                        topic.setCost(r.cost);
                        topic.setId(r.id);
                    }
                }
                log("+"+topic);
            }
            else {
                synchronized (Connections.this) {
                    if (r.responseCode == 429) {
                        errorLimitReachedTopics.add(topic);
                        if (maxTotalCost > 0) {
                            totalCost = maxTotalCost;
                        }
                    }
                    else {
                        topic.increaseErrorCount();
                        errorTopics.add(topic);
                    }
                    removeTopic(topic, false);
                }
                log("Error: "+topic);
            }
        });
    }
    
    private void unregisterTopic(Topic topic) {
        if (topic.getId() == null) {
            return;
        }
        api.removeEventSub(topic.getId(), r -> {
            if (r == 204 || r == 404) {
                synchronized (Connections.this) {
                    if (topic.getCost() > 0 && r == 204) {
                        totalCost -= topic.getCost();
                        retryFailedTopic();
                    }
                }
                log("-"+topic);
            }
        });
    }
    
    private void log(String event) {
        LOGGER.info("[EventSub] "+event+" / "+getDebugText());
    }
    
    private synchronized String getDebugText() {
        return String.format("Cost: %d/%d %s%s%s",
                totalCost, maxTotalCost, errorLimitReachedTopics, errorTopics, removedAuthTopics);
    }
    
    public synchronized void tokenUpdated() {
        Set<Topic> topics = new HashSet<>(removedAuthTopics);
        removedAuthTopics.clear();
        for (Topic topic : topics) {
            addTopicInternal(topic);
        }
    }
    
}
