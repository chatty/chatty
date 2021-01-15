
package chatty.gui.components;

import chatty.Room;
import chatty.gui.MouseClickedListener;
import chatty.gui.StyleManager;
import chatty.gui.StyleServer;
import chatty.gui.MainGui;
import chatty.User;
import chatty.gui.Channels.DockChannelContainer;
import chatty.gui.GuiUtil;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.TextSelectionMenu;
import chatty.gui.components.textpane.ChannelTextPane;
import chatty.gui.components.textpane.InfoMessage;
import chatty.gui.components.textpane.Message;
import chatty.util.StringUtil;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A single channel window, combining styled text pane, userlist and input box.
 * 
 * @author tduva
 */
public final class Channel extends JPanel {
    
    public enum Type {
        NONE, CHANNEL, WHISPER, SPECIAL
    }
    
    private static final int DIVIDER_SIZE = 5;
    
    private final ChannelEditBox input;
    private final ChannelTextPane text;
    private final UserList users;
    private final JSplitPane mainPane;
    private final JScrollPane userlist;
    private final JScrollPane west;
    private final StyleServer styleManager;
    private final MainGui main;
    private Type type;
    
    private DockChannelContainer content;
    
    private boolean userlistEnabled = true;
    private int previousUserlistWidth;
    private int userlistMinWidth;

    private Room room;

    public Channel(final Room room, Type type, MainGui main, StyleManager styleManager,
            ContextMenuListener contextMenuListener) {
        this.setLayout(new BorderLayout());
        this.styleManager = styleManager;
        this.main = main;
        this.type = type;
        this.room = room;
        setName(room.getDisplayName());
        
        // Text Pane
        text = new ChannelTextPane(main,styleManager);
        text.setContextMenuListener(contextMenuListener);
        
        setTextPreferredSizeTemporarily();
        
        west = new JScrollPane(text);
        text.setScrollPane(west);
        //System.out.println(west.getVerticalScrollBarPolicy());
        //System.out.println(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        west.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // PageUp/Down hotkeys / Scrolling
        InputMap westScrollInputMap = west.getInputMap(WHEN_IN_FOCUSED_WINDOW);
        westScrollInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "pageUp");
        west.getActionMap().put("pageUp", new ScrollAction("pageUp", west.getVerticalScrollBar()));
        westScrollInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "pageDown");
        west.getActionMap().put("pageDown", new ScrollAction("pageDown", west.getVerticalScrollBar()));
        west.getVerticalScrollBar().setUnitIncrement(40);

        
        // User list
        users = new UserList(contextMenuListener, main.getUserListener(), main.getSettings());
        updateUserlistSettings();
        userlist = new JScrollPane(users);
        
        
        // Split Pane
        mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,west,userlist);
        mainPane.setResizeWeight(1);
        mainPane.setDividerSize(DIVIDER_SIZE);
        
        // Text input
        input = new ChannelEditBox(40);
        input.addActionListener(main.getActionListener());
        input.setCompletionServer(new ChannelCompletion(this, main, input, users));
        input.setCompletionEnabled(main.getSettings().getBoolean("completionEnabled"));
        // Remove PAGEUP/DOWN so it can scroll chat (as before JTextArea)
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "-");
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "-");
        GuiUtil.installLengthLimitDocumentFilter(input, 500, false);
        TextSelectionMenu.install(input);

        // Add components
        add(mainPane, BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);
    }
    
    public DockChannelContainer getDockContent() {
        return content;
    }
    
    public void setDockContent(DockChannelContainer content) {
        this.content = content;
        updateContentData();
    }
    
    private void updateContentData() {
        if (content != null) {
            content.setTitle(getName());
        }
    }
    
    public void init() {
        text.setChannel(this);
        
        input.requestFocusInWindow();
        setStyles();
    }
    
    public boolean setRoom(Room room) {
        if (room != null && this.room != room) {
            this.room = room;
            refreshBufferSize();
            setName(room.getDisplayName());
            updateContentData();
            return true;
        }
        return false;
    }
    
    public Room getRoom() {
        return room;
    }
    
    public void cleanUp() {
        text.cleanUp();
        input.cleanUp();
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setScrollbarAlways(boolean always) {
        west.setVerticalScrollBarPolicy(always ? 
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    }
    
    public void setMouseClickedListener(MouseClickedListener listener) {
        text.setMouseClickedListener(listener);
    }
    
    public String getChannel() {
        return room.getChannel();
    }
    
    @Override
    public String getToolTipText() {
        if (room.getStreamId() != null) {
            return room.getChannel()+" ("+room.getStreamId()+")";
        }
        return room.getChannel();
    }
    
    public String getFilename() {
        return room.getFilename();
    }
    
    @Override
    public String getName() {
        return room != null ? room.getDisplayName() : null;
    }
    
    public String getOwnerChannel() {
        return room.getOwnerChannel();
    }
    
    /**
     * Gets the name of the stream (without leading #) if it is a stream channel
     * (and thus has a leading #) ;)
     * 
     * @return The stream name, may return null
     */
    public String getStreamName() {
        return room.getStream();
    }
    
    public void addUser(User user) {
        users.addUser(user);
    }
    
    public void removeUser(User user) {
        users.removeUser(user);
    }
    
    public void updateUser(User user) {
        users.updateUser(user);
    }
    
    public void resortUserlist() {
        users.resort();
    }
    
    public void clearUsers() {
        users.clearUsers();
    }
    
    public int getNumUsers() {
        return users.getNumUsers();
    }
    
    public final void updateUserlistSettings() {
        users.setDisplayNamesMode(main.getSettings().getLong("displayNamesModeUserlist"));
    }

    public ChannelEditBox getInput() {
        return input;
    }
    
    public String getInputText() {
        return input.getText();
    }

    @Override
    public boolean requestFocusInWindow() {
        // Invoke later, because otherwise it wouldn't get focus for some
        // reason.
        SwingUtilities.invokeLater(() -> {
            input.requestFocusInWindow();
        });
        return input.requestFocusInWindow();
    }
    
    
    // Messages
    
    public boolean search(String searchText) {
        return text.search(searchText);
    }
    
    public void resetSearch() {
        text.resetSearch();
    }
    
    public void printLine(String line) {
        text.printLine(line);
    }
    
    public void printInfoMessage(InfoMessage message) {
        text.printInfoMessage(message);
    }
    
    public void userBanned(User user, long duration, String reason, String id) {
        text.userBanned(user, duration, reason, id);
    }
    
    public void printCompact(String type, User user) {
        text.printCompact(type, user);
    }
    
    public void printMessage(Message message) {
        text.printMessage(message);
    }
    
    
    // Style
    
    public void refreshStyles() {
        text.refreshStyles();
        setStyles();
    }
    
    private void setStyles() {
        input.setFont(styleManager.getFont("input"));
        input.setBackground(styleManager.getColor("inputBackground"));
        input.setCaretColor(styleManager.getColor("inputForeground"));
        input.setForeground(styleManager.getColor("inputForeground"));
        input.setHistoryRequireCtrlMultirow(main.getSettings().getBoolean("inputHistoryMultirowRequireCtrl"));
        users.setFont(styleManager.getFont("userlist"));
        users.setBackground(styleManager.getColor("background"));
        users.setForeground(styleManager.getColor("foreground"));
        refreshBufferSize();
    }
    
    private void refreshBufferSize() {
        Long bufferSize = (Long)main.getSettings().mapGet("bufferSizes", StringUtil.toLowerCase(getChannel()));
        text.setBufferSize(bufferSize != null ? bufferSize.intValue() : -1);
    }
    
    public void clearChat() {
        text.clearAll();
    }
    
    /**
     * Insert text into the input box at the current caret position.
     * 
     * @param text
     * @param withSpace 
     * @throws NullPointerException if the text is null
     */
    public void insertText(String text, boolean withSpace) {
        input.insertAtCaret(text, withSpace);
    }
    
    private static class ScrollAction extends AbstractAction {
        
        private final String action;
        private final JScrollBar scrollbar;
        
        ScrollAction(String action, JScrollBar scrollbar) {
            this.scrollbar = scrollbar;
            this.action = action;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            int now = scrollbar.getValue();
            int height = scrollbar.getVisibleAmount();
            height = height - height / 10;
            int newValue = 0;
            switch (action) {
                case "pageUp": newValue = now - height; break;
                case "pageDown": newValue = now + height; break;
            }
            scrollbar.setValue(newValue);
        }
    }
    
    public final void setUserlistWidth(int width, int minWidth) {
        userlist.setPreferredSize(new Dimension(width, 10));
        userlist.setMinimumSize(new Dimension(minWidth, 0));
        userlistMinWidth = minWidth;
    }
    
    /**
     * Setting the preferred size to 0, so the text pane doesn't influence the
     * size of the userlist. Setting it back later so it doesn't flicker when
     * being scrolled up (and possibly other issues). This is an ugly hack, but
     * I don't know enough about this to find a proper solution.
     */
    private void setTextPreferredSizeTemporarily() {
        text.setPreferredSize(new Dimension(0, 0));
        Timer t = new Timer(5000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                text.setPreferredSize(null);
            }
        });
        t.setRepeats(false);
        t.start();
    }
    
    /**
     * Return 0 size, so resizing in a split pane works.
     * 
     * @return 
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
    }
    
    /**
     * Toggle visibility for the text input box.
     */
    public final void toggleInput() {
        input.setVisible(!input.isVisible());
        revalidate();
    }
    
    /**
     * Enable or disable the userlist. As with setting the initial size, this
     * requires some hacky stuff to get the size back correctly.
     * 
     * @param enable 
     */
    public final void setUserlistEnabled(boolean enable) {
        if (enable == userlistEnabled) {
            return;
        }
        if (enable) {
            userlist.setVisible(true);
            mainPane.setDividerSize(DIVIDER_SIZE);
            setUserlistWidth(previousUserlistWidth, userlistMinWidth);
            setTextPreferredSizeTemporarily();
            mainPane.setDividerLocation(-1);
        } else {
            previousUserlistWidth = userlist.getWidth();
            userlist.setVisible(false);
            mainPane.setDividerSize(0);
        }
        userlistEnabled = enable;
        revalidate();
    }
    
    public void setCompletionEnabled(boolean enabled) {
        input.setCompletionEnabled(enabled);
    }
    
    /**
     * Toggle the userlist.
     */
    public final void toggleUserlist() {
        setUserlistEnabled(!userlistEnabled);
    }
    
    public void selectPreviousUser() {
        text.selectPreviousUser();
    }
    
    public void selectNextUser() {
        text.selectNextUser();
    }
    
    public void selectNextUserExitAtBottom() {
        text.selectNextUserExitAtBottom();
    }
    
    public void exitUserSelection() {
        text.exitUserSelection();
    }
    
    public void toggleUserSelection() {
        text.toggleUserSelection();
    }
    
    public User getSelectedUser() {
        return text.getSelectedUser();
    }
    
    @Override
    public String toString() {
        return String.format("%s '%s'", type, room);
    }
    
}
