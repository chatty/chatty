
package chatty.gui.components.menus;

import chatty.Helper;
import chatty.Room;
import chatty.User;
import java.awt.event.ActionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Context Menu for URLs.
 * 
 * @author tduva
 */
public class UrlContextMenu extends ContextMenu {
    
    private final ContextMenuListener listener;
    private final String url;
    private String channel;
    private User dummyUser;
    
    /**
     * Contructs a new Context Menu.
     * 
     * @param url The URL
     * @param deleted Whether the URL was deleted (timed out/banned)
     * @param listener The listener for this menu
     */
    public UrlContextMenu(String url, boolean deleted, ContextMenuListener listener) {
        this.url = url;
        this.listener = listener;
        
        if (deleted) {
            addItem("","Warning: Link may be malicious");
            addSeparator();
        }
        addItem("open", "Open link");
        addItem("copy", "Copy to clipboard");
        
        channel = Helper.getChannelFromUrl(url);
        if (channel != null && Helper.isValidChannel(channel)) {
            addSeparator();
            addItem("join", "Join #"+channel);
            Helper.TwitchPopoutUrlInfo popoutInfo = Helper.getPopoutUrlInfo(url);
            if (popoutInfo != null && popoutInfo.username != null) {
                addSeparator();
                dummyUser = new User(popoutInfo.username, Room.EMPTY);
                addItem("userinfo.#"+channel, String.format("User: %s in #%s",
                        popoutInfo.username, channel));
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            if (e.getActionCommand().equals("join")) {
                listener.urlMenuItemClicked(e, channel);
            }
            else if (e.getActionCommand().startsWith("userinfo.")) {
                listener.userMenuItemClicked(e, dummyUser, null, null);
            }
            else {
                listener.urlMenuItemClicked(e, url);
            }
        }
    }
    
}