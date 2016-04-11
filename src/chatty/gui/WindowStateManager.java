
package chatty.gui;

import chatty.Helper;
import chatty.Helper.IntegerPair;
import chatty.util.settings.Settings;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Allows you to add windows that you can then save the position and size for
 * (usually at the end of the program session), which can be reloaded later,
 * based on the restore settings. Also attaches the added windows to the
 * default parent, moving them along if the parent is moved (if enabled).
 * 
 * The window attributes are saved to the settings.
 * 
 * The position/size of the window is restored just once at the start (even if
 * it isn't shown yet) and then just not changed if it's set to restore. If it's
 * set to not restore, then the position is set everytime it's opened.
 * 
 * @author tduva
 */
public class WindowStateManager {
    
    private static final Logger LOGGER = Logger.getLogger(WindowStateManager.class.getName());
    
    private static final String SETTING = "windows";
    private static final String MODE_SETTING = "restoreMode";
    private static final String CHECK_ON_SCREEN_SETTING = "restoreOnlyIfOnScreen";
    
    public static final int DONT_RESTORE = 0;
    public static final int RESTORE_MAIN = 1;
    public static final int RESTORE_ALL = 2;
    public static final int RESTORE_ON_START = 3;
    public static final int REOPEN_ON_START = 4;
    
    private final HashMap<Window, StateItem> windows = new HashMap<>();
    
    /**
     * Saves which windows have had their position set already in this session.
     */
    private final Set<Window> locationSet = new HashSet<>();
    private final Settings settings;
    
    /**
     * The default parent is the one that the windows are centered on, if they
     * are opened and their position hasn't been set yet or their position
     * should not be restored.
     */
    private final Window defaultParent;
    
    /**
     * The primary window is special, because it can be set to be restored even
     * if the others aren't ({@code RESTORE_MAIN}).
     */
    private Window primaryWindow;
    
    private final AttachedWindowManager attachedWindowManager;

    public WindowStateManager(Window defaultParent, Settings settings) {
        this.settings = settings;
        this.defaultParent = defaultParent;
        attachedWindowManager = new AttachedWindowManager(defaultParent);
    }
    
    /**
     * Adds a window to be managed.
     * 
     * @param window The {@code Window} object
     * @param id The id of the window, which is used when saving
     * @param saveSize Whether to save (or rather restore) the size
     * @param reopen Whether to reopen the window on start
     */
    public void addWindow(Window window, String id, boolean saveSize, boolean reopen) {
        windows.put(window, new StateItem(id, saveSize, reopen));
        attachedWindowManager.attach(window);
    }
    
    /**
     * Sets the primary window, which can be restored even if the other windows
     * are set to not restore.
     * 
     * @param window 
     */
    public void setPrimaryWindow(Window window) {
        this.primaryWindow = window;
    }
    
    public void setAttachedWindowsEnabled(boolean enabled) {
        attachedWindowManager.setEnabled(enabled);
    }
    
    /**
     * Saves the attributes for all managed windows.
     */
    public void saveWindowStates() {
        for (Window window : windows.keySet()) {
            saveWindowState(window);
        }
    }
    
    /**
     * Loads the window attributes for all managed windows.
     */
    public void loadWindowStates() {
        for (Window window : windows.keySet()) {
            loadWindowState(window);
        }
    }
    
    /**
     * Save the window attributes for a single window.
     * 
     * @param window The window to save the attributes for
     */
    private void saveWindowState(Window window) {
        // Only save state when window state was actually set during this
        // this session at least once
        if (!locationSet.contains(window) && window != primaryWindow) {
            return;
        }
        // Should still save whether it was open
        StateItem item = windows.get(window);
        Point location = window.getLocation();
        String state = location.x+","+location.y;
        Dimension size = window.getSize();
        state += ";"+size.width+","+size.height;
        state += ";"+(window.isVisible() ? "1" : "0");
        settings.mapPut(SETTING, item.id, state);
    }
    
    /**
     * Try to load the window attributes for the given {@literal Window} and sets
     * the location and size for the window (if it's set to restore).
     * 
     * The attributes are loaded from a map in the settings, with the window id
     * as the key. The item has to be a {@literal String} in the format:
     * x,y;width,height;wasOpen
     * 
     * @param window 
     */
    private void loadWindowState(Window window) {
        StateItem item = windows.get(window);
        String state = (String)settings.mapGet(SETTING, item.id);
        if (state == null) {
            return;
        }
        
        String[] states = state.split(";");
        if (states.length < 3) {
            return;
        }
        
        if (mode() >= RESTORE_ON_START
                || (mode() >= RESTORE_MAIN && window == primaryWindow)) {
            // Set position and size
            
            // Set size first, so width is set already for checking position
            IntegerPair sizeTemp = Helper.getNumbersFromString(states[1]);
            if (sizeTemp != null && item.saveSize) {
                Dimension size = new Dimension(sizeTemp.a, sizeTemp.b);
                window.setSize(size);
            }
            
            // Set position
            IntegerPair locationTemp = Helper.getNumbersFromString(states[0]);
            if (locationTemp != null) {
                Point location = new Point(locationTemp.a, locationTemp.b);
                if (!settings.getBoolean(CHECK_ON_SCREEN_SETTING)
                        || isOnScreen(location, window.getWidth())) {
                    // If this is the window the others are attached to, ignore
                    // this movement.
                    if (window == defaultParent) {
                        attachedWindowManager.ignoreLocationOnce(location);
                    }
                    window.setLocation(location);
                    locationSet.add(window);
                } else {
                    LOGGER.info("Location for "+item.id+" ["+location+"] not restored (not on screen)");
                }
            }
        }
        
        item.wasOpen = states[2].equals("1");
    }
    
    private boolean isOnScreen(Point location, int width) {
        return GuiUtil.isPointOnScreen(location, 100)
            || GuiUtil.isPointOnScreen(location, width - 100);
    }
    
    /**
     * Sets the location to the default one (centered on the default parent),
     * unless the mode is set to restore and the position was set before, in
     * which case nothing is done, so the window just keeps the location it
     * already has.
     * 
     * @param window The window to set the position for
     * @param parent The window to center the window on, can be null in which
     * case it centers on the default parent
     */
    public void setWindowPosition(Window window, Window parent) {
        boolean setLocationBefore = locationSet.contains(window);
        if (mode() < RESTORE_ALL || !setLocationBefore) {
            if (parent == null) {
                parent = defaultParent;
            }
            window.setLocationRelativeTo(parent);
            locationSet.add(window);
        }
    }
    
    public void setWindowPosition(Window window) {
        setWindowPosition(window, null);
    }
    
    /**
     * Returns whether the window was open in the last session.
     * 
     * @param window The window to check.
     * @return {@code true} if the window was open last session, {@code false}
     * if it wasn't or there is no data saved
     */
    public boolean wasOpen(Window window) {
        StateItem item = windows.get(window);
        return item != null && item.wasOpen;
    }
    
    /**
     * Returns whether the window should be reopened on start.
     * 
     * @param window
     * @return {@code true} if it should be reopened, {@code false} otherwise
     */
    public boolean shouldReopen(Window window) {
        return mode() >= REOPEN_ON_START && wasOpen(window);
    }
    
    /**
     * Gets the current restore mode setting.
     * 
     * @return The mode as an integer
     */
    private int mode() {
        return (int)settings.getLong(MODE_SETTING);
    }
    


    /**
     * Saves some stuff for a single window.
     */
    private static class StateItem {
        
        /**
         * Whether to save (or rather restore) the size.
         */
        public final boolean saveSize;
        
        /**
         * The id of this window, which is used when saving the settings.
         */
        public final String id;
        
        /**
         * Whether this window should be reopened (if the restore setting allows
         * it).
         */
        public final boolean reopen;
        
        /**
         * Saves whether the window was open when the program was closed last
         * session.
         */
        public boolean wasOpen;
        
        StateItem(String id, boolean saveSize, boolean reopen) {
            this.saveSize = saveSize;
            this.reopen = reopen;
            this.id = id;
        }
    }
        
}
