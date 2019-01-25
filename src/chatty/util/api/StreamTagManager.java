
package chatty.util.api;

import chatty.util.JSONUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
public class StreamTagManager {
    
    private static final Logger LOGGER = Logger.getLogger(StreamTagManager.class.getName());
    
    private final Set<StreamTag> withInfo = new HashSet<>();
    private final Set<StreamTag> all = new HashSet<>();
    
    public synchronized StreamTag getCachedByIdWithInfo(String id) {
        for (StreamTag c : withInfo) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }
    
    public synchronized StreamTag getCachedById(String id) {
        for (StreamTag c : all) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }
    
    /**
     * Add the given Community to the cache. Both objects with info and without
     * are accepted.
     * 
     * @param c 
     */
    public synchronized void addTag(StreamTag c) {
        if (c != null) {
            all.add(c);
            if (c.getSummary() != null) {
                withInfo.add(c);
            }
        }
    }
    
    public static class StreamTag implements Comparable<StreamTag> {
        
        public static final StreamTag EMPTY = new StreamTag(null, "", "");
        
        private final String display_name;
        private final String id;
        private final String summary;
        private final boolean isAuto;
        
        public StreamTag(String id, String display_name, String summary, boolean is_auto) {
            this.display_name = display_name;
            this.id = id;
            this.summary = summary;
            this.isAuto = is_auto;
        }
        
        public StreamTag(String id, String name, String display_name) {
            this(id, display_name, null, false);
        }
        
        public StreamTag(String id, String name) {
            this(id, name, null, false);
        }
        
        public String getDisplayName() {
            return display_name;
        }
        
        public String getId() {
            return id;
        }
        
        /**
         * The summary in HTML format.
         * 
         * @return The summary, or null if not set
         */
        public String getSummary() {
            return summary;
        }
        
        public boolean isAuto() {
            return isAuto;
        }
        
        public boolean canUserSet() {
            return isValid() && !isAuto();
        }
        
        /**
         * Check if this is a valid community with at least an id and name.
         * 
         * @return 
         */
        public boolean isValid() {
            return id != null && display_name != null && !display_name.isEmpty();
        }
        
        @Override
        public String toString() {
            return display_name;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StreamTag other = (StreamTag) obj;
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public int compareTo(StreamTag o) {
            if (o != null && Objects.equals(id, o.id)) {
                return 0;
            }
            if (o == null || o.display_name == null) {
                return -1;
            }
            if (display_name == null) {
                return 1;
            }
            return display_name.compareToIgnoreCase(o.display_name);
        }

    }
    
    public static class StreamTagsResult {
        
        public final Collection<StreamTag> tags;
        public final String cursor;
        
        public StreamTagsResult(Collection<StreamTag> data, String cursor) {
            this.tags = data;
            this.cursor = cursor;
        }
        
    }
    
    public interface StreamTagListener {
        
        /**
         * 
         * @param community The community, or null if an error occured
         * @param error A description of the error, or null if none is specified
         */
        public void received(StreamTag community, String error);
    }
    
    public interface StreamTagsListener {
        public void received(Collection<StreamTag> communities, String error);
    }
    
    public interface StreamTagPutListener {
        public void result(String error);
    }
    
    /**
     * Parse a StreamTag result with pagination and data array.
     * 
     * @param text
     * @return 
     */
    public static StreamTagsResult parseAllTags(String text) {
        if (text == null) {
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(text);
            JSONArray data = (JSONArray)root.get("data");
            Collection<StreamTag> tags = getTags(data);
            JSONObject pagination = (JSONObject)root.get("pagination");
            String cursor = null;
            if (pagination != null) {
                cursor = JSONUtil.getString(pagination, "cursor");
            }
            return new StreamTagsResult(tags, cursor);
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing Tags: "+ex);
        }
        return null;
    }
    
    /**
     * Parse an array of StreamTag objects.
     * 
     * @param data
     * @return 
     */
    private static Collection<StreamTag> getTags(JSONArray data) {
        List<StreamTag> result = new ArrayList<>();
        for (Object obj : data) {
            JSONObject values = (JSONObject)obj;
            StreamTag tag = getTag(values);
            if (tag != null) {
                result.add(tag);
            }
        }
        return result;
    }
    
    /**
     * Parse a single StreamTag entry.
     * 
     * @param data
     * @return 
     */
    private static StreamTag getTag(JSONObject data) {
        String id = JSONUtil.getString(data, "tag_id");
        JSONObject names = (JSONObject)data.get("localization_names");
        String display_name = JSONUtil.getString(names, "en-us");
        JSONObject descriptions = (JSONObject)data.get("localization_descriptions");
        String summary = JSONUtil.getString(descriptions, "en-us");
        boolean isAuto = JSONUtil.getBoolean(data, "is_auto", false);
        return new StreamTag(id, display_name, summary, isAuto);
    }
    
}
