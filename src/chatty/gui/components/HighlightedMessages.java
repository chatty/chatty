
package chatty.gui.components;

import chatty.Room;
import chatty.User;
import chatty.gui.Channels;
import chatty.gui.DockStyledTabContainer;
import chatty.gui.DockedDialogHelper;
import chatty.gui.DockedDialogManager;
import chatty.gui.GuiUtil;
import chatty.gui.Highlighter.Match;
import chatty.gui.MainGui;
import chatty.gui.components.textpane.UserMessage;
import chatty.gui.StyleServer;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.textpane.ChannelTextPane;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.HighlightsContextMenu;
import chatty.gui.components.textpane.InfoMessage;
import chatty.gui.components.textpane.MyStyleConstants;
import chatty.util.Debugging;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
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
    
    private final TextPane messages;
    private final DockedDialogHelper helper;
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
    
    // Dock
    private final DockStyledTabContainer content;
    
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
            DockedDialogManager dockedDialogs, String settingName) {
        super(owner);
        this.title = title;
        this.label = label;
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
                () -> new HighlightsContextMenu(isDocked(), autoOpenActivity()));
        messages.setContextMenuListener(new ContextMenuAdapter(contextMenuListener) {
            
            @Override
            public void menuItemClicked(ActionEvent e) {
                switch (e.getActionCommand()) {
                    case "clearHighlights":
                        clear();
                        break;
                    default:
                        break;
                }
                helper.menuAction(e);
                super.menuItemClicked(e);
            }
            
        });
        //messages.setLineWrap(true);
        //messages.setWrapStyleWord(true);
        //messages.setEditable(false);
        
        JScrollPane scroll = new JScrollPane(messages);
        messages.setScrollPane(scroll);
        
        add(scroll);
        content = dockedDialogs.createStyledContent(scroll, shortTitle,
                settingName.equals("highlightDock") ? "-highlight-" : "-ignore-");
        
        helper = dockedDialogs.createHelper(new DockedDialogHelper.DockedDialog() {
            
            @Override
            public void setVisible(boolean visible) {
                HighlightedMessages.super.setVisible(visible);
            }

            @Override
            public boolean isVisible() {
                return HighlightedMessages.super.isVisible();
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
                return HighlightedMessages.this;
            }

            @Override
            public DockContent getContent() {
                return content;
            }
            
        });
        
        setPreferredSize(new Dimension(400,300));
        
        pack();
    }
    
    private boolean isDocked() {
        return helper.isDocked();
    }
    
    private boolean autoOpenActivity() {
        return helper.autoOpenActivity();
    }
    
    @Override
    public void setVisible(boolean visible) {
        setVisible(visible, true);
    }
    
    public void setVisible(boolean visible, boolean switchTo) {
        helper.setVisible(visible, switchTo);
        if (visible) {
            newCount = 0;
        }
    }
    
    @Override
    public boolean isVisible() {
        return helper.isVisible();
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
        helper.setActivity();
        if (helper.isDocked() && !content.isContentVisible()) {
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
