
package chatty.gui.components.textpane;

import chatty.Helper;
import chatty.gui.components.Channel;
import chatty.util.StringUtil;
import chatty.util.api.pubsub.ModeratorActionData;
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
    
    public final ModeratorActionData data;
    public final boolean showActionBy;
    public final boolean ownAction;
    public final Channel chan;
    
    public ModLogInfo(Channel chan, ModeratorActionData data,
            boolean showActionBy, boolean ownAction) {
        super(Type.INFO, makeText(data));
        this.chan = chan;
        this.data = data;
        this.showActionBy = showActionBy;
        this.ownAction = ownAction;
    }
    
    private static String makeText(ModeratorActionData data) {
        return String.format("[ModAction] %s: /%s %s",
                data.created_by,
                data.moderation_action,
                makeArgsText(data));
    }
    
    public static String makeArgsText(ModeratorActionData data) {
        if (data.type == ModeratorActionData.Type.AUTOMOD_REJECTED && data.args.size() == 3) {
            return String.format("[%s] <%s> %s",
                    data.args.get(2), data.args.get(0), data.args.get(1));
        }
        if ((data.type == ModeratorActionData.Type.AUTOMOD_APPROVED || data.type == ModeratorActionData.Type.AUTOMOD_DENIED) && data.args.size() > 1) {
            return String.format("<%s> %s",
                    data.args.get(0), data.args.get(1));
        }
        if (data.moderation_action.equals("delete") && data.args.size() > 1) {
            return String.format("%s (%s)", data.args.get(0), data.args.get(1));
        }
        return StringUtil.join(data.args, " ");
    }
    
    @Override
    public String makeCommand() {
        return makeCommand(data);
    }
    
    public static String makeCommand(ModeratorActionData data) {
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
    
    public static boolean isBanCommand(ModeratorActionData data) {
        return BAN_COMMANDS.contains(data.moderation_action);
    }
    
    public static boolean isUnbanCommand(ModeratorActionData data) {
        return UNBAN_COMMANDS.contains(data.moderation_action);
    }
    
    public static boolean isBanOrInfoAssociated(ModeratorActionData data) {
        return isBanCommand(data) || InfoMessage.msgIdHasCommand(data.moderation_action);
    }
    
    public static boolean isAssociated(ModeratorActionData data) {
        return isBanOrInfoAssociated(data) || isAutoModAction(data);
    }
    
    /**
     * An action that is attributed to the user, but not actually performed
     * directly by the user. For example accepting/rejecting an AutoMod message,
     * which can trigger terms being permitted/blocked without the moderator
     * explicitly doing it.
     * 
     * @param data
     * @return 
     */
    public static boolean isIndirectAction(ModeratorActionData data) {
        return data.moderation_action.equals("add_permitted_term")
                || data.moderation_action.equals("add_blocked_term");
    }
    
    public boolean isAutoModAction() {
        return isAutoModAction(data);
    }
    
    public static boolean isAutoModAction(ModeratorActionData data) {
        return data.type == ModeratorActionData.Type.AUTOMOD_APPROVED
                || data.type == ModeratorActionData.Type.AUTOMOD_DENIED;
    }
    
    public static String makeDeleteCommand(ModeratorActionData data) {
        if (data.args.size() > 2) {
            return data.moderation_action+" "+data.args.get(2);
        }
        return "";
    }
    
    public static String makeBanCommand(ModeratorActionData data) {
        if (data.args.size() > 0) {
            return data.moderation_action+" "+data.args.get(0);
        }
        return "";
    }
    
    public static String makeTimeoutCommand(ModeratorActionData data) {
        if (data.args.size() > 1) {
            return data.moderation_action+" "+data.args.get(0)+" "+data.args.get(1);
        }
        return "";
    }
    
    public String getReason() {
        return getReason(data);
    }
    
    public static String getReason(ModeratorActionData data) {
        switch (data.moderation_action) {
            case "timeout": return getReason(data, 2);
            case "ban": return getReason(data, 1);
            default: return null;
        }
    }
    
    private static String getReason(ModeratorActionData data, int index) {
        if (data.args.size() > index && !data.args.get(index).isEmpty()) {
            return data.args.get(index);
        }
        return null;
    }
    
    public static String getBannedUsername(ModeratorActionData data) {
        if (isBanCommand(data) && data.args.size() > 0
                && Helper.isValidStream(data.args.get(0))) {
            return data.args.get(0);
        }
        return null;
    }
    
    public static String getUnbannedUsername(ModeratorActionData data) {
        if (isUnbanCommand(data) && data.args.size() > 0
                && Helper.isValidStream(data.args.get(0))) {
            return data.args.get(0);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return text;
    }
    
}
