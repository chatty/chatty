
package chatty.gui.components;

import chatty.User;
import chatty.gui.DockStyledTabContainer;
import chatty.gui.DockedDialogHelper;
import chatty.gui.DockedDialogManager;
import chatty.gui.MainGui;
import chatty.gui.StyleManager;
import chatty.gui.StyleServer;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.StreamChatContextMenu;
import chatty.gui.components.textpane.ChannelTextPane;
import chatty.gui.components.textpane.Message;
import chatty.util.dnd.DockContent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * Simple dialog that contains a ChannelTextPane with stream chat features
 * enabled (optional timeout of messages). Can have messages from several
 * channels redirected to it.
 * 
 * @author tduva
 */
public class StreamChat extends JDialog {
    
    private final DockedDialogHelper helper;
    private final DockStyledTabContainer content;
    private final ChannelTextPane textPane;
    private final ContextMenuListener contextMenuListener;
    
    public StreamChat(MainGui g, StyleManager styles, ContextMenuListener contextMenuListener,
            boolean startAtBottom, DockedDialogManager dockedDialogs) {
        super(g);
        this.contextMenuListener = contextMenuListener;
        setTitle("Stream Chat");

        textPane = new TextPane(g, styles, startAtBottom);
        textPane.setContextMenuListener(new ContextMenuAdapter(contextMenuListener) {
            
            @Override
            public void menuItemClicked(ActionEvent e) {
                if (e.getActionCommand().equals("clearHighlights")) {
                    textPane.clearAll();
                }
                helper.menuAction(e);
                super.menuItemClicked(e);
            }
            
        });
        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        textPane.setScrollPane(scroll);
        
        add(scroll, BorderLayout.CENTER);
        
        content = dockedDialogs.createStyledContent(scroll, "Stream Chat", "-streamChat-");
        
        helper = dockedDialogs.createHelper(new DockedDialogHelper.DockedDialog() {
            
            @Override
            public void setVisible(boolean visible) {
                StreamChat.super.setVisible(visible);
            }

            @Override
            public boolean isVisible() {
                return StreamChat.super.isVisible();
            }

            @Override
            public void addComponent(Component comp) {
                add(comp, BorderLayout.CENTER);
            }

            @Override
            public void removeComponent(Component comp) {
                remove(comp);
            }

            @Override
            public Window getWindow() {
                return StreamChat.this;
            }

            @Override
            public DockContent getContent() {
                return content;
            }
            
        });
        
        setSize(400, 200);
    }
    
    @Override
    public void setVisible(boolean visible) {
        helper.setVisible(visible, true);
    }
    
    @Override
    public boolean isVisible() {
        return helper.isVisible();
    }
    
    public void printMessage(Message message) {
        textPane.printMessage(message);
        if (helper.isDocked() && !content.isContentVisible()) {
            content.setNewMessage(true);
        }
    }
    
    public void userBanned(User user, long duration, String reason, String id) {
        textPane.userBanned(user, duration, reason, id);
    }
    
    public void setMessageTimeout(int seconds) {
        textPane.setMessageTimeout(seconds);
    }
    
    public void refreshStyles() {
        textPane.refreshStyles();
    }
    
    public void clear() {
        textPane.clearAll();
    }
    
    /**
     * Normal channel text pane modified a bit to fit the needs for this.
     */
    class TextPane extends ChannelTextPane {
        
        public TextPane(MainGui main, StyleServer styleServer, boolean startAtBottom) {
            // Enables the "special" parameter to be able to remove old lines
            super(main, styleServer, ChannelTextPane.Type.STREAM_CHAT, startAtBottom);
            
            // Overriding constructor is required to set the custom context menu
            linkController.setContextMenuCreator(() -> new StreamChatContextMenu(helper.isDocked()));
        }
        
    }
    
}
