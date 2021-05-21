
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

    public void requestResult(TwitchApi.AutoModAction action, String msgId, TwitchApi.AutoModActionResult result) {
        if (pendingApprove.containsKey(msgId)) {
            if (result != TwitchApi.AutoModActionResult.SUCCESS) {
                gui.printSystem("[AutoMod/Approve] Error: "+result.errorMessage);
            }
            pendingApprove.remove(msgId);
        }
        if (pendingDeny.containsKey(msgId)) {
            if (result != TwitchApi.AutoModActionResult.SUCCESS) {
                gui.printSystem("[AutoMod/Deny] Error: "+result.errorMessage);
            }
            pendingDeny.remove(msgId);
        }
    }
    
}
