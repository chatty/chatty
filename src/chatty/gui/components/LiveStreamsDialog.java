
package chatty.gui.components;

import chatty.ChannelFavorites;
import chatty.Chatty;
import chatty.Helper;
import chatty.Room;
import chatty.gui.DockedDialogHelper;
import chatty.gui.DockedDialogManager;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.TwitchUrl;
import chatty.gui.components.LiveStreamsList.ListDataChangedListener;
import chatty.gui.components.menus.CommandActionEvent;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.settings.CommandSettings;
import chatty.lang.Language;
import chatty.util.api.StreamInfo;
import chatty.util.commands.CustomCommand;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockContentContainer;
import chatty.util.settings.Settings;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 *
 * @author tduva
 */
public class LiveStreamsDialog extends JFrame {
    
    /**
     * Sorting algorithms for the live streams list. All the ones listed here
     * are automatically added to the context menu (in this order).
     */
    public enum Sorting {
    
        RECENT("recent", new LiveStreamsTimeChangedComparator()),
        UPTIME("uptime", new LiveStreamsUptimeComparator()),
        NAME("name", new LiveStreamsNameComparator()),
        GAME("game", new LiveStreamsGameComparator()),
        VIEWERS("viewers", new LiveStreamsViewersComparator());
        
        public final String key;
        public final Comparator<StreamInfo> comparator;
        
        Sorting(String key, Comparator<StreamInfo> comparator) {
            this.key = key;
            this.comparator = comparator;
        }
        
        /**
         * Get the label as defined in the localization file.
         * 
         * @return The string
         */
        public String getLabel() {
            return Language.getString("streams.sorting."+key);
        }
        
        /**
         * Get the tooltip text as defined in the localization file, or null if
         * none exists.
         * 
         * @return The string (or null if none exists)
         */
        public String getToolTipText() {
            return Language.getString("streams.sorting."+key+".tip", false);
        }
        
        public static Sorting fromKey(String key) {
            for (Sorting s : Sorting.values()) {
                if (s.key.equals(key)) {
                    return s;
                }
            }
            return null;
        }
    }
    
    public enum OpenAction {
    
        INFO("info"),
        JOIN("join"),
        STREAM("stream"),
        STREAM_POPOUT("streamPopout"),
        COMMAND("command");
        
        public final String key;
        
        OpenAction(String key) {
            this.key = key;
        }
        
        /**
         * Get the label as defined in the localization file.
         * 
         * @return The string
         */
        public String getLabel() {
            return Language.getString("streams.openAction."+key);
        }
        
        /**
         * Get the tooltip text as defined in the localization file, or null if
         * none exists.
         * 
         * @return The string (or null if none exists)
         */
        public String getToolTipText() {
            return Language.getString("streams.openAction."+key+".tip", false);
        }
        
        public static OpenAction fromKey(String key) {
            for (OpenAction s : OpenAction.values()) {
                if (s.key.equals(key)) {
                    return s;
                }
            }
            return null;
        }
    }
    
    
    private final ChannelInfoDialog channelInfo;
    
    private final JScrollPane scroll;
    private final LiveStreamsList list;
    private final LiveStreamsRemovedList removedList;
    private final CardLayout cardLayout;
    
    private String titleSorting = "";
    private String titleCounts = "";
    
    private boolean liveStreamListSelected = true;
    
    private final DockedDialogHelper helper;
    private final ContextMenuListener listener;
    private final Settings settings;
    
    public LiveStreamsDialog(MainGui g, ContextMenuListener listener,
            ChannelFavorites favs, Settings settings,
            DockedDialogManager dockedDialogs) {
        
        setTitle("Live Streams");
        setPreferredSize(new Dimension(280,350));
        
        this.listener = listener;
        this.settings = settings;
        
        ContextMenuListener localCml = new MyContextMenuListener();
        LiveStreamListener localLiveStreamListener = new LiveStreamListener() {

            /**
             * Actions other than INFO support several streams. One command is
             * dispatched as a CommandActionEvent with all selected streams.
             * This allows the command to either handle several streams at once
             * (e.g. joining) or handle each streams separately (opening URL).
             * 
             * @param streams 
             */
            @Override
            public void liveStreamClicked(Collection<StreamInfo> streams) {
                if (streams.isEmpty()) {
                    return;
                }
                handleStreamsAction(streams, false);
            }
        };
        // Create list
        list = new LiveStreamsList(localLiveStreamListener, favs, settings);
        list.addContextMenuListener(listener);
        list.addContextMenuListener(localCml);
        setSorting(Sorting.RECENT, true);
        list.addListDataChangedListener(new ListDataChangedListener() {

            @Override
            public void listDataChanged() {
                listUpdated();
            }

            @Override
            public void itemRemoved(StreamInfo item) {
                removedList.addStreamInfo(item);
            }

            @Override
            public void itemAdded(StreamInfo item) {
                removedList.removeStreamInfo(item);
            }
        });
        
        removedList = new LiveStreamsRemovedList(localLiveStreamListener);
        removedList.addContextMenuListener(localCml);
        removedList.addContextMenuListener(listener);
        removedList.addBackButtonListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                switchList();
            }
        });
        
        // Test data
        if (Chatty.DEBUG) {
//            List<StreamInfo> testData = new ArrayList<>();
//            StreamInfo testEntry1 = new StreamInfo("joshimuz", null);
//            testEntry1.set("Crash Nitro Kart WR Attempts - GTA:SA in December hopefully possibly maybe", "Crash Nitro Kart", 140, -1);
//            testEntry1.setDisplayName("Joshimoose");
//            testData.add(testEntry1);
//            StreamInfo testEntry2 = new StreamInfo("mrnojojojo", null);
//            testEntry2.set("GTA3 Wintermod Race", "Grand Theft Auto III", 123, -1);
//            testData.add(testEntry2);
//            StreamInfo testEntry3 = new StreamInfo("ismokybacon", null);
//            testEntry3.set("GTA:SA Badlands any% Race", "Grand Theft Auto: San Andreas", 14, -1);
//            testData.add(testEntry3);
//            StreamInfo testEntry4 = new StreamInfo("abc", null);
//            testEntry4.set("GTA:SA Badlands any% Race", null, 14, -1);
//            testData.add(testEntry4);
//            for (int i=0;i<0;i++) {
//                StreamInfo newEntry = new StreamInfo("test"+i, null);
//                newEntry.set("Random Title", "Some Game", i, -1);
//                testData.add(newEntry);
//            }
//            list.addStreams(testData);
//            removedList.addStreamInfo(testEntry1);
//            removedList.addStreamInfo(testEntry2);
//            removedList.addStreamInfo(testEntry3);
//            removedList.addStreamInfo(testEntry4);
        }
        
        
        
        channelInfo = new ChannelInfoDialog(this, null);
        GuiUtil.installEscapeCloseOperation(channelInfo);
        channelInfo.addContextMenuListener(listener);
        
        // Add to dialog
        cardLayout = new CardLayout();
        JPanel mainPanel = new JPanel(cardLayout);
        scroll = new JScrollPane(list);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        mainPanel.add(scroll);
        mainPanel.add(removedList);
        add(mainPanel);
        pack();
        
        DockContent content = new DockContentContainer("Live", mainPanel, dockedDialogs.getDockManager());
        content.setId("-liveStreams-");
        helper = dockedDialogs.createHelper(new DockedDialogHelper.DockedDialog() {
            @Override
            public void setVisible(boolean visible) {
                LiveStreamsDialog.super.setVisible(visible);
            }

            @Override
            public boolean isVisible() {
                return LiveStreamsDialog.super.isVisible();
            }

            @Override
            public void addComponent(Component comp) {
                add(comp);
            }

            @Override
            public void removeComponent(Component comp) {
                remove(comp);
            }

            @Override
            public Window getWindow() {
                return LiveStreamsDialog.this;
            }

            @Override
            public DockContent getContent() {
                return content;
            }
        });
        list.setDockedDialogHelper(helper);
    }
    
    public boolean isDocked() {
        if (helper != null) {
            return helper.isDocked();
        }
        return false;
    }
    
    @Override
    public void setVisible(boolean visible) {
        helper.setVisible(visible, true);
    }
    
    @Override
    public boolean isVisible() {
        if (helper != null) {
            return helper.isVisible();
        }
        else {
            return super.isVisible();
        }
    }
    
    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (helper != null) {
            helper.getContent().setLongTitle(title);
        }
    }
    
    /**
     * Adds the given stream info to the list, if valid and online, or sets
 it if it is already in the list.
     * 
     * @param streamInfo 
     */
    public void addStream(StreamInfo streamInfo) {
        list.addStream(streamInfo);
        channelInfo.update(streamInfo);
    }
    
    /**
     * Sets the sorting of the list.
     * 
     * @param sorting 
     */
    public void setSorting(String sorting, boolean favFirst) {
        Sorting s = Sorting.fromKey(sorting);
        if (s != null) {
            setSorting(s, favFirst);
        }
    }
    
    public void setFiltering(boolean favsOnly) {
        list.setFiltering(favsOnly);
        updateTitle();
    }
    
    public void setHistoryRange(int range) {
        channelInfo.setHistoryRange(range);
    }
    
    public void setHistoryVerticalZoom(boolean zoom) {
        channelInfo.setHistoryVerticalZoom(zoom);
    }
    
    /**
     * Sets the sorting of the list to the given comparator and changes the
     * title accordingly.
     * 
     * @param mode 
     */
    private void setSorting(Sorting s, boolean favFirst) {
        titleSorting = s.getLabel();
        updateTitle();
        list.setComparator(s, favFirst);
    }
    
    private void updateTitle() {
        if (liveStreamListSelected) {
            int shown = list.getModel().getSize();
            int total = ((SortedListModel)list.getModel()).getTotalSize();
            setTitle(Language.getString("streams.title",
                    shown == total ? shown : shown+"/"+total,
                    titleSorting));
        } else {
            setTitle(Language.getString("streams.removed.title"));
        }
    }
    
    /**
     * Called when any list data was changed (added/removed/"new" color).
     */
    private void listUpdated() {
        titleCounts = String.valueOf(list.getModel().getSize());
        updateTitle();
    }
    
    private class MyContextMenuListener extends ContextMenuAdapter {

        @Override
        public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streams) {
            String cmd = e.getActionCommand();
            if (cmd.equals("openChannelInfo")) {
                openChannelInfoDialog(streams.iterator().next());
            } else if (cmd.equals("showRemovedList")) {
                switchList();
            }
            helper.menuAction(e);
        }
    }
    
    private void openChannelInfoDialog(StreamInfo info) {
        if (!channelInfo.isVisible()) {
            channelInfo.setLocationRelativeTo(helper.getContent().getComponent());
        }
        channelInfo.set(info);
        channelInfo.setVisible(true);
    }
    
    private void switchList() {
        liveStreamListSelected = !liveStreamListSelected;
        updateTitle();
        cardLayout.next(helper.getContent().getComponent());
    }
    
    /**
     * Handle streams action based on the liveStreams settings.
     * 
     * @param streams
     * @param external Must be set to true if this is not triggered out of the
     * Live Streams List
     */
    public void handleStreamsAction(Collection<StreamInfo> streams, boolean external) {
        Function<String, String> makeOpenCommand = url -> "/chain /foreach $fs($1-) > /openUrl " + url + " | /join $replace($1-, ,\\,)";
        switch (OpenAction.fromKey(settings.getString("liveStreamsAction"))) {
            case INFO:
                if (!external) {
                    /**
                     * Only open the Live Streams List specific Channel Info
                     * Dialog when opened out of the Live Streams List.
                     */
                    openChannelInfoDialog(streams.iterator().next());
                }
                else {
                    CustomCommand command = CustomCommand.parse("/join $replace($1-, ,\\,)");
                    CommandActionEvent e = new CommandActionEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, "command"), command);
                    listener.streamInfosMenuItemClicked(e, streams);
                }
                break;
            case JOIN: {
                CustomCommand command = CustomCommand.parse("/join $replace($1-, ,\\,)");
                CommandActionEvent e = new CommandActionEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, "command"), command);
                listener.streamInfosMenuItemClicked(e, streams);
            }
            break;
            case STREAM: {
                CustomCommand command = CustomCommand.parse(makeOpenCommand.apply(TwitchUrl.makeTwitchStreamUrl("\\$1")));
                CommandActionEvent e = new CommandActionEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, "command"), command);
                listener.streamInfosMenuItemClicked(e, streams);
            }
            break;
            case STREAM_POPOUT: {
                CustomCommand command = CustomCommand.parse(makeOpenCommand.apply(TwitchUrl.makeTwitchPlayerUrl("\\$1")));
                CommandActionEvent e = new CommandActionEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, "command"), command);
                listener.streamInfosMenuItemClicked(e, streams);
            }
            break;
            case COMMAND: {
                String commandValue = settings.getString("liveStreamsCommand");
                CustomCommand customCommand = CustomCommand.parse(commandValue.trim());
                if (customCommand.hasError()) {
                    CommandSettings.showCommandInfoPopup(scroll, customCommand);
                }
                else {
                    CommandActionEvent e = new CommandActionEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, "command"), customCommand);
                    listener.streamInfosMenuItemClicked(e, streams);
                }
            }
            break;
        }
    }
    
    /**
     * Comparator to sort StreamInfo objects by the name of the stream.
     */
    private static class LiveStreamsNameComparator implements Comparator<StreamInfo> {

        @Override
        public int compare(StreamInfo o1, StreamInfo o2) {
            return o1.getStream().compareTo(o2.getStream());
        } 
    }
    
    /**
     * Comparator to sort StreamInfo objects by the uptime.
     */
    private static class LiveStreamsUptimeComparator implements Comparator<StreamInfo> {

        @Override
        public int compare(StreamInfo o1, StreamInfo o2) {
            long time1 = o1.getTimeStartedWithPicnicAgo();
            long time2 = o2.getTimeStartedWithPicnicAgo();
            //System.out.println("Comparing: "+o1.getStream()+time1+" "+time2+o2.getStream());
            if (time1 == time2) {
                return o1.getStream().compareTo(o2.getStream());
            }
            if (time1 > time2) {
                return 1;
            }
            return -1;
        } 
    }
    
    /**
     * Comparator to sort StreamInfo objects by the last time the status was
     * changed.
     */
    private static class LiveStreamsTimeChangedComparator implements Comparator<StreamInfo> {

        @Override
        public int compare(StreamInfo o1, StreamInfo o2) {
            long time1 = o1.getStatusChangeTimeAgo();
            long time2 = o2.getStatusChangeTimeAgo();
            //System.out.println("Comparing: "+o1.getStream()+time1+" "+time2+o2.getStream());
            if (time1 == time2) {
                return o1.getStream().compareTo(o2.getStream());
            }
            if (time1 > time2) {
                return 1;
            }
            return -1;
        } 
    }
    
    /**
     * Comparator to sort StreamInfo objects by the game.
     */
    private static class LiveStreamsGameComparator implements Comparator<StreamInfo> {

        @Override
        public int compare(StreamInfo o1, StreamInfo o2) {
            String game1 = o1.getGame();
            String game2 = o2.getGame();
            //if (game1 == game2 || (game1 != null && game1.equals(game2))) {
            if (game1 == null ? game2 == null : game1.equals(game2)) {
                return o1.getStream().compareTo(o2.getStream());
            }
            if (game1 == null) {
                return 1;
            }
            if (game2 == null) {
                return -1;
            }
            return game1.compareTo(game2);
        }
    }
    
    /**
     * Comparator to sort StreamInfo objects by the name of the stream.
     */
    private static class LiveStreamsViewersComparator implements Comparator<StreamInfo> {

        @Override
        public int compare(StreamInfo o1, StreamInfo o2) {
            if (o1.getViewers() > o2.getViewers()) {
                return -1;
            }
            if (o1.getViewers() < o2.getViewers()) {
                return 1;
            }
            return o1.getStream().compareTo(o2.getStream());
        } 
    }
    
}
