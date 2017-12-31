
package chatty.gui.components;

import chatty.Chatty;
import chatty.gui.LinkListener;
import chatty.gui.UrlOpener;
import chatty.gui.components.admin.StatusHistoryEntry;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.DateTime;
import static chatty.util.DateTime.H;
import static chatty.util.DateTime.S;
import chatty.util.MiscUtil;
import chatty.util.StringUtil;
import chatty.util.api.CommunitiesManager.Community;
import chatty.util.api.StreamInfo;
import chatty.util.api.StreamInfo.StreamType;
import chatty.util.api.StreamInfoHistoryItem;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 *
 * @author tduva
 */
public class ChannelInfoDialog extends JDialog implements ViewerHistoryListener {

    private static final String STATUS_LABEL_TEXT = Chatty.lang.GET("CHANNELINFODIAG_STATUS", "Status:");
    private static final String STATUS_LABEL_TEXT_HISTORY = Chatty.lang.GET("CHANNELINFODIAG_STATUS_HISTORY", "Status (History):");
    
    private static final String GAME_LABEL_TEXT = Chatty.lang.GET("CHANNELINFODIAG_PLAYING", "Playing:");
    private static final String GAME_LABEL_TEXT_VOD = Chatty.lang.GET("CHANNELINFODIAG_VODCAST", "VODCAST / Playing:");
    
    private final JLabel statusLabel = new JLabel(Chatty.lang.GET("CHANNELINFODIAG_STATUS", "Status:"));
    private final ExtendedTextPane title = new ExtendedTextPane();
    
    private final JLabel onlineSince = new JLabel();
    private boolean historyItemSelected;
    
    private final JLabel gameLabel = new JLabel(GAME_LABEL_TEXT);
    private final JTextField game = new JTextField();
    
    private final LinkLabel communityLabel;
    private final LinkLabel testLabel = new LinkLabel(null, null);
    private List<Community> communities;
    
    private final JLabel historyLabel = new JLabel(Chatty.lang.GET("CHANNELINFODIAG_VIEWERS", "Viewers:"));
    private final ViewerHistory history = new ViewerHistory();
    
    private StreamInfo currentStreamInfo;
    
    private String statusText = "";
    private String gameText = "";
    
    /**
     * The time the stream of the current StreamInfo started. This is -1 if the
     * stream is offline or the StreamInfo is invalid, or no time was received.
     */
    private long timeStarted = -1;
    
    public ChannelInfoDialog(Frame owner) {
        super(owner);
        setTitle(Chatty.lang.GET("CHANNELINFODIAG_TITLE", "Channel Info"));
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        // Status
        gbc = makeGbc(0,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        add(statusLabel,gbc);
        
        gbc = makeGbc(1,0,1,1);
        gbc.anchor = GridBagConstraints.EAST;
        add(onlineSince, gbc);
        
        title.setEditable(false);
        title.setLinkListener(new MyLinkListener());
        JScrollPane scroll = new JScrollPane(title);
        scroll.setPreferredSize(new Dimension(300,70));
        scroll.setMinimumSize(new Dimension(1,45));
        
        gbc = makeGbc(0,1,2,1);
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(scroll,gbc);

        // Game
        gbc = makeGbc(0,2,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        add(gameLabel,gbc);
        
        game.setEditable(false);
        gbc = makeGbc(0,3,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(game,gbc);
        
        gbc = makeGbc(0, 2, 2, 1);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.9;
        communityLabel = new LinkLabel(" ", new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                if (type.equals("overflow")) {
                    int overflow = Integer.valueOf(ref);
                    JPopupMenu menu = new JPopupMenu();
                    for (int i=communities.size()-overflow;i<communities.size();i++) {
                        Community c = communities.get(i);
                        Action a = new AbstractAction(c.toString()) {
                            
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String url = "https://www.twitch.tv/communities/"+c.getName();
                                UrlOpener.openUrlPrompt(ChannelInfoDialog.this, url);
                            }
                        };
                        menu.add(new JMenuItem(a));
                    }
                    menu.addSeparator();
                    menu.add(new JMenuItem(new AbstractAction("Copy all") {
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            MiscUtil.copyToClipboard(StringUtil.join(communities, ", "));
                        }
                    }));
                    menu.show(communityLabel, communityLabel.getWidth(), communityLabel.getHeight());
                } else {
                    String url = "https://www.twitch.tv/communities/"+ref;
                    UrlOpener.openUrlPrompt(ChannelInfoDialog.this, url);
                }
            }
        });
        communityLabel.setMargin(game.getMargin());
        
        // Size listener to the dialog, since that is most relevant (other
        // changes update anyway)
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                updateCommunities();
            }
        });
        add(communityLabel, gbc);
 
        // Graph
        gbc = makeGbc(0,4,1,1);
        gbc.insets = new Insets(3,5,3,3);
        gbc.anchor = GridBagConstraints.WEST;
        add(historyLabel,gbc);
        
        history.setListener(this);
        history.setForegroundColor(gameLabel.getForeground());
        history.setBackgroundColor(gameLabel.getBackground());
        history.setFontSize(gameLabel.getFont().getSize());
        history.setPreferredSize(new Dimension(300,150));
        history.setMinimumSize(new Dimension(1,20));
        gbc = makeGbc(0,5,2,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4,4,4,4);
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(history,gbc);
        
        Timer timer = new Timer(30000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateOnlineTime(currentStreamInfo);
            }
        });
        timer.setRepeats(true);
        timer.start();
        
        pack();
        setMinimumSize(new Dimension(200,0));
    }
    
    /**
     * Sets a new StreamInfo object.
     * 
     * @param streamInfo 
     */
    public void set(StreamInfo streamInfo) {
        String name = streamInfo.getDisplayName();
        if (!streamInfo.hasRegularDisplayName()) {
            name += " ("+streamInfo.getCapitalizedName()+")";
        }
        this.setTitle("Channel: "+name+(streamInfo.getFollowed() ? " (followed)" : ""));
        if (streamInfo.isValid() && streamInfo.getOnline()) {
            statusText = streamInfo.getTitle();
            gameText = streamInfo.getGame();
            if (gameText.isEmpty()) {
                gameText = Chatty.lang.GET("CHANNELINFODIAG_NOGAME", "No game");
            }
            timeStarted = streamInfo.getTimeStarted();
            onlineSince.setText(null);
            setCommunities(streamInfo.getCommunities());
            updateStreamType(streamInfo.getStreamType());
        }
        else if (streamInfo.isValid()) {
            statusText = Chatty.lang.GET("CHANNELINFODIAG_STREAMOFFLINE", "Stream offline");
            gameText = "";
            timeStarted = -1;
            setCommunities(null);
        }
        else {
            statusText = Chatty.lang.GET("CHANNELINFODIAG_NOSTREAMINFO", "[No Stream Information]");
            gameText = "";
            timeStarted = -1;
            onlineSince.setText(null);
            onlineSince.setToolTipText(null);
            setCommunities(null);
        }
        title.setText(statusText);
        game.setText(gameText);
        updateCommunities();
        if (!Chatty.DEBUG || streamInfo.isValid()) {
            history.setHistory(streamInfo.getStream(), streamInfo.getHistory());
        }
        currentStreamInfo = streamInfo;
        updateOnlineTime(streamInfo);
    }
    
    /**
     * Updates the dialog if the given StreamInfo object is the one already
     * set, or does nothing otherwise.
     * 
     * @param streamInfo 
     */
    public void update(StreamInfo streamInfo) {
        if (streamInfo == currentStreamInfo) {
            set(streamInfo);
        }
    }
    
    private void setCommunities(List<Community> c) {
        if (c != null && c.contains(null)) {
            // This usually shouldn't contain null elements, but just in case
            c = new ArrayList<>(c);
            c.removeIf(e -> { return e == null; });
        }
        communities = c;
    }
    
    private void updateCommunities() {
        // Wrapped in invokeLater so that the size of the gameLabel is updated
        SwingUtilities.invokeLater(() -> {
            if (communities == null || communities.isEmpty()) {
                communityLabel.setText("");
            } else {
                // -80 to leave some distance between "Playing" and this
                int availableWidth = getWidth() - gameLabel.getWidth() - 80;
                for (int i = 0; i <= communities.size(); i++) {
                    String result = makeCommunitiesText(communities, i);
                    // Use a separate label, as to not update the acual one
                    // until it fits
                    testLabel.setText(result);
                    if (availableWidth > testLabel.getPreferredSize().width) {
                        // Found content that fits, so go set it and stop
                        communityLabel.setText(result);
                        break;
                    }
                }
            }
        });
    }
    
    /**
     * Creates the communities label text with the given data.
     * 
     * @param communities The data
     * @param overflow How many communities should be in the overflow menu
     * @return The resulting String
     */
    private String makeCommunitiesText(List<Community> communities, int overflow) {
        StringBuilder b = new StringBuilder("<div style='text-align:right'>");
        for (int i = 0; i < communities.size() - overflow; i++) {
            Community c = communities.get(i);
            if (i > 0) {
                b.append(", ");
            }
            b.append("[community:").append(c.getName()).append(" ");
            b.append(c.getDisplayName()).append("]");
        }
        if (overflow > 0) {
            b.append(" [overflow:").append(overflow).append(" (+").append(overflow).append(")]");
        }
        b.append("</div>");
        return b.toString();
    }
    
    private void updateStreamType(StreamType streamType) {
        if (streamType == StreamType.WATCH_PARTY) {
            gameLabel.setText(GAME_LABEL_TEXT_VOD);
        } else {
            gameLabel.setText(GAME_LABEL_TEXT);
        }
    }
    
    private void updateOnlineTime(StreamInfo info) {
        if (historyItemSelected) {
            return;
        }
        if (info == null) {
            onlineSince.setText(null);
        } else if (info.isValid() && info.getOnline()) {
            updateOnlineTime(info.getTimeStarted(), info.getTimeStartedWithPicnic(), System.currentTimeMillis());
        } else if (info.isValid()) {
            onlineSince.setText("Offline");
            if (info.getLastOnlineTime() != -1) {
                String lastBroadcastTime = formatTime(info.getTimeStarted(), info.getLastOnlineTime());
                if (info.getTimeStarted() != info.getTimeStartedWithPicnic()) {
                    String withPicnic = formatTime(info.getTimeStartedWithPicnic(), info.getLastOnlineTime());
                    onlineSince.setToolTipText("Last broadcast length (probably approx.): " + lastBroadcastTime+" (With PICNIC: "+withPicnic+")");
                } else {
                    onlineSince.setToolTipText("Last broadcast length (probably approx.): " + lastBroadcastTime);
                }
            } else {
                onlineSince.setToolTipText(null);
            }
        } else {
            onlineSince.setText(null);
        }
    }
    
    private void updateOnlineTime(StreamInfoHistoryItem item) {
        updateOnlineTime(item.getStreamDuration(), item.getStreamDurationWithPicnic(), item.getTime());
    }
    
    private void updateOnlineTime(long started, long withPicnic, long current) {
        if (started != -1) {
            if (withPicnic != started) {
                onlineSince.setText("Live: " + formatTime(started, current) + " (" + formatTime(withPicnic, current)+")");
                onlineSince.setToolTipText("Stream started: "+DateTime.formatFullDatetime(started)
                        +" (With PICNIC: "
                        +DateTime.formatFullDatetime(withPicnic)+")");
            } else {
                onlineSince.setText("Live: " + formatTime(started, current));
                onlineSince.setToolTipText("Stream started: "+DateTime.formatFullDatetime(timeStarted));
            }
        }
    }
    
    private static String formatTime(long time, long time2) {
        return formatDuration(time2 - time);
    }
    
    private static String formatDuration(long time) {
        return DateTime.duration(time, H, 2, S);
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(3,3,3,3);
        return gbc;
    }

    @Override
    public void itemSelected(StreamInfoHistoryItem item) {
        historyItemSelected = true;
        title.setText(item.getTitle());
        game.setText(item.getGame());
        statusLabel.setText(STATUS_LABEL_TEXT_HISTORY);
        updateStreamType(item.getStreamType());
        updateOnlineTime(item);
        setCommunities(item.getCommunities());
        updateCommunities();
    }
    
    @Override
    public void noItemSelected() {
        historyItemSelected = false;
        this.title.setText(statusText);
        this.game.setText(gameText);
        statusLabel.setText(STATUS_LABEL_TEXT);
        updateStreamType(currentStreamInfo.getStreamType());
        updateOnlineTime(currentStreamInfo);
        setCommunities(currentStreamInfo.getCommunities());
        updateCommunities();
    }
    
    public void setHistoryRange(int minutes) {
        history.setRange(minutes*60*1000);
    }
    
    public void addContextMenuListener(ContextMenuListener listener) {
        history.addContextMenuListener(listener);
        title.setContextMenuListener(listener);
    }
    
    private class MyLinkListener implements LinkListener {

        @Override
        public void linkClicked(String url) {
            UrlOpener.openUrlPrompt(ChannelInfoDialog.this, url);
        }
        
    }
    
}
