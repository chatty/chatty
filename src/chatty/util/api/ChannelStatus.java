
package chatty.util.api;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Channel status such as title and category. The new API has this more
 * separated from other information such as account creation.
 * 
 * @author tduva
 */
public class ChannelStatus {
    
    private static final Logger LOGGER = Logger.getLogger(ChannelStatus.class.getName());

    public final String channelId;
    public final String channelLogin;
    public final String channelName;
    public final String title;
    public final StreamCategory category;
    public final List<StreamTag> tags;
    
    public ChannelStatus(String channelId, String channelLogin, String channelName, String title, StreamCategory category, List<StreamTag> tags) {
        this.channelId = channelId;
        this.channelLogin = channelLogin;
        this.channelName = channelName;
        this.title = title;
        this.category = category;
        this.tags = tags;
    }
    
    public static ChannelStatus createInvalid(String channelId, String channelLogin) {
        return new ChannelStatus(channelId, channelLogin, null, null, null, null);
    }
    
    public static ChannelStatus createPut(String channelLogin, String title, StreamCategory category, List<StreamTag> tags) {
        if (category == null) {
            category = new StreamCategory("", "");
        }
        if (tags == null) {
            tags = new ArrayList<>();
        }
        return new ChannelStatus(null, channelLogin, null, title, category, tags);
    }
    
    public boolean isValid() {
        return StringUtil.isNullOrEmpty(channelId, channelLogin, title) && category != null && tags != null;
    }
    
    public boolean hasCategoryId() {
        return category != null && category.hasId();
    }
    
    /**
     * Creates a new ChannelStatus object with the StreamCategory replaced by
     * the given category.
     * 
     * @param category
     * @return 
     */
    public ChannelStatus changeCategory(StreamCategory category) {
        return new ChannelStatus(channelId, channelLogin, channelName, title, category, tags);
    }
    
    public static List<ChannelStatus> parseJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            List<ChannelStatus> result = new ArrayList<>();
            
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            JSONArray data = (JSONArray) root.get("data");
            for (Object item : data) {
                if (item instanceof JSONObject) {
                    JSONObject channel = (JSONObject) item;
                    String channelId = JSONUtil.getString(channel, "broadcaster_id");
                    String channelLogin = JSONUtil.getString(channel, "broadcaster_login");
                    String channelName = JSONUtil.getString(channel, "broadcaster_name");
                    String title = JSONUtil.getString(channel, "title");
                    String gameId = JSONUtil.getString(channel, "game_id", "");
                    if (gameId.equals("0")) {
                        // Setting "0" is also legal, so just in case it might also be returned like this
                        gameId = "";
                    }
                    String gameName = JSONUtil.getString(channel, "game_name", "");
                    List<String> tagsArray = JSONUtil.getStringList(channel, "tags");
                    List<StreamTag> tags = new ArrayList<>();
                    for (String tag : tagsArray) {
                        tags.add(new StreamTag(tag));
                    }
                    
                    if (!StringUtil.isNullOrEmpty(channelId, channelLogin)) {
                        result.add(new ChannelStatus(channelId, channelLogin, channelName, title, new StreamCategory(gameId, gameName), tags));
                    }
                    else {
                        LOGGER.warning("Error parsing ChannelStatus: Invalid data");
                    }
                }
            }
            return result;
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing ChannelStatus: "+ex);
        }
        return null;
    }
    
    public String makePutJson() {
        List<String> tags2 = new ArrayList<>();
        tags.forEach(t -> tags2.add(t.getName()));
        return JSONUtil.listMapToJSON(
                "title", title,
                "game_id", category.id,
                "tags", tags2
        );
    }
    
    public static class StreamTag implements Comparable<StreamTag> {
        
        public static final StreamTag EMPTY = new StreamTag("");
        
        private final String name;
        
        public StreamTag(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDisplayName() {
            return name;
        }
        
        public boolean isValid() {
            return !StringUtil.isNullOrEmpty(name) && !name.contains(" ");
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StreamTag other = (StreamTag) obj;
            return Objects.equals(this.name, other.name);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public int compareTo(StreamTag o) {
            return name.compareToIgnoreCase(o.name);
        }

    }
    
}
