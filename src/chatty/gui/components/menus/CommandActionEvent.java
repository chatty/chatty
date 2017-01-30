
package chatty.gui.components.menus;

import chatty.util.commands.CustomCommand;
import java.awt.event.ActionEvent;

/**
 *
 * @author tduva
 */
public class CommandActionEvent extends ActionEvent {
    
    private final CustomCommand command;
    
    public CommandActionEvent(ActionEvent e, CustomCommand command) {
        super(e.getSource(), e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers());
        this.command = command;
    }
    
    public CustomCommand getCommand() {
        return command;
    }
    
}
