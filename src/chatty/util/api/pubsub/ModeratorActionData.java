
package chatty.util.api.pubsub;

import chatty.util.DateTime;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public enum Type {
        AUTOMOD_REJECTED, AUTOMOD_APPROVED, AUTOMOD_DENIED, OTHER, UNMODDED
    }
    
    /**
     * The name of the action. Can never be null.
     */
    public final String moderation_action;
    
    /**
     * The args associated with this action. An empty list if not present.
     */
    public final List<String> args;
    
    /**
     * The name of the user this action orginiated from. Can never be null.
     */
    public final String created_by;
    
    /**
     * The stream/room this action originated in. May be null if some kind of
     * error occured.
     */
    public final String stream;
    
    /**
     * The msg_id value. If not present, an empty value.
     */
    public final String msgId;
    
    /**
     * Determine some known types of actions (but not all).
     */
    public final Type type;
    
    public ModeratorActionData(String msgType, String topic, String message, String stream,
            String moderation_action, List<String> args, String created_by,
            String msgId) {
        super(topic, message);
        
        this.moderation_action = moderation_action;
        this.args = args;
        this.created_by = created_by;
        this.stream = stream;
        this.msgId = msgId;
        
        // Determine some known types of actions
        if (msgType.equals("moderator_removed")) {
            type = Type.UNMODDED;
        } else {
            switch (moderation_action) {
                case "twitchbot_rejected":
                case "automod_rejected":
                case "rejected_automod_message":
                case "automod_cheer_rejected":
                    // Just guessing at this point D:
                    type = Type.AUTOMOD_REJECTED;
                    break;
                case "approved_automod_message":
                    type = Type.AUTOMOD_APPROVED;
                    break;
                case "denied_automod_message":
                    type = Type.AUTOMOD_DENIED;
                    break;
                default:
                    type = Type.OTHER;
                    break;
            }
        }
    }
    
    public static ModeratorActionData decode(String topic, String message, Map<String, String> userIds) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject)parser.parse(message);
        
        String msgType = JSONUtil.getString(root, "type", "");
        
        JSONObject data = (JSONObject)root.get("data");
        
        String moderation_action = (String)data.get("moderation_action");
        if (moderation_action == null) {
            moderation_action = "";
        }
        
        List<String> args = new ArrayList<>();
        JSONArray argsData = (JSONArray)data.get("args");
        if (argsData != null) {
            for (Object argsItem : argsData) {
                String item = String.valueOf(argsItem);
                // AutoMod messages seem to sometimes require this
                item = StringUtil.removeLinebreakCharacters(item);
                item = StringUtil.shortenTo(item, 500);
                args.add(item);
            }
        }
        
        // For type:"moderator_added" events, which don't have args
        if (args.isEmpty() && (moderation_action.equals("mod") || moderation_action.equals("unmod"))) {
            String target_user = (String)data.get("target_user_login");
            if (target_user != null) {
                args.add(target_user);
            }
        }
        // For type:"approve/deny_unban_request" events
        if (args.isEmpty() && (moderation_action.equals("APPROVE_UNBAN_REQUEST") || moderation_action.equals("DENY_UNBAN_REQUEST"))) {
            String target_user = JSONUtil.getString(data, "target_user_login");
            if (target_user != null) {
                args.add(target_user);
            }
            String mod_message = JSONUtil.getString(data, "moderator_message");
            if (mod_message != null) {
                args.add(mod_message);
            }
        }
        
        String created_by = (String)data.get("created_by");
        if (created_by == null) {
            created_by = JSONUtil.getString(data, "created_by_login");
        }
        // Warn command has empty created_by when broadcaster does it
        if (StringUtil.isNullOrEmpty(created_by)) {
            String createdById = JSONUtil.getString(data, "created_by_user_id");
            String chanId = JSONUtil.getString(data, "channel_id");
            String chanLogin = JSONUtil.getString(data, "channel_login");
            if (Objects.equals(chanId, createdById)) {
                created_by = chanLogin;
            }
        }
        if (created_by == null) {
            created_by = "";
        }
        
        String msgId = (String)data.get("msg_id");
        if (msgId == null) {
            msgId = "";
        }
        
        // This one has different parameters (add allowed/blocked AutoMod term)
        if (msgType.equals("channel_terms_action")) {
            moderation_action = JSONUtil.getString(data, "type", "");
            created_by = JSONUtil.getString(data, "requester_login", "");
            args.clear();
            args.add(JSONUtil.getString(data, "text", ""));
            String expireTime = getTime(JSONUtil.getString(data, "expires_at"), null);
            if (expireTime != null) {
                if (moderation_action.startsWith("delete")) {
                    args.add("(would have expired in "+expireTime+")");
                }
                else {
                    args.add("(expires in "+expireTime+")");
                }
            }
            if (!JSONUtil.getBoolean(data, "from_automod", true)) {
                args.add("[manual]");
            }
        }
        
        String stream = Helper.getStreamFromTopic(topic, userIds);
        
        return new ModeratorActionData(msgType, topic, message, stream, moderation_action, args, created_by, msgId);
    }
    
    public static ModeratorActionData decodeAutoMod(String topic, String message, Map<String, String> userIds) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject)parser.parse(message);
        
        String type = JSONUtil.getString(root, "type", "");
        if (!type.equals("automod_caught_message")) {
            throw new RuntimeException("Unknown AutoMod type: "+type);
        }
        
        JSONObject data = (JSONObject) root.get("data");
        JSONObject msg = (JSONObject) data.get("message");
        
        //--------------------------
        // General
        //--------------------------
        String msgId = JSONUtil.getString(msg, "id", "");
        String created_by = JSONUtil.getString(data, "resolver_login", "");
        
        // Use mod action names from old topic where possible
        String moderation_action = "";
        switch (JSONUtil.getString(data, "status")) {
            case "PENDING":
                moderation_action = "automod_rejected";
                break;
            case "DENIED":
                moderation_action = "denied_automod_message";
                break;
            case "ALLOWED":
                moderation_action = "approved_automod_message";
                break;
            case "EXPIRED":
                moderation_action = "automod_message_expired";
                break;
            default:
                moderation_action = "unknown_automod_action";
        }
        
        //--------------------------
        // Args
        //--------------------------
        // username, msg, additional..
        List<String> args = new ArrayList<>();
        
        Object senderObj = msg.get("sender");
        if (senderObj instanceof JSONObject) {
            JSONObject sender = (JSONObject) senderObj;
            args.add(JSONUtil.getString(sender, "login"));
        }
        else {
            args.add("");
        }
        
        // Add content info (msg)
        Object contentObj = msg.get("content");
        String fragmentsInfo = null;
        if (contentObj instanceof JSONObject) {
            JSONObject content = (JSONObject) contentObj;
            args.add(JSONUtil.getString(content, "text"));
            
            // Fragements info is additional info, but is retrieved from here
            try {
                fragmentsInfo = buildAutomodFragments((JSONArray)content.get("fragments"));
            }
            catch (Exception ex) {
                // Keep framgents info empty
            }
        }
        else {
            args.add("");
        }
        
        // Add additional info
        Object contentClassObj = data.get("content_classification");
        if (contentClassObj instanceof JSONObject) {
            JSONObject contentClass = (JSONObject) contentClassObj;
            args.add(String.format("%s%s; %s",
                    JSONUtil.getString(contentClass, "category", ""),
                    JSONUtil.getInteger(contentClass, "level", 0),
                    fragmentsInfo));
        }
        
        // Build data
        String stream = Helper.getStreamFromTopic(topic, userIds);
        return new ModeratorActionData(type, topic, message, stream, moderation_action, args, created_by, msgId);
    }
    
    private static String buildAutomodFragments(JSONArray fragments) {
        StringBuilder b = new StringBuilder();
        for (Object o : fragments) {
            JSONObject fragment = (JSONObject) o;
            String text = JSONUtil.getString(fragment, "text");
            if (fragment.containsKey("automod")) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append("\"").append(text).append("\":");
                JSONObject topics = (JSONObject)((JSONObject) fragment.get("automod")).get("topics");
                boolean first = true;
                for (Object o2 : topics.keySet()) {
                    if (!first) {
                        b.append("/");
                    }
                    first = false;
                    String key = (String) o2;
                    b.append(key).append(topics.get(key));
                }
            }
        }
        return b.toString();
    }
    
    private static String getTime(String value, String errorValue) {
        if (StringUtil.isNullOrEmpty(value)) {
            return errorValue;
        }
        try {
            long datetime = DateTime.parseDatetime(value);
            long duration = Math.abs(System.currentTimeMillis() - datetime);
            return DateTime.duration(duration, 0, 1, 0, DateTime.Formatting.COMPACT);
        }
        catch (IllegalArgumentException ex) {
            return errorValue;
        }
    }
    
    public String getCommandAndParameters() {
        return moderation_action+" "+StringUtil.join(args, " ");
    }
    
    /**
     * Get the argument with the given index, or defaultValue if it an argument
     * with that index doesn't exist.
     * 
     * @param index The index
     * @param defaultValue The fallback value
     * @return The argument value, or the defaultValue if it doesn't exist
     */
    public String getArg(int index, String defaultValue) {
        if (args.size() > index) {
            return args.get(index);
        }
        return defaultValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ModeratorActionData other = (ModeratorActionData) obj;
        if (!Objects.equals(this.topic, other.topic)) {
            return false;
        }
        if (!Objects.equals(this.moderation_action, other.moderation_action)) {
            return false;
        }
        if (!Objects.equals(this.args, other.args)) {
            return false;
        }
        if (!Objects.equals(this.created_by, other.created_by)) {
            return false;
        }
        if (!Objects.equals(this.stream, other.stream)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.topic);
        hash = 53 * hash + Objects.hashCode(this.moderation_action);
        hash = 53 * hash + Objects.hashCode(this.args);
        hash = 53 * hash + Objects.hashCode(this.created_by);
        hash = 53 * hash + Objects.hashCode(this.stream);
        return hash;
    }
    
    @Override
    public String toString() {
        return String.format("'%s'@%s", getCommandAndParameters(), created_by);
    }
    
}
