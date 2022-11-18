
package chatty.util.jws;

import chatty.util.DateTime;
import chatty.util.ElapsedTime;
import chatty.util.TimedCounter;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Intended for a websocket connection that, once initialized, will stay open
 * indefinitely. It will automatically try to reconnect if the connection is
 * lost (with increasingly long wait times based on recent connection issues).
 * 
 * @author tduva
 */
public class JWSClient implements MessageHandler {

    private static final Logger LOGGER = Logger.getLogger(JWSClient.class.getName());
    
    private final BlockingQueue<Received> received = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> sending = new LinkedBlockingQueue<>();
    private final WebSocketClient c;
    private final TimedCounter reconnectCounter = new TimedCounter(TimeUnit.MINUTES.toMillis(30));
    private final ElapsedTime lastReceivedMessage = new ElapsedTime();
    private final ElapsedTime lastConnectionAttempt = new ElapsedTime();
    private final ElapsedTime lastActivity = new ElapsedTime();
    private final MessageHandler handler;
    
    private volatile String debugPrefix = "";
    private volatile boolean requestedClose = false;
    private volatile Thread readerThread;
    private volatile Thread writerThread;
    
    /**
     * Extend this class and override the handler methods.
     * 
     * @param server The server to connect to, including protocol
     */
    public JWSClient(URI server) {
        this(server, null);
    }
    
    /**
     * Provide a MessageHandler or extend this class and override the according
     * methods.
     * 
     * @param server The server to connect to, including protocol
     * @param handler The MessageHandler, or null to use this created instance
     * as handler
     */
    public JWSClient(URI server, MessageHandler handler) {
        if (handler != null) {
            this.handler = handler;
        }
        else {
            this.handler = this;
        }
        c = new WebSocketClient(server) {
            
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                LOGGER.info(String.format("%sConnection opened (%s/%s) [%s/%s]",
                        debugPrefix,
                        handshakedata.getHttpStatus(),
                        handshakedata.getHttpStatusMessage(),
                        getSocket().getRemoteSocketAddress(),
                        getDraft()));
                received.add(new Received(Received.Type.OPEN, null, -1));
            }
            
            @Override
            public void onMessage(String message) {
                received.add(new Received(Received.Type.MESSAGE, message, -1));
                lastReceivedMessage.setSync();
                lastActivity.setSync();
            }
            
            @Override
            public void onWebsocketPing(WebSocket conn, Framedata f) {
                super.onWebsocketPing(conn, f);
                lastActivity.setSync();
            }
            
            @Override
            public void onWebsocketPong(WebSocket conn,
                                        Framedata f) {
                lastActivity.setSync();
            }
            
            @Override
            public void onClose(int code, String reason, boolean remote) {
                LOGGER.warning(String.format(debugPrefix+"Connection closed after %s (%s/%s)%s",
                        DateTime.duration(lastConnectionAttempt.millisElapsedSync()),
                        code,
                        reason,
                        remote ? " by remote host" : ""));
                received.add(new Received(Received.Type.CLOSE, reason, code));
                if (!requestedClose) {
                    reconnectTimer();
                }
            }
            
            @Override
            public void onError(Exception ex) {
                LOGGER.warning(debugPrefix+"Error "+ex);
            }
            
            private void reconnectTimer() {
                long interval = increaseAndGetReconnectWaitSeconds();
                LOGGER.info(debugPrefix+"Reconnecting in "+interval+"s..");
                Timer timer = new Timer(false);
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        LOGGER.info(debugPrefix+"Trying to reconnect to "+c.getURI());
                        // For better debug output on failing to reconnect
                        lastConnectionAttempt.setSync();
                        reconnect();
                    }
                }, interval*1000);
            }
        };
    }
    
    /**
     * Prepare connection and connect. Intended to be used once, although it
     * would probably work if properly disconnected.
     */
    public void init() {
        startConsumer();
        LOGGER.info(debugPrefix+"Trying to connect to "+c.getURI());
        lastConnectionAttempt.setSync();
        c.connect();
    }
    
    private long increaseAndGetReconnectWaitSeconds() {
        reconnectCounter.increase();
        int count = reconnectCounter.getCount();
        return Math.min(600, (long)Math.pow(2, count)) + ThreadLocalRandom.current().nextInt(count);
    }
    
    /**
     * Sets the string that is added to all logging messages.
     * 
     * @param prefix 
     */
    public void setDebugPrefix(String prefix) {
        this.debugPrefix = prefix;
    }
    
    private void startConsumer() {
        readerThread = new Thread(() -> {
            while (true) {
                try {
                    Received r = received.poll(10, TimeUnit.SECONDS);
                    if (r != null) {
                        switch (r.type) {
                            case MESSAGE:
                                handler.handleReceived(r.message);
                                break;
                            case OPEN:
                                handler.handleConnect(this);
                                break;
                            case CLOSE:
                                handler.handleDisconnect(r.code);
                                break;
                        }
                    }
                }
                catch (InterruptedException ex) {
                    return;
                }
            }
        }, debugPrefix+"WS-Reader");
        readerThread.start();
        
        writerThread = new Thread(() -> {
            while (true) {
                try {
                    String message = sending.poll(10, TimeUnit.SECONDS);
                    if (message != null) {
                        c.send(message);
                        handler.handleSent(message);
                        lastActivity.setSync();
                    }
                }
                catch (WebsocketNotConnectedException ex) {
                    // That's ok, just don't send
                }
                catch (InterruptedException ex) {
                    return;
                }
            }
        }, debugPrefix+"WS-Writer");
        writerThread.start();
    }
    
    /**
     * How long ago the last message was received.
     * 
     * @return 
     */
    public long getLastReceivedSecondsAgo() {
        return lastReceivedMessage.secondsElapsedSync();
    }
    
    /**
     * How long ago the last connection was attemped. Check {@link isOpen()} for
     * whether the connection is actually currently open.
     * 
     * @return 
     */
    public long getConnectionSeconds() {
        return lastConnectionAttempt.secondsElapsedSync();
    }
    
    /**
     * A textual representation of the current connection status.
     * 
     * @return 
     */
    public String getStatus() {
        if (!isOpen()) {
            return "Not connected.";
        }
        return String.format("Connected to %s (%s) since %s (last message received %s ago, last activity %s ago)",
                c.getURI(),
                c.getRemoteSocketAddress(),
                DateTime.duration(lastConnectionAttempt.millisElapsedSync()),
                DateTime.duration(lastReceivedMessage.millisElapsedSync()),
                DateTime.duration(lastActivity.millisElapsedSync()));
    }
    
    /**
     * Send a string, if connected.
     * 
     * @param message 
     */
    public void sendMessage(String message) {
        sending.add(message);
    }
    
    /**
     * Check if connection is currently open.
     * 
     * @return 
     */
    public boolean isOpen() {
        return c.isOpen();
    }
    
    /**
     * Disconnect and stay disconnected. For proper cleanup when program is
     * closed.
     */
    public void disconnect() {
        LOGGER.info(debugPrefix+"Disconnecting");
        try {
            requestedClose = true;
            c.close();
            readerThread.interrupt();
            writerThread.interrupt();
        } catch (Exception ex) {
            LOGGER.warning(debugPrefix+"Error closing: "+ex);
        }
    }
    
    /**
     * Disconnect in order to reconnect.
     */
    public void reconnect() {
        if (!c.isOpen()) {
            return;
        }
        LOGGER.info(debugPrefix+"Closing connection for reconnect");
        try {
            c.close();
        }
        catch (Exception ex) {
            LOGGER.warning(debugPrefix+"Error closing for reconnect: "+ex);
        }
    }
    
    /**
     * Force disconnected in order to reconnect. This is for situations where
     * it's unlikely that a close packet will actually go through.
     */
    public void forceReconnect() {
        if (!c.isOpen()) {
            return;
        }
        LOGGER.info(debugPrefix+"Forcing connection close for reconnect");
        try {
            c.closeConnection(CloseFrame.ABNORMAL_CLOSE, "Force reconnect");
        }
        catch (Exception ex) {
            LOGGER.warning(debugPrefix+"Error force close: "+ex);
        }
    }

    @Override
    public void handleReceived(String text) {
    }

    @Override
    public void handleSent(String text) {
    }

    @Override
    public void handleConnect(JWSClient c) {
    }

    @Override
    public void handleDisconnect(int code) {
    }
    
    public static class Received {
        
        public enum Type {
            OPEN, MESSAGE, CLOSE
        }
        
        public final Type type;
        public final String message;
        public final int code;
        
        public Received(Type type, String message, int code) {
            this.type = type;
            this.message = message;
            this.code = code;
        }
        
    }
    
}
