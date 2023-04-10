
package chatty;

import chatty.util.StringUtil;
import chatty.util.commands.Parameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class Commands {
    
    private final Map<String, Command> commands = new HashMap<>();
    
    public void add(String name, Consumer<CommandParameters> action,
                                 String... aliases) {
        add(name, "", action, false, aliases);
    }
    
    public void add(String name, String description,
                                 Consumer<CommandParameters> action,
                                 String... aliases) {
        add(name, description, action, false, aliases);
    }
    
    public void addEdt(String name, Consumer<CommandParameters> action,
                                    String... aliases) {
        add(name, "", action, true, aliases);
    }
    
    public void addEdt(String name, String description,
                                    Consumer<CommandParameters> action,
                                    String... aliases) {
        add(name, description, action, true, aliases);
    }
    
    public void add(String name, String description,
                                 Consumer<CommandParameters> action,
                                 boolean edt,
                                 String... aliases) {
        synchronized(commands) {
            Command c = new Command(name, new ArrayList<>(Arrays.asList(aliases)), description, action, edt);
            commands.put(StringUtil.toLowerCase(name), c);
            for (String alias : aliases) {
                commands.put(StringUtil.toLowerCase(alias), c);
            }
        }
    }
    
    public boolean isCommand(String name) {
        synchronized(commands) {
            return commands.containsKey(StringUtil.toLowerCase(name));
        }
    }
    
    public Collection<String> getCommandNames() {
        synchronized(commands) {
            Collection<String> result = new HashSet<>();
            for (Command c : commands.values()) {
                result.add(c.name);
                c.aliases.forEach(name -> result.add(name));
            }
            return result;
        }
    }
    
    private Command getCommand(String commandName) {
        synchronized(commands) {
            return commands.get(StringUtil.toLowerCase(commandName));
        }
    }
    
    public boolean performCommand(String commandName, Room room, Parameters parameters) {
        Command c = getCommand(commandName);
        if (c != null) {
            c.performAction(room, parameters, commandName);
            return true;
        }
        return false;
    }
    
    
    public static class Command {
        
        private final String name;
        private final String description;
        private final Consumer<CommandParameters> action;
        private final boolean edt;
        private final List<String> aliases;
        
        public Command(String name, List<String> aliases, String description, Consumer<CommandParameters> action, boolean edt) {
            this.name = name;
            this.aliases = aliases;
            this.description = description;
            this.action = action;
            this.edt = edt;
        }
        
        /**
         * The name of the command. Note that if an alias of the command was
         * entered this will be the main name of the command, not the alias.
         * 
         * @return 
         */
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getUsage() {
            if (StringUtil.isNullOrEmpty(description)) {
                return null;
            }
            return String.format("Usage: /%s %s%s",
                name,
                description,
                StringUtil.aEmptyb(StringUtil.join(aliases, ", ", o -> "/"+o), "", " (Alias: %s)"));
        }
        
        public void performAction(Room room, Parameters parameters, String enteredCommandName) {
            if (edt && !SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(() -> {
                    action.accept(new CommandParameters(room, parameters, this, enteredCommandName));
                });
            }
            else {
                action.accept(new CommandParameters(room, parameters, this, enteredCommandName));
            }
        }
        
    }
    
    public static class CommandParameters {
        
        private final Room room;
        private final Parameters parameters;
        private final Command command;
        private final String enteredCommandName;
        
        public CommandParameters(Room room,
                                 Parameters parameters,
                                 Command command,
                                 String enteredCommandName) {
            this.room = room;
            this.parameters = parameters;
            this.command = command;
            this.enteredCommandName = enteredCommandName;
        }
        
        public Command getCommand() {
            return command;
        }
        
        public String getEnteredCommandName() {
            return enteredCommandName;
        }
        
        public Room getRoom() {
            return room;
        }
        
        public String getChannel() {
            return room.getChannel();
        }
        
        public Parameters getParameters() {
            return parameters;
        }
        
        /**
         * May be null.
         * 
         * @return 
         */
        public String getArgs() {
            return parameters.getArgs();
        }
        
        public boolean hasArgs() {
            return !StringUtil.isNullOrEmpty(parameters.getArgs());
        }
        
        /**
         * Split up the args into {@code numArgs} parts, separated by a space.
         * The last part contains the remaining args text. If there aren't
         * enough parts, {@code null} is returned.
         * <p>
         * Additionally when the args begin with "-" followed by no to several
         * letters they are interpreted as options.
         * 
         * @param numArgs Into how many parts total to split the args
         * @return The resulting CommandParsedArgs object, or null
         */
        public CommandParsedArgs parsedArgs(int numArgs) {
            return CommandParsedArgs.parse(getArgs(), numArgs);
        }
        
        public CommandParsedArgs parsedArgs(int numArgs, int numRequiredArgs) {
            return CommandParsedArgs.parse(getArgs(), numArgs, numRequiredArgs);
        }
        
    }
    
    public static class CommandParsedArgs {
        
        public final String[] args;
        public final String options;
        
        public CommandParsedArgs(String options, String[] args) {
            this.args = args;
            this.options = options;
        }
        
        public String get(int index) {
            return args[index];
        }
        
        public boolean has(int index) {
            return args.length > index;
        }
        
        public String get(int index, String def) {
            if (has(index)) {
                return args[index];
            }
            return def;
        }
        
        public int getInt(int index, int def) {
            if (has(index)) {
                try {
                    return Integer.parseInt(args[index]);
                }
                catch (NumberFormatException ex) {
                    // Do nothing
                }
            }
            return def;
        }
        
        public boolean hasOption(String option) {
            return options != null && options.contains(option);
        }
        
        public static CommandParsedArgs parse(String input, int numArgs) {
            return parse(input, numArgs, numArgs);
        }
        
        public static CommandParsedArgs parse(String input, int numArgs, int numRequiredArgs) {
            if (input == null) {
                if (numRequiredArgs == 0) {
                    return new CommandParsedArgs(null, "".split(""));
                }
                return null;
            }
            String options = null;
            int optionsTo = 0;
            if (input.startsWith("-")) {
                optionsTo = input.indexOf(" ");
                if (optionsTo == -1) {
                    optionsTo = input.length();
                }
                options = input.substring(1, optionsTo);
                if (options.isEmpty()) {
                    options = null;
                }
                optionsTo++;
            }
            if (optionsTo >= input.length()) {
                if (numArgs == 0) {
                    return new CommandParsedArgs(options, null);
                }
                return null;
            }
            String args = input.substring(optionsTo);
            if (numArgs > 0) {
                String[] split = args.split(" ", numArgs);
                if (split.length >= numRequiredArgs) {
                    return new CommandParsedArgs(options, split);
                }
                return null;
            }
            return new CommandParsedArgs(options, new String[]{args});
        }
        
    }
    
}
