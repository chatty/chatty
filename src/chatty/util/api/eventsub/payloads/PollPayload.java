
package chatty.util.api.eventsub.payloads;

import chatty.util.DateTime;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.eventsub.Message;
import chatty.util.api.eventsub.Payload;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class PollPayload extends Payload {

    public final String stream;
    public final String title;
    public final List<Choice> choices;
    public final int bitsVotingAmount;
    public final int pointsVotingAmount;
    public final long endsAt;
    public final String status;
    
    public PollPayload(String stream, String title, List<Choice> choices, int bitsVotingAmount, int pointsVotingAmount, long endsAt, String status) {
        this.stream = stream;
        this.title = title;
        this.choices = choices;
        this.bitsVotingAmount = bitsVotingAmount;
        this.pointsVotingAmount = pointsVotingAmount;
        this.endsAt = endsAt;
        this.status = status;
    }
    
    public static class Choice {
        
        public final String id;
        public final String title;
        public final int bitsVotes;
        public final int pointsVotes;
        public final int votes;
        
        public Choice(String id, String title, int bitsVotes, int pointsVotes, int votes) {
            this.id = id;
            this.title = title;
            this.bitsVotes = bitsVotes;
            this.pointsVotes = pointsVotes;
            this.votes = votes;
        }
        
    }
    
    public static PollPayload decode(JSONObject payload) {
        JSONObject event = (JSONObject) payload.get("event");
        if (event != null) {
            String stream = JSONUtil.getString(event, "broadcaster_user_login");
            String title = JSONUtil.getString(event, "title");
            
            List<Choice> choices = new ArrayList<>();
            JSONArray choicesData = (JSONArray) event.get("choices");
            for (Object o : choicesData) {
                choices.add(decodeChoice((JSONObject) o));
            }
            int bitsVotingAmount = getVotingSetting((JSONObject) event.get("bits_voting"));
            int pointsVotingAmount = getVotingSetting((JSONObject) event.get("channel_points_voting"));
            long endsAt = JSONUtil.getDatetime(event, "ends_at", -1);
            String status = JSONUtil.getString(event, "status", "");
            return new PollPayload(stream, title, choices, bitsVotingAmount, pointsVotingAmount, endsAt, status);
        }
        return null;
    }
    
    private static Choice decodeChoice(JSONObject data) {
        String title = JSONUtil.getString(data, "title");
        int bitsVotes = JSONUtil.getInteger(data, "bits_votes", -1);
        int pointsVotes = JSONUtil.getInteger(data, "channel_points_votes", -1);
        int votes = JSONUtil.getInteger(data, "votes", -1);
        return new Choice(title, title, bitsVotes, pointsVotes, votes);
    }
    
    private static int getVotingSetting(JSONObject data) {
        if (JSONUtil.getBoolean(data, "is_enabled", false)) {
            return JSONUtil.getInteger(data, "amount_per_vote", -1);
        }
        return -1;
    }
    
    public static String getPollMessage(Message msg) {
        if (!(msg.data instanceof PollPayload)) {
            return null;
        }
        PollPayload data = (PollPayload) msg.data;
        if ("archived".equals(data.status)) {
            return null;
        }
        
        String prefix = "[Poll]";
        String duration = "";
        switch (msg.subType) {
            case "channel.poll.begin":
                prefix = "[Poll Start]";
                duration = " (ends in "+DateTime.duration(data.endsAt - System.currentTimeMillis())+")";
                break;
            case "channel.poll.end":
                prefix = "[Poll End]";
                break;
        }
        return String.format("%s %s - %s%s",
                prefix,
                data.title,
                getChoices(data),
                duration);
    }
    
    private static String getChoices(PollPayload data) {
        StringBuilder b = new StringBuilder();
        int totalVotes = 0;
        for (Choice choice : data.choices) {
            if (choice.votes > 0) {
                totalVotes += choice.votes;
            }
        }
        for (Choice choice : data.choices) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(choice.title);
            if (choice.votes >= 0) {
                b.append(" (");
                b.append(choice.votes);
                if (totalVotes > 0) {
                    b.append(" / ");
                    b.append(String.format("%.0f%%", (choice.votes / (float) totalVotes)*100));
                }
                b.append(")");
            }
        }
        return b.toString();
    }
    
}
