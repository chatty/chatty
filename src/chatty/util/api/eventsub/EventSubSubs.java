
package chatty.util.api.eventsub;

import chatty.util.JSONUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * A list of current EventSub topics requested from the API, mainly for
 * debugging at this point.
 * 
 * @author tduva
 */
public class EventSubSubs {

    private static final Logger LOGGER = Logger.getLogger(EventSubSubs.class.getName());
    
    public final int total;
    public final int totalCost;
    public final int maxTotalCost;
    public final List<Sub> subs;
    
    public EventSubSubs(int total, int totalCost, int maxTotalCost, List<Sub> subs) {
        this.total = total;
        this.totalCost = totalCost;
        this.maxTotalCost = maxTotalCost;
        this.subs = subs;
    }
    
    public void addResult(EventSubSubs subs) {
        this.subs.addAll(subs.subs);
    }
    
    public static EventSubSubs decode(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            int total = JSONUtil.getInteger(root, "total", -1);
            int totalCost = JSONUtil.getInteger(root, "total_cost", -1);
            int maxTotalCost = JSONUtil.getInteger(root, "max_total_cost", -1);
            
            List<Sub> subs = new ArrayList<>();
            JSONArray data = (JSONArray) root.get("data");
            for (Object o : data) {
                JSONObject entry = (JSONObject) o;
                subs.add(new Sub(entry));
            }
            return new EventSubSubs(total, totalCost, maxTotalCost, subs);
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing EventSub subs: "+ex);
        }
        return null;
    }
    
    private static class Sub {
        
        public final String id;
        public final String status;
        public final String type;
        public final String version;
        public final String broadcasterId;
        public final String createdAt;
        public final String method;
        public final String sessionId;
        public final int cost;
        
        public Sub(JSONObject data) {
            id = JSONUtil.getString(data, "id");
            status = JSONUtil.getString(data, "status");
            type = JSONUtil.getString(data, "type");
            JSONObject condition = (JSONObject) data.get("condition");
            broadcasterId = JSONUtil.getString(condition, "broadcaster_user_id");
            version = JSONUtil.getString(data, "version");
            JSONObject transport = (JSONObject) data.get("transport");
            method = JSONUtil.getString(transport, "method", "");
            if (method.equals("websocket")) {
                sessionId = JSONUtil.getString(transport, "session_id");
            }
            else {
                sessionId = null;
            }
            createdAt = JSONUtil.getString(data, "created_at");
            cost = JSONUtil.getInteger(data, "cost", -1);
        }
        
    }
    
    public Map<String, List<Sub>> getBySession() {
        Map<String, List<Sub>> result = new HashMap<>();
        Set<String> ids = new HashSet<>();
        for (Sub sub : subs) {
            if (!ids.add(sub.id)) {
                System.out.println("Duped id: "+sub.id);
                continue;
            }
            if (!result.containsKey(sub.sessionId)) {
                result.put(sub.sessionId, new ArrayList<>());
            }
            result.get(sub.sessionId).add(sub);
        }
        return result;
    }
    
    public Map<String, Integer> getCountBySession() {
        Map<String, List<Sub>> bySession = getBySession();
        Map<String, Integer> count = new HashMap<>();
        for (Map.Entry<String, List<Sub>> entry : bySession.entrySet()) {
            count.put(entry.getKey(), entry.getValue().size());
        }
        return count;
    }
    
    @Override
    public String toString() {
        Map<String, List<Sub>> bySession = getBySession();
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, List<Sub>> entry : bySession.entrySet()) {
            b.append("\n[").append(entry.getKey()).append("]").append("\n");
            List<Sub> sorted = new ArrayList<>();
            sorted.addAll(entry.getValue());
            Collections.sort(sorted, (o1, o2) -> {
                         if (o1.broadcasterId == o2.broadcasterId) {
                             return 0;
                         }
                         if (o1.broadcasterId == null && o2.broadcasterId != null) {
                             return -1;
                         }
                         if (o2.broadcasterId == null) {
                             return 1;
                         }
                         return o1.broadcasterId.compareTo(o2.broadcasterId);
                     });
            String currentId = null;
            int count = 0;
            for (Sub sub : sorted) {
                if (currentId != null && !currentId.equals(sub.broadcasterId)) {
                    b.append(count);
                    count = 0;
                    b.append("\n---\n");
                }
                count++;
                currentId = sub.broadcasterId;
                b.append(sub.broadcasterId).append(" ");
                b.append(sub.type);
                b.append("\n");
            }
        }
        return b.toString();
    }
    
}
