
package chatty.util;

import chatty.Helper;
import chatty.Usericon;
import chatty.util.api.Emoticon;
import java.util.*;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Request FrankerFaceZ emoticons and mod icons.
 * 
 * @author tduva
 */
public class FrankerFaceZ {
    
    private static final Logger LOGGER = Logger.getLogger(FrankerFaceZ.class.getName());
    
    private boolean botNamesRequested;
    
    /**
     * The channels that have already been requested in this session.
     */
    private final Set<String> alreadyRequested
            = Collections.synchronizedSet(new HashSet<String>());
    
    /**
     * The channels whose request is currently pending. Channels get removed
     * from here again once the request result is received.
     */
    private final Set<String> requestPending
            = Collections.synchronizedSet(new HashSet<String>());

    private final FrankerFaceZListener listener;
    
    private enum Type { GLOBAL, ROOM };
    
    public FrankerFaceZ(FrankerFaceZListener listener) {
        this.listener = listener;
    }
    
    /**
     * Requests the emotes for the given channel and global emotes. It only
     * requests each set of emotes once, unless {@code forcedUpdate} is true.
     * 
     * @param stream The name of the channel/stream
     * @param forcedUpdate Whether to update even if it was already requested
     */
    public synchronized void requestEmotes(String stream, boolean forcedUpdate) {
        stream = Helper.toStream(stream);
        if (stream == null || stream.isEmpty()) {
            return;
        }
        request(Type.ROOM, stream, forcedUpdate);
        requestGlobalEmotes(false);
        if (!botNamesRequested) {
            requestBotNames();
            botNamesRequested = true;
        }
    }
    
    public synchronized void requestGlobalEmotes(boolean forcedUpdate) {
        request(Type.GLOBAL, null, forcedUpdate);
    }
    
    /**
     * Issue a request of the given type and stream.
     * 
     * <p>
     * The URL which is used for the request is build from the paramters. Only
     * requests each URL once, unless {@code forcedUpdate} is true. Always
     * prevents the same URL from being requested twice at the same time.</p>
     * 
     * <p>This is not safe to be called unsynchronized, because of the way
     * check the URL for being already requested/pending is done.</p>
     *
     * @param type The type of request
     * @param stream The stream, can be [@code null} if not needed for this type
     * @param forcedUpdate Whether to request even if already requested before
     */
    private void request(final Type type, final String stream, boolean forcedUpdate) {
        final String url = getUrl(type, stream);
        if (requestPending.contains(url)
                || (alreadyRequested.contains(url) && !forcedUpdate)) {
            return;
        }
        alreadyRequested.add(url);
        requestPending.add(url);
        
        // Create request and run it in a seperate thread
        UrlRequest request = new UrlRequest() {

            @Override
            public void requestResult(String result, int responseCode) {
                requestPending.remove(url);
                parseResult(type, stream, result);
            }
        };
        request.setLabel("[FFZ]");
        request.setUrl(url);
        new Thread(request).start();
    }
    
    /**
     * Gets the URL for the given request type and stream.
     * 
     * @param type The type
     * @param stream The stream, if applicable to the type
     * @return The URL as a String
     */
    private String getUrl(Type type, String stream) {
        if (type == Type.GLOBAL) {
            return "http://api.frankerfacez.com/v1/set/global";
        } else {
            return "http://api.frankerfacez.com/v1/room/"+stream;
        }
    }
    
    private void parseResult(Type type, String stream, String result) {
        if (result == null) {
            return;
        }
        // Determine whether these emotes should be global
        final boolean global = type == Type.GLOBAL;
        String globalText = global ? "global" : "local";
        
        Set<Emoticon> emotes = new HashSet<>();
        List<Usericon> usericons = new ArrayList<>();
        if (type == Type.GLOBAL) {
            emotes = parseGlobalEmotes(result);
        } else if (type == Type.ROOM) {
            emotes = parseRoomEmotes(result);
            Usericon modIcon = parseModIcon(result);
            if (modIcon != null) {
                usericons.add(modIcon);
            }
        }
        
        LOGGER.info("[FFZ] ("+stream+", "+globalText+"): "+emotes.size()+" emotes received.");
        if (!usericons.isEmpty()) {
            LOGGER.info("[FFZ] ("+stream+"): "+usericons.size()+" usericons received.");
        }
        
        listener.channelEmoticonsReceived(emotes);
        // Return icons if mod icon was found (will be empty otherwise)
        listener.usericonsReceived(usericons);
    }

    public void requestBotNames() {
        UrlRequest request = new UrlRequest("http://cdn.frankerfacez.com/script/bots.txt") {
            
            @Override
            public void requestResult(String result, int responseCode) {
                if (result != null && responseCode == 200) {
                    String[] lines = result.split("\n");
                    Set<String> botNames = new HashSet<>();
                    for (String line : lines) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            botNames.add(line);
                        }
                    }
                    LOGGER.info("FFZ Bots: Found "+botNames.size()+" names");
                    listener.botNamesReceived(botNames);
                }
            }
        };
        request.setLabel("FFZ Bots");
        new Thread(request).start();
    }
    
    private Usericon parseModIcon(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject o = (JSONObject)parser.parse(json);
            JSONObject room = (JSONObject)o.get("room");
            String roomId = (String)room.get("id");
            String modBadgeUrl = (String)room.get("moderator_badge");
            if (modBadgeUrl == null) {
                return null;
            }
            return Usericon.createTwitchLikeIcon(Usericon.Type.MOD,
                            roomId, modBadgeUrl, Usericon.SOURCE_FFZ);
        } catch (ParseException | ClassCastException | NullPointerException ex) {
            
        }
        return null;
    }
    
    private Set<Emoticon> parseGlobalEmotes(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject o = (JSONObject)parser.parse(json);
            JSONArray defaultSets = (JSONArray)o.get("default_sets");
            JSONObject sets = (JSONObject)o.get("sets");
            for (Object setObject : defaultSets) {
                int set = ((Number)setObject).intValue();
                JSONObject setData = (JSONObject)sets.get(String.valueOf(set));
                return parseEmoteSet(setData, null);
            }
        } catch (ParseException | ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing global FFZ emotes: "+ex);
        }
        return new HashSet<>();
    }
    
    /**
     * Parse the result of a request for a single room.
     * 
     * @param json
     * @return Set of emotes, can be empty if there are no emotes or an error
     * occured
     */
    private Set<Emoticon> parseRoomEmotes(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject o = (JSONObject)parser.parse(json);
            JSONObject room = (JSONObject)o.get("room");
            String roomId = (String)room.get("id");
            int set = ((Number)room.get("set")).intValue();
            JSONObject sets = (JSONObject)o.get("sets");
            JSONObject setData = (JSONObject)sets.get(String.valueOf(set));
            return parseEmoteSet(setData, roomId);
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
     * @return The set of parsed emotes, can be empty if no emotes were found or
     * an error occured
     */
    private Set<Emoticon> parseEmoteSet(JSONObject setData, String streamRestriction) {
        try {
            JSONArray emoticons = (JSONArray)setData.get("emoticons");
            String title = JSONUtil.getString(setData, "title");
            return parseEmoticons(emoticons, streamRestriction, title);
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
     * @param info 
     * @return 
     */
    private Set<Emoticon> parseEmoticons(JSONArray emotes, String streamRestriction, String info) {
        Set<Emoticon> result = new HashSet<>();
        if (emotes != null) {
            for (Object emote : emotes) {
                if (emote != null && emote instanceof JSONObject) {
                    Emoticon createdEmote = parseEmote((JSONObject)emote, streamRestriction, info);
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
     * @return The Emoticon object or null if an error occured
     */
    public static Emoticon parseEmote(JSONObject emote, String streamRestriction,
            String info) {
        try {
            // Base information
            int width = JSONUtil.getInteger(emote, "width", -1);
            int height = JSONUtil.getInteger(emote, "height", -1);
            String code = (String)emote.get("name");
            JSONObject urls = (JSONObject)emote.get("urls");
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
            b.setNumericId(id);
            b.addStreamRestriction(streamRestriction);
            b.setInfo(info);
            return b.build();
        } catch (ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing FFZ emote: "+ex);
            return null;
        }
    }
    
}
