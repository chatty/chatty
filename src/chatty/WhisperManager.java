
package chatty;

import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class WhisperManager {
    
    public static final String WHISPER_CHANNEL = "$[whisper]";

    public static final int DISPLAY_IN_CHAT = 0;
    public static final int DISPLAY_ONE_WINDOW = 1;
    public static final int DISPLAY_PER_USER = 2;
    
    private final String AUTO_RESPOND_MESSAGE = "[Auto-Message] This user has "
            + "not allowed to receive whispers from you.";
    private final Set<String> autoRespondedTo = new HashSet<>();
    
    //-----------
    // References
    //-----------
    private final WhisperListener listener;
    private final Settings settings;
    private final TwitchConnection c;
    private final TwitchClient client;
    
    public WhisperManager(WhisperListener listener, Settings settings,
            TwitchConnection c, TwitchClient client) {
        this.listener = listener;
        this.settings = settings;
        this.c = c;
        this.client = client;
    }
    
    public boolean isAvailable() {
        return c.isRegistered() && isEnabled();
    }
    
    /**
     * Check whether the whisper feature is enabled and configured correctly.
     * 
     * @return true if enabled and configured correctly, false otherwise
     */
    public boolean isEnabled() {
        return settings.getBoolean("whisperEnabled");
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
     * Send a whisper, while checking some stuff.
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
            if (Helper.isBeforeChatCommandsShutoff() && !settings.getBoolean("whisperApi")) {
                if (!rawWhisper(nick, message)) {
                    listener.info("# Whisper not sent (spam protection): " + message);
                }
                else {
                    User user = c.getUser(WHISPER_CHANNEL, nick);
                    listener.whisperSent(user, message);
                    if (isUserIgnored(user)) {
                        listener.info("You haven't allowed to receive whispers from " + user);
                    }
                }
            }
            else {
                client.api.whisper(nick, message, r -> {
                    if (r.error != null) {
                        listener.info("# Whisper not sent: " + r.error);
                    }
                    else {
                        User user = c.getUser(WHISPER_CHANNEL, nick);
                        listener.whisperSent(user, message);
                        if (isUserIgnored(user)) {
                            listener.info("You haven't allowed to receive whispers from " + user);
                        }
                    }
                });
            }
        } else {
            listener.info("Can't send whisper: not connected");
        }
    }
    
    /**
     * Check if whispers from the given user should be output or ignored.
     *
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
        if (settings.listContains("ignoredUsersWhisper", user.getName())) {
            return true;
        }
        return !isUserAllowed(user);
    }
    
    public void whisperReceived(User user, String message, String emotes) {
        if (!isEnabled()) {
            return;
        }
        if (isUserAllowed(user)) {
            listener.whisperReceived(user, message, emotes);
        }
        if (isUserIgnored(user) && settings.getBoolean("whisperAutoRespond")) {
            if (!autoRespondedTo.contains(user.getName())) {
                String msg = AUTO_RESPOND_MESSAGE;
                String customMsg = settings.getString("whisperAutoRespondCustom");
                if (!StringUtil.isNullOrEmpty(customMsg)) {
                    msg = String.format("%s (%s)", msg, customMsg);
                }
                rawWhisper(user.getName(), msg);
                autoRespondedTo.add(user.getName());
            }
        } else {
            autoRespondedTo.remove(user.getName());
        }
    }
    
    /**
     * Send a whisper without any checks.
     * 
     * @param nick
     * @param message
     * @return 
     */
    private boolean rawWhisper(String nick, String message) {
        return c.sendSpamProtectedMessage("#"+c.getUsername(), "/w " + nick + " " + message, false);
    }
            

    public interface WhisperListener {

        public void whisperReceived(User user, String message, String emotes);

        public void whisperSent(User to, String message);

        public void info(String message);
    }  
}
