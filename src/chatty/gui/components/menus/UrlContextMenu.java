
package chatty.gui.components.menus;

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
    
    private final static Pattern channelFromUrl =
            Pattern.compile("(?i)(?:https?://)?(?:[a-z]+.)?twitch.tv/([a-z_0-9]+)(?:/.*)?");
    
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
        
        Matcher m = channelFromUrl.matcher(url);
        if (m.matches()) {
            channel = m.group(1);
            addSeparator();
            addItem("join", "Join #"+channel);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            if (e.getActionCommand().equals("join")) {
                listener.urlMenuItemClicked(e, channel);
            }
            else {
                listener.urlMenuItemClicked(e, url);
            }
        }
    }
    
}