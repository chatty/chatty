
package chatty.util.api.pubsub;

import chatty.util.JSONUtil;
import chatty.util.api.TwitchApi;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LowTrustUserMessageData extends MessageData {

    private static final Logger LOGGER = Logger.getLogger(LowTrustUserMessageData.class.getName());

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

    public enum BanEvasionEvaluation {
        NOT_EVALUATED("not evaluated"),
        UNLIKELY_EVADER("not a ban evader"),
        POSSIBLE_EVADER("possible ban evader"),
        LIKELY_EVADER("ban evader"),
        UNKNOWN_EVADER(null);

        public final String description;

        BanEvasionEvaluation(String description) {
            this.description = description;
        }
    }

    public enum Treatment {
        NO_TREATMENT("not monitored"),
        ACTIVE_MONITORING("monitoring"),
        RESTRICTED("restricted"),
        UNKNOWN("unknown");

        public final String description;

        Treatment(String description) {
            this.description = description;
        }
    }

    public enum Type {
        //FIXME find out what the other values here are
        BANNED_IN_SHARED_CHANNEL("shared ban"),
        MANUALLY_ADDED("manual"),
        DETECTED_BAN_EVADER("ban evader");

        public final String description;

        Type(String description) {
            this.description = description;
        }
    }

    public LowTrustUserMessageData(String topic,
                                   String message,
                                   String stream,
                                   String username,
                                   String aboutMessageId,
                                   String moderatorUsername,
                                   Set<Type> userTypes,
                                   BanEvasionEvaluation evaluation,
                                   Treatment treatment,
                                   Set<String> bannedInChannels,
                                   String text) {
        super(topic, message);
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
    
    public void fetchUserInfoForBannedChannels(TwitchApi api, final Runnable reportDone) {
        if (bannedInChannels.isEmpty()) {
            reportDone.run();
            return;
        }
        
        api.getCachedUserInfoById(new ArrayList<>(bannedInChannels), userInfoMap -> {
            // Result may contain null values in case of errors, so filter out
            bannedInChannelsNames.addAll(userInfoMap.values().stream().filter(Objects::nonNull).map(u -> u.displayName).collect(Collectors.toSet()));
            reportDone.run();
        });
    }

    public static LowTrustUserMessageData decode(String topic, String message, Map<String, String> userIds) throws ParseException {
        String stream = Helper.getStreamFromTopic(topic, userIds);
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(message);
        String msgType = JSONUtil.getString(root, "type");
        JSONObject data = (JSONObject) root.get("data");

        if (msgType.equals("low_trust_user_new_message")) {
            JSONObject lowTrustUser = (JSONObject) data.get("low_trust_user");

            JSONObject updatedBy = (JSONObject) lowTrustUser.get("updated_by");
            String moderatorUsername = JSONUtil.getString(updatedBy, "login");
            
            Set<Type> userTypes = getTypes(JSONUtil.getStringList(lowTrustUser, "types"));
            BanEvasionEvaluation evaluation = null;
            String evaluationString = JSONUtil.getString(lowTrustUser, "ban_evasion_evaluation");

            try {
                evaluation = BanEvasionEvaluation.valueOf(evaluationString);
            } catch (IllegalArgumentException e) {
                LOGGER.warning(String.format("FIXME: Unhandled low trust user evaluation %s", evaluationString));
            }

            Treatment treatment = Treatment.UNKNOWN;
            String treatmentString = JSONUtil.getString(lowTrustUser, "treatment");

            try {
                treatment = Treatment.valueOf(treatmentString);
            } catch (IllegalArgumentException e) {
                LOGGER.warning(String.format("FIXME: Unhandled low trust user treatment %s", treatmentString));
            }

            Set<String> bannedInChannels = new HashSet<>();
            List<String> sharedBanChannelIds = JSONUtil.getStringList(lowTrustUser, "shared_ban_channel_ids");
            if (sharedBanChannelIds != null) {
                bannedInChannels.addAll(sharedBanChannelIds);
            }
            
            return new LowTrustUserMessageData(
                    topic,
                    message,
                    stream,
                    JSONUtil.getString((JSONObject) lowTrustUser.get("sender"), "login"),
                    JSONUtil.getString(data, "message_id"),
                    moderatorUsername,
                    userTypes,
                    evaluation,
                    treatment,
                    bannedInChannels,
                    JSONUtil.getString((JSONObject) data.get("message_content"), "text")
            );
        }

        return null;
    }
    
    public static Set<Type> getTypes(List<String> data) {
        Set<Type> userTypes = new HashSet<>();
        for (String s : data) {
            try {
                userTypes.add(Type.valueOf(s));
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
        
        for (LowTrustUserMessageData.Type userType : userTypes) {
            String str = userType.description;

            if (userType == LowTrustUserMessageData.Type.BANNED_IN_SHARED_CHANNEL
                    && !bannedInChannelsNames.isEmpty()) {
                str += ": " + String.join("/", bannedInChannelsNames);
            }

            elements.add(str);
        }
        
        if (userTypes.contains(Type.MANUALLY_ADDED) && moderatorUsername != null) {
            elements.add("@" + moderatorUsername);
        }

        if (banEvasionEvaluation != null
                && banEvasionEvaluation != LowTrustUserMessageData.BanEvasionEvaluation.UNLIKELY_EVADER) {
            elements.add(banEvasionEvaluation.description);
        }

        return elements.stream().filter(Objects::nonNull).collect(Collectors.joining(", "));
    }
    
}
