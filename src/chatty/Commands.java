
package chatty;

import chatty.util.StringUtil;
import chatty.util.commands.Parameters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    
    public void add(String name, Consumer<CommandParameters> action, String... aliases) {
        add(name, action, false, aliases);
    }
    
    public void addEdt(String name, Consumer<CommandParameters> action, String... aliases) {
        add(name, action, true, aliases);
    }
    
    public void add(String name, Consumer<CommandParameters> action, boolean edt, String... aliases) {
        synchronized(commands) {
            commands.put(StringUtil.toLowerCase(name), new Command(name, "", action, edt));
            for (String alias : aliases) {
                commands.put(StringUtil.toLowerCase(alias), new Command(alias, "", action, edt));
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
            List<String> result = new ArrayList<>();
            for (Command c : commands.values()) {
                result.add(c.name);
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
            c.performAction(room, parameters);
            return true;
        }
        return false;
    }
    
    
    public static class Command {
        
        private final String name;
        private final String description;
        private final Consumer<CommandParameters> action;
        private final boolean edt;
        
        public Command(String name, String description, Consumer<CommandParameters> action, boolean edt) {
            this.name = name;
            this.description = description;
            this.action = action;
            this.edt = edt;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void performAction(Room room, Parameters parameters) {
            if (edt && !SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(() -> {
                    action.accept(new CommandParameters(room, parameters));
                });
            }
            else {
                action.accept(new CommandParameters(room, parameters));
            }
        }
        
    }
    
    public static class CommandParameters {
        
        private final Room room;
        private final Parameters parameters;
        
        public CommandParameters(Room room, Parameters parameters) {
            this.room = room;
            this.parameters = parameters;
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
        
        public boolean hasOption(String option) {
            return options != null && options.contains(option);
        }
        
        public static CommandParsedArgs parse(String input, int numArgs) {
            if (input == null) {
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
                if (split.length == numArgs) {
                    return new CommandParsedArgs(options, split);
                }
                return null;
            }
            return new CommandParsedArgs(options, new String[]{args});
        }
        
    }
    
}
