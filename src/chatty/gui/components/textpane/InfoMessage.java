
package chatty.gui.components.textpane;

import chatty.gui.Highlighter.Match;
import chatty.util.Pair;
import chatty.util.irc.MsgTags;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tduva
 */
public class InfoMessage {
    
    private static final Map<String, String> COMMANDS = new HashMap<>();
    
    static {
        COMMANDS.put("subs_on", "subscribers");
        COMMANDS.put("subs_off", "subscribersoff");
        COMMANDS.put("followers_on", "followers");
        COMMANDS.put("followers_off", "followersoff");
        COMMANDS.put("emote_only_on", "emoteonly");
        COMMANDS.put("emote_only_off", "emoteonlyoff");
        COMMANDS.put("host_on", "host");
        COMMANDS.put("host_off", "unhost");
        COMMANDS.put("r9k_on", "r9kbeta");
        COMMANDS.put("r9k_off", "r9kbetaoff");
        COMMANDS.put("slow_on", "slow");
        COMMANDS.put("slow_off", "slowoff");
    }
    
    public static boolean msgIdHasCommand(String msgId) {
        return COMMANDS.containsValue(msgId);
    }
    
    public enum Type {
        INFO, SYSTEM
    }
    
    public final long createdTime = System.currentTimeMillis();
    
    public final Type msgType;
    public final String text;
    public final MsgTags tags;
    
    public boolean highlighted;
    private boolean hidden;
    public Color color;
    public Color bgColor;
    public List<Match> highlightMatches;
    public Object colorSource;
    public Object highlightSource;
    
    public InfoMessage(Type msgType, String text) {
        this(msgType, text, MsgTags.EMPTY);
    }
    
    public InfoMessage(Type msgType, String text, MsgTags tags) {
        this.msgType = msgType;
        this.text = text;
        this.tags = tags;
    }
    
    public static InfoMessage createInfo(String text) {
        return new InfoMessage(Type.INFO, text);
    }
    
    public static InfoMessage createInfo(String text, MsgTags tags) {
        return new InfoMessage(Type.INFO, text, tags);
    }
    
    public static InfoMessage createSystem(String text) {
        return new InfoMessage(Type.SYSTEM, text);
    }
    
    public boolean isSystemMsg() {
        return msgType == Type.SYSTEM;
    }
    
    public boolean isHidden() {
        return this.hidden;
    }
    
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    public String makeCommand() {
        if (tags != null) {
            return COMMANDS.get(tags.get("msg-id"));
        }
        return null;
    }
    
    public long age() {
        return System.currentTimeMillis() - createdTime;
    }
    
    public Pair<String, String> getLink() {
        if (tags != null) {
            if (tags.getHosted() != null) {
                return new Pair("Join", "join."+tags.getHosted());
            }
        }
        return null;
    }
    
}
