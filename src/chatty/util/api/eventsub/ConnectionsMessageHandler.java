
package chatty.util.api.eventsub;

import chatty.util.jws.JWSClient;

/**
 *
 * @author tduva
 */
interface ConnectionsMessageHandler {
    
    public void handleReceived(int connection, String text, Message message);

    public void handleSent(int connection, String text);

    public void handleConnect(int connection, JWSClient c);

    public void handleDisconnect(int connection);
    
    public void handleRegisterError(int responseCode);
    
}
