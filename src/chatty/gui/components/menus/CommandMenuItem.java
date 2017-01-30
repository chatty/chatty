
package chatty.gui.components.menus;

import chatty.util.commands.CustomCommand;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author tduva
 */
public class CommandMenuItem {
    
    private static final AtomicLong NEXT_ID = new AtomicLong();
    
    private final String id = "command"+NEXT_ID.getAndIncrement();
    private final String label;
    private final CustomCommand command;
    private final String parent;
    private final String key;
    
    public CommandMenuItem(String label, CustomCommand command, String parent,
            String key) {
        this.label = label;
        this.command = command;
        this.parent = parent;
        this.key = key;
    }
    
    public String getLabel() {
        return label;
    }
    
    public CustomCommand getCommand() {
        return command;
    }
    
    public String getParent() {
        return parent;
    }
    
    public String getKey() {
        return key;
    }
    
    public boolean hasKey() {
        return key != null && !key.isEmpty();
    }
    
    @Override
    public String toString() {
        return "["+parent+"] "+label+"="+command;
    }
    
    public String getId() {
        return id;
    }
    
}
