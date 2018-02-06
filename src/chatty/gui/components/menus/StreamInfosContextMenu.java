
package chatty.gui.components.menus;

import chatty.lang.Language;
import chatty.util.api.StreamInfo;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tduva
 */
public class StreamInfosContextMenu extends ContextMenu {

    private final List<StreamInfo> streamInfos;
    
    private static final String SORT_SUBMENU = Language.getString("streams.cm.menu.sortBy");
    
    public StreamInfosContextMenu(List<StreamInfo> selected, boolean liveStreams) {
        this.streamInfos = selected;
        List<String> streams = new ArrayList<>();
        for (StreamInfo info : selected) {
            streams.add(info.getStream());
        }
        
        if (!selected.isEmpty()) {
            addItem("openChannelInfo", "Info: "+selected.get(0).getStream());
            addSeparator();
            
            ContextMenuHelper.addStreamsOptions(this, streams.size());
            
            if (liveStreams) {
                addSeparator();
            }
        }
        if (liveStreams) {
            addItem("sortRecent", Language.getString("streams.sorting.recent"), SORT_SUBMENU);
            addItem("sortName", Language.getString("streams.sorting.name"), SORT_SUBMENU);
            addItem("sortGame", Language.getString("streams.sorting.game"), SORT_SUBMENU);
            addItem("sortViewers", Language.getString("streams.sorting.viewers"), SORT_SUBMENU);
            addItem("showRemovedList", Language.getString("streams.cm.removedStreams"));
            addSeparator();
            addItem("manualRefreshStreams", Language.getString("streams.cm.refresh"));
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.streamInfosMenuItemClicked(e, streamInfos);
        }
    }
    
}
