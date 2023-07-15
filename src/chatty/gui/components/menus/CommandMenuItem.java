
package chatty.gui.components.menus;

import chatty.gui.components.settings.CommandSettings;
import chatty.util.StringUtil;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private final List<CustomCommand> restrictionCommands;
    private final int lineNumber;
    
    public CommandMenuItem(String label, CustomCommand command, String parent,
            int pos, String key, Collection<CustomCommand> restrictionCommands,
            int lineNumber) {
        this.label = label;
        this.labelCommand = label != null ? CustomCommand.parse(label) : null;
        this.command = command;
        this.parent = parent;
        this.pos = pos;
        this.key = key;
        this.restrictionCommands = restrictionCommands != null
                ? new ArrayList<>(restrictionCommands)
                : new ArrayList<>();
        this.lineNumber = lineNumber;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getLabel(Parameters parameters) {
        if (!checkRestrictions(parameters)) {
            return "";
        }
        if (parameters != null
                && labelCommand != null
                && parameters.hasKey("menuCommandLabels")) {
            return getLabelCommandResult(labelCommand, parameters);
        }
        return label;
    }
    
    public boolean checkRestrictions(Parameters parameters) {
        if (parameters != null
                && !parameters.hasKey("menu-test")
                && parameters.hasKey("menuRestrictions")
                && restrictionCommands != null) {
            for (CustomCommand cc : restrictionCommands) {
                String result = getLabelCommandResult(cc, parameters);
                if (StringUtil.isNullOrEmpty(result)) {
                    // Return empty label, which will get the entry removed
                    return false;
                }
            }
        }
        return true;
    }
    
    private String getLabelCommandResult(CustomCommand cc, Parameters parameters) {
        if (cc.hasError()) {
            LOGGER.info("Parse error: " + cc.getSingleLineError());
            return "Parse error (see debug log)";
        }
        return cc.replace(parameters);
    }
    
    public boolean hasValidLabelCommand() {
        return labelCommand != null && !labelCommand.hasError();
    }
    
    public CustomCommand getLabelCommand() {
        return labelCommand;
    }
    
    public boolean hasRestrictionCommands() {
        return restrictionCommands != null && !restrictionCommands.isEmpty();
    }
    
    public List<CustomCommand> getRestrictionCommands() {
        return restrictionCommands;
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
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public String getTooltipHtml() {
        if (getCommand() == null) {
            if (hasRestrictionCommands()) {
                return String.format("<html><body>Restrictions:<br /><code>%s</code>",
                CommandSettings.formatCommandInfo(StringUtil.join(getRestrictionCommands(), "\n")));
            }
            return "";
        }
        if (hasRestrictionCommands()) {
            return String.format("<html><body><code>%s</code><br /><br />Restrictions:<br /><code>%s</code><br />",
                CommandSettings.formatCommandInfo(StringUtil.shortenTo(getCommand().getRaw(), 100)),
                CommandSettings.formatCommandInfo(StringUtil.join(getRestrictionCommands(), "\n")));
        }
        return String.format("<html><body><code>%s</code>",
                CommandSettings.formatCommandInfo(StringUtil.shortenTo(getCommand().getRaw(), 100)));
    }
    
    @Override
    public String toString() {
        return "["+parent+","+pos+","+key+","+restrictionCommands+","+lineNumber+"] "+label+"="+command;
    }
    
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CommandMenuItem other = (CommandMenuItem) obj;
        if (this.pos != other.pos) {
            return false;
        }
        if (this.lineNumber != other.lineNumber) {
            return false;
        }
        if (!Objects.equals(this.label, other.label)) {
            return false;
        }
        if (!Objects.equals(this.parent, other.parent)) {
            return false;
        }
        if (!Objects.equals(this.key, other.key)) {
            return false;
        }
        if (!Objects.equals(this.command, other.command)) {
            return false;
        }
        return Objects.equals(this.restrictionCommands, other.restrictionCommands);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.label);
        hash = 71 * hash + Objects.hashCode(this.command);
        hash = 71 * hash + Objects.hashCode(this.parent);
        hash = 71 * hash + this.pos;
        hash = 71 * hash + Objects.hashCode(this.key);
        hash = 71 * hash + Objects.hashCode(this.restrictionCommands);
        hash = 71 * hash + this.lineNumber;
        return hash;
    }
    
}
