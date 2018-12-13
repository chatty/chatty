
package chatty.gui.components.textpane;

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
                StringUtil.join(data.args, " "));
    }
    
    @Override
    public String makeCommand() {
        switch (data.moderation_action) {
            case "timeout": return makeTimeoutCommand();
            case "ban": return makeBanCommand();
            case "delete": return makeDeleteCommand();
            default: return data.moderation_action;
        }
    }
    
    public boolean isBanCommand() {
        return BAN_COMMANDS.contains(data.moderation_action);
    }
    
    public static boolean isBanOrInfoAssociated(ModeratorActionData data) {
        return BAN_COMMANDS.contains(data.moderation_action) || InfoMessage.msgIdHasCommand(data.moderation_action);
    }
    
    private String makeDeleteCommand() {
        if (data.args.size() > 2) {
            return data.moderation_action+" "+data.args.get(2);
        }
        return "";
    }
    
    private String makeBanCommand() {
        if (data.args.size() > 0) {
            return data.moderation_action+" "+data.args.get(0);
        }
        return "";
    }
    
    private String makeTimeoutCommand() {
        if (data.args.size() > 1) {
            return data.moderation_action+" "+data.args.get(0)+" "+data.args.get(1);
        }
        return "";
    }
    
    public String getReason() {
        switch (data.moderation_action) {
            case "timeout": return getReason(2);
            case "ban": return getReason(1);
            default: return null;
        }
    }
    
    private String getReason(int index) {
        if (data.args.size() > index && !data.args.get(index).isEmpty()) {
            return data.args.get(index);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return text;
    }
    
}
