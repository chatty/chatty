
package chatty.gui.components.menus;

import chatty.util.srl.Race.Entrant;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author tduva
 */
public class RaceEntrantContextMenu extends ContextMenu {

    private final Collection<String> streams;
    private final ContextMenuListener listener;
    
    public RaceEntrantContextMenu(List<Entrant> entrants, ContextMenuListener listener) {
        this.listener = listener;
        streams = new ArrayList<>();
        for (Entrant entrant : entrants) {
            if (!entrant.twitch.isEmpty()) {
                streams.add(entrant.twitch);
            }
        }
        if (streams.isEmpty()) {
            addItem("", "No stream set");
        } else {
            ContextMenuHelper.addStreamsOptions(this, streams.size(), null);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        listener.streamsMenuItemClicked(e, streams);
    }
}
