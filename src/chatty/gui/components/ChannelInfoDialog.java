
package chatty.gui.components;

import chatty.Chatty;
import chatty.gui.DockedDialogHelper;
import chatty.gui.DockedDialogManager;
import chatty.gui.GuiUtil;
import chatty.gui.LinkListener;
import chatty.gui.UrlOpener;
import chatty.gui.components.admin.StatusHistoryEntry;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.lang.Language;
import chatty.util.DateTime;
import static chatty.util.DateTime.H;
import static chatty.util.DateTime.S;
import chatty.util.MiscUtil;
import chatty.util.StringUtil;
import chatty.util.api.StreamTagManager.StreamTag;
import chatty.util.api.StreamInfo;
import chatty.util.api.StreamInfo.StreamType;
import chatty.util.api.StreamInfoHistoryItem;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockContentContainer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
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

    private static final String GAME_LABEL_TEXT = Language.getString("channelInfo.playing")+":";
    private static final String GAME_LABEL_TEXT_VOD = "VOD / "+Language.getString("channelInfo.playing")+":";
    
    private final JLabel statusLabel = new JLabel() {
        
        @Override
        public Dimension getMinimumSize() {
            return new Dimension(1, super.getMinimumSize().height);
        }
        
    };
    private final ExtendedTextPane title = new ExtendedTextPane();
    
    private final JLabel onlineSince = new JLabel();
    private boolean historyItemSelected;
    
    private final JLabel gameLabel = new JLabel(GAME_LABEL_TEXT);
    private final JTextField game = new JTextField();
    
//    private final LinkLabel communityLabel;
    private final LinkLabel testLabel = new LinkLabel(null, null);
    private List<StreamTag> communities;
    
    private final JLabel historyLabel = new JLabel(Language.getString("channelInfo.viewers")+":");
    private final ViewerHistory history = new ViewerHistory();
    
    private StreamInfo currentStreamInfo;
    
    private String statusText = "";
    private String gameText = "";
    
    private final DockedDialogHelper helper;
    
    /**
     * The time the stream of the current StreamInfo started. This is -1 if the
     * stream is offline or the StreamInfo is invalid, or no time was received.
     */
    private long timeStarted = -1;
    
    public ChannelInfoDialog(Frame owner, DockedDialogManager dockedDialogs) {
        super(owner);
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        // Status
        gbc = makeGbc(0,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        mainPanel.add(statusLabel,gbc);
        
        gbc = makeGbc(1,0,1,1);
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(onlineSince, gbc);
        
        title.setEditable(false);
        title.setLinkListener(new MyLinkListener());
        JScrollPane scroll = new JScrollPane(title);
        scroll.setPreferredSize(new Dimension(300,70));
        scroll.setMinimumSize(new Dimension(1,45));
        
        gbc = makeGbc(0,1,2,1);
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(scroll,gbc);

        // Game
        gbc = makeGbc(0,2,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(gameLabel,gbc);
        
        game.setEditable(false);
        gbc = makeGbc(0,3,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(game,gbc);
        
        gbc = makeGbc(0, 2, 2, 1);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.9;
//        communityLabel = new LinkLabel(" ", new LinkLabelListener() {
//
//            @Override
//            public void linkClicked(String type, String ref) {
//                if (type.equals("overflow")) {
//                    int overflow = Integer.valueOf(ref);
//                    JPopupMenu menu = new JPopupMenu();
//                    for (int i=communities.size()-overflow;i<communities.size();i++) {
//                        StreamTag c = communities.get(i);
//                        Action a = new AbstractAction(c.toString()) {
//                            
//                            @Override
//                            public void actionPerformed(ActionEvent e) {
//                                String url = "https://www.twitch.tv/communities/"+c.getName();
//                                UrlOpener.openUrlPrompt(ChannelInfoDialog.this, url);
//                            }
//                        };
//                        menu.add(new JMenuItem(a));
//                    }
//                    menu.addSeparator();
//                    menu.add(new JMenuItem(new AbstractAction(Language.getString("channelInfo.cm.copyAllCommunities")) {
//                        
//                        @Override
//                        public void actionPerformed(ActionEvent e) {
//                            MiscUtil.copyToClipboard(StringUtil.join(communities, ", "));
//                        }
//                    }));
//                    menu.show(communityLabel, communityLabel.getWidth(), communityLabel.getHeight());
//                } else {
//                    String url = "https://www.twitch.tv/communities/"+ref;
//                    UrlOpener.openUrlPrompt(ChannelInfoDialog.this, url);
//                }
//            }
//        });
//        communityLabel.setMargin(game.getMargin());
        
        // Size listener to the dialog, since that is most relevant (other
        // changes update anyway)
//        addComponentListener(new ComponentAdapter() {
//
//            @Override
//            public void componentResized(ComponentEvent e) {
//                updateCommunities();
//            }
//        });
//        add(communityLabel, gbc);
 
        // Graph
        gbc = makeGbc(0,4,1,1);
        gbc.insets = new Insets(3,5,3,3);
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(historyLabel,gbc);
        
        history.setListener(this);
        history.setForegroundColor(game.getForeground());
        history.setBaseFont(game.getFont());
        history.setPreferredSize(new Dimension(300,150));
        history.setMinimumSize(new Dimension(1,20));
        gbc = makeGbc(0,5,2,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4,4,4,4);
        gbc.weightx = 1;
        gbc.weighty = 1;
        mainPanel.add(history,gbc);
        
        add(mainPanel);
        
        if (dockedDialogs != null) {
            DockContent content = dockedDialogs.createContent(mainPanel, "Channel Info", "-channelInfo-");
            helper = dockedDialogs.createHelper(new DockedDialogHelper.DockedDialog() {
                @Override
                public void setVisible(boolean visible) {
                    ChannelInfoDialog.super.setVisible(visible);
                }

                @Override
                public boolean isVisible() {
                    return ChannelInfoDialog.super.isVisible();
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
                    return ChannelInfoDialog.this;
                }

                @Override
                public DockContent getContent() {
                    return content;
                }
                
                @Override
                public void dockedChanged() {
                    updateDocked();
                }
            });
            helper.installContextMenu(mainPanel);
        }
        else {
            helper = null;
        }
        
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
        
        GuiUtil.installEscapeCloseOperation(this);
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (helper != null) {
            helper.setVisible(visible, true);
        }
        else {
            super.setVisible(visible);
        }
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
            // TODO: Separate way to enable/disable extra title, because this could also be useful for dialog/window title
//            helper.getContent().setLongTitle(title);
        }
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
        this.setTitle(Language.getString("channelInfo.title", name)
                +(streamInfo.getFollowed() ? " ("+Language.getString("channelInfo.title.followed")+")" : ""));
        if (streamInfo.isValid() && streamInfo.getOnline()) {
            statusText = streamInfo.getTitle();
            gameText = streamInfo.getGame();
            if (gameText.isEmpty()) {
                gameText = "No game";
            }
            timeStarted = streamInfo.getTimeStarted();
            onlineSince.setText(null);
            setCommunities(streamInfo.getCommunities());
            updateStreamType(streamInfo.getStreamType());
        }
        else if (streamInfo.isValid()) {
            statusText = Language.getString("channelInfo.streamOffline");
            gameText = "";
            timeStarted = -1;
            setCommunities(null);
        }
        else {
            statusText = Language.getString("channelInfo.noInfo");
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
        updateStatusLabel();
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
    
    private void setCommunities(List<StreamTag> c) {
        if (c != null && c.contains(null)) {
            // This usually shouldn't contain null elements, but just in case
            c = new ArrayList<>(c);
            c.removeIf(e -> { return e == null; });
        }
        communities = c;
    }
    
    private void updateCommunities() {
//        // Wrapped in invokeLater so that the size of the gameLabel is updated
//        SwingUtilities.invokeLater(() -> {
//            if (communities == null || communities.isEmpty()) {
//                communityLabel.setText("");
//            } else {
//                // -80 to leave some distance between "Playing" and this
//                int availableWidth = getWidth() - gameLabel.getWidth() - 80;
//                for (int i = 0; i <= communities.size(); i++) {
//                    String result = makeCommunitiesText(communities, i);
//                    // Use a separate label, as to not update the acual one
//                    // until it fits
//                    testLabel.setText(result);
//                    if (availableWidth > testLabel.getPreferredSize().width) {
//                        // Found content that fits, so go set it and stop
//                        communityLabel.setText(result);
//                        break;
//                    }
//                }
//            }
//        });
    }
    
    /**
     * Creates the communities label text with the given data.
     * 
     * @param communities The data
     * @param overflow How many communities should be in the overflow menu
     * @return The resulting String
     */
//    private String makeCommunitiesText(List<StreamTag> communities, int overflow) {
//        StringBuilder b = new StringBuilder("<div style='text-align:right'>");
//        for (int i = 0; i < communities.size() - overflow; i++) {
//            StreamTag c = communities.get(i);
//            if (i > 0) {
//                b.append(", ");
//            }
//            b.append("[community:").append(c.getName()).append(" ");
//            b.append(c.getDisplayName()).append("]");
//        }
//        if (overflow > 0) {
//            b.append(" [overflow:").append(overflow).append(" (+").append(overflow).append(")]");
//        }
//        b.append("</div>");
//        return b.toString();
//    }
    
    private void updateStreamType(StreamType streamType) {
        if (streamType != StreamType.LIVE) {
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
            onlineSince.setText(Language.getString("channelInfo.offline"));
            if (info.getLastOnlineTime() != -1) {
                String lastBroadcastTime = formatTime(info.getTimeStarted(), info.getLastOnlineTime());
                if (info.getTimeStarted() != info.getTimeStartedWithPicnic()) {
                    String withPicnic = formatTime(info.getTimeStartedWithPicnic(), info.getLastOnlineTime());
                    onlineSince.setToolTipText(Language.getString("channelInfo.offline.tip.picnic",
                            lastBroadcastTime, withPicnic));
                } else {
                    onlineSince.setToolTipText(Language.getString("channelInfo.offline.tip",
                            lastBroadcastTime));
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
                onlineSince.setText(Language.getString("channelInfo.uptime.picnic",
                        formatTime(started, current), formatTime(withPicnic, current)));
                onlineSince.setToolTipText(Language.getString("channelInfo.uptime.tip.picnic",
                        DateTime.formatFullDatetime(started), DateTime.formatFullDatetime(withPicnic)));
            } else {
                onlineSince.setText(Language.getString("channelInfo.uptime",
                        formatTime(started, current)));
                onlineSince.setToolTipText(Language.getString("channelInfo.uptime.tip",
                        DateTime.formatFullDatetime(timeStarted)));
            }
        }
    }
    
    private void updateStatusLabel() {
        if (helper != null && helper.isDocked() && currentStreamInfo != null) {
            if (historyItemSelected) {
                statusLabel.setText(String.format("%s (%s):",
                        Language.getString("channelInfo.history"),
                        currentStreamInfo.stream));
            }
            else {
                statusLabel.setText(String.format("%s (%s):",
                        Language.getString("channelInfo.status"),
                        currentStreamInfo.stream));
            }
        }
        else {
            if (historyItemSelected) {
                statusLabel.setText(String.format("%s:",
                        Language.getString("channelInfo.history")));
            }
            else {
                statusLabel.setText(String.format("%s:",
                        Language.getString("channelInfo.status")));
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
        updateStatusLabel();
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
        updateStatusLabel();
        updateStreamType(currentStreamInfo.getStreamType());
        updateOnlineTime(currentStreamInfo);
        setCommunities(currentStreamInfo.getCommunities());
        updateCommunities();
    }
    
    public void setHistoryRange(int minutes) {
        history.setRange(minutes);
    }
    
    public void setHistoryVerticalZoom(boolean verticalZoom) {
        history.setVerticalZoom(verticalZoom);
    }
    
    public void updateDocked() {
        history.setDocked(helper.isDocked());
        updateStatusLabel();
    }
    
    public void addContextMenuListener(ContextMenuListener listener) {
        history.addContextMenuListener(new ContextMenuAdapter(listener) {
            
            @Override
            public void menuItemClicked(ActionEvent e) {
                super.menuItemClicked(e);
                helper.menuAction(e);
            }
            
        });
        title.setContextMenuListener(listener);
    }
    
    private class MyLinkListener implements LinkListener {

        @Override
        public void linkClicked(String url) {
            UrlOpener.openUrlPrompt(ChannelInfoDialog.this, url);
        }
        
    }
    
}
