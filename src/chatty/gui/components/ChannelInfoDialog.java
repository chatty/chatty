
package chatty.gui.components;

import chatty.gui.LinkListener;
import chatty.gui.UrlOpener;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.DateTime;
import static chatty.util.DateTime.H;
import static chatty.util.DateTime.S;
import chatty.util.api.CommunitiesManager.Community;
import chatty.util.api.StreamInfo;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

/**
 *
 * @author tduva
 */
public class ChannelInfoDialog extends JDialog implements ViewerHistoryListener {

    private static final String STATUS_LABEL_TEXT = "Status:";
    private static final String STATUS_LABEL_TEXT_HISTORY = "Status (History):";
    
    private final JLabel statusLabel = new JLabel("Status:");
    private final ExtendedTextPane title = new ExtendedTextPane();
    
    private final JLabel onlineSince = new JLabel();
    private boolean showWithPicnic;
    
    private final JLabel gameLabel = new JLabel("Playing:");
    private final JTextField game = new JTextField();
    
    private final LinkLabel communityLabel;
    
    private final JLabel historyLabel = new JLabel("Viewers:");
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
        setTitle("Channel Info");
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        // Status
        gbc = makeGbc(0,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        add(statusLabel,gbc);
        
        gbc = makeGbc(1,0,1,1);
        gbc.anchor = GridBagConstraints.EAST;
        onlineSince.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                showWithPicnic = !showWithPicnic;
                updateOnlineTime();
            }
        });
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
        
        gbc = makeGbc(1, 2, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        communityLabel = new LinkLabel("", new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                String url = "https://www.twitch.tv/communities/"+ref;
                UrlOpener.openUrlPrompt(ChannelInfoDialog.this, url);
            }
        });
        communityLabel.setMargin(game.getMargin());
        add(communityLabel, gbc);
 
        // Graph
        gbc = makeGbc(0,4,1,1);
        gbc.insets = new Insets(3,5,3,3);
        gbc.anchor = GridBagConstraints.WEST;
        add(historyLabel,gbc);
        
        history.setListener(this);
        history.setForegroundColor(gameLabel.getForeground());
        history.setBackgroundColor(gameLabel.getBackground());
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
                updateOnlineTime();
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
        String communityText = "";
        if (streamInfo.isValid() && streamInfo.getOnline()) {
            statusText = streamInfo.getTitle();
            gameText = streamInfo.getGame();
            if (gameText.isEmpty()) {
                gameText = "No game";
            }
            timeStarted = streamInfo.getTimeStarted();
            onlineSince.setText(null);
            onlineSince.setToolTipText("Stream started: "+DateTime.formatFullDatetime(timeStarted));
            Community community = streamInfo.getCommunity();
            if (community != null) {
                communityText = String.format("[community:%s %s]",
                        community.getName(),
                        community.getName());
            }
        }
        else if (streamInfo.isValid()) {
            statusText = "Stream offline";
            gameText = "";
            timeStarted = -1;
            onlineSince.setText("Offline");
            if (streamInfo.getLastOnlineTime() != -1) {
                String lastBroadcastTime = formatTime(streamInfo.getTimeStarted(), streamInfo.getLastOnlineTime());
                if (streamInfo.getTimeStarted() != streamInfo.getTimeStartedWithPicnic()) {
                    String withPicnic = formatTime(streamInfo.getTimeStartedWithPicnic(), streamInfo.getLastOnlineTime());
                    onlineSince.setToolTipText("Last broadcast length (probably approx.): " + lastBroadcastTime+" (With PICNIC: "+withPicnic+")");
                } else {
                    onlineSince.setToolTipText("Last broadcast length (probably approx.): " + lastBroadcastTime);
                }
            } else {
                onlineSince.setToolTipText(null);
            }
        }
        else {
            statusText = "[No Stream Information]";
            gameText = "";
            timeStarted = -1;
            onlineSince.setText(null);
            onlineSince.setToolTipText(null);
        }
        title.setText(statusText);
        game.setText(gameText);
        communityLabel.setText(communityText);
        history.setHistory(streamInfo.getStream(), streamInfo.getHistory());
        currentStreamInfo = streamInfo;
        updateOnlineTime();
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
    
    private void updateOnlineTime() {
        if (timeStarted != -1) {
            long withPicnic = currentStreamInfo.getTimeStartedWithPicnic();
            if (withPicnic != timeStarted) {
                onlineSince.setText("Online: " + formatTime(timeStarted) + " (" + formatTime(withPicnic)+")");
                onlineSince.setToolTipText("Stream started: "+DateTime.formatFullDatetime(timeStarted)
                        +" (With PICNIC: "
                        +DateTime.formatFullDatetime(withPicnic)+")");
            } else {
                onlineSince.setText("Online: " + formatTime(timeStarted));
            }
        }
    }
    
    private static String formatTime(long time) {
        return formatDuration(System.currentTimeMillis() - time);
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
    public void itemSelected(int viewers, String historyTitle, String historyGame) {
        title.setText(historyTitle);
        game.setText(historyGame);
        statusLabel.setText(STATUS_LABEL_TEXT_HISTORY);
    }
    
    @Override
    public void noItemSelected() {
        this.title.setText(statusText);
        this.game.setText(gameText);
        statusLabel.setText(STATUS_LABEL_TEXT);
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
