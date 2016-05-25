
package chatty.util.ffz;

import chatty.util.DateTime;
import static chatty.util.MiscUtil.getStackTrace;
import chatty.util.SSLUtil;
import chatty.util.ffz.WebsocketClient.MyConfigurator;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    private final MessageHandler handler;
    
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

    public WebsocketClient(MessageHandler handler) {
        this.handler = handler;
    }

    /**
     * The way it is now, this will only work once, because this is intended to
     * stay connected.
     * 
     * @param servers 
     */
    public synchronized void connect(String[] servers) {
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
                    + "\tMessages received: %d (last %s ago)",
                    
                    DateTime.ago(timeConnected),
                    s.getRequestURI(),
                    sentCount,
                    DateTime.ago(timeLastMessageSent),
                    receivedCount,
                    DateTime.ago(timeLastMessageReceived));
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
            connectionAttempts++;
            LOGGER.info("[FFZ-WS] Reconnecting in "+getDelay()+"s");
            return true;
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
            
            // Reconnect manually, so that the server can be changed
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            connectToRandomServer();
                        }
                    },
                    getDelay()*1000
            );
            return false;
        }

        @Override
        public long getDelay() {
            return connectionAttempts*connectionAttempts;
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
     * @param text 
     */
    public synchronized void send(String text) {
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
     * @param command
     * @param param 
     */
    public synchronized void sendCommand(String command, String param) {
        if (s != null && s.isOpen()) {
            sentCount++;
            send(String.format("%d %s %s", sentCount, command, param));
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
        handler.handleCommand(id, command, params);
        if (command.equals("error")) {
            LOGGER.warning("[FFZ-WS] Error: "+params);
        }
    }

    @OnOpen
    public synchronized void onOpen(Session session) {
        s = session;
        sentCount = 0;
        receivedCount = 0;
        requestedDisconnect = false;
        connectionAttempts = 0;
        LOGGER.info("[FFZ-WS] Connected");
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
            LOGGER.warning("Invalid message: "+message);
        }
    }

    @OnClose
    public synchronized void onClose(Session session, CloseReason closeReason) {
        s = null;
        LOGGER.info(String.format("[FFZ-WS] Session closed [%s]", closeReason));
    }
    
    @OnError
    public void onError(Session session, Throwable t) {
        LOGGER.warning("[FFZ-WS] ERROR: "+getStackTrace(t));
    }
    
    public static interface MessageHandler {
        public void handleReceived(String text);
        public void handleSent(String sent);
        public void handleCommand(int id, String command, String params);
        public void handleConnect();
    }
}
