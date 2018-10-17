
package chatty.gui.components;

import chatty.Room;
import chatty.gui.MouseClickedListener;
import chatty.gui.StyleManager;
import chatty.gui.StyleServer;
import chatty.gui.MainGui;
import chatty.User;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.textpane.ChannelTextPane;
import chatty.gui.components.textpane.Message;
import chatty.util.StringUtil;
import chatty.util.api.Emoticon;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
public class Channel extends JPanel {
    
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
        users = new UserList(contextMenuListener, main.getUserListener());
        updateUserlistSettings();
        userlist = new JScrollPane(users);
        
        
        // Split Pane
        mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,west,userlist);
        mainPane.setResizeWeight(1);
        mainPane.setDividerSize(DIVIDER_SIZE);
        
        // Text input
        input = new ChannelEditBox(40);
        input.addActionListener(main.getActionListener());
        input.setCompletionServer(new InputCompletionServer());
        // Remove PAGEUP/DOWN so it can scroll chat (as before JTextArea)
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "-");
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "-");

        // Add components
        add(mainPane, BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);

        input.requestFocusInWindow();
        setStyles();
        
        input.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (onceOffEditListener != null && room != Room.EMPTY) {
                    onceOffEditListener.edited(room.getChannel());
                    onceOffEditListener = null;
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }
    
    public boolean setRoom(Room room) {
        if (room != null && this.room != room) {
            this.room = room;
            refreshBufferSize();
            setName(room.getDisplayName());
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

    private class InputCompletionServer implements AutoCompletionServer {
        
        private final UserSorterNew userSorterNew = new UserSorterNew();
        private final UserSorterAlphabetic userSorterAlphabetical = new UserSorterAlphabetic();
        
        private final Set<String> commands = new TreeSet<>(Arrays.asList(new String[]{
            "subscribers", "subscribersOff", "timeout", "ban", "unban", "host", "unhost", "raid", "unraid", "clear", "mods",
            "part", "close", "reconnect", "slow", "slowOff", "r9k", "r9koff", "emoteonly", "emoteonlyoff",
            "connection", "uptime", "appinfo", "releaseInfo",
            "dir", "wdir", "openDir", "openWdir",
            "showBackupDir", "openBackupDir", "showDebugDir", "openDebugDir",
            "showTempDir", "openTempDir", "showJavaDir", "openJavaDir",
            "clearChat", "refresh", "changetoken", "testNotification", "server",
            "set", "add", "clearSetting", "remove", "customCompletion",
            "clearStreamChat", "getStreamChatSize", "setStreamChatSize", "streamChatTest", "openStreamChat",
            "customEmotes", "reloadCustomEmotes", "addStreamHighlight", "openStreamHighlights",
            "ignore", "unignore", "ignoreWhisper", "unignoreWhisper", "ignoreChat", "unignoreChat",
            "follow", "unfollow", "ffzws", "followers", "followersoff",
            "setcolor", "untimeout", "userinfo", "joinhosted", "favorite", "unfavorite"
        }));
        
        private final Set<String> prefixesPreferUsernames = new HashSet<>(Arrays.asList(new String[]{
            "/ban ", "/to ", "/setname ", "/resetname ", "/timeout ", "/host ",
            "/unban ", "/ignore ", "/unignore ", "/ignoreChat ", "/unignoreChat ",
            "/ignoreWhisper ", "/unignoreWhisper ", "/follow ", "/unfollow ",
            "/untimeout ", "/favorite ", "/unfavorite "
        }));
        
        private void updateSettings() {
            input.setCompletionMaxItemsShown((int) main.getSettings().getLong("completionMaxItemsShown"));
            input.setCompletionShowPopup(main.getSettings().getBoolean("completionShowPopup"));
            input.setCompleteToCommonPrefix(main.getSettings().getBoolean("completionCommonPrefix"));
        }
        
        @Override
        public CompletionItems getCompletionItems(String type, String prefix, String search) {
            updateSettings();
            search = StringUtil.toLowerCase(search);
            if (type == null) {
                return getRegularCompletionItems(prefix, search);
            } else if (type.equals("special")) {
                return getSpecialItems(prefix, search);
            }
            return new CompletionItems();
        }
        
        /**
         * TAB
         * 
         * @param prefix
         * @param search
         * @return 
         */
        private CompletionItems getRegularCompletionItems(String prefix, String search) {
            List<String> items;
            if (prefix.startsWith("/")
                    && (prefix.equals("/set ") || prefix.equals("/get ")
                    || prefix.equals("/add ") || prefix.equals("/remove ")
                    || prefix.equals("/clearSetting ")
                    || prefix.equals("/reset "))) {
                //--------------
                // Setting Names
                //--------------
                items = filterCompletionItems(main.getSettingNames(), search);
                input.setCompleteToCommonPrefix(true);
            } else if (prefix.equals("/")) {
                //--------------
                // Command Names
                //--------------
                items = filterCompletionItems(commands, search);
                items.addAll(filterCompletionItems(main.getCustomCommandNames(), search));
            } else {
                //--------------------
                // Depending on Config
                //--------------------
                return getMainItems(main.getSettings().getString("completionTab"), prefix, search);
            }
            return new CompletionItems(items, "");
        }
        
        /**
         * Shift-TAB
         * 
         * @param prefix
         * @param search
         * @return 
         */
        private CompletionItems getSpecialItems(String prefix, String search) {
            return getMainItems(main.getSettings().getString("completionTab2"), prefix, search);
        }
        
        /**
         * Returns items that depend on current settings, but also some
         * prefixes.
         *
         * @param setting
         * @param prefix
         * @param search
         * @return 
         */
        private CompletionItems getMainItems(String setting, String prefix, String search) {
            boolean preferUsernames = prefixesPreferUsernames.contains(prefix)
                        && main.getSettings().getBoolean("completionPreferUsernames");
            
            // Check prefixes and other non-setting dependant stuff first
            if (prefix.endsWith("@") || prefixesPreferUsernames.contains(prefix)) {
                return getCompletionItemsNames(search, preferUsernames);
            }
            if (prefix.endsWith(".")) {
                return new CompletionItems(getCustomCompletionItems(search), ".");
            }
            if (prefix.endsWith(":")) {
                return getCompletionItemsEmoji(search);
            }
            
            // Then check settings
            if (setting.equals("names")) {
                return getCompletionItemsNames(search, preferUsernames);
            }
            if (setting.equals("emotes")) {
                return getCompletionItemsEmotes(search, false);
            }
            if (setting.equals("custom")) {
                return new CompletionItems(getCustomCompletionItems(search), "");
            }
            CompletionItems names = getCompletionItemsNames(search, preferUsernames);
            CompletionItems emotes = getCompletionItemsEmotes(search, true);
            if (setting.equals("both")) {
                names.append(emotes);
                return names;
            } else { // both2
                emotes.append(names);
                return emotes;
            }
        }

        private CompletionItems getCompletionItemsEmotes(String search, boolean includeInfo) {
            Collection<String> allEmotes = new LinkedList<>(main.getEmoteNames());
            allEmotes.addAll(main.getEmoteNamesPerStream(getStreamName()));
            List<String> result = filterCompletionItems(allEmotes, search);
            if (includeInfo) {
                Map<String, String> info = new HashMap<>();
                result.forEach(n -> info.put(n, "<small>Emote</small>"));
                return new CompletionItems(result, info, "");
            }
            return new CompletionItems(result, "");
        }
        
        private List<String> getCustomCompletionItems(String search) {
            String result = main.getCustomCompletionItem(search);
            List<String> list = new ArrayList<>();
            if (result != null) {
                list.add(result);
            }
            return list;
        }
        
        private CompletionItems getCompletionItemsEmoji(String search) {
            List<String> result = new LinkedList<>();
            Map<String, String> info = new HashMap<>();
            // Get font height for correct display size of Emoji
            int height = input.getFontMetrics(input.getFont()).getHeight();
            for (Emoticon emote : main.emoticons.getEmoji()) {
                if (emote.stringId != null
                        && (emote.stringId.startsWith(":"+search)
                            || (search.length() > 3 && emote.stringId.contains(search)))) {
                    if (main.getSettings().getBoolean("emojiReplace")) {
                        result.add(emote.stringId);
                        info.put(emote.stringId, "<img width='"+height+"' height='"+height+"' src='"+emote.url+"'/>");
                    } else {
                        result.add(emote.code);
                        info.put(emote.code, emote.stringId+" <img width='"+height+"' height='"+height+"' src='"+emote.url+"'/>");
                    }
                }
            }
            return new CompletionItems(result, info, ":");
        }
        
        private List<String> filterCompletionItems(Collection<String> data,
                String search) {
            List<String> matched = new ArrayList<>();
            for (String name : data) {
                if (StringUtil.toLowerCase(name).startsWith(search)) {
                    matched.add(name);
                }
            }
            Collections.sort(matched);
            return matched;
        }
            
        private CompletionItems getCompletionItemsNames(String search, boolean preferUsernames) {
            List<User> matchedUsers = new ArrayList<>();
            Set<User> regularMatched = new HashSet<>();
            Set<User> customMatched = new HashSet<>();
            Set<User> localizedMatched = new HashSet<>();
            for (User user : users.getData()) {
                boolean matched = false;
                if (user.getName().startsWith(search)) {
                    matched = true;
                    regularMatched.add(user);
                }
                if (!user.hasRegularDisplayNick() && StringUtil.toLowerCase(user.getDisplayNick()).startsWith(search)) {
                    matched = true;
                    localizedMatched.add(user);
                }
                if (user.hasCustomNickSet() && StringUtil.toLowerCase(user.getCustomNick()).startsWith(search)) {
                    matched = true;
                    customMatched.add(user);
                }
                
                if (matched) {
                    matchedUsers.add(user);
                }
            }
            switch (main.getSettings().getString("completionSorting")) {
                case "predictive":
                    Collections.sort(matchedUsers, userSorterNew);
                    break;
                case "alphabetical":
                    Collections.sort(matchedUsers, userSorterAlphabetical);
                    break;
                default:
                    Collections.sort(matchedUsers);
            }
            boolean includeAllNameTypes = main.getSettings().getBoolean("completionAllNameTypes");
            boolean includeAllNameTypesRestriction = main.getSettings().getBoolean("completionAllNameTypesRestriction");
            List<String> nicks = new ArrayList<>();
            Map<String, String> info = new HashMap<>();
            for (User user : matchedUsers) {
                if (includeAllNameTypes
                        && (!includeAllNameTypesRestriction || matchedUsers.size() <= 2)) {
                    if (customMatched.contains(user) && !preferUsernames) {
                        nicks.add(user.getCustomNick());
                        if (!user.hasRegularDisplayNick()) {
                            nicks.add(user.getDisplayNick());
                        }
                        if (user.hasCustomNickSet() && !user.getCustomNick().equalsIgnoreCase(user.getRegularDisplayNick())) {
                            nicks.add(user.getRegularDisplayNick());
                        }
                    }
                    else if (localizedMatched.contains(user) && !preferUsernames) {
                        nicks.add(user.getDisplayNick());
                        if (user.hasCustomNickSet() && !user.getCustomNick().equalsIgnoreCase(user.getRegularDisplayNick())) {
                            nicks.add(user.getCustomNick());
                        }
                        nicks.add(user.getRegularDisplayNick());
                    }
                    else {
                        nicks.add(user.getRegularDisplayNick());
                        if (!user.hasRegularDisplayNick()) {
                            nicks.add(user.getDisplayNick());
                        }
                        if (user.hasCustomNickSet() && !user.getCustomNick().equalsIgnoreCase(user.getRegularDisplayNick())) {
                            nicks.add(user.getCustomNick());
                        }
                    }
                }
                else {
                    if (regularMatched.contains(user) || preferUsernames) {
                        nicks.add(user.getRegularDisplayNick());
                    }
                    if (localizedMatched.contains(user) && !preferUsernames) {
                        nicks.add(user.getDisplayNick());
                    }
                    if (customMatched.contains(user) && !preferUsernames) {
                        nicks.add(user.getCustomNick());
                    }
                }
                
                if (!user.hasRegularDisplayNick()) {
                    info.put(user.getDisplayNick(), user.getRegularDisplayNick());
                    info.put(user.getRegularDisplayNick(), user.getDisplayNick());
                }
                if (user.hasCustomNickSet()) {
                    info.put(user.getCustomNick(), user.getRegularDisplayNick());
                    info.put(user.getRegularDisplayNick(), user.getCustomNick());
                }
            }
            return new CompletionItems(nicks, info, "");
        }
        
        private class UserSorterNew implements Comparator<User> {

            @Override
            public int compare(User o1, User o2) {
                int s1 = o1.getActivityScore();
                int s2 = o2.getActivityScore();
                //System.out.println(o1+" "+s1+" "+o2+" "+s2);
                if (s1 == s2) {
                    return o1.compareTo(o2);
                } else if (s1 > s2) {
                    return -1;
                }
                return 1;
            }
            
        }
        
        private class UserSorterAlphabetic implements Comparator<User> {

            @Override
            public int compare(User o1, User o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
            
        }
        
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
    
    private OnceOffEditListener onceOffEditListener;
    
    public void setOnceOffEditListener(OnceOffEditListener listener) {
        onceOffEditListener = listener;
    }
    
    public interface OnceOffEditListener {
        public void edited(String channel);
    }
    
}
