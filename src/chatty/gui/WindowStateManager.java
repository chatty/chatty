
package chatty.gui;

import chatty.Helper;
import chatty.Helper.IntegerPair;
import chatty.util.settings.Settings;
import java.awt.Dimension;
import java.awt.Frame;
import static java.awt.Frame.MAXIMIZED_BOTH;
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
     * The primary window is special, because it can be set to be restored even
     * if the others aren't ({@code RESTORE_MAIN}).
     * 
     * Other windows are centered on it as their default position, as well as
     * attached to it (if enabled).
     */
    private final Window primaryWindow;
    
    private Point primaryMovedBy;
    
    private final AttachedWindowManager attachedWindowManager;

    public WindowStateManager(Window primaryWindow, Settings settings) {
        this.settings = settings;
        this.primaryWindow = primaryWindow;
        attachedWindowManager = new AttachedWindowManager(primaryWindow);
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
    
    public void setWindowAttached(Window window, boolean attached) {
        if (attached) {
            attachedWindowManager.attach(window);
        } else {
            attachedWindowManager.detach(window);
        }
    }
    
    public Set<Window> getWindows() {
        return new HashSet<>(windows.keySet());
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
        // Primary first, so dialogs can be moved the same if off-screen
        loadWindowState(primaryWindow);
        for (Window window : windows.keySet()) {
            if (window != primaryWindow) {
                loadWindowState(window);
            }
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
                    if (window == primaryWindow) {
                        attachedWindowManager.ignoreLocationOnce(location);
                    }
                    window.setLocation(location);
                    locationSet.add(window);
                } else {
                    // Failed to restore normally due to not being on screen
                    if (window == primaryWindow) {
                        /**
                         * Store how far the primary window has moved from it's
                         * intended location to the new default location.
                         */
                        if (!(window instanceof Frame) || (((Frame) window).getExtendedState() & MAXIMIZED_BOTH) != MAXIMIZED_BOTH) {
                            /**
                             * Center as default location, but only if not
                             * maximized. If maximized, the location should
                             * already have been set automatically.
                             */
                            window.setLocationRelativeTo(null);
                            attachedWindowManager.ignoreLocationOnce(window.getLocation());
                        }
                        locationSet.add(window);
                        primaryMovedBy = new Point(
                                window.getLocation().x - location.x,
                                window.getLocation().y - location.y);
                        LOGGER.info("Location for "+item.id+" ["+location+"] moved (not on screen)");
                    } else if (primaryMovedBy != null) {
                        /**
                         * If there is information on how far the primary window
                         * has moved, use that to hopefully get to an on-screen
                         * location for this.
                         */
                        Point newLocation = new Point(
                                location.x+primaryMovedBy.x,
                                location.y+primaryMovedBy.y);
                        if (isOnScreen(newLocation, window.getWidth())) {
                            window.setLocation(newLocation);
                            locationSet.add(window);
                            LOGGER.info("Location for "+item.id+" ["+location+"] moved same as primary (not on screen)");
                        } else {
                            LOGGER.info("Location for "+item.id+" ["+location+"] not restored (moved location not on screen)");
                        }
                    } else {
                        LOGGER.info("Location for "+item.id+" ["+location+"] not restored (not on screen)");
                    }
                }
            }
        }
        
        item.wasOpen = states[2].equals("1");
    }
    
    private boolean isOnScreen(Point location, int width) {
        return GuiUtil.isPointOnScreen(location, 100, 8)
            || GuiUtil.isPointOnScreen(location, width - 100, 8);
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
        if (window == primaryWindow) {
            if (!setLocationBefore) {
                window.setLocationRelativeTo(null);
            }
        } else {
            if (mode() < RESTORE_ALL || !setLocationBefore) {
                if (parent == null) {
                    parent = primaryWindow;
                }
                window.setLocationRelativeTo(parent);
                locationSet.add(window);
            }
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
        StateItem item = windows.get(window);
        return mode() >= REOPEN_ON_START
                && item != null && item.reopen && item.wasOpen;
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
