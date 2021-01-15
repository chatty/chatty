
package chatty.gui;

import chatty.Helper;
import chatty.Helper.IntegerPair;
import chatty.Room;
import chatty.gui.components.Channel;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.TabContextMenu;
import chatty.lang.Language;
import chatty.util.Debugging;
import chatty.util.IconManager;
import chatty.util.KeyChecker;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockListener;
import chatty.util.dnd.DockManager;
import chatty.util.dnd.DockPath;
import chatty.util.dnd.DockPathEntry;
import chatty.util.dnd.DockSetting;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.util.*;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import chatty.util.dnd.DockPopout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * Managing the Channel objects in the main window and popouts, providing a
 * default channel while no other is added.
 * 
 * TODO:
 * - Switching tabs in right-hand split while the focus is not on the tab (e.g.
 * input) will first switch focus to active tab in left-hand split
 * 
 * @author tduva
 */
public class Channels {

    private final MainGui gui;
            
    private ChangeListener changeListener;
    
    /**
     * Saves all added channels by name.
     */
    private final HashMap<String, Channel> channels = new HashMap<>();
    
    /**
     * Saves attributes of closed popout dialogs.
     */
    private final List<LocationAndSize> dialogsAttributes = new ArrayList<>();
    private final DockManager dock;
    
    /**
     * The default channel does not represent an actual channel, but is just a
     * placeholder for when the main window does not contain a channel.
     */
    private Channel defaultChannel;
    private final StyleManager styleManager;
    private final ContextMenuListener contextMenuListener;
    private final MouseClickedListener mouseClickedListener = new MyMouseClickedListener();
    
    /**
     * Default width of the userlist, given to Channel objects when created.
     */
    private int defaultUserlistWidth = 140;
    private int minUserlistWidth = 0;
    private boolean defaultUserlistVisibleState = true;
    private boolean chatScrollbarAlaways;
    
    private boolean savePopoutAttributes;
    private boolean closeLastChannelPopout;
    
    /**
     * The DockManager tracks the last active content, however that might not be
     * a Channel, so the last active Channel is tracked here.
     */
    private Channel lastActiveChannel;
    
    /**
     * Store which streams are currently live, since this is only updated when a
     * stream changes status, which could be before the channel tab is actually
     * added. This can be referred to when the tab is added to set the correct
     * initial value.
     */
    private final Set<String> liveStreams = new HashSet<>();
    
    public Channels(MainGui gui, StyleManager styleManager,
            ContextMenuListener contextMenuListener) {
        dock = new DockManager(new DockListener() {
            @Override
            public void activeContentChanged(DockPopout window, DockContent content, boolean focusChange) {
                if (content.getComponent() instanceof Channel) {
                    Channel c = (Channel) content.getComponent();
                    lastActiveChannel = c;
                    if (!focusChange) {
                        // Changing focus due to a focus change is dodgy, so don't
                        // do that
                        setInitialFocus(c);
                    }
                }
                // This also resets lastActiveChannel if necessary
                updateActiveContent();
                resetTab(content);
                /**
                 * Only inform listener if there is at least one channel and the
                 * main isn't empty. This is to prevent stuff from trying to
                 * update when the last overall channel is removed and the last
                 * content is popped out and the default channel has not been
                 * added yet. Let what handles the default channel call the
                 * listener.
                 */
                if ((!channels.isEmpty() || defaultChannel != null) && !dock.isMainEmpty()) {
                    channelChanged();
                }
                Debugging.println("dnda", "Path: %s", content.getPath());
            }
            
            @Override
            public void popoutOpened(DockPopout popout, DockContent content) {
                dockPopoutOpened(popout);
            }
            
            @Override
            public void popoutClosing(DockPopout popout) {
                if (KeyChecker.isPressed(KeyEvent.VK_SHIFT)) {
                    dock.closePopout(popout);
                }
                else if (KeyChecker.isPressed(KeyEvent.VK_CONTROL)) {
                    removeContents(dock.getContents(popout));
                }
                else {
                    switch (gui.getSettings().getString("popoutClose")) {
                        case "close":
                            removeContents(dock.getContents(popout));
                            break;
                        case "move":
                            dock.closePopout(popout);
                            break;
                        default:
                            ClosingDialog d = new ClosingDialog(popout);
                            if (d.getChoice().equals("close")) {
                                removeContents(dock.getContents(popout));
                            }
                            else if (d.getChoice().equals("move")) {
                                dock.closePopout(popout);
                            }
                            
                            if (d.shouldSaveChoice()) {
                                gui.getSettings().setString("popoutClose", d.getChoice());
                            }
                    }
                }
            }

            @Override
            public void popoutClosed(DockPopout popout, List<DockContent> contents) {
                dockPopoutClosed(popout, contents);
            }

            @Override
            public void contentAdded(DockContent content) {
                checkDefaultChannel();
            }

            @Override
            public void contentRemoved(DockContent content) {
                checkDefaultChannel();
            }

        });
        // One-time settings
        dock.setSetting(DockSetting.Type.POPOUT_ICONS, IconManager.getMainIcons());
        dock.setSetting(DockSetting.Type.POPOUT_PARENT, gui);
        this.styleManager = styleManager;
        this.contextMenuListener = contextMenuListener;
        this.gui = gui;
        addDefaultChannel();
        updateSettings();
        gui.getSettings().addSettingChangeListener((setting, type, value) -> {
            if (setting.startsWith("tab") || setting.equals("laf")) {
                SwingUtilities.invokeLater(() -> {
                    updateSettings();
                });
            }
        });
        KeyChecker.watch(KeyEvent.VK_SHIFT);
        KeyChecker.watch(KeyEvent.VK_CONTROL);
    }
    
    public DockManager getDock() {
        return dock;
    }
    
    public Component getComponent() {
        return dock.getBase();
    }
    
    private void channelTabClosed(Channel channel) {
        gui.client.closeChannel(channel.getChannel());
    }
    
    //==========================
    // Dock Settings
    //==========================
    private void updateSettings() {
        for (Channel chan : channels.values()) {
            updateSettings(chan);
        }
        dock.setSetting(DockSetting.Type.TAB_LAYOUT, getTabLayoutPolicyValue(gui.getSettings().getString("tabsLayout")));
        dock.setSetting(DockSetting.Type.TAB_PLACEMENT, getTabPlacementValue(gui.getSettings().getString("tabsPlacement")));
        dock.setSetting(DockSetting.Type.TAB_SCROLL, gui.getSettings().getBoolean("tabsMwheelScrolling"));
        dock.setSetting(DockSetting.Type.TAB_SCROLL_ANYWHERE, gui.getSettings().getBoolean("tabsMwheelScrollingAnywhere"));
        dock.setSetting(DockSetting.Type.TAB_ORDER, getTabOrderValue(gui.getSettings().getString("tabOrder")));
        dock.setSetting(DockSetting.Type.FILL_COLOR, UIManager.getColor("TextField.selectionBackground"));
        dock.setSetting(DockSetting.Type.LINE_COLOR, UIManager.getColor("TextField.selectionForeground"));
        dock.setSetting(DockSetting.Type.POPOUT_TYPE_DRAG, getPopoutTypeValue((int)gui.getSettings().getLong("tabsPopoutDrag")));
        dock.setSetting(DockSetting.Type.DIVIDER_SIZE, 7);
    }
    
    private int getTabLayoutPolicyValue(String type) {
        switch(type) {
            case "wrap": return JTabbedPane.WRAP_TAB_LAYOUT;
            case "scroll": return JTabbedPane.SCROLL_TAB_LAYOUT;
        }
        return JTabbedPane.WRAP_TAB_LAYOUT;
    }
    
    private int getTabPlacementValue(String location) {
        switch(location) {
            case "top": return JTabbedPane.TOP;
            case "bottom": return JTabbedPane.BOTTOM;
            case "left": return JTabbedPane.LEFT;
            case "right": return JTabbedPane.RIGHT;
        }
        return JTabbedPane.TOP;
    }
    
    private DockSetting.TabOrder getTabOrderValue(String order) {
        if (order.equals("alphabetical")) {
            return DockSetting.TabOrder.ALPHABETIC;
        }
        return DockSetting.TabOrder.INSERTION;
    }
    
    private DockSetting.PopoutType getPopoutTypeValue(int type) {
        switch (type) {
            case 1: return DockSetting.PopoutType.DIALOG;
            case 2: return DockSetting.PopoutType.FRAME;
        }
        return DockSetting.PopoutType.NONE;
    }
    
    private void updateSettings(Channel chan) {
        chan.getDockContent().setSettings(
                (int) gui.getSettings().getLong("tabsLive"),
                (int) gui.getSettings().getLong("tabsMessage"),
                (int) gui.getSettings().getLong("tabsHighlight"),
                (int) gui.getSettings().getLong("tabsStatus"),
                (int) gui.getSettings().getLong("tabsActive"));
    }
    
    //==========================
    // Change Channel state
    //==========================
    
    public void updateRoom(Room room) {
        Channel channel = channels.get(room.getChannel());
        if (channel != null) {
            if (channel.setRoom(room)) {
                Debugging.printlnf("Update Room: %s", room);
            }
        }
    }
    
    /**
     * Update tabs to show which one is active. Also implicitly (by calling
     * getActiveChannel()) resets lastACtiveChannel is necessary.
     */
    private void updateActiveContent() {
        if (!doesChannelExist(lastActiveChannel)) {
            resetLastActiveChannel();
        }
        Channel active = getActiveChannel();
        for (Channel chan : channels.values()) {
            chan.getDockContent().setActive(active == chan);
        }
        if (defaultChannel != null) {
            defaultChannel.getDockContent().setActive(active == defaultChannel);
        }
    }
    
    /**
     * Set channel to show a new highlight messages has arrived, changes color
     * of the tab. ONly if not currently active tab.
     * 
     * @param channel 
     */
    public void setChannelHighlighted(Channel channel) {
        if (!channel.getDockContent().hasNewHighlight()
                && !dock.isContentVisible(channel.getDockContent())) {
            channel.getDockContent().setNewHighlight(true);
        }
    }
    
    /**
     * Set channel to show a new message arrived, changes color of the tab.
     * Only if not currently active tab.
     * 
     * @param channel 
     */
    public void setChannelNewMessage(Channel channel) {
        if (!channel.getDockContent().hasNewMessages()
                && !channel.getDockContent().hasNewHighlight()
                && !dock.isContentVisible(channel.getDockContent())) {
            channel.getDockContent().setNewMessage(true);
        }
    }
    
    public void setStreamLive(String stream, boolean isLive) {
        if (!Helper.isValidStream(stream)) {
            return;
        }
        if (isLive) {
            liveStreams.add(stream);
        }
        else {
            liveStreams.remove(stream);
        }
        Channel chan = getExistingChannel(Helper.toChannel(stream));
        if (chan != null) {
            chan.getDockContent().setLive(isLive);
        }
    }
    
    /**
     * Reset state (color, title suffixes) to default.
     * 
     * @param content 
     */
    public void resetTab(DockContent content) {
        if (content instanceof DockStyledTabContainer) {
            ((DockStyledTabContainer)content).resetNew();
        }
    }
    
    /**
     * Set channel to new status available, adds a suffix to indicate that.
     * Only if not currently the active tab.
     * 
     * @param ownerChannel
     */
    public void setChannelNewStatus(String ownerChannel) {
        Collection<Channel> chans = getExistingChannelsByOwner(ownerChannel);
        // If any of the tabs for this channel is active, don't change
        for (Channel chan : chans) {
            if (dock.isContentVisible(chan.getDockContent())) {
                return;
            }
        }
        for (Channel chan : chans) {
            chan.getDockContent().setNewStatus(true);
        }
    }
    
    //==========================
    // Add/remove
    //==========================
    
    public Channel getChannel(Room room) {
        String channelName = room.getChannel();
        return getChannel(room, getTypeFromChannelName(channelName));
    }
    
    public Channel.Type getTypeFromChannelName(String name) {
        if (name.startsWith("#")) {
            return Channel.Type.CHANNEL;
        }
        else if (name.startsWith("$")) {
            return Channel.Type.WHISPER;
        }
        else if (name.startsWith("*")) {
            return Channel.Type.SPECIAL;
        }
        return Channel.Type.NONE;
    }
    
    /**
     * Gets the Channel object for the given channel name. If none exists, the
     * channel is automatically added.
     *
     * @param room Must not be null, but can be Room.EMPTY
     * @param type
     * @return
     */
    public Channel getChannel(Room room, Channel.Type type) {
        Channel panel = channels.get(room.getChannel());
        if (panel == null) {
            panel = addChannel(room, type);
        }
        else if (panel.setRoom(room)) {
            Debugging.println("Updating Channel Name to " + panel.getName());
        }
        return panel;
    }
    
    /**
     * This is the channel when no channel has been added yet.
     */
    private void addDefaultChannel() {
        defaultChannel = createChannel(Room.EMPTY, Channel.Type.NONE);
        addDefaultChannelToDock();
    }
    
    private void addDefaultChannelToDock() {
        // Ensure that it's being added in main window
        setTargetPath(defaultChannel.getDockContent(), "main");
        dock.addContent(defaultChannel.getDockContent());
    }
    
    private void setTargetPath(DockContent content) {
        setTargetPath(content, gui.getSettings().getString("tabsOpen"));
    }
    
    private void setTargetPath(DockContent content, String target) {
        content.setTargetPath(null);
        if (target.equals("active")) {
            DockContent active = dock.getActiveContent();
            if (active != null) {
                content.setTargetPath(active.getPath());
            }
        }
        else if (target.equals("active2")) {
            DockContent active = getActiveContent();
            if (active != null) {
                DockPopout popout = dock.getPopoutFromContent(active);
                if ((popout == null && gui.isActive())
                        || (popout != null && popout.getWindow().isActive())) {
                    content.setTargetPath(active.getPath());
                }
            }
        }
        // Main in default location
        if (content.getTargetPath() == null) {
            DockPath path = new DockPath(content);
            path.addParent(DockPathEntry.createPopout(null));
            content.setTargetPath(path);
        }
    }
    
    /**
     * Adds a channel with the given name. If the default channel is still there
     * it is used for this channel and renamed.
     * 
     * @param room
     * @param type
     * @return 
     */
    public Channel addChannel(Room room, Channel.Type type) {
        if (channels.get(room.getChannel()) != null) {
            return null;
        }
        Channel panel;
        if (defaultChannel != null) {
            // Reuse default channel
            panel = defaultChannel;
            defaultChannel = null;
            panel.setType(type);
            panel.setRoom(room);
            channels.put(room.getChannel(), panel);
        }
        else {
            // No default channel, so create a new one
            panel = createChannel(room, type);
            // Add to channels first so it's already known as "existing"
            channels.put(room.getChannel(), panel);
            setTargetPath(panel.getDockContent());
            dock.addContent(panel.getDockContent());
            if (type != Channel.Type.WHISPER) {
                dock.setActiveContent(panel.getDockContent());
            }
        }
        // Update after it has been added to "channels"
        updateActiveContent();
        channelChanged();
        return panel;
    }
    
    /**
     * Create and configure a Channel.
     * 
     * @param room Must not be null
     * @param type
     * @return
     */
    private Channel createChannel(Room room, Channel.Type type) {
        Channel channel = new Channel(room,type,gui,styleManager, contextMenuListener);
        channel.setDockContent(new DockChannelContainer(channel, dock, this, contextMenuListener));
        channel.init();
        channel.setUserlistWidth(defaultUserlistWidth, minUserlistWidth);
        channel.setMouseClickedListener(mouseClickedListener);
        channel.setScrollbarAlways(chatScrollbarAlaways);
        channel.setUserlistEnabled(defaultUserlistVisibleState);
        channel.getDockContent().setLive(liveStreams.contains(room.getStream()));
        updateSettings(channel);
        if (type == Channel.Type.SPECIAL || type == Channel.Type.WHISPER) {
            channel.setUserlistEnabled(false);
        }
        if (!gui.getSettings().getBoolean("inputEnabled")) {
            channel.toggleInput();
        }
        return channel;
    }
    
    public void removeChannel(final String channelName) {
        Channel channel = channels.get(channelName);
        if (channel == null) {
            return;
        }
        channels.remove(channelName);
        // Removing will automatically run checkDefaultChannel()
        dock.removeContent(channel.getDockContent());
        channel.cleanUp();
    }
    
    private void removeContents(Collection<DockContent> contents) {
        for (DockContent c : contents) {
            c.remove();
        }
    }
    
    /**
     * Add/remove/move default channel depending on whether the main window is
     * empty and any other channels are presently added.
     */
    private void checkDefaultChannel() {
        if (defaultChannel == null) {
            /**
             * Currently no default channel, check if it should be added
             */
            if (channels.isEmpty()) {
                addDefaultChannel();
            }
            else if (dock.isMainEmpty()) {
                if (!closeLastChannelPopout || !dock.closePopout()) {
                    addDefaultChannel();
                }
            }
        }
        else {
            /**
             * Default channel exists, check if it should be removed. In some
             * cases this may be called again when it's already removed, so also
             * check that's still in there, just to be safe.
             */
            if (dock.hasContent(defaultChannel.getDockContent())) {
                if (!channels.isEmpty() && dock.getContents(null).size() > 1) {
                    dock.removeContent(defaultChannel.getDockContent());
                    defaultChannel.cleanUp();
                    defaultChannel = null;
                }
            }
        }
        updateActiveContent();
        channelChanged();
    }
    
    //==========================
    // Popout
    //==========================
    
    /**
     * Popout the given content.
     * 
     * @param content The {@code Channel} to popout
     * @param window Open in window instead of dialog
     */
    public void popout(DockContent content, boolean window) {
        if (!content.canPopout()) {
            return;
        }
        /**
         * Setting the location/size should only be done for popouts not created
         * by drag, so not in dockPopoutOpened()
         */
        Point location = null;
        Dimension size = null;
        if (!dialogsAttributes.isEmpty()) {
            LocationAndSize attr = dialogsAttributes.remove(0);
            if (GuiUtil.isPointOnScreen(attr.location, 5, 5)) {
                location = attr.location;
            }
            size = attr.size;
        }
        
        dock.popout(content, window ? DockSetting.PopoutType.FRAME : DockSetting.PopoutType.DIALOG, location, size);
        gui.updateState(true);
    }
    
    /**
     * This is called by the DockManager when a popout is created. This also
     * includes popouts created by the DockManager itself (e.g. by drag).
     * 
     * @param popout 
     */
    private void dockPopoutOpened(DockPopout popout) {
        // Register hotkeys if necessary
        gui.popoutCreated(popout.getWindow());
        checkDefaultChannel();
    }
    
    private void dockPopoutClosed(DockPopout popout, List<DockContent> contents) {
        if (savePopoutAttributes) {
            Window window = popout.getWindow();
            dialogsAttributes.add(0, new LocationAndSize(
                    window.getLocation(), window.getSize()));
        }
//        if (!contents.isEmpty() && defaultChannel != null
//                && !contents.contains(defaultChannel.getDockContent())
//                && !channels.isEmpty()) {
//            dock.removeContent(defaultChannel.getDockContent());
//            defaultChannel.cleanUp();
//            defaultChannel = null;
//        }
        checkDefaultChannel();
        gui.updateState(true);
    }
    
    //--------------------------
    // Saving/loading attributes
    //--------------------------
    
    /**
     * Returns a list of Strings that contain the location/size of open dialogs
     * and of closed dialogs that weren't reused.
     * 
     * Format: "x,y;width,height"
     * 
     * @return 
     */
    public List getPopoutAttributes() {
        List<String> attributes = new ArrayList<>();
        for (DockPopout popout : dock.getPopouts()) {
            Window w = popout.getWindow();
            attributes.add(w.getX()+","+w.getY()
                    +";"+w.getWidth()+","+w.getHeight());
        }
        for (LocationAndSize attr : dialogsAttributes) {
            attributes.add(attr.location.x + "," + attr.location.y
                    + ";" + attr.size.width + "," + attr.size.height);
        }
        return attributes;
    }
    
    /**
     * Sets the attributes for popouts that will be opened, as a List of Strings
     * that have to be parsed into {@literal LocationAndSize} objects.
     * 
     * @param attributes 
     */
    public void setPopoutAttributes(List<String> attributes) {
        dialogsAttributes.clear();
        for (String attr : attributes) {
            if (attr == null) {
                continue;
            }
            String[] split = attr.split(";");
            if (split.length != 2) {
                continue;
            }
            IntegerPair location = Helper.getNumbersFromString(split[0]);
            IntegerPair size = Helper.getNumbersFromString(split[1]);
            if (location != null && size != null) {
                dialogsAttributes.add(
                        new LocationAndSize(
                                new Point(location.a, location.b),
                                new Dimension(size.a, size.b)));
            }
        }
    }
    
    //--------------------------
    // Popout settings
    //--------------------------
    
    public void setCloseLastChannelPopout(boolean close) {
        closeLastChannelPopout = close;
    }
    
    public void setSavePopoutAttributes(boolean save) {
        savePopoutAttributes = save;
        if (!save) {
            dialogsAttributes.clear();
        }
    }
    
    //==========================
    // Active content
    //==========================
    
    public void setChangeListener(ChangeListener listener) {
        changeListener = listener;
    }

    private void channelChanged() {
        if (changeListener != null) {
            changeListener.stateChanged(new ChangeEvent(this));
        }
    }
    
    /**
     * Return the currently active Channel, which means the one that has focus,
     * either because it is a popout dialog that has focus, or the currently
     * selected tab if the focus is on the main window.
     * 
     * <p>
     * If the focus is not on a window containing a Channel, then the current
     * tab of the main window is returned. If no Channel currently has focus, a
     * channel is selected from the existing channels.
     * </p>
     * 
     * @return The Channel object which is currently selected, could be null,
     * but that would likely be a bug, since there should always be at least one
     * channel
     */
    public Channel getActiveChannel() {
        // Prefer actual active
        DockContent activeContent = dock.getActiveContent();
        if (activeContent instanceof DockChannelContainer) {
            return ((DockChannelContainer)activeContent).getContent();
        }
        if (doesChannelExist(lastActiveChannel)) {
            return lastActiveChannel;
        }
        // Checked before, so must not be valid
        resetLastActiveChannel();
        return lastActiveChannel;
    }
    
    private void resetLastActiveChannel() {
        lastActiveChannel = null;
        // Find new channel
        if (!channels.isEmpty()) {
            // Try visible first (selected tab, popout)
            for (Channel chan : channels.values()) {
                if (dock.isContentVisible(chan.getDockContent())) {
                    lastActiveChannel = chan;
                    break;
                }
            }
            if (lastActiveChannel == null) {
                lastActiveChannel = channels.values().iterator().next();
            }
        }
        else {
            lastActiveChannel = defaultChannel;
        }
        Debugging.println("dnda", "Reset last active channel to: %s", lastActiveChannel);
    }
    
    /**
     * This used to be slightly different, but is now just returning
     * {@link getActiveChannel()}.
     *
     * @return
     */
    public Channel getLastActiveChannel() {
        return getActiveChannel();
    }
    
    /**
     * 
     * 
     * @return The active content, may be null, although that could probably
     * be considered a bug since there should always be an active content
     */
    public DockContent getActiveContent() {
        return dock.getActiveContent();
    }
    
    /**
     * Returns channel of the active tab in the main window. Falls back to the
     * overall active channel, but that normally shouldn't happen.
     * 
     * @return Can return null
     */
    public Channel getMainActiveChannel() {
        DockContent activeContent = getMainActiveContent();
        if (activeContent instanceof DockChannelContainer) {
            return ((DockChannelContainer)activeContent).getContent();
        }
        return null;
    }
    
    public DockContent getMainActiveContent() {
        return dock.getActiveContent(null);
    }
    
    /**
     * Returns all popouts with their currently active content.
     * 
     * @return The popouts, without the main base (which would be null)
     */
    public Map<DockPopout, DockContent> getActivePopoutContent() {
        Map<DockPopout, DockContent> result = new HashMap<>();
        for (DockPopout popout : dock.getPopouts()) {
            DockContent active = dock.getActiveContent(popout);
            if (active != null) {
                result.put(popout, active);
            }
        }
        return result;
    }
    
    //==========================
    // Get channels (other)
    //==========================
    
    /**
     * Return the channel from the given input box.
     *
     * @param input The reference to the input box.
     * @return The Channel object, or null if the given reference isn't an input
     * box
     */
    public Channel getChannelFromInput(Object input) {
        if (defaultChannel != null && input == defaultChannel.getInput()) {
            return defaultChannel;
        }
        for (String key : channels.keySet()) {
            Channel value = channels.get(key);
            if (input == value.getInput()) {
                return value;
            }
        }
        return null;
    }
    
    /**
     * Check if the given channel is added.
     *
     * @param channel
     * @return
     */
    public boolean isChannel(String channel) {
        if (channel == null) {
            return false;
        }
        return channels.get(channel) != null;
    }
    
    public Channel getExistingChannel(String channel) {
        return channels.get(channel);
    }
    
    /**
     * Check if the given channel is currently present.
     * 
     * @param channel
     * @return 
     */
    private boolean doesChannelExist(Channel channel) {
        return channel != null && (channels.containsValue(channel) || channel == defaultChannel);
    }
    
    public Collection<Channel> getExistingChannelsByOwner(String channel) {
        List<Channel> result = new ArrayList<>();
        for (Channel chan : channels.values()) {
            if (Objects.equals(chan.getOwnerChannel(), channel)) {
                result.add(chan);
            }
        }
        return result;
    }
    
    public Collection<Channel> channels() {
        return channels.values();
    }
    
    /**
     * Includes the defaultChannel that is there when no actual channel has been
     * added yet.
     * 
     * @return 
     */
    public Collection<Channel> allChannels() {
        if (channels.isEmpty() && defaultChannel != null) {
            Collection<Channel> result = new ArrayList<>();
            result.add(defaultChannel);
            return result;
        }
        return channels.values();
    }
    
    public int getChannelCount() {
        return channels.size();
    }
    
    public List<Channel> getChannels() {
        return getChannelsOfType(null);
    }
    
    public List<Channel> getChannelsOfType(Channel.Type type) {
        List<Channel> result = new ArrayList<>();
        for (DockContent content : dock.getContents()) {
            if (content.getComponent() instanceof Channel) {
                Channel chan = (Channel)content.getComponent();
                boolean typeMatches = type == null || type == chan.getType();
                if (typeMatches && doesChannelExist(chan)) {
                    result.add(chan);
                }
            }
        }
        return result;
    }
    
    public Collection<Channel> getTabsRelativeTo(Channel chan, int direction) {
        List<Channel> result = new ArrayList<>();
        for (DockContent c : dock.getContentsRelativeTo(chan.getDockContent(), direction)) {
            if (c.getComponent() instanceof Channel) {
                result.add((Channel)c.getComponent());
            }
        }
        return result;
    }
    
    //==========================
    // Change active content
    //==========================
    
    public void switchToChannel(String channel) {
        Channel c = getExistingChannel(channel);
        if (c != null) {
            dock.setActiveContent(c.getDockContent());
        }
    }
    
    public void switchToNextTab() {
        DockContent c = dock.getContentTab(getActiveContent(), 1);
        if (c != null) {
            dock.setActiveContent(c);
        }
    }
    
    public void switchToPreviousTab() {
        DockContent c = dock.getContentTab(getActiveContent(), -1);
        if (c != null) {
            dock.setActiveContent(c);
        }
    }
    
    
    //==========================
    // Focus
    //==========================
    
    public void setInitialFocus() {
        setInitialFocus(getActiveChannel());
    }
    
    public void setInitialFocus(Channel channel) {
        if (gui.getSettings().getLong("inputFocus") != 2) {
            if (channel == null) {
                channel = getActiveChannel();
            }
            if (channel != null) {
                channel.requestFocusInWindow();
            }
        }
    }
    
    //==========================
    // Settings
    //==========================
    
    public void refreshStyles() {
        for (Channel channel : getChannels()) {
            channel.refreshStyles();
        }
    }

    public void updateUserlistSettings() {
        for (Channel channel : getChannels()) {
            channel.updateUserlistSettings();
        }
    }

    public void setCompletionEnabled(boolean enabled) {
        for (Channel channel : getChannels()) {
            channel.setCompletionEnabled(enabled);
        }
    }
    
    public void setDefaultUserlistWidth(int width, int minWidth) {
        defaultUserlistWidth = width;
        minUserlistWidth = minWidth;
        if (defaultChannel != null) {
            // Set the width of the default channel because it's created before
            // the width is loaded from the settings
            defaultChannel.setUserlistWidth(width, minWidth);
        }
    }

    public void setDefaultUserlistVisibleState(boolean state){
        defaultUserlistVisibleState = state;
        if (defaultChannel != null) {
            defaultChannel.setUserlistEnabled(state);
        }
    }
    
    public void setChatScrollbarAlways(boolean always) {
        chatScrollbarAlaways = always;
        for (Channel chan : channels.values()) {
            chan.setScrollbarAlways(always);
        }
        if (defaultChannel != null) {
            defaultChannel.setScrollbarAlways(always);
        }
    }
    
    /**
     * Creates a map of channels for different closing options.
     * 
     * @param channels
     * @param channel
     * @return 
     */
    public static Map<String, Collection<Channel>> getCloseTabsChans(Channels channels, Channel channel) {
        Map<String, Collection<Channel>> result = new HashMap<>();
        result.put("closeAllTabsButCurrent", channels.getTabsRelativeTo(channel, 0));
        result.put("closeAllTabsToLeft", channels.getTabsRelativeTo(channel, -1));
        result.put("closeAllTabsToRight", channels.getTabsRelativeTo(channel, 1));
        
        Collection<Channel> all = channels.getTabsRelativeTo(channel, 0);
        all.add(channel);
        result.put("closeAllTabs", all);
        Collection<Channel> allOffline = new ArrayList<>();
        for (Channel c : all) {
            if (!c.getDockContent().isLive()) {
                allOffline.add(c);
            }
        }
        result.put("closeAllTabsOffline", allOffline);
        
        Collection<Channel> all2 = channels.getChannels();
        all2.remove(channel);
        result.put("closeAllTabs2ButCurrent", all2);
        
        Collection<Channel> all2Offline = new ArrayList<>();
        result.put("closeAllTabs2", channels.getChannels());
        for (Channel c : channels.getChannels()) {
            if (!c.getDockContent().isLive()) {
                all2Offline.add(c);
            }
        }
        result.put("closeAllTabs2Offline", all2Offline);
        return result;
    }
    
    /**
     * Sets the focus to the input bar when clicked anywhere on the channel.
     */
    private class MyMouseClickedListener implements MouseClickedListener {

        @Override
        public void mouseClicked(Channel chan) {
            setInitialFocus(chan);
        }
    }
    
    private static class LocationAndSize {
        public final Point location;
        public final Dimension size;
        
        LocationAndSize(Point location, Dimension size) {
            this.location = location;
            this.size = size;
        }
    }
    
    /**
     * The container used to add a Channel to the DockManager.
     */
    public static class DockChannelContainer extends DockStyledTabContainer<Channel> {
        
        //--------------------------
        // References
        //--------------------------
        private final ContextMenuListener listener;
        private final Channels channels;
        
        public DockChannelContainer(Channel channel, DockManager m, Channels channels, ContextMenuListener listener) {
            super(channel, channel.getName(), m);
            this.listener = listener;
            this.channels = channels;
        }
        
        @Override
        public JPopupMenu getContextMenu() {
            return new TabContextMenu(listener, (Channel) getComponent(), Channels.getCloseTabsChans(channels, (Channel) getComponent()));
        }
        
        @Override
        public void remove() {
            channels.channelTabClosed(getContent());
        }
        
        @Override
        public boolean canPopout() {
            /**
             * Prevent default channel popout. The default channel should stay
             * in main. It can still be moved when tabs are visible in main, but
             * it will be moved back if main becomes empty.
             */
            return ((Channel)(getComponent())).getRoom() != Room.EMPTY;
        }
        
        @Override
        public void setTitle(String title) {
            if (title.isEmpty()) {
                title = "No channel";
            }
            super.setTitle(title);
        }

    }
    
    private class ClosingDialog extends JDialog {
        
        private final JCheckBox rememberChoice;
        
        private String result = "";
        
        ClosingDialog(DockPopout popout) {
            super(popout.getWindow());
            setTitle(Language.getString("popoutClose.title"));
            setModal(true);
            setResizable(false);
            
            setLayout(new GridBagLayout());
            
            JButton closeChannels = new JButton(Language.getString("settings.string.popoutClose.option.close"));
            closeChannels.addActionListener(e -> {
                result = "close";
                setVisible(false);
            });
            
            JButton moveChannels = new JButton(Language.getString("settings.string.popoutClose.option.move"));
            moveChannels.addActionListener(e -> {
                result = "move";
                setVisible(false);
            });
            
            rememberChoice = new JCheckBox(Language.getString("popoutClose.rememberChoice"));
            
            GridBagConstraints gbc = GuiUtil.makeGbc(1, 0, 1, 1);
            
            Icon icon = UIManager.getIcon("OptionPane.questionIcon");
            if (icon != null) {
                gbc = GuiUtil.makeGbc(0, 0, 1, 1);
                gbc.insets = new Insets(10, 10, 10, 10);
                add(new JLabel(icon), gbc);
            }
            
            gbc = GuiUtil.makeGbc(1, 0, 2, 1, GridBagConstraints.EAST);
            add(new JLabel("<html><body width='250px'><p>"+Language.getString("popoutClose.question")+"</p>"
                    + "<p style='margin-top:5px;'>"+Language.getString("popoutClose.keyTip")+"</p>"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 1, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            add(closeChannels, gbc);
            
            gbc = GuiUtil.makeGbc(2, 1, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            gbc.insets = new Insets(5, 5, 5, 10);
            add(moveChannels, gbc);
            
            gbc = GuiUtil.makeGbc(1, 2, 2, 1, GridBagConstraints.EAST);
            gbc.insets = new Insets(0, 5, 5, 10);
            add(rememberChoice, gbc);
            pack();
            setLocationRelativeTo(popout.getWindow());
            setVisible(true);
        }
        
        public String getChoice() {
            return result;
        }
        
        public boolean shouldSaveChoice() {
            return rememberChoice.isSelected();
        }
        
    }
    
}
