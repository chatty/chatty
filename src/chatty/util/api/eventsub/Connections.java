
package chatty.util.api.eventsub;

import chatty.util.Debugging;
import chatty.util.api.eventsub.payloads.SessionPayload;
import chatty.util.api.eventsub.payloads.SubscriptionPayload;
import chatty.util.api.TwitchApi;
import chatty.util.jws.JWSClient;
import chatty.util.jws.MessageHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
    
    /**
     * These topics will pontentially be registered when possible, but they are
     * not added to a specific connection.
     */
    private final Set<Topic> errorTopics = new HashSet<>();
    private final Set<Topic> errorLimitReachedTopics = new HashSet<>();
    private final Set<Topic> removedAuthTopics = new HashSet<>();
    
    /**
     * These topics will be unregistered when possible, while they are still
     * added to a specific connection.
     */
    private final Set<Topic> toRemove = new HashSet<>();
    
    /**
     * Topics waiting to be added while they are still being removed.
     */
    private final Set<Topic> toAdd = new HashSet<>();
    
    private final Set<Topic> requestPending = new HashSet<>();
    
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
                // Unregister first, which will also check if topic is to be added
                unregisterTopics();
                addTopics();
            }
        }, 5000, 5000);
    }
    
    public synchronized boolean addTopic(Topic topic) {
        // If a topic will be removed, it's not really added
        if (hasTopic(topic) && !toRemove.contains(topic)) {
            Debugging.println("es", "Add Topic: %s (already added)", topic);
            return true;
        }
        if (toRemove.contains(topic)) {
            // Could re-add immediately if no remove request pending, but doing it on a timer should do
            Debugging.println("es", "Add Topic: %s (try again later)", topic);
            toAdd.add(topic);
            // Don't actually know if it can be added in this case
            return true;
        }
        return addTopicInternal(topic);
    }
    
    private synchronized boolean addTopicInternal(Topic topic) {
        for (Connection c : connections.values()) {
            if (c.addTopic(topic)) {
                Debugging.println("es", "Add Topic: %s (added to %s)", topic, c.id);
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
            Debugging.println("es", "Add Topic: %s (added to new %s)", topic, c.id);
            c.init();
            return result;
        }
        return false;
    }
    
    private synchronized Connection addConnection(URI server) {
        int connId = connIdCounter++;
        Connection c = new Connection(server, createHandler(connId), connId);
        c.setDebugPrefix(String.format(Locale.ROOT, "[EventSub][%d] ", connId));
        connections.put(connId, c);
        return c;
    }
    
    /**
     * No longer listen to a topic. If the topic is added to a connection, but
     * not registered yet, it will wait a bit to unregister it.
     *
     * @param topic
     */
    public synchronized void removeTopic(Topic topic) {
        Debugging.println("es", "Remove Topic: %s", topic);
        errorLimitReachedTopics.remove(topic);
        errorTopics.remove(topic);
        removedAuthTopics.remove(topic);
        toAdd.remove(topic);
        
        // Need the object that may have the id set (none may be found)
        Topic existingTopic = null;
        for (Connection c : connections.values()) {
            existingTopic = c.getTopic(topic);
            if (existingTopic != null) {
                break;
            }
        }
        
        /**
         * If it is already set to remove then don't try immediately (since it
         * would have already done that), but wait for the unregisterTopics()
         * timer.
         */
        if (existingTopic != null
                && !toRemove.contains(existingTopic)) {
            toRemove.add(existingTopic);
            unregisterTopic(existingTopic);
        }
    }
    
    /**
     * Called when the topic is not registered with the connection, for example
     * because it has been unregistered or access revoked.
     * 
     * @param topic
     * @return 
     */
    private synchronized boolean removeTopicInternal(Topic topic) {
        Debugging.println("es", "Remove Topic: %s (internal)", topic);
        toRemove.remove(topic);
        for (Connection c : connections.values()) {
            if (c.removeTopic(topic) != null) {
                if (c.numTopics() == 0) {
                    disconnect(c);
                }
                return true;
            }
        }
        return false;
    }
    
    private void retryFailedTopic(boolean withCost) {
        if (errorLimitReachedTopics.isEmpty()) {
            return;
        }
        // Get the first topic with no cost or just any if cost was reduced
        Iterator<Topic> it = errorLimitReachedTopics.iterator();
        Topic topic = null;
        while (it.hasNext()) {
            topic = it.next();
            if (topic.getCost() == 0 || withCost) {
                it.remove();
                break;
            }
        }
        if (topic != null) {
            LOGGER.info("[EventSub] Retrying: " + topic + " "+totalCost);
            addTopicInternal(topic);
        }
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
                removeTopicInternal(topic);
                return topic;
            }
        }
        return null;
    }
    
    private MessageHandler createHandler(int id) {
        return new MessageHandler() {
            
            @Override
            public void handleReceived(String text) {
                Message msg = Message.fromJson(text);
                handler.handleReceived(id, text, msg);
                handleMessage2(id, msg);
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
     * @param msg
     */
    private synchronized void handleMessage2(int id, Message msg) {
        Connection c = getConnection(id);
        if (c == null) {
            return;
        }
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
            
            // Topic is now removed, check if it should be tried again
            if (topic != null) {
                // Topic is no longer registered, so reset associated stuff
                topic.setId(null);
                topic.setCost(0);
                if ("authorization_revoked".equals(subscription.status)) {
                    if (!toRemove.contains(topic)) {
                        removedAuthTopics.add(topic);
                    }
                }
                else if ("moderator_removed".equals(subscription.status)) {
                    // Don't add to a set to try to register again
                }
                else {
                    if (!toRemove.contains(topic)) {
                        errorTopics.add(topic);
                    }
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
        requestPending.add(topic);
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
                    requestPending.remove(topic);
                }
                log("+"+topic, c.id);
            }
            else {
                boolean reportError = false;
                synchronized (Connections.this) {
                    // If registering failed, but it has to be removed anyway,
                    // don't add to error sets
                    if (!toRemove.contains(topic)) {
                        reportError = true;
                        if (r.responseCode == 429) {
                            errorLimitReachedTopics.add(topic);
                            // Error 429 is not just due to cost
//                            if (maxTotalCost > 0) {
//                                totalCost = maxTotalCost;
//                            }
                        }
                        else {
                            topic.increaseErrorCount();
                            errorTopics.add(topic);
                        }
                    }
                    removeTopicInternal(topic);
                    requestPending.remove(topic);
                }
                // Outside lock, just in case
                if (reportError) {
                    handler.handleRegisterError(r.responseCode);
                }
                log("Error: "+topic+" / "+getDebugText(), c.id);
            }
        });
    }
    
    /**
     * Unregister topics that are waiting to be removed, but weren't registered
     * yet. To be called from the timer in the expected order.
     */
    private synchronized void unregisterTopics() {
        if (toRemove.isEmpty()) {
            return;
        }
        Iterator<Topic> it = toRemove.iterator();
        while (it.hasNext()) {
            Topic topic = it.next();
            /**
             * If a request is pending (which could be registering or
             * unregistering it) neither try to register or unregister it again.
             */
            if (!requestPending.contains(topic)) {
                if (toAdd.contains(topic)) {
                    // Adding will be checked next in the timer
                    it.remove();
                }
                else {
                    // Keep topic in toRemove in case it fails
                    unregisterTopic(topic);
                }
            }
        }
    }
    
    /**
     * Add topics that are queued to be added while they were still being
     * removed. Clears toAdd since added topics will retry registering if
     * necessary, although it may fail to add them if there is no space, but
     * that's the case when normally adding them as well.
     */
    private synchronized void addTopics() {
        Set<Topic> toAddCopy = new HashSet<>(toAdd);
        toAdd.clear();
        for (Topic topic : toAddCopy) {
            Debugging.println("es", "Re-adding: %s", topic);
            addTopic(topic);
        }
    }
    
    private boolean unregisterTopic(Topic topic) {
        if (topic.getId() == null) {
            Debugging.println("es", "Unregister %s (no id)", topic);
            return false;
        }
        if (!topic.shouldRequest()) {
            Debugging.println("es", "Unregister %s (wait)", topic);
            return false;
        }
        Debugging.println("es", "Unregister %s", topic);
        requestPending.add(topic);
        api.removeEventSub(topic.getId(), r -> {
            if (r == 204 || r == 404) {
                synchronized (Connections.this) {
                    if (topic.getCost() > 0 && r == 204) {
                        totalCost -= topic.getCost();
                    }
                    removeTopicInternal(topic);
                    requestPending.remove(topic);
                    
                    // Space may have opened up, not just when cost is involved
                    retryFailedTopic(topic.getCost() > 0);
                }
                log("-"+topic);
            }
            else {
                synchronized (Connections.this) {
                    topic.increaseErrorCount();
                    requestPending.remove(topic);
                }
            }
        });
        return true;
    }
    
    private void log(String event, int connectionId) {
        LOGGER.info(String.format("[EventSub]%s %s",
                                  getConnectionPrefix(connectionId), event));
    }
    
    private void log(String event) {
        LOGGER.info(String.format("[EventSub] %s",
                                  event));
    }
    
    public String getConnectionPrefix(int connectionId) {
        return String.format(Locale.ROOT, "[%d(%3d)/%d(%3d)]",
                             connectionId,
                             getNumTopics(connectionId),
                             getNumConnections(),
                             getNumTopics());
    }
    
    public String getDebugText() {
        String result;
        synchronized (Connections.this) {
            result = String.format("Cost: %d/%d L:%s E:%s A:%s R:%s",
                totalCost, maxTotalCost, errorLimitReachedTopics, errorTopics, removedAuthTopics, toRemove);
        }
        return result;
    }
    
    public Map<String, List<Topic>> getTopics() {
        Map<String, List<Topic>> result = new HashMap<>();
        for (Connection c : connections.values()) {
            List<Topic> topics = new ArrayList<>();
            topics.addAll(c.getTopics());
            result.put(c.getSessionId(), topics);
        }
        return result;
    }
    
    public synchronized void tokenUpdated() {
        Set<Topic> topics = new HashSet<>(removedAuthTopics);
        removedAuthTopics.clear();
        for (Topic topic : topics) {
            addTopicInternal(topic);
        }
    }
    
}
