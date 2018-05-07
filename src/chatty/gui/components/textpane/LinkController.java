
package chatty.gui.components.textpane;

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
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.Emoticons;
import chatty.util.api.usericons.Usericon;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
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
public class LinkController extends MouseAdapter implements MouseMotionListener {
    
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
    
    private Object prevHoverObject;
    
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
        EmoticonImage emoteImage = getEmoticonImage(element);
        Usericon usericon = getUsericon(element);
        if (emoteImage != null) {
            showPopup(e, makeEmoticonPopupText(emoteImage), emoteImage.getImageIcon().getIconWidth());
        } else if (usericon != null) {
            showPopup(e, makeUsericonPopupText(usericon), usericon.image.getIconWidth());
        } else {
            hidePopup();
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
            m = new UserContextMenu(user, getAutoModMsgId(element), contextMenuListener);
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
        hidePopup();
    }
    
    private final JLabel popupLabel = new JLabel();
    private Popup popup;
    
    private void showPopup(MouseEvent e, String text, int width) {
        if (popup == null) {
            try {
                popupLabel.setText(text);
                Dimension labelSize = popupLabel.getPreferredSize();
                Element element = getElement(e);
                JTextPane textPane = (JTextPane) e.getSource();
                Rectangle r = textPane.modelToView(element.getStartOffset());
                r.translate(textPane.getLocationOnScreen().x, textPane.getLocationOnScreen().y);
                r.translate(0, - labelSize.height - 3);
                r.translate(width / 2 - labelSize.width / 2, 0);
                popup = PopupFactory.getSharedInstance().getPopup(e.getComponent(), popupLabel, r.x, r.y);
                popup.show();
            } catch (BadLocationException ex) {
                Logger.getLogger(LinkController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void hidePopup() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
    }
    
    private void hidePopupIfDifferentElement(Object obj) {
        if (prevHoverObject != obj) {
            hidePopup();
        }
        prevHoverObject = obj;
    }
    
    private static final String POPUP_HTML_PREFIX = "<html><body style='text-align:center;border:1px solid #000;padding:3px 5px 3px 5px;'>";
    
    private static String makeEmoticonPopupText(EmoticonImage emoticonImage) {
        Emoticon emote = emoticonImage.getEmoticon();
        String emoteInfo = "";
        if (!emote.hasStreamSet() && emote.hasEmotesetInfo()) {
            emoteInfo = emote.getEmotesetInfo() + " Emoticon";
        } else if (Emoticons.isTurboEmoteset(emote.emoteSet)) {
            emoteInfo = "Turbo/Prime";
        } else if (emote.hasStreamSet()) {
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
        return String.format("%s%s<br /><span style='font-weight:normal'>%s</span>",
                POPUP_HTML_PREFIX,
                emote.type == Emoticon.Type.EMOJI ? emote.stringId : emote.code,
                emoteInfo);
    }
    
    private static String makeUsericonPopupText(Usericon usericon) {
        String info;
        if (!usericon.metaTitle.isEmpty()) {
            info = POPUP_HTML_PREFIX+"Badge: "+usericon.metaTitle;
        } else {
            info = POPUP_HTML_PREFIX+"Badge: "+usericon.type.label;
        }
        if (usericon.source == Usericon.SOURCE_CUSTOM) {
            info += " (Custom)";
        }
        return info;
    }
    
    public void cleanUp() {
        hidePopup();
    }
    
}