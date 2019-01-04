
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.util.colors.HtmlColors;
import chatty.gui.LaF;
import chatty.gui.MainGui;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.lang.Language;
import chatty.util.Sound;
import chatty.util.StringUtil;
import chatty.util.api.TokenInfo;
import chatty.util.api.usericons.Usericon;
import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
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
    
    private final JButton ok = new JButton(Language.getString("dialog.button.save"));
    private final JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
    
    private final Set<String> restartRequiredDef = new HashSet<>(Arrays.asList(
            "ffz", "nod3d", "noddraw",
            "userlistWidth", "userlistMinWidth", "userlistEnabled",
            "capitalizedNames", "correctlyCapitalizedNames", "ircv3CapitalizedNames",
            "tabOrder", "tabsMwheelScrolling", "tabsMwheelScrollingAnywhere", "inputFont",
            "bttvEmotes", "botNamesBTTV", "botNamesFFZ", "ffzEvent",
            "logPath", "logTimestamp", "logSplit", "logSubdirectories",
            "tabsPlacement", "tabsLayout", "logLockFiles",
            "laf", "lafTheme", "language"
    ));
    
    private final Set<String> reconnectRequiredDef = new HashSet<>(Arrays.asList(
            "membershipEnabled"
    ));
    
    private boolean restartRequired = false;
    private boolean reconnectRequired = false;
    
    private static final String RESTART_REQUIRED_INFO = "<html><body style='width: 280px'>"
            + Language.getString("settings.restartRequired");
    
    private static final String RECONNECT_REQUIRED_INFO = "<html><body style='width: 280px'>One or more settings "
            + "you have changed require you to reconnect to have any effect.";
    
    private final HashMap<String,StringSetting> stringSettings = new HashMap<>();
    private final HashMap<String,LongSetting> longSettings = new HashMap<>();
    private final HashMap<String,BooleanSetting> booleanSettings = new HashMap<>();
    private final HashMap<String,ListSetting> listSettings = new HashMap<>();
    private final HashMap<String,MapSetting> mapSettings = new HashMap<>();
    
    private final Settings settings;
    private final MainGui owner;
    
    private final NotificationSettings notificationSettings;
    private final UsercolorSettings usercolorSettings;
    private final MsgColorSettings msgColorSettings;
    private final ImageSettings imageSettings;
    private final HotkeySettings hotkeySettings;
    private final NameSettings nameSettings;
    
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
        HISTORY("History", Language.getString("settings.page.history")),
        NOTIFICATIONS("Notifications", Language.getString("settings.page.notifications")),
        SOUNDS("Sounds", Language.getString("settings.page.sound")),
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
        STREAM("Stream", Language.getString("settings.page.stream"));
        
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
            Page.LOGGING,
        }));
        MENU.put(Page.WINDOW, Arrays.asList(new Page[]{
            Page.TABS,
            Page.NOTIFICATIONS,
            Page.SOUNDS,
        }));
        MENU.put(Page.OTHER, Arrays.asList(new Page[]{
            Page.COMMANDS,
            Page.ADVANCED,
            Page.COMPLETION,
            Page.HISTORY,
            Page.STREAM,
            Page.HOTKEYS,
        }));
    }

    public SettingsDialog(final MainGui owner, final Settings settings) {
        super(owner, Language.getString("settings.title"), true);
        setResizable(false);
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

        // Create and add tree
        selection = Tree.createTree(MENU);
        selection.setSelectionRow(0);
        selection.setBorder(BorderFactory.createEtchedBorder());

        gbc = makeGbc(0,0,1,1);
        gbc.insets = new Insets(10,10,10,3);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 1;
        add(selection, gbc);

        // Create setting pages, the order here doesn't matter
        cardManager = new CardLayout();
        cards = new JPanel(cardManager);
        cards.add(new MainSettings(this), Page.MAIN.name);
        cards.add(new MessageSettings(this), Page.MESSAGES.name);
        cards.add(new ModerationSettings(this), Page.MODERATION.name);
        cards.add(new EmoteSettings(this), Page.EMOTES.name);
        imageSettings = new ImageSettings(this);
        cards.add(imageSettings, Page.USERICONS.name);
        cards.add(new LookSettings(this), Page.LOOK.name);
        cards.add(new FontSettings(this), Page.FONTS.name);
        cards.add(new ColorSettings(this, settings), Page.CHATCOLORS.name);
        cards.add(new HighlightSettings(this), Page.HIGHLIGHT.name);
        cards.add(new IgnoreSettings(this), Page.IGNORE.name);
        cards.add(new FilterSettings(this), Page.FILTER.name);
        msgColorSettings = new MsgColorSettings(this);
        cards.add(msgColorSettings, Page.MSGCOLORS.name);
        cards.add(new HistorySettings(this), Page.HISTORY.name);
        cards.add(new SoundSettings(this), Page.SOUNDS.name);
        notificationSettings = new NotificationSettings(this, settings);
        cards.add(notificationSettings, Page.NOTIFICATIONS.name);
        usercolorSettings = new UsercolorSettings(this);
        cards.add(usercolorSettings, Page.USERCOLORS.name);
        cards.add(new LogSettings(this), Page.LOGGING.name);
        cards.add(new WindowSettings(this), Page.WINDOW.name);
        cards.add(new TabSettings(this), Page.TABS.name);
        cards.add(new CommandSettings(this), Page.COMMANDS.name);
        cards.add(new OtherSettings(this), Page.OTHER.name);
        cards.add(new AdvancedSettings(this), Page.ADVANCED.name);
        hotkeySettings = new HotkeySettings(this);
        cards.add(hotkeySettings, Page.HOTKEYS.name);
        cards.add(new CompletionSettings(this), Page.COMPLETION.name);
        cards.add(new ChatSettings(this), Page.CHAT.name);
        nameSettings = new NameSettings(this);
        cards.add(nameSettings, Page.NAMES.name);
        cards.add(new StreamSettings(this), Page.STREAM.name);

        // Track current settings page
        currentlyShown = Page.MAIN;
        selection.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selection.getLastSelectedPathComponent();
            if (node != null) {
                showPanel((Page)node.getUserObject());
            }
        });
        
        // Cards
        gbc = makeGbc(1,0,2,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(cards, gbc);
        
        // Help Link
        gbc = makeGbc(0,2,1,1);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0,10,0,0);
        add(new LinkLabel("[maeh:muh Help]", new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                owner.openHelp("help-settings.html", currentlyShown.name);
            }
        }), gbc);
        
        // Buttons
        ok.setMnemonic(KeyEvent.VK_S);
        gbc = makeGbc(1,2,1,1);
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(4,3,8,8);
        gbc.ipadx = 16;
        gbc.ipady = 4;
        ok.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        add(ok,gbc);
        cancel.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(2,2,1,1);
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4,3,8,8);
        gbc.ipadx = 16;
        gbc.ipady = 4;
        cancel.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        add(cancel,gbc);
        
        // Button Listeners
        ok.addActionListener(this);
        cancel.addActionListener(this);

        pack();
    }
    
    /**
     * Opens the settings dialog
     */
    public void showSettings() {
        showSettings(null, null);
    }
    
    public void showSettings(String action, Object parameter) {
        loadSettings();
        notificationSettings.setUserReadPermission(settings.getList("scopes").contains(TokenInfo.Scope.USERINFO.scope));
        setLocationRelativeTo(owner);
        if (action != null) {
            editDirectly(action, parameter);
        }
        stuffBasedOnPanel();
        selection.requestFocusInWindow();
        
        pack();
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
                    usercolorSettings.editItem((String)parameter);
                } else if (action.equals("editCustomNameItem")) {
                    showPanel(Page.NAMES);
                    nameSettings.editCustomName((String)parameter);
                } else if (action.equals("addUsericonOfBadgeType")) {
                    showPanel(Page.USERICONS);
                    Usericon icon = (Usericon)parameter;
                    imageSettings.addUsericonOfBadgeType(icon.type, icon.badgeType.toString());
                } else if (action.equals("addUsericonOfBadgeTypeAllVariants")) {
                    showPanel(Page.USERICONS);
                    Usericon icon = (Usericon)parameter;
                    imageSettings.addUsericonOfBadgeType(icon.type, icon.badgeType.id);
                }
            }
        });
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
        usercolorSettings.setData(owner.getUsercolorData());
        msgColorSettings.setData(owner.getMsgColorData());
        imageSettings.setData(owner.getUsericonData());
        imageSettings.setTwitchBadgeTypes(owner.getTwitchBadgeTypes());
        hotkeySettings.setData(owner.hotkeyManager.getActionsMap(),
                owner.hotkeyManager.getData(), owner.hotkeyManager.globalHotkeysAvailable());
        notificationSettings.setData(owner.getNotificationData());
    }
    
    public void updateBackgroundColor() {
        Color foreground = HtmlColors.decode(getStringSetting("foregroundColor"));
        msgColorSettings.setDefaultForeground(foreground);
        Color background = HtmlColors.decode(getStringSetting("backgroundColor"));
        usercolorSettings.setDefaultBackground(background);
        msgColorSettings.setDefaultBackground(background);
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
        owner.setUsercolorData(usercolorSettings.getData());
        owner.setMsgColorData(msgColorSettings.getData());
        owner.setUsericonData(imageSettings.getData());
        owner.hotkeyManager.setData(hotkeySettings.getData());
        owner.setNotificationData(notificationSettings.getData());
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
        if (restartRequiredDef.contains(settingName)) {
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
    
    
    protected GridBagConstraints makeGbc(int x, int y, int w, int h) {
        return makeGbc(x, y, w, h, GridBagConstraints.CENTER);
    }
    
    protected GridBagConstraints makeGbc(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(4,5,4,5);
        gbc.anchor = anchor;
        return gbc;
    }
    
    protected GridBagConstraints makeGbcCloser(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(1,5,1,5);
        gbc.anchor = anchor;
        return gbc;
    }

    protected GridBagConstraints makeGbcSub(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(1,18,2,5);
        gbc.anchor = anchor;
        return gbc;
    }
    
    protected void addBooleanSetting(String name, BooleanSetting setting) {
        booleanSettings.put(name, setting);
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
        if (tooltipText != null && !tooltipText.isEmpty()) {
            tooltipText = "<html><body>"+StringUtil.addLinebreaks(tooltipText, 70, true);
        }
        SimpleBooleanSetting result = new SimpleBooleanSetting(description, tooltipText);
        booleanSettings.put(name,result);
        return result;
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
    
    protected ComboStringSetting addComboStringSetting(String name, boolean editable, String[] choices) {
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
        SimpleStringSetting s = new SimpleStringSetting(size, editable);
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
    protected String getStringSetting(String name) {
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
    
    protected ComboLongSetting addComboLongSetting(String name, int[] choices) {
        Map<Long, String> localizedChoices = new LinkedHashMap<>();
        for (Integer choice : choices) {
            String label = Language.getString("settings.long."+name+".option."+choice);
            localizedChoices.put((long)choice, label);
        }
        ComboLongSetting result = new ComboLongSetting(localizedChoices);
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
    protected Long getLongSetting(String name) {
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
    
    protected SimpleTableEditor addStringMapSetting(String name, int width, int height) {
        SimpleTableEditor<String> table = new SimpleTableEditor<String>(this) {

            @Override
            protected String valueFromString(String input) {
                return input;
            }
        };
        table.setPreferredSize(new Dimension(width, height));
        mapSettings.put(name, table);
        return table;
    }
    
    protected SimpleTableEditor addLongMapSetting(String name, int width, int height) {
        SimpleTableEditor<Long> table = new SimpleTableEditor<Long>(this) {

            @Override
            protected Long valueFromString(String input) {
                return Long.valueOf(input);
            }
        };
        table.setValueFilter("[^0-9]");
        table.setPreferredSize(new Dimension(width, height));
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
        if (!settings.getString("laf").equals(stringSettings.get("laf").getSettingValue())
                || !settings.getString("lafTheme").equals(stringSettings.get("lafTheme").getSettingValue())) {
            LaF.setLookAndFeel(settings.getString("laf"), settings.getString("lafTheme"));
            LaF.updateLookAndFeel();
        }
        close();
    }
    
    private void close() {
        owner.hotkeyManager.setEnabled(true);
        setVisible(false);
    }
    
    protected LinkLabelListener getLinkLabelListener() {
        return owner.getLinkLabelListener();
    }
    
    protected LinkLabelListener getSettingsHelpLinkLabelListener() {
        return settingsHelpLinkLabelListener;
    }
    
}
