
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
                Emoticon.Builder builder = new Emoticon.Builder(Emoticon.Type.TWITCH, code);
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
                        break;
                    default:
                        info = type;
                }
                builder.setEmotesetInfo(info);
                
                boolean add = true;
                if (type.equals("smilies") && !Debugging.isEnabled("smilies+")) {
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
            if (source == Source.HELIX_CHANNEL) {
                updateBuilder.setTypeToRemove(Emoticon.Type.TWITCH);
                updateBuilder.setsSetsAddedToRemove(emotesets);
                updateBuilder.setSource(source);
            }
            else {
                updateBuilder.setSetsAdded(emotesets);
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

}
