
package chatty.gui.components;

import chatty.Chatty;
import chatty.gui.GuiUtil;
import chatty.gui.components.LiveStreamsList.ListDataChangedListener;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.api.StreamInfo;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Comparator;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

/**
 *
 * @author tduva
 */
public class LiveStreamsDialog extends JFrame {
    
    private static final Comparator<StreamInfo> NAME_COMPARATOR
            = new LiveStreamsNameComparator();
    private static final Comparator<StreamInfo> CHANGED_COMPARATOR
            = new LiveStreamsTimeChangedComparator();
    private static final Comparator<StreamInfo> GAME_COMPARATOR
            = new LiveStreamsGameComparator();
    private static final Comparator<StreamInfo> VIEWERS_COMPARATOR
            = new LiveStreamsViewersComparator();
    
    
    private final ChannelInfoDialog channelInfo;
    private static final String BASE_TITLE = "Live Streams";
    
    private final JScrollPane scroll;
    private final LiveStreamsList list;
    private final LiveStreamsRemovedList removedList;
    private final CardLayout cardLayout;
    
    private String titleSorting = "";
    private String titleCounts = "";
    
    private boolean liveStreamListSelected = true;
    
    public LiveStreamsDialog(ContextMenuListener listener) {
        
        setTitle("Live Streams");
        setPreferredSize(new Dimension(280,350));
        
        ContextMenuListener localCml = new MyContextMenuListener();
        LiveStreamListener localLiveStreamListener = new LiveStreamListener() {

            @Override
            public void liveStreamClicked(StreamInfo stream) {
                openChannelInfoDialog(stream);
            }
        };
        // Create list
        list = new LiveStreamsList(localLiveStreamListener);
        list.addContextMenuListener(listener);
        list.addContextMenuListener(localCml);
        setSorting(CHANGED_COMPARATOR);
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
        
        
        
        channelInfo = new ChannelInfoDialog(this);
        GuiUtil.installEscapeCloseOperation(channelInfo);
        channelInfo.addContextMenuListener(listener);
        
        // Add to dialog
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        scroll = new JScrollPane(list);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll);
        add(removedList);
        pack();
    };
    
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
    public void setSorting(String sorting) {
        if (sorting.equals("recent")) {
            setSorting(CHANGED_COMPARATOR);
        } else if (sorting.equals("name")) {
            setSorting(NAME_COMPARATOR);
        } else if (sorting.equals("game")) {
            setSorting(GAME_COMPARATOR);
        } else if (sorting.equals("viewers")) {
            setSorting(VIEWERS_COMPARATOR);
        }
    }
    
    public void setHistoryRange(int range) {
        channelInfo.setHistoryRange(range);
    }
    
    /**
     * Sets the sorting of the list to the given comparator and changes the
     * title accordingly.
     * 
     * @param mode 
     */
    private void setSorting(Comparator<StreamInfo> mode) {
        String text = "Sorting: ";
        if (mode == NAME_COMPARATOR) {
            text += "Name";
        } else if (mode == CHANGED_COMPARATOR) {
            text += "Recent";
        } else if (mode == GAME_COMPARATOR) {
            text += "Game";
        } else if (mode == VIEWERS_COMPARATOR) {
            text += "Viewers";
        }
        titleSorting = text;
        updateTitle();
        list.setComparator(mode);
    }
    
    private void updateTitle() {
        if (liveStreamListSelected) {
            setTitle(BASE_TITLE+" ("+titleCounts+") ["+titleSorting+"]");
        } else {
            setTitle("Offline/Left Streams");
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
        }
    }
    
    private void openChannelInfoDialog(StreamInfo info) {
        if (!channelInfo.isVisible()) {
            channelInfo.setLocationRelativeTo(LiveStreamsDialog.this);
        }
        channelInfo.set(info);
        channelInfo.setVisible(true);
    }
    
    private void switchList() {
        liveStreamListSelected = !liveStreamListSelected;
        updateTitle();
        cardLayout.next(getContentPane());
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
