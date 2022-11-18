
package chatty.util.api.pubsub;

import chatty.util.jws.JWSClient;
import chatty.util.jws.MessageHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An implementation that is able to listen and unlisten to topics and do some
 * connection management for Twitch's PubSub.
 * 
 * @author tduva
 */
public class PubSub extends JWSClient {
    
    private static final int MAX_TOPICS = 49;

    private final MessageHandler handler;
    private final Set<String> topics;
    
    private volatile Timer pingTimer;
    private volatile String token;
    
    public PubSub(URI server, MessageHandler handler) {
        super(server);
        this.handler = handler;
        this.topics = new HashSet<>();
    }
    
    @Override
    public void handleConnect(JWSClient c) {
        handler.handleConnect(c);
        startPinging();
        sendAllTopics();
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
    
    public boolean addTopic(String topic, String token) {
        this.token = token;
        synchronized(topics) {
            if (topics.contains(topic)) {
                return true;
            }
            if (topics.size() >= MAX_TOPICS) {
                return false;
            }
            topics.add(topic);
        }
        sendListen(topic, true);
        return true;
    }
    
    public boolean removeTopic(String topic, String token) {
        this.token = token;
        boolean unlisten = false;
        synchronized(topics) {
            if (topics.remove(topic)) {
                unlisten = true;
            }
        }
        if (unlisten) {
            sendListen(topic, false);
        }
        return unlisten;
    }
    
    public void updateToken(String token) {
        this.token = token;
        reconnect();
    }
    
    public boolean hasTopic(String topic) {
        synchronized(topics) {
            return topics.contains(topic);
        }
    }
    
    public int numTopics() {
        synchronized(topics) {
            return topics.size();
        }
    }
    
    private void sendAllTopics() {
        Set<String> toSend = new HashSet<>();
        synchronized(topics) {
            toSend.addAll(topics);
        }
        sendListen(toSend, true);
    }
    
    private void sendListen(String topic, boolean listen) {
        Set<String> topics = new HashSet<>();
        topics.add(topic);
        sendListen(topics, listen);
    }
    
    private void sendListen(Set<String> topics, boolean listen) {
        // Send separately for now, in case one topic errors, they don't all
        // error
        for (String topic : topics) {
            List<String> topicsArray = new ArrayList<>();
            topicsArray.add(topic);

            Map<String, Object> data = new HashMap<>();
            data.put("topics", topicsArray);
            data.put("auth_token", token);
            sendMessage(Helper.createOutgoingMessage(listen ? "LISTEN" : "UNLISTEN", "", data));
        }
    }
    
    private void startPinging() {
        if (pingTimer == null) {
            pingTimer = new Timer("PubSubPing", true);
            schedulePing();
        }
    }
    
    /**
     * Perform a ping with some delay, while scheduling another ping.
     */
    private void schedulePing() {
        pingTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                sendPing();
                schedulePing();
                // Check on a delay if the ping received a response
                pingTimer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        if (getLastReceivedSecondsAgo() > 15) {
                            /**
                             * Checking 10s after PING was send if there was a
                             * message received in the last 15s.
                             */
                            forceReconnect();
                        }
                    }
                }, 10*1000);
            }
        }, 280*1000+(new Random()).nextInt(5000)); // Random Jitter
    }
    
    public void sendPing() {
        if (isOpen()) {
            sendMessage(Helper.createOutgoingMessage("PING", null, null));
        }
    }
    
    public void cleanup() {
        if (pingTimer != null) {
            pingTimer.cancel();
        }
    }
    
    @Override
    public String toString() {
        return getStatus();
    }
    
}
