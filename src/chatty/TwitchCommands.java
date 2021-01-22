
package chatty;

import chatty.gui.UrlOpener;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.irc.MsgTags;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Twitch Chat commands. All the Twitch specific commands like /mod, /timeout..
 * 
 * @author tduva
 */
public class TwitchCommands {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchCommands.class.getName());
    
    /**
     * The delay between /mods requests. This is the delay in between each
     * request, not how often it is requested for one channel (it is currently
     * only requested once for each channel).
     */
    private static final int REQUEST_MODS_DELAY = 30*1000;
    
    /**
     * Channels which currently wait for a /mods response that should be silent
     * (no message output).
     */
    private final Set<String> silentModsRequestChannel
            = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Channels for which the /mods list has already been requested.
     */
    private final Set<String> modsAlreadyRequested
            = Collections.synchronizedSet(new HashSet<String>());
    
    private static final Set<String> SIMPLE_COMMANDS = new HashSet<>(Arrays.asList(new String[]{
        "unban", "untimeout", "delete", "clear",
        "followers", "followersoff", "subscribers", "subscribersoff", "slow", "slowoff",
        "emoteonly", "emoteonlyoff", "r9kbeta", "r9kbetaoff",
        "vip", "unvip", "vips", "mod", "unmod", "mods", "commercial",
        "host", "unhost",
        "color"
    }));
    
    /**
     * Commands that don't have a parameter, and probably never will have, so
     * don't include one. This can be necessary when triggering the command e.g.
     * through the Channel Context Menu using the Command Name format, where it
     * automatically adds a parameter.
     */
    private static final Set<String> NO_PARAMETER_COMMANDS = new HashSet<>(Arrays.asList(new String[]{
        "followersoff", "subscribers", "subscribersoff", "slowoff", "emoteonly",
        "emoteonlyoff", "r9kbeta", "r9kbetaoff"
    }));
    
    /**
     * Other commands, only used for isCommand().
     */
    private static final Set<String> OTHER_COMMANDS = new HashSet<>(Arrays.asList(new String[]{
        "ban", "timeout", "fixmods", "host2", "raid", "unraid", "requests",
        "to", "r9k", "r9koff", "host"
    }));
    
    private TwitchConnection c;
    
    public TwitchCommands(TwitchConnection c) {
        this.c = c;
    }
    
    public static boolean isCommand(String command) {
        command = StringUtil.toLowerCase(command);
        if (SIMPLE_COMMANDS.contains(command)) {
            return true;
        }
        if (NO_PARAMETER_COMMANDS.contains(command)) {
            return true;
        }
        if (OTHER_COMMANDS.contains(command)) {
            return true;
        }
        return false;
    }
    
    public boolean command(String channel, String msgId, String command, String parameter) {
        if (command.equals("to")) {
            command = "timeout";
        }
        if (command.equals("r9k")) {
            command = "r9kbeta";
        }
        if (command.equals("r9koff")) {
            command = "r9kbetaoff";
        }
        if (command.equals("host") && parameter == null) {
            commandHostmode2(Helper.toChannel(c.getUsername()), Helper.toStream(channel));
        }
        else if (SIMPLE_COMMANDS.contains(command)) {
            // Simple commands that don't require any special handling for
            // decent output
            if (onChannel(channel, true)) {
                parameter = StringUtil.trim(parameter);
                // Get custom message for this command, if available
                String message = Language.getStringNull("chat.twitchcommands."+command,
                        !StringUtil.isNullOrEmpty(parameter) ? parameter : "default");
                if (parameter == null || parameter.trim().isEmpty()
                        || NO_PARAMETER_COMMANDS.contains(command)) {
                    // No parameter
                    String output = message != null ? message : "Trying to "+command+"..";
                    sendMessage(channel, "/"+command, output);
                } else {
                    // Parameters
                    String output = message != null ? message : "Trying to "+command+" "+parameter+"..";
                    sendMessage(channel, "/"+command+" "+parameter, output);
                }
            }
        }
        else if (command.equals("ban")) {
            commandBan(channel, parameter);
        }
        else if (command.equals("timeout")) {
            commandTimeout(channel, msgId, parameter);
        }
        else if (command.equals("fixmods")) {
            modsSilent(channel);
        }
        else if (command.equals("host2")) {
            commandHostmode2(Helper.toChannel(c.getUsername()), parameter);
        }
        else if (command.equals("raid")) {
            commandRaid(channel, parameter);
        }
        else if (command.equals("unraid")) {
            commandUnraid(channel);
        }
        else if (command.equals("requests")) {
            if (Helper.isRegularChannelStrict(channel)) {
                UrlOpener.openUrl("https://www.twitch.tv/popout/"+Helper.toStream(channel)+"/reward-queue");
            }
            else {
                printLine(channel, "Invalid channel to open reward queue for");
            }
        }
        else {
            return false;
        }
       return true;
    }
    
    //==================
    // Helper Functions
    //==================
    
    private boolean onChannel(String channel, boolean message) {
        return c.onChannel(channel, message);
    }
    
    private void sendMessage(String channel, String message, String echo) {
        sendMessage(channel, message, echo, MsgTags.EMPTY);
    }
    
    private void sendMessage(String channel, String message, String echo, MsgTags tags) {
        c.sendCommandMessage(channel, message, echo, tags);
    }
    
    private void printLine(String channel, String message) {
        c.info(channel, message, null);
    }
    
    private void printLine(String message) {
        c.info(message);
    }
    
    private MsgTags createTags(String msgId) {
        if (msgId != null) {
            return MsgTags.create("target-msg-id", msgId);
        }
        return MsgTags.EMPTY;
    }
    
    //===========================
    // Special Command Functions
    //===========================
    
    private void commandBan(String channel, String parameter) {
        if (StringUtil.isNullOrEmpty(parameter)) {
            sendMessage(channel, "/ban", "Trying to ban..");
            return;
        }
        String[] parts = parameter.split(" ", 2);
        String nick = parts[0];
        String reason = null;
        if (parts.length == 2) {
            reason = parts[1].trim();
        }
        ban(channel, nick, reason);
    }
    
    public void ban(String channel, String name, String reason) {
        if (onChannel(channel, true)) {
            if (reason == null || reason.isEmpty()) {
                sendMessage(channel,".ban "+name, "Trying to ban "+name+"..");
            } else {
                sendMessage(channel,".ban "+name+" "+reason, "Trying to ban "+name+".. ("+reason+")");
            }
        }
    }
    
    protected void commandTimeout(String channel, String msgId, String parameter) {
        if (parameter == null) {
            sendMessage(channel, "/timeout", "Trying to timeout..");
            return;
        }
        String[] parts = parameter.split(" ", 3);
        String nick = parts[0];
        String time = null;
        String timeLabel = null;
        String reason = null;
        if (parts.length > 1) {
            time = parts[1];
            try {
                long seconds = Long.parseLong(parts[1]);
                String formatted = DateTime.duration(seconds*1000, 0, 2, 0);
                String onlySeconds = seconds+"s";
                timeLabel = formatted.equals(onlySeconds)
                        ? onlySeconds : onlySeconds+"/"+formatted;
            } catch (NumberFormatException ex) {
                timeLabel = parts[1];
            }
        }
        if (parts.length > 2) {
            reason = parts[2];
        }
        timeout(channel, msgId, nick, time, timeLabel, reason);
    }

    /**
     * Sends a timeout command to the server.
     * 
     * @param channel
     * @param msgId
     * @param name
     * @param time The raw timeout time, can be null
     * @param timeLabel For outputting the timeout time (formatted), can be null
     * if time is null
     * @param reason The timeout reason, can be null
     */
    public void timeout(String channel, String msgId, String name, String time, String timeLabel, String reason) {
        if (onChannel(channel, true)) {
            MsgTags tags = createTags(msgId);
            if (time == null) {
                sendMessage(channel,".timeout "+name, "Trying to timeout "+name+"..", tags);
            }
            else {
                if (reason == null || reason.isEmpty()) {
                    sendMessage(channel,".timeout "+name+" "+time,
                        "Trying to timeout "+name+" ("+timeLabel+")", tags);
                } else {
                    sendMessage(channel,".timeout "+name+" "+time+" "+reason,
                        "Trying to timeout "+name+" ("+timeLabel+", "+reason+")", tags);
                }
            }
            
        }
    }
    
    protected void commandRaid(String channel, String parameter) {
        if (parameter == null) {
            printLine("Usage: /raid <stream>");
        } else {
            if (onChannel(channel, true)) {
                sendMessage(channel, ".raid "+parameter, "Trying to raid "+parameter+"..");
            }
        }
    }
    
    protected void commandUnraid(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel, ".unraid", "Trying to stop raid..");
        }
    }
    
    protected void commandHostmode2(String channel, String parameter) {
        if (parameter == null) {
            printLine("Usage: /host2 <stream>");
        } else {
            hostmode2(channel, parameter);
        }
    }
    
    public void hostmode2(String channel, String target) {
        if (c.isRegistered()) {
            c.sendSpamProtectedMessage(channel, ".host "+target, false);
            printLine(String.format("Trying to host %s from %s", target, channel));
        } else {
            printLine("Must be connected to chat to start hosting.");
        }
    }

    //==================
    // Silent Mods List
    //==================
    // For updating who is mod, probably mostly obsolete
    
    public void modsSilent(String channel) {
        if (onChannel(channel, true)) {
            printLine(channel, "Trying to fix moderators..");
            requestModsSilent(channel);
        }
    }
    
    public void requestModsSilent(String channel) {
        if (onChannel(channel, false)) {
            silentModsRequestChannel.add(channel);
            c.sendSpamProtectedMessage(channel, ".mods", false);
        }
    }
    
    public boolean removeModsSilent(String channel) {
        return silentModsRequestChannel.remove(channel);
    }
    
    public boolean waitingForModsSilent() {
        return !silentModsRequestChannel.isEmpty();
    }
    
    /**
     * Prase the list of mods as returned from the Twitch Chat. The
     * comma-separated list should start after the first colon ("The moderators
     * of this room are: ..").
     *
     * @param text The text as received from the Twitch Chat
     * @return A List of moderator names
     */
    public static List<String> parseModsList(String text) {
        int start = text.indexOf(":") + 1;
        List<String> modsList = new ArrayList<>();
        if (start > 1 && text.length() > start) {
            String mods = text.substring(start);
            if (!mods.trim().isEmpty()) {
                String[] modsArray = mods.split(",");
                for (String mod : modsArray) {
                    modsList.add(mod.trim());
                }
            }
        }
        return modsList;
    }
    
    /**
     * Starts the timer which requests the /mods list for joined channels.
     */
    public void startAutoRequestMods() {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                autoRequestMods();
            }
        }, 1000, REQUEST_MODS_DELAY);
    }
    
    /**
     * If enabled in the settings, requests /mods for one currently joined
     * channel (and only one), ignoring the ones it was already requested for.
     */
    private void autoRequestMods() {
        if (!c.autoRequestModsEnabled()) {
            return;
        }
        Set<String> joinedChannels = c.getJoinedChannels();
        for (String channel : joinedChannels) {
            if (!modsAlreadyRequested.contains(channel)) {
                LOGGER.info("Auto-requesting mods for "+channel);
                modsAlreadyRequested.add(channel);
                requestModsSilent(channel);
                return;
            }
        }
    }
    
    /**
     * Removes one or all entries from the list of channels the /mods list was
     * already requested for. This can be used on part/disconnect, since users
     * are removed then.
     * 
     * @param channel The name of the channel to remove, or null to remove all
     * entries
     */
    public void clearModsAlreadyRequested(String channel) {
        if (channel == null) {
            modsAlreadyRequested.clear();
        } else {
            modsAlreadyRequested.remove(channel);
        }
    }

}
