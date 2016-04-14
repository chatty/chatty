
package chatty;

import chatty.util.ParameterReplacer;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Managing custom commands, that return the text they are defined with, which
 * can then be entered like text in the input box. Custom commands are
 * case-insensitive.
 * 
 * @author tduva
 */
public class CustomCommands {
    
    private final Map<String, String> commands = new HashMap<>();
    
    private final Settings settings;
    
    public CustomCommands(Settings settings) {
        this.settings = settings;
    }
    
    /**
     * Returns the text associated with the given command, inserting the given
     * parameters as defined.
     * 
     * @param command The command
     * @param parameter The parameters, each seperated by a space from eachother
     * @param channel
     * @return A {@code String} as result of the command, or {@code null} if the
     * command doesn't exist or the number of parameters were invalid
     */
    public synchronized String command(String command, String parameter, String channel) {
        command = StringUtil.toLowerCase(command);
        if (commands.containsKey(command)) {
            String commandText = commands.get(command);
            String[] parameters;
            if (parameter == null || parameter.isEmpty()) {
                parameters = new String[0];
            } else {
                parameters = parameter.split(" ");
            }
            String replaced = new ParameterReplacer().replace(commandText, parameters);
            if (replaced != null) {
                // If no required parameters are missing, replace channel
                replaced = replaced.replaceAll("\\$chan", channel);
            }
            return replaced;
        }
        return null;
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
                    if (!command.isEmpty()) {
                        commands.put(StringUtil.toLowerCase(command), split[1]);
                    }
                }
            }
        }
    }
    
}
