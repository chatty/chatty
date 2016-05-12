
package chatty.util.ffz;

import static chatty.util.MiscUtil.getStackTrace;
import chatty.util.SSLUtil;
import chatty.util.ffz.WebsocketClient.MyConfigurator;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
 *
 * @author tduva
 */
@ClientEndpoint(configurator = MyConfigurator.class)
public class WebsocketClient {
    
    private final static Logger LOGGER = Logger.getLogger(WebsocketClient.class.getName());

    private volatile Session s;
    private final MessageHandler handler;
    private volatile boolean requestedDisconnect;
    private int connectionAttempts;
    
    private int commandCount;
    private boolean connecting;
    
    public WebsocketClient(MessageHandler handler) {
        this.handler = handler;
    }

    public void connect(String server, String alternateServer) {
        if (connecting) {
            return;
        }
        connecting = true;
        ClientManager client = ClientManager.createClient();
        try {
            client.getProperties().put(ClientProperties.RECONNECT_HANDLER, new Reconnect());
            
            try {
                if (server.startsWith("wss://")) {
                    SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(
                            SSLUtil.getSSLContextWithLE(), true, false, false);
                    client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
                }
            } catch (Exception ex) {
                LOGGER.warning("Failed adding support for Lets Encrypt: "+ex);
                if (alternateServer != null) {
                    server = alternateServer;
                }
            }

            client.asyncConnectToServer(this, new URI(server));
            LOGGER.info("[FFZ-WS] Connecting to "+server);
        } catch (Exception ex) {
            LOGGER.warning("[FFZ-WS] Error connecting: "+ex);
        }
    }
    
    public void disonnect() {
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
                LOGGER.warning("[FFZ-WS] Cancelled reconnecting..");
                return false;
            }
            if (connectionAttempts > 30) {
                return false;
            }
            connectionAttempts++;
            LOGGER.info(String.format("[FFZ-WS] Another connection attempt (%d) in %ds [%s/%s]",
                    connectionAttempts,
                    getDelay(),
                    exception,
                    exception.getCause().toString()));
            return true;
        }

        @Override
        public long getDelay() {
            return connectionAttempts*connectionAttempts;
        }
    }
    
    public static class MyConfigurator extends ClientEndpointConfig.Configurator {
        
        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            headers.put("Origin", Arrays.asList("www.twitch.tv"));
        }

        @Override
        public void afterResponse(HandshakeResponse hr) {
            //process the handshake response
        }
    }
    
    public synchronized void send(String text) {
        if (s != null && s.isOpen()) {
            s.getAsyncRemote().sendText(text);
            System.out.println("SENT: "+text);
        }
    }
    
    public void sendCommand(String command, String param) {
        if (s != null && s.isOpen()) {
            commandCount += 1;
            send(String.format("%d %s %s", commandCount, command, param));
        }
    }
    
    private void handleCommand(int id, String command, String params) {
        handler.handleCommand(id, command, params);
        if (command.equals("error")) {
            LOGGER.warning("[FFZ-WS] Error: "+params);
        }
    }

    @OnOpen
    public synchronized void onOpen(Session session) {
        s = session;
        commandCount = 0;
        requestedDisconnect = false;
        connectionAttempts = 0;
        LOGGER.info("[FFZ-WS] Connected");
        handler.handleConnect();
    }

    @OnMessage
    public synchronized void onMessage(String message, Session session) {
        System.out.println("RECEIVED: " + message);
        handler.handleMessage(message);
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
        public void handleMessage(String text);
        public void handleCommand(int id, String command, String params);
        public void handleConnect();
    }
}
