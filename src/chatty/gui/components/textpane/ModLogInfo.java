
package chatty.gui.components.textpane;

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
    
    public ModLogInfo(ModeratorActionData data, boolean showActionBy) {
        super(Type.INFO, makeText(data));
        this.data = data;
        this.showActionBy = showActionBy;
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
            case "timeout":
            case "ban": return data.getCommandAndParameters();
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
        if (data.args.size() == 3) {
            return data.moderation_action+" "+data.args.get(2);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return text;
    }
    
}
