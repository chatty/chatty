
package chatty.util.api.pubsub;

import chatty.util.JSONUtil;
import chatty.util.api.pubsub.LowTrustUserMessageData.BanEvasionEvaluation;
import chatty.util.api.pubsub.LowTrustUserMessageData.Treatment;
import chatty.util.api.pubsub.LowTrustUserMessageData.Type;
import static chatty.util.api.pubsub.LowTrustUserMessageData.getTypes;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LowTrustUserUpdateData extends MessageData {
    
    private static final Logger LOGGER = Logger.getLogger(LowTrustUserUpdateData.class.getName());

    public final String stream;
    public final String targetUsername;
    public final String moderatorUsername;
    public final Set<Type> userTypes;
    public final BanEvasionEvaluation banEvasionEvaluation;
    public final Treatment treatment;

    public LowTrustUserUpdateData(String topic,
                                   String message,
                                   String stream,
                                   String targetUsername,
                                   String moderatorUsername,
                                   Set<Type> userTypes,
                                   BanEvasionEvaluation evaluation,
                                   Treatment treatment) {
        super(topic, message);
        this.stream = stream;
        this.targetUsername = targetUsername;
        this.moderatorUsername = moderatorUsername;
        this.userTypes = userTypes;
        this.banEvasionEvaluation = evaluation;
        this.treatment = treatment;
    }
    
    public static LowTrustUserUpdateData decode(String topic, String message, Map<String, String> userIds) throws ParseException {
        String stream = Helper.getStreamFromTopic(topic, userIds);
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(message);
        String msgType = JSONUtil.getString(root, "type");
        JSONObject data = (JSONObject) root.get("data");

        if (msgType.equals("low_trust_user_treatment_update")) {
            String targetUsername = JSONUtil.getString(data, "target_user");
            JSONObject updatedBy = (JSONObject) data.get("updated_by");
            String moderatorUsername = JSONUtil.getString(updatedBy, "login");
            
            Set<Type> userTypes = getTypes(JSONUtil.getStringList(data, "types"));

            BanEvasionEvaluation evaluation = null;
            String evaluationString = JSONUtil.getString(data, "ban_evasion_evaluation");

            try {
                evaluation = BanEvasionEvaluation.valueOf(evaluationString);
            } catch (IllegalArgumentException e) {
                LOGGER.warning(String.format("FIXME: Unhandled low trust user evaluation %s", evaluationString));
            }

            Treatment treatment = null;
            String treatmentString = JSONUtil.getString(data, "treatment");

            try {
                treatment = Treatment.valueOf(treatmentString);
            } catch (IllegalArgumentException e) {
                LOGGER.warning(String.format("FIXME: Unhandled low trust user treatment %s", treatmentString));
            }

            return new LowTrustUserUpdateData(
                topic,
                message,
                stream,
                targetUsername,
                moderatorUsername,
                userTypes,
                evaluation,
                treatment
            );
        }

        return null;
    }
    
    public String makeInfo() {
        List<String> elements = new ArrayList<>();

        if (treatment != null) {
            elements.add(LowTrustUserMessageData.getTreatmentShort(treatment));
        }
        
        for (LowTrustUserMessageData.Type userType : userTypes) {
            String str = userType.description;
            elements.add(str);
        }
        
        if ((userTypes.contains(Type.MANUALLY_ADDED) || userTypes.isEmpty())
                && moderatorUsername != null) {
            elements.add("@" + moderatorUsername);
        }

        if (banEvasionEvaluation != null
                && banEvasionEvaluation != LowTrustUserMessageData.BanEvasionEvaluation.UNLIKELY_EVADER) {
            elements.add(banEvasionEvaluation.description);
        }
        
        return "Updated to: " + elements.stream().filter(Objects::nonNull).collect(Collectors.joining(", "));
    }
    
}
