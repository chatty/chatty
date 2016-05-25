
package chatty.util.ffz;

import chatty.Chatty;
import chatty.Helper;
import chatty.util.JSONUtil;
import chatty.util.UrlRequest;
import chatty.util.api.Emoticon;
import chatty.util.api.EmoticonUpdate;
import chatty.util.settings.Settings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * FFZ Websocket Manager. Handling the more top-level stuff of parsing actual
 * commands and maintaining the list of rooms to sub to.
 * 
 * @author tduva
 */
public class WebsocketManager {
    
    private final static Logger LOGGER = Logger.getLogger(WebsocketManager.class.getName());
    
    private final static String VERSION = "chatty_"+Chatty.VERSION;
    
    private final Set<String> rooms = new HashSet<>();
    private final Map<String, Set<Integer>> prevEmotesets = new HashMap<>();
    private final WebsocketClient c;
    
    private final JSONParser parser = new JSONParser();
    private final FrankerFaceZListener listener;
    
    private long serverTimeOffset;
    
    private final Settings settings;
    
    public WebsocketManager(final FrankerFaceZListener listener,
            final Settings settings) {
        this.listener = listener;
        this.settings = settings;
        
        c = new WebsocketClient(new WebsocketClient.MessageHandler() {

            @Override
            public void handleReceived(String text) {
                listener.wsInfo(">> "+text);
            }

            @Override
            public void handleCommand(int id, String command, String params) {
                if (id == 1 && command.equals("ok")) {
                    // ok ["uuid",serverTime]
                    parseHelloResponse(params);
                }
                if (command.equals("follow_sets")) {
                    // follow_sets {\"sirstendec\": [3779]}
                    parseFollowsets(params);
                }
            }

            @Override
            public void handleConnect() {
                c.sendCommand("hello", JSONUtil.listToJSON(VERSION, false));
                for (String room : rooms) {
                    subRoom(room);
                }
                c.sendCommand("ready", "0");
            }
            
            @Override
            public void handleSent(String sent) {
                listener.wsInfo("SENT: "+sent);
            }
        });
    }
    
    private static String[] getServers() {
        return new String[] {
            "catbag.frankerfacez.com",
            "andknuckles.frankerfacez.com",
            "tuturu.frankerfacez.com"
        };
    }
    
    public String getStatus() {
        return c.getStatus();
    }
    
    public synchronized void connect() {
        if (!settings.getBoolean("ffz") || !settings.getBoolean("ffzEvent")) {
            return;
        }
        c.connect(getServers());
    }
    
    public synchronized void disconnect() {
        c.disonnect();
    }
    
    /**
     * Subscribe to a room.
     * 
     * @param room 
     */
    public synchronized void addRoom(String room) {
        if (!Helper.validateStream(room)) {
            return;
        }
        connect();
        room = room.toLowerCase();
        if (rooms.add(room)) {
            subRoom(room);
        }
    }
    
    /**
     * Remove subscription to a room. This also removes all current FFZ EVENT
     * emotes from that room.
     * 
     * @param room 
     */
    public synchronized void removeRoom(String room) {
        if (!Helper.validateStream(room)) {
            return;
        }
        room = room.toLowerCase();
        if (rooms.remove(room)) {
            unsubRoom(room);
            removeEmotes(room);
            prevEmotesets.remove(room);
        }
    }
    
    private void subRoom(String room) {
        c.sendCommand("sub", "\"room."+room+"\"");
    }
    
    private void unsubRoom(String room) {
        c.sendCommand("unsub", "\"room."+room+"\"");
    }
    
    private void parseHelloResponse(String json) {
        try {
            JSONArray data = (JSONArray) parser.parse(json);
            String clientId = (String)data.get(0);
            long serverTime = ((Number)data.get(1)).longValue();
            serverTimeOffset = System.currentTimeMillis() - serverTime;
            LOGGER.info("[FFZ-WS] Server Time Offset: "+serverTimeOffset);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing 'hello' response: "+ex);
        }
    }
    
    /**
     * Parses the "follow_sets" message from the server and updates the emotes
     * accordingly, but only if the emotesets have changed.
     * 
     * @param json 
     */
    private void parseFollowsets(String json) {
        try {
            JSONObject data = (JSONObject) parser.parse(json);
            for (Object key : data.keySet()) {
                String room = ((String)key).toLowerCase();
                Set<Integer> emotesets = new HashSet<>();
                for (Object set : (JSONArray)data.get(key)) {
                    emotesets.add(((Number)set).intValue());
                }
                if (!prevEmotesets.containsKey(room) || !prevEmotesets.get(room).equals(emotesets)) {
                    fetchEmotes(room, emotesets);
                    prevEmotesets.put(room, emotesets);
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("[FFZ] Error parsing 'follow_sets': "+ex+" ["+json+"]");
        }
    }
    
    /**
     * Fetches all emotes of the given emotesets, useable in the given room and
     * sends them to the listener (removing previous EVENT emotes in that room).
     * 
     * @param room The room the emotes will be useable in
     * @param emotesets The set of FFZ emotesets
     */
    private void fetchEmotes(String room, Set<Integer> emotesets) {
        Set<Emoticon> result = new HashSet<>();
        for (int set : emotesets) {
            Set<Emoticon> fetched = fetchEmoteSet(room, set);
            for (Emoticon emoteToAdd : fetched) {
                // Add info to already existing emotes
                for (Emoticon emote : result) {
                    if (emote.equals(emoteToAdd)) {
                        emote.addInfos(emoteToAdd.getInfos());
                        break;
                    }
                }
                // Add emote to result if not already added (Set)
                result.add(emoteToAdd);
            }
        }
        EmoticonUpdate update = new EmoticonUpdate(result,
                Emoticon.Type.FFZ,
                Emoticon.SubType.EVENT,
                room);
        listener.channelEmoticonsReceived(update);
    }
    
    /**
     * Sends an update to the listener to remove all FFZ EVENT emotes from the
     * given room.
     * 
     * @param room The channel the emotes should be removed from
     */
    private void removeEmotes(String room) {
        EmoticonUpdate update = new EmoticonUpdate(null,
                Emoticon.Type.FFZ,
                Emoticon.SubType.EVENT,
                room);
        listener.channelEmoticonsReceived(update);
    }
    
    /**
     * Get the emotes from a specific emoteset, useable in the given room.
     * 
     * @param room The channel the emotes should be useable in
     * @param emoteset The FFZ emoteset to fetch
     * @return A set of emotes or an empty set if an error occured
     */
    private Set<Emoticon> fetchEmoteSet(String room, int emoteset) {
        UrlRequest r = new UrlRequest("https://api.frankerfacez.com/v1/set/"+emoteset);
        r.run();
        if (r.getResult() != null) {
            Set<Emoticon> emotes = FrankerFaceZParsing.parseSetEmotes(
                    r.getResult(), Emoticon.SubType.EVENT, room);
            return emotes;
        }
        return new HashSet<>();
    }
    
}
