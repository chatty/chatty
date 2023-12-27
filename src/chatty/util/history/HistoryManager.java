package chatty.util.history;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import chatty.Room;
import chatty.gui.components.settings.ChannelFormatter;
import chatty.util.UrlRequest;
import chatty.util.UrlRequest.FullResult;

import chatty.util.irc.MsgTags;
import chatty.util.settings.Settings;


/**
 * History Manager which should be the entry point for getting historic Chat messages from external services.
 * Currently, only robotty https://recent-messages.robotty.de is implemented
 * @author m00hlti
 */
public class HistoryManager {
    private enum MessageType {
        UNDEFINED,
        PRIVATMESSAGE,
        CLEARCHAT
    }

    private final Settings settings;
    private static final ChannelFormatter channelFormater = new ChannelFormatter();

    private final static String STRHISTORYURL = "https://recent-messages.robotty.de/api/v2/recent-messages/";
    private final static String strRegexRobotty = "display-name=([^;|^\\s]*)[;|\\s]|" +  /* Display Name */
                                                  ":[\\S]+\\s+PRIVMSG\\s+#[\\S]+\\s+:?([^\\n]*)|" + /* Message */
                                                  "rm-received-ts=([^;|^\\s]*)[;|\\s]|" + /* TimeStamp */
                                                  "color=([^;|^\\s]*)[;|\\s]|"; /* Color */
    private Pattern patRegexRobotty;

    private final int defaultLimitMessages = 30;
    private long timeStampBefore = 0;
    private long timeStampAfter = 0;

    /**
     * Default Constructor
     */
    public HistoryManager(Settings settings) {
        this.settings = settings;
        this.patRegexRobotty = Pattern.compile(strRegexRobotty, Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES) ;
    }

    /**
     * Checks if a channel is on the exclusion list
     * @param channel Channel which should be checked
     * @return false if not excluded, true if
     */
    public boolean isChannelExcluded(String channel) {
        return settings.listContains("externalHistoryExclusion", channelFormater.format(channel));
    }

    /**
     * Check whether the chat history feature is enabled and configured correctly.
     *
     * @return true if enabled and configured correctly, false otherwise
     */
    public boolean isEnabled() {
        return settings.getBoolean("historyEnableService");
    }

    /**
     * Detects input from the API with regex and transforms it into a History Message Object
     *
     * @param message Input from the external API
     * @param channel A Room Object for the current channel
     * @return A HistoryMessage Object containing all information from the historic message
     */
    private HistoryMessage transformStringToMessage(String message, Room channel) {
        MessageType type = MessageType.UNDEFINED;
        HistoryMessage ret = new HistoryMessage();
        ret.action = false;
        ret.tags = MsgTags.EMPTY;

        Matcher matcher = patRegexRobotty.matcher(message);

        while(matcher.find()) {
            if (matcher.group(1) != null && !matcher.group(1).isEmpty()) {
                ret.userName = matcher.group(1);//new User(matcher.group(1), channel);
            }
            else if (matcher.group(2) != null && !matcher.group(2).isEmpty()) {
                ret.message = matcher.group(2);
                type = MessageType.PRIVATMESSAGE;
            }
            else if (matcher.group(3) != null && !matcher.group(3).isEmpty()) {
                ret.tags = MsgTags.addTag(ret.tags, "historic-timestamp", matcher.group(3));
                ret.timeStamp = Long.parseLong(matcher.group(3));
            }
            else if (matcher.group(4) != null && !matcher.group(4).isEmpty()) {
                ret.userColor = matcher.group(4);
            }
        }

        if (type == MessageType.PRIVATMESSAGE && !ret.userName.isEmpty()) {
            return ret;
        }

        return null;
    }

    /**
     * Executes the actual HTTP request for historical Data
     *
     * @param channel Channel to start the request for
     * @return A JSONObject with all messages requested accordingly to the parameters
     */
    private JSONObject executeRequest(String channel) {
        JSONObject root = null;

        try {
            String url = STRHISTORYURL + channel;
            if (settings.getBoolean("historyEnableRowLimit")) {
                url += "?limit=" + settings.getLong("historyCountMessages");
            } else {
                url += "?limit=" + defaultLimitMessages;
            }
            url += "&before=" + timeStampBefore;
            url += "&after=" + timeStampAfter;

            UrlRequest request = new UrlRequest(url);
            request.setLabel("ChatHistory/");
            FullResult result = request.sync();

            if (result.getResponseCode() != 200) {
                // Some error detection in future??
            } else {
                String res = result.getResult();
                JSONParser parser = new JSONParser();
                root = (JSONObject) parser.parse(res);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return root;
    }

    /**
     * Get all the chat messages from the room in the given constraints from the settings
     * @param room
     * @return a List of HistoryMessages
     */
    public List<HistoryMessage> getHistoricChatMessages(Room room) {
        ArrayList<HistoryMessage> ret = new ArrayList<HistoryMessage>();

        String channelName = room.getChannel().replace("#", "");
        //?hide_moderation_messages=true/false: Omits CLEARCHAT and CLEARMSG messages from the response. Optional, defaults to false.
        //?hide_moderated_messages=true/false: Omits all messages from the response that have been deleted by a CLEARCHAT or CLEARMSG message. Optional, defaults to false.
        //?clearchat_to_notice=true/false: Converts CLEARCHAT messages into NOTICE messages with a user-presentable message.

        // General settings, 30 messages, from -24h until now.
        this.timeStampBefore = System.currentTimeMillis();
        this.timeStampAfter  = System.currentTimeMillis() - 24*60*60*1000;

        JSONObject historyObject = this.executeRequest(channelName);
        if (historyObject == null) {
            return ret;
        }

        JSONArray jsArray = (JSONArray) historyObject.get("messages");
        for (int i = 0; i< jsArray.size(); i++) {
            HistoryMessage msgHistory = this.transformStringToMessage((String)jsArray.get(i), room);
            if (msgHistory != null) {
                ret.add(msgHistory);
            }
        }
        return ret;
    }
}
