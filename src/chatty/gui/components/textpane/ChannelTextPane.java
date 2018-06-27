
package chatty.gui.components.textpane;

import chatty.gui.components.ChannelEditBox;
import chatty.Helper;
import chatty.SettingsManager;
import chatty.gui.MouseClickedListener;
import chatty.gui.UserListener;
import chatty.gui.HtmlColors;
import chatty.gui.LinkListener;
import chatty.gui.StyleServer;
import chatty.gui.UrlOpener;
import chatty.gui.MainGui;
import chatty.User;
import chatty.util.api.usericons.Usericon;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.TwitchEmotes.Emoteset;
import chatty.util.api.CheerEmoticon;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.Emoticon.EmoticonUser;
import chatty.util.api.Emoticons;
import chatty.util.api.Emoticons.TagEmotes;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.swing.*;
import static javax.swing.JComponent.WHEN_FOCUSED;
import javax.swing.border.Border;
import javax.swing.text.*;
import javax.swing.text.html.HTML;


/**
 * Text pane that displays chat, provides auto-scrolling, styling, context
 * menus, clickable elements.
 * 
 * <p>Special Attributes:</p>
 * <ul>
 * <li>Elements containing a user name mostly (where available) contain the User
 * object in Attribute.USER (Chat messages, ban messages, joins/parts, etc.)</li>
 * <li>Chat messages (from a user) contain Attribute.USER_MESSAGE for the leaf
 * containing the user name</li>
 * <li>Ban messages ({@code <name> has been banned from talking}) contain
 * Attribute.BAN_MESSAGE=User in the first leaf and Attribute.BAN_MESSAGE_COUNT
 * with an int showing how many bans were combined in the leaf showing the
 * number of bans (if present, usually the last or second to last element)</li>
 * <li>Deleted lines contain Attribute.DELETED_LINE as paragraph attribute</li>
 * </ul>
 * 
 * @author tduva
 */
public class ChannelTextPane extends JTextPane implements LinkListener, EmoticonUser {
    
    private static final Logger LOGGER = Logger.getLogger(ChannelTextPane.class.getName());
    
    private final StyledDocument doc;
    
    private static final Color BACKGROUND_COLOR = new Color(250,250,250);
    
    // Compact mode
    private String compactMode = null;
    private long compactModeStart = 0;
    private int compactModeLength = 0;
    private static final int MAX_COMPACTMODE_LENGTH = 10;
    private static final int MAX_COMPACTMODE_TIME = 30*1000;
    
    private static final int MAX_BAN_MESSAGE_COMBINE_TIME = 10*1000;
    
    /**
     * Min and max buffer size to restrict the setting range
     */
    private static final int BUFFER_SIZE_MIN = 10;
    private static final int BUFFER_SIZE_MAX = 10000;

    /**
     * The Matcher to use for finding URLs in messages.
     */
    private static final Matcher urlMatcher = Helper.getUrlPattern().matcher("");
    
    public MainGui main;

    protected LinkController linkController = new LinkController();
    private static StyleServer styleServer;
    
    public enum Attribute {
        IS_BAN_MESSAGE, BAN_MESSAGE_COUNT, TIMESTAMP, USER, IS_USER_MESSAGE,
        URL_DELETED, DELETED_LINE, EMOTICON, IS_APPENDED_INFO, INFO_TEXT, BANS,
        BAN_MESSAGE, ID, ID_AUTOMOD, USERICON
    }
    
    public enum MessageType {
        REGULAR, HIGHLIGHTED, IGNORED_COMPACT
    }
    
    /**
     * Whether the next line needs a newline-character prepended
     */
    private boolean newlineRequired = false;
    
    public enum Setting {
        TIMESTAMP_ENABLED, EMOTICONS_ENABLED, AUTO_SCROLL, USERICONS_ENABLED, 
        
        SHOW_BANMESSAGES, COMBINE_BAN_MESSAGES, DELETE_MESSAGES,
        DELETED_MESSAGES_MODE, BAN_DURATION_APPENDED, BAN_REASON_APPENDED,
        BAN_DURATION_MESSAGE, BAN_REASON_MESSAGE,
        
        ACTION_COLORED, BUFFER_SIZE, AUTO_SCROLL_TIME,
        EMOTICON_MAX_HEIGHT, EMOTICON_SCALE_FACTOR, BOT_BADGE_ENABLED,
        FILTER_COMBINING_CHARACTERS, PAUSE_ON_MOUSEMOVE,
        PAUSE_ON_MOUSEMOVE_CTRL_REQUIRED, EMOTICONS_SHOW_ANIMATED,
        COLOR_CORRECTION, SHOW_TOOLTIPS,
        
        DISPLAY_NAMES_MODE
    }
    
    private static final long DELETED_MESSAGES_KEEP = 0;
    
    protected final Styles styles = new Styles();
    private final ScrollManager scrollManager;
    
    public final LineSelection lineSelection;
    
    private int messageTimeout = -1;
    
    private final javax.swing.Timer updateTimer;
    
    public ChannelTextPane(MainGui main, StyleServer styleServer) {
        this(main, styleServer, false, true);
    }
    
    public ChannelTextPane(MainGui main, StyleServer styleServer, boolean special, boolean startAtBottom) {
        lineSelection = new LineSelection(main.getUserListener());
        ChannelTextPane.styleServer = styleServer;
        this.main = main;
        this.setBackground(BACKGROUND_COLOR);
        this.addMouseListener(linkController);
        this.addMouseMotionListener(linkController);
        linkController.addUserListener(main.getUserListener());
        linkController.addUserListener(lineSelection);
        linkController.setLinkListener(this);
        scrollManager = new ScrollManager();
        this.addMouseListener(scrollManager);
        this.addMouseMotionListener(scrollManager);
        setEditorKit(new MyEditorKit(startAtBottom));
        this.setDocument(new MyDocument());
        doc = getStyledDocument();
        setEditable(false);
        DefaultCaret caret = new NoScrollCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        setCaret(caret);
        styles.setStyles();
        
        if (special) {
            updateTimer = new javax.swing.Timer(2000, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    removeOldLines();
                }
            });
            updateTimer.setRepeats(true);
            updateTimer.start();
        } else {
            updateTimer = null;
        }
        
        FixSelection.install(this);
    }
    
    /**
     * This has to be called when the ChannelTextPane is no longer used, so it
     * can be gargabe collected.
     */
    public void cleanUp() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        scrollManager.cleanUp();
        linkController.cleanUp();
    }
    
    public void setMessageTimeout(int seconds) {
        this.messageTimeout = seconds;
    }
    
    public void setContextMenuListener(ContextMenuListener listener) {
        linkController.setContextMenuListener(listener);
    }
    
    public void setMouseClickedListener(MouseClickedListener listener) {
        linkController.setMouseClickedListener(listener);
    }
    
    /**
     * Can be called when an icon finished loading, so it is displayed correctly.
     * 
     * This seems pretty ineffecient, because it refreshes the whole document.
     */
    @Override
    public void iconLoaded() {
        ((MyDocument)doc).refresh();
        scrollDownIfNecessary();
    }
    
    /**
     * Outputs some type of message.
     * 
     * @param message Message object containing all the data
     */
    public void printMessage(Message message) {
        if (message instanceof UserMessage) {
            printUserMessage((UserMessage)message);
        } else if (message instanceof UserNotice) {
            printUsernotice((UserNotice)message);
        } else if (message instanceof AutoModMessage) {
            printAutoModMessage((AutoModMessage)message);
        }
    }
    
    /**
     * Print the notification when a user has subscribed, which may contain an
     * attached message from the user which requires special handling.
     * 
     * @param message 
     */
    private void printUsernotice(UserNotice message) {
        closeCompactMode();
        print(getTimePrefix(), styles.info());
        
        MutableAttributeSet style;
        if (message.user.getName().isEmpty()) {
            // Only dummy User attached (so no custom message attached as well)
            style = styles.info();
        } else {
            /**
             * This is kind of a hack to allow this message to be clicked and
             * deleted.
             * 
             * Note: For shortening/deleting messages, everything after the User
             * element is affected, so in this case just the attached message.
             */
            style = styles.nick(message.user, styles.info());
            style.addAttribute(Attribute.IS_USER_MESSAGE, true);
        }
        
        String text = message.text;
        print("["+message.type+"] "+text+" ", style);
        if (!StringUtil.isNullOrEmpty(message.attachedMessage)) {
            print("[", styles.info());
            // Output with emotes, but don't turn URLs into clickable links
            printSpecials(message.attachedMessage, message.user, styles.info(), message.emotes, true, false);
            print("]", styles.info());
        }
        printNewline();
    }
    
    private void printAutoModMessage(AutoModMessage message) {
        closeCompactMode();
        print(getTimePrefix(), styles.info());
        
        MutableAttributeSet style = styles.nick(message.user, styles.info());
        style.addAttribute(Attribute.ID_AUTOMOD, message.id);
        print("[AutoMod] <"+message.user.getDisplayNick()+"> ", style);
        print(message.text, styles.info());
        printNewline();
    }

    /**
     * Output a regular message from a user.
     * 
     * @param message The object contain all the data
     */
    private void printUserMessage(UserMessage message) {
        User user = message.user;
        boolean ignored = message.ignored_compact;
        if (ignored) {
            printCompact("IGNORED", user);
            return;
        }

        Color color = message.color;
        boolean action = message.action;
        String text = message.text;
        TagEmotes emotes = message.emotes;
        boolean highlighted = message.highlighted;
        if (message.whisper && message.action) {
            color = StyleConstants.getForeground(styles.info());
            highlighted = true;
        }
        
        closeCompactMode();

        MutableAttributeSet style;
        if (highlighted) {
            style = styles.highlight(color);
        } else {
            style = styles.standard(color);
        }
        print(getTimePrefix(), style);
        printUser(user, action, message.whisper, message.id);
        
        // Change style for text if /me and no highlight (if enabled)
        if (!highlighted && color == null && action && styles.actionColored()) {
            style = styles.standard(user.getDisplayColor());
        }
        printSpecials(text, user, style, emotes, false, message.bits > 0);
        printNewline();
    }
    
    private long getTimeAgo(Element element) {
        Long timestamp = (Long)element.getAttributes().getAttribute(Attribute.TIMESTAMP);
        if (timestamp != null) {
            return System.currentTimeMillis() - timestamp;
        }
        return Long.MAX_VALUE;
    }
    
    /**
     * Gets the first element containing the given key in it's attributes, or
     * null if it wasn't found.
     * 
     * @param parent The Element whose subelements are searched
     * @param key The key of the attributes the searched element should have
     * @return The found Element
     */
    private static Element getElementContainingAttributeKey(Element parent, Object key) {
        for (int i = 0; i < parent.getElementCount(); i++) {
            Element element = parent.getElement(i);
            if (element.getAttributes().getAttribute(key) != null) {
                return element;
            }
        }
        return null;
    }

    /**
     * Adds or increases the number behind the given ban message.
     * 
     * @param line 
     */
    private void increasePreviousBanMessage(Element line, final long duration, final String reason) {
        changeInfo(line, new InfoChanger() {

            @Override
            public void changeInfo(MutableAttributeSet attributes) {
                Integer count = (Integer)attributes.getAttribute(Attribute.BAN_MESSAGE_COUNT);
                if (count == null) {
                    // If it doesn't exist set to 2, because this will be the second
                    // timeout this message represents
                    count = 2;
                } else {
                    // Otherwise increase number and removet text of previous number
                    count++;
                }
                attributes.addAttribute(Attribute.BAN_MESSAGE_COUNT, count);
            }
        });
    }
    
    private interface InfoChanger {
        public void changeInfo(MutableAttributeSet attributes);
    }
    
    /**
     * Changes the info at  the end of a line.
     * 
     * @param line 
     * @param changer 
     */
    private void changeInfo(Element line, InfoChanger changer) {
        try {
            Element infoElement = getElementContainingAttributeKey(line,
                    Attribute.IS_APPENDED_INFO);
            
            boolean isNew = false;
            
            MutableAttributeSet attributes;
            if (infoElement == null) {
                infoElement = line.getElement(line.getElementCount() - 1);
                attributes = new SimpleAttributeSet(styles.info());
                isNew = true;
            } else {
                attributes = new SimpleAttributeSet(infoElement.getAttributes());
            }
            
            int start = infoElement.getStartOffset();
            int length = infoElement.getEndOffset() - infoElement.getStartOffset();
            
//            String currentText = StringUtil.removeLinebreakCharacters(getElementText(infoElement));
//            System.out.println(String.format("'%s' %d %s %d", currentText, start, infoElement, doc.getLength()));
            
            // Change attributes
            changer.changeInfo(attributes);
            attributes.addAttribute(Attribute.IS_APPENDED_INFO, true);
            
            if (!isNew) {
                doc.remove(start, length);
            }
            
            // Make text based on current attributes
            String text = "";
            Integer banCount = (Integer)attributes.getAttribute(Attribute.BAN_MESSAGE_COUNT);
            if (banCount != null && banCount > 1) {
                text += String.format("(%d)", banCount);
            }
            
            String infoText = (String)attributes.getAttribute(Attribute.INFO_TEXT);
            if (infoText != null && !infoText.isEmpty()) {
                text = StringUtil.append(text, " ", infoText);
            }
            
            /**
             * Insert at the end of the countElement (which is either the last
             * element or the one that contains the count), but if it contains
             * a linebreak (which should be at the end), then start before the
             * linebreak.
             * 
             * With no next line of different style line (extra element with
             * linebreak, so starting at the beginning of that would work):
             * '[17:02] ''tduva'' has been banned from talking''
             * '
             * 
             * With same style line (info style) in the next line (linebreak at
             * the end of the last text containing element, starting at the
             * beginning of that would place it after the name):
             * '[17:02] ''tduva'' has been banned from talking
             * '
             * 
             * Once the count element is added properly (linebreak in it's own
             * element, probably because of different attributes):
             * '[17:02] ''tduva'' has been banned from talking'' (2)''
             * '
             */
            
            int insertStart = infoElement.getEndOffset();
            if (getElementText(infoElement).contains("\n")) {
                insertStart--;
            }
            // Add with space
            doc.insertString(insertStart, " "+text, attributes);
        } catch (BadLocationException ex) {
            LOGGER.warning("Bad location: "+ex);
        }
        scrollDownIfNecessary();
    }
    
    private String getElementText(Element element) {
        try {
            return doc.getText(element.getStartOffset(), element.getEndOffset() - element.getStartOffset());
        } catch (BadLocationException ex) {
            LOGGER.warning("Bad location");
        }
        return "";
    }
    
    /**
     * Searches backwards from the newest message for a ban message from the
     * same user that is within the time threshold for combining ban messages
     * and if no message from that user was posted in the meantime.
     *
     * @param user
     * @return 
     */
    private Element findPreviousBanMessage(User user, String newMessage) {
        Element root = doc.getDefaultRootElement();
        for (int i=root.getElementCount()-1;i>=0;i--) {
            Element line = root.getElement(i);
            if (isLineFromUserAndId(line, user, null)) {
                // Stop immediately a message from that user is found first
                return null;
            }
            // By convention, the first element of the ban message must contain
            // the info that it is a ban message and of which user (and a
            // timestamp)
            Element firstElement = line.getElement(0);
            if (firstElement != null) {
                AttributeSet attr = firstElement.getAttributes();
                if (attr.containsAttribute(Attribute.IS_BAN_MESSAGE, user)
                        && getTimeAgo(firstElement) < MAX_BAN_MESSAGE_COMBINE_TIME) {
                    if (attr.getAttribute(Attribute.BAN_MESSAGE).equals(newMessage)) {
                        return line;
                    } else {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Called when a user is banned or timed out and outputs a message as well
     * as deletes the lines of the user.
     * 
     * @param user 
     * @param duration 
     * @param reason 
     * @param id The id of the deleted message, null if no specific message
     */
    public void userBanned(User user, long duration, String reason, String id) {
        if (styles.showBanMessages()) {
            String banInfo = Helper.makeBanInfo(duration, reason,
                    styles.isEnabled(Setting.BAN_DURATION_MESSAGE),
                    styles.isEnabled(Setting.BAN_REASON_MESSAGE),
                    false);
            
            String message = "has been banned";
            if (duration > 0) {
                message = "has been timed out";
            }
            if (!StringUtil.isNullOrEmpty(id)) {
                message += " (single message)";
            }
            if (!banInfo.isEmpty()) {
                message = message+" "+banInfo;
            }

            Element prevMessage = null;
            if (styles.combineBanMessages()) {
                prevMessage = findPreviousBanMessage(user, message);
            }
            if (prevMessage != null) {
                increasePreviousBanMessage(prevMessage, duration, reason);
            } else {
                closeCompactMode();
                print(getTimePrefix(), styles.banMessage(user, message));
                print(user.getCustomNick(), styles.nick(user, styles.info()));
                print(" "+message, styles.info());
                printNewline();
            }
        }
        
        String banInfo = Helper.makeBanInfo(duration, reason,
                styles.isEnabled(Setting.BAN_DURATION_APPENDED),
                styles.isEnabled(Setting.BAN_REASON_APPENDED),
                true);

        ArrayList<Integer> lines = getLinesFromUser(user, id);
        Iterator<Integer> it = lines.iterator();
        /**
         * values > 0 mean strike through, shorten message
         * value == 0 means strike through
         * value < 0 means delete message
         */
        boolean delete = styles.deletedMessagesMode() < DELETED_MESSAGES_KEEP;
        int i = 0;
        while (it.hasNext()) {
            int lineId = it.next();
            Element line = doc.getDefaultRootElement().getElement(lineId);
            if (delete) {
                deleteMessage(lineId);
            } else {
                strikeThroughMessage(lineId, styles.deletedMessagesMode());
            }
            if (i == lines.size() - 1) {
                setInfoText(line, banInfo);
            }
            i++;
        }
    }
    
    /**
     * Changes the free-form text behind a message.
     * 
     * @param line
     * @param info 
     */
    private void setInfoText(Element line, String info) {
        Element firstElement = line.getElement(0);
        if (firstElement != null && getTimeAgo(firstElement) > 60*1000) {
            info += "*";
        }
        final String info2 = info;
        changeInfo(line, new InfoChanger() {

            @Override
            public void changeInfo(MutableAttributeSet attributes) {
                attributes.addAttribute(Attribute.INFO_TEXT, info2);
            }
        });
    }
    
    /**
     * Searches the Document for all lines by the given user.
     * 
     * @param nick
     * @return 
     */
    private ArrayList<Integer> getLinesFromUser(User user, String id) {
        Element root = doc.getDefaultRootElement();
        ArrayList<Integer> result = new ArrayList<>();
        for (int i=0;i<root.getElementCount();i++) {
            Element line = root.getElement(i);
            if (isLineFromUserAndId(line, user, id)) {
                result.add(i);
            }
        }
        return result;
    }
    
    private boolean isMessageLine(Element line) {
        return getUserFromLine(line) != null;
    }
    
    private User getUserFromLine(Element line) {
        return getUserFromElement(getUserElementFromLine(line));
    }
    
    private Element getUserElementFromLine(Element line) {
        for (int i = 0; i < 20; i++) {
            if (i > line.getElementCount()) {
                break;
            }
            Element element = line.getElement(i);
            User elementUser = getUserFromElement(element);
            // If there is a User object, we're done
            if (elementUser != null) {
                return element;
            }
        }
        // No User object was found, so it's probably not a chat message
        return null;
    }
    
    /**
     * Checks if the given element is a line that is associated with the given
     * User.
     * 
     * @param line
     * @param user
     * @param id If non-null, only messages with this id will return true
     * @return 
     */
    private boolean isLineFromUserAndId(Element line, User user, String id) {
        Element element = getUserElementFromLine(line);
        User elementUser = getUserFromElement(element);
        if (elementUser == user) {
            if (id == null || id.equals(getIdFromElement(element))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the User-object from an element. If there is none, it returns null.
     * 
     * @param element
     * @return The User object or null if none was found
     */
    private User getUserFromElement(Element element) {
        if (element != null) {
            User elementUser = (User)element.getAttributes().getAttribute(Attribute.USER);
            Boolean isMessage = (Boolean)element.getAttributes().getAttribute(Attribute.IS_USER_MESSAGE);
            if (isMessage != null && isMessage == true) {
                return elementUser;
            }
        }
        return null;
    }
    
    /**
     * Gets the id attached to the message.
     * 
     * @param element
     * @return The ID element, or null if none was found
     */
    public static String getIdFromElement(Element element) {
        if (element != null) {
            return (String)element.getAttributes().getAttribute(Attribute.ID);
        }
        return null;
    }
    
    /**
     * Crosses out the specified line. This is used for messages that are
     * removed because a user was banned/timed out. Optionally shortens the
     * message to maxLength.
     * 
     * @param line The number of the line in the document
     * @param maxLength The maximum number of characters to shorten the message
     *  to. If maxLength <= 0 then it is not shortened.
     */
    private void strikeThroughMessage(int line, int maxLength) {
        Element elementToRemove = doc.getDefaultRootElement().getElement(line);
        if (elementToRemove == null) {
            LOGGER.warning("Line "+line+" is unexpected null.");
            return;
        }
        if (isLineDeleted(elementToRemove)) {
            return;
        }
        
        // Determine the offsets of the whole line and the message part
        int[] offsets = getMessageOffsets(elementToRemove);
        if (offsets.length != 2) {
            return;
        }
        int startOffset = elementToRemove.getStartOffset();
        int endOffset = elementToRemove.getEndOffset();
        int messageStartOffset = offsets[0];
        int messageEndOffset = offsets[1];
        int length = endOffset - startOffset;
        int messageLength = messageEndOffset - messageStartOffset - 1;
        
        if (maxLength > 0 && messageLength > maxLength) {
            // Delete part of the message if it exceeds the maximum length
            try {
                int removedStart = messageStartOffset + maxLength;
                int removedLength = messageLength - maxLength;
                doc.remove(removedStart, removedLength);
                length = length - removedLength - 1;
                doc.insertString(removedStart, "..", styles.info());
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location");
            }
        }
        doc.setCharacterAttributes(startOffset, length, styles.deleted(), false);
        setLineDeleted(startOffset);
    }
    
    /**
     * Deletes the message of the given line by replacing it with
     * <message deleted>.
     * 
     * @param line The number of the line in the document
     */
    private void deleteMessage(int line) {
        Element elementToRemove = doc.getDefaultRootElement().getElement(line);
        if (elementToRemove == null) {
            LOGGER.warning("Line "+line+" is unexpected null.");
            return;
        }
        if (isLineDeleted(elementToRemove)) {
            //System.out.println(line+"already deleted");
            return;
        }
        int[] messageOffsets = getMessageOffsets(elementToRemove);
        if (messageOffsets.length == 2) {
            int startOffset = messageOffsets[0];
            int endOffset = messageOffsets[1];
            try {
                // -1 to length to not delete newline character (I think :D)
                doc.remove(startOffset, endOffset - startOffset - 1);
                doc.insertString(startOffset, "<message deleted>", styles.info());
                setLineDeleted(startOffset);
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location: "+startOffset+"-"+endOffset+" "+ex.getLocalizedMessage());
            }
        }
    }
    
    /**
     * Checks if the given line contains an attribute indicating that the line
     * is already deleted.
     * 
     * @param line The element representing this line
     * @return 
     */
    private boolean isLineDeleted(Element line) {
        return line.getAttributes().containsAttribute(Attribute.DELETED_LINE, true);
    }
    
    /**
     * Adds a attribute to the paragraph at offset to prevent trying to delete
     * it again. 
    * 
     * @param offset 
     */
    private void setLineDeleted(int offset) {
        doc.setParagraphAttributes(offset, 1, styles.deletedLine(), false);
    }
    
    private int[] getMessageOffsets(Element line) {
        int count = line.getElementCount();
        int start = 0;
        for (int i=0;i<count;i++) {
            Element element = line.getElement(i);
            if (element.getAttributes().isDefined(Attribute.USER)) {
                start = i + 1;
            }
        }
        if (start < count) {
            int startOffset = line.getElement(start).getStartOffset();
            int endOffset = line.getElement(count - 1).getEndOffset();
            return new int[]{startOffset, endOffset};
        }
        return new int[0];
    }
    
    public void selectPreviousUser() {
        lineSelection.move(-1);
    }
    
    public void selectNextUser() {
        lineSelection.move(1);
    }
    
    public void selectNextUserExitAtBottom() {
        lineSelection.move(1, true);
    }
    
    public void exitUserSelection() {
        lineSelection.disable();
    }
    
    public void toggleUserSelection() {
        lineSelection.toggleLineSelection();
    }
    
    public User getSelectedUser() {
        return lineSelection.getSelectedUser();
    }
    
    /**
     * Allows to select a user/line using keyboard shortcuts.
     */
    private class LineSelection implements UserListener {
        
        /**
         * The line that is currently selected.
         */
        private Element currentSelection;
        
        /**
         * The User that is currently selected.
         */
        private User currentUser;
        
        /**
         * The component to return focus to when leaving the mode.
         */
        private Component shouldReturnFocusTo;
        
        /**
         * If true, don't mark the currently selected line with a different
         * color.
         */
        private boolean subduedHl;
        
        private LineSelection(final UserListener userListener) {
            
            addFocusListener(new FocusAdapter() {
                
                @Override
                public void focusGained(FocusEvent e) {
                    /**
                     * If the previous focus was on the text inputbox, then
                     * return focus there when done.
                     */
                    if (e.getOppositeComponent() != null && e.getOppositeComponent().getClass() == ChannelEditBox.class) {
                        shouldReturnFocusTo = e.getOppositeComponent();
                    }
                }
                
                @Override
                public void focusLost(FocusEvent e) {
                    /**
                     * Only quit out of mode when focus is lost to the text
                     * inputbox.
                     */
                    if (e.getOppositeComponent() != null && e.getOppositeComponent().getClass() == ChannelEditBox.class) {
                        disable();
                    }
                }
                
            });
            
//            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl SPACE"), "LineSelection.toggle");
//            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl S"), "LineSelection.toggle");
//            getActionMap().put("LineSelection.toggle", new AbstractAction() {
//
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    toggleLineSelection();
//                }
//            });

            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("W"), "LineSelection.moveUp");
            getActionMap().put("LineSelection.moveUp", new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    move(-1);
                }
            });
            
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("A"), "LineSelection.moveUpMore");
            getActionMap().put("LineSelection.moveUpMore", new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    move(-1);
                    move(-1);
                }
            });
            
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("D"), "LineSelection.moveDownMore");
            getActionMap().put("LineSelection.moveDownMore", new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    move(1);
                    move(1);
                }
            });
            
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("S"), "LineSelection.moveDown");
            getActionMap().put("LineSelection.moveDown", new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    move(1);
                }
            });
            
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("E"), "LineSelection.action");
            getActionMap().put("LineSelection.action", new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentSelection != null && doesLineExist(currentSelection)) {
                        userListener.userClicked(currentUser, getCurrentId(), null, null);
                    }
                }
            });
            
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("Q"), "LineSelection.exit");
            getActionMap().put("LineSelection.exit", new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    disable();
                }
            });
        }
        
        /**
         * Toggles the mode on and off. If something is currently selected, then
         * quit out of the mode, otherwise try to enter the mode.
         */
        void toggleLineSelection() {
            if (currentSelection != null) {
                disable();
            } else {
                requestFocusInWindow();
                move(-1);
            }
        }
        
        /**
         * Disables the mode. Remove selection, scroll down and return focus if
         * applicable.
         */
        private void disable() {
            if (currentSelection == null) {
                return;
            }
            resetSearch();
            if (currentSelection != null) {
                scrollManager.scrollDown();
            }
            currentSelection = null;
            currentUser = null;
            if (shouldReturnFocusTo != null) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        shouldReturnFocusTo.requestFocusInWindow();
                    }
                });
            }
        }
        
        private void move(int jump) {
            move(jump, false);
        }
        
        /**
         * Start searching from the currently selected line into the direction
         * indicated by jump.
         * 
         * @param jump 
         */
        private void move(int jump, boolean exitAtBottom) {
            
            subduedHl = false;
            
            int count = doc.getDefaultRootElement().getElementCount();
            if (currentSelection != null && !doesLineExist(currentSelection)) {
                currentSelection = null;
            }
            // Determine if search should start immediately.
            boolean startSearch = currentSelection == null;
            // Loop through all lines
            
            if (currentSelection == null && exitAtBottom) {
                return;
            }
            
            int start = 0;
            int direction = 1;
            if (jump < 0) {
                start = count - 1;
                direction = -1;
            }
            
            for (int i = start; i >= 0 && i < count+1; i=i+direction) {
                if (i == count) {
                    // Count to max+1, so when further than the bottom, exit
                    // mode if requested.
                    if (exitAtBottom) {
                        disable();
                    }
                    return;
                }
                Element element = doc.getDefaultRootElement().getElement(i);
                User user = getUserFromLine(element);
                if (element == currentSelection) {
                    // If this lines contained the last result, start searching
                    // on next line
                    startSearch = true;
                    
                    continue;
                }
                if (user == currentUser) {
                    continue;
                }
                if (!startSearch) {
                    continue;
                }

                boolean selected = select(element);
                if (selected) {
                    break;
                }
            }
        }
        
        /**
         * Try to select the given line. Only selects chat messages. Also
         * selects all other messages in the buffer from the same user and
         * unselects any previously selected.
         * 
         * @param line The line to select
         * @return true if the line was selected (if it is a chat message),
         * false otherwise
         */
        private boolean select(Element line) {
            if (isMessageLine(line)) {
                User user = getUserFromLine(line);
                clearSearchResult();
                highlightLine(line, true);
                currentSelection = line;

                currentUser = user;
                ArrayList<Integer> lines = getLinesFromUser(user, null);
                for (Integer lineNumber : lines) {
                    Element otherLine = doc.getDefaultRootElement().getElement(lineNumber);
                    if (otherLine != currentSelection) {
                        highlightLine(otherLine, false);
                    }
                }
                return true;
            }
            return false;
        }
        
        /**
         * Changes the background color of the given line and scrolls to it if
         * primary is true.
         * 
         * @param element The line to highlight
         * @param primary If true, then it uses a different background color and
         * scrolls to the line
         */
        private void highlightLine(Element element, boolean primary) {
            int startOffset = element.getStartOffset();
            int endOffset = element.getEndOffset() - 1;
            int length = endOffset - startOffset;
//            MutableAttributeSet style = primary && !subduedHl ? styles.searchResult2(primary) : styles.searchResult(primary);
            MutableAttributeSet style = primary ? styles.searchResult2(primary) : styles.searchResult(primary);
            doc.setCharacterAttributes(startOffset, length, style, false);
            if (primary) {
                scrollManager.scrollToOffset(startOffset);
            }
        }
        
        /**
         * Called when a line is added to the buffer. If a user is currently
         * selected and this new line is from that user, then highlight it as
         * well as secondary line.
         * 
         * @param element The line that was added and that is checked as to
         * whether it should be highlighted
         */
        private void onLineAdded(Element element) {
            //GuiUtil.debugLineContents(element);
            if (currentUser != null && isLineFromUserAndId(element, currentUser, null)) {
                highlightLine(element, false);
            }
        }
        
        private String getCurrentId() {
            if (currentSelection != null) {
                return getIdFromElement(getUserElementFromLine(currentSelection));
            }
            return null;
        }

        /**
         * When a user is clicked while holding Ctrl down, then select that user
         * and line.
         * 
         * @param user The user that was clicked
         * @param e The MouseEvent of the click
         */
        @Override
        public void userClicked(User user, String messageId, String autoModMsgId, MouseEvent e) {
            if (e != null && ((e.isAltDown() && e.isControlDown()) || e.isAltGraphDown())) {
                Element element = LinkController.getElement(e);
                Element line = null;
                while (element.getParentElement() != null) {
                    line = element;
                    element = element.getParentElement();
                }
                select(line);
            }
            
            /**
             * Mark lines when clicking on User top open User Info Dialog (needs
             * more testing and probably a setting). This would replace the code
             * above.
             */
//            if (e != null) {
//                subduedHl = true;
//                if ((e.isAltDown() && e.isControlDown()) || e.isAltGraphDown()) {
//                    subduedHl = false;
//                }
//
//                Element element = LinkController.getElement(e);
//                Element line = null;
//                while (element.getParentElement() != null) {
//                    line = element;
//                    element = element.getParentElement();
//                }
//                if (line != null) {
//                    select(line);
//                }
//            }
        }
        
        public User getSelectedUser() {
            if (doesLineExist(currentSelection)) {
                return currentUser;
            }
            return null;
        }

        @Override
        public void emoteClicked(Emoticon emote, MouseEvent e) {
        }

        @Override
        public void usericonClicked(Usericon usericon, MouseEvent e) {
        }
        
    }
    
    private Element lastSearchPos = null;
    
    /**
     * Checks if the given line exists in this document.
     * 
     * @param line The line to check
     * @return true if the line was found in the document, false otherwise
     */
    private boolean doesLineExist(Object line) {
        int count = doc.getDefaultRootElement().getElementCount();
        for (int i=0;i<count;i++) {
            if (doc.getDefaultRootElement().getElement(i) == line) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Perform search in the chat buffer. Starts searching for the given text
     * backwards from the last found position.
     * 
     * @param searchText 
     * @return  
     */
    public boolean search(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return false;
        }
        clearSearchResult();
        int count = doc.getDefaultRootElement().getElementCount();
        if (lastSearchPos != null && !doesLineExist(lastSearchPos)) {
            //System.out.println(lastSearchPos+"doesnt exist");
            lastSearchPos = null;
        }
        // Determine if search should start immediately.
        boolean startSearch = lastSearchPos == null;
        searchText = StringUtil.toLowerCase(searchText);
        // Loop through all lines
        for (int i=count-1;i>=0;i--) {
            //System.out.println(i+"/"+count);
            Element element = doc.getDefaultRootElement().getElement(i);
            if (element == lastSearchPos) {
                // If this lines contained the last result, start searching
                // on next line
                startSearch = true;
                if (i == 0) {
                    lastSearchPos = null;
                }
                continue;
            }
            if (!startSearch) {
                continue;
            }
            int startOffset = element.getStartOffset();
            int endOffset = element.getEndOffset() - 1;
            int length = endOffset - startOffset;
            try {
                String text = doc.getText(startOffset, length);
                if (StringUtil.toLowerCase(text).contains(searchText)) {
                    //this.setCaretPosition(startOffset);
                    //this.moveCaretPosition(endOffset);
                    doc.setCharacterAttributes(startOffset, length, styles.searchResult(false), false);
                    scrollManager.cancelScrollDownRequest();
                    scrollManager.scrollToOffset(startOffset);
                    //System.out.println(text);
//                    if (i == 0) {
//                        lastSearchPos = null;
//                    } else {
                        lastSearchPos = element;
//                    }
                    break;
                }
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location");
            }
            lastSearchPos = null;
        }
        if (lastSearchPos == null) {
            scrollManager.scrollDown();
            return false;
        }
        return true;
    }
    
    /**
     * Remove any highlighted search results and start the search from the
     * beginning next time.
     */
    public void resetSearch() {
        clearSearchResult();
        lastSearchPos = null;
    }
    
    /**
     * Removes any prior style changes used to highlight a search result.
     */
    private void clearSearchResult() {
        doc.setCharacterAttributes(0, doc.getLength(), styles.clearSearchResult(), false);
    }

    /**
     * Outputs a clickable and colored nickname.
     * 
     * @param user
     * @param action 
     * @param whisper 
     * @param id 
     */
    private void printUser(User user, boolean action,
            boolean whisper, String id) {
        
        // Decide on name based on settings and available names
        String userName;
        if (user.hasCustomNickSet()) {
            userName = user.getCustomNick();
        }
        else if (styles.namesMode() == SettingsManager.DISPLAY_NAMES_MODE_USERNAME) {
            userName = user.getName();
        }
        else if (styles.namesMode() != SettingsManager.DISPLAY_NAMES_MODE_CAPITALIZED
                || user.hasRegularDisplayNick()) {
            userName = user.getDisplayNick();
        }
        else {
            userName = user.getName();
        }
        
//        if (user.hasCustomNickSet()
//                || styles.namesMode() != SettingsManager.NAMES_MODE_USERNAME 
//                || (user.hasRegularDisplayNick()
//                    || styles.namesMode() != SettingsManager.NAMES_MODE_CAPITALIZED)) {
//            userName = user.getCustomNick();
//        } else {
//            userName = user.getNick();
//        }
        
        // Badges or Status Symbols
        if (styles.showUsericons()) {
            printUserIcons(user);
        }
        else {
            userName = user.getModeSymbol()+userName;
        }
        
        // Output name
        if (user.hasCategory("rainbow")) {
            printRainbowUser(user, userName, action, SpecialColor.RAINBOW, id);
        } else if (user.hasCategory("golden")) {
            printRainbowUser(user, userName, action, SpecialColor.GOLD, id);
        } else {
            MutableAttributeSet style = styles.nick(user, null);
            if (id != null) {
                style.addAttribute(Attribute.ID, id);
            }
            if (whisper) {
                if (action) {
                    print(">>["+userName + "]", style);
                } else {
                    print("-["+userName + "]-", style);
                }
            } else if (action) {
                print("* " + userName, style);
            } else {
                print(userName, style);
            }
        }
        
        // Add username in parentheses behind, if necessary
        if (!user.hasRegularDisplayNick() && !user.hasCustomNickSet()
                && styles.namesMode() == SettingsManager.DISPLAY_NAMES_MODE_BOTH) {
            MutableAttributeSet style = styles.nick(user, null);
            StyleConstants.setBold(style, false);
            int fontSize = StyleConstants.getFontSize(style) - 2;
            if (fontSize <= 0) {
                fontSize = StyleConstants.getFontSize(style);
            }
            StyleConstants.setFontSize(style, fontSize);
            print(" ("+user.getName()+")", style);
        }
        
        // Finish up
        // Requires user style because it needs the metadata to detect the end
        // of the nick when deleting messages (and possibly other stuff)
        if (!action && !whisper) {
            print(": ", styles.nick(user, null));
        } else {
            print(" ", styles.nick(user, null));
        }
    }
    
    private enum SpecialColor { RAINBOW, GOLD }
    
    /**
     * Output the username in rainbow colors. This means each character has to
     * be output on it's own, while changing the color. One style with the
     * appropriate User metadata is used and the color changed.
     * 
     * Prints the rest (what doesn't belong to the nick itself) based on the
     * default user style.
     * 
     * @param user
     * @param userName The username to actually output. This also depends on
     * whether badges are output or if the prefixes should be output.
     * @param action 
     */
    private void printRainbowUser(User user, String userName, boolean action,
            SpecialColor type, String id) {
        SimpleAttributeSet userStyle = new SimpleAttributeSet(styles.nick());
        userStyle.addAttribute(Attribute.IS_USER_MESSAGE, true);
        userStyle.addAttribute(Attribute.USER, user);
        if (id != null) {
            userStyle.addAttribute(Attribute.ID, id);
        }

        int length = userName.length();
        if (action) {
            print("* ", styles.nick());
        }
        for (int i=0;i<length;i++) {
            Color c;
            if (type == SpecialColor.RAINBOW) {
                c = makeRainbowColor(i, length);
            } else {
                c = makeGoldColor(i, length);
            }
            StyleConstants.setForeground(userStyle, c);
            print(userName.substring(i, i+1), userStyle);
        }
    }
    
    private Color makeRainbowColor(int i, int length) {
        double step = 2*Math.PI / length;
        double delta = 2 * Math.PI / 3;
        
        int r = (int) (Math.cos(i * step + 0 * delta) * 127.5 + 127.5);
        int g = (int) (Math.cos(i * step + 1 * delta) * 127.5 + 127.5);
        int b = (int) (Math.cos(i * step + 2 * delta) * 110 + 110);

        return new Color(r, g, b);
    }
    
    private Color makeGoldColor(int i, int length) {
        double step = Math.PI*2 / length;
        double delta = Math.PI*1.15;
        
        int r = 255;
        int g = (int) (Math.cos(i * step + 1 * delta) * 42 + 195);
        int b = 0;

        return new Color(r, g, b);
    }
    
    /**
     * Prints the icons for the given User.
     * 
     * @param user 
     */
    private void printUserIcons(User user) {
        java.util.List<Usericon> badges = user.getBadges(styles.botBadgeEnabled());
        if (badges != null) {
            for (Usericon badge : badges) {
                if (badge.image != null && !badge.removeBadge) {
                    print(badge.getSymbol(), styles.makeIconStyle(badge));
                }
            }
        }
    }
    
    /**
     * Removes some chat lines from the top, depending on the current
     * scroll position.
     */
    private void clearSomeChat() {
        if (scrollManager.fixedChat) {
            return;
        }
        if (!scrollManager.isScrollPositionNearEnd()) {
            return;
        }
        int count = doc.getDefaultRootElement().getElementCount();
        int max = styles.bufferSize();
        if (count > max) {
            removeFirstLines(2);
        }
    }

    /**
     * Removes the specified amount of lines from the top (oldest messages).
     * 
     * @param amount 
     */
    public void removeFirstLines(int amount) {
        if (amount < 1) {
            amount = 1;
        }
        if (doc.getDefaultRootElement().getElementCount() == 0) {
            return;
        }
        Element firstToRemove = doc.getDefaultRootElement().getElement(0);
        Element lastToRemove = doc.getDefaultRootElement().getElement(amount - 1);
        //System.out.println(firstToRemove+" "+lastToRemove);
        int startOffset = firstToRemove.getStartOffset();
        int endOffset = lastToRemove.getEndOffset();
        if (endOffset > doc.getLength()) {
            endOffset = doc.getLength();
        }
        //System.out.println(startOffset+" "+endOffset+" "+doc.getLength());
        try {
            doc.remove(startOffset,endOffset);
        } catch (BadLocationException ex) {
            //Logger.getLogger(ChannelTextPane.class.getName()).log(Level.SEVERE, ex.toString(), ex);
        }
   }
    
    public void removeOldLines() {
        if (messageTimeout > 0) {
            Element element = doc.getDefaultRootElement().getElement(0).getElement(0);
            if (element != null && getTimeAgo(element) > messageTimeout * 1000) {
                //System.out.println(getTimeAgo(element));
                removeFirstLines(1);
                scrollDownIfNecessary();
                resetNewlineRequired();
            }
        }
    }
    
    public void clearAll() {
        try {
            doc.remove(0, doc.getLength());
            resetNewlineRequired();
        } catch (BadLocationException ex) {
            Logger.getLogger(ChannelTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void resetNewlineRequired() {
        if (doc.getLength() == 0) {
            newlineRequired = false;
        }
    }
    
    /**
     * Prints something in compact mode, meaning that nick events of the same
     * type appear in the same line, for as long as possible.
     * 
     * This is mainly used for a compact way of printing joins/parts/mod/unmod.
     * 
     * @param type 
     * @param user 
     */
    public void printCompact(String type, User user) {
        String seperator = ", ";
        if (startCompactMode(type)) {
            // If compact mode has actually been started for this print,
            // print prefix first
            print(getTimePrefix(), styles.compact());
            print(type+": ", styles.compact());
            seperator = "";
        }
        print(seperator, styles.compact());
        print(user.getCustomNick(), styles.nick(user, styles.compact()));
        
        compactModeLength++;
        // If max number of compact prints happened, close compact mode to
        // start a new line
        if (compactModeLength >= MAX_COMPACTMODE_LENGTH) {
            closeCompactMode();
        }
    }
    
    /**
     * Enters compact mode, closes it first if necessary.
     *
     * @param type
     * @return
     */
    private boolean startCompactMode(String type) {
        
        // Check if max time has passed, and if so close first
        long timePassed = System.currentTimeMillis() - compactModeStart;
        if (timePassed > MAX_COMPACTMODE_TIME) {
            closeCompactMode();
        }

        // If this is another type, close first
        if (!type.equals(compactMode)) {
            closeCompactMode();
        }
        
        // Only start if not already/still going
        if (compactMode == null) {
            compactMode = type;
            compactModeStart = System.currentTimeMillis();
            compactModeLength = 0;
            return true;
        }
        return false;
    }
    
    /**
     * Leaves compact mode (if necessary).
     */
    protected void closeCompactMode() {
        if (compactMode != null) {
            printNewline();
            compactMode = null;
        }
    }
    
    /*
     * ########################
     * # General purpose print
     * ########################
     */
    
//    private static Highlighter.HighlightPainter painter = new TestPainter();
    
    /**
     * Start the next print with a newline. This must be called when the current
     * line is finished.
     */
    protected void printNewline() {
        newlineRequired = true;
        lineSelection.onLineAdded(getLastLine(doc));
//        try {
//            getHighlighter().addHighlight(doc.getLength(), doc.getLength(), painter);
//        } catch (BadLocationException ex) {
//            Logger.getLogger(ChannelTextPane.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    
   /**
     * Prints a regular-styled line (ended with a newline).
     * @param line 
     */
    public void printLine(String line) {
        printLine(line, styles.info());
    }

    /**
     * Prints a line in the given style (ended with a newline).
     * @param line
     * @param style 
     */
    private void printLine(String line, MutableAttributeSet style) {
        // Close compact mode, because this is definately a new line (timestamp)
        closeCompactMode();
        print(getTimePrefix()+line,style);
        printNewline();
    }

    /**
     * Print special stuff in the text like links and emoticons differently.
     * 
     * First a map of all special stuff that can be found in the text is built,
     * in a way that stuff doesn't overlap with previously found stuff.
     * 
     * Then all the special stuff in this map is printed accordingly, while
     * printing the stuff inbetween with regular style.
     * 
     * @param text 
     * @param user 
     * @param style 
     * @param emotes 
     * @param ignoreLinks 
     */
    protected void printSpecials(String text, User user, MutableAttributeSet style,
            TagEmotes emotes, boolean ignoreLinks, boolean containsBits) {
        // Where stuff was found
        TreeMap<Integer,Integer> ranges = new TreeMap<>();
        // The style of the stuff (basicially metadata)
        HashMap<Integer,MutableAttributeSet> rangesStyle = new HashMap<>();
        
        if (!ignoreLinks) {
            findLinks(text, ranges, rangesStyle);
        }
        
        if (styles.showEmoticons()) {
            findEmoticons(text, user, ranges, rangesStyle, emotes);
            if (containsBits) {
                findBits(main.emoticons.getCheerEmotes(), text, ranges, rangesStyle, user);
            }
        }
        
        // Actually print everything
        int lastPrintedPos = 0;
        Iterator<Entry<Integer, Integer>> rangesIt = ranges.entrySet().iterator();
        while (rangesIt.hasNext()) {
            Entry<Integer, Integer> range = rangesIt.next();
            int start = range.getKey();
            int end = range.getValue();
            if (start > lastPrintedPos) {
                // If there is anything between the special stuff, print that
                // first as regular text
                String processed = processText(user, text.substring(lastPrintedPos, start));
                print(processed, style);
            }
            print(text.substring(start, end + 1),rangesStyle.get(start));
            lastPrintedPos = end + 1;
        }
        // If anything is left, print that as well as regular text
        if (lastPrintedPos < text.length()) {
            print(processText(user, text.substring(lastPrintedPos)), style);
        }
        
    }
    
    /**
     * This has to be done after parsing for emote tags, so the offsets send by
     * the server fit the text worked on here.
     * 
     * @param text
     * @return 
     */
    private String processText(User user, String text) {
        String result = Helper.htmlspecialchars_decode(text);
        result = StringUtil.removeDuplicateWhitespace(result);
        int filterMode = styles.filterCombiningCharacters();
        if (filterMode > Helper.FILTER_COMBINING_CHARACTERS_OFF) {
            String prev = result;
            result = Helper.filterCombiningCharacters(result, "****", filterMode);
            if (!prev.equals(result)) {
                //LOGGER.info("Filtered combining characters: [" + prev + "] -> [" + result + "]");
                LOGGER.info("Filtered combining characters from message by "+user.getRegularDisplayNick()+" [result: "+result+"]");
            }
        }
        return result;
    }
    
    private void findLinks(String text, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle) {
        // Find links
        urlMatcher.reset(text);
        while (urlMatcher.find()) {
            int start = urlMatcher.start();
            int end = urlMatcher.end() - 1;
            if (!inRanges(start, ranges) && !inRanges(end,ranges)) {
                String foundUrl = urlMatcher.group();
                
                // Check if URL contains ( ) like http://example.com/test(abc)
                // or is just contained in ( ) like (http://example.com)
                // (of course this won't work perfectly, but it should be ok)
                if (foundUrl.endsWith(")") && !foundUrl.contains("(")) {
                    foundUrl = foundUrl.substring(0, foundUrl.length() - 1);
                    end--;
                }
                if (checkUrl(foundUrl)) {
                    ranges.put(start, end);
                    if (!foundUrl.startsWith("http")) {
                        foundUrl = "http://"+foundUrl;
                    }
                    rangesStyle.put(start, styles.url(foundUrl));
                }
            }
        }
    }
    
    
    private void findEmoticons(String text, User user, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle, TagEmotes tagEmotes) {
        
        findEmoticons(user, main.emoticons.getCustomEmotes(), text, ranges, rangesStyle);
        findEmoticons(user, main.emoticons.getEmoji(), text, ranges, rangesStyle);
        
        if (tagEmotes != null) {
            // Add emotes from tags
            Map<Integer, Emoticon> emoticonsById = main.emoticons.getEmoticonsById();
            addTwitchTagsEmoticons(user, emoticonsById, text, ranges, rangesStyle, tagEmotes);
        }
        
        // Emoteset based
        for (Integer set : user.getEmoteSet()) {
            HashSet<Emoticon> emoticons = main.emoticons.getEmoticons(set);
            findEmoticons(emoticons, text, ranges, rangesStyle);
        }
        
        // Global emotes
        if (tagEmotes == null) {
            Set<Emoticon> emoticons = main.emoticons.getGlobalTwitchEmotes();
            findEmoticons(emoticons, text, ranges, rangesStyle);
        }
        Set<Emoticon> emoticons = main.emoticons.getOtherGlobalEmotes();
        findEmoticons(emoticons, text, ranges, rangesStyle);
        
        // Channel based (may also have a emoteset restriction)
        HashSet<Emoticon> channelEmotes = main.emoticons.getEmoticons(user.getStream());
        findEmoticons(user, channelEmotes, text, ranges, rangesStyle);
    }
    
    /**
     * Adds the emoticons from the Twitch IRCv3 tags.
     * 
     * @param emoticons Map of emotes associated with Twitch emote id
     * @param text The message text
     * @param ranges The ranges for this message
     * @param rangesStyle The styles for this message
     * @param emotesDef The emotes definition from the IRCv3 tags
     */
    private void addTwitchTagsEmoticons(User user, Map<Integer, Emoticon> emoticons, String text,
            Map<Integer, Integer> ranges, Map<Integer, MutableAttributeSet> rangesStyle,
            TagEmotes emotesDef) {
        if (emotesDef == null) {
            return;
        }
        Map<Integer, Emoticons.TagEmote> def = emotesDef.emotes;
        
        /**
         * Iterate over each character of the message and check if an emote starts
         * at the current position.
         * 
         * The offset is used to handle supplemantary characters that consist
         * of two UTF-16 characters. Twitch Chat sees these as only one character
         * so that has to be corrected.
         * 
         * https://discuss.dev.twitch.tv/t/jtv-2-receiving-messages/1635/10
         * 
         * Example message: "Kappa 𠜎 Kappa"
         */
        int offset = 0;
        for (int i=0;i<text.length();) {
            
            if (def.containsKey(i-offset)) {
                // An emote starts at the current position, so add it.
                Emoticons.TagEmote emoteData = def.get(i-offset);
                int id = emoteData.id;
                int start = i;
                int end = emoteData.end+offset;
                
                // Get and check emote
                Emoticon emoticon = null;
                Emoticon customEmote = main.emoticons.getCustomEmoteById(id);
                if (customEmote != null && customEmote.allowedForStream(user.getStream())) {
                    emoticon = customEmote;
                } else {
                    emoticon = emoticons.get(id);
                }
                if (end < text.length()) {
                    if (emoticon == null) {
                        /**
                         * Add emote from message alone
                         */
                        String code = text.substring(start, end+1);
                        String url = Emoticon.getTwitchEmoteUrlById(id, 1);
                        Emoticon.Builder b = new Emoticon.Builder(
                                Emoticon.Type.TWITCH, code, url);
                        b.setNumericId(id);
                        Emoteset emotesetInfo = main.emoticons.getInfoByEmoteId(id);
                        if (emotesetInfo != null) {
                            b.setEmoteset(emotesetInfo.emoteset_id);
                            b.setStream(emotesetInfo.stream);
                            b.setEmotesetInfo(emotesetInfo.product);
                        } else {
                            b.setEmoteset(Emoticon.SET_UNKNOWN);
                        }
                        emoticon = b.build();
                        main.emoticons.addTempEmoticon(emoticon);
                        LOGGER.info("Added emote from message: "+emoticon);
                    }
                    if (!main.emoticons.isEmoteIgnored(emoticon)) {
                        addEmoticon(emoticon, start, end, ranges, rangesStyle);
                    }
                }
            }
            /**
             * If the current position in the String consists of an character
             * thats more than one long (some Unicode characters), then add to
             * the offset and jump ahead accordingly.
             */
            offset += Character.charCount(text.codePointAt(i))-1;
            i += Character.charCount(text.codePointAt(i));
        }
    }
    
    private void findEmoticons(Set<Emoticon> emoticons, String text,
            Map<Integer, Integer> ranges, Map<Integer, MutableAttributeSet> rangesStyle) {
        findEmoticons(null, emoticons, text, ranges, rangesStyle);
    }
    
    private void findEmoticons(User user, Set<Emoticon> emoticons, String text,
            Map<Integer, Integer> ranges, Map<Integer, MutableAttributeSet> rangesStyle) {
        // Find emoticons
        for (Emoticon emoticon : emoticons) {
            // Check the text for every single emoticon
            if (!emoticon.matchesUser(user)) {
                continue;
            }
            if (main.emoticons.isEmoteIgnored(emoticon)) {
                continue;
            }
            if (emoticon.isAnimated
                    && !styles.isEnabled(Setting.EMOTICONS_SHOW_ANIMATED)) {
                continue;
            }
            Matcher m = emoticon.getMatcher(text);
            while (m.find()) {
                // As long as this emoticon is still found in the text, add
                // it's position (if it doesn't overlap with something already
                // found) and move on
                int start = m.start();
                int end = m.end() - 1;
                addEmoticon(emoticon, start, end, ranges, rangesStyle);
            }
        }
    }
    
    private void findBits(Set<CheerEmoticon> emotes, String text,
            Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle,
            User user) {
        for (CheerEmoticon emote : emotes) {
            if (!emote.matchesUser(user)) {
                // CONTINUE
                continue;
            }
            Matcher m = emote.getMatcher(text);
            while (m.find()) {
                int start = m.start();
                int end = m.end() - 1;
                try {
                    int bits = Integer.parseInt(m.group(1));
                    int bitsLength = m.group(1).length();
                    if (bits < emote.min_bits) {
                        // CONTINUE
                        continue;
                    }
                    boolean ignored = main.emoticons.isEmoteIgnored(emote);
                    if (!ignored && addEmoticon(emote, start, end - bitsLength, ranges, rangesStyle)) {
                        // Add emote
                        addFormattedText(emote.color, end - bitsLength + 1, end, ranges, rangesStyle);
                    } else {
                        // Add just text
                        addFormattedText(emote.color, start, end, ranges, rangesStyle);
                    }
                } catch (NumberFormatException ex) {
                    System.out.println("Error parsing cheer: " + ex);
                }
            }
        }
    }
    
    private boolean addEmoticon(Emoticon emoticon, int start, int end,
            Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle) {
        if (!inRanges(start, ranges) && !inRanges(end, ranges)) {
            if (emoticon.getIcon(this) != null) {
                ranges.put(start, end);
                MutableAttributeSet attr = styles.emoticon(emoticon);
                // Add an extra attribute, making this Style unique
                // (else only one icon will be output if two of the same
                // follow in a row)
                attr.addAttribute("start", start);
                rangesStyle.put(start, attr);
                return true;
            }
        }
        return false;
    }
    
    private void addFormattedText(Color color, int start, int end,
            Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle) {
        if (!inRanges(start, ranges) && !inRanges(end, ranges)) {
            ranges.put(start, end);
            MutableAttributeSet attr = styles.standard(color);
            StyleConstants.setBold(attr, true);
            attr.addAttribute("start", start);
            rangesStyle.put(start,attr);
        }
    }
    
    /**
     * Checks if the given integer is within the range of any of the key=value
     * pairs of the Map (inclusive).
     * 
     * @param i
     * @param ranges
     * @return 
     */
    private boolean inRanges(int i, Map<Integer,Integer> ranges) {
        Iterator<Entry<Integer, Integer>> rangesIt = ranges.entrySet().iterator();
        while (rangesIt.hasNext()) {
            Entry<Integer, Integer> range = rangesIt.next();
            if (i >= range.getKey() && i <= range.getValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the Url can be later used as a URI.
     * 
     * @param uriToCheck
     * @return 
     */
    private boolean checkUrl(String uriToCheck) {
        try {
            new URI(uriToCheck);
        } catch (URISyntaxException ex) {
            return false;
        }
        return true;
    }
    
    public static Element getLastLine(Document doc) {
        return doc.getDefaultRootElement().getElement(doc.getDefaultRootElement().getElementCount() - 1);
    }

    /**
     * Prints the given text in the given style. Runs the function that actually
     * adds the text in the Event Dispatch Thread.
     * 
     * @param text
     * @param style 
     */
    private void print(final String text,final MutableAttributeSet style) {
        try {
            String newline = "";
            if (doc.getLength() == 0 || newlineRequired) {
                style.addAttribute(Attribute.TIMESTAMP, System.currentTimeMillis());
            }
            if (newlineRequired) {
                newline = "\n";
                newlineRequired = false;
                clearSomeChat();
            }
            //System.out.println("1:"+doc.getLength());
            doc.insertString(doc.getLength(), newline+text, style);
            //System.out.println("2:"+doc.getLength());
            //this.getHighlighter().addHighlight(doc.getLength(), 10, null);
            // TODO: check how this works
            doc.setParagraphAttributes(doc.getLength(), 1, styles.paragraph(), true);
            scrollDownIfNecessary();
        } catch (BadLocationException e) {
            System.err.println("BadLocationException");
        }
    }

    private void scrollDownIfNecessary() {
        if (lastSearchPos == null) {
            if (scrollManager.isScrollPositionNearEnd()
                    || scrollManager.scrolledUpTimeout()) {
                /**
                 * This should work fine, however using scrollDown() instead
                 * might not, because it would try to scroll down before the
                 * layout is finished (invokeLater would be necessary).
                 *
                 * Plus this should be better performance for lines that contain
                 * many parts (causing many scroll down requests), for example
                 * many Emotes.
                 */
                scrollManager.requestScrollDown();
            }
        }
    }
    
    
    /**
     * Sets the scrollpane used for this JTextPane. Should be possible to do
     * this more elegantly.
     * 
     * @param scroll
     */
    public void setScrollPane(JScrollPane scroll) {
        scrollManager.setScrollPane(scroll);
        scroll.getVerticalScrollBar().addAdjustmentListener(e -> {
            linkController.updatePopup();
        });
    }
    
    /**
     * Makes the time prefix.
     * 
     * @return 
     */
    protected String getTimePrefix() {
        if (styles.timestampFormat() != null) {
            return DateTime.currentTime(styles.timestampFormat())+" ";
        }
        return "";
    }
    
    public void refreshStyles() {
        styles.refresh();
    }
    
    public void setBufferSize(int size) {
        styles.setBufferSize(size);
    }

    /**
     * Simply uses UrlOpener to prompt the user to open the given URL. The
     * prompt is centered on this object (the text pane).
     * 
     * @param url 
     */
    @Override
    public void linkClicked(String url) {
        UrlOpener.openUrlPrompt(this.getTopLevelAncestor(), url);
    }
    
    /**
     * Handles scrolling down and when to not scroll down.
     */
    private class ScrollManager extends MouseAdapter implements MouseMotionListener {
        
        /**
         * Stop scrolling of chat.
         */
        private boolean fixedChat = false;
        
        /**
         * The key used in conjunction with the mouse to pause scrolling of chat
         * (currently Ctrl).
         */
        private boolean pauseKeyPressed = false;
        
        /**
         * Listen for keys to detect when Ctrl is pressed application wide.
         */
        private final KeyEventDispatcher keyListener;
        
        private Popup popup;
        private JLabel fixedChatInfoLabel;
        private long mouseLastMoved;
        
        private JScrollPane scrollpane;
        
        /**
         * When the scroll position was last changed.
         */
        private long lastManualScrollChange = 0;
        
        private boolean scrollingDownInProgress = false;

        private boolean scrollDownRequest = false;
        
        /**
         * The most recent width/height of the scrollpane, used to determine
         * whether it is decreased.
         */
        private int width;
        private int height;
        
        private final javax.swing.Timer updateTimer;
        
        ScrollManager() {
            updateTimer = new javax.swing.Timer(500, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (System.currentTimeMillis() - mouseLastMoved > 700) {
                        setFixedChat(false);
                    }
                }
            });
            updateTimer.setRepeats(true);
            updateTimer.start();
            
            keyListener = new KeyEventDispatcher() {

                @Override
                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                        if (e.getID() == KeyEvent.KEY_PRESSED) {
                            return handleKeyPressed();
                        } else {
                            return handleKeyReleased();
                        }
                    }
                    return false;
                }
            };
        }
        
        /**
         * Clean up any outside reference to this so it can be garbage
         * collected (also close the info popup if necessary).
         */
        public void cleanUp() {
            KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            kfm.removeKeyEventDispatcher(keyListener);
            hideFixedChatInfo();
            updateTimer.stop();
        }

        public void setScrollPane(JScrollPane pane) {
            this.scrollpane = pane;
            addListeners();
        }
        
        private void addListeners() {
            
            // Listener to detect when the scrollpane was reduced in size, so
            // scrolling down might be necessary
            scrollpane.addComponentListener(new ComponentAdapter() {
                
                @Override
                public void componentResized(ComponentEvent e) {
                    Component c = e.getComponent();
                    if (c.getWidth() < width || c.getHeight() < height) {
                        scrollDown();
                    }
                    width = c.getWidth();
                    height = c.getHeight();
                }
                
            });
            
            // Listener to detect when the scroll position was last changed
            scrollpane.getVerticalScrollBar().addAdjustmentListener(
                    new AdjustmentListener() {
                        
                        private int lastValue = 0;
                        private int lastMax = 0;
                        private int lastExtent = 0;

                        @Override
                        public void adjustmentValueChanged(AdjustmentEvent e) {
                            boolean valueChanged = e.getValue() != lastValue;
                            boolean maxChanged = e.getAdjustable().getMaximum() != lastMax;
                            boolean extentChanged = e.getAdjustable().getVisibleAmount() != lastExtent;
                            
                            lastValue = e.getValue();
                            lastMax = e.getAdjustable().getMaximum();
                            lastExtent = e.getAdjustable().getVisibleAmount();

                            /**
                             * The difficulty here is to decide between manual
                             * scrolling (the user pressed a button/clicked on
                             * the scrollbar) and when an event means scrolling
                             * should occur (e.g. when a line was added).
                             * 
                             * Indications for manual scrolling (or at least
                             * something similiar that isn't the normal "keep
                             * scrolling down"), should be:
                             *
                             * - Scroll position has changed (obviously)
                             * - Maximum and Extent have NOT changed, since
                             *   normal scrolling wouldn't change that, but more
                             *   likely something that would prompt automatic
                             *   scrolling (like adding lines)
                             * - Scrolling wasn't initiated by scrollDown(),
                             *   which would look the same otherwise
                             */
                            boolean manualScrolled = valueChanged
                                            && !maxChanged
                                            && !extentChanged
                                            && !scrollingDownInProgress;
                            if (manualScrolled) {
                                // For scroll timeout, when scrolled up without
                                // changing position for a while
                                lastManualScrollChange = System.currentTimeMillis();
                                
                                // When changing scroll position, chat should
                                // not be fixed anymore (but don't scroll down)
                                fixedChat = false;
                                hideFixedChatInfo();

                                // Any manual scrolling should stop auto scroll
                                scrollDownRequest = false;
                            }
                            if (scrollDownRequest && !scrollingDownInProgress) {
                                scrollDown();
                            }
                            // After scrollDown() execution of the original
                            // event will continue here, so preferably don't
                            // do anything here
                        }
                    });

            KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            kfm.addKeyEventDispatcher(keyListener);
        }
        
        /**
         * Checks if the scroll position is at the end of the document, with
         * some margin or error.
         *
         * @return true if scroll position is at the end, false otherwise
         */
        private boolean isScrollPositionNearEnd() {
            JScrollBar vbar = scrollpane.getVerticalScrollBar();
            return vbar.getMaximum() - 20 <= vbar.getValue() + vbar.getVisibleAmount();
        }

        /**
         * If enabled, checks whether the time that has passed since the scroll
         * position was last changed is greater than the defined timeout.
         *
         * @return {@code true} if the timeout was exceeded, {@code false}
         * otherwise
         */
        private boolean scrolledUpTimeout() {
            if (scrollDownRequest) {
                return false;
            }
            if (fixedChat || pauseKeyPressed) {
                return false;
            }
            if (!styles.autoScroll()) {
                return false;
            }
            long timePassed = System.currentTimeMillis() - lastManualScrollChange;
            if (timePassed > 1000 * styles.autoScrollTimeout()) {
                LOGGER.info("ScrolledUp Timeout (" + timePassed + ")");
                return true;
            }
            return false;
        }
        
        /**
         * Request scrolling down when the vertical scrollbar changes values
         * (e.g. the maximum changes due to lines being added), until manual
         * scrolling occurs or the request is cancelled.
         * 
         * This allows scrolling to occur when it is required, instead of just
         * trying to scroll down immediately after changing the document.
         */
        private void requestScrollDown() {
            scrollDownRequest = true;
        }
        
        /**
         * Cancel the scrolling down request. If already scrolled down, this
         * will prevent any additional scrolling via with method (until
         * requested again).
         */
        private void cancelScrollDownRequest() {
            scrollDownRequest = false;
        }
        
        /**
         * Scrolls to the very end of the document.
         * 
         * This is using setValue() on the scrollbar because it doesn't seem to
         * cause a layout (like modelToView() for getting the position for
         * scrollRectToVisible() would), so it's faster. However most of the
         * time requestScrollDown() would be used anyway (so it doesn't scroll
         * down as often), so if setValue() turns out to not work perfectly
         * further usage of scrollRectToVisible() may be viable as well.
         * 
         * There has been a rare instance of setValue(Integer.MAX_VALUE)
         * scrolling up somehow instead of down, but maybe using getMaximum()
         * works better.
         */
        private void scrollDown() {
            if (fixedChat) {
                return;
            }
            scrollingDownInProgress = true;
            scrollDown1();
            scrollingDownInProgress = false;
        }
        
        private void scrollDown1() {
            scrollpane.getVerticalScrollBar().setValue(scrollpane.getVerticalScrollBar().getMaximum());
        }
        
        private void scrollDown2() {
            scrollRectToVisible(new Rectangle(0,getPreferredSize().height,10,10));
        }
        
        private void scrollDown3() {
            try {
                int endPosition = doc.getLength();
                Rectangle bottom = modelToView(endPosition);
                if (bottom != null) {
                    bottom.height = bottom.height + 100;
                    scrollRectToVisible(bottom);
                }
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad Location");
            }
        }
        
        /**
         * Scrolls to the given offset in the document. Counts as manual
         * scrolling as far as requestScrollDown() is concerned.
         *
         * @param offset
         */
        private void scrollToOffset(int offset) {
            try {
                Rectangle rect = modelToView(offset);
                if (rect != null) {
                    scrollRectToVisible(rect);
                }
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad Location");
            }
        }
        
        public void setFixedChat(boolean fixed) {
            /**
             * Only works if actually scrolling, so ignore otherwise.
             */
            if (!scrollpane.getVerticalScrollBar().isVisible()) {
                return;
            }
            // Check if should scroll down
            if (!fixed && fixedChat) {
                this.fixedChat = fixed;
                scrollDown();
            }
            // Hide or show info
            if (fixed) {
                showFixedChatInfo();
            } else {
                hideFixedChatInfo();
            }
            // Update value either way
            this.fixedChat = fixed;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        }

        /**
         * When the mouse is moved over the area, then stop scrolling (if
         * enabled).
         *
         * @param e 
         */
        @Override
        public void mouseMoved(MouseEvent e) {
            mouseLastMoved = System.currentTimeMillis();
            if (isPauseEnabled() && isScrollPositionNearEnd()) {
                setFixedChat(true);
            }
        }
        
        /**
         * Only allow chat pause if enabled altogether and if Ctrl isn't
         * required to be pressed or it's pressed anyway.
         * 
         * @return true if chat pause can be enabled, false otherwise
         */
        private boolean isPauseEnabled() {
            if (!styles.isEnabled(Setting.PAUSE_ON_MOUSEMOVE)) {
                return false;
            }
            return !styles.isEnabled(Setting.PAUSE_ON_MOUSEMOVE_CTRL_REQUIRED)
                    || pauseKeyPressed;
        }
        
        /**
         * Disable fixed chat when mouse leaves the area.
         * 
         * @param e 
         */
        @Override
        public void mouseExited(MouseEvent e) {
            if (!pauseKeyPressed) {
                setFixedChat(false);
            }
        }
        
        private boolean handleKeyPressed() {
            pauseKeyPressed = true;
            if (fixedChat) {
                mouseLastMoved = System.currentTimeMillis();
                return true;
            }
            return false;
        }
        
        private boolean handleKeyReleased() {
            pauseKeyPressed = false;
            return false;
        }
        
        private void createFixedChatInfoLabel() {
            JLabel label = new JLabel("chat paused");
            Border border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(2, 4, 2, 4));
            label.setBorder(border);
            label.setForeground(Color.BLACK);
            label.setBackground(HtmlColors.decode("#EEEEEE"));
            label.setOpaque(true);
            fixedChatInfoLabel = label;
        }
        
        private void showFixedChatInfo() {
            if (popup == null) {
                if (fixedChatInfoLabel == null) {
                    createFixedChatInfoLabel();
                }
                JLabel label = fixedChatInfoLabel;
                Point p = scrollpane.getLocationOnScreen();
                int labelWidth = label.getPreferredSize().width;
                p.x += scrollpane.getViewport().getWidth() - labelWidth - 5;
                popup = PopupFactory.getSharedInstance().getPopup(
                        ChannelTextPane.this, label, p.x, p.y);
                popup.show();
            }
        }
        
        private void hideFixedChatInfo() {
            if (popup != null) {
                popup.hide();
                popup = null;
            }
        }
    }
    
    /**
     * Manages everything to do with styles (AttributeSets).
     */
    public class Styles {
        /**
         * Styles that are get from the StyleServer
         */
        private final String[] baseStyles = new String[]{"standard","special","info","base","highlight","paragraph"};
        /**
         * Stores the styles
         */
        private final HashMap<String,MutableAttributeSet> styles = new HashMap<>();
        /**
         * Stores immutable/unmodified copies of the styles got from the
         * StyleServer for comparison
         */
        private final HashMap<String,AttributeSet> rawStyles = new HashMap<>();
        /**
         * Stores boolean settings
         */
        private final HashMap<Setting, Boolean> settings = new HashMap<>();
        
        private final Map<Setting, Integer> numericSettings = new HashMap<>();
        /**
         * Stores all the style types that were changed in the most recent update
         */
        private final ArrayList<String> changedStyles = new ArrayList<>();
        /**
         * Key for the style type attribute
         */
        private final String TYPE = "ChannelTextPanel Style Type";
        /**
         * Store the timestamp format
         */
        private SimpleDateFormat timestampFormat;
        
        private int bufferSize = -1;

        /**
         * Icons that have been modified for use and saved into a style. Should
         * only be done once per icon.
         */
        private final HashMap<Usericon, MutableAttributeSet> savedIcons = new HashMap<>();
        
        /**
         * Creates a new ImageIcon based on the given ImageIcon that has a small
         * space on the side, so it can be displayed properly.
         * 
         * @param icon
         * @return 
         */
        private ImageIcon addSpaceToIcon(ImageIcon icon) {
            int width = icon.getIconWidth();
            int height = icon.getIconHeight();
            int hspace = 3;
            BufferedImage res = new BufferedImage(width + hspace, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = res.getGraphics();
            g.drawImage(icon.getImage(), 0, 0, null);
            g.dispose();

            return new ImageIcon(res);
        }
        
        /**
         * Get the current styles from the StyleServer and also set some
         * other special styles based on that.
         * 
         * @return 
         */
        public boolean setStyles() {
            changedStyles.clear();
            boolean somethingChanged = false;
            for (String styleName : baseStyles) {
                if (loadStyle(styleName)) {
                    somethingChanged = true;
                    changedStyles.add(styleName);
                }
            }
            
            // Additional styles
            SimpleAttributeSet nick = new SimpleAttributeSet(base());
            StyleConstants.setBold(nick, true);
            styles.put("nick", nick);
            
            MutableAttributeSet paragraph = styles.get("paragraph");
            //StyleConstants.setLineSpacing(paragraph, 0.3f);
            paragraph.addAttribute(Attribute.DELETED_LINE, false);
            styles.put("paragraph", paragraph);
            
            SimpleAttributeSet deleted = new SimpleAttributeSet();
            StyleConstants.setStrikeThrough(deleted, true);
            StyleConstants.setUnderline(deleted, false);
            deleted.addAttribute(Attribute.URL_DELETED, true);
            styles.put("deleted", deleted);
            
            SimpleAttributeSet deletedLine = new SimpleAttributeSet();
            deletedLine.addAttribute(Attribute.DELETED_LINE, true);
            //StyleConstants.setAlignment(deletedLine, StyleConstants.ALIGN_RIGHT);
            styles.put("deletedLine", deletedLine);
            
            SimpleAttributeSet searchResult = new SimpleAttributeSet();
            StyleConstants.setBackground(searchResult, styleServer.getColor("searchResult"));
            StyleConstants.setItalic(searchResult, false);
            styles.put("searchResult", searchResult);
            
            SimpleAttributeSet searchResult2 = new SimpleAttributeSet();
            StyleConstants.setBackground(searchResult2, styleServer.getColor("searchResult2"));
            styles.put("searchResult2", searchResult2);
            
            SimpleAttributeSet clearSearchResult = new SimpleAttributeSet();
            StyleConstants.setBackground(clearSearchResult, new Color(0,0,0,0));
            StyleConstants.setItalic(clearSearchResult, false);
            styles.put("clearSearchResult", clearSearchResult);
            
            setBackground(styleServer.getColor("background"));
            
            // Load other stuff from the StyleServer
            setSettings();
            
            return somethingChanged;
        }
        
        /**
         * Loads some settings from the StyleServer.
         */
        private void setSettings() {
            addSetting(Setting.EMOTICONS_ENABLED,true);
            addSetting(Setting.USERICONS_ENABLED, true);
            addSetting(Setting.TIMESTAMP_ENABLED, true);
            addSetting(Setting.SHOW_BANMESSAGES, false);
            addSetting(Setting.AUTO_SCROLL, true);
            addSetting(Setting.DELETE_MESSAGES, false);
            addSetting(Setting.ACTION_COLORED, false);
            addSetting(Setting.COMBINE_BAN_MESSAGES, true);
            addSetting(Setting.BAN_DURATION_APPENDED, true);
            addSetting(Setting.BAN_REASON_APPENDED, true);
            addSetting(Setting.BAN_DURATION_MESSAGE, true);
            addSetting(Setting.BAN_REASON_MESSAGE, true);
            addSetting(Setting.BOT_BADGE_ENABLED, true);
            addSetting(Setting.PAUSE_ON_MOUSEMOVE, true);
            addSetting(Setting.PAUSE_ON_MOUSEMOVE_CTRL_REQUIRED, false);
            addSetting(Setting.EMOTICONS_SHOW_ANIMATED, false);
            addSetting(Setting.COLOR_CORRECTION, true);
            addSetting(Setting.SHOW_TOOLTIPS, true);
            addNumericSetting(Setting.FILTER_COMBINING_CHARACTERS, 1, 0, 2);
            addNumericSetting(Setting.DELETED_MESSAGES_MODE, 30, -1, 9999999);
            addNumericSetting(Setting.BUFFER_SIZE, 250, BUFFER_SIZE_MIN, BUFFER_SIZE_MAX);
            addNumericSetting(Setting.AUTO_SCROLL_TIME, 30, 5, 1234);
            addNumericSetting(Setting.EMOTICON_MAX_HEIGHT, 200, 0, 300);
            addNumericSetting(Setting.EMOTICON_SCALE_FACTOR, 100, 1, 200);
            addNumericSetting(Setting.DISPLAY_NAMES_MODE, 0, 0, 10);
            timestampFormat = styleServer.getTimestampFormat();
            linkController.setPopupEnabled(settings.get(Setting.SHOW_TOOLTIPS));
        }
        
        /**
         * Gets a single boolean setting from the StyleServer.
         * 
         * @param key
         * @param defaultValue 
         */
        private void addSetting(Setting key, boolean defaultValue) {
            MutableAttributeSet loadFrom = styleServer.getStyle("settings");
            Object obj = loadFrom.getAttribute(key);
            boolean result = defaultValue;
            if (obj != null && obj instanceof Boolean) {
                result = (Boolean)obj;
            }
            settings.put(key, result);
        }
        
        private void addNumericSetting(Setting key, int defaultValue, int min, int max) {
            MutableAttributeSet loadFrom = styleServer.getStyle("settings");
            Object obj = loadFrom.getAttribute(key);
            int result = defaultValue;
            if (obj != null && obj instanceof Number) {
                result = ((Number)obj).intValue();
            }
            if (result > max) {
                result = max;
            }
            if (result < min) {
                result = min;
            }
            numericSettings.put(key, result);
        }
        
        /**
         * Retrieves a single style from the StyleServer and compares it to
         * the previously saved style.
         * 
         * @param name
         * @return true if the style has changed, false otherwise
         */
        private boolean loadStyle(String name) {
            MutableAttributeSet newStyle = styleServer.getStyle(name);
            AttributeSet oldStyle = rawStyles.get(name);
            if (oldStyle != null && oldStyle.isEqual(newStyle)) {
                // Nothing in the style has changed, so nothing further to do
                return false;
            }
            // Save immutable copy of new style for next comparison
            rawStyles.put(name, newStyle.copyAttributes());
            // Add type attribute to the style, so it can be recognized when
            // refreshing the styles in the document
            newStyle.addAttribute(TYPE, name);
            styles.put(name, newStyle);
            return true;
        }
        
        /**
         * Retrieves the current styles and updates the elements in the document
         * as necessary. Scrolls down since font size changes and such could
         * move the scroll position.
         */
        public void refresh() {
            if (!setStyles()) {
                return;
            }

            LOGGER.info("Update styles (only types "+changedStyles+")");
            Element root = doc.getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                Element line = root.getElement(i);
                for (int j = 0; j < line.getElementCount(); j++) {
                    Element element = line.getElement(j);
                    String type = (String)element.getAttributes().getAttribute(TYPE);
                    int start = element.getStartOffset();
                    int end = element.getEndOffset();
                    int length = end - start;
                    if (type == null) {
                        type = "base";
                    }
                    MutableAttributeSet style = styles.get(type);
                    // Only change style if this style type was different from
                    // the previous one
                    // (seems to be faster than just setting all styles)
                    if (changedStyles.contains(type)) {
                        if (type.equals("paragraph")) {
                            //doc.setParagraphAttributes(start, length, rawStyles.get(type), false);
                        } else {
                            doc.setCharacterAttributes(start, length, style, false);
                        }
                    }
                }
            }
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    scrollManager.scrollDown();
                }
            });
        }
        
        public MutableAttributeSet base() {
            return styles.get("base");
        }
        
        public MutableAttributeSet info() {
            return styles.get("info");
        }
        
        public MutableAttributeSet compact() {
            return styles.get("special");
        }
        
        public MutableAttributeSet standard(Color color) {
            if (color != null) {
                SimpleAttributeSet specialColor = new SimpleAttributeSet(standard());
                StyleConstants.setForeground(specialColor, color);
                return specialColor;
            }
            return standard();
        }
        
        public MutableAttributeSet standard() {
            return styles.get("standard");
        }
        
        public MutableAttributeSet banMessage(User user, String message) {
            MutableAttributeSet style = new SimpleAttributeSet(standard());
            style.addAttribute(Attribute.IS_BAN_MESSAGE, user);
            style.addAttribute(Attribute.TIMESTAMP, System.currentTimeMillis());
            style.addAttribute(Attribute.BAN_MESSAGE, message);
            return style;
        }
        
        public MutableAttributeSet banMessageCount(int count) {
            MutableAttributeSet style = new SimpleAttributeSet(info());
            style.addAttribute(Attribute.BAN_MESSAGE_COUNT, count);
            return style;
        }
        
        public MutableAttributeSet nick() {
            return styles.get("nick");
        }
        
        public MutableAttributeSet deleted() {
            return styles.get("deleted");
        }
        
        public MutableAttributeSet deletedLine() {
            return styles.get("deletedLine");
        }
        
        public MutableAttributeSet paragraph() {
            //System.out.println(styles.get("paragraph"));
            //styles.get("paragraph").addAttribute(Attribute.TIMESTAMP, System.currentTimeMillis());
            return styles.get("paragraph");
        }
        
        public MutableAttributeSet highlight(Color color) {
            if (color != null) {
                SimpleAttributeSet specialColor = new SimpleAttributeSet(highlight());
                StyleConstants.setForeground(specialColor, color);
                return specialColor;
            }
            return highlight();
        }
        
        public MutableAttributeSet highlight() {
            return styles.get("highlight");
        }
        
        public MutableAttributeSet searchResult(boolean italic) {
            StyleConstants.setItalic(styles.get("searchResult"), italic);
            return styles.get("searchResult");
        }
        
        public MutableAttributeSet searchResult2(boolean italic) {
            StyleConstants.setItalic(styles.get("searchResult2"), italic);
            return styles.get("searchResult2");
        }
        
        public MutableAttributeSet clearSearchResult() {
            return styles.get("clearSearchResult");
        }
        
        /**
         * Makes a style for the given User, containing the User-object itself
         * and the user-color. Changes the color to hopefully improve readability.
         * 
         * @param user The User-object to base this style on
         * @param style Attributes to base the user style on
         * @return 
         */
        public MutableAttributeSet nick(User user, MutableAttributeSet style) {
            SimpleAttributeSet userStyle;
            if (style == null) {
                userStyle = new SimpleAttributeSet(nick());
                userStyle.addAttribute(Attribute.IS_USER_MESSAGE, true);
                Color userColor = user.getColor();
                // Only correct color if no custom color is defined
                if (!user.hasCustomColor() && isEnabled(Setting.COLOR_CORRECTION)) {
                    userColor = HtmlColors.correctReadability(userColor, getBackground());
                    user.setCorrectedColor(userColor);
                }
                StyleConstants.setForeground(userStyle, userColor);
            }
            else {
                userStyle = new SimpleAttributeSet(style);
            }
            userStyle.addAttribute(Attribute.USER, user);
            return userStyle;
        }
        
        /**
         * Creates a style for the given icon. Also modifies the icon to add a
         * little space on the side, so it can be displayed easier. It caches
         * styles, so it only needs to create the style and modify the icon
         * once.
         * 
         * @param icon
         * @return The created style (or read from the cache)
         */
        public MutableAttributeSet makeIconStyle(Usericon icon) {
            MutableAttributeSet style = savedIcons.get(icon);
            if (style == null) {
                //System.out.println("Creating icon style: "+icon);
                style = new SimpleAttributeSet(nick());
                if (icon != null && icon.image != null) {
                    StyleConstants.setIcon(style, addSpaceToIcon(icon.image));
                    style.addAttribute(Attribute.USERICON, icon);
                }
                savedIcons.put(icon, style);
            }
            return style;
        }

        public boolean showTimestamp() {
            return settings.get(Setting.TIMESTAMP_ENABLED);
        }
        
        public boolean showUsericons() {
            return settings.get(Setting.USERICONS_ENABLED);
        }
        
        public boolean showEmoticons() {
            return settings.get(Setting.EMOTICONS_ENABLED);
        }
        
        public int emoticonMaxHeight() {
            return numericSettings.get(Setting.EMOTICON_MAX_HEIGHT);
        }
        
        public float emoticonScaleFactor() {
            return (float)(numericSettings.get(Setting.EMOTICON_SCALE_FACTOR) / 100.0);
        }
        
        public boolean botBadgeEnabled() {
            return settings.get(Setting.BOT_BADGE_ENABLED);
        }
        
        public int filterCombiningCharacters() {
            return numericSettings.get(Setting.FILTER_COMBINING_CHARACTERS);
        }
        
        public boolean showBanMessages() {
            return settings.get(Setting.SHOW_BANMESSAGES);
        }
        
        public boolean combineBanMessages() {
            return settings.get(Setting.COMBINE_BAN_MESSAGES);
        }
        
        public boolean autoScroll() {
            return settings.get(Setting.AUTO_SCROLL);
        }
        
        public Integer autoScrollTimeout() {
            return numericSettings.get(Setting.AUTO_SCROLL_TIME);
        }
        
        public boolean actionColored() {
            return settings.get(Setting.ACTION_COLORED);
        }
        
        public boolean deleteMessages() {
            return settings.get(Setting.DELETE_MESSAGES);
        }

        public int deletedMessagesMode() {
            return (int)numericSettings.get(Setting.DELETED_MESSAGES_MODE);
        }
        
        public boolean isEnabled(Setting setting) {
            return settings.get(setting);
        }
        
        /**
         * Make a link style for the given URL.
         * 
         * @param url
         * @return 
         */
        public MutableAttributeSet url(String url) {
            SimpleAttributeSet urlStyle = new SimpleAttributeSet(standard());
            StyleConstants.setUnderline(urlStyle, true);
            urlStyle.addAttribute(HTML.Attribute.HREF, url);
            return urlStyle;
        }
        
        /**
         * Make a style with the given icon.
         * 
         * @param emoticon
         * @return 
         */
        public MutableAttributeSet emoticon(Emoticon emoticon) {
            // Does this need any other attributes e.g. standard?
            SimpleAttributeSet emoteStyle = new SimpleAttributeSet();
            EmoticonImage emoteImage = emoticon.getIcon(
                    emoticonScaleFactor(), emoticonMaxHeight(), ChannelTextPane.this);
            StyleConstants.setIcon(emoteStyle, emoteImage.getImageIcon());
            
            emoteStyle.addAttribute(Attribute.EMOTICON, emoteImage);
            Emoticons.addInfo(main.emoticons.getEmotesetInfo(), emoticon);
            return emoteStyle;
        }
        
        public SimpleDateFormat timestampFormat() {
            return timestampFormat;
        }
        
        public int bufferSize() {
            if (bufferSize > 0) {
                return bufferSize;
            }
            return (int)numericSettings.get(Setting.BUFFER_SIZE);
        }
        
        public void setBufferSize(int size) {
            bufferSize = size;
        }
        
        public long namesMode() {
            return numericSettings.get(Setting.DISPLAY_NAMES_MODE);
        }
    }
    

    
}


 


