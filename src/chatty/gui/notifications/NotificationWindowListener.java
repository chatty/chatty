
package chatty.gui.notifications;

/**
 * Called by the Notification class, as opposed to NotificationActionListener,
 * which is called by the NotificationManager.
 * 
 * @author tduva
 */
public interface NotificationWindowListener {
    
    /**
     * A notification window has been closed and been disposed off. Any data
     * related to it can be removed if necessary.
     * 
     * @param source The NotificationWindow that has been removed
     */
    void notificationRemoved(NotificationWindow source);
    
    /**
     * The notification window has been right-clicked, which might trigger an
     * appropriate action. This also means that the notification has been
     * closed, so notificationRemoved() will be called shortly after this.
     * 
     * @param source The NotificationWindow that has been right-clicked
     */
    void notificationAction(NotificationWindow source);
}
