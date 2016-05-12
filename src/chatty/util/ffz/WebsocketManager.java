
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class WebsocketManager {
    
    private final static String VERSION = "chatty-"+Chatty.VERSION;
    private final static Logger LOGGER = Logger.getLogger(WebsocketManager.class.getName());
    
    private final Set<String> rooms = new HashSet<>();
    private final Map<String, Set<Integer>> prevEmotesets = new HashMap<>();
    private final WebsocketClient c;
    
    private final JSONParser parser = new JSONParser();
    private final FrankerFaceZListener listener;
    
    private long timeLastReceived;
    private long serverTimeOffset;
    
    private final Settings settings;
    
    public WebsocketManager(FrankerFaceZListener listener, final Settings settings) {
        this.listener = listener;
        this.settings = settings;
        
        c = new WebsocketClient(new WebsocketClient.MessageHandler() {

            @Override
            public void handleMessage(String text) {
                timeLastReceived = System.currentTimeMillis();
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
                c.sendCommand("hello", JSONUtil.listToJSON(VERSION, "false"));
                for (String room : rooms) {
                    subRoom(room);
                }
                c.sendCommand("ready", "0");
            }
        });
    }
    
    private static String getServer() {
        switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0: return "catbag.frankerfacez.com";
            case 1: return "andknuckles.frankerfacez.com";
            case 2: return "tuturu.frankerfacez.com";
        }
        return "catbag.frankerfacez.com";
    }
    
    public static void main(String[] args) {
        System.out.println(getServer());
    }
    
    public synchronized void connect() {
        // TODO: Disabled for now
//        if (!settings.getBoolean("ffz") || !settings.getBoolean("ffzEvent")) {
//            return;
//        }
//        String server = getServer();
//        c.connect("wss://"+server, "ws://"+server);
    }
    
    public synchronized void disconnect() {
        c.disonnect();
    }
    
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
            
        }
    }
    
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
            Logger.getLogger(WebsocketManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void fetchEmotes(String room, Set<Integer> emotesets) {
        Set<Emoticon> result = new HashSet<>();
        for (int set : emotesets) {
            result.addAll(fetchEmoteSet(room, set));
        }
        EmoticonUpdate update = new EmoticonUpdate(result,
                Emoticon.Type.FFZ,
                Emoticon.SubType.EVENT,
                room);
        listener.channelEmoticonsReceived(update);
    }
    
    private void removeEmotes(String room) {
        EmoticonUpdate update = new EmoticonUpdate(null,
                Emoticon.Type.FFZ,
                Emoticon.SubType.EVENT,
                room);
        listener.channelEmoticonsReceived(update);
    }
    
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
