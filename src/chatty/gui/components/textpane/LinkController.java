
package chatty.gui.components.textpane;

import chatty.Helper;
import chatty.User;
import chatty.gui.LinkListener;
import chatty.gui.MouseClickedListener;
import chatty.gui.UserListener;
import chatty.gui.components.menus.ChannelContextMenu;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.gui.components.menus.UrlContextMenu;
import chatty.gui.components.menus.UserContextMenu;
import chatty.gui.components.menus.UsericonContextMenu;
import chatty.util.Debugging;
import chatty.util.StringUtil;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.Emoticons;
import chatty.util.api.usericons.Usericon;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;

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
    /**
     * When a link is clicked, the String with the url is send here
     */
    private LinkListener linkListener;
    
    private MouseClickedListener mouseClickedListener;
    
    private ContextMenuListener contextMenuListener;
    
    private ContextMenu defaultContextMenu;
    
    private MyPopup popup = new MyPopup();
    
    private Element prevHoverElement;
    
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
        if (defaultContextMenu != null) {
            defaultContextMenu.addContextMenuListener(listener);
        }
    }
    
    /**
     * Set the context menu for when no special context menus (user, link) are
     * appropriate.
     * 
     * @param contextMenu 
     */
    public void setDefaultContextMenu(ContextMenu contextMenu) {
        defaultContextMenu = contextMenu;
        contextMenu.addContextMenuListener(contextMenuListener);
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
        else if (e.isPopupTrigger()) {
            openContextMenu(e);
        }
    }
    
    private void handleSingleLeftClick(MouseEvent e, Element element) {
        String url;
        User user;
        EmoticonImage emoteImage;
        Usericon usericon;

        if ((url = getUrl(element)) != null && !isUrlDeleted(element)) {
            if (linkListener != null) {
                linkListener.linkClicked(url);
            }
        } else if ((user = getUser(element)) != null) {
            for (UserListener listener : userListener) {
                SwingUtilities.invokeLater(() -> {
                    listener.userClicked(user, getMsgId(element), getAutoModMsgId(element), e);
                });
            }
        } else if ((emoteImage = getEmoticonImage(element)) != null) {
            for (UserListener listener : userListener) {
                listener.emoteClicked(emoteImage.getEmoticon(), e);
            }
        } else if ((usericon = getUsericon(element)) != null) {
            for (UserListener listener : userListener) {
                listener.usericonClicked(usericon, e);
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
                && !e.isAltGraphDown()) {
            // Doing this on mousePressed would prevent selection of text,
            // because this is used to change the focus to the input
            mouseClickedListener.mouseClicked();
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
            popup.show(textPane, element, debugElement(element), -1);
            return;
        }
        
        EmoticonImage emoteImage = getEmoticonImage(element);
        Usericon usericon = getUsericon(element);
        String replacedText = getReplacedText(element);
        if (emoteImage != null) {
            popup.show(textPane, element, makeEmoticonPopupText(emoteImage), emoteImage.getImageIcon().getIconWidth());
        } else if (usericon != null) {
            popup.show(textPane, element, makeUsericonPopupText(usericon), usericon.image.getIconWidth());
        } else if (replacedText != null) {
            popup.show(textPane, element, makeReplacementPopupText(replacedText), 1);
        } else {
            popup.hide();
        }

        boolean isClickableElement = (getUrl(element) != null && !isUrlDeleted(element))
                || getUser(element) != null
                || emoteImage != null
                || usericon != null;
        
        if (isClickableElement) {
            textPane.setCursor(HAND_CURSOR);
        } else {
            textPane.setCursor(NORMAL_CURSOR);
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

    private User getUser(Element e) {
        return (User) e.getAttributes().getAttribute(ChannelTextPane.Attribute.USER);
    }
    
    private String getMsgId(Element e) {
        return (String) e.getAttributes().getAttribute(ChannelTextPane.Attribute.ID);
    }
    
    private String getAutoModMsgId(Element e) {
        return (String) e.getAttributes().getAttribute(ChannelTextPane.Attribute.ID_AUTOMOD);
    }
    
    private EmoticonImage getEmoticonImage(Element e) {
        return (EmoticonImage)(e.getAttributes().getAttribute(ChannelTextPane.Attribute.EMOTICON));
    }
    
    private Usericon getUsericon(Element e) {
        return (Usericon)(e.getAttributes().getAttribute(ChannelTextPane.Attribute.USERICON));
    }
    
    private String getReplacedText(Element e) {
        return (String)(e.getAttributes().getAttribute(ChannelTextPane.Attribute.REPLACEMENT_FOR));
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
        User user = getUser(element);
        String url = getUrl(element);
        EmoticonImage emoteImage = getEmoticonImage(element);
        Usericon usericon = getUsericon(element);
        JPopupMenu m;
        if (user != null) {
            m = new UserContextMenu(user, getMsgId(element),
                    getAutoModMsgId(element), contextMenuListener);
        }
        else if (url != null) {
            m = new UrlContextMenu(url, isUrlDeleted(element), contextMenuListener);
        }
        else if (emoteImage != null) {
            m = new EmoteContextMenu(emoteImage, contextMenuListener);
        }
        else if (usericon != null) {
            m = new UsericonContextMenu(usericon, contextMenuListener);
        }
        else {
            if (defaultContextMenu == null) {
                m = new ChannelContextMenu(contextMenuListener);
            } else {
                m = defaultContextMenu;
            }
        }
        m.show(e.getComponent(), e.getX(), e.getY());
        popup.hide();
    }
    
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
        private String text;
        private Element element;
        private Point position;

        public MyPopup() {
            showTimer = new Timer(SHOW_DELAY, e -> {
                showNow();
            });
            showTimer.setRepeats(false);
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public void show(JTextPane textPane, Element element, String text, int sourceWidth) {
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
            this.text = text;
            this.sourceWidth = sourceWidth;
            this.element = element;
            
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
        
        public void update() {
            if (popup != null) {
                Point newPos = determinePosition();
                if (newPos == null) {
                    hide();
                } else if (!newPos.equals(position)) {
                    hide();
                    showNow();
                }
            }
        }
        
        private void showNow() {
            if (popup != null) {
                return;
            }
            label.setText(text);

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
                    if (bounds.y - 20 > r.y) {
                        return null;
                    }
                    if (bounds.y + viewPort.getHeight() < r.y) {
                        return null;
                    }
                    int overLeftEdge = (bounds.x - 5) - r.x;
                    if (overLeftEdge > 0) {
                        r.x = r.x + overLeftEdge;
                    }
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
    
    private static final String POPUP_HTML_PREFIX = "<html>"
            + "<body style='text-align:center;font-weight:bold;border:1px solid #000;padding:3px 5px 3px 5px;'>";
    
    private static String makeEmoticonPopupText(EmoticonImage emoticonImage) {
        Emoticon emote = emoticonImage.getEmoticon();
        String emoteInfo = "";
        if (!emote.hasStreamSet() && emote.hasEmotesetInfo()) {
            emoteInfo = emote.getEmotesetInfo() + " Emoticon";
        } else if (emote.subType == Emoticon.SubType.CHEER) {
            emoteInfo = "Cheering Emote";
            if (emote.hasStreamRestrictions()) {
                emoteInfo += " Local";
            } else {
                emoteInfo += " Global";
            }
        } else if (Emoticons.isTurboEmoteset(emote.emoteSet)) {
            emoteInfo = "Turbo/Prime";
        } else if (!emote.hasGlobalEmoteset() && emote.hasStreamSet()) {
            emoteInfo = "Subemote ("+emote.getStream()+")";
        } else if (!emote.hasGlobalEmoteset()) {
            emoteInfo = "Unknown Emote";
        } else {
            emoteInfo = emote.type.label;
            if (emote.type != Emoticon.Type.EMOJI) {
                if (emote.hasStreamRestrictions()) {
                    emoteInfo += " Local";
                } else {
                    emoteInfo += " Global";
                }
            }
        }
        if (Debugging.isEnabled("tt")) {
            emoteInfo += " ["+emoticonImage.getImageIcon().getDescription()+"]";
        }
        String code = emote.type == Emoticon.Type.EMOJI ? emote.stringId : Emoticons.toWriteable(emote.code);
        return String.format("%s%s<br /><span style='font-weight:normal'>%s</span>",
                POPUP_HTML_PREFIX,
                Helper.htmlspecialchars_encode(code),
                emoteInfo);
    }
    
    private static String makeUsericonPopupText(Usericon usericon) {
        String info;
        if (!usericon.metaTitle.isEmpty()) {
            info = POPUP_HTML_PREFIX+"Badge: "+usericon.metaTitle;
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
            info += " ["+usericon.image.getDescription()+"]";
        }
        return info;
    }
    
    private static String makeReplacementPopupText(String replacedText) {
        return String.format("%sFiltered Text<div style='text-align:left;font-weight:normal'>%s</div>",
                POPUP_HTML_PREFIX,
                StringUtil.addLinebreaks(Helper.htmlspecialchars_encode(replacedText), 70, true));
    }
    
    public void cleanUp() {
        popup.cleanUp();
    }
    
    private static String debugElement(Element e) {
        StringBuilder result = new StringBuilder();
        result.append("<html><body style='font-weight:normal;border:1px solid #000;padding:3px 5px 3px 5px;'>");
        try {
            String text = e.getDocument().getText(e.getStartOffset(), e.getEndOffset() - e.getStartOffset());
            text = text.replace("\n", "\\n"); // Make linebreaks visible
            result.append("'").append(text).append("'");
            result.append("<br />");
            if (e.isLeaf() && e.getParentElement() != null) {
                Element parent = e.getParentElement();
                int elementIndex = parent.getElementIndex(e.getStartOffset());
                result.append("Index: ").append(elementIndex).append(" - Count: ").append(parent.getElementCount());
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
                result.append(key).append(" => ").append(value);
                result.append("<br />");
            }
            attrs = attrs.getResolveParent();
        }
        return result.toString();
    }
    
}