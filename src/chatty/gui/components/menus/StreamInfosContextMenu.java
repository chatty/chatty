
package chatty.gui.components.menus;

import chatty.gui.components.LiveStreamsDialog;
import chatty.gui.components.settings.SettingsUtil;
import chatty.lang.Language;
import chatty.util.api.StreamInfo;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;

/**
 *
 * @author tduva
 */
public class StreamInfosContextMenu extends ContextMenu {

    private final List<StreamInfo> streamInfos;
    
    private static final String SORT_SUBMENU = Language.getString("streams.cm.menu.sortBy");
    private static final String SORT_GROUP = "sort";
    
    private static final ImageIcon FAV_ICON = new ImageIcon(ContextMenu.class.getResource("/chatty/gui/star.png"));
    private static final ImageIcon GAME_FAV_ICON = new ImageIcon(ContextMenu.class.getResource("/chatty/gui/game.png"));
    
    public StreamInfosContextMenu(List<StreamInfo> selected, boolean liveStreams,
            boolean favFirst, boolean docked) {
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
            for (LiveStreamsDialog.Sorting s : LiveStreamsDialog.Sorting.values()) {
                addRadioItem("sort_"+s.key, s.getLabel(), SORT_GROUP, SORT_SUBMENU);
                getItem("sort_"+s.key).setToolTipText(s.getToolTipText());
            }
            addSeparator(SORT_SUBMENU);
            addCheckboxItem("sortOption_favFirst", Language.getString("streams.sortingOption.fav"), SORT_SUBMENU, favFirst);
            getItem("sortOption_favFirst").setToolTipText(SettingsUtil.addTooltipLinebreaks(Language.getString("streams.sortingOption.fav.tip")));
            
            addSeparator(SORT_SUBMENU);
            addItem("favoriteChannel", Language.getString("channelCm.favorite"), SORT_SUBMENU, FAV_ICON);
            addItem("unfavoriteChannel", Language.getString("channelCm.unfavorite"), SORT_SUBMENU);
            addItem("favoriteGame", Language.getString("channelCm.favoriteGame"), SORT_SUBMENU, GAME_FAV_ICON);
            addItem("unfavoriteGame", Language.getString("channelCm.unfavoriteGame"), SORT_SUBMENU);
            
            addItem("liveStreamsSettings", Language.getString("streams.cm.moreSettings"));
            
            addItem("showRemovedList", Language.getString("streams.cm.removedStreams"));
            addSeparator();
            addItem("manualRefreshStreams", Language.getString("streams.cm.refresh"));
            addSeparator();
            addCheckboxItem("dockToggleDocked", "Dock as tab", docked);
        }
    }
    
    public void setSorting(String key) {
        JMenuItem item = getItem("sort_"+key);
        if (item != null) {
            item.setSelected(true);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.streamInfosMenuItemClicked(e, streamInfos);
        }
    }
    
}
