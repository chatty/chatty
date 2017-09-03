
package chatty.util.api;

import chatty.util.JSONUtil;
import chatty.util.StringUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class CommunitiesManager {
    
    private static final Logger LOGGER = Logger.getLogger(CommunitiesManager.class.getName());
    
    private final TwitchApi api;
    
    private final Set<Community> withInfo = new HashSet<>();
    private final Set<Community> all = new HashSet<>();
    
    private final Map<String, List<CommunityListener>> listeners = new HashMap<>();
    
    private final Set<String> error = Collections.synchronizedSet(new HashSet<>());
    
    public CommunitiesManager(TwitchApi api) {
        this.api = api;
    }
    
    public synchronized Community getCachedByIdWithInfo(String id) {
        for (Community c : withInfo) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }
    
    public synchronized Community getCachedById(String id) {
        for (Community c : all) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }
    
    public void getById(String id, CommunityListener listener) {
        Community c = getCachedById(id);
        if (c != null) {
            listener.received(c, null);
        } else {
            // Not cached, request
            if (!error.contains(id)) {
                synchronized (this) {
                    if (!requestPending(id)) {
                        addListener(id, listener);
                        api.requests.getCommunityById(id, (r, e) -> {
                            if (r == null) {
                                error.add(id);
                            }
                            informListeners(id, r, e);
                        });
                    } else {
                        addListener(id, listener);
                    }
                }
            } else {
                listener.received(null, "Error (*)");
            }
        }
    }
    
    private synchronized void addListener(String id, CommunityListener listener) {
        if (!listeners.containsKey(id)) {
            listeners.put(id, new ArrayList<>());
        }
        listeners.get(id).add(listener);
    }
    
    private synchronized boolean requestPending(String id) {
        return listeners.containsKey(id);
    }
    
    private void informListeners(String id, Community r, String e) {
        for (CommunityListener l : getListeners(id)) {
            l.received(r, e);
        }
    }
    
    private synchronized List<CommunityListener> getListeners(String id) {
        List<CommunityListener> result = new ArrayList<>();
        if (listeners.containsKey(id)) {
            result = new ArrayList<>(listeners.get(id));
            listeners.remove(id);
        }
        return result;
    }
    
    /**
     * Add the given Community to the cache. Both objects with info and without
     * are accepted.
     * 
     * @param c 
     */
    public synchronized void addCommunity(Community c) {
        if (c != null) {
            all.add(c);
            if (c.getSummary() != null) {
                withInfo.add(c);
            }
        }
    }
    
    public static class Community implements Comparable<Community> {
        
        public static final Community EMPTY = new Community(null, "", "");
        
        private final String name;
        private final String display_name;
        private final String id;
        private final String summary;
        private final String rules;
        
        public Community(String id, String name, String display_name, String summary, String rules) {
            this.name = StringUtil.toLowerCase(name);
            this.display_name = display_name;
            this.id = id;
            this.summary = summary;
            this.rules = rules;
        }
        
        public Community(String id, String name, String display_name) {
            this(id, name, display_name, null, null);
        }
        
        public Community(String id, String name) {
            this(id, name, name, null, null);
        }
        
        public String getName() {
            return name;
        }
        
        public String getDisplayName() {
            return display_name;
        }
        
        public String getCapitalizedName() {
            if (display_name.equalsIgnoreCase(name)) {
                return display_name;
            }
            return name;
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
        
        /**
         * The rules in HTML format.
         * 
         * @return The rules, or null if not set
         */
        public String getRules() {
            return rules;
        }
        
        /**
         * Check if this is a valid community with at least an id and name.
         * 
         * @return 
         */
        public boolean isValid() {
            return id == null || name == null || name.isEmpty();
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
            final Community other = (Community) obj;
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
        public int compareTo(Community o) {
            if (o != null && Objects.equals(id, o.id)) {
                return 0;
            }
            if (o == null || o.name == null) {
                return -1;
            }
            if (name == null) {
                return 1;
            }
            return name.compareToIgnoreCase(o.name);
        }

    }
    
    public interface CommunityListener {
        
        /**
         * 
         * @param community The community, or null if an error occured
         * @param error A description of the error, or null if none is specified
         */
        public void received(Community community, String error);
    }
    
    public interface CommunitiesListener {
        public void received(List<Community> communities, String error);
    }
    
    public interface CommunityTopListener {
        public void received(Collection<Community> communities);
    }
    
    public interface CommunityPutListener {
        public void result(String error);
    }
    
    public static Collection<Community> parseTop(String text) {
        Collection<Community> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(text);
            JSONArray communities = (JSONArray) root.get("communities");
            
            
            for (Object o : communities) {
                JSONObject community = (JSONObject)o;
                String id = JSONUtil.getString(community, "_id");
                String name = JSONUtil.getString(community, "name");
                String display_name = JSONUtil.getString(community, "display_name");
                if (id != null && name != null) {
                    result.add(new Community(id, name, display_name));
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing Top Communities: "+ex);
        }
        return result;
    }
    
    public static Community parse(String text) {
        if (text == null) {
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject community = (JSONObject) parser.parse(text);
            return getCommunity(community);
        } catch (Exception ex) {
            LOGGER.warning("Error parsing Community: "+ex);
        }
        return null;
    }
    
    public static List<Community> parseCommunities(String text) {
        if (text == null) {
            return null;
        }
        try {
            List<Community> result = new ArrayList<>();
            
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(text);
            JSONArray list = (JSONArray) root.get("communities");
            for (Object obj : list) {
                if (obj instanceof JSONObject) {
                    Community c = getCommunity((JSONObject)obj);
                    if (c != null) {
                        result.add(c);
                    }
                }
            }
            return result;
        } catch (Exception ex) {
            LOGGER.warning("Error parsing Communities: "+ex);
        }
        return null;
    }
    
    private static Community getCommunity(JSONObject data) {
        String id = JSONUtil.getString(data, "_id");
        String name = JSONUtil.getString(data, "name");
        String display_name = JSONUtil.getString(data, "display_name");
        String summary = JSONUtil.getString(data, "description_html");
        String rules = JSONUtil.getString(data, "rules_html");
        if (id != null && name != null) {
            return new Community(id, name, display_name, summary, rules);
        }
        return null;
    }
    
}
