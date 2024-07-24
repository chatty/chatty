
package chatty.util;

import chatty.Helper;
import chatty.util.api.Emoticon;
import chatty.util.api.EmoticonUpdate;
import chatty.util.api.TwitchApi;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Requests and parses the BTTV emotes.
 * 
 * @author tduva
 */
public class BTTVEmotes {
    
    private static final Logger LOGGER = Logger.getLogger(BTTVEmotes.class.getName());
    
    private static final String URL_GLOBAL = "https://api.betterttv.net/3/cached/emotes/global";
    private static final String URL_CHANNEL = "https://api.betterttv.net/3/cached/users/twitch/";
    public static final String TEMPLATE = "https://cdn.betterttv.net/emote/{{id}}/{{image}}";
    
    public static final String GLOBAL = "$global$";
    
    private final EmoticonListener listener;
    private final TwitchApi api;
    
    public BTTVEmotes(EmoticonListener listener, TwitchApi api) {
        this.listener = listener;
        this.api = api;
    }
    
    public synchronized void requestEmotes(String channel, boolean forcedUpdate) {
        String stream = Helper.toStream(channel);
        if (!Helper.isValidStream(stream) && !GLOBAL.equals(stream)) {
            return;
        }
        if (stream.equals(GLOBAL)) {
            request(GLOBAL, null, forcedUpdate);
        } else {
            api.getUserId(r -> {
                if (!r.hasError()) {
                    request(stream, r.getId(stream), forcedUpdate);
                }
            }, stream);
        }
    }
    
    private void request(String stream, String id, boolean forceRefresh) {
        String url = getUrlForStream(id);
        if (forceRefresh) {
            requestNow(url, stream);
        }
        else {
            RetryManager.getInstance().retry(url, k -> {
                requestNow(url, stream);
            });
        }
    }
    
    private void requestNow(String url, String stream) {
        UrlRequest request = new UrlRequest(url);
        request.setLabel("BTTV");
        request.async((result, responseCode) -> {
            if (responseCode == 200 && result != null) {
                loadEmotes(result, stream);
                RetryManager.getInstance().setSuccess(url);
            }
            else if (String.valueOf(responseCode).startsWith("4")) {
                RetryManager.getInstance().setNotFound(url);
            }
            else {
                RetryManager.getInstance().setError(url);
            }
        });
    }
    
    private String getUrlForStream(String id) {
        if (id == null) {
            return URL_GLOBAL;
        }
        return URL_CHANNEL+id;
    }
    
    /**
     * Load stuff from the given JSON in the context of the given channel
     * restriction. The channel restriction can be "$global$" which means all
     * channels.
     * 
     * @param json The JSON
     * @param streamRestriction
     * @return 
     */
    private int loadEmotes(String json, String streamRestriction) {
        Set<Emoticon> emotes;
        Set<String> bots = new HashSet<>();
        if (streamRestriction != null && streamRestriction.equals(GLOBAL)) {
            streamRestriction = null;
        }
        
        if (streamRestriction == null) {
            emotes = parseGlobalEmotes(json);
        }
        else {
            emotes = parseChannelEmotes(json, streamRestriction);
            bots = parseBots(json);
        }
        LOGGER.info("|[BTTV] Found " + emotes.size() + " emotes / "+bots.size()+" bots");
        EmoticonUpdate.Builder updateBuilder = new EmoticonUpdate.Builder(emotes);
        updateBuilder.setTypeToRemove(Emoticon.Type.BTTV);
        updateBuilder.setRoomToRemove(streamRestriction);
        listener.receivedEmoticons(updateBuilder.build());
        listener.receivedBotNames(streamRestriction, bots);
        return emotes.size();
    }
    
    /**
     * Parse list of bots from the given JSON.
     * 
     * @param json
     * @return 
     */
    private static Set<String> parseBots(String json) {
        Set<String> result = new HashSet<>();
        if (json == null) {
            return result;
        }
        JSONParser parser = new JSONParser();
        try {
            JSONObject root = (JSONObject)parser.parse(json);
            JSONArray botsArray = (JSONArray)root.get("bots");
            if (botsArray == null) {
                // No bots for this channel
                return result;
            }
            for (Object o : botsArray) {
                result.add((String)o);
            }
        } catch (ParseException | ClassCastException ex) {
            LOGGER.warning("|[BTTV] Error parsing bots: "+ex);
        }
        return result;
    }
    
    private static Set<Emoticon> parseGlobalEmotes(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONArray root = (JSONArray)parser.parse(json);
            if (root != null) {
                return parseEmotes(root, null);
            }
        }
        catch (Exception ex) {
            LOGGER.warning("|[BTTV] Error parsing global emotes: "+ex);
        }
        return new HashSet<>();
    }
    
    private static Set<Emoticon> parseChannelEmotes(String json, String streamRestriction) {
        Set<Emoticon> result = new HashSet<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            if (root != null) {
                
                result.addAll(parseEmotes((JSONArray)root.get("channelEmotes"), streamRestriction));
                result.addAll(parseEmotes((JSONArray)root.get("sharedEmotes"), streamRestriction));
            }
        }
        catch (Exception ex) {
            LOGGER.warning("|[BTTV] Error parsing channel emotes: "+ex);
        }
        return result;
    }
    
    /**
     * Parse emotes from the given JSON.
     * 
     * @param json
     * @param channelRestriction
     * @return 
     */
    private static Set<Emoticon> parseEmotes(JSONArray data, String channelRestriction) {
        Set<Emoticon> emotes = new HashSet<>();
        try {
            for (Object o : data) {
                if (o instanceof JSONObject) {
                    Emoticon emote = parseEmote((JSONObject)o, TEMPLATE,
                            channelRestriction);
                    if (emote != null) {
                        emotes.add(emote);
                    }
                }
            }
        } catch (ClassCastException ex) {
            // ClassCastException is also caught in parseEmote(), so it won't
            // quit completely when one emote is invalid.
            LOGGER.warning("|[BTTV] Error parsing emotes: "+ex);
        }
        return emotes;
    }
    
    /**
     * Parse a single emote from the given JSONObject.
     * 
     * @param o The object containing the emote info
     * @param urlTemplate The URL Template to use for this emote
     * @param channelRestriction The channel restriction to use
     * @return 
     */
    private static Emoticon parseEmote(JSONObject o, String urlTemplate, String channelRestriction) {
        try {
            String code = (String)o.get("code");
            JSONObject user = (JSONObject)o.get("user");
            String userName = null;
            if (user != null) {
                userName = JSONUtil.getString(user, "name");
            }
            else {
                userName = channelRestriction;
            }
            String id = (String)o.get("id");
            String imageType = null;
            if (o.get("imageType") instanceof String) {
                imageType = (String)o.get("imageType");
            }
            
            if (code == null || code.isEmpty() || id == null || id.isEmpty()) {
                return null;
            }

            Emoticon.Builder builder = new Emoticon.Builder(Emoticon.Type.BTTV,
                    code);
            builder.setCreator(userName);
            builder.setLiteral(true);
            builder.setStringId(id);
            if (channelRestriction != null) {
                builder.addStreamRestriction(channelRestriction);
                builder.setStream(channelRestriction);
            }
            if (imageType != null && imageType.equals("gif")) {
                builder.setAnimated(true);
            }
            
            // Adds restrictions to emote (if present)
            Object restriction = o.get("restrictions");
            if (restriction != null && restriction instanceof JSONObject) {
                 JSONObject restrictions = (JSONObject)restriction;
                for (Object r : restrictions.keySet()) {
                    boolean knownAndValid = addRestriction(r, restrictions,
                            builder);
                    // Don't add emotes with unknown or invalid restrictions
                    if (!knownAndValid) {
                        return null;
                    }
                }
            }
            return builder.build();
        } catch (ClassCastException | NullPointerException ex) {
            LOGGER.warning("|[BTTV] Error parsing emote: "+o+" ["+ex+"]");
            return null;
        }
    }
    
    /**
     * Helper to add a restriction to the emote. Returns whether the restriction
     * is known and valid, so unknown restrictions can prevent the emote from
     * being added at all.
     * 
     * @param restriction The name of the restriction
     * @param restrictions The value(s) of the restriction
     * @param builder Emote builder to put the restriction in
     * @return true if the restriction is known and valid, false otherwise
     */
    private static boolean addRestriction(Object restriction,
            JSONObject restrictions, Emoticon.Builder builder) {
        try {
            String key = (String)restriction;
            if (key.equals("channels")) {
                for (Object chan : (JSONArray) restrictions.get(restriction)) {
                    if (chan instanceof String) {
                        builder.addStreamRestriction((String) chan);
                    }
                }
                return true;
            } else if (key.equals("emoticonSet")) {
                Object emoticon_set = restrictions.get(key);
                if (emoticon_set != null) {
                    if (emoticon_set instanceof String) {
                        // This also includes "night"
                        return false;
                    } else {
                        builder.setEmoteset(String.valueOf(((Number) emoticon_set).intValue()));
                        return true;
                    }
                }
            } else {
                /**
                 * Unknown or unhandled restriction, ignore restriction and
                 * return true anyway if restriction value is empty.
                 */
                Object value = restrictions.get(restriction);
                if (value == null || ((JSONArray)value).isEmpty()) {
                    return true;
                }
            }
        } catch (NullPointerException | ClassCastException ex) {
            // Don't do anything, just return false
        }
        return false;
    }
    
}
