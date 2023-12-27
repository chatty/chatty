package chatty.util.history;

import chatty.util.irc.MsgTags;

/**
 * History Message which is needed for internal Data holding in the History Manager
  * @author m00hlti
 */

public class HistoryMessage {
    public String userName;
    public String message;
    public boolean action;
    public MsgTags tags;
    public long timeStamp;
    public String userColor;

    /**
     * Create a new History Message
     */
    public HistoryMessage() {
        message = "";
        userName = "";
        action = false;
        timeStamp = -1;
        userColor = "";
    }
}
