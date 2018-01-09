
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.HtmlColors;
import chatty.gui.LaF;
import chatty.gui.MainGui;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.util.Sound;
import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Main settings dialog class that provides ways to add different kinds of
 * settings which are then automatically loaded and saved.
 * 
 * @author tduva
 */
public class SettingsDialog extends JDialog implements ActionListener {
    
    private final static Logger LOGGER = Logger.getLogger(SettingsDialog.class.getName());
    
    private final JButton ok = new JButton("Save");
    private final JButton cancel = new JButton("Cancel");
    
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
    
    private static final String RESTART_REQUIRED_INFO = "<html><body style='width: 280px'>One or more settings "
            + "you have changed require a restart of Chatty to take any or full effect.";
    
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

    private static final String PANEL_MAIN = "Main";
    private static final String PANEL_MESSAGES = "Messages";
    private static final String PANEL_EMOTES = "Emoticons";
    private static final String PANEL_USERICONS = "Usericons";
    private static final String PANEL_COLORS = "Colors";
    private static final String PANEL_MSGCOLORS = "Msg Colors";
    private static final String PANEL_HIGHLIGHT = "Highlight";
    private static final String PANEL_IGNORE = "Ignore";
    private static final String PANEL_HISTORY = "History";
    private static final String PANEL_SOUND = "Sounds";
    private static final String PANEL_NOTIFICATIONS = "Notifications";
    private static final String PANEL_USERCOLORS = "Usercolors";
    private static final String PANEL_LOG = "Log to file";
    private static final String PANEL_WINDOW = "Window";
    private static final String PANEL_TABS = "Tabs";
    private static final String PANEL_COMMANDS = "Commands";
    private static final String PANEL_OTHER = "Other";
    private static final String PANEL_ADVANCED = "Advanced";
    private static final String PANEL_HOTKEYS = "Hotkeys";
    private static final String PANEL_COMPLETION = "TAB Completion";
    private static final String PANEL_CHAT = "Chat";
    private static final String PANEL_NAMES = "Names";
    private static final String PANEL_MODERATION = "Moderation";

    private String currentlyShown;
    
    private final CardLayout cardManager;
    private final JPanel cards;
    private final JList<String> selection;
    
    private final LinkLabelListener settingsHelpLinkLabelListener;
    
    private static final String[] MENU = {
        PANEL_MAIN,
        PANEL_MESSAGES,
        PANEL_MODERATION,
        PANEL_CHAT,
        PANEL_EMOTES,
        PANEL_USERICONS,
        PANEL_COLORS,
        PANEL_MSGCOLORS,
        PANEL_USERCOLORS,
        PANEL_NAMES,
        PANEL_HIGHLIGHT,
        PANEL_IGNORE,
        PANEL_HISTORY,
        PANEL_SOUND,
        PANEL_NOTIFICATIONS,
        PANEL_LOG,
        PANEL_WINDOW,
        PANEL_TABS,
        PANEL_COMMANDS,
        PANEL_OTHER,
        PANEL_ADVANCED,
        PANEL_HOTKEYS,
        PANEL_COMPLETION
    };

    public SettingsDialog(final MainGui owner, final Settings settings) {
        super(owner,"Settings",true);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });
        
        settingsHelpLinkLabelListener = new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                owner.openHelp("help-settings.html", ref);
            }
        };
        
        // Save references
        this.owner = owner;
        this.settings = settings;

        /*
         * Layout
         */
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;

        /*
         * Add to Tabs
         */
        //JTabbedPane tabs = new JTabbedPane();
        selection = new JList<>(MENU);
        selection.setSelectedIndex(0);
        selection.setSize(200, 200);
        Font defaultFont = selection.getFont();
        //selection.setFont(new Font(defaultFont.getFontName(), Font.BOLD, 12));
        selection.setFixedCellHeight(20);
        selection.setFixedCellWidth(100);
        selection.setBorder(BorderFactory.createEtchedBorder());
//        selection.setBackground(getBackground());
//        selection.setForeground(getForeground());

        gbc = makeGbc(0,0,1,1);
        gbc.insets = new Insets(10,10,10,3);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 1;
        add(selection, gbc);
        
        cardManager = new CardLayout();
        cards = new JPanel(cardManager);
        cards.add(new MainSettings(this), PANEL_MAIN);
        cards.add(new MessageSettings(this), PANEL_MESSAGES);
        cards.add(new ModerationSettings(this), PANEL_MODERATION);
        cards.add(new EmoteSettings(this), PANEL_EMOTES);
        imageSettings = new ImageSettings(this);
        cards.add(imageSettings, PANEL_USERICONS);
        cards.add(new ColorSettings(this, settings), PANEL_COLORS);
        cards.add(new HighlightSettings(this), PANEL_HIGHLIGHT);
        cards.add(new IgnoreSettings(this), PANEL_IGNORE);
        msgColorSettings = new MsgColorSettings(this);
        cards.add(msgColorSettings, PANEL_MSGCOLORS);
        cards.add(new HistorySettings(this), PANEL_HISTORY);
        cards.add(new SoundSettings(this), PANEL_SOUND);
        notificationSettings = new NotificationSettings(this, settings);
        cards.add(notificationSettings, PANEL_NOTIFICATIONS);
        usercolorSettings = new UsercolorSettings(this);
        cards.add(usercolorSettings, PANEL_USERCOLORS);
        cards.add(new LogSettings(this), PANEL_LOG);
        cards.add(new WindowSettings(this), PANEL_WINDOW);
        cards.add(new TabSettings(this), PANEL_TABS);
        cards.add(new CommandSettings(this), PANEL_COMMANDS);
        cards.add(new OtherSettings(this), PANEL_OTHER);
        cards.add(new AdvancedSettings(this), PANEL_ADVANCED);
        hotkeySettings = new HotkeySettings(this);
        cards.add(hotkeySettings, PANEL_HOTKEYS);
        cards.add(new CompletionSettings(this), PANEL_COMPLETION);
        cards.add(new ChatSettings(this), PANEL_CHAT);
        nameSettings = new NameSettings(this);
        cards.add(nameSettings, PANEL_NAMES);

        currentlyShown = PANEL_MAIN;
        selection.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                showPanel(selection.getSelectedValue());
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
                owner.openHelp("help-settings.html", currentlyShown);
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
        
        // Listeners
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
    
    public void showSettings(String action, String parameter) {
        loadSettings();
        notificationSettings.setUserReadPermission(settings.getBoolean("token_user"));
        setLocationRelativeTo(owner);
        if (action != null) {
            editDirectly(action, parameter);
        }
        stuffBasedOnPanel();
        selection.requestFocusInWindow();
        
        setVisible(true);
    }
    
    private void stuffBasedOnPanel() {
        if (currentlyShown.equals(PANEL_HOTKEYS)) {
            owner.hotkeyManager.setEnabled(false);
        }
    }
    
    private void editDirectly(final String action, final String parameter) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (action.equals("editUsercolorItem")) {
                    showPanel(PANEL_USERCOLORS);
                    usercolorSettings.editItem(parameter);
                } else if (action.equals("editCustomNameItem")) {
                    showPanel(PANEL_NAMES);
                    nameSettings.editCustomName(parameter);
                } else if (action.equals("addUsericonOfBadgeType")) {
                    showPanel(PANEL_USERICONS);
                    imageSettings.addUsericonOfBadgeType(parameter);
                }
            }
        });
    }

    private void showPanel(String showCard) {
        cardManager.show(cards, showCard);
        currentlyShown = showCard;
        selection.setSelectedValue(showCard, true);
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
        Color color = HtmlColors.decode(getStringSetting("backgroundColor"));
        usercolorSettings.setBackgroundColor(color);
        msgColorSettings.setBackgroundColor(color);
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
        gbc.insets = new Insets(1,18,1,5);
        gbc.anchor = anchor;
        return gbc;
    }
    
    protected void addBooleanSetting(String name, BooleanSetting setting) {
        booleanSettings.put(name, setting);
    }
    
    protected JCheckBox addSimpleBooleanSetting(String name, String description, String tooltipText) {
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
    
    protected ComboStringSetting addComboStringSetting(String name, int size, boolean editable, String[] choices) {
        ComboStringSetting result = new ComboStringSetting(choices);
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
    protected ListSelector addListSetting(String name, int width, int height, 
            boolean manualSorting, boolean alphabeticSorting) {
        ListSelector result = new ListSelector(this, manualSorting, alphabeticSorting);
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
