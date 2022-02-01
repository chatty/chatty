
package chatty;

import static chatty.Irc.SSL_ERROR;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.RingBuffer;
import chatty.util.StringUtil;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * A single connection to a server that can receive and send data.
 * 
 * @author tduva
 */
public class Connection implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Connection.class.getName());
    
    private final InetSocketAddress address;
    private final Irc irc;
    private final RingBuffer<Msg> debugBuffer = new RingBuffer<>(20);
    private int debugCounter = -1;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    
    private int disconnectReason = -1;
    private String disconnectMessage = null;

    private int connectionCheckedCount;
    
    private static final int CONNECT_TIMEOUT = 10*1000; // 10 seconds timeout
    private static final int SOCKET_BLOCK_TIMEOUT = 15*1000; // 15 seconds
    private static final int PING_AFTER_CHECKS = 3; // 45 seconds (3*SOCKET_BLOCK_TIMEOUT)
    
    private final String idPrefix;
    
    private final boolean secured;
    
    public Connection(Irc irc, InetSocketAddress address, String id, boolean secured) {
        this.irc = irc;
        this.address = address;
        this.idPrefix = "["+id+"] ";
        this.secured = secured;
    }
    
    private final void info(String message) {
        LOGGER.info(idPrefix+message);
    }
    
    private final void warning(String message) {
        LOGGER.warning(idPrefix+message);
    }
    
    public InetSocketAddress getAddress() {
        return address;
    }
    
    /**
     * Thread that opens the connection and receives data from the connection.
     */
    @Override
    public void run() {
        Charset charset = Charset.forName("UTF-8");
        try {
            info("Trying to connect to "+address+(secured ? " (secured)" : ""));
            // Try to connect and open streams
            if (secured) {
                SSLSocketFactory sf = (SSLSocketFactory)SSLSocketFactory.getDefault();
                socket = sf.createSocket();
                ((SSLSocket) socket).setUseClientMode(true);
                
                /**
                 * Workaround for "Could not generate DH keypair" exception
                 * https://stackoverflow.com/a/6862383
                 * 
                 * Maybe not be necessary anymore for now
                 */
//                List<String> limited = new LinkedList<>();
//                for (String suite : ((SSLSocket) socket).getEnabledCipherSuites()) {
//                    if (!suite.contains("_DHE_")) {
//                        limited.add(suite);
//                    }
//                }
//                ((SSLSocket) socket).setEnabledCipherSuites(limited.toArray(new String[limited.size()]));
            } else {
                socket = new Socket();
            }
            socket.connect(address, CONNECT_TIMEOUT);
//            info("Connecting to "+socket.getRemoteSocketAddress().toString());
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(),charset)
                    );
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(),charset)
                );
            socket.setSoTimeout(SOCKET_BLOCK_TIMEOUT);
        } catch (UnknownHostException ex) {
            irc.disconnected(Irc.ERROR_UNKNOWN_HOST);
            warning("Error opening connection to "+address+": "+ex);
            return;
        } catch (SocketTimeoutException ex) {
            warning("Error opening connection: "+ex);
            irc.disconnected(Irc.ERROR_SOCKET_TIMEOUT);
            return;
        } catch (IOException ex) {
            warning("Error opening connection: "+ex);
            irc.disconnected(Irc.ERROR_SOCKET_ERROR, ex.getLocalizedMessage());
            return;
        }
        // At this point the connection succeeded, but not registered with the
        // IRC server (wich is often called "connected" in this context)
        
        info("Connected to "+socket.getRemoteSocketAddress().toString());
        connected = true;
        irc.connected(socket.getInetAddress().toString(),address.getPort());
        
        StringBuilder b = new StringBuilder();
        boolean previousWasCR = false;
        String receivedLine = null;
        while (true) {
            try {
                /**
                 * Read line ending with \r\n (blocks, but has a timeout set).
                 * 
                 * This also filters \r and \n characters from the parsed
                 * messages, because they are not added to the buffer.
                 */
                int c = in.read();
                if (c == -1) {
                    // End of stream
                    break;
                }
                if (c == '\r') {
                    previousWasCR = true;
                    //System.out.print("\\r");
                } else if (c == '\n') {
                    //System.out.println("\\n");
                    if (previousWasCR) {
                        // Take buffer as line and reset
                        receivedLine = b.toString();
                        b.setLength(0);
                        previousWasCR = false;
                    }
                } else {
                    b.append((char)c);
                    previousWasCR = false;
                    //System.out.print((char)c);
                }
                
                if (receivedLine == null) {
                    // Read more characters to get to end of line
                    continue;
                }
                
                // Line was received
                debugBuffer.add(new Msg(System.currentTimeMillis(), receivedLine, false));
                irc.received(receivedLine);
                receivedLine = null;
                activity();
            } catch (SocketTimeoutException ex) {
                checkConnection();
            } catch (SSLException ex) {
                warning("SSL Error reading from socket: "+ex);
                disconnectReason = SSL_ERROR;
                disconnectMessage = ex.getLocalizedMessage();
                break;
            } catch (IOException ex) {
                info("Error reading from socket: "+ex);
                break;
            }
        }
        
        // No longer receiving data, so properly close connection if necessary.
        close();
    }
    
    /**
     * Notifies the activity tracker that there was activity on the connection.
     */
    private void activity() {
        connectionCheckedCount = 0;
        if (debugCounter != -1) {
            debugCounter++;
            if (debugCounter == 10) {
                debug();
                debugCounter = -1;
            }
        }
    }
    
    /**
     * Checks if the server should be pinged. Takes into account the approximate
     * passed time and how active the connection was before it stopped being
     * active.
     */
    private void checkConnection() {
        connectionCheckedCount++;
        if (connectionCheckedCount == PING_AFTER_CHECKS) {
            send("PING");
        } else if (connectionCheckedCount > PING_AFTER_CHECKS) {
            LOGGER.log(Logging.USERINFO, "Warning: Server not responding");
            warning("No message received from server after PING for "+SOCKET_BLOCK_TIMEOUT+"ms");
            debugCounter = 0;
            // Wait a bit longer until next Ping
            connectionCheckedCount -= PING_AFTER_CHECKS*2;
        }
    }
    
    /**
     * Closes the connection if still connected and cleans up.
     */
    synchronized public void close() {
        if (connected) {
            info("Closing socket.");
            try {
                out.close();
                in.close();
                socket.close();
            } catch (IOException ex) {
                warning("Error closing socket: "+ex);
            }
            if (disconnectReason == -1) {
                disconnectReason = Irc.ERROR_CONNECTION_CLOSED;
            }
            if (disconnectMessage == null) {
                disconnectMessage = "";
            }
            irc.disconnected(disconnectReason, disconnectMessage);
        }
        connected = false;
    }
    
    /**
     * Send a line of data to the server.
     * 
     * @param data 
     */
    synchronized public void send(String data) {
        data = StringUtil.removeLinebreakCharacters(data);
        debugBuffer.add(new Msg(System.currentTimeMillis(), data, true));
        irc.sent(data);
        out.print(data+"\r\n");
        out.flush();
    }
    
    public void debug() {
        List<Msg> recent = debugBuffer.getItems();
        StringBuilder b = new StringBuilder();
        b.append(idPrefix);
        b.append(address);
        if (secured) {
            b.append(" (secured)");
        }
        b.append(" / Check count: ");
        b.append(connectionCheckedCount).append("/").append(PING_AFTER_CHECKS);
        b.append("\n");
        for (Msg msg : recent) {
            b.append(DateTime.formatExact(msg.time));
            b.append(" ");
            if (msg.sent) {
                b.append("<<< ");
            }
            b.append(filterToken(msg.raw)).append("\n");
        }
        LOGGER.info(b.toString());
    }
    
    private static String filterToken(String msg) {
        if (msg.startsWith("PASS")) {
            return "PASS <token>";
        }
        return msg;
    }
    
    private static class Msg {
        
        public final long time;
        public final String raw;
        public final boolean sent;
        
        public Msg(long time, String raw, boolean sent) {
            this.time = time;
            this.raw = raw;
            this.sent = sent;
        }
    }
    
}
