
package chatty.util.api;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import chatty.util.api.StreamLabels.StreamLabel;
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
    public final List<StreamLabel> labels;
    
    public ChannelStatus(String channelId, String channelLogin, String channelName, String title, StreamCategory category, List<StreamTag> tags, List<StreamLabel> labels) {
        this.channelId = channelId;
        this.channelLogin = channelLogin;
        this.channelName = channelName;
        this.title = title;
        this.category = category;
        this.tags = tags;
        this.labels = labels;
    }
    
    public static ChannelStatus createInvalid(String channelId, String channelLogin) {
        return new ChannelStatus(channelId, channelLogin, null, null, null, null, null);
    }
    
    public static ChannelStatus createPut(String channelLogin, String title, StreamCategory category, List<StreamTag> tags, List<StreamLabel> labels) {
        if (category == null) {
            category = new StreamCategory("", "");
        }
        if (tags == null) {
            tags = new ArrayList<>();
        }
        if (labels == null) {
            labels = new ArrayList<>();
        }
        else {
            labels = new ArrayList<>(labels);
        }
        labels.removeIf(label -> !label.isEditable());
        return new ChannelStatus(null, channelLogin, null, title, category, tags, labels);
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
        return new ChannelStatus(channelId, channelLogin, channelName, title, category, tags, labels);
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
                    
                    List<String> labelsArray = JSONUtil.getStringList(channel, "content_classification_labels");
                    List<StreamLabel> labels = new ArrayList<>();
                    for (String id : labelsArray) {
                        labels.add(new StreamLabel(id));
                    }
                    
                    if (!StringUtil.isNullOrEmpty(channelId, channelLogin)) {
                        result.add(new ChannelStatus(channelId, channelLogin, channelName, title, new StreamCategory(gameId, gameName), tags, labels));
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
        
        List<JSONObject> labels2 = new ArrayList<>();
        for (StreamLabel label : StreamLabels.getAvailableLabels()) {
            if (label.isEditable()) {
                labels2.add(JSONUtil.listMapToJSONObject(
                        "id", label.getId(),
                        "is_enabled", labels.contains(label) ? "true" : "false")
                );
            }
        }
        
        return JSONUtil.listMapToJSON(
                "title", title,
                "game_id", category.id,
                "tags", tags2,
                "content_classification_labels", labels2
        );
    }
    
    public boolean sameStatus(ChannelStatus other) {
        String thisTitle = StringUtil.trim(title);
        String otherTitle = StringUtil.trim(other.title);
        return Objects.equals(thisTitle, otherTitle) && Objects.equals(category, other.category) && Objects.equals(tags, other.tags);
    }
    
    public String getStatusDifference(ChannelStatus other) {
        String thisTitle = StringUtil.trim(title);
        String otherTitle = StringUtil.trim(other.title);
        String difference = "";
        if (!Objects.equals(thisTitle, otherTitle)) {
            difference = StringUtil.append(difference, ", ", "title");
        }
        if (!Objects.equals(category, other.category)) {
            difference = StringUtil.append(difference, ", ", "category");
        }
        if (!Objects.equals(tags, other.tags)) {
            difference = StringUtil.append(difference, ", ", "tags");
        }
        if (!Objects.equals(StreamLabels.copyEditableLabelsOnly(labels), StreamLabels.copyEditableLabelsOnly(other.labels))) {
            difference = StringUtil.append(difference, ", ", "labels");
        }
        return difference;
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
