package chatty.util.api.pubsub;

import chatty.util.JSONUtil;
import chatty.util.api.TwitchApi;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LowTrustUserMessageData extends MessageData {
    private static final Logger LOGGER = Logger.getLogger(LowTrustUserMessageData.class.getName());

    public final String stream;
    public final String username;
    public final String aboutMessageId;
    public final Set<Type> userTypes;
    public final BanEvasionEvaluation evaluation;
    public final Treatment treatment;
    public final Set<Long> bannedInChannels;
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
        RESTRICTED("restricted");

        public final String description;

        Treatment(String description) {
            this.description = description;
        }
    }

    public enum Type {
        //FIXME find out what the other values here are
        BANNED_IN_SHARED_CHANNEL("banned in shared mod channels"),
        MANUALLY_ADDED("added manually"),
        DETECTED_BAN_EVADER("detected as a ban evader");

        public final String description;

        Type(String description) {
            this.description = description;
        }
    }

    public LowTrustUserMessageData(String topic, String message, String stream, String username, String aboutMessageId, Set<Type> userTypes, BanEvasionEvaluation evaluation, Treatment treatment, Set<Long> bannedInChannels, String text) {
        super(topic, message);
        this.stream = stream;
        this.username = username;
        this.aboutMessageId = aboutMessageId;
        this.userTypes = userTypes;
        this.evaluation = evaluation;
        this.treatment = treatment;
        this.bannedInChannels = bannedInChannels;
        this.text = text;
    }
    
    public void fetchUserInfoForBannedChannels(TwitchApi api, final Consumer<Object> reportDone) {
        if (bannedInChannels.isEmpty()) {
            reportDone.accept(null);
            return;
        }
        
        api.getCachedUserInfoById(new ArrayList<>(bannedInChannels), longUserInfoMap -> {
            bannedInChannelsNames.addAll(longUserInfoMap.values().stream().map(ui -> ui.displayName).collect(Collectors.toSet()));
            reportDone.accept(null);
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

            Set<Type> userTypes = new HashSet<>();
            for (String s : Objects.requireNonNull(JSONUtil.getStringList(lowTrustUser, "types"))) {
                try {
                    userTypes.add(Type.valueOf(s));
                } catch (IllegalArgumentException e) {
                    LOGGER.warning(String.format("FIXME: Unhandled low trust user type %s", s));
                }
            }

            BanEvasionEvaluation evaluation = null;
            String evaluationString = JSONUtil.getString(lowTrustUser, "ban_evasion_evaluation");

            try {
                evaluation = BanEvasionEvaluation.valueOf(evaluationString);
            } catch (IllegalArgumentException e) {
                LOGGER.warning(String.format("FIXME: Unhandled low trust user evaluation %s", evaluationString));
            }

            Treatment treatment = null;
            String treatmentString = JSONUtil.getString(lowTrustUser, "treatment");

            try {
                treatment = Treatment.valueOf(treatmentString);
            } catch (IllegalArgumentException e) {
                LOGGER.warning(String.format("FIXME: Unhandled low trust user treatment %s", treatmentString));
            }

            Set<Long> bannedInChannels = new HashSet<>();
            List<String> sharedBanChannelIds = JSONUtil.getStringList(lowTrustUser, "shared_ban_channel_ids");
            if (sharedBanChannelIds != null) {
                bannedInChannels.addAll(sharedBanChannelIds
                    .stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toSet())
                );
            }

            return new LowTrustUserMessageData(
                topic,
                message,
                stream,
                JSONUtil.getString((JSONObject) lowTrustUser.get("sender"), "login"),
                JSONUtil.getString(data, "message_id"),
                userTypes,
                evaluation,
                treatment,
                bannedInChannels,
                JSONUtil.getString((JSONObject) data.get("message_content"), "text")
            );
        }

        return null;
    }
}
