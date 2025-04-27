
package chatty.util.api.eventsub.payloads;

import chatty.Helper;
import chatty.TwitchCommands;
import chatty.util.BatchAction;
import chatty.util.DateTime;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.eventsub.Payload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * 
 * 
 * @author tduva
 */
public class ModActionPayload extends Payload {
    
    private static final Logger LOGGER = Logger.getLogger(ModActionPayload.class.getName());
    
    public enum Type {
        AUTOMOD_FILTERED, AUTOMOD_APPROVED, AUTOMOD_DENIED, OTHER, UNMODDED
    }
    
    public final String moderation_action;
    public final String created_by;
    public final ModAction action;
    public final String stream;
    public final String source_stream;
    public final Type type;
    
    public ModActionPayload(String moderator_action, String created_by, ModAction action, String stream, String source_stream) {
        this.moderation_action = moderator_action;
        this.created_by = created_by;
        if (action == null) {
            action = new Simple(moderator_action);
        }
        this.action = action;
        this.stream = stream;
        this.source_stream = source_stream;
        
        // Determine some known types of actions
//        if (msgType.equals("moderator_removed")) {
//            type = Type.UNMODDED;
//        } else {
            switch (moderation_action) {
                case "automod_filtered":
                    type = Type.AUTOMOD_FILTERED;
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
//            }
        }
    }
    
    public String getPseudoCommandString() {
        return String.format("/%s",
                             action.toString());
    }
    
    public String getPseudoCommandStringNoSlash() {
        return action.toString();
    }
    
    /**
     * Get the raw event data, if available. Should only be used for testing,
     * proper data retrieval should be encapsulated in ModAction classes.
     * 
     * @return May be null
     */
    public String getData() {
        return action.getEvent();
    }
    
    public boolean isShared() {
        return !StringUtil.isNullOrEmpty(source_stream) && !source_stream.equals(stream);
    }
    
    public String getSourceChannel() {
        return isShared() ? Helper.toChannel(source_stream) : null;
    }
    
    public static ModActionPayload decode(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            ModAction action = getAction(payload);
            if (action != null && action.isValid()) {
                return new ModActionPayload(
                        action.action,
                        JSONUtil.getString(event, "moderator_user_login"),
                        action,
                        JSONUtil.getString(event, "broadcaster_user_login"),
                        JSONUtil.getString(event, "source_broadcaster_user_login"));
            }
            else {
                LOGGER.warning("Invalid ModAction: "+action);
            }
        }
        return null;
    }
    
    public static ModActionPayload decodeAutomodHeld(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            ModAction action = new AutoModMessageUpdate(payload);
            if (action.isValid()) {
                return new ModActionPayload(
                        "automod_filtered",
                        "",
                        new AutoModMessageUpdate(payload),
                        JSONUtil.getString(event, "broadcaster_user_login"),
                        JSONUtil.getString(event, "source_broadcaster_user_login"));
            }
        }
        return null;
    }
    
    public static ModActionPayload decodeAutomodUpdate(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            ModAction action = new AutoModMessageUpdate(payload);
            if (action.isValid()) {
                return new ModActionPayload(
                        action.action,
                        JSONUtil.getString(event, "moderator_user_login"),
                        action,
                        JSONUtil.getString(event, "broadcaster_user_login"),
                        JSONUtil.getString(event, "source_broadcaster_user_login"));
            }
        }
        return null;
    }
    
    private static ModAction getAction(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        switch (ModAction2.getModAction(payload)) {
            case "mod":
                return new Mod(payload);
            case "unmod":
                return new Unmod(payload);
            case "vip":
                return new Vip(payload);
            case "unvip":
                return new Unvip(payload);
            case "warn":
                return new Warn(payload);
            case "followers":
                return new Followers(payload);
            case "slow":
                return new Slow(payload);
            case "ban":
                return new Ban(payload);
            case "unban":
                return new Unban(payload);
            case "timeout":
                return new Timeout(payload);
            case "untimeout":
                return new Untimeout(payload);
            case "raid":
                return new Raid(payload);
            case "unraid":
                return new Unraid(payload);
            case "delete":
                return new Delete(payload);
                
        }
        if (event.get("automod_terms") instanceof JSONObject) {
            return new Automod_Terms(payload);
        }
        if (event.get("unban_request") instanceof JSONObject) {
            return new Unban_Request(payload);
        }
        // Actions that have no arguments, like "/followersoff" and the like
        return new NoArgsAction(payload);
    }
    
    /**
     * Actions that are not from the main mod actions topic can implement this
     * directly, otherwise one of the abstract subclasses may fit better.
     */
    public abstract static class ModAction {
        
        public final String action;
        public final long eventCreatedAt;
        
        /**
         * The "event" section from "channel.moderate" event, may be null if
         * from another source.
         */
        protected JSONObject event;
        
        /**
         * 
         * 
         * @param action
         * @param payload Full JSON from EventSub "channel.moderate" topic, may
         * be null
         */
        public ModAction(String action, JSONObject payload) {
            this.action = action;
            // "created_at" in "subscription" in payload seemed quite off
            this.eventCreatedAt = System.currentTimeMillis();
            if (payload == null) {
                this.event = null;
            }
            else {
                this.event = (JSONObject) payload.get("event");
            }
        }
        
        public String getEvent() {
            if (event != null) {
                return event.toString();
            }
            return null;
        }
        
        /**
         * An action that is attributed to the user, but not actually performed
         * directly by the user. For example accepting/rejecting an AutoMod
         * message, which can trigger terms being permitted/blocked without the
         * moderator explicitly doing it.
         *
         * @return
         */
        public boolean isIndirectAction() {
            return false;
        }
        
        public abstract boolean isValid();
        
    }
    
    
    //=================
    // AutoMod Actions
    //=================
    
    // ok, maybe expired?
    public static class AutoModMessageUpdate extends ModAction {
        
        public AutoModMessageUpdate(JSONObject payload) {
            super(getActionFromStatus(payload), payload);
        }
        
        private static String getActionFromStatus(JSONObject payload) {
            JSONObject event = (JSONObject) payload.get("event");
            String status = JSONUtil.getString(event, "status");
            if (status == null) {
                return "automod_filtered";
            }
            switch (status) {
                case "approved":
                    return "approved_automod_message";
                case "denied":
                    return "denied_automod_message";
                case "expired":
                    return "automod_message_expired";
            }
            return "unkown_automod_status";
        }
        
        @Override
        public String toString() {
            return String.format("%s [%s] <%s> %s",
                                 action, getReason(), getUsername(), getMessage());
        }
        
        public String getMsgId() {
            return JSONUtil.getString(event, "message_id");
        }
        
        public String getUsername() {
            return JSONUtil.getString(event, "user_login");
        }
        
        public String getMessage() {
            return JSONUtil.getString(JSONUtil.getOrEmpty(event, "message"), "text");
        }

        public String getReason() {
            switch (JSONUtil.getString(event, "reason")) {
                case "automod":
                    JSONObject automod = JSONUtil.getOrEmpty(event, "automod");
                    String category = JSONUtil.getString(automod, "category");
                    int level = JSONUtil.getInteger(automod, "level", 0);
                    
                    List<String> fragments = new ArrayList<>();
                    JSONArray boundaries = (JSONArray) automod.get("boundaries");
                    for (Object boundary : boundaries) {
                        String fragment = getFragment((JSONObject) boundary);
                        if (fragment != null) {
                            fragments.add(fragment);
                        }
                    }
                    return String.format("AutoMod: %s%s/%s",
                                         category, level, StringUtil.join(fragments,", "));
                case "blocked_term":
                    JSONObject blocked_term = JSONUtil.getOrEmpty(event, "blocked_term");
                    JSONArray terms = (JSONArray) blocked_term.get("terms_found");
                    String stream = JSONUtil.getString(event, "broadcaster_user_login");
                    List<String> result = new ArrayList<>();
                    for (Object o : terms) {
                        JSONObject term = (JSONObject) o;
                        String fragment = getFragment((JSONObject) term.get("boundary"));
                        if (fragment != null) {
                            String term_stream = JSONUtil.getString(term, "owner_broadcaster_user_login");
                            if (term_stream != null && !term_stream.equals(stream)) {
                                result.add(String.format("%s (from: %s)",
                                                         fragment, term_stream));
                            }
                            else {
                                result.add(String.format("%s",
                                                         fragment));
                            }
                        }
                    }
                    return String.format("BlockedTerm: %s",
                                         StringUtil.join(result, ","));
            }
            return JSONUtil.getString(event, "category");
        }
        
        private String getFragment(JSONObject boundary) {
            int start = JSONUtil.getInteger((JSONObject) boundary, "start_pos", -1);
            int end = JSONUtil.getInteger((JSONObject) boundary, "end_pos", -1);
            if (start > -1 && end > -1) {
                /**
                 * Error before using codePointSubstring(), although it's
                 * unclear if that error could even happen due to that (if
                 * Chatty sees it as more characters than the API), so catching
                 * error here now since it's more important for the AutoMod
                 * message to appear at all than the reason being wrong.
                 *
                 * [2025-03-31 01:09:23/654 WARNING] Error parsing EventSub
                 * message: java.lang.StringIndexOutOfBoundsException: begin 9, end 17, length 8
                 * [java.base/java.lang.String.checkBoundsBeginEnd,
                 * java.base/java.lang.String.substring,
                 * chatty.util.api.eventsub.payloads.ModActionPayload$AutoModMessageUpdate.getFrament(ModActionPayload.java:332),
                 * chatty.util.api.eventsub.payloads.ModActionPayload$AutoModMessageUpdate.getReason(ModActionPayload.java:309),
                 * chatty.util.api.eventsub.payloads.ModActionPayload$AutoModMessageUpdate.isValid(ModActionPayload.java:339)...]
                 */
                try {
                    return StringUtil.codePointSubstring(getMessage(), start, end + 1);
                }
                catch (Exception ex) {
                    // Would output several times otherwise
                    BatchAction.queue(event, 100, false, false, () -> {
                        LOGGER.warning("[EventSub] Error getting AutoMod reason: "+ex+" ["+event+"]");
                    });
                    return "error, check debug log";
                }
            }
            return null;
        }
        
        @Override
        public boolean isValid() {
            return !StringUtil.isNullOrEmpty(getMsgId(), getReason(), getUsername(), action);
        }
        
    }
    
    //=================
    // Regular Actions
    //=================
    
    /**
     * Regular mod action from the main topic. Helps with building the
     * pseudo-command string and selects the actionData automatically.
     */
    public abstract static class ModAction2 extends ModAction {
        
        protected JSONObject actionData;
        
        public ModAction2(JSONObject payload) {
            super(getModAction(payload), payload);
            actionData = (JSONObject) event.get(action);
            if (actionData == null) {
                // If shared chat event, it's under different key
                actionData = (JSONObject) event.get("shared_chat_"+action);
            }
        }
        
        public static String getModAction(JSONObject payload) {
            JSONObject event = (JSONObject) payload.get("event");
            String action = JSONUtil.getString(event, "action");
            if (action.startsWith("shared_chat_")) {
                action = action.substring("shared_chat_".length());
            }
            return action;
        }
        
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(action);
            for (String key : getParamKeys()) {
                String value = getStringForKey(key);
                if (!StringUtil.isNullOrEmpty(value)) {
                    if (b.length() > 0) {
                        b.append(" ");
                    }
                    b.append(value);
                }
            }
            return b.toString();
        }
        
        private String getStringForKey(String key) {
            String value = getParamForKey(key);
            if (value == null) {
                value = JSONUtil.getString(actionData, key);
            }
            return value;
        }
        
        /**
         * Return a list of keys that are the args for the pseudo-commands.
         * They also have to be non-null for the action to be considered valid,
         * unless {@link isValid()} is overwritten.
         * 
         * @return 
         */
        abstract List<String> getParamKeys();
        
        /**
         * If this returns a non-null value for a key this one is used instead
         * of the default directly from the {@code actionData}.
         * 
         * @param key
         * @return 
         */
        String getParamForKey(String key) {
            return null;
        }
        
        final List<String> args(String... keys) {
            return Arrays.asList(keys);
        }
        
        @Override
        public boolean isValid() {
            for (String key : getParamKeys()) {
                if (getStringForKey(key) == null) {
                    return false;
                }
            }
            return true;
        }
        
    }
    
    /**
     * Actions that have a target user attached. The isValid() method should be
     * overwritten if more than the username is relevant.
     */
    public abstract static class ModActionUser extends ModAction2 {

        public ModActionUser(JSONObject payload) {
            super(payload);
        }
        
        public String getTargetUsername() {
            return JSONUtil.getString(actionData, "user_login");
        }
        
        @Override
        public boolean isValid() {
            return !StringUtil.isNullOrEmpty(getTargetUsername());
        }
        
    }
    
    /**
     * Actions without any arguments, like "/followersoff" and the like.
     */
    public static class NoArgsAction extends ModAction2 {

        public NoArgsAction(JSONObject event) {
            super(event);
        }

        @Override
        List<String> getParamKeys() {
            return args();
        }
        
    }
    
    public static class Mod extends ModActionUser {

        public Mod(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login");
        }
        
    }
    
    public static class Unmod extends ModActionUser {

        public Unmod(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login");
        }
        
    }
    
    public static class Warn extends ModActionUser {

        public Warn(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login", "reason"); // custom reason output contains rules
        }
        
        @Override
        String getParamForKey(String key) {
            if (key.equals("reason")) {
                return getReason();
            }
            return null;
        }
        
        public String getReason() {
            String reason = JSONUtil.getString(actionData, "reason");
            List<String> rules = JSONUtil.getStringList(actionData, "chat_rules_cited");
            StringBuilder b = new StringBuilder();
            
            if (!StringUtil.isNullOrEmpty(reason)) {
                b.append(reason);
            }
            if (rules != null) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append("Selected rules: ");
                b.append(StringUtil.join(rules, ", "));
            }
            return b.toString();
        }
        
        @Override
        public boolean isValid() {
            return !StringUtil.isNullOrEmpty(getTargetUsername(), getReason());
        }
        
    }
    
    public static class Followers extends ModAction2 {

        public Followers(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("follow_duration_minutes");
        }
        
        @Override
        String getParamForKey(String key) {
            if (key.equals("follow_duration_minutes")) {
                int minutes = JSONUtil.getInteger(actionData, "follow_duration_minutes", 0);
                return TwitchCommands.formatDuration(minutes * 60); // empty if 0
            }
            return null;
        }
        
    }
    
    public static class Slow extends ModAction2 {

        public Slow(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("wait_time_seconds");
        }
        
        @Override
        String getParamForKey(String key) {
            if (key.equals("wait_time_seconds")) {
                int seconds = JSONUtil.getInteger(actionData, "wait_time_seconds", 0);
                return TwitchCommands.formatDuration(seconds); // empty if 0
            }
            return null;
        }
        
    }
    
    public static class Vip extends ModActionUser {

        public Vip(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login");
        }
        
    }
    
    public static class Unvip extends ModActionUser {

        public Unvip(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login");
        }
        
    }
    
    public static class Ban extends ModActionUser {

        public Ban(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login", "reason");
        }
        
        public String getReason() {
            return JSONUtil.getString(actionData, "reason");
        }
        
    }
    
    public static class Unban extends ModActionUser {

        public Unban(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login");
        }
        
    }
    
    public static class Timeout extends ModActionUser {

        public Timeout(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login", "expires_at", "reason");
        }
        
        @Override
        String getParamForKey(String key) {
            if (key.equals("expires_at")) {
                return getDuration();
            }
            return null;
        }
        
        public String getReason() {
            return JSONUtil.getString(actionData, "reason");
        }
        
        public String getDuration() {
            String datetime = JSONUtil.getString(actionData, "expires_at");
            long expires = DateTime.parseDatetime(datetime);
            return DateTime.duration(expires - eventCreatedAt);
        }
        
        @Override
        public boolean isValid() {
            return !StringUtil.isNullOrEmpty(getTargetUsername()) && getDuration() != null;
        }
        
    }
    
    public static class Untimeout extends ModActionUser {

        public Untimeout(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login");
        }
        
    }
    
    public static class Raid extends ModActionUser {

        public Raid(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login");
        }
        
    }
    
    public static class Unraid extends ModActionUser {

        public Unraid(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login");
        }
        
    }
    
    public static class Delete extends ModActionUser {

        public Delete(JSONObject payload) {
            super(payload);
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login", "message_body");
        }
        
        @Override
        String getParamForKey(String key) {
            if (key.equals("message_body")) {
                return getMsgText();
            }
            return null;
        }
        
        public String getMsgId() {
            return JSONUtil.getString(actionData, "message_id");
        }
        
        public String getMsgText() {
            return String.format("(%s)", JSONUtil.getString(actionData, "message_body"));
        }
        
        @Override
        public boolean isValid() {
            return !StringUtil.isNullOrEmpty(getTargetUsername(), getMsgId(), getMsgText());
        }
        
    }
    
    public static class Automod_Terms extends ModAction2 {

        public Automod_Terms(JSONObject payload) {
            super(payload);
            this.actionData = (JSONObject) event.get("automod_terms"); // Overwrite since it's not under the action
        }

        @Override
        List<String> getParamKeys() {
            return args("terms", "from_automod");
        }
        
        @Override
        String getParamForKey(String key) {
            if (key.equals("terms")) {
                List<String> terms = JSONUtil.getStringList(actionData, "terms");
                if (terms != null) {
                    return StringUtil.join(terms, ",");
                }
            }
            else if (key.equals("from_automod")) {
                return JSONUtil.getBoolean(actionData, "from_automod", false)
                        ? "[auto]"
                        : "[manual]";
            }
            return null;
        }
        
        @Override
        public boolean isIndirectAction() {
            return JSONUtil.getBoolean(actionData, "from_automod", false);
        }
        
    }
    
    public static class Unban_Request extends ModActionUser {

        public Unban_Request(JSONObject payload) {
            super(payload);
            this.actionData = (JSONObject) event.get("unban_request"); // Overwrite since it's not under the action
        }

        @Override
        List<String> getParamKeys() {
            return args("user_login", "moderator_message");
        }
        
    }

    //====================================
    // Other Actions (from other sources)
    //====================================
    
    /**
     * Actions that aren't regular actions from EventSub modactions topic, like
     * from another topic or other source entirely.
     */
    public static class Simple extends ModAction {

        public Simple(String action) {
            super(action, null);
        }

        @Override
        public String toString() {
            return action;
        }

        @Override
        public boolean isValid() {
            return !StringUtil.isNullOrEmpty(action);
        }

    }
    
    public static class Shoutout extends ModAction {

        public final String target;
        
        public Shoutout(String target) {
            super("shoutout", null);
            this.target = target;
        }
        
        @Override
        public String toString() {
            return "shoutout "+target;
        }

        @Override
        public boolean isValid() {
            return !StringUtil.isNullOrEmpty(target);
        }
        
    }
    
    public static class SuspiciousUpdate extends ModAction {

        public final String treatment;
        public final String target;
        
        public SuspiciousUpdate(String treatment, String target) {
            super("suspicious_user_"+treatment, null);
            this.treatment = treatment;
            this.target = target;
        }
        
        @Override
        public String toString() {
            return String.format("suspicious_user_%s %s",
                                 treatment,
                                 target);
        }
        
        @Override
        public boolean isValid() {
            return !StringUtil.isNullOrEmpty(treatment, target);
        }
        
    }
    
}
