
package chatty.gui.components.textpane;

import chatty.Helper;
import chatty.gui.components.Channel;
import chatty.util.api.eventsub.payloads.ModActionPayload;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class ModLogInfo extends InfoMessage {

    private static final Set<String> BAN_COMMANDS
            = new HashSet<>(Arrays.asList(new String[]{"timeout", "ban", "delete"}));
    
    private static final Set<String> UNBAN_COMMANDS
            = new HashSet<>(Arrays.asList(new String[]{"untimeout", "unban"}));
    
    public final ModActionPayload data;
    public final boolean showActionBy;
    public final boolean ownAction;
    public final Channel chan;
    
    public ModLogInfo(Channel chan, ModActionPayload data,
            boolean showActionBy, boolean ownAction) {
        super(Type.INFO, makeText(data));
        this.chan = chan;
        this.data = data;
        this.showActionBy = showActionBy;
        this.ownAction = ownAction;
    }
    
    public ModLogInfo(ModLogInfo other) {
        super(other);
        data = other.data;
        showActionBy = other.showActionBy;
        ownAction = other.ownAction;
        chan = other.chan;
    }
    
    @Override
    public ModLogInfo copy() {
        return new ModLogInfo(this);
    }
    
    private static String makeText(ModActionPayload data) {
        return String.format("[ModAction] %s: %s",
                data.created_by,
                data.getPseudoCommandString());
    }
    
    @Override
    public String makeCommand() {
        return makeCommand(data);
    }
    
    public static String makeCommand(ModActionPayload data) {
        switch (data.moderation_action) {
            case "timeout": return makeTimeoutCommand(data);
            case "ban": return makeBanCommand(data);
            case "delete": return makeDeleteCommand(data);
            default: return data.moderation_action;
        }
    }
    
    public boolean isBanCommand() {
        return isBanCommand(data);
    }
    
    public static boolean isBanCommand(ModActionPayload data) {
        return BAN_COMMANDS.contains(data.moderation_action);
    }
    
    public static boolean isUnbanCommand(ModActionPayload data) {
        return UNBAN_COMMANDS.contains(data.moderation_action);
    }
    
    public static boolean isBanOrInfoAssociated(ModActionPayload data) {
        return isBanCommand(data) || InfoMessage.msgIdHasCommand(data.moderation_action);
    }
    
    public static boolean isAssociated(ModActionPayload data) {
        return isBanOrInfoAssociated(data) || isAutoModAction(data);
    }
    
    public boolean isAutoModAction() {
        return isAutoModAction(data);
    }
    
    public static boolean isAutoModAction(ModActionPayload data) {
        return data.type == ModActionPayload.Type.AUTOMOD_APPROVED
                || data.type == ModActionPayload.Type.AUTOMOD_DENIED;
    }
    
    public static String makeDeleteCommand(ModActionPayload data) {
        if (data.action instanceof ModActionPayload.Delete) {
            ModActionPayload.Delete delete = (ModActionPayload.Delete) data.action;
            return data.moderation_action+" "+delete.getMsgId();
        }
        return "";
    }
    
    public static String makeBanCommand(ModActionPayload data) {
        if (data.action instanceof ModActionPayload.Ban) {
            ModActionPayload.Ban ban = (ModActionPayload.Ban) data.action;
            return data.moderation_action+" "+ban.getTargetUsername();
        }
        return "";
    }
    
    public static String makeTimeoutCommand(ModActionPayload data) {
        if (data.action instanceof ModActionPayload.Timeout) {
            ModActionPayload.Timeout timeout = (ModActionPayload.Timeout) data.action;
            return data.moderation_action+" "+timeout.getTargetUsername(); // Duration is inaccurate now
        }
        return "";
    }
    
    public String getReason() {
        return getReason(data);
    }
    
    public static String getReason(ModActionPayload data) {
        switch (data.moderation_action) {
            case "timeout": return ((ModActionPayload.Timeout) data.action).getReason();
            case "ban": return ((ModActionPayload.Ban) data.action).getReason();
            default: return null;
        }
    }
    
    public static String getTargetUsername(ModActionPayload data) {
        if (data.action instanceof ModActionPayload.ModActionUser) {
            ModActionPayload.ModActionUser userAction = (ModActionPayload.ModActionUser) data.action;
            if (Helper.isValidStream(userAction.getTargetUsername())) {
                return userAction.getTargetUsername();
            }
        }
        return null;
    }

    public static String getBannedUsername(ModActionPayload data) {
        if (isBanCommand(data)) {
            return getTargetUsername(data);
        }
        return null;
    }
    
    public static String getUnbannedUsername(ModActionPayload data) {
        if (isUnbanCommand(data)) {
            return getTargetUsername(data);
        }
        return null;
    }
    
    public static String getTargetUserInfo(ModActionPayload data) {
        String targetUser = getTargetUsername(data);
        if (targetUser != null) {
            return String.format("@%s used \"%s\" on this user",
                                 data.created_by,
                                 data.getPseudoCommandString());
        }
        return null;
    }
    
    @Override
    public String toString() {
        return text;
    }
    
}
