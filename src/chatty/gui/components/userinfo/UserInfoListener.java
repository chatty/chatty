
package chatty.gui.components.userinfo;

import chatty.Room;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;

/**
 *
 * @author tduva
 */
public interface UserInfoListener {

    public void anonCustomCommand(Room room, CustomCommand command, Parameters parameters);
    
}
