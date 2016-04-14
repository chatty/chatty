
package chatty.gui.notifications;

/**
 * When a notification causes an action, e.g. when it is clicked. This is used
 * with the NotificationManager.
 * 
 * @author tduva
 * @param <T> Type for data that can be included with the notification to work
 *  with when the action is invoked.
 */
public interface NotificationActionListener<T> {
    public void notificationAction(T data);
}
