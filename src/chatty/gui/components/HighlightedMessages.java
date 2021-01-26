
package chatty.gui.components;

import chatty.Room;
import chatty.User;
import chatty.gui.Channels;
import chatty.gui.DockStyledTabContainer;
import chatty.gui.GuiUtil;
import chatty.gui.Highlighter.Match;
import chatty.gui.MainGui;
import chatty.gui.components.textpane.UserMessage;
import chatty.gui.StyleServer;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.textpane.ChannelTextPane;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.HighlightsContextMenu;
import chatty.gui.components.textpane.InfoMessage;
import chatty.gui.components.textpane.MyStyleConstants;
import chatty.util.MiscUtil;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.Emoticons.TagEmotes;
import chatty.util.api.StreamInfo;
import chatty.util.api.usericons.Usericon;
import chatty.util.colors.ColorCorrector;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockContentContainer;
import chatty.util.irc.MsgTags;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

/**
 * Window showing all highlighted (or ignored) messages.
 * 
 * @author tduva
 */
public class HighlightedMessages extends JDialog {
    
    public final static int DOCKED = 1 << 0;
    public final static int AUTO_OPEN = 1 << 1;
    
    private final TextPane messages;
    private String currentChannel;
    private int currentChannelMessageCount = 0;
    
    /**
     * This may not be the count that is actually displayed, if messages have
     * been cleared automatically from the buffer in the meantime.
     */
    private int displayedCount;
    private int newCount;
    private boolean setChatIconsYet = false;
    
    private final String title;
    private final String label;
    
    private final ContextMenuListener contextMenuListener;
    private final Channels channels;
    private final Settings settings;
    
    // Dock
    private final DockStyledTabContainer content;
    private boolean isDocked;
    
    // Settings
    private boolean autoOpen;
    private String settingName;
    
    
    /**
     * Creates a new dialog.
     * 
     * @param owner Reference to the MainGui, required for the text pane
     * @param styleServer The style server, style information for the text pane
     * @param title The title to display for the dialog
     * @param label What to show as description of the messges in the text pane
     * (when the channel name is output)
     * @param contextMenuListener
     */
    public HighlightedMessages(MainGui owner, StyleServer styleServer,
            String title, String shortTitle, String label, ContextMenuListener contextMenuListener,
            Channels channels, String settingName) {
        super(owner);
        this.title = title;
        this.label = label;
        this.channels = channels;
        this.contextMenuListener = contextMenuListener;
        this.settingName = settingName;
        this.settings = owner.getSettings();
        updateTitle();
        
        this.addComponentListener(new MyVisibleListener());
        
        /**
         * Modify a couple of things to remove highlight foreground/background.
         */
        StyleServer modifiedStyleServer = new StyleServer() {

            @Override
            public Color getColor(String type) {
                if (type.equals("highlight")) {
                    return styleServer.getColor("foreground");
                }
                else if (type.equals("highlightBackground")) {
                    return styleServer.getColor("background");
                }
                return styleServer.getColor(type);
            }

            @Override
            public MutableAttributeSet getStyle(String type) {
                if (type.equals("highlight")) {
                    return getStyle("standard");
                }
                if (type.equals("paragraph")) {
                    MutableAttributeSet attr = new SimpleAttributeSet(styleServer.getStyle(type));
                    MyStyleConstants.setHighlightBackground(attr, null);
                    return attr;
                }
                if (type.equals("settings")) {
                    MutableAttributeSet attr = new SimpleAttributeSet(styleServer.getStyle(type));
                    // For crossing out messages for timeouts, but never show separate message
                    attr.addAttribute(ChannelTextPane.Setting.SHOW_BANMESSAGES, false);
                    return attr;
                }
                return styleServer.getStyle(type);
            }

            @Override
            public Font getFont(String type) {
                return styleServer.getFont(type);
            }

            @Override
            public SimpleDateFormat getTimestampFormat() {
                return styleServer.getTimestampFormat();
            }

            @Override
            public ColorCorrector getColorCorrector() {
                return styleServer.getColorCorrector();
            }
        };
        ChannelTextPane.Type textPaneType = settingName.equals("highlightDock")
                                    ? ChannelTextPane.Type.HIGHLIGHTS
                                    : ChannelTextPane.Type.IGNORED;
        messages = new TextPane(owner, modifiedStyleServer, textPaneType,
                () -> new HighlightsContextMenu(isDocked, autoOpen));
        messages.setContextMenuListener(new MyContextMenuListener());
        //messages.setLineWrap(true);
        //messages.setWrapStyleWord(true);
        //messages.setEditable(false);
        
        JScrollPane scroll = new JScrollPane(messages);
        messages.setScrollPane(scroll);
        
        add(scroll);
        content = new DockStyledTabContainer(scroll, shortTitle, channels.getDock());
        
        setPreferredSize(new Dimension(400,300));
        
        pack();
        
        updateTabSettings(owner.getSettings().getLong("tabsMessage"));
        owner.getSettings().addSettingChangeListener((setting, type, value) -> {
            if (setting.equals("tabsMessage")) {
                SwingUtilities.invokeLater(() -> updateTabSettings((Long)value));
            }
        });
    }
    
    private void updateTabSettings(long value) {
        content.setSettings(0, (int)value, 0, 0, 0);
    }
        
    private void saveSettings() {
        int value = isDocked ? DOCKED : 0;
        value = value | (autoOpen ? AUTO_OPEN : 0);
        settings.setLong(settingName, value);
    }
    
    public void loadSettings() {
        int value = (int)settings.getLong(settingName);
        setDocked(MiscUtil.isBitEnabled(value, DOCKED));
        autoOpen = MiscUtil.isBitEnabled(value, AUTO_OPEN);
    }
    
    @Override
    public void setVisible(boolean visible) {
        setVisible(visible, true);
    }
    
    public void setVisible(boolean visible, boolean switchTo) {
        if (visible == isVisible()) {
            return;
        }
        if (isDocked) {
            if (visible) {
                channels.getDock().addContent(content);
                if (switchTo) {
                    channels.getDock().setActiveContent(content);
                }
            }
            else {
                channels.getDock().removeContent(content);
            }
        }
        else {
            if (!switchTo) {
                GuiUtil.setNonAutoFocus(this);
            }
            super.setVisible(visible);
        }
        if (visible) {
            newCount = 0;
        }
    }
    
    @Override
    public boolean isVisible() {
        if (isDocked) {
            return channels.getDock().hasContent(content);
        }
        else {
            return super.isVisible();
        }
    }
    
    public void addMessage(String channel, UserMessage message) {
        messageAdded(channel);
        messages.printMessage(message);
    }
    
    public void addMessage(String channel, User user, String text, boolean action,
            TagEmotes emotes, int bits, boolean whisper, List<Match> highlightMatches,
            Object highlightSource, MsgTags tags) {
        messageAdded(channel);
        UserMessage message = new UserMessage(user, text, emotes, null, bits, highlightMatches, null, null, tags);
        message.whisper = whisper;
        message.highlightSource = highlightSource;
        messages.printMessage(message);
    }
    
    public void addInfoMessage(String channel, InfoMessage message) {
        messageAdded(channel);
        messages.printInfoMessage(message);
    }
    
    public void addInfoMessage(String channel, String text, List<Match> highlightMatches, Object highlightSource) {
        messageAdded(channel);
        InfoMessage message = InfoMessage.createInfo(text);
        message.highlightMatches = highlightMatches;
        message.highlightSource = highlightSource;
        messages.printInfoMessage(message);
    }
    
    /**
     * This should be fine as long as the text pane only searches for lines
     * containing the same User object, so it doesn't affect messages with the
     * same username from another channel.
     */
    public void addBan(User user, long duration, String reason, String targetMsgId) {
        messages.userBanned(user, duration, reason, targetMsgId);
    }
    
    private void messageAdded(String channel) {
        if (currentChannel == null || !currentChannel.equals(channel)
                || currentChannelMessageCount > 12) {
            messages.printLine(MessageFormat.format(label, channel));
            currentChannel = channel;
            currentChannelMessageCount = 0;
        }
        currentChannelMessageCount++;
        displayedCount++;
        updateTitle();
        if (!isVisible()) {
            newCount++;
        }
        if (autoOpen) {
            setVisible(true, false);
        }
        if (isDocked && !content.isContentVisible()) {
            content.setNewMessage(true);
        }
    }
    
    private void updateTitle() {
        if (displayedCount > 0) {
            setTitle(title+" ("+displayedCount+")");
        } else {
            setTitle(title);
        }
    }
    
    public void refreshStyles() {
        messages.refreshStyles();
    }
    
    /**
     * Removes all text from the window.
     */
    public void clear() {
        messages.clear();
        currentChannel = null;
        currentChannelMessageCount = 0;
        displayedCount = 0;
        updateTitle();
    }
    
    /**
     * Get the count of all messages added after the last clear of the window.
     * 
     * @return 
     */
    public int getDisplayedCount() {
        return displayedCount;
    }
    
    /**
     * Get the count of all messages added while the window wasn't visible.
     * 
     * @return 
     */
    public int getNewCount() {
        return newCount;
    }
    
    private void setDocked(boolean docked) {
        if (isDocked != docked && isVisible()) {
            // Will make change visible as well, so only do if already visible
            toggleDock();
        }
        // Always update value, even if not currently visible
        isDocked = docked;
    }
    
    private void toggleDock() {
        if (isDocked) {
            channels.getDock().removeContent(content);
            add(content.getComponent());
            isDocked = false;
            super.setVisible(true);
        }
        else {
            remove(content.getComponent());
            channels.getDock().addContent(content);
            channels.getDock().setActiveContent(content);
            isDocked = true;
            super.setVisible(false);
        }
        saveSettings();
    }
    
    /**
     * Normal channel text pane modified a bit to fit the needs for this.
     */
    static class TextPane extends ChannelTextPane {
        
        public TextPane(MainGui main, StyleServer styleServer, ChannelTextPane.Type type, Supplier<ContextMenu> contextMenuCreator) {
            super(main, styleServer, type);
            linkController.setContextMenuCreator(contextMenuCreator);
        }
        
        public void clear() {
            setText("");
        }
        
    }
    
    private class MyContextMenuListener implements ContextMenuListener {
        
        @Override
        public void menuItemClicked(ActionEvent e) {
            switch (e.getActionCommand()) {
                case "clearHighlights":
                    clear();
                    break;
                case "toggleDock":
                    toggleDock();
                    break;
                case "toggleAutoOpen":
                    autoOpen = !autoOpen;
                    saveSettings();
                    break;
                default:
                    break;
            }
            contextMenuListener.menuItemClicked(e);
        }

        @Override
        public void userMenuItemClicked(ActionEvent e, User user, String msgId, String autoModMsgId) {
            contextMenuListener.userMenuItemClicked(e, user, msgId, autoModMsgId);
        }

        @Override
        public void urlMenuItemClicked(ActionEvent e, String url) {
            contextMenuListener.urlMenuItemClicked(e, url);
        }

        @Override
        public void streamsMenuItemClicked(ActionEvent e, Collection<String> streams) {
            contextMenuListener.streamsMenuItemClicked(e, streams);
        }

        @Override
        public void streamInfosMenuItemClicked(ActionEvent e, Collection<StreamInfo> streamInfos) {
            contextMenuListener.streamInfosMenuItemClicked(e, streamInfos);
        }

        @Override
        public void emoteMenuItemClicked(ActionEvent e, EmoticonImage emote) {
            contextMenuListener.emoteMenuItemClicked(e, emote);
        }

        @Override
        public void usericonMenuItemClicked(ActionEvent e, Usericon usericon) {
            contextMenuListener.usericonMenuItemClicked(e, usericon);
        }

        @Override
        public void roomsMenuItemClicked(ActionEvent e, Collection<Room> rooms) {
            contextMenuListener.roomsMenuItemClicked(e, rooms);
        }

        @Override
        public void channelMenuItemClicked(ActionEvent e, Channel channel) {
            contextMenuListener.channelMenuItemClicked(e, channel);
        }

        @Override
        public void textMenuItemClick(ActionEvent e, String selected) {
            contextMenuListener.textMenuItemClick(e, selected);
        }
    }
    
    /**
     * Checks if the window is being shown, so the new messages count can be
     * reset (which kind of indicates unread messages).
     */
    private class MyVisibleListener extends ComponentAdapter {
        
        @Override
        public void componentShown(ComponentEvent e) {
            newCount = 0;
        }
        
    }
    
}
