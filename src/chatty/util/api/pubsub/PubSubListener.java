
package chatty.util.api.pubsub;

/**
 *
 * @author tduva
 */
public interface PubSubListener {
    public void messageReceived(Message message);
    public void info(String info);
}
