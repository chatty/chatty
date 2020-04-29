
package chatty.gui.components;

import chatty.Room;
import chatty.User;
import chatty.gui.Highlighter.Match;
import chatty.gui.MainGui;
import chatty.gui.components.textpane.UserMessage;
import chatty.gui.StyleServer;
import chatty.gui.components.textpane.ChannelTextPane;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.HighlightsContextMenu;
import chatty.gui.components.textpane.InfoMessage;
import chatty.gui.components.textpane.MyStyleConstants;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.Emoticons.TagEmotes;
import chatty.util.api.StreamInfo;
import chatty.util.api.usericons.Usericon;
import chatty.util.colors.ColorCorrector;
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
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

/**
 * Window showing all highlighted (or ignored) messages.
 * 
 * @author tduva
 */
public class HighlightedMessages extends JDialog {
    
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
            String title, String label, ContextMenuListener contextMenuListener) {
        super(owner);
        this.title = title;
        this.label = label;
        this.contextMenuListener = contextMenuListener;
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
        messages = new TextPane(owner, modifiedStyleServer);
        messages.setContextMenuListener(new MyContextMenuListener());
        //messages.setLineWrap(true);
        //messages.setWrapStyleWord(true);
        //messages.setEditable(false);
        
        JScrollPane scroll = new JScrollPane(messages);
        messages.setScrollPane(scroll);
        
        add(scroll);
        
        setPreferredSize(new Dimension(400,300));
        
        pack();
    }
    
    public void addMessage(String channel, UserMessage message) {
        messageAdded(channel);
        messages.printMessage(message);
    }
    
    public void addMessage(String channel, User user, String text, boolean action,
            TagEmotes emotes, int bits, boolean whisper, List<Match> highlightMatches) {
        messageAdded(channel);
        UserMessage message = new UserMessage(user, text, emotes, null, bits, highlightMatches, null, null);
        message.whisper = whisper;
        messages.printMessage(message);
    }
    
    public void addInfoMessage(String channel, InfoMessage message) {
        messageAdded(channel);
        messages.printInfoMessage(message);
    }
    
    public void addInfoMessage(String channel, String text) {
        messageAdded(channel);
        messages.printLine(text);
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
        
        public TextPane(MainGui main, StyleServer styleServer) {
            super(main, styleServer);
            linkController.setContextMenuCreator(() -> new HighlightsContextMenu());
        }
        
        public void clear() {
            setText("");
        }
        
    }
    
    private class MyContextMenuListener implements ContextMenuListener {
        
        @Override
        public void menuItemClicked(ActionEvent e) {
            if (e.getActionCommand().equals("clearHighlights")) {
                clear();
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
