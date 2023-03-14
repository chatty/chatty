
package chatty.util.ffz;

import chatty.util.JSONUtil;
import chatty.util.api.Emoticon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Emotes/Mod Icon parsing functions.
 * 
 * @author tduva
 */
public class FrankerFaceZParsing {
    
    private static final Logger LOGGER = Logger.getLogger(FrankerFaceZParsing.class.getName());
    
    /**
     * Parses the mod/VIP badge url.
     * 
     * Request: /room/:room
     * 
     * @param json
     * @param stream
     * @param type Which key to look up the badge URLs under
     * @param factor
     * @return The URL to the badge image, or null if none was found
     */
    public static String parseCustomBadge(String json, String stream, String type, String factor) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject o = (JSONObject)parser.parse(json);
            JSONObject room = (JSONObject)o.get("room");
            Object badgeUrls = room.get(type);
            if (badgeUrls instanceof JSONObject) {
                return JSONUtil.getString((JSONObject) badgeUrls, factor);
            }
        } catch (ParseException | ClassCastException | NullPointerException ex) {
            
        }
        return null;
    }
    
    /**
     * Parses the global emotes request.
     * 
     * Request: /set/global
     * 
     * @param json
     * @return 
     */
    public static Set<Emoticon> parseGlobalEmotes(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject o = (JSONObject)parser.parse(json);
            JSONArray defaultSets = (JSONArray)o.get("default_sets");
            JSONObject sets = (JSONObject)o.get("sets");
            for (Object setObject : defaultSets) {
                int set = ((Number)setObject).intValue();
                JSONObject setData = (JSONObject)sets.get(String.valueOf(set));
                return parseEmoteSet(setData, null, Emoticon.SubType.REGULAR);
            }
        } catch (ParseException | ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing global FFZ emotes: "+ex);
        }
        return new HashSet<>();
    }
    
    /**
     * Parse the result of a request for a single room.
     * 
     * Request: /room/:room
     * 
     * @param json The JSON to parse
     * @param stream
     * @return Set of emotes, can be empty if there are no emotes or an error
     * occured
     */
    public static Set<Emoticon> parseRoomEmotes(String json, String stream) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject o = (JSONObject)parser.parse(json);
            JSONObject room = (JSONObject)o.get("room");
            // Room ID (name) may not be correct if name changed
            String roomId = (String)room.get("id");
            int set = ((Number)room.get("set")).intValue();
            JSONObject sets = (JSONObject)o.get("sets");
            JSONObject setData = (JSONObject)sets.get(String.valueOf(set));
            return parseEmoteSet(setData, stream, Emoticon.SubType.REGULAR);
        } catch (ParseException | ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing FFZ emotes: "+ex);
        }
        return new HashSet<>();
    }
    
    /**
     * Parses the emotes of the set request.
     * 
     * Request: /set/:id
     * 
     * @param json The JSON to parse
     * @param subType
     * @return 
     */
    public static Set<Emoticon> parseSetEmotes(String json, Emoticon.SubType subType,
            String room) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            JSONObject setData = (JSONObject)root.get("set");
            return parseEmoteSet(setData, room, subType);
        } catch (ParseException | ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing FFZ emotes: "+ex);
        }
        return new HashSet<>();
    }
    
    /**
     * Parses a single emote set. Emote set in this context is a set of emotes
     * that users have access to either globally or in a single room.
     * 
     * @param setData The set JSONObject, containing the list of emotes and meta
     * information
     * @param streamRestriction The stream this emote set should be restricted
     * to or null for no restriction
     * @param subType The subType to be set for the Emoticons
     * @return The set of parsed emotes, can be empty if no emotes were found or
     * an error occured
     */
    public static Set<Emoticon> parseEmoteSet(JSONObject setData,
            String streamRestriction, Emoticon.SubType subType) {
        try {
            JSONArray emoticons = (JSONArray)setData.get("emoticons");
            String title = JSONUtil.getString(setData, "title");
            return parseEmoticons(emoticons, streamRestriction, title, subType);
        } catch (ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing FFZ emote set: "+ex);
        }
        return new HashSet<>();
    }

    /**
     * Parses the list of emotes.
     * 
     * @param emotes The JSONArray containing the emote objects
     * @param streamRestriction The stream these emotes should be restricted to
     * or null for no restriction
     * @param info The info to be set for the Emoticons
     * @param subType The subType to be set for the Emoticons
     * @return 
     */
    public static Set<Emoticon> parseEmoticons(JSONArray emotes,
            String streamRestriction, String info, Emoticon.SubType subType) {
        Set<Emoticon> result = new HashSet<>();
        if (emotes != null) {
            for (Object emote : emotes) {
                if (emote != null && emote instanceof JSONObject) {
                    Emoticon createdEmote = parseEmote((JSONObject)emote, streamRestriction, info, subType);
                    if (createdEmote != null) {
                        result.add(createdEmote);
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Parses a single emote. Required for creating an emote are the emote code,
     * the id and the x1 URL.
     * 
     * @param emote The emote data as a JSONObject
     * @param streamRestriction The stream restriction to use for this emote,
     * can be null if the emote is global
     * @param info The info to set for this emote, can be null if no info should
     * be set
     * @param subType The subType to be set for the Emoticon
     * @return The Emoticon object or null if an error occured
     */
    public static Emoticon parseEmote(JSONObject emote, String streamRestriction,
            String info, Emoticon.SubType subType) {
        try {
            // Base information
            int width = JSONUtil.getInteger(emote, "width", -1);
            int height = JSONUtil.getInteger(emote, "height", -1);
            String code = (String)emote.get("name");
            
            JSONObject urls = (JSONObject)emote.get("urls");
            boolean isAnimated = false;
            if (emote.containsKey("animated")) {
                urls = (JSONObject)emote.get("animated");
                isAnimated = true;
            }
            String url1 = (String)urls.get("1");
            String url2 = (String)urls.get("2");
            
            int id = ((Number)emote.get("id")).intValue();
            
            // Creator
            Object owner = emote.get("owner");
            String creator = null;
            if (owner != null && owner instanceof JSONObject) {
                creator = (String)((JSONObject)owner).get("display_name");
            }
            
            // Check if required data is there
            if (code == null || code.isEmpty()) {
                return null;
            }
            if (url1 == null || url1.isEmpty()) {
                return null;
            }
            
            Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.FFZ, code, url1);
            b.setX2Url(url2);
            b.setSize(width, height);
            b.setCreator(creator);
            b.setStringId(String.valueOf(id));
            b.addStreamRestriction(streamRestriction);
            b.setAnimated(isAnimated);
            b.addInfo(info);
            b.setSubType(subType);
            return b.build();
        } catch (ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing FFZ emote: "+ex);
            return null;
        }
    }
    
    public static Set<String> getBotNames(String json) {
        Set<String> result = new HashSet<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            JSONObject badge = (JSONObject)root.get("badge");
            if (badge.get("name").equals("bot")) {
                int badgeId = ((Number)badge.get("id")).intValue();
                JSONObject users = (JSONObject)root.get("users");
                JSONArray names = (JSONArray)users.get(String.valueOf(badgeId));
                for (Object item : names) {
                    if (item != null && item instanceof String) {
                        result.add((String)item);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing bot names: "+ex);
        }
        return result;
    }
    
    /**
     * Get the badge id. Used to get bot names.
     * 
     * @param json
     * @return A string containing the id, or null if an error occured
     */
    public static String getBotBadgeId(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            JSONObject badge = (JSONObject)root.get("badge");
            Number id = (Number)badge.get("id");
            if (id == null) {
                return null;
            }
            return String.valueOf(id);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing bot badge id: "+ex);
        }
        return null;
    }
    
    /**
     * Parse the badges contained in the single room response. Currently only
     * used to retrieve bot names.
     * 
     * @param json
     * @return A map with badge id as key and names as value (never null, may be
     * empty)
     */
    public static Map<String, Set<String>> parseRoomBadges(String json) {
        Map<String, Set<String>> result = new HashMap<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject o = (JSONObject)parser.parse(json);
            JSONObject room = (JSONObject)o.get("room");
            JSONObject badges = (JSONObject)room.get("user_badges");
            for (Object key : badges.keySet()) {
                Object value = badges.get(key);
                if (key instanceof String && value instanceof JSONArray) {
                    String badgeId = (String) key;
                    JSONArray names = (JSONArray)value;
                    Set<String> namesResult = new HashSet<>();
                    for (Object item : names) {
                        if (item instanceof String) {
                            namesResult.add((String)item);
                        }
                    }
                    result.put(badgeId, namesResult);
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing room badges: "+ex);
        }
        return result;
    }
    
}
