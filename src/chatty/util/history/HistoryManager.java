
package chatty.util.history;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import chatty.Room;
import chatty.User;
import chatty.gui.components.settings.ChannelFormatter;
import chatty.util.SpecialMap;
import chatty.util.UrlRequest;
import chatty.util.api.Requests;

import chatty.util.irc.MsgTags;
import chatty.util.irc.ParsedMsg;
import chatty.util.settings.Settings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;

/**
 * History Manager which should be the entry point for getting historic Chat messages from external services.
 * Currently, only robotty https://recent-messages.robotty.de is implemented
 * @author m00hlti
 */
public class HistoryManager {
    
    private static final Logger LOGGER = Logger.getLogger(HistoryManager.class.getName());

    private final Settings settings;
    private static final ChannelFormatter channelFormater = new ChannelFormatter();

    private final static String STRHISTORYURL = "https://recent-messages.robotty.de/api/v2/recent-messages/";

    private final Object LOCK = new Object();
    
    private final Map<String, Long> latestMessageSeen = new HashMap<>();
    
    private final Set<String> requestPendingChannels = new HashSet<>();
    private final SpecialMap<String, List<QueuedMessage>> queuedMessages = new SpecialMap<>(new HashMap<>(), () -> new ArrayList<>());
    
    /**
     * Default Constructor
     * 
     * @param settings
     */
    public HistoryManager(Settings settings) {
        this.settings = settings;
    }
    
    public void setMessageSeen(String stream) {
        synchronized (LOCK) {
            latestMessageSeen.put(stream, System.currentTimeMillis());
        }
    }
    
    public void channelClosed(String stream) {
        synchronized (LOCK) {
            latestMessageSeen.remove(stream);
            queuedMessages.remove(stream);
        }
    }

    /**
     * Checks if a channel is on the exclusion list.
     * 
     * @param channel Channel which should be checked
     * @return false if not excluded, true if
     */
    public boolean isChannelExcluded(String channel) {
        return settings.listContains("historyServiceExcluded", channelFormater.format(channel));
    }

    /**
     * Check whether the chat history feature is enabled and configured
     * correctly.
     *
     * @return true if enabled and configured correctly, false otherwise
     */
    public boolean isEnabled() {
        return settings.getBoolean("historyServiceEnabled");
    }

    /**
     * Detects input from the API with regex and transforms it into a History
     * Message Object
     *
     * @param rawMessage Input from the external API
     * @return A HistoryMessage Object containing all information from the historic message
     */
    private HistoryMessage transformStringToMessage(String rawMessage) {
        ParsedMsg parsed = ParsedMsg.parse(rawMessage);
        if (parsed == null) {
            return null;
        }
        
        if (parsed.getCommand().equals("PRIVMSG")) {
            if (parsed.getParameters().has(1)
                    && parsed.getParameters().get(0).startsWith("#")) {
                String message = parsed.getParameters().get(1);
                
                HistoryMessage result = new HistoryMessage();
                result.action = message.charAt(0) == (char) 1 && message.startsWith("ACTION", 1);
                result.message = result.action ? message.substring(7).trim() : message;
                result.tags = MsgTags.merge(
                        parsed.getTags(),
                        MsgTags.create(
                                "historic-timestamp",
                                parsed.getTags().get("rm-received-ts")
                        )
                );
                result.userName = parsed.getNick();
                if (!result.userName.isEmpty()) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Executes the actual HTTP request for historical Data
     *
     * @param stream Channel to start the request for
     * @return A JSONObject with all messages requested accordingly to the parameters
     */
    private void executeRequest(String stream, Consumer<List<HistoryMessage>> listener) {
        String url = STRHISTORYURL + stream;

        long limit = settings.getLong("historyServiceLimit");
        if (limit <= 0) {
            limit = 30;
        }

        // -24h until now.
        long timestampBefore = System.currentTimeMillis();
        long timestampAfter = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        synchronized (LOCK) {
            if (latestMessageSeen.containsKey(stream)) {
                timestampAfter = latestMessageSeen.get(stream);
            }
        }

        url = Requests.makeUrl(url,
                               "limit", String.valueOf(limit),
                               "before", String.valueOf(timestampBefore),
                               "after", String.valueOf(timestampAfter));

        UrlRequest request = new UrlRequest(url);
        request.setLabel("ChatHistory/");
        request.setTimeouts(5000, 3000);
        request.async((String resultText, int responseCode) -> {
            List<HistoryMessage> result = new ArrayList<>();
            if (responseCode != 200) {
                // Some error detection in future??
            }
            else {
                try {
                    JSONParser parser = new JSONParser();
                    JSONObject root = (JSONObject) parser.parse(resultText);
                    JSONArray jsArray = (JSONArray) root.get("messages");
                    for (int i = 0; i < jsArray.size(); i++) {
                        HistoryMessage historyMsg = this.transformStringToMessage((String) jsArray.get(i));
                        if (historyMsg != null) {
                            result.add(historyMsg);
                        }
                    }
                } catch (ParseException ex) {
                    LOGGER.warning("Error requesting chat history: " + ex);
                }
            }
            // Always return a result, even if empty
            listener.accept(result);
        });
    }
    
    /**
     * Get all the chat messages from the room in the given constraints from the settings
     * @param room
     * @param listener
     */
    public void getHistoricChatMessages(Room room, Consumer<List<HistoryMessage>> listener) {
        //?hide_moderation_messages=true/false: Omits CLEARCHAT and CLEARMSG messages from the response. Optional, defaults to false.
        //?hide_moderated_messages=true/false: Omits all messages from the response that have been deleted by a CLEARCHAT or CLEARMSG message. Optional, defaults to false.
        //?clearchat_to_notice=true/false: Converts CLEARCHAT messages into NOTICE messages with a user-presentable message.

        synchronized (LOCK) {
            requestPendingChannels.add(room.getStream());
            queuedMessages.remove(room.getStream());
        }
        
        this.executeRequest(room.getStream(), listener);
    }
    
    //-------
    // Queue
    //-------
    public List<QueuedMessage> getQueuedMessages(String stream) {
        synchronized (LOCK) {
            requestPendingChannels.remove(stream);
            List<QueuedMessage> result = queuedMessages.remove(stream);
            return result != null ? result : new ArrayList<>();
        }
    }
    
    /**
     * Add message to queue.
     * 
     * @param user
     * @param text
     * @param tags
     * @param action
     * @return true if the message was added, false if the message should be
     * output directly
     */
    public boolean addQueueMessage(User user, String text, MsgTags tags, boolean action) {
        synchronized (LOCK) {
            if (requestPendingChannels.contains(user.getStream())) {
                queuedMessages.getPut(user.getStream()).add(
                        new QueuedMessage(user, text, action, tags));
                return true;
            }
            return false;
        }
    }
    
}
