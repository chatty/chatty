
package chatty.gui;

import chatty.Chatty;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import java.awt.Dimension;
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
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuListener;

/**
 * The main menu of the application (actually several menus in a MenuBar).
 * 
 * @author tduva
 */
public class MainMenu extends JMenuBar {
    
    private final JMenu main = new JMenu("Main");
    protected final JMenu view = new JMenu("View");
    private final JMenu channels = new JMenu("Channels");
    private final JMenu srl = new JMenu("SRL");
    protected final JMenu srlStreams = new JMenu("Races with..");
    private final JMenu extra = new JMenu("Extra");
    private final JMenu help = new JMenu("Help");
    
    private final JMenuItem highlights;
    private final JMenuItem ignored;
    
    private final ItemListener itemListener;
    private final ActionListener actionListener;
    
    // Set here because it is used more than once
    private final String IGNORED_LABEL = "Ignored";
    private final String HIGHLIGHTS_LABEL = "Highlights";
    
    /**
     * Stores whether the "Update Available!" message has been added yet, so
     * it's guaranteed to be only added once.
     */
    private boolean addedUpdateMessage;
    /**
     * Store whether the update notification is currently set to the smaller
     * version, so it doesn't constantly change unless necessary.
     */
    private boolean updateMessageSmaller;
    
    /**
     * Stores all the menu items associated with a key
     */
    private final HashMap<String,JMenuItem> menuItems = new HashMap<>();
    
    public MainMenu(ActionListener actionListener, ItemListener itemListener) {
        this.itemListener = itemListener;
        this.actionListener = actionListener;
        
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
        
        // Main
        addItem(main,"connect","Connect");
        addItem(main,"disconnect","Disconnect").setEnabled(false);
        main.addSeparator();
        setIcon(addItem(main,"settings","Settings", KeyEvent.VK_S), "preferences-system.png");
        addItem(main,"configureLogin","Login..");
        main.addSeparator();
        addItem(main,"exit","Exit");
        
        // View
        addCheckboxItem(view,"ontop","Always on top");
        
        
        JMenu viewOptions = new JMenu("Options");
        
        JMenu titleOptions = new JMenu("Titlebar");
        addCheckboxItem(titleOptions, "titleShowUptime", "Stream Uptime");
        addCheckboxItem(titleOptions, "titleShowChannelState", "Channel State");
        addCheckboxItem(titleOptions, "titleShowViewerCount", "Viewer/Chatter Count");
        
        viewOptions.add(titleOptions);
        
        addCheckboxItem(viewOptions,"showJoinsParts","Show joins/parts");
        addCheckboxItem(viewOptions, "showModMessages", "Show mod/unmod");
        addCheckboxItem(viewOptions, "attachedWindows", "Attached dialogs");
        addCheckboxItem(viewOptions, "mainResizable", "Window resizable");

        view.add(viewOptions);
        view.addSeparator();
        addItem(view,"dialog.channelInfo","Channel Info");
        addItem(view,"dialog.channelAdmin","Channel Admin");
        view.addSeparator();
        highlights = addItem(view,"dialog.highlightedMessages",HIGHLIGHTS_LABEL);
        ignored = addItem(view,"dialog.ignoredMessages",IGNORED_LABEL);
        view.addSeparator();
        addItem(view,"dialog.search","Find text..");
        
        // Channels
        addItem(channels, "favoritesDialog", "Favorites/History", KeyEvent.VK_F);
        addItem(channels, "dialog.streams", "Live Channels");
        addItem(channels, "dialog.addressbook", "Addressbook");
        channels.addSeparator();
        addItem(channels, "dialog.joinChannel", "Join Channel");
        
        // SRL
        addItem(srl, "srlRaces", "Race List");
        srl.addSeparator();
        srl.add(srlStreams);
        srlStreams.addMenuListener((MenuListener)itemListener);

        // Extra
        addItem(extra,"livestreamer","Livestreamer", KeyEvent.VK_L);
        addItem(extra,"dialog.toggleEmotes","Emoticons");
        extra.addSeparator();
        addItem(extra,"dialog.followers","Followers");
        addItem(extra,"dialog.subscribers","Subscribers");
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
        addCheckboxItem(debugOptions,"simpleTitle","Simple Title");
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

        // Help
        addItem(help,"website","Website");
        JMenuItem helpItem = addItem(help,"about","About/Help", KeyEvent.VK_H);
        helpItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        setIcon(helpItem, "help-browser.png");
        
        
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
    
    public final JMenuItem addItem(JMenu menu, String key, String label) {
        return addItem(menu, key, label, -1);
    }
    
    public final void setAction(String key, Action action) {
        menuItems.get(key).setAction(action);
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
    
    /**
     * Regular version of the update notification.
     */
    private static final String UPDATE_MESSAGE = "<html>"
            + "<body style='text-align: right;padding-right:5px;'>"
            + "[update:show Update&nbsp;available!]";
    /**
     * Smaller version of the update notification.
     */
    private static final String UPDATE_MESSAGE_SMALL = "<html>"
            + "<body style='text-align: right;padding-right:5px;'>"
            + "[update:show Update!]";
    
    /**
     * Add the Update available! link in the menubar.
     * 
     * @param listener The listener that reacts on a click on the link
     */
    public void setUpdateAvailable(LinkLabelListener listener) {
        if (!addedUpdateMessage) {
            final LinkLabel updateNotification = new LinkLabel(UPDATE_MESSAGE, listener);
            
            // Add listener and stuff to change notification size when less
            // space is there (Update available! -> Update!)
            // Save the preferred size for the regular version here, because
            // checking the preferred size in the listener would change between
            // the regular and smaller version
            final Dimension requiredSize = updateNotification.getPreferredSize();
            updateNotification.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    Dimension actualSize = e.getComponent().getSize();
                    if (actualSize.width < requiredSize.width+10) {
                        if (!updateMessageSmaller) {
                            updateNotification.setText(UPDATE_MESSAGE_SMALL);
                            updateMessageSmaller = true;
                            //System.out.println("made smaller");
                        }
                    } else {
                        if (updateMessageSmaller) {
                            updateNotification.setText(UPDATE_MESSAGE);
                            updateMessageSmaller = false;
                            //System.out.println("made bigger again");
                        }
                    }
                }
            });
            
            add(updateNotification);
            addedUpdateMessage = true;
        }
    }
}
