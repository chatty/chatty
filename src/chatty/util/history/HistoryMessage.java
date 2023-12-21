package chatty.util.history;

import chatty.User;
import chatty.util.irc.MsgTags;

/**
 * History Message which is needed for internal Data holding in the History Manager
  * @author m00hlti
 */

public class HistoryMessage {
    public User User;
    public String Message;
    public Boolean Action;
    public MsgTags Tags;
    public long Timestamp;
    public String UserColor;

    /**
     * Create a new History Message
     */
    public HistoryMessage(){
        Message = "";
        Action = false;
        Timestamp = -1;
        UserColor = "";
    }

    /**
     * Updates the user object with the setted color if possible
     */
    public void updateUserColor(){
        if(User != null && !UserColor.isEmpty()){
            User.setColor(UserColor);
        }
    }
}
