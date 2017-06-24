
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
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.usericons.Usericon;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
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
            String url = getUrl(e);
            if (url != null && !isUrlDeleted(e)) {
                if (linkListener != null) {
                    linkListener.linkClicked(url);
                }
                return;
            }
            User user = getUser(e);
            if (user != null) {
                for (UserListener listener : userListener) {
                    listener.userClicked(user, getMsgId(e), getAutoModMsgId(e), e);
                }
                return;
            }
            EmoticonImage emote = getEmoticon(e);
            if (emote != null) {
                for (UserListener listener : userListener) {
                    listener.emoteClicked(emote.getEmoticon(), e);
                }
                return;
            }
            Usericon usericon = getUsericon(e);
            if (usericon != null) {
                for (UserListener listener : userListener) {
                    listener.usericonClicked(usericon, e);
                }
            }
        }
        else if (e.isPopupTrigger()) {
            openContextMenu(e);
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
        if (mouseClickedListener != null && e.getClickCount() == 1
                && !e.isAltDown() && !e.isAltGraphDown()) {
            // Doing this on mousePressed will prevent selection of text,
            // because this is used to change the focus to the input
            mouseClickedListener.mouseClicked();
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {

        JTextPane text = (JTextPane)e.getSource();
        
        String url = getUrl(e);
        if ((url != null && !isUrlDeleted(e)) || getUser(e) != null ||
                getEmoticon(e) != null || getUsericon(e) != null) {
            text.setCursor(HAND_CURSOR);
        } else {
            text.setCursor(NORMAL_CURSOR);
        }
    }

    /**
     * Gets the URL from the MouseEvent (if there is any).
     * 
     * @param e
     * @return The URL or null if none was found.
     */
    private String getUrl(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (String)(attributes.getAttribute(HTML.Attribute.HREF));
        }
        return null;
    }
    
    private boolean isUrlDeleted(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            Boolean deleted = (Boolean)attributes.getAttribute(ChannelTextPane.Attribute.URL_DELETED);
            if (deleted == null) {
                return false;
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * Gets the User object from the MouseEvent (if there is any).
     * 
     * @param e
     * @return The User object or null if none was found.
     */
    private User getUser(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (User)(attributes.getAttribute(ChannelTextPane.Attribute.USER));
        }
        return null;
    }
    
    private String getMsgId(MouseEvent e) {
        return (String) getAttributes(e).getAttribute(ChannelTextPane.Attribute.ID);
    }
    
    private String getAutoModMsgId(MouseEvent e) {
        return (String) getAttributes(e).getAttribute(ChannelTextPane.Attribute.ID_AUTOMOD);
    }
    
    private EmoticonImage getEmoticon(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (EmoticonImage)(attributes.getAttribute(ChannelTextPane.Attribute.EMOTICON));
        }
        return null;
    }
    
    private Usericon getUsericon(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (Usericon)(attributes.getAttribute(ChannelTextPane.Attribute.USERICON));
        }
        return null;
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
             * http://stackoverflow.com/questions/24036650/detecting-image-on-current-mouse-position-only-works-on-part-of-image
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
    
    /**
     * Gets the attributes from the element in the document the mouse is
     * pointing at.
     * 
     * @param e
     * @return The attributes of this element or null if the mouse wasn't
     *          pointing at an element
     */
    public static AttributeSet getAttributes(MouseEvent e) {
        Element element = getElement(e);
        if (element != null) {
            return element.getAttributes();
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
        User user = getUser(e);
        String url = getUrl(e);
        EmoticonImage emote = getEmoticon(e);
        Usericon usericon = getUsericon(e);
        JPopupMenu m;
        if (user != null) {
            m = new UserContextMenu(user, getAutoModMsgId(e), contextMenuListener);
        }
        else if (url != null) {
            m = new UrlContextMenu(url, isUrlDeleted(e), contextMenuListener);
        }
        else if (emote != null) {
            m = new EmoteContextMenu(emote, contextMenuListener);
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
    }
    
}