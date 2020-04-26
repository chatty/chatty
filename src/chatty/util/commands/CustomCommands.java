
package chatty.util.commands;

import chatty.Helper;
import chatty.Room;
import chatty.TwitchClient;
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
import java.util.logging.Logger;

/**
 * Managing custom commands, that return the text they are defined with, which
 * can then be entered like text in the input box. Custom commands are
 * case-insensitive.
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
     * Returns the text associated with the given command, inserting the given
     * parameters as defined.
     * 
     * @param commandName The command
     * @param parameters The parameters, each separated by a space from eachother
     * @param channel
     * @return A {@code String} as result of the command, or {@code null} if the
     * command doesn't exist or the number of parameters were invalid
     */
    public synchronized String command(String commandName, Parameters parameters, Room room) {
        CustomCommand command = getCommand(commands, commandName, room.getOwnerChannel());
        if (command != null) {
            return command(command, parameters, room);
        }
        return null;
    }
    
    public synchronized String command(CustomCommand command, Parameters parameters, Room room) {
        // Add some more parameters
        addChans(room, parameters);
        Set<String> chans = new HashSet<>();
        client.getOpenChannels().forEach(chan -> { if (Helper.isRegularChannelStrict(chan)) chans.add(Helper.toStream(chan));});
        parameters.put("chans", StringUtil.join(chans, " "));
        parameters.put("hostedchan", client.getHostedChannel(room.getChannel()));
        parameters.putObject("localUser", client.getLocalUser(room.getChannel()));
        parameters.putObject("settings", client.settings);
        if (!command.getIdentifiersWithPrefix("stream").isEmpty()) {
            System.out.println("request");
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
        
        // Add parameters for custom replacements
        Set<String> customIdentifiers = command.getIdentifiersWithPrefix("_");
        for (String identifier : customIdentifiers) {
            CustomCommand replacement = getCommand(replacements, identifier, room.getOwnerChannel());
            if (replacement != null) {
                parameters.put(identifier, replacement.replace(parameters));
            }
        }

        return command.replace(parameters);
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
    private static CustomCommand getCommand(Map<String, Map<String, CustomCommand>> commands,
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
    
}
