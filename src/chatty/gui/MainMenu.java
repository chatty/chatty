
package chatty.gui;

import chatty.Chatty;
import chatty.lang.Language;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.MenuListener;

/**
 * The main menu of the application (actually several menus in a MenuBar).
 * 
 * @author tduva
 */
public class MainMenu extends JMenuBar {
    
    private final JMenu main = new JMenu(Language.getString("menubar.menu.main"));
    protected final JMenu view = new JMenu(Language.getString("menubar.menu.view"));
    private final JMenu channels = new JMenu(Language.getString("menubar.menu.channels"));
    private final JMenu srl = new JMenu(Language.getString("menubar.menu.srl"));
    protected final JMenu srlStreams = new JMenu("Races with..");
    private final JMenu extra = new JMenu(Language.getString("menubar.menu.extra"));
    private final JMenu help = new JMenu(Language.getString("menubar.menu.help"));
    
    private final JMenuItem highlights;
    private final JMenuItem ignored;
    
    private final ItemListener itemListener;
    private final ActionListener actionListener;
    private final LinkLabelListener linkLabelListener;
    
    // Set here because it is used more than once
    private final String IGNORED_LABEL = Language.getString("menubar.dialog.ignoredMessages");
    private final String HIGHLIGHTS_LABEL = Language.getString("menubar.dialog.highlightedMessages");
    
    private final Notification notification = new Notification();
    
    /**
     * Stores all the menu items associated with a key
     */
    private final HashMap<String,JMenuItem> menuItems = new HashMap<>();
    
    public MainMenu(ActionListener actionListener, ItemListener itemListener,
            LinkLabelListener linkLabelListener) {
        this.itemListener = itemListener;
        this.actionListener = actionListener;
        this.linkLabelListener = linkLabelListener;
        
        //this.setBackground(Color.black);
        //this.setForeground(Color.white);
        
        main.addActionListener(actionListener);
        view.addActionListener(actionListener);
        channels.addActionListener(actionListener);
        srl.addActionListener(actionListener);
        extra.addActionListener(actionListener);
        help.addActionListener(actionListener);
        
        view.addMenuListener((MenuListener)itemListener);
        
        main.setMnemonic(KeyEvent.VK_M);
        view.setMnemonic(KeyEvent.VK_V);
        channels.setMnemonic(KeyEvent.VK_C);
        srl.setMnemonic(KeyEvent.VK_S);
        extra.setMnemonic(KeyEvent.VK_E);
        help.setMnemonic(KeyEvent.VK_H);
        
        //------
        // Main
        //------
        addItem(main, "connect", Language.getString("menubar.dialog.connect"));
        addItem(main, "disconnect", Language.getString("menubar.action.disconnect")).setEnabled(false);
        main.addSeparator();
        setIcon(addItem(main,"settings", Language.getString("menubar.dialog.settings"),
                KeyEvent.VK_S), "preferences-system.png");
        addItem(main,"configureLogin", Language.getString("menubar.dialog.login"));
        main.addSeparator();
        addItem(main,"saveSettings", Language.getString("menubar.dialog.save"));
        main.addSeparator();
        addItem(main,"application.exit");
        
        //------
        // View
        //------
        addCheckboxItemSetting(view, "ontop");

        JMenu viewOptions = new JMenu(Language.getString("menubar.menu.options"));
        
        JMenu titleOptions = new JMenu(Language.getString("menubar.menu.titlebar"));
        addCheckboxItemSetting(titleOptions, "titleShowUptime");
        addCheckboxItemSetting(titleOptions, "titleLongerUptime");
        addCheckboxItemSetting(titleOptions, "titleShowChannelState");
        addCheckboxItemSetting(titleOptions, "titleShowViewerCount");
        titleOptions.addSeparator();
        addCheckboxItemSetting(titleOptions, "simpleTitle");
        
        viewOptions.add(titleOptions);
        
        addCheckboxItemSetting(viewOptions, "showJoinsParts");
        addCheckboxItemSetting(viewOptions, "showModMessages");
        addCheckboxItemSetting(viewOptions, "attachedWindows");
        addCheckboxItemSetting(viewOptions, "mainResizable");

        view.add(viewOptions);
        view.addSeparator();
        addItem(view, "dialog.channelInfo");
        addItem(view, "dialog.channelAdmin");
        view.addSeparator();
        highlights = addItem(view,"dialog.highlightedMessages",HIGHLIGHTS_LABEL);
        ignored = addItem(view,"dialog.ignoredMessages",IGNORED_LABEL);
        view.addSeparator();
        addItem(view, "dialog.search");
        
        //----------
        // Channels
        //----------
        addItem(channels, "favoritesDialog", Language.getString("menubar.dialog.favorites"), KeyEvent.VK_F);
        addItem(channels, "dialog.streams");
        addItem(channels, "dialog.addressbook");
        channels.addSeparator();
        addItem(channels, "dialog.joinChannel");
        
        //-----
        // SRL
        //-----
        addItem(srl, "srlRaces", "Race List");
        srl.addSeparator();
        srl.add(srlStreams);
        srlStreams.addMenuListener((MenuListener)itemListener);

        //-------
        // Extra
        //-------
        addItem(extra,"livestreamer",Language.getString("menubar.dialog.livestreamer"), KeyEvent.VK_L);
        addItem(extra,"dialog.toggleEmotes");
        extra.addSeparator();
        addItem(extra,"dialog.followers");
        addItem(extra,"dialog.subscribers");
        extra.addSeparator();
        addItem(extra,"dialog.moderationLog");
        addItem(extra,"dialog.autoModDialog");
        addItem(extra,"dialog.chatRules");
        extra.addSeparator();
        JMenu streamChat = new JMenu("Stream Chat");
        addItem(streamChat,"dialog.streamchat");
        addCheckboxItem(streamChat, "streamChatResizable", "Resizable");
        extra.add(streamChat);
        
        JMenu streamHighlights = new JMenu("Stream Highlights");
        addItem(streamHighlights, "addStreamHighlight", "Add Stream Highlight");
        addItem(streamHighlights, "openStreamHighlights", "Open Stream Highlights");
        extra.add(streamHighlights);
        
        extra.addSeparator();
        JMenu debugOptions = new JMenu("Options");
        addCheckboxItem(debugOptions,"globalHotkeysEnabled","Global Hotkeys");
        extra.add(debugOptions);
        if (Chatty.DEBUG) {
            addItem(extra,"unhandledException", "Unhandled Exception");
        }
        addItem(extra,"debug","Debug window");
        
        // Maybe add own submenu later when more functions are available
//        extra.addSeparator();
//        JMenu speedruncom = new JMenu("Speedrun.com");
//        addItem(speedruncom, "srcOpen", "Open Game Website");
//        extra.add(speedruncom);

        //------
        // Help
        //------
        addItem(help,"website",Language.getString("menubar.action.openWebsite"));
        JMenuItem helpItem = addItem(help,"about","About/Help", KeyEvent.VK_H);
        helpItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        setIcon(helpItem, "help-browser.png");
        help.addSeparator();
        addItem(help,"news","Announcements");
        
        
        add(main);
        add(view);
        add(channels);
        add(srl);
        add(extra);
        add(help);
    }
    

    /**
     * Adds a MenuItem to a menu.
     * 
     * @param menu The Menu to which the item is added
     * @param key The key this item is associated with
     * @param label The text of the item
     * @return The created MenuItem
     */
    public final JMenuItem addItem(JMenu menu, String key, String label, int mnemonic) {
        JMenuItem item = new JMenuItem(label);
        if (mnemonic != -1) {
            item.setMnemonic(mnemonic);
        }
        menuItems.put(key,item);
        item.setActionCommand(key);
        menu.add(item);
        item.addActionListener(actionListener);
        return item;
    }
    
    public final JMenuItem addItem(JMenu menu, String key) {
        return addItem(menu, key, Language.getString("menubar."+key));
    }
    
    public final JMenuItem addItem(JMenu menu, String key, String label) {
        return addItem(menu, key, label, -1);
    }
    
    public final void setAction(String key, Action action) {
        JMenuItem item = menuItems.get(key);
        // Preserve icon
        Icon icon = item.getIcon();
        item.setAction(action);
        item.setIcon(icon);
    }
    
    /**
     * Adds a CheckboxMenuItem to a menu.
     * 
     * @param menu The Menu to which the item is added
     * @param key The key this item is associated with (the setting)
     * @param label The text of the item
     * @return The created MenuItem
     */
    public final JMenuItem addCheckboxItem(JMenu menu, String key, String label) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
        menuItems.put(key, item);
        item.setActionCommand(key);
        menu.add(item);
        item.addItemListener(itemListener);
        return item;
    }
    
    /**
     * Same as addCheckboxItem, but automatically sets the label based on the
     * setting name.
     * 
     * @param menu
     * @param key
     * @return 
     */
    public final JMenuItem addCheckboxItemSetting(JMenu menu, String key) {
        return addCheckboxItem(menu, key, Language.getString("menubar.setting."+key));
    }
    
    /**
     * Gets the MenuItem for the given setting name.
     * 
     * @param key
     * @return 
     */
    public JMenuItem getMenuItem(String key) {
        return menuItems.get(key);
    }
    
    /**
     * Gets the setting name for the given menu item.
     * 
     * @param item
     * @return 
     */
    public String getSettingByMenuItem(Object item) {
        Iterator<Entry<String,JMenuItem>> it = menuItems.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String,JMenuItem> entry = it.next();
            if (entry.getValue() == item) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Sets the state (selected/unselected) of the CheckboxMenuItem associated
     * with the given setting.
     * 
     * @param setting
     * @param state 
     */
    public void setItemState(String setting, boolean state) {
        JMenuItem item = getMenuItem(setting);
        if (item != null && item instanceof JCheckBoxMenuItem) {
            ((JCheckBoxMenuItem)item).setState(state);
        }
    }
    
    private void setIcon(JMenuItem item, String name) {
        item.setIcon(new ImageIcon(MainMenu.class.getResource(name)));
    }
    
    /**
     * Updates the labels for the highlights/ignored messages menu entries.
     * 
     * @param highlightsCountNew
     * @param highlightsCount
     * @param ignoredCountNew
     * @param ignoredCount 
     */
    public void updateCount(int highlightsCountNew, int highlightsCount, 
            int ignoredCountNew, int ignoredCount) {
        
        highlights.setText(buildCount(HIGHLIGHTS_LABEL, highlightsCountNew,
                highlightsCount));
        ignored.setText(buildCount(IGNORED_LABEL, ignoredCountNew,
                ignoredCount));
    }
    
    /**
     * Create count String for highlights/ignored mesages.
     * 
     * @param label
     * @param countNew
     * @param count
     * @return 
     */
    private String buildCount(String label, int countNew, int count) {
        if (count == 0) {
            return label;
        }
        return label+" ("+countNew+"/"+count+")";
    }
    
    /**
     * Update the entries for the "Races with.." submenu.
     * 
     * @param active The currently active stream
     * @param popout Current streams in popout dialogs
     */
    public void updateSrlStreams(String active, List<String> popout) {
        srlStreams.removeAll();
        if (active == null || active.isEmpty()) {
            addItem(srlStreams, "", "No channel joined");
        } else {
            addItem(srlStreams, "srlRaceActive", active);
        }
        if (!popout.isEmpty()) {
            srlStreams.addSeparator();
            for (String chan : popout) {
                addItem(srlStreams, "srlRace4"+chan, chan);
            }
        }
    }
    
    public void setUpdateNotification(boolean enabled) {
        notification.setUpdateNotification(enabled);
    }
    
    public void setAnnouncementNotification(boolean enabled) {
        notification.setAnnouncementNotification(enabled);
    }

    private class Notification {
        
        private static final String MESSAGE_BASE = "<html>"
            + "<body style='text-align: right;padding-right:5px;'>";
        
        /**
         * Stores whether the notification label has been added to the layout
         * yet, so it's guaranteed to be only added once.
         */
        private boolean addedLabelToLayout;
        
        /**
         * Store whether the notification is currently set to the smaller
         * version, so it doesn't constantly change unless necessary.
         */
        private boolean updateMessageSmaller;

        private String message;
        private String shortMessage;
        private Dimension preferredSize = new Dimension();
        private LinkLabel notification;
        private boolean updateNotificationEnabled;
        private boolean announcementNotificationEnabled;
        
//        private Timer flashTimer;
//        private int flashCount;

        public void setUpdateNotification(boolean enabled) {
            if (updateNotificationEnabled != enabled) {
                updateNotificationEnabled = enabled;
                setNotification();
            }
        }

        public void setAnnouncementNotification(boolean enabled) {
            if (announcementNotificationEnabled != enabled) {
                announcementNotificationEnabled = enabled;
                setNotification();
            }
        }

        private void makeText() {
            message = MESSAGE_BASE;
            shortMessage = MESSAGE_BASE;
            if (announcementNotificationEnabled) {
                message += "[announcement:show Announcement]";
                shortMessage += "[announcement:show News]";
            }
            if (updateNotificationEnabled) {
                if (announcementNotificationEnabled) {
                    message += "&nbsp;|&nbsp;";
                    shortMessage += "&nbsp;|&nbsp;";
                }
                message += "[update:show Update&nbsp;available!]";
                shortMessage += "[update:show Update!]";
            }
        }
        
        private void setNotification() {
            makeText();
            if (!addedLabelToLayout) {
                addNotificationToLayout();
                addedLabelToLayout = true;
            }
            
            // Save preferred size of regular version to compare to in listener
            notification.setText(message);
            preferredSize = notification.getPreferredSize();
            
            // This needs to be improved/tested more first
//            if (announcementNotificationEnabled || updateNotificationEnabled) {
//                flashCount = 9;
//
//                if (flashTimer != null) {
//                    flashTimer.stop();
//                    flashTimer = null;
//                }
//                flashTimer = new Timer(500, new ActionListener() {
//
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//                        System.out.println(flashCount);
//                        if (flashCount % 2 == 0) {
//                            notification.addRule("a { color: blue; }");
//                        } else {
//                            notification.addRule("a { color: red; }");
//
//                        }
//                        if (flashCount == 0) {
//                            flashTimer.stop();
//                            System.out.println("Stop");
//                        } else {
//                            flashCount--;
//                        }
//                    }
//                });
//                flashTimer.setRepeats(true);
//                flashTimer.start();
//            }
        }

        private void addNotificationToLayout() {
            notification = new LinkLabel("", linkLabelListener);
            
            /**
             * Add listener to change notification text to a shorter version
             * when less space is available ("Update available!" -> "Update!").
             */
            notification.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    Dimension actualSize = e.getComponent().getSize();
                    if (actualSize.width < preferredSize.width + 10) {
                        if (!updateMessageSmaller) {
                            notification.setText(shortMessage);
                            updateMessageSmaller = true;
                            //System.out.println("made smaller");
                        }
                    } else {
                        if (updateMessageSmaller) {
                            notification.setText(message);
                            updateMessageSmaller = false;
                            //System.out.println("made bigger again");
                        }
                    }
                }
            });

            add(notification);
        }
        
    }
}
