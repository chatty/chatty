
package chatty.util.api;

import chatty.util.DateTime;
import chatty.util.JSONUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
public class ChannelInfoManager {
    
    private static final Logger LOGGER = Logger.getLogger(ChannelInfoManager.class.getName());
    
    private final TwitchApi api;
    private final TwitchApiResultListener listener;
    
    public ChannelInfoManager(TwitchApi api, TwitchApiResultListener listener) {
        this.api = api;
        this.listener = listener;
    }
    
    /**
     * Handle the ChannelInfo request result, which can also be from changing
     * the channel info.
     * 
     * @param result The JSON received
     * @param responseCode The HTTP response code of the request
     */
    protected void handleChannelInfoResult(boolean put, String result,
            int responseCode, String stream) {
        // Handle requested ChannelInfo but also the result of changing
        // channel info, since that returns ChannelInfo as well.
        if (result == null || responseCode != 200) {
            handleChannelInfoResultError(stream, put, responseCode);
            return;
        }
        // Request should have gone through fine
        ChannelInfo info = parseChannelInfo(result);
        if (info == null) {
            LOGGER.warning("Error parsing channel info: " + result);
            handleChannelInfoResultError(stream, put, responseCode);
            return;
        }
        if (put) {
            listener.putChannelInfoResult(TwitchApi.RequestResultCode.SUCCESS);
        }
        listener.receivedChannelInfo(stream, info, TwitchApi.RequestResultCode.SUCCESS);
    }
    
    /**
     * Handle the error of a ChannelInfo request result, this can be from
     * changing the channel info as well. Handle by logging the error as well
     * as informing the client who can inform the user on the GUI.
     * 
     * @param type
     * @param responseCode 
     */
    private void handleChannelInfoResultError(String stream, boolean put, int responseCode) {
        if (!put) {
            if (responseCode == 404) {
                listener.receivedChannelInfo(stream, null, TwitchApi.RequestResultCode.NOT_FOUND);
            } else {
                listener.receivedChannelInfo(stream, null, TwitchApi.RequestResultCode.FAILED);
            }
        } else {
            // The result of changing channel info requires some extra
            // handling, because it can have different response codes.
            if (responseCode == 404) {
                listener.putChannelInfoResult(TwitchApi.RequestResultCode.NOT_FOUND);
            } else if (responseCode == 401 || responseCode == 403) {
                LOGGER.warning("Error setting channel info: Access denied");
                listener.putChannelInfoResult(TwitchApi.RequestResultCode.ACCESS_DENIED);
                api.accessDenied();
            } else if (responseCode == 422) {
                LOGGER.warning("Error setting channel info: Probably invalid title");
                listener.putChannelInfoResult(TwitchApi.RequestResultCode.INVALID_STREAM_STATUS);
            } else {
                LOGGER.warning("Error setting channel info: Unknown error (" + responseCode + ")");
                listener.putChannelInfoResult(TwitchApi.RequestResultCode.FAILED);
            }
        }
    }
    
    /**
     * Parses the channel info returned from the Twitch API into a ChannelInfo
     * object.
     * 
     * @param json
     * @return 
     */
    private ChannelInfo parseChannelInfo(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            String name = (String)root.get("name");
            String id = (String)root.get("_id");
            String status = (String)root.get("status");
            String game = (String)root.get("game");
            String broadcaster_type = (String)root.get("broadcaster_type");
            String description = (String)root.get("description");
            int views = JSONUtil.getInteger(root, "views", -1);
            int followers = JSONUtil.getInteger(root, "followers", -1);
            long createdAt = -1;
            long updatedAt = -1;
            try {
                createdAt = DateTime.parseDatetime(JSONUtil.getString(root, "created_at"));
                updatedAt = DateTime.parseDatetime(JSONUtil.getString(root, "updated_at"));
            } catch (Exception ex) {
                LOGGER.warning("Error parsing ChannelInfo: "+ex);
            }
            return new ChannelInfo(name, id, createdAt, followers,
                    views, updatedAt, broadcaster_type, description);
        }
        catch (ParseException ex) {
            LOGGER.warning("Error parsing ChannelInfo.");
            return null;
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing ChannelInfo: Unexpected type");
            return null;
        }
        
    }
    
    /**
     * Turns a ChannelInfo object to JOSN to send it to the API.
     * 
     * @param info The ChannelInfo object
     * @return The created JSON
     */
    protected String makeChannelInfoJson(ChannelStatus info) {
        Map<String, String> channel = new HashMap<>();
        channel.put("status", info.title);
        channel.put("game", info.category.name);
        return JSONUtil.listMapToJSON("channel", channel);
    }
    
}
