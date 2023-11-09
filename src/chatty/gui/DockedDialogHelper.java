
package chatty.gui;

import chatty.Helper;
import chatty.gui.components.Channel;
import chatty.gui.components.menus.ContextMenu;
import chatty.util.MiscUtil;
import chatty.util.dnd.DockContent;
import chatty.util.settings.Settings;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Manages docking related actions, such as docking, undocking, settings and so
 * on for Info Panels. Info Panels can switch between a regular dialog (what
 * they always have been in Chatty) and being docked (a DockContent that is
 * added as a tab in the new system). The Info Panel would usually have a JPanel
 * that holds the content and can be either added to the dialog or the dock.
 *
 * For this to work, the Info Panel must overwrite some of it's methods
 * (setVisible, isVisible) which should call the helper's methods instead, so
 * that they can be used as before in the docked context (e.g. isVisible would
 * be true if the tab is currently added). It also has to provide some methods
 * to the helper via the DockedDialog constructor argument so the helper can
 * perform some actions such as making the actual dialog visible.
 *
 * For changing settings a context menu is used. The Info Panel can provide it's
 * own menu and call menuAction when an menu item is clicked on, or install a
 * menu via the helper.
 *
 * @author tduva
 */
public class DockedDialogHelper {
    
    public final static int DOCKED = 1 << 0;
    public final static int AUTO_OPEN = 1 << 1;
    public final static int AUTO_OPEN_ACTIVITY = 1 << 2;
    public final static int FIXED_CHANNEL = 1 << 3;
    
    private final DockedDialog dialog;
    private final DockContent content;
    private final Channels channels;
    private final Settings settings;
    private final MainGui gui;
    
    private boolean isDocked;
    private boolean autoOpen;
    private boolean autoOpenActivity;
    private boolean fixedChannel;
    private String currentChannel;
    
    private Consumer<String> channelChangeListener;
    
    public DockedDialogHelper(DockedDialog dialog, MainGui gui, Channels channels, Settings settings) {
        this.dialog = dialog;
        this.content = dialog.getContent();
        this.gui = gui;
        this.channels = channels;
        this.settings = settings;
    }
    
    public DockContent getContent() {
        return content;
    }
    
    public void setVisible(boolean visible, boolean switchTo) {
        if (visible == isVisible()) {
            return;
        }
        if (isDocked) {
            if (visible) {
                channels.addContent(content);
                if (switchTo) {
                    channels.getDock().setActiveContent(content);
                }
            }
            else {
                channels.getDock().removeContent(content);
            }
        }
        else {
            if (!switchTo) {
                GuiUtil.setNonAutoFocus(dialog.getWindow());
            }
            dialog.setVisible(visible);
        }
        if (visible) {
            /**
             * This will run the method that would normally be used to open the
             * window. This will of course likely run this method again, but
             * only if visible is not already true (guard-condition at the top).
             * 
             * If already opened through the open method, then it will be run
             * twice, but that shouldn't be an issue.
             */
            gui.openWindow(dialog.getWindow());
        }
    }
    
    public boolean isVisible() {
        if (isDocked) {
            return channels.getDock().hasContent(content);
        }
        return dialog.isVisible();
    }
    
    public boolean isContentVisible() {
        if (isDocked) {
            return channels.getDock().isContentVisible(content);
        }
        return dialog.isVisible();
    }
    
    public void setDocked(boolean docked) {
        if (isDocked != docked && isVisible()) {
            // Will make change visible as well, so only do if already visible
            toggleDock();
        }
        // Always update value, even if not currently visible
        isDocked = docked;
        dialog.dockedChanged();
    }
    
    public void toggleDock() {
        if (isDocked) {
            channels.getDock().removeContent(content);
            dialog.addComponent(content.getComponent());
            isDocked = false;
            gui.setWindowPosition(dialog.getWindow());
            dialog.setVisible(true);
        }
        else {
            dialog.removeComponent(content.getComponent());
            channels.addContent(content);
            channels.getDock().setActiveContent(content);
            isDocked = true;
            dialog.setVisible(false);
        }
        saveSettings();
        dialog.dockedChanged();
    }
    
    public void menuAction(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "dockToggleDocked":
                toggleDock();
                break;
            case "dockToggleAutoOpen":
                autoOpen = !autoOpen;
                saveSettings();
                break;
            case "dockToggleAutoOpenActivity":
                autoOpenActivity = !autoOpenActivity;
                saveSettings();
                break;
            case "dockToggleFixedChannel":
                fixedChannel = !fixedChannel;
                saveSettings();
                break;
        }
        if (e.getActionCommand().startsWith("dockChangeChannel.")) {
            String channel = e.getActionCommand().substring("dockChangeChannel.".length());
            if (!channel.isEmpty()) {
                if (channelChangeListener != null) {
                    channelChangeListener.accept(channel);
                }
            }
        }
    }
    
    private void saveSettings() {
        int value = isDocked ? DOCKED : 0;
        value = value | (autoOpen ? AUTO_OPEN : 0);
        value = value | (autoOpenActivity ? AUTO_OPEN_ACTIVITY : 0);
        value = value | (fixedChannel ? FIXED_CHANNEL : 0);
        settings.mapPut("dock", content.getId(), value);
    }
    
    public void loadSettings() {
        int value = 0;
        Object o = settings.mapGet("dock", content.getId());
        if (o != null && o instanceof Number) {
            value = ((Number) o).intValue();
        }
        setDocked(MiscUtil.isBitEnabled(value, DOCKED));
        setVisible(MiscUtil.isBitEnabled(value, AUTO_OPEN), false);
        autoOpenActivity = MiscUtil.isBitEnabled(value, AUTO_OPEN);
        fixedChannel = MiscUtil.isBitEnabled(value, FIXED_CHANNEL);
    }
    
    public void loadTabSettings() {
        if (content instanceof DockStyledTabContainer) {
            ((DockStyledTabContainer)content).setSettings(0, settings.getLong("tabsMessage"), 0, 0, 0, -1);
        }
    }
    
    public void setActivity() {
        if (autoOpenActivity) {
            setVisible(true, false);
        }
    }
    
    public void setNewMessage() {
        if (isDocked() && !channels.getDock().isContentVisible(content) && content instanceof DockStyledTabContainer) {
            ((DockStyledTabContainer)content).setNewMessage(true);
        }
    }
    
    public void resetNewMessage() {
        if (content instanceof DockStyledTabContainer) {
            ((DockStyledTabContainer)content).setNewMessage(false);
        }
    }
    
    public boolean isDocked() {
        return isDocked;
    }
    
    public boolean autoOpen() {
        return autoOpen;
    }
    
    public boolean autoOpenActivity() {
        return autoOpenActivity;
    }
    
    public void installContextMenu(Component c) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openContextMenu(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                openContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                openContextMenu(e);
            }
        });
    }
    
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            ContextMenu menu = new MyContextMenu();
            addToContextMenu(menu);
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    public void addToContextMenu(ContextMenu menu) {
        //--------------------------
        // Dock
        //--------------------------
        menu.addCheckboxItem("dockToggleDocked", "Dock as tab", isDocked);
        //--------------------------
        // Channel Change
        //--------------------------
        if (channelChangeListener != null) {
            addChannelSelectionToContextMenu(menu,
                    channels.getChannelsOfType(Channel.Type.CHANNEL),
                    fixedChannel,
                    false, false, currentChannel);
        }
    }
    
    public static void addChannelSelectionToContextMenu(ContextMenu menu,
            List<Channel> open,
            boolean fixedChannelEnabled,
            boolean addAllEntry,
            boolean showAll,
            String currentChannel) {
        String optionsLabel = "Options";
        String openChansLabel = "Channel";
        JMenu changeChanMenu = new JMenu(optionsLabel);
        JMenu openChansMenu = new JMenu(openChansLabel);
        openChansMenu.setToolTipText("All open chans, active tabs marked with *");
        menu.registerSubmenu(changeChanMenu);
        menu.registerSubmenu(openChansMenu);

        Collections.sort(open, (o1, o2) -> {
            // Stream name should always be available for regular channels
            return o1.getChannel().compareTo(o2.getChannel());
        });
        if (!open.isEmpty()) {
            menu.add(openChansMenu);
            if (addAllEntry) {
                menu.addCheckboxItem("dockChannelsShowAll", "All", openChansLabel, showAll);
                menu.addSeparator(openChansLabel);
            }
        }
        for (Channel chan : open) {
            // Add menu item for each channel, mark visible with *
            menu.addRadioItem("dockChangeChannel." + chan.getChannel(),
                    chan.getChannel() + (chan.getDockContent().isContentVisible() ? "*" : ""),
                    "chansGroup", openChansLabel);
        }
        if (currentChannel != null) {
            JMenuItem selectedChannel = menu.getItem("dockChangeChannel." + currentChannel);
            if (selectedChannel != null) {
                selectedChannel.setSelected(true);
            }
        }
        menu.add(changeChanMenu);
        menu.addCheckboxItem("dockToggleFixedChannel", "Channel Fixed", optionsLabel, fixedChannelEnabled);
        menu.getItem("dockToggleFixedChannel").setToolTipText("Stay on the channel it was opened on (or the first channel joined if it was open on start)");
    }
    
    private class MyContextMenu extends ContextMenu {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            menuAction(e);
        }
        
    }
    
    public void setChannelChangeListener(Consumer<String> listener) {
        channelChangeListener = listener;
    }
    
    public void channelChanged(String channel) {
        if (channelChangeListener != null && !fixedChannel) {
            channelChangeListener.accept(channel);
        }
    }
    
    /**
     * Set the current channel for the context menu.
     * 
     * @param channel 
     */
    public void setCurrentChannel(String channel) {
        currentChannel = Helper.toChannel(channel);
    }
    
    public static abstract class DockedDialog {
        
        /**
         * Must set dialog visibility (super method, since the dialog's method
         * has to be overwritten).
         * 
         * @param visible 
         */
        public abstract void setVisible(boolean visible);
        
        /**
         * Must get dialog visibility (super method, since the dialog's method
         * has to be overwritten).
         * 
         * @return 
         */
        public abstract boolean isVisible();
        
        /**
         * Add the content component to the dialog.
         * 
         * @param comp 
         */
        public abstract void addComponent(Component comp);
        
        /**
         * Remove the content component from the dialog.
         * 
         * @param comp 
         */
        public abstract void removeComponent(Component comp);
        
        /**
         * Get the dialog object itself.
         * 
         * @return 
         */
        public abstract Window getWindow();
        
        /**
         * Get the DockContent for this dialog. This is probably a JPanel that
         * holds the content and can be either added to the dialog or the dock.
         * 
         * @return 
         */
        public abstract DockContent getContent();
    
        public void dockedChanged() {
            
        }
    }
    
}
