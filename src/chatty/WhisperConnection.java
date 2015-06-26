
package chatty;

import chatty.util.settings.Settings;
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
    
    public void whisper(String nick, String message, boolean onlyReply) {
        if (onlyReply && c.getExistingUser(WHISPER_CHANNEL, nick) == null) {
            listener.info("Didn't receive any whisper from '"+nick+"', use /w command");
            return;
        }
        if (isAvailable()) {
            c.sendSpamProtectedMessage("#jtv", "/w "+nick+" "+message, false);
            User user = c.getUser(WHISPER_CHANNEL, nick);
            listener.whisperSent(user, message);
            if (isUserIgnored(user)) {
                listener.info("You haven't allowed to receive whispers from "+user);
            }
        } else {
            listener.info("Can't send whisper: not connected");
        }
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
