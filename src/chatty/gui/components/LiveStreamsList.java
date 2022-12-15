
package chatty.gui.components;

import chatty.ChannelFavorites;
import chatty.ChannelFavorites.ChangeListener;
import chatty.Helper;
import chatty.gui.DockedDialogHelper;
import chatty.gui.GuiUtil;
import chatty.gui.components.JListActionHelper.Action;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.StreamInfosContextMenu;
import chatty.util.DateTime;
import chatty.util.ElapsedTime;
import chatty.util.api.StreamInfo;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 *
 * @author tduva
 */
public class LiveStreamsList extends JList<StreamInfo> {
    
    private static final Logger LOGGER = Logger.getLogger(LiveStreamsList.class.getName());

    private static final int UPDATE_TIMER_DELAY = 5;
    private static final int CHECK_DELAY = 20;
    private static final int REPAINT_DELAY = 60;
    
    private final SortedListModel<StreamInfo> data;
    private final MyCellRenderer renderer;
    private final List<ContextMenuListener> contextMenuListeners;
    private final LiveStreamListener liveStreamListener;
    
    /**
     * Holds a copy of the current channel favorites.
     */
    private final Set<String> favs = new HashSet<>();
    private final Set<String> gameFavs = new HashSet<>();
    
    /**
     * How long after the last stream status change, that it uses the TITLE_NEW
     * border.
     */
    private static final int STREAMINFO_NEW_TIME = 180;
    
    private ListDataChangedListener listDataChangedListener;
    
    private JPopupMenu lastContextMenu;
    private LiveStreamsDialog.Sorting currentSorting;
    private boolean favFirst;
    private boolean favsOnly;
    private DockedDialogHelper dockedHelper;
    
    private final ElapsedTime lastCheckedET = new ElapsedTime();
    private final ElapsedTime lastRepaintedET = new ElapsedTime();
    
    private final Timer resortTimer;
    
    public LiveStreamsList(LiveStreamListener liveStreamListener,
            ChannelFavorites channelFavorites, Settings settings) {
        data = new SortedListModel<>();
        setModel(data);
        renderer = new MyCellRenderer(favs, gameFavs);
        setCellRenderer(renderer);
        contextMenuListeners = new ArrayList<>();
        this.liveStreamListener = liveStreamListener;
        addListeners();
        
        Timer updateTimer = new Timer(UPDATE_TIMER_DELAY*1000, e -> update());
        updateTimer.setRepeats(true);
        updateTimer.start();
        
        resortTimer = new Timer(50, e -> {
            resortTimer();
        });
        
        ChangeListener favChangeListener = () -> {
            // Listener may be in ChannelFavorites lock, but calling it should
            // be fine (same lock)
            Set<String> favorites = channelFavorites.getFavorites();
            SwingUtilities.invokeLater(() -> {
                // Make sure to run in EDT for updating
                favs.clear();
                for (String chan : favorites) {
                    // Convert for easier usage
                    favs.add(Helper.toStream(chan));
                }
                resort();
                data.updateFiltering();
            });
        };
        channelFavorites.addChangeListener(favChangeListener);
        // Init once
        favChangeListener.favoritesChanged();
        
        settings.addSettingChangeListener((String setting, int type, Object value) -> {
            if (setting.equals("gameFavorites")) {
                updateGameFavs(settings);
            }
            if (setting.equals("liveStreamsChatIcon")) {
                renderer.setShowIsOpen((Boolean) value);
                repaint();
            }
        });
        updateGameFavs(settings);
        renderer.setShowIsOpen(settings.getBoolean("liveStreamsChatIcon"));
    }
    
    public void setDockedDialogHelper(DockedDialogHelper helper) {
        this.dockedHelper = helper;
    }
    
    private void updateGameFavs(Settings settings) {
        SwingUtilities.invokeLater(() -> {
            gameFavs.clear();
            gameFavs.addAll(settings.getList("gameFavorites"));
            resort();
            data.updateFiltering();
        });
    }

    public void addContextMenuListener(ContextMenuListener listener) {
        if (listener != null) {
            contextMenuListeners.add(listener);
        }
    }
    
    public void addStreams(List<StreamInfo> infos) {
        for (StreamInfo info : infos) {
            addStream(info);
        }
    }
    
    public void setComparator(LiveStreamsDialog.Sorting s, boolean favFirst) {
        Comparator<StreamInfo> comp = s.comparator;
        if (favFirst) {
            comp = new Comparator<StreamInfo>() {

                @Override
                public int compare(StreamInfo o1, StreamInfo o2) {
                    boolean fav1 = favs.contains(o1.stream) || gameFavs.contains(o1.getGame());
                    boolean fav2 = favs.contains(o2.stream) || gameFavs.contains(o2.getGame());
                    if (fav1 && !fav2) {
                        return -1;
                    }
                    if (fav2 && !fav1) {
                        return 1;
                    }
                    return s.comparator.compare(o1, o2);
                }
            };
        }
        data.setComparator(comp);
        resort();
        currentSorting = s;
        this.favFirst = favFirst;
    }
    
    public void setFiltering(boolean favsOnly) {
        if (favsOnly == this.favsOnly) {
            return;
        }
        this.favsOnly = favsOnly;
        if (favsOnly) {
            data.setFilter(item -> {
                return favs.contains(item.stream) || gameFavs.contains(item.getGame());
            });
        }
        else {
            data.setFilter(null);
        }
    }
    
    /**
     * Adds or removes and readds a stream.
     * 
     * Note: The "old" way to resort the list was to remove+add each updated
     * StreamInfo. This seems to be fine until any item in the list was selected
     * (even if the selection is removed again), which causes a huge amount of
     * getListCellRendererComponent calls (as in over 1k with 25 entries, ~30
     * normally).
     * 
     * The new way is to only add non-added items and resort the list
     * afterwards, which seems to cause a few more calls (~60) but doesn't seem
     * to be affected by the selection. Collections.sort() throws an exception
     * sometimes though (IllegalArgumentException: Comparison method violates
     * its general contract!), which may be caused by the StreamInfo being
     * modified during the sorting, or some Comparator actually being wrong.
     * Either way, that probably was an issue before as well, but finding the
     * insertion point when adding an item probably simply didn't throw an
     * exception.
     *
     * @param info 
     */
    public void addStream(StreamInfo info) {
        if (info.isValidEnough() && info.getOnline()) {
            if (!data.contains(info)) {
                data.add(info);
            }
            itemAdded(info);
        } else if (data.contains(info)) {
            data.remove(info);
            itemRemoved(info);
        }
        resort();
        listDataChanged();
    }
    
    private void resort() {
        resortTimer.start();
    }
    
    private void resortTimer() {
        /**
         * This is not really a fix, but modification of the StreamStatus
         * objects while they are being sorted may be annoying to change, and
         * if the sorting is indeed wrong it should fix itself on the next
         * update.
         */
        try {
            data.resort();
        } catch (Exception ex) {
            LOGGER.warning("LiveStreamsList resort: "+ex);
        }
        resortTimer.stop();
    }
    
    /**
     * Adds the listener to notify about list data changes.
     * 
     * @param listener 
     */
    public void addListDataChangedListener(ListDataChangedListener listener) {
        this.listDataChangedListener = listener;
    }
    
    /**
     * Checks all added streams and removes invalid ones.
     */
    private void checkStreams() {
        if (!lastCheckedET.secondsElapsed(CHECK_DELAY)) {
            return;
        }
        lastCheckedET.set();
        Set<StreamInfo> toRemove = new HashSet<>();
        for (StreamInfo info : data) {
            if (!info.isValidEnough() || !info.getOnline()) {
                toRemove.add(info);
            }
        }
        // Remove invalid items
        for (StreamInfo info : toRemove) {
            data.remove(info);
            itemRemoved(info);
        }
        // Update and inform only if items were actually removed
        if (!toRemove.isEmpty()) {
            listDataChanged();
        }
    }
    
    /**
     * Clears the selection if dialog is not active.
     */
    private void checkToClearSelection() {
        if (!isFocusOwner() &&
                (lastContextMenu == null || !lastContextMenu.isVisible())) {
            clearSelection();
        }
    }
    
    /**
     * Call to all the regular update stuff.
     */
    private void update() {
        checkToClearSelection();
        checkStreams();
        checkToRepaint();
    }
    
    /**
     * Repaints the list on a set delay to update colors.
     */
    private void checkToRepaint() {
        if (lastRepaintedET.secondsElapsed(REPAINT_DELAY)) {
            repaint();
            lastRepaintedET.set();
        }
    }
    
    /**
     * Inform the listener that the list data has changed (new items, removed
     * items, updated new streams count).
     */
    private void listDataChanged() {
        if (listDataChangedListener != null) {
            listDataChangedListener.listDataChanged();
        }
    }
    
    private void itemRemoved(StreamInfo item) {
        if (listDataChangedListener != null) {
            listDataChangedListener.itemRemoved(item);
        }
    }
    
    private void itemAdded(StreamInfo item) {
        if (listDataChangedListener != null) {
            listDataChangedListener.itemAdded(item);
        }
    }
    
    private void addListeners() {
        ComponentListener cl = new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
// Trick from kleopatra:
// https://stackoverflow.com/questions/7306295/swing-jlist-with-multiline-text-and-dynamic-height
                // next line possible if list is of type JXList
                // list.invalidateCellSizeCache();
                // for core: force cache invalidation by temporarily setting fixed height
                setFixedCellHeight(10);
                setFixedCellHeight(-1);
            }

        };
        addComponentListener(cl);

        JListActionHelper.install(this, (a, l, s) -> {
            if (a == Action.CONTEXT_MENU) {
                StreamInfosContextMenu m = new StreamInfosContextMenu(s, true, favFirst, favsOnly, dockedHelper.isDocked());
                m.setSorting(currentSorting.key);
                for (ContextMenuListener cml : contextMenuListeners) {
                    m.addContextMenuListener(cml);
                }
                lastContextMenu = m;
                m.show(this, l.x, l.y);
            } else if (a == Action.ENTER) {
                List<String> channels = new ArrayList<>();
                s.forEach(si -> channels.add(si.stream));
                for (ContextMenuListener cml : contextMenuListeners) {
                    cml.streamsMenuItemClicked(
                            new ActionEvent(s, 0, "join"),
                            channels);
                }
            } else if (a == Action.DOUBLE_CLICK || a == Action.SPACE) {
                if (s != null && !s.isEmpty() && liveStreamListener != null) {
                    liveStreamListener.liveStreamClicked(s);
                }
            }
        });
    }
    
    /**
     * To prevent horizontal scrolling and allow for tracking of the viewport
     * width.
     * 
     * @return 
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Custom renderer to use a text area and borders etc.
     */
    private static class MyCellRenderer extends DefaultListCellRenderer {

        private static final Border PADDING =
                BorderFactory.createEmptyBorder(2, 3, 1, 3);
        private static final Border MARGIN =
                BorderFactory.createEmptyBorder(4, 3, 1, 3);
        private static final Border TITLE =
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY);
        private static final Border TITLE_SELECTED =
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(165,165,165));
        private static final Border TITLE_NEW =
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK);
        
        private final ImageIcon favIcon = new ImageIcon(getClass().getResource("/chatty/gui/star.png"));
        private final ImageIcon gameFavIcon = new ImageIcon(getClass().getResource("/chatty/gui/game.png"));
        private final ImageIcon chatIcon = new ImageIcon(getClass().getResource("/chatty/gui/chat.png"));
        
        private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
        
        private final JTextArea area;
        private final Set<String> favs;
        private final Set<String> gameFavs;
        
        private boolean showIsOpen;
        
        public MyCellRenderer(Set<String> favs, Set<String> gameFavs) {
            area = new JTextArea();
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            this.favs = favs;
            this.gameFavs = gameFavs;
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (value == null) {
                area.setText(null);
                return area;
            }
            //System.out.println("Getting rubberstamp for "+value);
            StreamInfo info = (StreamInfo)value;
            
            // Make Text
            String text = info.getTitle();
            if (!info.getGame().isEmpty()) {
                text += "\n("+info.getGame()+")";
            }
            area.setText(text);
            
            // Adjust size
            int width = list.getWidth();
            if (width > 0) {
                area.setSize(width, Short.MAX_VALUE);
            }
            
            // Add Borders
            String title;
            if (info.getTimeStartedWithPicnic() != -1) {
                title = String.format("%s%s (%s | %s)",
                        info.getStreamTypeString(),
                        info.getCapitalizedName(),
                        Helper.formatViewerCount(info.getViewers()),
                        DateTime.agoUptimeCompact(info.getTimeStartedWithPicnic()));
            }
            else {
                title = String.format("%s%s (%s)",
                        info.getStreamTypeString(),
                        info.getCapitalizedName(),
                        Helper.formatViewerCount(info.getViewers()));
            }
            Border titleBaseBorder = isSelected ? TITLE_SELECTED : TITLE;
            if (info.getStatusChangeTimeAgo() < STREAMINFO_NEW_TIME) {
                titleBaseBorder = TITLE_NEW;
            }
            LiveStreamsTitledBorder titleBorder = new LiveStreamsTitledBorder(titleBaseBorder, title, TitledBorder.CENTER, TitledBorder.TOP);
            
            boolean fav = favs.contains(info.stream);
            boolean gameFav = gameFavs.contains(info.getGame());
            boolean showChatIcon = showIsOpen && info.isOpen();
            if (fav || gameFav || showChatIcon) {
                titleBorder.getLabel().setIcon(getTitleIcon(fav, gameFav, showChatIcon));
            }
            Border innerBorder = BorderFactory.createCompoundBorder(titleBorder, PADDING);
            Border border = BorderFactory.createCompoundBorder(MARGIN, innerBorder);
            area.setBorder(border);
            
            // Selected Color
            if (isSelected) {
                area.setBackground(list.getSelectionBackground());
                area.setForeground(list.getSelectionForeground());
            } else {
                area.setBackground(list.getBackground());
                area.setForeground(list.getForeground());
            }
            return area;
        }
        
        private ImageIcon getTitleIcon(boolean fav, boolean gameFav, boolean isOpen) {
            int id = (fav ? 1 << 0 : 0) + (gameFav ? 1 << 1 : 0) + (isOpen ? 1 << 2 : 0);
            ImageIcon icon = iconCache.get(id);
            if (icon != null) {
                return icon;
            }
            if (fav) {
                icon = combineIcons(icon, favIcon);
            }
            if (gameFav) {
                icon = combineIcons(icon, gameFavIcon);
            }
            if (isOpen) {
                icon = combineIcons(icon, chatIcon);
            }
            if (icon != null) {
                iconCache.put(id, icon);
            }
            return icon;
        }
        
        private ImageIcon combineIcons(ImageIcon a, ImageIcon b) {
            if (a == null) {
                return b;
            }
            return GuiUtil.combineIcons(a, b, 2);
        }
        
        public void setShowIsOpen(boolean showIsOpen) {
            this.showIsOpen = showIsOpen;
        }
        
    }
    
    public interface ListDataChangedListener {
        void listDataChanged();
        void itemRemoved(StreamInfo item);
        void itemAdded(StreamInfo item);
    }
    
}
