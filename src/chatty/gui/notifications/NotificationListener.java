
package chatty.gui.notifications;

/**
 * Called by the Notification class, as opposed to NotificationActionListener,
 * which is called by the NotificationManager.
 * 
 * @author tduva
 */
public interface NotificationListener {
    void notificationRemoved(Notification source);
    void notificationAction(Notification source);
}
