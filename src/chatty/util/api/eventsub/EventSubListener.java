
package chatty.util.api.eventsub;

/**
 *
 * @author tduva
 */
public interface EventSubListener {
    public void messageReceived(Message message);
    public void info(String info);
}
