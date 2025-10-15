
package chatty;

import chatty.gui.MainGui;
import chatty.gui.emoji.EmojiUtil;
import chatty.util.Debugging;
import chatty.util.SpecialMap;
import chatty.util.api.TwitchApi;
import chatty.util.history.QueuedMessage;
import chatty.util.irc.MsgTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sending message through the API causes a message to be received back.
 * Displaying that message would cause a small delay between the user action and
 * the message appearing. Instead, the message is output immediately as before,
 * with the received message then not being shown.
 *
 * Since the Message ID is received from the API request only after a small
 * delay, and it may not be guaranteed that it is received before the chat
 * message is received, ignoring the message requires a two step process:
 * 1. Ignoring all received local user messages as long as a request is pending
 * 2. Ignoring a specific message id once the request is completed, and
 * outputting any messages ignored in the meantime that were sent from another
 * client by the local user
 *
 * @author tduva
 */
public class SendMessageManager {

    private int sentMessageId = 0;
    private final SpecialMap<String, Set<String>> sentMessagePending = new SpecialMap<>(new HashMap<>(), () -> new HashSet<>());
    private final Set<String> ignoreByMsgId = new HashSet<>();
    
    private final TwitchApi api;
    private final MainGui g;
    
    private final Object LOCK = new Object();
    
    private final SpecialMap<String, List<QueuedMessage>> queuedMessages = new SpecialMap<>(new HashMap<>(), () -> new ArrayList<>());
    
    public SendMessageManager(TwitchApi api, MainGui g) {
        this.api = api;
        this.g = g;
    }
    
    public String sendApiMessage(String channel, String text, String replyToMsgId, boolean action) {
        if (g.getSettings().getLong("emojiZWJ") == 2) {
            text = EmojiUtil.encodeZWJ(text);
        }
        if (action) {
            text = (char)1+"ACTION "+text+(char)1;
        }
        String tempMsgId = String.valueOf(sentMessageId);
        sentMessageId++;
        sentMessagePending.getPut(channel).add(tempMsgId);
        api.sendChatMessage(Helper.toStream(channel), text, replyToMsgId, result -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(SendMessageManager.class.getName()).log(Level.SEVERE, null, ex);
//            }
                        if (!result.wasSent) {
                            g.printLine("# Message not sent: " + result.dropReasonMessage);
                        }
                        synchronized (LOCK) {
                            sentMessagePending.getOptional(channel).remove(tempMsgId);
                            sentMessagePending.removeEmptyValues();
                            if (result.wasSent && result.msgId != null) {
                                ignoreByMsgId.add(result.msgId);
                                Debugging.println("sendmsg", "Now ignoring: %s", result.msgId);
                            }
                        }
                        handleQueuedMessages(channel);
                    });
        
        return tempMsgId;
    }
    
    /**
     * Messages that have been held due to a pending sent API request can now
     * either be sent or ignored if the msg id to ignore has now been added.
     * 
     * @param channel 
     */
    private void handleQueuedMessages(String channel) {
        synchronized (LOCK) {
            debugStatus("sendQueued");
            if (sentMessagePending.containsKey(channel)) {
                return;
            }
            if (!queuedMessages.containsKey(channel)) {
                return;
            }
        }
        List<QueuedMessage> messages;
        synchronized (LOCK) {
            messages = new ArrayList<>(queuedMessages.getOptional(channel));
            queuedMessages.remove(channel);
        }
        for (QueuedMessage message : messages) {
            if (ignoreByMsgId.contains(message.tags.getId())) {
                ignoreByMsgId.remove(message.tags.getId());
            }
            else {
                g.printMessage(message.user, message.text, message.action, message.tags);
            }
        }
    }
    
    /**
     * Check if the given message should be ignored (should have already checked
     * that it is indeed a local user message). The message may be added to a
     * queue to be output later if the msg id hasn't been received yet.
     * 
     * @param user
     * @param text
     * @param tags
     * @param action
     * @return 
     */
    public boolean shouldIgnoreMessage(User user, String text, MsgTags tags, boolean action) {
        synchronized (LOCK) {
            debugStatus("shouldIgnore");
            if (ignoreByMsgId.contains(tags.getId())) {
                ignoreByMsgId.remove(tags.getId());
                Debugging.println("sendMsg", "Ignored: %s", text);
                return true;
            }
            if (sentMessagePending.containsKey(user.getChannel())) {
                Debugging.println("sendMsg", "Ignored for now (messages pending): %s", text);
                queuedMessages.getPut(user.getChannel()).add(
                        new QueuedMessage(user, text, action, tags));
                return true;
            }
            return false;
        }
    }
    
    private void debugStatus(String where) {
        Debugging.println("sendMsg", "[%s] Temp: %s Ignore: %s Queue: %s", where, sentMessagePending, ignoreByMsgId, queuedMessages);
    }
    
}
