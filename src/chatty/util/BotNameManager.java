
package chatty.util;

import chatty.util.settings.Settings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bot names to add a bot badge to.
 * 
 * @author tduva
 */
public class BotNameManager {
    
    private final Map<String, Set<String>> botNames = new HashMap<>();
    private final Set<BotNameListener> listeners = new HashSet<>();
    
    public BotNameManager(Settings settings) {
        if (settings != null) {
            for (Object name : settings.getList("botNames")) {
                addBotName(null, (String)name);
            }
        }
    }
    
    /**
     * Adds the given bot names.
     * 
     * @param channel The channel the bot names should be valid in, can be null
     * for all channels
     * @param botNames The bot names
     */
    public void addBotNames(String channel, Set<String> botNames) {
        for (String name : botNames) {
            addBotName(channel, name);
        }
    }
    
    /**
     * Adds a bot name for the given channel.
     * 
     * @param channel The channel the bot names should be valid in, can be null
     * for all channels
     * @param botName The bot name
     */
    public final void addBotName(String channel, String botName) {
        if (botName == null || botName.isEmpty()) {
            return;
        }
        botName = StringUtil.toLowerCase(botName);
        synchronized(botNames) {
            if (!botNames.containsKey(channel)) {
                botNames.put(channel, new HashSet<String>());
            }
            botNames.get(channel).add(StringUtil.toLowerCase(botName));
        }
        informListeners(channel, botName);
    }
    
    /**
     * Checks if the given name is a bot name in the given channel or globally.
     * 
     * @param channel The channel to check in, can be null (although it checks
     * globally anyway)
     * @param botName The name to check
     * @return true if this is a bot name, false otherwise
     */
    public boolean isBotName(String channel, String botName) {
        synchronized(botNames) {
            return (botNames.containsKey(channel) && botNames.get(channel).contains(botName))
                    || (botNames.containsKey(null) && botNames.get(null).contains(botName));
        }
    }
    
    /**
     * Add a listener to be informed about added bot names.
     * 
     * @param listener The listener, null values will be ignored
     */
    public void addListener(BotNameListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Inform all listeners of the given added channel/name.
     * 
     * @param channel The channel the name is valid in
     * @param botName The bot name
     */
    private void informListeners(String channel, String botName) {
        for (BotNameListener listener : listeners) {
            listener.botNameAdded(channel, botName);
        }
    }
    
    public interface BotNameListener {
        
        /**
         * Informs listeners that a bot name has been added.
         * 
         * @param channel The channel the botname is valid for, can be null
         * which means it's valid for all channels
         * @param botName Can not be null or empty
         */
        public void botNameAdded(String channel, String botName);
    }
    
}
