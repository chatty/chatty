
package chatty.util.api.eventsub.payloads;

import chatty.util.JSONUtil;
import chatty.util.api.TwitchApi;
import chatty.util.api.eventsub.Payload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class SuspiciousMessagePayload extends Payload {

    private static final Logger LOGGER = Logger.getLogger(SuspiciousMessagePayload.class.getName());
    
    public enum Type {
        BANNED_IN_SHARED_CHANNEL("banned_in_shared_channel", "shared ban"),
        MANUALLY_ADDED("manually_added", "manual"),
        DETECTED_BAN_EVADER("ban_evader", "ban evader"),
        UNKNOWN("", "unknown");

        public final String id;
        public final String description;

        Type(String id, String description) {
            this.id = id;
            this.description = description;
        }
        
        public static Type fromId(String id) {
            for (Type type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
        
    }
    
    public enum BanEvasionEvaluation {
        POSSIBLE_EVADER("possible", "possible ban evader"),
        LIKELY_EVADER("likely", "ban evader"),
        UNKNOWN_EVADER("unknown", "unknown ban evader");

        public final String id;
        public final String description;

        BanEvasionEvaluation(String id, String description) {
            this.id = id;
            this.description = description;
        }
        
        public static BanEvasionEvaluation fromId(String id) {
            for (BanEvasionEvaluation value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return UNKNOWN_EVADER;
        }
        
    }
    
    public enum Treatment {
        NO_TREATMENT("no_treatment", "not monitored"),
        ACTIVE_MONITORING("active_monitoring", "monitoring"),
        RESTRICTED("restricted", "restricted"),
        UNKNOWN_TREATMENT("", "unknown");

        public final String id;
        public final String description;

        Treatment(String id, String description) {
            this.id = id;
            this.description = description;
        }
        
        public static Treatment fromId(String id) {
            for (Treatment value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return UNKNOWN_TREATMENT;
        }
        
    }
    
    public final String stream;
    public final String username;
    public final String aboutMessageId;
    public final String moderatorUsername;
    public final Set<Type> userTypes;
    public final BanEvasionEvaluation banEvasionEvaluation;
    public final Treatment treatment;
    public final Set<String> bannedInChannels;
    public final Set<String> bannedInChannelsNames = new HashSet<>();
    public final String text;
    
    public SuspiciousMessagePayload(
                                   String stream,
                                   String username,
                                   String aboutMessageId,
                                   String moderatorUsername,
                                   Set<Type> userTypes,
                                   BanEvasionEvaluation evaluation,
                                   Treatment treatment,
                                   Set<String> bannedInChannels,
                                   String text) {
        this.stream = stream;
        this.username = username;
        this.aboutMessageId = aboutMessageId;
        this.moderatorUsername = moderatorUsername;
        this.userTypes = userTypes;
        this.banEvasionEvaluation = evaluation;
        this.treatment = treatment;
        this.bannedInChannels = bannedInChannels;
        this.text = text;
    }
    
    public static SuspiciousMessagePayload decode(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            String stream = JSONUtil.getString(event, "broadcaster_user_login");
            String username = JSONUtil.getString(event, "user_login");
            String modUsername = null;
            
            JSONObject message = (JSONObject) event.get("message");
            String msgId = JSONUtil.getString(message, "message_id");
            String msgText = JSONUtil.getString(message, "text", "");
            
            Set<Type> userTypes = getTypes(JSONUtil.getStringList(event, "types"));
            
            BanEvasionEvaluation banEvasionEvaluation = BanEvasionEvaluation.fromId(JSONUtil.getString(event, "ban_evasion_evaluation"));
            Treatment treatment = Treatment.fromId(JSONUtil.getString(event, "low_trust_status"));
            
            Set<String> bannedInChannels = new HashSet<>();
            List<String> sharedBanChannelIds = JSONUtil.getStringList(event, "shared_ban_channel_ids");
            if (sharedBanChannelIds != null) {
                bannedInChannels.addAll(sharedBanChannelIds);
            }
            
            return new SuspiciousMessagePayload(stream, username, msgId, modUsername, userTypes, banEvasionEvaluation, treatment, bannedInChannels, msgText);
        }
        return null;
    }
    
    public static Set<Type> getTypes(List<String> data) {
        Set<Type> userTypes = new HashSet<>();
        for (String s : data) {
            try {
                userTypes.add(Type.fromId(s));
            }
            catch (IllegalArgumentException e) {
                LOGGER.warning(String.format("FIXME: Unhandled low trust user type %s", s));
            }
        }
        return userTypes;
    }
    
    public static String getTreatmentShort(Treatment treatment) {
        switch (treatment) {
            case ACTIVE_MONITORING:
                return "Monitored user";
            case RESTRICTED:
                return "Restricted user";
        }
        return treatment.description;
    }
    
    public String makeInfo() {
        List<String> elements = new ArrayList<>();

        if (treatment != null) {
            elements.add(getTreatmentShort(treatment));
        }
        
        for (Type userType : userTypes) {
            String str = userType.description;

            synchronized (bannedInChannelsNames) {
                if (userType == Type.BANNED_IN_SHARED_CHANNEL
                        && !bannedInChannelsNames.isEmpty()) {
                    str += ": " + String.join("/", bannedInChannelsNames);
                }
            }

            elements.add(str);
        }
        
//        if (userTypes.contains(Type.MANUALLY_ADDED) && moderatorUsername != null) {
//            elements.add("@" + moderatorUsername);
//        }

        if (banEvasionEvaluation != null
                && banEvasionEvaluation != BanEvasionEvaluation.UNKNOWN_EVADER) {
            elements.add(banEvasionEvaluation.description);
        }

        return elements.stream().filter(Objects::nonNull).collect(Collectors.joining(", "));
    }
    
    public void fetchUserInfoForBannedChannels(TwitchApi api, final Runnable reportDone) {
        if (bannedInChannels.isEmpty()) {
            reportDone.run();
            return;
        }
        
        api.getCachedUserInfoById(new ArrayList<>(bannedInChannels), userInfoMap -> {
            // Result may contain null values in case of errors, so filter out
            synchronized (bannedInChannelsNames) {
                bannedInChannelsNames.addAll(userInfoMap.values().stream().filter(Objects::nonNull).map(u -> u.displayName).collect(Collectors.toSet()));
            }
            reportDone.run();
        });
    }
    
}
