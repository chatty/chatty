
package chatty.gui.notifications;

import chatty.gui.notifications.NotificationWindow.HideMethod;
import chatty.util.ActivityTracker;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import javax.swing.SwingUtilities;

/**
 * Create and position notifications.
 * 
 * @author tduva
 * @param <T> The type of the data associated with a notification
 */
public class NotificationWindowManager<T> {
    
    // Margin between notifications and the border of the screen
    private static final int VERTICAL_MARGIN = 3;
    private static final int HORIZONTAL_MARGIN = 4;
    
    private static final int SECOND = 1000;
    private static final int MINUTE = 60*SECOND;
    
    // List of displayed notifications.
    private final LinkedList<NotificationWindow> displayed = new LinkedList<>();
    // List of notifications queued for display.
    private final LinkedList<NotificationWindow> queue = new LinkedList<>();
    
    private final Map<NotificationWindow,T> notificationData = new HashMap<>();
    
    /**
     * Settings
     */
    private int screen;
    private int position;
    // Defined based on position
    private int verticalMoveDirection;
    private int horizontalMoveDirection;
    
    // Regular display time
    private int displayTime = 10*SECOND;
    // How long to display a notification at the most
    private int maxDisplayTime = 30*MINUTE;
    // Shorter max display time, in case the queue is full
    private int shortMaxDisplayTime = 2*SECOND;
    
    private boolean keepOpenOnHover;
    
    private int activityTime = -1;
    
    // Maximum number of items to display at once
    private int maxItems = 4;
    // Maximum number of items in the queue
    private int maxQueueSize = 4;
    
    private HideMethod hideMethod = HideMethod.FADE_OUT;

    // Used to choose a screen for the notification to tbe displayed on
    private final Component parent;
    
    private final NotificationWindowListener listener;
    private NotificationActionListener<T> actionListener;
    
    /**
     *
     * @param parent
     */
    public NotificationWindowManager(Component parent) {
        this.parent = parent;
        setPosition(0);
        setScreen(0);

        this.listener = new NotificationWindowListener() {

            @Override
            public void notificationRemoved(NotificationWindow source) {
                NotificationWindowManager.this.notificationRemoved(source);
            }
            
            @Override
            public void notificationAction(NotificationWindow source) {
                if (actionListener != null) {
                    actionListener.notificationAction(notificationData.get(source));
                }
            }
        };
    }
    
    /**
     * Create a notification with the given message and either show or queue it.
     *
     * @param title
     * @param message
     * @param foreground
     * @param background
     * @param data Associated data, which will be send to the listener when the
     * Notification is right-clicked
     */
    public void showMessage(String title, String message, Color foreground,
            Color background, T data) {
        if (queue.size() > maxQueueSize*10) {
            // If queue is too large already, simply give up, nobody is gonna
            // click/wait through that many notifications anyway
            return;
        }
        NotificationWindow n = new NotificationWindow(title, message, foreground, background, listener);
        n.setHideMethod(hideMethod);
        //n.setTimeout(displayTime);
        n.setFallbackTimeout(maxDisplayTime);
        n.setActivityTime(activityTime);
        if (displayed.size() < maxItems) {
            showNotification(n);
        } else {
            queue.add(n);
            checkQueueSize();
        }
        notificationData.put(n, data);
    }
    
    public void showMessage(String title, String message, Color foreground, Color background) {
        showMessage(title, message, foreground, background, null);
    }
    
    /**
     * Removes any queued notifications and closes displayed ones.
     */
    public void clearAll() {
        queue.clear();
        clearAllShown();
    }
    
    /**
     * Closes all displayed notifications.
     */
    public void clearAllShown() {
        // Create a copy of the list so it can iterate over all elements while
        // the original list is modified.
        for (NotificationWindow n : new LinkedList<>(displayed)) {
            n.close();
        }
    }
    
    /**
     * Set in which corner of the screen the notification should appear in.
     * 0 - Top Left (default for invalid values)
     * 1 - Top Right
     * 2 - Bottom Left
     * 3 - Bottom Right
     * 
     * Only changes the location if no notifications are currently displayed.
     * 
     * @param position 
     */
    public final void setPosition(int position) {
        if (isClear()) {
            this.position = position;
            updateVariables();
        }
    }
    
    /**
     * Set on which screen the notification should appear. Values smaller than
     * 0 mean the screen should be determined another way (from the parent
     * JFrame if set or just the default one).
     * 
     * Only changes the screen if no notifications are currently displayed.
     * 
     * @param screen 
     */
    public final void setScreen(int screen) {
        if (isClear()) {
            this.screen = screen;
        }
    }
    
    public final void setDisplayTime(int displayTime) {
        this.displayTime = displayTime * SECOND;
    }
    
    public final void setMaxDisplayTime(int maxDisplayTime) {
        this.maxDisplayTime = maxDisplayTime * SECOND;
    }
    
    public final void setShortMaxDisplayTime(int shortDisplayTime) {
        this.shortMaxDisplayTime = shortDisplayTime;
    }
    
    public final void keepOpenOnHover(boolean enabled) {
        this.keepOpenOnHover = enabled;
    }
    
    public final void setMaxQueueSize(int size) {
        this.maxQueueSize = size;
    }
    
    public final void setMaxDisplayItems(int count) {
        this.maxItems = count;
    }
    
    /**
     * Sets the time in seconds in which activity should have been detected to
     * be considered active. Automatically starts tracking if it's greater than
     * 0. Values <= 0 disable this feature (however tracking may still be
     * active).
     *
     * @param time 
     */
    public final void setActivityTime(int time) {
        this.activityTime = time * SECOND;
        if (activityTime > 0) {
            ActivityTracker.startTracking();
        }
    }
    
    public final void setHideMethod(HideMethod hideMethod) {
        this.hideMethod = hideMethod;
    }
    
    public final void setNotificationActionListener(NotificationActionListener<T> listener) {
        this.actionListener = listener;
    }
    
    /**
     * Checks if no notifications are displayed or queued.
     * 
     * @return 
     */
    private boolean isClear() {
        if (displayed.isEmpty() && queue.isEmpty()) {
            return true;
        }
        return false;
    }
    
    /**
     * Update the variables that depend on the position.
     */
    private void updateVariables() {
        if (position == 2 || position == 3) {
            verticalMoveDirection = 1;
        } else {
            verticalMoveDirection = -1;
        }
        if (position == 1 || position == 3) {
            horizontalMoveDirection = 1;
        } else {
            horizontalMoveDirection = -1;
        }
    }
    
    /**
     * A notification was removed, reposition any other ones if necessary and
     * remove it from the displayed-list.
     * 
     * @param removed The notification that was removed.
     */
    private void notificationRemoved(NotificationWindow removed) {
        int offset = 0;
        Iterator<NotificationWindow> it = displayed.iterator();
        while (it.hasNext()) {
            NotificationWindow n = it.next();
            if (removed == n) {
                offset = n.getHeight() + VERTICAL_MARGIN;
                it.remove();
            }
            else if (offset > 0) {
                // Move any notifcations after the removed one
                n.moveVertical(verticalMoveDirection * offset);
            }
        }
        // Show next notification if possible and if one is queued.
        Iterator<NotificationWindow> itQueue = queue.iterator();
        while (itQueue.hasNext() && displayed.size() < maxItems) {
            NotificationWindow next = itQueue.next();
            itQueue.remove();
            showNotification(next);
        }
        notificationData.remove(removed);
    }

    /**
     * If the queue size exceeds the maxQueueSize and any notifications are
     * displayed, then make the oldest one disappear faster.
     */
    private void checkQueueSize() {
        if (queue.size() > maxQueueSize && displayed.size() > 0) {
            displayed.getFirst().setFallbackTimeout(shortMaxDisplayTime);
        }
    }
    
    /**
     * Actually shows a notification after it was created. Positions the
     * notification appropriately and checks if the queue is full and acts
     * appropriately.
     * 
     * @param n 
     */
    private void showNotification(NotificationWindow n) {
        
        checkQueueSize();
        GraphicsConfiguration config = getGraphicsConfig();
        Point location = calculateLocation(position, getSafeBounds(config), 
                n.getSize(), getCurrentOffset());
        n.setLocation(location);
        n.setTimeout(displayTime + (displayTime/4 * displayed.size()));
        n.setKeepOpenOnHover(keepOpenOnHover);
        if (queue.size() > maxQueueSize && displayed.size() == 0) {
            n.setFallbackTimeout(shortMaxDisplayTime);
        }
        displayed.add(n);
        n.show();
    }
    
    /**
     * Calculate the current offset based on the displayed notifications.
     * 
     * @return 
     */
    private int getCurrentOffset() {
        int offset = VERTICAL_MARGIN;
        for (NotificationWindow n : displayed) {
            offset += n.getHeight() + VERTICAL_MARGIN;
        }
        return offset;
    }
    
    /**
     * Get a GraphicsConfiguration based on the settings. Either from a defined
     * screen, from the parent or the default one.
     * 
     * @return 
     */
    private GraphicsConfiguration getGraphicsConfig() {
        GraphicsDevice[] devices = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getScreenDevices();
        if (screen >= 0 && devices.length - 1 >= screen) {
            return devices[screen].getDefaultConfiguration();
        }
        
        if (parent != null) {
            GraphicsConfiguration g = parent.getGraphicsConfiguration();
            if (g != null) {
                return g;
            }
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * Calculates the location within the bounds to display something with the
     * given size, so it appears at the given position, which is in one of the
     * four corners (0: top-left, 1: top-right, 2: bottom-left, 3: bottom-right).
     * 
     * @param position
     * @param bounds
     * @param size
     * @return 
     */
    private Point calculateLocation(int position, Rectangle bounds, Dimension size, int offset) {
        Point location = new Point();
        location.x = bounds.x - horizontalMoveDirection*HORIZONTAL_MARGIN;
        location.y = bounds.y;
        if (position == 1 || position == 3) {
            location.x += (bounds.width - size.width);
        }
        if (position == 2 || position == 3) {
            location.y += (bounds.height - size.height);
            offset = -offset;
        }
        location.y += offset;
        return location;
    }
    
    /**
     * Calculates the safe bounds (without taskbar) of the given
     * GraphicsConfiguration.
     *
     * @param config
     * @return
     */
    private static Rectangle getSafeBounds(GraphicsConfiguration config) {
        Rectangle bounds = new Rectangle(config.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= (insets.left + insets.right);
        bounds.height -= (insets.top + insets.bottom);
        return bounds;
    }
    
    public static final void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                NotificationWindowManager m = new NotificationWindowManager(null);
                m.setPosition(0);
                m.setScreen(1);
                m.showMessage("Test", "Test message with some text.", Color.WHITE, Color.BLACK);
                
            }
        });
    }
}
