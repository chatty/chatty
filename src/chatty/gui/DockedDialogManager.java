
package chatty.gui;

import chatty.gui.components.Channel;
import chatty.gui.components.menus.TabContextMenu;
import chatty.util.BatchAction;
import chatty.util.Debugging;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockContentContainer;
import chatty.util.dnd.DockManager;
import chatty.util.settings.Settings;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * Info Panels that can be docked are registered in this, so settings can be
 * loaded and panels can be opened/closed by their id.
 * 
 * Each Info Panel contains a helper that manages all the docking related stuff.
 * 
 * @author tduva
 */
public class DockedDialogManager {

    private final Map<String, DockedDialogHelper> dialogs = new HashMap<>();
    private final Channels channels;
    private final MainGui gui;
    private final Settings settings;
    
    public DockedDialogManager(MainGui gui, Channels channels, Settings settings) {
        this.gui = gui;
        this.channels = channels;
        this.settings = settings;
        settings.addSettingChangeListener((setting, type, value) -> {
            if (setting.equals("tabsMessage")) {
                SwingUtilities.invokeLater(() -> loadTabSettings());
            }
        });
    }
    
    public DockManager getDockManager() {
        return channels.getDock();
    }
    
    public DockContent createContent(JComponent component, String title, String id) {
        DockContent content = new DockContentContainer(title, component, channels.getDock()) {
            
            @Override
            public JPopupMenu getContextMenu() {
                return getPopupMenu(this);
            }
            
        };
        content.setId(id);
        return content;
    }
    
    public DockStyledTabContainer createStyledContent(JComponent component, String title, String id) {
        DockStyledTabContainer content = new DockStyledTabContainer(component, title, getDockManager()) {
            
            @Override
            public JPopupMenu getContextMenu() {
                return getPopupMenu(this);
            }
            
        };
        content.setId(id);
        return content;
    }
    
    private JPopupMenu getPopupMenu(DockContent content) {
        return new TabContextMenu(gui.contextMenuListener,
                content,
                Channels.getCloseTabs(channels, content, settings.getBoolean("closeTabsSameType")),
                settings);
    }
    
    public DockedDialogHelper createHelper(DockedDialogHelper.DockedDialog dialog) {
        DockedDialogHelper helper = new DockedDialogHelper(dialog, gui, channels, settings);
        helper.loadTabSettings();
        register(helper);
        return helper;
    }
    
    public void register(DockedDialogHelper helper) {
        dialogs.put(helper.getContent().getId(), helper);
    }
    
    public void loadSettings() {
        for (DockedDialogHelper helper : dialogs.values()) {
            helper.loadSettings();
        }
    }
    
    public void loadTabSettings() {
        for (DockedDialogHelper helper : dialogs.values()) {
            helper.loadTabSettings();
        }
    }
    
    public void openInDock(String id) {
        if (dialogs.containsKey(id)) {
            DockedDialogHelper helper = dialogs.get(id);
            helper.setDocked(true);
            helper.setVisible(true, false);
        }
    }
    
    public void closeFromDock(String id) {
        if (dialogs.containsKey(id)) {
            DockedDialogHelper helper = dialogs.get(id);
            if (helper.isDocked()) {
                helper.setVisible(false, false);
            }
        }
    }
    
    private final Object channelChangedUnique = new Object();
    
    public void activeContentChanged() {
        DockContent content = channels.getActiveContent();
        if (content.getComponent() instanceof Channel) {
            Channel chan = (Channel) content.getComponent();
            if (chan.getType() == Channel.Type.CHANNEL) {
                String channel = chan.getChannel();
                Debugging.println("changechan", "Set channel to %s", channel);
                /**
                 * Batch up, only using the last action, so that quickly
                 * changing channels doesn't trigger too many change
                 * notifications.
                 */
                BatchAction.queue(channelChangedUnique, 300, true, true, () -> {
                    for (DockedDialogHelper helper : dialogs.values()) {
                        helper.channelChanged(channel);
                    }
                });
            }
        }
        else {
            /**
             * Overwrite with an action doing nothing, this way if the content
             * quickly changes to something that that is not a Channel, it won't
             * change channel. For example clicking from another window into a
             * docked previously not active Admin Panel, which could first make
             * a channel in the window active, then very quickly after that the
             * Admin Panel. This still doesn't help in every case, but at least
             * in some.
             */
            BatchAction.queue(channelChangedUnique, 300, true, true, () -> {
                Debugging.println("changechan", "Prevented channel change");
            });
        }
    }
    
}
