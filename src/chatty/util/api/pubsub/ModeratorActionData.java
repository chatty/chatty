
package chatty.util.api.pubsub;

import chatty.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Data of the moderator action message.
 * 
 * @author tduva
 */
public class ModeratorActionData extends MessageData {

    public final String moderation_action;
    public final List<String> args;
    public final String created_by;
    public final String stream;
    
    public ModeratorActionData(String topic, String message, String stream,
            String moderation_action, List<String> args, String created_by) {
        super(topic, message);
        
        this.moderation_action = moderation_action;
        this.args = args;
        this.created_by = created_by;
        this.stream = stream;
    }
    
    public static ModeratorActionData decode(String topic, String message, Map<Long, String> userIds) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject)parser.parse(message);
        JSONObject data = (JSONObject)root.get("data");
        
        String moderation_action = (String)data.get("moderation_action");
        if (moderation_action == null) {
            moderation_action = "";
        }
        
        List<String> args = new ArrayList<>();
        JSONArray argsData = (JSONArray)data.get("args");
        if (argsData != null) {
            for (Object argsItem : argsData) {
                args.add(String.valueOf(argsItem));
            }
        }
        
        String created_by = (String)data.get("created_by");
        if (created_by == null) {
            created_by = "";
        }
        
        String stream = Helper.getStreamFromTopic(topic, userIds);
        
        return new ModeratorActionData(topic, message, stream, moderation_action, args, created_by);
    }
    
    public String getCommandAndParameters() {
        return moderation_action+" "+StringUtil.join(args, " ");
    }
    
}
