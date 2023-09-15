
package chatty.gui.components.textpane;

import chatty.Helper;
import chatty.User;
import chatty.gui.Highlighter;
import chatty.gui.LinkListener;
import chatty.gui.MouseClickedListener;
import chatty.gui.UserListener;
import chatty.gui.colors.ColorItem;
import chatty.gui.components.Channel;
import chatty.gui.components.menus.ChannelContextMenu;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.gui.components.menus.StreamsContextMenu;
import chatty.gui.components.menus.TextSelectionMenu;
import chatty.gui.components.menus.UrlContextMenu;
import chatty.gui.components.menus.UserContextMenu;
import chatty.gui.components.menus.UsericonContextMenu;
import chatty.gui.components.textpane.ChannelTextPane.Attribute;
import static chatty.gui.components.textpane.SettingConstants.USER_HOVER_HL_CTRL;
import static chatty.gui.components.textpane.SettingConstants.USER_HOVER_HL_MENTIONS;
import static chatty.gui.components.textpane.SettingConstants.USER_HOVER_HL_MENTIONS_CTRL_ALL;
import chatty.gui.notifications.Notification;
import chatty.lang.Language;
import chatty.util.CombinedEmoticon;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.ReplyManager;
import chatty.util.ReplyManager.Reply;
import chatty.util.StringUtil;
import chatty.util.TwitchEmotesApi;
import chatty.util.TwitchEmotesApi.EmotesetInfo;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticons;
import chatty.util.api.CachedImage;
import chatty.util.api.CachedImage.ImageType;
import chatty.util.api.usericons.Usericon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import static java.awt.event.ActionEvent.ACTION_FIRST;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;
import org.json.simple.JSONArray;

/**
 * Detects any clickable text in the document and reacts accordingly. It shows
 * the appropriate cursor when moving over it with the mouse and reacts to
 * clicks on clickable text.
 * 
 * It knows to look for links and User objects at the moment.
 * 
 * @author tduva
 */
public class LinkController extends MouseAdapter {
    
    private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final Cursor NORMAL_CURSOR = Cursor.getDefaultCursor();
    
    /**
     * When a User is clicked, the User object is send here
     */
    private final Set<UserListener> userListener = new HashSet<>();
    
    private Consumer<User> userHoverListener;
    private int userHoverHighlightMode;
    
    /**
     * When a link is clicked, the String with the url is send here
     */
    private LinkListener linkListener;
    
    private MouseClickedListener mouseClickedListener;
    
    private ContextMenuListener contextMenuListener;
    
    private Supplier<ContextMenu> defaultContextMenuCreator;
    
    private Channel channel;
    
    private MyPopup popup = new MyPopup();
    
    private boolean popupImagesEnabled;
    
    private int mentionMessages;
    
    private Element prevHoverElement;
    
    private ChannelTextPane.Type type;
    
    public void setType(ChannelTextPane.Type type) {
        this.type = type;
    }
    
    /**
     * Set the object that should receive the User object once a User is clicked
     * 
     * @param listener 
     */
    public void addUserListener(UserListener listener) {
        if (listener != null) {
            userListener.add(listener);
        }
    }
    
    public void setUserHoverListener(Consumer<User> listener) {
        this.userHoverListener = listener;
    }
    
    public void setUserHoverHighlightMode(int mode) {
        this.userHoverHighlightMode = mode;
    }
    
    /**
     * Set the object that should receive the url String once a link is clicked
     * 
     * @param listener 
     */
    public void setLinkListener(LinkListener listener) {
        linkListener = listener;
    }
    
    public void setMouseClickedListener(MouseClickedListener listener) {
        mouseClickedListener = listener;
    }

    /**
     * Set the listener for all context menus.
     * 
     * @param listener 
     */
    public void setContextMenuListener(ContextMenuListener listener) {
        contextMenuListener = listener;
    }
    
    /**
     * Set the context menu for when no special context menus (user, link) are
     * appropriate.
     * 
     * @param contextMenu 
     */
    public void setContextMenuCreator(Supplier<ContextMenu> contextMenu) {
        defaultContextMenuCreator = contextMenu;
    }
    
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
   
    /**
     * Handles mouse presses. This is favourable to mouseClicked because it
     * might work better in a fast moving chat and you won't select text
     * instead of opening userinfo etc.
     * 
     * @param e 
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
            Element element = getElement(e);
            if (element != null) {
                handleSingleLeftClick(e, element);
            }
        }
        else if (e.getClickCount() == 1
                && SwingUtilities.isMiddleMouseButton(e)
                && !e.isPopupTrigger()) {
            Element element = getElement(e);
            if (element != null) {
                handleSingleMiddleClick(e, element);
            }
        }
        else if (e.isPopupTrigger()) {
            openContextMenu(e);
        }
    }
    
    private void handleSingleLeftClick(MouseEvent e, Element element) {
        String url;
        String link;
        User user;
        CachedImage<Emoticon> emoteImage;
        CachedImage<Usericon> usericonImage;

        if ((url = getUrl(element)) != null && !isUrlDeleted(element)) {
            if (linkListener != null) {
                linkListener.linkClicked(url);
            }
        } else if ((link = getGeneralLink(element)) != null) {
            for (UserListener listener : userListener) {
                listener.linkClicked(channel, link);
            }
        } else if ((user = getUser(element)) != null
                || (user = getMention(element)) != null) {
            for (UserListener listener : userListener) {
                final User finalUser = user;
                SwingUtilities.invokeLater(() -> {
                    listener.userClicked(finalUser, getMsgId(element), getAutoModMsgId(element), e);
                });
            }
        } else if ((emoteImage = getEmoticonImage(element)) != null) {
            for (UserListener listener : userListener) {
                listener.emoteClicked(emoteImage.getObject(), e);
            }
        } else if ((usericonImage = getUsericonImage(element)) != null) {
            for (UserListener listener : userListener) {
                listener.usericonClicked(usericonImage.getObject(), e);
            }
        }
    }
    
    private void handleSingleMiddleClick(MouseEvent e, Element element) {
        User user;
        if ((user = getUser(element)) != null || (user = getMention(element)) != null) {
            for (UserListener listener : userListener) {
                final User finalUser = user;
                SwingUtilities.invokeLater(() -> {
                    listener.userClicked(finalUser, getMsgId(element), getAutoModMsgId(element), e);
                });
            }
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            openContextMenu(e);
        }
    }
    
    /**
     * Handle clicks (pressed and released) on the text pane.
     * 
     * @param e 
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (mouseClickedListener != null
                && e.getClickCount() == 1
                && !e.isAltDown()
                && !e.isAltGraphDown()
                && e.getButton() == MouseEvent.BUTTON1) {
            // Doing this on mousePressed would prevent selection of text,
            // because this is used to change the focus to the input
            mouseClickedListener.mouseClicked(channel, false);
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        Element element = getElement(e);
        if (element == null) {
            return;
        }
        // Check for Element instead of e.g. EmoticonImage because the same one
        // can occur in several Element objects
        hidePopupIfDifferentElement(element);
        
        JTextPane textPane = (JTextPane)e.getSource();
        if (Debugging.isEnabled("attr")) {
            popup.show(textPane, element, p -> debugElement(element, p), -1);
            return;
        }
        
        CachedImage<Emoticon> emoteImage = getEmoticonImage(element);
        CachedImage<Usericon> usericonImage = getUsericonImage(element);
        String replacedText = getReplacedText(element);
        String replyMsgId = getReplyText(element);
        User mention = getMention(element);
        String hypeChatInfo = (String) element.getAttributes().getAttribute(Attribute.HYPE_CHAT);
        boolean isRestricted = element.getAttributes().getAttribute(ChannelTextPane.Attribute.IS_RESTRICTED) != null;
        if (emoteImage != null) {
            popup.show(textPane, element, p -> makeEmoticonPopupText(emoteImage, popupImagesEnabled, p, element), emoteImage.getImageIcon().getIconWidth());
        } else if (usericonImage != null) {
            popup.show(textPane, element, p -> makeUsericonPopupText(usericonImage, getUsericonInfo(element), p), usericonImage.getImageIcon().getIconWidth());
        } else if (replacedText != null) {
            popup.show(textPane, element, p -> makeReplacementPopupText(replacedText, p), 1);
        } else if (replyMsgId != null) {
            popup.show(textPane, element, p -> makeReplyPopupText(replyMsgId, p), 1);
        } else if (mention != null && mentionMessages > 0) {
            popup.show(textPane, element, p -> makeMentionPopupText(mention, p, mentionMessages), 1);
        } else if (isRestricted) {
            popup.show(textPane, element, p -> p.setText(POPUP_HTML_PREFIX+"Message not posted to chat (restricted)"), 1);
        } else if (hypeChatInfo != null) {
            popup.show(textPane, element, p -> p.setText(POPUP_HTML_PREFIX+hypeChatInfo), 1);
        } else {
            popup.hide();
        }

        User user = null;
        boolean isClickableElement = (getUrl(element) != null && !isUrlDeleted(element))
                || getGeneralLink(element) != null
                || (user = getUser(element)) != null
                || mention != null
                || emoteImage != null
                || usericonImage != null
                || isRestricted
                || hypeChatInfo != null;
        
        if (isClickableElement) {
            textPane.setCursor(HAND_CURSOR);
        } else {
            textPane.setCursor(NORMAL_CURSOR);
        }
        if (userHoverListener != null) {
            if (user == null) {
                user = mention;
            }
            // Don't highlight depending on setting, whether it's a mention and
            // ctrl is being held
            if ((userHoverHighlightMode == USER_HOVER_HL_MENTIONS_CTRL_ALL
                        && mention == null && !e.isControlDown())
                    || (userHoverHighlightMode == USER_HOVER_HL_CTRL
                        && !e.isControlDown())
                    || (userHoverHighlightMode == USER_HOVER_HL_MENTIONS
                        && mention == null)) {
                user = null;
            }
            userHoverListener.accept(user);
        }
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        popup.hide();
    }

    private String getUrl(Element e) {
        return (String)(e.getAttributes().getAttribute(HTML.Attribute.HREF));
    }
    
    private boolean isUrlDeleted(Element e) {
        Boolean deleted = (Boolean) e.getAttributes().getAttribute(ChannelTextPane.Attribute.URL_DELETED);
        if (deleted == null) {
            return false;
        }
        return deleted;
    }
    
    private String getGeneralLink(Element e) {
        return (String)(e.getAttributes().getAttribute(ChannelTextPane.Attribute.GENERAL_LINK));
    }

    private User getUser(Element e) {
        return (User) e.getAttributes().getAttribute(ChannelTextPane.Attribute.USER);
    }
    
    private User getMention(Element e) {
        return (User) e.getAttributes().getAttribute(ChannelTextPane.Attribute.MENTION);
    }
    
    private String getMsgId(Element e) {
        return (String) e.getAttributes().getAttribute(ChannelTextPane.Attribute.ID);
    }
    
    private String getAutoModMsgId(Element e) {
        return (String) e.getAttributes().getAttribute(ChannelTextPane.Attribute.ID_AUTOMOD);
    }
    
    private CachedImage<Emoticon> getEmoticonImage(Element e) {
        return (CachedImage<Emoticon>)(e.getAttributes().getAttribute(ChannelTextPane.Attribute.EMOTICON));
    }
    
    private CachedImage<Usericon> getUsericonImage(Element e) {
        return (CachedImage<Usericon>)(e.getAttributes().getAttribute(ChannelTextPane.Attribute.USERICON));
    }
    
    private String getUsericonInfo(Element e) {
        return (String)(e.getAttributes().getAttribute(ChannelTextPane.Attribute.USERICON_INFO));
    }
    
    private String getReplacedText(Element e) {
        return (String)(e.getAttributes().getAttribute(ChannelTextPane.Attribute.REPLACEMENT_FOR));
    }
    
    private String getReplyText(Element e) {
        return (String)(e.getAttributes().getAttribute(ChannelTextPane.Attribute.REPLY_PARENT_MSG_ID));
    }
    
    private Object getSource(Attribute attribute, Element e) {
        return e.getAttributes().getAttribute(attribute);
    }
    
    private String getSelectedText(MouseEvent e) {
        JTextPane text = (JTextPane) e.getSource();
        return text.getSelectedText();
    }
    
    public static Element getElement(MouseEvent e) {
        JTextPane text = (JTextPane) e.getSource();
        Point mouseLocation = new Point(e.getX(), e.getY());
        int pos = text.viewToModel(mouseLocation);

        if (pos >= 0) {

            /**
             * Check if the found element is actually located where the mouse is
             * pointing, and if not try the previous element.
             *
             * This is a fix to make detection of emotes more reliable. The
             * viewToModel() method apparently searches for the closest element
             * to the given position, disregarding the size of the elements,
             * which means on the right side of an emote the next (non-emote)
             * element is nearer.
             *
             * See also:
             * https://stackoverflow.com/questions/24036650/detecting-image-on-current-mouse-position-only-works-on-part-of-image
             */
            try {
                Rectangle rect = text.modelToView(pos);
                if (e.getX() < rect.x && e.getY() < rect.y + rect.height && pos > 0) {
                    pos--;
                }
            } catch (BadLocationException ex) {

            }

            StyledDocument doc = text.getStyledDocument();
            Element element = doc.getCharacterElement(pos);
            return element;
        }
        return null;
    }
    
    private void openContextMenu(MouseEvent e) {
        // Component to show the context menu on has to be showing to determine
        // it's location (it might not be showing if the channel changed after
        // the click)
        if (!e.getComponent().isShowing()) {
            return;
        }
        Element element = getElement(e);
        if (element == null) {
            return;
        }
        String selectedText = getSelectedText(e);
        User user = getUser(element);
        if (user == null) {
            user = getMention(element);
        }
        String url = getUrl(element);
        String link = getGeneralLink(element);
        CachedImage<Emoticon> emoteImage = getEmoticonImage(element);
        CachedImage<Usericon> usericonImage = getUsericonImage(element);
        JPopupMenu m = null;
        if (user != null) {
            m = new UserContextMenu(user, getMsgId(element),
                    getAutoModMsgId(element), contextMenuListener);
        }
        else if (url != null) {
            m = new UrlContextMenu(url, isUrlDeleted(element), contextMenuListener);
        }
        else if (link != null) {
            if (link.startsWith("join.")) {
                String c = Helper.toStream(link.substring("join.".length()));
                m = new StreamsContextMenu(Arrays.asList(new String[]{c}), contextMenuListener);
            }
        }
        else if (emoteImage != null) {
            m = new EmoteContextMenu(emoteImage, contextMenuListener);
        }
        else if (usericonImage != null) {
            m = new UsericonContextMenu(usericonImage, contextMenuListener);
        }
        else if (!StringUtil.isNullOrEmpty(selectedText) && ((JTextPane) e.getSource()).hasFocus()) {
            /**
             * Text will stay selected when the focus shifts aways, but won't be
             * selected visually anymore. This can be confusing when
             * right-clicking directly back into the channel, since there won't
             * be any text visibly selected but still open this menu. So check
             * focus first.
             */
            m = new TextSelectionMenu((JTextComponent)e.getSource(), false);
        }
        else {
            if (defaultContextMenuCreator == null) {
                if (channel != null) {
                    m = new ChannelContextMenu(contextMenuListener, channel);
                }
            } else {
                ContextMenu menu = defaultContextMenuCreator.get();
                menu.addContextMenuListener(contextMenuListener);
                m = menu;
            }
            addMessageInfoItems(m, element);
        }
        if (m != null) {
            JPopupMenu m2 = m;
            /**
             * Use invokeLater so the focus is already changed to this channel
             * (if necessary), so that closing the menu will return focus to
             * this channel.
             * 
             * This helps prevent the following bug: Have Channel Info open,
             * click into top chat of a split pane (to focus it), right-click
             * into the bottom chat (-> Channel Info switches to bottom
             * channel), press ESC to close context menu (-> Channel Info
             * switches back to top channel instead of the one clicked in).
             */
            SwingUtilities.invokeLater(() -> {
                m2.show(e.getComponent(), e.getX(), e.getY());
            });
        }
        popup.hide();
        if (mouseClickedListener != null) {
            /**
             * Triggering the mouseClicked event here may be necessary to switch
             * channel focus.
             *
             * This was also added because the mouseClicked (in this class, not
             * the one being called) does not get triggered when moving the
             * mouse while right-clicking, however now that one is restricted to
             * triggering only on left-click anyway, in order to handle opening
             * context menus differently.
             */
            mouseClickedListener.mouseClicked(channel, true);
        }
    }
    
    //=============
    // Popup Stuff
    //=============
    
    private static class MyPopup {
        
        private static final int SHOW_DELAY = 300;
        private static final int NO_DELAY_WINDOW = 800;
        
        private final JLabel label = new JLabel();
        private final Timer showTimer;

        private boolean enabled;
        
        /**
         * Current popup. If not null, it means it is currently showing.
         */
        private Popup popup;
        private long lastShown;
        
        /**
         * Popup not showing yet, but it has already been decided to try to show
         * it (timer running or showing, if position is fine).
         */
        private boolean preparingToShow;
        private JTextPane textPane;
        private int sourceWidth;
        private Element element;
        private Point position;
        private boolean contentChanged;
        private boolean contentSet;
        private Consumer<MyPopup> provider;

        public MyPopup() {
            showTimer = new Timer(SHOW_DELAY, e -> {
                showNow();
            });
            showTimer.setRepeats(false);
            label.setHorizontalTextPosition(SwingConstants.CENTER);
            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public void show(JTextPane textPane, Element element, Consumer<MyPopup> provider, int sourceWidth) {
            if (!enabled) {
                return;
            }
            if (popup != null) {
                return;
            }
            if (preparingToShow) {
                return;
            }

            this.textPane = textPane;
            this.sourceWidth = sourceWidth;
            this.element = element;
            this.provider = provider;
            
            contentSet = false;
            preparingToShow = true;
            if (System.currentTimeMillis() - lastShown < NO_DELAY_WINDOW) {
                showNow();
            } else {
                showTimer.restart();
            }
        }
        
        public void hide() {
            if (popup != null) {
                popup.hide();
                popup = null;
                lastShown = System.currentTimeMillis();
            }
            if (preparingToShow) {
                preparingToShow = false;
                showTimer.stop();
            }
        }
        
        /**
         * Set new text and update if showing.
         * 
         * @param newText 
         */
        public void setText(String newText) {
            if (!Objects.equals(newText, label.getText())) {
                label.setText(newText);
                contentChanged = true;
                // Will only update if showing
                update();
            }
        }
        
        /**
         * Set a new icon and update if showing.
         * 
         * @param icon 
         */
        public void setIcon(ImageIcon icon) {
            if (!Objects.equals(icon, label.getIcon())) {
                label.setIcon(icon);
                contentChanged = true;
                // Will only update if showing
                update();
            }
        }
                
        public boolean isCurrentElement(Element element) {
            return this.element == element;
        }
        
        /**
         * Reshow (if still visible), even without explicitly changing content.
         */
        public void forceUpdate() {
            contentChanged = true;
            update();
        }
        
        /**
         * Reshow or hide, depending on updated position. Can't change position
         * of a Popup, so reshowing appears to be necessary.
         */
        public void update() {
            if (popup != null) {
                Point newPos = determinePosition();
                if (newPos == null) {
                    hide();
                } else if (!newPos.equals(position) || contentChanged) {
                    hide();
                    showNow();
                }
            }
        }
        
        private void showNow() {
            if (popup != null) {
                return;
            }
            if (!contentSet) {
                // Reset/set once per show()
                label.setText(null);
                label.setIcon(null);
                // Fill values
                provider.accept(this);
                contentSet = true;
            }
            contentChanged = false;
            
            Point p = determinePosition();
            if (p != null) {
                position = p;
                popup = PopupFactory.getSharedInstance().getPopup(textPane, label, p.x, p.y);
                popup.show();
            }
        }
        
        public void cleanUp() {
            hide();
        }
        
        private Point determinePosition() {
            try {
                // Component has to be showing to determine it's location (and
                // showing the popup only makes sense if it's showing anyway)
                if (!textPane.isShowing()) {
                    return null;
                }
                Dimension labelSize = label.getPreferredSize();
                Rectangle r = textPane.modelToView(element.getStartOffset());
                r.translate(0, - labelSize.height - 3);
                r.translate(sourceWidth / 2 - labelSize.width / 2, 0);
                r.translate(textPane.getLocationOnScreen().x, textPane.getLocationOnScreen().y);
                Component viewPort = textPane.getParent();
                if (viewPort instanceof JViewport) {
                    // Only check bounds if parent is as expected
                    Point bounds = viewPort.getLocationOnScreen();
                    // Top
                    if (bounds.y - 20 > r.y) {
                        r.y += labelSize.height + r.height + 4;
                        if (bounds.y > r.y) {
                            return null;
                        }
                    }
                    // Bottom
                    if (bounds.y + viewPort.getHeight() < r.y + labelSize.height) {
                        return null;
                    }
                    // Left
                    int overLeftEdge = (bounds.x - 5) - r.x;
                    if (overLeftEdge > 0) {
                        r.x = r.x + overLeftEdge;
                    }
                    // Right
                    int overRightEdge = (r.x + labelSize.width) - (bounds.x + textPane.getWidth() + 10);
                    if (overRightEdge > 0) {
                        r.x = r.x - overRightEdge;
                    }
                }
                return new Point(r.x, r.y);
            } catch (BadLocationException ex) {
                // Just return null
            }
            return null;
        }
        
    }

    private void hidePopupIfDifferentElement(Element element) {
        if (prevHoverElement != element) {
            popup.hide();
        }
        prevHoverElement = element;
    }
    
    //======================
    // Public Popup Methods
    //======================
    
    /**
     * This ultimately calls modelToView(), so it probably shouldn't be called
     * during printing a line with many elements.
     */
    public void updatePopup() {
        popup.update();
    }
    
    public void setPopupEnabled(boolean enabled) {
        popup.setEnabled(enabled);
    }
    
    public void setPopupImagesEnabled(boolean enabled) {
        popupImagesEnabled = enabled;
    }
    
    public void setPopupMentionMessages(int amount) {
        mentionMessages = amount;
    }
    
    public void cleanUp() {
        popup.cleanUp();
    }

    //===============
    // Popup Content
    //===============
    
    private static final String POPUP_HTML_PREFIX = "<html>"
            + "<body style='text-align:center;font-weight:bold;'>";
    
    //----------------
    // Emoticon Popup
    //----------------
    
    private static final Object unique = new Object();
    
    private static void makeEmoticonPopupText(CachedImage<Emoticon> emoticonImage, boolean showImage, MyPopup popup, Element element) {
        Debugging.println("emoteinfo", "makePopupText %s", emoticonImage.getObject());
        popup.setText(makeEmoticonPopupText2(emoticonImage, showImage, popup));
    }
    
    private static String makeEmoticonPopupText2(CachedImage<Emoticon> emoticonImage, boolean showImage, MyPopup popup) {
        Emoticon emote = emoticonImage.getObject();
        String result = getEmoteType(emote);
        String code = emote.type == Emoticon.Type.EMOJI
                ? emote.stringId
                : Emoticons.toWriteable(emote.code);
        
        if (emote instanceof CombinedEmoticon) {
            List<Emoticon> combinedEmotes = ((CombinedEmoticon) emote).getEmotes();
            code = combinedEmotes.get(0).code;
            String combinedWith = "";
            for (int i=1; i<combinedEmotes.size(); i++) {
                Emoticon cEmote = combinedEmotes.get(i);
                combinedWith += String.format("<br />+ <span style='font-weight:bold'>%s</span> (%s)",
                        Helper.htmlspecialchars_encode(cEmote.code),
                        getEmoteType(cEmote));
            }
            if (!combinedWith.isEmpty()) {
                result += "<br />"+combinedWith;
            }
        }

        if (Debugging.isEnabled("tt")) {
            result += " [" + emoticonImage.getImageIcon().getDescription() + "]";
        }

        if (showImage) {
            CachedImage<Emoticon> icon = emote.getIcon(2, 0, ImageType.ANIMATED_DARK, (o,n,c) -> {
                // The set ImageIcon will have been updated
                popup.forceUpdate();
            });
            popup.setIcon(icon.getImageIcon());
        }
        return String.format("%s%s<br /><span style='font-weight:normal'>%s</span>",
                POPUP_HTML_PREFIX,
                Helper.htmlspecialchars_encode(code),
                result);
    }
    
    private static String getEmoteType(Emoticon emote) {
        String result;
        if (emote.type == Emoticon.Type.TWITCH) {
            if (emote.subType == Emoticon.SubType.CHEER) {
                result = "Cheering Emote";
                if (emote.hasStreamRestrictions()) {
                    result += " Local";
                }
                else {
                    result += " Global";
                }
            }
            else {
                result = TwitchEmotesApi.getEmoteType(emote);
            }
        }
        else if (emote.type == Emoticon.Type.CUSTOM2) {
            result = "Local Emote";
        }
        else {
            result = emote.type.label;
            if (emote.type != Emoticon.Type.EMOJI) {
                if (emote.hasStreamRestrictions()) {
                    result += " Local";
                }
                else {
                    result += " Global";
                }
            }
        }
        return result;
    }
    
    //----------------
    // Usericon Popup
    //----------------
    
    private static void makeUsericonPopupText(CachedImage<Usericon> usericonImage, String moreInfo, MyPopup p) {
        Usericon usericon = usericonImage.getObject();
        String info;
        if (!usericon.metaTitle.isEmpty()) {
            info = POPUP_HTML_PREFIX+"Badge: "+usericon.metaTitle;
        } else if (usericon.type == Usericon.Type.HL || usericon.type == Usericon.Type.FIRSTMSG) {
            // Customize text since not really a badge
            info = POPUP_HTML_PREFIX+usericon.type.label;
        } else if (usericon.type == Usericon.Type.CHANNEL_LOGO) {
            info = POPUP_HTML_PREFIX+"Channel Logo: "+usericon.channel;
        } else {
            info = POPUP_HTML_PREFIX+"Badge: "+usericon.type.label;
        }
        if (!usericon.channel.isEmpty() && !usericon.channelInverse) {
            info += " (Local)";
        }
        if (usericon.source == Usericon.SOURCE_CUSTOM) {
            info += " (Custom)";
        }
        if (Debugging.isEnabled("tt")) {
            info += " ["+usericonImage.getImageIcon().getDescription()+"]";
        }
        if (usericon.metaDescription != null && !usericon.metaDescription.isEmpty()) {
            info += "<br />"+usericon.metaDescription;
        }
        if (!StringUtil.isNullOrEmpty(moreInfo)) {
            info += "<br />("+moreInfo+")";
        }
        p.setText(info);
    }
    
    //-------------------
    // Replacement Popup
    //-------------------
    
    private static void makeReplacementPopupText(String replacedText, MyPopup p) {
        p.setText(String.format("%sFiltered Text<div style='text-align:left;font-weight:normal'>%s</div>",
                POPUP_HTML_PREFIX,
                StringUtil.addLinebreaks(Helper.htmlspecialchars_encode(replacedText), 70, true)));
    }
    
    //-------------
    // Reply Popup
    //-------------
    
    private static void makeReplyPopupText(String replyMsgId, MyPopup p) {
        List<Reply> replies = ReplyManager.getReplies(replyMsgId);
        StringBuilder b = new StringBuilder();
        if (replies != null) {
            for (Reply reply : replies) {
                b.append(StringUtil.addLinebreaks(Helper.htmlspecialchars_encode(reply.userMsg), 70, true));
                b.append("<br />");
            }
        }
        else {
            b.append("No reply data found (may have expired).");
        }
        p.setText(String.format("%sThread:<div style='text-align:left;font-weight:normal'>%s</div>",
                POPUP_HTML_PREFIX, b.toString()));
    }
    
    private static void makeMentionPopupText(User user, MyPopup p, int amount) {
        List<User.Message> msgs = user.getMessages();
        int count = 0;
        StringBuilder b = new StringBuilder();
        for (int i = msgs.size() - 1; i >= 0; i--) {
            User.Message msg = msgs.get(i);
            if (msg instanceof User.TextMessage) {
                b.insert(0, String.format("[%s] %s<br />",
                        DateTime.format2(msg.getTime()),
                        StringUtil.addLinebreaks(Helper.htmlspecialchars_encode(((User.TextMessage) msg).text), 70, true)));
                count++;
            }
            if (count >= amount) {
                break;
            }
        }
        p.setText(String.format("%sLatest messages of %s:<div style='text-align:left;font-weight:normal'>%s</div>",
                POPUP_HTML_PREFIX, user, b.toString()));
    }
    
    //-------------
    // Debug Popup
    //-------------
    
    private static void debugElement(Element e, MyPopup p) {
        StringBuilder result = new StringBuilder();
        result.append("<html><body style='font-weight:normal;'>");
        try {
            String text = e.getDocument().getText(e.getStartOffset(), e.getEndOffset() - e.getStartOffset());
            text = text.replace("\n", "\\n"); // Make linebreaks visible
            result.append("'").append(text).append("'");
            result.append("<br />");
            if (e.isLeaf() && e.getParentElement() != null) {
                Element parent = e.getParentElement();
                int elementIndex = parent.getElementIndex(e.getStartOffset());
                result.append("Index: ").append(elementIndex).append(" of ").append(parent.getElementCount());
                result.append(" (Length: ").append(e.getEndOffset() - e.getStartOffset()).append(")");
                result.append("<br />");
            }
        } catch (BadLocationException ex) {
            Logger.getLogger(LinkController.class.getName()).log(Level.SEVERE, null, ex);
        }
        AttributeSet attrs = e.getAttributes();
        while (attrs != null) {
            result.append("<b>").append(attrs.toString()).append("</b>");
            result.append("<br />");
            Enumeration en = attrs.getAttributeNames();
            while (en.hasMoreElements()) {
                Object key = en.nextElement();
                Object value = attrs.getAttribute(key);
                result.append(Helper.htmlspecialchars_encode(String.valueOf(key)));
                result.append(" => ");
                result.append(Helper.htmlspecialchars_encode(String.valueOf(value)));
                result.append("<br />");
            }
            attrs = attrs.getResolveParent();
        }
        p.setText(result.toString());
    }
    
    /**
     * Add menu items showing message info like the source of a Highlight. The
     * items have their own action listener set, which calls the
     * contextMenuListener set in the LinkController.
     * 
     * @param m
     * @param element 
     */
    private void addMessageInfoItems(JPopupMenu m, Element element) {
        Object highlightSource = getSource(Attribute.HIGHLIGHT_SOURCE, element);
        Object ignoreSource = getSource(Attribute.IGNORE_SOURCE, element);
        Object colorSource = getSource(Attribute.CUSTOM_COLOR_SOURCE, element);
        Object routingSource = getSource(Attribute.ROUTING_SOURCE, element);
        
        if (highlightSource != null
                || ignoreSource != null
                || colorSource != null
                || routingSource != null) {
            JMenu menu = new JMenu("Message Info");
            
            addMessageInfoItem(menu, "Highlight Source", highlightSource);
            addMessageInfoItem(menu, "Ignore Source", ignoreSource);
            addMessageInfoItem(menu, "Custom Color Source", colorSource);
            addMessageInfoItem(menu, "Routing Source", routingSource);
            
            m.addSeparator();
            m.add(menu);
        }
    }
    
    /**
     * Create the actual menu item with the item text and tooltip, as well as
     * action listener.
     * 
     * The source can be either from a highlight or a color source, whereas a
     * color source can be a Highlight item (e.g. with a color: prefix) or a
     * Msg. Colors item. What matters for the action listener is what setting
     * it belongs to, not whether it caused a highlight or the color to change.
     * Highlight/Ignore settings expect a list to select several entries in the
     * Settings Dialog, Msg. Colors expects a single item (since for the color
     * only one item can be the source, but Highlight items can also be the
     * source for marking several text matches).
     */
    private void addMessageInfoItem(JMenu menu, String label, Object source) {
        if (source != null) {
            JMenuItem item = new JMenuItem(label);
            String sourceText;
            String sourceType;
            String sourceLabel;
            /**
             * Since the source for highlight/ignore is a list now, turn single
             * items (e.g. for color source) into a list so it can be handled
             * the same.
             */
            if (source instanceof Highlighter.HighlightItem) {
                List<Highlighter.HighlightItem> temp = new ArrayList<>();
                temp.add((Highlighter.HighlightItem) source);
                source = temp;
            }
            
            if (source instanceof ColorItem) {
                sourceType = "msgColorSource";
                sourceText = ((ColorItem)source).getId();
                sourceLabel = "["+Language.getString("settings.page.msgColors")+"] "+sourceText;
            }
            else if (source instanceof Notification) {
                Notification n = (Notification) source;
                sourceType = "notificationSource";
                sourceText = String.valueOf(n.id);
                sourceLabel = String.format("[%s] %s",
                        Language.getString("settings.page.notifications"),
                        n.toString());
            }
            else if (source instanceof List) {
                Object sourceItem = getSingleItem(source);
                if (sourceItem instanceof Highlighter.HighlightItem) {
                    Highlighter.HighlightItem hlItem = (Highlighter.HighlightItem) sourceItem;
                    if (hlItem.getUsedForFeature() != null) {
                        switch (hlItem.getUsedForFeature()) {
                            case "highlight":
                                sourceType = "highlightSource";
                                sourceLabel = Language.getString("settings.page.highlight");
                                break;
                            case "ignore":
                                sourceType = "ignoreSource";
                                sourceLabel = Language.getString("settings.page.ignore");
                                break;
                            case "msgcolor":
                                sourceType = "msgColorSource";
                                sourceLabel = Language.getString("settings.page.msgColors");
                                break;
                            case "routing":
                                sourceType = "routingSource";
                                sourceLabel = Language.getString("settings.page.customTabs");
                                break;
                            case "noPresetsUsernameHighlight":
                                sourceType = "highlightSource";
                                sourceLabel = Language.getString("settings.boolean.highlightUsername");
                                break;
                            default:
                                sourceType = "unknownSource";
                                sourceLabel = "Unknown";
                        }
                    }
                    else {
                        sourceType = "unknownSource";
                        sourceLabel = "UnknownType";
                    }
                    List<Highlighter.HighlightItem> list = (List) source;
                    JSONArray rawList = new JSONArray();
                    list.forEach(entry -> rawList.add(entry.getRaw()));
                    sourceText = rawList.toJSONString();
                    sourceLabel = "["+sourceLabel+"] ";
                    sourceLabel += list.get(0).getRaw();
                    if (list.size() > 1) {
                        sourceLabel += " (and " + (list.size() - 1) + " more for marked matches)";
                    }
                }
                else {
                    sourceType = "";
                    sourceText = "";
                    sourceLabel = "";
                }
            }
            else {
                sourceType = "";
                sourceText = "";
                sourceLabel = "";
            }
            item.addActionListener(e -> {
                contextMenuListener.menuItemClicked(new ActionEvent(item, ACTION_FIRST, sourceType+"."+sourceText));
            });
            item.setToolTipText(sourceLabel);
            menu.add(item);
        }
    }
    
    private static Object getSingleItem(Object o) {
        if (o != null && o instanceof List
                && !((List) o).isEmpty()) {
            return ((List) o).get(0);
        }
        return o;
    }
    
}