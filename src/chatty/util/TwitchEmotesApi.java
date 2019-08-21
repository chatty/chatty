
package chatty.util;

import chatty.Helper;
import static chatty.util.CachedBulkManager.ASAP;
import static chatty.util.CachedBulkManager.RETRY;
import chatty.util.CachedBulkManager.Result;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticons;
import chatty.util.api.TwitchApi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
public class TwitchEmotesApi {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchEmotesApi.class.getName());
    
    public static final TwitchEmotesApi api = new TwitchEmotesApi();
    private TwitchApi twitchApi;
    
    private final CachedBulkManager<Integer, EmotesetInfo> byId;
    private final CachedBulkManager<String, Set<EmotesetInfo>> byStream;
    private final CachedBulkManager<Integer, EmotesetInfo> bySet;
    
    public TwitchEmotesApi() {
        byId = new CachedBulkManager<>(new CachedBulkManager.Requester<Integer, EmotesetInfo>() {

            @Override
            public void request(CachedBulkManager<Integer, EmotesetInfo> manager, Set<Integer> asap, Set<Integer> normal, Set<Integer> backlog) {
                Debugging.println("emoteinfo", "byId: %s %s %s", asap, normal, backlog);
                Set<Integer> toRequest = manager.makeAndSetRequested(asap, normal, backlog, 50);
                
                String url = "https://api.twitchemotes.com/api/v4/emotes?id="+StringUtil.join(toRequest, ",");
                UrlRequest request = new UrlRequest(url);
                request.async((result, responseCode) -> {
                    // TEST ------------------------------
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(TwitchEmotesApi.class.getName()).log(Level.SEVERE, null, ex);
//                    }
                    // -----------------------------------
                    if (responseCode == 404) {
                        manager.setNotFound(toRequest);
                    } else if (result == null) {
                        manager.setError(toRequest);
                    } else {
                        Map<Integer, EmotesetInfo> emotes = parseById(result);
                        if (emotes == null) {
                            manager.setError(toRequest);
                        } else {
                            Set<Integer> notFound = new HashSet<>(toRequest);
                            notFound.removeAll(emotes.keySet());
                            manager.setNotFound(notFound);
                            manager.setResult(emotes);
                        }
                    }
                });
            }
        }, CachedBulkManager.DAEMON);
        
        byStream = new CachedBulkManager<>(new CachedBulkManager.Requester<String, Set<EmotesetInfo>>() {

            @Override
            public void request(CachedBulkManager<String, Set<EmotesetInfo>> manager, Set<String> asap, Set<String> normal, Set<String> backlog) {
                Debugging.println("emoteinfo", "byStream: %s %s %s", asap, normal, backlog);
                String stream;
                if (!asap.isEmpty()) {
                    stream = asap.iterator().next();
                } else {
                    stream = normal.iterator().next();
                }
                manager.setRequested(stream);
                
                String url = "https://api.twitchemotes.com/api/v4/channels/"+stream;
                UrlRequest request = new UrlRequest(url);
                
                request.async((result, responseCode) -> {
                    // TEST -------------------------------------
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(TwitchEmotesApi.class.getName()).log(Level.SEVERE, null, ex);
//                    }
                    // ------------------------------------------
                    if (responseCode == 404) {
                        manager.setNotFound(stream);
                    } else if (result == null) {
                        manager.setError(stream);
                    } else {
                        Map<Integer, EmotesetInfo> emotes = parseByStream(result);
                        if (emotes == null) {
                            manager.setError(stream);
                        } else {
                            Set<EmotesetInfo> sets = new HashSet<>(emotes.values());
                            System.out.println(sets);
                            // Set other managers first, in case data from those
                            // is required in the result listener of this
                            byId.setResult(emotes);
                            for (EmotesetInfo set : sets) {
                                bySet.setResult(set.emoteset_id, set);
                            }
                            manager.setResult(stream, sets);
                        }
                    }
                });
            }
        }, CachedBulkManager.DAEMON);
        
        bySet = new CachedBulkManager<>(new CachedBulkManager.Requester<Integer, EmotesetInfo>() {

            @Override
            public void request(CachedBulkManager<Integer, EmotesetInfo> manager, Set<Integer> asap, Set<Integer> normal, Set<Integer> backlog) {
                Debugging.println("emoteinfo", "bySet: %s %s %s", asap, normal, backlog);
                Set<Integer> toRequest = manager.makeAndSetRequested(asap, normal, backlog, 100);
                
                String url = "https://api.twitchemotes.com/api/v4/sets?id="+StringUtil.join(toRequest, ",");
                UrlRequest request = new UrlRequest(url);

                request.async((result, responseCode) -> {
                    if (responseCode == 404) {
                        manager.setNotFound(toRequest);
                    } else if (result == null) {
                        manager.setError(toRequest);
                    } else {
                        Map<Integer, EmotesetInfo> sets = parseBySet(result);
                        if (sets == null) {
                            manager.setError(toRequest);
                        } else {
                            Set<Integer> notFound = new HashSet<>(toRequest);
                            notFound.removeAll(sets.keySet());
                            manager.setNotFound(notFound);
                            
                            // TEST -----------------------------
//                            for (Map.Entry<Integer, EmotesetInfo> entry : sets.entrySet()) {
//                                if (ThreadLocalRandom.current().nextInt(3) == 0) {
//                                    System.out.println("Error: "+entry.getKey());
//                                    manager.setError(entry.getKey());
//                                } else {
//                                    manager.setResult(entry.getKey(), entry.getValue());
//                                }
//                            }
                            // ------------------------------------
                            manager.setResult(sets);
                            // ------------------------------------
                        }
                    }
                });
            }
        }, CachedBulkManager.DAEMON);
    }
    
    public void setTwitchApi(TwitchApi api) {
        this.twitchApi = api;
    }
    
    //================
    // Get Emote Info
    //================
    
    /**
     * Get info for the given Emoticon. If request errored before, this might
     * call the listener immediately with a null value.
     * 
     * @param unique A previous request with the same Object will be overwritten
     * @param listener If no cached info available, this listener will be
     * notified with the request result, possibly null if an error occured
     * (optional)
     * @param emote The Emoticon to get the info for
     * @return 
     */
    public EmotesetInfo getInfoByEmote(Object unique, Consumer<EmotesetInfo> listener, Emoticon emote) {
        EmotesetInfo info;
        if (emote.type != Emoticon.Type.TWITCH
                || emote.numericId == Emoticon.ID_UNDEFINED) {
            return null;
        }
        if (emote.hasGlobalEmoteset()) {
            return null;
        }
        if (emote.emoteSet > Emoticon.SET_GLOBAL) {
            info = bySet.get(emote.emoteSet);
            if (info != null) {
                return info;
            }
        }
        info = byId.getOrQuerySingle(unique, result -> {
            if (listener != null) {
                listener.accept(result.get(emote.numericId));
            }
        }, ASAP, emote.numericId);
        return info;
    }
    
    private final Object BY_STREAM_UNIQUE = new Object();
    
    /**
     * Get by stream for the Emote Dialog "Channel" tab. Only one request can
     * be active at a time. Requested as soon as possible with no rety on error.
     * Also requests the emotes for the resulting emotesets if necessary.
     * 
     * @param listener The listener that will be called with the result
     * @param stream The stream name to request for
     */
    public void requestByStream(Consumer<Set<EmotesetInfo>> listener, String stream) {
        if (twitchApi == null) {
            return;
        }
        twitchApi.getUserIdAsap(result -> {
            if (!result.hasError()) {
                String streamId = result.getId(stream);
                byStream.query(BY_STREAM_UNIQUE, result2 -> {
                    Set<EmotesetInfo> data = result2.get(streamId);
                    if (data != null) {
                        listener.accept(data);
                        for (EmotesetInfo info : data) {
                            twitchApi.getEmotesBySets(info.emoteset_id);
                        }
                    }
                }, ASAP, streamId);
            }
        }, stream);
    }
    
    private final Object BY_STREAM_ID_UNIQUE = new Object();
    
    /**
     * Get by stream id for the Emote Dialog "Emote Details". Only one request
     * can be active at a time. Requested as soon as possible with no retry on
     * error.
     * 
     * @param listener The listener, receiving a Set of EmotesetInfo objects,
     * which may be null or empty
     * @param streamId A stream id
     */
    public void requestByStreamId(Consumer<Set<EmotesetInfo>> listener, String streamId) {
        byStream.query(BY_STREAM_ID_UNIQUE, result -> {
            Set<EmotesetInfo> data = result.get(streamId);
            listener.accept(data);
        }, ASAP, streamId);
    }
    
    private final Object BY_SETS_UNIQUE = new Object();
    
    /**
     * Get by sets for the Emote Dialog "My Emotes" tab. Retry missing errored
     * sets. Multiple partial results may be returned in case of errors. Only
     * one request can be active at a time.
     * 
     * Emotes for the set "0" are not requested.
     * 
     * @param listener The listener, receiving the result map, where some keys
     * may return null if errors occured
     * @param emotesets
     * @return The result, if already cached, or null
     */
    public Map<Integer, EmotesetInfo> requestBySets(Consumer<Map<Integer, EmotesetInfo>> listener, Set<Integer> emotesets) {
        Set<Integer> modifiedSets = new HashSet<>(emotesets);
        modifiedSets.remove(0);
        if (modifiedSets.isEmpty()) {
            return null;
        }
        
        // Get or request
        Result<Integer, EmotesetInfo> result = bySet.getOrQuery(BY_SETS_UNIQUE, result2 -> {
            listener.accept(result2.getResults());
        }, RETRY, modifiedSets);
        if (result != null) {
            return result.getResults();
        }
        return null;
    }
    
    /**
     * Helper function to compare a set of integer emotesets against
     * EmotesetInfo objects.
     *
     * @param accessTo
     * @param emotesets
     * @return true if all emotesets ids are in accessTo, false otherwise
     */
    public static boolean hasAccessTo(Set<Integer> accessTo, Set<EmotesetInfo> emotesets) {
        for (EmotesetInfo set : emotesets) {
            if (!accessTo.contains(set.emoteset_id)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the stream from the emote, or from the EmotesetInfo (if available).
     * 
     * @param emote The Emoticon
     * @param info The EmotesetInfo for the emote (optional)
     * @return The stream, or null if info not found
     */
    public static String getStream(Emoticon emote, EmotesetInfo info) {
        String stream = Helper.isValidStream(emote.getStream()) ? emote.getStream() : null;
        if (stream == null && info != null && info.stream_name != null) {
            stream = info.stream_name;
        }
        return stream;
    }
    
    /**
     * Get the emoteset from the emote, or from the EmotesetInfo (if available).
     * 
     * @param emote The Emoticon
     * @param info The EmotesetInfo for the emote (optional)
     * @return 
     */
    public static int getSet(Emoticon emote, EmotesetInfo info) {
        int emoteset = emote.emoteSet;
        if (emoteset == Emoticon.SET_UNKNOWN && info != null) {
            emoteset = info.emoteset_id;
        }
        return emoteset;
    }
    
    /**
     * Get a description of the emote type for the given Emoticon, using the
     * EmotesetInfo as well.
     * 
     * @param emote The Emoticon
     * @param info The EmotesetInfo (optional)
     * @param includeStream Whether to include the stream name in the output for
     * Subemotes
     * @return A non-null string
     */
    public static String getEmoteType(Emoticon emote, EmotesetInfo info, boolean includeStream) {
        int emoteset = getSet(emote, info);
        if (emote.hasGlobalEmoteset()) {
            return "Twitch Global";
        } else if (Emoticons.isTurboEmoteset(emoteset)) {
            return "Turbo Emoticon";
        } else if (info == null) {
            return "Unknown Emote";
        } else if (info.stream_name != null && !info.stream_name.equals("Twitch")) {
            if (includeStream) {
                return "Subemote (" + info.stream_name + ")";
            } else {
                return "Subemote";
            }
        } else {
            return "Other Twitch Emote";
        }
    }
    
    public void debug() {
        System.out.println(byId.debug());
    }
    
    private static Map<Integer, EmotesetInfo> parseById(String input) {
        try {
            Map<Integer, EmotesetInfo> result = new HashMap<>();
            JSONParser parser = new JSONParser();
            JSONArray array = (JSONArray)parser.parse(input);
            for (Object o : array) {
                if (o instanceof JSONObject) {
                    JSONObject item = (JSONObject)o;
                    int id = JSONUtil.getInteger(item, "id", -1);
                    int emoteset = JSONUtil.getInteger(item, "emoticon_set", Emoticon.SET_UNKNOWN);
                    String channel_id = JSONUtil.getString(item, "channel_id");
                    String channel_name = JSONUtil.getString(item, "channel_name");
                    if (id != -1 && emoteset != -1) {
                        result.put(id, new EmotesetInfo(emoteset, channel_name, channel_id, null));
                    }
                }
            }
            return result;
        } catch (ParseException ex) {
            return null;
        }
    }
    
    private static Map<Integer, EmotesetInfo> parseByStream(String input) {
        try {
            Map<Integer, EmotesetInfo> result = new HashMap<>();
            JSONParser parser = new JSONParser();
            JSONObject data = (JSONObject)parser.parse(input);
            String channel_id = JSONUtil.getString(data, "channel_id");
            String channel_name = JSONUtil.getString(data, "channel_name");
            Map<Integer, String> plans = getPlans(data);
            
            JSONArray emotes = (JSONArray)data.get("emotes");
            for (Object o : emotes) {
                if (o instanceof JSONObject) {
                    JSONObject item = (JSONObject)o;
                    int id = JSONUtil.getInteger(item, "id", -1);
                    int emoteset = JSONUtil.getInteger(item, "emoticon_set", -1);
                    if (id != -1 && emoteset != -1) {
                        result.put(id, new EmotesetInfo(emoteset, channel_name, channel_id, plans.get(emoteset)));
                    }
                }
            }
            return result;
        } catch (ParseException ex) {
            return null;
        }
    }
    
    private static Map<Integer, String> getPlans(JSONObject data) {
        Map<Integer, String> result = new HashMap<>();
        JSONObject plans = (JSONObject) data.get("plans");
        try {
            for (Object o : plans.entrySet()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) o;
                if (entry.getKey() != null && entry.getValue() != null) {
                    int emoteset = Integer.parseInt(entry.getValue());
                    String plan = entry.getKey();
                    result.put(emoteset, plan);
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing plans: "+plans);
        }
        return result;
    }
    
    private static Map<Integer, EmotesetInfo> parseBySet(String input) {
        try {
            Map<Integer, EmotesetInfo> result = new HashMap<>();
            JSONParser parser = new JSONParser();
            JSONArray array = (JSONArray)parser.parse(input);
            for (Object o : array) {
                if (o instanceof JSONObject) {
                    JSONObject item = (JSONObject)o;
                    int emoteset = JSONUtil.parseInteger(item, "set_id", -1);
                    String channel_id = JSONUtil.getString(item, "channel_id");
                    String channel_name = JSONUtil.getString(item, "channel_name");
                    if (emoteset != -1) {
                        result.put(emoteset, new EmotesetInfo(emoteset, channel_name, channel_id, null));
                    }
                }
            }
            return result;
        } catch (ParseException ex) {
            return null;
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
//        System.out.println(TwitchEmotesApi.api.requestById(result -> {
//            System.out.println("Requested: "+result);
//        }, 1));
//        
//        Thread.sleep(5000);
//        
//        System.out.println(TwitchEmotesApi.api.requestById(result -> {
//            System.out.println("Requested: "+result);
//        }, 1));
        
//        System.out.println(TwitchEmotesApi.api.requestByStream(result -> {
//            System.out.println("Requested: "+result);
//        }, "23161357"));
        
//        Set<Integer> test = new HashSet<>();
//        test.add(100);
//        test.add(29712);
//        test.add(1198191);
//        TwitchEmotesApi.api.requestBySets(result -> {
//            System.out.println("Requested: "+result);
//        }, test);
        
        TwitchEmotesApi.api.requestByStreamId(result -> {
            System.out.println("0");
        }, "22680841");
        System.out.println("1");
//        Thread.sleep(5000);
        TwitchEmotesApi.api.requestByStreamId(result -> {
            System.out.println("a");
        }, "22680841");
        System.out.println("b");
    }
            
    
    public static class EmotesetInfo {

        public final int emoteset_id;
        public final String product;
        public final String stream_name;
        public final String stream_id;

        public EmotesetInfo(int emoteset_id, String stream_name, String stream_id, String product) {
            this.emoteset_id = emoteset_id;
            this.product = product;
            this.stream_name = stream_name;
            this.stream_id = stream_id;
        }

        @Override
        public String toString() {
            return String.format("%d(%s,%s,%s)", emoteset_id, stream_name, stream_id, product);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final EmotesetInfo other = (EmotesetInfo) obj;
            if (this.emoteset_id != other.emoteset_id) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + this.emoteset_id;
            return hash;
        }
        
    }
    
//    public static class Emote {
//        
//        public final int emote_id;
//        public final int emoteset_id;
//        public final String stream_name;
//        public final String stream_id;
//        
//        public Emote(int emote_id, int emoteset_id, String stream_name, String stream_id) {
//            this.emote_id = emote_id;
//            this.emoteset_id = emoteset_id;
//            this.stream_name = stream_name;
//            this.stream_id = stream_id;
//        }
//        
//        @Override
//        public String toString() {
//            return String.format("%d:%d[%s,%s]", emote_id, emoteset_id, stream_name, stream_id);
//        }
//    }
    
}
