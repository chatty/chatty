
package chatty;

import java.util.logging.Logger;

/**
 * This is used to perform some actions once the program shuts down, although
 * they should already be perfomed before, if the program was exited regulary.
 * This is just as a backup in case the program was closed by the OS or
 * something.
 * 
 * @author tduva
 * @see chatty.TwitchClient#exit()
 */
public class Shutdown implements Runnable {
    
    private static final Logger LOGGER = Logger.getLogger(Shutdown.class.getName());

    private final TwitchClient client;
    
    public Shutdown(TwitchClient client) {
        this.client = client;
    }
    
    @Override
    public void run() {
        LOGGER.info("Shutdown");
        System.out.println("Shutdown");
        
        // Save/close stuff that should already be closed by a regular shutdown
        // so only use methods that are only executed once and synchronized.
        client.addressbook.saveToFileOnce();
        if (!client.settings.getBoolean("dontSaveSettings")) {
            client.settings.saveSettingsToJson();
        }
        client.chatLog.close();
        if (client.capitalizedNames != null) {
            client.capitalizedNames.saveToFileOnce();
        }
    }
}
