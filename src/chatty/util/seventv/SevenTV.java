
package chatty.util.seventv;

import chatty.Helper;
import chatty.util.Debugging;
import chatty.util.EmoticonListener;
import chatty.util.JSONUtil;
import chatty.util.MiscUtil;
import chatty.util.RetryManager;
import chatty.util.StringUtil;
import chatty.util.UrlRequest;
import chatty.util.api.Emoticon;
import chatty.util.api.EmoticonUpdate;
import chatty.util.api.TwitchApi;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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
    private final TwitchApi api;

    public SevenTV(EmoticonListener listener, TwitchApi api) {
        this.listener = listener;
        this.api = api;
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
            requestEmotes(Type.GLOBAL, null, null, forcedUpdate);
        }
        else {
            // Channel
            String stream = channel;
            api.getUserId(r -> {
                if (!r.hasError()) {
                    requestEmotes(Type.CHANNEL, stream, r.getId(stream), forcedUpdate);
                }
            }, stream);
        }
    }
    
    private void requestEmotes(Type type, String stream, String streamId, boolean forcedUpdate) {
        String url = getUrl(type, streamId);
        if (forcedUpdate) {
            requestNow(type, stream, url);
        }
        else {
            RetryManager.getInstance().retry(url, k -> requestNow(type, stream, url));
        }
    }
    
    private static String getUrl(Type type, String streamId) {
        switch (type) {
            case GLOBAL:
                return "https://7tv.io/v3/emote-sets/global";
            case CHANNEL:
                return String.format("https://7tv.io/v3/users/twitch/%s",
                        streamId);
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
        Set<Emoticon> emotes = parseEmoteList(type, stream, json);
        LOGGER.info(String.format("|[SevenTV] (%s): %d emotes received.",
                stream, emotes.size()));
        
        EmoticonUpdate.Builder updateBuilder = new EmoticonUpdate.Builder(emotes);
        updateBuilder.setTypeToRemove(Emoticon.Type.SEVENTV);
        if (type == Type.CHANNEL) {
            updateBuilder.setRoomToRemove(stream);
        }
        listener.receivedEmoticons(updateBuilder.build());
    }
    
    private Set<Emoticon> parseEmoteList(Type type, String stream, String json) {
        Set<Emoticon> result = new HashSet<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONObject set = root;
            if (type == Type.CHANNEL) {
                set = (JSONObject) root.get("emote_set");
            }
            if (set != null) {
                JSONArray emotes = (JSONArray) set.get("emotes");
                for (Object o : emotes) {
                    if (o instanceof JSONObject) {
                        Emoticon emote = parseEmote(stream, (JSONObject) o);
                        if (emote != null) {
                            result.add(emote);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing SevenTV emote list: "+ex);
        }
        return result;
    }
    
    private Emoticon parseEmote(String stream, JSONObject emoteObject) {
        try {
            String id = JSONUtil.getString(emoteObject, "id");
            String code = JSONUtil.getString(emoteObject, "name");
            
            JSONObject data = (JSONObject) emoteObject.get("data");
            boolean animated = JSONUtil.getBoolean(data, "animated", false);
            JSONObject host = (JSONObject) data.get("host");

            File file = getFile(host);
            if (file == null) {
                LOGGER.warning("SevenTV emote: No file found");
                return null;
            }
            int width = file.width;
            int height = file.height;
            
            Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.SEVENTV, code, file.url);
            b.setSize(width, height);
            b.setStringId(id);
            b.setAnimated(animated);
            b.setLiteral(true);
            b.setZeroWidth(MiscUtil.isBitEnabled(JSONUtil.getInteger(data, "flags", 0), 1 << 8));
            
            if (stream != null) {
                b.addStreamRestriction(stream);
            }
            return b.build();
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing SevenTV emote: " + Debugging.getStacktrace(ex));
            return null;
        }
    }
    
    private File getFile(JSONObject host) {
        String baseUrl = JSONUtil.getString(host, "url");
        if (baseUrl == null) {
            return null;
        }
        JSONArray files = (JSONArray) host.get("files");
        for (Object o : files) {
            JSONObject file = (JSONObject) o;
            if (file.get("format").equals("WEBP")) {
                String name = (String) file.get("name");
                int width = JSONUtil.getInteger(file, "width", -1);
                int height = JSONUtil.getInteger(file, "height", -1);
                if (name.contains("1x") && width != -1 && height != -1) {
                    return new File(baseUrl+"/"+name.replace("1x", "{size}"), width, height);
                }
            }
        }
        return null;
    }
    
    private static class File {
        
        public final String url;
        public final int width;
        public final int height;
        
        private File(String url, int width, int height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }
        
    }
    
}
