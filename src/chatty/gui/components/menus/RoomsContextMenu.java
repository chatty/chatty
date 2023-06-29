
package chatty.gui.components.menus;

import chatty.Room;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * Context menu that has entries to do stream related things like join the
 * associated channels or open URLs.
 * 
 * @author tduva
 */
public class RoomsContextMenu extends ContextMenu {
    
    private final ContextMenuListener listener;
    private final Collection<Room> rooms;
    
    public RoomsContextMenu(Collection<Room> rooms, ContextMenuListener listener) {
        this.listener = listener;
        this.rooms = rooms;
        
        ContextMenuHelper.addStreamsOptions(this, rooms.size(), null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.roomsMenuItemClicked(e, rooms);
        }
    }
    
}
