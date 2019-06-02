
package chatty.util.api.pubsub;

import chatty.util.DateTime;
import static chatty.util.MiscUtil.getStackTrace;
import chatty.util.TimedCounter;
import chatty.util.api.pubsub.Client.MyConfigurator;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

/**
 *
 * @author tduva
 */
@ClientEndpoint(configurator = MyConfigurator.class)
public class Client {
    
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    
    private final MessageHandler handler;
    
    private final TimedCounter disconnectsPerHour = new TimedCounter(60*60*1000);
    private AtomicInteger connectionAttempts = new AtomicInteger();
    
    private boolean connecting;
    private boolean requestedDisconnect;

    private Session s;

    private long connectedSince;
    private long lastMessageReceived;
    
    public Client(MessageHandler handler) {
        this.handler = handler;
    }
    
    public synchronized long getLastMessageReceived() {
        return lastMessageReceived;
    }
    
    public synchronized String getStatus() {
        if (!connecting) {
            return "Not connected";
        }
        if (s != null && s.isOpen()) {
            return String.format("Connected to %s for %s (last message received %s ago)",
                    s.getRequestURI(),
                    DateTime.ago(connectedSince),
                    DateTime.ago(lastMessageReceived));
        }
        return "Connecting..";
    }
    
    public static class MyConfigurator extends ClientEndpointConfig.Configurator {
        
        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            // Empty Origin, otherwise would default to server name
            headers.put("Origin", Arrays.asList(""));
        }

        @Override
        public void afterResponse(HandshakeResponse hr) {
        }
    }
    
    private class Reconnect extends ClientManager.ReconnectHandler {
        
        @Override
        public boolean onDisconnect(CloseReason closeReason) {
            if (requestedDisconnect) {
                return false;
            }
            disconnectsPerHour.increase();
            connectionAttempts.incrementAndGet();
            LOGGER.info("[PubSub] Reconnecting in "+getDelay()+"s");
            return true;
        }

        @Override
        public boolean onConnectFailure(Exception exception) {
            if (requestedDisconnect) {
                LOGGER.info("[PubSub] Cancelled reconnecting..");
                return false;
            }
            connectionAttempts.incrementAndGet();
            LOGGER.info(String.format("[PubSub] Another connection attempt (%d) in %ds [%s/%s]",
                    connectionAttempts,
                    getDelay(),
                    exception,
                    exception.getCause().toString()));
            
            return true;
        }

        @Override
        public long getDelay() {
            /**
             * Wait longer if connection doesn't succeed, however too many
             * disconnects after a successful connections in a short period of
             * time should slow down connecting as well, just in case.
             */
            int disconnects = disconnectsPerHour.getCount();
            return connectionAttempts.get()*connectionAttempts.get()+disconnects*disconnects;
        }
        
    }
    
    public synchronized void connect(String server) {
        /**
         * Only connect once, which is intended to stay connected forever. If
         * manually disconnecting/connecting again should be a thing, some stuff
         * may have to be changed.
         */
        if (connecting) {
            return;
        }
        connecting = true;
        try {
            LOGGER.info("[PubSub] Connecting to "+server);
            ClientManager clientManager = ClientManager.createClient();
            clientManager.getProperties().put(ClientProperties.RECONNECT_HANDLER, new Reconnect());
            clientManager.asyncConnectToServer(this, new URI(server));
        } catch (Exception ex) {
            LOGGER.warning("[PubSub] Error connecting "+ex);
        }
    }
    
    public synchronized void disconnect() {
        requestedDisconnect = true;
        close();
    }
    
    public synchronized void reconnect() {
        close();
    }
    
    /**
     * Close connection. If requestedDisconnect is false, it will automatically
     * reconnect.
     */
    private synchronized void close() {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ex) {
                LOGGER.warning("[PubSub] Error disconnecting "+ex);
            }
        }
    }
    
    public synchronized void send(String message) {
        if (s != null && s.isOpen()) {
            s.getAsyncRemote().sendText(message);
            handler.handleSent(message);
        }
    }
    
    @OnOpen
    public synchronized void onOpen(Session session) {
        LOGGER.info("[PubSub] Connected: "+session.getRequestURI());
        connectedSince = System.currentTimeMillis();
        connectionAttempts.set(0);
        this.s = session;
        handler.handleConnect();
    }
    
    @OnMessage
    public synchronized void onMessage(String message, Session session) {
        lastMessageReceived = System.currentTimeMillis();
        handler.handleReceived(message);
    }
    
    @OnClose
    public synchronized void onClose(Session session, CloseReason closeReason) {
        LOGGER.info(String.format("[PubSub] Connection closed after %s [%s]",
                DateTime.ago(connectedSince),
                closeReason));
        s = null;
        handler.handleDisconnect();
    }
    
    @OnError
    public void onError(Session session, Throwable t) {
        LOGGER.warning("[PubSub] ERROR: "+getStackTrace(t));
    }
    
    public interface MessageHandler {
        public void handleReceived(String text);
        public void handleSent(String text);
        public void handleConnect();
        public void handleDisconnect();
    }
    
}
