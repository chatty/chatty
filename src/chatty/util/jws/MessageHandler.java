
package chatty.util.jws;

/**
 *
 * @author tduva
 */
public interface MessageHandler {

    public void handleReceived(String text);

    public void handleSent(String text);

    public void handleConnect(JWSClient c);

    public void handleDisconnect(int code);
}
