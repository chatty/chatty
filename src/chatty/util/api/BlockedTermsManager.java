
package chatty.util.api;

import chatty.util.DateTime;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class BlockedTermsManager {
    
    private static final Logger LOGGER = Logger.getLogger(BlockedTermsManager.class.getName());

    private final Map<String, BlockedTerms> cache = new HashMap<>();
    private final Requests requests;
    
    private String currentLogin;
    private BlockedTerms currentTerms;
    private Consumer<BlockedTerms> currentListener;
    private int currentRequestCount;
    
    public static final int MAX_PARTIAL_REQUESTS = 10;
    public static final int MAX_RESULTS_PER_REQUEST = 100;
    public static final int MAX_RESULTS = MAX_PARTIAL_REQUESTS * MAX_RESULTS_PER_REQUEST;
    
    public BlockedTermsManager(Requests requests) {
        this.requests = requests;
    }
    
    public void getBlockedTerms(String streamId, String streamName, boolean refresh, Consumer<BlockedTerms> listener) {
        BlockedTerms cached = null;
        synchronized(cache) {
            cached = cache.get(streamName);
        }
        if (refresh || cached == null) {
            synchronized(cache) {
                currentLogin = streamName;
                currentTerms = null;
                currentListener = listener;
                currentRequestCount = 1;
            }
            requests.getBlockedTerms(streamId, streamName, null);
        }
        else {
            listener.accept(cached);
        }
    }

    public void resultReceived(String streamId, String streamName, String json, int responseCode) {
        BlockedTerms result = getResult(streamId, streamName, json, responseCode);
        Consumer<BlockedTerms> listener = null;
        synchronized(cache) {
            listener = currentListener;
        }
        if (listener != null && result != null) {
            listener.accept(result);
        }
    }
    
    private BlockedTerms getResult(String streamId, String streamName, String json, int responseCode) {
        synchronized(cache) {
            if (currentLogin == null || !currentLogin.equals(streamName)) {
                return null;
            }
            BlockedTerms parsed = BlockedTerms.parse(json, streamId, streamName);
            if (parsed != null) {
                //--------------------------
                // Successful request
                //--------------------------
                // Combine with previous partial request if present
                if (currentTerms != null) {
                    parsed = parsed.combine(currentTerms);
                }
                String cursor = Requests.getCursor(json);
                boolean requestCountCheck = currentRequestCount < MAX_PARTIAL_REQUESTS;
                if (cursor != null && requestCountCheck) {
                    // Next partial request
                    currentTerms = parsed;
                    currentRequestCount++;
                    requests.getBlockedTerms(streamId, streamName, cursor);
                }
                else {
                    // Requests finished, return data
                    if (cursor != null && !requestCountCheck) {
                        LOGGER.warning("Blocked Terms request limit reached");
                    }
                    currentTerms = null;
                    currentLogin = null;
                    cache.put(streamName, parsed);
                    return parsed;
                }
            }
            else {
                //--------------------------
                // Failed request
                //--------------------------
                String errorText = "Error requesting data";
                if (responseCode == 403) {
                    errorText = "Access denied";
                }
                BlockedTerms errorTerms = new BlockedTerms(streamId, streamName, errorText);
                // Cache error as well so it doesn't retry every time (can do
                // that manually if an error occured where that makes sense)
                cache.put(streamName, errorTerms);
                return errorTerms;
            }
        }
        return null;
    }
    
    public static class BlockedTerms {
        
        public final String streamId;
        public final String streamName;
        public final List<BlockedTerm> data;
        public final long createdAt;
        public final String error;
        
        public BlockedTerms(String streamId, String streamName, List<BlockedTerm> data) {
            this.streamId = streamId;
            this.streamName = streamName;
            this.data = data;
            this.createdAt = System.currentTimeMillis();
            this.error = null;
        }
        
        public BlockedTerms(String streamId, String streamName, String error) {
            this.streamId = streamId;
            this.streamName = streamName;
            this.data = null;
            this.createdAt = System.currentTimeMillis();
            this.error = error;
        }
        
        public boolean hasError() {
            return error != null;
        }
        
        public BlockedTerms combine(BlockedTerms other) {
            List<BlockedTerm> newData = new ArrayList<>();
            newData.addAll(data);
            newData.addAll(other.data);
            return new BlockedTerms(streamId, streamName, newData);
        }
        
        public static BlockedTerms parse(String json, String streamId, String streamName) {
            if (json == null) {
                return null;
            }
            try {
                List<BlockedTerm> terms = new ArrayList<>();
                JSONParser parser = new JSONParser();
                JSONObject root = (JSONObject) parser.parse(json);
                JSONArray data = (JSONArray) root.get("data");
                for (Object o : data) {
                    if (o instanceof JSONObject) {
                        JSONObject entry = (JSONObject) o;
                        BlockedTerm blockedTerm = BlockedTerm.parse(entry, streamName);
                        if (blockedTerm != null) {
                            terms.add(blockedTerm);
                        }
                    }
                }
                return new BlockedTerms(streamId, streamName, terms);
            }
            catch (Exception ex) {
                LOGGER.warning("Error parsing blocked terms: "+ex);
            }
            return null;
        }
        
        @Override
        public String toString() {
            return String.valueOf(data);
        }
        
    }
    
    public static class BlockedTerm {
        
        public final String id;
        public final long createdAt;
        public final long updatedAt;
        public final long expiresAt;
        public final String text;
        public final String moderatorId;
        public final String streamId;
        public final String streamLogin;
        
        public BlockedTerm(String id, long createdAt, long updatedAt, long expiresAt, String text, String moderatorId, String streamId, String streamLogin) {
            this.id = id;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.expiresAt = expiresAt;
            this.text = text;
            this.moderatorId = moderatorId;
            this.streamId = streamId;
            this.streamLogin = streamLogin;
        }
        
//        public BlockedTerm(String text, String streamLogin) {
//            this.id = null;
//            this.createdAt = System.currentTimeMillis();
//            this.updatedAt = -1;
//            this.expiresAt = -1;
//            this.text = text;
//            this.moderatorId = null;
//            this.streamId = null;
//            this.streamLogin = streamLogin;
//        }
        
        public static BlockedTerm parse(JSONObject data, String streamLogin) {
            String id = JSONUtil.getString(data, "id");
            long createdAt = JSONUtil.getDatetime(data, "created_at", -1);
            long updatedAt = JSONUtil.getDatetime(data, "updated_at", -1);
            long expiresAt = JSONUtil.getDatetime(data, "expires_at", -1);
            String text = JSONUtil.getString(data, "text");
            String moderatorId = JSONUtil.getString(data, "moderator_id");
            String streamId = JSONUtil.getString(data, "broadcaster_id");
            if (!StringUtil.isNullOrEmpty(id, text, streamId)) {
                return new BlockedTerm(id, createdAt, updatedAt, expiresAt, text, moderatorId, streamId, streamLogin);
            }
            return null;
        }
        
        @Override
        public String toString() {
            return text;
        }
        
    }
    
}
