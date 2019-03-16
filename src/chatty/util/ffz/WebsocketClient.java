
package chatty.util.ffz;

import chatty.util.DateTime;
import static chatty.util.MiscUtil.getStackTrace;
import chatty.util.SSLUtil;
import chatty.util.TimedCounter;
import chatty.util.ffz.WebsocketClient.MyConfigurator;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;

/**
 * Maintain the connection and handle sending/receiving commands correctly.
 * 
 * @author tduva
 */
@ClientEndpoint(configurator = MyConfigurator.class)
public class WebsocketClient {
    
    private final static Logger LOGGER = Logger.getLogger(WebsocketClient.class.getName());

    private static final long PING_INTERVAL = 10*60*1000; // 10 minutes
    
    private final MessageHandler handler;
    
    private final TimedCounter disconnectsPerHour = new TimedCounter(60*60*1000);
    
    private volatile boolean requestedDisconnect;
    private int connectionAttempts;
    private volatile boolean ssl;
    private ClientManager clientManager;
    private String[] servers;
    
    private boolean connecting;
    private volatile Session s;
    private int sentCount;
    private int receivedCount;
    private long timeConnected;
    private long timeLastMessageReceived;
    private long timeLastMessageSent;
    private long lastMeasuredLatency;
    private long timeLatencyMeasured;
    private int totalConnects;
    
    private final Map<Integer, String> commandId = new HashMap<>();

    public WebsocketClient(MessageHandler handler) {
        this.handler = handler;
    }

    /**
     * The way it is now, this will only work once, because this is intended to
     * stay connected.
     * 
     * @param servers Connect to one of these servers, randomly selected
     */
    public synchronized void connect(String[] servers) {
        // Stuff may have to be changed to only run once/be stopped if this is
        // removed
        if (connecting) {
            return;
        }
        connecting = true;
        this.servers = servers;
        new Thread(new Runnable() {

            @Override
            public void run() {
                prepareConnection();
                connectToRandomServer();
            }
        }).start();
        
        // Ping Timer (should only be started once)
        Timer ping = new Timer(true);
        ping.schedule(new TimerTask() {

            @Override
            public void run() {
                sendPing();
            }
        }, PING_INTERVAL, PING_INTERVAL);
    }
    
    /**
     * Get Websocket status in text form, with some basic formatting.
     * 
     * @return 
     */
    public synchronized String getStatus() {
        if (!connecting) {
            return "Not connected";
        }
        if (s != null && s.isOpen()) {
            return String.format("Connected for %s\n"
                    + "\tServer: %s\n"
                    + "\tCommands sent: %d (last %s ago)\n"
                    + "\tMessages received: %d (last %s ago)\n"
                    + "\tLatency: %dms (%s)",
                    
                    DateTime.ago(timeConnected),
                    s.getRequestURI(),
                    sentCount,
                    DateTime.ago(timeLastMessageSent),
                    receivedCount,
                    DateTime.ago(timeLastMessageReceived),
                    lastMeasuredLatency,
                    timeLatencyMeasured == 0 ? "not yet measured" : "measured "+DateTime.ago(timeLatencyMeasured)+" ago");
        }
        return "Connecting..";
    }
    
    /**
     * Create and configure a Client Manager.
     */
    private void prepareConnection() {
        clientManager = ClientManager.createClient();
        clientManager.getProperties().put(ClientProperties.RECONNECT_HANDLER, new Reconnect());

        // Try to add Let's Encrypt cert for SSL
        try {
            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(
                    SSLUtil.getSSLContextWithLE(), true, false, false);
            clientManager.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
            ssl = true;
        } catch (Exception ex) {
            LOGGER.warning("Failed adding support for Lets Encrypt: " + ex);
            ssl = false;
        }
    }
    
    /**
     * Randomly select a server from the given list of servers. Prepend ws:// or
     * wss:// depending on whether this should use SSL or not.
     * 
     * @param servers Array of servers, without protocol prefix
     * @param ssl 
     * @return The server, including protocol prefix
     */
    private static String getRandomServer(String[] servers, boolean ssl) {
        String server = servers[ThreadLocalRandom.current().nextInt(servers.length)];
        if (ssl) {
            server = "wss://" + server;
        } else {
            server = "ws://" + server;
        }
        return server;
    }
    
    private void connectToRandomServer() {
        connect(getRandomServer(servers, ssl));
    }
    
    private void connect(String server) {
        try {
            LOGGER.info("[FFZ-WS] Connecting to "+server);
            clientManager.asyncConnectToServer(this, new URI(server));
        } catch (Exception ex) {
            LOGGER.warning("[FFZ-WS] Error connecting: "+ex);
        }
    }
    
    /**
     * Disconnect from the server.
     * 
     * Currently connecting to the server only works once, since it's intended
     * to stay connected all the time, so using this should only be done when
     * the program is closed.
     */
    public synchronized void disonnect() {
        try {
            requestedDisconnect = true;
            if (s != null) {
                s.close();
            }
        } catch (IOException ex) {
            LOGGER.warning("Failed closing connection: "+ex);
        }
    }
    
    private class Reconnect extends ClientManager.ReconnectHandler {
        
        @Override
        public boolean onDisconnect(CloseReason closeReason) {
            if (requestedDisconnect) {
                return false;
            }
            disconnectsPerHour.increase();
            connectionAttempts++;
            LOGGER.info("[FFZ-WS] Reconnecting in "+getDelay()+"s");
            reconnect(getDelay());
            return false;
        }

        @Override
        public boolean onConnectFailure(Exception exception) {
            if (requestedDisconnect) {
                LOGGER.info("[FFZ-WS] Cancelled reconnecting..");
                return false;
            }
            connectionAttempts++;
            LOGGER.info(String.format("[FFZ-WS] Another connection attempt (%d) in %ds [%s/%s]",
                    connectionAttempts,
                    getDelay(),
                    exception,
                    exception.getCause().toString()));
            
            reconnect(getDelay());
            return false;
        }

        @Override
        public long getDelay() {
            /**
             * Wait longer if connection doesn't succeed, however too many
             * disconnects after a successful connections in a short period of
             * time should slow down connecting as well, just in case.
             */
            int disconnects = disconnectsPerHour.getCount();
            return connectionAttempts*connectionAttempts+disconnects*disconnects;
        }
        
        private void reconnect(long delay) {
            // Reconnect manually, so that the server can be changed
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            connectToRandomServer();
                        }
                    },
                    delay*1000
            );
        }
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
    
    /**
     * Send a message to the server. Does nothing if the connection is not open.
     * 
     * <p>You normally shouldn't use this directly. Use {@link sendCommand(String, String) sendCommand}
     * instead.</p>
     * 
     * @param text 
     */
    private synchronized void send(String text) {
        if (s != null && s.isOpen()) {
            s.getAsyncRemote().sendText(text);
            System.out.println("SENT: "+text);
            handler.handleSent(text);
            timeLastMessageSent = System.currentTimeMillis();
        }
    }
    
    /**
     * Send a command to the server. Does nothing if the connection is not open.
     * 
     * <p>Automatically increases and prefixes the command counter.</p>
     * 
     * @param command The command
     * @param param The parameter
     */
    public synchronized void sendCommand(String command, String param) {
        if (s != null && s.isOpen()) {
            sentCount++;
            if (param == null) {
                send(String.format("%d %s", sentCount, command));
            } else {
                send(String.format("%d %s %s", sentCount, command, param));
            }
            commandId.put(sentCount, command);
        }
    }
    
    /**
     * Send a protocol-level Ping message with the current time as payload. Does
     * nothing if not currently connected.
     */
    private synchronized void sendPing() {
        if (s != null && s.isOpen()) {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.putLong(0, System.currentTimeMillis());
                s.getAsyncRemote().sendPing(buffer);
                System.out.println("Sending"+buffer);
            } catch (Exception ex) {
                LOGGER.warning("[FFZ-WS] Failed to send ping: "+ex);
            }
        }
    }
    
    /**
     * Handle message already parsed into id, command and parameters.
     * 
     * @param id The message id
     * @param command The command
     * @param params The parameters
     */
    private void handleCommand(int id, String command, String params) {
        String originCommand = commandId.get(id);
        if (originCommand == null) {
            originCommand = "";
        }
        handler.handleCommand(id, command, params, originCommand);
        if (command.equals("error")) {
            LOGGER.warning("[FFZ-WS] Error: "+params);
        }
    }

    @OnOpen
    public synchronized void onOpen(Session session) {
        s = session;
        connectionAttempts = 0;
        sentCount = 0;
        receivedCount = 0;
        requestedDisconnect = false;
        totalConnects++;
        LOGGER.info("[FFZ-WS] Connected ("+totalConnects+")");
        handler.handleConnect();
        timeConnected = System.currentTimeMillis();
    }

    @OnMessage
    public synchronized void onMessage(String message, Session session) {
        System.out.println("RECEIVED: " + message);
        timeLastMessageReceived = System.currentTimeMillis();
        handler.handleReceived(message);
        receivedCount++;
        try {
            String[] split = message.split(" ", 3);
            int id = Integer.parseInt(split[0]);
            String command = split[1];
            String params = "";
            if (split.length == 3) {
                params = split[2];
            }
            handleCommand(id, command, params);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
            LOGGER.warning("[FFZ-WS] Invalid message: "+message);
        }
    }
    
    /**
     * Receive Pong response, take the time from the payload and calculate
     * latency.
     * 
     * @param message 
     */
    @OnMessage
    public synchronized void onPong(PongMessage message) {
        try {
            long timeSent = message.getApplicationData().getLong();
            long latency = System.currentTimeMillis() - timeSent;
            lastMeasuredLatency = latency;
            timeLatencyMeasured = System.currentTimeMillis();
            if (latency > 200) {
                LOGGER.info(String.format("[FFZ-WS] High Latency (%dms)",
                        System.currentTimeMillis() - timeSent));
            }
        } catch (Exception ex) {
            LOGGER.warning("[FFZ-WS] Invalid Pong message: "+ex);
        }
    }

    @OnClose
    public synchronized void onClose(Session session, CloseReason closeReason) {
        s = null;
        LOGGER.info(String.format("[FFZ-WS] Session closed after %s [%s]",
                DateTime.ago(timeConnected),
                closeReason));
    }
    
    @OnError
    public void onError(Session session, Throwable t) {
        LOGGER.warning("[FFZ-WS] ERROR: "+getStackTrace(t));
    }
    
    public static interface MessageHandler {
        public void handleReceived(String text);
        public void handleSent(String sent);
        public void handleCommand(int id, String command, String params, String originCommand);
        public void handleConnect();
    }
}
