
package chatty.util.seventv;

import chatty.Helper;
import chatty.util.EmoticonListener;
import chatty.util.JSONUtil;
import chatty.util.RetryManager;
import chatty.util.StringUtil;
import chatty.util.UrlRequest;
import chatty.util.api.Emoticon;
import chatty.util.api.EmoticonUpdate;
import java.util.HashSet;
import java.util.Set;
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
public class SevenTV {
    
    private static final Logger LOGGER = Logger.getLogger(SevenTV.class.getName());
    
    private static enum Type {
        GLOBAL, CHANNEL
    }
    
    private final EmoticonListener listener;

    public SevenTV(EmoticonListener listener) {
        this.listener = listener;
    }
    
    /**
     * Request emotes. Each URL is only requested once, unless
     * {@code forcedUpdate} is set, or the request failed, in which case it may
     * be attempted again.
     *
     * @param channel The channel, or {@code null} for global emotes
     * @param forcedUpdate Request emotes again, even if they already have been
     * successfully requested
     */
    public synchronized void requestEmotes(String channel, boolean forcedUpdate) {
        channel = Helper.toStream(channel);
        if (StringUtil.isNullOrEmpty(channel)) {
            // Global
            WebPUtil.runIfWebPAvailable(() -> {
                requestEmotes(Type.GLOBAL, null, forcedUpdate);
            });
        }
        else {
            // Channel
            String stream = channel;
            WebPUtil.runIfWebPAvailable(() -> {
                requestEmotes(Type.CHANNEL, stream, forcedUpdate);
            });
        }
    }
    
    private void requestEmotes(Type type, String stream, boolean forcedUpdate) {
        String url = getUrl(type, stream);
        if (forcedUpdate) {
            requestNow(type, stream, url);
        }
        else {
            RetryManager.getInstance().retry(url, k -> requestNow(type, stream, url));
        }
    }
    
    private static String getUrl(Type type, String stream) {
        switch (type) {
            case GLOBAL:
                return "https://api.7tv.app/v2/emotes/global";
            case CHANNEL:
                return String.format("https://api.7tv.app/v2/users/%s/emotes",
                        stream);
        }
        return null;
    }
    
    private void requestNow(final Type type, final String stream, String url) {
        // Create request and run it in a separate thread
        UrlRequest request = new UrlRequest();
        request.setLabel("SevenTV/"+stream);
        request.setUrl(url);
        request.async((result, responseCode) -> {
            if (Integer.toString(responseCode).startsWith("4")) {
                RetryManager.getInstance().setNotFound(url);
            }
            else if (responseCode != 200 && result == null) {
                RetryManager.getInstance().setError(url);
            }
            else {
                RetryManager.getInstance().setSuccess(url);
            }
            parseResult(type, stream, result);
        });
    }
    
    private void parseResult(Type type, String stream, String json) {
        if (json == null) {
            return;
        }
        Set<Emoticon> emotes = parseEmoteList(stream, json);
        LOGGER.info(String.format("|[SevenTV] (%s): %d emotes received.",
                stream, emotes.size()));
        
        EmoticonUpdate.Builder updateBuilder = new EmoticonUpdate.Builder(emotes);
        updateBuilder.setTypeToRemove(Emoticon.Type.SEVENTV);
        if (type == Type.CHANNEL) {
            updateBuilder.setRoomToRemove(stream);
        }
        listener.receivedEmoticons(updateBuilder.build());
    }
    
    private Set<Emoticon> parseEmoteList(String stream, String json) {
        Set<Emoticon> result = new HashSet<>();
        try {
            JSONParser parser = new JSONParser();
            JSONArray list = (JSONArray) parser.parse(json);
            for (Object o : list) {
                if (o instanceof JSONObject) {
                    Emoticon emote = parseEmote(stream, (JSONObject) o);
                    if (emote != null) {
                        result.add(emote);
                    }
                }
            }
        }
        catch (ParseException ex) {
            Logger.getLogger(SevenTV.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    private Emoticon parseEmote(String stream, JSONObject data) {
        try {
            String id = JSONUtil.getString(data, "id");
            String code = JSONUtil.getString(data, "name");

            int width = ((Number) ((JSONArray) data.get("width")).get(0)).intValue();
            int height = ((Number) ((JSONArray) data.get("height")).get(0)).intValue();
            
            Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.SEVENTV, code, null);
            b.setSize(width, height);
            b.setStringId(id);
            if (stream != null) {
                b.addStreamRestriction(stream);
            }
            return b.build();
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing SevenTV emote: " + ex);
            return null;
        }
    }
    
}
