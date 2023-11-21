
package chatty.gui.components.routing;

import chatty.User;
import chatty.gui.Channels;
import chatty.gui.DockStyledTabContainer;
import chatty.gui.MainGui;
import chatty.gui.StyleManager;
import chatty.gui.StyleServer;
import chatty.gui.components.Channel;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.RoutingTargetContextMenu;
import chatty.gui.components.menus.TabContextMenu;
import chatty.gui.components.textpane.ChannelTextPane;
import chatty.gui.components.textpane.InfoMessage;
import chatty.gui.components.textpane.Message;
import chatty.util.Timestamp;
import chatty.util.colors.ColorCorrector;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

/**
 * The tab/text pane that messages get added to.
 * 
 * @author tduva
 */
public class RoutingTarget {
    
    //-------
    // Basic
    //-------
    private final String targetId;
    private final DockStyledTabContainer<JComponent> content;
    private int numMessages;
    
    //------------
    // References
    //------------
    private final MainGui main;
    private final StyleServer modifiedStyles;
    private final ContextMenuListener contextMenuListener;
    private final Channels channels;
    private final RoutingManager routingManager;
    
    //---------------
    // Multi channel
    //---------------
    private static final String EMPTY_CHANNEL_KEY = "--empty--";
    private static final String ALL_CHANNEL_KEY = "--all--";
    
    private final Map<String, TextPane> textPanes = new HashMap<>();
    
    /**
     * Which card currently has unread messages (channel name or
     * ALL_CHANNEL_KEY). Empty channels should not have unread messages.
     */
    private final Set<String> unread = new HashSet<>();
    
    private final JPanel base;
    private final CardLayout cardLayout;
    
    /**
     * An empty channel to show for channels with no message yet.
     */
    private TextPane emptyChannel;
    
    /**
     * The channel name (#channel) that is currently selected. This may not be
     * identical to which card is shown (could also be {@code emptyChannel) or
     * the all-in-one card).
     */
    private String currentChannel;
    
    /**
     * The key of the card that is currently shown.
     */
    private String currentKey;
    
    
    public RoutingTarget(String id, String title, MainGui main, StyleManager styles, Channels channels, ContextMenuListener contextMenuListener, RoutingManager routingManager) {
        this.targetId = id;
        this.routingManager = routingManager;
        this.main = main;
        this.contextMenuListener = contextMenuListener;
        this.channels = channels;
        modifiedStyles = new StyleServer() {

            @Override
            public Color getColor(String type) {
                return styles.getColor(type);
            }

            @Override
            public MutableAttributeSet getStyle(String type) {
                if (type.equals("settings")) {
                    MutableAttributeSet attr = new SimpleAttributeSet(styles.getStyle(type));
                    // For crossing out messages for timeouts, but never show separate message
                    attr.addAttribute(ChannelTextPane.Setting.SHOW_BANMESSAGES, false);
                    attr.addAttribute(ChannelTextPane.Setting.CHANNEL_LOGO_SIZE, 22);
                    return attr;
                }
                return styles.getStyle(type);
            }

            @Override
            public Font getFont(String type) {
                return styles.getFont(type);
            }

            @Override
            public Timestamp getTimestampFormat() {
                return styles.getTimestampFormat();
            }

            @Override
            public ColorCorrector getColorCorrector() {
                return styles.getColorCorrector();
            }
        };
        
        cardLayout = new CardLayout();
        base = new JPanel(cardLayout);
        
        String contentId = "'"+id+"'";
        this.content = new DockStyledTabContainer<JComponent>(base, contentId, channels.getDock()) {
            
            @Override
            public JPopupMenu getContextMenu() {
                return new TabContextMenu(contextMenuListener,
                        this,
                        Channels.getCloseTabs(channels, this, main.getSettings().getBoolean("closeTabsSameType")),
                        main.getSettings());
            }
            
        };
        this.content.setId(contentId);
        this.content.setTitle(title);
        createTextPane(ALL_CHANNEL_KEY);
        showChannel(null, ALL_CHANNEL_KEY);
    }
    
    //==========
    // Settings
    //==========
    public void settingsUpdated() {
        setChannel(currentChannel, true);
    }
    
    private int multiChannel() {
        return routingManager.getSettings(targetId).multiChannel;
    }
    
    private boolean showAll() {
        return routingManager.getSettings(targetId).showAll;
    }
    
    private boolean channelFixed() {
        return routingManager.getSettings(targetId).channelFixed;
    }
    
    //==========
    // Channels
    //==========
    public void setChannel(String channel, boolean force) {
        if (multiChannel() == 0 || showAll()) {
            showChannel(null, ALL_CHANNEL_KEY);
            if ((!channelFixed() || currentChannel == null) && channel != null) {
                currentChannel = channel;
            }
            return;
        }
        if (channelFixed() && currentChannel != null && !force) {
            return;
        }
        
        if (textPanes.containsKey(channel)) {
            showChannel(channel, channel);
        }
        else {
            if (emptyChannel == null) {
                emptyChannel = createTextPane(EMPTY_CHANNEL_KEY);
            }
            showChannel(channel, EMPTY_CHANNEL_KEY);
        }
    }
    
    private void showChannel(String channel, String key) {
        if (content.isContentVisible()) {
            unread.remove(key);
        }
        content.setNewMessage(unread.contains(key));
        
        cardLayout.show(base, key);
        if (channel != null) {
            content.setLongTitle(String.format("%s (%s)",
                    content.getTitle(), channel));
            this.currentChannel = channel;
        }
        else {
            content.setLongTitle(String.format("%s",
                    content.getTitle()));
        }
        this.currentKey = key;
    }
    
    private TextPane getTextPane(String channel) {
        TextPane textPane = textPanes.get(channel);
        if (textPane == null) {
            textPane = createTextPane(channel);
            textPanes.put(channel, textPane);
            if (Objects.equals(currentChannel, channel)) {
                showChannel(channel, channel);
            }
        }
        return textPane;
    }
    
    private TextPane createTextPane(String channel) {
        TextPane textPane = new TextPane(main, modifiedStyles, true);
        JScrollPane scroll = new JScrollPane(textPane);
        textPane.setScrollPane(scroll);
        textPane.setContextMenuListener(new ContextMenuAdapter(contextMenuListener) {
            
            @Override
            public void menuItemClicked(ActionEvent e) {
                if (e.getActionCommand().equals("clearAll")) {
                    clearAll();
                }
                else if (e.getActionCommand().equals("clearCurrent")) {
                    clearCurrent();
                }
                else if (e.getActionCommand().startsWith("dockChangeChannel.")) {
                    String chan = e.getActionCommand().substring("dockChangeChannel.".length());
                    if (chan.equals("all")) {
                        chan = ALL_CHANNEL_KEY;
                    }
                    setChannel(chan, true);
                }
                else if (e.getActionCommand().equals("dockChannelsShowAll")) {
                    RoutingTargetSettings settings = routingManager.getSettings(targetId);
                    boolean showAll = !settings.showAll;
                    // Updating settings will automatically change card if necessary
                    routingManager.updateSettings(targetId,
                            settings.setShowAll(showAll));
                }
                else if (e.getActionCommand().equals("dockToggleFixedChannel")) {
                    RoutingTargetSettings settings = routingManager.getSettings(targetId);
                    routingManager.updateSettings(targetId,
                            settings.setChannelFixed(!settings.channelFixed));
                }
                super.menuItemClicked(e);
            }
            
        });
        base.add(scroll, channel);
        return textPane;
    }
    
    public void messageAdded(String channelKey) {
        numMessages++;
        // When a message has been added, the TextPane would have been created
        // and EMPTY_CHANNEL_KEY would not be a factor anymore
        boolean currentChan = Objects.equals(currentKey, channelKey);
        if (!content.isContentVisible() || !currentChan) {
            unread.add(channelKey);
        }
        if (!content.isContentVisible() && currentChan) {
            content.setNewMessage(true);
        }
    }
    
    /**
     * The action function is called with one or several TextPane objects,
     * depending on current settings.
     *
     * @param channel
     * @param isMsg
     * @param action
     */
    private void performForChannel(String channel, boolean isMsg, Consumer<TextPane> action) {
        if (multiChannel() == 0 || multiChannel() == 2) {
            action.accept(getTextPane(ALL_CHANNEL_KEY));
            if (isMsg) {
                messageAdded(ALL_CHANNEL_KEY);
            }
        }
        if (multiChannel() > 0 && channel != null) {
            action.accept(getTextPane(channel));
            if (isMsg) {
                messageAdded(channel);
            }
        }
    }
    
    public void addMessage(String channel, Message message) {
        performForChannel(channel, true,
                t -> t.printMessage(message));
    }
    
    public void addInfoMessage(String channel, InfoMessage message) {
        performForChannel(channel, true,
                t -> t.printInfoMessage(message));
    }
    
    /**
     * This should be fine as long as the text pane only searches for lines
     * containing the same User object, so it doesn't affect messages with the
     * same username from another channel.
     *
     * @param user
     * @param duration
     * @param reason
     * @param targetMsgId
     */
    public void addBan(User user, long duration, String reason, String targetMsgId) {
        performForChannel(user.getChannel(), false,
                t -> t.userBanned(user, duration, reason, targetMsgId));
    }
    
    public void refreshStyles() {
        for (TextPane textPane : textPanes.values()) {
            textPane.refreshStyles();
        }
    }
    
    public void clearAll() {
        numMessages = 0;
        for (Map.Entry<String,TextPane> entry : textPanes.entrySet()) {
            entry.getValue().clearAll();
            unread.remove(entry.getKey());
        }
    }
    
    public void clearCurrent() {
        TextPane textPane = textPanes.get(currentKey);
        if (textPane != null) {
            textPane.clearAll();
            unread.remove(currentKey);
        }
    }
    
    public DockStyledTabContainer<JComponent> getContent() {
        return content;
    }
    
    public int getNumMessages() {
        return numMessages;
    }

    protected void setName(String name) {
        content.setTitle(name);
    }
    
    protected void scroll(String action) {
        TextPane textPane = textPanes.get(currentKey);
        if (textPane != null) {
            Channel.scroll(textPane.getScrollPane().getVerticalScrollBar(), action);
        }
    }
    
    class TextPane extends ChannelTextPane {
        
        private JScrollPane scrollPane;
        
        public TextPane(MainGui main, StyleServer styleServer, boolean startAtBottom) {
            super(main, styleServer, ChannelTextPane.Type.REGULAR, startAtBottom);
            
            // Overriding constructor is required to set the custom context menu
            linkController.setContextMenuCreator(() -> new RoutingTargetContextMenu(
                    multiChannel() > 0 ? channels.getChannelsOfType(Channel.Type.CHANNEL) : null,
                    routingManager.getSettings(targetId).channelFixed,
                    multiChannel() == 2,
                    routingManager.getSettings(targetId).showAll,
                    currentChannel));
        }
        
        @Override
        public void setScrollPane(JScrollPane scrollPane) {
            super.setScrollPane(scrollPane);
            this.scrollPane = scrollPane;
        }
        
        public JScrollPane getScrollPane() {
            return scrollPane;
        }
        
    }
    
}
