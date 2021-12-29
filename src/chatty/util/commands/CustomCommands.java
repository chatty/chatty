
package chatty.util.commands;

import chatty.Commands;
import chatty.Helper;
import chatty.Room;
import chatty.TwitchClient;
import chatty.TwitchCommands;
import chatty.gui.components.eventlog.EventLog;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.StreamInfo;
import chatty.util.api.TwitchApi;
import chatty.util.settings.Settings;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Manages named Custom Commands and provides additional methods like adding
 * parameters to anonymous Custom Commands.
 * 
 * @author tduva
 */
public class CustomCommands {
    
    private static final Logger LOGGER = Logger.getLogger(CustomCommands.class.getName());
    
    private final Map<String, Map<String, CustomCommand>> commands = new HashMap<>();
    private final Map<String, Map<String, CustomCommand>> replacements = new HashMap<>();
    
    private final Settings settings;
    private final TwitchApi api;
    private final TwitchClient client;
    
    public CustomCommands(Settings settings, TwitchApi api, TwitchClient client) {
        this.settings = settings;
        this.api = api;
        this.client = client;
    }
    
    /**
     * Build the text based on the command with the given name and the
     * parameters. The result is returned to the result function on the same
     * thread as this is called, unless the command contains any async
     * replacements.
     * 
     * <p>If a command with the given name does not exist, null is returned to
     * the result function.
     * 
     * <p>This will add additional default parameters.
     *
     * @param commandName The command name
     * @param parameters The parameters (must not be null)
     * @param room The room the channel is in (must not be null)
     * @param result Function that will be called with the result string
     */
    public void command(String commandName, Parameters parameters, Room room, Consumer<String> result) {
        CustomCommand command = getCommand(commands, commandName, room.getOwnerChannel());
        if (command != null) {
            command(command, parameters, room, result);
        }
        else {
            result.accept(null);
        }
    }
    
    /**
     * Build the text based on the given command and the parameters. The result
     * is returned to the result function on the same thread as this is called,
     * unless the command ocntains any async replacements.
     * 
     * <p>This will add additional default parameters.
     * 
     * @param command The command (must not be null)
     * @param parameters The parameters (must not be null)
     * @param room The room
     * @param result The result function
     */
    public void command(CustomCommand command, Parameters parameters, Room room, Consumer<String> result) {
        // Add some default parameters
        addChans(room, parameters);
        Set<String> chans = new HashSet<>();
        client.getOpenChannels().forEach(chan -> { if (Helper.isRegularChannelStrict(chan)) chans.add(Helper.toStream(chan));});
        parameters.put("chans", StringUtil.join(chans, " "));
        parameters.put("hostedchan", client.getHostedChannel(room.getChannel()));
        parameters.putObject("localUser", client.getLocalUser(room.getChannel()));
        parameters.putObject("settings", client.settings);
        if (!command.getIdentifiersWithPrefix("stream").isEmpty()) {
            String stream = Helper.toValidStream(room.getStream());
            StreamInfo streamInfo = api.getStreamInfo(stream, null);
            if (streamInfo.isValid()) {
                parameters.put("streamstatus", streamInfo.getFullStatus());
                if (streamInfo.getOnline()) {
                    parameters.put("streamuptime", DateTime.agoUptimeCompact2(streamInfo.getTimeStartedWithPicnic()));
                    parameters.put("streamtitle", streamInfo.getTitle());
                    parameters.put("streamgame", streamInfo.getGame());
                    parameters.put("streamviewers", String.valueOf(streamInfo.getViewers()));
                }
            }
        }
        parameters.put("chain-test", "| /echo Test || Message");
        parameters.put("allow-request", "true");
        if (command.getRaw() != null && command.getRaw().startsWith("/chain ")) {
            parameters.put(Helper.ESCAPE_FOR_CHAIN_COMMAND, "true");
        }
        else {
            // Need to remove, so it doesn't stay for chained commands which
            // still use the same Parameters
            parameters.remove(Helper.ESCAPE_FOR_CHAIN_COMMAND);
        }
        if (command.getRaw() != null && command.getRaw().startsWith("/foreach ")) {
            parameters.put(Helper.ESCAPE_FOR_FOREACH_COMMAND, "true");
        }
        else {
            parameters.remove(Helper.ESCAPE_FOR_FOREACH_COMMAND);
        }
        parameters.put("nested-custom-commands", String.valueOf(getCustomCommandCount(parameters) + 1));
        
        boolean performAsync = false;
        if (shouldPerformAsyncReplacement(command)) {
            performAsync = true;
        }
        
        // Collect commands for custom replacements
        Map<String, CustomCommand> customIdentifiersCommands = getCustomIdentifierCommands(command, room.getOwnerChannel());
        if (containsAsyncReplacement(customIdentifiersCommands.values())) {
            performAsync = true;
        }
        
        // Actual replacement taking place
        if (performAsync) {
            new Thread(() -> {
                addCustomIdentifiers(customIdentifiersCommands, parameters);
                result.accept(command.replace(parameters));
            }, "CustomCommand").start();
        }
        else {
            addCustomIdentifiers(customIdentifiersCommands, parameters);
            result.accept(command.replace(parameters));
        }
    }
    
    public void addCustomIdentifierParametersForCommand(CustomCommand command, Parameters parameters) {
        Map<String, CustomCommand> customIdentifiersCommands = getCustomIdentifierCommands(command, null);
        addCustomIdentifiers(customIdentifiersCommands, parameters);
    }
    
    private Map<String, CustomCommand> getCustomIdentifierCommands(CustomCommand command, String channel) {
        Map<String, CustomCommand> customIdentifiersCommands = new HashMap<>();
        for (String identifier : command.getIdentifiersWithPrefix("_")) {
            CustomCommand identifierCommand = getCommand(replacements, identifier, channel);
            if (identifierCommand != null) {
                customIdentifiersCommands.put(identifier, identifierCommand);
            }
        }
        return customIdentifiersCommands;
    }
    
    private static void addCustomIdentifiers(Map<String, CustomCommand> commands, Parameters parameters) {
        for (Map.Entry<String, CustomCommand> entry : commands.entrySet()) {
            parameters.putObject(entry.getKey(), entry.getValue());
        }
    }
        
    private boolean containsAsyncReplacement(Collection<CustomCommand> commands) {
        for (CustomCommand command : commands) {
            if (shouldPerformAsyncReplacement(command)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean shouldPerformAsyncReplacement(CustomCommand command) {
        return !command.getIdentifiersWithPrefix("-async-").isEmpty();
    }
    
    public static void addChans(Room room, Parameters parameters) {
        parameters.put("chan", Helper.toStream(room.getChannel()));
        parameters.put("stream", room.getStream());
    }
    
    /**
     * Checks if the given command exists (case-insensitive).
     * 
     * @param command The command
     * @return {@code true} if the command exists, {@code false} otherwise
     */
    public synchronized boolean containsCommand(String command, Room room) {
        return getCommand(commands, command, room.getOwnerChannel()) != null;
    }
    
    public void update(Commands regularCommands) {
        loadFromSettings();
        Set<String> nameCollisions = new HashSet<>();
        for (String customCommandName : getCommandNames()) {
            if (regularCommands.isCommand(customCommandName)
                    || TwitchCommands.isCommand(customCommandName)) {
                nameCollisions.add(customCommandName);
            }
        }
        // Remove event when no longer necessary, also for updating event
        EventLog.removeSystemEvent("session.settings.customCommandNameCollsision");
        if (!nameCollisions.isEmpty()) {
            EventLog.addSystemEvent("session.settings.customCommandNameCollsision", StringUtil.join(nameCollisions, ", ", s -> "/"+s));
        }
    }
    
    /**
     * Load the commands from the settings.
     */
    public synchronized void loadFromSettings() {
        List<String> commandsToLoad = settings.getList("commands");
        commands.clear();
        replacements.clear();
        for (String entry : commandsToLoad) {
            CustomCommand command = parseCommandWithName(entry);
            if (command != null) {
                // Always has non-empty name at this point
                if (!command.hasError()) {
                    String commandName = command.getName();
                    String chan = command.getChan();
                    if (commandName.startsWith("_") && commandName.length() > 1) {
                        addCommand(replacements, commandName, chan, command);
                    }
                    else {
                        addCommand(commands, commandName, chan, command);
                    }
                }
                else {
                    LOGGER.warning("Error parsing custom command: " + command.getError());
                }
            }
        }
    }
    
    /**
     * Parses a Custom Command in "commands" setting format.
     * 
     * 
     * 
     * @param c Non-empty line
     * @return The CustomCommand (maybe with parsing errors), with name set, or
     * null if no name or command is found
     */
    public static CustomCommand parseCommandWithName(String c) {
        if (c != null && !c.isEmpty()) {
            // Trim to ensure consistent behaviour between setting loading and
            // Test-button
            c = c.trim();
            String[] split = c.split(" ", 2);
            if (split.length == 2) {
                String commandName = split[0];
                if (commandName.startsWith("/")) {
                    commandName = commandName.substring(1);
                }
                commandName = StringUtil.toLowerCase(commandName.trim());
                String chan = null;
                if (commandName.contains("#")) {
                    String[] splitChan = commandName.split("#", 2);
                    commandName = splitChan[0];
                    chan = splitChan[1];
                }

                // Trim again, to ensure consistent behaviour
                String commandValue = split[1].trim();
                if (!commandName.isEmpty()) {
                    return CustomCommand.parse(commandName, chan, commandValue);
                }
            }
        }
        return null;
    }
    
    public synchronized Collection<String> getCommandNames() {
        return new HashSet<>(commands.keySet());
    }
    
    /**
     * Get the CustomCommand from the given dataset, based on name and channel.
     * This will prefer the command variation of the channel, but fallback on
     * the non-restricted command, if that is available.
     * 
     * @param commands The dataset to retrieve the Custom Command from
     * @param commandName The name of the command
     * @param channel The channel the command is run in
     * @return 
     */
    private synchronized static CustomCommand getCommand(Map<String, Map<String, CustomCommand>> commands,
            String commandName, String channel) {
        commandName = StringUtil.toLowerCase(commandName);
        channel = StringUtil.toLowerCase(Helper.toStream(channel));
        if (!commands.containsKey(commandName)) {
            return null;
        }
        Map<String, CustomCommand> variations = commands.get(commandName);
        if (variations.containsKey(channel)) {
            // If the channel parameter was null, this will try to get the
            // non-restricted command
            return variations.get(channel);
        }
        // Non-restricted commands are saved under "null"
        return variations.get(null);
    }
    
    /**
     * Add the given CustomCommand to the dataset, with it's name and channel.
     * 
     * @param commands The dataset to store the CustomCommand in
     * @param commandName The name of the command (all lowercase)
     * @param channel The channel the command is restricted to (all lowercase),
     * can be null if non-restricted
     * @param parsedCommand The CustomCommand
     */
    private static void addCommand(Map<String, Map<String, CustomCommand>> commands,
            String commandName, String channel, CustomCommand parsedCommand) {
        if (!commands.containsKey(commandName)) {
            commands.put(commandName, new HashMap<>());
        }
        commands.get(commandName).put(channel, parsedCommand);
    }
    
    public static int getCustomCommandCount(Parameters parameters) {
        String countString = parameters.get("nested-custom-commands");
        return countString == null ? 0 : Integer.parseInt(countString);
    }
    
}
