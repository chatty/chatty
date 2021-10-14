
package chatty.util.api.pubsub;

import chatty.util.jws.JWSClient;
import chatty.util.jws.MessageHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Manage several connections.
 * 
 * @author tduva
 */
public class Connections {
    
    private static final int MAX_CONNECTIONS = 6;
    
    private final List<PubSub> connections;
    private final URI server;
    private final ConnectionsMessageHandler handler;
    
    public Connections(URI server, ConnectionsMessageHandler handler) {
        this.connections = new ArrayList<>();
        this.server = server;
        this.handler = handler;
    }
    
    public synchronized boolean addTopic(String topic, String token) {
        if (hasTopic(topic)) {
            return true;
        }
        for (PubSub c : connections) {
            if (c.addTopic(topic, token)) {
                return true;
            }
        }
        if (connections.size() < MAX_CONNECTIONS) {
            int connId = connections.size();
            PubSub c = new PubSub(server, createHandler(connId));
            c.setDebugPrefix(String.format("[PubSub][%d] ", connId));
            c.init();
            connections.add(c);
            return c.addTopic(topic, token);
        }
        return false;
    }
    
    public synchronized boolean removeTopic(String topic, String token) {
        for (PubSub c : connections) {
            if (c.removeTopic(topic, token)) {
                if (c.numTopics() == 0) {
                    c.disconnect();
                    c.cleanup();
                    connections.remove(c);
                }
                return true;
            }
        }
        return false;
    }
    
    public synchronized boolean hasTopic(String topic) {
        for (PubSub c : connections) {
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
        if (connections.size() > id) {
            return connections.get(id).numTopics();
        }
        return -1;
    }
    
    public synchronized int getNumTopics() {
        int total = 0;
        for (PubSub c : connections) {
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
        for (PubSub c : connections) {
            if (!c.isOpen()) {
                return false;
            }
        }
        return true;
    }
    
    public synchronized void disconnect() {
        for (PubSub c : connections) {
            c.disconnect();
        }
    }
    
    public synchronized void reconnect() {
        for (PubSub c : connections) {
            c.reconnect();
        }
    }
    
    public synchronized void sendPing() {
        for (PubSub c : connections) {
            c.sendPing();
        }
    }
    
    /**
     * Only for testing. May cause issues.
     * 
     * @param text 
     */
    public synchronized void simulate(String text) {
        connections.get(0).handleReceived(text);
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
            public void handleDisconnect() {
                handler.handleDisconnect(id);
            }
        };
    }
    
}
