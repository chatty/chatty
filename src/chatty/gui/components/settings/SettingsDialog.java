
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.util.colors.HtmlColors;
import chatty.gui.laf.LaF;
import chatty.gui.laf.LaF.LaFSettings;
import chatty.gui.MainGui;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.gui.components.settings.Tree.HighlightTreeCellRenderer;
import chatty.lang.Language;
import chatty.util.Sound;
import chatty.util.StringUtil;
import chatty.util.api.TokenInfo;
import chatty.util.api.usericons.Usericon;
import chatty.util.colors.ColorCorrectionNew;
import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Main settings dialog class that provides ways to add different kinds of
 * settings which are then automatically loaded and saved.
 * 
 * @author tduva
 */
public class SettingsDialog extends JDialog implements ActionListener {
    
    private final static Logger LOGGER = Logger.getLogger(SettingsDialog.class.getName());
    
    private static SettingsDialog INSTANCE;
    
    /**
     * Get the SettingsDialog instance and apply an action to it. Creates the
     * dialog when first used.
     * 
     * @param g
     * @param action 
     */
    public static void get(MainGui g, Consumer<SettingsDialog> action) {
        if (INSTANCE == null) {
            if (action == null) {
                INSTANCE = new SettingsDialog(g, g.getSettings());
            }
            else {
                // invokeLater to give whatever called this a chance to finish
                // (e.g. closing menu)
                SwingUtilities.invokeLater(() -> {
                    g.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    g.getGlassPane().setVisible(true);
                    long start = System.currentTimeMillis();
                    INSTANCE = new SettingsDialog(g, g.getSettings());
                    System.out.println("Settings init: "+(System.currentTimeMillis() - start));
                    g.getGlassPane().setCursor(Cursor.getDefaultCursor());
                    g.getGlassPane().setVisible(false);
                    action.accept(INSTANCE);
                });
            }
        }
        else {
            action.accept(INSTANCE);
        }
    }
    
    private final JButton ok = new JButton(Language.getString("dialog.button.save"));
    private final JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
    
    private final Set<String> restartRequiredDef = new HashSet<>(Arrays.asList(
            "ffz", "nod3d", "noddraw",
            "userlistWidth", "userlistMinWidth", "userlistEnabled",
            "capitalizedNames", "correctlyCapitalizedNames", "ircv3CapitalizedNames",
            "inputFont",
            "bttvEmotes", "botNamesBTTV", "botNamesFFZ", "ffzEvent", "seventv",
            "logPath", "logTimestamp", "logSplit", "logSubdirectories",
            "logLockFiles", "logMessageTemplate",
            "laf", "lafTheme", "lafFontScale", "language", "timezone", "locale",
            "userDialogMessageLimit", "cachePath", "imgPath", "exportPath",
            "webp"
    ));
    
    private final Set<String> reconnectRequiredDef = new HashSet<>(Arrays.asList(
            "membershipEnabled"
    ));
    
    private boolean restartRequired = false;
    private boolean reconnectRequired = false;
    protected boolean lafPreviewed;
    private Dimension autoSetSize;
    
    private static final String RESTART_REQUIRED_INFO = "<html><body style='width: 280px'>"
            + Language.getString("settings.restartRequired");
    
    private static final String RECONNECT_REQUIRED_INFO = "<html><body style='width: 280px'>One or more settings "
            + "you have changed require you to reconnect to have any effect.";
    
    private final HashMap<String,StringSetting> stringSettings = new HashMap<>();
    private final HashMap<String,LongSetting> longSettings = new HashMap<>();
    private final HashMap<String,BooleanSetting> booleanSettings = new HashMap<>();
    private final HashMap<String,ListSetting> listSettings = new HashMap<>();
    private final HashMap<String,MapSetting> mapSettings = new HashMap<>();
    
    private final Map<Page, SettingsPanel> panels = new LinkedHashMap<>();
    
    final Settings settings;
    private final MainGui owner;
    
    private final MatchingPresets matchingPresets;
    
    public enum Page {
        MAIN("Main", Language.getString("settings.page.main")),
        MESSAGES("Messages", Language.getString("settings.page.messages")),
        EMOTES("Emoticons", Language.getString("settings.page.emoticons")),
        USERICONS("Usericons", Language.getString("settings.page.usericons")),
        LOOK("Look", Language.getString("settings.page.look")),
        FONTS("Fonts", Language.getString("settings.page.fonts")), 
        CHATCOLORS("Chat Colors", Language.getString("settings.page.chatColors")),
        MSGCOLORS("Message Colors", Language.getString("settings.page.msgColors")),
        HIGHLIGHT("Highlight", Language.getString("settings.page.highlight")),
        IGNORE("Ignore", Language.getString("settings.page.ignore")),
        FILTER("Filter", Language.getString("settings.page.filter")),
        CUSTOM_TABS("Custom Tabs", Language.getString("settings.page.customTabs")),
        HISTORY("History", Language.getString("settings.page.history")),
        NOTIFICATIONS("Notifications", Language.getString("settings.page.notifications")),
        LIVE_STREAMS("Live Streams", Language.getString("settings.page.liveStreams")),
        USERCOLORS("Usercolors", Language.getString("settings.page.usercolors")),
        LOGGING("Log to file", Language.getString("settings.page.logging")),
        WINDOW("Window", Language.getString("settings.page.window")),
        TABS("Tabs", Language.getString("settings.page.tabs")),
        COMMANDS("Commands", Language.getString("settings.page.commands")),
        OTHER("Other", Language.getString("settings.page.other")),
        ADVANCED("Advanced", Language.getString("settings.page.advanced")),
        HOTKEYS("Hotkeys", Language.getString("settings.page.hotkeys")),
        COMPLETION("TAB Completion", Language.getString("settings.page.completion")),
        CHAT("Chat", Language.getString("settings.page.chat")),
        NAMES("Names", Language.getString("settings.page.names")),
        MODERATION("Moderation", Language.getString("settings.page.moderation")),
        STREAM("Stream", Language.getString("settings.page.stream")),
        SIMPLE("Simple Settings", Language.getString("settings.page.simple"));
        
        public final String name;
        public final String displayName;
        Page(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
        
    }

    private Page currentlyShown;
    
    private final CardLayout cardManager;
    private final JPanel cards;
    private final JTree selection;
    
    private final LinkLabelListener settingsHelpLinkLabelListener;
    
    private final static Map<Page, List<Page>> MENU = new LinkedHashMap<>();
    
    // Page definition for JTree navigation
    static {
        MENU.put(Page.MAIN, Arrays.asList(new Page[]{}));
        MENU.put(Page.LOOK, Arrays.asList(new Page[]{
            Page.CHATCOLORS,
            Page.MSGCOLORS,
            Page.USERCOLORS,
            Page.USERICONS,
            Page.EMOTES,
            Page.FONTS,
        }));
        MENU.put(Page.CHAT, Arrays.asList(new Page[]{
            Page.MESSAGES,
            Page.MODERATION,
            Page.NAMES,
            Page.HIGHLIGHT,
            Page.IGNORE,
            Page.FILTER,
            Page.CUSTOM_TABS,
            Page.LOGGING,
        }));
        MENU.put(Page.WINDOW, Arrays.asList(new Page[]{
            Page.TABS,
            Page.NOTIFICATIONS,
            Page.LIVE_STREAMS
        }));
        MENU.put(Page.OTHER, Arrays.asList(new Page[]{
            Page.COMMANDS,
            Page.ADVANCED,
            Page.COMPLETION,
            Page.HISTORY,
            Page.STREAM,
            Page.HOTKEYS,
        }));
        MENU.put(Page.SIMPLE, Arrays.asList(new Page[]{}));
    }

    public SettingsDialog(final MainGui owner, final Settings settings) {
        super(owner, Language.getString("settings.title"), true);
//        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });
        
        // For help links on setting pages
        settingsHelpLinkLabelListener = new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                owner.openHelp("help-settings.html", ref);
            }
        };
        
        // Save references
        this.owner = owner;
        this.settings = settings;

        // Layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;

        //--------------------------
        // Create and add tree
        //--------------------------
        selection = Tree.createTree(MENU, searchHighlightColor);
        selection.setSelectionRow(0);
        selection.setBorder(BorderFactory.createEtchedBorder());
        JScrollPane selectionScroll = new JScrollPane(selection);
        selectionScroll.setBorder(BorderFactory.createEmptyBorder());
        selectionScroll.setMinimumSize(selectionScroll.getPreferredSize());

        gbc = makeGbc(0,0,1,1);
        gbc.insets = new Insets(10,10,10,3);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 1;
        add(selectionScroll, gbc);
        
        //--------------------------
        // Create setting pages
        //--------------------------
        cardManager = new CardLayout();
        cards = new JPanel(cardManager) {
            
            @Override
            public void add(Component comp, Object constraints) {
                JScrollPane scroll = new JScrollPane(comp);
                // Set to empty instead of null, so it's not overridden when changing LaF
                scroll.setBorder(BorderFactory.createEmptyBorder());
                super.add(scroll, constraints);
            }
            
        };
        
        panels.put(Page.MAIN, new MainSettings(this));
        panels.put(Page.MESSAGES, new MessageSettings(this));
        panels.put(Page.MODERATION, new ModerationSettings(this));
        panels.put(Page.EMOTES, new EmoteSettings(this));
        panels.put(Page.USERICONS, new ImageSettings(this));
        panels.put(Page.LOOK, new LookSettings(this));
        panels.put(Page.FONTS, new FontSettings(this));
        panels.put(Page.CHATCOLORS, new ColorSettings(this, settings));
        panels.put(Page.HIGHLIGHT, new HighlightSettings(this));
        panels.put(Page.IGNORE, new IgnoreSettings(this));
        panels.put(Page.FILTER, new FilterSettings(this));
        panels.put(Page.CUSTOM_TABS, new RoutingSettings(this));
        panels.put(Page.MSGCOLORS, new MsgColorSettings(this));
        panels.put(Page.HISTORY, new HistorySettings(this));
        panels.put(Page.NOTIFICATIONS, new NotificationSettings(this, settings));
        panels.put(Page.LIVE_STREAMS, new LiveStreamsSettings(this));
        panels.put(Page.USERCOLORS, new UsercolorSettings(this));
        panels.put(Page.LOGGING, new LogSettings(this));
        panels.put(Page.WINDOW, new WindowSettings(this));
        panels.put(Page.TABS, new TabSettings(this));
        panels.put(Page.COMMANDS, new CommandSettings(this));
        panels.put(Page.OTHER, new OtherSettings(this));
        panels.put(Page.ADVANCED, new AdvancedSettings(this));
        panels.put(Page.HOTKEYS, new HotkeySettings(this));
        panels.put(Page.COMPLETION, new CompletionSettings(this));
        panels.put(Page.CHAT, new ChatSettings(this));
        panels.put(Page.NAMES, new NameSettings(this));
        panels.put(Page.STREAM, new StreamSettings(this));
        panels.put(Page.SIMPLE, new SimpleSettings(this));
        
        for (Map.Entry<Page, SettingsPanel> entry : panels.entrySet()) {
            cards.add(entry.getValue(), entry.getKey().name);
        }
        
        matchingPresets = new MatchingPresets(this);
        
        // Track current settings page
        currentlyShown = Page.MAIN;
        selection.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selection.getLastSelectedPathComponent();
            if (node != null) {
                showPanel((Page)node.getUserObject());
            }
        });
        
        //--------------------------
        // Cards
        //--------------------------
        gbc = makeGbc(1,0,3,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(cards, gbc);
        
        //--------------------------
        // Help Link
        //--------------------------
        gbc = makeGbc(0,2,1,1);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0,10,0,0);
        add(new LinkLabel("[maeh:muh Help]", new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                if (currentlyShown == Page.COMMANDS) {
                    owner.openHelp("help-custom_commands.html", "");
                }
                else {
                    owner.openHelp("help-settings.html", currentlyShown.name);
                }
            }
        }), gbc);
        
        //--------------------------
        // Search
        //--------------------------
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JTextField searchField = new JTextField(10);
        GuiUtil.addChangeListener(searchField.getDocument(), e -> {
            search(searchField.getText());
        });
        
        JLabel searchLabel = new JLabel(Language.getString("settings.search"));
        searchLabel.setLabelFor(searchField);
        
        JButton resetSearchButton = new JButton(Language.getString("settings.resetSearch"));
        GuiUtil.smallButtonInsets(resetSearchButton);
        resetSearchButton.addActionListener(e -> {
            searchField.setText("");
        });
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(resetSearchButton);
        
        gbc = makeGbc(1,2,1,1);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 10, 0, 0);
        add(searchPanel, gbc);
        
        //--------------------------
        // Buttons
        //--------------------------
        ok.setMnemonic(KeyEvent.VK_S);
        gbc = makeGbc(2,2,1,1);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(4,3,8,8);
        gbc.ipadx = 16;
        gbc.ipady = 4;
        GuiUtil.smallButtonInsets(ok);
        add(ok,gbc);
        cancel.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(3,2,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4,3,8,8);
        gbc.ipadx = 16;
        gbc.ipady = 4;
        GuiUtil.smallButtonInsets(cancel);
        add(cancel,gbc);
        
        // Button Listeners
        ok.addActionListener(this);
        cancel.addActionListener(this);
        
        pack();
    }
    
    /**
     * Get the panel of the given class. As long as only one panel per class is
     * added (which should be normal for this), this should work fine.
     * 
     * @param <T>
     * @param c
     * @return 
     */
    private <T> T getPanel(Class<T> c) {
        for (SettingsPanel panel : panels.values()) {
            if (panel.getClass().equals(c)) {
                return c.cast(panel);
            }
        }
        return null;
    }
    
    /**
     * Opens the settings dialog
     */
    public void showSettings() {
        showSettings(null, null);
    }
    
    public void showSettings(String action, Object parameter) {
        //------------
        // Initialize
        //------------
        loadSettings();
        getPanel(LiveStreamsSettings.class).setUserReadPermission(settings.getList("scopes").contains(TokenInfo.Scope.FOLLOWS.scope));
        if (action != null) {
            editDirectly(action, parameter);
        }
        stuffBasedOnPanel();
        selection.requestFocusInWindow();
        
        //-----------------
        // Size / Position
        //-----------------
        // If not set and not manually resized window (not ideal, but should be
        // good enough for now, manually resizing indicates wanting to have it
        // a certain way)
        if (autoSetSize == null || autoSetSize.equals(getSize())) {
            pack();
            Rectangle screenBounds = GuiUtil.getEffectiveScreenBounds(this);
//            screenBounds = new Rectangle(700, 400); // Test
            if (getHeight() > screenBounds.height) {
                /**
                 * Add some width for possible scrollbars, not ideal but should do
                 * for now (especially since this shouldn't happen for many users)
                 */
                setSize(getWidth()+50, screenBounds.height);
            }
            if (getWidth() > screenBounds.width) {
                setSize(screenBounds.width, getHeight());
            }
            GuiUtil.setLocationRelativeTo(this, owner);
            autoSetSize = getSize(autoSetSize);
        }
        lafPreviewed = false;
        setVisible(true);
    }
    
    private void stuffBasedOnPanel() {
        if (currentlyShown.equals(Page.HOTKEYS)) {
            owner.hotkeyManager.setEnabled(false);
        }
    }
    
    private void editDirectly(final String action, final Object parameter) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (action.equals("editUsercolorItem")) {
                    showPanel(Page.USERCOLORS);
                    getPanel(UsercolorSettings.class).editItem((String)parameter);
                } else if (action.equals("editCustomNameItem")) {
                    showPanel(Page.NAMES);
                    getPanel(NameSettings.class).editCustomName((String)parameter);
                } else if (action.equals("addUsericonOfBadgeType")) {
                    showPanel(Page.USERICONS);
                    Usericon icon = (Usericon)parameter;
                    getPanel(ImageSettings.class).addUsericonOfBadgeType(icon.type, icon.badgeType.toString());
                } else if (action.equals("addUsericonOfBadgeTypeAllVariants")) {
                    showPanel(Page.USERICONS);
                    Usericon icon = (Usericon)parameter;
                    getPanel(ImageSettings.class).addUsericonOfBadgeType(icon.type, icon.badgeType.id);
                } else if (action.equals("selectHighlight")) {
                    showPanel(Page.HIGHLIGHT);
                    @SuppressWarnings("unchecked") // By convention
                    Collection<String> data = (Collection<String>) parameter;
                    getPanel(HighlightSettings.class).selectItems(data);
                } else if (action.equals("selectIgnore")) {
                    showPanel(Page.IGNORE);
                    @SuppressWarnings("unchecked") // By convention
                    Collection<String> data = (Collection<String>) parameter;
                    getPanel(IgnoreSettings.class).selectItems(data);
                } else if (action.equals("selectMsgColor")) {
                    showPanel(Page.MSGCOLORS);
                    getPanel(MsgColorSettings.class).selectItem((String) parameter);
                } else if (action.equals("selectRouting")) {
                    showPanel(Page.CUSTOM_TABS);
                    @SuppressWarnings("unchecked") // By convention
                    Collection<String> data = (Collection<String>) parameter;
                    getPanel(RoutingSettings.class).selectItems(data);
                } else if (action.equals("selectNotification")) {
                    showPanel(Page.NOTIFICATIONS);
                    @SuppressWarnings("unchecked") // By convention
                    long id = Long.parseLong((String) parameter);
                    getPanel(NotificationSettings.class).selectItem(id);
                } else if (action.equals("show")) {
                    showPage((String) parameter);
                } else if (action.equals("editHotkey")) {
                    showPanel(Page.HOTKEYS);
                    getPanel(HotkeySettings.class).edit((String) parameter);
                }
            }
        });
    }
    
    public void showPage(String page) {
        try {
            showPanel(Page.valueOf(page));
        }
        catch (IllegalArgumentException ex) {
            LOGGER.warning("Invalid settings page: "+page);
        }
    }
    
    private void showPanel(Page page) {
        cardManager.show(cards, page.name);
        currentlyShown = page;
        Tree.setSelected(selection, page);
        stuffBasedOnPanel();
    }
    
    /**
     * Loads all settings from the settings object
     */
    private void loadSettings() {
        loadStringSettings();
        loadNumericSettings();
        loadBooleanSettings();
        loadListSettings();
        loadMapSettings();
        updateBackgroundColor();
        getPanel(UsercolorSettings.class).setData(owner.getUsercolorData());
        getPanel(MsgColorSettings.class).setData(owner.getMsgColorData());
        getPanel(ImageSettings.class).setData(owner.getUsericonData());
        getPanel(ImageSettings.class).setHiddenBadgesData(owner.getHiddenBadgesData());
        getPanel(ImageSettings.class).setTwitchBadgeTypes(owner.getTwitchBadgeTypes());
        getPanel(HotkeySettings.class).setData(owner.hotkeyManager.getActionsMap(),
                owner.hotkeyManager.getDescriptionsMap(),
                owner.hotkeyManager.getData(),
                owner.hotkeyManager.globalHotkeysAvailable());
        getPanel(NotificationSettings.class).setData(owner.getNotificationData());
        getPanel(EmoteSettings.class).setData(owner.localEmotes.getData());
        getPanel(RoutingSettings.class).setData(owner.routingManager.getData());
    }
    
    public void updateBackgroundColor() {
        Color foreground = HtmlColors.decode(getStringSetting("foregroundColor"));
        getPanel(MsgColorSettings.class).setDefaultForeground(foreground);
        Color background = HtmlColors.decode(getStringSetting("backgroundColor"));
        getPanel(UsercolorSettings.class).setDefaultBackground(background);
        getPanel(MsgColorSettings.class).setDefaultBackground(background);
    }
    
    /**
     * Loads all settings of type String
     */
    private void loadStringSettings() {
        for (String settingName : stringSettings.keySet()) {
            StringSetting setting = stringSettings.get(settingName);
            String value = settings.getString(settingName);
            setting.setSettingValue(value);
        }
    }
    
    /**
     * Loads all settings of type Integer
     */
    private void loadNumericSettings() {
        for (String settingName : longSettings.keySet()) {
            LongSetting setting = longSettings.get(settingName);
            Long value = settings.getLong(settingName);
            setting.setSettingValue(value);
        }
    }
    
    /**
     * Loads all settings of type Boolean
     */
    private void loadBooleanSettings() {
        for (String settingName : booleanSettings.keySet()) {
            BooleanSetting setting = booleanSettings.get(settingName);
            Boolean value = settings.getBoolean(settingName);
            setting.setSettingValue(value);
        }
    }
    
    private void loadListSettings() {
        for (String settingName : listSettings.keySet()) {
            ListSetting setting = listSettings.get(settingName);
            List data = settings.getList(settingName);
            setting.setSettingValue(data);
        }
    }
    
    private void loadMapSettings() {
        for (String settingName : mapSettings.keySet()) {
            MapSetting setting = mapSettings.get(settingName);
            Map data = settings.getMap(settingName);
            setting.setSettingValue(data);
        }
    }
    
    /**
     * Saves settings into the settings object
     */
    private void saveSettings() {
        restartRequired = false;
        reconnectRequired = false;
        saveStringSettings();
        saveBooleanSettings();
        saveIntegerSettings();
        saveListSettings();
        saveMapSettings();
        owner.setUsercolorData(getPanel(UsercolorSettings.class).getData());
        owner.setMsgColorData(getPanel(MsgColorSettings.class).getData());
        owner.setUsericonData(getPanel(ImageSettings.class).getData());
        owner.setHiddenBadgesData(getPanel(ImageSettings.class).getHiddenBadgesData());
        owner.hotkeyManager.setData(getPanel(HotkeySettings.class).getData());
        owner.setNotificationData(getPanel(NotificationSettings.class).getData());
        owner.localEmotes.setData(getPanel(EmoteSettings.class).getData());
        owner.routingManager.setData(getPanel(RoutingSettings.class).getData());
        if (restartRequired) {
            JOptionPane.showMessageDialog(this, RESTART_REQUIRED_INFO, "Info", JOptionPane.INFORMATION_MESSAGE);
        }
        if (reconnectRequired) {
            String[] options = new String[]{"Reconnect now", "Reconnect manually"};
            int result = JOptionPane.showOptionDialog(this,
                    RECONNECT_REQUIRED_INFO,
                    "Reconect?",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, null);
            if (result == 0) {
                owner.reconnect();
            }
        }
    }
    
    /**
     * Saves all settings of type String
     */
    private void saveStringSettings() {
        for (String settingName  : stringSettings.keySet()) {
            StringSetting setting = stringSettings.get(settingName);
            String value = setting.getSettingValue();
            if (settings.setString(settingName,value) == Setting.CHANGED) {
                changed(settingName);
            }
        }
    }
    
    /**
     * Saves all settings of type Boolean
     */
    private void saveBooleanSettings() {
        for (String settingName : booleanSettings.keySet()) {
            BooleanSetting setting = booleanSettings.get(settingName);
            if (settings.setBoolean(settingName, setting.getSettingValue()) == Setting.CHANGED) {
                changed(settingName);
            }
        }
    }
    
    /**
     * Saves all settings of type Integer.
     * 
     * Parses the String of the JTextFields into an Integer and only saves if
     * it succeeds
     */
    private void saveIntegerSettings() {
        for (String settingName : longSettings.keySet()) {
            LongSetting setting = longSettings.get(settingName);
            Long value = setting.getSettingValue();
            if (value != null) {
                if (settings.setLong(settingName, setting.getSettingValue()) == Setting.CHANGED) {
                    changed(settingName);
                }
            } else {
                LOGGER.warning("Invalid number format for setting "+settingName);
            }
        }
    }
    
    private void changed(String settingName) {
        if (restartRequiredDef.contains(settingName) || lafPreviewed) {
            restartRequired = true;
            reconnectRequired = false;
        }
        if (reconnectRequiredDef.contains(settingName) && !restartRequired) {
            reconnectRequired = true;
        }
    }
    
    private void saveListSettings() {
        for (String settingName : listSettings.keySet()) {
            ListSetting setting = listSettings.get(settingName);
            settings.putList(settingName, setting.getSettingValue());
//            settingsgetList2t(settingName).clear();
//            settinggetList2st(settingName).addAll(setting.getSettingValue());
            settings.setSettingChanged(settingName);
        }
    }
    
    private void saveMapSettings() {
        for (String settingName : mapSettings.keySet()) {
            MapSetting setting = mapSettings.get(settingName);
            boolean changed = settings.putMap(settingName, setting.getSettingValue());
            if (changed) {
                settings.setSettingChanged(settingName);
            }
        }
    }
    
    protected void showMatchingPresets() {
        matchingPresets.show(this);
    }
    
    protected static GridBagConstraints makeGbc(int x, int y, int w, int h) {
        return makeGbc(x, y, w, h, GridBagConstraints.CENTER);
    }
    
    protected static GridBagConstraints makeGbc(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(4,5,4,5);
        gbc.anchor = anchor;
        return gbc;
    }
    
    protected static GridBagConstraints makeNoGapGbc(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(0,0,0,0);
        gbc.anchor = anchor;
        return gbc;
    }
    
    protected static GridBagConstraints makeGbcCloser(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(1,5,1,5);
        gbc.anchor = anchor;
        return gbc;
    }

    protected static GridBagConstraints makeGbcSub(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(1,18,2,5);
        gbc.anchor = anchor;
        return gbc;
    }
    
    protected static GridBagConstraints makeGbcStretchHorizontal(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        return gbc;
    }
    
    protected void addBooleanSetting(String name, BooleanSetting setting) {
        booleanSettings.put(name, setting);
    }
    
    public Boolean getBooleanSettingValue(String name) {
        if (booleanSettings.containsKey(name)) {
            return booleanSettings.get(name).getSettingValue();
        }
        return null;
    }
    
    /**
     * Add boolean setting where the description is in the Strings file as
     * "settings.boolean.[settingName]" and the optional tooltip as
     * "settings.boolean.[settingName].tip".
     * 
     * @param name The setting name
     * @return 
     */
    protected SimpleBooleanSetting addSimpleBooleanSetting(String name) {
        return addSimpleBooleanSetting(name,
                Language.getString("settings.boolean."+name),
                Language.getString("settings.boolean."+name+".tip", false));
    }
    
    protected SimpleBooleanSetting addSimpleBooleanSetting(String name, String description, String tooltipText) {
        SimpleBooleanSetting result = makeSimpleBooleanSetting(description, tooltipText);
        booleanSettings.put(name,result);
        return result;
    }
    
    protected SimpleBooleanSetting makeSimpleBooleanSetting(String labelName) {
        return makeSimpleBooleanSetting(
                Language.getString("settings.label."+labelName),
                Language.getString("settings.label."+labelName+".tip", false));
    }
    
    protected SimpleBooleanSetting makeSimpleBooleanSetting(String description, String tooltipText) {
        tooltipText = SettingsUtil.addTooltipLinebreaks(tooltipText);
        return new SimpleBooleanSetting(description, tooltipText);
    }
    
    protected void setBooleanSetting(String name, Boolean value) {
        if (booleanSettings.containsKey(name)) {
            booleanSettings.get(name).setSettingValue(value);
        }
    }
    
    protected Boolean getBooleanSetting(String name) {
        if (booleanSettings.containsKey(name)) {
            return booleanSettings.get(name).getSettingValue();
        }
        return null;
    }
    
    protected ComboStringSetting addComboStringSetting(String name, boolean editable, String... choices) {
        Map<String, String> localizedChoices = new LinkedHashMap<>();
        for (String choice : choices) {
            String label = Language.getString("settings.string."+name+".option."+choice, false);
            if (label != null) {
                localizedChoices.put(choice, label);
            } else {
                localizedChoices.put(choice, choice);
            }
        }
        ComboStringSetting result = new ComboStringSetting(localizedChoices);
        result.setEditable(editable);
        stringSettings.put(name, result);
        return result;
    }
    
    protected ComboStringSetting addComboStringSetting(String name, int size, boolean editable, Map<String, String> choices) {
        ComboStringSetting result = new ComboStringSetting(choices);
        result.setEditable(editable);
        stringSettings.put(name, result);
        return result;
    }
    
    protected StringSetting addStringSetting(String settingName, StringSetting setting) {
        stringSettings.put(settingName, setting);
        return setting;
    }
    
    protected JTextField addSimpleStringSetting(String settingName, int size, boolean editable) {
        return addSimpleStringSetting(settingName, size, editable, null);
    }
    
    protected JTextField addSimpleStringSetting(String settingName, int size,
            boolean editable, DataFormatter<String> formatter) {
        SimpleStringSetting s = new SimpleStringSetting(size, editable, formatter);
        addStringSetting(settingName, s);
        return s;
    }
    
    protected EditorStringSetting addEditorStringSetting(String settingName, int size,
            boolean editable, final String title, final boolean linebreaks,
            String info) {
        return addEditorStringSetting(settingName, size, editable, title, linebreaks, info, null);
    }
    
    protected EditorStringSetting addEditorStringSetting(String settingName, int size,
            boolean editable, final String title, final boolean linebreaks,
            String info, Editor.Tester tester) {
        EditorStringSetting s = new EditorStringSetting(this, title, size, true, linebreaks, info, tester);
        addStringSetting(settingName, s);
        return s;
    }
    
    /**
     * Changes the String setting with the given name to the given value. Does
     * nothing if a setting with this name doesn't exist.
     * 
     * @param name The name of the setting
     * @param value The new value
     */
    protected void setStringSetting(String name, String value) {
        if (stringSettings.containsKey(name)) {
            stringSettings.get(name).setSettingValue(value);
        }
    }
    
    /**
     * Retrieves the value of the String setting with the given name.
     * 
     * @param name The name of the setting
     * @return The value of the setting or null if it doesn't exist
     */
    public String getStringSetting(String name) {
        if (stringSettings.containsKey(name)) {
            return stringSettings.get(name).getSettingValue();
        }
        return null;
    }
    
    /**
     * Adds an Integer setting.
     * 
     * @param name The name of the setting
     * @param size The size of the editbox
     * @param editable Whether the value can be changed by the user
     * @return The JTextField used for this setting
     */
    protected JTextField addSimpleLongSetting(String name, int size, boolean editable) {
        SimpleLongSetting result = new SimpleLongSetting(size, editable);
        addLongSetting(name, result);
        return result;
    }
    
    protected ComboLongSetting addComboLongSetting(String name, int... choices) {
        return addComboLongSetting(name, false, choices);
    }
    
    protected ComboLongSetting addComboLongSetting(String name, boolean editable, int... choices) {
        Map<Long, String> localizedChoices = new LinkedHashMap<>();
        for (Integer choice : choices) {
            String label = Language.getString("settings.long."+name+".option."+choice, false);
            if (label == null) {
                label = String.valueOf(choice);
            }
            localizedChoices.put((long)choice, label);
        }
        ComboLongSetting result = new ComboLongSetting(localizedChoices);
        result.setEditable(editable);
        result.setToolTipText(SettingsUtil.addTooltipLinebreaks(Language.getString("settings.long."+name+".tip", false)));
        longSettings.put(name, result);
        return result;
    }
    
    protected void addLongSetting(String settingName, LongSetting setting) {
        longSettings.put(settingName, setting);
    }
    
    /**
     * Changes the value of an Integer setting to the given value. Does nothing
     * if the setting doesn't exist.
     * 
     * @param name The name of the setting
     * @param value The new value
     */
    protected void setLongSetting(String name, Long value) {
        if (longSettings.containsKey(name)) {
            longSettings.get(name).setSettingValue(value);
        }
    }
    
    /**
     * Retrieves the Integer value for the given Integer setting. Returns null
     * if value couldn't be parsed as an Integer or if the setting doesn't
     * exist.
     * 
     * @param name
     * @return 
     */
    public Long getLongSetting(String name) {
        if (longSettings.containsKey(name)) {
            return longSettings.get(name).getSettingValue();
        }
        return null;
    }
    
    /**
     * Adds a List setting.
     * 
     * @param name
     * @param width
     * @param height
     * @return 
     */
    protected ListSelector addListSetting(String name, String title, int width, int height, 
            boolean manualSorting, boolean alphabeticSorting) {
        ListSelector result = new ListSelector(this, title, manualSorting, alphabeticSorting);
        result.setPreferredSize(new Dimension(width, height));
        listSettings.put(name, result);
        return result;
    }
    
    protected SimpleTableEditor<String> addStringMapSetting(String name,
            int width, int height,
            String keyLabel, String valueLabel) {
        SimpleTableEditor<String> table = new SimpleTableEditor<String>(this, String.class, keyLabel, valueLabel) {

            @Override
            protected String valueFromString(String input) {
                return input;
            }
        };
        table.setPreferredSize(new Dimension(width, height));
        mapSettings.put(name, table);
        return table;
    }
    
    protected SimpleTableEditor<Long> addLongMapSetting(String name,
            int width, int height,
            String keyLabel, String valueLabel) {
        SimpleTableEditor<Long> table = new SimpleTableEditor<Long>(this, Long.class, keyLabel, valueLabel) {

            @Override
            protected Long valueFromString(String input) {
                try {
                    return Long.valueOf(input);
                }
                catch (NumberFormatException ex) {
                    return (long)0;
                }
            }
        };
        table.setValueFilter("[^0-9]");
        table.setPreferredSize(new Dimension(width, height));
        table.setTableEditorEditAllHandler(new TableEditor.TableEditorEditAllHandler<SimpleTableEditor.MapItem<Long>>() {
            
            private final Pattern PARSE_LINE = Pattern.compile("(.*) (-?[0-9]+)");
            
            @Override
            public String toString(List<SimpleTableEditor.MapItem<Long>> data) {
                StringBuilder b = new StringBuilder();
                for (SimpleTableEditor.MapItem<Long> entry : data) {
                    b.append(entry.key).append(" ").append(entry.value).append("\n");
                }
                return b.toString();
            }

            @Override
            public List<SimpleTableEditor.MapItem<Long>> toData(String input) {
                List<SimpleTableEditor.MapItem<Long>> result = new ArrayList<>();
                String[] split = StringUtil.splitLines(input);
                for (String line : split) {
                    Matcher m = PARSE_LINE.matcher(line);
                    if (m.matches()) {
                        try {
                            result.add(new SimpleTableEditor.MapItem<>(m.group(1), Long.valueOf(m.group(2))));
                        }
                        catch (NumberFormatException ex) {
                            // Don't add
                        }
                    }
                }
                return result;
            }

            @Override
            public StringEditor getEditor() {
                return null;
            }

            @Override
            public String getEditorTitle() {
                return "Edit all entries";
            }

            @Override
            public String getEditorHelp() {
                return null;
            }
        });
        mapSettings.put(name, table);
        return table;
    }
    
    protected void clearHistory() {
        owner.clearHistory();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == ok) {
            save();
        }
        else if (e.getSource() == cancel) {
            cancel();
        }
    }
    
    private void save() {
        saveSettings();
        close();
    }
    
    private void cancel() {
        Sound.setDeviceName(settings.getString("soundDevice"));
        if (lafPreviewed) {
            LaF.setLookAndFeel(LaFSettings.fromSettings(settings));
            LaF.updateLookAndFeel();
        }
        close();
    }
    
    private void close() {
        owner.hotkeyManager.setEnabled(true);
        setVisible(false);
        dispose();
    }
    
    protected LinkLabelListener getLinkLabelListener() {
        return owner.getLinkLabelListener();
    }
    
    protected LinkLabelListener getSettingsHelpLinkLabelListener() {
        return settingsHelpLinkLabelListener;
    }
    
    //==========================
    // Search
    //==========================
    private final Color searchHighlightColor = ColorCorrectionNew.offset(getBackground(), 0.8f);
    private final Map<Component, State> stateBeforeSearch = new HashMap<>();
    
    private static class State {
        private final Color color;
        private final boolean opaque;

        public State(Color color, boolean opaque) {
            this.color = color;
            this.opaque = opaque;
        }
        
    }
    
    /**
     * Search for a given text in the Settings Dialog. The previous search
     * results are reset. The search is case-insensitive.
     * 
     * @param search Must be longer than 2 characters, otherwise the search is
     * just reset
     */
    private void search(String search) {
        resetSearchHighlights();
        if (search != null && search.length() > 2) {
            search = StringUtil.toLowerCase(search);
            for (Map.Entry<Page, SettingsPanel> panel : panels.entrySet()) {
                int numResults = search2(search, panel.getValue());
                if (numResults > 0) {
                    ((HighlightTreeCellRenderer) selection.getCellRenderer()).setHighlight(panel.getKey(), true);
                }
            }
        }
        selection.repaint();
    }
    
    private void addSearchHighlight(Component comp) {
        if (stateBeforeSearch.containsKey(comp)) {
            return;
        }
        JComponent component = (JComponent) comp;
        Color color = comp.isBackgroundSet() ? comp.getBackground() : null;
        stateBeforeSearch.put(comp, new State(color, component.isOpaque()));
        if (!component.isOpaque()) {
            component.setOpaque(true);
        }
        comp.setBackground(searchHighlightColor);
    }
    
    private void resetSearchHighlights() {
        // Tree
        for (Page page : panels.keySet()) {
            ((HighlightTreeCellRenderer) selection.getCellRenderer()).setHighlight(page, false);
        }
        // Components
        for (Map.Entry<Component, State> entry : stateBeforeSearch.entrySet()) {
            Component comp = entry.getKey();
            if (comp instanceof JTabbedPane) {
                JTabbedPane tabs = (JTabbedPane) comp;
                for (int i=0; i<tabs.getTabCount(); i++) {
                    tabs.setBackgroundAt(i, null);
                }
            }
            else {
                JComponent component = (JComponent) entry.getKey();
                component.setBackground(entry.getValue().color);
                component.setOpaque(entry.getValue().opaque);
            }
        }
        stateBeforeSearch.clear();
    }
    
    /**
     * Recursive search through all components.
     * 
     * @param search
     * @param container
     * @return 
     */
    private int search2(String search, Container container) {
        int numResults = 0;
        for (Component comp : container.getComponents()) {
            int subResults = 0;
            // Search children of container
            if (comp instanceof Container
                    && !(comp instanceof JList)
                    && !(comp instanceof JTable)) {
                subResults = search2(search, (Container) comp);
            }
            // Highlight parent if results have been found
            if (subResults > 0) {
                if (comp instanceof JTabbedPane) {
                    JTabbedPane tabs = (JTabbedPane) comp;
                    boolean highlighted = false;
                    for (int i = 0; i < tabs.getTabCount(); i++) {
                        for (Component hl : stateBeforeSearch.keySet()) {
                            if (SwingUtilities.isDescendingFrom(hl, tabs.getComponentAt(i))) {
                                tabs.setBackgroundAt(i, searchHighlightColor);
                                highlighted = true;
                            }
                        }
                    }
                    if (highlighted) {
                        stateBeforeSearch.put(tabs, null);
                    }
                }
            }
            // Check this component
            if (checkSearchComponent(comp, search)) {
                numResults++;
                addSearchHighlight(comp);
            }
            numResults += subResults;
        }
        return numResults;
    }
    
    /**
     * Check a single component for the searched text.
     * 
     * @param comp
     * @param search
     * @return 
     */
    private boolean checkSearchComponent(Component comp, String search) {
        if (comp instanceof JLabel) {
            if (checkSearchText(((JLabel) comp).getText(), search)) {
                return true;
            }
            if (checkSearchText(((JLabel) comp).getToolTipText(), search)) {
                return true;
            }
        }
        if (comp instanceof JEditorPane) {
            if (checkSearchText(((JEditorPane) comp).getText(), search)) {
                return true;
            }
        }
        if (comp instanceof JCheckBox) {
            if (checkSearchText(((JCheckBox) comp).getText(), search)) {
                return true;
            }
            if (checkSearchText(((JCheckBox) comp).getToolTipText(), search)) {
                return true;
            }
        }
        if (comp instanceof JButton) {
            if (checkSearchText(((JButton) comp).getText(), search)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean checkSearchText(String text, String search) {
        if (text == null || search == null) {
            return false;
        }
        return StringUtil.toLowerCase(text).contains(search);
    }
    
}
