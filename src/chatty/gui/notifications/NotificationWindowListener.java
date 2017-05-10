
package chatty.gui.notifications;

/**
 * Called by the Notification class, as opposed to NotificationActionListener,
 * which is called by the NotificationManager.
 * 
 * @author tduva
 */
public interface NotificationWindowListener {
    void notificationRemoved(NotificationWindow source);
    void notificationAction(NotificationWindow source);
}
