
package chatty.gui;

import chatty.Chatty;
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
    
    private final JMenu main = new JMenu(Chatty.lang.GET("MENUBAR_MAIN", "Main"));
    protected final JMenu view = new JMenu(Chatty.lang.GET("MENUBAR_VIEW", "View"));
    private final JMenu channels = new JMenu(Chatty.lang.GET("MENUBAR_CHANNELS", "Channels"));
    private final JMenu extra = new JMenu(Chatty.lang.GET("MENUBAR_EXTRA", "Extra"));
    private final JMenu help = new JMenu(Chatty.lang.GET("MENUBAR_HELP", "Help"));
    
    private final JMenuItem highlights;
    private final JMenuItem ignored;
    
    private final ItemListener itemListener;
    private final ActionListener actionListener;
    private final LinkLabelListener linkLabelListener;
    
    // Set here because it is used more than once
    private final String IGNORED_LABEL = Chatty.lang.GET("MENUBAR_IGNORED_MSG_LABEL", "Ignored");
    private final String HIGHLIGHTS_LABEL = Chatty.lang.GET("MENUBAR_HIGHLIGHTS_MSG_LABEL", "Highlights");
    
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
        extra.addActionListener(actionListener);
        help.addActionListener(actionListener);
        
        view.addMenuListener((MenuListener)itemListener);
        
        main.setMnemonic(KeyEvent.VK_M);
        view.setMnemonic(KeyEvent.VK_V);
        channels.setMnemonic(KeyEvent.VK_C);
        extra.setMnemonic(KeyEvent.VK_E);
        help.setMnemonic(KeyEvent.VK_H);
        
        // Main
        addItem(main,"connect",Chatty.lang.GET("MENUBAR_CONNECT", "Connect"));
        addItem(main,"disconnect",Chatty.lang.GET("MENUBAR_DISCONNECT", "Disconnect")).setEnabled(false);
        main.addSeparator();
        setIcon(addItem(main,"settings",Chatty.lang.GET("MENUBAR_SETTINGS", "Settings"), KeyEvent.VK_S), "preferences-system.png");
        addItem(main,"configureLogin",Chatty.lang.GET("MENUBAR_CONFIGURE_LOGIN", "Login.."));
        main.addSeparator();
        addItem(main,"saveSettings",Chatty.lang.GET("MENUBAR_SAVE_SETTINGS", "Save.."));
        main.addSeparator();
        addItem(main,"application.exit",Chatty.lang.GET("MENUBAR_APPLICATION_EXIT", "Exit"));
        
        // View
        addCheckboxItem(view,"ontop",Chatty.lang.GET("MENUBAR_ALWAYSONTOP", "Always on top"));
        
        
        JMenu viewOptions = new JMenu(Chatty.lang.GET("MENUBAR_OPTIONS", "Options"));
        
        JMenu titleOptions = new JMenu("Titlebar");
        addCheckboxItem(titleOptions, "titleShowUptime", "Stream Uptime");
        addCheckboxItem(titleOptions, "titleLongerUptime", "More Detailed Uptime");
        addCheckboxItem(titleOptions, "titleShowChannelState", "Channel State");
        addCheckboxItem(titleOptions, "titleShowViewerCount", "Viewer/Chatter Count");
        titleOptions.addSeparator();
        addCheckboxItem(titleOptions, "simpleTitle", "Simple Title");
        
        viewOptions.add(titleOptions);
        
        addCheckboxItem(viewOptions,"showJoinsParts","Show joins/parts");
        addCheckboxItem(viewOptions, "showModMessages", "Show mod/unmod");
        addCheckboxItem(viewOptions, "attachedWindows", "Attached dialogs");
        addCheckboxItem(viewOptions, "mainResizable", Chatty.lang.GET("MENUBAR_WINDOW_RESIZABLE", "Window resizable"));

        view.add(viewOptions);
        view.addSeparator();
        addItem(view,"dialog.channelInfo",Chatty.lang.GET("MENUBAR_CHANNELINFO", "Channel Info"));
        addItem(view,"dialog.channelAdmin",Chatty.lang.GET("MENUBAR_CHANNELADMIN", "Channel Admin"));
        view.addSeparator();
        highlights = addItem(view,"dialog.highlightedMessages",HIGHLIGHTS_LABEL);
        ignored = addItem(view,"dialog.ignoredMessages",IGNORED_LABEL);
        view.addSeparator();
        addItem(view,"dialog.search",Chatty.lang.GET("MENUBAR_SEARCH", "Find text.."));
        
        // Channels
        addItem(channels, "favoritesDialog", Chatty.lang.GET("MENUBAR_FAVORITESDIAG", "Favorites/History"), KeyEvent.VK_F);
        addItem(channels, "dialog.streams", Chatty.lang.GET("MENUBAR_LIVE_CHANNELS", "Live Channels"));
        addItem(channels, "dialog.addressbook", Chatty.lang.GET("MENUBAR_ADDRESSBOOK", "Addressbook"));
        channels.addSeparator();
        addItem(channels, "dialog.joinChannel", Chatty.lang.GET("MENUBAR_JOIN_CHANNEL", "Join Channel"));

        // Extra
        addItem(extra,"streamlink","Streamlink", KeyEvent.VK_L); // not translatable! application name // brand
        addItem(extra,"dialog.toggleEmotes",Chatty.lang.GET("MENUBAR_EMOTICONS", "Emoticons"));
        extra.addSeparator();
        addItem(extra,"dialog.followers","Followers");
        addItem(extra,"dialog.subscribers","Subscribers");
        extra.addSeparator();
        addItem(extra,"dialog.moderationLog", "Moderation Log");
        addItem(extra,"dialog.autoModDialog", "AutoMod");
        addItem(extra,"dialog.chatRules", Chatty.lang.GET("MENUBAR_CHAT_RULES", "Chat Rules"));
        extra.addSeparator();
        JMenu streamChat = new JMenu("Stream Chat");
        addItem(streamChat,"dialog.streamchat", "Open");
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

        // Help
        addItem(help,"website","Website");
        JMenuItem helpItem = addItem(help,"about","About/Help", KeyEvent.VK_H);
        helpItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        setIcon(helpItem, "help-browser.png");
        help.addSeparator();
        addItem(help,"news",Chatty.lang.GET("MENUBAR_ANNOUNCEMENTS", "Announcements"));
        
        
        add(main);
        add(view);
        add(channels);
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
