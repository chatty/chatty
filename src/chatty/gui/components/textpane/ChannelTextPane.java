
package chatty.gui.components.textpane;

import chatty.Chatty;
import chatty.gui.components.ChannelEditBox;
import chatty.Helper;
import chatty.SettingsManager;
import chatty.gui.MouseClickedListener;
import chatty.gui.UserListener;
import chatty.util.colors.HtmlColors;
import chatty.gui.LinkListener;
import chatty.gui.StyleServer;
import chatty.gui.UrlOpener;
import chatty.gui.MainGui;
import chatty.User;
import chatty.gui.Highlighter.Match;
import chatty.gui.components.Channel;
import chatty.util.api.usericons.Usericon;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.emoji.EmojiUtil;
import chatty.util.ChattyMisc;
import chatty.util.ChattyMisc.CombinedEmotesInfo;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.MiscUtil;
import chatty.util.Pair;
import chatty.util.ReplyManager;
import chatty.util.RingBuffer;
import chatty.util.StringUtil;
import chatty.util.api.CheerEmoticon;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.Emoticon.EmoticonUser;
import chatty.util.api.Emoticons;
import chatty.util.api.Emoticons.TagEmotes;
import chatty.util.api.pubsub.ModeratorActionData;
import chatty.util.colors.ColorCorrectionNew;
import chatty.util.colors.ColorCorrector;
import chatty.util.irc.MsgTags;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import static javax.swing.JComponent.WHEN_FOCUSED;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
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
    
    private static final ImageIcon REPLY_ICON = new ImageIcon(MainGui.class.getResource("reply.png"));
    
    private final DefaultStyledDocument doc;
    
    private static AtomicLong idCounter = new AtomicLong();
    
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
    private Channel channel;

    protected LinkController linkController = new LinkController();
    private final StyleServer styleServer;
    
    private final RingBuffer<MentionCheck> lastUsers = new RingBuffer<>(300);
    
    protected static User hoveredUser;
    
    public enum Attribute {
        BASE_STYLE, ORIGINAL_BASE_STYLE, TIMESTAMP_COLOR_INHERIT,
        TIME_CREATED,
        
        IS_BAN_MESSAGE, BAN_MESSAGE_COUNT, TIMESTAMP, USER, IS_USER_MESSAGE,
        URL_DELETED, DELETED_LINE, EMOTICON, IS_APPENDED_INFO, INFO_TEXT, BANS,
        BAN_MESSAGE, ID, ID_AUTOMOD, AUTOMOD_ACTION, USERICON, IMAGE_ID, ANIMATED,
        APPENDED_INFO_UPDATED, MENTION, USERICON_INFO,
        
        HIGHLIGHT_WORD, HIGHLIGHT_LINE, EVEN, PARAGRAPH_SPACING,
        CUSTOM_BACKGROUND, CUSTOM_FOREGROUND,
        
        IS_REPLACEMENT, REPLACEMENT_FOR, REPLACED_WITH, COMMAND, ACTION_BY,
        ACTION_REASON,
        
        REPLY_PARENT_MSG, REPLY_PARENT_MSG_ID
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
        SHOW_TOOLTIPS, SHOW_TOOLTIP_IMAGES, BOTTOM_MARGIN,
        
        DISPLAY_NAMES_MODE,
        MENTIONS, MENTIONS_INFO, MENTION_MESSAGES,
        HIGHLIGHT_HOVERED_USER, HIGHLIGHT_MATCHES_ALL, USERCOLOR_BACKGROUND
    }
    
    private static final long DELETED_MESSAGES_KEEP = 0;
    
    protected final Styles styles = new Styles();
    private final ScrollManager scrollManager;
    
    public final LineSelection lineSelection;
    
    private int messageTimeout = -1;
    
    private final MyEditorKit kit;
    
    private final javax.swing.Timer updateTimer;
    
    private final boolean isStreamChat;
    
    public ChannelTextPane(MainGui main, StyleServer styleServer) {
        this(main, styleServer, false, true);
    }
    
    public ChannelTextPane(MainGui main, StyleServer styleServer, boolean isStreamChat, boolean startAtBottom) {
        getAccessibleContext().setAccessibleName("Chat Output");
        getAccessibleContext().setAccessibleDescription("");
        lineSelection = new LineSelection(main.getUserListener());
        this.styleServer = styleServer;
        this.main = main;
        this.setBackground(BACKGROUND_COLOR);
        this.addMouseListener(linkController);
        this.addMouseMotionListener(linkController);
        linkController.addUserListener(main.getUserListener());
        linkController.addUserListener(lineSelection);
        linkController.setUserHoverListener(user -> setHoveredUser(user));
        linkController.setLinkListener(this);
        scrollManager = new ScrollManager();
        this.addMouseListener(scrollManager);
        this.addMouseMotionListener(scrollManager);
        kit = new MyEditorKit(startAtBottom);
        setEditorKit(kit);
        this.setDocument(new MyDocument());
        doc = (DefaultStyledDocument)getStyledDocument();
        setEditable(false);
        DefaultCaret caret = new NoScrollCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        setCaret(caret);
        styles.setStyles();
        
        updateTimer = new javax.swing.Timer(500, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isStreamChat) {
                    removeOldLines();
                }
                removeAbandonedModLogInfo();
            }
        });
        updateTimer.setRepeats(true);
        updateTimer.start();
        
        FixSelection.install(this);
        this.isStreamChat = isStreamChat;
        
        if (Chatty.DEBUG) {
            addCaretListener(new CaretListener() {
                @Override
                public void caretUpdate(CaretEvent e) {
                    System.out.println(String.format("Caret update: %s '%s' %d",
                            e,
                            Util.getText(doc, e.getDot(), e.getDot() + 1).replace("\n", "\\n"),
                            doc.getLength()));
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    getCaret().setVisible(true); // show the caret anyway
                }
            });
        }
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
        // Clearing the images returns false on imageUpdate() to stop animator
        // threads
        kit.clearImages();
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
    
    public void setChannel(Channel channel) {
        this.channel = channel;
        linkController.setChannel(channel);
    }
    
    private void setHoveredUser(User user) {
        int mode = styles.getInt(Setting.HIGHLIGHT_HOVERED_USER);
        if (mode == SettingConstants.USER_HOVER_HL_OFF) {
            if (hoveredUser != null) {
                hoveredUser = null;
                repaint();
            }
        } else if (user != hoveredUser) {
            hoveredUser = user;
            repaint();
        }
    }
    
    /**
     * Can be called when an icon finished loading, so it is displayed correctly.
     * 
     * This seems pretty ineffecient, because it refreshes the whole document.
     */
    @Override
    public void iconLoaded(Image oldImage, Image newImage, boolean sizeChanged) {
        kit.changeImage(oldImage, newImage);
        boolean repainted = false;
        if (!sizeChanged) {
            repainted = repaintImage(newImage);
        }
        if (!repainted) {
            ((MyDocument)doc).refresh();
            if (Debugging.isEnabled("gifd", "gifd2")) {
                Debugging.println("Refresh");
            }
        }
        scrollDownIfNecessary();
    }
    
    /**
     * Customized to handle Animated GIFs more efficiently. Anything else is
     * handled as default.
     * 
     * This will achieve two main things: Only repaint a small area when a new
     * frame is received (instead of the whole chat) and return false if the GIF
     * is no longer present in the chat buffer (to stop further notifications).
     * 
     * For this purpose MyEditorKit stores the views that are created for
     * Emotes, by Image. When chat messages are removed, the stored views are
     * removed as well.
     * 
     * Note: Since the Emoticon class will first create a temporary image (the
     * first time an Emote is added this session), the iconLoaded() method is
     * called with the temporary and actual loaded image, so the references to
     * the views can be updated. In order for the correct references to be found
     * it is important that the temporary image is not reused across several
     * different Emotes, since it acts as a key for lookup (this could probably
     * be changed if reusing temporary images is added).
     */
    @Override
    public boolean imageUpdate(Image img, int infoflags,
                               int x, int y, int w, int h) {
        if ((infoflags & FRAMEBITS) != 0 && !Debugging.isEnabled("gif1")) {
            // Paint new frame of multi-frame image
            boolean imageToRepaintStillPresent = repaintImage(img);
            
            if (Debugging.isEnabled("gifd")
                    || (Debugging.isEnabled("gifd2") && !imageToRepaintStillPresent)) {
                Debugging.println(String.format("FRAMEBITS (%s) %d,%d,%d,%d %d %s",
                        img, x, y, w, h,
                        Debugging.millisecondsElapsed("gifd"),
                        imageToRepaintStillPresent));
            }
            return imageToRepaintStillPresent;
        }
        return super.imageUpdate(img, infoflags, x, y, w, h);
    }
    
    /**
     * Repaint the given image based on the views stored for it. If no views are
     * present, then nothing is repainted and this returns false.
     * 
     * @param image The image to repaint, used to find the associated views
     * @return true if any repainting was attempted, false otherwise
     */
    private boolean repaintImage(Image image) {
        Collection<MyIconView> set = kit.getByImage(image);
        boolean anyVisible = false;
        if (set != null && !set.isEmpty()) {
            Rectangle r = new Rectangle();
            for (final MyIconView v : set) {
                if (!Debugging.isEnabled("gift") && v.shouldCheckVisibility()) {
                    SwingUtilities.invokeLater(() -> {
                        checkViewVisibility(v);
                    });
                }
                if (v.getShouldRepaint()) {
                    v.getRectangle(r);
                    repaint(r);
                    anyVisible = true;
                }
            }
        }
        return anyVisible;
    }
    
    /**
     * Check if the given MyIconView has scrolled out of the visible area on the
     * top. This is necessary because when the view moves out of screen it will
     * not be painted anymore, so it won't receive it's coordinates anymore, and
     * if old lines get removed from the top the coordinates shift, moving the
     * view upwards, while it still has the old further down coordinates, so it
     * would trigger a repaint request further downwards than it actually is.
     * When the chat buffer is full (so that old lines do get removed) the
     * visible area will sort of hover around the same coordinates (depending on
     * line heights), so the the invisible view could actually trigger repaints
     * in the visible area. Aside from causing repaints when it's not actually
     * necessary, it might also cause other issues.
     * 
     * Methods like modelToView() depend on layout stuff, so this should be run
     * in the EDT. In some cases modelToView() can actually be quite costly, but
     * it might be fine in this case since it's not always called at a time
     * where the layout is invalid (like scrolling down automatically), so it
     * probably doesn't have to calculate too much stuff.
     *
     * @param v 
     */
    private void checkViewVisibility(MyIconView v) {
        try {
            Rectangle viewRect = modelToView(v.getStartOffset());
            boolean northOfVisibleRect = viewRect != null
                    && viewRect.y + v.getAdjustedHeight() < getVisibleRect().y;
            
            // Testing
            if (Debugging.isEnabled("gifd4")) {
                Debugging.println("Check "+(viewRect.y + v.getAdjustedHeight()) + " < " + getVisibleRect().y);
                if (northOfVisibleRect) {
                    Debugging.println("Stop "+v);
                }
            }
            
            if (northOfVisibleRect) {
                v.setDontRepaint();
                kit.debug();
            }
        } catch (BadLocationException ex) {
            // Give up in case of error
        }
    }
    
    /**
     * Remove text from chat, while also handling Emotes in the removed section
     * correctly.
     *
     * @param start Start offset
     * @param len Length of section to remove
     * @throws BadLocationException 
     */
    private void remove(int start, int len) throws BadLocationException {
        Debugging.edt();
        int startLine = doc.getDefaultRootElement().getElementIndex(start);
        int endLine = doc.getDefaultRootElement().getElementIndex(start+len);
        Set<Long> before = Util.getImageIds(doc, startLine, endLine);
        doc.remove(start, len);
        if (!before.isEmpty()) {
            Set<Long> after = Util.getImageIds(doc, startLine, endLine);
            if (Debugging.isEnabled("gifd", "gifd2")) {
                Debugging.println(String.format("Removed %d+%d (Before: %s After: %s)",
                        start, len, before, after));
            }
            before.removeAll(after);
            for (long id : before) {
                kit.clearImage(id);
            }
        }
    }
    
    /**
     * Outputs some type of message.
     * 
     * @param message Message object containing all the data
     */
    public void printMessage(Message message) {
        if (message instanceof UserMessage) {
            printUserMessage((UserMessage)message);
        }
    }
    
    /**
     * Print the notification when a user has subscribed, which may contain an
     * attached message from the user which requires special handling.
     * 
     * @param message 
     */
    private void printUsernotice(UserNotice message, MutableAttributeSet style) {
        closeCompactMode();
        printTimestamp(style);
        
        MutableAttributeSet userStyle;
        if (message.user.getName().isEmpty()) {
            // Only dummy User attached (so no custom message attached as well)
            userStyle = style;
        } else {
            /**
             * This is kind of a hack to allow this message to be clicked and
             * deleted.
             * 
             * Note: For shortening/deleting messages, everything after the User
             * element is affected, so in this case just the attached message.
             */
            userStyle = styles.user(message.user, style);
            userStyle.addAttribute(Attribute.IS_USER_MESSAGE, true);
            lastUsers.add(new MentionCheck(message.user));
        }
        
        String text = message.infoText;
        print("["+message.type+"] "+text, userStyle);
        if (!StringUtil.isNullOrEmpty(message.attachedMessage)) {
            print(" [", style);
            // Output with emotes, but don't turn URLs into clickable links
            printSpecialsNormal(message.attachedMessage, message.user, style, message.emotes, true, false, null, null, null, message.tags);
            print("]", style);
        }
        finishLine();
    }
    
    private void printAutoModMessage(AutoModMessage message, AttributeSet style) {
        closeCompactMode();
        printTimestamp(style);
        
        MutableAttributeSet userStyle = styles.user(message.user, style);
        userStyle.addAttribute(Attribute.ID_AUTOMOD, message.msgId);
        // Should be the same as the start of the "text" in the AutoModMessage,
        // so highlight matches are displayed properly
        String startText = "[AutoMod] <"+message.user.getDisplayNick()+">";
        print(startText, userStyle);
        printSpecialsInfo(" "+message.message, style,
                Match.shiftMatchList(message.highlightMatches, -startText.length()));
        finishLine();
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
        
        Color background = null;
        if (message.backgroundColor != null) {
            background = message.backgroundColor;
        } else if (message.highlighted) {
            background = MyStyleConstants.getHighlightBackground(styles.paragraph());
        }
        
        closeCompactMode();

        MutableAttributeSet style;
        if (highlighted) {
            style = styles.highlight(color);
        } else {
            style = styles.standard(color);
        }
        printTimestamp(style);
        printUser(user, action, message.whisper, message.id, background, message.tags.isHighlightedMessage());
        
        // Change style for text if /me and no highlight (if enabled)
        if (!highlighted && color == null && action && styles.isEnabled(Setting.ACTION_COLORED)) {
            style = styles.standard(user.getDisplayColor());
        }
        print(" ", style);
        printSpecialsNormal(text, user, style, emotes, false, message.bits > 0,
                message.highlightMatches,
                message.replaceMatches, message.replacement, message.tags);
        
        if (message.highlighted) {
            setLineHighlighted(doc.getLength());
        }
        if (message.backgroundColor != null) {
            setCustomBackgroundColor(doc.getLength(), message.backgroundColor);
        }
        finishLine();
        
        lastUsers.add(new MentionCheck(user));
    }
    
    public void printInfoMessage(InfoMessage message) {
        //-------
        // Style
        //-------
        MutableAttributeSet style;
        if (message.highlighted) {
            style = styles.highlight(message.color);
        } else {
            style = styles.info(message.color);
        }
        
        if (message instanceof ModLogInfo) {
            //-------------
            // ModLog Info
            //-------------
            // May be an extra message or just for adding name behind action
            ModLogInfo modLogInfo = (ModLogInfo)message;
            if (modLogInfo.showActionBy) {
                // Add the mod name behind actions
                boolean success = printModLogInfo(modLogInfo);
                if (!success) {
                    cachedModLogInfo.add(modLogInfo);
                }
            }
            if (!message.isHidden()) {
                // Show separate ModAction message
                printInfoMessage2(message, style);
            }
        } else if (message instanceof UserNotice) {
            printUsernotice((UserNotice) message, style);
        } else if (message instanceof AutoModMessage) {
            printAutoModMessage((AutoModMessage)message, style);
        } else {
            //--------------------
            // Other Info Message
            //--------------------
            printInfoMessage2(message, style);
            
            String command = message.makeCommand();
            if (command != null) {
                setLineCommand(doc.getLength(), command);
            }
            
            replayModLogInfo();
        }
        //-----------------
        // Line properties
        //-----------------
        if (!message.isHidden()) {
            if (message.highlighted) {
                setLineHighlighted(doc.getLength());
            }
            if (message.bgColor != null) {
                setCustomBackgroundColor(doc.getLength(), message.bgColor);
            }
        }
    }
    
    private void printInfoMessage2(InfoMessage message, AttributeSet style) {
        closeCompactMode();
        printTimestamp(style);
        printSpecialsInfo(message.text, style, message.highlightMatches);
        finishLine();
    }
    
    private final java.util.List<ModLogInfo> cachedModLogInfo = new ArrayList<>();
    
    // Wait for an action to occur after the ModLog has come in (ms)
    private static final int MOD_ACTION_WAIT = 1000;
    
    /**
     * Cached ModLog messages, for which there hasn't been found a matching
     * message yet, which can happen when the actual action comes in after the
     * ModLog message (info from different connections).
     */
    private void replayModLogInfo() {
        removeAbandonedModLogInfo();
        if (cachedModLogInfo.isEmpty()) {
            return;
        }
        Debugging.println("modlog", "ModLog Replay %s", cachedModLogInfo);
        Iterator<ModLogInfo> it = cachedModLogInfo.iterator();
        while (it.hasNext()) {
            ModLogInfo info = it.next();
            boolean success = printModLogInfo(info);
            if (success) {
                it.remove();
            }
        }
        Debugging.println("modlog", "ModLog Replay After %s", cachedModLogInfo);
    }
    
    /**
     * Remove ModLogInfo objects cached for replay that are too old, and send
     * them back for ModAction output if applicable.
     */
    private void removeAbandonedModLogInfo() {
        if (cachedModLogInfo.isEmpty()) {
            return;
        }
        Iterator<ModLogInfo> it = cachedModLogInfo.iterator();
        java.util.List<ModLogInfo> print = new ArrayList<>();
        while (it.hasNext()) {
            ModLogInfo info = it.next();
            if (info.age() > MOD_ACTION_WAIT) {
                Debugging.println("modlog", "ModLogAction Abandoned: %s", info);
                it.remove();
                if (info.isHidden()) {
                    // Only output if originally was hidden, so it's not already
                    // (sent back so highlighting etc. can be applied properly)
                    print.add(info);
                }
            }
        }
        // Don't output while in loop
        for (ModLogInfo info : print) {
            main.printAbandonedModLogInfo(info);
        }
    }
    
    private boolean printModLogInfo(ModLogInfo info) {
        Element root = doc.getDefaultRootElement();
        String command = info.makeCommand().trim();
        Debugging.println("modlog", "ModLog Command: %s", command);
        for (int i=root.getElementCount()-1;i>=0;i--) {
            Element line = root.getElement(i);
            if (info.isBanCommand()) {
                /**
                 * Bans (and related) can be newly applied to otherwise old
                 * messages, and it should only apply to the already appended
                 * ban info, so it has to be handled differently from other info
                 * messages.
                 */
                if (line.getAttributes().containsAttribute(Attribute.COMMAND, command)) {
                    Element infoElement = getElementContainingAttributeKey(line,
                        Attribute.IS_APPENDED_INFO);
                    if (infoElement == null) {
                        Debugging.println("modlog", "ModLog: No info");
                        return true;
                    }
                    long updated = (Long) infoElement.getAttributes().getAttribute(Attribute.APPENDED_INFO_UPDATED);
                    if (System.currentTimeMillis() - updated > MOD_ACTION_WAIT) {
                         Debugging.println("modlog", "ModLog: Appended too old");
                        // The ban might come in after this, so wait
                        return false;
                    }
                    changeInfo(line, attributes -> {
                        addBy(attributes, info.data.created_by);
                        if (info.getReason() != null) {
                            // Shouldn't add null value since it sometimes seems
                            // to cause error when printing with these attrs
                            attributes.addAttribute(Attribute.ACTION_REASON, info.getReason());
                        }
                    });
                    return true;
                }
            } else if (info.isAutoModAction()) {
                /**
                 * Approved/denied action, which is not quite the same as
                 * appending the mod name (more similiar to a regular ban, where
                 * it changes some possibly older message and doesn't directly
                 * associate with a received action), but also originating from
                 * Modlog and should be output again if no match was found.
                 */
                Element userElement = getUserElementFromLine(line, false);
                if (userElement != null) {
                    if (userElement.getAttributes().containsAttribute(Attribute.ID_AUTOMOD, info.data.msgId)) {
                        changeInfo(line, attr -> {
                            String existing = (String) attr.getAttribute(Attribute.AUTOMOD_ACTION);
                            String action = "approved";
                            if (info.data.type == ModeratorActionData.Type.AUTOMOD_DENIED) {
                                action = "denied";
                            }
                            /**
                             * Usually there should only be one mod approving/
                             * denying a particular message, but just in case
                             * allow for several to be added.
                             */
                            String infoText = StringUtil.append(existing, ", ", action + "/@" + info.data.created_by);
                            attr.addAttribute(Attribute.AUTOMOD_ACTION, infoText);
                        });
                        return true;
                    }
                }
            } else {
                /**
                 * Regular info messages such as "This room is now in x mode",
                 * which should be applied almost instantly, so finding any old
                 * message first indicates no match at this time (the associated
                 * message may not have come in yet or there is no match at all
                 * in the chat buffer).
                 */
                if (getTimeAgo(line) > MOD_ACTION_WAIT) {
                     Debugging.println("modlog", "ModLog: Line too old");
                    // If the most recent line is too old, then don't mark as
                    // done yet, but instead wait if the matching info message
                    // still comes in
                    return false;
                }
                if (line.getAttributes().containsAttribute(Attribute.COMMAND, command)) {
                    changeInfo(line, attributes -> {
                        addBy(attributes, info.data.created_by);
                    });
                    return true;
                }
            }
        }
        Debugging.println("modlog", "ModLog: Gave up");
        return false;
    }
    
    private void addBy(MutableAttributeSet attributes, String byName) {
        java.util.List<String> old = (java.util.List)attributes.getAttribute(Attribute.ACTION_BY);
        java.util.List<String> changed;
        if (old != null) {
            changed = new ArrayList<>(old);
        } else {
            changed = new ArrayList<>();
        }
        // If already present, append to end
        changed.remove(byName);
        changed.add(byName);
        attributes.addAttribute(Attribute.ACTION_BY, changed);
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
            if (element.getAttributes().isDefined(key)) {
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
        changeInfo(line, attributes -> {
            Integer count = (Integer) attributes.getAttribute(Attribute.BAN_MESSAGE_COUNT);
            if (count == null) {
                // If it doesn't exist set to 2, because this will be the second
                // timeout this message represents
                count = 2;
            } else {
                // Otherwise increase number and removet text of previous number
                count++;
            }
            attributes.addAttribute(Attribute.BAN_MESSAGE_COUNT, count);
        });
    }
    
    private interface InfoChanger {
        public void changeInfo(MutableAttributeSet attributes);
    }
    
    /**
     * Changes the info at the end of a line.
     * 
     * @param line 
     * @param changer 
     */
    private void changeInfo(Element line, InfoChanger changer) {
        try {
            
            //---------------------------------------
            // Find and handle current appended info
            //---------------------------------------
            Element infoElement = getElementContainingAttributeKey(line,
                    Attribute.IS_APPENDED_INFO);
            
            boolean isNew = false;
            
            MutableAttributeSet attributes;
            if (infoElement == null) {
                // No info appended yet, so get last element of line as starting point
                infoElement = line.getElement(line.getElementCount() - 1);
                attributes = new SimpleAttributeSet(styles.info());
                isNew = true;
            } else {
                // Info already appended, copy current attributes
                attributes = new SimpleAttributeSet(infoElement.getAttributes());
            }
            
            // Change attributes
            changer.changeInfo(attributes);
            attributes.addAttribute(Attribute.IS_APPENDED_INFO, true);
            attributes.addAttribute(Attribute.APPENDED_INFO_UPDATED, System.currentTimeMillis());
            
            if (!isNew) {
                // Remove already appended info
                int start = infoElement.getStartOffset();
                int length = infoElement.getEndOffset() - infoElement.getStartOffset();
                doc.remove(start, length);
            }
            
            //---------------------------------------
            // Make text based on current attributes
            //---------------------------------------
            String text = "";
            Integer banCount = (Integer)attributes.getAttribute(Attribute.BAN_MESSAGE_COUNT);
            if (banCount != null && banCount > 1) {
                text += String.format("(%d)", banCount);
            }
            
            String autoModAction = (String)attributes.getAttribute(Attribute.AUTOMOD_ACTION);
            if (!StringUtil.isNullOrEmpty(autoModAction)) {
                text = StringUtil.append(text, " ", "("+autoModAction+")");
            }
            
            String infoText = (String)attributes.getAttribute(Attribute.INFO_TEXT);
            if (infoText != null && !infoText.isEmpty()) {
                text = StringUtil.append(text, " ", infoText);
            }
            
            String actionReason = (String)attributes.getAttribute(Attribute.ACTION_REASON);
            if (!StringUtil.isNullOrEmpty(actionReason)
                    && styles.isEnabled(Setting.BAN_REASON_APPENDED)) {
                text = StringUtil.append(text, " ", "["+actionReason+"]");
            }
            
            java.util.List<String> actionBy = (java.util.List)attributes.getAttribute(Attribute.ACTION_BY);
            if (actionBy != null) {
                text = StringUtil.append(text, " ", "(@"+StringUtil.join(actionBy, ", ")+")");
            }
            
            //--------------------------
            // Insert new appended info
            //--------------------------
            /**
             * Insert at the end of the appended info element, which may or may
             * not be the last element of the line, depending on whether the
             * next line is of the same style or not. If it is the last element,
             * then it will contain a linebreak, which has to be excluded.
             * 
             * With next line of different style (separate element for the
             * linebreak, so insert at the start of that would work):
             * '[17:02] ''tduva'' has been banned from talking''
             * '
             * 
             * With next line of same style (linebreak at the end of the last
             * element of the line, inserting at the start of that would place
             * it after the name):
             * '[17:02] ''tduva'' has been banned from talking
             * '
             * 
             * With the appended element already added (separate element for the
             * linebreak as well, probably because of different attributes):
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
            if (isLineFromUserAndId(line, user, null, true)) {
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
     * @param targetMsgId The id of the deleted message, null if no specific message
     */
    public void userBanned(User user, long duration, String reason, String targetMsgId) {
        if (styles.isEnabled(Setting.SHOW_BANMESSAGES)) {
            //-----------------------
            // For extra ban message
            //-----------------------
            String banInfo = Helper.makeBanInfo(duration, reason,
                    styles.isEnabled(Setting.BAN_DURATION_MESSAGE),
                    styles.isEnabled(Setting.BAN_REASON_MESSAGE),
                    false);
            
            String message = "has been banned";
            if (duration > 0) {
                message = "has been timed out";
            }
            if (duration == -2) {
                message = "had a message deleted";
            }
            if (!StringUtil.isNullOrEmpty(targetMsgId)) {
                message += " (single message)";
            }
            if (!banInfo.isEmpty()) {
                message = message+" "+banInfo;
            }

            Element prevMessage = null;
            if (styles.isEnabled(Setting.COMBINE_BAN_MESSAGES)) {
                prevMessage = findPreviousBanMessage(user, message);
            }
            if (prevMessage != null) {
                increasePreviousBanMessage(prevMessage, duration, reason);
            } else {
                closeCompactMode();
                printTimestamp(styles.banMessage(user, message));
                print(user.getCustomNick(), styles.user(user, styles.info()));
                print(" "+message, styles.info());
                finishLine();
            }
        }
        
        //--------------------------------------
        // For indicator behind deleted message
        //--------------------------------------
        String banInfo = Helper.makeBanInfo(duration, reason,
                styles.isEnabled(Setting.BAN_DURATION_APPENDED),
                styles.isEnabled(Setting.BAN_REASON_APPENDED),
                true);

        /**
         * values > 0 mean strike through, shorten message
         * value == 0 means strike through
         * value < 0 means delete message
         */
        int mode = styles.getInt(Setting.DELETED_MESSAGES_MODE);
        boolean delete = mode < DELETED_MESSAGES_KEEP;
        
        boolean first = true;
        for (Userline l : getUserLines(user)) {
            boolean msgIdMatches = targetMsgId == null || targetMsgId.equals(getIdFromElement(l.userElement));
            boolean isAutoModMessage = Util.hasAttributeKey(l.userElement, Attribute.ID_AUTOMOD);
            boolean isUserMessage = Util.hasAttributeKeyValue(l.userElement, Attribute.IS_USER_MESSAGE, true);

            if (msgIdMatches && (isAutoModMessage || isUserMessage)) {
                if (delete) {
                    deleteMessage(l.line);
                } else {
                    strikeThroughMessage(l.line, mode);
                }
                if (first) {
                    setBanInfo(l.line, banInfo);
                    setLineCommand(l.line.getStartOffset(), Helper.makeBanCommand(user, duration, targetMsgId));
                    replayModLogInfo();
                    first = false;
                }
            }
        }
    }
    
    /**
     * Changes the free-form text behind a message.
     * 
     * @param line
     * @param info 
     */
    private void setBanInfo(Element line, String info) {
        Element firstElement = line.getElement(0);
        if (firstElement != null && getTimeAgo(firstElement) > 60*1000) {
            info += "*";
        }
        final String info2 = info;
        changeInfo(line, attributes -> {
            attributes.addAttribute(Attribute.INFO_TEXT, info2);
        });
    }
    
    private static class Userline {
        
        private User user;
        private Element line;
        private Element userElement;
        
        Userline(User user, Element userElement, Element line) {
            this.user = user;
            this.userElement = userElement;
            this.line = line;
        }
        
    }
    
    private java.util.List<Userline> getUserLines(User searchUser) {
        java.util.List<Userline> result = new ArrayList<>();
        Element root = doc.getDefaultRootElement();
        for (int i = root.getElementCount() - 1; i >= 0; i--) {
            Element line = root.getElement(i);
            Element userElement = getUserElementFromLine(line, false);
            if (userElement != null) {
                User foundUser = (User)userElement.getAttributes().getAttribute(Attribute.USER);
                if (searchUser == null || foundUser == searchUser) {
                    result.add(new Userline(searchUser, userElement, line));
                }
            }
        }
        return result;
    }
    
    /**
     * Searches the Document for all lines by the given user.
     * 
     * @param nick
     * @return 
     */
    private ArrayList<Integer> getLinesFromUser(User user, String id, boolean onlyUserMessages) {
        Element root = doc.getDefaultRootElement();
        ArrayList<Integer> result = new ArrayList<>();
        for (int i=0;i<root.getElementCount();i++) {
            Element line = root.getElement(i);
            if (isLineFromUserAndId(line, user, id, onlyUserMessages)) {
                result.add(i);
            }
        }
        return result;
    }
    
    private boolean isMessageLine(Element line) {
        return getUserFromLine(line) != null;
    }
    
    private User getUserFromLine(Element line) {
        return getUserFromElement(getUserElementFromLine(line, true), true);
    }
    
    private Element getUserElementFromLine(Element line, boolean onlyUserMessage) {
        for (int i = 0; i < line.getElementCount(); i++) {
            Element element = line.getElement(i);
            User elementUser = getUserFromElement(element, onlyUserMessage);
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
    private boolean isLineFromUserAndId(Element line, User user, String id, boolean onlyUserMessages) {
        Element element = getUserElementFromLine(line, onlyUserMessages);
        User elementUser = getUserFromElement(element, onlyUserMessages);
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
    private User getUserFromElement(Element element, boolean onlyUserMessage) {
        if (element != null) {
            User elementUser = (User)element.getAttributes().getAttribute(Attribute.USER);
            Boolean isMessage = (Boolean)element.getAttributes().getAttribute(Attribute.IS_USER_MESSAGE);
            if (!onlyUserMessage || (isMessage != null && isMessage == true)) {
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
    private void strikeThroughMessage(Element elementToRemove, int maxLength) {
        if (isLineDeleted(elementToRemove)) {
            return;
        }
        
        // Determine the offsets of the whole line and the message part
        int[] offsets = Util.getMessageOffsets(elementToRemove);
        if (offsets.length != 2) {
            return;
        }
        int lineStart = elementToRemove.getStartOffset();
        int msgStart = offsets[0];
        int msgEnd = offsets[1];
        int msgLength = msgEnd - msgStart;
        
        // 0123456
        //   2   6   4
        //     4
        
        if (maxLength > 0 && msgLength > maxLength) {
            // Delete part of the message if it exceeds the maximum length
            try {
                int removedStart = msgStart + maxLength;
                int removedLength = msgLength - maxLength;
                remove(removedStart, removedLength);
                msgEnd = removedStart;
                doc.insertString(removedStart, "..", styles.info());
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location");
            }
        }
        doc.setCharacterAttributes(lineStart, msgEnd - lineStart, styles.deleted(), false);
        setLineDeleted(lineStart);
    }
    
    /**
     * Deletes the message of the given line by replacing it with
     * <message deleted>.
     * 
     * @param line The number of the line in the document
     */
    private void deleteMessage(Element elementToRemove) {
        if (isLineDeleted(elementToRemove)) {
            //System.out.println(line+"already deleted");
            return;
        }
        int[] messageOffsets = Util.getMessageOffsets(elementToRemove);
        if (messageOffsets.length == 2) {
            int msgStart = messageOffsets[0];
            int msgEnd = messageOffsets[1];
            try {
                remove(msgStart, msgEnd - msgStart);
                doc.insertString(msgStart, "<message deleted>", styles.info());
                setLineDeleted(msgStart);
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location: "+msgStart+"-"+msgEnd+" "+ex.getLocalizedMessage());
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
    
    private void setLineCommand(int offset, String command) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(Attribute.COMMAND, command);
        doc.setParagraphAttributes(offset, 1, attr, false);
    }
    
    private void setVariableLineAttributes(int offset, boolean even, boolean updateTimestamp) {
        doc.setParagraphAttributes(offset, 1, styles.variableLineAttributes(even, updateTimestamp), false);
    }
    
    private void setLineHighlighted(int offset) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(Attribute.HIGHLIGHT_LINE, true);
        doc.setParagraphAttributes(offset, 1, attr, false);
    }
    
    private void setCustomBackgroundColor(int offset, Color color) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        attr.addAttribute(Attribute.CUSTOM_BACKGROUND, color);
        doc.setParagraphAttributes(offset, 1, attr, false);
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
                ArrayList<Integer> lines = getLinesFromUser(user, null, true);
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
            if (currentUser != null && isLineFromUserAndId(element, currentUser, null, true)) {
                highlightLine(element, false);
            }
        }
        
        private String getCurrentId() {
            if (currentSelection != null) {
                return getIdFromElement(getUserElementFromLine(currentSelection, true));
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
     * @param msgId 
     */
    private void printUser(User user, boolean action,
            boolean whisper, String msgId, Color background, boolean pointsHl) {
        
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
        
        // Badges or Status Symbols
        if (styles.isEnabled(Setting.USERICONS_ENABLED)) {
            printUserIcons(user, pointsHl);
        }
        else {
            userName = user.getModeSymbol()+userName;
        }
        
        // Output name
        if (user.hasCategory("rainbow")) {
            printRainbowUser(user, userName, action, SpecialColor.RAINBOW, msgId);
        } else if (user.hasCategory("golden")) {
            printRainbowUser(user, userName, action, SpecialColor.GOLD, msgId);
        } else {
            MutableAttributeSet style = styles.messageUser(user, msgId, background);
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
            MutableAttributeSet style = styles.messageUser(user, msgId, background);
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
            print(":", styles.messageUser(user, msgId, background));
        } else {
            //print(" ", styles.messageUser(user));
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
    private void printUserIcons(User user, boolean pointsHl) {
        boolean botBadgeEnabled = styles.isEnabled(Setting.BOT_BADGE_ENABLED);
        java.util.List<Usericon> badges = user.getBadges(botBadgeEnabled, pointsHl, isStreamChat);
        if (badges != null) {
            for (Usericon badge : badges) {
                if (badge.image != null && !badge.removeBadge) {
                    print(badge.getSymbol(), styles.makeIconStyle(badge, user));
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
        // TODO: change to fix for amount, maybe change to removing elements
        clearImages(firstToRemove);
        clearImages(lastToRemove);
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
            Element paragraph = doc.getDefaultRootElement().getElement(0);
            if (doc.getLength() > 1 && getTimeAgo(paragraph) > messageTimeout * 1000) {
//                removeFirstLines(1);
                
                // Don't use doc.remove() for this, since removing one line with
                // it seems to copy paragraph attributes to the follow line
                // (visible if alternating backgrounds are showing)
                if (doc.getDefaultRootElement().getElementCount() > 1) {
                    // Can't use this if it's the last element
                    doc.removeElement(doc.getDefaultRootElement().getElement(0));
                } else {
                    clearAll();
                }
                scrollDownIfNecessary();
                resetNewlineRequired();
            }
        }
    }
    
    public void clearAll() {
        try {
            doc.remove(0, doc.getLength());
            resetNewlineRequired();
            kit.clearImages();
        } catch (BadLocationException ex) {
            Logger.getLogger(ChannelTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void clearImages(Element element) {
        Long imageId = (Long)element.getAttributes().getAttribute(Attribute.IMAGE_ID);
        if (imageId != null) {
            kit.clearImage(imageId);
        }
        if (!element.isLeaf()) {
            for (int i=0; i<element.getElementCount(); i++) {
                clearImages(element.getElement(i));
            }
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
            printTimestamp(styles.compact());
            print(type+": ", styles.compact());
            seperator = "";
        }
        print(seperator, styles.compact());
        print(user.getCustomNick(), styles.user(user, styles.compact()));
        
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
            finishLine();
            compactMode = null;
        }
    }
    
    /**
     * Start the next print with a newline. This must be called when the current
     * line is finished.
     */
    protected void finishLine() {
        newlineRequired = true;
        lineSelection.onLineAdded(getLastLine(doc));
        even = !even;
        setVariableLineAttributes(doc.getLength() - 1, even, true);
    }
    
    boolean even = false;
    
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
        printTimestamp(style);
        print(line,style);
        finishLine();
    }

    /**
     * For info messages. Only applys links and mentions.
     */
    private void printSpecialsInfo(String text, AttributeSet style,
            java.util.List<Match> highlightMatches) {
        TreeMap<Integer,Integer> ranges = new TreeMap<>();
        HashMap<Integer,MutableAttributeSet> rangesStyle = new HashMap<>();
        
        findLinks(text, ranges, rangesStyle, style);
        
        if (styles.isEnabled(Setting.MENTIONS_INFO)) {
            findMentions(text, ranges, rangesStyle, style, Setting.MENTIONS_INFO);
        }
        
        // Actually output it
        printSpecials(null, text, style, ranges, rangesStyle, highlightMatches);
    }
    
    /**
     * For user messages. Applys replacements, links, emotes, bits, mentions.
     */
    protected void printSpecialsNormal(String text, User user, MutableAttributeSet style,
            TagEmotes emotes, boolean ignoreLinks, boolean containsBits,
            java.util.List<Match> highlightMatches,
            java.util.List<Match> replacements, String replacement,
            MsgTags tags) {
        // Where stuff was found
        TreeMap<Integer,Integer> ranges = new TreeMap<>();
        // The style of the stuff (basicially metadata)
        HashMap<Integer,MutableAttributeSet> rangesStyle = new HashMap<>();
        
        if (tags != null && tags.isReply() && text.startsWith("@")) {
            Pair<User, String> replyData = getReplyData(tags);
            if (replyData.value != null) {
                ranges.put(0, 0);
                rangesStyle.put(0, styles.reply(replyData.value, tags.getReplyParentMsgId()));
            }
        }
        
        applyReplacements(text, replacements, replacement, ranges, rangesStyle);
        
        if (!ignoreLinks) {
            findLinks(text, ranges, rangesStyle, styles.standard());
        }
        
        if (styles.isEnabled(Setting.EMOTICONS_ENABLED)) {
            findEmoticons(text, user, ranges, rangesStyle, emotes);
            if (containsBits) {
                findBits(main.emoticons.getCheerEmotes(), text, ranges, rangesStyle, user);
            }
        }
        
        if (styles.isEnabled(Setting.MENTIONS)) {
            findMentions(text, ranges, rangesStyle, style, Setting.MENTIONS);
        }
        
        // Actually output it
        printSpecials(user, text, style, ranges, rangesStyle, highlightMatches);
    }
    
    private void printSpecials(User user, String text,
            AttributeSet style,
            Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle,
            java.util.List<Match> highlightMatches) {
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
                specialPrint(user, text, lastPrintedPos, start, style, highlightMatches);
            }
            AttributeSet rangeStyle = rangesStyle.get(start);
            String rangeText;
            if (rangeStyle.containsAttribute(Attribute.IS_REPLACEMENT, true)) {
                rangeText = (String)rangeStyle.getAttribute(Attribute.REPLACED_WITH);
                if (!rangeText.isEmpty()) {
                    print(rangeText, rangeStyle);
                }
            } else {
                if (styles.isEnabled(Setting.HIGHLIGHT_MATCHES_ALL)
                        && StyleConstants.getIcon(rangeStyle) == null) {
                    specialPrint(user, text, start, end+1, rangeStyle, highlightMatches);
                } else {
                    print(text.substring(start, end+1), rangeStyle);
                }
            }
            lastPrintedPos = end + 1;
        }
        // If anything is left, print that as well as regular text
        if (lastPrintedPos < text.length()) {
            specialPrint(user, text, lastPrintedPos, text.length(), style, highlightMatches);
        }
    }
    
    /**
     * Prints the given range of text from start to end, adding the attribute
     * for drawing Highlight/Ignore matches.
     * 
     * @param user
     * @param text
     * @param start
     * @param end
     * @param style
     * @param highlightMatches 
     */
    private void specialPrint(User user, String text, int start, int end, AttributeSet style, java.util.List<Match> highlightMatches) {
        if (highlightMatches != null) {
            for (Match m : highlightMatches) {
                if (m.start < end && m.end > start) {
                    // Affects this region at all
                    int from = m.start;
                    if (from < start) {
                        from = start;
                    }
                    int to = m.end;
                    if (to > end) {
                        to = end;
                    }
                    if (from > start) {
                        String processed = processText(user, text.substring(start, from));
                        print(processed, style);
                    }
                    
                    String processed = processText(user, text.substring(from, to));
                    MutableAttributeSet styleCopy = new SimpleAttributeSet(style);
                    styleCopy.addAttribute(Attribute.HIGHLIGHT_WORD, true);
                    print(processed, styleCopy);
                    start = to;
                }
            }
        }
        if (start < end) {
            print(processText(user, text.substring(start, end)), style);
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
        result = Helper.removeEmojiVariationSelector(result);
        int filterMode = styles.getInt(Setting.FILTER_COMBINING_CHARACTERS);
        if (filterMode > Helper.FILTER_COMBINING_CHARACTERS_OFF) {
            String prev = result;
            result = Helper.filterCombiningCharacters(result, "****", filterMode);
            if (!prev.equals(result)) {
                //LOGGER.info("Filtered combining characters: [" + prev + "] -> [" + result + "]");
                LOGGER.info("Filtered combining characters from message by "+user+" [result: "+result+"]");
            }
        }
        return result;
    }
    
    private void applyReplacements(String text, java.util.List<Match> matches,
            String replacement,
            Map<Integer, Integer> ranges, Map<Integer, MutableAttributeSet> rangesStyle) {
        if (matches != null) {
            if (StringUtil.isNullOrEmpty(replacement)) {
                replacement = "..";
            } else if (replacement.equals("none")) {
                replacement = "";
            }
            for (Match m : matches) {
                if (!inRanges(m.start, ranges) && !inRanges(m.end, ranges)) {
                    ranges.put(m.start, m.end - 1);
                    String replacedText = text.substring(m.start, m.end);
                    rangesStyle.put(m.start, styles.replacement(replacedText, replacement));
                }
            }
        }
    }
    
    private void findLinks(String text, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle, AttributeSet baseStyle) {
        // Find links
        urlMatcher.reset(text);
        while (urlMatcher.find()) {
            int start = urlMatcher.start();
            int end = urlMatcher.end() - 1;
            if (!inRanges(start, ranges) && !inRanges(end, ranges)) {
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
                    rangesStyle.put(start, styles.url(foundUrl, baseStyle));
                }
            }
        }
    }
    
    private void findMentions(String text,
            Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle,
            AttributeSet baseStyle,
            Setting setting) {
        Set<User> alreadyChecked = new HashSet<>();
        for (MentionCheck check : lastUsers.getItems()) {
            if (alreadyChecked.contains(check.user)) {
                continue;
            }
            alreadyChecked.add(check.user);
            Matcher m = check.matcher.reset(text);
            while (m.find()) {
                int start = m.start();
                int end = m.end() - 1;
                if (!inRanges(start, ranges) && !inRanges(end, ranges)) {
                    ranges.put(start, end);
                    rangesStyle.put(start, styles.mention(check.user, baseStyle, setting));
                }
            }
        }
    }
    
    /**
     * Returns the user that was being replied to as key, the replied to text
     * to be shown in the popup as value. Both of those can be null.
     * 
     * @param tags
     * @return 
     */
    private Pair<User, String> getReplyData(MsgTags tags) {
        User user = null;
        String replyMsgText = tags.getReplyUserMsg();
        if (replyMsgText == null) {
            replyMsgText = ReplyManager.getFirstUserMsg(tags.getReplyParentMsgId());
        }
        if (replyMsgText == null) {
            // Check in recent users if no text supplied (usually for sent msgs)
            String msgId = tags.getReplyParentMsgId();
            Set<User> alreadyChecked = new HashSet<>();
            for (MentionCheck check : lastUsers.getItems()) {
                if (alreadyChecked.contains(check.user)) {
                    continue;
                }
                alreadyChecked.add(check.user);
                String msg = check.user.getMessageText(msgId);
                if (msg != null) {
                    replyMsgText = String.format("<%s> %s",
                            check.user.getDisplayNick(),
                            msg);
                    user = check.user;
                    break;
                }
            }
        }
        return new Pair(user, replyMsgText);
    }
    
    private int fCount = 0;
    private final Random fRand = new Random();
    private long fSeed;
    
    public int getRand(int bound) {
        long seed = System.currentTimeMillis() / 20000;
        if (seed != fSeed) {
            fRand.setSeed(seed);
            fSeed = seed;
        }
        return fRand.nextInt(bound);
    }
    
    private void findEmoticons(String text, User user, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle, TagEmotes tagEmotes) {
        
        Set<String> accessToSets = user.isLocalUser() ? main.emoticons.getLocalEmotesets() : null;
        findEmoticons(user, main.emoticons.getCustomEmotes(), text, ranges, rangesStyle, accessToSets);
        if (Debugging.isEnabled("emoji2") || EmojiUtil.mightContainEmoji(text)) {
            findEmoticons(user, main.emoticons.getEmoji(), text, ranges, rangesStyle);
        }
        
        if (tagEmotes != null) {
            // Add emotes from tags
            Map<String, Emoticon> emoticonsById = main.emoticons.getEmoticonsById();
            addTwitchTagsEmoticons(user, emoticonsById, text, ranges, rangesStyle, tagEmotes);
        }
        
        if (user.isLocalUser()) {
            for (String set : main.emoticons.getLocalEmotesets()) {
                HashSet<Emoticon> emoticons = main.emoticons.getEmoticonsBySet(set);
                findEmoticons(emoticons, text, ranges, rangesStyle);
            }
        }
        
        // Global emotes
        if (tagEmotes == null) {
            Set<Emoticon> emoticons = main.emoticons.getGlobalTwitchEmotes();
            findEmoticons(emoticons, text, ranges, rangesStyle);
        }
        Set<Emoticon> emoticons = main.emoticons.getOtherGlobalEmotes();
        findEmoticons(emoticons, text, ranges, rangesStyle);
        
        // Channel based (may also have a emoteset restriction)
        HashSet<Emoticon> channelEmotes = main.emoticons.getEmoticonsByStream(user.getStream());
        findEmoticons(user, channelEmotes, text, ranges, rangesStyle);
        
        // Special Combined Emotes
        CombinedEmotesInfo cei = ChattyMisc.getCombinedEmotesInfo();
        if (!cei.isEmpty()) {
            int baseStart = -1;
            int lastEnd = -1;
            Map<Integer, Integer> changes = new HashMap<>();
            Map<Integer, MutableAttributeSet> styleChanges = new HashMap<>();
            java.util.List<Emoticon> emotes = new ArrayList<>();
            Iterator<Entry<Integer, Integer>> rangesIt = ranges.entrySet().iterator();
            // Go through all parts with a special style
            while (rangesIt.hasNext()) {
                Entry<Integer, Integer> range = rangesIt.next();
                int start = range.getKey();
                int end = range.getValue();
                MutableAttributeSet style = rangesStyle.get(start);
                EmoticonImage image = (EmoticonImage) style.getAttribute(Attribute.EMOTICON);
                // Only affect emotes that aren't GIFs
                if (image != null && !image.isAnimated()) {
                    // Check for emote that can overlay over another one, and
                    // that there is an emote to modify directly before that
                    if (cei.containsCode(image.getEmoticon().code)
                            && !emotes.isEmpty()
                            && lastEnd + 2 == start) {
                        // Extend original emote to span to the end of this one
                        changes.put(baseStart, end);
                        rangesIt.remove();
                    }
                    else {
                        // This isn't an overlay emote, so check if a previous
                        // combined emote still needs to be created
                        if (emotes.size() > 1) {
                            Emoticon emote = main.emoticons.getCombinedEmote(emotes);
                            styleChanges.put(baseStart, styles.emoticon(emote));
                        }
                        // Always reset when it's not an overlay emote
                        emotes.clear();
                        baseStart = start;
                    }
                    // Add all emotes, if this is not an overlay emote it will
                    // start empty (and only add each emote only once)
                    if (!emotes.contains(image.getEmoticon())) {
                        emotes.add(image.getEmoticon());
                    }
                    lastEnd = end;
                }
            }
            // Finish any remaining changes
            if (emotes.size() > 1) {
                Emoticon emote = main.emoticons.getCombinedEmote(emotes);
                styleChanges.put(baseStart, styles.emoticon(emote));
            }
            // Apply changes (except removing entries, which is already done)
            for (Entry<Integer, Integer> entry : changes.entrySet()) {
                ranges.put(entry.getKey(), entry.getValue());
            }
            for (Entry<Integer, MutableAttributeSet> entry : styleChanges.entrySet()) {
                rangesStyle.put(entry.getKey(), entry.getValue());
            }
        }
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
    private void addTwitchTagsEmoticons(User user, Map<String, Emoticon> emoticons, String text,
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
                String id = emoteData.id;
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
                        b.setStringId(id);
                        b.setEmoteset(Emoticon.SET_UNKNOWN);
                        emoticon = b.build();
                        main.emoticons.addTempEmoticon(emoticon);
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
        findEmoticons(user, emoticons, text, ranges, rangesStyle, null);
    }
    
    private void findEmoticons(User user, Set<Emoticon> emoticons, String text,
            Map<Integer, Integer> ranges, Map<Integer, MutableAttributeSet> rangesStyle,
            Set<String> accessToSets) {
        // Find emoticons
        for (Emoticon emoticon : emoticons) {
            // Check the text for every single emoticon
            if (!emoticon.matchesUser(user, accessToSets)) {
                continue;
            }
            if (main.emoticons.isEmoteIgnored(emoticon)) {
                continue;
            }
            if (emoticon.isAnimated()
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
                // For Emoji, check for text style variation selector
                boolean textEmoji = emoticon.type == Emoticon.Type.EMOJI && m.group().endsWith("\uFE0E");
                if (!textEmoji) {
                    addEmoticon(emoticon, start, end, ranges, rangesStyle);
                }
            }
        }
    }
    
    private void findBits(Set<CheerEmoticon> emotes, String text,
            Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle,
            User user) {
        for (CheerEmoticon emote : emotes) {
            if (!emote.matchesUser(user, null)) {
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
     * Split up long sections of text by newline to prevent lag. This is not a
     * hard limit, sections may be a bit longer.
     *
     * This should be way longer than any regular chat message, since putting
     * newlines in messages can cause issues (especially for applying custom
     * line-based attributes) and looks kind of weird.
     *
     * This can mostly happen with some info messages (like really long /mods
     * output).
     */
    private final int MAX_TEXT_LENGTH = 2000;
    private int lengthSinceNewline = 0;

    /**
     * Prints the given text in the given style. Runs the function that actually
     * adds the text in the Event Dispatch Thread.
     * 
     * @param text
     * @param style 
     */
    private void print(final String text, final AttributeSet style) {
        try {
            String newline = "";
            if (newlineRequired) {
                lengthSinceNewline = 0;
                newline = "\n";
                newlineRequired = false;
                clearSomeChat();
            }
            /**
             * Split up long sections by a newline. See MAX_TEXT_LENGTH.
             */
            lengthSinceNewline += text.length();
            if (lengthSinceNewline > MAX_TEXT_LENGTH) {
                // How much is the current text above limit
                int breakTarget = MAX_TEXT_LENGTH - (lengthSinceNewline - text.length());
                // Prefer breaking at space, if within reasonable range
                int firstSpace = text.indexOf(' ', breakTarget);
                if (firstSpace != -1 && firstSpace - breakTarget < MAX_TEXT_LENGTH / 20) {
                    breakTarget = firstSpace;
                }
                String part = text.substring(0, breakTarget);
                String remaining = text.substring(breakTarget);
                doc.insertString(doc.getLength(), newline+part, style);
                doc.setParagraphAttributes(doc.getLength(), 1, styles.paragraph(), true);
                newlineRequired = true;
                print(remaining, style);
            }
            else {
                //System.out.println("1:"+doc.getLength());
                doc.insertString(doc.getLength(), newline+text, style);
                //System.out.println("2:"+doc.getLength());
                //this.getHighlighter().addHighlight(doc.getLength(), 10, null);
                // TODO: check how this works
                doc.setParagraphAttributes(doc.getLength(), 1, styles.paragraph(), true);
                scrollDownIfNecessary();
            }
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
     * @param style
     */
    protected void printTimestamp(AttributeSet style) {
        if (styles.timestampFormat() != null) {
            print(DateTime.currentTime(styles.timestampFormat())+" ", styles.timestamp(style));
        }
        else {
            // Inserts the linebreak with a style that shouldn't break anything
            print("", style);
        }
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
            if (!styles.isEnabled(Setting.AUTO_SCROLL)) {
                return false;
            }
            long timePassed = System.currentTimeMillis() - lastManualScrollChange;
            if (timePassed > 1000 * styles.getInt(Setting.AUTO_SCROLL_TIME)) {
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
        private final String[] baseStyles = new String[]{"standard","timestamp","special","info","base","highlight","paragraph"};
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
         * Store the timestamp format
         */
        private SimpleDateFormat timestampFormat;
        
        private int bufferSize = -1;
        
        private ColorCorrector colorCorrector;

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
            
            // Load other stuff from the StyleServer
            setSettings();

            // Additional styles
            SimpleAttributeSet nick = new SimpleAttributeSet(base());
            StyleConstants.setBold(nick, true);
            styles.put("nick", nick);

            //-----------------
            // Paragraph Style
            //-----------------
//            Font font = new Font(StyleConstants.getFontFamily(standard()),
//                    Font.PLAIN, StyleConstants.getFontSize(standard()));
//            int fontHeight = new JFrame().getFontMetrics(font).getHeight();
//            int fontHeight = font.getSize();
            MutableAttributeSet paragraph = styles.get("paragraph");
            paragraph.addAttribute(Attribute.DELETED_LINE, false);
            /**
             * Since the line spacing only gets added below the text, first add
             * as much paragaph spacing above (if available), then distribute
             * the rest above and below evenly. This should sort of center the
             * text vertically in the paragraph, which is required for it to
             * look good with paragraph seperating lines and backgounds.
             */
            int fontHeight = MyStyleConstants.getFontHeight(paragraph);
            int preferredSpaceAbove = (int)Math.ceil(fontHeight * StyleConstants.getLineSpacing(paragraph));
            int availableSpace = ((Long)paragraph.getAttribute(Attribute.PARAGRAPH_SPACING)).intValue();
            int remainingSpace = Math.max(availableSpace - preferredSpaceAbove, 0);
            int actualSpaceAbove = Math.min(preferredSpaceAbove, availableSpace);
            int topSpacing = (int)(remainingSpace / 2 + actualSpaceAbove);
            int bottomSpacing = (int)((remainingSpace / 2) + remainingSpace % 2);
//            System.out.println(preferredSpaceAbove + " " + topSpacing + " " + bottomSpacing);
            if (Debugging.isEnabled("oldspacings")) {
                topSpacing = availableSpace / 3;
                bottomSpacing = (availableSpace / 3) * 2 + availableSpace % 3;
            }
            StyleConstants.setSpaceAbove(paragraph, topSpacing);
            StyleConstants.setSpaceBelow(paragraph, bottomSpacing);
            styles.put("paragraph", paragraph);
            
            //editorKit.setBottomMargin(Math.max((preferredSpaceAbove + availableSpace) / 4, 1));
            int prevBottomMargin = getMargin().bottom;
            int bottomMarginSetting = numericSettings.get(Setting.BOTTOM_MARGIN);
            int bottomMargin = bottomMarginSetting;
            if (bottomMarginSetting < 0) {
                /**
                 * Determine bottom margin automatically.
                 * 
                 * When there's nothing visually separating lines, then the
                 * bottom line should (roughly) have as much space below as the
                 * space to the previous line (so that the text seems vertically
                 * centered), so add an additional bottom margin. When the lines
                 * are separated, then only the paragraph's own spacing visually
                 * belongs to it, so no additional margin is required.
                 */
                boolean alternatingBackgrounds = MyStyleConstants.getBackground2(paragraph) != null;
                boolean separators = MyStyleConstants.getSeparatorColor(paragraph) != null;
                int fullSpacing = preferredSpaceAbove + availableSpace;
                if (!alternatingBackgrounds && !separators) {
                    bottomMargin = (int)Math.max(fullSpacing / 2.5, 1);
                } else if (!alternatingBackgrounds) {
                    bottomMargin = 2;
                } else {
                    bottomMargin = 0;
                }
            }
            if (Debugging.isEnabled("oldspacings")) {
                bottomMargin = 3;
            }
            if (bottomMargin != prevBottomMargin) {
                Chatty.println("Bottom Margin: "+bottomMargin);
                setMargin(new Insets(3, 3, bottomMargin, 3));
                repaint();
            }
            
            //--------------
            // Other Styles
            //--------------
            SimpleAttributeSet even = new SimpleAttributeSet();
            even.addAttribute(Attribute.EVEN, true);
            styles.put("even", even);
            
            SimpleAttributeSet odd = new SimpleAttributeSet();
            odd.addAttribute(Attribute.EVEN, false);
            styles.put("odd", odd);
            
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
            
            colorCorrector = styleServer.getColorCorrector();

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
            addSetting(Setting.SHOW_TOOLTIPS, true);
            addSetting(Setting.SHOW_TOOLTIP_IMAGES, true);
            addNumericSetting(Setting.MENTIONS, 0, 0, 200);
            addNumericSetting(Setting.MENTIONS_INFO, 0, 0, 200);
            addNumericSetting(Setting.MENTION_MESSAGES, 0, 0, 200);
            addSetting(Setting.HIGHLIGHT_MATCHES_ALL, true);
            addNumericSetting(Setting.USERCOLOR_BACKGROUND, 1, 0, 200);
            addNumericSetting(Setting.FILTER_COMBINING_CHARACTERS, 1, 0, 2);
            addNumericSetting(Setting.DELETED_MESSAGES_MODE, 30, -1, 9999999);
            addNumericSetting(Setting.BUFFER_SIZE, 250, BUFFER_SIZE_MIN, BUFFER_SIZE_MAX);
            addNumericSetting(Setting.AUTO_SCROLL_TIME, 30, 5, 1234);
            addNumericSetting(Setting.EMOTICON_MAX_HEIGHT, 200, 0, 300);
            addNumericSetting(Setting.EMOTICON_SCALE_FACTOR, 100, 1, 200);
            addNumericSetting(Setting.DISPLAY_NAMES_MODE, 0, 0, 10);
            addNumericSetting(Setting.BOTTOM_MARGIN, -1, -1, 100);
            addNumericSetting(Setting.HIGHLIGHT_HOVERED_USER, 0, 0, 4);
            timestampFormat = styleServer.getTimestampFormat();
            linkController.setPopupEnabled(settings.get(Setting.SHOW_TOOLTIPS));
            linkController.setPopupImagesEnabled(settings.get(Setting.SHOW_TOOLTIP_IMAGES));
            linkController.setUserHoverHighlightMode(getInt(Setting.HIGHLIGHT_HOVERED_USER));
            linkController.setPopupMentionMessages(getInt(Setting.MENTION_MESSAGES));
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
            newStyle.addAttribute(Attribute.BASE_STYLE, name);
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
                Element paragraph = root.getElement(i);
                if (changedStyles.contains("paragraph")) {
                    doc.setParagraphAttributes(paragraph.getStartOffset(),
                        1, paragraph(), false);
                }
                
                for (int j = 0; j < paragraph.getElementCount(); j++) {
                    Element element = paragraph.getElement(j);
                    String type = (String)element.getAttributes().getAttribute(Attribute.BASE_STYLE);
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
                        if (type.equals("timestamp")) {
                            /**
                             * Timestamp style is based on another style, so
                             * reapply here properly.
                             */
                            String originalBaseStyle = (String)element.getAttributes().getAttribute(Attribute.ORIGINAL_BASE_STYLE);
                            style = timestamp(styles.get(originalBaseStyle));
                        }
                        doc.setCharacterAttributes(start, length, style, false);
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
        
        public MutableAttributeSet info(Color color) {
            if (color != null) {
                SimpleAttributeSet specialColor = new SimpleAttributeSet(info());
                StyleConstants.setForeground(specialColor, color);
                specialColor.addAttribute(Attribute.CUSTOM_FOREGROUND, color);
                return specialColor;
            }
            return info();
        }
        
        public MutableAttributeSet compact() {
            return styles.get("special");
        }
        
        public MutableAttributeSet standard(Color color) {
            if (color != null) {
                SimpleAttributeSet specialColor = new SimpleAttributeSet(standard());
                StyleConstants.setForeground(specialColor, color);
                specialColor.addAttribute(Attribute.CUSTOM_FOREGROUND, color);
                return specialColor;
            }
            return standard();
        }
        
        public MutableAttributeSet standard() {
            return styles.get("standard");
        }
        
        public SimpleAttributeSet timestamp(AttributeSet style) {
            SimpleAttributeSet resultStyle = new SimpleAttributeSet(style);
            AttributeSet timestamp = styles.get("timestamp");
            resultStyle.addAttribute(Attribute.ORIGINAL_BASE_STYLE, style.getAttribute(Attribute.BASE_STYLE));
            resultStyle.addAttributes(timestamp);
            Object inherit = timestamp.getAttribute(Attribute.TIMESTAMP_COLOR_INHERIT);
            if (inherit != null
                    && (style.containsAttribute(Attribute.BASE_STYLE, "highlight")
                        || style.containsAttribute(Attribute.BASE_STYLE, "info")
                        || style.getAttribute(Attribute.CUSTOM_FOREGROUND) != null)) {
                float matchFactor = (Float)inherit;
                if (matchFactor < 0.1) {
                    StyleConstants.setForeground(resultStyle, StyleConstants.getForeground(style));
                } else {
                    Color newColor = ColorCorrectionNew.matchLightness(
                            StyleConstants.getForeground(style),
                            StyleConstants.getForeground(timestamp), matchFactor);
                    StyleConstants.setForeground(resultStyle, newColor);
                }
            }
            return resultStyle;
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
            return styles.get("paragraph");
        }
        
        /**
         * Currently whether the line is even and the timestamp of when the line
         * was added (this method called).
         * 
         * @param even
         * @return 
         */
        public MutableAttributeSet variableLineAttributes(boolean even, boolean updateTimestamp) {
            MutableAttributeSet style = even ? styles.get("even") : styles.get("odd");
            if (updateTimestamp) {
                style.addAttribute(Attribute.TIMESTAMP, System.currentTimeMillis());
            }
            return style;
        }
        
        public MutableAttributeSet highlight(Color color) {
            if (color != null) {
                SimpleAttributeSet specialColor = new SimpleAttributeSet(highlight());
                StyleConstants.setForeground(specialColor, color);
                specialColor.addAttribute(Attribute.CUSTOM_FOREGROUND, color);
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
        
        private Color getUserColor(User user) {
            // Color
            Color userColor = user.getColor();
            // Only correct color if no custom color is defined
            if (!user.hasCustomColor()) {
                // If turned off, it will just return the same color
                userColor = colorCorrector.correctColor(userColor, getBackground());
                user.setCorrectedColor(userColor);
            }
            return userColor;
        }
        
        /**
         * Makes a style for the given User, containing the User-object itself
         * and the user-color. Changes the color to hopefully improve readability.
         * 
         * @param user The User object to configure this style for
         * @param msgId The msg-id of this line (may be null)
         * @param background The background color used for this line, may be
         * null (e.g. if not using a custom background)
         * @return 
         */
        public MutableAttributeSet messageUser(User user, String msgId, Color background) {
            SimpleAttributeSet userStyle = new SimpleAttributeSet(nick());
            userStyle.addAttribute(Attribute.IS_USER_MESSAGE, true);
            userStyle.addAttribute(Attribute.USER, user);
            
            Color foreground = getUserColor(user);
            StyleConstants.setForeground(userStyle, foreground);
//            if (background != null) {
//                System.out.println(user+" "+ColorCorrectionNew.getLightnessDifference(foreground, background));
//            }
            int threshold;
            switch(getInt(Setting.USERCOLOR_BACKGROUND)) {
                case 1:
                    threshold = 38;
                    break;
                case 2:
                    threshold = 89;
                    break;
                default:
                    threshold = 0;
            }
            if (threshold > 0 && background != null) {
                int difference = ColorCorrectionNew.getLightnessDifferenceAbs(foreground, background);
                if (difference < threshold
                        && ColorCorrectionNew.getLightnessDifferenceAbs(foreground, getBackground()) > difference) {
                    MyStyleConstants.setLabelBackground(userStyle, getBackground());
                }
            }
                    
            if (msgId != null) {
                userStyle.addAttribute(Attribute.ID, msgId);
            }
            return userStyle;
        }
        
        public MutableAttributeSet user(User user, AttributeSet style) {
            SimpleAttributeSet userStyle = new SimpleAttributeSet(style);
            userStyle.addAttribute(Attribute.USER, user);
            return userStyle;
        }
        
        public MutableAttributeSet mention(User user, AttributeSet style,
                Setting setting) {
            int settingValue = numericSettings.get(setting);
            SimpleAttributeSet mentionStyle = new SimpleAttributeSet(style);
            if (MiscUtil.biton(settingValue, 1)) {
                StyleConstants.setBold(mentionStyle, true);
            }
            if (MiscUtil.biton(settingValue, 2)) {
                StyleConstants.setUnderline(mentionStyle, true);
            }
            if (MiscUtil.biton(settingValue, 3)) {
                StyleConstants.setForeground(mentionStyle, getUserColor(user));
            }
            mentionStyle.addAttribute(Attribute.MENTION, user);
            return mentionStyle;
        }
        
        /**
         * Creates a style for the given icon. Also modifies the icon to add a
         * little space on the side, so it can be displayed easier. It caches
         * styles, so it only needs to create the style and modify the icon
         * once.
         * 
         * @param icon
         * @param user
         * @return The created style (or read from the cache)
         */
        public MutableAttributeSet makeIconStyle(Usericon icon, User user) {
            MutableAttributeSet style = savedIcons.get(icon);
            if (style == null) {
                //System.out.println("Creating icon style: "+icon);
                style = new SimpleAttributeSet(nick());
                if (icon != null && icon.image != null) {
                    StyleConstants.setIcon(style, addSpaceToIcon(icon.image));
                    style.addAttribute(Attribute.USERICON, icon);
                    if (icon.type == Usericon.Type.TWITCH
                            && Usericon.typeFromBadgeId(icon.badgeType.id) == Usericon.Type.SUB
                            && user != null
                            && user.getSubMonths() > 0) {
                        style.addAttribute(Attribute.USERICON_INFO, DateTime.formatMonthsVerbose(user.getSubMonths()));
                    }
                }
                savedIcons.put(icon, style);
            }
            return style;
        }
        
        public int emoticonMaxHeight() {
            return numericSettings.get(Setting.EMOTICON_MAX_HEIGHT);
        }
        
        public float emoticonScaleFactor() {
            return (float)(numericSettings.get(Setting.EMOTICON_SCALE_FACTOR) / 100.0);
        }
        
        public int getInt(Setting setting) {
            return (int)numericSettings.get(setting);
        }
        
        public boolean isEnabled(Setting setting) {
            if (setting == Setting.MENTIONS || setting == Setting.MENTIONS_INFO) {
                return MiscUtil.biton(numericSettings.get(setting), 0);
            }
            return settings.get(setting);
        }
        
        /**
         * Make a link style for the given URL.
         * 
         * @param url
         * @param baseStyle
         * @return 
         */
        public MutableAttributeSet url(String url, AttributeSet baseStyle) {
            SimpleAttributeSet urlStyle = new SimpleAttributeSet(baseStyle);
            StyleConstants.setUnderline(urlStyle, true);
            urlStyle.addAttribute(HTML.Attribute.HREF, url);
            return urlStyle;
        }
        
        public MutableAttributeSet replacement(String text, String replacement) {
            SimpleAttributeSet style = new SimpleAttributeSet(standard());
            StyleConstants.setUnderline(style, true);
            style.addAttribute(Attribute.IS_REPLACEMENT, true);
            style.addAttribute(Attribute.REPLACEMENT_FOR, text);
            style.addAttribute(Attribute.REPLACED_WITH, replacement);
            return style;
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
            emoteStyle.addAttribute(Attribute.IMAGE_ID, idCounter.getAndIncrement());
            emoteStyle.addAttribute(Attribute.ANIMATED, emoticon.isAnimated());
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
        
        public MutableAttributeSet reply(String parentUserText, String parentMsgId) {
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setIcon(style, addSpaceToIcon(REPLY_ICON));
            style.addAttribute(Attribute.REPLY_PARENT_MSG, parentUserText);
            style.addAttribute(Attribute.REPLY_PARENT_MSG_ID, parentMsgId);
            return style;
        }
    }
    
    private static class MentionCheck {
        public final User user;
        public final Matcher matcher;
        
        public MentionCheck(User user) {
            this.user = user;
            this.matcher = Pattern.compile("(?i)\\b"+Pattern.quote(user.getName())+"\\b").matcher("");
        }
    }
    
}





