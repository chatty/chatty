
package chatty.util.api;

import chatty.gui.MainGui;
import chatty.util.StringUtil;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tduva
 */
public class AutoModCommandHelper {
    
    private final MainGui gui;
    private final Map<String, String> pendingApprove = new HashMap<>();
    private final Map<String, String> pendingDeny = new HashMap<>();
    private final TwitchApi api;
    
    public AutoModCommandHelper(MainGui gui, TwitchApi api) {
        this.gui = gui;
        this.api = api;
    }
    
    public void approve(String channel, String msgId) {
        if (StringUtil.isNullOrEmpty(msgId)) {
            gui.printSystem("Invalid message id.");
            return;
        }
        pendingApprove.put(msgId, channel);
        api.autoModApprove(msgId);
    }
    
    public void deny(String channel, String msgId) {
        if (StringUtil.isNullOrEmpty(msgId)) {
            gui.printSystem("Invalid message id.");
            return;
        }
        pendingDeny.put(msgId, channel);
        api.autoModDeny(msgId);
    }

    public void requestResult(String result, String msgId) {
        if (pendingApprove.containsKey(msgId)) {
            if (!result.equals("approved")) {
                gui.printSystem("[AutoMod/Approve] Error: "+makeError(result));
            }
            pendingApprove.remove(msgId);
        }
        if (pendingDeny.containsKey(msgId)) {
            if (!result.equals("denied")) {
                gui.printSystem("[AutoMod/Deny] Error: "+makeError(result));
            }
            pendingDeny.remove(msgId);
        }
    }
    
    private String makeError(String result) {
        switch (result) {
            case "404": return "Message not/no longer available.";
            case "400": return "Message already handled.";
            default: return "Unknown error.";
        }
    }
    
}
