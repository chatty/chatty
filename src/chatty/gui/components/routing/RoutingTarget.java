
package chatty.gui.components.routing;

import chatty.User;
import chatty.gui.DockStyledTabContainer;
import chatty.gui.MainGui;
import chatty.gui.StyleManager;
import chatty.gui.StyleServer;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.RoutingTargetContextMenu;
import chatty.gui.components.textpane.ChannelTextPane;
import chatty.gui.components.textpane.InfoMessage;
import chatty.gui.components.textpane.Message;
import chatty.util.Timestamp;
import chatty.util.colors.ColorCorrector;
import chatty.util.dnd.DockManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

/**
 * The tab/text pane that messages get added to.
 * 
 * @author tduva
 */
public class RoutingTarget {

    private final TextPane textPane;
    private final DockStyledTabContainer<JComponent> content;
    
    private int numMessages;
    
    public RoutingTarget(String id, String title, MainGui main, StyleManager styles, DockManager dock, ContextMenuListener contextMenuListener) {
        
        StyleServer modifiedStyleServer = new StyleServer() {

            @Override
            public Color getColor(String type) {
                return styles.getColor(type);
            }

            @Override
            public MutableAttributeSet getStyle(String type) {
                if (type.equals("settings")) {
                    MutableAttributeSet attr = new SimpleAttributeSet(styles.getStyle(type));
                    // For crossing out messages for timeouts, but never show separate message
                    attr.addAttribute(ChannelTextPane.Setting.SHOW_BANMESSAGES, false);
                    attr.addAttribute(ChannelTextPane.Setting.CHANNEL_LOGO_SIZE, 22);
                    return attr;
                }
                return styles.getStyle(type);
            }

            @Override
            public Font getFont(String type) {
                return styles.getFont(type);
            }

            @Override
            public Timestamp getTimestampFormat() {
                return styles.getTimestampFormat();
            }

            @Override
            public ColorCorrector getColorCorrector() {
                return styles.getColorCorrector();
            }
        };
        
        this.textPane = new TextPane(main, modifiedStyleServer, true);
        JScrollPane scroll = new JScrollPane(textPane);
        textPane.setScrollPane(scroll);
        textPane.setContextMenuListener(new ContextMenuAdapter(contextMenuListener) {
            
            @Override
            public void menuItemClicked(ActionEvent e) {
                if (e.getActionCommand().equals("clearHighlights")) {
                    clear();
                }
                super.menuItemClicked(e);
            }
            
        });
        textPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
//                numMessages = 0;
            }
        });
        
        this.content = new DockStyledTabContainer<>(scroll, id, dock);
        this.content.setId(id);
        this.content.setTitle(title);
    }
    
    public void addMessage(Message message) {
        textPane.printMessage(message);
        if (!content.isContentVisible()) {
            content.setNewMessage(true);
        }
        numMessages++;
    }
    
    public void addInfoMessage(InfoMessage message) {
        textPane.printInfoMessage(message);
        if (!content.isContentVisible()) {
            content.setNewMessage(true);
        }
        numMessages++;
    }
    
    /**
     * This should be fine as long as the text pane only searches for lines
     * containing the same User object, so it doesn't affect messages with the
     * same username from another channel.
     */
    public void addBan(User user, long duration, String reason, String targetMsgId) {
        textPane.userBanned(user, duration, reason, targetMsgId);
    }
    
    public void refreshStyles() {
        textPane.refreshStyles();
    }
    
    public void clear() {
        numMessages = 0;
        textPane.clearAll();
    }
    
    public DockStyledTabContainer<JComponent> getContent() {
        return content;
    }
    
    public int getNumMessages() {
        return numMessages;
    }

    void setName(String name) {
        content.setTitle(name);
    }
    
    class TextPane extends ChannelTextPane {
        
        public TextPane(MainGui main, StyleServer styleServer, boolean startAtBottom) {
            super(main, styleServer, ChannelTextPane.Type.REGULAR, startAtBottom);
            
            // Overriding constructor is required to set the custom context menu
            linkController.setContextMenuCreator(() -> new RoutingTargetContextMenu());
        }
        
    }
    
}
