
package chatty.util.commands;

import chatty.Helper;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.StreamInfo;
import chatty.util.api.TwitchApi;
import chatty.util.settings.Settings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    private final Map<String, CustomCommand> commands = new HashMap<>();
    
    private final Settings settings;
    private final TwitchApi api;
    
    public CustomCommands(Settings settings, TwitchApi api) {
        this.settings = settings;
        this.api = api;
    }
    
    /**
     * Returns the text associated with the given command, inserting the given
     * parameters as defined.
     * 
     * @param commandName The command
     * @param parameters The parameters, each seperated by a space from eachother
     * @param channel
     * @return A {@code String} as result of the command, or {@code null} if the
     * command doesn't exist or the number of parameters were invalid
     */
    public synchronized String command(String commandName, Parameters parameters, String channel) {
        commandName = StringUtil.toLowerCase(commandName);
        if (commands.containsKey(commandName)) {
            return command(commands.get(commandName), parameters, channel);
        }
        return null;
    }
    
    public synchronized String command(CustomCommand command, Parameters parameters, String channel) {
        // Add some more parameters
        parameters.put("chan", Helper.toStream(channel));
        if (command.containsIdentifier("stream")) {
            System.out.println("Request");
            String stream = Helper.toValidStream(channel);
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

        return command.replace(parameters);
    }
    
    /**
     * Checks if the given command exists (case-insensitive).
     * 
     * @param command The command
     * @return {@code true} if the command exists, {@code false} otherwise
     */
    public synchronized boolean containsCommand(String command) {
        return commands.containsKey(StringUtil.toLowerCase(command));
    }
    
    /**
     * Load the commands from the settings. Everything before the first space
     * is interpreted as command name, removing a leading "/" if present.
     * Everything else is used as the command parameters.
     */
    public synchronized void loadFromSettings() {
        List<String> commandsToLoad = settings.getList("commands");
        commands.clear();
        for (String c : commandsToLoad) {
            if (c != null && !c.isEmpty()) {
                String[] split = c.split(" ", 2);
                if (split.length == 2) {
                    String command = split[0];
                    if (command.startsWith("/")) {
                        command = command.substring(1);
                    }
                    command = command.trim();
                    String commandValue = split[1].trim();
                    if (!command.isEmpty()) {
                        CustomCommand parsedCommand = CustomCommand.parse(commandValue);
                        if (parsedCommand.getError() == null) {
                            commands.put(StringUtil.toLowerCase(command), parsedCommand);
                        } else {
                            LOGGER.warning("Error parsing custom command: "+parsedCommand.getError());
                        }
                    }
                }
            }
        }
    }
    
}
