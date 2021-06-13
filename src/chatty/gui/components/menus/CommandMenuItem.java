
package chatty.gui.components.menus;

import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class CommandMenuItem {
    
    private static final Logger LOGGER = Logger.getLogger(CommandMenuItem.class.getName());
    
    private static final AtomicLong NEXT_ID = new AtomicLong();
    
    private final String id = "command"+NEXT_ID.getAndIncrement();
    private final String label;
    private final CustomCommand labelCommand;
    private final CustomCommand command;
    private final String parent;
    private final int pos;
    private final String key;
    
    public CommandMenuItem(String label, CustomCommand command, String parent,
            int pos, String key) {
        this.label = label;
        this.labelCommand = label != null ? CustomCommand.parse(label) : null;
        this.command = command;
        this.parent = parent;
        this.pos = pos;
        this.key = key;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getLabel(Parameters parameters) {
        if (parameters != null && labelCommand != null) {
            CustomCommand cc = labelCommand;
            if (cc.hasError()) {
                LOGGER.info("Parse error: " + cc.getSingleLineError());
                return "Parse error (see debug log)";
            }
            return cc.replace(parameters);
        }
        return label;
    }
    
    public boolean hasValidLabelCommand() {
        return labelCommand != null && !labelCommand.hasError();
    }
    
    public CustomCommand getLabelCommand() {
        return labelCommand;
    }
    
    public CustomCommand getCommand() {
        return command;
    }
    
    public String getParent() {
        return parent;
    }
    
    public int getPos() {
        return pos;
    }
    
    public String getKey() {
        return key;
    }
    
    public boolean hasKey() {
        return key != null && !key.isEmpty();
    }
    
    @Override
    public String toString() {
        return "["+parent+","+pos+","+key+"] "+label+"="+command;
    }
    
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CommandMenuItem other = (CommandMenuItem) obj;
        if (!Objects.equals(this.label, other.label)) {
            return false;
        }
        if (!Objects.equals(this.command, other.command)) {
            return false;
        }
        if (!Objects.equals(this.parent, other.parent)) {
            return false;
        }
        if (this.pos != other.pos) {
            return false;
        }
        if (!Objects.equals(this.key, other.key)) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.label);
        hash = 89 * hash + Objects.hashCode(this.command);
        hash = 89 * hash + Objects.hashCode(this.parent);
        hash = 89 * hash + this.pos;
        hash = 89 * hash + Objects.hashCode(this.key);
        return hash;
    }
    
}
