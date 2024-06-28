
package chatty;

import chatty.Commands.Command;
import chatty.gui.MainGui;
import chatty.gui.UrlOpener;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.RecentlyAffectedUsers;
import chatty.util.StringUtil;
import chatty.util.api.TwitchApi;
import chatty.util.api.TwitchApi.SimpleRequestResultListener;
import chatty.util.irc.MsgTags;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Twitch Chat commands. All the Twitch specific commands like /mod, /timeout..
 * 
 * @author tduva
 */
public class TwitchCommands {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchCommands.class.getName());
    
    /**
     * Channels which currently wait for a /mods response that should be silent
     * (no message output).
     */
    private final Set<String> silentModsRequestChannel
            = Collections.synchronizedSet(new HashSet<String>());

    private static final Set<String> SIMPLE_COMMANDS = new HashSet<>(Arrays.asList(new String[]{
        "commercial"
    }));

    /**
     * Commands that don't have a parameter, and probably never will have, so
     * don't include one. This can be necessary when triggering the command e.g.
     * through the Channel Context Menu using the Command Name format, where it
     * automatically adds a parameter.
     */
    private static final Set<String> NO_PARAMETER_COMMANDS = new HashSet<>(Arrays.asList(new String[]{
        
    }));
    
    /**
     * Other commands, only used for isCommand().
     */
    private static final Set<String> OTHER_COMMANDS = new HashSet<>(Arrays.asList(new String[]{
    }));
    
    private final TwitchConnection c;
    
    public TwitchCommands(TwitchConnection c) {
        this.c = c;
    }
    
    public static boolean isCommand(String command) {
        command = StringUtil.toLowerCase(command);
        if (SIMPLE_COMMANDS.contains(command)) {
            return true;
        }
        if (OTHER_COMMANDS.contains(command)) {
            return true;
        }
        return false;
    }
    
    public boolean command(String channel, String msgId, String command, String parameter) {
        if (SIMPLE_COMMANDS.contains(command)) {
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
        else {
            return false;
        }
       return true;
    }
    
    public void addNewCommands(Commands commands, TwitchClient client) {
        MainGui g = client.g;
        TwitchApi api = client.api;
        
        //--------------------------
        // Moderation
        //--------------------------
        for (String color : TwitchApi.ANNOUNCEMENT_COLORS) {
            commands.add("announce"+color, p -> {
                if (!p.hasArgs() || p.getArgs().trim().isEmpty()) {
                    g.printSystem("Usage: /announce"+color+" <message>");
                }
                else {
                    api.sendAnnouncement(p.getRoom().getStream(), p.getArgs(), color);
                }
            });
        }
        commands.add("timeout", "<user> [duration] [reason]", p -> {
            Commands.CommandParsedArgs args = p.parsedArgs(3, 1);
            int seconds;
            String reason;
            if (args != null) {
                seconds = getTimeoutSeconds(args);
                reason = args.get(2, "");
            }
            else {
                seconds = -1;
                reason = "";
            }
            String info = StringUtil.append(formatDuration(seconds), ", ", reason);
            userCommand(client, p, args, (user, resultListener) -> {
                api.ban(user, seconds, reason, resultListener);
            }, StringUtil.aEmptyb(info, "", " (%s)"));
        }, "to");
        commands.add("ban", "<user> [reason]", p -> {
            Commands.CommandParsedArgs args = p.parsedArgs(2, 1);
            String reason = args != null ? args.get(1, "") : "";
            userCommand(client, p, args, (user, resultListener) -> {
                api.ban(user, 0, reason, resultListener);
            }, StringUtil.aEmptyb(reason, "", " (%s)"));
        });
        commands.add("unban", "<user>", p -> {
            userCommand(client, p, p.parsedArgs(1, 1), (user, resultListener) -> {
                api.unban(user, resultListener);
            }, "");
        }, "untimeout");
        commands.add("delete", "<messageId>", p -> {
            Commands.CommandParsedArgs args = p.parsedArgs(1, 1);
            channelCommand(client, p, args, listener -> {
                api.deleteMsg(p.getRoom(), args.get(0), listener);
            });
        });
        commands.add("clear", p -> {
            channelCommand(client, p, p.parsedArgs(1, 0), listener -> {
                api.deleteMsg(p.getRoom(), null, listener);
            });
        });
        commands.add("shoutout", "<user>", p -> {
            userCommand(client, p, p.parsedArgs(1, 1), (user, resultListener) -> {
                api.shoutout(user, resultListener);
            }, "");
        });
        commands.add("warn", "<user> <reason>", p -> {
            Commands.CommandParsedArgs args = p.parsedArgs(2, 2);
            String reason = args != null ? args.get(1, "") : "";
            userCommand(client, p, args, (user, resultListener) -> {
                api.warn(user, reason, resultListener);
            }, StringUtil.aEmptyb(reason, "", " (%s)"));
        });
        //--------------------------
        // Broadcaster
        //--------------------------
        commands.add("mod", "<user>", p -> {
            userCommand(client, p, p.parsedArgs(1, 1), (user, resultListener) -> {
                api.setModerator(user, true, resultListener);
            }, "");
        });
        commands.add("unmod", "<user>", p -> {
            userCommand(client, p, p.parsedArgs(1, 1), (user, resultListener) -> {
                api.setModerator(user, false, resultListener);
            }, "");
        });
        commands.add("vip", "<user>", p -> {
            userCommand(client, p, p.parsedArgs(1, 1), (user, resultListener) -> {
                api.setVip(user, true, resultListener);
            }, "");
        });
        commands.add("unvip", "<user>", p -> {
            userCommand(client, p, p.parsedArgs(1, 1), (user, resultListener) -> {
                api.setVip(user, false, resultListener);
            }, "");
        });
        commands.add("mods", p -> {
            if (localUserIsBroadcaster(p.getRoom(), client)) {
                channelCommand(client, p, p.parsedArgs(1, 0), listener -> {
                    api.requestModerators(p.getRoom(), listener);
                });
            }
            else {
                sendMessage(p.getChannel(), "/mods", Language.getString("chat.twitchcommands.mods")+" (will only work for broadcaster after February 18, 2023)");
            }
        });
        commands.add("vips", p -> {
            if (localUserIsBroadcaster(p.getRoom(), client)) {
                channelCommand(client, p, p.parsedArgs(1, 0), listener -> {
                    api.requestVips(p.getRoom(), listener);
                });
            }
            else {
                sendMessage(p.getChannel(), "/vips", Language.getString("chat.twitchcommands.vips")+" (will only work for broadcaster after February 18, 2023)");
            }
        });
        commands.add("raid", p -> {
            Commands.CommandParsedArgs args = p.parsedArgs(1, 1);
            channelCommand(client, p, args, listener -> {
                api.startRaid(p.getRoom(), args.get(0), listener);
            }, () -> args.get(0));
        });
        commands.add("unraid", p -> {
            channelCommand(client, p, p.parsedArgs(1, 0), listener -> {
                api.cancelRaid(p.getRoom(), listener);
            });
        });
        // Add here so it keeps working (API) after chat command is removed
        commands.add("commercial", p -> {
            Commands.CommandParsedArgs args = p.parsedArgs(1, 0);
            client.runCommercial(p.getRoom().getStream(), args.getInt(0, 30));
        });
        //--------------------------
        // Chat Settings
        //--------------------------
        commands.add("uniquechat", p -> {
            updateChatSettings(client, p, "",
                    TwitchApi.CHAT_SETTINGS_UNIQUE, true);
        }, "r9kbeta", "r9k");
        commands.add("uniquechatOff", p -> {
            updateChatSettings(client, p, "",
                    TwitchApi.CHAT_SETTINGS_UNIQUE, false);
        }, "r9kbetaOff", "r9kOff");
        commands.add("followers", p -> {
            int minutes = (int) DateTime.parseDurationSeconds(p.getArgs()) / 60;
            if (minutes == -1) {
                updateChatSettings(client, p, "",
                        TwitchApi.CHAT_SETTINGS_FOLLOWER_MODE, true);
            }
            else {
                updateChatSettings(client, p, " ("+formatDuration(minutes * 60)+")",
                        TwitchApi.CHAT_SETTINGS_FOLLOWER_MODE, true,
                        TwitchApi.CHAT_SETTINGS_FOLLOWER_MODE_DURATION, minutes);
            }
        });
        commands.add("followersOff", p -> {
            updateChatSettings(client, p, "",
                    TwitchApi.CHAT_SETTINGS_FOLLOWER_MODE, false);
        });
        commands.add("slow", p -> {
            int seconds = (int) DateTime.parseDurationSeconds(p.getArgs());
            if (seconds == -1) {
                updateChatSettings(client, p, "",
                        TwitchApi.CHAT_SETTINGS_SLOWMODE, true);
            }
            else {
                updateChatSettings(client, p, " ("+formatDuration(seconds)+")",
                        TwitchApi.CHAT_SETTINGS_SLOWMODE, true,
                        TwitchApi.CHAT_SETTINGS_SLOWMODE_TIME, seconds);
            }
        });
        commands.add("slowOff", p -> {
            updateChatSettings(client, p, "",
                    TwitchApi.CHAT_SETTINGS_SLOWMODE, false);
        });
        commands.add("subscribers", p -> {
            updateChatSettings(client, p, "",
                    TwitchApi.CHAT_SETTINGS_SUBONLY, true);
        });
        commands.add("subscribersOff", p -> {
            updateChatSettings(client, p, "",
                    TwitchApi.CHAT_SETTINGS_SUBONLY, false);
        });
        commands.add("emoteonly", p -> {
            updateChatSettings(client, p, "",
                    TwitchApi.CHAT_SETTINGS_EMOTEONLY, true);
        });
        commands.add("emoteonlyOff", p -> {
            updateChatSettings(client, p, "",
                    TwitchApi.CHAT_SETTINGS_EMOTEONLY, false);
        });
        commands.add("shieldMode", p -> {
            channelCommand(client, p, p.parsedArgs(1, 0), listener -> {
                api.setShieldMode(p.getRoom(), true, listener);
            });
        });
        commands.add("shieldModeOff", p -> {
            channelCommand(client, p, p.parsedArgs(1, 0), listener -> {
                api.setShieldMode(p.getRoom(), false, listener);
            });
        });
        //--------------------------
        // User Settings
        //--------------------------
        commands.add("color", p -> {
            Commands.CommandParsedArgs args = p.parsedArgs(1, 1);
            simpleCommand(client, p, args, listener -> {
                api.setColor(args.get(0), listener);
            }, () -> args.get(0));
        });
        //--------------------------
        // Other
        //--------------------------
        commands.add("requests", p -> {
            String channel = p.getChannel();
            if (Helper.isRegularChannelStrict(channel)) {
                UrlOpener.openUrl("https://www.twitch.tv/popout/" + Helper.toStream(channel) + "/reward-queue");
            }
            else {
                printLine(channel, "Invalid channel to open reward queue for");
            }
        });
    }
    
    private static int getTimeoutSeconds(Commands.CommandParsedArgs args) {
        int seconds = (int) DateTime.parseDurationSeconds(args.get(1, null));
        if (seconds < 1) {
            seconds = 600; // Default timeout length
        }
        return seconds;
    }
    
    /**
     * Run the request to update chat settings. This one calls a specific
     * endpoint, as opposed to the other similar methods that are more generic.
     *
     * @param client
     * @param p
     * @param append
     * @param data The data to submit with the request
     */
    private void updateChatSettings(TwitchClient client,
                                    Commands.CommandParameters p,
                                    Object append,
                                    Object... data) {
        if (!Helper.isValidChannelStrict(p.getChannel())) {
            client.g.printSystem(p.getRoom(), "Invalid channel.");
            return;
        }
        if (data == null) {
            client.g.printSystem(p.getRoom(), "Invalid command");
            return;
        }
        String msg = makeMsg(p.getCommand(), append);
        Object objectId = client.g.printLine(p.getRoom(), msg);
        client.api.updateChatSettings(p.getRoom(), r -> {
            if (r.error == null) {
                // Success
                client.g.addToLine(p.getRoom(), objectId, "OK");
            }
            else {
                // Failed
                client.g.addToLine(p.getRoom(), objectId, r.error);
            }
        }, data);
    }
    
    /**
     * Run a request for a command involving a user.
     * 
     * @param client
     * @param p
     * @param args If this is null, an error is output and nothing else is run
     * @param doRequest Implement this to perform the actual request, the User
     * is provided for your convenient, takes from the first command parameter,
     * the provided listener needs to be called to handle the result
     * @param append 
     */
    private void userCommand(TwitchClient client,
                             Commands.CommandParameters p,
                             Commands.CommandParsedArgs args,
                             BiConsumer<User, SimpleRequestResultListener> doRequest,
                             String append) {
        if (!Helper.isValidChannelStrict(p.getChannel())) {
            client.g.printSystem(p.getRoom(), "Invalid channel.");
            return;
        }
        if (args == null) {
            client.g.printSystem(p.getRoom(), getInvalidParametersMessage(p));
            return;
        }
        User user = client.getUser(p.getChannel(), args.get(0));
        String msg = String.format("Trying to %s %s%s..",
                p.getCommand().getName(),
                user.getName(),
                append);
        Object objectId = client.g.printLine(p.getRoom(), msg);
        doRequest.accept(user, r -> {
            if (r.error == null) {
                // Success
                client.g.addToLine(p.getRoom(), objectId, "OK");
                RecentlyAffectedUsers.addUser(user);
            }
            else {
                // Failed
                client.g.addToLine(p.getRoom(), objectId, r.error);
            }
        });
    }
    
    private void channelCommand(TwitchClient client,
                               Commands.CommandParameters p,
                               Commands.CommandParsedArgs args,
                               Consumer<SimpleRequestResultListener> doRequest) {
        channelCommand(client, p, args, doRequest, () -> null);
    }
    
    private void channelCommand(TwitchClient client,
                               Commands.CommandParameters p,
                               Commands.CommandParsedArgs args,
                               Consumer<SimpleRequestResultListener> doRequest,
                               Supplier<String> outputArg) {
        if (!Helper.isValidChannelStrict(p.getChannel())) {
            client.g.printSystem(p.getRoom(), "Invalid channel.");
            return;
        }
        simpleCommand(client, p, args, doRequest, outputArg);
    }
    
    /**
     * Outputs an info message based on the command name and handles the request
     * result, either indicating a success or outputting an error message.
     * 
     * @param client
     * @param p Info from entering the command
     * @param args If this is null only an error message will be output, without
     * trying to run the request (indicating invalid parameters)
     * @param doRequest This must be implemented for performing the actual
     * request, the listener it provides must be called so the result can be
     * handled
     * @param outputArgs 
     */
    private void simpleCommand(TwitchClient client,
                               Commands.CommandParameters p,
                               Commands.CommandParsedArgs args,
                               Consumer<SimpleRequestResultListener> doRequest,
                               Supplier<String> outputArg) {
        if (args != null) {
            String msg = makeMsg(p.getCommand(), outputArg.get());
            Object objectId = client.g.printLine(p.getRoom(), msg);
            doRequest.accept(r -> {
                if (r.error == null) {
                    // Success
                    if (r.result == null) {
                        client.g.addToLine(p.getRoom(), objectId, "OK");
                    }
                    else {
                        client.g.printLine(p.getRoom(), r.result);
                    }
                }
                else {
                    // Failed
                    client.g.addToLine(p.getRoom(), objectId, r.error);
                }
            });
        }
        else {
            // Invalid parameters
            client.g.printSystem(p.getRoom(), getInvalidParametersMessage(p));
        }
    }
    
    private static String getInvalidParametersMessage(Commands.CommandParameters p) {
        String usage = p.getCommand().getUsage();
        return !StringUtil.isNullOrEmpty(usage) ? usage : "Invalid parameters";
    }
    
    private static String makeMsg(Command command, Object... args) {
        String message = Language.getStringNull("chat.twitchcommands."+command.getName(), args);
        if (message == null) {
            message = "Trying to "+command.getName()+"..";
        }
        return message;
    }
    
    private static String formatDuration(int seconds) {
        if (seconds > 0) {
            return DateTime.duration(TimeUnit.SECONDS.toMillis(seconds), DateTime.Formatting.NO_ZERO_VALUES);
        }
        return "";
    }
    
    private boolean localUserIsBroadcaster(Room room, TwitchClient client) {
        return room.getStream() != null && room.getStream().equals(client.getUsername());
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

}
