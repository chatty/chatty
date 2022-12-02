
package chatty.util.api.pubsub;

import chatty.util.jws.JWSClient;
import chatty.util.jws.MessageHandler;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manage several connections.
 * 
 * @author tduva
 */
public class Connections {
    
    private static final int MAX_CONNECTIONS = 6;
    
    private final Map<Integer, PubSub> connections;
    private final URI server;
    private final ConnectionsMessageHandler handler;
    private int connIdCounter;
    
    public Connections(URI server, ConnectionsMessageHandler handler) {
        this.connections = new HashMap<>();
        this.server = server;
        this.handler = handler;
    }
    
    public synchronized boolean addTopic(String topic, String token) {
        if (hasTopic(topic)) {
            return true;
        }
        for (PubSub c : connections.values()) {
            if (c.addTopic(topic, token)) {
                return true;
            }
        }
        if (connections.size() < MAX_CONNECTIONS) {
            int connId = connIdCounter++;
            PubSub c = new PubSub(server, createHandler(connId));
            c.setDebugPrefix(String.format(Locale.ROOT, "[PubSub][%d] ", connId));
            c.init();
            connections.put(connId, c);
            return c.addTopic(topic, token);
        }
        return false;
    }
    
    public synchronized boolean removeTopic(String topic, String token) {
        for (PubSub c : connections.values()) {
            if (c.removeTopic(topic, token)) {
                if (c.numTopics() == 0) {
                    c.disconnect();
                    c.cleanup();
                    // Should be fine, since the iteration stops after this
                    connections.values().remove(c);
                }
                return true;
            }
        }
        return false;
    }
    
    public synchronized void updateToken(String token) {
        for (PubSub c : connections.values()) {
            c.updateToken(token);
        }
    }
    
    public synchronized boolean hasTopic(String topic) {
        for (PubSub c : connections.values()) {
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
        for (PubSub c : connections.values()) {
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
        for (PubSub c : connections.values()) {
            if (!c.isOpen()) {
                return false;
            }
        }
        return true;
    }
    
    public synchronized void disconnect() {
        for (PubSub c : connections.values()) {
            c.disconnect();
        }
    }
    
    public synchronized void reconnect() {
        for (PubSub c : connections.values()) {
            c.reconnect();
        }
    }
    
    public synchronized void sendPing() {
        for (PubSub c : connections.values()) {
            c.sendPing();
        }
    }
    
    /**
     * Only for testing. May cause issues.
     * 
     * @param text 
     */
    public synchronized void simulate(String text) {
        connections.values().iterator().next().handleReceived(text);
    }
    
    private MessageHandler createHandler(int id) {
        return new MessageHandler() {

            @Override
            public void handleReceived(String text) {
                handler.handleReceived(id, text);
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
            }
        };
    }
    
}
