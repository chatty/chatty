
package chatty.gui.components.menus;

import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * Context menu that has entries to do stream related things like join the
 * associated channels or open URLs.
 * 
 * @author tduva
 */
public class StreamsContextMenu extends ContextMenu {
    
    private final ContextMenuListener listener;
    private final Collection<String> nicks;
    
    public StreamsContextMenu(Collection<String> streams, ContextMenuListener listener) {
        this.listener = listener;
        this.nicks = streams;
        
        ContextMenuHelper.addStreamsOptions(this, streams.size());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.streamsMenuItemClicked(e, nicks);
        }
    }
    
}
