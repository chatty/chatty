
package chatty.util.hotkeys;

import chatty.Chatty;
import chatty.Logging;
import chatty.gui.MainGui;
import chatty.util.StringUtil;
import chatty.util.hotkeys.Hotkey.Type;
import chatty.util.settings.Settings;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;

/**
 * Manage custom hotkeys. Can add regular, app-wide and global hotkeys and loads
 * them from the settings/saves it to the settings when changed.
 * 
 * @author tduva
 */
public class HotkeyManager {
    
    private static final Logger LOGGER = Logger.getLogger(HotkeyManager.class.getName());
    
    private static final String SETTING_NAME = "hotkeys";
    
    /**
     * What inputmap to use, so it's consistent between methods.
     */
    private static final int INPUT_MAP_KEY = JComponent.WHEN_IN_FOCUSED_WINDOW;
    
    /**
     * Prefix for adding the action to input/action maps. This is searched for
     * when removing all hotkeys.
     */
    private static final String PREFIX = "chatty.util.hotkeys.";
    
    private Settings settings;
    
    private final MainGui main;
    private final List<Hotkey> hotkeys = new ArrayList<>();
    private final Map<String, HotkeyAction> actions = new LinkedHashMap<>();
    private final Map<JRootPane, Object> popouts = new WeakHashMap<>();
    
    /**
     * Whether global hotkeys are currently to be enabled (registered).
     */
    private boolean globalHotkeysRegister = true;
    
    /**
     * Whether global hotkeys are currently enabled as per setting. They may
     * still be not registered if temporarily disabled.
     */
    private boolean globalHotkeysEnabled = false;
    private boolean enabled = true;
    
    private GlobalHotkeySetter globalHotkeys;
    private boolean attemptedToInitGlobalHotkeys;
    
    /**
     * Warning text intended to be output to the user, about an error of the
     * global hotkey feature. Should be set to null if warning was output, so
     * it's only shown once.
     */
    private String globalHotkeyErrorWarning;
    
    public HotkeyManager(MainGui main) {
        this.main = main;
                
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher(new KeyEventDispatcher() {

            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                return applicationKeyTriggered(e);
            }
        });
    }
    
    /**
     * Should be called the first time global hotkeys are registered, not on
     * start. This makes sure that they are only initialized when a global
     * hotkey is actually configured and enabled.
     */
    private void initGlobalHotkeys() {
        if (Chatty.HOTKEY && !attemptedToInitGlobalHotkeys) {
            // Try to init only once
            attemptedToInitGlobalHotkeys = true;
            try {
                globalHotkeys = new GlobalHotkeySetter(new GlobalHotkeySetter.GlobalHotkeyListener() {

                    @Override
                    public void onHotkey(Object hotkeyId) {
                        onGlobalHotkey(hotkeyId);
                    }
                });
                // If an error occured during initialization, then set to null
                // which means it's not going to be used.
                if (!globalHotkeys.isActive()) {
                    globalHotkeyErrorWarning = globalHotkeys.getError();
                    globalHotkeys = null;
                }
            } catch (NoClassDefFoundError ex) {
                LOGGER.warning("Failed to initialize hotkey setter [" + ex + "]");
                globalHotkeyErrorWarning = "Failed to initialize global hotkeys (library not found).";
                globalHotkeys = null;
            }
        }
    }
    
    public void setSettings(Settings settings) {
        this.settings = settings;
    }
    
    /**
     * Registers an action to be performed by a hotkey referring to the given
     * id.
     * 
     * @param id The id to be referred to by the hotkey
     * @param label The label to be displayed to the user identifying the action
     * @param description An optional description displayed to the user
     * @param action The action itself
     */
    public void registerAction(String id, String label, String description, Action action) {
        HotkeyAction hotkeyAction = new HotkeyAction(id, label, description, action);
        actions.put(id, hotkeyAction);
    }
    
    public void registerAction(String id, String label, Action action) {
        registerAction(id, label, null, action);
    }
    
    /**
     * Register a popout window, so regular hotkeys can be added to it if
     * necessary. References are saved in a WeakHashMap.
     *
     * @param popout 
     */
    public void registerPopout(Object popout) {
        JRootPane pane = null;
        if (popout instanceof JWindow) {
            pane = ((JWindow)popout).getRootPane();
        }
        else if (popout instanceof JFrame) {
            pane = ((JFrame)popout).getRootPane();
        }
        else if (popout instanceof JDialog) {
            pane = ((JDialog)popout).getRootPane();
        }
        if (pane != null) {
            popouts.put(pane, null);
            addHotkeys(pane);
        }
    }
    
    /**
     * Get a Map of action ids and their labels for display in the GUI.
     * 
     * @return A Map with ids and their labels
     */
    public Map<String, String> getActionsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (HotkeyAction action : actions.values()) {
            map.put(action.id, action.label);
        }
        return map;
    }
    
    public Map<String, String> getDescriptionsMap() {
        Map<String, String> map = new HashMap<>();
        for (HotkeyAction action : actions.values()) {
            if (!StringUtil.isNullOrEmpty(action.description)) {
                map.put(action.id, action.description);
            }
        }
        return map;
    }
    
    /**
     * Enable/disable global and app hotkeys.
     * 
     * @param enabled 
     */
    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled || globalHotkeysEnabled) {
            setGlobalHotkeysRegistered(enabled);
        }
    }
    
    /**
     * Enable or disable global hotkeys.
     * 
     * @param enabled true to enable global hotkeys, false to disable
     */
    public synchronized void setGlobalHotkeysEnabled(boolean enabled) {
        globalHotkeysEnabled = enabled;
        setGlobalHotkeysRegistered(enabled);
    }
    
    /**
     * Enable or disable global hotkeys.
     * 
     * @param enabled true to enable global hotkeys, false to disable
     */
    private void setGlobalHotkeysRegistered(boolean register) {
        if (register == globalHotkeysRegister) {
            return;
        }
        globalHotkeysRegister = register;
        if (register) {
            removeGlobalHotkeys();
            addGlobalHotkeys();
        }
        else {
            removeGlobalHotkeys();
        }
    }
    
    /**
     * Replaces current hotkeys with the given ones. Hotkeys are removed and
     * readded according to the new data.
     * 
     * @param hotkeysData 
     */
    public synchronized void setData(Collection<Hotkey> hotkeysData) {
        if (!hotkeys.equals(hotkeysData)) {
            hotkeys.clear();
            hotkeys.addAll(hotkeysData);
            
            updateHotkeys();
            saveToSettings();
        }
    }

    /**
     * Returns a copy of the current hotkeys. (The list is a defensive copy and
     * can be modified, the Hotkey should be largely immutable, however the
     * shouldExecuteAction() method shouldn't be called outside the EDT.)
     * 
     * @return 
     */
    public synchronized List<Hotkey> getData() {
        return new ArrayList<>(hotkeys);
    }

    /**
     * Whether the global hotkey feature is available (version dependant and
     * whether it has loaded proplery).
     *
     * @return
     */
    public boolean globalHotkeysAvailable() {
        /**
         * Since global hotkeys are now lazily initialized, the warning in the
         * hotkey settings would have to be adjusted and this doesn't really
         * determine if global hotkeys could be available.
         */
        return true;
//        return globalHotkeys != null;
    }
    
    /**
     * Cleans up the global hotkeys.
     */
    public void cleanUp() {
        if (globalHotkeys != null) {
            globalHotkeys.cleanUp();
        }
    }
    
    /**
     * Remove and re-add all regular custom hotkeys for the given pane.
     * 
     * @param pane 
     */
    public void refreshHotkeys(JRootPane pane) {
        removeHotkeys(pane);
        addHotkeys(pane);
    }
    
    /**
     * Whether this Hotkey has an action registered.
     * 
     * @param hotkey The Hotkey to check
     * @return true if an action is registered, false otherwise
     */
    private boolean doesHotkeyHaveAction(Hotkey hotkey) {
        return actions.get(hotkey.actionId) != null;
    }
    
    private boolean isValidHotkey(Hotkey hotkey) {
        return doesHotkeyHaveAction(hotkey) && hotkey.hasValidCode();
    }
    
    /**
     * Removes and reads all hotkeys according to the current data.
     */
    private void updateHotkeys() {
        removeAllHotkeys();
        addHotkeys(null);
        addGlobalHotkeys();
        updateActions();
    }

    /**
     * Adds all regular hotkeys to the given JRootPane, or to all (main and
     * popouts) if the pane is null.
     * 
     * @param pane The JRootPane to add the hotkeys to
     */
    private void addHotkeys(JRootPane pane) {
        for (Hotkey hotkey : hotkeys) {
            if (isValidHotkey(hotkey) && hotkey.type == Type.REGULAR) {
                if (pane == null) {
                    addHotkey(hotkey, main.getRootPane());
                    for (JRootPane popoutPane : popouts.keySet()) {
                        addHotkey(hotkey, popoutPane);
                    }
                } else {
                    addHotkey(hotkey, pane);
                }
            }
        }
    }
    
    /**
     * Adds a single regular hotkey to the given JRootPane.
     * 
     * @param hotkey
     * @param pane 
     */
    private void addHotkey(Hotkey hotkey, JRootPane pane) {
        String id = String.valueOf(hotkey.hashCode());
        pane.getInputMap(INPUT_MAP_KEY).put(hotkey.keyStroke, PREFIX + id);
        pane.getActionMap().put(PREFIX + id, createAction(hotkey));
    }
    
    /**
     * Adds global hotkeys based on the current data, if global hotkeys are
     * currently enabled.
     */
    private void addGlobalHotkeys() {
        if (!globalHotkeysRegister) {
            return;
        }
        if (hasGlobalHotkey()) {
            initGlobalHotkeys();
        }
        if (globalHotkeys == null) {
            return;
        }
        for (Hotkey hotkey : hotkeys) {
            if (isValidHotkey(hotkey) && hotkey.type == Type.GLOBAL) {
                globalHotkeys.registerHotkey(hotkey, hotkey.keyStroke);
            }
        }
    }
    
    /**
     * Removes all hotkeys that have to be registered (regular and global).
     */
    private void removeAllHotkeys() {
        removeHotkeys(main.getRootPane());
        for (JRootPane popoutPane : popouts.keySet()) {
            removeHotkeys(popoutPane);
        }
        removeGlobalHotkeys();
        removeHotkeysFromActions();
    }
    
    /**
     * Removes all global hotkeys.
     */
    private void removeGlobalHotkeys() {
        if (globalHotkeys != null) {
            globalHotkeys.unregisterAllHotkeys();
        }
    }
    
    /**
     * Removes all entries from the InputMap that point to an action with the
     * prefix of this.
     * 
     * @param pane The JRootPane to remove the hotkeys from
     */
    private void removeHotkeys(JRootPane pane) {
        Set<KeyStroke> toBeRemoved = new HashSet<>();
        InputMap input = pane.getInputMap(INPUT_MAP_KEY);
        ActionMap action = pane.getActionMap();
        if (input.keys() == null) {
            return;
        }
        for (KeyStroke keyStroke : input.keys()) {
            Object key = input.get(keyStroke);
            if (key instanceof String && ((String)key).startsWith(PREFIX)) {
                toBeRemoved.add(keyStroke);
                action.remove(key);
            }
        }
        for (KeyStroke keyStroke : toBeRemoved) {
            input.remove(keyStroke);
        }
    }
    
    /**
     * Removes the hotkeys from all actions, so only those that are still set
     * are readded.
     */
    private void removeHotkeysFromActions() {
        for (HotkeyAction action : actions.values()) {
            action.action.putValue(Action.ACCELERATOR_KEY, null);
        }
    }
    
    /**
     * Turns all current hotkeys into something that can be written to the
     * settings.
     */
    private synchronized void saveToSettings() {
        if (settings != null) {
            List<List> dataToSave = new ArrayList<>();
            for (Hotkey hotkey : hotkeys) {
                dataToSave.add(hotkeyToList(hotkey));
            }
            settings.putList(SETTING_NAME, dataToSave);
        }
    }
    
    /**
     * Removes all current hotkeys and loads the data from the settings.
     * 
     * @param settings 
     */
    public synchronized void loadFromSettings(Settings settings) {
        this.settings = settings;
        List<List> loadFrom = settings.getList(SETTING_NAME);
        hotkeys.clear();
        for (List l : loadFrom) {
            Hotkey entry = listToHotkey(l);
            if (entry != null) {
                hotkeys.add(entry);
            }
        }
        updateHotkeys();
        checkGlobalHotkeyWarning();
    }
    
    /**
     * Turns a Hotkey into a List to save in the settings.
     * 
     * @param hotkey
     * @return 
     */
    private List<Object> hotkeyToList(Hotkey hotkey) {
        List<Object> l = new ArrayList<>();
        l.add(hotkey.actionId);
        l.add(hotkey.keyStroke.toString());
        l.add(hotkey.type.id);
        l.add(hotkey.custom);
        l.add(hotkey.delay);
        return l;
    }
    
    /**
     * Turns a List from the settings into a Hotkey.
     * 
     * @param list
     * @return 
     */
    private Hotkey listToHotkey(List list) {
        try {
            String actionId = (String)list.get(0);
            KeyStroke keyStroke = KeyStroke.getKeyStroke((String)list.get(1));
            if (keyStroke == null) {
                LOGGER.warning("Error loading hotkey, invalid: "+list);
                return null;
            }
            
            // Optional data with default values
            Type type = Hotkey.Type.REGULAR;
            String custom = "";
            int delay = 0;
            if (list.size() > 2) {
                type = Hotkey.Type.getTypeFromId(((Number)list.get(2)).intValue());
            }
            if (list.size() > 3) {
                custom = (String)list.get(3);
            }
            if (list.size() > 4) {
                delay = ((Number)list.get(4)).intValue();
            }
            return new Hotkey(actionId, keyStroke, type, custom, delay);
        } catch (IndexOutOfBoundsException | NullPointerException | ClassCastException ex) {
            LOGGER.warning("Error loading hotkey: "+list+" ["+ex+"]");
            return null;
        }
    }
    
    /**
     * Output warning of error when initializing global hotkey feature. Only
     * output once and only when a global hotkey is currently configured.
     */
    private void checkGlobalHotkeyWarning() {
        if (globalHotkeyErrorWarning == null) {
            return;
        }
        if (hasGlobalHotkey()) {
            LOGGER.log(Logging.USERINFO, globalHotkeyErrorWarning + " "
                    + "[You are getting this message because you have a "
                    + "global hotkey configured. If you don't use it you "
                    + "can ignore this warning.]");
            globalHotkeyErrorWarning = null;
        }
    }
    
    private boolean hasGlobalHotkey() {
        for (Hotkey hotkey : hotkeys) {
            if (isValidHotkey(hotkey) && hotkey.type == Type.GLOBAL) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Creates a bridge Action which calls the actual registered action for this
     * hotkey, adding some more information.
     * 
     * @param hotkey
     * @return 
     */
    private Action createAction(final Hotkey hotkey) {
        
        final HotkeyAction hotkeyAction = actions.get(hotkey.actionId);
        if (hotkeyAction == null) {
            return null;
        }
        return new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (enabled && hotkey.shouldExecuteAction()) {
                    hotkeyAction.action.actionPerformed(new ActionEvent(hotkeyAction, 0, hotkey.custom));
                }
            }
        };
    }
    
    private void updateActions() {
        for (Hotkey hotkey : hotkeys) {
            updateAction(hotkey);
        }
    }
    
    private void updateAction(final Hotkey hotkey) {
        final HotkeyAction hotkeyAction = actions.get(hotkey.actionId);
        if (hotkeyAction == null) {
            return;
        }
        if (hotkey.hasValidCode()) {
            hotkeyAction.action.putValue(Action.ACCELERATOR_KEY, hotkey.keyStroke);
        }
    }

    /**
     * Called when a global hotkey is executed.
     * 
     * @param hotkeyId 
     */
    private void onGlobalHotkey(Object hotkeyId) {
        Hotkey hotkey = (Hotkey)hotkeyId;
        HotkeyAction action = actions.get(hotkey.actionId);
        if (enabled && action != null && hotkey.shouldExecuteAction()) {
            action.action.actionPerformed(new ActionEvent(action, 0, hotkey.custom));
        }
    }
    
    /**
     * Called when a key event is triggered anywhere in the application. This
     * doesn't have to be a Hotkey defined here, so check if it is first.
     * 
     * @param e
     * @return 
     */
    private boolean applicationKeyTriggered(KeyEvent e) {
        if (!enabled) {
            return false;
        }
        KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
        for (Hotkey hotkey : hotkeys) {
            if (hotkey.type == Hotkey.Type.APPLICATION && hotkey.keyStroke.equals(keyStroke) && hotkey.hasValidCode()) {
                HotkeyAction action = actions.get(hotkey.actionId);
                if (action != null && hotkey.shouldExecuteAction()) {
                    action.action.actionPerformed(new ActionEvent(action, 0, hotkey.custom));
                    return true;
                }
            }
        }
        return false;
    }
    

}
