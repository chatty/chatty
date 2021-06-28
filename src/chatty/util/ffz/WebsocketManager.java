
package chatty.util.ffz;

import chatty.Chatty;
import chatty.Helper;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.UrlRequest;
import chatty.util.UrlRequest.FullResult;
import chatty.util.api.Emoticon;
import chatty.util.api.EmoticonUpdate;
import chatty.util.settings.Settings;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
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
    
    private final Set<String> rooms = Collections.synchronizedSet(new HashSet<String>());
    private final Map<String, Set<Integer>> prevEmotesets = new HashMap<>();
    private volatile FFZWS c;
    
    private final JSONParser parser = new JSONParser();
    private final FrankerFaceZListener listener;
    
    private long serverTimeOffset;
    
    private final Settings settings;
    
    /**
     * The username that was send with the "setuser" command, during this
     * connection.
     */
    private String setUser;
    
    public WebsocketManager(final FrankerFaceZListener listener,
            final Settings settings) {
        this.listener = listener;
        this.settings = settings;
        
        /**
         * These methods may be called out of a lock on the WebsocketClient
         * instance, so to be safe this usually shouldn't call anything that
         * creates a lock which may also access the WebsocketClient instance.
         */
        
    }
    
    private static String[] getServers() {
        String main = "socket.frankerfacez.com";
        return new String[]{main};
        // TODO: Run in separate thread, since this is slow in some cases
//        try {
//            InetAddress[] servers = InetAddress.getAllByName(main);
//            String[] result = new String[servers.length];
//            for (int i = 0; i < servers.length; i++) {
//                String host = servers[i].getCanonicalHostName();
//                /**
//                 * Check that it's not an IP (not ideal, but should be ok), and
//                 * fall back to the main host if needed. It needs the hostname
//                 * for SSL host verification (which can be turned off, but I'd
//                 * rather try it like this for now).
//                 */
//                if (host.contains("frankerfacez.com")) {
//                    result[i] = host;
//                } else {
//                    result[i] = main;
//                }
//            }
//            return result;
//        } catch (Exception ex) {
//            LOGGER.warning("Failed to resolve socket.frankerfacez.com (using host directly)");
//            return new String[]{
//                main
//            };
//        }
    }
    
    /**
     * Thread-safe defensive copy of the current set of rooms.
     * 
     * @return 
     */
    private Set<String> getRooms() {
        synchronized (rooms) {
            return new HashSet<>(rooms);
        }
    }
    
    public String getStatus() {
        if (c != null) {
            return c.getStatus();
        }
        return "Not connected";
    }
    
    public boolean isConnected() {
        return c != null && c.isOpen();
    }
    
    public void connect() {
        if (!settings.getBoolean("ffz") || !settings.getBoolean("ffzEvent")
                || c != null) {
            return;
        }
        try {
            c = new FFZWS(new URI("wss://socket.frankerfacez.com"), new FFZWS.MessageHandler() {

                @Override
                public void handleReceived(String text) {
                    listener.wsInfo("--> "+text);
                }

                @Override
                public void handleSent(String sent) {
                    listener.wsInfo("<-- "+sent);
                }

                @Override
                public void handleCommand(int id, String command, String params, String originCommand) {
                    if (id == 1 && command.equals("ok")) {
                        // ok ["uuid",serverTime]
                        parseHelloResponse(params);
                    }
                    else if (command.equals("follow_sets")) {
                        // follow_sets {\"sirstendec\": [3779]}
                        parseFollowsets(params);
                    }
                    else if (command.equals("do_authorize")) {
                        // do_authorize "string"
                        parseDoAuthorize(params);
                    }
                    else if (originCommand.equals("update_follow_buttons")) {
                        if (command.equals("ok")) {
                            // ok {"updated_clients":1}
                            parseFollowingResponse(params);
                        }
                        else if (command.equals("error")) {
                            listener.wsUserInfo("Failed updating follow buttons: "+params);
                        }
                    }
                }

                @Override
                public void handleConnect() {
                    setUser = null;
                    c.sendCommand("hello", JSONUtil.listToJSON(VERSION, false));
                    for (String room : getRooms()) {
                        subRoom(room);
                    }
                    c.sendCommand("ready", "0");
                }

            });
            c.init();
        }
        catch (URISyntaxException ex) {
            Logger.getLogger(WebsocketManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void disconnect() {
        if (c != null) {
            c.disconnect();
        }
    }
    
    /**
     * Subscribe to a room.
     * 
     * @param room 
     */
    public synchronized void addRoom(String room) {
        if (!Helper.isValidStream(room)) {
            return;
        }
        connect();
        room = StringUtil.toLowerCase(room);
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
        if (!Helper.isValidStream(room)) {
            return;
        }
        room = StringUtil.toLowerCase(room);
        if (rooms.remove(room)) {
            unsubRoom(room);
            removeEmotes(room);
            prevEmotesets.remove(room);
        }
    }
    
    public synchronized void setFollowing(String user, String room, String following) {
        if (c == null) {
            return;
        }
        String[] split = following.split(",");
        JSONArray rooms = new JSONArray();
        for (String item : split) {
            item = item.trim();
            if (Helper.isValidStream(item)) {
                rooms.add(item);
            }
        }
        JSONArray root = new JSONArray();
        root.add(room);
        root.add(rooms);
        if (!user.equals(setUser)) {
            c.sendCommand("setuser", "\""+user+"\"");
            setUser = user;
        }
        c.sendCommand("update_follow_buttons", root.toJSONString());
    }
    
    private void subRoom(String room) {
        if (c != null) {
            c.sendCommand("sub", "\"room."+room+"\"");
        }
    }
    
    private void unsubRoom(String room) {
        if (c != null) {
            c.sendCommand("unsub", "\"room."+room+"\"");
        }
    }
    
    private void parseHelloResponse(String json) {
        try {
            JSONArray data = (JSONArray) parser.parse(json);
            String clientId = (String)data.get(0);
            long serverTime = ((Number)data.get(1)).longValue();
            serverTimeOffset = System.currentTimeMillis() - serverTime;
            LOGGER.info("[FFZ-WS] Server Time Offset: "+serverTimeOffset);
        } catch (Exception ex) {
            LOGGER.warning(String.format("[FFZ-WS] Error parsing 'hello' response: %s [%s]", ex, json));
        }
    }
    
    private void parseDoAuthorize(String json) {
        try {
            String code = (String) parser.parse(json);
            listener.authorizeUser(code);
        } catch (Exception ex) {
            LOGGER.warning(String.format("[FFZ-WS] Error parsing 'do_authorize' response: %s [%s]", ex, json));
        }
    }
    
    /**
     * Parses the response to the "/ffz following" command.
     * 
     * @param json 
     */
    private void parseFollowingResponse(String json) {
        try {
            JSONObject root = (JSONObject) parser.parse(json);
            int updatedClients = ((Number)root.get("updated_clients")).intValue();
            listener.wsUserInfo("Updated following buttons for "+updatedClients+" users.");
        } catch (Exception ex) {
            LOGGER.warning(String.format("[FFZ-WS] Error parsing 'update_follow_buttons' response: %s [%s]", ex, json));
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
                String room = StringUtil.toLowerCase((String)key);
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
            LOGGER.warning(String.format("[FFZ-WS] Error parsing 'follow_sets': %s [%s]", ex, json));
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
        EmoticonUpdate.Builder updateBuilder = new EmoticonUpdate.Builder(result);
        updateBuilder.setTypeToRemove(Emoticon.Type.FFZ);
        updateBuilder.setSubTypeToRemove(Emoticon.SubType.EVENT);
        updateBuilder.setRoomToRemove(room);
        listener.channelEmoticonsReceived(updateBuilder.build());
    }
    
    /**
     * Sends an update to the listener to remove all FFZ EVENT emotes from the
     * given room.
     * 
     * @param room The channel the emotes should be removed from
     */
    private void removeEmotes(String room) {
        EmoticonUpdate.Builder updateBuilder = new EmoticonUpdate.Builder(null);
        updateBuilder.setTypeToRemove(Emoticon.Type.FFZ);
        updateBuilder.setSubTypeToRemove(Emoticon.SubType.EVENT);
        updateBuilder.setRoomToRemove(room);
        listener.channelEmoticonsReceived(updateBuilder.build());
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
        FullResult result = r.sync();
        if (result.getResult() != null) {
            Set<Emoticon> emotes = FrankerFaceZParsing.parseSetEmotes(
                    result.getResult(), Emoticon.SubType.EVENT, room);
            return emotes;
        }
        return new HashSet<>();
    }
    
}
