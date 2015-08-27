
package chatty;

import chatty.util.settings.Settings;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class WhisperConnection {
    
    public static final String WHISPER_CHANNEL = "$[whisper]";
    
    public static final int DISPLAY_IN_CHAT = 0;
    public static final int DISPLAY_ONE_WINDOW = 1;
    public static final int DISPLAY_PER_USER = 2;
    
    private final TwitchConnection c;
    private final WhisperListener listener;
    private final Settings settings;
    
    private final String AUTO_RESPOND_MESSAGE = "[Auto-Message] This user has "
            + "not allowed to receive whispers from you.";
    private final Set<String> autoRespondedTo = new HashSet<>();
    
    public WhisperConnection(WhisperListener listener, Settings settings) {
        this.listener = listener;
        this.settings = settings;
        c = new TwitchConnection(new Events(), settings, "whisper");
        c.setWhisperConnection(true);
    }
    
    public void disconnect() {
        c.disconnect();
    }
    
    public void setUsericonManager(UsericonManager m) {
        c.setUsericonManager(m);
    }
    
    public void setAddressbook(Addressbook a) {
        c.setAddressbook(a);
    }
    
    public void setUsercolorManager(UsercolorManager m) {
        c.setUsercolorManager(m);
    }
    
    public String getConnectionInfo() {
        return c.getConnectionInfo();
    }
    
    public boolean isOffline() {
        return c.isOffline();
    }
    
    public boolean isAvailable() {
        //return !c.isOffline() && c.onChannel("#jtv");
        return c.isRegistered();
    }
    
    /**
     * Check whether the whisper feature is enabled and configured correctly.
     * 
     * @return true if enabled and configured correctly, false otherwise
     */
    public boolean isEnabled() {
        if (!settings.getBoolean("whisperEnabled")) {
            return false;
        }
        String server = settings.getString("groupChatServer");
        String port = settings.getString("groupChatPort");
        if (server.isEmpty() || port.isEmpty()) {
            return false;
        }
        return true;
    }
    
    /**
     * Connect to the Group Chat Server (specified in the settings) using the
     * given name and password. If already connected, this will simply store
     * the given name/password and not connect. It may use the stored data to
     * reconnect.
     * 
     * @param username
     * @param password 
     */
    public void connect(String username, String password) {
        if (settings.getBoolean("whisperEnabled")) {
            String server = settings.getString("groupChatServer");
            String port = settings.getString("groupChatPort");
            if (server.isEmpty() || port.isEmpty()) {
                listener.info("Whisper feature: No server/port defined (read help)");
                return;
            }
            c.connect(server, port, username, password, new String[]{});
        }
    }
    
    /**
     * The whisper command in the form: {@code /w <target> <message>}
     * 
     * @param parameter The target and message in a String
     * @param onlyReply Whether to only send a whisper to people you already
     * have whispered with this session
     */
    public void whisperCommand(String parameter, boolean onlyReply) {
        if (parameter == null) {
            listener.info("Whisper: Invalid parameters.");
            return;
        }
        String[] split = parameter.split(" ", 2);
        if (split.length == 2) {
            whisper(split[0], split[1], onlyReply);
        } else {
            listener.info("Whisper: Invalid parameters.");
        }
    }
    
    /**
     * Whisper entered directly into the channel input box of a whisper tab, so
     * get the target of the whisper.
     * 
     * If a whisper is entered in the combined whisper channel, then the message
     * has to be in the format {@code <target> <message>}. In case the user only
     * types the message, whispers entered this way will only be send to users
     * that the local user has already whispered with this session.
     * 
     * @param channel The channel
     * @param message The message entered, which may be the {@code <message>} or
     * {@code <target> <message>} depending on the channel
     */
    public void whisperChannel(String channel, String message) {
        if (channel.equals(WHISPER_CHANNEL)) {
            whisperCommand(message, true);
        } else if (channel.startsWith("$") && channel.length() > 1) {
            String nick = channel.substring(1);
            whisper(nick, message, false);
        } else {
            listener.info("Whisper: Invalid parameters.");
        }
    }
    
    /**
     * Send a whisper.
     * 
     * @param nick The target of the whisper
     * @param message The message
     * @param onlyReply If true, it will only send a whisper to people that have
     * already whispered with you this session
     */
    public void whisper(String nick, String message, boolean onlyReply) {
        if (!isEnabled()) {
            listener.info("Whisper feature not enabled (<Main - Settings - Advanced>)");
            return;
        }
        if (onlyReply && c.getExistingUser(WHISPER_CHANNEL, nick) == null) {
            listener.info("Didn't receive any whisper from '"+nick+"', use /w command");
            return;
        }
        if (isAvailable()) {
            if (!rawWhisper(nick, message)) {
                listener.info("# Whisper not sent (spam protection): " + message);
            } else {
                User user = c.getUser(WHISPER_CHANNEL, nick);
                listener.whisperSent(user, message);
                if (isUserIgnored(user)) {
                    listener.info("You haven't allowed to receive whispers from " + user);
                }
            }
        } else {
            listener.info("Can't send whisper: not connected");
        }
    }
    
    private boolean rawWhisper(String nick, String message) {
        return c.sendSpamProtectedMessage("#jtv", "/w "+nick+" "+message, false);
    }
    
    /**
     * Check if whispers from the given user should be output or ignored.
     * @param user The user to check
     * @return true if whispers from the given user should be output, false
     * otherwise
     */
    private boolean isUserAllowed(User user) {
        if (user.hasCategory("blockwhisper")) {
            return false;
        }
        if (settings.getBoolean("whisperWhitelist") && !user.hasCategory("whisper")) {
            return false;
        }
        return true;
    }
    
    private boolean isUserIgnored(User user) {
        if (settings.listContains("ignoredUsersWhisper", user.nick)) {
            return true;
        }
        return !isUserAllowed(user);
    }
    
    private class Events implements TwitchConnection.ConnectionListener {

        @Override
        public void onJoinAttempt(String channel) {
        }

        @Override
        public void onChannelJoined(String channel) {
        }

        @Override
        public void onChannelLeft(String channel) {
        }

        @Override
        public void onJoin(User user) {
        }

        @Override
        public void onPart(User user) {
        }

        @Override
        public void onUserAdded(User user) {
        }

        @Override
        public void onUserRemoved(User user) {
        }

        @Override
        public void onUserlistCleared(String channel) {
        }

        @Override
        public void onUserUpdated(User user) {
        }

        @Override
        public void onChannelMessage(User user, String message, boolean action, String emotes) {
            
        }

        @Override
        public void onNotice(String message) {
        }

        @Override
        public void onInfo(String channel, String infoMessage) {
            if (channel.equals("#jtv")) {
                listener.info(infoMessage);
            }
        }

        @Override
        public void onInfo(String message) {

        }

        @Override
        public void onGlobalInfo(String message) {
        }

        @Override
        public void onBan(User user) {
        }

        @Override
        public void onRegistered() {
            listener.info("Whisper feature: connected");
        }

        @Override
        public void onDisconnect(int reason, String reasonMessage) {
            listener.info("Whisper feature: Disconnected ("+reasonMessage+")");
        }

        @Override
        public void onMod(User user) {
        }

        @Override
        public void onUnmod(User user) {
        }

        @Override
        public void onConnectionStateChanged(int state) {
        }

        @Override
        public void onSpecialUserUpdated() {
        }

        @Override
        public void onConnectError(String message) {
        }

        @Override
        public void onJoinError(Set<String> toJoin, String errorChannel, TwitchConnection.JoinError error) {
        }

        @Override
        public void onRawReceived(String text) {
            listener.onRawReceived(text);
        }

        @Override
        public void onRawSent(String text) {
            listener.onRawSent(text);
        }

        @Override
        public void onHost(String channel, String target) {
        }

        @Override
        public void onChannelCleared(String channel) {
        }

        @Override
        public void onWhisper(User user, String message, String emotes) {
            if (isUserAllowed(user)) {
                listener.whisperReceived(user, message, emotes);
            }
            if (isUserIgnored(user) && settings.getBoolean("whisperAutoRespond")) {
                if (!autoRespondedTo.contains(user.nick)) {
                    rawWhisper(user.nick, AUTO_RESPOND_MESSAGE);
                    autoRespondedTo.add(user.nick);
                }
            } else {
                autoRespondedTo.remove(user.nick);
            }
        }
        
    }
    
    public interface WhisperListener {
        public void whisperReceived(User user, String message, String emotes);
        public void whisperSent(User to, String message);
        public void info(String message);
        public void onRawSent(String text);
        public void onRawReceived(String text);
    }
    
}
