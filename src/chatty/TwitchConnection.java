package chatty;

import chatty.lang.Language;
import chatty.gui.colors.UsercolorManager;
import chatty.util.api.usericons.UsericonManager;
import chatty.ChannelStateManager.ChannelStateListener;
import chatty.util.BotNameManager;
import chatty.util.MsgTags;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author tduva
 */
public class TwitchConnection {
    
    public enum JoinError {
        NOT_REGISTERED, ALREADY_JOINED, INVALID_NAME
    }
    
    private static final Logger LOGGER = Logger.getLogger(TwitchConnection.class.getName());

    private final ConnectionListener listener;
    private final Settings settings;

    /**
     * Channels that should be joined after connecting.
     */
    private volatile String[] autojoin;
    
    /**
     * Channels that are open in the program (in tabs if it's more than one).
     */
    private final Set<String> openChannels = Collections.synchronizedSet(new HashSet<String>());

    /**
     * How many times to try to reconnect
     */
    private long maxReconnectionAttempts = 40;
    
    /**
     * The time in seconds between reconnection attempts. The first entry is the
     * time for the first attempt, second entry for the second attempt and so
     * on. The last entry is used for all further attempts.
     */
    private final static int[] RECONNECTION_DELAY = new int[]{1, 5, 5, 10, 10, 60};

    private volatile Timer reconnectionTimer;

    /**
     * The username to send to the server. This is stored to reconnect.
     */
    private volatile String username;

    /**
     * The actual password to send to the server. This can be a token as well as
     * a password. This is stored to reconnect.
     */
    private volatile String password;
    
    private volatile String server;

    private volatile String serverPorts = "6667";

    /**
     * Holds the UserManager instance, which manages all the user objects.
     */
    protected UserManager users = new UserManager();

    private final IrcConnection irc;

    private final TwitchCommands twitchCommands;
    private final SpamProtection spamProtection;
    private final ChannelStateManager channelStates = new ChannelStateManager();

    public TwitchConnection(final ConnectionListener listener, Settings settings,
            String label) {
        irc = new IrcConnection(label);
        this.listener = listener;
        this.settings = settings;
        this.twitchCommands = new TwitchCommands(this);
        spamProtection = new SpamProtection();
        spamProtection.setLinesPerSeconds(settings.getString("spamProtection"));
        users.setCapitalizedNames(settings.getBoolean("capitalizedNames"));
        users.addListener(new UserManager.UserManagerListener() {

            @Override
            public void userUpdated(User user) {
                if (user.isOnline()) {
                    listener.onUserUpdated(user);
                }
            }
        });
    }
    
    private TimerTask getReconnectionTimerTask() {
        return new TimerTask() {

            @Override
            public void run() {
                reconnect();
            }
        };
    }
    
    public void simulate(String data) {
        irc.simulate(data);
    }
    
    public void addChannelStateListener(ChannelStateListener listener) {
        channelStates.addListener(listener);
    }
    
    public ChannelState getChannelState(String channel) {
        return channelStates.getState(channel);
    }
    
    public void setUsercolorManager(UsercolorManager m) {
        users.setUsercolorManager(m);
    }
    
    public void setAddressbook(Addressbook addressbook) {
        users.setAddressbook(addressbook);
    }
    
    public void setUsericonManager(UsericonManager usericonManager) {
        users.setUsericonManager(usericonManager);
    }
    
    public void setBotNameManager(BotNameManager m) {
        users.setBotNameManager(m);
    }
    
    public void setCustomNamesManager(CustomNames customNames) {
        users.setCustomNamesManager(customNames);
    }
    
    public void setMaxReconnectionAttempts(long num) {
        this.maxReconnectionAttempts = num;
    }
    
    public void setSpamProtection(String setting) {
        spamProtection.setLinesPerSeconds(setting);
    }
    
    public String getSpamProtectionInfo() {
        return spamProtection.toString();
    }
    
    public User getUser(String channel, String name) {
        return users.getUser(channel, name);
    }
    
    public User getExistingUser(String channel, String name) {
        name = StringUtil.toLowerCase(name);
        return users.getUserIfExists(channel, name);
    }
    
    /**
     * The username used for the last connection.
     * 
     * @return 
     */
    public String getUsername() {
        return username;
    }
    
    public boolean isUserlistLoaded(String channel) {
        return irc.isRegistered() && irc.userlistReceived.contains(channel);
    }
    
    public Set<String> getOpenChannels() {
        synchronized(openChannels) {
            return new HashSet<>(openChannels);
        }
    }
    
    /**
     * Gets the reconnection delay based on the number of attempts.
     * 
     * @param attempt The number of attempts
     * @return The delay in seconds
     */
    private int getReconnectionDelay(int attempt) {
        if (attempt < 1 || attempt > RECONNECTION_DELAY.length) {
            return getMaxReconnectionDelay();
        }
        return RECONNECTION_DELAY[attempt-1];
    }
    
    public int getState() {
        return irc.getState();
    }
    
    public boolean isOffline() {
        return irc.isOffline();
    }
    
    public boolean isRegistered() {
        return irc.isRegistered();
    }

    /**
     * Checks if actually joined to the given channel.
     *
     * @param channel
     * @return
     */
    public boolean onChannel(String channel) {
        return onChannel(channel, false);
    }

    public Set<String> getJoinedChannels() {
        return irc.getJoinedChannels();
    }
    
    public boolean isChannelOpen(String channel) {
        return openChannels.contains(channel);
    }
    
    public void closeChannel(String channel) {
        if (channel.equals(WhisperManager.WHISPER_CHANNEL)) {
            return;
        }
        partChannel(channel);
        openChannels.remove(channel);
        users.clear(channel);
        irc.cancelJoinAttempt(channel);
    }
    
    public void setAllOffline() {
        users.setAllOffline();
    }
    
    public void partChannel(String channel) {
        if (onChannel(channel)) {
            irc.partChannel(channel);
        }
    }

    /**
     * Checks if actually joined to the given channel and also, if not,
     * optionally outputs a message to inform the user about it.
     *
     * @param channel
     * @param showMessage
     * @return
     */
    public boolean onChannel(String channel, boolean showMessage) {
        boolean onChannel = irc.joinedChannels.contains(channel);
        if (showMessage && !onChannel) {
            if (channel == null || channel.isEmpty()) {
                listener.onInfo("Not in a channel");
            } else {
                listener.onInfo("Not in this channel (" + channel + ")");
            }
        }
        return onChannel;
    }


    /**
     * Actually performs the reconnect.
     */
    protected void reconnect() {
        cancelReconnectionTimer();
        
        //listener.onGlobalInfo("Attempting to reconnect.. ("+irc.connectionAttempts+"/"+maxReconnectionAttempts+")");
        connect();
    }
    
    private boolean cancelReconnectionTimer() {
        if (reconnectionTimer != null) {
            reconnectionTimer.cancel();
            reconnectionTimer = null;
            return true;
        }
        return false;
    }
    
    /**
     * This actually connects to the server. All data necessary for connecting
     * should already be present at this point, however it still checks again if
     * it exists.
     * 
     * Even if connected, this will store the given data and potentially use it
     * for reconnecting.
     * 
     * @param server The server address to connect to
     * @param serverPorts The server ports to connect to (comma-seperated list)
     * @param username The username to use for connecting
     * @param password The password
     * @param autojoin The channels to join after connecting
     */
    public void connect(String server, String serverPorts, String username,
            String password, String[] autojoin) {
        this.server = server;
        this.serverPorts = serverPorts;
        this.username = username;
        users.setLocalUsername(username);
        this.password = password;
        this.autojoin = autojoin;
        connect();
    }
    
    /**
     * Connect to the main connection based on the current login data. Will only
     * connect it not already connected/connecting.
     */
    private void connect() {
        if (irc.getState() <= Irc.STATE_OFFLINE) {
            cancelReconnectionTimer();
            irc.connect(server,serverPorts,username,password, getSecuredPorts());
        } else {
            listener.onConnectError("Already connected or connecting.");
        }
    }
    
    private Collection<Integer> getSecuredPorts() {
        List setting = settings.getList("securedPorts");
        Collection<Integer> result = new HashSet<>();
        for (Object value : setting) {
            result.add(((Long)value).intValue());
        }
        return result;
    }
    
    public User getSpecialUser() {
        return users.specialUser;
    }
    
    /**
     * Gets the maximum reconnection delay defined.
     * 
     * @return The delay in seconds
     */
    private int getMaxReconnectionDelay() {
        return RECONNECTION_DELAY[RECONNECTION_DELAY.length - 1];
    }
    
    
    /**
     * Disconnect from the server or cancel trying to reconnect.
     *
     * @return true if the disconnect did something, or false if not actually
     * connected
     */
    public boolean disconnect() {
        if (cancelReconnectionTimer()) {
            listener.onGlobalInfo("Canceled reconnecting");
            irc.setState(Irc.STATE_OFFLINE);
            irc.connectionAttempts = 0;
        }
        boolean success = irc.disconnect();
        return success;
    }
    
    public void quit() {
        irc.disconnect();
    }

    public String getConnectionInfo() {
        String regular = irc.getConnectionInfo();
        if (regular == null) {
            return "Not connected.";
        }
        return "Connected to: "+regular;
    }
    
    public boolean autoRequestModsEnabled() {
        return settings.getBoolean("autoRequestMods");
    }
    
    public User localUserJoined(String channel) {
        return userJoined(channel, username);
    }
    
    public User getLocalUser(String channel) {
        return users.getUser(channel, username);
    }
    
    public void sendRaw(String text) {
        irc.send(text);
    }
    
    public boolean command(String channel, String command, String parameters,
            String msgId) {
        return twitchCommands.command(channel, msgId, command, parameters);
    }
    
    public void sendCommandMessage(String channel, String message, String echo) {
        sendCommandMessage(channel, message, echo, MsgTags.EMPTY);
    }
    
    /**
     * Send a spam protected command to a channel, with the given echo message
     * that will be displayed to the user.
     * 
     * This doesn't check if you're actually on the channel.
     *
     * @param channel The channel to send the message to
     * @param message The message to send (e.g. a moderation command)
     * @param echo The message to display to the user
     * @param tags
     */
    public void sendCommandMessage(String channel, String message, String echo,
            MsgTags tags) {
        if (sendSpamProtectedMessage(channel, message, false, tags)) {
            listener.onInfo(channel, echo);
        } else {
            listener.onInfo(channel, "# Command not sent to prevent ban: " + message);
        }
    }
    
    public boolean sendSpamProtectedMessage(String channel, String message, boolean action) {
        return sendSpamProtectedMessage(channel, message, action, MsgTags.EMPTY);
    }
    
    /**
     * Tries to send a spam protected message, which will either be send or not,
     * depending on the status of the spam protection.
     * 
     * <p>This doesn't check if you're actually on the channel.</p>
     *
     * @param channel The channel to send the message to
     * @param message The message to send
     * @param action
     * @return true if the message was send, false otherwise
     */
    public boolean sendSpamProtectedMessage(String channel, String message,
            boolean action, MsgTags tags) {
        if (!spamProtection.check()) {
            return false;
        } else {
            spamProtection.increase();
            if (action) {
                irc.sendActionMessage(channel, message);
            } else {
                irc.sendMessage(channel, message, tags);
            }
            return true;
        }
    }

    public int getNumJoinedChannels() {
        return irc.joinedChannels.size();
    }
    
    

    public void join(String channel) {
        irc.joinChannel(channel);
    }
    
    /**
     * Joins the channel with the given name, but only if the channel name
     * is deemed valid, it's possible to join channels at this point and we are
     * not already on the channel.
     * 
     * @param channel The name of the channel, with or without leading '#'.
     */
    public void joinChannel(String channel) {
        Set<String> channels = new HashSet<>();
        channels.add(channel);
        joinChannels(channels);
    }
    
    /**
     * Join a rename of channels. Sorts out invalid channels and outputs an error
     * message, then joins the valid channels.
     *
     * @param channels Set of channelnames (valid/invalid, leading # or not).
     */
    public void joinChannels(Set<String> channels) {
        Set<String> valid = new LinkedHashSet<>();
        Set<String> invalid = new LinkedHashSet<>();
        for (String channel : channels) {
            String checkedChannel = Helper.toValidChannel(channel);
            if (checkedChannel == null) {
                invalid.add(channel);
            } else {
                valid.add(checkedChannel);
            }
        }
        for (String channel : invalid) {
            listener.onJoinError(channels, channel, JoinError.INVALID_NAME);
        }
        joinValidChannels(valid);
    }

    /**
     * Joins the valid channels. If offline, opens the connect dialog with the
     * valid channels already entered.
     * 
     * @param valid A Set of valid channels (valid names, with leading #).
     */
    private void joinValidChannels(Set<String> valid) {
        if (valid.isEmpty()) {
            return;
        } else if (!irc.isRegistered()) {
            listener.onJoinError(valid, null, JoinError.NOT_REGISTERED);
        } else {
            for (String channel : valid) {
                if (onChannel(channel)) {
                    listener.onJoinError(valid, channel, JoinError.ALREADY_JOINED);
                } else {
                    join(channel);
                }
            }
        }
    }

    /**
     * IRC Connection which handles the messages (manages users, special
     * messages etc.) and redirects them to the listener accordingly.
     */
    private class IrcConnection extends Irc {
        
        /**
         * How many times was tried to connect. Reset when the connection is
         * fully established (registered).
         */
        private int connectionAttempts = 0;
        
        /**
         * At what time this connection last attempted to connect.
         */
        private long lastConnectionAttempt;

        private final JoinChecker joinChecker = new JoinChecker(this);
        
        /**
         * Channels that this connection has joined. This is per connection, so
         * the main and secondary connection have different data here.
         */
        private final Set<String> joinedChannels = Collections.synchronizedSet(
                new HashSet<String>());
        
        /**
         * The prefix used for debug messages, so it can be determined which
         * connection it is from.
         */
        private final String idPrefix;
        
        /**
         * This only applies to irc2. This is reset on every new connection.
         * It's set to true once either a JOIN or a userlist from any channel
         * is received. It roughly indicates that the connection has probably
         * started to receive users.
         */
        private Set<String> userlistReceived = Collections.synchronizedSet(
                new HashSet<String>());
        
        
        public IrcConnection(String id) {
            super(id);
            this.idPrefix= "["+id+"] ";
        }
        
        public int getLastConnectionAttemptAgo() {
            return (int)((System.currentTimeMillis() - lastConnectionAttempt) / 1000);
        }
        
        public Set<String> getJoinedChannels() {
            synchronized (joinedChannels) {
                return new HashSet<>(joinedChannels);
            }
        }
        
        public boolean onChannel(String channel) {
            return joinedChannels.contains(channel);
        }

        public boolean primaryOnChannel(String channel) {
            return irc.onChannel(channel);
        }
        
        @Override
        void onUserlist(String channel, String[] nicknames) {
            channel = channel.toLowerCase();
            if (isChannelOpen(channel)) {
                
                /**
                 * Don't clear userlist just yet if only local name is in the
                 * userlist, which may mean that the actual userlist is send
                 * using JOINs later.
                 */
                if (nicknames.length == 1
                        && nicknames[0].equalsIgnoreCase(username)) {
                    localUserJoined(channel);
                    return;
                }
                
                /**
                 * Clear current userlist before adding the new userlist if this
                 * is the first time receiving the userlist this connection.
                 */
                if (!userlistReceived.contains(channel)) {
                    clearUserlist(channel);
                }
                userlistReceived.add(channel);
                for (String nick : nicknames) {
                    userJoined(channel, nick);
                }
            }
        }
        
        @Override
        public void debug(String line) {
            LOGGER.info(idPrefix+line);
        }

        @Override
        void onConnectionAttempt(String server, int port, boolean secured) {
            connectionAttempts++;
            lastConnectionAttempt = System.currentTimeMillis();
            if (this != irc) {
                return;
            }
            
            if (server != null) {
                listener.onGlobalInfo("Trying to connect to " + server + ":" + port+(secured ? " (secured)" : ""));
            } else {
                listener.onGlobalInfo("Failed to connect (server or port invalid)");
            }
        }

        @Override
        void onConnect() {
            if (this == irc) {
                send("CAP REQ :twitch.tv/tags");
                send("CAP REQ :twitch.tv/commands");
                if (settings.getBoolean("membershipEnabled")) {
                    send("CAP REQ :twitch.tv/membership");
                }
                send("CAP END");
                //send("TWITCHCLIENT 4");
            }
            userlistReceived.clear();
        }

        @Override
        void onRegistered() {
            connectionAttempts = 1;

            if (this != irc) {
                return;
            }
            
            
            if (autojoin != null) {
                for (String channel : autojoin) {
                    joinChannel(channel);
                }
                /**
                 * Only use autojoin once, to prevent it from being used on
                 * reconnect (open channels should be used for that).
                 */
                autojoin = null;
            } else {
                joinChannels(getOpenChannels());
            }
            listener.onRegistered();
        }
        
        @Override
        void onDisconnect(int reason, String reasonMessage) {
            joinedChannels.clear();
            joinChecker.cancelAll();
            
            if (this == irc) {
                channelStates.reset();
                twitchCommands.clearModsAlreadyRequested(null);
                listener.onGlobalInfo(Language.getString("chat.disconnected",
                        Helper.makeDisconnectReason(reason, reasonMessage)));

                if (reason != Irc.REQUESTED_DISCONNECT) {
                    startReconnectTimer(reason);
                } else {
                    connectionAttempts = 0;
                }
                listener.onDisconnect(reason, reasonMessage);
            }
        }
        
        private void startReconnectTimer(int reason) {
            if (reconnectionTimer == null) {
                if (connectionAttempts > maxReconnectionAttempts
                        && maxReconnectionAttempts > -1) {
                    listener.onGlobalInfo("Gave up reconnecting. :(");
                } else {
                    int delay = getReconnectionDelay(connectionAttempts);
                    listener.onGlobalInfo(String.format(
                            "Attempting to reconnect in %s seconds.. (%s/%s)",
                            delay,
                            irc.connectionAttempts,
                            maxReconnectionAttempts < 0 ? "∞" : maxReconnectionAttempts));
                    setState(Irc.STATE_RECONNECTING);
                    reconnectionTimer = new Timer();
                    reconnectionTimer.schedule(getReconnectionTimerTask(), delay * 1000);
                }
            }
        }

        @Override
        void onJoinAttempt(String channel) {
            channel = channel.toLowerCase();
            joinChecker.joinAttempt(channel);
            if (this == irc) {
                listener.onJoinAttempt(channel);
                openChannels.add(channel);
            }
        }

        @Override
        void onJoin(String channel, String nick, String prefix) {
            channel = channel.toLowerCase();
            if (nick.equalsIgnoreCase(username)) {
                /**
                 * Local user has joined a channel.
                 */
                joinChecker.cancel(channel);
                debug("JOINED: " + channel);
                User user = userJoined(channel, nick);
                if (this == irc && !onChannel(channel)) {
                    listener.onChannelJoined(user);
                }
                joinedChannels.add(channel);
            } else {
                /**
                 * Another user has joined a channel we are currently in.
                 */
                if (isChannelOpen(channel)) {
                    if (!userlistReceived.contains(channel)) {
                        clearUserlist(channel);
                        // Add local user again, must be on this channel but
                        // may not be in the batch of joins again
                        localUserJoined(channel);
                    }
                    User user = userJoined(channel, nick);
                    listener.onJoin(user);
                    userlistReceived.add(channel);
                }
            }
        }
        
        private void clearUserlist(String channel) {
            //System.out.println("userlist cleared"+channel);
            users.setAllOffline(channel);
            listener.onUserlistCleared(channel);
        }
        
        public void cancelJoinAttempt(String channel) {
            joinChecker.cancel(channel);
        }

        @Override
        void onPart(String channel, String nick, String prefix, String message) {
            channel = channel.toLowerCase();
            if (nick.isEmpty()) {
                return;
            }
            if (!onChannel(channel)) {
                return;
            }
            if (nick.equalsIgnoreCase(username)) {
                /**
                 * Local User Leaving Channel
                 */
                joinChecker.cancel(channel);
                if (this == irc) {
                    userOffline(channel, nick);
                }
                joinedChannels.remove(channel);
                if (this == irc) {
                    twitchCommands.clearModsAlreadyRequested(channel);
                    // Remove users for this channel, clearing the userlist in the
                    // GUI shouldn't be necessary if this channel is closed since
                    // the GUI userlist is removed as well.
                    users.clear(channel);
                    listener.onChannelLeft(channel);
                    channelStates.reset(channel);
                }
                // Leaving the channel on the userlist connection means
                // the userlist can no longer be considered as received for
                // this channel.
                userlistReceived.remove(channel);
                debug("PARTED: "+channel);
            } else {
                if (isChannelOpen(channel)) {
                    User user = userOffline(channel, nick);
                    listener.onPart(user);
                }
            }
        }

        @Override
        void onModeChange(String channel, String nick, boolean modeAdded, String mode, String prefix) {
            channel = channel.toLowerCase();
            if (!onChannel(channel)) {
                return;
            }
            User user = users.getUser(channel, nick);
            if (modeAdded) {
                user.setMode(mode);
                if (mode.equals("o")) {
                    if (this == irc) {
                        listener.onMod(user);
                    }
                    if (!isUserlistLoaded(channel)) {
                        userJoined(user);
                    }
                }
            } else {
                user.setMode("");
                if (mode.equals("o")) {
                    if (this == irc) {
                        listener.onUnmod(user);
                    }
                }
            }
            // Notify userlist to update the changed user, but only if he is still
            // in the channel
            if (user.isOnline()) {
                listener.onUserUpdated(user);
            }
        }
        
        private void updateUserFromTags(User user, MsgTags tags) {
            if (tags.isEmpty()) {
                return;
            }
            /**
             * Any and all tag values may be null, so account for that when
             * checking against them.
             */
            // Whether anything in the user changed to warrant an update
            boolean changed = false;
            
            Map<String, String> badges = Helper.parseBadges(tags.get("badges"));
            if (user.setTwitchBadges(badges)) {
                changed = true;
            }
            
            if (settings.getBoolean("ircv3CapitalizedNames")) {
                if (user.setDisplayNick(StringUtil.trim(tags.get("display-name")))) {
                    changed = true;
                }
            }
            
            // Update color
            String color = tags.get("color");
            if (color != null && !color.isEmpty()) {
                user.setColor(color);
            }
            
            // Update user status
            boolean turbo = tags.isTrue("turbo") || badges.containsKey("turbo") || badges.containsKey("premium");
            if (user.setTurbo(turbo)) {
                changed = true;
            }
            if (user.setSubscriber(tags.isTrue("subscriber"))) {
                changed = true;
            }
            
            // Temporarily check both for containing a value as Twitch is
            // changing it
            String userType = tags.get("user-type");
            if (user.setModerator("mod".equals(userType))) {
                changed = true;
            }
            if (user.setStaff("staff".equals(userType))) {
                changed = true;
            }
            if (user.setAdmin("admin".equals(userType))) {
                changed = true;
            }
            if (user.setGlobalMod("global_mod".equals(userType))) {
                changed = true;
            }
            
            user.setId(tags.get("user-id"));
            
            if (changed && user != users.specialUser) {
                listener.onUserUpdated(user);
            }
        }

        @Override
        void onChannelMessage(String channel, String nick, String from, String text,
                MsgTags tags, boolean action) {
            channel = channel.toLowerCase();
            if (this != irc) {
                return;
            }
            if (nick.isEmpty()) {
                return;
            }
            if (onChannel(channel)) {
                if (settings.getBoolean("twitchnotifyAsInfo") && nick.equals("twitchnotify")) {
                    // Just output as Notification, subs shouldn't come over this anymore (soon),
                    // but just in case
                    listener.onInfo(channel, "[Notification] "+text);
                } else {
                    User user = userJoined(channel, nick);
                    updateUserFromTags(user, tags);
                    String emotesTag = tags.get("emotes");
                    String id = tags.get("id");
                    int bits = tags.getInteger("bits", 0);
                    listener.onChannelMessage(user, text, action, emotesTag, id, bits);
                }
            }
        }

        @Override
        void onNotice(String nick, String from, String text) {
            if (this != irc) {
                return;
            }
            // Should only be from the server for now
            listener.onNotice(text);
        }
        
        @Override
        void onNotice(String channel, String text, MsgTags tags) {
            channel = channel.toLowerCase();
            if (this != irc) {
                return;
            }
            if (tags.isValue("msg-id", "whisper_invalid_login")) {
                listener.onInfo(text);
            } else if (onChannel(channel)) {
                infoMessage(channel, text);
            } else {
                listener.onInfo(String.format("[Info/%s] %s", channel, text));
            }
        }
        
        @Override
        void onUsernotice(String channel, String message, MsgTags tags) {
            if (tags.isEmpty()) {
                return;
            }
            if (!onChannel(channel)) {
                return;
            }
            String login = tags.get("login");
            String text = StringUtil.removeLinebreakCharacters(tags.get("system-msg"));
            String emotes = tags.get("emotes");
            int months = tags.getInteger("msg-param-months", -1);
            if (StringUtil.isNullOrEmpty(login, text)) {
                return;
            }
            User user = userJoined(channel, login);
            updateUserFromTags(user, tags);
            if (tags.isValue("msg-id", "resub") || tags.isValue("msg-id", "sub")
                    || tags.isValue("msg-id", "subgift")) {
                listener.onSubscriberNotification(channel, user, text, message, months, emotes);
            } else if (tags.isValue("msg-id", "charity") && login.equals("twitch")) {
                listener.onInfo(channel, "[Charity] "+text);
            } else if (tags.isValue("msg-id", "raid")) {
                String m = text;
                if (!message.isEmpty()) {
                    m += " ["+message+"]";
                }
                listener.onInfo(channel, "[Raid] "+m);
            } else {
                // Just output like this if unknown, since Twitch keeps adding
                // new messages types for this
                listener.onInfo(channel, "[Usernotice] "+text);
            }
        }

        @Override
        void onQueryMessage(String nick, String from, String text) {
            if (this != irc) {
                return;
            }
            if (nick.startsWith("*")) {
                listener.onSpecialMessage(nick, text);
            }
            if (nick.equals("jtv")) {
                listener.onInfo("[Info] "+text);
            }
        }

        /**
         * Any kind of info message. This can be either from jtv (legacy) or the
         * new NOTICE messages to the channel.
         * 
         * @param channel
         * @param text 
         */
        private void infoMessage(String channel, String text) {
            if (text.startsWith("The moderators of")) {
                parseModeratorsList(text, channel);
            } else {
                listener.onInfo(channel, "[Info] " + text);
            }
        }

        /**
         * Counts the moderators in the /mods response and outputs the count.
         *
         * @param text The mesasge from jtv containing the comma-seperated
         * moderator list.
         * @param channel The channel the moderators list was received on, or
         * {@literal null} if the channel is unknown
         */
        private void parseModeratorsList(String text, String channel) {

            // Get list of users from message
            List<String> modsList = TwitchCommands.parseModsList(text);
            users.modsListReceived(channel, modsList);

            /**
             * Output messages only if either:
             *
             * a) No /mod response is currently expected to be silent Or b) The
             * channel was detected (TC3 or through guessing) and is not on the
             * list of channels with a /mod response expected to be silent
             *
             * a) has to be checked first, because b) might remove the channel,
             * so a) might be true even if it shouldn't be
             */
            if (!twitchCommands.waitingForModsSilent()
                    || (channel != null && !twitchCommands.removeModsSilent(channel))) {
                listener.onInfo(channel, "[Info] " + text);

                // Output appropriate message
                if (modsList.size() > 0) {
                    listener.onInfo(channel, "There are " + modsList.size() + " mods for this channel.");
                } else {
                    listener.onInfo(channel, "There are no mods for this channel.");
                }
            } else {
                debug("Silent mods list (" + channel + ")");
            }
        }

        /**
         * Inform the user that a channel was cleared. If {@literal channel} is
         * not {@literal null}, then it is output to that channel. Otherwise it
         * is output to the current channel.
         *
         * @param channel The channel that was cleared, or {@literal null} if
         * the channel is unknown
         */
        private void channelCleared(String channel) {
            listener.onChannelCleared(channel);
        }

        @Override
        void onWhoResponse(String channel, String nickname) {
            // Not working on Twitch Chat anyway
        }

        @Override
        protected void setState(int state) {
            super.setState(state);
            listener.onConnectionStateChanged(state);
        }

        /**
         * Checks if the given channel should be open.
         *
         * @param channel The channel name
         * @return
         */
        public boolean isChannelOpen(String channel) {
            return openChannels.contains(channel);
        }

        @Override
        public void raw(String text) {
            listener.onRawReceived(idPrefix+text);
        }

        @Override
        public void sent(String text) {
            if (text.startsWith("PASS")) {
                listener.onRawSent(idPrefix+"PASS <password>");
            } else {
                listener.onRawSent(idPrefix+text);
            }
        }
        
        @Override
        public void onUserstate(String channel, MsgTags tags) {
            channel = channel.toLowerCase();
            if (onChannel(channel)) {
                updateUserstate(channel, tags);
            }
        }
        
        @Override
        public void onGlobalUserstate(MsgTags tags) {
            updateUserstate(null, tags);
        }
        
        private void updateUserstate(String channel, MsgTags tags) {
            String emotesets = tags.get("emote-sets");
            if (channel != null) {
                /**
                 * Update state for the local user in the given channel, also
                 * assuming the user is now in that channel and thus adding the
                 * user if necessary.
                 */
                User user = localUserJoined(channel);
                updateUserFromTags(user, tags);
                user.setEmoteSets(emotesets);
            } else {
                /**
                 * Update all existing users with the local name, assuming that
                 * all the state is global if no channel is given.
                 */
                for (User user : users.getUsersByName(username)) {
                    updateUserFromTags(user, tags);
                    user.setEmoteSets(emotesets);
                }
            }

            /**
             * Update special user which can be used to initialize newly created
             * local users on other channels. This may be necessary when some
             * info is only being send in the GLOBALUSERSTATE command, which may
             * not be send after every join or message.
             * 
             * This may be updated with local and global info, however only the
             * global info is used to initialize newly created local users.
             * 
             * The special user is also used to get the emotesets the local user
             * has access to in other areas of the program like the Emotes
             * Dialog.
             */
            users.specialUser.setEmoteSets(emotesets);
            listener.onSpecialUserUpdated();
            updateUserFromTags(users.specialUser, tags);
        }
        
        @Override
        public void onClearChat(MsgTags tags, String channel, 
                String nick) {
            channel = channel.toLowerCase();
            if (nick != null) {
                // A single user was timed out/banned
                User user = users.getUserIfExists(channel, nick);
                if (user != null) {
                    long duration = tags.getLong("ban-duration", -1);
                    String reason = tags.get("ban-reason", "");
                    String targetMsgId = tags.get("target-msg-id", null);
                    if (isChannelOpen(user.getChannel())) {
                        listener.onBan(user, duration, reason, targetMsgId);
                    }
                }
            } else {
                // No nick specified means the channel is cleared
                channelCleared(channel);
            }
        }
        
        @Override
        public void onChannelCommand(MsgTags tags, String nick,
                String channel, String command, String trailing) {
            channel = channel.toLowerCase();
            if (command.equals("HOSTTARGET")) {
                String[] parameters = trailing.split(" ");
                if (parameters.length == 2) {
                    String target = parameters[0];
                    if (target.equals("-")) {
                        listener.onHost(channel, null);
                        channelStates.setHosting(channel, null);
                    } else {
                        listener.onHost(channel, target);
                        channelStates.setHosting(channel, target);
                    }
                }
            } else if (command.equals("ROOMSTATE")) {
                if (!tags.isEmpty()) {
                    /**
                     * ROOMSTATE doesn't always have to contain all states, so
                     * only work with those that are actually there (otherwise
                     * they may be inadvertently recognized as false).
                     */
                    if (tags.containsKey("r9k")) {
                        channelStates.setR9kMode(channel, tags.isTrue("r9k"));
                    }
                    if (tags.containsKey("emote-only")) {
                        channelStates.setEmoteOnly(channel, tags.isTrue("emote-only"));
                    }
                    if (tags.containsKey("subs-only")) {
                        channelStates.setSubmode(channel, tags.isTrue("subs-only"));
                    }
                    if (tags.containsKey("slow")) {
                        channelStates.setSlowmode(channel, tags.get("slow"));
                    }
                    if (tags.containsKey("broadcaster-lang")) {
                        channelStates.setLang(channel, tags.get("broadcaster-lang"));
                    }
                    if (tags.containsKey("followers-only")) {
                        channelStates.setFollowersOnly(channel, tags.get("followers-only"));
                    }
                    if (!tags.isEmpty("room-id")) {
                        listener.onRoomId(channel, tags.get("room-id"));
                    }
                }
            } else if (command.equals("SERVERCHANGE")) {
                listener.onInfo(channel, "*** You may be on the wrong server "
                        + "for this channel. Enter /fixserver to connect to the "
                        + "correct server (which may cause other channels to not "
                        + "work anymore, because Chatty only supports one main "
                        + "connection to a single server). ***");
            }
        }
        
        @Override
        public void onCommand(String nick, String command, String parameter, String text, MsgTags tags) {
            if (nick.isEmpty()) {
                return;
            }
            if (command.equals("WHISPER")) {
                User user = userJoined(WhisperManager.WHISPER_CHANNEL, nick);
                updateUserFromTags(user, tags);
                listener.onWhisper(user, text, tags.get("emotes"));
            }
        }
    }

    /**
     * Sets a user as offline, removing the user from the userlist, the user
     * won't be deleted though, for possible further reference
     *
     * @param channel
     * @param name
     * @return
     */
    public User userOffline(String channel, String name) {
        User user = users.getUser(channel, name);
        if (user != null) {
            user.setOnline(false);
            listener.onUserRemoved(user);
        }
        return user;
    }
    
    /**
     * Sets a user as online, add the user to the userlist if not already
     * online.
     *
     * @param channel The channel the user joined
     * @param name The name of the user
     * @return The User
     */
    public User userJoined(String channel, String name) {
        User user = users.getUser(channel, name);
        return userJoined(user);
    }

    public User userJoined(User user) {
        if (user.setOnline(true)) {
            String channel = user.getChannel();
            if (channel.substring(1).equals(user.getName())) {
                user.setBroadcaster(true);
            }
            listener.onUserAdded(user);
        }
        return user;
    }
    
    public void info(String channel, String message) {
        listener.onInfo(channel, message);
    }
    
    public void info(String message) {
        listener.onInfo(message);
    }

    public interface ConnectionListener {

        void onJoinAttempt(String channel);

        void onChannelJoined(User channel);

        void onChannelLeft(String channel);

        void onJoin(User user);

        void onPart(User user);

        void onUserAdded(User user);

        void onUserRemoved(User user);
        
        void onUserlistCleared(String channel);

        void onUserUpdated(User user);

        void onChannelMessage(User user, String message, boolean action, String emotes, String id, int bits);
        
        void onWhisper(User user, String message, String emotes);

        void onNotice(String message);

        /**
         * An info message to a specific channel, usually intended to be
         * directly output to the user.
         * 
         * <p>The channel should not be null. If no channel is associated, use
         * {@link onInfo(String) onInfo(infoMessage)} instead.</p>
         * 
         * @param channel The channel the info message belongs to
         * @param infoMessage The info message
         */
        void onInfo(String channel, String infoMessage);

        /**
         * An info message, usually intended to be directly output to the user.
         * 
         * <p>Since no channel is associated, this is likely to be output to the
         * currently active channel/tab.</p>
         * 
         * @param infoMessage The info message
         */
        void onInfo(String infoMessage);
        
        void onGlobalInfo(String message);

        void onBan(User user, long length, String reason, String targetMsgId);

        void onRegistered();
        
        void onDisconnect(int reason, String reasonMessage);

        void onMod(User user);

        void onUnmod(User user);

        void onConnectionStateChanged(int state);
        
        void onSpecialUserUpdated();

        void onConnectError(String message);
        
        void onJoinError(Set<String> toJoin, String errorChannel, JoinError error);
        
        void onRawReceived(String text);
        
        void onRawSent(String text);
        
        void onHost(String channel, String target);
        
        void onChannelCleared(String channel);
        
        /**
         * A notification in chat for a new subscriber or resub.
         * 
         * @param channel The channel (never null)
         * @param user The User object (may be dummy user object with empty
         * name, but never null)
         * @param text The notification text (never null or empty)
         * @param message The attached message (may be null or empty)
         * @param months The number of subscribed months (may be -1 if invalid)
         * @param emotes The emotes tag, yet to be parsed (may be null)
         */
        void onSubscriberNotification(String channel, User user, String text, String message, int months, String emotes);
        
        void onSpecialMessage(String name, String message);
        
        void onRoomId(String channel, String id);
        
    }

}
