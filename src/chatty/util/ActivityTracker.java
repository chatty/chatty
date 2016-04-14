
package chatty.util;

import java.awt.AWTEvent;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Timer;

/**
 * Tracks whether there is user activity. Currently only tracks the moues
 * location and allows for manual triggering of activity from the rest of the
 * program.
 * 
 * This runs in the EDT.
 * 
 * @author tduva
 */
public class ActivityTracker {
    
    private static final Logger LOGGER = Logger.getLogger(ActivityTracker.class.getName());
    
    private static final int DELAY = 5000;
    
    private static final List<ActivityListener> listeners = new ArrayList<>();
    
    private static Point lastLocation;
    private static long lastMoved = 0;
    private static long lastActivity = 0;
    private static Timer timer;
    
    /**
     * Start tracking activity (globally currently only mouse activity, inside
     * of the program keypresses and mouse actions).
     */
    public static void startTracking() {
        if (timer == null) {
            checkMouseLocation();
            timer = new Timer(DELAY, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    checkMouseLocation();
                }
            });
            timer.start();
            Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

                @Override
                public void eventDispatched(AWTEvent event) {
                    triggerActivity();
                }
            }, AWTEvent.KEY_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK);
            LOGGER.info("Started tracking user activity..");
        }
    }
    
    /**
     * Manually trigger activity.
     */
    public static void triggerActivity() {
        lastActivity = System.currentTimeMillis();
        informListeners();
    }
    
    /**
     * Check if mouse location has changed from previous check.
     */
    private static void checkMouseLocation() {
        PointerInfo info = MouseInfo.getPointerInfo();
        // This can sometimes be null, so check for it (e.g. when Windows' UAC
        // screen is active)
        if (info == null) {
            return;
        }
        Point currentLocation = info.getLocation();
        
        if (lastLocation != null && !lastLocation.equals(currentLocation)) {
            lastMoved = System.currentTimeMillis();
            triggerActivity();
        }
        lastLocation = currentLocation;
    }

    /**
     * Get when any activity was last detected.
     * 
     * @return 
     */
    public static long getLastActivityTime() {
        return lastActivity;
    }
    
    /**
     * Get how long ago any activity was last detected.
     * 
     * @return 
     */
    public static long getLastActivityAgo() {
        return System.currentTimeMillis() - lastActivity;
    }
    
    /**
     * Get when the mouse was last detected moving.
     * 
     * @return 
     */
    public static long getLastMouseMovedTime() {
        startTracking();
        return lastMoved;
    }

    /**
     * Get how long ago the mouse was last detected moving.
     * 
     * @return 
     */
    public static long getLastMouseMovedAgo() {
        startTracking();
        return System.currentTimeMillis() - lastMoved;
    }
    
    /**
     * Adds a listener that will be informed one time about user activity, then
     * removed.
     * 
     * @param listener 
     */
    public static void addActivityListener(ActivityListener listener) {
        if (listener != null) {
            startTracking();
            listeners.add(listener);
        }
    }
    
    /**
     * Remove listener, if present.
     * 
     * @param listener 
     */
    public static void removeActivityListener(ActivityListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Inform all current listeners, then remove them.
     */
    private static void informListeners() {
        Iterator<ActivityListener> it = listeners.iterator();
        while (it.hasNext()) {
            it.next().activity();
            it.remove();
        }
    }
    
}