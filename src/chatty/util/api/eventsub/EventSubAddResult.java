
package chatty.util.api.eventsub;

import chatty.util.JSONUtil;
import chatty.util.api.queue.ResultListener.Result;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class EventSubAddResult {
    
    private static final Logger LOGGER = Logger.getLogger(EventSubAddResult.class.getName());

    public final String id;
    public final String type;
    public final String status;
    public final int cost;
    public final int totalSubs;
    public final int totalCost;
    public final int maxTotalCost;
    public final int responseCode;
    public final String errorMsg;
    public final boolean hasError;
    
    private EventSubAddResult(String id, String type, String status, int totalSubs, int cost, int totalCost, int maxTotalCost, int responseCode, String errorMsg) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.totalSubs = totalSubs;
        this.cost = cost;
        this.totalCost = totalCost;
        this.maxTotalCost = maxTotalCost;
        this.responseCode = responseCode;
        this.errorMsg = errorMsg;
        this.hasError = type == null;
    }
    
    public static EventSubAddResult decode(Result r) {
        if (r.text == null) {
            return new EventSubAddResult(null, null, null, -1, -1, -1, -1, r.responseCode, r.errorText);
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(r.text);
            JSONObject data = (JSONObject) ((JSONArray) root.get("data")).get(0);
            String id = JSONUtil.getString(data, "id");
            String status = JSONUtil.getString(data, "status");
            String type = JSONUtil.getString(data, "type");
            int cost = JSONUtil.getInteger(data, "cost", -1);
            int totalSubs = JSONUtil.getInteger(root, "total", 0);
            int totalCost = JSONUtil.getInteger(root, "total_cost", 0);
            int maxTotalCost = JSONUtil.getInteger(root, "max_total_cost", 0);
            return new EventSubAddResult(id, type, status, totalSubs, cost, totalCost, maxTotalCost, r.responseCode, r.errorText);
        }
        catch (Exception ex) {
            LOGGER.warning("Failed parsing EventSubAdd result: "+ex);
        }
        return new EventSubAddResult(null, null, null, -1, -1, -1, -1, r.responseCode, r.errorText);
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s/subs:%d,cost:+%d/%d/%d]",
                type, status, totalSubs, cost, totalCost, maxTotalCost);
    }
    
}
