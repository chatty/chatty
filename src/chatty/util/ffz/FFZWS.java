
package chatty.util.ffz;

import chatty.util.jws.JWSClient;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handle FFZ websocket protocol (id for sent commands, store commands).
 * 
 * @author tduva
 */
public class FFZWS extends JWSClient {
    
    private static final Logger LOGGER = Logger.getLogger(FFZWS.class.getName());

    private final Object LOCK = new Object();
    
    private int sentCount;
    private final MessageHandler handler;
    private final Map<Integer, String> commandId = new HashMap<>();
    
    public FFZWS(URI server, MessageHandler handler) {
        super(server);
        this.handler = handler;
        setDebugPrefix("[FFZ-WS] ");
    }
    
    @Override
    public void handleReceived(String text) {
        handler.handleReceived(text);
        try {
            String[] split = text.split(" ", 3);
            int id = Integer.parseInt(split[0]);
            String command = split[1];
            String params = "";
            if (split.length == 3) {
                params = split[2];
            }
            handleCommand(id, command, params);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
            LOGGER.warning("[FFZ-WS] Invalid message: "+text);
        }
    }

    @Override
    public void handleSent(String text) {
        handler.handleSent(text);
    }

    @Override
    public void handleConnect(JWSClient c) {
        reset();
        handler.handleConnect();
    }

    @Override
    public void handleDisconnect(int code) {
    }
    
    private void reset() {
        synchronized(LOCK) {
            sentCount = 0;
            commandId.clear();
        }
    }
    
    private void handleCommand(int id, String command, String params) {
        String originCommand = "";
        synchronized(LOCK) {
            String c = commandId.get(id);
            if (c != null) {
                originCommand = c;
            }
        }
        handler.handleCommand(id, command, params, originCommand);
        if (command.equals("error")) {
            LOGGER.warning("[FFZ-WS] Error: "+params);
        }
    }
    
    public void sendCommand(String command, String data) {
        String toSend = null;
        synchronized(LOCK) {
            if (isOpen()) {
                sentCount++;
                if (data == null) {
                    toSend = String.format("%d %s", sentCount, command);
                } else {
                    toSend = String.format("%d %s %s", sentCount, command, data);
                }
                commandId.put(sentCount, command);
            }
        }
        if (toSend != null) {
            sendMessage(toSend);
        }
    }
    
    public static interface MessageHandler {
        public void handleReceived(String text);
        public void handleSent(String sent);
        public void handleCommand(int id, String command, String params, String originCommand);
        public void handleConnect();
    }
    
}
