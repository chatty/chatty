
package chatty;

import chatty.util.StringUtil;
import chatty.util.commands.Parameters;
import java.util.HashMap;
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
        
    }
    
}
