
package chatty.util.api.eventsub.payloads;

import chatty.util.JSONUtil;
import chatty.util.api.eventsub.Payload;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class SessionPayload extends Payload {

    public final String id;
    public final String status;
    public final long connectedAt;
    public final int keepAliveTimeout;
    public final String reconnectUrl;
    
    private SessionPayload(String id, String status, long connectedAt, int keepAliveTimeout, String reconnectUrl) {
        this.id = id;
        this.status = status;
        this.connectedAt = connectedAt;
        this.keepAliveTimeout = keepAliveTimeout;
        this.reconnectUrl = reconnectUrl;
    }
    
    public static SessionPayload decode(JSONObject payload) {
        JSONObject data = (JSONObject) payload.get("session");
        String id = JSONUtil.getString(data, "id");
        String status = JSONUtil.getString(data, "status");
        int keepAliveTimeout = JSONUtil.getInteger(data, "keepalive_timeout_seconds", -1);
        String reconnectUrl = JSONUtil.getString(data, "reconnect_url");
        long connectedAt = JSONUtil.getDatetime(data, "connected_at", -1);
        return new SessionPayload(id, status, connectedAt, keepAliveTimeout, reconnectUrl);
    }
    
    
}
