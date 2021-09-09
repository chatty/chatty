
package chatty.gui;

import chatty.Chatty;
import chatty.lang.Language;
import chatty.gui.components.settings.SettingsUtil;
import chatty.util.dnd.DockLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
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
    protected final JMenu channels = new JMenu(Language.getString("menubar.menu.channels"));
    private final JMenu srl = new JMenu(Language.getString("menubar.menu.srl"));
    protected final JMenu srlStreams = new JMenu("Races with..");
    private final JMenu extra = new JMenu(Language.getString("menubar.menu.extra"));
    private final JMenu help = new JMenu(Language.getString("menubar.menu.help"));
    protected final JMenu layoutsMenu = new JMenu("Layouts");
    
    private final JMenuItem highlights;
    private final JMenuItem ignored;
    
    private final ItemListener itemListener;
    private final ActionListener actionListener;
    
    // Set here because it is used more than once
    private final String IGNORED_LABEL = Language.getString("menubar.dialog.ignoredMessages");
    private final String HIGHLIGHTS_LABEL = Language.getString("menubar.dialog.highlightedMessages");
    
    private final NotifyIcons notifyIcons = new NotifyIcons();
    
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
        channels.addMenuListener((MenuListener)itemListener);
        srl.addActionListener(actionListener);
        extra.addActionListener(actionListener);
        help.addActionListener(actionListener);
        
        view.addMenuListener((MenuListener)itemListener);
        layoutsMenu.addMenuListener((MenuListener)itemListener);
        
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
        addCheckboxItemSetting(titleOptions, "titleConnections");
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
        addItem(view, "dialog.eventLog");
        view.addSeparator();
        addItem(view, "dialog.search");
        view.addSeparator();
        view.add(layoutsMenu);
        
        //----------
        // Channels
        //----------
        addItem(channels, "dialog.favorites");
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
        addItem(extra,"dialog.livestreamer");
        addItem(extra,"dialog.toggleEmotes");
        extra.addSeparator();
        addItem(extra,"dialog.followers");
        addItem(extra,"dialog.subscribers");
        extra.addSeparator();
        addItem(extra,"dialog.moderationLog");
        addItem(extra,"dialog.autoModDialog");
        extra.addSeparator();
        JMenu streamChat = new JMenu("Stream Chat");
        addItem(streamChat,"dialog.streamchat");
        addCheckboxItem(streamChat, "streamChatResizable", "Resizable");
        extra.add(streamChat);
        
        JMenu streamHighlights = new JMenu("Stream Highlights");
        streamHighlights.setMnemonic(KeyEvent.VK_H);
        addItem(streamHighlights, "stream.addhighlight", "Add Stream Highlight");
        addItem(streamHighlights, "openStreamHighlights", "Open Stream Highlights");
        extra.add(streamHighlights);
        
        extra.addSeparator();
        JMenu debugOptions = new JMenu("Options");
        addCheckboxItem(debugOptions,"globalHotkeysEnabled","Global Hotkeys");
        extra.add(debugOptions);
        if (Chatty.DEBUG) {
            addItem(extra,"unhandledException", "Unhandled Exception");
            addItem(extra,"errorTest", "Error Test");
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
        addItem(help, "dialog.updates");
        //help.addSeparator();
        //addItem(help,"news","Announcements");
        
        
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
        JMenuItem item = addCheckboxItem(menu, key, Language.getString("menubar.setting."+key));
        String tooltip = Language.getStringNull("menubar.setting."+key+".tip");
        if (tooltip != null) {
            item.setToolTipText(SettingsUtil.addTooltipLinebreaks(tooltip));
        }
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
    
    //==========================
    // Update menu entries
    //==========================
    
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
    
    public void updateLayouts(Map<String, DockLayout> layouts) {
        // Remove last session layout
        layouts.remove("");
        
        layoutsMenu.removeAll();
        if (!layouts.isEmpty()) {
            for (Map.Entry<String, DockLayout> entry : layouts.entrySet()) {
                JMenu submenu = new JMenu(entry.getKey());
                addItem(submenu, "layouts.load."+entry.getKey(), "Load").setToolTipText("Load this layout (you'll still have the chance to cancel loading)");
                submenu.addSeparator();
                addItem(submenu, "layouts.remove."+entry.getKey(), "Remove");
                addItem(submenu, "layouts.save."+entry.getKey(), "Overwrite").setToolTipText("");
                layoutsMenu.add(submenu);
            }
            layoutsMenu.addSeparator();
        }
        addItem(layoutsMenu, "layouts.add", "Add");
    }
    
    //==========================
    // Notifications
    //==========================
    
    public void setUpdateNotification(boolean enabled) {
        notifyIcons.addItem("update", 1, "Update", "download.png", unused -> {
            String id = "dialog.updates";
            menuItems.get(id).getAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_FIRST, id));
        });
    }
    
    public void setSystemEventCount(int count) {
        if (count > 0) {
            notifyIcons.addItem("chattyInfo", 0, String.valueOf(count), "warning.png", id -> {
                actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_FIRST, "dialog.chattyInfo"));
            });
        }
        else {
            notifyIcons.removeItem("chattyInfo");
        }
    }
    
    private class NotifyIcons {
        
        /**
         * Associates an item id with a label.
         */
        private final Map<String, JLabel> current = new HashMap<>();
        
        /**
         * Stores the target position for an added label.
         */
        private final Map<JLabel, Integer> targetPositions = new HashMap<>();
        
        /**
         * Stores whether a label is currently flashing.
         */
        private final Map<JLabel, Boolean> flashing = new HashMap<>();
        
        /**
         * For aligning to the right.
         */
        private final Component glue = Box.createHorizontalGlue();
        
        /**
         * Add a notification item. If an item with the same id already exists
         * it's text will be updated.
         * 
         * @param id Item id, also sent to the consumer
         * @param pos The position compared to other items (not other components
         * in general)
         * @param text The text after the icon
         * @param imageFile The icon file name
         * @param consumer Called when a click on this item occurs
         */
        public void addItem(String id, int pos, String text, String imageFile, Consumer<String> consumer) {
            JLabel label = current.get(id);
            if (label == null) {
                label = new JLabel();
                int iconSize = getGraphics().getFontMetrics(label.getFont()).getHeight();
                ImageIcon icon = GuiUtil.getScaledIcon(GuiUtil.getIcon(this, imageFile), iconSize, iconSize);
                label.setIcon(icon);
                label.setIconTextGap(0);
                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                label.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 4));
                label.setToolTipText(Language.getStringNull("menubar.notification."+id));
                label.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        consumer.accept(id);
                    }

                });
                if (current.isEmpty()) {
                    add(glue);
                }
                add(label, findInsertionPos(pos));
                current.put(id, label);
                targetPositions.put(label, pos);
                revalidate();
                repaint();
            }
            if (!label.getText().equals(text)) {
                flash(label);
            }
            label.setText(text);
        }
        
        /**
         * Find the index where an element with the given targetPos should be
         * inserted.
         * 
         * @param targetPos The position relative to other already added
         * components
         * @return The index where the component should be inserted
         */
        private int findInsertionPos(int targetPos) {
            for (int i = 0;i < getComponentCount(); i++) {
                Component c = getComponent(i);
                Integer cPos = targetPositions.get(c);
                if (cPos != null && targetPos <= cPos) {
                    return i;
                }
            }
            return getComponentCount();
        }
        
        /**
         * Flash the icon of the given label for a few seconds, if it's not
         * already flashing.
         * 
         * @param label 
         */
        private void flash(JLabel label) {
            Icon icon = label.getIcon();
            if (icon != null && !flashing.containsKey(label)) {
                flashing.put(label, Boolean.TRUE);
                ImageIcon grayIcon = GuiUtil.createEmptyIcon(icon.getIconWidth(), icon.getIconHeight());
                Timer timer = new Timer(450, e -> {
                    if (label.getIcon() == icon) {
                        label.setIcon(grayIcon);
                    }
                    else {
                        label.setIcon(icon);
                    }
                });
                timer.setRepeats(true);
                timer.start();
                Timer timer2 = new Timer(3000, e-> {
                    timer.stop();
                    label.setIcon(icon);
                    flashing.remove(label);
                });
                timer2.setRepeats(false);
                timer2.start();
            }
        }
        
        /**
         * Remove the item with the given id, if it exists.
         * 
         * @param id The item id
         */
        public void removeItem(String id) {
            JLabel label = current.get(id);
            if (label != null) {
                remove(label);
                current.remove(id);
                targetPositions.remove(label);
                if (current.isEmpty()) {
                    remove(glue);
                }
                revalidate();
                repaint();
            }
        }
        
    }

}
