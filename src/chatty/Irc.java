
package chatty;

import chatty.util.DateTime;
import chatty.util.DelayedActionQueue;
import chatty.util.DelayedActionQueue.DelayedActionListener;
import chatty.util.irc.MsgParameters;
import chatty.util.irc.MsgTags;
import chatty.util.irc.ParsedMsg;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author tduva
 */
public abstract class Irc {
    
    private static final Logger LOGGER = Logger.getLogger(Irc.class.getName());
    
    /**
     * Delay between JOINs (in milliseconds).
     */
    private static final int JOIN_DELAY = 750;
    
    private final AddressManager addressManager = new AddressManager();
    private final DelayedActionQueue<String> joinQueue
            = DelayedActionQueue.create(new DelayedJoinAction(), JOIN_DELAY);
    
    private String nick;
    private String pass;
    
    private Connection connection;
    
    private String quitmessage = "Quit";
    
    private String connectedIp = "";
    private int connectedPort;
    private long connectedSince = -1;
    
    private volatile int state = STATE_OFFLINE;
    
    /**
     * State while reconnecting.
     */
    public static final int STATE_RECONNECTING = -1;
    /**
     * State while being offline, either not connected or already disconnected
     * without reconnecting.
     */
    public static final int STATE_OFFLINE = 0;
    /**
     * State value while trying to connect (opening socket and streams).
     */
    public static final int STATE_CONNECTING = 1;
    /**
     * State value after having connected (socket and streams successfully opened).
     */
    public static final int STATE_CONNECTED = 2;
    /**
     * State value once the connection has been accepted by the IRC Server
     * (registered).
     */
    public static final int STATE_REGISTERED = 3;
    
    /**
     * Disconnect reason value for Unknown host.
     */
    public static final int ERROR_UNKNOWN_HOST = 100;
    /**
     * Disconnect reason value for socket timeout.
     */
    public static final int ERROR_SOCKET_TIMEOUT = 101;
    /**
     * Disconnect reason value for socket error.
     */
    public static final int ERROR_SOCKET_ERROR = 102;
    /**
     * Disconnect reason value for requested disconnect, meaning the user
     * wanted to disconnect from the server.
     */
    public static final int REQUESTED_DISCONNECT = 103;
    /**
     * Disconnect reason value for when the connection was closed.
     */
    public static final int ERROR_CONNECTION_CLOSED = 104;
    
    public static final int ERROR_REGISTRATION_FAILED = 105;
    
    public static final int REQUESTED_RECONNECT = 106;
    
    public static final int SSL_ERROR = 107;
    
    /**
     * Indicates that the user wanted the connection to be closed.
     */
    private boolean requestedDisconnect = false;
    
    private final String id;
    private final String idPrefix;
    
    public Irc(String id) {
        this.id = id;
        this.idPrefix = "["+id+"] ";
    }
    
    private void info(String message) {
        LOGGER.info(idPrefix+message);
    }
    
    private void warning(String message) {
        LOGGER.warning(idPrefix+message);
    }
    
    /**
     * Set a new connection state.
     * 
     * @param newState 
     */
    protected void setState(int newState) {
        this.state = newState;
    }
    
    /**
     * Get the current connection state
     * 
     * @return 
     */
    public int getState() {
        return state;
    }
    
    public boolean isRegistered() {
        return state == STATE_REGISTERED;
    }
    
    public boolean isOffline() {
        return state == STATE_OFFLINE;
    }
    
    public String getIp() {
        return connectedIp;
    }
    
    public String getConnectionInfo() {
        if (state >= STATE_CONNECTED) {
            return connectedIp+":"+connectedPort;
        }
        return null;
    }
    
    public String getConnectedSince() {
        return DateTime.ago(connectedSince);
    }
 
    /**
     * Outputs the debug string
     * 
     * @param line 
     */
    abstract public void debug(String line);
    
    /**
     * Connects to a server using the given credentials. This starts a new
     * Thread, running the Connection class, after checking if already
     * connected.
     * 
     * @param server The ip or host of the server
     * @param port The port of the server
     * @param nick The nick to connect with
     * @param pass The password (required at Twitch)
     * @param securedPorts Which ports should be treated as SSL
     */
    public final void connect(final String server, final String port,
            final String nick, final String pass, Collection<Integer> securedPorts) {

        if (state >= STATE_CONNECTED) {
            warning("Already connected.");
            return;
        }
        if (state >= STATE_CONNECTING) {
            warning("Already trying to connect.");
            return;
        }
        
        InetSocketAddress address = null;
        try {
            address = addressManager.getAddress(server, port);
        } catch (UnknownHostException ex) {
            onConnectionAttempt(server, -1, false);
            warning("Could not resolve host: "+server);
            disconnected(ERROR_UNKNOWN_HOST);
            return;
        }
        if (address == null) {
            onConnectionAttempt(null, -1, false);
            warning("Invalid address: "+server+":"+port);
            return;
        }

        state = STATE_CONNECTING;
        
        // Save for further use
        this.pass = pass;
        this.nick = nick;
        
        // Only give server and port, nick and pass are saved in this class
        // and sent once the initial connection has been established.
        //System.out.println(securedPorts+" "+address.getPort());
        boolean secured = securedPorts.contains(address.getPort());
        onConnectionAttempt(address.getHostString(), address.getPort(), secured);
        connection = new Connection(this,address, id, secured);
        new Thread(connection).start();
    }
    
    
    /**
     * Disconnect if connected.
     */
    public boolean disconnect() {
        if (state > STATE_CONNECTING && connection != null) {
            requestedDisconnect = true;
            quit();
            connection.close();
            return true;
        }
        return false;
    }
    
    /**
     * Send a QUIT to the server, after which the server should close the
     * connection.
     */
    private void quit() {
        sendCommand("QUIT",quitmessage);
    }
    
    public void simulate(String data) {
        received(data);
    }
    
    public void debugConnection() {
        if (connection != null) {
            connection.debug();
        }
    }

    /**
     * Parse IRC-Messages receveived from the Connection-Thread.
     * 
     * @param data The line of data received
     */
    protected void received(String data) {
        if (data == null) {
            return;
        }
        raw(data);
        
        ParsedMsg p = ParsedMsg.parse(data);

        if (p != null) {
            receivedCommand(p.getPrefix(), p.getNick(), p.getCommand(),
                    p.getParameters(), p.getTags());
        }
    }
    
    /**
     * Message has already been parsed, so let's check what command it is.
     * 
     * @param prefix
     * @param nick
     * @param command The name of the command, can't be null
     * @param parameters String array of parameters, array can have different
     * length, so checking there may be necessary
     * @param tags The IRCv3 tags, can be null
     */
    private void receivedCommand(String prefix, String nick, String command,
            MsgParameters parameters, MsgTags tags) {
        
        parsed(prefix, command, parameters);
        
        if (!parameters.isEmpty(0)) {
            if (parameters.isChan(0)) {
                onChannelCommand(tags, nick, parameters.get(0), command, parameters.getOrEmpty(1));
            } else {
                onCommand(nick, command, parameters.get(0), parameters.getOrEmpty(1), tags);
            }
        }
        
        if (command.equals("PING")) {
            sendCommand("PONG", parameters.getOrEmpty(0));
        }
        else if (command.equals("PRIVMSG")) {
            if (parameters.has(1)) {
                String channel = parameters.get(0);
                String message = parameters.get(1);
                if (parameters.get(0).startsWith("#")) {
                    if (message.charAt(0) == (char) 1 && message.startsWith("ACTION", 1)) {
                        onChannelMessage(channel, nick, prefix, message.substring(7).trim(), tags, true);
                    } else {
                        onChannelMessage(channel, nick, prefix, message, tags, false);
                    }
                } else if (channel.equalsIgnoreCase(this.nick)) {
                    onQueryMessage(nick, prefix, message);
                }
            } else if (parameters.has(0)) {
                /**
                 * For hosting message, which is as follows (no channel/name as
                 * PRIVMSG target): :jtv!jtv@jtv.tmi.twitch.tv PRIVMSG
                 * :tduvatest is now hosting you for 0 viewers. [0]
                 */
                String message = parameters.get(0);
                onQueryMessage(nick, prefix, message);
            }
        }
        else if (command.equals("NOTICE")) {
            if (parameters.has(1)) {
                String channel = parameters.get(0);
                String message = parameters.get(1);
                if (!channel.startsWith("#")) {
                    onNotice(nick, prefix, message);
                } else {
                    onNotice(channel, message, tags);
                }
            }
        }
        else if (command.equals("USERNOTICE")) {
            if (parameters.isChan(0)) {
                String channel = parameters.get(0);
                String message = parameters.getOrEmpty(1);
                onUsernotice(channel, message, tags);
            }
        }
        else if (command.equals("JOIN")) {
            if (parameters.has(0)) {
                String channel = parameters.get(0);
                onJoin(channel, nick);
            }
        }
        else if (command.equals("PART")) {
            if (parameters.has(0)) {
                String channel = parameters.get(0);
                onPart(channel, nick);
            }
        }
        else if (command.equals("MODE")) {
            if (parameters.size() == 3) {
                String chan = parameters.get(0);
                String mode = parameters.get(1);
                String name = parameters.get(2);
                
                if (mode.length() == 2) {
                    String modeChar = mode.substring(1, 2);
                    if (mode.startsWith("+")) {
                        onModeChange(chan,name,true,modeChar, prefix);
                    }
                    else if (mode.startsWith("-")) {
                        onModeChange(chan,name,false,modeChar, prefix);
                    }
                    
                }
            }
        }
        // Now the connection is really going.. ;)
        else if (command.equals("004")) {
            setState(STATE_REGISTERED);
            onRegistered();
        }
        // Nick list, usually on channel join
        else if (command.equals("353")) {
            if (parameters.size() == 4 && parameters.get(1).equals("=") && parameters.isChan(2)) {
                String[] names = parameters.get(3).split(" ");
                onUserlist(parameters.get(2), names);
            }
            
        }
        // WHO response not really correct now
        else if (command.equals("352")) {
            //String[] parts = trailing.split(" ");
            //if (parts.length > 1) {
                //onWhoResponse(parts[0],parts[1]);
            //}
        }
        else if (command.equals("USERSTATE")) {
            if (tags != null && parameters.isChan(0)) {
                String channel = parameters.get(0);
                onUserstate(channel, tags);
            }
        }
        else if (command.equals("GLOBALUSERSTATE")) {
            if (tags != null) {
                onGlobalUserstate(tags);
            }
        }
        else if (command.equals("CLEARCHAT")) {
            if (parameters.isChan(0)) {
                String channel = parameters.get(0);
                String message = parameters.getOrEmpty(1);
                if (message.isEmpty()) {
                    onClearChat(tags, channel, null);
                } else {
                    onClearChat(tags, channel, message);
                }
            }
        }
        else if (command.equals("CLEARMSG")) {
            if (parameters.isChan(0)) {
                String channel = parameters.get(0);
                String message = parameters.getOrEmpty(1);
                onClearMsg(tags, channel, message);
            }
        }
    }
    
    /**
     * Extracts the nick from the prefix (like nick!mail@host)
     * 
     * @param sender
     * @return 
     */
    public String getNickFromPrefix(String sender) {
        int endOfNick = sender.indexOf("!");
        if (endOfNick == -1) {
            return sender;
        }
        return sender.substring(0, endOfNick);
    }
    
    
    /**
     * Send any command with a parameter to the server
     * 
     * @param command
     * @param parameter 
     */
    public void sendCommand(String command,String parameter) {
        send(command+" :"+parameter);
        
    }
    
    /**
     * Joins {@code channel} on a queue, that puts some time between each join.
     * 
     * @param channel The name of the channel to join
     */
    public void joinChannel(String channel) {
        info("JOINING: " + channel);
        joinQueue.add(channel);
    }
    
    /**
     * Join a channel. This adds # in front if not there.
     * 
     * @param channel 
     */
    public void joinChannelImmediately(String channel) {
        if (state >= STATE_REGISTERED) {
            if (!channel.startsWith("#")) {
                channel = "#" + channel;
            }
            // if-condition for testing (to simulate failed joins)
            //if (new Random().nextBoolean()) {
                send("JOIN " + channel);
            //}
            onJoinAttempt(channel);
        }
    }
    
    /**
     * Listener for the join queue, which is called when the next channel can
     * be joined.
     */
    private class DelayedJoinAction implements DelayedActionListener<String> {

        @Override
        public void actionPerformed(String item) {
            info("JOIN: "+item+" (delayed)");
            joinChannelImmediately(item);
        }
        
    }
    
    /**
     * Part a channel. This adds # in front if not there.
     * 
     * @param channel 
     */
    public void partChannel(String channel) {
        if (!channel.startsWith("#")) {
            channel = "#"+channel;
        }
        send("PART "+channel);
    }
    
    /**
     * Send a message, usually to a channel.
     * 
     * @param to
     * @param message 
     * @param tags 
     */
    public void sendMessage(String to, String message, MsgTags tags) {
        if (!tags.isEmpty()) {
            send(String.format("@%s PRIVMSG %s :%s",
                    tags.toTagsString(),
                    to,
                    message));
        } else {
            send("PRIVMSG "+to+" :"+message);
        }
    }
    
    public void sendActionMessage(String to,String message) {
        send("PRIVMSG "+to+" :"+(char)1+"ACTION "+message+(char)1);
    }
    
    synchronized public void send(String data) {
        if (state > STATE_OFFLINE) {
            connection.send(data);
        }
    }
    
    /**
     * Called from the Connection Thread once the initial connection has
     * been established without an error.
     * 
     * So now work on getting the connection to the IRC Server going by
     * sending credentials and stuff.
     * 
     * @param ip
     * @param port
     */
    protected void connected(String ip, int port) {
        this.connectedIp = ip;
        this.connectedPort = port;
        this.connectedSince = System.currentTimeMillis();
        setState(Irc.STATE_CONNECTED);
        onConnect();
        if (pass != null) {
            send("PASS " + pass);
        }
        //send("USER " + nick + " * * : "+nick);
        send("NICK " + nick);
        send(String.format("USER %s 0 * :Chatty", nick));
    }
    
    /**
     * Called by the Connection Thread, when the Connection was closed, be
     * it because it was closed by the server, the program itself or because
     * of an error.
     * 
     * @param reason The reason of the disconnect as defined in various
     * constants in this class
     * @param reasonMessage An error message or other information about the
     * disconnect
     */
    protected void disconnected(int reason, String reasonMessage) {
        // Clear any potential join queue, so it doesn't carry over to the next
        // connection
        joinQueue.clear();
        
        // Retrieve state before changing it, but must be changed before calling
        // onDisconnect() which might check the state when trying to reconnect
        int oldState = getState();
        setState(Irc.STATE_OFFLINE);
        
        // If connecting failed, then add it as an error
        if (!requestedDisconnect && oldState != STATE_REGISTERED && connection != null) {
            addressManager.addError(connection.getAddress());
        }

        if (requestedDisconnect) {
            // If the disconnect was requested (like the user clicking on
            // a menu item), include the appropriate reason
            requestedDisconnect = false;
            onDisconnect(REQUESTED_DISCONNECT, reasonMessage);
        } else if (reason == ERROR_CONNECTION_CLOSED && oldState != STATE_REGISTERED) {
            onDisconnect(ERROR_REGISTRATION_FAILED, reasonMessage);
        } else {
            onDisconnect(reason, reasonMessage);
        }
    }
    
    /**
     * Convenience method without a reason message.
     * 
     * @param reason 
     */
    void disconnected(int reason) {
        disconnected(reason,"");
    }
    
    /*
     * Methods that can by overwritten by another Class
     */
    
    void onChannelMessage (String channel, String nick, String from, String text, MsgTags tags, boolean action) {}
    
    void onQueryMessage (String nick, String from, String text) {}
    
    void onNotice(String nick, String from, String text) {}
    
    void onNotice(String channel, String text, MsgTags tags) { }
    
    void onJoinAttempt(String channel) {}
    
    void onJoin(String channel, String nick) {}
    
    void onPart(String channel, String nick) { }
    
    void onModeChange(String channel, String nick, boolean modeAdded, String mode, String prefix) { }
    
    void onUserlist(String channel, String[] nicks) {}
    
    void onWhoResponse(String channel, String nick) {}
    
    void onConnectionAttempt(String server, int port, boolean secured) { }
    
    void onConnect() { }
    
    void onRegistered() { }
    
    void onDisconnect(int reason, String reasonMessage) { }
    
    void parsed(String prefix, String command, MsgParameters parameters) { }
    
    void raw(String message) { }
    
    void sent(String message) { }
    
    void onUserstate(String channel, MsgTags tags) { }
    
    void onGlobalUserstate(MsgTags tags) { }
    
    void onClearChat(MsgTags tags, String channel, String name) { }
    
    void onClearMsg(MsgTags tags, String channel, String msg) { }
    
    void onChannelCommand(MsgTags tags, String nick, String channel, String command, String trailing) { }
    
    void onCommand(String nick, String command, String parameter, String text, MsgTags tags) { }
    
    void onUsernotice(String channel, String message, MsgTags tags) { }
}
