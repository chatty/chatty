
package chatty.util.api;

import chatty.util.Debugging;
import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.TwitchEmotesApi.EmotesetInfo;
import chatty.util.api.EmoticonUpdate.Source;
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
public class EmoticonParsing {
    
    private static final Logger LOGGER = Logger.getLogger(EmoticonParsing.class.getName());
    
    protected static EmoticonUpdate parseEmoteList(String json, EmoticonUpdate.Source source, String streamName, String streamId) {
        if (json == null) {
            return null;
        }
        try {
            Set<Emoticon> emotes = new HashSet<>();
            Set<String> emotesets = new HashSet<>();
            Set<EmotesetInfo> setInfos = new HashSet<>();
            
            //--------------------------
            // Parsing
            //--------------------------
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray data = (JSONArray) root.get("data");
            for (Object o : data) {
                JSONObject entry = (JSONObject) o;
                String id = JSONUtil.getString(entry, "id");
                String code = JSONUtil.getString(entry, "name");
                String type = JSONUtil.getString(entry, "emote_type", "");
                String set = JSONUtil.getString(entry, "emote_set_id");
                String tier = JSONUtil.getString(entry, "tier");
                String owner_id = JSONUtil.getString(entry, "owner_id");
                if (owner_id == null) {
                    // Channel emotes API doesn't include owner_id
                    owner_id = streamId;
                }
                Emoticon.Builder builder = new Emoticon.Builder(Emoticon.Type.TWITCH, code, null);
                builder.setStringId(id);
                builder.setEmoteset(set);
                builder.setStream(streamName);
                String info = null;
                switch (type) {
                    case "subscriptions":
                        if (!StringUtil.isNullOrEmpty(tier)) {
                            info = "Tier "+tier.substring(0, 1);
                        }
                        else {
                            info = "Subemote";
                        }
                        break;
                    case "bitstier":
                        info = "Bits";
                        break;
                    case "follower":
                        info = "Follower";
                        builder.setSubType(Emoticon.SubType.FOLLOWER);
                        builder.addStreamRestriction(streamName);
                        break;
                    default:
                        info = type;
                }
                builder.setEmotesetInfo(info);
                
                boolean add = true;
                if (type.equals("follower") && streamName == null) {
                    // Stream restriction is required
                    add = false;
                }
                if (add) {
                    emotes.add(builder.build());
                    emotesets.add(set);
                    // Not all information may be available, but add anyway
                    EmotesetInfo setInfo = new EmotesetInfo(set, streamName, owner_id, info);
                    setInfos.add(setInfo);
                }
            }
            //--------------------------
            // Result
            //--------------------------
            EmoticonUpdate.Builder updateBuilder = new EmoticonUpdate.Builder(emotes);
            if (source == Source.CHANNEL) {
                updateBuilder.setTypeToRemove(Emoticon.Type.TWITCH);
                updateBuilder.setsSetsAddedToRemove(emotesets);
                updateBuilder.setSource(source);
            }
            if (!setInfos.isEmpty()) {
                updateBuilder.setSetInfos(setInfos);
            }
            return updateBuilder.build();
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing emoticons by sets: " + ex);
        }
        return null;
    }
    
    /**
     * Parse result of ?emotesets=0 request.
     * 
     * @param json
     * @return 
     */
    protected static EmoticonUpdate parseEmoticonSets(String json, EmoticonUpdate.Source source) {
        if (json == null) {
            return null;
        }
        Set<Emoticon> emotes = new HashSet<>();
        Set<String> emotesets = new HashSet<>();
        JSONParser parser = new JSONParser();
        int errors = 0;
        try {
            JSONObject root = (JSONObject)parser.parse(json);
            JSONObject sets = (JSONObject)root.get("emoticon_sets");
            for (Object key : sets.keySet()) {
                String emoteSet = (String)key;
                JSONArray emoticons = (JSONArray)sets.get(key);
                for (Object obj : emoticons) {
                    JSONObject emote_json = (JSONObject)obj;
                    Emoticon emote = parseEmoticon(emote_json, emoteSet);
                    if (emote == null) {
                        if (errors < 10) {
                            LOGGER.warning("Error loading emote: "+emote_json);
                        }
                        errors++;
                    } else {
                        if (!Debugging.isEnabled("et") || !emote.code.equals("joshO")) {
                            emotes.add(emote);
                        }
                    }
                }
                emotesets.add(emoteSet);
            }
            if (errors > 0) {
                LOGGER.warning(errors+" emotes couldn't be loaded");
            }
            if (errors > 100) {
                return null;
            }
            EmoticonUpdate.Builder builder = new EmoticonUpdate.Builder(emotes);
            if (source == EmoticonUpdate.Source.USER_EMOTES) {
                /**
                 * Don't remove, since some sets may not contain all emotes in
                 * this old API. Still include emotesets though for adding to
                 * usable emotesets.
                 */
                builder.setSetsAdded(emotesets);
                builder.setSource(source);
            }
            return builder.build();
        } catch (Exception ex) {
            LOGGER.warning("Error parsing emoticons by sets: "+ex);
        }
        return null;
    }
    
    /**
     * Parses an Emoticon from the given JSONObject.
     * 
     * @param emote The JSONObject containing the emoticon data
     * @return The Emoticon object or null if an error occured
     */
    private static Emoticon parseEmoticon(JSONObject emote, String emoteSet) {
        try {
            String code = (String)emote.get("code");
            int id = ((Number)emote.get("id")).intValue();
            Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.TWITCH, code, null);
            if (emote.get("emoticon_set") != null) {
                emoteSet = String.valueOf(((Number)emote.get("emoticon_set")).longValue());
            }
            b.setEmoteset(emoteSet);
            b.setStringId(String.valueOf(id));
            return b.build();
        } catch (NullPointerException | ClassCastException ex) {
            return null;
        }
    }

}
