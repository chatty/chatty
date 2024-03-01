
package chatty.gui;

import chatty.Helper;
import chatty.Helper.IntegerPair;
import chatty.Room;
import chatty.gui.components.Channel;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.TabContextMenu;
import chatty.gui.components.settings.TabSettings;
import chatty.gui.transparency.TransparencyManager;
import chatty.lang.Language;
import chatty.util.Debugging;
import chatty.util.IconManager;
import chatty.util.KeyChecker;
import chatty.util.MiscUtil;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockLayout;
import chatty.util.dnd.DockLayoutPopout;
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
import chatty.util.dnd.DockUtil;
import chatty.util.settings.Settings;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
     * Saves channels that are in the process of being closed, so that they can
     * still be used without opening it again (for example when a few messages
     * still come in), without being in "channels" and appearing like properly
     * open channels.
     */
    private final Map<String, Channel> closingChannels = new HashMap<>();
    
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
    
    /**
     * The content id for the default channel. This should preferably not be
     * changed, since it could be saved in layouts. It may be displayed in tab
     * order settings, so it's chosen to be somewhat similiar to the tab title.
     */
    public static final String DEFAULT_CHANNEL_ID = "-nochannel-";
    
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
                        setInitialFocus(c, true);
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
                TransparencyManager.check();
                Debugging.println("dnda", "Path: %s", content.getPath());
            }
            
            @Override
            public void popoutOpened(DockPopout popout, DockContent content) {
                dockPopoutOpened(popout);
            }
            
            @Override
            public void popoutClosing(DockPopout popout) {
                if (dock.getContents(popout).isEmpty()) {
                    dock.closePopout(popout);
                }
                else if (KeyChecker.isPressed(KeyEvent.VK_SHIFT)) {
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
                TransparencyManager.check();
            }

        });
        // One-time settings
        dock.setSetting(DockSetting.Type.POPOUT_ICONS, IconManager.getPopoutIcons());
        dock.setSetting(DockSetting.Type.POPOUT_PARENT, gui);
        this.styleManager = styleManager;
        this.contextMenuListener = contextMenuListener;
        this.gui = gui;
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
        instance = this;
    }
    
    public void init() {
        if (gui.getSettings().getBoolean("restoreLayout")) {
            loadLastSessionLayout();
        }
        checkDefaultChannel();
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
        if (defaultChannel != null) {
            updateSettings(defaultChannel);
        }
        dock.setSetting(DockSetting.Type.TAB_LAYOUT, getTabLayoutPolicyValue(gui.getSettings().getString("tabsLayout")));
        dock.setSetting(DockSetting.Type.TAB_PLACEMENT, getTabPlacementValue(gui.getSettings().getString("tabsPlacement")));
        dock.setSetting(DockSetting.Type.TAB_SCROLL, gui.getSettings().getBoolean("tabsMwheelScrolling"));
        dock.setSetting(DockSetting.Type.TAB_SCROLL_ANYWHERE, gui.getSettings().getBoolean("tabsMwheelScrollingAnywhere"));
        dock.setSetting(DockSetting.Type.TAB_CLOSE_MMB, gui.getSettings().getBoolean("tabsCloseMMB"));
        dock.setSetting(DockSetting.Type.TAB_CLOSE_SWITCH_TO_PREV, gui.getSettings().getBoolean("tabsCloseSwitchToPrev"));
        dock.setSetting(DockSetting.Type.TAB_ORDER, DockSetting.TabOrder.INSERTION);
        dock.setSetting(DockSetting.Type.FILL_COLOR, UIManager.getColor("TextField.selectionBackground"));
        dock.setSetting(DockSetting.Type.LINE_COLOR, UIManager.getColor("TextField.selectionForeground"));
        dock.setSetting(DockSetting.Type.POPOUT_TYPE_DRAG, getPopoutTypeValue((int)gui.getSettings().getLong("tabsPopoutDrag")));
        dock.setSetting(DockSetting.Type.DIVIDER_SIZE, 7);
        dock.setSetting(DockSetting.Type.NO_SINGLE, !gui.getSettings().getBoolean("tabsHideIfSingle"));
        updateTabComparator();
        updateKeepEmptySetting();
    }
    
    private void updateTabComparator() {
        boolean alphabetical = gui.getSettings().getString("tabOrder").equals("alphabetical");
        dock.setSetting(DockSetting.Type.TAB_COMPARATOR, new Comparator<DockContent>() {
            @Override
            public int compare(DockContent o1, DockContent o2) {
                long o1Pos = getPos(o1);
                long o2Pos = getPos(o2);
                if (o1Pos < o2Pos) {
                    return -1;
                }
                if (o1Pos > o2Pos) {
                    return 1;
                }
                if (alphabetical) {
                    return o1.getTitle().compareToIgnoreCase(o2.getTitle());
                }
                return 0;
            }
            
            private long getPos(DockContent content) {
                long pos = gui.getSettings().mapGetLong("tabsPos", content.getId(), 0);
                if (pos == 0 && !content.getId().isEmpty()) {
                    pos = gui.getSettings().mapGetLong("tabsPos", content.getId().substring(0, 1), 0);
                }
                return pos;
            }
        });
    }
    
    /**
     * This may need to be reset to default in other places, hence a method.
     */
    private void updateKeepEmptySetting() {
        dock.setSetting(DockSetting.Type.KEEP_EMPTY, !gui.getSettings().getBoolean("tabsCloseEmpty"));
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
                gui.getSettings().getLong("tabsLive"),
                gui.getSettings().getLong("tabsMessage"),
                gui.getSettings().getLong("tabsHighlight"),
                gui.getSettings().getLong("tabsStatus"),
                gui.getSettings().getLong("tabsActive"),
                gui.getSettings().getLong("tabsMaxWidth"));
    }
    
    //==========================
    // Layouts
    //==========================
    
    public void saveLayout() {
        gui.getSettings().mapPut("layouts", "", dock.getLayout().toList());
    }
    
    private boolean loadingLayout;
    private DockLayout lastLoadedLayout;
    private final Set<String> openedSinceLayoutLoad = new HashSet<>();
    
    /**
     * Switch from an open layout to another, optionally joining/leaving
     * channels.
     *
     * @param layout The layout to switch to
     * @param options Options for loading the layout, encoded by {@link makeLoadLayoutOptions(boolean, boolean, boolean)},
     * will be used if not -1, which will also not show the dialog, if -1 then
     * the default options will be used
     */
    public void changeLayout(DockLayout layout, int options) {
        if (layout == null) {
            return;
        }
        List<DockContent> current = dock.getContents();
        DockLayoutPopout mainWindow = layout.getMain();
        
        // Determine if main window would actually change
        boolean offerMainChange = mainWindow != null && mainWindow.canChange(gui.getLocation(), gui.getSize(), gui.getExtendedState());
        
        boolean showDialog = options == -1;
        
        // Show options (modal), check results afterwards
        ChangeLayoutDialog d = new ChangeLayoutDialog(gui,
                current,
                layout,
                offerMainChange,
                options != -1 ? options : gui.getSettings().getInt("layoutsOptions"));
        if (showDialog) {
            d.setVisible(true);
        }
        // At this point the dialog was closed again (or never open)
        if (d.saveOptions()) {
            gui.getSettings().setLong("layoutsOptions", d.getOptions());
        }
        if (!d.shouldLoad() && showDialog) {
            return;
        }
        
        loadingLayout = true;
        Debugging.println("layout", "Loading layout.. (%s)", layout);
        
        //--------------------------
        // Window/Layout
        //--------------------------
        if (d.loadMain()) {
            gui.setLocation(mainWindow.location);
            gui.setSize(mainWindow.size);
            gui.setExtendedState(mainWindow.state);
        }
        
        loadLayout(layout);
        
        //--------------------------
        // Channels
        //--------------------------
        // Disable the normal tab order (tabs should be ordered as added)
        dock.setSetting(DockSetting.Type.TAB_COMPARATOR, null);
        dock.setSetting(DockSetting.Type.KEEP_EMPTY, true);
        
        /**
         * Always add at first, since other active channels may not be available
         * depending on the order stuff is opened.
         * 
         * Since this is added after loading the layout (which removes all
         * content) it will stay added until it is re-added for proper tab order
         * (when in layout) or removed at the end if not necessary anymore.
         */
        if (defaultChannel != null) {
            addDefaultChannelToDock();
        }
        else {
            addDefaultChannel();
        }
        
        /**
         * At this point the still joined channels still exist and messages can
         * be added to it, however they won't be re-added to the layout
         * automatically (because that normally only happens when a new channel
         * is created). The ones that should stay open are re-added here.
         * 
         * The closing channels are added to a separate map that prevents them
         * from being assumed to still be properly open (e.g. for removing
         * default channel), but also prevents a new channel from being created
         * when e.g. more messages come in before the channel is parted.
         * 
         * While the channel is being parted, this will prevent the channel from
         * being opened again, but hopefully that's not an issue.
         */
        
        /**
         * Add everything from the loaded layout first (depending on options
         * selected in the dialog).
         * 
         * All types of content (channels, dialogs) are handled in the same
         * loop so that the order they are in can be retained. Currently open
         * channels that are not in the layout of course don't have a defined
         * order, so they are just appened in the next loop.
         */
        for (String id : layout.getContentIds()) {
            DockContent currentContent = DockUtil.getContentById(current, id);
            if (id.equals(DEFAULT_CHANNEL_ID)) {
                // Remove first to fix tab order
                dock.removeContent(defaultChannel.getDockContent());
                addDefaultChannelToDock();
            }
            else if (getTypeFromChannelName(id) == Channel.Type.CHANNEL) { // Regular
                handleContent(id, currentContent, d.getAddChannels().contains(id));
            }
            else if (getTypeFromChannelName(id) != Channel.Type.NONE) { // Whisper, ..
                handleContent(id, currentContent, d.getAddChannels().contains(id));
            }
            else { // Other content
                handleContent(id, currentContent, true);
            }
        }
        
        /**
         * Add currently open channels not in the layout (depending on options),
         * clear up everything else not in the layout.
         */
        for (DockContent currentContent : current) {
            String id = currentContent.getId();
            boolean inLayout = layout.getContentIds().contains(id);
            // If in layout, the above loop would have already handled it
            if (!inLayout) {
                if (getTypeFromChannelName(id) == Channel.Type.CHANNEL) { // Regular
                    handleContent(id, currentContent, d.getAddChannels().contains(id));
                }
                else if (getTypeFromChannelName(id) != Channel.Type.NONE) { // Whisper, ..
                    handleContent(id, currentContent, d.getAddChannels().contains(id));
                }
                else { // Other content
                    // If not in layout (all of these), then undock
                    // Only works for stuff that is registered with the docked
                    // dialog manager, so not for -nochannel-
                    handleContent(id, currentContent, false);
                }
            }
        }
        
        /**
         * Join channels that need to be joined. Their tab will already have
         * been opened.
         */
        if (!d.getJoinChannels().isEmpty()) {
            Set<String> toJoin = new HashSet<>();
            for (String channel : d.getJoinChannels()) {
                if (channel.startsWith("#")) {
                    // The joining state will be shown on the tab (reduced opacity)
                    channels.get(channel).getDockContent().setJoining(true);
                    toJoin.add(channel);
                }
            }
            gui.client.joinChannels(new HashSet<>(toJoin));
        }
        
        // Enable the normal tab order again
        updateTabComparator();
        updateKeepEmptySetting();
        loadingLayout = false;
        checkDefaultChannel();
        setActiveTabs(layout);
        
        Debugging.println("layout", "Finished loading layout. Add: %s Join: %s Closing: %s Channels: %s", d.getAddChannels(), d.getJoinChannels(), closingChannels, channels);
    }
    
    /**
     * Handle content with the given id, either add or remove.
     * 
     * @param id The content id
     * @param currentContent If content for the id already exists, this is it
     * @param add If true the content should be added, otherwise cleaned up
     */
    private void handleContent(String id, DockContent currentContent, boolean add) {
        Debugging.println("layout", "Handle %s (%s) [%s]", id, add, currentContent);
        if (getTypeFromChannelName(id) != Channel.Type.NONE) {
            //--------------------------
            // Any channel (regular, whisper)
            //--------------------------
            if (add) {
                if (currentContent != null) {
                    // Reuse existing content
                    setTargetPath(currentContent);
                    dock.addContent(currentContent);
                }
                else {
                    // Create/add new channel (works if it doesn't exist)
                    getChannel(Room.createRegular(id));
                }
            }
            else {
                // Close, if necessary
                Channel channel = channels.remove(id);
                if (channel != null) {
                    closingChannels.put(id, channel);
                }
                gui.client.closeChannel(id);
            }
        }
        else if (id.startsWith("'")) {
            if (add) {
                gui.routingManager.addTarget(id.replace("'", ""));
            }
        }
        else {
            //--------------------------
            // Other content (dialogs)
            //--------------------------
            if (add) {
                gui.dockedDialogs.openInDock(id);
            }
            else {
                gui.dockedDialogs.closeFromDock(id);
            }
        }
        
    }
    
    private void loadLastSessionLayout() {
        DockLayout layout = DockLayout.fromList((List) gui.getSettings().mapGet("layouts", ""));
        if (layout != null) {
            loadingLayout = true;
            
            loadLayout(layout);
            
            /**
             * Always add default channel first since depending on the order
             * stuff is opened there may not be an active channel when needed.
             * 
             * Loading layout removes all content, so add after that.
             */
            addDefaultChannel();
            dock.setSetting(DockSetting.Type.TAB_COMPARATOR, null);
            dock.setSetting(DockSetting.Type.KEEP_EMPTY, true);
            for (String id : layout.getContentIds()) {
                if (id.equals(DEFAULT_CHANNEL_ID)) {
                    // Default channel should be added at this point, so remove first for proper tab order
                    dock.removeContent(defaultChannel.getDockContent());
                    addDefaultChannelToDock();
                }
                else if (getTypeFromChannelName(id) == Channel.Type.CHANNEL) {
                    if (gui.getSettings().getLong("onStart") == 3) {
                        // Load previously open channels (they will be joined otherwise)
                        handleContent(id, null, true);
                        channels.get(id).getDockContent().setJoining(true);
                    }
                }
                else if (getTypeFromChannelName(id) != Channel.Type.NONE) {
                    // Always open whisper etc.
                    handleContent(id, null, gui.getSettings().getBoolean("restoreLayoutWhisper"));
                }
                else {
                    // Always open docked dialogs
                    handleContent(id, null, true);
                }
            }
            loadingLayout = false;
            updateTabComparator();
            updateKeepEmptySetting();
            setActiveTabs(layout);
        }
    }
    
    private void loadLayout(DockLayout layout) {
        lastLoadedLayout = layout;
        openedSinceLayoutLoad.clear();
        dock.loadLayout(layout);
    }
    
    /**
     * Switches to each content id that was marked as active tab in the layout.
     * For areas that only contain a single content or tabs that are already
     * active this probably doesn't do much, but otherwise it switches to the
     * previously active tab for each tab pane.
     *
     * @param layout 
     */
    private void setActiveTabs(DockLayout layout) {
        for (String id : layout.getActiveContentIds()) {
            DockContent content = dock.getContentById(id);
            if (content != null) {
                dock.setActiveContent(content);
            }
        }
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
    
    public void joinScheduled(String channel) {
        getChannel(Room.createRegular(channel)).getDockContent().setJoining(true);
    }
    
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
            panel = closingChannels.get(room.getChannel());
        }
        if (panel == null) {
            panel = addChannel(room, type);
        }
        else if (panel.setRoom(room)) {
            Debugging.println("Updating Channel Name to " + panel.getName());
        }
        if (panel.getDockContent().isJoining()) {
            panel.getDockContent().setJoining(!gui.client.isChannelJoined(panel.getDockContent().getId()));
        }
        return panel;
    }
    
    /**
     * This is the channel when no channel has been added yet.
     */
    private void addDefaultChannel() {
        defaultChannel = createChannel(Room.EMPTY, Channel.Type.NONE);
        defaultChannel.getDockContent().setId(DEFAULT_CHANNEL_ID);
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
    
    /**
     * Should use this instead of adding content to the dock directly, so that
     * the correct target path is set (for docked dialogs).
     * 
     * @param content 
     */
    public void addContent(DockContent content) {
        setTargetPath(content);
        dock.addContent(content);
    }
    
    private void setTargetPath(DockContent content, String target) {
        DockPath layoutPath = getLayoutPath(content.getId());
        
        content.setTargetPath(null);
        if (layoutPath != null) {
            content.setTargetPath(layoutPath);
        }
        else if (dock.getPathOnRemove(content.getId()) != null) {
            // TODO: Add setting for this
            content.setTargetPath(dock.getPathOnRemove(content.getId()));
        }
        else if (target.equals("active")) {
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
        else if (target.equalsIgnoreCase("activeChan")) {
            if (lastActiveChannel != null) {
                content.setTargetPath(lastActiveChannel.getDockContent().getPath());
            }
        }
        // Other "target" values, in default location
        if (content.getTargetPath() == null) {
            DockPath path = new DockPath(content);
            path.addParent(DockPathEntry.createPopout(null));
            content.setTargetPath(path);
        }
        
        openedSinceLayoutLoad.add(content.getId());
    }
    
    private DockPath getLayoutPath(String id) {
        if (lastLoadedLayout != null && !openedSinceLayoutLoad.contains(id)) {
            return lastLoadedLayout.getPath(id);
        }
        return null;
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
        DockPath layoutPath = getLayoutPath(room.getChannel());
        Channel panel;
        if (defaultChannel != null
                && (layoutPath == null)) {
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
        channel.getDockContent().setId(room.getChannel());
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
        Channel channel = channels.remove(channelName);
        if (channel != null) {
            // Removing will automatically run checkDefaultChannel()
            dock.removeContent(channel.getDockContent());
            channel.cleanUp();
        }
        Channel closingChannel = closingChannels.remove(channelName);
        if (closingChannel != null) {
            closingChannel.cleanUp();
        }
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
        if (loadingLayout) {
            /**
             * While changing layouts there may be some inconsistent state (e.g.
             * channels containing channels that are already being removed), so
             * ignore this for now.
             */
            return;
        }
        Debugging.println("defaultchannel", "Default channel: %s Chans: %s Main Contents: %s",
                defaultChannel != null ? "Present" : "-",
                channels.size(),
                dock.getContents(null).size());
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
                    /**
                     * Temp turn off closing empty stuff. This will be the case
                     * always when removing the defaultChannel, not sure if this
                     * causes any issues, but at least the user should be able
                     * to manually close if it happens when it shouldn't.
                     */
                    dock.setSetting(DockSetting.Type.KEEP_EMPTY, true);
                    dock.removeContent(defaultChannel.getDockContent());
                    updateKeepEmptySetting();
                    defaultChannel.cleanUp();
                    defaultChannel = null;
                    Debugging.println("defaultchannel", "Default channel removed");
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
        clearOpenPopoutsAttributes();
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
            LocationAndSize attr = new LocationAndSize(popout.getWindow());
            dialogsAttributes.remove(attr);
            dialogsAttributes.add(0, attr);
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
    
    private static final int POPOUT_ATTRIBUTES_LIMIT = 30;
    
    /**
     * Returns a list of unique Strings that contain the location/size of open
     * dialogs and of closed dialogs that weren't reused.
     * 
     * Format: "x,y;width,height"
     * 
     * @return 
     */
    public List getPopoutAttributes() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        /**
         * Not sure if makes sense to save attributes of open popouts, since
         * they may be reopened already anyway next time and the opening order
         * doesn't necessarily seem to be retained.
         */
        for (DockPopout popout : dock.getPopouts()) {
            Window w = popout.getWindow();
            result.add(w.getX()+","+w.getY()
                    +";"+w.getWidth()+","+w.getHeight());
        }
        for (LocationAndSize attr : dialogsAttributes) {
            result.add(attr.location.x + "," + attr.location.y
                    + ";" + attr.size.width + "," + attr.size.height);
            if (result.size() >= POPOUT_ATTRIBUTES_LIMIT) {
                break;
            }
        }
        return new ArrayList<>(result);
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
    
    /**
     * Removes all entries with the same location/size as currently open
     * popouts.
     */
    private void clearOpenPopoutsAttributes() {
        for (DockPopout popout : dock.getPopouts()) {
            LocationAndSize attr = new LocationAndSize(popout.getWindow());
            dialogsAttributes.removeIf(entry -> attr.equals(entry));
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
     * For accessing this instance for the $input() function. Not my favorite
     * way of doing this, but it'll do for now.
     */
    private static Channels instance;
    
    public static Channels getInstance() {
        return instance;
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
     * Returns all popouts with their currently active content. The active
     * content may be null if the popout contains no content.
     * 
     * @return The popouts, without the main base (which would be null)
     */
    public Map<DockPopout, DockContent> getActivePopoutContent() {
        Map<DockPopout, DockContent> result = new HashMap<>();
        for (DockPopout popout : dock.getPopouts()) {
            result.put(popout, dock.getActiveContent(popout));
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
    
    public Collection<DockContent> getTabsRelativeTo(DockContent relativeToContent, int direction, String prefix) {
        List<DockContent> result = new ArrayList<>();
        for (DockContent content : dock.getContentsRelativeTo(relativeToContent, direction)) {
            if (content.getId().startsWith(prefix)) {
                result.add(content);
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
        DockContent c = dock.getContentTabRelative(getActiveContent(), 1);
        if (c != null) {
            dock.setActiveContent(c);
        }
    }
    
    public void switchToPreviousTab() {
        DockContent c = dock.getContentTabRelative(getActiveContent(), -1);
        if (c != null) {
            dock.setActiveContent(c);
        }
    }
    
    public void switchToTabId(String id) {
        DockContent c = dock.getContentById(id);
        if (c != null) {
            dock.setActiveContent(c);
        }
    }
    
    public void switchToTabIndex(int index) {
        DockContent c = dock.getContentTabAbsolute(getActiveContent(), index);
        if (c != null) {
            dock.setActiveContent(c);
        }
    }
    
    //==========================
    // Focus
    //==========================
    
    public void setInitialFocus() {
        setInitialFocus(getActiveChannel(), true);
    }
    
    public void setInitialFocus(Channel channel, boolean later) {
        if (gui.getSettings().getLong("inputFocus") != 2) {
            if (channel == null) {
                channel = getActiveChannel();
            }
            if (channel != null) {
                Channel channel2 = channel;
                if (later) {
                    /**
                     * After some actions invokeLater seems to be required,
                     * otherwise the focus doesn't change and the request focus
                     * function returns false (not entirely sure why).
                     */
                    SwingUtilities.invokeLater(() -> {
                        channel2.requestFocusInWindow();
                    });
                }
                else {
                    channel2.requestFocusInWindow();
                }
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
     * @param activeContent
     * @param sameType
     * @return 
     */
    public static Map<String, Collection<DockContent>> getCloseTabs(Channels channels, DockContent activeContent, boolean sameType) {
        String prefix = sameType ? activeContent.getId().substring(0, 1) : "";
        Map<String, Collection<DockContent>> result = new HashMap<>();
        result.put("closeAllTabsButCurrent", channels.getTabsRelativeTo(activeContent, 0, prefix));
        result.put("closeAllTabsToLeft", channels.getTabsRelativeTo(activeContent, -1, prefix));
        result.put("closeAllTabsToRight", channels.getTabsRelativeTo(activeContent, 1, prefix));
        
        Collection<DockContent> all = channels.getTabsRelativeTo(activeContent, 0, prefix);
        all.add(activeContent);
        result.put("closeAllTabs", all);
        Collection<DockContent> allOffline = new ArrayList<>();
        for (DockContent c : all) {
            if (isChanOffline(c)) {
                allOffline.add(c);
            }
        }
        result.put("closeAllTabsOffline", allOffline);
        
        Collection<DockContent> all2 = DockUtil.getContentsByPrefix(channels.getDock().getContents(), prefix);
        all2.remove(activeContent);
        result.put("closeAllTabs2ButCurrent", all2);
        
        Collection<DockContent> all2Offline = new ArrayList<>();
        result.put("closeAllTabs2", DockUtil.getContentsByPrefix(channels.getDock().getContents(), prefix));
        for (DockContent c : DockUtil.getContentsByPrefix(channels.getDock().getContents(), prefix)) {
            if (isChanOffline(c)) {
                all2Offline.add(c);
            }
        }
        result.put("closeAllTabs2Offline", all2Offline);
        return result;
    }
    
    /**
     * Checks if the given DockContent is offline, that is, if it is an actual
     * channel that can be live, but is not live.
     * 
     * @param content
     * @return 
     */
    private static boolean isChanOffline(DockContent content) {
        if (content instanceof DockChannelContainer) {
            Channel channel = ((DockChannelContainer) content).getContent();
            if (channel.getType() == Channel.Type.CHANNEL) {
                return !channel.getDockContent().isLive();
            }
        }
        return false;
    }

    public void closeModPanels() {
        for (Channel chan : getChannels()) {
            chan.closeModPanel();
        }
    }
    
    /**
     * Sets the focus to the input bar when clicked anywhere on the channel.
     */
    private class MyMouseClickedListener implements MouseClickedListener {

        @Override
        public void mouseClicked(Channel chan, boolean onlyChangeChan) {
            /**
             * Enabling the "later" option might take focus away from an opened
             * context menu (which also triggers this event).
             * 
             * For opening context menu (onlyChangeChan enabled) only set focus
             * when changing channel is necessary. This prevents focus shifting
             * away, which - if text is selected - would remove the selection
             * visibility and possibly break copying text.
             */
            if (onlyChangeChan) {
                if (getActiveChannel() != chan) {
                    setInitialFocus(chan, false);
                }
            }
            else {
                setInitialFocus(chan, false);
            }
        }
    }
    
    private static class LocationAndSize {
        public final Point location;
        public final Dimension size;

        public LocationAndSize(Window window) {
            this.location = window.getLocation();
            this.size = window.getSize();
        }
        
        LocationAndSize(Point location, Dimension size) {
            this.location = location;
            this.size = size;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LocationAndSize other = (LocationAndSize) obj;
            if (!Objects.equals(this.location, other.location)) {
                return false;
            }
            return Objects.equals(this.size, other.size);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.location);
            hash = 79 * hash + Objects.hashCode(this.size);
            return hash;
        }
        
    }
    
    public void sortContent(String id) {
        if (id == null) {
            dock.sortContent(null);
        }
        else {
            DockContent content = DockUtil.getContentById(dock.getContents(), id);
            if (content != null) {
                dock.sortContent(content);
            }
        }
    }
    
    public static long getTabPos(Settings settings, String id) {
        return settings.mapGetLong("tabsPos", id, 0);
    }
    
    public static Map<Long, List<String>> getTabPosIds(Settings settings) {
        Map<String, Long> tabsPos = settings.getMap("tabsPos");
        Map<Long, List<String>> result = new HashMap<>();
        for (Map.Entry<String, Long> entry : tabsPos.entrySet()) {
            if (!result.containsKey(entry.getValue())) {
                result.put(entry.getValue(), new ArrayList<>());
            }
            result.get(entry.getValue()).add(TabSettings.tabPosLabel(entry.getKey()));
        }
        return result;
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
            Channel channel = (Channel) getComponent();
            return new TabContextMenu(listener,
                    channel.getDockContent(),
                    Channels.getCloseTabs(channels, channel.getDockContent(), channels.gui.getSettings().getBoolean("closeTabsSameType")),
                    channels.gui.getSettings());
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
                title = Language.getString("tabs.noChannel");
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
    
    private static final int LAYOUT_OPTION_CURRENT_CHANS = 1 << 0;
    private static final int LAYOUT_OPTION_LAYOUT_CHANS = 1 << 1;
    private static final int LAYOUT_OPTION_MAIN = 1 << 2;
    
    private class ChangeLayoutDialog extends JDialog {
        
        private final List<String> joinChannels = new ArrayList<>();
        private final List<String> addChannels = new ArrayList<>();
        private final List<String> partChannels = new ArrayList<>();
        private final JCheckBox changeMain;
        private final JCheckBox keepCurrentChannels;
        private final JCheckBox openLayoutChannels;
        private final JCheckBox rememberSettings;
        private boolean load;
        
        ChangeLayoutDialog(Window parent, List<DockContent> current, DockLayout layout, boolean offerMainChange, int options) {
            super(parent);
            setTitle("Load Layout");
            setModal(true);
            setResizable(false);
            
            setLayout(new GridBagLayout());
            
            List<String> currentChannelIds = new ArrayList<>();
            for (DockContent content : current) {
                if (content.getId().startsWith("#") || content.getId().startsWith("$")) {
                    currentChannelIds.add(content.getId());
                }
            }
            
            List<String> layoutChannelIds = new ArrayList<>();
            for (String id : layout.getContentIds()) {
                if (id.startsWith("#") || id.startsWith("$")) {
                    layoutChannelIds.add(id);
                }
            }
            
            keepCurrentChannels = new JCheckBox(String.format("Keep current channels open (%d)", currentChannelIds.size()));
            keepCurrentChannels.setEnabled(!currentChannelIds.isEmpty());
            
            openLayoutChannels = new JCheckBox(String.format("Join channels in layout (%d)", layoutChannelIds.size()));
            openLayoutChannels.setEnabled(!layoutChannelIds.isEmpty());
            
            changeMain = new JCheckBox("Load main window location and size");
            changeMain.setEnabled(offerMainChange);
            
            rememberSettings = new JCheckBox("Remember current selection");
            
            JLabel result = new JLabel();
            
            Runnable updateResults = () -> {
                joinChannels.clear();
                addChannels.clear();
                partChannels.clear();
                // Layout
                if (openLayoutChannels.isSelected()) {
                    for (String id : layoutChannelIds) {
                        if (!currentChannelIds.contains(id)) {
                            joinChannels.add(id);
                        }
                        addChannels.add(id);
                    }
                }
                // Current
                if (keepCurrentChannels.isSelected()) {
                    for (String id : currentChannelIds) {
                        if (!addChannels.contains(id)) {
                            addChannels.add(id);
                        }
                    }
                }
                else {
                    for (String id : currentChannelIds) {
                        if (!addChannels.contains(id)) {
                            partChannels.add(id);
                        }
                    }
                }
                result.setText(String.format("Open %d channels, Close %d channels",
                        joinChannels.size(),
                        partChannels.size()));
            };
            
            GridBagConstraints gbc;
            
            JPanel optionsPanel = new JPanel(new GridBagLayout());
            optionsPanel.setBorder(BorderFactory.createEtchedBorder());
            
            gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(1, 5, 1, 5);
            optionsPanel.add(keepCurrentChannels, gbc);
            
            gbc = GuiUtil.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(1, 5, 1, 5);
            optionsPanel.add(openLayoutChannels, gbc);
            
            gbc = GuiUtil.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(2, 5, 5, 5);
            optionsPanel.add(result, gbc);
            
            gbc = GuiUtil.makeGbc(0, 4, 2, 1, GridBagConstraints.WEST);
            optionsPanel.add(changeMain, gbc);
            
            JButton loadLayout = new JButton("Load Layout");
            loadLayout.addActionListener(e -> {
                load = true;
                setVisible(false);
            });
            
            ItemListener checkboxListener = (ItemEvent e) -> updateResults.run();
            keepCurrentChannels.addItemListener(checkboxListener);
            openLayoutChannels.addItemListener(checkboxListener);
            
            keepCurrentChannels.setSelected(MiscUtil.isBitEnabled(options, LAYOUT_OPTION_CURRENT_CHANS));
            openLayoutChannels.setSelected(MiscUtil.isBitEnabled(options, LAYOUT_OPTION_LAYOUT_CHANS));
            changeMain.setSelected(MiscUtil.isBitEnabled(options, LAYOUT_OPTION_MAIN));
            
            updateResults.run();
            
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(e -> {
                load = false;
                setVisible(false);
            });
            
            Icon icon = UIManager.getIcon("OptionPane.questionIcon");
            if (icon != null) {
                gbc = GuiUtil.makeGbc(0, 0, 1, 1);
                gbc.insets = new Insets(10, 10, 10, 10);
                add(new JLabel(icon), gbc);
            }
            
            gbc = GuiUtil.makeGbc(1, 0, 2, 1, GridBagConstraints.EAST);
            add(new JLabel("<html><body width='250px'><p>Docked info dialogs ('Dock as tab'-option in their context menu) will be opened/closed to match the loaded layout, however non-docked dialogs are not affected.</p>"
                    + "<p style='margin-top:5px;'>Channels will be joined/parted depending on the selection below.</p>"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 5, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(optionsPanel, gbc);
            
            gbc = GuiUtil.makeGbc(1, 11, 2, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(3, 5, 3, 5);
            add(rememberSettings, gbc);
            
            gbc = GuiUtil.makeGbc(1, 10, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            add(loadLayout, gbc);
            
            gbc = GuiUtil.makeGbc(2, 10, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            gbc.insets = new Insets(5, 5, 5, 10);
            add(cancel, gbc);

            pack();
            setLocationRelativeTo(parent);
            
            GuiUtil.installEscapeCloseOperation(this);
        }
    
        public boolean shouldLoad() {
            return load;
        }
        
        public List<String> getJoinChannels() {
            return joinChannels;
        }
        
        public List<String> getPartChannels() {
            return partChannels;
        }
        
        public List<String> getAddChannels() {
            return addChannels;
        }
        
        public boolean loadMain() {
            return changeMain.isSelected();
        }
        
        public int getOptions() {
            return makeLoadLayoutOptions(keepCurrentChannels.isSelected(), openLayoutChannels.isSelected(), changeMain.isSelected());
        }

        private boolean saveOptions() {
            return rememberSettings.isSelected();
        }
        
    }
    
    public static int makeLoadLayoutOptions(boolean keepCurrentChannels,
                                            boolean openLayoutChannels,
                                            boolean changeMain) {
        return (keepCurrentChannels ? LAYOUT_OPTION_CURRENT_CHANS : 0)
                | (openLayoutChannels ? LAYOUT_OPTION_LAYOUT_CHANS : 0)
                | (changeMain ? LAYOUT_OPTION_MAIN : 0);
    }
    
}
